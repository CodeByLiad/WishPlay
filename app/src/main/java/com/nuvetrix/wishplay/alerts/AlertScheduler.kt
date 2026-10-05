package com.nuvetrix.wishplay.alerts

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nuvetrix.wishplay.data.local.dao.AlertDao
import com.nuvetrix.wishplay.data.local.entity.ScheduledAlertEntity
import com.nuvetrix.wishplay.domain.model.Game
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alertDao: AlertDao
) {
    private val workManager = WorkManager.getInstance(context)

    suspend fun scheduleAlertsForGame(
        game: Game,
        leadPreference: String = "1 week",
        alertReleaseEnabled: Boolean = true,
        alertCountdownEnabled: Boolean = true
    ) {
        val earliestDate = game.earliestDate ?: return
        val targetDate = try {
            LocalDate.parse(earliestDate)
        } catch (_: Exception) {
            return
        }

        // Cancel previous alerts for this game first
        cancelAlertsForGame(game.id)

        val platformsText = game.platforms.keys.joinToString(", ")
        val scheduledList = mutableListOf<ScheduledAlertEntity>()

        // 1. Release day alert (9:00 AM)
        if (alertReleaseEnabled && game.alertEnabled) {
            val releaseTime = targetDate.atTime(9, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val releaseDelay = releaseTime - System.currentTimeMillis()
            if (releaseDelay > 0) {
                val inputData = Data.Builder()
                    .putString("alert_id", "alert_release_${game.id}")
                    .putString("game_id", game.id)
                    .putString("game_title", game.title)
                    .putString("alert_type", "RELEASE")
                    .putString("platforms_text", platformsText)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<AlertNotificationWorker>()
                    .setInitialDelay(releaseDelay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .addTag("alert_game_${game.id}")
                    .addTag("alert_type_RELEASE")
                    .build()

                workManager.enqueueUniqueWork(
                    "alert_release_${game.id}",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

                scheduledList.add(
                    ScheduledAlertEntity(
                        id = "alert_release_${game.id}",
                        gameId = game.id,
                        alertType = "RELEASE",
                        targetTimeMillis = releaseTime
                    )
                )
            }
        }

        // 2. Countdown reminder (9:00 AM lead days prior)
        if (alertCountdownEnabled && game.alertEnabled) {
            val leadDays = when (leadPreference) {
                "1 day" -> 1
                "3 days" -> 3
                else -> 7
            }

            val countdownDate = targetDate.minusDays(leadDays.toLong())
            val countdownTime = countdownDate.atTime(9, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val countdownDelay = countdownTime - System.currentTimeMillis()
            if (countdownDelay > 0) {
                val inputData = Data.Builder()
                    .putString("alert_id", "alert_countdown_${game.id}")
                    .putString("game_id", game.id)
                    .putString("game_title", game.title)
                    .putString("alert_type", "COUNTDOWN")
                    .putString("platforms_text", platformsText)
                    .putString("lead_phrase", leadPreference)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<AlertNotificationWorker>()
                    .setInitialDelay(countdownDelay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .addTag("alert_game_${game.id}")
                    .addTag("alert_type_COUNTDOWN")
                    .build()

                workManager.enqueueUniqueWork(
                    "alert_countdown_${game.id}",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

                scheduledList.add(
                    ScheduledAlertEntity(
                        id = "alert_countdown_${game.id}",
                        gameId = game.id,
                        alertType = "COUNTDOWN",
                        targetTimeMillis = countdownTime
                    )
                )
            }
        }

        if (scheduledList.isNotEmpty()) {
            alertDao.insertScheduledAlerts(scheduledList)
        }
    }

    suspend fun cancelAlertsForGame(gameId: String) {
        workManager.cancelAllWorkByTag("alert_game_$gameId")
        alertDao.deleteAlertsForGame(gameId)
    }
}
