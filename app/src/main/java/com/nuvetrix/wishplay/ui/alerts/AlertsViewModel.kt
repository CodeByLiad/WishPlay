package com.nuvetrix.wishplay.ui.alerts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvetrix.wishplay.data.local.entity.RecentAlertEntity
import com.nuvetrix.wishplay.data.repository.AlertsRepository
import com.nuvetrix.wishplay.data.repository.WishlistRepository
import com.nuvetrix.wishplay.domain.model.Game
import com.nuvetrix.wishplay.domain.model.ReleaseStatus
import com.nuvetrix.wishplay.notifications.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class AlertsUiState(
    val upcomingByMonth: List<Pair<String, List<Game>>> = emptyList(),
    val upcomingCount: Int = 0,
    val recentAlerts: List<RecentAlertEntity> = emptyList(),
    val alertRelease: Boolean = true,
    val alertCountdown: Boolean = true,
    val alertLead: String = "1 week",
    val alertChanges: Boolean = true,
    val alertPrice: Boolean = true,
    val isPro: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val showPrimingSheet: Boolean = false,
    val showBatterySheet: Boolean = false
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alertsRepository: AlertsRepository,
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    private val _showPrimingSheet = MutableStateFlow(false)
    val showPrimingSheet: StateFlow<Boolean> = _showPrimingSheet.asStateFlow()

    private val _showBatterySheet = MutableStateFlow(false)
    val showBatterySheet: StateFlow<Boolean> = _showBatterySheet.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(NotificationHelper.areNotificationsEnabled(context))

    init {
        viewModelScope.launch {
            alertsRepository.seedInitialRecentAlertsIfEmpty()
        }
    }

    val uiState: StateFlow<AlertsUiState> = combine(
        wishlistRepository.wishlistGames,
        alertsRepository.recentAlerts,
        alertsRepository.alertRelease,
        alertsRepository.alertCountdown,
        alertsRepository.reminderLead,
        alertsRepository.alertChanges,
        alertsRepository.alertPrice,
        alertsRepository.userRole,
        _showPrimingSheet,
        _showBatterySheet,
        _notificationsEnabled
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val games = args[0] as List<Game>
        @Suppress("UNCHECKED_CAST")
        val recent = args[1] as List<RecentAlertEntity>
        val release = args[2] as Boolean
        val countdown = args[3] as Boolean
        val lead = args[4] as String
        val changes = args[5] as Boolean
        val price = args[6] as Boolean
        val role = args[7] as String
        val priming = args[8] as Boolean
        val battery = args[9] as Boolean
        val notifEnabled = args[10] as Boolean

        val upcomingGames = games.filter { it.alertEnabled && it.status() == ReleaseStatus.UPCOMING }
            .sortedBy { it.earliestDate ?: "9999" }

        val byMonth = mutableListOf<Pair<String, MutableList<Game>>>()
        upcomingGames.forEach { game ->
            val dateStr = game.earliestDate
            if (dateStr != null) {
                val monthKey = if (dateStr.length >= 7) dateStr.substring(0, 7) else "Unknown"
                val existing = byMonth.find { it.first == monthKey }
                if (existing != null) {
                    existing.second.add(game)
                } else {
                    byMonth.add(monthKey to mutableListOf(game))
                }
            }
        }

        AlertsUiState(
            upcomingByMonth = byMonth.map { (key, list) -> formatMonthLabel(key) to list },
            upcomingCount = upcomingGames.size,
            recentAlerts = recent,
            alertRelease = release,
            alertCountdown = countdown,
            alertLead = lead,
            alertChanges = changes,
            alertPrice = price,
            isPro = role == "pro" || role == "admin",
            notificationsEnabled = notifEnabled,
            showPrimingSheet = priming,
            showBatterySheet = battery
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlertsUiState()
    )

    fun onToggleRelease(enabled: Boolean) {
        viewModelScope.launch { alertsRepository.setAlertRelease(enabled) }
    }

    fun onToggleCountdown(enabled: Boolean) {
        viewModelScope.launch { alertsRepository.setAlertCountdown(enabled) }
    }

    fun onSelectLead(lead: String) {
        viewModelScope.launch { alertsRepository.setReminderLead(lead) }
    }

    fun onToggleChanges(enabled: Boolean) {
        viewModelScope.launch { alertsRepository.setAlertChanges(enabled) }
    }

    fun onTogglePrice(enabled: Boolean) {
        viewModelScope.launch { alertsRepository.setAlertPrice(enabled) }
    }

    fun openPrimingSheet() {
        _showPrimingSheet.value = true
    }

    fun closePrimingSheet() {
        _showPrimingSheet.value = false
    }

    fun openBatterySheet() {
        _showBatterySheet.value = true
    }

    fun closeBatterySheet() {
        _showBatterySheet.value = false
    }

    fun refreshNotificationStatus() {
        _notificationsEnabled.value = NotificationHelper.areNotificationsEnabled(context)
    }

    fun simulateDateChange(gameId: String, newDate: String, oldDate: String) {
        viewModelScope.launch {
            alertsRepository.simulateDateChange(gameId, newDate, oldDate)
        }
    }

    private fun formatMonthLabel(key: String): String {
        return try {
            val ym = LocalDate.parse("$key-01")
            ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
        } catch (_: Exception) {
            key
        }
    }
}
