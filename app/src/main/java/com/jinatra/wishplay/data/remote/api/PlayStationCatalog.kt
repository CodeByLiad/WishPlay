package com.jinatra.wishplay.data.remote.api

import android.text.Html
import com.jinatra.wishplay.data.remote.dto.GameDto
import com.jinatra.wishplay.data.remote.dto.GameSearchPage
import com.jinatra.wishplay.domain.model.ReleaseEvidence
import com.jinatra.wishplay.domain.model.validReleaseDate
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/** Uses the public storefront's getSearchResults operation, with its published persisted hash. */
internal class PlayStationCatalog(private val client: OkHttpClient) {
    private val cursors = object : LinkedHashMap<String, String>(32, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 64
    }

    suspend fun search(query: String, platform: String, page: Int): GameSearchPage {
        val term = query.trim().ifBlank { "games" }
        val variables = JSONObject().put("countryCode", "US").put("languageCode", "en")
            .put("nextCursor", synchronized(cursors) { cursors["$term:$page"] }.orEmpty())
            .put("pageOffset", (page.coerceAtLeast(1) - 1) * PAGE_SIZE).put("pageSize", PAGE_SIZE).put("searchTerm", term)
        val extensions = JSONObject().put("persistedQuery", JSONObject().put("version", 1).put("sha256Hash", SEARCH_HASH))
        val params = mapOf("operationName" to "getSearchResults", "variables" to variables.toString(), "extensions" to extensions.toString())
        val json = client.catalogJson("https://web.np.playstation.com/api/graphql/v1/op?" + params.entries.joinToString("&") {
            (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}"
        }, mapOf("Content-Type" to "application/json"))
        val results = json.getJSONObject("data").getJSONObject("universalSearch")
        results.optString("next").takeIf { it.isNotBlank() }?.let { next -> synchronized(cursors) { cursors["$term:${page + 1}"] = next } }
        return parsePage(results, platform)
    }

    internal fun parsePage(results: JSONObject, platform: String): GameSearchPage {
        val rows = results.getJSONArray("results")
        val games = (0 until rows.length()).mapNotNull { mapGame(rows.getJSONObject(it)) }
            .filter { platform == "All" || platform in it.platforms }
        return GameSearchPage(games, hasMore = !results.getJSONObject("pageInfo").getBoolean("isLast"))
    }

    suspend fun details(id: String): GameDto? {
        val productId = id.removePrefix("ps_").takeIf { it.matches(PRODUCT_ID) } ?: return null
        val html = client.catalogText("https://store.playstation.com/en-us/product/$productId")
        return parseDetails(html, productId)
    }

    internal fun parseDetails(html: String, productId: String): GameDto? {
        val combined = JSONObject()
        // Product pages publish normalized JSON caches in independent, server-rendered modules.
        Regex("<script[^>]*type=\"application/json\"[^>]*>([\\s\\S]*?)</script>").findAll(html).forEach { match ->
            val product = runCatching { JSONObject(match.groupValues[1]).optJSONObject("cache")?.optJSONObject("Product:$productId") }.getOrNull()
            product?.keys()?.forEach { key -> if (!product.isNull(key)) combined.put(key, product.get(key)) }
        }
        val game = mapGame(combined) ?: return null
        val date = validReleaseDate(combined.optString("releaseDate").take(10))
        val descriptions = combined.optJSONArray("descriptions")
        val about = descriptions.objects().firstOrNull { it.optString("type") == "LONG" }?.optString("value")
        return game.copy(developer = combined.optString("publisherName").ifBlank { null },
            about = about?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().trim() },
            platforms = game.platforms.mapValues { date },
            releaseEvidence = game.platforms.keys.associateWith {
                listOf(ReleaseEvidence("PlayStation Store (US)", date, "https://store.playstation.com/en-us/product/$productId"))
            })
    }

    internal fun mapGame(item: JSONObject): GameDto? {
        if (item.optString("storeDisplayClassification") !in setOf("FULL_GAME", "PREMIUM_EDITION", "GAME_BUNDLE")) return null
        val id = item.optString("id").takeIf { it.matches(PRODUCT_ID) } ?: return null
        val title = item.optString("name").takeIf { it.isNotBlank() } ?: return null
        val names = item.optJSONArray("platforms") ?: return null
        val platforms = (0 until names.length()).map { names.optString(it) }.filter { it in listOf("PS4", "PS5") }.distinct()
        if (platforms.isEmpty()) return null
        val image = item.optJSONArray("media").objects().filter { it.optString("type") == "IMAGE" }.sortedBy {
            when (it.optString("role")) { "MASTER" -> 0; "GAMEHUB_COVER_ART" -> 1; "PORTRAIT_BANNER" -> 2; else -> 3 }
        }.firstNotNullOfOrNull { it.optString("url").takeIf { url -> url.startsWith("https://") } }
        return GameDto(id = "ps_$id", title = title, coverUrl = image, platforms = platforms.associateWith { null },
            releaseEvidence = platforms.associateWith {
                listOf(ReleaseEvidence("PlayStation Store (US)", null, "https://store.playstation.com/en-us/product/$id"))
            })
    }

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
    companion object {
        private const val PAGE_SIZE = 24
        // Verified against the storefront's public bundle on 2026-11-11. A failed provider stays isolated.
        private const val SEARCH_HASH = "4df6284f982e57bec70f23c77e2c219dc792eb19af7fb3d3a81767aa3f1958aa"
        private val PRODUCT_ID = Regex("[A-Z]{2}[0-9]{4}-[A-Z0-9]{9}_[0-9]{2}-[A-Z0-9]{16}")
    }
}
