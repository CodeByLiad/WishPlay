// Vercel Serverless Function: /api/webhook
// Verifies Paddle HMAC signature, grants/revokes Pro entitlements in Firebase Firestore.

const crypto = require("crypto");
const { getDb, admin } = require("./firebase");

const EXPECTED_AMOUNT_CENTS = 299;
const EXPECTED_CURRENCY = "USD";

function verifyPaddleSignature(rawBody, signatureHeader, secret) {
  if (!signatureHeader || !secret) return false;

  const parts = Object.fromEntries(
    signatureHeader.split(";").map((p) => p.split("="))
  );
  const ts = parts["ts"];
  const h1 = parts["h1"];
  if (!ts || !h1) return false;

  const signed = `${ts}:${rawBody}`;
  const expected = crypto.createHmac("sha256", secret).update(signed).digest("hex");
  if (expected === h1) return true;

  // Sometimes secrets in test mode start without pdl_
  if (!secret.startsWith("pdl_")) {
    const withPrefix = crypto.createHmac("sha256", `pdl_${secret}`).update(signed).digest("hex");
    if (withPrefix === h1) return true;
  }

  return false;
}

function getRawBody(req) {
  return new Promise((resolve, reject) => {
    if (typeof req.body === "string") return resolve(req.body);
    if (Buffer.isBuffer(req.body)) return resolve(req.body.toString("utf8"));

    const chunks = [];
    req.on("data", (chunk) => chunks.push(chunk));
    req.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
    req.on("error", (err) => reject(err));
  });
}

module.exports = async function handler(req, res) {
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method Not Allowed" });
  }

  try {
    const rawBody = await getRawBody(req);
    const signatureHeader = req.headers["paddle-signature"] || "";
    const secret = process.env.PADDLE_WEBHOOK_SECRET || "";

    const db = getDb();

    if (!verifyPaddleSignature(rawBody, signatureHeader, secret)) {
      console.warn("Invalid Paddle signature received.");
      try {
        await db.collection("auditLog").add({
          action: "paddle_signature_fail",
          at: admin.firestore.Timestamp.now(),
        });
      } catch (_) {}
      return res.status(401).json({ error: "Invalid signature" });
    }

    const event = JSON.parse(rawBody);
    const eventType = event.event_type || "";
    const txnData = event.data || {};
    const txnId = txnData.id || "";
    const customUserId = txnData.custom_data?.user_id || "";
    const currency = txnData.currency_code || "";
    const amountCents = Math.round(
      parseFloat(txnData.details?.totals?.grand_total || "0") * 100
    );

    console.log(`[Paddle Webhook] Received ${eventType} for user: ${customUserId}, txn: ${txnId}`);

    if (!txnId || !customUserId) {
      return res.status(400).json({ error: "Missing txnId or custom_data.user_id" });
    }

    if (eventType === "transaction.completed") {
      // Amount / Currency verification
      if (currency !== EXPECTED_CURRENCY || amountCents < EXPECTED_AMOUNT_CENTS - 1) {
        await db.collection("auditLog").add({
          action: "paddle_amount_mismatch",
          target: customUserId,
          metadata: { currency, amountCents, txnId },
          at: admin.firestore.Timestamp.now(),
        });
        return res.status(400).json({ error: "Amount or currency mismatch" });
      }

      // Idempotency check
      const existingPayment = await db
        .collection("payments")
        .where("paddleTxnId", "==", txnId)
        .limit(1)
        .get();

      if (!existingPayment.empty) {
        return res.json({ status: "already_processed" });
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
        db.collection("users").doc(customUserId).collection("entitlements").doc("pro"),
        {
          plan: "pro",
          source: "paddle",
          sourceRef: txnId,
          grantedAt: admin.firestore.Timestamp.now(),
          revokedAt: null,
        },
        { merge: true }
      );

      // Audit log
      batch.set(db.collection("auditLog").doc(), {
        action: "grant_pro_paddle",
        target: customUserId,
        metadata: { txnId, amountCents, currency },
        at: admin.firestore.Timestamp.now(),
      });

      await batch.commit();
      console.log(`[Paddle Webhook] Pro granted to user ${customUserId} successfully!`);
    } else if (
      eventType === "transaction.refunded" ||
      eventType === "transaction.chargeback"
    ) {
      // Revoke Pro
      await db
        .collection("users")
        .doc(customUserId)
        .collection("entitlements")
        .doc("pro")
        .update({ revokedAt: admin.firestore.Timestamp.now() });

      const payments = await db
        .collection("payments")
        .where("paddleTxnId", "==", txnId)
        .limit(1)
        .get();

      for (const p of payments.docs) {
        await p.ref.update({
          status: eventType === "transaction.refunded" ? "refunded" : "chargeback",
        });
      }

      await db.collection("auditLog").add({
        action:
          eventType === "transaction.refunded"
            ? "revoke_pro_refund"
            : "revoke_pro_chargeback",
        target: customUserId,
        metadata: { txnId },
        at: admin.firestore.Timestamp.now(),
      });
    }

    return res.status(200).json({ received: true });
  } catch (err) {
    console.error("Error processing Paddle webhook:", err);
    return res.status(500).json({ error: err.message });
  }
};

// Disable Vercel's automatic body parser so we can read the raw byte stream for HMAC check
module.exports.config = {
  api: {
    bodyParser: false,
  },
};
