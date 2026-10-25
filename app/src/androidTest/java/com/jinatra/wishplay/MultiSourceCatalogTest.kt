package com.jinatra.wishplay

import com.jinatra.wishplay.data.remote.api.WishPlayApiServiceImpl
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class MultiSourceCatalogTest {
    @Test fun primaryFailureStillReturnsRealStoreResults() = runBlocking {
        val page = WishPlayApiServiceImpl(fixtureClient(503, 200)).searchGamesPage("Ports", "PC", 1)
        assertEquals("steam_123", page.games.single().id)
        assertTrue(page.warnings.any { it.contains("RAWG") })
    }

    @Test fun secondaryFailureKeepsPrimaryResultsAndReportsDegradation() = runBlocking {
        val page = WishPlayApiServiceImpl(fixtureClient(200, 503)).searchGamesPage("Ports", "PC", 1)
        assertEquals("rawg_1", page.games.single().id)
        assertTrue(page.warnings.any { it.contains("Steam") })
        assertTrue(runCatching { WishPlayApiServiceImpl(fixtureClient(503, 503)).searchGamesPage("Ports", "PC", 1) }.isFailure)
    }

    @Test fun linkedStoreDatesDoNotOverwriteOtherPortsAndKeepConflictingEvidence() = runBlocking {
        val game = WishPlayApiServiceImpl(fixtureClient(200, 200)).getGameDetails("rawg_1")!!
        assertEquals("2021-02-26", game.platforms["PC"])
        assertEquals("2022-01-01", game.platforms["PS5"])
        assertEquals(setOf("2020-01-01", "2021-02-26"), game.releaseEvidence["PC"]!!.map { it.date }.toSet())
    }

    private fun fixtureClient(rawgCode: Int, steamCode: Int) = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request()
        val rawgGame = """{"id":1,"name":"Ports","slug":"ports","platforms":[{"platform":{"slug":"pc","name":"PC"},"released_at":"2020-01-01"},{"platform":{"slug":"playstation5","name":"PlayStation 5"},"released_at":"2022-01-01"}]}"""
        val (code, body) = when {
            request.url.host == "api.rawg.io" && request.url.encodedPath.endsWith("/stores") -> rawgCode to """{"results":[{"url":"https://store.steampowered.com/app/123/"}]}"""
            request.url.host == "api.rawg.io" && request.url.encodedPath.endsWith("/1") -> rawgCode to rawgGame
            request.url.host == "api.rawg.io" -> rawgCode to """{"results":[$rawgGame],"next":null}"""
            request.url.encodedPath.contains("storesearch") -> steamCode to """{"items":[{"type":"app","id":123,"name":"Ports","tiny_image":"https://example.com/cover.jpg"}]}"""
            request.url.encodedPath.contains("appdetails") -> steamCode to """{"123":{"success":true,"data":{"type":"game","name":"Ports","release_date":{"date":"26 Feb, 2021"},"header_image":"https://example.com/cover.jpg"}}}"""
            else -> 200 to """{"results":[]}"""
        }
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("Fixture")
            .body(body.toResponseBody("application/json".toMediaType())).build()
    }.build()

    @Test fun platformFilterExclusionsAreOfferedSeparately() = runBlocking {
        val page = WishPlayApiServiceImpl(coverageClient()).searchGamesPage("Ports", "PC", 1)
        assertEquals(listOf("rawg_1"), page.games.map { it.id })
        assertEquals(listOf("rawg_2"), page.otherPlatformGames.map { it.id })
        assertEquals("Console Only", page.otherPlatformGames.single().title)
    }

    @Test fun unfilteredPassFailureLeavesPrimaryResultsIntact() = runBlocking {
        val page = WishPlayApiServiceImpl(coverageClient(unfilteredCode = 503)).searchGamesPage("Ports", "PC", 1)
        assertEquals(listOf("rawg_1"), page.games.map { it.id })
        assertTrue(page.otherPlatformGames.isEmpty())
    }

    private fun coverageClient(unfilteredCode: Int = 200) = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request()
        val pcGame = """{"id":1,"name":"Ports","slug":"ports","platforms":[{"platform":{"slug":"pc","name":"PC"}}]}"""
        val consoleGame = """{"id":2,"name":"Console Only","slug":"console-only","platforms":[{"platform":{"slug":"playstation5","name":"PlayStation 5"}}]}"""
        val filtered = request.url.queryParameter("platforms") != null
        val (code, body) = when {
            request.url.host == "api.rawg.io" && filtered -> 200 to """{"results":[$pcGame],"next":null}"""
            request.url.host == "api.rawg.io" -> unfilteredCode to """{"results":[$pcGame,$consoleGame],"next":null}"""
            else -> 200 to """{"items":[]}"""
        }
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("Fixture")
            .body(body.toResponseBody("application/json".toMediaType())).build()
    }.build()
}
