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
        val count = wishlistDao.getWishlistCount()
        if (count > 0) return

        // Seed sample games from prototype
        val seedGames = listOf(
            GameEntity(
                id = "pk",
                title = "Pocket Kingdoms",
                developer = "Lanternfish Studio",
                hueHex = "#1F7A6E",
                shapeKey = "c9",
                platformsJson = JSONObject().apply {
                    put("Android", "2026-10-02")
                    put("iOS", "2026-10-02")
                }.toString(),
                about = "Build a tiny kingdom and defend it from pocket-sized dragons. Short sessions, deep strategy.",
                isCustom = false,
                hasTrailer = true,
                storageSizesJson = JSONObject().apply {
                    put("Android", "1.8 GB")
                    put("iOS", "2.1 GB")
                }.toString(),
                progress = 0.94f
            ),
            GameEntity(
                id = "nd",
                title = "Neon Drift 2",
                developer = "Velocity Forge",
                hueHex = "#B8325F",
                shapeKey = "c4",
                platformsJson = JSONObject().apply {
                    put("PC", "2026-10-09")
                    put("Android", "2026-10-09")
                    put("iOS", "2026-10-23")
                }.toString(),
                about = "Arcade street racing across a city that rebuilds itself every night.",
                isCustom = false,
                hasTrailer = true,
                storageSizesJson = JSONObject().apply {
                    put("PC", "38 GB")
                    put("Android", "3.4 GB")
                }.toString(),
                progress = 0.82f
            ),
            GameEntity(
                id = "sf",
                title = "Starfall Odyssey",
                developer = "Northlight Interactive",
                hueHex = "#3A48B8",
                shapeKey = "c12",
                platformsJson = JSONObject().apply {
                    put("PC", "2026-11-13")
                    put("PS5", "2026-11-13")
                    put("Xbox Series", "2026-11-13")
                }.toString(),
                about = "A space opera RPG where every star on the map is a place you can land.",
                isCustom = false,
                hasTrailer = true,
                storageSizesJson = JSONObject().apply {
                    put("PC", "120 GB")
                    put("PS5", "98 GB")
                }.toString(),
                progress = 0.61f
            ),
            GameEntity(
                id = "ic",
                title = "Iron Circuit: Rebellion",
                developer = "Brassworks",
                hueHex = "#A3441F",
                shapeKey = "sq",
                platformsJson = JSONObject().apply {
                    put("PS5", "2026-12-03")
                    put("Xbox Series", "2026-12-03")
                    put("PC", "2026-12-03")
                }.toString(),
                about = "Squad-based mech tactics. Rebuild your machines between missions from what you salvage.",
                isCustom = false,
                hasTrailer = true,
                movedFromDate = "2026-10-22",
                progress = 0.45f
            ),
            GameEntity(
                id = "hk",
                title = "Hollow Keep",
                developer = "Mossgate Games",
                hueHex = "#4A6630",
                shapeKey = "c6",
                platformsJson = JSONObject().apply {
                    put("Switch 2", JSONObject.NULL)
                    put("PC", JSONObject.NULL)
                }.toString(),
                expectedYear = "2027",
                about = "A hand-drawn castle crawler. The developer has shown one short teaser so far.",
                isCustom = false,
                hasTrailer = false,
                progress = 0.20f
            ),
            GameEntity(
                id = "tb",
                title = "Tidebound",
                developer = "Saltwater Collective",
                hueHex = "#17618F",
                shapeKey = "circle",
                platformsJson = JSONObject().apply {
                    put("Android", "2026-09-18")
                }.toString(),
                about = "Sail between drowned islands and trade stories for supplies.",
                isCustom = false,
                hasTrailer = true,
                storageSizesJson = JSONObject().apply {
                    put("Android", "900 MB")
                }.toString(),
                progress = 1.0f
            )
        )

        wishlistDao.insertGames(seedGames)

        // Add them to wishlist by default to match prototype state
        seedGames.forEachIndexed { index, gameEntity ->
            wishlistDao.insertWishlistItem(
                WishlistItemEntity(
                    gameId = gameEntity.id,
                    alertEnabled = gameEntity.id != "tb",
                    orderIndex = index,
                    addedAt = System.currentTimeMillis() - (seedGames.size - index) * 60000L
                )
            )
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
