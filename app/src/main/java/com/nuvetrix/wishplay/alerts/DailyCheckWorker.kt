package com.nuvetrix.wishplay.alerts

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nuvetrix.wishplay.data.local.dao.AlertDao
import com.nuvetrix.wishplay.data.local.dao.WishlistDao
import com.nuvetrix.wishplay.data.local.entity.RecentAlertEntity
import com.nuvetrix.wishplay.data.local.prefs.UserPreferences
import com.nuvetrix.wishplay.data.remote.api.WishPlayApiService
import com.nuvetrix.wishplay.domain.model.Game
import com.nuvetrix.wishplay.notifications.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DailyCheckWorkerEntryPoint {
    fun wishlistDao(): WishlistDao
    fun alertDao(): AlertDao
    fun alertScheduler(): AlertScheduler
    fun apiService(): WishPlayApiService
    fun userPreferences(): UserPreferences
}

class DailyCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val entryPoint: DailyCheckWorkerEntryPoint? by lazy {
        try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                DailyCheckWorkerEntryPoint::class.java
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun doWork(): Result {
        val ep = entryPoint ?: return Result.retry()
        val wishlistDao = ep.wishlistDao()
        val alertDao = ep.alertDao()
        val alertScheduler = ep.alertScheduler()
        val apiService = ep.apiService()
        val userPreferences = ep.userPreferences()

        val alertChangesEnabled = userPreferences.alertChanges.first()
        val alertPriceEnabled = userPreferences.alertPrice.first()
        val reminderLead = userPreferences.reminderLead.first()
        val alertRelease = userPreferences.alertRelease.first()
        val alertCountdown = userPreferences.alertCountdown.first()

        val wishlist = wishlistDao.getWishlistSync()

        for (item in wishlist) {
            val gameEntity = item.game
            if (gameEntity.isCustom) continue // PRD: Custom games use only the user's date; no daily check.

            val freshDto = try {
                apiService.getGameDetails(gameEntity.id)
            } catch (_: Exception) {
                null
            } ?: continue

            val oldPlatformsMap = try {
                Json.decodeFromString<Map<String, String?>>(gameEntity.platformsJson)
            } catch (_: Exception) {
                emptyMap()
            }
            val oldEarliest = oldPlatformsMap.values.filterNotNull().sorted().firstOrNull()

            val newPlatformsMap = freshDto.platforms
            val newEarliest = newPlatformsMap.values.filterNotNull().sorted().firstOrNull()

            // 1. Check Date Change (delays, earlier launches, or moved back to TBA)
            if (oldEarliest != newEarliest) {
                val updatedGameEntity = gameEntity.copy(
                    platformsJson = Json.encodeToString(newPlatformsMap),
                    movedFromDate = oldEarliest ?: gameEntity.movedFromDate
                )
                wishlistDao.insertGame(updatedGameEntity)

                val updatedGame = Game(
                    id = updatedGameEntity.id,
                    title = updatedGameEntity.title,
                    platforms = newPlatformsMap,
                    alertEnabled = item.wishlistItem?.alertEnabled ?: true,
                    movedFromDate = updatedGameEntity.movedFromDate
                )

                // Reschedule reminders
                alertScheduler.scheduleAlertsForGame(
                    game = updatedGame,
                    leadPreference = reminderLead,
                    alertReleaseEnabled = alertRelease,
                    alertCountdownEnabled = alertCountdown
                )

                // Post date change notification if alerts enabled
                if (alertChangesEnabled && (item.wishlistItem?.alertEnabled != false)) {
                    val oldDisplay = oldEarliest ?: "TBA"
                    val newDisplay = newEarliest ?: "TBA"
                    NotificationHelper.postDateChangeNotification(
                        context = context,
                        gameId = updatedGameEntity.id,
                        gameTitle = updatedGameEntity.title,
                        oldDate = oldDisplay,
                        newDate = newDisplay
                    )

                    alertDao.insertRecentAlert(
                        RecentAlertEntity(
                            id = UUID.randomUUID().toString(),
                            gameId = updatedGameEntity.id,
                            type = "DELAY",
                            title = "${updatedGameEntity.title} was delayed",
                            subtitle = "Moved from $oldDisplay to $newDisplay. Your reminders moved with it.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }

            // 2. Check Price Drop (Pro PC stores)
            if (alertPriceEnabled && freshDto.price != null) {
                val oldPriceJson = gameEntity.priceJson
                if (oldPriceJson != null) {
                    val freshPrice = freshDto.price
                    val updatedPriceJson = Json.encodeToString(freshPrice)
                    if (updatedPriceJson != oldPriceJson && freshPrice.was != null) {
                        NotificationHelper.postPriceDropNotification(
                            context = context,
                            gameId = gameEntity.id,
                            gameTitle = gameEntity.title,
                            newPrice = freshPrice.now,
                            wasPrice = freshPrice.was,
                            store = freshPrice.store
                        )

                        alertDao.insertRecentAlert(
                            RecentAlertEntity(
                                id = UUID.randomUUID().toString(),
                                gameId = gameEntity.id,
                                type = "PRICE_DROP",
                                title = "${gameEntity.title} is on sale",
                                subtitle = "${freshPrice.now} on ${freshPrice.store}, down from ${freshPrice.was}",
                                timestamp = System.currentTimeMillis()
                            )
                        )

                        wishlistDao.insertGame(gameEntity.copy(priceJson = updatedPriceJson))
                    }
                }
            }
        }

        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "wishplay_daily_check"

        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<DailyCheckWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
