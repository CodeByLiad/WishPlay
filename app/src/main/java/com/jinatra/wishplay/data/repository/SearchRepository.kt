package com.jinatra.wishplay.data.repository

import com.jinatra.wishplay.data.local.dao.WishlistDao
import com.jinatra.wishplay.data.local.prefs.UserPreferences
import com.jinatra.wishplay.data.remote.api.WishPlayApiService
import com.jinatra.wishplay.data.remote.dto.GameDto
import com.jinatra.wishplay.domain.model.Game
import com.jinatra.wishplay.domain.model.GamePrice
import com.jinatra.wishplay.domain.model.RequirementsLevel
import com.jinatra.wishplay.domain.model.SystemRequirements
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

data class SearchPageResult(
    val games: List<Game>,
    val hasMore: Boolean,
    val warnings: List<String>,
    val otherPlatformGames: List<Game>
)

@Singleton
class SearchRepository @Inject constructor(
    private val apiService: WishPlayApiService,
    private val wishlistRepository: WishlistRepository,
    private val wishlistDao: WishlistDao,
    private val userPreferences: UserPreferences
) {
    val popularQueries = listOf(
        "Minecraft",
        "Genshin Impact",
        "Stardew Valley",
        "Elden Ring",
        "Fortnite"
    )

    val recentSearches: Flow<List<String>> = userPreferences.recentSearches

    suspend fun addRecentSearch(query: String) {
        userPreferences.addRecentSearch(query)
    }

    suspend fun removeRecentSearch(query: String) {
        userPreferences.removeRecentSearch(query)
    }

    suspend fun clearRecentSearches() {
        userPreferences.clearRecentSearches()
    }

    suspend fun searchGames(query: String, platform: String): List<Game> {
        val dtos = apiService.searchGames(query, platform)
        return dtos.map { dto ->
            dto.toDomainModel(inWishlist = false)
        }
    }

    suspend fun searchGamesPage(query: String, platform: String, page: Int): SearchPageResult {
        val response = apiService.searchGamesPage(query, platform, page)
        return SearchPageResult(
            games = response.games.map { it.toDomainModel() },
            hasMore = response.hasMore,
            warnings = response.warnings,
            otherPlatformGames = response.otherPlatformGames.map { it.toDomainModel() }
        )
    }

    suspend fun addGameToWishlist(game: Game, isPro: Boolean = false): Result<Unit> {
        // 1. Save game entity to Room
        cacheSummary(game)
        try {
            apiService.getGameDetails(game.id)?.let { wishlistRepository.saveGame(it.toDomainModel()) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The cached summary can still be tracked offline and refreshed by the daily worker.
        }
        // 2. Add to wishlist
        return wishlistRepository.addGameToWishlist(game.id, isPro)
    }

    suspend fun cacheSummary(game: Game) {
        if (wishlistDao.getGameById(game.id) == null) wishlistRepository.saveGame(game)
    }
}

fun GameDto.toDomainModel(inWishlist: Boolean = false): Game {
    val req = requirements?.let { r ->
        SystemRequirements(
            min = r.min?.let { RequirementsLevel(it.os, it.cpu, it.gpu, it.ram) },
            rec = r.rec?.let { RequirementsLevel(it.os, it.cpu, it.gpu, it.ram) }
        )
    }

    val pr = price?.let {
        GamePrice(store = it.store, now = it.now, was = it.was)
    }

    return Game(
        id = id,
        title = title,
        developer = developer,
        hueHex = hueHex,
        shapeKey = shapeKey,
        coverUrl = coverUrl,
        logoUrl = logoUrl,
        platforms = platforms,
        storageSizes = storageSizes,
        about = about,
        isCustom = false,
        customNotes = null,
        hasTrailer = hasTrailer,
        trailerYoutubeId = trailerYoutubeId,
        requirements = req,
        price = pr,
        movedFromDate = movedFromDate,
        expectedYear = expectedYear,
        progress = progress,
        igdbId = igdbId,
        inWishlist = inWishlist,
        alertEnabled = true,
        priceAlertEnabled = false,
        releaseEvidence = releaseEvidence
    )
}
