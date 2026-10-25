package com.jinatra.wishplay.ui.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinatra.wishplay.data.local.prefs.UserPreferences
import com.jinatra.wishplay.data.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.jinatra.wishplay.domain.model.validReleaseDate
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

data class CustomGameUiState(
    val title: String = "",
    val titleError: Boolean = false,
    val selectedPlatforms: Set<String> = setOf("Android"),
    val releaseDate: String? = LocalDate.now().plusDays(30).toString(),
    val isTba: Boolean = false,
    val notes: String = "",
    val reminderLead: String = "1 week",
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val separateDates: Boolean = false,
    val platformDates: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false
)

@HiltViewModel
class CustomGameViewModel @Inject constructor(
    private val wishlistRepository: WishlistRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _title = MutableStateFlow("")
    private val _titleError = MutableStateFlow(false)
    private val _platforms = MutableStateFlow(setOf("Android"))
    private val _date = MutableStateFlow<String?>(LocalDate.now().plusDays(30).toString())
    private val _isTba = MutableStateFlow(false)
    private val _notes = MutableStateFlow("")
    private val _isSaved = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _separateDates = MutableStateFlow(false)
    private val _platformDates = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _saving = MutableStateFlow(false)

    val uiState = combine(
        _title,
        _titleError,
        _platforms,
        _date,
        _isTba,
        _notes,
        userPreferences.reminderLead,
        _isSaved,
        _error,
        _separateDates,
        _platformDates,
        _saving
    ) { args: Array<Any?> ->
        CustomGameUiState(
            title = args[0] as String,
            titleError = args[1] as Boolean,
            selectedPlatforms = @Suppress("UNCHECKED_CAST") (args[2] as Set<String>),
            releaseDate = args[3] as String?,
            isTba = args[4] as Boolean,
            notes = args[5] as String,
            reminderLead = args[6] as String,
            isSaved = args[7] as Boolean,
            errorMessage = args[8] as String?,
            separateDates = args[9] as Boolean,
            platformDates = @Suppress("UNCHECKED_CAST") (args[10] as Map<String, String>),
            isSaving = args[11] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CustomGameUiState()
    )

    fun onTitleChange(newTitle: String) {
        _title.value = newTitle
        if (newTitle.trim().isNotEmpty()) {
            _titleError.value = false
        }
    }

    fun togglePlatform(platform: String) {
        _platforms.update { current ->
            val set = current.toMutableSet()
            if (set.contains(platform)) set.remove(platform) else set.add(platform)
            set
        }
    }

    fun onDateChange(newDate: String) {
        _date.value = newDate
        _isTba.value = false
    }

    fun onQuickDate(daysToAdd: Long) {
        val calculated = LocalDate.now().plusDays(daysToAdd)
        _date.value = calculated.toString()
        _isTba.value = false
    }

    fun onToggleTba(tba: Boolean) {
        _isTba.value = tba
        if (tba) {
            _date.value = null
        } else {
            _date.value = LocalDate.now().plusDays(30).toString()
        }
    }

    fun onNotesChange(newNotes: String) {
        _notes.value = newNotes
    }

    fun setSeparateDates(enabled: Boolean) { _separateDates.value = enabled }

    fun setPlatformDate(platform: String, date: String) {
        _platformDates.update { it + (platform to date) }
    }

    fun saveCustomGame(onSuccess: (String) -> Unit) {
        if (_saving.value) return
        val titleText = _title.value.trim()
        if (titleText.isEmpty()) {
            _titleError.value = true
            return
        }

        if (_platforms.value.isEmpty()) {
            _error.value = "Select at least one platform."
            return
        }
        val dates = _platforms.value.associateWith { platform ->
            if (_separateDates.value) _platformDates.value[platform]?.trim()?.ifBlank { null }
            else if (_isTba.value) null else _date.value?.trim()?.ifBlank { null }
        }
        if (dates.values.any { it != null && validReleaseDate(it) == null }) {
            _error.value = "Enter a valid date as YYYY-MM-DD, or leave it empty for TBA."
            return
        }

        _error.value = null
        _saving.value = true
        viewModelScope.launch {
            try {
            val result = wishlistRepository.addCustomGame(
                title = titleText,
                platforms = _platforms.value.toList(),
                date = if (_isTba.value) null else _date.value,
                notes = _notes.value.trim().ifEmpty { null },
                platformDates = dates
            )

            result.onSuccess { game ->
                _isSaved.value = true
                onSuccess(game.id)
            }.onFailure { ex ->
                _error.value = if (ex.message == "CAP_REACHED") "Wishlist full. Upgrade to Pro for unlimited games." else "Couldn't save the game. Try again."
            }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _error.value = "Couldn't save the game. Try again."
            } finally { _saving.value = false }
        }
    }
}
