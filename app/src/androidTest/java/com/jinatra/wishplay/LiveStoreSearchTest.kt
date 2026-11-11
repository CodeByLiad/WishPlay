package com.jinatra.wishplay

import com.jinatra.wishplay.data.remote.api.WishPlayApiServiceImpl
import com.jinatra.wishplay.data.remote.api.GooglePlayCatalog
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Intentionally uses real network responses, separate from deterministic regression fixtures. */
class LiveStoreSearchTest {
    @Test fun androidOnePieceAndGenshinHaveRealArtworkAndDetails() = runBlocking {
        val client = OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build()
        val api = WishPlayApiServiceImpl(client)
        for (query in listOf("one piece", "Genshin")) {
            val started = System.nanoTime()
            val page = api.searchGamesPage(query, "Android", 1)
            assertTrue("Search exceeded deadline", TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started) < 12)
            val game = page.games.first { it.title.contains(query, ignoreCase = true) && it.id.startsWith("play_") }
            assertEquals(setOf("Android"), game.platforms.keys)
            assertNull(game.platforms["Android"])
            client.newCall(Request.Builder().url(requireNotNull(game.coverUrl)).build()).execute().use {
                assertTrue("Artwork HTTP ${it.code}", it.isSuccessful)
                assertTrue(it.header("Content-Type").orEmpty().startsWith("image/"))
                assertTrue(it.body!!.bytes().size > 100)
            }
            val details = requireNotNull(api.getGameDetails(game.id))
            assertEquals(game.id, details.id)
            assertTrue(details.title.contains(query, true))
            assertTrue(details.releaseEvidence["Android"]!!.single().url!!.startsWith("https://play.google.com/"))
        }
        assertTrue(api.searchGamesPage("", "Android", 1).games.isNotEmpty())
    }

    @Test fun pcAndIosSearchHaveMatchingRealStoreResults() = runBlocking {
        val api = WishPlayApiServiceImpl(OkHttpClient())
        assertTrue(api.searchGamesPage("Elden Ring", "PC", 1).games.any { it.title.contains("ELDEN RING", true) })
        assertTrue(api.searchGamesPage("one piece", "iOS", 1).games.any { it.title.contains("ONE PIECE", true) })
        assertTrue(api.searchGamesPage("", "PC", 1).games.isNotEmpty())
    }

    @Test fun unreadablePlayResponseIsAnErrorRatherThanEmptyResults() {
        assertTrue(runCatching { GooglePlayCatalog(OkHttpClient()).parseSearch("<html>Unavailable</html>") }.isFailure)
    }

    @Test fun playParserExcludesWallpapersAndPreservesUnknownDates() {
        val records = """[[["com.example.game"],[null,null,null,[null,null,"https://play-lh.googleusercontent.com/game"]],null,"Example game",null,"Action"],[["com.example.wallpaper"],[null,null,null,[null,null,"https://play-lh.googleusercontent.com/wallpaper"]],null,"Wallpaper",null,"Personalization"]]"""
        val html = "AF_initDataCallback({key: 'ds:4', data:$records, sideChannel: {}});"
        val games = GooglePlayCatalog(OkHttpClient()).parseSearch(html)
        assertEquals("Example game", games.single().title)
        assertNull(games.single().platforms["Android"])
    }
}
