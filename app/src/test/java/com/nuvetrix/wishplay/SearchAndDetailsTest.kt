package com.nuvetrix.wishplay

import com.nuvetrix.wishplay.data.remote.dto.GameDto
import com.nuvetrix.wishplay.data.remote.dto.GamePriceDto
import com.nuvetrix.wishplay.data.remote.dto.RequirementsLevelDto
import com.nuvetrix.wishplay.data.remote.dto.SystemRequirementsDto
import com.nuvetrix.wishplay.data.repository.toDomainModel
import com.nuvetrix.wishplay.domain.model.ReleaseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchAndDetailsTest {

    @Test
    fun testDtoToDomainModelMapping() {
        val dto = GameDto(
            id = "test_game",
            title = "Cyber Odyssey",
            developer = "Neo Games",
            hueHex = "#1F7A6E",
            shapeKey = "c4",
            platforms = mapOf("PC" to "2026-11-20", "PS5" to null),
            storageSizes = mapOf("PC" to "45 GB"),
            about = "A cyberpunk adventure.",
            hasTrailer = true,
            trailerYoutubeId = "abc123xyz",
            requirements = SystemRequirementsDto(
                min = RequirementsLevelDto(
                    os = "Windows 10",
                    cpu = "Intel i5",
                    gpu = "GTX 1060",
                    ram = "8 GB"
                ),
                rec = RequirementsLevelDto(
                    os = "Windows 11",
                    cpu = "Intel i7",
                    gpu = "RTX 3070",
                    ram = "16 GB"
                )
            ),
            price = GamePriceDto(store = "Steam", now = "$39.99", was = "$49.99"),
            progress = 0.75f
        )

        val game = dto.toDomainModel(inWishlist = false)

        assertEquals("test_game", game.id)
        assertEquals("Cyber Odyssey", game.title)
        assertEquals("Neo Games", game.developer)
        assertTrue(game.hasTrailer)
        assertEquals("abc123xyz", game.trailerYoutubeId)
        assertNotNull(game.requirements)
        assertEquals("Windows 10", game.requirements?.min?.os)
        assertEquals("Intel i7", game.requirements?.rec?.cpu)
        assertNotNull(game.price)
        assertEquals("Steam", game.price?.store)
        assertEquals("$39.99", game.price?.now)
        assertEquals("$49.99", game.price?.was)
        assertFalse(game.inWishlist)
    }

    @Test
    fun testReleaseStatusCalculations() {
        val upcomingGame = GameDto(
            id = "up",
            title = "Upcoming Title",
            platforms = mapOf("PC" to "2026-10-15")
        ).toDomainModel()

        assertEquals(ReleaseStatus.UPCOMING, upcomingGame.status("2026-09-30"))
        assertEquals(15, upcomingGame.daysUntilRelease("2026-09-30"))

        val outNowGame = GameDto(
            id = "out",
            title = "Out Title",
            platforms = mapOf("PC" to "2026-09-15")
        ).toDomainModel()

        assertEquals(ReleaseStatus.OUT_NOW, outNowGame.status("2026-09-30"))
        assertTrue(outNowGame.daysUntilRelease("2026-09-30")!! <= 0)

        val tbaGame = GameDto(
            id = "tba",
            title = "TBA Title",
            platforms = mapOf("PC" to null)
        ).toDomainModel()

        assertEquals(ReleaseStatus.TBA, tbaGame.status("2026-09-30"))
        assertNull(tbaGame.daysUntilRelease("2026-09-30"))
    }
}
