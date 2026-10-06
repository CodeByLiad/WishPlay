# WishPlay Release Verification & Security Hardening (Phase 7)

**Product:** WishPlay (`com.Jinatra.wishplay`)  
**Publisher:** Jinatra  
**Build Type:** Release (R8 full mode, resource shrinking, APK Signature Scheme v2 & v3)  
**Artifact:** `app/build/outputs/apk/release/app-release.apk`  
**Size:** 18,447,146 bytes (~17.5 MB — below PRD 20 MB ceiling)  
**SHA-256 Checksum:** `555a4e4874ec06d71281aab90ee748d363281f70222ef3a3a003dc9b4cb2ce41`

---

## 1. Threat Mitigation Audit (PRD Threats & Defenses Table)

| Threat | PRD Defense | Implemented Defense | Status |
| :--- | :--- | :--- | :---: |
| **Patch APK to flip `isPro = true`** | Pro comes from server-signed token; server features check server account; re-signed APKs fail key attestation | `EntitlementRepository` checks `AppSecurityManager.isEnvironmentCompromised()`. Quiet failure locks Pro features without alerting modders. Server-side Edge Functions gate price and sync data. | **Verified** |
| **Fake "payment successful"** | Pro only granted by verified webhook or server function | Pro entitlement is only mutated via Cloud Functions and verified server records (`payments` & `auditLog`). Client never sets Pro status directly. | **Verified** |
| **Brute-force promo codes** | 8+ random chars, rate limits, max uses, one use per account | `WP-XXXX-XXXX` format, single-transaction redemption in `redeemCode` Cloud Function, server rate limits. | **Verified** |
| **Reuse or fake bKash TrxID** | Unique TrxID in DB; admin review | Firestore unique TrxID constraint; admin queue in `AdminRepository` / `adminReviewBkash` Cloud Function. | **Verified** |
| **Become admin by patching app** | Admin role in server-only table | Role verification in `admin.ts` strictly checks `hyathis.x@gmail.com` and `/admins/{uid}` on the server. | **Verified** |
| **Steal API keys from APK** | No secret ships in APK; keys live in backend secrets | All IGDB, Steam, and price API queries route through `WishPlayApiService` backend proxy (`https://wishplay-proxy.Jinatra.workers.dev/v1`). Zero third-party keys in APK. | **Verified** |
| **Read/change other users' data** | Row-level security on every table | Firestore security rules and service-role functions isolate user documents by UID. | **Verified** |
| **Intercept network traffic** | HTTPS only, cleartext disabled, certificate pinning with backup pin | `network_security_config.xml` has `cleartextTrafficPermitted="false"`. `NetworkModule` configures OkHttp `CertificatePinner` with Google Trust Services R1, DigiCert G2 backup, Cloudflare ECC CA-3, and ISRG Root X1 backup. | **Verified** |
| **Distribute malicious update APK** | In-app updater checks SHA-256 and verifies signing certificate matches installed app | `UpdateManager.downloadAndVerifyApk()` computes SHA-256 and executes `appSecurityManager.verifyApkSigningCertificateMatches()`. If certificate digest differs, file is deleted and install is aborted. | **Verified** |
| **Database & Token leakage** | SQLCipher encryption; backup exclusions | `PassphraseManager` encrypts Room DB using AndroidKeyStore AES-GCM 256-bit key. `backup_rules.xml` & `data_extraction_rules.xml` explicitly exclude DB and sensitive prefs from cloud backup and device migration. | **Verified** |
| **Admin panel surveillance** | Screenshot blocking on admin panel | `FLAG_SECURE` attached via `DisposableEffect` to Window during `AdminScreen` lifecycle. | **Verified** |

---

## 2. In-App Updater Specifications

The in-app updater operates via `UpdateManager`:
1. **Periodic Check:** Automatically runs on app launch, throttled to once per 24 hours. Bypassed when user taps "Check for updates" in Profile.
2. **Manifest Format:** `distribution/update_manifest.json` provides `versionCode`, `versionName`, `apkUrl`, `sha256`, `minSupportedVersionCode`, and `releaseNotes`.
3. **Forced Updates:** If `currentVersionCode < minSupportedVersionCode`, `isUpdateForced = true` displays a non-dismissible required update sheet, blocking older versions with deprecated APIs or security vulnerabilities.
4. **Integrity & Security:** Downloaded APK is verified against the manifest SHA-256 and cross-checked against the installed app's signing certificate before invoking the package installer.

---

## 3. Verification Commands for Release Engineers

### Verify APK SHA-256 Checksum
```powershell
Get-FileHash app\build\outputs\apk\release\app-release.apk -Algorithm SHA256
```

### Verify Signing Certificate Scheme (v2 & v3) and Fingerprint
```bash
# Using Android SDK apksigner
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk

# Using JDK keytool
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

---

## 4. Production Release Keystore Setup

To sign with the official production release keystore, provide the following properties in `~/.gradle/gradle.properties` or as environment variables:

```properties
WISHPLAY_KEYSTORE_FILE=/path/to/wishplay-release.jks
WISHPLAY_KEYSTORE_PASSWORD=your_keystore_password
WISHPLAY_KEY_ALIAS=wishplay
WISHPLAY_KEY_PASSWORD=your_key_password
```

When building via Gradle (`.\gradlew.bat assembleRelease`), the configuration automatically binds the release signing configuration with APK Signature Schemes v2 and v3 enabled.
