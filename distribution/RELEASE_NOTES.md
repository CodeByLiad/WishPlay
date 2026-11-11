# WishPlay 1.2.2 — Working Android search and notification setup

- Android searches now use Google Play listings as an independent source. Search and browse can show real games when RAWG is unavailable.
- Steam and App Store results also stay available during a RAWG outage. Provider deadlines and an overall search deadline prevent an endless loading spinner.
- Saving a result no longer waits for another remote details request. Store-only records preserve unknown release dates rather than inventing dates.
- Onboarding uses platform marks and a PC monitor icon instead of two-letter placeholders. Platform names distinguish PS4/PS5 and Switch/Switch 2.
- Android 13+ offers notification setup after onboarding, and once for existing users who were never asked. “Turn on notifications” opens Android's permission dialog; “Not now” lets you continue without repeated prompts. Earlier Android versions do not have this runtime notification dialog.
- Fixed onboarding navigation rebuilding the main screen and interrupting permission setup.
- Every feature remains free. Optional bKash support: **01978900129**. No admin screens.

Install over the existing Jinatra app to keep your wishlist. Same package and signing certificate; version code **6**.

Console searches still depend on RAWG availability. Google Play listing formats and store availability can change. Real-account Google sign-in/cloud sync and physical-phone background delivery are not claimed as fully verified by emulator tests. The sign-in TLS fix from 1.2.1 is included.

Verification: 41 unit tests and 15 targeted Android checks passed across live catalog, search/details/save, platform selection, actual notification permission, reminder posting, and provider-failure regression runs. Release lint and signed assembly passed. See Docs/Release_Verification.md for exact scope and limits.
