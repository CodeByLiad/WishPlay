package com.jinatra.wishplay.data.remote.api

import com.jinatra.wishplay.data.remote.dto.GameDto
import com.jinatra.wishplay.domain.model.catalogTitleKey
import java.net.URI
import java.util.Locale

/** Names alone cannot establish identity: unrelated games and editions can share a title. */
internal fun mergeVerifiedListings(games: List<GameDto>): List<GameDto> {
    val result = mutableListOf<GameDto>()
    val titleKeys = hashMapOf<String, String>()
    fun titleKey(title: String) = titleKeys.getOrPut(title) { catalogTitleKey(title) }
    for (game in games) {
        val urls = game.listingUrls()
        val key = titleKey(game.title)
        val index = result.indexOfFirst { existing ->
            existing.id == game.id || (key.isNotEmpty() && titleKey(existing.title) == key &&
                existing.platforms.keys == game.platforms.keys && urls.isNotEmpty() &&
                existing.listingUrls().any { it in urls })
        }
        if (index < 0) { result.add(game); continue }
        val first = result[index]
        result[index] = first.copy(
            developer = first.developer ?: game.developer,
            coverUrl = first.coverUrl ?: game.coverUrl,
            logoUrl = first.logoUrl ?: game.logoUrl,
            about = first.about ?: game.about,
            platforms = (game.platforms + first.platforms).mapValues { (platform, date) -> date ?: game.platforms[platform] },
            requirements = first.requirements ?: game.requirements,
            storageSizes = game.storageSizes + first.storageSizes.filterValues { it != null },
            releaseEvidence = (first.releaseEvidence.keys + game.releaseEvidence.keys).associateWith {
                (first.releaseEvidence[it].orEmpty() + game.releaseEvidence[it].orEmpty()).distinct()
            }
        )
    }
    return result
}

private fun GameDto.listingUrls(): Set<String> = releaseEvidence.values.flatten().mapNotNull { evidence ->
    runCatching {
        val uri = URI(evidence.url).normalize()
        if (!uri.scheme.equals("https", true) || uri.host.isNullOrBlank()) null else
            "https://${uri.host.lowercase(Locale.ROOT)}" +
                (if (uri.port == -1 || uri.port == 443) "" else ":${uri.port}") +
                uri.rawPath.orEmpty().trimEnd('/') + (uri.rawQuery?.let { "?$it" } ?: "")
    }.getOrNull()
}.toSet()
