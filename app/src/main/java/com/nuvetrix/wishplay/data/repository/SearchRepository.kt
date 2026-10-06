package com.nuvetrix.wishplay.data.repository

import com.nuvetrix.wishplay.data.local.dao.WishlistDao
import com.nuvetrix.wishplay.data.local.prefs.UserPreferences
import com.nuvetrix.wishplay.data.remote.api.WishPlayApiService
import com.nuvetrix.wishplay.data.remote.dto.GameDto
import com.nuvetrix.wishplay.domain.model.Game
import com.nuvetrix.wishplay.domain.model.GamePrice
import com.nuvetrix.wishplay.domain.model.RequirementsLevel
import com.nuvetrix.wishplay.domain.model.SystemRequirements
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val apiService: WishPlayApiService,
    private val wishlistRepository: WishlistRepository,
    private val wishlistDao: WishlistDao,
    private val userPreferences: UserPreferences
) {
    val popularQueries = listOf(
        "Starfall Odyssey",
        "Neon Drift 2",
        "Hollow Keep",
        "Iron Circuit",
        "Lumen Tactics"
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

    suspend fun addGameToWishlist(game: Game, isPro: Boolean = false): Result<Unit> {
        // 1. Save game entity to Room
        wishlistRepository.saveGame(game)
        // 2. Add to wishlist
        return wishlistRepository.addGameToWishlist(game.id, isPro)
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
        priceAlertEnabled = false
    )
}
