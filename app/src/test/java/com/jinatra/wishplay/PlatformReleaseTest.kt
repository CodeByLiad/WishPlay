package com.jinatra.wishplay

import com.jinatra.wishplay.domain.model.*
import com.jinatra.wishplay.ui.wishlist.WishlistGrouping
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PlatformReleaseTest {
    private val ports = mapOf("PC" to "2025-01-01", "Android" to "2027-04-20", "PS5" to null)
    private val game = Game(id = "ports", title = "Ports", platforms = ports)

    @Test fun `upcoming port survives an older PC release`() {
        assertEquals(ReleaseStatus.UPCOMING, game.status("2026-10-18"))
        assertEquals("2027-04-20", ports.nextReleaseDate("2026-10-18"))
        assertEquals(ReleaseStatus.OUT_NOW, game.forPlatform("PC").status("2026-10-18"))
        assertEquals(ReleaseStatus.TBA, game.forPlatform("PS5").status("2026-10-18"))
        assertNull(game.forPlatform("PS5").daysUntilRelease("2026-10-18"))
    }

    @Test fun `unknown port remains TBA after other ports have shipped`() {
        assertEquals(ReleaseStatus.TBA, game.status("2028-01-01"))
        assertNull(ports.displayReleaseDate("2028-01-01"))
    }

    @Test fun `release groups share reminders only for matching dates`() {
        val groups = (ports + ("iOS" to "2027-04-20") + ("Invalid" to "null")).releaseGroups()
        assertEquals(2, groups.size)
        assertEquals(listOf("Android", "iOS"), groups["2027-04-20"])
        assertFalse(groups.values.flatten().contains("PS5"))
        assertFalse(groups.values.flatten().contains("Invalid"))
    }

    @Test fun `invalid dates never create releases`() {
        listOf("", "null", "TBA", "2027-02-30", "2027", "2027-01-01T00:00:00Z").forEach {
            assertNull(validReleaseDate(it))
        }
        assertEquals("2028-02-29", validReleaseDate("2028-02-29"))
    }

    @Test fun `default countdown uses device date`() {
        val tomorrow = Game("tomorrow", "Tomorrow", platforms = mapOf("PC" to LocalDate.now().plusDays(1).toString()))
        assertEquals(1, tomorrow.daysUntilRelease())
    }

    @Test fun `sort and grouping use the next port at the provided clock`() {
        val later = Game("later", "Later", platforms = mapOf("PC" to "2027-06-01"))
        val sorted = WishlistGrouping.sortGames(listOf(later, game), SortOption.NEAREST_RELEASE, "2026-10-18")
        assertEquals("ports", sorted.first().id)
        assertEquals("April 2027", WishlistGrouping.groupGames(sorted, SortOption.NEAREST_RELEASE, "2026-10-18").first().header)
    }
}
