package com.nuvetrix.wishplay.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nuvetrix.wishplay.data.local.dao.AlertDao
import com.nuvetrix.wishplay.data.local.dao.WishlistDao
import com.nuvetrix.wishplay.data.local.entity.GameEntity
import com.nuvetrix.wishplay.data.local.entity.RecentAlertEntity
import com.nuvetrix.wishplay.data.local.entity.ScheduledAlertEntity
import com.nuvetrix.wishplay.data.local.entity.WishlistItemEntity

@Database(
    entities = [
        GameEntity::class,
        WishlistItemEntity::class,
        ScheduledAlertEntity::class,
        RecentAlertEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class WishPlayDatabase : RoomDatabase() {
    abstract fun wishlistDao(): WishlistDao
    abstract fun alertDao(): AlertDao
}
