package com.jinatra.wishplay.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.jinatra.wishplay.data.local.db.WishPlayDatabase
import com.jinatra.wishplay.data.local.entity.WishlistItemEntity
import com.jinatra.wishplay.data.local.prefs.UserPreferences
import com.jinatra.wishplay.domain.model.WishlistBackup
import com.jinatra.wishplay.domain.model.WishlistBackupCodec
import com.jinatra.wishplay.widget.NextDropWidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class BackupPreview(val backup: WishlistBackup, val newCount: Int, val duplicateCount: Int)
data class BackupImportResult(val imported: Int, val skipped: Int, val remindersPending: Boolean = false)

@Singleton
class BackupRepository @Inject constructor(
    private val database: WishPlayDatabase,
    private val wishlistRepository: WishlistRepository,
    private val preferences: UserPreferences,
    @ApplicationContext private val context: Context
) {
    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val games = database.wishlistDao().getWishlistSync().map { it.toDomainModel() }.sortedBy { it.orderIndex }
        val contents = WishlistBackupCodec.encode(games)
        require(contents.toByteArray(Charsets.UTF_8).size <= WishlistBackupCodec.MAX_BYTES) { "This wishlist is too large for a single backup." }
        val output = context.contentResolver.openOutputStream(uri, "wt") ?: error("Couldn't open the selected destination.")
        output.bufferedWriter(Charsets.UTF_8).use { it.write(contents) }
        games.size
    }

    suspend fun preview(uri: Uri): BackupPreview = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: error("Couldn't open the selected file.")
        val bytes = input.use { it.readBytesBounded() }
        preview(WishlistBackupCodec.decode(bytes.toString(Charsets.UTF_8)))
    }

    internal suspend fun preview(backup: WishlistBackup): BackupPreview {
        val ids = database.wishlistDao().getWishlistSync().map { it.game.id }.toSet()
        val added = backup.games.count { it.id !in ids }
        return BackupPreview(backup, added, backup.games.size - added)
    }

    suspend fun importBackup(backup: WishlistBackup): BackupImportResult {
        val role = preferences.userRole.first()
        val dao = database.wishlistDao()
        val imported = database.withTransaction {
            val ids = dao.getWishlistSync().map { it.game.id }.toSet()
            val additions = backup.games.filter { it.id !in ids }
            require(role in setOf("pro", "admin") || ids.size + additions.size <= WishlistRepository.FREE_CAP) {
                "This import would exceed the 25-game free wishlist limit. No games were imported."
            }
            val now = System.currentTimeMillis()
            val order = dao.getMaxOrderIndex() ?: 0
            additions.forEachIndexed { index, game ->
                dao.insertGame(game.toEntity())
                dao.insertWishlistItem(WishlistItemEntity(gameId = game.id, alertEnabled = game.alertEnabled,
                    priceAlertEnabled = game.priceAlertEnabled, orderIndex = order + index + 1,
                    addedAt = now, updatedAt = now, syncStatus = if (role == "guest") "LOCAL" else "PENDING_INSERT",
                    trackedPlatformsJson = game.trackedPlatforms?.let { Json.encodeToString(it) }))
            }
            additions.map { it.id }
        }
        // Commit the complete merge before creating any background work.
        var remindersPending = false
        imported.forEach {
            try { wishlistRepository.scheduleAlertsIfAvailable(it) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { remindersPending = true }
        }
        NextDropWidgetUpdater.updateWidget(context)
        return BackupImportResult(imported.size, backup.games.size - imported.size, remindersPending)
    }

    private fun java.io.InputStream.readBytesBounded(): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            require(output.size() + count <= WishlistBackupCodec.MAX_BYTES) { "Backup files must be smaller than 5 MB." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
