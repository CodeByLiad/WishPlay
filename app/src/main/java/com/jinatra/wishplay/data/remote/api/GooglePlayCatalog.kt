package com.jinatra.wishplay.data.remote.api

import com.jinatra.wishplay.data.remote.dto.GameDto
import com.jinatra.wishplay.domain.model.ReleaseEvidence
import com.jinatra.wishplay.domain.model.catalogTitleKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.Base64
import java.util.concurrent.TimeUnit

/** Reads public Google Play listings. Store-only entries never invent a release date. */
internal class GooglePlayCatalog(client: OkHttpClient) {
    private val client = client.newBuilder().callTimeout(6, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false).build()
    private val games = setOf("Action", "Adventure", "Arcade", "Board", "Card", "Casino", "Casual",
        "Educational", "Music", "Puzzle", "Racing", "Role Playing", "Simulation", "Sports", "Strategy", "Trivia", "Word")
    private val packagePattern = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
    private val dataPattern = Regex("AF_initDataCallback\\(\\{key:\\s*'[^']+'[\\s\\S]*?data:([\\s\\S]*?),\\s*sideChannel:\\s*\\{\\}\\}\\);")
    private val metadataPattern = Regex("<script[^>]*type=\"application/ld\\+json\"[^>]*>([\\s\\S]*?)</script>")

    suspend fun search(query: String): List<GameDto> = withContext(Dispatchers.IO) {
        val term = URLEncoder.encode(query.trim().ifBlank { "games" }, "UTF-8")
        parseSearch(client.catalogText("https://play.google.com/store/search?q=$term&c=apps&hl=en&gl=US"), query)
    }

    internal fun parseSearch(html: String, query: String = ""): List<GameDto> {
        val result = linkedMapOf<String, GameDto>()
        var parsedPayload = false
        fun visit(node: JSONArray, depth: Int) {
            if (depth > 70) return
            // Play renders an exact/featured match as a full app record, rather than
            // the compact records used by the recommendation list below it.
            val featured = node.optJSONArray(2)
            val featuredPackage = node.optJSONObject(3)?.optJSONArray("12")?.optJSONArray(0)?.optString(0).orEmpty()
            if (featured != null && packagePattern.matches(featuredPackage)) {
                val name = featured.optJSONArray(0)?.optString(0).orEmpty()
                val category = featured.optJSONArray(79)?.optJSONArray(0)?.optJSONArray(0)?.optString(2).orEmpty()
                val image = featured.optJSONArray(95)?.optJSONArray(0)?.optJSONArray(3)?.optString(2)
                if (name.isNotBlank() && category.startsWith("GAME_") &&
                    image?.startsWith("https://play-lh.googleusercontent.com/") == true) {
                    val dto = game(featuredPackage, name, image).copy(
                        developer = featured.optJSONArray(68)?.optString(0))
                    result.putIfAbsent(dto.id, dto)
                }
            }
            val pkg = node.optJSONArray(0)?.optString(0).orEmpty()
            val title = node.opt(3) as? String
            val genre = node.opt(5) as? String
            val icon = node.optJSONArray(1)?.optJSONArray(3)?.optString(2)
            if (packagePattern.matches(pkg) && !title.isNullOrBlank() && genre in games &&
                icon?.startsWith("https://play-lh.googleusercontent.com/") == true) {
                val dto = game(pkg, title, icon)
                result.putIfAbsent(dto.id, dto)
                return
            }
            for (index in 0 until node.length()) node.optJSONArray(index)?.let { visit(it, depth + 1) }
        }
        dataPattern.findAll(html).forEach { match ->
            val payload = runCatching { JSONArray(match.groupValues[1]) }.getOrNull()
            if (payload != null) { parsedPayload = true; visit(payload, 0) }
        }
        if (!parsedPayload) throw IOException("Google Play catalog could not be read")
        val key = catalogTitleKey(query).replace(" ", "")
        return result.values.sortedBy { game ->
            val title = catalogTitleKey(game.title).replace(" ", "")
            when {
                key.isBlank() -> 0
                title == key -> 0
                title.startsWith(key) -> 1
                title.contains(key) -> 2
                else -> 3
            }
        }.take(40)
    }

    suspend fun details(id: String): GameDto? = withContext(Dispatchers.IO) {
        val pkg = runCatching { String(Base64.getUrlDecoder().decode(id.removePrefix("play_")), Charsets.UTF_8) }.getOrNull()
            ?.takeIf { packagePattern.matches(it) } ?: return@withContext null
        val html = client.catalogText("https://play.google.com/store/apps/details?id=${URLEncoder.encode(pkg, "UTF-8")}&hl=en&gl=US")
        val metadata = metadataPattern.findAll(html).mapNotNull {
            runCatching { JSONObject(it.groupValues[1]) }.getOrNull()
        }.firstOrNull { it.optString("applicationCategory").startsWith("GAME") }
            ?: throw IOException("Google Play game details are unavailable")
        val image = metadata.optString("image").takeIf { it.startsWith("https://") }
        game(pkg, metadata.getString("name"), image).copy(
            developer = metadata.optJSONObject("author")?.optString("name"),
            about = metadata.optString("description").takeIf { it.length > 40 }
        )
    }

    private fun game(pkg: String, title: String, icon: String?) = GameDto(
        id = "play_" + Base64.getUrlEncoder().withoutPadding().encodeToString(pkg.toByteArray(Charsets.UTF_8)),
        title = title, coverUrl = icon, logoUrl = icon, platforms = mapOf("Android" to null),
        releaseEvidence = mapOf("Android" to listOf(ReleaseEvidence("Google Play (US)", null,
            "https://play.google.com/store/apps/details?id=$pkg")))
    )
}
