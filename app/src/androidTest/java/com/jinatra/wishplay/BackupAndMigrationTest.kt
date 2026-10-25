package com.jinatra.wishplay

import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.jinatra.wishplay.data.local.db.*
import com.jinatra.wishplay.data.local.prefs.UserPreferences
import com.jinatra.wishplay.data.repository.*
import com.jinatra.wishplay.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class BackupAndMigrationTest {
    @Test fun mergeIsAtomicIdempotentAndPreservesLocalEntries() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WishPlayDatabase::class.java).build()
        val file = File(context.cacheDir, "backup-roundtrip-test.json")
        try {
            val prefs = UserPreferences(context)
            prefs.setUserRole("guest")
            val wishlist = WishlistRepository(database.wishlistDao())
            val backups = BackupRepository(database, wishlist, prefs, context)
            val existing = Game("c_existing", "Local title", platforms = mapOf("PC" to null))
            wishlist.saveGame(existing)
            wishlist.addGameToWishlist(existing.id).getOrThrow()
            val incoming = Game("c_new", "Imported", isCustom = true,
                platforms = mapOf("PC" to "2027-03-01", "PS5" to "2027-04-01"), trackedPlatforms = setOf("PS5"),
                customNotes = "Preserve me", alertEnabled = false)
            val backup = WishlistBackup(exportedAt = "now", games = listOf(existing.copy(title = "Remote overwrite"), incoming))
            assertEquals(BackupImportResult(1, 1), backups.importBackup(backup))
            assertEquals("Local title", database.wishlistDao().getGameById(existing.id)!!.game.title)
            val imported = database.wishlistDao().getGameById(incoming.id)!!.toDomainModel()
            assertEquals(setOf("PS5"), imported.trackedPlatforms)
            assertFalse(imported.alertEnabled)
            assertEquals("Preserve me", imported.customNotes)
            assertEquals(BackupImportResult(0, 2), backups.importBackup(backup))
            val oversized = backup.copy(games = (1..25).map { incoming.copy(id = "c_many_$it") })
            assertTrue(runCatching { backups.importBackup(oversized) }.isFailure)
            assertEquals(2, database.wishlistDao().getWishlistCount())
            assertEquals(2, backups.export(Uri.fromFile(file)))
            val preview = backups.preview(Uri.fromFile(file))
            assertEquals(0, preview.newCount)
            assertEquals(2, preview.duplicateCount)
        } finally { database.close(); file.delete() }
    }

    @Test fun versionFourWishlistSurvivesMigration() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-4-5-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL("CREATE TABLE games (id TEXT NOT NULL PRIMARY KEY,title TEXT NOT NULL,developer TEXT,hueHex TEXT NOT NULL,shapeKey TEXT NOT NULL,platformsJson TEXT NOT NULL,about TEXT,isCustom INTEGER NOT NULL,customNotes TEXT,hasTrailer INTEGER NOT NULL,coverUrl TEXT,logoUrl TEXT,trailerYoutubeId TEXT,requirementsJson TEXT,priceJson TEXT,movedFromDate TEXT,expectedYear TEXT,storageSizesJson TEXT NOT NULL,progress REAL NOT NULL,igdbId INTEGER)")
            db.execSQL("CREATE TABLE wishlist (gameId TEXT NOT NULL PRIMARY KEY,alertEnabled INTEGER NOT NULL,priceAlertEnabled INTEGER NOT NULL,orderIndex INTEGER NOT NULL,addedAt INTEGER NOT NULL,updatedAt INTEGER NOT NULL,deletedAt INTEGER,syncStatus TEXT NOT NULL,FOREIGN KEY(gameId) REFERENCES games(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX index_wishlist_gameId ON wishlist(gameId)")
            db.execSQL("CREATE TABLE scheduled_alerts (id TEXT NOT NULL PRIMARY KEY,gameId TEXT NOT NULL,alertType TEXT NOT NULL,targetTimeMillis INTEGER NOT NULL,isFired INTEGER NOT NULL,createdAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE recent_alerts (id TEXT NOT NULL PRIMARY KEY,gameId TEXT NOT NULL,type TEXT NOT NULL,title TEXT NOT NULL,subtitle TEXT NOT NULL,timestamp INTEGER NOT NULL,isRead INTEGER NOT NULL)")
            db.execSQL("INSERT INTO games(id,title,hueHex,shapeKey,platformsJson,isCustom,hasTrailer,storageSizesJson,progress) VALUES('legacy','Keep me','#1F7A6E','c9','{\"PC\":null}',0,0,'{}',0.5)")
            db.execSQL("INSERT INTO wishlist VALUES('legacy',0,0,7,1,1,NULL,'LOCAL')")
            db.version = 4
        }
        val migrated = Room.databaseBuilder(context, WishPlayDatabase::class.java, name).addMigrations(MIGRATION_4_5).build()
        try {
            val item = migrated.wishlistDao().getGameById("legacy")!!
            assertEquals("Keep me", item.game.title)
            assertEquals(7, item.wishlistItem!!.orderIndex)
            assertFalse(item.wishlistItem!!.alertEnabled)
            assertNull(item.wishlistItem!!.trackedPlatformsJson)
            assertEquals("{}", item.game.releaseEvidenceJson)
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}
