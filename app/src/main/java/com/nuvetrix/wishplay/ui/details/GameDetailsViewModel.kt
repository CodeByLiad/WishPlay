package com.nuvetrix.wishplay.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvetrix.wishplay.data.remote.api.WishPlayApiService
import com.nuvetrix.wishplay.data.repository.WishlistRepository
import com.nuvetrix.wishplay.data.repository.toDomainModel
import com.nuvetrix.wishplay.domain.model.Game
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameDetailsUiState(
    val game: Game? = null,
    val selectedPlatform: String? = null,
    val reqMode: String = "min", // "min" or "rec"
    val isPro: Boolean = false,
    val snackbarMessage: String? = null
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

    val uiState: StateFlow<GameDetailsUiState> = combine(
        wishlistRepository.observeGameById(gameId),
        _remoteGame,
        _selectedPlatform,
        _reqMode,
        _snackbar
    ) { localGame, remoteGame, selectedPlatform, reqMode, snackbar ->
        val game = localGame ?: remoteGame
        val effectivePlat = selectedPlatform ?: game?.platforms?.keys?.firstOrNull()
        GameDetailsUiState(
            game = game,
            selectedPlatform = effectivePlat,
            reqMode = reqMode,
            isPro = false,
            snackbarMessage = snackbar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GameDetailsUiState()
    )

    init {
        // If not in local database or needs fresh details, fetch from API
        viewModelScope.launch {
            try {
                val dto = apiService.getGameDetails(gameId)
                if (dto != null) {
                    val domainGame = dto.toDomainModel()
                    _remoteGame.value = domainGame
                    // Save to local cache so details are available offline
                    wishlistRepository.saveGame(domainGame)
                }
            } catch (_: Exception) {}
        }
    }

    fun selectPlatform(platform: String) {
        _selectedPlatform.value = platform
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
