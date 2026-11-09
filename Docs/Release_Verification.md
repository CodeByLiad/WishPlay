# WishPlay 1.2.1 release verification

Verified November 9, 2026.

| Property | Verified value |
| --- | --- |
| Android package | `com.jinatra.wishplay` |
| Version | 1.2.1 (version code 5) |
| Minimum Android | 8.0 / API 26 |
| Signed APK | `WishPlay-1.2.1-Jinatra.apk` |
| APK size | 18,665,674 bytes |
| APK SHA-256 | `7a2c8872cf563006ccd514d0e030c7087e2c2b6b9d9a7b301b46571f994c2f9e` |
| Signing certificate SHA-256 | `BF:AC:F7:85:47:D7:04:0B:A0:4F:C6:DC:74:83:86:3B:4A:22:89:1F:6E:6C:45:47:A0:E8:41:26:85:F4:50:A8` |
| Signature schemes | Android APK signature v2 and v3 |
| Unit tests | 41 passed |
| Live Android authentication transport tests | 3 passed |

## Reproduced failure and fix

The installed 1.2.0 app failed TLS connections to `identitytoolkit.googleapis.com`, `securetoken.googleapis.com`, and `firestore.googleapis.com` with `SSLHandshakeException: Pin verification failed`. Its global Google API certificate pins did not match the providers' valid certificate chains.

Version 1.2.1 removes stale pins in Android network configuration and the shared OkHttp client. Android's system certificate trust, normal hostname verification, and cleartext blocking remain enabled. User-added certificate authorities are no longer trusted by the base configuration. No permissive trust manager, custom hostname bypass, or plaintext fallback was introduced. See [Android's security guidance](https://developer.android.com/privacy-and-security/security-ssl).

The same Android HTTPS test that failed against 1.2.0 passes after the fix. A separate Firebase SDK request using a deliberately invalid custom token reaches the server and returns `ERROR_INVALID_CUSTOM_TOKEN`, instead of a certificate failure. It uses a separate temporary FirebaseApp and does not create or sign in a real account. Cleartext-blocking checks also pass.

These live checks were run on the debug variant using the shared main network configuration. The signed release's signature, package/version metadata, and packaged network security configuration are checked separately. Google account selection and successful cloud sync with a real account still require the user's retry after installation; they are not claimed as completed by the invalid-token test.

## Release and compatibility

- [GitHub release](https://github.com/CodeByLiad/WishPlay/releases/tag/v1.2.1)
- [Signed APK](https://github.com/CodeByLiad/WishPlay/releases/download/v1.2.1/WishPlay-1.2.1-Jinatra.apk)
- Install over the existing Jinatra app to retain local data. The package, signing key, and database version are unchanged.
- All features remain free; optional donations only display/copy bKash number **01978900129**.
- The updater reads `https://raw.githubusercontent.com/CodeByLiad/WishPlay/main/distribution/update_manifest.json`. Version 1.2.0 already uses this endpoint. Older versions using the separate `wishplay-release` repository may need a manual update.
- APK, checksum, and update manifest are GitHub release assets; APKs are excluded from Git source history.

## Other verification limits

The 15 app/backup/catalog/notification regressions passed during the previous release work; this hotfix specifically adds the live authentication checks above. RAWG's earlier HTTP 522 outage, completeness of game catalogs, and physical-device background reminder timing remain separate limitations. The existing RAWG client key remains bundled; moving provider calls behind a server proxy is outstanding. No server deployments or account records were modified by this hotfix.
