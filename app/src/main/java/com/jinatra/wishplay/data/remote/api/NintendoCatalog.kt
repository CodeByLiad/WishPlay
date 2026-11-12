package com.jinatra.wishplay.data.remote.api

import com.jinatra.wishplay.data.remote.dto.GameDto
import com.jinatra.wishplay.data.remote.dto.GameSearchPage
import com.jinatra.wishplay.domain.model.ReleaseEvidence
import com.jinatra.wishplay.domain.model.validReleaseDate
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.net.URLEncoder

/** Nintendo's public European catalog, including physical games and Switch 2 editions. */
internal class NintendoCatalog(private val client: OkHttpClient) {
    suspend fun search(query: String, platform: String, page: Int): GameSearchPage {
        val filter = when (platform) {
            "Switch" -> "type:GAME AND system_type:*nintendoswitch* AND NOT system_type:*nintendoswitch2*"
            "Switch 2" -> "type:GAME AND system_type:*nintendoswitch2*"
            else -> "type:GAME AND system_type:*nintendoswitch*"
        }
        // Dismax searches title fields. Escape query syntax so a typed title remains text.
        val text = query.trim().replace(Regex("([+\\-!(){}\\[\\]^\"~*?:\\\\/|&])"), "\\\\$1")
        val params = linkedMapOf("q" to text.ifBlank { "*:*" }, "defType" to "edismax",
            "qf" to "title^10 title_extras_txt^5", "fq" to filter,
            "start" to ((page.coerceAtLeast(1) - 1) * PAGE_SIZE).toString(), "rows" to PAGE_SIZE.toString(), "wt" to "json")
        return parsePage(client.catalogJson(url(params)), platform)
    }

    suspend fun details(id: String): GameDto? {
        val number = id.removePrefix("nintendo_").takeIf { it.matches(Regex("[0-9]+")) } ?: return null
        val json = client.catalogJson(url(mapOf("q" to "fs_id:$number", "fq" to "type:GAME", "rows" to "1", "wt" to "json")))
        return json.optJSONObject("response")?.optJSONArray("docs")?.optJSONObject(0)?.let(::mapGame)
    }

    internal fun parsePage(json: JSONObject, platform: String): GameSearchPage {
        val response = json.getJSONObject("response")
        val docs = response.getJSONArray("docs")
        val games = (0 until docs.length()).mapNotNull { mapGame(docs.getJSONObject(it)) }
            .filter { platform == "All" || platform in it.platforms }
        return GameSearchPage(games, hasMore = response.optInt("start") + docs.length() < response.optInt("numFound"))
    }

    internal fun mapGame(item: JSONObject): GameDto? {
        if (item.optString("type", "GAME") != "GAME") return null
        val id = item.optString("fs_id").takeIf { it.matches(Regex("[0-9]+")) } ?: return null
        val names = item.optJSONArray("system_names_txt") ?: return null
        val platforms = (0 until names.length()).mapNotNull {
            when (names.optString(it)) { "Nintendo Switch" -> "Switch"; "Nintendo Switch 2" -> "Switch 2"; else -> null }
        }.distinct()
        if (platforms.isEmpty()) return null
        val title = item.optString("title").takeIf { it.isNotBlank() } ?: return null
        val date = validReleaseDate(item.optString("date_from").take(10))
        val path = item.optString("url").takeIf { it.startsWith("/en-gb/Games/") } ?: return null
        val source = "https://www.nintendo.com$path"
        return GameDto(id = "nintendo_$id", title = title,
            developer = item.optString("developer").ifBlank { item.optString("publisher") }.ifBlank { null },
            coverUrl = sequenceOf("image_url_sq_s", "image_url", "image_url_h2x1_s", "image_url_h16x9_s")
                .map { item.optString(it) }.firstOrNull { it.startsWith("https://") },
            about = item.optString("excerpt").ifBlank { item.optString("product_catalog_description_s") }.ifBlank { null },
            platforms = platforms.associateWith { date },
            releaseEvidence = platforms.associateWith { listOf(ReleaseEvidence("Nintendo (Europe)", date, source)) })
    }

    private fun url(params: Map<String, String>) = "https://search.nintendo-europe.com/en/select?" +
        params.entries.joinToString("&") { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }

    companion object { private const val PAGE_SIZE = 40 }
}
