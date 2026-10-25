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
    fun storeSourcedPrimaryRowSuppressesItsRawgTwin() {
        // The store passes key their rows steam_<appid> while RAWG keys rawg_<id>, so an id-only
        // dedupe lets the same game render twice: once as a Steam PC row, once under a section
        // whose subtitle says it is not on PC. Exactly the case this feature exists for.
        val steamRow = game("steam_413150", "Palworld", "PC")
        val rawgTwin = game("rawg_58812", "Palworld", "PS5")

        val result = selectOtherPlatformGames(listOf(steamRow), listOf(rawgTwin), "PC")

        assertTrue(result.isEmpty())
    }

    @Test
    fun titleMatchIgnoresPunctuationAndCase() {
        val steamRow = game("steam_1", "Ori and the Will of the Wisps", "PC")
        val rawgTwin = game("rawg_1", "Ori and the Will of the Wisps™", "Switch")

        val result = selectOtherPlatformGames(listOf(steamRow), listOf(rawgTwin), "PC")

        assertTrue(result.isEmpty())
    }

    @Test
    fun candidatesSharingATitleCollapseToOne() {
        val candidates = listOf(
            game("rawg_1", "Astro Bot", "PS5"),
            game("rawg_2", "Astro Bot", "PS5")
        )

        val result = selectOtherPlatformGames(emptyList(), candidates, "PC")

        assertEquals(listOf("rawg_1"), result.map { it.id })
    }

    @Test
    fun aSymbolOnlyPrimaryTitleIsNotAWildcard() {
        // catalogTitleKey("!!!") is "", and so is any other symbol-only title. Without guarding
        // that, one such row in the primary list would suppress every empty-keyed candidate.
        val result = selectOtherPlatformGames(
            primary = listOf(game("rawg_9", "!!!", "PC")),
            candidates = listOf(game("rawg_1", "???", "PS5")),
            selectedPlatform = "PC"
        )

        assertEquals(listOf("rawg_1"), result.map { it.id })
    }

    @Test
    fun allPlatformMeansNoSection() {
        val candidates = listOf(game("rawg_2", "Grand Theft Auto VI", "Xbox Series", "PS5"))

        assertTrue(selectOtherPlatformGames(emptyList(), candidates, "All").isEmpty())
    }
}
