package com.jinatra.wishplay.data.remote.api

import com.jinatra.wishplay.data.remote.dto.GameDto
import com.jinatra.wishplay.data.remote.dto.GameSearchPage
import com.jinatra.wishplay.domain.model.validReleaseDate
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.jinatra.wishplay.data.remote.dto.RequirementsLevelDto
import com.jinatra.wishplay.data.remote.dto.SystemRequirementsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import com.jinatra.wishplay.domain.model.ReleaseEvidence
import com.jinatra.wishplay.domain.model.catalogTitleKey

interface WishPlayApiService {
    suspend fun searchGames(query: String, platform: String): List<GameDto>
    suspend fun searchGamesPage(query: String, platform: String, page: Int): GameSearchPage =
        GameSearchPage(if (page == 1) searchGames(query, platform) else emptyList())
    fun searchGamesUpdates(query: String, platform: String, page: Int): kotlinx.coroutines.flow.Flow<GameSearchPage> =
        kotlinx.coroutines.flow.flow { emit(searchGamesPage(query, platform, page)) }
    fun cachedSearchPage(query: String, platform: String, page: Int): GameSearchPage? = null

    /** Games matching [query] that the [platform] filter excluded, minus anything in [shown]. */
    suspend fun searchOtherPlatforms(query: String, platform: String, shown: List<GameDto>): List<GameDto> = emptyList()
    suspend fun getGameDetails(id: String): GameDto?
}

@Singleton
class WishPlayApiServiceImpl @Inject constructor(
    client: OkHttpClient
) : WishPlayApiService {

    private val client = client.newBuilder().callTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(false).build()
    private val googlePlay = GooglePlayCatalog(this.client)

    // RAWG Live Video Game Database API
    private val rawgApiKey = "0daeeb89a3e440e7864d61b5e2a94d26"
    private val rawgBaseUrl = "https://api.rawg.io/api"
    private val stores = StoreCatalog(client)

    override suspend fun searchGames(query: String, platform: String): List<GameDto> =
        searchGamesPage(query, platform, 1).games

    private data class Snapshot(val time: Long, val page: GameSearchPage)
    private val searchCache = object : LinkedHashMap<String, Snapshot>(32, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Snapshot>?) = size > 32
    }
    private fun cacheKey(query: String, platform: String, page: Int) = "$platform:$page:${catalogTitleKey(query)}"
    override fun cachedSearchPage(query: String, platform: String, page: Int): GameSearchPage? = synchronized(searchCache) {
        searchCache[cacheKey(query, platform, page)]?.takeIf {
            android.os.SystemClock.elapsedRealtime() - it.time < 24 * 60 * 60_000L
        }?.page
    }

    override suspend fun searchGamesPage(query: String, platform: String, page: Int): GameSearchPage =
        searchGamesUpdates(query, platform, page).last()

    /** Emit each working store immediately; a slow provider only delays its own additions. */
    override fun searchGamesUpdates(query: String, platform: String, page: Int): kotlinx.coroutines.flow.Flow<GameSearchPage> =
        kotlinx.coroutines.flow.channelFlow {
            val key = cacheKey(query, platform, page)
            val snapshot = synchronized(searchCache) { searchCache[key] }
                ?.takeIf { android.os.SystemClock.elapsedRealtime() - it.time < 24 * 60 * 60_000L }
            if (snapshot != null) {
                if (snapshot.page.isComplete && android.os.SystemClock.elapsedRealtime() - snapshot.time < 60_000L) {
                    send(snapshot.page)
                    return@channelFlow
                }
                send(snapshot.page.copy(isComplete = false, hasMore = false,
                    warnings = listOf("Saved results. Checking for updates…")))
            }
            if (page != 1) {
                val value = kotlinx.coroutines.withTimeout(6000) { searchRawgPage(query, platform, page) }
                synchronized(searchCache) { searchCache[key] = Snapshot(android.os.SystemClock.elapsedRealtime(), value) }
                send(value)
                return@channelFlow
            }
            val sources = linkedMapOf<String, suspend () -> GameSearchPage>()
            sources["RAWG"] = { searchRawgPage(query, platform, page) }
            if (platform in listOf("All", "PC")) sources["Steam"] = { GameSearchPage(stores.searchSteam(query)) }
            if (platform in listOf("All", "iOS")) sources["App Store"] = { GameSearchPage(stores.searchApple(query)) }
            if (platform in listOf("All", "Android")) sources["Google Play"] = { GameSearchPage(googlePlay.search(query)) }
            val responses = linkedMapOf<String, Result<GameSearchPage>>()
            val lock = Mutex()
            sources.map { (name, request) -> launch {
                val response = boundedSource(if (name == "RAWG") 4000 else 7000, request)
                lock.withLock {
                    responses[name] = response
                    val complete = responses.size == sources.size
                    if (complete && responses.values.none { it.isSuccess }) {
                        if (snapshot == null) throw IOException("Game providers are unavailable", response.exceptionOrNull())
                        send(snapshot.page.copy(isComplete = true, warnings = listOf("Showing saved results. Providers are unavailable; try again to refresh.")))
                        return@withLock
                    }
                    val base = responses["RAWG"]?.getOrNull()?.games.orEmpty()
                    val additions = listOf("Steam", "App Store", "Google Play").flatMap { responses[it]?.getOrNull()?.games.orEmpty() }
                    val unique = additions.filter { extra -> base.none { existing ->
                        catalogTitleKey(existing.title) == catalogTitleKey(extra.title) && existing.platforms.keys.containsAll(extra.platforms.keys)
                    } }
                    val queryKey = catalogTitleKey(query)
                    val games = (base + unique).distinctBy { it.id }.sortedBy { game ->
                        val title = catalogTitleKey(game.title)
                        when { queryKey.isBlank() || title == queryKey -> 0
                            title.startsWith(queryKey) -> 1; title.contains(queryKey) -> 2; else -> 3 }
                    }
                    if (games.isNotEmpty() || complete) {
                        val warnings = sources.keys.filter { responses[it]?.isFailure == true }.map {
                            if (it == "RAWG") "RAWG is unavailable; showing store results."
                            else "$it is unavailable; other results are still shown."
                        }
                        val value = GameSearchPage(games, hasMore = complete && responses["RAWG"]?.getOrNull()?.hasMore == true,
                            warnings = warnings, isComplete = complete)
                        synchronized(searchCache) { searchCache[key] = Snapshot(android.os.SystemClock.elapsedRealtime(), value) }
                        send(value)
                    }
                }
            } }.joinAll()
        }.flowOn(Dispatchers.IO)

    private suspend fun <T> boundedSource(millis: Long, block: suspend () -> T): Result<T> =
        kotlinx.coroutines.withTimeoutOrNull(millis) { attempt(block) }
            ?: Result.failure(IOException("Game provider timed out"))

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try { Result.success(block()) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (e: Exception) { Result.failure(e) }

    private suspend fun searchRawgPage(
        query: String,
        platform: String,
        page: Int,
        httpClient: OkHttpClient = client
    ): GameSearchPage = withContext(Dispatchers.IO) {
        val q = query.trim()
        val platformFilter = mapPlatformToRawgId(platform)

        val url = if (q.isNotBlank()) {
            val encodedQ = URLEncoder.encode(q, "UTF-8")
            "$rawgBaseUrl/games?key=$rawgApiKey&search=$encodedQ&page_size=40&page=$page$platformFilter"
        } else {
            // Browse the full provider catalog when search is empty.
            "$rawgBaseUrl/games?key=$rawgApiKey&ordering=-added&page_size=40&page=$page$platformFilter"
        }

        val json = httpClient.catalogJson(url)
        val results = json.optJSONArray("results") ?: throw IOException("Invalid catalog response")
        GameSearchPage(
            games = (0 until results.length()).map { mapRawgGameToDto(results.getJSONObject(it)) },
            hasMore = !json.isNull("next") && json.optString("next").isNotBlank()
        )
    }

    /** Supplementary results cannot outlive their bounded request or delay primary results. */
    override suspend fun searchOtherPlatforms(query: String, platform: String, shown: List<GameDto>): List<GameDto> {
        if (query.trim().length < 2 || mapPlatformToRawgId(platform).isEmpty()) return emptyList()
        val unfiltered = boundedSource(5000) { searchRawgPage(query, "All", 1) }.getOrNull() ?: return emptyList()
        return selectOtherPlatformGames(primary = shown, candidates = unfiltered.games, selectedPlatform = platform)
    }

    override suspend fun getGameDetails(id: String): GameDto? = withContext(Dispatchers.IO) {
        if (id.startsWith("play_")) return@withContext googlePlay.details(id)
        if (id.startsWith("steam_")) return@withContext id.removePrefix("steam_").toLongOrNull()?.let { stores.steamDetails(it) }
        if (id.startsWith("apple_")) return@withContext id.removePrefix("apple_").toLongOrNull()?.let { stores.appleDetails(it) }
        val rawgId = if (id.startsWith("rawg_")) id.removePrefix("rawg_") else id.toLongOrNull()?.toString()

        if (rawgId != null) {
            val url = "$rawgBaseUrl/games/$rawgId?key=$rawgApiKey"
            val game = mapRawgGameToDto(requestJson(url))
            return@withContext crossCheckLinkedStores(game, rawgId)
        }

        null
    }

    private suspend fun requestJson(url: String): JSONObject = client.catalogJson(url)

    private suspend fun crossCheckLinkedStores(game: GameDto, rawgId: String): GameDto = coroutineScope {
        if (game.platforms.keys.none { it == "PC" || it == "iOS" }) return@coroutineScope game
        val links = attempt { requestJson("$rawgBaseUrl/games/$rawgId/stores?key=$rawgApiKey") }.getOrNull()
            ?.optJSONArray("results") ?: return@coroutineScope game
        val urls = (0 until links.length()).map { links.getJSONObject(it).optString("url") }
        val steamId = urls.firstNotNullOfOrNull { Regex("^https?://store\\.steampowered\\.com/app/(\\d+)(?:/|$)").find(it)?.groupValues?.get(1)?.toLongOrNull() }
        val appleId = urls.firstNotNullOfOrNull { url ->
            if (url.startsWith("https://apps.apple.com/") || url.startsWith("https://itunes.apple.com/"))
                Regex("/id(\\d+)(?:[/?]|$)").find(url)?.groupValues?.get(1)?.toLongOrNull() else null
        }
        val steam = async { if (steamId != null && "PC" in game.platforms) attempt { stores.steamDetails(steamId) }.getOrNull() else null }
        val apple = async { if (appleId != null && "iOS" in game.platforms) attempt { stores.appleDetails(appleId) }.getOrNull() else null }
        listOfNotNull(steam.await(), apple.await()).fold(game) { result, store ->
            result.copy(platforms = result.platforms + store.platforms,
                coverUrl = result.coverUrl ?: store.coverUrl,
                requirements = store.requirements ?: result.requirements,
                storageSizes = result.storageSizes + store.storageSizes.filterValues { it != null },
                releaseEvidence = result.releaseEvidence.toMutableMap().apply {
                    store.releaseEvidence.forEach { (platform, evidence) -> put(platform, (get(platform).orEmpty() + evidence).distinct()) }
                })
        }
    }

    private fun mapPlatformToRawgId(platform: String): String {
        return when (platform) {
            "PC" -> "&platforms=4"
            "PS5" -> "&platforms=187"
            "PS4" -> "&platforms=18"
            "Xbox Series" -> "&platforms=186"
            "Switch" -> "&platforms=7"
            "Android" -> "&platforms=21"
            "iOS" -> "&platforms=3"
            else -> ""
        }
    }

    internal fun mapRawgGameToDto(gameObj: JSONObject): GameDto {
        val id = gameObj.optLong("id")
        val title = gameObj.optString("name", "Unknown Title")
        val bgImage = sequenceOf("background_image", "background_image_additional")
            .map { gameObj.optString(it, "") }.firstOrNull { it.startsWith("https://") }
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
                val platRel = validReleaseDate(pItem.optString("released_at", ""))

                val standardName = when {
                    slug == "pc" -> "PC"
                    slug == "playstation5" || name.contains("PlayStation 5", ignoreCase = true) -> "PS5"
                    slug == "playstation4" || name.contains("PlayStation 4", ignoreCase = true) -> "PS4"
                    slug.contains("xbox-series") || name.contains("Xbox Series", ignoreCase = true) -> "Xbox Series"
                    name.contains("Switch 2", ignoreCase = true) -> "Switch 2"
                    slug.contains("switch") || name.contains("Switch", ignoreCase = true) -> "Switch"
                    slug == "android" || name.contains("Android", ignoreCase = true) -> "Android"
                    slug == "ios" || name.contains("iOS", ignoreCase = true) -> "iOS"
                    else -> name.takeIf { it.isNotBlank() }
                }

                if (standardName != null) {
                    platformsMap[standardName] = platRel
                }

                if (slug == "pc") {
                    val reqObj = pItem.optJSONObject("requirements_en") ?: pItem.optJSONObject("requirements")
                    if (reqObj != null) {
                        val minStr = reqObj.optString("minimum", "")
                        val recStr = reqObj.optString("recommended", "")
                        if (minStr.isNotBlank()) parsedMinReq = parseRequirementsText(minStr)
                        if (recStr.isNotBlank()) parsedRecReq = parseRequirementsText(recStr)
                    }
                }
            }
        }

        val shapeKeys = listOf("c9", "c4", "c12", "c6", "sq")
        val hueColors = listOf("#1F7A6E", "#B8325F", "#3A48B8", "#A3441F", "#4A6630", "#5B3FA8", "#0F6A80", "#8C3B2E")
        val shapeKey = shapeKeys[Math.floorMod(title.hashCode(), shapeKeys.size)]
        val hueHex = hueColors[Math.floorMod(title.hashCode(), hueColors.size)]

        val rawDesc = gameObj.optString("description_raw", "").ifBlank {
            gameObj.optString("description", "").replace(Regex("<[^>]*>"), " ").trim()
        }.ifBlank { null }

        return GameDto(
            id = "rawg_$id",
            igdbId = null,
            title = title,
            developer = gameObj.optJSONArray("developers")?.optJSONObject(0)?.optString("name")
                ?: gameObj.optJSONArray("publishers")?.optJSONObject(0)?.optString("name")
                ?.takeIf { it != "null" && it.isNotBlank() },
            hueHex = hueHex,
            shapeKey = shapeKey,
            coverUrl = bgImage,
            platforms = platformsMap,
            releaseEvidence = platformsMap.mapValues { (_, date) -> listOf(ReleaseEvidence("RAWG", date,
                gameObj.optString("slug").takeIf { it.isNotBlank() }?.let { "https://rawg.io/games/$it" } ?: "https://rawg.io")) },
            storageSizes = emptyMap(),
            about = rawDesc,
            hasTrailer = false,
            trailerYoutubeId = null,
            requirements = if (parsedMinReq != null || parsedRecReq != null) {
                SystemRequirementsDto(min = parsedMinReq, rec = parsedRecReq)
            } else null,
            price = null,
            expectedYear = null,
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

}
