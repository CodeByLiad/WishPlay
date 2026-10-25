# Search Platform Coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-dev (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop the search platform filter from silently hiding games RAWG has but has not yet listed on that platform, by surfacing them in a labelled "On other platforms" section.

**Architecture:** `WishPlayApiServiceImpl.searchGamesPage` already fans out to RAWG, Steam and the App Store with `async` + an `attempt { }` wrapper. Add a fourth parallel pass that repeats the RAWG query with no platform filter, then select from it with a pure function that excludes anything already shown and anything that already claims the selected platform. The result travels through a new `GameSearchPage.otherPlatformGames` field to a new `SearchUiState.otherPlatformResults`, which `SearchScreen` renders as a second section below the primary results.

**Tech Stack:** Kotlin, Jetpack Compose Material 3 Expressive, Hilt, OkHttp + org.json, Coroutines, JUnit4, Compose UI test.

## Global Constraints

- Package root is `com.jinatra.wishplay`. Layering: `data/{local,remote,repository}`, `domain/model`, `ui/<feature>/{Screen,ViewModel}`.
- No invented catalog data in user-facing flows. Fictional games, prices and dates are defects.
- Platform keys are the exact strings `"All"`, `"PC"`, `"PS5"`, `"PS4"`, `"Xbox Series"`, `"Switch"`, `"Switch 2"`, `"Android"`, `"iOS"`.
- The section cap is **10** and it never paginates.
- `selectedPlatform == "All"` means no filter, so no section.
- Unit tests run `./gradlew testReleaseUnitTest`; instrumented tests run `./gradlew connectedDebugAndroidTest` against emulator `lockout_qa`. The emulator must have no `com.jinatra.wishplay` release build installed, or the debug APK fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
- `versionCode 3` / `versionName 1.1.0` stay unchanged.

### Deviation from the spec's test plan

The spec listed a Compose test asserting the section renders. The existing Compose tests
(`AppUiRegressionTest`) drive the real app against the live RAWG API, so such an assertion would
depend on RAWG's current port list for a specific game and would start failing the day a PC port is
added. Task 2 covers the behaviour deterministically with the `OkHttpClient` interceptor fixture
pattern already used by `MultiSourceCatalogTest`, and Task 4 is verified by the manual emulator pass
the spec also requires. No Compose test is added.

---

### Task 1: Cross-platform selection rule

**Files:**
- Create: `app/src/main/java/com/jinatra/wishplay/data/remote/api/PlatformCoverage.kt`
- Test: `app/src/test/java/com/jinatra/wishplay/PlatformCoverageTest.kt`

**Interfaces:**
- Consumes: `com.jinatra.wishplay.data.remote.dto.GameDto` — existing, has `id: String`, `title: String`, `platforms: Map<String, String?>`.
- Produces: `fun selectOtherPlatformGames(primary: List<GameDto>, candidates: List<GameDto>, selectedPlatform: String, limit: Int = 10): List<GameDto>` in package `com.jinatra.wishplay.data.remote.api`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/jinatra/wishplay/PlatformCoverageTest.kt`:

```kotlin
package com.jinatra.wishplay

import com.jinatra.wishplay.data.remote.api.selectOtherPlatformGames
import com.jinatra.wishplay.data.remote.dto.GameDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformCoverageTest {

    private fun game(id: String, title: String, vararg platforms: String) = GameDto(
        id = id,
        title = title,
        platforms = platforms.associateWith { null }
    )

    @Test
    fun titleMissingTheSelectedPlatformIsOffered() {
        val primary = listOf(game("rawg_1", "Avernum 6", "PC"))
        val candidates = listOf(game("rawg_2", "Grand Theft Auto VI", "Xbox Series", "PS5"))

        val result = selectOtherPlatformGames(primary, candidates, "PC")

        assertEquals(listOf("rawg_2"), result.map { it.id })
    }

    @Test
    fun titlesAlreadyInThePrimaryListAreExcluded() {
        val shown = game("rawg_1", "Palworld", "PC")
        val result = selectOtherPlatformGames(listOf(shown), listOf(shown), "PC")

        assertTrue(result.isEmpty())
    }

    @Test
    fun candidateThatAlreadyClaimsTheSelectedPlatformIsExcluded() {
        val candidates = listOf(game("rawg_9", "Helldivers 2", "PC", "PS5"))

        val result = selectOtherPlatformGames(emptyList(), candidates, "PC")

        assertTrue(result.isEmpty())
    }

    @Test
    fun resultIsCappedAtTenAndKeepsCandidateOrder() {
        val candidates = (1..15).map { game("rawg_$it", "Game $it", "PS5") }

        val result = selectOtherPlatformGames(emptyList(), candidates, "PC")

        assertEquals(10, result.size)
        assertEquals("rawg_1", result.first().id)
        assertEquals("rawg_10", result.last().id)
    }

    @Test
    fun duplicateCandidateIdsAreCollapsed() {
        val candidate = game("rawg_3", "Wuthering Waves", "PS5")

        val result = selectOtherPlatformGames(emptyList(), listOf(candidate, candidate), "PC")

        assertEquals(1, result.size)
    }

    @Test
    fun allPlatformMeansNoSection() {
        val candidates = listOf(game("rawg_2", "Grand Theft Auto VI", "Xbox Series", "PS5"))

        assertTrue(selectOtherPlatformGames(emptyList(), candidates, "All").isEmpty())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testReleaseUnitTest --tests "com.jinatra.wishplay.PlatformCoverageTest"`
Expected: FAIL — compilation error, `Unresolved reference: selectOtherPlatformGames`.

- [ ] **Step 3: Write minimal implementation**

Create `app/src/main/java/com/jinatra/wishplay/data/remote/api/PlatformCoverage.kt`:

```kotlin
package com.jinatra.wishplay.data.remote.api

import com.jinatra.wishplay.data.remote.dto.GameDto

/**
 * Picks the search hits a platform filter excluded.
 *
 * RAWG's `platforms=` filter drops every title whose port list does not already name that
 * platform, so an unannounced port makes a game invisible rather than merely unavailable.
 * These are shown in their own section, never mixed into the filtered results.
 */
fun selectOtherPlatformGames(
    primary: List<GameDto>,
    candidates: List<GameDto>,
    selectedPlatform: String,
    limit: Int = 10
): List<GameDto> {
    if (selectedPlatform == "All" || limit <= 0) return emptyList()
    val alreadyShown = primary.map { it.id }.toSet()
    return candidates.asSequence()
        .filter { it.id !in alreadyShown }
        .filter { selectedPlatform !in it.platforms.keys }
        .distinctBy { it.id }
        .take(limit)
        .toList()
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testReleaseUnitTest --tests "com.jinatra.wishplay.PlatformCoverageTest"`
Expected: PASS, 6 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/data/remote/api/PlatformCoverage.kt app/src/test/java/com/jinatra/wishplay/PlatformCoverageTest.kt
git commit -m "feat: add cross-platform search selection rule"
```

---

### Task 2: Unfiltered RAWG pass in the API service

**Files:**
- Modify: `app/src/main/java/com/jinatra/wishplay/data/remote/dto/GameDtos.kt:6`
- Modify: `app/src/main/java/com/jinatra/wishplay/data/remote/api/WishPlayApiService.kt:48-70`
- Test: `app/src/androidTest/java/com/jinatra/wishplay/MultiSourceCatalogTest.kt`

**Interfaces:**
- Consumes: `selectOtherPlatformGames(...)` from Task 1.
- Produces: `GameSearchPage(games, hasMore, warnings, otherPlatformGames)` — `otherPlatformGames: List<GameDto>` defaults to `emptyList()`, so the `WishPlayApiService` interface default and every existing call site keep compiling.

- [ ] **Step 1: Write the failing test**

Add to `app/src/androidTest/java/com/jinatra/wishplay/MultiSourceCatalogTest.kt`, inside the class, after the existing tests:

```kotlin
    @Test fun platformFilterExclusionsAreOfferedSeparately() = runBlocking {
        val page = WishPlayApiServiceImpl(coverageClient()).searchGamesPage("Ports", "PC", 1)
        assertEquals(listOf("rawg_1"), page.games.map { it.id })
        assertEquals(listOf("rawg_2"), page.otherPlatformGames.map { it.id })
        assertEquals("Console Only", page.otherPlatformGames.single().title)
    }

    @Test fun unfilteredPassFailureLeavesPrimaryResultsIntact() = runBlocking {
        val page = WishPlayApiServiceImpl(coverageClient(unfilteredCode = 503)).searchGamesPage("Ports", "PC", 1)
        assertEquals(listOf("rawg_1"), page.games.map { it.id })
        assertTrue(page.otherPlatformGames.isEmpty())
    }

    private fun coverageClient(unfilteredCode: Int = 200) = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request()
        val pcGame = """{"id":1,"name":"Ports","slug":"ports","platforms":[{"platform":{"slug":"pc","name":"PC"}}]}"""
        val consoleGame = """{"id":2,"name":"Console Only","slug":"console-only","platforms":[{"platform":{"slug":"playstation5","name":"PlayStation 5"}}]}"""
        val filtered = request.url.queryParameter("platforms") != null
        val (code, body) = when {
            request.url.host == "api.rawg.io" && filtered -> 200 to """{"results":[$pcGame],"next":null}"""
            request.url.host == "api.rawg.io" -> unfilteredCode to """{"results":[$pcGame,$consoleGame],"next":null}"""
            else -> 200 to """{"items":[]}"""
        }
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("Fixture")
            .body(body.toResponseBody("application/json".toMediaType())).build()
    }.build()
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew connectedDebugAndroidTest --tests "com.jinatra.wishplay.MultiSourceCatalogTest"`
Expected: FAIL — compilation error, `Unresolved reference: otherPlatformGames`.

If the run instead fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.jinatra.wishplay signatures do not match newer version`, run `adb uninstall com.jinatra.wishplay` first — a release-signed build is installed on the emulator.

- [ ] **Step 3: Add the DTO field**

In `app/src/main/java/com/jinatra/wishplay/data/remote/dto/GameDtos.kt`, replace line 6:

```kotlin
data class GameSearchPage(val games: List<GameDto>, val hasMore: Boolean = false, val warnings: List<String> = emptyList())
```

with:

```kotlin
data class GameSearchPage(
    val games: List<GameDto>,
    val hasMore: Boolean = false,
    val warnings: List<String> = emptyList(),
    val otherPlatformGames: List<GameDto> = emptyList()
)
```

- [ ] **Step 4: Add the unfiltered pass**

In `app/src/main/java/com/jinatra/wishplay/data/remote/api/WishPlayApiService.kt`, in `searchGamesPage`, after the existing `apple` declaration:

```kotlin
        val apple = if (platform in listOf("All", "iOS")) async { attempt { stores.searchApple(query) } } else null
```

add:

```kotlin
        val crossPlatform = if (platform != "All") async { attempt { searchRawgPage(query, "All", page) } } else null
```

Then replace the final `GameSearchPage(...)` expression:

```kotlin
        GameSearchPage((base + unique).distinctBy { it.id }, rawgResult.getOrNull()?.hasMore == true, warnings)
```

with:

```kotlin
        val merged = (base + unique).distinctBy { it.id }
        GameSearchPage(
            games = merged,
            hasMore = rawgResult.getOrNull()?.hasMore == true,
            warnings = warnings,
            otherPlatformGames = selectOtherPlatformGames(
                primary = merged,
                candidates = crossPlatform?.await()?.getOrNull()?.games.orEmpty(),
                selectedPlatform = platform
            )
        )
```

No extra page or query-length guard is needed: the method already returns early at its first line
for `query.trim().length < 2 || page != 1`, so this block only ever runs for page 1 of a real query.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew connectedDebugAndroidTest --tests "com.jinatra.wishplay.MultiSourceCatalogTest"`
Expected: PASS, 5 tests — the 3 existing plus the 2 new ones.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/data/remote/dto/GameDtos.kt app/src/main/java/com/jinatra/wishplay/data/remote/api/WishPlayApiService.kt app/src/androidTest/java/com/jinatra/wishplay/MultiSourceCatalogTest.kt
git commit -m "feat: fetch platform-filter exclusions alongside filtered search"
```

---

### Task 3: Carry the section through repository and view model

**Files:**
- Modify: `app/src/main/java/com/jinatra/wishplay/data/repository/SearchRepository.kt:51-55`
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/search/SearchViewModel.kt`

**Interfaces:**
- Consumes: `GameSearchPage.otherPlatformGames` from Task 2.
- Produces:
  - `data class SearchPageResult(val games: List<Game>, val hasMore: Boolean, val warnings: List<String>, val otherPlatformGames: List<Game>)` in `com.jinatra.wishplay.data.repository`.
  - `SearchRepository.searchGamesPage(query: String, platform: String, page: Int): SearchPageResult` — replaces the `Triple` return.
  - `SearchUiState.otherPlatformResults: List<Game>` — defaults to `emptyList()`.

- [ ] **Step 1: Replace the repository return type**

In `app/src/main/java/com/jinatra/wishplay/data/repository/SearchRepository.kt`, replace:

```kotlin
    suspend fun searchGamesPage(query: String, platform: String, page: Int): Triple<List<Game>, Boolean, List<String>> {
        val response = apiService.searchGamesPage(query, platform, page)
        return Triple(response.games.map { it.toDomainModel() }, response.hasMore, response.warnings)
    }
```

with:

```kotlin
    suspend fun searchGamesPage(query: String, platform: String, page: Int): SearchPageResult {
        val response = apiService.searchGamesPage(query, platform, page)
        return SearchPageResult(
            games = response.games.map { it.toDomainModel() },
            hasMore = response.hasMore,
            warnings = response.warnings,
            otherPlatformGames = response.otherPlatformGames.map { it.toDomainModel() }
        )
    }
```

and add, immediately above the `@Singleton class SearchRepository` declaration:

```kotlin
data class SearchPageResult(
    val games: List<Game>,
    val hasMore: Boolean,
    val warnings: List<String>,
    val otherPlatformGames: List<Game>
)
```

- [ ] **Step 2: Add the state field**

In `app/src/main/java/com/jinatra/wishplay/ui/search/SearchViewModel.kt`, add to `SearchUiState`, after `val results: List<Game> = emptyList(),`:

```kotlin
    val otherPlatformResults: List<Game> = emptyList(),
```

- [ ] **Step 3: Add the backing flow and fold it into the existing combine**

Add beside the other private flows, after `private val _results = MutableStateFlow<List<Game>>(emptyList())`:

```kotlin
    private val _otherResults = MutableStateFlow<List<Game>>(emptyList())
```

Replace the second argument of the outer `combine` — currently:

```kotlin
        combine(_results, wishlistRepository.wishlistGames) { results, wishlist ->
            val ids = wishlist.map { it.id }.toSet()
            val mapped = results.map { it.copy(inWishlist = ids.contains(it.id)) }
            Pair(mapped, ids)
        },
```

with:

```kotlin
        combine(_results, _otherResults, wishlistRepository.wishlistGames) { results, others, wishlist ->
            val ids = wishlist.map { it.id }.toSet()
            Triple(
                results.map { it.copy(inWishlist = ids.contains(it.id)) },
                ids,
                others.map { it.copy(inWishlist = ids.contains(it.id)) }
            )
        },
```

Change the lambda's destructuring from:

```kotlin
    ) { (q, plat, loading), (results, ids), recent, snackbar, metadata ->
```

to:

```kotlin
    ) { (q, plat, loading), (results, ids, otherResults), recent, snackbar, metadata ->
```

and add to the `SearchUiState(...)` construction, after `results = results,`:

```kotlin
            otherPlatformResults = otherResults,
```

- [ ] **Step 4: Populate and clear it**

In `onQueryChanged`, after `_results.value = emptyList()`, add:

```kotlin
        _otherResults.value = emptyList()
```

In `performSearch`, inside `if (page == 1) { ... }`, after `_results.value = emptyList()`, add:

```kotlin
            _otherResults.value = emptyList()
```

Replace the destructuring call and its result handling — currently:

```kotlin
                val (list, more, warnings) = searchRepository.searchGamesPage(q, plat, page)
                if (requestGeneration == generation) {
                    _results.value = (if (page == 1) list else _results.value + list).distinctBy { it.id }
                    loadedPage = page
                    _pageState.value = more to null
                    if (page == 1 || warnings.isNotEmpty()) _sourceNotice.value = warnings.takeIf { it.isNotEmpty() }?.joinToString(" ")
                }
```

with:

```kotlin
                val result = searchRepository.searchGamesPage(q, plat, page)
                if (requestGeneration == generation) {
                    _results.value = (if (page == 1) result.games else _results.value + result.games).distinctBy { it.id }
                    if (page == 1) _otherResults.value = result.otherPlatformGames
                    loadedPage = page
                    _pageState.value = result.hasMore to null
                    if (page == 1 || result.warnings.isNotEmpty()) {
                        _sourceNotice.value = result.warnings.takeIf { it.isNotEmpty() }?.joinToString(" ")
                    }
                }
```

- [ ] **Step 5: Verify it compiles and nothing regressed**

Run: `./gradlew testReleaseUnitTest`
Expected: PASS, 42 tests (36 existing plus Task 1's 6).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/data/repository/SearchRepository.kt app/src/main/java/com/jinatra/wishplay/ui/search/SearchViewModel.kt
git commit -m "feat: expose platform-filter exclusions in search state"
```

---

### Task 4: Render the "On other platforms" section

**Files:**
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/search/SearchScreen.kt:353-397`

**Interfaces:**
- Consumes: `SearchUiState.otherPlatformResults` from Task 3; existing `SearchResultRow(game, selectedPlatform, isUpdating, onClick, onToggleWishlist)` and the existing `platformShortNames` map in the same file.
- Produces: no new interface.

- [ ] **Step 1: Keep the empty state from firing when the section has content**

In the empty-state condition, replace:

```kotlin
                if (!uiState.isLoading && uiState.error == null && uiState.results.isEmpty()) {
```

with:

```kotlin
                if (!uiState.isLoading && uiState.error == null && uiState.results.isEmpty() &&
                    uiState.otherPlatformResults.isEmpty()) {
```

Without this, a query whose only hits are on other platforms renders "No match" directly above a
list of matches.

- [ ] **Step 2: Add the section**

Immediately after the primary results block:

```kotlin
                items(uiState.results, key = { it.id }) { game ->
                    SearchResultRow(
                        game = game,
                        selectedPlatform = uiState.selectedPlatform,
                        isUpdating = game.id in uiState.busyGameIds,
                        onClick = { viewModel.openDetails(game, onNavigateToDetails) },
                        onToggleWishlist = { viewModel.toggleWishlist(game) }
                    )
                }
```

add:

```kotlin
                if (uiState.otherPlatformResults.isNotEmpty()) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = "On other platforms",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val label = platformShortNames[uiState.selectedPlatform] ?: uiState.selectedPlatform
                            Text(
                                text = "Not listed on $label yet — these match your search on other platforms.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(uiState.otherPlatformResults, key = { "other_${it.id}" }) { game ->
                        SearchResultRow(
                            game = game,
                            selectedPlatform = "All",
                            isUpdating = game.id in uiState.busyGameIds,
                            onClick = { viewModel.openDetails(game, onNavigateToDetails) },
                            onToggleWishlist = { viewModel.toggleWishlist(game) }
                        )
                    }
                }
```

`selectedPlatform = "All"` is deliberate: `SearchResultRow` passes it to `game.forPlatform(...)`
for the date line, and these games have no release on the selected platform. The row's platform
text already prints the game's full `platforms.keys` (`SearchScreen.kt:517`), so each row states
its real ports. The `"other_"` key prefix keeps `LazyColumn` keys unique if a game somehow appears
in both lists.

- [ ] **Step 3: Verify on the emulator**

```bash
./gradlew installDebug
adb shell am start -n com.jinatra.wishplay/.ui.MainActivity
```

In the app: Search tab → select the `PC` chip → type `GTA VI`.
Expected: `Grand Theft Auto VI` appears under `On other platforms`, its row reading
`Xbox Series, PS5`, above the subtitle `Not seeing it` never appearing. Tapping the row opens
details; the `+` button adds it to the wishlist.

- [ ] **Step 4: Confirm the suites still pass**

Run: `./gradlew testReleaseUnitTest connectedDebugAndroidTest`
Expected: PASS — 42 unit, 11 instrumented.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/ui/search/SearchScreen.kt
git commit -m "feat: show games the platform filter excluded in their own section"
```

---

### Task 5: Copy defects

**Files:**
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/wishlist/WishlistScreen.kt:436`
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/welcome/WelcomeScreen.kt:408-418`

**Interfaces:**
- Consumes: nothing from earlier tasks. This task is independent and may be done first.
- Produces: no new interface.

- [ ] **Step 1: Fix the plural**

In `app/src/main/java/com/jinatra/wishplay/ui/wishlist/WishlistScreen.kt`, replace line 436:

```kotlin
                text = "$totalCount games you're waiting on",
```

with:

```kotlin
                text = if (totalCount == 1) "1 game you're waiting on" else "$totalCount games you're waiting on",
```

- [ ] **Step 2: Remove the invented game from onboarding**

In `app/src/main/java/com/jinatra/wishplay/ui/welcome/WelcomeScreen.kt`, replace:

```kotlin
                        Text(
                            text = "Starfall Odyssey drops in $selectedLead",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Nov 13 on PC, PS5 and Xbox",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
```

with:

```kotlin
                        Text(
                            text = "A game you're tracking drops in $selectedLead",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Plus another reminder on release day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
```

`Starfall Odyssey`, its `Nov 13` date and its platform list were all invented. The replacement
previews the same notification shape using only what the app actually knows at that point in
onboarding: the lead time the user just picked, and that a release-day alert always fires
(the screen's own subtitle already states "You always get an alert on release day").

- [ ] **Step 3: Verify**

Run: `./gradlew testReleaseUnitTest`
Expected: PASS.

Then on the emulator, clear data and walk onboarding to step 3 — the preview card reads
`A game you're tracking drops in 1 week`. Add one game to the wishlist and confirm the header
reads `1 game you're waiting on`.

```bash
adb shell pm clear com.jinatra.wishplay
adb shell am start -n com.jinatra.wishplay/.ui.MainActivity
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/ui/wishlist/WishlistScreen.kt app/src/main/java/com/jinatra/wishplay/ui/welcome/WelcomeScreen.kt
git commit -m "fix: correct wishlist plural and drop invented game from onboarding"
```

---

### Task 6: Refresh the release artifacts

**Files:**
- Modify: `distribution/WishPlay-1.1.0-Jinatra.sha256`
- Modify: `distribution/update_manifest.json`
- Modify: `Docs/Release_Verification.md:7-10`

**Interfaces:**
- Consumes: the finished build from Tasks 1-5.
- Produces: no new interface.

- [ ] **Step 1: Rebuild the release APK**

```bash
./gradlew assembleRelease
cp app/build/outputs/apk/release/app-release.apk distribution/WishPlay-1.1.0-Jinatra.apk
```

- [ ] **Step 2: Run the boot assertion from `.spine/config.md`**

```bash
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --print-certs distribution/WishPlay-1.1.0-Jinatra.apk
"$ANDROID_HOME/build-tools/36.0.0/aapt2" dump badging distribution/WishPlay-1.1.0-Jinatra.apk | head -1
```

Expected: certificate SHA-256 digest `bfacf78547d7040ba04fc6dc7483863b4a22891f6e6c4547a0e8412685f450a8`,
matching `AppSecurityManager.OFFICIAL_RELEASE_SHA256`, and `package: name='com.jinatra.wishplay'
versionCode='3' versionName='1.1.0'`. Any other value means the build used the wrong keystore or
package — stop and report rather than publishing.

- [ ] **Step 3: Record the new checksum and size**

```bash
sha256sum distribution/WishPlay-1.1.0-Jinatra.apk
wc -c < distribution/WishPlay-1.1.0-Jinatra.apk
```

Write the digest into `distribution/WishPlay-1.1.0-Jinatra.sha256` in the existing
`<digest>  WishPlay-1.1.0-Jinatra.apk` format, into the `"sha256"` field of
`distribution/update_manifest.json`, and into the `**SHA-256 Checksum:**` line of
`Docs/Release_Verification.md`. Update that file's `**Size:**` line with the byte count.

- [ ] **Step 4: Commit**

```bash
git add distribution/WishPlay-1.1.0-Jinatra.sha256 distribution/update_manifest.json Docs/Release_Verification.md
git commit -m "chore: refresh 1.1.0 release checksums after search coverage fix"
```

---

## Follow-up, not in this plan

`UpdateManager.kt:43` reads `https://raw.githubusercontent.com/jinatra/wishplay-release/main/update_manifest.json`,
which returns HTTP 404 — the repository does not exist. Until it is created with the manifest on
`main` and a `v1.1.0` release holding the APK, no installed build can discover an update. That is a
publishing step, not a code change.
