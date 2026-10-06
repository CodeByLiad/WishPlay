package com.nuvetrix.wishplay.data.repository

import android.content.Context
import com.nuvetrix.wishplay.alerts.AlertScheduler
import com.nuvetrix.wishplay.data.local.dao.WishlistDao
import com.nuvetrix.wishplay.data.local.entity.GameEntity
import com.nuvetrix.wishplay.data.local.entity.GameWithWishlist
import com.nuvetrix.wishplay.data.local.entity.WishlistItemEntity
import com.nuvetrix.wishplay.data.local.prefs.UserPreferences
import com.nuvetrix.wishplay.domain.model.Game
import com.nuvetrix.wishplay.widget.NextDropWidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.UUID

class WishlistRepository(
    private val wishlistDao: WishlistDao,
    private val alertScheduler: AlertScheduler? = null,
    private val userPreferences: UserPreferences? = null,
    private val context: Context? = null
) {
    companion object {
        const val FREE_CAP = 25
    }

    val wishlistGames: Flow<List<Game>> = wishlistDao.observeWishlist().map { list ->
        list.map { it.toDomainModel() }
    }

    val allGames: Flow<List<Game>> = wishlistDao.observeAllGames().map { list ->
        list.map { it.toDomainModel() }
    }

    fun observeGameById(id: String): Flow<Game?> = wishlistDao.observeGameById(id).map {
        it?.toDomainModel()
    }

    suspend fun addGameToWishlist(gameId: String, isPro: Boolean = false): Result<Unit> {
        val count = wishlistDao.getWishlistCount()
        if (!isPro && count >= FREE_CAP) {
            return Result.failure(IllegalStateException("CAP_REACHED"))
        }

        val maxOrder = wishlistDao.getMaxOrderIndex() ?: 0
        val role = userPreferences?.userRole?.first() ?: "guest"
        val status = if (role == "guest") "LOCAL" else "PENDING_INSERT"
        val now = System.currentTimeMillis()
        wishlistDao.insertWishlistItem(
            WishlistItemEntity(
                gameId = gameId,
                alertEnabled = true,
                orderIndex = maxOrder + 1,
                addedAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = status
            )
        )

        scheduleAlertsIfAvailable(gameId)
        context?.let { NextDropWidgetUpdater.updateWidget(it) }
        return Result.success(Unit)
    }

    suspend fun removeGameFromWishlist(gameId: String) {
        val role = userPreferences?.userRole?.first() ?: "guest"
        if (role == "guest") {
            wishlistDao.removeWishlistItem(gameId)
        } else {
            // Soft delete for two-way cloud sync
            wishlistDao.markItemDeleted(gameId, System.currentTimeMillis())
        }
        alertScheduler?.cancelAlertsForGame(gameId)
        context?.let { NextDropWidgetUpdater.updateWidget(it) }
    }

    suspend fun toggleAlert(gameId: String, alertEnabled: Boolean) {
        wishlistDao.updateAlert(gameId, alertEnabled)
        if (alertEnabled) {
            scheduleAlertsIfAvailable(gameId)
        } else {
            alertScheduler?.cancelAlertsForGame(gameId)
        }
    }

    private suspend fun scheduleAlertsIfAvailable(gameId: String) {
        if (alertScheduler == null) return
        val game = wishlistDao.getGameById(gameId)?.toDomainModel() ?: return
        val lead = userPreferences?.reminderLead?.first() ?: "1 week"
        val alertRelease = userPreferences?.alertRelease?.first() ?: true
        val alertCountdown = userPreferences?.alertCountdown?.first() ?: true
        alertScheduler.scheduleAlertsForGame(
            game = game,
            leadPreference = lead,
            alertReleaseEnabled = alertRelease,
            alertCountdownEnabled = alertCountdown
        )
    }

    suspend fun saveGame(game: Game) {
        val platMap = JSONObject()
        game.platforms.forEach { (p, d) -> platMap.put(p, d ?: JSONObject.NULL) }

        val sizeMap = JSONObject()
        game.storageSizes.forEach { (p, s) -> sizeMap.put(p, s ?: JSONObject.NULL) }

        val reqJson = game.requirements?.let { req ->
            JSONObject().apply {
                req.min?.let { m ->
                    put("min", JSONObject().apply {
                        put("OS", m.os)
                        put("CPU", m.cpu)
                        put("GPU", m.gpu)
                        put("RAM", m.ram)
                    })
                }
                req.rec?.let { r ->
                    put("rec", JSONObject().apply {
                        put("OS", r.os)
                        put("CPU", r.cpu)
                        put("GPU", r.gpu)
                        put("RAM", r.ram)
                    })
                }
            }.toString()
        }

        val priceJson = game.price?.let { pr ->
            JSONObject().apply {
                put("store", pr.store)
                put("now", pr.now)
                put("was", pr.was ?: JSONObject.NULL)
            }.toString()
        }

        val entity = GameEntity(
            id = game.id,
            title = game.title,
            developer = game.developer,
            hueHex = game.hueHex,
            shapeKey = game.shapeKey,
            coverUrl = game.coverUrl,
            logoUrl = game.logoUrl,
            platformsJson = platMap.toString(),
            about = game.about,
            isCustom = game.isCustom,
            customNotes = game.customNotes,
            hasTrailer = game.hasTrailer,
            trailerYoutubeId = game.trailerYoutubeId,
            requirementsJson = reqJson,
            priceJson = priceJson,
            movedFromDate = game.movedFromDate,
            expectedYear = game.expectedYear,
            storageSizesJson = sizeMap.toString(),
            progress = game.progress,
            igdbId = game.igdbId
        )
        wishlistDao.insertGame(entity)
        context?.let { NextDropWidgetUpdater.updateWidget(it) }
    }

    suspend fun addCustomGame(
        title: String,
        platforms: List<String>,
        date: String?,
        notes: String?,
        isPro: Boolean = false
    ): Result<Game> {
        val count = wishlistDao.getWishlistCount()
        if (!isPro && count >= FREE_CAP) {
            return Result.failure(IllegalStateException("CAP_REACHED"))
        }

        val customId = "c" + System.currentTimeMillis()
        val platMap = JSONObject()
        val defaultPlatforms = if (platforms.isEmpty()) listOf("Android") else platforms
        defaultPlatforms.forEach { p ->
            platMap.put(p, date ?: JSONObject.NULL)
        }

        val hues = listOf("#1F7A6E", "#B8325F", "#3A48B8", "#A3441F", "#5B3FA8", "#0F6A80")
        val randomHue = hues.random()

        val entity = GameEntity(
            id = customId,
            title = title,
            developer = null,
            hueHex = randomHue,
            shapeKey = "c9",
            platformsJson = platMap.toString(),
            about = null,
            isCustom = true,
            customNotes = notes,
            hasTrailer = false,
            movedFromDate = null,
            expectedYear = null,
            storageSizesJson = "{}",
            progress = 0.2f
        )

        wishlistDao.insertGame(entity)

        val maxOrder = wishlistDao.getMaxOrderIndex() ?: 0
        val role = userPreferences?.userRole?.first() ?: "guest"
        val status = if (role == "guest") "LOCAL" else "PENDING_INSERT"
        val now = System.currentTimeMillis()
        wishlistDao.insertWishlistItem(
            WishlistItemEntity(
                gameId = customId,
                alertEnabled = true,
                orderIndex = maxOrder + 1,
                addedAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = status
            )
        )

        val game = wishlistDao.getGameById(customId)?.toDomainModel()
            ?: return Result.failure(IllegalStateException("Failed to load saved custom game"))

        scheduleAlertsIfAvailable(customId)
        context?.let { NextDropWidgetUpdater.updateWidget(it) }
        return Result.success(game)
    }

    suspend fun seedInitialDataIfEmpty() {
        // Disabled per user request: Provides a clean experience for real users.
        // New installs start with an empty wishlist and show the "Find games to track" empty state.
        return
    }

    suspend fun clearSampleGames() {
        val sampleIds = listOf("pk", "nd", "sf", "ic", "hk", "tb")
        sampleIds.forEach { id ->
            wishlistDao.removeWishlistItem(id)
        }
    }
}

private fun GameWithWishlist.toDomainModel(): Game {
    val platformsMap = mutableMapOf<String, String?>()
    try {
        val json = JSONObject(game.platformsJson)
        json.keys().forEach { key ->
            platformsMap[key] = if (json.isNull(key)) null else json.getString(key)
        }
    } catch (_: Exception) {}

    val sizesMap = mutableMapOf<String, String?>()
    try {
        val json = JSONObject(game.storageSizesJson)
        json.keys().forEach { key ->
            sizesMap[key] = if (json.isNull(key)) null else json.getString(key)
        }
    } catch (_: Exception) {}

    var requirements: com.nuvetrix.wishplay.domain.model.SystemRequirements? = null
    game.requirementsJson?.let { rj ->
        try {
            val json = JSONObject(rj)
            val minObj = if (json.has("min") && !json.isNull("min")) json.getJSONObject("min") else null
            val recObj = if (json.has("rec") && !json.isNull("rec")) json.getJSONObject("rec") else null

            val minLevel = minObj?.let {
                com.nuvetrix.wishplay.domain.model.RequirementsLevel(
                    os = if (it.has("OS") && !it.isNull("OS")) it.getString("OS") else null,
                    cpu = if (it.has("CPU") && !it.isNull("CPU")) it.getString("CPU") else null,
                    gpu = if (it.has("GPU") && !it.isNull("GPU")) it.getString("GPU") else null,
                    ram = if (it.has("RAM") && !it.isNull("RAM")) it.getString("RAM") else null
                )
            }
            val recLevel = recObj?.let {
                com.nuvetrix.wishplay.domain.model.RequirementsLevel(
                    os = if (it.has("OS") && !it.isNull("OS")) it.getString("OS") else null,
                    cpu = if (it.has("CPU") && !it.isNull("CPU")) it.getString("CPU") else null,
                    gpu = if (it.has("GPU") && !it.isNull("GPU")) it.getString("GPU") else null,
                    ram = if (it.has("RAM") && !it.isNull("RAM")) it.getString("RAM") else null
                )
            }
            requirements = com.nuvetrix.wishplay.domain.model.SystemRequirements(min = minLevel, rec = recLevel)
        } catch (_: Exception) {}
    }

    var price: com.nuvetrix.wishplay.domain.model.GamePrice? = null
    game.priceJson?.let { pj ->
        try {
            val json = JSONObject(pj)
            price = com.nuvetrix.wishplay.domain.model.GamePrice(
                store = json.getString("store"),
                now = json.getString("now"),
                was = if (json.has("was") && !json.isNull("was")) json.getString("was") else null
            )
        } catch (_: Exception) {}
    }

    return Game(
        id = game.id,
        title = game.title,
        developer = game.developer,
        hueHex = game.hueHex,
        shapeKey = game.shapeKey,
        coverUrl = game.coverUrl,
        logoUrl = game.logoUrl,
        platforms = platformsMap,
        about = game.about,
        isCustom = game.isCustom,
        customNotes = game.customNotes,
        hasTrailer = game.hasTrailer,
        trailerYoutubeId = game.trailerYoutubeId,
        requirements = requirements,
        price = price,
        movedFromDate = game.movedFromDate,
        expectedYear = game.expectedYear,
        storageSizes = sizesMap,
        progress = game.progress,
        igdbId = game.igdbId,
        inWishlist = wishlistItem != null,
        alertEnabled = wishlistItem?.alertEnabled ?: true,
        priceAlertEnabled = wishlistItem?.priceAlertEnabled ?: false,
        orderIndex = wishlistItem?.orderIndex ?: 0
    )
}
