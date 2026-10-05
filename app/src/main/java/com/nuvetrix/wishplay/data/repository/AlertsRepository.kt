package com.nuvetrix.wishplay.data.repository

import android.content.Context
import com.nuvetrix.wishplay.alerts.AlertScheduler
import com.nuvetrix.wishplay.data.local.dao.AlertDao
import com.nuvetrix.wishplay.data.local.dao.WishlistDao
import com.nuvetrix.wishplay.data.local.entity.RecentAlertEntity
import com.nuvetrix.wishplay.data.local.entity.ScheduledAlertEntity
import com.nuvetrix.wishplay.data.local.prefs.UserPreferences
import com.nuvetrix.wishplay.domain.model.Game
import com.nuvetrix.wishplay.notifications.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alertDao: AlertDao,
    private val wishlistDao: WishlistDao,
    private val alertScheduler: AlertScheduler,
    private val userPreferences: UserPreferences
) {
    val recentAlerts: Flow<List<RecentAlertEntity>> = alertDao.observeRecentAlerts()
    val scheduledAlerts: Flow<List<ScheduledAlertEntity>> = alertDao.observePendingScheduledAlerts()

    val alertRelease: Flow<Boolean> = userPreferences.alertRelease
    val alertCountdown: Flow<Boolean> = userPreferences.alertCountdown
    val alertChanges: Flow<Boolean> = userPreferences.alertChanges
    val alertPrice: Flow<Boolean> = userPreferences.alertPrice
    val reminderLead: Flow<String> = userPreferences.reminderLead
    val userRole: Flow<String> = userPreferences.userRole

    suspend fun setAlertRelease(enabled: Boolean) {
        userPreferences.setAlertRelease(enabled)
        refreshAllAlerts()
    }

    suspend fun setAlertCountdown(enabled: Boolean) {
        userPreferences.setAlertCountdown(enabled)
        refreshAllAlerts()
    }

    suspend fun setAlertChanges(enabled: Boolean) {
        userPreferences.setAlertChanges(enabled)
    }

    suspend fun setAlertPrice(enabled: Boolean) {
        userPreferences.setAlertPrice(enabled)
    }

    suspend fun setReminderLead(lead: String) {
        userPreferences.setReminderLead(lead)
        refreshAllAlerts()
    }

    private suspend fun refreshAllAlerts() {
        val lead = userPreferences.reminderLead.first()
        val release = userPreferences.alertRelease.first()
        val countdown = userPreferences.alertCountdown.first()
        val items = wishlistDao.getWishlistSync()

        for (item in items) {
            if (item.wishlistItem?.alertEnabled == true) {
                alertScheduler.scheduleAlertsForGame(
                    game = item.toDomainModel(),
                    leadPreference = lead,
                    alertReleaseEnabled = release,
                    alertCountdownEnabled = countdown
                )
            }
        }
    }

    /**
     * Seeds initial recent alerts from prototype if none exist
     */
    suspend fun seedInitialRecentAlertsIfEmpty() {
        val existing = alertDao.getRecentAlerts()
        if (existing.isNotEmpty()) return

        val seed = listOf(
            RecentAlertEntity(
                id = "seed_delay",
                gameId = "ic",
                type = "DELAY",
                title = "Iron Circuit: Rebellion was delayed",
                subtitle = "Moved from Oct 22 to Dec 3. Your reminders moved with it.",
                timestamp = System.currentTimeMillis() - 7200000
            ),
            RecentAlertEntity(
                id = "seed_price",
                gameId = "sf",
                type = "PRICE_DROP",
                title = "Starfall Odyssey is 20% off",
                subtitle = "$47.99 on Steam, down from $59.99",
                timestamp = System.currentTimeMillis() - 86400000
            ),
            RecentAlertEntity(
                id = "seed_release",
                gameId = "tb",
                type = "RELEASE",
                title = "Tidebound is out now",
                subtitle = "Released Sep 18, 2026 on Android",
                timestamp = System.currentTimeMillis() - 172800000
            )
        )
        alertDao.insertRecentAlerts(seed)
    }

    /**
     * Simulates a date change from the server to verify Phase 3 "Done when" condition:
     * "Done when a changed date on the server produces an alert on the phone."
     */
    suspend fun simulateDateChange(
        gameId: String,
        newDate: String,
        oldDate: String = "2026-10-22"
    ): Result<Unit> {
        val gameWithWishlist = wishlistDao.getGameById(gameId)
            ?: return Result.failure(IllegalArgumentException("Game not found: $gameId"))

        val gameEntity = gameWithWishlist.game
        val platformsMap = mutableMapOf<String, String?>()
        try {
            val json = org.json.JSONObject(gameEntity.platformsJson)
            json.keys().forEach { key ->
                platformsMap[key] = if (json.isNull(key)) null else json.getString(key)
            }
        } catch (_: Exception) {}

        if (platformsMap.isEmpty()) {
            if (gameId == "pk") {
                platformsMap["Android"] = newDate
                platformsMap["iOS"] = newDate
            } else {
                platformsMap["PC"] = newDate
            }
        } else {
            platformsMap.keys.forEach { plat ->
                platformsMap[plat] = newDate
            }
        }

        val outJson = org.json.JSONObject()
        platformsMap.forEach { (k, v) ->
            if (v != null) outJson.put(k, v) else outJson.put(k, org.json.JSONObject.NULL)
        }

        val updatedEntity = gameEntity.copy(
            platformsJson = outJson.toString(),
            movedFromDate = oldDate
        )
        wishlistDao.insertGame(updatedEntity)

        val updatedGame = gameWithWishlist.copy(game = updatedEntity).toDomainModel()

        // Reschedule alerts
        val lead = userPreferences.reminderLead.first()
        val release = userPreferences.alertRelease.first()
        val countdown = userPreferences.alertCountdown.first()
        alertScheduler.scheduleAlertsForGame(
            game = updatedGame,
            leadPreference = lead,
            alertReleaseEnabled = release,
            alertCountdownEnabled = countdown
        )

        // Post Date Change Notification
        NotificationHelper.postDateChangeNotification(
            context = context,
            gameId = gameId,
            gameTitle = updatedEntity.title,
            oldDate = oldDate,
            newDate = newDate
        )

        // Record in recent alerts
        alertDao.insertRecentAlert(
            RecentAlertEntity(
                id = UUID.randomUUID().toString(),
                gameId = gameId,
                type = "DELAY",
                title = "${updatedEntity.title} was delayed",
                subtitle = "Moved from $oldDate to $newDate. Your reminders moved with it.",
                timestamp = System.currentTimeMillis()
            )
        )

        return Result.success(Unit)
    }

    private fun com.nuvetrix.wishplay.data.local.entity.GameWithWishlist.toDomainModel(): Game {
        val platMap = mutableMapOf<String, String?>()
        try {
            val json = org.json.JSONObject(game.platformsJson)
            json.keys().forEach { key ->
                platMap[key] = if (json.isNull(key)) null else json.getString(key)
            }
        } catch (_: Exception) {}
        return Game(
            id = game.id,
            title = game.title,
            developer = game.developer,
            hueHex = game.hueHex,
            shapeKey = game.shapeKey,
            platforms = platMap,
            about = game.about,
            isCustom = game.isCustom,
            customNotes = game.customNotes,
            hasTrailer = game.hasTrailer,
            coverUrl = game.coverUrl,
            logoUrl = game.logoUrl,
            trailerYoutubeId = game.trailerYoutubeId,
            movedFromDate = game.movedFromDate,
            expectedYear = game.expectedYear,
            progress = game.progress,
            igdbId = game.igdbId,
            inWishlist = wishlistItem != null,
            alertEnabled = wishlistItem?.alertEnabled ?: true,
            priceAlertEnabled = wishlistItem?.priceAlertEnabled ?: false,
            orderIndex = wishlistItem?.orderIndex ?: 0
        )
    }
}
