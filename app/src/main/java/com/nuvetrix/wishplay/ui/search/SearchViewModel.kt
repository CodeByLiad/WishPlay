package com.nuvetrix.wishplay.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvetrix.wishplay.data.local.prefs.UserPreferences
import com.nuvetrix.wishplay.data.repository.SearchRepository
import com.nuvetrix.wishplay.data.repository.WishlistRepository
import com.nuvetrix.wishplay.domain.model.Game
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
import kotlinx.coroutines.launch
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
    val snackbarMessage: String? = null
)

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
        _snackbar
    ) { (q, plat, loading), (results, ids), recent, snackbar ->
        SearchUiState(
            query = q,
            selectedPlatform = plat,
            results = results,
            recentSearches = recent,
            popularQueries = popularQueries,
            wishlistedIds = ids,
            isLoading = loading,
            isPro = false,
            snackbarMessage = snackbar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState(popularQueries = popularQueries)
    )

    init {
        // Initial search to populate upcoming games before typing
        performSearch("", "All")

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
        _query.value = newQuery
    }

    fun onClearQuery() {
        _query.value = ""
        performSearch("", _selectedPlatform.value)
    }

    fun onPlatformSelected(platform: String) {
        _selectedPlatform.value = platform
        performSearch(_query.value, platform)
    }

    fun onSelectRecent(recent: String) {
        _query.value = recent
        searchRepository.popularQueries // no-op touch
        viewModelScope.launch {
            searchRepository.addRecentSearch(recent)
        }
        performSearch(recent, _selectedPlatform.value)
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
        viewModelScope.launch {
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
        }
    }

    fun clearSnackbar() {
        _snackbar.value = null
    }

    private fun performSearch(q: String, plat: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = searchRepository.searchGames(q, plat)
                _results.value = list
            } catch (_: Exception) {
                _results.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
