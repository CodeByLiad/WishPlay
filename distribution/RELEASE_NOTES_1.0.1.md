# WishPlay 1.0.1 — Jinatra

- Package and application ID: `com.jinatra.wishplay`. This installs separately from the previous application ID; its local data is not migrated automatically. Firebase registration and release/debug signing certificates are configured for the new ID.
- Search now pages through the live game catalog, with Android, PC, PS5, PS4, Xbox Series, Switch, and iOS filters. Connection failures show a retry action instead of fictional demo games. Provider coverage varies; custom games remain available for missing titles.
- Search results retain every listed platform. Full game details load when a game is opened or added, including separate release dates for each port. Unknown dates remain TBA.
- Wishlist platform filters, countdowns, sorting, next-release cards, the calendar, the widget, reminders, and date-change detection account for staggered platform launches and use the current device date.
- Custom games support independent release dates per platform and validate dates before saving.
- Artwork shows a loading placeholder and falls back from a missing logo to the cover. Real artwork uses a less restrictive rounded crop.
- Shared safe-area and keyboard handling prevents content overlapping system bars. Navigation uses measured layout padding and keeps the selected tab on rotation. Search titles and platform labels can wrap.
- Refreshing a game preserves its wishlist entry and alert settings. Removed invented prices, release years, trailer links, and seeded demo alerts from the user flow.

Additional data providers, region-specific releases, store links, and import/export are suggestions for a future version and require approval.
