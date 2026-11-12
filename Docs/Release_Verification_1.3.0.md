# WishPlay 1.3.0 release verification

Verified November 11, 2026, on Android 16. The final focused checks use the dedicated `wishplay_qa` emulator.

| Property | Verified value |
| --- | --- |
| Package | `com.jinatra.wishplay` |
| Version | 1.3.0 (version code 7) |
| Minimum Android | 8.0 / API 26 |
| Signed APK | `WishPlay-1.3.0-Jinatra.apk` |
| APK size | 18,950,010 bytes |
| APK SHA-256 | `aada1a58204845e586af06922fc4994012b4d4ba419909bf585a49b53f76bd29` |
| Certificate SHA-256 | `bfacf78547d7040ba04fc6dc7483863b4a22891f6e6c4547a0e8412685f450a8` |
| Signature schemes | v2 and v3 |
| Unit tests | 41 passed |
| Distinct Android checks | 37 have passing results across focused runs below |
| Build checks | Debug/test compilation, unit tests, release lint, and signed release assembly passed |

## What was actually checked

| Test group | Passing checks | Scope |
| --- | ---: | --- |
| ExpressiveDesignRegressionTest | 4 | Eight palette combinations, text/thumb contrast, real on-screen colors and persistence, native library/settings/details captures, Steam HTML parsing, live World Seeker minimum/recommended/storage |
| FeaturedPlaySearchTest | 4 | Featured game records, rejecting non-games, exact match after 45 recommendations, live Offroad spelling variants/artwork/details, actual UI search/open/save |
| SearchResponsivenessTest | 3 | A blocked RAWG call cannot delay a working store, cancellation cancels HTTP, repeat cache makes no new requests, measured live first/cached result times |
| SearchKeyboardRegressionTest | 1 | Rapid individual native keystrokes preserve the exact query; clear and recent selection restore it correctly |
| LiveStoreSearchTest | 4 | Real One Piece/Genshin searches, artwork downloads/details, Android browse, PC Elden Ring, iOS One Piece, PC browse, parsing/error guards |
| MultiSourceCatalogTest | 6 | Provider failures, linked-store date evidence, platform exclusions, supplementary query behavior |
| CatalogRegressionTest | 2 | Catalog mapping and pagination regressions |
| BackupAndMigrationTest | 2 | Atomic/idempotent imports, existing-entry preservation, encrypted wishlist migration |
| UxComponentRegressionTest | 2 | Date picker cancellation/validation, exact bKash number display and clipboard copy |
| UxFlowRegressionTest | 1 | Calendar, custom game, and recoverable removal flow |
| AppUiRegressionTest | 2 | Main tabs/system bars, live Genshin artwork, per-game platform choices and pending alerts |
| AuthenticationTransportTest | 3 | Live Google/Firebase TLS, invalid-token server response, cleartext blocking |
| NotificationDeliveryTest | 1 | Four notification types, channels, and content actions |
| SearchAndPermissionUiTest | 2 | Actual Android permission dialog/Allow after onboarding, and live One Piece details/save |

The Android tests use the debug variant and shared production UI/networking code. Signed release identity, metadata, packaged behavior, and download integrity are checked separately. The optional permission prompt is never replaced by a shell grant in its interactive test. Temporary wishlist entries are cleaned up; pre-existing entries are preserved.

## Missing game and latency diagnosis

Google Play's live responses contained Offroad League Online for `off-road league online`, `offroad league online`, and `off road league online`. It was in a featured full-app record that the earlier compact-row parser skipped. The parser now reads both formats, accepts a featured item only with a valid package/image and `GAME_` category, and ranks normalized exact titles first. The regression includes a featured match after more than 40 earlier recommendations, so truncation cannot hide it.

Search previously waited for RAWG before returning stores that had already answered. Production search now streams provider updates. A deterministic test holds RAWG blocked while the store appears, then verifies cancellation of the outstanding call. The live Offroad test measured **1,382 ms to its first results** and **1 ms for its cached repeat**. A separate cache test measured 6 ms and zero extra HTTP requests. These are model-result timings on this emulator/connection, not a promise about every device, image download, or network.

The in-memory cache holds 32 query/page snapshots. Complete snapshots are fresh for 60 seconds. Older snapshots up to 24 hours old appear immediately while refreshing, with an explicit saved-results notice; unavailable providers leave usable saved results. The cache does not survive process termination. Provider budgets and a 12-second overall deadline remain. Superseded queries cancel HTTP calls and generation checks prevent late results overwriting newer searches. The typing debounce is 180 ms. Text editing uses native state-based Material input rather than feeding IME edits through asynchronously combined results state.

## UI and requirements evidence

The screen captures in [the design system](Design_System.md) and [README](../README.md) come from the running app. Gold, Violet, Teal, and Coral were selected in both modes and checked against actual screen pixels, not just stored preference values. Palette contrast checks cover body text, primary/container text, and the switch thumb. The final visual batch confirms separated settings surfaces, readable dark system bars, wrapped PC requirement values, distinct switch thumbs/checkmarks, and artwork-led library cards.

World Seeker (`steam_755500`) supplies minimum/recommended PC specifications and 25 GB storage in Steam's live response. Steam's `OS *:` footnote marker is handled. Unknown requirements/storage/trailers say unavailable rather than promising future data. Generic invented mobile hardware specifications were removed. Store links provide the original listing. Material Expressive APIs use Google's experimental Material3 1.5.0-alpha06 with Compose BOM 2025.10.01; source/API availability and Mobbin references are recorded in the design-system document.

Some attempts failed during emulator boot, loss of unlocked-user/network state, or unrelated System UI/Digital Wellbeing ANR overlays. Those attempts are not counted as successful verification. Focused UI checks were rerun after clearing that interference on the dedicated emulator. Test-harness return-type and input-readiness issues were corrected. Native typing exposed a real field-state issue; state-based input and a separate native-keystroke regression now cover it. Reminder scheduling also uses supported on-demand initialization after an update/startup path exposed an initialization failure.

## Signed APK smoke check

The final minified, signed APK was installed fresh on the dedicated QA emulator. A normal native-input flow completed guest onboarding, chose Not now for notifications, typed the exact `off-road league online` query, found Offroad League Online, opened Rooster Games details, opened Settings, and selected Teal. This check uses the release APK rather than the debug/test build. Signing certificate, package/version, and checksum were verified separately.

## Limits and compatibility

- RAWG was unavailable during live search testing. Android, PC, and iOS have independent store results; console catalog coverage still depends on RAWG. No claim of a complete universal game catalog is made.
- Public Google Play formats and store coverage can change. Store regions are US. Missing release dates remain unknown; release state is not invented from an update date.
- Completed Google account selection/cloud sync with a real account is not proved by the transport-only tests. Physical-phone background scheduling and battery restrictions remain device-dependent.
- Android 8–12 behavior was inspected in code; this release was not tested on a separate emulator for every supported Android version.
- Every feature remains free. Donations only display/copy bKash **01978900129**; admin/purchase flows remain absent. No server/account records were deployed or modified.
- Same package, release key, and database version. Install over the existing Jinatra app to retain data. APK binaries are release assets, excluded from Git source history. The updater uses the canonical CodeByLiad/WishPlay manifest; older builds using a separate release repository may need manual installation.

[Release](https://github.com/CodeByLiad/WishPlay/releases/tag/v1.3.0) · [Signed APK](https://github.com/CodeByLiad/WishPlay/releases/download/v1.3.0/WishPlay-1.3.0-Jinatra.apk)
