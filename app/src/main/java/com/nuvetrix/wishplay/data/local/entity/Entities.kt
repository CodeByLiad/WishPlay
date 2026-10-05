package com.nuvetrix.wishplay.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val title: String,
    val developer: String? = null,
    val hueHex: String = "#1F7A6E",
    val shapeKey: String = "c9",
    val platformsJson: String = "{}",
    val about: String? = null,
    val isCustom: Boolean = false,
    val customNotes: String? = null,
    val hasTrailer: Boolean = false,
    val coverUrl: String? = null,
    val logoUrl: String? = null,
    val trailerYoutubeId: String? = null,
    val requirementsJson: String? = null,
    val priceJson: String? = null,
    val movedFromDate: String? = null,
    val expectedYear: String? = null,
    val storageSizesJson: String = "{}",
    val progress: Float = 0.5f,
    val igdbId: Long? = null
)

@Entity(
    tableName = "wishlist",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gameId")]
)
data class WishlistItemEntity(
    @PrimaryKey val gameId: String,
    val alertEnabled: Boolean = true,
    val priceAlertEnabled: Boolean = false,
    val orderIndex: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "LOCAL"
)

data class GameWithWishlist(
    @Embedded val game: GameEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "gameId"
    )
    val wishlistItem: WishlistItemEntity?
)

@Entity(tableName = "scheduled_alerts")
data class ScheduledAlertEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val alertType: String, // "RELEASE" or "COUNTDOWN"
    val targetTimeMillis: Long,
    val isFired: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_alerts")
data class RecentAlertEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val type: String, // "DELAY", "PRICE_DROP", "RELEASE"
    val title: String,
    val subtitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
