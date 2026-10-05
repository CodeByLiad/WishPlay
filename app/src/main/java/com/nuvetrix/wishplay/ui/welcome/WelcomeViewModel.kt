package com.nuvetrix.wishplay.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvetrix.wishplay.data.local.prefs.UserPreferences
import com.nuvetrix.wishplay.data.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WelcomeUiState(
    val currentStep: Int = 0, // 0 = welcome, 1 = platforms, 2 = lead
    val selectedPlatforms: Set<String> = emptySet(),
    val selectedLead: String = "1 week"
)

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState = _uiState.asStateFlow()

    fun onContinueAsGuest() {
        viewModelScope.launch {
            userPreferences.setUserRole("guest")
            _uiState.update { it.copy(currentStep = 1) }
        }
    }

    fun onContinueWithGoogle() {
        viewModelScope.launch {
            userPreferences.setUserRole("free")
            _uiState.update { it.copy(currentStep = 1) }
        }
    }

    fun togglePlatform(platform: String) {
        _uiState.update { current ->
            val set = current.selectedPlatforms.toMutableSet()
            if (set.contains(platform)) set.remove(platform) else set.add(platform)
            current.copy(selectedPlatforms = set)
        }
    }

    fun selectLead(lead: String) {
        _uiState.update { it.copy(selectedLead = lead) }
    }

    fun onStepBack() {
        _uiState.update { current ->
            if (current.currentStep > 0) current.copy(currentStep = current.currentStep - 1) else current
        }
    }

    fun onStepNext() {
        _uiState.update { it.copy(currentStep = 2) }
    }

    fun onCompleteOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            userPreferences.setPreferredPlatforms(_uiState.value.selectedPlatforms)
            userPreferences.setReminderLead(_uiState.value.selectedLead)
            userPreferences.setOnboardingDone(true)
            wishlistRepository.seedInitialDataIfEmpty()
            onFinished()
        }
    }

    fun onSkipOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            userPreferences.setOnboardingDone(true)
            wishlistRepository.seedInitialDataIfEmpty()
            onFinished()
        }
    }
}
