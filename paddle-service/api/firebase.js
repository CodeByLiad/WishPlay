const admin = require("firebase-admin");
const path = require("path");
const fs = require("fs");

let initialized = false;

function getDb() {
  if (!initialized) {
    let credential;

    if (process.env.FIREBASE_SERVICE_ACCOUNT) {
      try {
        const raw = process.env.FIREBASE_SERVICE_ACCOUNT;
        const parsed = raw.startsWith("{") ? JSON.parse(raw) : JSON.parse(Buffer.from(raw, "base64").toString("utf8"));
        credential = admin.credential.cert(parsed);
      } catch (e) {
        console.error("Failed to parse FIREBASE_SERVICE_ACCOUNT env var:", e);
      }
    }

    if (!credential) {
      const localPaths = [
        path.join(__dirname, "..", "serviceAccountKey.json"),
        path.join(__dirname, "..", "..", "firebase", "serviceAccountKey.json"),
      ];
      for (const p of localPaths) {
        if (fs.existsSync(p)) {
          credential = admin.credential.cert(require(p));
          break;
        }
      }
    }

    if (!credential) {
      // Fallback to default application credentials if running in GCP
      credential = admin.credential.applicationDefault();
    }

    admin.initializeApp({
      credential,
      projectId: "wishplayapk",
    });
    initialized = true;
  }

  return admin.firestore();
}

module.exports = { getDb, admin };
