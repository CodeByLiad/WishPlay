# WishPlay UX and UI improvements

Reviewed and implemented November 9, 2026. Scope: the existing native Android app, preserving its Material design, gold accent, offline wishlist, and existing services.

## Mobbin references

Reviewed the screenshots and multi-step discovery/wishlist flows returned by Mobbin. Applied their compact, artwork-led list hierarchy and clear navigation patterns using Android-native controls.

- [Meta Quest wishlist](https://mobbin.com/screens/ace9cca4-7045-4041-899e-441fb163a7bb): compact artwork and metadata hierarchy.
- [PlayStation App wishlist](https://mobbin.com/screens/35e99a4f-1477-4c35-9f8b-e08a6182cc58): scannable saved-game rows.
- [Hypelist game list](https://mobbin.com/screens/9234f50a-7888-45ad-99f2-f5539fdcc926): grouped collections and distinct list actions.

## Current free-app policy

All features are free for guests and signed-in users. There is no paid wishlist/import limit, accent or widget lock, upgrade checkout, code redemption, or admin interface. Profile contains an optional bKash donation section showing **01978900129** and a copy-number action. Donations do not grant privileges or unlock features. The Android payment/admin screens, repositories, routes, domain models, and obsolete admin/paywall tests were removed. Existing server deployments and historical account data were not modified.

## Changes

| Flow | Changes |
| --- | --- |
| Main navigation | Native Material navigation items expose selection and touch feedback. Each tab preserves its scroll state. System Back returns from another tab to Wishlist. Removed unused prototype tab implementations. |
| Wishlist | Compact next-release summary prioritizes game information. Share opens Android's share sheet with actual wishlist content. Platform chips reflect saved games. Reset filters clears both platform and release filters. The add menu sits above the navigation bar and dismisses with Back. Empty lists use their existing direct actions without an additional floating menu. |
| Search | Consistent heading size; larger clear and recent-search removal targets; readable placeholder contrast; accurate empty discovery copy; wishlist buttons identify the game and the action, including removal. |
| Custom games | Calendar selection alongside manual date input, inline invalid-date feedback, persistent Save action, accessible error placement, single-line title, draft-discard confirmation, and navigation directly to the saved game's details. Reminder previews distinguish unknown release dates. |
| Details | Confirmation before removing a game; disabled controls during wishlist/alert updates; recoverable update errors; explicit removal action and alert labels. Games without cover art use a shorter banner. |
| Alerts | Empty schedule links directly to game discovery. Header spacing and calendar touch targets are consistent with other tabs. |
| Calendar | Starts at the current month rather than October 2026. Month/day selections survive recreation. Added This month. Day cells scale with available width, expose full dates and release counts, and allow selection of empty days. |
| Onboarding | Continue with Google invokes the existing real credential flow, waits for its result, and offers recovery after failure. Guest continuation remains available. Back follows setup steps. Setup content scrolls on short screens. |
| Account and donations | All themes and the home widget are available without purchase. Optional bKash donation number with clipboard feedback. Upgrade, checkout, promo-code, and admin flows removed. Profile sharing now uses actual wishlist titles rather than a fabricated public link. |
| Free price alerts | Global price-alert controls are available to everyone. Per-game price-alert switches save their state; background checks honor that per-game setting. |
| Shared controls | Buttons and chips grow with text instead of clipping to a fixed height. Chips and grouped rows provide native press feedback. |

## Verification

- Debug APK and Android test APK build successfully.
- 41 unit tests pass. Five obsolete admin/paywall tests were removed alongside those features.
- Android lint completes successfully with 101 warnings and no errors. This work does not claim to eliminate the repository's entire lint-warning backlog.
- All 15 selected emulator tests pass: backup/import/migration, catalog fixture handling and fallback, complete calendar/custom-game/removal flow, date-picker behavior, copying the donation number, guest accent selection, per-game platform persistence, and all four Android notification types.
- Existing per-game platform preference persistence test passes.
- Final emulator screenshots reviewed for the empty/populated wishlist, search, profile, alerts, calendar, custom form, and saved custom-game details. Test game names and dates in those captures are fixtures.
- Existing `mainTabsAndCustomDatesStayInsideSystemBars` test times out at its remote-artwork loading check (40 seconds). It does not reach the later assertions in that test. The new local flow test independently exercises all four tabs; remote catalog/artwork verification remains unresolved.
- Successful Google authentication and cloud sync were not completed with a real account. Donation transfers are performed outside WishPlay; the app only displays/copies the requested number.
- The read-only live catalog check returned HTTP 522 from RAWG. Complete catalog availability and discovery of every game cannot be guaranteed. Steam/App Store fallback behavior is covered by fixture-based tests.
- Backup/import tests now cover merging more than 25 games and adding ordinary/custom games beyond the former cap. File-size and format validation remain in place for safe import handling.
- Release, countdown, date-change, and price-drop notifications were confirmed present in Android notification management with their correct channels and open actions, then cleaned up. Notification permission was granted on the emulator for this test. Long-running delivery under battery management still requires a physical-device soak test.
- Initial Gradle-managed instrumentation startup failed before running tests. Direct Android instrumentation subsequently ran the new tests successfully.

Artifacts are generated under `app/build/outputs/apk/debug/` and `app/build/reports/ux-qa/`. The new regression tests remain in `app/src/androidTest/`.

## 1.2.1 authentication connection fix

The user reported Firebase sign-in failing with `Pin verification failed`. A live Android test against the installed 1.2.0 app reproduced the same TLS failure for `identitytoolkit.googleapis.com`, `securetoken.googleapis.com`, and `firestore.googleapis.com`.

The network security configuration pinned the entire Google API domain to a fixed set of CA keys. Version 1.2.1 removes those stale third-party pins from both Android's XML configuration and the shared HTTP client, retaining system certificate trust, hostname verification, and cleartext blocking. This follows [Android's transport-security guidance](https://developer.android.com/privacy-and-security/security-ssl).

After the fix, all three live Android authentication transport tests pass: HTTPS requests reach the affected services, Firebase Authentication returns the expected invalid-token response for a deliberately invalid test token, and cleartext stays blocked. No real account was used or created. All 41 unit tests pass. Google account selection and full cloud sync still need the user's retry after updating.
