# WishPlay 1.3.0 — Material Expressive redesign

- Fixed missing Google Play featured/exact matches, including Offroad League Online. Exact title matches rank before recommendations, and punctuation variants share a cache entry.
- State-based native text input preserves rapid keystrokes and cursor/IME state. Clear and recent-query selection are covered by a native-keyboard regression test.
- Search streams each working provider immediately instead of waiting for RAWG. Recent queries use an in-memory cache, stale entries refresh in the background, superseded requests cancel, and typing debounce is reduced.
- Rebuilt the shared theme with full Gold, Violet, Teal, and Coral palettes in light/dark modes. Backgrounds, cards, navigation, controls, sheets, and text follow the selected palette.
- Adopted Google's Material Expressive theme, motion, native shape families, animated button shapes, morphing loading indicator, standard Material switches, filter chips, and segmented selection controls.
- Replaced the compact wishlist rows with a cover-led library. Search uses larger artwork, concise metadata, a native Material text field, and calmer add controls.
- Reorganized Settings around a compact account header and appearance controls, followed by backups, device/reminder settings, account actions, and optional support. Accent choices have visible names.
- Fixed enabled switches whose track and thumb previously used the same color. The thumb and checked indicator now remain distinct.
- Added Steam minimum/recommended PC requirements and storage parsing, including labelled HTML, entities, multiline values, and footnote markers. Linked Steam evidence enriches RAWG details too.
- Unknown specifications, trailers, and storage no longer promise they will arrive later. Removed generic invented mobile hardware requirements and added a direct store-listing action.
- Added on-demand scheduler initialization for reliable update/startup handling.

Every feature remains free. Optional donations only display/copy bKash number **01978900129**. No admin or paid upgrade screens.

Install over the existing Jinatra app to retain your wishlist. Same Android package and signing certificate; version code **7**. Material Expressive currently uses Google's experimental Material3 1.5.0-alpha06 APIs. See `Docs/Design_System.md` for component mapping and the Material/Mobbin references used.

Console search remains dependent on RAWG. Missing provider data is not invented. Store regions are US; catalogs and physical-device background delivery can vary. A completed Google account sign-in/cloud sync is not claimed by the transport-only tests.

Verification: 41 unit tests and 37 distinct targeted Android checks passed across final successful runs. Live Offroad first results measured 1,382 ms and its cached repeat 1 ms on the test emulator; connection/device speed varies. The deterministic cache test made zero additional HTTP requests. See Docs/Release_Verification.md for the exact scope.

The final signed APK also passes a native-input smoke check for guest setup, optional notifications, the exact Offroad query, real details, and theme selection.
