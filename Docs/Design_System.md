# WishPlay — Material Expressive interface

WishPlay is a free, offline-first game wishlist and release tracker. The main tasks are discovering a real game, saving it, tracking release dates by platform, and controlling reminders. Optional donations display/copy bKash number 01978900129; they unlock nothing. Admin and purchase flows are absent.

## Visual system

The pinned direction is Google's Material 3 Expressive, adapted to a personal game library. Artwork carries the library; color and shape support hierarchy and state. Navigation stays native Android. Avoid decorating every row with a bright primary fill.

- Wishlist: grouped two-column artwork cards, with a wider last card when a group has an odd item count. Titles and platform labels sit on a separate readable surface; release badges sit on a quiet surface over the image.
- Search: larger readable game covers, title first, concise source/platform metadata, tonal add controls, and honest provider degradation messages.
- Details: source-backed specifications, minimum/recommended segmented control, wrapped long values, and a store-listing action. Unknown fields say unavailable; no fabricated Android/iOS requirements or promises about future trailers.
- Settings: page heading, compact account identity, appearance first, backups, reminder/device controls, account actions, and optional support at the end. Appearance choices have both names and color marks.
- Alerts: standard Material switches with a distinct thumb and checked mark; native segmented countdown selection. Switches retain enabled/disabled semantics and accessible names.

## Component ownership

The app uses `MaterialExpressiveTheme`, expressive motion, native Material shapes, expressive Button shapes, standard filled/tonal/outlined/text buttons, FilterChip, Card, Switch, SegmentedButton, NavigationBar, and morphing LoadingIndicator. The selected palette supplies all primary, secondary, tertiary, surface/container, text, outline, inverse, and custom app roles. Do not override a switch thumb with the track color. Never change only primaryContainer when applying an accent.

The Expressive APIs currently use Google's experimental Material3 1.5.0-alpha06, alongside Compose BOM 2025.10.01. This choice is explicit in the dependency catalog. Upgrade the library with app-level regression checks; don't label custom approximations as native components.

## Color and behavior checks

Verify Gold, Violet, Teal, and Coral in light and dark modes. Text on its surface and the switch thumb against its track must meet at least 4.5:1 contrast. Changing appearance must immediately change the rendered screen, not only the selected swatch, and persist after recreation. Shapes must not clip names or shrink touch targets below 48dp. Long specifications and large-font text must wrap. Retain loading, error, cancellation, empty/filter-empty, offline cached details, import preview, and optional notification permission paths.

## References consulted

- [Material styles](https://m3.material.io/styles) — color roles, expressive shape and type hierarchy.
- [Android Material 3 implementation](https://developer.android.com/develop/ui/compose/designsystems/material3) and [library release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3) — actual components and API availability.
- [PlayStation library on Mobbin](https://mobbin.com/screens/f155f4ad-f85a-43b9-96c6-8896e3e9a429) — cover-led library and restrained surrounding controls.
- [Xbox library on Mobbin](https://mobbin.com/screens/4f278d6c-115b-4391-aab4-778107da24da) — compact library organization and artwork rhythm.
- [Obsidian appearance on Mobbin](https://mobbin.com/screens/3c049e50-9311-47ed-86b6-201f00a9a055) — grouped settings and explicit appearance controls.

References inform hierarchy, not copied visual assets. Game artwork comes from its catalog/store source. No Mobbin images are shipped in the APK.

Search delivery is incremental: retain valid results while slower sources finish, use recent cached results immediately, refresh stale snapshots, and cancel superseded requests. Exact title matches precede recommendations. Featured Google Play game cards and ordinary result rows are both parsed; require a game category and a valid package/icon before accepting a featured listing.
