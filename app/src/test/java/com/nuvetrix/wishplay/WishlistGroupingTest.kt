package com.nuvetrix.wishplay

import com.nuvetrix.wishplay.domain.model.Game
import com.nuvetrix.wishplay.domain.model.ReleaseStatus
import com.nuvetrix.wishplay.domain.model.SortOption
import com.nuvetrix.wishplay.ui.wishlist.WishlistGrouping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WishlistGroupingTest {

    private val today = "2026-09-30"

    private val gameOutNow = Game(
        id = "1",
        title = "Tidebound",
        platforms = mapOf("Android" to "2026-09-18"),
        orderIndex = 1
    )

    private val gameNearFuture = Game(
        id = "2",
        title = "Pocket Kingdoms",
        platforms = mapOf("Android" to "2026-10-02"),
        orderIndex = 2
    )

    private val gameLaterFuture = Game(
        id = "3",
        title = "Starfall Odyssey",
        platforms = mapOf("PC" to "2026-11-13"),
        orderIndex = 3
    )

    private val gameTba = Game(
        id = "4",
        title = "Hollow Keep",
        platforms = mapOf("PC" to null),
        expectedYear = "2027",
        orderIndex = 4
    )

    @Test
    fun `sort by name A to Z sorts alphabetically`() {
        val list = listOf(gameLaterFuture, gameNearFuture, gameTba, gameOutNow)
        val sorted = WishlistGrouping.sortGames(list, SortOption.NAME_A_TO_Z, today)

        assertEquals("Hollow Keep", sorted[0].title)
        assertEquals("Pocket Kingdoms", sorted[1].title)
        assertEquals("Starfall Odyssey", sorted[2].title)
        assertEquals("Tidebound", sorted[3].title)
    }

    @Test
    fun `sort by recently added puts highest orderIndex first`() {
        val list = listOf(gameOutNow, gameNearFuture, gameLaterFuture, gameTba)
        val sorted = WishlistGrouping.sortGames(list, SortOption.RECENTLY_ADDED, today)

        assertEquals("4", sorted[0].id)
        assertEquals("3", sorted[1].id)
        assertEquals("2", sorted[2].id)
        assertEquals("1", sorted[3].id)
    }

    @Test
    fun `sort by nearest release puts upcoming first and out now last`() {
        val list = listOf(gameOutNow, gameLaterFuture, gameTba, gameNearFuture)
        val sorted = WishlistGrouping.sortGames(list, SortOption.NEAREST_RELEASE, today)

        assertEquals("Pocket Kingdoms", sorted[0].title) // 2026-10-02
        assertEquals("Starfall Odyssey", sorted[1].title) // 2026-11-13
        assertEquals("Hollow Keep", sorted[2].title)      // TBA
        assertEquals("Tidebound", sorted[3].title)        // 2026-09-18 (Out now)
    }

    @Test
    fun `group by date groups items into month headers`() {
        val list = listOf(gameNearFuture, gameLaterFuture, gameTba, gameOutNow)
        val sorted = WishlistGrouping.sortGames(list, SortOption.NEAREST_RELEASE, today)
        val groups = WishlistGrouping.groupGames(sorted, SortOption.NEAREST_RELEASE, today)

        assertEquals(4, groups.size)
        assertEquals("October 2026", groups[0].header)
        assertEquals(1, groups[0].games.size)
        assertEquals("Pocket Kingdoms", groups[0].games[0].title)

        assertEquals("November 2026", groups[1].header)
        assertEquals("Starfall Odyssey", groups[1].games[0].title)

        assertEquals("Date to be announced", groups[2].header)
        assertEquals("Hollow Keep", groups[2].games[0].title)

        assertEquals("Already out", groups[3].header)
        assertEquals("Tidebound", groups[3].games[0].title)
    }

    @Test
    fun `non date sort does not group by month`() {
        val list = listOf(gameNearFuture, gameLaterFuture)
        val sorted = WishlistGrouping.sortGames(list, SortOption.NAME_A_TO_Z, today)
        val groups = WishlistGrouping.groupGames(sorted, SortOption.NAME_A_TO_Z, today)

        assertEquals(1, groups.size)
        assertNull(groups[0].header)
        assertEquals(2, groups[0].games.size)
    }
}
