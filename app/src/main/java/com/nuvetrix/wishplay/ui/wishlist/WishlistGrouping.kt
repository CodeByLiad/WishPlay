package com.nuvetrix.wishplay.ui.wishlist

import com.nuvetrix.wishplay.domain.model.Game
import com.nuvetrix.wishplay.domain.model.ReleaseStatus
import com.nuvetrix.wishplay.domain.model.SortOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object WishlistGrouping {

    fun sortGames(
        games: List<Game>,
        sortOption: SortOption,
        today: String = "2026-09-30"
    ): List<Game> {
        return when (sortOption) {
            SortOption.NAME_A_TO_Z -> games.sortedBy { it.title.lowercase() }
            SortOption.RECENTLY_ADDED -> games.sortedByDescending { it.orderIndex }
            SortOption.NEAREST_RELEASE -> games.sortedWith { a, b ->
                val sa = if (a.status(today) == ReleaseStatus.OUT_NOW) 1 else 0
                val sb = if (b.status(today) == ReleaseStatus.OUT_NOW) 1 else 0
                if (sa != sb) {
                    sa - sb
                } else {
                    val ea = a.earliestDate ?: "9999-99-99"
                    val eb = b.earliestDate ?: "9999-99-99"
                    ea.compareTo(eb)
                }
            }
        }
    }

    data class MonthGroup(
        val header: String?,
        val games: List<Game>
    )

    fun groupGames(
        sortedGames: List<Game>,
        sortOption: SortOption,
        today: String = "2026-09-30"
    ): List<MonthGroup> {
        if (sortOption != SortOption.NEAREST_RELEASE) {
            return listOf(MonthGroup(header = null, games = sortedGames))
        }

        val groups = mutableListOf<MonthGroup>()
        var currentKey: String? = null
        var currentList = mutableListOf<Game>()

        for (game in sortedGames) {
            val key = when (game.status(today)) {
                ReleaseStatus.TBA -> "tba"
                ReleaseStatus.OUT_NOW -> "out"
                ReleaseStatus.UPCOMING -> {
                    val d = game.earliestDate
                    if (d != null && d.length >= 7) d.substring(0, 7) else "tba"
                }
            }

            if (currentKey != key) {
                if (currentKey != null && currentList.isNotEmpty()) {
                    groups.add(MonthGroup(header = formatHeader(currentKey), games = currentList))
                }
                currentKey = key
                currentList = mutableListOf()
            }
            currentList.add(game)
        }

        if (currentKey != null && currentList.isNotEmpty()) {
            groups.add(MonthGroup(header = formatHeader(currentKey), games = currentList))
        }

        return groups
    }

    private fun formatHeader(key: String): String = when (key) {
        "out" -> "Already out"
        "tba" -> "Date to be announced"
        else -> try {
            val ym = LocalDate.parse("$key-01")
            ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
        } catch (e: Exception) {
            key
        }
    }
}
