// Firebase Cloud Function: redeemCode
// Validates a promo code, enforces uniqueness per user, grants Pro entitlement.
// Callable function — requires Firebase Auth.

import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";

const db = admin.firestore();

export const redeemCode = functions.https.onCall(
  { enforceAppCheck: false },
  async (request) => {
    if (!request.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Must be signed in.");
    }

    const uid = request.auth.uid;
    const code: string = (request.data.code ?? "").trim().toUpperCase();

    if (!code) {
      throw new functions.https.HttpsError("invalid-argument", "code is required.");
    }

    // 1. Fetch promo code
    const codeRef = db.collection("promoCodes").doc(code);
    const codeSnap = await codeRef.get();

    if (!codeSnap.exists) {
      return { success: false, error: "invalid_code", message: "Invalid or expired code." };
    }

    const promo = codeSnap.data()!;

    if (!promo.active) {
      return { success: false, error: "code_inactive", message: "This code is no longer active." };
    }
    if (promo.expiresAt && promo.expiresAt.toDate() < new Date()) {
      return { success: false, error: "code_expired", message: "This code has expired." };
    }
    if (promo.uses >= promo.maxUses) {
      return { success: false, error: "code_exhausted", message: "This code has reached its maximum uses." };
    }

    // 2. Check uniqueness per user
    const redemptionId = `${code}_${uid}`;
    const redemptionRef = db.collection("codeRedemptions").doc(redemptionId);
    const redemptionSnap = await redemptionRef.get();
    if (redemptionSnap.exists) {
      return { success: false, error: "already_redeemed", message: "You have already used this code." };
    }

    // 3. Run atomically
    await db.runTransaction(async (t) => {
      t.set(redemptionRef, { code, userId: uid, redeemedAt: admin.firestore.Timestamp.now() });
      t.update(codeRef, { uses: admin.firestore.FieldValue.increment(1) });

      if (promo.type === "lifetime") {
        const entitlementRef = db
          .collection("users").doc(uid)
          .collection("entitlements").doc("pro");
        t.set(entitlementRef, {
          plan: "pro",
          source: "code",
          sourceRef: code,
          grantedAt: admin.firestore.Timestamp.now(),
          revokedAt: null,
        }, { merge: true });
      }

      // Audit log
      t.set(db.collection("auditLog").doc(), {
        action: "redeem_code",
        target: uid,
        metadata: { code, type: promo.type, discountPct: promo.discountPct },
        at: admin.firestore.Timestamp.now(),
      });
    });

    const proGranted = promo.type === "lifetime";
    return {
      success: true,
      type: promo.type,
      discount_pct: promo.discountPct,
      pro_granted: proGranted,
      message: proGranted
        ? "Code accepted! WishPlay Pro unlocked for life."
        : `${promo.discountPct}% discount code applied.`,
    };
  }
);
