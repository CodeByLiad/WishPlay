# WishPlay PRD

Sep 30, 2026 · @Liad Bin Aowlad

## Overview

WishPlay is a native Android app, published by Jinatra, that lets players save games from any platform to a wishlist and get alerted before they release. It ships as a direct APK (package `com.Jinatra.wishplay`), free to use, with a one-time lifetime Pro upgrade.

**Problem.** Players follow games across PC, consoles and mobile, and release dates are scattered across stores, trailers and social posts. Dates also move. People forget what they wanted to play, or miss the launch.

**Goals**

- One list for every game a player is waiting for, on any platform, including games no database knows about yet.
- A useful details page for each game: cover, logo, trailer, system requirements, storage size, and clear "to be announced" states when data is missing.
- Reliable alerts for release day, countdown reminders, date changes and (Pro) price drops.
- Works offline and without an account; Google sign-in adds sync.
- A Pro upgrade that cannot be unlocked by modifying the APK.

**Non-goals for v1**

- Buying games or linking to store checkouts for games.
- Social features: friends, comments, reviews, or lists other people can edit. A one-way, read-only share link for your own list is in scope; collaboration is not.
- An iOS app or a web app.
- Price tracking for console and mobile stores (no reliable public APIs).

## Users, platforms and plans

The target user is a player on Android who follows upcoming games on any platform, with a strong early audience in Bangladesh. The app tracks games for PC, PlayStation, Xbox, Nintendo Switch, Android and iOS; release dates are tracked per platform.

| Plan | Who | Access | How it's granted |
| --- | --- | --- | --- |
| Guest | Anyone, no account | Full wishlist and free alerts, stored on this phone only | Default when skipping sign-in |
| Free | Signed in with Google | Everything in Guest, plus cloud sync and backup | Google sign-in |
| Pro | Signed-in user who paid or redeemed | Unlimited wishlist, price-drop alerts, accent themes, home-screen widget | Server-side entitlement after payment, code or bKash approval |
| Admin | hyathis.x@gmail.com only | Lifetime Pro plus the admin panel (codes, bKash approvals) | Server-side admin role; never checked in the app |

Pro requires a Google account because the entitlement lives on the server, tied to the account. Guests can see the Pro screen but must sign in before paying.

## Features and requirements

| Feature | Free and Guest | Pro | Requirement |
| --- | --- | --- | --- |
| Wishlist | Up to 25 games | Unlimited | Add, remove, filter (All, Upcoming, Out now, Date TBA), sort (nearest release, recently added, name A-Z); the date sort groups the list by month |
| Game search | Yes | Yes | Search by title with platform filter; recent searches and popular titles before typing; results show platforms and release label |
| Custom games | Yes, counts toward the cap | Yes | Name required; platforms, release date or "not announced", notes optional |
| Game details | Yes | Yes | Cover, logo, trailer, about, per-platform release date, requirements, storage |
| Release-day alert | Yes | Yes | On release day at a user-set time (default 9:00 AM) |
| Countdown reminder | Yes | Yes | 1 day, 3 days or 1 week before release |
| Date-change alert | Yes | Yes | Delays and earlier launches; reminders reschedule automatically |
| Price-drop alert | No | Yes | PC stores only (Steam, via price APIs) |
| Accent themes | No | Yes | Light, dark and system theme are free; extra accent colors are Pro |
| Home-screen widget | No | Yes | Next-release countdown widget |
| Cloud sync | Signed-in only | Yes | Sync across phones; guest list merges on first sign-in |
| Redeem codes | Signed-in only | n/a | Free-Pro codes and percentage discount codes |
| Release calendar | Yes | Yes | Month grid with a marker on every release day, plus an agenda for the selected day |
| Share your list | Yes | Yes | Read-only link, or the list as an image through the Android share sheet. Recipients cannot change anything |
| Restore purchase | n/a | Yes | Re-fetches the entitlement for the signed-in account after a reinstall or on a new phone |
| Delete account | Signed-in only | Signed-in only | Typed confirmation, then server-side removal of the profile, list and entitlement |

**Per-game alert switch.** Every game has its own bell toggle, on by default for upcoming games.

**Free cap.** When a free list reaches 25 games, adding another shows the "Your list is full" sheet with an upgrade option. Existing games are never removed if a Pro user's entitlement is ever revoked; they just can't add past the cap.

## Screens and flows

The UI follows the approved Material 3 Expressive prototype: bottom navigation with four tabs, gold accent (#FFB938) on ink (#16132E), Bricolage Grotesque for display type, light, dark and system themes. Every list that can grow past a screenful has an explicit sort control and month grouping, and every empty state distinguishes "you have nothing yet" from "this filter matches nothing".

| Screen | Contents |
| --- | --- |
| Welcome | Logo, "Never miss a game drop", Continue with Google, Continue as guest |
| Setup step 1 of 2 | "Which platforms do you play on?" - multi-select platform grid, progress bar, Back and Skip. Seeds the Search platform filter |
| Setup step 2 of 2 | "How early should we tap you?" - 1 day, 3 days or 1 week, with a preview of the notification. Sets the default countdown lead |
| Wishlist tab | "Next drop" countdown card with wavy progress, filter chips, sort button, share button, month-grouped game list with release badges, free-cap row, Add game FAB (Search games, Add a custom game). First-run empty state is its own screen, separate from an empty filter |
| Search tab | Search field, platform chips, recent searches (removable, clear all) and popular titles before typing, upcoming games, add buttons, "Add as a custom game" empty state |
| Game details | Cover, logo, quick actions under the title (add or on-your-list, alert bell, share), platform switcher, countdown or TBA or Out now card, trailer, about, requirements (Minimum and Recommended for PC), space needed, PC price with Pro price alert, remove at the bottom |
| Custom game form | Name, platforms, date with quick presets (a month, 3 months, next year) or "not announced yet", notes, live preview of the alert that will fire |
| Alerts tab | Coming up grouped by month, calendar entry point, Recent (delays, price drops, releases), alert settings, delivery rows (Android notification state, background work) |
| Release calendar | Month grid with a marker on days that have a release, month navigation, agenda for the selected day or the whole month |
| Profile tab | Account, Pro card, theme, accent colors, widget, notifications state, background-work tip, share your list, redeem code or restore purchase, admin panel (admin only), check for updates, privacy policy, about, sign out, delete account |
| Get Pro | Price card, a Free-vs-Pro comparison of every feature, Pay with card, Pay with bKash, Redeem a code, Restore a purchase, "Good to know" answers (reinstall, new phone, refunds) |
| Admin panel | Stats, bKash payments to check, create code, searchable code list with copy and on/off |

**Key flows**

1. First launch: Welcome, then Google sign-in or guest, then two setup steps (platforms, then reminder lead), then a first-run empty wishlist that points to Search. Both steps can be skipped, and both settings are editable later in Profile.
2. Add a game: Search, tap add, the game joins the list with alerts on. On the very first add, the notification-permission sheet appears before the confirmation; it is never shown at launch. At the cap, the full-list sheet appears with Get Pro and Manage my list.
3. Add a custom game: FAB, form, save; the date the user sets drives its alerts. The form previews the alert it will produce.
4. Browse the calendar: Alerts tab, calendar button, month grid; tap a day for that day's releases, or leave it unselected for the whole month.
5. Share the list: wishlist header or Profile, then either a read-only link or the list as an image through the Android share sheet.
6. Buy Pro with card: Get Pro, sign in if needed, Paddle checkout in the browser, the server confirms payment, the app refreshes and unlocks Pro.
7. Buy Pro with bKash: send money, enter TrxID, the admin approves, Pro unlocks on the next refresh.
8. Redeem a code: a free-Pro code unlocks Pro; a discount code shows the new price and continues to checkout.
9. Restore a purchase: Profile or Get Pro, Restore; the app re-fetches the entitlement for the signed-in account. Nothing is granted on the phone.
10. Delete the account: Profile, Danger zone, type DELETE to confirm; the server removes the profile, list and entitlement, and the app returns to Welcome as a guest.

## Game data and "to be announced" rules

All game data comes through WishPlay's own backend proxy, never straight from the app to a third-party API, so no API keys ship in the APK and results can be cached.

| Data | Source | Notes |
| --- | --- | --- |
| Search, title, developer, summary | IGDB (Twitch developer API) | Client-credentials token held on the server only |
| Cover and logo | IGDB images | Cached URLs; app caches images on disk |
| Release date per platform | IGDB release dates | Includes TBA and year-only dates |
| Trailer | IGDB videos (YouTube IDs) | Opens in the YouTube app or browser |
| PC system requirements, PC install size | Steam store data, matched by IGDB's Steam ID | PC only |
| Console and mobile storage size | IGDB or store listings where available | Often missing |
| PC price and discounts | A PC price API such as IsThereAnyDeal | Pro feature only |

**Fallback text when data is missing**

| Missing field | Shown as |
| --- | --- |
| Release date | "To be announced", plus "Expected in 2027" when only a year is known |
| Trailer | "Trailer will be shared later" |
| System requirements, one level or all | "Will be shared later" per row |
| Storage size | "Will be shared closer to launch" |
| Console requirements | "Runs on any PS5. No extra hardware needed." (not a missing state) |

**Caching.** The backend caches each game for 24 hours and each search for 1 hour. The app keeps a local copy of every wishlisted game so the list and details work offline.

**Custom games** store only what the user typed. They show a "Custom" tag and a note that covers, trailers and requirements can't be fetched.

## Alerts engine

All alerts run on the phone; there is no push server in v1.

- **Daily check.** A WorkManager periodic job runs once a day (network required, battery-not-low). It asks the backend for fresh data on every wishlisted game in one batched request, compares release dates and prices with the local copy, and records changes.
- **Scheduled reminders.** Release-day and countdown reminders are scheduled locally from the stored dates, so they fire even if the phone is offline that day. When a date changes, the old reminders are cancelled and new ones scheduled.
- **Date-change alert.** Fires when a platform's date moves, appears for the first time, or goes back to TBA. The game shows "Moved from Oct 22 to Dec 3" in the list and details.
- **Price-drop alert (Pro).** Fires when a PC price falls below the last seen price. The Pro check for this is done by the backend, which only returns price data to Pro accounts.
- **Custom games** use only the user's date; no daily check.
- **Notifications** use a single-color notification icon, one channel per alert type (Releases, Reminders, Date changes, Price drops) so users can mute each in Android settings, and a deep link to the game's details.
- **Permissions.** Ask for notification permission (Android 13+) after the first game is added, not at launch. An in-app sheet explains what the alerts are and shows a sample notification before the system dialog appears, so a reflex "Don't allow" does not cost the app its only delivery channel. If the user declines, Profile and the Alerts tab both show the blocked state and a way back. Exact alarms are not needed; a reminder landing within a few minutes of 9:00 AM is fine.
- **Reliability.** Survive reboots (reschedule on boot), and show a one-time tip on phones with aggressive battery savers explaining how to allow background work. The same tip stays reachable from the Alerts tab and Profile as a "Background work" row.

## Accounts, guest mode and sync

The app is offline-first: the local Room database is the source of truth for the UI, and sync runs in the background.

- **Sign-in.** Google sign-in through Android Credential Manager. The app sends the Google ID token to Supabase Auth, which verifies it and returns a session. The release signing certificate's SHA-1 must be registered in Google Cloud, or sign-in fails on the release APK.
- **Guest mode.** Everything free works with no account. Data stays on the phone and is lost on uninstall; the Profile tab says so and offers sign-in.
- **First sign-in merge.** The guest list is uploaded and merged with any existing cloud list, matched by IGDB game ID (custom games by name). Nothing is deleted in a merge.
- **Sync.** Each wishlist item carries `updated_at`; the newest change wins. Deletes are soft (a `deleted_at` flag) so they sync to other phones.
- **Sign-out** clears local data, cached entitlements and tokens, then returns to Welcome.
- **Account deletion** is available from Profile and removes the user's rows server-side (a requirement for many app stores and good practice for a direct APK too).

## Monetization, codes and admin

Pro is a one-time lifetime purchase of $2.99, tied to the user's Google account, with no upfront costs: every payment route takes its cut per sale.

| Route | For | How Pro unlocks | Cost |
| --- | --- | --- | --- |
| Paddle card checkout | International buyers and local dual-currency cards | Paddle sends a signed webhook to the backend, which grants Pro | 5% + $0.50 per sale; paid monthly to Payoneer or bank |
| bKash Send Money | Buyers in Bangladesh | User submits TrxID; admin checks the bKash statement and approves | bKash's normal cash-out charge |
| Free-Pro code | Giveaways | Server validates and redeems the code | Free |
| Discount code | Promotions | Server validates the code and creates a Paddle checkout at the discounted price | Paddle fee on the lower price |

**At $2.99 per sale.** Paddle's standard fee would be about $0.65 (22%), leaving about $2.34, and Paddle treats products under $10 as custom pricing, so the rate must be confirmed with Paddle before launch. Discounts cut deep at this price: 50% off is $1.50, of which about $0.58 goes to fees. Paddle's $100 minimum payout is reached at about 43 full-price sales.

**Checkout.** The app never builds a price. It asks the backend for a checkout link; the backend creates a Paddle transaction with the user's ID in `custom_data` and the price (after any discount), and returns the URL, which opens in a Custom Tab. When Paddle's `transaction.completed` webhook arrives, the backend grants Pro. The app then refreshes its entitlement.

**Codes**

- Format `WP-XXXX-XXXX`, from an alphabet without 0, O, 1 and I; at least 8 random characters from a secure random source.
- Each code has a type (free Pro or percentage), max uses, expiry date and an on/off switch.
- One redemption per account per code; redemption is a single database transaction so two people can't take the last use at once.
- Redeem attempts are rate-limited: 5 per account per hour, 20 per IP per hour.

**Admin panel (in the app, admin account only)**

- Stats: active codes, redemptions, bKash payments waiting.
- bKash queue: TrxID, sender number (masked), time; Approve or Reject. A TrxID can only ever be approved once.
- Create code: type, discount %, max uses, expiry.
- Code list: copy, turn off, uses so far.
- Every admin action is written to an audit log with the time and target.

## Architecture and tech stack

The app talks only to WishPlay's own backend. Game data, payments and Pro decisions all pass through Edge Functions, which hold every secret key.

&#91;embedded content: WishPlay architecture · app, backend, outside services\]

The only direct outside calls from the phone are Google sign-in and opening Paddle's checkout page; Paddle then reports the payment straight to the backend.

| Layer | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose with Material 3 Expressive |
| Architecture | MVVM with a repository layer, Hilt for dependency injection |
| Local database | Room, encrypted with SQLCipher |
| Background work | WorkManager |
| Networking | OkHttp with certificate pinning, Kotlin Serialization |
| Images | Coil with disk cache |
| Sign-in | Android Credential Manager and Supabase Auth |
| Widget | Jetpack Glance |
| Backend | Supabase: Postgres, Auth, Edge Functions (TypeScript) |
| Payments | Paddle Billing (hosted checkout, webhooks) |
| Crash reports | A crash reporter with personal data stripped |
| Build | R8 full mode, release signing with v2 and v3 schemes |

## Data model

**Backend (Supabase Postgres, row-level security on every table)**

| Table | Key fields | Who can write |
| --- | --- | --- |
| `profiles` | `id` (auth user), `email`, `created_at` | Owner (limited fields) |
| `wishlist_items` | `id`, `user_id`, `igdb_id` or `custom_title`, `platforms`, `custom_dates`, `notes`, `alert_on`, `price_alert_on`, `updated_at`, `deleted_at` | Owner only |
| `entitlements` | `user_id`, `plan`, `source` (paddle, bkash, code, admin), `granted_at`, `source_ref` | Server only |
| `admins` | `user_id` | Server only (set by hand in the database) |
| `payments` | `paddle_txn_id` (unique), `user_id`, `amount`, `currency`, `status` | Server only |
| `promo_codes` | `code`, `type`, `pct`, `max_uses`, `uses`, `expires_at`, `active` | Server only, via admin functions |
| `code_redemptions` | `code`, `user_id`, `redeemed_at`; unique on (`code`, `user_id`) | Server only |
| `bkash_submissions` | `trx_id` (unique), `user_id`, `status`, `reviewed_by`, `reviewed_at` | Owner can insert; server updates |
| `audit_log` | `actor`, `action`, `target`, `at` | Server only, append-only |
| `game_cache` | `igdb_id`, `data`, `fetched_at` | Server only |

"Server only" means no row-level policy allows writes from the app's user session; only Edge Functions running with the service role can write.

**On the phone (Room)**

| Table | Purpose |
| --- | --- |
| `games` | Cached game data for everything on the list |
| `wishlist` | The user's items, sync state |
| `scheduled_alerts` | Reminders set with WorkManager, for rescheduling |
| `entitlement` | Last signed entitlement token and its expiry |
| `settings` | Theme, accent, alert preferences |

The local database is encrypted with SQLCipher, key stored in Android Keystore.

## Security

The rule behind every decision here: **the APK is untrusted.** Anyone can decompile it, patch it and re-sign it, so no check that runs only on the phone can be the thing that protects Pro, payments or admin. Every valuable decision is made on the server, and the phone only displays the result.

No Android app can be made impossible to mod. The goal is that a modded APK gets nothing worth having: server-backed features stay locked, and the local ones take real effort to unlock and stop working when the token expires.

### Threats and defenses

| Threat | Defense |
| --- | --- |
| Patch the APK to flip `isPro = true` | Pro comes from a server-signed token; server features check the account on the server; re-signed APKs fail key attestation and never get a token |
| Fake a "payment successful" screen or callback | Pro is only granted by a verified Paddle webhook, never by anything the app reports |
| Replay or forge a Paddle webhook | Verify the `Paddle-Signature` HMAC with the webhook secret, reject old timestamps, process each transaction ID once |
| Pay, then refund or charge back | Refund and chargeback webhooks revoke the entitlement |
| Reuse or fake a bKash TrxID | TrxID is unique in the database; admin matches it against the bKash statement before approving |
| Brute-force or share promo codes | 8+ random characters, rate limits, max uses, one use per account, expiry, admin can switch a code off |
| Become admin by patching the app | Admin role lives in a server-only `admins` table and is checked inside every admin function |
| Steal API keys from the APK | No secret ships in the APK; IGDB, Paddle and price API keys live in Supabase secrets |
| Read or change other users' data | Row-level security on every table; users can only touch their own rows |
| Intercept traffic | HTTPS only, cleartext disabled, certificate pinning for the backend domain with a backup pin |
| Distribute a malicious "WishPlay" APK | Publish the signing-certificate fingerprint; the in-app updater only installs APKs signed with the same certificate |
| Share one Pro account widely | Up to 5 active installs per account; admin can see and revoke installs |

### How Pro is protected

1. **Entitlement lives on the server.** The `entitlements` table can only be written by server functions: the Paddle webhook, bKash approval, code redemption, or the admin setup.
2. **Server-side features check the account, not the app.** The backend only returns price data to Pro accounts, and the sync API refuses to store more than 25 games for non-Pro accounts. A modded app can pretend locally, but these features simply don't work for it.
3. **Signed entitlement token.** For local Pro features (unlimited local list, accent themes, widget), the server issues a short-lived token signed with an Ed25519 private key that never leaves the server. It holds the user ID, install ID, plan and an expiry of 14 days. The app verifies it with the public key and refreshes it whenever online. With no valid token, local Pro features switch off.
4. **Hardware key attestation before issuing a token.** On first sign-in the app creates a key in the Android Keystore with a server-provided challenge. The attestation certificate chain, signed by the phone's secure hardware, reports the app's package name and signing-certificate digest. The server checks the chain against Google's root certificates and confirms the digest matches the Jinatra release certificate. A modded APK must be re-signed with a different key, so it fails this check and never receives a Pro token.
5. **App hardening** to slow down patching: R8 full-mode obfuscation, release logging stripped, runtime check of the app's own signing certificate, and light detection of hooking tools and root. These never block the free app; they only stop the phone from requesting a Pro token and flag the install for review.
6. **Quiet failure.** When tampering is detected, the app doesn't crash or show a warning that tells a modder what triggered. Pro features just stay locked.

Play Integrity's app-recognition check is built around Play Store distribution, so it isn't the main defense for a direct APK; key attestation does that job here.

### Payment security

- Card details never touch WishPlay; Paddle's hosted checkout handles them.
- The backend creates every checkout, sets the price, and attaches the user ID. The app can't set or change a price.
- The webhook handler checks the signature, the product ID, the amount and currency against the expected price or valid discount, and the transaction status before granting Pro.
- All grants and revocations go into `payments` and `audit_log`.

### Backend security

- Edge Functions hold the service role; the app only has the Supabase publishable (anon) key, which is safe to ship because row-level security guards every table.
- Every function verifies the user's session token and validates its input.
- Rate limits on search, sign-in, code redemption and bKash submissions.
- Two-factor authentication on the admin Google account, Supabase, Paddle, Payoneer and GitHub.
- Daily database backups; secrets rotated if anyone else ever gets access.

### Data on the phone

- Room database encrypted with SQLCipher; the key sits in Android Keystore.
- Tokens stored encrypted with a Keystore-backed key; excluded from Android backups (`allowBackup` off or backup rules exclude them).
- Screenshots blocked on the admin panel.

### Privacy

- Collect only the Google account ID, email, name and the user's list. No ads, no third-party trackers.
- Crash reports carry no personal data.
- A privacy policy on the website, and in-app account deletion that removes server data.

## Distribution and updates

WishPlay is released as a signed APK on GitHub Releases and a download page on the Jinatra website.

- **Signing.** APK Signature Scheme v2 and v3. The release keystore is kept offline with two encrypted backups; losing it means users can't update, so this is critical.
- **Verification for users.** The download page lists the APK's SHA-256 and the signing-certificate fingerprint.
- **In-app updates.** On launch (at most once a day) the app fetches a small signed update manifest: latest version code, APK URL, SHA-256, minimum supported version, and release notes. It downloads over HTTPS, checks the hash and that the APK's signing certificate matches the installed app's, then hands it to the Android package installer.
- **Forced updates.** If the installed version is below the manifest's minimum, Pro-token refresh stops and a full-screen update prompt appears. This closes old versions with known security holes.
- **Install permission.** The app asks for "Install unknown apps" only when the user taps Update.
- **Minimum Android version:** Android 8.0 (API 26); target the latest API level.

## Non-functional requirements

| Area | Target |
| --- | --- |
| Cold start | Wishlist visible in under 1.5 s on a mid-range phone, from local data |
| Search | Results in under 1 s on a normal connection (cached searches faster) |
| Offline | Wishlist, details and scheduled reminders work with no connection |
| APK size | Under 20 MB |
| Battery | One background check per day; no wake locks |
| Accessibility | Touch targets at least 48 dp, TalkBack labels on every icon button, text contrast 4.5:1, supports system font scaling to 200% |
| Themes | Light, dark and system; Material 3 Expressive components and motion, with reduced-motion respected |
| Language | English at launch; strings kept in resources so Bangla can be added |
| Backend cost | Stays within Supabase's free tier at launch |
| Crash-free sessions | 99.5% or better |

## Build phases

Each phase ends with a working APK you can install and test.

1. **Foundation.** Project setup (Kotlin, Compose, Material 3 Expressive theme, Room with SQLCipher), design system from the prototype, Welcome and guest mode, wishlist and custom games stored locally. Done when a guest can build a list offline.
2. **Game data.** Supabase project, IGDB and Steam proxy Edge Functions with caching, Search tab, Game details with every "to be announced" state. Done when search and details work with real data and no keys are in the APK.
3. **Alerts.** Daily WorkManager check, release-day and countdown reminders, date-change detection, notification channels, reboot handling. Done when a changed date on the server produces an alert on the phone.
4. **Accounts and sync.** Google sign-in, guest merge, two-way sync with soft deletes, account deletion. Done when one list shows up on two phones.
5. **Pro and payments.** Entitlements, key attestation and signed tokens, Paddle checkout and webhook, bKash submission, codes, server-side cap and price gating. Done when a Paddle sandbox purchase and a code both unlock Pro, and a re-signed APK does not.
6. **Admin and Pro features.** Admin panel, audit log, price-drop alerts, accent themes, home-screen widget. Done when the admin account can create codes and approve bKash from the app.
7. **Hardening and release.** Security pass against the threats table, certificate pinning, R8 rules, in-app updater, privacy policy, download page, Paddle live approval. Done when a signed release APK is on GitHub and the website.

## Settled decisions

- [x] **Pro price.** $2.99 lifetime, one-time.
- [x] **BDT price.** 349 BDT for the bKash route.
- [x] **bKash number.** 01978900129, personal account, Send Money (not Payment). Held as a server-side config value that the bKash sheet fetches, so it can change without shipping an app update and is never hardcoded in the APK.
- [x] **Free-list cap.** 25 games, enforced on the server for signed-in accounts as well as in the app.
- [x] **Install limit per Pro account.** 5 active installs; the admin can see and revoke them.
- [x] **Price data provider.** IsThereAnyDeal, for PC price drops. Attribution is required, and its terms permit alerting on a user's own wishlist but not rebuilding a price-comparison service. The key lives in Supabase secrets, is called only from the Edge Function, and price data is returned only to Pro accounts.
- [x] **Bangla translation.** A later update, not launch. From phase 1 every string lives in resources, plurals use quantity strings, and dates go through the locale formatter, so adding `values-bn` needs no code changes. Bricolage Grotesque has no Bangla coverage, so a second display face has to be chosen and the type ramp retuned when Bangla ships.
- [x] **List sharing.** Read-only share link and share-as-image only. No friends, no comments, no lists other people can edit.

## Open decisions

- [ ] **Jinatra website and domain** for the download page, privacy policy and Paddle seller approval. Not chosen yet; waiting on the Jinatra site going live. Phases 1 to 6 do not need it. The share links in the prototype read `wishplay.app/l/...` as a placeholder, so the share base URL must be a config value rather than a literal. Paddle's seller review needs a live site showing the product, pricing and a refund policy and can take several days, so start it as soon as the domain exists.
- [ ] **Paddle's quote for a $2.99 product.** Paddle's standard 5% + $0.50 rate applies at $10 and above; below that is custom pricing through their sales team. Get the quote before building the checkout, and if it doesn't work, compare Lemon Squeezy.
