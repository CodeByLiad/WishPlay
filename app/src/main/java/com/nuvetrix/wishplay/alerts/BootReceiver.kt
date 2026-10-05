package com.nuvetrix.wishplay.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nuvetrix.wishplay.data.local.dao.WishlistDao
import com.nuvetrix.wishplay.data.local.prefs.UserPreferences
import com.nuvetrix.wishplay.domain.model.Game
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.nuvetrix.wishplay.data.repository.AlertsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BootReceiverEntryPoint {
    fun wishlistDao(): WishlistDao
    fun alertScheduler(): AlertScheduler
    fun userPreferences(): UserPreferences
    fun alertsRepository(): AlertsRepository
}

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.nuvetrix.wishplay.ACTION_SIMULATE_DATE_CHANGE") {
            val gameId = intent.getStringExtra("game_id") ?: "pk"
            val newDate = intent.getStringExtra("new_date") ?: "2026-10-16"
            val oldDate = intent.getStringExtra("old_date") ?: "2026-10-02"
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        BootReceiverEntryPoint::class.java
                    )
                    entryPoint.alertsRepository().simulateDateChange(gameId, newDate, oldDate)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        BootReceiverEntryPoint::class.java
                    )
                    val wishlistDao = entryPoint.wishlistDao()
                    val alertScheduler = entryPoint.alertScheduler()
                    val userPreferences = entryPoint.userPreferences()

                    val reminderLead = userPreferences.reminderLead.first()
                    val alertRelease = userPreferences.alertRelease.first()
                    val alertCountdown = userPreferences.alertCountdown.first()

                    val items = wishlistDao.getWishlistSync()
                    for (item in items) {
                        if (item.wishlistItem?.alertEnabled == true) {
                            val platformsMap = try {
                                Json.decodeFromString<Map<String, String?>>(item.game.platformsJson)
                            } catch (_: Exception) {
                                emptyMap()
                            }
                            val game = Game(
                                id = item.game.id,
                                title = item.game.title,
                                platforms = platformsMap,
                                alertEnabled = true,
                                movedFromDate = item.game.movedFromDate
                            )
                            alertScheduler.scheduleAlertsForGame(
                                game = game,
                                leadPreference = reminderLead,
                                alertReleaseEnabled = alertRelease,
                                alertCountdownEnabled = alertCountdown
                            )
                        }
                    }

                    // Ensure daily check is alive
                    DailyCheckWorker.enqueuePeriodic(context)
                } catch (_: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
