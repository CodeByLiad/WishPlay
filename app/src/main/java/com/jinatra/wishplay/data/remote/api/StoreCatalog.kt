package com.jinatra.wishplay.data.remote.api

import com.jinatra.wishplay.data.remote.dto.GameDto
import com.jinatra.wishplay.domain.model.ReleaseEvidence
import com.jinatra.wishplay.domain.model.storeReleaseDate
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.net.URLEncoder
import java.time.OffsetDateTime
import android.text.Html
import com.jinatra.wishplay.data.remote.dto.RequirementsLevelDto
import com.jinatra.wishplay.data.remote.dto.SystemRequirementsDto

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
        if (query.isBlank()) return browseSteam()
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

    private suspend fun browseSteam(): List<GameDto> {
        val page = json("https://store.steampowered.com/api/featuredcategories/?cc=us&l=english")
        return listOf("top_sellers", "new_releases", "coming_soon", "specials").flatMap { section ->
            val items = page.optJSONObject(section)?.optJSONArray("items") ?: return@flatMap emptyList()
            (0 until items.length()).mapNotNull { index ->
                val item = items.getJSONObject(index)
                val id = item.optLong("id")
                if (id <= 0 || item.optInt("type") != 0) return@mapNotNull null
                GameDto(id = "steam_$id", title = item.getString("name"),
                    coverUrl = item.optString("header_image").takeIf { it.startsWith("https://") },
                    platforms = mapOf("PC" to null), releaseEvidence = mapOf("PC" to listOf(
                        ReleaseEvidence("Steam (US)", null, "https://store.steampowered.com/app/$id/"))))
            }
        }.distinctBy { it.id }.take(40)
    }

    suspend fun steamDetails(id: Long): GameDto? {
        val response = json("https://store.steampowered.com/api/appdetails?appids=$id&l=english&cc=us").optJSONObject(id.toString()) ?: return null
        if (!response.optBoolean("success")) return null
        val data = response.getJSONObject("data")
        if (data.optString("type") != "game") return null
        val date = storeReleaseDate(data.optJSONObject("release_date")?.optString("date"))
        val pc = data.optJSONObject("pc_requirements")
        val minimum = parseRequirements(pc?.optString("minimum").orEmpty())
        val recommended = parseRequirements(pc?.optString("recommended").orEmpty())
        return GameDto(id = "steam_$id", title = data.getString("name"),
            developer = data.optJSONArray("developers")?.optString(0),
            coverUrl = data.optString("header_image").takeIf { it.startsWith("https://") },
            about = data.optString("short_description").replace(Regex("<[^>]*>"), " "),
            requirements = if (minimum.first != null || recommended.first != null)
                SystemRequirementsDto(minimum.first, recommended.first) else null,
            storageSizes = mapOf("PC" to (recommended.second ?: minimum.second)),
            platforms = mapOf("PC" to date),
            releaseEvidence = mapOf("PC" to listOf(ReleaseEvidence("Steam (US)", date, "https://store.steampowered.com/app/$id/"))))
    }

    /** Store HTML contains labelled list items, sometimes with nested markup and entities. */
    internal fun parseRequirements(html: String): Pair<RequirementsLevelDto?, String?> {
        val text = Html.fromHtml(html.replace(Regex("(?i)<br\\s*/?>|</li>"), "\n"), Html.FROM_HTML_MODE_LEGACY)
            .toString().replace('\u00a0', ' ')
        fun field(vararg names: String): String? {
            val labels = "OS|Processor|Memory|Graphics|Storage|DirectX|Sound Card|Additional Notes|Network|Minimum|Recommended"
            val pattern = Regex("(?is)(?:^|\\n)\\s*(?:${names.joinToString("|") { Regex.escape(it) }})\\s*\\*?\\s*:\\s*(.*?)(?=\\n\\s*(?:$labels)\\s*\\*?\\s*:|$)")
            return pattern.find(text)?.groupValues?.get(1)?.replace(Regex("\\s+"), " ")?.trim()?.takeIf { it.isNotBlank() }
        }
        val level = RequirementsLevelDto(field("OS"), field("Processor"), field("Graphics"), field("Memory"))
        return level.takeIf { listOf(it.os, it.cpu, it.gpu, it.ram).any { value -> value != null } } to field("Storage")
    }

    suspend fun searchApple(query: String): List<GameDto> {
        val items = json("https://itunes.apple.com/search?term=${URLEncoder.encode(query.ifBlank { "games" }, "UTF-8")}&entity=software&country=us&limit=100").optJSONArray("results")
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
