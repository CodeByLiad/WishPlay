# WishPlay 1.2.2 release verification

Verified November 11, 2026, on the Android 16 emulator (lockout_qa).

| Property | Verified value |
| --- | --- |
| Package | `com.jinatra.wishplay` |
| Version | 1.2.2 (version code 6) |
| Minimum Android | 8.0 / API 26 |
| Signed APK | `WishPlay-1.2.2-Jinatra.apk` |
| APK size | 18,703,938 bytes |
| APK SHA-256 | `95d4cb1b3ed043b72d8a055c8243936c093ec43120fd1f29e4f0202ed2452bed` |
| Certificate SHA-256 | `bfacf78547d7040ba04fc6dc7483863b4a22891f6e6c4547a0e8412685f450a8` |
| Signature schemes | v2 and v3 |
| Unit tests | 41 passed |
| Targeted Android checks | 15 passed across the final individual/suite runs described below |
| Build checks | Debug/test compilation, unit tests, release lint, signed release assembly passed |

## Live search and actual UI evidence

`LiveStoreSearchTest` passes four checks: real Android searches for “one piece” and Genshin, image downloads with successful image responses, reopening real Google Play details, Android browse, PC Elden Ring, iOS One Piece, PC browse, and two parsing/error regression cases. No fixture supplies the live game results or artwork.

`SearchAndPermissionUiTest` passes three flows: Android “one piece” results/artwork, opening ONE PIECE Bounty Rush and saving it to Room through the visible Add action; fresh onboarding with PC/PS5 selection and real platform icons in light/dark themes; and a returning user choosing Not now, recreating the activity, and continuing without another prompt. The save test preserves pre-existing entries and removes only its own new wishlist entry.

The permission test starts with OS notification permission revoked and user flags cleared, presses the app's Turn on notifications button, observes the actual Android permission-controller dialog, clicks Allow, and asserts Android permission plus stored enabled/asked state. It does not substitute a shell grant for the user interaction. `NotificationDeliveryTest` then passes reminder posting. `AppUiRegressionTest#mainTabsAndCustomDatesStayInsideSystemBars` passes, including live Genshin artwork and system-bar/keyboard layout checks. Six `MultiSourceCatalogTest` regressions pass for source failures, platform exclusions, and linked-store evidence.

Initial runs were interrupted by emulator startup/System UI ANR overlays. One test initially selected both duplicated top/bottom Add buttons; its selector was corrected to use the visible first action. Those failed attempts are not counted as successful verification. Final tests run after clearing the System UI overlay. Onboarding testing also exposed a real navigation race: changing the graph start destination while finishing onboarding could interrupt permission setup. The initial graph now stays stable.

The screenshot batch and live network captures are local ignored artifacts under `app/build/reports/`. Live checks use the debug variant with shared production networking/parser/UI code. The signed release's certificate, package/version, and hash are verified separately.

## Behavior and limits

- RAWG timed out during live testing. Android has independent Google Play results; PC and iOS use Steam/App Store. Console searches still depend on RAWG and can report a provider error. No claim of complete catalog coverage is made.
- Provider calls have deadlines, cancellation cancels HTTP calls, and the main search has a 12-second deadline with a retryable error. Saving a loaded result no longer waits for remote details.
- Google Play uses public listing data, whose format may change. Only game genres are accepted, package IDs persist in game IDs, and unavailable release dates remain unknown. Store regions are US. Availability can vary by region.
- Notification prompting is contextual on Android 13+; earlier Android versions have no notification runtime dialog. Android 8–12 behavior was inspected in code, not re-tested on a separate emulator in this patch. Physical-phone battery restrictions and background delivery timing remain device-dependent.
- The 1.2.1 TLS fix is retained: normal system certificate/hostname verification and cleartext blocking. Its prior three live authentication transport checks do not prove a completed real Google account sign-in or successful cloud sync. Those still require a real-account retry.
- All features remain free; bKash donation number **01978900129** only. No admin options. No server deployment or account modification in this patch.

## Distribution

Install over the existing Jinatra app to keep local data; package, signing key, and database version are unchanged. APK, checksum, and manifest are release assets; APK binaries remain excluded from Git history. The updater uses the canonical CodeByLiad/WishPlay main-branch manifest. Older builds using the separate wishplay-release repository may need manual installation.

[Release](https://github.com/CodeByLiad/WishPlay/releases/tag/v1.2.2) · [Signed APK](https://github.com/CodeByLiad/WishPlay/releases/download/v1.2.2/WishPlay-1.2.2-Jinatra.apk)
