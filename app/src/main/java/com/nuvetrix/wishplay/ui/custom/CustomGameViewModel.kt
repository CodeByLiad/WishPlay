package com.nuvetrix.wishplay.ui.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvetrix.wishplay.data.local.prefs.UserPreferences
import com.nuvetrix.wishplay.data.repository.WishlistRepository
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
import javax.inject.Inject

data class CustomGameUiState(
    val title: String = "",
    val titleError: Boolean = false,
    val selectedPlatforms: Set<String> = setOf("Android"),
    val releaseDate: String? = "2026-10-30",
    val isTba: Boolean = false,
    val notes: String = "",
    val reminderLead: String = "1 week",
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CustomGameViewModel @Inject constructor(
    private val wishlistRepository: WishlistRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _title = MutableStateFlow("")
    private val _titleError = MutableStateFlow(false)
    private val _platforms = MutableStateFlow(setOf("Android"))
    private val _date = MutableStateFlow<String?>("2026-10-30")
    private val _isTba = MutableStateFlow(false)
    private val _notes = MutableStateFlow("")
    private val _isSaved = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState = combine(
        _title,
        _titleError,
        _platforms,
        _date,
        _isTba,
        _notes,
        userPreferences.reminderLead,
        _isSaved,
        _error
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
            errorMessage = args[8] as String?
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
        val calculated = LocalDate.parse("2026-09-30").plusDays(daysToAdd)
        _date.value = calculated.toString()
        _isTba.value = false
    }

    fun onToggleTba(tba: Boolean) {
        _isTba.value = tba
        if (tba) {
            _date.value = null
        } else {
            _date.value = "2026-10-30"
        }
    }

    fun onNotesChange(newNotes: String) {
        _notes.value = newNotes
    }

    fun saveCustomGame(onSuccess: (String) -> Unit) {
        val titleText = _title.value.trim()
        if (titleText.isEmpty()) {
            _titleError.value = true
            return
        }

        viewModelScope.launch {
            val result = wishlistRepository.addCustomGame(
                title = titleText,
                platforms = _platforms.value.toList(),
                date = if (_isTba.value) null else _date.value,
                notes = _notes.value.trim().ifEmpty { null }
            )

            result.onSuccess { game ->
                _isSaved.value = true
                onSuccess(game.id)
            }.onFailure { ex ->
                _error.value = ex.message
            }
        }
    }
}
