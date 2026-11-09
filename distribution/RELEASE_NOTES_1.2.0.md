# WishPlay 1.2.0

WishPlay is now completely free. All features are available to guests and signed-in users without upgrading or donating.

## Changes

- Removed the 25-game wishlist/import limit and unlocked all accent themes, the home widget, and supported price-drop alerts.
- Added optional bKash donations in Profile: **01978900129**, with a copy-number button. Donations do not unlock extra features.
- Removed Pro upgrades, checkout, promo-code redemption, admin screens, admin navigation, and privileged-account controls from the Android app.
- Improved navigation, preserved tab positions, made empty filters recoverable, and connected wishlist sharing to actual saved games.
- Added calendar date selection, inline date validation, a persistent Save action, draft-discard confirmation, and direct navigation to saved custom games.
- Added removal confirmation and protection against repeated wishlist/alert updates.
- The release calendar now opens the current month and includes a This month shortcut.
- Connected onboarding to the real Google sign-in flow with loading and error feedback.
- Connected per-game price-alert settings to storage and background checks.
- Future in-app update checks use this repository's distribution manifest.

## Verification and known limits

- Package: `com.jinatra.wishplay`; version code: **4**. Signed with the existing Jinatra release certificate for compatibility with previous Jinatra APKs.
- 41 unit tests and 15 selected emulator tests passed before release preparation. Tests cover backup/import beyond the former cap, migration, catalog fallbacks with fixture data, free themes, donation copying, date selection, platform preferences, and posting all four notification types to Android.
- Live catalog verification returned HTTP 522 from RAWG. Catalog completeness and remote artwork availability are not guaranteed; custom games remain available.
- Successful Google sign-in/cloud sync with a real account and long-running reminder delivery under physical-device battery restrictions remain unverified.

The SHA-256 checksum and update manifest are attached alongside the APK.
