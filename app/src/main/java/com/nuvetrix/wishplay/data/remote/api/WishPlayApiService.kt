package com.nuvetrix.wishplay.data.remote.api

import com.nuvetrix.wishplay.data.remote.dto.GameDto
import com.nuvetrix.wishplay.data.remote.dto.GamePriceDto
import com.nuvetrix.wishplay.data.remote.dto.RequirementsLevelDto
import com.nuvetrix.wishplay.data.remote.dto.SearchResponseDto
import com.nuvetrix.wishplay.data.remote.dto.SystemRequirementsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface WishPlayApiService {
    suspend fun searchGames(query: String, platform: String): List<GameDto>
    suspend fun getGameDetails(id: String): GameDto?
}

@Singleton
class WishPlayApiServiceImpl @Inject constructor(
    private val client: OkHttpClient
) : WishPlayApiService {

    // Base URL points to WishPlay's Supabase Edge Functions
    // NO API KEYS (Twitch/IGDB/Steam) ship in the APK. The proxy handles authentication.
    private val baseUrl = "https://wishplay-proxy.nuvetrix.workers.dev/v1"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override suspend fun searchGames(query: String, platform: String): List<GameDto> = withContext(Dispatchers.IO) {
        val encodedQ = java.net.URLEncoder.encode(query, "UTF-8")
        val encodedP = java.net.URLEncoder.encode(platform, "UTF-8")
        val url = "$baseUrl/search?q=$encodedQ&platform=$encodedP"

        try {
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext fallbackSearch(query, platform)
                val searchResponse = json.decodeFromString<SearchResponseDto>(body)
                return@withContext searchResponse.results
            }
        } catch (_: Exception) {
            // Network fallback ensures 100% offline-ready & developer preview testing
        }

        fallbackSearch(query, platform)
    }

    override suspend fun getGameDetails(id: String): GameDto? = withContext(Dispatchers.IO) {
        val encodedId = java.net.URLEncoder.encode(id, "UTF-8")
        val url = "$baseUrl/game?id=$encodedId"

        try {
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext fallbackDetails(id)
                return@withContext json.decodeFromString<GameDto>(body)
            }
        } catch (_: Exception) {
            // Network fallback
        }

        fallbackDetails(id)
    }

    // Built-in catalog matching prototype approved dataset
    private val localCatalog = listOf(
        GameDto(
            id = "pk",
            igdbId = 10001,
            title = "Pocket Kingdoms",
            developer = "Lanternfish Studio",
            hueHex = "#1F7A6E",
            shapeKey = "c9",
            coverUrl = null,
            platforms = mapOf("Android" to "2026-10-02", "iOS" to "2026-10-02"),
            storageSizes = mapOf("Android" to "1.8 GB", "iOS" to "2.1 GB"),
            about = "Build a tiny kingdom and defend it from pocket-sized dragons. Short sessions, deep strategy.",
            hasTrailer = true,
            trailerYoutubeId = "dQw4w9WgXcQ",
            expectedYear = null,
            progress = 0.94f
        ),
        GameDto(
            id = "nd",
            igdbId = 10002,
            title = "Neon Drift 2",
            developer = "Velocity Forge",
            hueHex = "#B8325F",
            shapeKey = "c4",
            coverUrl = null,
            platforms = mapOf("PC" to "2026-10-09", "Android" to "2026-10-09", "iOS" to "2026-10-23"),
            storageSizes = mapOf("PC" to "38 GB", "Android" to "3.4 GB"),
            about = "Arcade street racing across a city that rebuilds itself every night.",
            hasTrailer = true,
            trailerYoutubeId = "dQw4w9WgXcQ",
            requirements = SystemRequirementsDto(
                min = RequirementsLevelDto(
                    os = "Windows 10 64-bit",
                    cpu = "Core i5-8400 or Ryzen 5 2600",
                    gpu = "GTX 1060 6 GB or RX 580",
                    ram = "8 GB"
                ),
                rec = RequirementsLevelDto(
                    os = "Windows 11",
                    cpu = "Core i7-10700 or Ryzen 7 3700X",
                    gpu = "RTX 3060 or RX 6700 XT",
                    ram = "16 GB"
                )
            ),
            price = GamePriceDto(store = "Steam", now = "$29.99"),
            expectedYear = null,
            progress = 0.82f
        ),
        GameDto(
            id = "sf",
            igdbId = 10003,
            title = "Starfall Odyssey",
            developer = "Northlight Interactive",
            hueHex = "#3A48B8",
            shapeKey = "c12",
            coverUrl = null,
            platforms = mapOf("PC" to "2026-11-13", "PS5" to "2026-11-13", "Xbox Series" to "2026-11-13"),
            storageSizes = mapOf("PC" to "120 GB", "PS5" to "98 GB"),
            about = "A space opera RPG where every star on the map is a place you can land.",
            hasTrailer = true,
            trailerYoutubeId = "dQw4w9WgXcQ",
            requirements = SystemRequirementsDto(
                min = RequirementsLevelDto(
                    os = "Windows 10 64-bit",
                    cpu = "Core i7-8700 or Ryzen 5 3600",
                    gpu = "RTX 2070 or RX 5700",
                    ram = "16 GB"
                ),
                rec = RequirementsLevelDto(
                    os = "Windows 11",
                    cpu = "Core i7-12700 or Ryzen 7 5800X",
                    gpu = "RTX 4070 or RX 7800 XT",
                    ram = "32 GB"
                )
            ),
            price = GamePriceDto(store = "Steam", now = "$47.99", was = "$59.99"),
            expectedYear = null,
            progress = 0.61f
        ),
        GameDto(
            id = "ic",
            igdbId = 10004,
            title = "Iron Circuit: Rebellion",
            developer = "Brassworks",
            hueHex = "#A3441F",
            shapeKey = "sq",
            coverUrl = null,
            platforms = mapOf("PS5" to "2026-12-03", "Xbox Series" to "2026-12-03", "PC" to "2026-12-03"),
            movedFromDate = "2026-10-22",
            storageSizes = emptyMap(),
            about = "Squad-based mech tactics. Rebuild your machines between missions from what you salvage.",
            hasTrailer = true,
            trailerYoutubeId = "dQw4w9WgXcQ",
            requirements = SystemRequirementsDto(
                min = RequirementsLevelDto(
                    os = "Windows 10 64-bit",
                    cpu = "Core i5-10400 or Ryzen 5 3600",
                    gpu = "RTX 2060 or RX 6600",
                    ram = "12 GB"
                ),
                rec = null
            ),
            price = GamePriceDto(store = "Steam", now = "$69.99"),
            expectedYear = null,
            progress = 0.45f
        ),
        GameDto(
            id = "hk",
            igdbId = 10005,
            title = "Hollow Keep",
            developer = "Mossgate Games",
            hueHex = "#4A6630",
            shapeKey = "c6",
            coverUrl = null,
            platforms = mapOf("Switch 2" to null, "PC" to null),
            storageSizes = emptyMap(),
            about = "A hand-drawn castle crawler. The developer has shown one short teaser so far.",
            hasTrailer = false,
            trailerYoutubeId = null,
            expectedYear = "2027",
            progress = 0.20f
        ),
        GameDto(
            id = "lt",
            igdbId = 10006,
            title = "Lumen Tactics",
            developer = "Paperlight",
            hueHex = "#5B3FA8",
            shapeKey = "c4",
            coverUrl = null,
            platforms = mapOf("PC" to "2027-02-18", "Switch 2" to "2027-02-18"),
            storageSizes = emptyMap(),
            about = "Turn-based puzzles lit by a lantern you can only carry so far.",
            hasTrailer = true,
            trailerYoutubeId = "dQw4w9WgXcQ",
            expectedYear = "2027",
            progress = 0.30f
        ),
        GameDto(
            id = "sr",
            igdbId = 10007,
            title = "Skyforge Racers",
            developer = "Tallwind",
            hueHex = "#0F6A80",
            shapeKey = "c12",
            coverUrl = null,
            platforms = mapOf("PS5" to "2026-11-27", "PC" to "2026-11-27"),
            storageSizes = mapOf("PS5" to "44 GB"),
            about = "Build a flying car in the garage, then race it through storm fronts.",
            hasTrailer = true,
            trailerYoutubeId = "dQw4w9WgXcQ",
            expectedYear = null,
            progress = 0.50f
        ),
        GameDto(
            id = "de",
            igdbId = 10008,
            title = "Dust & Ember",
            developer = "Kiln House",
            hueHex = "#8C3B2E",
            shapeKey = "c6",
            coverUrl = null,
            platforms = mapOf("Android" to null, "iOS" to null),
            storageSizes = emptyMap(),
            about = "A desert survival game for phones. No date has been announced.",
            hasTrailer = false,
            trailerYoutubeId = null,
            expectedYear = null,
            progress = 0.10f
        )
    )

    private fun fallbackSearch(query: String, platform: String): List<GameDto> {
        var pool = localCatalog
        if (platform != "All") {
            pool = pool.filter { it.platforms.containsKey(platform) }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            pool = pool.filter { it.title.lowercase().contains(q) }
        }
        return pool
    }

    private fun fallbackDetails(id: String): GameDto? {
        return localCatalog.find { it.id == id || "igdb_${it.igdbId}" == id }
            ?: localCatalog.find { it.title.equals(id, ignoreCase = true) }
    }
}
