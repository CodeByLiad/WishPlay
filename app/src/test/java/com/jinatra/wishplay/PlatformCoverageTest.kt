package com.jinatra.wishplay

import com.jinatra.wishplay.data.remote.api.selectOtherPlatformGames
import com.jinatra.wishplay.data.remote.dto.GameDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformCoverageTest {

    private fun game(id: String, title: String, vararg platforms: String) = GameDto(
        id = id,
        title = title,
        platforms = platforms.associateWith { null }
    )

    @Test
    fun titleMissingTheSelectedPlatformIsOffered() {
        val primary = listOf(game("rawg_1", "Avernum 6", "PC"))
        val candidates = listOf(game("rawg_2", "Grand Theft Auto VI", "Xbox Series", "PS5"))

        val result = selectOtherPlatformGames(primary, candidates, "PC")

        assertEquals(listOf("rawg_2"), result.map { it.id })
    }

    @Test
    fun titlesAlreadyInThePrimaryListAreExcluded() {
        val shown = game("rawg_1", "Palworld", "PC")

        val result = selectOtherPlatformGames(listOf(shown), listOf(shown), "PC")

        assertTrue(result.isEmpty())
    }

    @Test
    fun candidateThatAlreadyClaimsTheSelectedPlatformIsExcluded() {
        val candidates = listOf(game("rawg_9", "Helldivers 2", "PC", "PS5"))

        val result = selectOtherPlatformGames(emptyList(), candidates, "PC")

        assertTrue(result.isEmpty())
    }

    @Test
    fun resultIsCappedAtTenAndKeepsCandidateOrder() {
        val candidates = (1..15).map { game("rawg_$it", "Game $it", "PS5") }

        val result = selectOtherPlatformGames(emptyList(), candidates, "PC")

        assertEquals(10, result.size)
        assertEquals("rawg_1", result.first().id)
        assertEquals("rawg_10", result.last().id)
    }

    @Test
    fun duplicateCandidateIdsAreCollapsed() {
        val candidate = game("rawg_3", "Wuthering Waves", "PS5")

        val result = selectOtherPlatformGames(emptyList(), listOf(candidate, candidate), "PC")

        assertEquals(1, result.size)
    }

    @Test
    fun allPlatformMeansNoSection() {
        val candidates = listOf(game("rawg_2", "Grand Theft Auto VI", "Xbox Series", "PS5"))

        assertTrue(selectOtherPlatformGames(emptyList(), candidates, "All").isEmpty())
    }
}
