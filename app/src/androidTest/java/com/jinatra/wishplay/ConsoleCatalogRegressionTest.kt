package com.jinatra.wishplay

import com.jinatra.wishplay.data.remote.api.MicrosoftCatalog
import com.jinatra.wishplay.data.remote.api.NintendoCatalog
import com.jinatra.wishplay.data.remote.api.PlayStationCatalog
import com.jinatra.wishplay.data.remote.api.EpicCatalog
import com.jinatra.wishplay.data.remote.api.WishPlayApiServiceImpl
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ConsoleCatalogRegressionTest {
    @Test fun microsoftFullSearchReadsBothResponseFormsAndChecksContinuationOrigin() {
        val catalog = MicrosoftCatalog(OkHttpClient())
        assertEquals(0, catalog.parseSearchPayload("""{"Payload":{"SearchResults":[]}}""").getJSONArray("SearchResults").length())
        assertEquals(0, catalog.parseSearchPayload("""[{"Payload":{"PageObject":{}}},{"Payload":{"SearchResults":[]}}]""").getJSONArray("SearchResults").length())
        assertTrue(catalog.trustedContinuation("/v9.0/search?cursor=abc")!!.startsWith("https://storeedgefd.dsx.mp.microsoft.com/"))
        assertNull(catalog.trustedContinuation("https://example.com/v9.0/search?cursor=abc"))
        assertNull(catalog.trustedContinuation("http://storeedgefd.dsx.mp.microsoft.com/v9.0/search"))
        assertNull(catalog.trustedContinuation("/v9.0/other"))
    }
    @Test fun epicRequiresGameCategoryAndVerifiedPlatformTags() {
        val catalog = EpicCatalog(OkHttpClient())
        val product = JSONObject("""{"id":"a7364ebfa54147f1b90f78a81c8093f7","namespace":"c4763f236d08423eb47b4c3008779c84","title":"Example","productSlug":"example","releaseDate":"2023-10-27T04:00:00Z","categories":[{"path":"games"}],"tags":[{"name":"Windows","groupName":"platform"},{"name":"Android","groupName":"platform"}]}""")
        val game = catalog.mapGame(product)!!
        assertEquals(setOf("PC", "Android"), game.platforms.keys)
        assertEquals("2023-10-27", game.platforms["PC"])
        assertNull(game.platforms["Android"])
        assertNull(catalog.mapGame(JSONObject(product.toString()).put("categories", org.json.JSONArray("[{\"path\":\"addons\"}]"))))
        assertNull(catalog.mapGame(JSONObject(product.toString()).put("tags", org.json.JSONArray())))
    }
    @Test fun playStationExcludesAddOnsAndPreservesActualPlatformsAndDates() {
        val catalog = PlayStationCatalog(OkHttpClient())
        val product = JSONObject("""{"id":"UP9000-PPSA21564_00-0000000000000000","name":"Example","platforms":["PS5"],"storeDisplayClassification":"FULL_GAME","media":[{"type":"VIDEO","url":"https://example.com/video.mp4"},{"type":"IMAGE","role":"MASTER","url":"https://example.com/cover.png"}]}""")
        val mapped = catalog.mapGame(product)!!
        assertEquals(setOf("PS5"), mapped.platforms.keys)
        assertNull(mapped.platforms["PS5"])
        assertEquals("https://example.com/cover.png", mapped.coverUrl)
        assertNull(catalog.mapGame(JSONObject(product.toString()).put("storeDisplayClassification", "COSTUME")))
        val metadata = JSONObject(product.toString()).put("releaseDate", "2024-09-06T04:00:00Z")
        val html = """<script id="env:example" type="application/json">{"cache":{"Product:UP9000-PPSA21564_00-0000000000000000":$metadata}}</script>"""
        assertEquals("2024-09-06", catalog.parseDetails(html, product.getString("id"))!!.platforms["PS5"])
    }
    private val nintendoRecord = """{"fs_id":"123","type":"GAME","title":"A real Switch 2 edition","url":"/en-gb/Games/Nintendo-Switch-2-games/Example-123.html","date_from":"2025-06-05T00:00:00Z","system_names_txt":["Nintendo Switch 2"],"image_url":"https://www.nintendo.com/cover.jpg"}"""

    @Test fun switch2DoesNotLeakIntoSwitchOrOtherPlatforms() {
        val catalog = NintendoCatalog(OkHttpClient())
        val page = JSONObject("""{"response":{"numFound":81,"start":0,"docs":[$nintendoRecord]}}""")
        assertEquals(setOf("Switch 2"), catalog.parsePage(page, "Switch 2").games.single().platforms.keys)
        assertTrue(catalog.parsePage(page, "Switch").games.isEmpty())
        assertTrue(catalog.parsePage(page, "Switch 2").hasMore)
        assertEquals("https://www.nintendo.com/new-cover.jpg", catalog.mapGame(JSONObject(nintendoRecord)
            .put("image_url", JSONObject.NULL).put("image_url_sq_s", "https://www.nintendo.com/new-cover.jpg"))!!.coverUrl)
        assertNull(catalog.mapGame(JSONObject(nintendoRecord).put("system_names_txt", org.json.JSONArray("[\"Wii U\"]"))))
    }

    @Test fun switch2UsesIndependentSourceAndSecondPageWithRawgUnavailable() = runBlocking {
        val paths = mutableListOf<HttpUrl>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val url = chain.request().url
            paths.add(url)
            val start = url.queryParameter("start")?.toIntOrNull() ?: 0
            val record = JSONObject(nintendoRecord).put("fs_id", (123 + start).toString())
            val body = """{"response":{"numFound":41,"start":$start,"docs":[$record]}}"""
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("Fixture")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        val api = WishPlayApiServiceImpl(client)
        assertTrue(api.searchGamesPage("Example + deluxe", "Switch 2", 1).hasMore)
        val last = api.searchGamesPage("Example + deluxe", "Switch 2", 2)
        assertEquals("nintendo_163", last.games.single().id)
        assertFalse(last.hasMore)
        assertEquals(listOf("0", "40"), paths.map { it.queryParameter("start") })
        assertTrue(paths.all { it.host == "search.nintendo-europe.com" })
        assertEquals("Example \\+ deluxe", paths.first().queryParameter("q"))
    }

    @Test fun microsoftChecksAllowedPlatformsRatherThanAssumingEveryGameIsXbox() {
        val catalog = MicrosoftCatalog(OkHttpClient())
        fun product(platform: String, gen9: Boolean = false, type: String = "Game") = JSONObject("""{
            "ProductId":"9NP1P1WFS0LB","ProductType":"$type","Properties":{"XboxConsoleGenCompatible":[${if (gen9) "\"ConsoleGen9\"" else "\"ConsoleGen8\""}]},
            "LocalizedProperties":[{"ProductTitle":"Example","Images":[{"ImagePurpose":"Poster","Uri":"//store-images.s-microsoft.com/cover"}]}],
            "DisplaySkuAvailabilities":[{"Availabilities":[{"Conditions":{"ClientConditions":{"AllowedPlatforms":[{"PlatformName":"$platform"}]}},"Properties":{"OriginalReleaseDate":"2020-12-03T00:00:00Z"}}]}]
        }""")
        val pc = catalog.mapGame(product("Windows.Desktop"))!!
        assertEquals(mapOf("PC" to "2020-12-03"), pc.platforms)
        assertEquals("https://store-images.s-microsoft.com/cover", pc.coverUrl)
        assertNull(catalog.mapGame(product("Windows.Xbox")))
        assertEquals(setOf("Xbox Series"), catalog.mapGame(product("Windows.Xbox", gen9 = true))!!.platforms.keys)
        assertNull(catalog.mapGame(product("Windows.Desktop", type = "Durable")))
        assertNull(catalog.mapGame(product("Unknown.Device")))
    }
}
