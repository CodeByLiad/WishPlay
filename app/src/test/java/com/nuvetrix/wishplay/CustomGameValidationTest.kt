package com.nuvetrix.wishplay

import com.nuvetrix.wishplay.domain.model.Game
import com.nuvetrix.wishplay.domain.model.ReleaseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CustomGameValidationTest {

    private val today = "2026-09-30"

    @Test
    fun `custom game with valid date computes correct days until release`() {
        val game = Game(
            id = "c1",
            title = "Mango Rush",
            isCustom = true,
            platforms = mapOf("Android" to "2026-10-30")
        )

        assertEquals(30, game.daysUntilRelease(today))
        assertEquals(ReleaseStatus.UPCOMING, game.status(today))
        assertEquals("2026-10-30", game.earliestDate)
        assertTrue(game.isCustom)
    }

    @Test
    fun `custom game marked TBA has null earliest date and TBA status`() {
        val game = Game(
            id = "c2",
            title = "Unannounced Indie",
            isCustom = true,
            platforms = mapOf("Android" to null)
        )

        assertNull(game.earliestDate)
        assertEquals(ReleaseStatus.TBA, game.status(today))
        assertNull(game.daysUntilRelease(today))
    }

    @Test
    fun `quick date helper computes exact target dates from base date`() {
        val base = LocalDate.parse(today)
        val monthTarget = base.plusDays(30).toString()
        val threeMonthsTarget = base.plusDays(90).toString()
        val yearTarget = base.plusDays(365).toString()

        assertEquals("2026-10-30", monthTarget)
        assertEquals("2026-12-29", threeMonthsTarget)
        assertEquals("2027-09-30", yearTarget)
    }
}
