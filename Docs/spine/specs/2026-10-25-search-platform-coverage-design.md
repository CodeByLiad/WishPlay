# Search platform coverage — design

**Date:** 2026-10-25
**Branch:** main
**Status:** awaiting approval

## Problem

`WishPlayApiServiceImpl.searchGamesPage` appends `&platforms=<id>` to the RAWG query whenever the
user has a platform chip selected (`mapPlatformToRawgId`, `WishPlayApiService.kt:134-145`). RAWG
then excludes every title whose port list does not already name that platform, and page 1 fills
with fuzzy near-misses instead.

Measured against the live RAWG API with the app's own key:

| Query | Unfiltered rank | Filtered rank |
| :--- | :--- | :--- |
| `GTA VI` | 1 (`Grand Theft Auto VI`) | 26, under `platforms=4` (PC) |
| `Delta Force Mobile` | — | 39, under `platforms=21` (Android) |

`Grand Theft Auto VI` lists only `Xbox Series S/X, PlayStation 5` in RAWG. A PC-filtered search
therefore cannot return it at all, and the first page reads `Avernum 6`, `Resident Evil 6`,
`Bloons TD 6`. To the user the game is simply absent from the database.

Two factors compound it:

1. Onboarding writes the picked platforms as the default filters ("Search and your wishlist start
   filtered to these"), so a new user searches pre-filtered from their first query onward.
2. Only page 1 merges the Steam and App Store passes, so anything RAWG ranks past 40 is invisible.

RAWG itself is not the gap. Every mobile title probed ranked first unfiltered: `Honkai Star Rail`,
`MONOPOLY GO!`, `Royal Match`, `Wuthering Waves`, `Zenless Zone Zero`.

## In scope

- Search tab only: surface titles the platform filter excludes, in a labelled second section.
- `WishlistScreen.kt:436` plural bug — `"$totalCount games you're waiting on"` renders `1 games`.
- `WelcomeScreen.kt:409` invented title — onboarding step 3 previews a notification for
  `Starfall Odyssey drops in 1 week` / `Nov 13 on PC, PS5 and Xbox`. No such game exists.

## Out of scope

- **Wishlist platform filter.** It keeps hiding non-matching games. You chose what is on your list,
  so a PC filter hiding a PS5-only game you added yourself is expected behaviour, not a discovery
  failure.
- **A Google Play / Android provider.** Google Play has no public search API; adding an Android
  source is a provider decision, not a code fix, and RAWG covers the mobile titles probed.
- **RAWG alias and abbreviation handling.** `GTA VI` ranks first unfiltered, so the filter is the
  defect. An alias map is a separate improvement.
- **Pagination of the new section**, and **moving the RAWG key server-side** (it ships in the APK
  today at `WishPlayApiService.kt:42`; pre-existing, tracked separately).
- `AlertsRepository.seedInitialRecentAlertsIfEmpty` (`AlertsRepository.kt:85`) seeds three invented
  games but **has no callers**. Dead code, left alone.

## Approaches considered

1. **Second section, always** — run an unfiltered RAWG query in parallel with the filtered one and
   render what the filter excluded under its own heading. The filter chip keeps meaning "only this
   platform" for the main list, and nothing is silently hidden. Costs one extra RAWG call per
   first-page search.
2. **Filter as a ranking boost** — drop the hard `platforms=` exclude, sort matching ports to the
   top. Half the API cost, but the chip stops meaning what it says and every list gets noisier.
3. **On-demand escape hatch** — keep the hard filter, offer a "Search all platforms" button that
   fires the second query only when tapped. Zero default cost, but relies on the user noticing a
   button at the bottom of a list that looks complete.

**Chosen: 1.** The failure is that results are missing without any signal; an escape hatch the user
must discover reproduces that. Approach 2 makes the filter dishonest.

## Design

### Data flow

1. `WishPlayApiServiceImpl.searchGamesPage` gains a fourth parallel `async` beside the existing
   RAWG / Steam / Apple passes, fired only when `platform != "All"`, `page == 1`, and the query is
   at least 2 characters — the same guard the store passes already use. It calls
   `searchRawgPage(query, "All", 1)`.
2. Candidates for the section = unfiltered results, minus any id already in the merged primary
   list, minus anything whose own `platforms` map already contains the selected platform (if RAWG's
   filter and the port list disagree, the port list wins). Capped at the first 10, RAWG's relevance
   order preserved.
3. `GameSearchPage` gains `otherPlatformGames: List<GameDto> = emptyList()`. Defaulted, so the
   interface default in `WishPlayApiService` and existing call sites keep compiling.
4. `SearchRepository.searchGamesPage` currently returns `Triple<List<Game>, Boolean, List<String>>`,
   which is full. Replace with `data class SearchPageResult(games, hasMore, warnings, otherPlatformGames)`.
5. `SearchUiState` gains `otherPlatformResults: List<Game> = emptyList()`, cleared on query change,
   platform change and retry, exactly where `_results` is cleared today.

### UI

`SearchScreen` renders the section after the primary `items(uiState.results)` block and before the
error / load-more item, only when `otherPlatformResults` is non-empty:

```
On other platforms
Not listed on PC yet — these match your search on other platforms.

  Grand Theft Auto VI      Xbox Series S/X, PS5
```

Rows reuse `SearchResultRow`, which already prints the game's full `platforms.keys`
(`SearchScreen.kt:517`). They pass `selectedPlatform = "All"` so `forPlatform()` shows the game's
own nearest release date rather than a date for a platform it does not ship on.

Rows stay fully interactive — tappable through to details, addable to the wishlist — since the
whole point is to let someone track a game whose port for their platform has not been announced.

### Failure handling

The unfiltered pass goes through the existing `attempt { }` wrapper. On failure the section is
simply absent; no warning is added, because the primary results are unaffected. Existing source
warnings for RAWG / Steam / App Store are untouched.

### API cost

One additional RAWG call per first-page search with a platform filter active. Search is already
debounced at 300 ms (`SearchViewModel.kt:106`) and load-more does not re-fire it. Free-tier budget
is 20,000 requests/month.

## Assumptions (stated rather than grilled)

- With `All` selected there is no filter, so no section.
- `page > 1` never fetches the section; load-more stays bound to the primary list.
- Section rows dedupe against the primary list by `id`.

## Test plan

Unit (`app/src/test`, `./gradlew testReleaseUnitTest`):

- A filtered page lacking a title plus an unfiltered page containing it yields that title in
  `otherPlatformGames`.
- Ids present in the primary list are excluded from the section.
- A candidate whose `platforms` already contains the selected platform is excluded.
- The section caps at 10 and preserves RAWG's order.
- `platform == "All"` and `page > 1` both yield an empty section.
- An unfiltered pass that throws leaves primary results and warnings unchanged.

Instrumented (`app/src/androidTest`, `./gradlew connectedDebugAndroidTest`):

- With `otherPlatformResults` populated, the `On other platforms` heading and its rows render.
- With it empty, no heading renders.

Manual, on the emulator: search `GTA VI` with the PC chip selected and confirm
`Grand Theft Auto VI` appears under the section labelled with its real platforms.

## Release impact

Code change, so the 1.1.0 APK in `distribution/` and its checksum are superseded. `versionCode 3` /
`versionName 1.1.0` stay as they are: 1.1.0 was never published anywhere — the updater manifest URL
returns HTTP 404 and the download page pointed at a repository that does not exist — so there is no
installed build for a bumped code to distinguish itself from. After the build:
rerun both suites, rebuild the release APK, refresh `distribution/WishPlay-1.1.0-Jinatra.sha256`,
`distribution/update_manifest.json` and `Docs/Release_Verification.md`, then publish the
`jinatra/wishplay-release` repo so `UpdateManager.kt:43` stops returning HTTP 404.
