package com.nuvetrix.wishplay.data.remote.api

import com.nuvetrix.wishplay.data.remote.dto.GameDto
import com.nuvetrix.wishplay.data.remote.dto.GamePriceDto
import com.nuvetrix.wishplay.data.remote.dto.RequirementsLevelDto
import com.nuvetrix.wishplay.data.remote.dto.SystemRequirementsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

interface WishPlayApiService {
    suspend fun searchGames(query: String, platform: String): List<GameDto>
    suspend fun getGameDetails(id: String): GameDto?
}

@Singleton
class WishPlayApiServiceImpl @Inject constructor(
    private val client: OkHttpClient
) : WishPlayApiService {

    // RAWG Live Video Game Database API
    private val rawgApiKey = "0daeeb89a3e440e7864d61b5e2a94d26"
    private val rawgBaseUrl = "https://api.rawg.io/api"

    override suspend fun searchGames(query: String, platform: String): List<GameDto> = withContext(Dispatchers.IO) {
        val q = query.trim()
        val platformFilter = mapPlatformToRawgId(platform)

        val url = if (q.isNotBlank()) {
            val encodedQ = URLEncoder.encode(q, "UTF-8")
            "$rawgBaseUrl/games?key=$rawgApiKey&search=$encodedQ&page_size=20$platformFilter"
        } else {
            // Popular upcoming / trending when search query is empty
            "$rawgBaseUrl/games?key=$rawgApiKey&ordering=-added&page_size=20$platformFilter"
        }

        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val parsed = mutableListOf<GameDto>()
                        for (i in 0 until results.length()) {
                            val gameObj = results.getJSONObject(i)
                            parsed.add(mapRawgGameToDto(gameObj))
                        }
                        if (parsed.isNotEmpty()) {
                            return@withContext parsed
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to local catalog if offline or network error
        }

        fallbackSearch(query, platform)
    }

    override suspend fun getGameDetails(id: String): GameDto? = withContext(Dispatchers.IO) {
        val rawgId = if (id.startsWith("rawg_")) id.removePrefix("rawg_") else id.toLongOrNull()?.toString()

        if (rawgId != null) {
            val url = "$rawgBaseUrl/games/$rawgId?key=$rawgApiKey"
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val gameObj = JSONObject(body)
                        return@withContext mapRawgGameToDto(gameObj)
                    }
                }
            } catch (_: Exception) {
                // Fallback
            }
        }

        fallbackDetails(id)
    }

    private fun mapPlatformToRawgId(platform: String): String {
        return when (platform) {
            "PC" -> "&platforms=4"
            "PS5" -> "&platforms=187"
            "Xbox Series" -> "&platforms=186"
            "Switch", "Switch 2" -> "&platforms=7"
            "Android" -> "&platforms=21"
            "iOS" -> "&platforms=3"
            else -> ""
        }
    }

    private fun mapRawgGameToDto(gameObj: JSONObject): GameDto {
        val id = gameObj.optLong("id")
        val title = gameObj.optString("name", "Unknown Title")
        val released = gameObj.optString("released", "").ifBlank { null }
        val bgImage = gameObj.optString("background_image", "").ifBlank { null }
        val rating = gameObj.optDouble("rating", 4.0).toFloat()

        val platformsMap = mutableMapOf<String, String?>()
        val platformsArr = gameObj.optJSONArray("platforms")
        var parsedMinReq: RequirementsLevelDto? = null
        var parsedRecReq: RequirementsLevelDto? = null

        if (platformsArr != null) {
            for (i in 0 until platformsArr.length()) {
                val pItem = platformsArr.getJSONObject(i)
                val pObj = pItem.optJSONObject("platform")
                val slug = pObj?.optString("slug", "") ?: ""
                val name = pObj?.optString("name", "") ?: ""
                val platRel = pItem.optString("released_at", "").ifBlank { released }

                val standardName = when {
                    slug == "pc" -> "PC"
                    slug == "playstation5" || name.contains("PlayStation 5", ignoreCase = true) -> "PS5"
                    slug.contains("xbox-series") || name.contains("Xbox Series", ignoreCase = true) -> "Xbox Series"
                    slug.contains("switch") || name.contains("Switch", ignoreCase = true) -> "Switch"
                    slug == "android" || name.contains("Android", ignoreCase = true) -> "Android"
                    slug == "ios" || name.contains("iOS", ignoreCase = true) -> "iOS"
                    else -> null
                }

                if (standardName != null) {
                    platformsMap[standardName] = platRel
                }

                if (slug == "pc") {
                    val reqObj = pItem.optJSONObject("requirements_en")
                    if (reqObj != null) {
                        val minStr = reqObj.optString("minimum", "")
                        val recStr = reqObj.optString("recommended", "")
                        if (minStr.isNotBlank()) parsedMinReq = parseRequirementsText(minStr)
                        if (recStr.isNotBlank()) parsedRecReq = parseRequirementsText(recStr)
                    }
                }
            }
        }

        if (platformsMap.isEmpty()) {
            platformsMap["PC"] = released
        }

        val shapeKeys = listOf("c9", "c4", "c12", "c6", "sq")
        val hueColors = listOf("#1F7A6E", "#B8325F", "#3A48B8", "#A3441F", "#4A6630", "#5B3FA8", "#0F6A80", "#8C3B2E")
        val shapeKey = shapeKeys[(title.hashCode().absoluteValue) % shapeKeys.size]
        val hueHex = hueColors[(title.hashCode().absoluteValue) % hueColors.size]

        val clipObj = gameObj.optJSONObject("clip")
        val hasTrailer = clipObj != null

        val rawDesc = gameObj.optString("description_raw", "").ifBlank {
            gameObj.optString("description", "").replace(Regex("<[^>]*>"), " ").trim()
        }.ifBlank { null }

        return GameDto(
            id = "rawg_$id",
            igdbId = id,
            title = title,
            developer = gameObj.optJSONArray("developers")?.optJSONObject(0)?.optString("name")
                ?: gameObj.optJSONArray("publishers")?.optJSONObject(0)?.optString("name")
                ?: "Studio",
            hueHex = hueHex,
            shapeKey = shapeKey,
            coverUrl = bgImage,
            platforms = platformsMap,
            storageSizes = emptyMap(),
            about = rawDesc,
            hasTrailer = hasTrailer,
            trailerYoutubeId = if (hasTrailer) clipObj?.optString("video") else null,
            requirements = if (parsedMinReq != null || parsedRecReq != null) {
                SystemRequirementsDto(min = parsedMinReq, rec = parsedRecReq)
            } else null,
            price = if (platformsMap.containsKey("PC")) GamePriceDto("Steam", "$59.99") else null,
            expectedYear = if (released == null) "2027" else null,
            progress = (rating / 5.0f).coerceIn(0.2f, 0.98f)
        )
    }

    private fun parseRequirementsText(text: String): RequirementsLevelDto {
        val clean = text.replace(Regex("<[^>]*>"), " ")
        var os: String? = null
        var cpu: String? = null
        var gpu: String? = null
        var ram: String? = null

        clean.split("\n", ";", ",").forEach { line ->
            val l = line.trim()
            when {
                l.startsWith("OS:", ignoreCase = true) || l.startsWith("OS ", ignoreCase = true) ->
                    os = l.substringAfter(":").trim()
                l.startsWith("Processor:", ignoreCase = true) || l.startsWith("CPU:", ignoreCase = true) ->
                    cpu = l.substringAfter(":").trim()
                l.startsWith("Graphics:", ignoreCase = true) || l.startsWith("GPU:", ignoreCase = true) ->
                    gpu = l.substringAfter(":").trim()
                l.startsWith("Memory:", ignoreCase = true) || l.startsWith("RAM:", ignoreCase = true) ->
                    ram = l.substringAfter(":").trim()
            }
        }

        return RequirementsLevelDto(
            os = os ?: if (clean.contains("Windows", ignoreCase = true)) "Windows 10/11 64-bit" else null,
            cpu = cpu,
            gpu = gpu,
            ram = ram
        )
    }

    // Built-in catalog matching prototype approved dataset (offline fallback)
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
        return localCatalog.find { it.id == id || "igdb_${it.igdbId}" == id || "rawg_${it.igdbId}" == id }
            ?: localCatalog.find { it.title.equals(id, ignoreCase = true) }
    }
}
