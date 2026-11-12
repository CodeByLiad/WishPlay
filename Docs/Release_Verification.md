# WishPlay 1.4.0 release verification

Verified November 12, 2026, on the dedicated `wishplay_qa` Android 16 emulator. This report describes the checked scope; it does not claim every app feature or every game was tested.

| Property | Verified value |
| --- | --- |
| Package | `com.jinatra.wishplay` |
| Version | 1.4.0 (version code 8) |
| Minimum Android | 8.0 / API 26 |
| Signed APK | `WishPlay-1.4.0-Jinatra.apk` |
| APK size | 18,966,394 bytes |
| APK SHA-256 | `ae23df68000cc816fdeac0d6957ad754d3bf59188090d22bd6f70f4d0d18cec0` |
| Original certificate SHA-256 | `bfacf78547d7040ba04fc6dc7483863b4a22891f6e6c4547a0e8412685f450a8` |
| Signature schemes | v2 and v3 |
| Unit tests | 46 passed, including verified listing identity and metadata merging |
| Final focused Android checks | 23 passed: 20 catalog checks and 3 actual-screen/native-input checks |
| Build | Debug/test compilation, unit tests and signed optimized release assembly passed; release lint reported 0 errors and 108 warnings |

## Final focused checks

| Test group | Checks | Scope |
| --- | ---: | --- |
| BroadDiscoveryLiveTest | 2 | 30 unrelated real title queries across Android, PC, PS4, PS5, Xbox, Switch and Switch 2; 16 reopened detail lookups using a new API instance and saved title |
| ConsoleCatalogRegressionTest | 6 | Explicit platform membership, game/add-on filtering, unknown dates, new Nintendo artwork fields, Microsoft response formats and continuation origin checks, independent Switch 2 pagination |
| CatalogPaginationTest | 2 | Steam HTML/title/date parsing and live additional Steam, Nintendo, PlayStation and Microsoft pages |
| MultiSourceCatalogTest | 6 | Primary/secondary outages, linked-store evidence, excluded-platform results and supplementary request behavior |
| SearchResponsivenessTest | 4 | Working results before blocked RAWG, HTTP cancellation, cache reuse, explicit retry bypass, live Offroad first/cached results |
| ConsoleDiscoveryUiTest | 1 | Actual app search and detail navigation for Astro Bot on PS5 and Mario Kart World on Switch 2 |
| SearchKeyboardRegressionTest | 1 | Rapid native key events preserve the complete query; clear and recent selection work |
| SearchListingUiTest | 1 | Selected-platform rows retain store names and missing dates say Date not listed |

Final counts refer to successful reruns. Earlier emulator attempts encountered Android startup timeouts before test execution; one trace was in framework instrumentation resource loading, and another main-thread trace was idle. The dedicated emulator was restarted with four cores, the build process was stopped before testing, and the checks were rerun. These failed attempts are not counted as passes. Signed-APK startup is checked separately below.

The combined 23-check run had 22 passes and a keyboard-visibility timeout. Its diagnostic capture showed a System UI error dialog and `windowFocus=false`. The unchanged native-keyboard check passed separately after that dialog was cleared. The final counts therefore describe successful checks across focused runs, not a single uninterrupted all-green run.

Eight additional existing live-store, catalog mapping, and backup/migration checks passed during this development cycle before the final Microsoft full-search addition; they are not included in the 23 final focused checks above.

## Discovery and response times

The final live matrix asserts expected provider IDs, titles, platform membership and source links. Minecraft PC specifically requires **Minecraft: Java & Bedrock Edition for PC**; Minecraft Legends is insufficient. Production code contains no per-title patch or fictional fallback record.

The expected game appeared in **44–2,089 ms** across the 16 timed PC/console queries. First-result and expected-title timings are measured separately. A complete provider aggregation can take longer while a slow source reaches its deadline. Artwork, phone performance and network conditions vary. Cached-repeat and cancellation checks remain separate deterministic tests.

Second pages returned **50 additional Steam games, 40 Nintendo games, 11 PlayStation games and 13 Microsoft games** in the final live pagination run. Counts can change as listings/ranking change. These checks use the store adapters directly and do not depend on RAWG.

See [the complete provider matrix, mechanisms and limits](Catalog_Discovery.md).

## Exact signed-APK smoke check

Installed the final original-key signed APK on the dedicated emulator, then used native controls and typing to check guest setup, optional notification setup, the exact Offroad query and Google Play details, appearance settings/Teal accent, Astro Bot PS5 search/details with its real date, and Alan Wake 2 PC search/Epic details with its real date. This exercises new discovery adapters in the optimized artifact, not just a debug build.

The final fresh installation launched cold in 2,059 ms. Search captures confirm that store names remain visible with PC selected and that missing dates no longer imply a future announcement. The APK was built from source revision `47a01eb`, retaining the original certificate.

## Remaining limits

- No public source guarantees every regional, removed, unindexed or unannounced game. Google Play and App Store search responses remain limited. Missing platform information and requirements are not invented.
- Nintendo dates are labelled Europe; the other independent stores use US listings. Public response formats and PlayStation's published query hash may change; provider failures are isolated.
- Google account completion/cloud sync and physical-phone background notification delivery are separate verification scopes. This discovery release does not claim those were fully exercised by its catalog tests.
- Material Expressive continues to use the existing experimental Material3 APIs. No unrelated framework upgrade was included.

[Release](https://github.com/CodeByLiad/WishPlay/releases/tag/v1.4.0) · [Signed APK](https://github.com/CodeByLiad/WishPlay/releases/download/v1.4.0/WishPlay-1.4.0-Jinatra.apk) · [Previous 1.3.0 verification](Release_Verification_1.3.0.md)
