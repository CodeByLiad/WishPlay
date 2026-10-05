// Firebase Cloud Function: paddleWebhook
// Verifies Paddle HMAC signature, grants/revokes Pro entitlements.

import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";
import { createHmac } from "crypto";

const db = admin.firestore();
const EXPECTED_AMOUNT_CENTS = 299;
const EXPECTED_CURRENCY = "USD";

function verifyPaddleSignature(
  rawBody: string,
  signatureHeader: string,
  secret: string
): boolean {
  const parts = Object.fromEntries(
    signatureHeader.split(";").map((p) => p.split("="))
  );
  const ts = parts["ts"];
  const h1 = parts["h1"];
  if (!ts || !h1) return false;
  const signed = `${ts}:${rawBody}`;
  const expected = createHmac("sha256", secret).update(signed).digest("hex");
  return expected === h1;
}

export const paddleWebhook = functions.https.onRequest(async (req, res) => {
  const secret = process.env.PADDLE_WEBHOOK_SECRET ?? "";
  const signatureHeader = req.headers["paddle-signature"] as string ?? "";
  const rawBody = JSON.stringify(req.body);

  if (!verifyPaddleSignature(rawBody, signatureHeader, secret)) {
    await db.collection("auditLog").add({
      action: "paddle_signature_fail",
      at: admin.firestore.Timestamp.now(),
    });
    res.status(401).json({ error: "Invalid signature" });
    return;
  }

  const event = req.body;
  const eventType: string = event.event_type ?? "";
  const txnData = event.data ?? {};
  const txnId: string = txnData.id ?? "";
  const customUserId: string = txnData.custom_data?.user_id ?? "";
  const currency: string = txnData.currency_code ?? "";
  const amountCents = Math.round(
    parseFloat(txnData.details?.totals?.grand_total ?? "0") * 100
  );

  if (!txnId || !customUserId) {
    res.status(400).json({ error: "Missing transaction data" });
    return;
  }

  if (eventType === "transaction.completed") {
    if (currency !== EXPECTED_CURRENCY || amountCents < EXPECTED_AMOUNT_CENTS - 1) {
      await db.collection("auditLog").add({
        action: "paddle_amount_mismatch",
        target: customUserId,
        metadata: { currency, amountCents, txnId },
        at: admin.firestore.Timestamp.now(),
      });
      res.status(400).json({ error: "Amount or currency mismatch" });
      return;
    }

    // Idempotency check
    const existingPayment = await db.collection("payments")
      .where("paddleTxnId", "==", txnId)
      .limit(1)
      .get();
    if (!existingPayment.empty) {
      res.json({ status: "already_processed" });
      return;
    }

    const batch = db.batch();

    // Record payment
    batch.set(db.collection("payments").doc(), {
      paddleTxnId: txnId,
      userId: customUserId,
      amountCents,
      currency: EXPECTED_CURRENCY,
      status: "completed",
      createdAt: admin.firestore.Timestamp.now(),
    });

    // Grant Pro
    batch.set(
      db.collection("users").doc(customUserId)
        .collection("entitlements").doc("pro"),
      {
        plan: "pro",
        source: "paddle",
        sourceRef: txnId,
        grantedAt: admin.firestore.Timestamp.now(),
        revokedAt: null,
      },
      { merge: true }
    );

    // Audit
    batch.set(db.collection("auditLog").doc(), {
      action: "grant_pro_paddle",
      target: customUserId,
      metadata: { txnId, amountCents, currency },
      at: admin.firestore.Timestamp.now(),
    });

    await batch.commit();

  } else if (
    eventType === "transaction.refunded" ||
    eventType === "transaction.chargeback"
  ) {
    await db.collection("users").doc(customUserId)
      .collection("entitlements").doc("pro")
      .update({ revokedAt: admin.firestore.Timestamp.now() });

    const payments = await db.collection("payments")
      .where("paddleTxnId", "==", txnId)
      .limit(1)
      .get();
    for (const p of payments.docs) {
      await p.ref.update({
        status: eventType === "transaction.refunded" ? "refunded" : "chargeback",
      });
    }

    await db.collection("auditLog").add({
      action: eventType === "transaction.refunded"
        ? "revoke_pro_refund"
        : "revoke_pro_chargeback",
      target: customUserId,
      metadata: { txnId },
      at: admin.firestore.Timestamp.now(),
    });
  }

  res.json({ received: true });
});

// Callable: generate a signed Paddle checkout session URL for the requesting user
export const getPaddleCheckoutUrl = functions.https.onCall(
  { enforceAppCheck: false },
  async (request) => {
    if (!request.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Must be signed in.");
    }
    const uid = request.auth.uid;
    // In production: create a Paddle checkout session via the Paddle API
    // and return the hosted URL with user_id embedded as custom_data.
    // Placeholder for now:
    const paddlePriceId = process.env.PADDLE_PRICE_ID ?? "";
    const checkoutUrl = `https://buy.paddle.com/product/${paddlePriceId}?passthrough=${uid}`;
    return { url: checkoutUrl };
  }
);
