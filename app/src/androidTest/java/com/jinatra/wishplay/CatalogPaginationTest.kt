package com.jinatra.wishplay

import com.jinatra.wishplay.data.remote.api.NintendoCatalog
import com.jinatra.wishplay.data.remote.api.PlayStationCatalog
import com.jinatra.wishplay.data.remote.api.StoreCatalog
import com.jinatra.wishplay.data.remote.api.MicrosoftCatalog
import com.jinatra.wishplay.data.remote.dto.GameSearchPage
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CatalogPaginationTest {
    @Test fun steamPagesDecodeTitlesSkipPackagesAndPreserveUnknownDates() {
        val html = """<a data-ds-appid="123" class="search_result_row"><img src="https://example.com/game.jpg"><span class="title">Game &amp; sequel</span><div class="search_released">Sep 23, 2024</div></a><a data-ds-packageid="77" class="search_result_row"><span class="title">Package</span></a>"""
        val json = JSONObject().put("success", 1).put("results_html", html).put("total_count", 52).put("start", 0)
        val page = StoreCatalog(OkHttpClient()).parseSteamPage(json)
        assertEquals("Game & sequel", page.games.single().title)
        assertEquals("2024-09-23", page.games.single().platforms["PC"])
        assertTrue(page.hasMore)
        assertFalse(StoreCatalog(OkHttpClient()).parseSteamPage(JSONObject(json.toString()).put("start", 50)).hasMore)
        assertNull(StoreCatalog(OkHttpClient()).parseSteamPage(JSONObject(json.toString()).put("results_html", html.replace("Sep 23, 2024", "Coming soon"))).games.single().platforms["PC"])
    }

    @Test fun liveStorePagesFindAdditionalGamesWithoutRawg() = runBlocking<Unit> {
        val client = OkHttpClient()
        val steam = StoreCatalog(client)
        val nintendo = NintendoCatalog(client)
        val ps = PlayStationCatalog(client)
        val microsoft = MicrosoftCatalog(client)
        suspend fun check(name: String, fetch: suspend (Int) -> GameSearchPage) {
            val first = fetch(1)
            assertTrue("$name first page has no games", first.games.isNotEmpty())
            assertTrue("$name must offer another page", first.hasMore)
            val second = fetch(2)
            val new = second.games.filter { next -> first.games.none { it.id == next.id } }
            assertTrue("$name page 2 repeats page 1", new.isNotEmpty())
            android.util.Log.i("WishPlayCoverage", "$name pagination | page1=${first.games.size} | page2=${second.games.size} | new=${new.size}")
        }
        check("Steam") { steam.searchSteamPage("one piece", it) }
        check("Nintendo") { nintendo.search("Mario", "Switch", it) }
        check("PlayStation") { ps.search("Astro Bot", "All", it) }
        check("Microsoft") { microsoft.searchPage("Minecraft", "PC", it) }
    }
}
