// Firebase Cloud Functions: Admin Panel APIs
// Gated strictly behind server-side admin role check.

import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";

const db = admin.firestore();

// Admin verification helper: check email or /admins/{uid} collection
async function verifyIsAdmin(auth: functions.https.CallableRequest["auth"]): Promise<string> {
  if (!auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be signed in.");
  }
  const email = (auth.token.email ?? "").toLowerCase();
  if (email === "hyathis.x@gmail.com") {
    return auth.uid;
  }
  const adminDoc = await db.collection("admins").doc(auth.uid).get();
  if (adminDoc.exists) {
    return auth.uid;
  }
  throw new functions.https.HttpsError("permission-denied", "Admin privileges required.");
}

function generatePromoCode(): string {
  const chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // Unambiguous chars
  let p1 = "";
  let p2 = "";
  for (let i = 0; i < 4; i++) {
    p1 += chars.charAt(Math.floor(Math.random() * chars.length));
    p2 += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return `WP-${p1}-${p2}`;
}

export const adminGetDashboard = functions.https.onCall(
  { enforceAppCheck: false },
  async (request) => {
    await verifyIsAdmin(request.auth);

    // 1. Fetch promo codes
    const codesSnap = await db.collection("promoCodes")
      .orderBy("createdAt", "desc")
      .get();

    let activeCodesCount = 0;
    const codes = codesSnap.docs.map(doc => {
      const data = doc.data();
      const isActive = data.active === true && (data.uses ?? 0) < (data.maxUses ?? 1);
      if (isActive) activeCodesCount++;
      return {
        code: doc.id,
        type: data.type ?? "lifetime",
        discountPct: data.discountPct ?? 0,
        maxUses: data.maxUses ?? 10,
        uses: data.uses ?? 0,
        expiresAt: data.expiresAt ? data.expiresAt.toDate().toISOString() : null,
        active: data.active ?? true,
        createdAt: data.createdAt ? data.createdAt.toDate().toISOString() : null,
      };
    });

    // 2. Fetch redeemed count
    const redemptionsSnap = await db.collection("codeRedemptions").get();
    const redeemedCount = redemptionsSnap.size;

    // 3. Fetch pending bKash submissions
    const pendingBkashSnap = await db.collection("bkashSubmissions")
      .where("status", "==", "pending")
      .orderBy("createdAt", "desc")
      .get();

    const pendingBkash = pendingBkashSnap.docs.map(doc => {
      const data = doc.data();
      return {
        id: doc.id,
        trxId: data.trxId ?? "",
        userId: data.userId ?? "",
        amountBdt: data.amountBdt ?? 349,
        senderNumber: data.senderNumber ?? "",
        status: data.status ?? "pending",
        createdAt: data.createdAt ? data.createdAt.toDate().toISOString() : null,
      };
    });

    return {
      success: true,
      stats: {
        activeCodes: activeCodesCount,
        redeemed: redeemedCount,
        pendingBkashCount: pendingBkash.length,
      },
      pendingBkash,
      codes,
    };
  }
);

export const adminReviewBkash = functions.https.onCall(
  { enforceAppCheck: false },
  async (request) => {
    const adminUid = await verifyIsAdmin(request.auth);

    const submissionId = (request.data.submissionId ?? "").trim();
    const action = (request.data.action ?? "").trim().toLowerCase(); // 'approve' | 'reject'

    if (!submissionId || (action !== "approve" && action !== "reject")) {
      throw new functions.https.HttpsError("invalid-argument", "Invalid submission ID or action.");
    }

    const submissionRef = db.collection("bkashSubmissions").doc(submissionId);
    const submissionSnap = await submissionRef.get();

    if (!submissionSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Submission not found.");
    }

    const submission = submissionSnap.data()!;
    const userId = submission.userId;
    const trxId = submission.trxId;
    const amountBdt = submission.amountBdt ?? 349;

    await db.runTransaction(async (t) => {
      // 1. Update submission status
      t.update(submissionRef, {
        status: action === "approve" ? "approved" : "rejected",
        reviewedBy: adminUid,
        reviewedAt: admin.firestore.Timestamp.now(),
      });

      // 2. If approved, grant Pro entitlement and record payment
      if (action === "approve") {
        const entitlementRef = db
          .collection("users").doc(userId)
          .collection("entitlements").doc("pro");

        t.set(entitlementRef, {
          plan: "pro",
          source: "bkash",
          sourceRef: trxId,
          grantedAt: admin.firestore.Timestamp.now(),
          revokedAt: null,
        }, { merge: true });

        // Record in payments
        const paymentRef = db.collection("payments").doc(`bkash_${trxId}`);
        t.set(paymentRef, {
          paddleTxnId: `bkash_${trxId}`,
          userId,
          amount: amountBdt,
          currency: "BDT",
          status: "completed",
          method: "bkash",
          at: admin.firestore.Timestamp.now(),
        });
      }

      // 3. Record in auditLog
      const auditRef = db.collection("auditLog").doc();
      t.set(auditRef, {
        actor: adminUid,
        action: action === "approve" ? "bkash_approve" : "bkash_reject",
        target: userId,
        metadata: { trxId, submissionId, amountBdt },
        at: admin.firestore.Timestamp.now(),
      });
    });

    return {
      success: true,
      message: action === "approve" ? `Payment ${trxId} approved. Pro unlocked.` : `Payment ${trxId} rejected.`,
    };
  }
);

export const adminCreateCode = functions.https.onCall(
  { enforceAppCheck: false },
  async (request) => {
    const adminUid = await verifyIsAdmin(request.auth);

    const type = (request.data.type ?? "free").trim().toLowerCase(); // 'free' | 'pct'
    const discountPct = parseInt(request.data.discountPct ?? (type === "free" ? "100" : "20"), 10);
    const maxUses = parseInt(request.data.maxUses ?? (type === "free" ? "10" : "100"), 10);
    const expiresAtStr = request.data.expiresAt;

    let expiresAt: admin.firestore.Timestamp;
    if (expiresAtStr) {
      expiresAt = admin.firestore.Timestamp.fromDate(new Date(expiresAtStr));
    } else {
      // Default: 1 year from now
      const d = new Date();
      d.setFullYear(d.getFullYear() + 1);
      expiresAt = admin.firestore.Timestamp.fromDate(d);
    }

    // Generate unique code
    let code = generatePromoCode();
    let codeRef = db.collection("promoCodes").doc(code);
    let attempts = 0;
    while ((await codeRef.get()).exists && attempts < 5) {
      code = generatePromoCode();
      codeRef = db.collection("promoCodes").doc(code);
      attempts++;
    }

    await codeRef.set({
      code,
      type: type === "free" ? "lifetime" : "discount",
      discountPct: type === "free" ? 100 : discountPct,
      maxUses,
      uses: 0,
      expiresAt,
      active: true,
      createdBy: adminUid,
      createdAt: admin.firestore.Timestamp.now(),
    });

    await db.collection("auditLog").add({
      actor: adminUid,
      action: "code_create",
      target: code,
      metadata: { type, discountPct, maxUses },
      at: admin.firestore.Timestamp.now(),
    });

    return {
      success: true,
      code,
      message: `Created promo code ${code}`,
    };
  }
);

export const adminToggleCode = functions.https.onCall(
  { enforceAppCheck: false },
  async (request) => {
    const adminUid = await verifyIsAdmin(request.auth);

    const code = (request.data.code ?? "").trim().toUpperCase();
    const active = request.data.active === true;

    if (!code) {
      throw new functions.https.HttpsError("invalid-argument", "code is required.");
    }

    const codeRef = db.collection("promoCodes").doc(code);
    const snap = await codeRef.get();
    if (!snap.exists) {
      throw new functions.https.HttpsError("not-found", "Code not found.");
    }

    await codeRef.update({ active });

    await db.collection("auditLog").add({
      actor: adminUid,
      action: "code_toggle",
      target: code,
      metadata: { active },
      at: admin.firestore.Timestamp.now(),
    });

    return {
      success: true,
      message: `Code ${code} is now ${active ? "active" : "inactive"}.`,
    };
  }
);
