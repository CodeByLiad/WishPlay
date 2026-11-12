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
    val snackbarMessage: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isUpdating: Boolean = false
)

@HiltViewModel
class GameDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wishlistRepository: WishlistRepository,
    private val apiService: WishPlayApiService
) : ViewModel() {

    private val gameId: String = checkNotNull(savedStateHandle["gameId"])

    private val _updating = MutableStateFlow(false)
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
            snackbarMessage = snackbar
        )
    }.combine(_loadState) { state, load -> state.copy(isLoading = load.first, error = load.second) }.combine(_updating) { state, updating -> state.copy(isUpdating = updating) }.stateIn(
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
                val cachedGame = wishlistRepository.observeGameById(gameId).first()
                if (cachedGame?.isCustom == true) {
                    _loadState.value = false to null
                    return@launch
                }
                val dto = apiService.getGameDetails(gameId, cachedGame?.title)
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
        val game = uiState.value.game ?: return
        if (_updating.value) return
        _updating.value = true
        viewModelScope.launch {
            try {
                if (game.inWishlist) {
                    wishlistRepository.removeGameFromWishlist(game.id)
                    _snackbar.value = "${game.title} removed from your wishlist"
                } else {
                    wishlistRepository.saveGame(game)
                    wishlistRepository.addGameToWishlist(game.id).onSuccess {
                        _snackbar.value = "${game.title} added to your wishlist"
                    }.onFailure {
                        _snackbar.value = "Couldn't add this game. Try again."
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _snackbar.value = "Couldn't update your wishlist. Try again." }
            finally { _updating.value = false }
        }
    }

    fun toggleAlert() {
        val game = uiState.value.game ?: return
        if (_updating.value) return
        _updating.value = true
        viewModelScope.launch {
            try {
                wishlistRepository.toggleAlert(game.id, !game.alertEnabled)
                _snackbar.value = if (game.alertEnabled) "Alerts off for ${game.title}" else "Alerts on for ${game.title}"
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _snackbar.value = "Couldn't change alerts. Try again." }
            finally { _updating.value = false }
        }
    }

    fun togglePriceAlert() {
        val game = uiState.value.game ?: return
        if (!game.inWishlist || _updating.value) return
        _updating.value = true
        viewModelScope.launch {
            try {
                wishlistRepository.togglePriceAlert(game.id, !game.priceAlertEnabled)
                _snackbar.value = if (game.priceAlertEnabled) "Price alerts off" else "Price alerts on"
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _snackbar.value = "Couldn't change price alerts. Try again." }
            finally { _updating.value = false }
        }
    }

    fun clearSnackbar() {
        _snackbar.value = null
    }
}
