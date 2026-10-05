package com.nuvetrix.wishplay.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvetrix.wishplay.data.local.prefs.UserPreferences
import com.nuvetrix.wishplay.data.repository.WishlistRepository
import com.nuvetrix.wishplay.domain.model.FilterOption
import com.nuvetrix.wishplay.domain.model.Game
import com.nuvetrix.wishplay.domain.model.ReleaseStatus
import com.nuvetrix.wishplay.domain.model.SortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class WishlistUiState(
    val games: List<Game> = emptyList(),
    val nextDropGame: Game? = null,
    val selectedFilter: FilterOption = FilterOption.ALL,
    val selectedSort: SortOption = SortOption.NEAREST_RELEASE,
    val isPro: Boolean = false,
    val isFabExpanded: Boolean = false,
    val showSortSheet: Boolean = false,
    val showCapSheet: Boolean = false,
    val showNotifPrimingSheet: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val wishlistRepository: WishlistRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _filter = MutableStateFlow(FilterOption.ALL)
    private val _sort = MutableStateFlow(SortOption.NEAREST_RELEASE)
    private val _fabExpanded = MutableStateFlow(false)
    private val _showSortSheet = MutableStateFlow(false)
    private val _showCapSheet = MutableStateFlow(false)
    private val _showNotifPrimingSheet = MutableStateFlow(false)
    private val _snackbar = MutableStateFlow<String?>(null)

    val uiState: StateFlow<WishlistUiState> = combine(
        wishlistRepository.wishlistGames,
        userPreferences.userRole,
        _filter,
        _sort,
        _fabExpanded,
        _showSortSheet,
        _showCapSheet,
        _showNotifPrimingSheet,
        _snackbar
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val games = args[0] as List<Game>
        val role = args[1] as String
        val filter = args[2] as FilterOption
        val sort = args[3] as SortOption
        val fab = args[4] as Boolean
        val sortSheet = args[5] as Boolean
        val capSheet = args[6] as Boolean
        val notifSheet = args[7] as Boolean
        val snack = args[8] as String?

        val isPro = role == "pro" || role == "admin"

        val nextDrop = games
            .filter { it.status() == ReleaseStatus.UPCOMING && it.alertEnabled }
            .minByOrNull { it.earliestDate ?: "9999-99-99" }

        WishlistUiState(
            games = games,
            nextDropGame = nextDrop,
            selectedFilter = filter,
            selectedSort = sort,
            isPro = isPro,
            isFabExpanded = fab,
            showSortSheet = sortSheet,
            showCapSheet = capSheet,
            showNotifPrimingSheet = notifSheet,
            snackbarMessage = snack
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WishlistUiState()
    )

    init {
        viewModelScope.launch {
            wishlistRepository.seedInitialDataIfEmpty()
        }
    }

    fun setFilter(filter: FilterOption) {
        _filter.value = filter
    }

    fun setSort(sort: SortOption) {
        _sort.value = sort
        _showSortSheet.value = false
    }

    fun toggleFab() {
        _fabExpanded.update { !it }
    }

    fun closeFab() {
        _fabExpanded.value = false
    }

    fun showSortSheet() {
        _showSortSheet.value = true
    }

    fun hideSortSheet() {
        _showSortSheet.value = false
    }

    fun hideCapSheet() {
        _showCapSheet.value = false
    }

    fun hideNotifSheet() {
        _showNotifPrimingSheet.value = false
    }

    fun clearSnackbar() {
        _snackbar.value = null
    }

    fun onAllowNotifications() {
        viewModelScope.launch {
            userPreferences.setNotificationsAsked(true)
            userPreferences.setNotificationsEnabled(true)
            _showNotifPrimingSheet.value = false
            _snackbar.value = "Notifications on. You'll hear about releases, delays and reminders."
        }
    }

    fun onDenyNotifications() {
        viewModelScope.launch {
            userPreferences.setNotificationsAsked(true)
            userPreferences.setNotificationsEnabled(false)
            _showNotifPrimingSheet.value = false
            _snackbar.value = "No notifications for now. Turn them on any time in Profile."
        }
    }

    fun removeGame(gameId: String) {
        viewModelScope.launch {
            wishlistRepository.removeGameFromWishlist(gameId)
            _snackbar.value = "Removed from wishlist"
        }
    }
}
