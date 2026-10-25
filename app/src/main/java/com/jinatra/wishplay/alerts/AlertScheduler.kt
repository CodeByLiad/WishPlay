package com.jinatra.wishplay.alerts

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jinatra.wishplay.data.local.dao.AlertDao
import com.jinatra.wishplay.data.local.entity.ScheduledAlertEntity
import com.jinatra.wishplay.domain.model.Game
import com.jinatra.wishplay.domain.model.releaseGroups
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
        // Always clear stale reminders, including dates that have returned to TBA.
        cancelAlertsForGame(game.id)
        if (!game.alertEnabled) return
        game.reminderPlatforms.releaseGroups().forEach { (date, ports) ->
                scheduleReleaseGroup(game, LocalDate.parse(date), ports.joinToString(", "),
                    leadPreference, alertReleaseEnabled, alertCountdownEnabled)
            }
    }

    private suspend fun scheduleReleaseGroup(
        game: Game,
        targetDate: LocalDate,
        platformsText: String,
        leadPreference: String,
        alertReleaseEnabled: Boolean,
        alertCountdownEnabled: Boolean
    ) {
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
                    .putString("alert_id", "alert_release_${game.id}_${targetDate}")
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
                    "alert_release_${game.id}_${targetDate}",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

                scheduledList.add(
                    ScheduledAlertEntity(
                        id = "alert_release_${game.id}_${targetDate}",
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
                    .putString("alert_id", "alert_countdown_${game.id}_${targetDate}")
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
                    "alert_countdown_${game.id}_${targetDate}",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

                scheduledList.add(
                    ScheduledAlertEntity(
                        id = "alert_countdown_${game.id}_${targetDate}",
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
