package com.jinatra.wishplay.data.remote.api

import com.jinatra.wishplay.data.remote.dto.GameDto
import com.jinatra.wishplay.data.remote.dto.GameSearchPage
import com.jinatra.wishplay.domain.model.ReleaseEvidence
import com.jinatra.wishplay.domain.model.validReleaseDate
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException

/** Public Microsoft catalog. Search suggestions are hydrated in one batch to verify platforms. */
internal class MicrosoftCatalog(private val client: OkHttpClient) {
    private val continuations = object : LinkedHashMap<String, String>(32, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 64
    }

    /** The full storefront search continues beyond autosuggest's short, server-limited list. */
    suspend fun searchPage(query: String, platform: String, page: Int): GameSearchPage {
        val key = "$platform:${query.trim()}:$page"
        val cursor = synchronized(continuations) { continuations[key] }
        if (page > 1 && cursor == null) {
            // Rebuild missing cursor state, e.g. after its bounded cache evicted an old query.
            val previous = searchPage(query, platform, page - 1)
            if (!previous.hasMore) return GameSearchPage(emptyList())
        }
        val url = if (page == 1) {
            val family = if (platform == "Xbox Series") "Windows.Xbox" else "Windows.Desktop"
            "$STOREFRONT/v9.0/pages/searchResults?market=US&locale=en-us&deviceFamily=$family&query=${URLEncoder.encode(query.trim(), "UTF-8")}&mediaType=games"
        } else synchronized(continuations) { continuations[key] } ?: throw IOException("Search continuation unavailable")
        val payload = parseSearchPayload(client.catalogText(url, "application/json"))
        val ids = (payload.getJSONArray("SearchResults").objects() + payload.optJSONArray("HighlightedResults").objects())
            .filter { it.optString("ProductFamilyName") == "Games" }.map { it.optString("ProductId") }.filter(::validId).distinct()
        val batches = coroutineScope {
            ids.chunked(5).map { chunk -> async {
                try { Result.success(products(chunk)) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { Result.failure(error) }
            } }.awaitAll()
        }
        if (batches.isNotEmpty() && batches.none { it.isSuccess }) throw IOException("Microsoft products unavailable", batches.first().exceptionOrNull())
        val next = trustedContinuation(payload.optString("NextUri"))
        if (next != null) synchronized(continuations) { continuations["$platform:${query.trim()}:${page + 1}"] = next }
        val games = batches.flatMap { it.getOrNull().orEmpty() }.filter { platform == "All" || platform in it.platforms }
        return GameSearchPage(games, hasMore = next != null, warnings =
            if (batches.any { it.isFailure }) listOf("Some Microsoft products could not be loaded. Retry to refresh.") else emptyList())
    }

    internal fun parseSearchPayload(text: String): JSONObject {
        val roots = if (text.trimStart().startsWith("[")) JSONArray(text).objects() else listOf(JSONObject(text))
        return roots.firstNotNullOfOrNull { it.optJSONObject("Payload")?.takeIf { payload -> payload.has("SearchResults") } }
            ?: throw IOException("Microsoft search could not be read")
    }

    internal fun trustedContinuation(path: String): String? {
        if (path.isBlank()) return null
        val url = "$STOREFRONT/".toHttpUrl().resolve(path) ?: return null
        return url.toString().takeIf { url.scheme == "https" && url.host == "storeedgefd.dsx.mp.microsoft.com" && url.port == 443 && url.encodedPath == "/v9.0/search" }
    }
    suspend fun search(query: String, platform: String): List<GameDto> {
        if (query.isBlank()) return emptyList()
        val json = client.catalogJson("$BASE/productFamilies/autosuggest?query=${URLEncoder.encode(query, "UTF-8")}&market=US&languages=en-us&productFamilyNames=Games&topProducts=30")
        val ids = json.getJSONArray("Results").objects().flatMap { it.optJSONArray("Products").objects() }
            .filter { it.optString("Type") == "Game" }.map { it.optString("ProductId") }
            .filter { validId(it) }.distinct()
        if (ids.isEmpty()) return emptyList()
        return products(ids).filter { platform == "All" || platform in it.platforms }
    }

    suspend fun details(id: String): GameDto? {
        val value = id.removePrefix("microsoft_")
        return if (validId(value)) products(listOf(value)).firstOrNull() else null
    }

    private suspend fun products(ids: List<String>): List<GameDto> =
        client.catalogJson("$BASE/products?bigIds=${ids.joinToString(",")}&market=US&languages=en-US")
            .getJSONArray("Products").objects().mapNotNull(::mapGame)

    internal fun mapGame(product: JSONObject): GameDto? {
        if (product.optString("ProductType") != "Game" || product.optJSONObject("Properties")?.optBoolean("IsDemo") == true) return null
        val id = product.optString("ProductId").takeIf(::validId) ?: return null
        val local = product.optJSONArray("LocalizedProperties")?.optJSONObject(0) ?: return null
        val title = local.optString("ProductTitle").takeIf { it.isNotBlank() } ?: return null
        val consoleGen9 = product.optJSONObject("Properties")?.optJSONArray("XboxConsoleGenCompatible")
            ?.let { array -> (0 until array.length()).any { array.optString(it) == "ConsoleGen9" } } == true
        val url = "https://www.xbox.com/en-US/games/store/_/$id"
        val evidence = linkedMapOf<String, MutableList<ReleaseEvidence>>()
        product.optJSONArray("DisplaySkuAvailabilities").objects().forEach { entry ->
            entry.optJSONArray("Availabilities").objects().forEach { availability ->
                val date = validReleaseDate(availability.optJSONObject("Properties")?.optString("OriginalReleaseDate").orEmpty().take(10))
                availability.optJSONObject("Conditions")?.optJSONObject("ClientConditions")
                    ?.optJSONArray("AllowedPlatforms").objects().forEach { allowed ->
                        val platform = when (allowed.optString("PlatformName")) {
                            "Windows.Desktop" -> "PC"
                            "Windows.Xbox" -> "Xbox Series".takeIf { consoleGen9 }
                            else -> null
                        }
                        if (platform != null) evidence.getOrPut(platform) { mutableListOf() }
                            .add(ReleaseEvidence("Microsoft Store (US)", date, url))
                    }
            }
        }
        if (evidence.isEmpty()) return null
        val image = local.optJSONArray("Images").objects().sortedBy {
            when (it.optString("ImagePurpose")) { "Poster" -> 0; "BoxArt" -> 1; "Tile" -> 2; "Logo" -> 3; else -> 4 }
        }.firstNotNullOfOrNull { secureImage(it.optString("Uri")) }
        return GameDto(id = "microsoft_$id", title = title, developer = local.optString("DeveloperName").ifBlank { null },
            coverUrl = image, about = local.optString("ProductDescription").ifBlank { local.optString("ShortDescription") }.ifBlank { null },
            platforms = evidence.mapValues { (_, values) -> values.mapNotNull { it.date }.minOrNull() },
            releaseEvidence = evidence.mapValues { (_, values) -> values.distinct() })
    }

    private fun secureImage(url: String): String? = (if (url.startsWith("//")) "https:$url" else url).takeIf { it.startsWith("https://") }
    private fun validId(id: String) = id.matches(Regex("[A-Z0-9]{12}"))
    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    companion object {
        private const val BASE = "https://displaycatalog.mp.microsoft.com/v7.0"
        private const val STOREFRONT = "https://storeedgefd.dsx.mp.microsoft.com"
    }
}
