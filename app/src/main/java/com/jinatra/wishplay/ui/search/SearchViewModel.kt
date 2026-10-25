package com.jinatra.wishplay.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinatra.wishplay.data.local.prefs.UserPreferences
import com.jinatra.wishplay.data.repository.SearchRepository
import com.jinatra.wishplay.data.repository.WishlistRepository
import com.jinatra.wishplay.domain.model.Game
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val selectedPlatform: String = "All",
    val results: List<Game> = emptyList(),
    val otherPlatformResults: List<Game> = emptyList(),
    val isOtherPlatformsLoading: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val popularQueries: List<String> = emptyList(),
    val wishlistedIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isPro: Boolean = false,
    val snackbarMessage: String? = null,
    val hasMore: Boolean = false,
    val error: String? = null,
    val busyGameIds: Set<String> = emptySet(),
    val sourceNotice: String? = null
)

private data class SearchMetadata(val hasMore: Boolean, val error: String?, val isPro: Boolean, val busyIds: Set<String>, val sourceNotice: String?, val otherLoading: Boolean)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val wishlistRepository: WishlistRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _selectedPlatform = MutableStateFlow("All")
    private val _isLoading = MutableStateFlow(false)
    private val _results = MutableStateFlow<List<Game>>(emptyList())
    private val _otherResults = MutableStateFlow<List<Game>>(emptyList())
    private val _otherLoading = MutableStateFlow(false)
    private val _snackbar = MutableStateFlow<String?>(null)
    private val _pageState = MutableStateFlow(Pair(false, null as String?))
    private var searchJob: Job? = null
    private var otherJob: Job? = null
    private var loadedPage = 0
    private var generation = 0
    private val _isPro = userPreferences.userRole
    private val _busyGames = MutableStateFlow<Set<String>>(emptySet())
    private val _sourceNotice = MutableStateFlow<String?>(null)
    private val _metadata = combine(_pageState, _isPro, _busyGames, _sourceNotice, _otherLoading) { page, role, busy, notice, otherLoading ->
        SearchMetadata(page.first, page.second, role == "pro" || role == "admin", busy, notice, otherLoading)
    }

    val popularQueries = searchRepository.popularQueries

    val uiState: StateFlow<SearchUiState> = combine(
        combine(_query, _selectedPlatform, _isLoading) { q, plat, loading ->
            Triple(q, plat, loading)
        },
        combine(_results, _otherResults, wishlistRepository.wishlistGames) { results, others, wishlist ->
            val ids = wishlist.map { it.id }.toSet()
            Triple(
                results.map { it.copy(inWishlist = ids.contains(it.id)) },
                ids,
                others.map { it.copy(inWishlist = ids.contains(it.id)) }
            )
        },
        searchRepository.recentSearches,
        _snackbar,
        _metadata
    ) { (q, plat, loading), (results, ids, otherResults), recent, snackbar, metadata ->
        SearchUiState(
            query = q,
            selectedPlatform = plat,
            results = results,
            otherPlatformResults = otherResults,
            isOtherPlatformsLoading = metadata.otherLoading,
            recentSearches = recent,
            popularQueries = popularQueries,
            wishlistedIds = ids,
            isLoading = loading,
            isPro = metadata.isPro,
            snackbarMessage = snackbar,
            hasMore = metadata.hasMore,
            error = metadata.error,
            busyGameIds = metadata.busyIds,
            sourceNotice = metadata.sourceNotice
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState(popularQueries = popularQueries)
    )

    init {
        viewModelScope.launch {
            _query
                .debounce(300)
                .distinctUntilChanged()
                .collect { q ->
                    performSearch(q, _selectedPlatform.value)
                }
        }
    }

    fun onQueryChanged(newQuery: String) {
        if (_query.value == newQuery) {
            performSearch(newQuery, _selectedPlatform.value)
            return
        }
        generation++
        searchJob?.cancel()
        otherJob?.cancel()
        _results.value = emptyList()
        _otherResults.value = emptyList()
        _otherLoading.value = false
        _pageState.value = false to null
        _isLoading.value = true
        _query.value = newQuery
    }

    fun onClearQuery() {
        onQueryChanged("")
    }

    fun onPlatformSelected(platform: String) {
        _selectedPlatform.value = platform
        performSearch(_query.value, platform)
    }

    fun onSelectRecent(recent: String) {
        onQueryChanged(recent)
        viewModelScope.launch {
            searchRepository.addRecentSearch(recent)
        }
    }

    fun onRemoveRecent(recent: String) {
        viewModelScope.launch {
            searchRepository.removeRecentSearch(recent)
        }
    }

    fun onClearRecent() {
        viewModelScope.launch {
            searchRepository.clearRecentSearches()
        }
    }

    fun onSubmitSearch() {
        val q = _query.value.trim()
        if (q.isNotBlank()) {
            viewModelScope.launch {
                searchRepository.addRecentSearch(q)
            }
        }
    }

    fun toggleWishlist(game: Game) {
        if (game.id in _busyGames.value) return
        _busyGames.update { it + game.id }
        viewModelScope.launch {
            try {
            val isCurrentlyIn = uiState.value.wishlistedIds.contains(game.id)
            if (isCurrentlyIn) {
                wishlistRepository.removeGameFromWishlist(game.id)
                _snackbar.value = "Removed ${game.title} from wishlist"
            } else {
                val result = searchRepository.addGameToWishlist(game, isPro = uiState.value.isPro)
                result.onSuccess {
                    _snackbar.value = "Added ${game.title} to wishlist"
                }.onFailure { error ->
                    if (error.message == "CAP_REACHED") {
                        _snackbar.value = "Wishlist full (25 games). Upgrade to Pro for unlimited."
                    } else {
                        _snackbar.value = "Could not add game"
                    }
                }
            }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _snackbar.value = "Couldn't update the wishlist. Try again."
            } finally { _busyGames.update { it - game.id } }
        }
    }

    fun openDetails(game: Game, navigate: (String) -> Unit) {
        viewModelScope.launch {
            searchRepository.cacheSummary(game)
            navigate(game.id)
        }
    }

    fun clearSnackbar() {
        _snackbar.value = null
    }

    fun loadMore() {
        if (!_isLoading.value && _pageState.value.first) performSearch(_query.value, _selectedPlatform.value, loadedPage + 1)
    }

    fun retry() = performSearch(_query.value, _selectedPlatform.value, if (_results.value.isEmpty()) 1 else loadedPage + 1)

    /**
     * Fills the "On other platforms" section after the primary results are already on screen.
     *
     * Runs in its own coroutine so it cannot delay them: the catalog answers a warm query in well
     * under a second but a cold one has been measured at 22s, and this is only ever supplementary.
     * Failure is silent — the section stays absent and the primary results are untouched.
     */
    private fun loadOtherPlatforms(q: String, plat: String, shown: List<Game>, requestGeneration: Int) {
        otherJob?.cancel()
        _otherLoading.value = true
        otherJob = viewModelScope.launch {
            val others = try {
                searchRepository.searchOtherPlatforms(q, plat, shown)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
            if (requestGeneration == generation) {
                _otherResults.value = others
                _otherLoading.value = false
            }
        }
    }

    private fun performSearch(q: String, plat: String, page: Int = 1) {
        val requestGeneration = ++generation
        searchJob?.cancel()
        if (page == 1) {
            // Only a new search invalidates the section. A load-more leaves any in-flight fetch
            // alone: cancelling it there would abort it permanently, since it is re-issued only
            // for page 1 and nothing would tell the user the section had been dropped.
            otherJob?.cancel()
            loadedPage = 0
            _results.value = emptyList()
            _otherResults.value = emptyList()
            _otherLoading.value = false
            _sourceNotice.value = null
        }
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _pageState.value = _pageState.value.first to null
            try {
                val result = searchRepository.searchGamesPage(q, plat, page)
                if (requestGeneration == generation) {
                    val merged = (if (page == 1) result.games else _results.value + result.games).distinctBy { it.id }
                    _results.value = merged
                    loadedPage = page
                    _pageState.value = result.hasMore to null
                    if (page == 1 || result.warnings.isNotEmpty()) {
                        _sourceNotice.value = result.warnings.takeIf { it.isNotEmpty() }?.joinToString(" ")
                    }
                    if (page == 1) loadOtherPlatforms(q, plat, merged, requestGeneration)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (requestGeneration == generation) _pageState.value = _pageState.value.first to
                    "Couldn't load the game catalog. Check your connection and try again."
            } finally {
                if (requestGeneration == generation) _isLoading.value = false
            }
        }
    }
}
