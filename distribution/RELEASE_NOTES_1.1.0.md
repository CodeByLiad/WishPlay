# WishPlay 1.1.0 — Jinatra

- Search includes Steam and App Store games alongside RAWG for relevant platform queries. Source failures leave other providers usable and show a notice. Store editions keep separate identifiers; exact title/platform matches already present in RAWG are suppressed in the first search page.
- Game details cross-check RAWG's linked Steam and App Store listings. Linked store dates apply only to that store's platform; other port dates stay independent. Source links and conflicting dates remain visible. Store data uses the US storefront, so dates can differ in other regions. Android and PlayStation dates continue to come from RAWG.
- Save the platforms you want to track on each wishlisted game's details page. Reminders, upcoming releases, calendar entries and the home widget use those choices. Platform preferences also sync with your account and are included in backups.
- Profile → Wishlist backups exports games, notes, release sources and reminder choices to a JSON file using Android's document picker. Import shows a preview, keeps existing wishlist entries, and checks the free wishlist limit before a single transactional merge. It never replaces or clears the list. Backup files exclude account credentials.
- Database version 4 migrates to version 5 without deleting existing games, wishlist entries or alert settings. The package and Jinatra signing certificate are unchanged from 1.0.1, so this APK updates that release in place.
- Platform selection controls expose their selected state to accessibility services and have 48 dp touch targets.

Catalog coverage is limited by provider availability. A linked store release refers to its store edition, which can differ from other PC or iOS releases.
