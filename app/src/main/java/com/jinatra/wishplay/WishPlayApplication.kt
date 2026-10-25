package com.jinatra.wishplay

import android.app.Application
import com.jinatra.wishplay.alerts.DailyCheckWorker
import com.jinatra.wishplay.notifications.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import net.sqlcipher.database.SQLiteDatabase

@HiltAndroidApp
class WishPlayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SQLiteDatabase.loadLibs(this)
        NotificationHelper.createNotificationChannels(this)
        DailyCheckWorker.enqueuePeriodic(this)
    }
}
