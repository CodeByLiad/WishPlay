package com.jinatra.wishplay.data.remote.api

import com.jinatra.wishplay.data.remote.dto.GameDto
import com.jinatra.wishplay.data.remote.dto.GameSearchPage
import com.jinatra.wishplay.domain.model.ReleaseEvidence
import com.jinatra.wishplay.domain.model.validReleaseDate
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Base64

/** Public Epic catalog. Platform tags and game categories are required, not inferred from title. */
internal class EpicCatalog(private val client: OkHttpClient) {
    private val recent = object : LinkedHashMap<String, GameDto>(64, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, GameDto>?) = size > 200
    }

    suspend fun search(query: String, platform: String, page: Int): GameSearchPage {
        val start = (page.coerceAtLeast(1) - 1) * PAGE_SIZE
        val request = """query { Catalog { searchStore(keywords:${JSONObject.quote(query.trim())}, country:"US", locale:"en-US", count:$PAGE_SIZE, start:$start) { paging {total} elements { $FIELDS } } } }"""
        val json = client.catalogJson("https://store.epicgames.com/graphql?query=${URLEncoder.encode(request, "UTF-8")}")
            .getJSONObject("data").getJSONObject("Catalog").getJSONObject("searchStore")
        val elements = json.getJSONArray("elements")
        val games = elements.objects().mapNotNull(::mapGame)
        synchronized(recent) { games.forEach { recent[it.id] = it } }
        return GameSearchPage(games.filter { platform == "All" || platform in it.platforms },
            hasMore = start + elements.length() < json.getJSONObject("paging").getInt("total"))
    }

    fun cachedDetails(id: String): GameDto? = synchronized(recent) { recent[id] }

    /** A stored title is a lookup hint only; the stable namespace/offer identity must still match. */
    suspend fun details(id: String, titleHint: String?): GameDto? {
        if (titleHint.isNullOrBlank()) return cachedDetails(id)
        return search(titleHint, "All", 1).games.firstOrNull { it.id == id }
    }

    internal fun mapGame(item: JSONObject): GameDto? {
        val categories = item.optJSONArray("categories").objects().map { it.optString("path") }
        if ("games" !in categories || categories.any { it == "addons" || it == "subscription" }) return null
        val tags = item.optJSONArray("tags").objects().filter { it.optString("groupName") == "platform" }.map { it.optString("name") }
        val platforms = tags.mapNotNull { when (it) { "Windows" -> "PC"; "Android" -> "Android"; "iOS" -> "iOS"; else -> null } }.distinct()
        if (platforms.isEmpty()) return null
        val offer = item.optString("id").takeIf { it.matches(Regex("[a-fA-F0-9]{32}")) } ?: return null
        val namespace = item.optString("namespace").takeIf { it.matches(Regex("[A-Za-z0-9-]{1,100}")) } ?: return null
        val title = item.optString("title").takeIf { it.isNotBlank() } ?: return null
        val slug = item.optJSONArray("offerMappings").objects().firstOrNull { it.optString("pageType") == "productHome" }
            ?.optString("pageSlug")?.takeIf { it.matches(SLUG) }
            ?: item.optString("productSlug").takeIf { it.matches(SLUG) } ?: return null
        val url = "https://store.epicgames.com/en-US/p/$slug"
        val date = validReleaseDate(item.optString("releaseDate").take(10))
        val dates = platforms.associateWith { if (it == "PC") date else null }
        val attributes = item.optJSONArray("customAttributes").objects().associate { it.optString("key") to it.optString("value") }
        val image = item.optJSONArray("keyImages").objects().sortedBy {
            when (it.optString("type")) { "DieselGameBoxTall" -> 0; "Thumbnail" -> 1; "OfferImageTall" -> 2; else -> 3 }
        }.firstNotNullOfOrNull { it.optString("url").takeIf { url -> url.startsWith("https://") } }
        return GameDto(id = "epic_" + Base64.getUrlEncoder().withoutPadding().encodeToString("$namespace:$offer".toByteArray(Charsets.UTF_8)),
            title = title, coverUrl = image, developer = attributes["developerName"] ?: attributes["publisherName"],
            about = item.optString("description").ifBlank { null }, platforms = dates,
            releaseEvidence = dates.mapValues { (_, release) -> listOf(ReleaseEvidence("Epic Games Store (US)", release, url)) })
    }

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
    companion object {
        private const val PAGE_SIZE = 40
        private val SLUG = Regex("[a-z0-9][a-z0-9-]{0,200}")
        private const val FIELDS = "id title namespace description keyImages {type url} releaseDate productSlug categories {path} tags {name groupName} customAttributes {key value} offerMappings {pageSlug pageType}"
    }
}
