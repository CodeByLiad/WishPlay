package com.jinatra.wishplay

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.jinatra.wishplay.notifications.NotificationHelper
import org.junit.Assert.*
import org.junit.Test

class NotificationDeliveryTest {
    @Test fun allFourNotificationTypesReachAndroidWithTheirChannelsAndOpenActions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertTrue("Notification permission must be granted on the test device",
            NotificationManagerCompat.from(context).areNotificationsEnabled())
        NotificationHelper.createNotificationChannels(context)
        val gameId = "notification_delivery_fixture"
        try {
            NotificationHelper.postReleaseNotification(context, gameId, "Notification test", "PC")
            NotificationHelper.postCountdownNotification(context, gameId, "Notification test", "1 week", "PC")
            NotificationHelper.postDateChangeNotification(context, gameId, "Notification test", "2027-01-01", "2027-02-01")
            NotificationHelper.postPriceDropNotification(context, gameId, "Notification test", "$10", "$20", "Steam")
            val deadline = android.os.SystemClock.elapsedRealtime() + 5000
            var posted = manager.activeNotifications.filter { it.id in (1..4).map { offset -> gameId.hashCode() + offset } }
            while (posted.size < 4 && android.os.SystemClock.elapsedRealtime() < deadline) {
                android.os.SystemClock.sleep(100)
                posted = manager.activeNotifications.filter { it.id in (1..4).map { offset -> gameId.hashCode() + offset } }
            }
            assertEquals(4, posted.size)
            assertEquals(setOf("releases", "reminders", "date_changes", "price_drops"),
                posted.map { it.notification.channelId }.toSet())
            posted.forEach { assertNotNull(it.notification.contentIntent) }
        } finally {
            (1..4).forEach { manager.cancel(gameId.hashCode() + it) }
        }
    }
}
