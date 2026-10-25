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

private data class SearchMetadata(val hasMore: Boolean, val error: String?, val isPro: Boolean, val busyIds: Set<String>, val sourceNotice: String?)

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
    private val _snackbar = MutableStateFlow<String?>(null)
    private val _pageState = MutableStateFlow(Pair(false, null as String?))
    private var searchJob: Job? = null
    private var loadedPage = 0
    private var generation = 0
    private val _isPro = userPreferences.userRole
    private val _busyGames = MutableStateFlow<Set<String>>(emptySet())
    private val _sourceNotice = MutableStateFlow<String?>(null)
    private val _metadata = combine(_pageState, _isPro, _busyGames, _sourceNotice) { page, role, busy, notice ->
        SearchMetadata(page.first, page.second, role == "pro" || role == "admin", busy, notice)
    }

    val popularQueries = searchRepository.popularQueries

    val uiState: StateFlow<SearchUiState> = combine(
        combine(_query, _selectedPlatform, _isLoading) { q, plat, loading ->
            Triple(q, plat, loading)
        },
        combine(_results, wishlistRepository.wishlistGames) { results, wishlist ->
            val ids = wishlist.map { it.id }.toSet()
            val mapped = results.map { it.copy(inWishlist = ids.contains(it.id)) }
            Pair(mapped, ids)
        },
        searchRepository.recentSearches,
        _snackbar,
        _metadata
    ) { (q, plat, loading), (results, ids), recent, snackbar, metadata ->
        SearchUiState(
            query = q,
            selectedPlatform = plat,
            results = results,
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
        _results.value = emptyList()
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

    private fun performSearch(q: String, plat: String, page: Int = 1) {
        searchJob?.cancel()
        val requestGeneration = ++generation
        if (page == 1) {
            loadedPage = 0
            _results.value = emptyList()
            _sourceNotice.value = null
        }
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _pageState.value = _pageState.value.first to null
            try {
                val (list, more, warnings) = searchRepository.searchGamesPage(q, plat, page)
                if (requestGeneration == generation) {
                    _results.value = (if (page == 1) list else _results.value + list).distinctBy { it.id }
                    loadedPage = page
                    _pageState.value = more to null
                    if (page == 1 || warnings.isNotEmpty()) _sourceNotice.value = warnings.takeIf { it.isNotEmpty() }?.joinToString(" ")
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
