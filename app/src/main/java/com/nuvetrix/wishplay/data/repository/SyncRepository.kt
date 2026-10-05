package com.nuvetrix.wishplay.data.repository

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nuvetrix.wishplay.data.local.dao.WishlistDao
import com.nuvetrix.wishplay.data.local.entity.GameEntity
import com.nuvetrix.wishplay.data.local.entity.WishlistItemEntity
import com.nuvetrix.wishplay.data.local.prefs.UserPreferences
import com.nuvetrix.wishplay.domain.model.SyncState
import com.nuvetrix.wishplay.domain.model.SyncSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore data model for a wishlist item stored under:
 * /users/{uid}/wishlist/{gameId}
 */
data class CloudWishlistItem(
    val gameId: String = "",
    val igdbId: Long? = null,
    val customTitle: String? = null,
    val platformsJson: String = "{}",
    val notes: String? = null,
    val alertOn: Boolean = true,
    val priceAlertOn: Boolean = false,
    val orderIndex: Int = 0,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
) {
    /** Convert to Firestore map for set/merge operations. */
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "gameId" to gameId,
        "igdbId" to igdbId,
        "customTitle" to customTitle,
        "platformsJson" to platformsJson,
        "notes" to notes,
        "alertOn" to alertOn,
        "priceAlertOn" to priceAlertOn,
        "orderIndex" to orderIndex,
        "updatedAt" to updatedAt,
        "deletedAt" to deletedAt,
        "serverTs" to Timestamp.now()
    )

    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>, docId: String): CloudWishlistItem =
            CloudWishlistItem(
                gameId = docId,
                igdbId = (map["igdbId"] as? Long),
                customTitle = map["customTitle"] as? String,
                platformsJson = (map["platformsJson"] as? String) ?: "{}",
                notes = map["notes"] as? String,
                alertOn = (map["alertOn"] as? Boolean) ?: true,
                priceAlertOn = (map["priceAlertOn"] as? Boolean) ?: false,
                orderIndex = ((map["orderIndex"] as? Long) ?: 0L).toInt(),
                updatedAt = (map["updatedAt"] as? Long) ?: 0L,
                deletedAt = map["deletedAt"] as? Long
            )
    }
}

@Singleton
class SyncRepository @Inject constructor(
    private val wishlistDao: WishlistDao,
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) {
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: Flow<SyncState> = _syncState.asStateFlow()

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Two-way sync between local Room DB and Firestore.
     *
     * Push: any item with syncStatus != "SYNCED" is written to Firestore.
     * Pull: Firestore items updated after [lastSyncedTime] are reconciled locally.
     */
    suspend fun syncWishlist(forceFull: Boolean = false): Result<SyncSummary> =
        withContext(Dispatchers.IO) {
            val role = userPreferences.userRole.first()
            val uid = firebaseAuth.currentUser?.uid
                ?: userPreferences.userId.first()

            if (role == "guest" || uid.isNullOrBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Guest mode — cloud sync unavailable")
                )
            }

            _syncState.value = SyncState.SYNCING
            userPreferences.setSyncState("SYNCING")

            try {
                val lastSynced = if (forceFull) 0L else userPreferences.lastSyncedTime.first()
                val wishlistRef = firestore
                    .collection("users")
                    .document(uid)
                    .collection("wishlist")

                // ──────── 1. PUSH pending local items to Firestore ────────
                val pendingItems = wishlistDao.getPendingSyncItems()
                var pushedCount = 0

                for (item in pendingItems) {
                    val game = wishlistDao.getGameById(item.gameId)?.game
                    val cloud = CloudWishlistItem(
                        gameId = item.gameId,
                        igdbId = game?.igdbId,
                        customTitle = if (game?.isCustom == true) game.title else null,
                        platformsJson = game?.platformsJson ?: "{}",
                        notes = game?.customNotes,
                        alertOn = item.alertEnabled,
                        priceAlertOn = item.priceAlertEnabled,
                        orderIndex = item.orderIndex,
                        updatedAt = item.updatedAt,
                        deletedAt = item.deletedAt
                    )

                    wishlistRef.document(item.gameId)
                        .set(cloud.toFirestoreMap(), SetOptions.merge())
                        .await()

                    // Purge hard-deleted items locally after server confirms
                    if (item.deletedAt != null) {
                        wishlistDao.purgeDeletedItem(item.gameId)
                    } else {
                        wishlistDao.updateSyncStatus(item.gameId, "SYNCED")
                    }
                    pushedCount++
                }

                // ──────── 2. PULL cloud items updated since lastSynced ────────
                val query = if (lastSynced > 0L) {
                    wishlistRef.whereGreaterThanOrEqualTo("updatedAt", lastSynced)
                } else {
                    wishlistRef
                }

                val snapshot = query.get().await()
                var pulledCount = 0
                var deletedCount = 0

                for (doc in snapshot.documents) {
                    val remoteItem = CloudWishlistItem.fromFirestoreMap(
                        doc.data ?: continue, doc.id
                    )

                    if (remoteItem.deletedAt != null) {
                        wishlistDao.removeWishlistItem(remoteItem.gameId)
                        deletedCount++
                        continue
                    }

                    // Ensure game entity exists locally
                    val localGame = wishlistDao.getGameById(remoteItem.gameId)
                    if (localGame == null) {
                        val isCustom = remoteItem.customTitle != null
                        val newGame = GameEntity(
                            id = remoteItem.gameId,
                            title = remoteItem.customTitle
                                ?: "Game ${remoteItem.gameId.uppercase()}",
                            developer = if (isCustom) null else "WishPlay Cloud",
                            hueHex = if (isCustom) "#8A5A00" else "#1F7A6E",
                            shapeKey = if (isCustom) "c9" else "c12",
                            platformsJson = remoteItem.platformsJson,
                            about = if (isCustom) "Custom game synced from cloud."
                            else "Synced from your WishPlay account.",
                            isCustom = isCustom,
                            customNotes = remoteItem.notes,
                            igdbId = remoteItem.igdbId
                        )
                        wishlistDao.insertGame(newGame)
                    }

                    wishlistDao.insertWishlistItem(
                        WishlistItemEntity(
                            gameId = remoteItem.gameId,
                            alertEnabled = remoteItem.alertOn,
                            priceAlertEnabled = remoteItem.priceAlertOn,
                            orderIndex = remoteItem.orderIndex,
                            addedAt = remoteItem.updatedAt,
                            updatedAt = remoteItem.updatedAt,
                            deletedAt = null,
                            syncStatus = "SYNCED"
                        )
                    )
                    pulledCount++
                }

                val now = System.currentTimeMillis()
                userPreferences.setLastSyncedTime(now)
                userPreferences.setSyncState("IDLE")
                _syncState.value = SyncState.SUCCESS
                com.nuvetrix.wishplay.widget.NextDropWidgetUpdater.updateWidget(context)

                Result.success(
                    SyncSummary(
                        pushedCount = pushedCount,
                        pulledCount = pulledCount,
                        deletedCount = deletedCount,
                        lastSyncedMillis = now
                    )
                )
            } catch (e: Exception) {
                userPreferences.setSyncState("ERROR")
                _syncState.value = SyncState.ERROR
                Result.failure(e)
            }
        }

    /**
     * Called when a guest user signs in for the first time.
     * Marks all local items as PENDING_INSERT then does a full two-way sync
     * so no games are lost on the transition from guest → account.
     */
    suspend fun mergeGuestListOnSignIn(uid: String): Result<SyncSummary> =
        withContext(Dispatchers.IO) {
            val allLocal = wishlistDao.getAllWishlistItems()
            for (item in allLocal) {
                if (item.deletedAt == null) {
                    wishlistDao.updateSyncStatus(item.gameId, "PENDING_INSERT")
                }
            }
            syncWishlist(forceFull = true)
        }
}
