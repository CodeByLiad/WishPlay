# WishPlay Release Verification & Security Hardening (Phase 7)

**Product:** WishPlay (`com.jinatra.wishplay`)  
**Publisher:** Jinatra  
**Build Type:** Release (R8 full mode, resource shrinking, APK Signature Scheme v2 & v3)  
**Artifact:** `app/build/outputs/apk/release/app-release.apk`  
**Size:** 18,511,970 bytes (~17.7 MB — below PRD 20 MB ceiling)  
**Version:** 1.1.0 (versionCode 3)  
**SHA-256 Checksum:** `1ffb4a10d5390fb848f32e7ee23db7c49d4a28b1fae357c07a6de2e27d78d954`  
**Signing Certificate SHA-256:** `BF:AC:F7:85:47:D7:04:0B:A0:4F:C6:DC:74:83:86:3B:4A:22:89:1F:6E:6C:45:47:A0:E8:41:26:85:F4:50:A8`

---

## 1. Threat Mitigation Audit (PRD Threats & Defenses Table)

| Threat | PRD Defense | Implemented Defense | Status |
| :--- | :--- | :--- | :---: |
| **Patch APK to flip `isPro = true`** | Pro comes from server-signed token; server features check server account; re-signed APKs fail key attestation | `EntitlementRepository` checks `AppSecurityManager.isEnvironmentCompromised()`. Quiet failure locks Pro features without alerting modders. Server-side Edge Functions gate price and sync data. | **Verified** |
| **Fake "payment successful"** | Pro only granted by verified webhook or server function | Pro entitlement is only mutated via Cloud Functions and verified server records (`payments` & `auditLog`). Client never sets Pro status directly. | **Verified** |
| **Brute-force promo codes** | 8+ random chars, rate limits, max uses, one use per account | `WP-XXXX-XXXX` format, single-transaction redemption in `redeemCode` Cloud Function, server rate limits. | **Verified** |
| **Reuse or fake bKash TrxID** | Unique TrxID in DB; admin review | Firestore unique TrxID constraint; admin queue in `AdminRepository` / `adminReviewBkash` Cloud Function. | **Verified** |
| **Become admin by patching app** | Admin role in server-only table | Role verification in `admin.ts` strictly checks `hyathis.x@gmail.com` and `/admins/{uid}` on the server. | **Verified** |
| **Steal API keys from APK** | No secret ships in APK; keys live in backend secrets | **Not met.** `WishPlayApiService.kt` hardcodes a RAWG API key and calls `https://api.rawg.io/api` directly; `StoreCatalog.kt` calls `store.steampowered.com` and `itunes.apple.com` directly. No request currently routes through a backend proxy. The key is extractable from the APK and the free-tier quota is abusable. Moving catalog calls behind a proxy is outstanding work. | **Open** |
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

# Note: keytool cannot read this APK. Release builds use APK Signature Scheme v2/v3
# only (v1 JAR signing is disabled), so keytool reports "Not a signed jar file".
# Use apksigner above.
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
