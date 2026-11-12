# WishPlay checkpoint — November 12, 2026

Version 1.4.0 is published and the code, README, screenshots, checksum and update manifest are pushed to main. The user resumed with “Continue” on November 12; the requested discovery correction and signed-release publication are complete.

## Verified implementation

- Independent Nintendo, PlayStation, Microsoft and Epic catalogs supplement Google Play, Steam, App Store and RAWG. Full Steam/Microsoft search and actual store pagination continue beyond short suggestion lists. Switch 2 has a separate filter.
- Native store metadata establishes platform membership. New Nintendo image fields are supported. Missing dates say Date not listed, and store names remain visible with platform filters.
- Verified duplicate listing identities merge metadata and evidence. Shared names alone do not hide different games or store identities. Compiled title patterns and normalized-name caching reduce repeated merge work.
- Retry refreshes the failed page and retains working results. Partial provider failures remain isolated.
- Every feature remains free; optional bKash number 01978900129, with no admin or paid-upgrade screens.

## Validation and artifact

- 46 unit tests passed. Successful focused runs cover 23 Android checks, including 30 unrelated live game queries, reopened details, pagination, source/date labels, console navigation and rapid native input.
- Emulator startup and keyboard runs initially encountered Android error dialogs/timeouts. Traces and captures identified framework startup delays and a System UI dialog stealing window focus. Those attempts are not counted as passes; the unchanged keyboard check passed after the dialog was cleared. Final emulator configuration: Android 16, 4 CPU cores, 2 GB memory. Optional Google Assistant/System Intelligence were disabled only on this QA emulator.
- Release lint: 0 errors, 108 warnings. Final signed APK passed native guest setup, Google Play/PlayStation/Epic discovery and details, appearance selection, and a 2,059 ms cold launch.
- APK: distribution/WishPlay-1.4.0-Jinatra.apk, version code 8, package com.jinatra.wishplay, 18,966,394 bytes.
- SHA-256: ae23df68000cc816fdeac0d6957ad754d3bf59188090d22bd6f70f4d0d18cec0.
- Original signing certificate SHA-256: bfacf78547d7040ba04fc6dc7483863b4a22891f6e6c4547a0e8412685f450a8.
- Source revisions: ba2d8b0 (discovery and verified listing fixes), 47a01eb (test diagnostics; final APK source revision).
- Final expected-title timings: 44–2,089 ms on this emulator/connection; Alan Wake 2 matched in 451 ms. No universal instant-network guarantee is made.

## Observed publication outcome

- [Published release](https://github.com/CodeByLiad/WishPlay/releases/tag/v1.4.0) is marked latest and is not a draft.
- [Signed APK](https://github.com/CodeByLiad/WishPlay/releases/download/v1.4.0/WishPlay-1.4.0-Jinatra.apk) was downloaded again after publication; its SHA-256 matches the verified local artifact above.
- GitHub's server-side digests match all three uploaded assets: APK, checksum file and update manifest.
- Source/documentation release commit: 5a51f88e39b4e8342e23fff977ce60eb6362a0f1, also the v1.4.0 tag target. Main was advanced only after the public APK download was verified, keeping the update feed from advertising an unavailable asset.
- No GitHub Actions workflows or active runs were present. Local build/test evidence and exact verification limits are recorded in the linked report.

See [discovery scope](Catalog_Discovery.md), [verification](Release_Verification.md), and [design/Mobbin references](Design_System.md). Real-account sync and physical-phone background delivery remain separate verification scopes. Public catalogs can omit regional, removed or unindexed games.

Unrelated untracked .claude/ was left untouched. Do not stage or delete it. The older unpublished candidate APK hashes are obsolete; publish only the final ae23df… artifact above.
