package com.jinatra.wishplay.data.remote.api

import com.jinatra.wishplay.data.remote.dto.GameDto

/**
 * Picks the search hits a platform filter excluded.
 *
 * RAWG's `platforms=` filter drops every title whose port list does not already name that
 * platform, so an unannounced port makes a game invisible rather than merely unavailable.
 * These are shown in their own section, never mixed into the filtered results.
 */
fun selectOtherPlatformGames(
    primary: List<GameDto>,
    candidates: List<GameDto>,
    selectedPlatform: String,
    limit: Int = 10
): List<GameDto> {
    if (selectedPlatform == "All" || limit <= 0) return emptyList()
    val alreadyShown = primary.map { it.id }.toSet()
    return candidates.asSequence()
        .filter { it.id !in alreadyShown }
        .filter { selectedPlatform !in it.platforms.keys }
        .distinctBy { it.id }
        .take(limit)
        .toList()
}
