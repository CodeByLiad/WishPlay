package com.jinatra.wishplay.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN releaseEvidenceJson TEXT NOT NULL DEFAULT '{}'")
        db.execSQL("ALTER TABLE wishlist ADD COLUMN trackedPlatformsJson TEXT DEFAULT NULL")
    }
}
