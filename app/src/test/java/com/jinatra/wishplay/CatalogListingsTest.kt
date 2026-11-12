package com.jinatra.wishplay

import com.jinatra.wishplay.data.remote.api.mergeVerifiedListings
import com.jinatra.wishplay.data.remote.dto.GameDto
import com.jinatra.wishplay.domain.model.ReleaseEvidence
import org.junit.Assert.*
import org.junit.Test

class CatalogListingsTest {
    private fun game(id: String, url: String?, date: String? = null, title: String = "Shared title") =
        GameDto(id = id, title = title, platforms = mapOf("PC" to date), releaseEvidence =
            if (url == null) emptyMap() else mapOf("PC" to listOf(ReleaseEvidence("Store", date, url))))

    @Test fun sameTitleAtDifferentListingsDoesNotHideAnotherGame() {
        val games = listOf(game("one", "https://store.example/app/1993", "1993-12-10"),
            game("two", "https://store.example/app/2016", "2016-05-13"))
        assertEquals(2, mergeVerifiedListings(games).size)
    }

    @Test fun confirmedDuplicateListingCombinesEvidenceAndKeepsFirstIdentity() {
        val games = listOf(game("first", "https://STORE.example/app/123/"),
            game("second", "https://store.example/app/123", "2023-10-27").copy(about = "Store description"))
        val merged = mergeVerifiedListings(games).single()
        assertEquals("first", merged.id)
        assertEquals("2023-10-27", merged.platforms["PC"])
        assertEquals("Store description", merged.about)
        assertEquals(2, merged.releaseEvidence["PC"]!!.size)
    }

    @Test fun queryParametersThatIdentifyDifferentAppsArePreserved() {
        assertEquals(2, mergeVerifiedListings(listOf(
            game("one", "https://play.google.com/store/apps/details?id=com.example.one"),
            game("two", "https://play.google.com/store/apps/details?id=com.example.two")
        )).size)
    }

    @Test fun missingEvidenceDoesNotMergeNamesOrDifferentPlatformPorts() {
        val first = game("one", null)
        assertEquals(2, mergeVerifiedListings(listOf(first, first.copy(id = "two"))).size)
        val linked = game("one", "https://store.example/game")
        assertEquals(2, mergeVerifiedListings(listOf(linked, linked.copy(id = "two", platforms = mapOf("Android" to null)))).size)
    }

    @Test fun repeatedProviderIdFillsSummaryWithoutLosingKnownDate() {
        val first = game("steam_123", "https://store.example/app/123", "2023-10-27")
        val second = first.copy(platforms = mapOf("PC" to null), coverUrl = "https://store.example/cover.jpg")
        val merged = mergeVerifiedListings(listOf(first, second)).single()
        assertEquals("2023-10-27", merged.platforms["PC"])
        assertNotNull(merged.coverUrl)
    }
}
