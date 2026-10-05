package com.nuvetrix.wishplay.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nuvetrix.wishplay.data.local.dao.AlertDao
import com.nuvetrix.wishplay.data.local.entity.RecentAlertEntity
import com.nuvetrix.wishplay.notifications.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.UUID

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AlertWorkerEntryPoint {
    fun alertDao(): AlertDao
}

class AlertNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val alertDao: AlertDao? by lazy {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                AlertWorkerEntryPoint::class.java
            )
            entryPoint.alertDao()
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun doWork(): Result {
        val alertId = inputData.getString("alert_id") ?: UUID.randomUUID().toString()
        val gameId = inputData.getString("game_id") ?: return Result.failure()
        val gameTitle = inputData.getString("game_title") ?: "Wishlisted Game"
        val alertType = inputData.getString("alert_type") ?: "RELEASE"
        val platformsText = inputData.getString("platforms_text") ?: "Multiple platforms"
        val leadPhrase = inputData.getString("lead_phrase") ?: "soon"

        if (alertType == "RELEASE") {
            NotificationHelper.postReleaseNotification(
                context = context,
                gameId = gameId,
                gameTitle = gameTitle,
                platformsText = platformsText
            )
            alertDao?.insertRecentAlert(
                RecentAlertEntity(
                    id = UUID.randomUUID().toString(),
                    gameId = gameId,
                    type = "RELEASE",
                    title = "$gameTitle is out now",
                    subtitle = "Released on $platformsText",
                    timestamp = System.currentTimeMillis()
                )
            )
        } else {
            NotificationHelper.postCountdownNotification(
                context = context,
                gameId = gameId,
                gameTitle = gameTitle,
                daysLeftPhrase = leadPhrase,
                platformsText = platformsText
            )
        }

        alertDao?.markAlertFired(alertId)
        return Result.success()
    }
}
