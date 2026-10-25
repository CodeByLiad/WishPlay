package com.jinatra.wishplay.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinatra.wishplay.data.remote.api.WishPlayApiService
import com.jinatra.wishplay.data.repository.WishlistRepository
import com.jinatra.wishplay.data.repository.toDomainModel
import com.jinatra.wishplay.domain.model.Game
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

data class GameDetailsUiState(
    val game: Game? = null,
    val selectedPlatform: String? = null,
    val reqMode: String = "min", // "min" or "rec"
    val isPro: Boolean = false,
    val snackbarMessage: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class GameDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wishlistRepository: WishlistRepository,
    private val apiService: WishPlayApiService
) : ViewModel() {

    private val gameId: String = checkNotNull(savedStateHandle["gameId"])

    private val _selectedPlatform = MutableStateFlow<String?>(null)
    private val _reqMode = MutableStateFlow("min")
    private val _remoteGame = MutableStateFlow<Game?>(null)
    private val _snackbar = MutableStateFlow<String?>(null)
    private val _loadState = MutableStateFlow(true to (null as String?))

    val uiState: StateFlow<GameDetailsUiState> = combine(
        wishlistRepository.observeGameById(gameId),
        _remoteGame,
        _selectedPlatform,
        _reqMode,
        _snackbar
    ) { localGame, remoteGame, selectedPlatform, reqMode, snackbar ->
        val game = localGame ?: remoteGame
        val effectivePlat = selectedPlatform ?: game?.trackedPlatforms?.firstOrNull { it in game.platforms }
            ?: game?.platforms?.keys?.firstOrNull()
        GameDetailsUiState(
            game = game,
            selectedPlatform = effectivePlat,
            reqMode = reqMode,
            isPro = false,
            snackbarMessage = snackbar
        )
    }.combine(_loadState) { state, load -> state.copy(isLoading = load.first, error = load.second) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GameDetailsUiState()
    )

    init {
        refresh()
    }

    fun refresh() {
        // If not in local database or needs fresh details, fetch from API
        viewModelScope.launch {
            _loadState.value = true to null
            try {
                if (wishlistRepository.observeGameById(gameId).first()?.isCustom == true) {
                    _loadState.value = false to null
                    return@launch
                }
                val dto = apiService.getGameDetails(gameId)
                if (dto != null) {
                    val domainGame = dto.toDomainModel()
                    _remoteGame.value = domainGame
                    // Save to local cache so details are available offline
                    wishlistRepository.saveGame(domainGame)
                }
                _loadState.value = false to if (dto == null) "Game details are unavailable." else null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _loadState.value = false to "Couldn't refresh this game. Check your connection and try again."
            }
        }
    }

    fun selectPlatform(platform: String) {
        _selectedPlatform.value = platform
    }

    fun trackPlatforms(platform: String?) {
        val game = uiState.value.game ?: return
        val selected = if (platform == null) null else game.trackedPlatforms?.let {
            if (platform in it) it - platform else it + platform
        } ?: setOf(platform)
        if (selected?.isEmpty() == true) {
            _snackbar.value = "Keep at least one platform selected, or turn alerts off."
            return
        }
        viewModelScope.launch {
            try {
                wishlistRepository.setTrackedPlatforms(game.id, selected)
                _snackbar.value = if (selected == null) "Tracking all available platforms" else "Tracking ${selected.joinToString()}"
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _snackbar.value = "Couldn't save your platform choices. Try again." }
        }
    }

    fun setReqMode(mode: String) {
        _reqMode.value = mode
    }

    fun toggleWishlist() {
        val currentGame = uiState.value.game ?: return
        viewModelScope.launch {
            if (currentGame.inWishlist) {
                wishlistRepository.removeGameFromWishlist(currentGame.id)
                _snackbar.value = "${currentGame.title} removed from your wishlist"
            } else {
                wishlistRepository.saveGame(currentGame)
                val result = wishlistRepository.addGameToWishlist(currentGame.id)
                result.onSuccess {
                    _snackbar.value = "${currentGame.title} added to your wishlist"
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

    fun toggleAlert() {
        val currentGame = uiState.value.game ?: return
        viewModelScope.launch {
            val newAlert = !currentGame.alertEnabled
            wishlistRepository.toggleAlert(currentGame.id, newAlert)
            _snackbar.value = if (newAlert) "Alerts on for ${currentGame.title}" else "Alerts off for ${currentGame.title}"
        }
    }

    fun togglePriceAlert() {
        _snackbar.value = "Get Pro to see price-drop alerts for PC stores."
    }

    fun clearSnackbar() {
        _snackbar.value = null
    }
}
