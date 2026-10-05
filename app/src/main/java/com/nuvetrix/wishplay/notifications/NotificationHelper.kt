package com.nuvetrix.wishplay.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nuvetrix.wishplay.R
import com.nuvetrix.wishplay.ui.MainActivity

object NotificationHelper {

    const val CHANNEL_RELEASES = "releases"
    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_DATE_CHANGES = "date_changes"
    const val CHANNEL_PRICE_DROPS = "price_drops"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val releasesChannel = NotificationChannel(
                CHANNEL_RELEASES,
                "Game Releases",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts on release day at 9:00 AM"
                enableVibration(true)
            }

            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Upcoming Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Countdowns before release day (1 day, 3 days, 1 week)"
            }

            val dateChangesChannel = NotificationChannel(
                CHANNEL_DATE_CHANGES,
                "Release Date Changes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a release date moves, delays, or is announced"
                enableVibration(true)
            }

            val priceDropsChannel = NotificationChannel(
                CHANNEL_PRICE_DROPS,
                "Price Drops",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Price drop alerts for PC stores"
            }

            notificationManager.createNotificationChannels(
                listOf(releasesChannel, remindersChannel, dateChangesChannel, priceDropsChannel)
            )
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun getGameDetailsPendingIntent(context: Context, gameId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_game_id", gameId)
        }
        return PendingIntent.getActivity(
            context,
            gameId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun postReleaseNotification(
        context: Context,
        gameId: String,
        gameTitle: String,
        platformsText: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_RELEASES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$gameTitle is out today")
            .setContentText("Now on $platformsText · Tap to open")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(getGameDetailsPendingIntent(context, gameId))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(gameId.hashCode() + 1, notification)
        } catch (_: SecurityException) {
            // Permission revoked
        }
    }

    fun postCountdownNotification(
        context: Context,
        gameId: String,
        gameTitle: String,
        daysLeftPhrase: String,
        platformsText: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$gameTitle drops in $daysLeftPhrase")
            .setContentText("Releasing on $platformsText · Tap to view details")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(getGameDetailsPendingIntent(context, gameId))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(gameId.hashCode() + 2, notification)
        } catch (_: SecurityException) {
            // Permission revoked
        }
    }

    fun postDateChangeNotification(
        context: Context,
        gameId: String,
        gameTitle: String,
        oldDate: String,
        newDate: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_DATE_CHANGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$gameTitle was delayed")
            .setContentText("Moved from $oldDate to $newDate. Your reminders moved with it.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(getGameDetailsPendingIntent(context, gameId))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(gameId.hashCode() + 3, notification)
        } catch (_: SecurityException) {
            // Permission revoked
        }
    }

    fun postPriceDropNotification(
        context: Context,
        gameId: String,
        gameTitle: String,
        newPrice: String,
        wasPrice: String,
        store: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_PRICE_DROPS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$gameTitle is on sale")
            .setContentText("$newPrice on $store, down from $wasPrice")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(getGameDetailsPendingIntent(context, gameId))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(gameId.hashCode() + 4, notification)
        } catch (_: SecurityException) {
            // Permission revoked
        }
    }
}
