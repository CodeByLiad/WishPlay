package com.nuvetrix.wishplay

import com.nuvetrix.wishplay.domain.model.Game
import com.nuvetrix.wishplay.domain.model.ReleaseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AlertSchedulingTest {

    @Test
    fun testEarliestDateCalculation() {
        val game = Game(
            id = "test_game",
            title = "Test Game",
            platforms = mapOf(
                "PC" to "2026-11-13",
                "PS5" to "2026-10-09",
                "Xbox" to "2026-12-01",
                "Switch" to null
            )
        )

        assertEquals("2026-10-09", game.earliestDate)
        assertEquals(ReleaseStatus.UPCOMING, game.status("2026-09-30"))
        assertEquals(9, game.daysUntilRelease("2026-09-30"))
    }

    @Test
    fun testLeadTimeCalculations() {
        val releaseDateStr = "2026-10-09"
        val releaseDate = LocalDate.parse(releaseDateStr)

        val oneDayPrior = releaseDate.minusDays(1)
        assertEquals(LocalDate.parse("2026-10-08"), oneDayPrior)

        val threeDaysPrior = releaseDate.minusDays(3)
        assertEquals(LocalDate.parse("2026-10-06"), threeDaysPrior)

        val oneWeekPrior = releaseDate.minusDays(7)
        assertEquals(LocalDate.parse("2026-10-02"), oneWeekPrior)
    }

    @Test
    fun testReleaseAlertNineAmEpoch() {
        val releaseDate = LocalDate.parse("2026-10-09")
        val nineAm = releaseDate.atTime(9, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        assertTrue(nineAm > 0)
        val checkDate = java.time.Instant.ofEpochMilli(nineAm)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        assertEquals(9, checkDate.hour)
        assertEquals(0, checkDate.minute)
        assertEquals(9, checkDate.dayOfMonth)
        assertEquals(10, checkDate.monthValue)
        assertEquals(2026, checkDate.year)
    }

    @Test
    fun testDateChangeDetection() {
        val oldPlatforms = mapOf("PS5" to "2026-10-22", "PC" to "2026-10-22")
        val newPlatforms = mapOf("PS5" to "2026-12-03", "PC" to "2026-12-03")

        val oldEarliest = oldPlatforms.values.filterNotNull().sorted().firstOrNull()
        val newEarliest = newPlatforms.values.filterNotNull().sorted().firstOrNull()

        assertNotNull(oldEarliest)
        assertNotNull(newEarliest)
        assertTrue(oldEarliest != newEarliest)

        // Verifying delay detection
        val isDelayed = newEarliest!! > oldEarliest!!
        assertTrue(isDelayed)
    }

    @Test
    fun testTBADateChangeDetection() {
        val oldPlatforms = mapOf("Switch 2" to "2026-12-15")
        val newPlatforms = mapOf("Switch 2" to null) // Moved back to TBA

        val oldEarliest = oldPlatforms.values.filterNotNull().sorted().firstOrNull()
        val newEarliest = newPlatforms.values.filterNotNull().sorted().firstOrNull()

        assertEquals("2026-12-15", oldEarliest)
        assertNull(newEarliest)
        assertTrue(oldEarliest != newEarliest)
    }
}
