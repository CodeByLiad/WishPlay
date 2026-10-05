package com.nuvetrix.wishplay

import android.app.Application
import com.nuvetrix.wishplay.alerts.DailyCheckWorker
import com.nuvetrix.wishplay.notifications.NotificationHelper
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
