package com.jinatra.wishplay

import android.app.Application
import com.jinatra.wishplay.alerts.DailyCheckWorker
import com.jinatra.wishplay.notifications.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import net.sqlcipher.database.SQLiteDatabase

@HiltAndroidApp
class WishPlayApplication : Application(), androidx.work.Configuration.Provider {
    // Also support on-demand initialization when Android starts an update/worker process
    // without the normal Startup provider path (including instrumented update launches).
    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        SQLiteDatabase.loadLibs(this)
        NotificationHelper.createNotificationChannels(this)
        DailyCheckWorker.enqueuePeriodic(this)
    }
}
