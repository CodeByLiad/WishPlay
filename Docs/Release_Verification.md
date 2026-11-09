# WishPlay 1.2.0 release verification

Verified November 9, 2026.

| Property | Verified value |
| --- | --- |
| Android package | `com.jinatra.wishplay` |
| Version | 1.2.0 (version code 4) |
| Minimum Android | 8.0 / API 26 |
| APK | `WishPlay-1.2.0-Jinatra.apk` |
| Size | 18,666,186 bytes |
| APK SHA-256 | `8ff8d196447e8fce36d24c6274090c0f2a59e2eeed99fcf151c2de40d968c786` |
| Signing certificate SHA-256 | `BF:AC:F7:85:47:D7:04:0B:A0:4F:C6:DC:74:83:86:3B:4A:22:89:1F:6E:6C:45:47:A0:E8:41:26:85:F4:50:A8` |
| Signature schemes | v2 and v3 verified with Android SDK apksigner |
| Previous release compatibility | Certificate matches the local 1.1.0 APK and the official Jinatra certificate |
| Build | `testDebugUnitTest lintRelease assembleRelease` succeeded |
| Unit tests | 41 passed |
| Device regression tests | 15 selected tests passed before version/release preparation |
| Release lint | No errors; 80 warnings |

## Current product policy

All app features are free. Wishlist/import purchase limits, Pro checkout, promo-code redemption, admin screens, and privileged-account controls are removed. Donations are optional and only display/copy bKash number **01978900129**.

The release keeps the existing package and Jinatra signing key, allowing in-place updates of the Jinatra 1.0.1/1.1.0 line. Builds using the older application ID install separately.

## Updates and downloads

- [GitHub release](https://github.com/CodeByLiad/WishPlay/releases/tag/v1.2.0)
- [Signed APK](https://github.com/CodeByLiad/WishPlay/releases/download/v1.2.0/WishPlay-1.2.0-Jinatra.apk)
- The APK, SHA-256 file, and update manifest are release assets; APK binaries are excluded from source control.
- Version 1.2.0 checks `https://raw.githubusercontent.com/CodeByLiad/WishPlay/main/distribution/update_manifest.json` for future releases.
- Already-installed older versions retain their old updater endpoint. This release does not change the separate `wishplay-release` repository; those installations can update manually using the APK above.
- Downloads are checked against the manifest SHA-256 and the installed app's signing certificate before installation.

## Verification limits and outstanding work

- Live RAWG verification returned HTTP 522. Catalog completeness and remote artwork availability cannot be guaranteed. Store fallback behavior passed fixture-based tests.
- Successful Google sign-in/cloud sync with a real account and long-running reminder delivery under physical-device battery restrictions remain unverified.
- Android posting of release, countdown, date-change, and price-drop notifications passed on the emulator with notification permission granted. This does not prove exact background delivery timing on every device.
- The RAWG client API key is still bundled in the existing catalog implementation. Moving catalog access behind a server proxy remains outstanding; this release makes no claim that all provider keys are server-only.
- Existing server deployments and historical account/payment records were not changed by the free-app/admin-UI removal.

## Local verification commands

```powershell
.\gradlew.bat --offline testDebugUnitTest lintRelease assembleRelease
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
Get-FileHash distribution/WishPlay-1.2.0-Jinatra.apk -Algorithm SHA256
```
