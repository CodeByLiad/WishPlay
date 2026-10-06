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
  if (expected === h1) return true;
  if (!secret.startsWith("pdl_")) {
    const withPrefix = createHmac("sha256", `pdl_${secret}`).update(signed).digest("hex");
    if (withPrefix === h1) return true;
  }
  return false;
}

export const paddleWebhook = functions.https.onRequest(async (req, res) => {
  const secret = process.env.PADDLE_WEBHOOK_SECRET ?? "";
  const signatureHeader = (req.headers["paddle-signature"] as string) ?? "";
  const rawBody = (req as any).rawBody
    ? (req as any).rawBody.toString("utf8")
    : JSON.stringify(req.body);

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
    const apiKey = process.env.PADDLE_API_KEY ?? "";
    const priceId = process.env.PADDLE_PRICE_ID ?? "";
    const isSandbox = (process.env.PADDLE_ENV ?? "production").toLowerCase() === "sandbox";

    if (apiKey && priceId) {
      const baseUrl = isSandbox
        ? "https://sandbox-api.paddle.com"
        : "https://api.paddle.com";

      try {
        const response = await fetch(`${baseUrl}/transactions`, {
          method: "POST",
          headers: {
            "Authorization": `Bearer ${apiKey}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            items: [
              {
                price_id: priceId,
                quantity: 1,
              },
            ],
            custom_data: {
              user_id: uid,
            },
          }),
        });

        if (response.ok) {
          const data = await response.json();
          const checkoutUrl = data?.data?.checkout?.url;
          if (checkoutUrl) {
            return { url: checkoutUrl };
          }
        } else {
          const errorText = await response.text();
          console.error("Paddle API transaction error:", response.status, errorText);
        }
      } catch (e) {
        console.error("Error creating Paddle transaction:", e);
      }
    }

    // Direct hosted URL fallback
    const fallbackBase = process.env.PADDLE_CHECKOUT_URL || (priceId ? `https://buy.paddle.com/product/${priceId}` : "");
    if (!fallbackBase) {
      return { url: "" };
    }
    const separator = fallbackBase.includes("?") ? "&" : "?";
    return { url: `${fallbackBase}${separator}passthrough=${encodeURIComponent(uid)}&user_id=${encodeURIComponent(uid)}` };
  }
);

// Public HTTP function that hosts the Paddle.js checkout page for transactions
export const paddleCheckout = functions.https.onRequest(async (req, res) => {
  const ptxn = (req.query._ptxn as string) || "";
  const isSandbox = (process.env.PADDLE_ENV ?? "production").toLowerCase() === "sandbox";
  const clientToken = process.env.PADDLE_CLIENT_TOKEN ?? "";

  res.set("Content-Type", "text/html; charset=utf-8");
  res.send(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>WishPlay Pro Checkout</title>
  <script src="https://cdn.paddle.com/paddle/v2/paddle.js"></script>
  <style>
    body {
      background-color: #16132E;
      color: #F8F5FF;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      margin: 0;
      padding: 20px;
      box-sizing: border-box;
      text-align: center;
    }
    .card {
      background: #231F46;
      border: 1px solid rgba(255, 185, 56, 0.2);
      border-radius: 24px;
      padding: 36px 28px;
      max-width: 420px;
      width: 100%;
      box-shadow: 0 12px 36px rgba(0,0,0,0.4);
    }
    .badge {
      background: #FFB938;
      color: #16132E;
      font-weight: 800;
      font-size: 13px;
      padding: 6px 14px;
      border-radius: 20px;
      display: inline-block;
      margin-bottom: 16px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    h1 { margin: 0 0 10px; font-size: 24px; font-weight: 800; }
    p { color: #A5A1C8; font-size: 15px; margin: 0 0 28px; line-height: 1.5; }
    .btn {
      background: #FFB938;
      color: #16132E;
      border: none;
      padding: 14px 28px;
      border-radius: 28px;
      font-size: 16px;
      font-weight: 700;
      cursor: pointer;
      width: 100%;
      box-sizing: border-box;
      transition: transform 0.2s, opacity 0.2s;
    }
    .btn:active { transform: scale(0.98); }
    .hint { margin-top: 18px; font-size: 13px; color: #79769B; }
  </style>
</head>
<body>
  <div class="card">
    <div class="badge">WishPlay Pro</div>
    <h1>Complete Checkout</h1>
    <p>Tap below to pay securely with card or PayPal via Paddle.</p>
    <button id="payBtn" class="btn" onclick="openCheckout()">Open Secure Checkout</button>
    <div class="hint">Secured by Paddle · 256-bit encryption</div>
  </div>

  <script>
    ${isSandbox ? 'Paddle.Environment.set("sandbox");' : ''}
    if ("${clientToken}") {
      Paddle.Initialize({
        token: "${clientToken}",
        eventCallback: function(data) {
          if (data && data.name === "checkout.completed") {
            document.body.innerHTML = '<div class="card"><div class="badge">Success</div><h1>Payment Complete!</h1><p>WishPlay Pro has been unlocked. You can now return to the app.</p></div>';
          }
        }
      });
    }

    function openCheckout() {
      const ptxn = "${ptxn}";
      if (ptxn) {
        Paddle.Checkout.open({ transactionId: ptxn });
      } else {
        alert("Transaction ID missing.");
      }
    }

    window.addEventListener("load", function() {
      setTimeout(openCheckout, 400);
    });
  </script>
</body>
</html>`);
});
