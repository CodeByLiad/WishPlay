# Antigravity build prompt — WishPlay

Paste the block below as the first message in Antigravity. After phase 1, use the per-phase kickoff at the bottom.

---

## First message

```text
Build WishPlay, a native Android app, from the two source documents in this repo.

SOURCES OF TRUTH
- Docs/WishPlay PRD.md — the specification. Requirements, data model, security model, build phases.
- Assets/WishPlay app prototype.html — the approved UI. Open it in a browser and click through it.
  Every screen, every state, every piece of copy in that file is the design contract.
  Where the PRD and the prototype disagree, the PRD wins for behaviour and the prototype wins for layout and wording.

WHAT TO BUILD
Package com.Jinatra.wishplay. Kotlin, Jetpack Compose with Material 3 Expressive, MVVM with a
repository layer, Hilt, Room encrypted with SQLCipher, WorkManager, OkHttp with certificate pinning,
Coil, Credential Manager + Supabase Auth, Jetpack Glance for the widget. Backend is Supabase
(Postgres, Auth, Edge Functions in TypeScript). Payments through Paddle Billing. Minimum Android 8.0
(API 26), target the latest API level.

DESIGN SYSTEM — extract these from the prototype, do not invent your own
- Gold #FFB938 on ink #16132E. The full light and dark token sets are the CSS custom properties
  at the top of the prototype; port each one to a Compose ColorScheme.
- Bricolage Grotesque for display type, Figtree for body. The type ramp is the .display/.h1/.h2/.small
  classes.
- M3 Expressive shape morphing: buttons, chips, list rows and FABs change corner radius on press.
  The prototype does this with a cubic-bezier(.2,1.35,.3,1) spring — use the equivalent Compose
  spring and the M3 Expressive shape APIs.
- The "cookie" polygon shapes, the wavy progress indicator on the countdown card, and the grouped
  list rows with rounded first and last children are all load-bearing. Keep them.
- Light, dark and system themes are free. Extra accent colours are Pro.

SCREENS — all of these exist in the prototype
Welcome, setup step 1 (platforms), setup step 2 (reminder lead), Wishlist tab, Search tab,
Game details, Custom game form, Alerts tab, Release calendar, Profile tab, Get Pro, Admin panel,
plus these sheets: list full, sign in to buy, card checkout, bKash, redeem a code, sort,
notification priming, share, background work, privacy, delete account.

NON-NEGOTIABLE RULES
1. The APK is untrusted. No check that runs only on the phone protects Pro, payments or admin.
   Pro comes from a server-signed Ed25519 entitlement token gated behind hardware key attestation.
   Server-side features (price data, the 25-game sync cap) check the account on the server.
2. No secret ships in the APK. IGDB, Steam, Paddle and price-API keys live in Supabase secrets.
   The app talks only to WishPlay's own Edge Functions.
3. Row-level security on every table. "Server only" in the PRD's data model means no RLS policy
   permits a write from the app's user session.
4. Offline-first. Room is the source of truth for the UI; sync runs in the background.
   Wishlist, details and scheduled reminders all work with no connection.
5. Notification permission is requested after the first game is added, never at launch, and the
   in-app priming sheet from the prototype comes before the system dialog.
6. Pro price is $2.99 lifetime. The app never builds a price — it asks the backend for a checkout link.
7. Every missing-data field has a specific fallback string. They are listed in the PRD's
   "to be announced" table and shown in the prototype. Use those exact strings.
8. Accessibility: 48dp touch targets, TalkBack labels on every icon button, 4.5:1 text contrast,
   font scaling to 200%, and reduced-motion respected.

HOW TO WORK
Follow the PRD's seven build phases in order. Each phase ends with an installable APK I can test —
do not start a phase before the previous one runs on a device. Phase 1 is project setup, the design
system ported from the prototype, Welcome and guest mode, and a local wishlist with custom games.

Write tests as you go: unit tests for the alert scheduling and date-change logic, the sort and
month-grouping logic, the entitlement token verification, and the code-redemption transaction.
Instrumented tests for the Room DAOs and the sync merge.

Before you write any code, read both source files in full, then tell me:
- your module and package structure,
- the Compose theme mapping from the prototype's CSS tokens,
- anything in the PRD you think is wrong, underspecified, or will not work as written.
Wait for my answer before starting phase 1.
```

---

## Per-phase kickoff (phases 2–7)

```text
Phase N from Docs/WishPlay PRD.md "Build phases". Same rules as the first message.
Show me the plan for this phase, then build it. Stop at the phase's stated "done when" condition.
```

---

## Settled decisions

These are answered and reflected in the PRD and the prototype. Listed here so the agent does not reopen them.

| Decision | Answer |
| --- | --- |
| Pro price | $2.99 lifetime, one-time |
| List sharing | Read-only share link and share-as-image only. No friends, no comments, no shared editing |
| Price data provider | IsThereAnyDeal. Attribution required; the terms allow alerting on a user's own wishlist but not rebuilding a price-comparison site. Key lives in Supabase secrets, called only from the Edge Function, and only for Pro accounts |
| BDT price | 349 BDT |
| bKash number | 01978900129, personal account, Send Money (not Payment). Store it as a server-side config value the bKash sheet fetches — never hardcode it in the APK, so it can change without an app update |
| Bangla translation | Later update. Ship English only, but from phase 1 every string lives in resources, plurals use quantity strings, and dates go through the locale formatter, so adding `values-bn` needs no code changes. Note that Bricolage Grotesque has no Bangla coverage — a second display face will be needed then |
| Free-list cap | 25 games. Enforced on the server for signed-in accounts, not only in the app |
| Install limit per Pro account | 5 active installs. Admin can see and revoke them |

## Still open

Answer these before the phase they block. Fill each row in as it is decided.

| Decision | Blocks | Status |
| --- | --- | --- |
| Jinatra website and domain | Phase 7 | Not chosen. Waiting on the Jinatra site going live. Build phases 1-6 without it. The share links in the prototype read `wishplay.app/l/...` as a placeholder; make the share base URL a config value, not a literal. Paddle seller approval needs a live site with the product, pricing and a refund policy, and can take days, so start it as soon as the domain exists |
| Paddle's quote for a $2.99 product | Phase 5 | Not requested yet. Paddle's standard 5% + $0.50 applies at $10 and above; below that is custom pricing through their sales team. If the quote is unworkable, compare Lemon Squeezy before building the checkout |
