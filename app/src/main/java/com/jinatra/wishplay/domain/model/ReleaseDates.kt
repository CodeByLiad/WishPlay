package com.jinatra.wishplay.domain.model

import java.time.LocalDate

fun validReleaseDate(value: String?): String? = value?.let {
    runCatching { LocalDate.parse(it).toString() }.getOrNull()
}

/** A missing port date is unknown, even if another edition has already shipped. */
fun Map<String, String?>.nextReleaseDate(today: String = LocalDate.now().toString()): String? =
    values.mapNotNull(::validReleaseDate).filter { it >= today }.minOrNull()

fun Map<String, String?>.displayReleaseDate(today: String = LocalDate.now().toString()): String? =
    nextReleaseDate(today) ?: if (values.any { validReleaseDate(it) == null }) null
    else values.mapNotNull(::validReleaseDate).minOrNull()

fun Game.forPlatform(platform: String): Game = if (platform == "All") this else
    copy(platforms = platforms.filterKeys { it.equals(platform, ignoreCase = true) }, trackedPlatforms = null)

fun Map<String, String?>.releaseGroups(): Map<String, List<String>> = entries
    .filter { validReleaseDate(it.value) != null }
    .groupBy({ it.value!! }, { it.key }).toSortedMap()
