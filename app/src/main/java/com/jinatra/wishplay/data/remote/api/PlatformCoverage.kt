package com.jinatra.wishplay.data.remote.api

import com.jinatra.wishplay.data.remote.dto.GameDto
import com.jinatra.wishplay.domain.model.catalogTitleKey

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
    val shownIds = primary.map { it.id }.toSet()
    // Match on title as well as id: the store passes key their rows steam_<appid> / apple_<id>
    // while these candidates are keyed rawg_<id>, so an id-only check would render the same game
    // twice whenever RAWG's port list lags behind the store — the very case this exists for.
    // Empty keys are dropped: a symbol-only title normalises to "", and keeping it would make one
    // such row suppress every other empty-keyed candidate.
    val shownTitles = primary.map { catalogTitleKey(it.title) }.filter { it.isNotEmpty() }.toSet()
    return candidates.asSequence()
        .filter { it.id !in shownIds }
        .filter { catalogTitleKey(it.title).let { key -> key.isEmpty() || key !in shownTitles } }
        .filter { selectedPlatform !in it.platforms.keys }
        .distinctBy { it.id }
        // Regional and re-release entries share a title; show the best-ranked one only.
        .distinctBy { catalogTitleKey(it.title).ifEmpty { it.id } }
        .take(limit)
        .toList()
}
