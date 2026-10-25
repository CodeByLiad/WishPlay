package com.jinatra.wishplay

import com.jinatra.wishplay.domain.model.*
import com.jinatra.wishplay.data.repository.CloudWishlistItem
import org.junit.Assert.*
import org.junit.Test

class ApprovedFeaturesTest {
    private val game = Game("rawg_123", "Ports", platforms = mapOf("PC" to "2027-01-01", "PS5" to "2027-03-01", "Android" to null),
        trackedPlatforms = setOf("PS5"), customNotes = "Keep my notes", alertEnabled = false,
        releaseEvidence = mapOf("PC" to listOf(ReleaseEvidence("Steam (US)", "2027-01-01", "https://store.steampowered.com/app/123/"))))

    @Test fun `backups preserve dates preferences notes and source evidence`() {
        val backup = WishlistBackupCodec.decode(WishlistBackupCodec.encode(listOf(game), "2026-10-18T00:00:00Z"))
        assertEquals(game, backup.games.single())
        assertEquals("2026-10-18T00:00:00Z", backup.exportedAt)
    }

    @Test fun `invalid backup fails before importing`() {
        val invalid = listOf("{}", "not json", WishlistBackupCodec.encode(listOf(game, game)),
            WishlistBackupCodec.encode(listOf(game.copy(platforms = mapOf("PC" to "2027-02-30")))),
            WishlistBackupCodec.encode(listOf(game)).replace("\"version\": 1", "\"version\": 99"))
        invalid.forEach { assertTrue(runCatching { WishlistBackupCodec.decode(it) }.isFailure) }
    }

    @Test fun `tracked port controls next release and reminder groups`() {
        assertEquals("2027-03-01", game.reminderPlatforms.nextReleaseDate("2026-10-18"))
        assertEquals(setOf("PS5"), game.reminderPlatforms.releaseGroups().values.flatten().toSet())
        assertEquals(ReleaseStatus.TBA, game.copy(trackedPlatforms = setOf("Android")).status("2026-10-18"))
        assertEquals(ReleaseStatus.OUT_NOW, game.copy(trackedPlatforms = setOf("PC")).status("2027-02-01"))
    }

    @Test fun `removed platform stays unknown instead of enabling other ports`() {
        val removed = game.copy(trackedPlatforms = setOf("iOS"))
        assertEquals(mapOf("iOS" to null), removed.reminderPlatforms)
        assertTrue(removed.reminderPlatforms.releaseGroups().isEmpty())
        assertEquals(removed, WishlistBackupCodec.decode(WishlistBackupCodec.encode(listOf(removed))).games.single())
    }

    @Test fun `cloud format preserves platform choices and accepts older entries`() {
        val item = CloudWishlistItem(gameId = game.id, trackedPlatformsJson = "[\"PS5\"]")
        assertEquals(item.trackedPlatformsJson, CloudWishlistItem.fromFirestoreMap(item.toFirestoreMap(), item.gameId).trackedPlatformsJson)
        assertNull(CloudWishlistItem.fromFirestoreMap(emptyMap(), "legacy").trackedPlatformsJson)
    }

    @Test fun `store dates accept full dates without inventing partial dates`() {
        assertEquals("2016-02-26", storeReleaseDate("26 Feb, 2016"))
        assertEquals("2016-02-26", storeReleaseDate("Feb 26, 2016"))
        listOf("2027", "Q2 2027", "Coming soon", "Feb 30, 2027").forEach { assertNull(storeReleaseDate(it)) }
        assertNotEquals(catalogTitleKey("Game 2"), catalogTitleKey("Game II"))
        assertNotEquals(catalogTitleKey("Game"), catalogTitleKey("Game Deluxe Edition"))
    }
}
