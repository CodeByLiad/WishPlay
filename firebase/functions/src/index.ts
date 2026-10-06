import * as admin from "firebase-admin";
import { redeemCode } from "./redeemCode";
import { submitBkash, bkashConfig } from "./bkash";
import { paddleWebhook, getPaddleCheckoutUrl, paddleCheckout } from "./paddle";

import { adminGetDashboard, adminReviewBkash, adminCreateCode, adminToggleCode } from "./admin";

// Initialize Firebase Admin SDK (singleton)
admin.initializeApp();

export {
  redeemCode,
  submitBkash,
  bkashConfig,
  paddleWebhook,
  getPaddleCheckoutUrl,
  paddleCheckout,
  adminGetDashboard,
  adminReviewBkash,
  adminCreateCode,
  adminToggleCode
};
