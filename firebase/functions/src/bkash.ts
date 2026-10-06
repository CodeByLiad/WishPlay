// Firebase Cloud Function: submitBkash
// Accepts a bKash TrxID submission from the app, validates it, and creates a pending record.
// Callable function — requires Firebase Auth.

import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";

const db = admin.firestore();
const BKASH_AMOUNT_BDT = 349;

function maskNumber(num: string): string {
  if (num.length < 6) return num;
  return num.slice(0, 5) + "***" + num.slice(-3);
}

export const submitBkash = functions.https.onCall(
  { enforceAppCheck: false },
  async (request) => {
    if (!request.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Must be signed in.");
    }

    const uid = request.auth.uid;
    const trxId: string = (request.data.trxId ?? "").trim().toUpperCase();
    const senderNumber: string = (request.data.senderNumber ?? "").trim();
    const amountBdt: number = parseInt(request.data.amountBdt ?? "0", 10);

    // Validation
    if (!trxId || trxId.length < 6 || trxId.length > 30) {
      return { success: false, error: "invalid_trx", message: "Invalid TrxID." };
    }
    if (!/^01[3-9]\d{8}$/.test(senderNumber)) {
      return { success: false, error: "invalid_number", message: "Invalid Bangladesh mobile number." };
    }
    if (amountBdt !== BKASH_AMOUNT_BDT) {
      return { success: false, error: "invalid_amount", message: `Amount must be ${BKASH_AMOUNT_BDT} BDT.` };
    }

    // TrxID uniqueness
    const existing = await db.collection("bkashSubmissions")
      .where("trxId", "==", trxId)
      .limit(1)
      .get();
    if (!existing.empty) {
      return { success: false, error: "duplicate_trx", message: "This TrxID has already been submitted." };
    }

    // Rate limit: max 3 pending per user
    const pending = await db.collection("bkashSubmissions")
      .where("userId", "==", uid)
      .where("status", "==", "pending")
      .get();
    if (pending.size >= 3) {
      return {
        success: false,
        error: "rate_limit",
        message: "You have too many pending submissions. Please wait for review.",
      };
    }

    await db.collection("bkashSubmissions").add({
      trxId,
      userId: uid,
      amountBdt,
      senderNumber: maskNumber(senderNumber),
      status: "pending",
      createdAt: admin.firestore.Timestamp.now(),
    });

    await db.collection("auditLog").add({
      action: "bkash_submit",
      target: uid,
      metadata: { trxId, amountBdt },
      at: admin.firestore.Timestamp.now(),
    });

    return {
      success: true,
      message: "Submission received. Your payment will be reviewed within 24 hours.",
    };
  }
);

// Public HTTP function to expose bKash recipient config (not callable — no auth required)
export const bkashConfig = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  const bkashNumber = process.env.BKASH_NUMBER ?? "01978900129";
  res.json({ recipient: bkashNumber, amount_bdt: BKASH_AMOUNT_BDT });
});
