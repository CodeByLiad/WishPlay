package com.jinatra.wishplay.data.remote.api

import com.jinatra.wishplay.data.remote.dto.GameDto
import com.jinatra.wishplay.domain.model.ReleaseEvidence
import com.jinatra.wishplay.domain.model.storeReleaseDate
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.net.URLEncoder
import java.time.OffsetDateTime

/** Public store catalogs. Store identifiers are kept separate from RAWG identifiers. */
internal class StoreCatalog(client: OkHttpClient) {
    private val client = client.newBuilder().callTimeout(8, java.util.concurrent.TimeUnit.SECONDS).build()
    private val cache = object : LinkedHashMap<String, Pair<Long, JSONObject>>(32, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, JSONObject>>?) = size > 20
    }
    private suspend fun json(url: String): JSONObject {
        synchronized(cache) { cache[url]?.takeIf { System.currentTimeMillis() - it.first < 600_000 }?.let { return it.second } }
        val value = client.catalogJson(url)
        synchronized(cache) { cache[url] = System.currentTimeMillis() to value }
        return value
    }

    suspend fun searchSteam(query: String): List<GameDto> {
        val items = json("https://store.steampowered.com/api/storesearch/?term=${URLEncoder.encode(query, "UTF-8")}&l=english&cc=us").optJSONArray("items")
            ?: return emptyList()
        return (0 until items.length()).mapNotNull { index ->
            val item = items.getJSONObject(index)
            if (item.optString("type") != "app") return@mapNotNull null
            val id = item.optLong("id")
            if (id <= 0) return@mapNotNull null
            GameDto(id = "steam_$id", title = item.getString("name"),
                coverUrl = item.optString("tiny_image").takeIf { it.startsWith("https://") },
                platforms = mapOf("PC" to null),
                releaseEvidence = mapOf("PC" to listOf(ReleaseEvidence("Steam (US)", null, "https://store.steampowered.com/app/$id/"))))
        }
    }

    suspend fun steamDetails(id: Long): GameDto? {
        val response = json("https://store.steampowered.com/api/appdetails?appids=$id&l=english&cc=us").optJSONObject(id.toString()) ?: return null
        if (!response.optBoolean("success")) return null
        val data = response.getJSONObject("data")
        if (data.optString("type") != "game") return null
        val date = storeReleaseDate(data.optJSONObject("release_date")?.optString("date"))
        return GameDto(id = "steam_$id", title = data.getString("name"),
            developer = data.optJSONArray("developers")?.optString(0),
            coverUrl = data.optString("header_image").takeIf { it.startsWith("https://") },
            about = data.optString("short_description").replace(Regex("<[^>]*>"), " "),
            platforms = mapOf("PC" to date),
            releaseEvidence = mapOf("PC" to listOf(ReleaseEvidence("Steam (US)", date, "https://store.steampowered.com/app/$id/"))))
    }

    suspend fun searchApple(query: String): List<GameDto> {
        val items = json("https://itunes.apple.com/search?term=${URLEncoder.encode(query, "UTF-8")}&entity=software&country=us&limit=100").optJSONArray("results")
            ?: return emptyList()
        return (0 until items.length()).mapNotNull { mapApple(items.getJSONObject(it)) }
    }

    suspend fun appleDetails(id: Long): GameDto? {
        val items = json("https://itunes.apple.com/lookup?id=$id&country=us&entity=software").optJSONArray("results")
        return items?.optJSONObject(0)?.let(::mapApple)
    }

    private fun mapApple(item: JSONObject): GameDto? {
        val genres = item.optJSONArray("genreIds")
        if (item.optInt("primaryGenreId") != 6014 && (genres == null || (0 until genres.length()).none { genres.optString(it) == "6014" })) return null
        val id = item.optLong("trackId")
        if (id <= 0) return null
        val date = runCatching { OffsetDateTime.parse(item.getString("releaseDate")).toLocalDate().toString() }.getOrNull()
        val url = item.optString("trackViewUrl").takeIf { it.startsWith("https://apps.apple.com/") } ?: "https://apps.apple.com/us/app/id$id"
        return GameDto(id = "apple_$id", title = item.getString("trackName"),
            developer = item.optString("sellerName").ifBlank { null },
            coverUrl = item.optString("artworkUrl512").ifBlank { item.optString("artworkUrl100") }.takeIf { it.startsWith("https://") },
            about = item.optString("description").ifBlank { null }, platforms = mapOf("iOS" to date),
            releaseEvidence = mapOf("iOS" to listOf(ReleaseEvidence("App Store (US)", date, url))))
    }
}
