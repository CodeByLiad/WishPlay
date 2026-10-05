package com.nuvetrix.wishplay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nuvetrix.wishplay.data.local.entity.GameEntity
import com.nuvetrix.wishplay.data.local.entity.GameWithWishlist
import com.nuvetrix.wishplay.data.local.entity.WishlistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    @Transaction
    @Query("SELECT * FROM games WHERE id IN (SELECT gameId FROM wishlist WHERE deletedAt IS NULL)")
    fun observeWishlist(): Flow<List<GameWithWishlist>>

    @Transaction
    @Query("SELECT * FROM games WHERE id IN (SELECT gameId FROM wishlist WHERE deletedAt IS NULL)")
    suspend fun getWishlistSync(): List<GameWithWishlist>

    @Transaction
    @Query("SELECT * FROM games")
    fun observeAllGames(): Flow<List<GameWithWishlist>>

    @Transaction
    @Query("SELECT * FROM games WHERE id = :id")
    fun observeGameById(id: String): Flow<GameWithWishlist?>

    @Transaction
    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: String): GameWithWishlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistItem(item: WishlistItemEntity)

    @Query("DELETE FROM wishlist WHERE gameId = :gameId")
    suspend fun removeWishlistItem(gameId: String)

    @Query("UPDATE wishlist SET deletedAt = :deletedAt, syncStatus = 'PENDING_DELETE', updatedAt = :deletedAt WHERE gameId = :gameId")
    suspend fun markItemDeleted(gameId: String, deletedAt: Long)

    @Query("DELETE FROM wishlist WHERE gameId = :gameId AND deletedAt IS NOT NULL")
    suspend fun purgeDeletedItem(gameId: String)

    @Query("UPDATE wishlist SET syncStatus = :status WHERE gameId = :gameId")
    suspend fun updateSyncStatus(gameId: String, status: String)

    @Query("SELECT * FROM wishlist WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncItems(): List<WishlistItemEntity>

    @Query("SELECT * FROM wishlist")
    suspend fun getAllWishlistItems(): List<WishlistItemEntity>

    @Query("SELECT * FROM wishlist WHERE gameId = :gameId LIMIT 1")
    suspend fun getWishlistItem(gameId: String): WishlistItemEntity?

    @Query("UPDATE wishlist SET alertEnabled = :alertEnabled, updatedAt = :updatedAt, syncStatus = 'PENDING_UPDATE' WHERE gameId = :gameId")
    suspend fun updateAlert(gameId: String, alertEnabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE wishlist SET priceAlertEnabled = :priceAlertEnabled, updatedAt = :updatedAt, syncStatus = 'PENDING_UPDATE' WHERE gameId = :gameId")
    suspend fun updatePriceAlert(gameId: String, priceAlertEnabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM wishlist WHERE deletedAt IS NULL")
    suspend fun getWishlistCount(): Int

    @Query("SELECT MAX(orderIndex) FROM wishlist WHERE deletedAt IS NULL")
    suspend fun getMaxOrderIndex(): Int?

    @Query("DELETE FROM wishlist")
    suspend fun clearAllWishlist()
}
