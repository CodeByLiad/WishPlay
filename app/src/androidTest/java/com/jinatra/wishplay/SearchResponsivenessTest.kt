package com.jinatra.wishplay

import android.os.SystemClock
import com.jinatra.wishplay.data.remote.api.WishPlayApiServiceImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class SearchResponsivenessTest {
    private val store = """{"items":[{"type":"app","id":123,"name":"Cached Game","tiny_image":"https://cdn.example.com/game.jpg"}]}"""

    @Test fun explicitRetryFetchesAgainInsteadOfReusingTheDegradedSnapshot() = runBlocking<Unit> {
        val requests = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests.incrementAndGet()
            if (chain.request().url.encodedPath.contains("storesearch")) response(chain.request(), 200, store)
            else response(chain.request(), 503, "{}")
        }.build()
        val api = WishPlayApiServiceImpl(client)
        api.searchGamesPage("Cached Game", "PC", 1)
        val before = requests.get()
        api.searchGamesPage("Cached Game", "PC", 1)
        assertEquals(before, requests.get())
        api.invalidateSearchPage("Cached Game", "PC", 1)
        assertEquals("steam_123", api.searchGamesPage("Cached Game", "PC", 1).games.single().id)
        assertTrue("Explicit retry must contact providers again", requests.get() > before)
    }

    @Test fun workingStoreAppearsWhileDatabaseIsStillBlockedAndCancellationStopsTheCall() = runBlocking<Unit> {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val blockedCall = AtomicReference<Call>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            if (chain.request().url.host == "api.rawg.io") {
                blockedCall.set(chain.call()); entered.countDown()
                release.await(10, TimeUnit.SECONDS)
                response(chain.request(), 503, "{}")
            } else {
                assertTrue(entered.await(3, TimeUnit.SECONDS))
                response(chain.request(), 200, store)
            }
        }.build()
        try {
            val first = withTimeout(5000) { WishPlayApiServiceImpl(client).searchGamesUpdates("Cached Game", "PC", 1).first() }
            assertEquals("steam_123", first.games.single().id)
            assertFalse(first.isComplete)
            assertEquals(1L, release.count)
            assertTrue("Cancelled searches must cancel their HTTP requests", blockedCall.get().isCanceled())
        } finally { release.countDown() }
    }

    @Test fun repeatSearchIsServedFromCacheWithoutAnotherNetworkRequest() = runBlocking<Unit> {
        val requests = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests.incrementAndGet()
            if (chain.request().url.host == "api.rawg.io") response(chain.request(), 503, "{}")
            else response(chain.request(), 200, store)
        }.build()
        val api = WishPlayApiServiceImpl(client)
        val pages = api.searchGamesUpdates("Cached-Game", "PC", 1).toList()
        assertTrue(pages.last().isComplete)
        val before = requests.get()
        val started = SystemClock.elapsedRealtime()
        val cached = api.searchGamesUpdates("Cached Game", "PC", 1).first()
        val elapsed = SystemClock.elapsedRealtime() - started
        assertEquals("steam_123", cached.games.single().id)
        assertTrue(cached.isComplete)
        assertEquals(before, requests.get())
        android.util.Log.i("WishPlaySearchTiming", "Repeat query cache response: ${elapsed}ms; zero additional HTTP requests")
    }

    @Test fun liveOffroadFirstResultsDoNotWaitForRawgAndRepeatIsCached() = runBlocking<Unit> {
        val api = WishPlayApiServiceImpl(OkHttpClient())
        val started = SystemClock.elapsedRealtime()
        var firstAt = 0L
        api.searchGamesUpdates("off-road league online", "Android", 1).collect { page ->
            if (page.games.any { it.title == "Offroad League Online" } && firstAt == 0L) {
                firstAt = SystemClock.elapsedRealtime() - started
                assertEquals("Offroad League Online", page.games.first().title)
            }
        }
        assertTrue(firstAt > 0)
        val cachedStart = SystemClock.elapsedRealtime()
        val cached = api.searchGamesUpdates("offroad league online", "Android", 1).first()
        assertEquals("Offroad League Online", cached.games.first().title)
        android.util.Log.i("WishPlaySearchTiming", "Live Offroad first results: ${firstAt}ms; repeat cache: ${SystemClock.elapsedRealtime() - cachedStart}ms")
    }

    private fun response(request: Request, code: Int, body: String) = Response.Builder().request(request)
        .protocol(Protocol.HTTP_1_1).code(code).message("Catalog test")
        .body(body.toResponseBody("application/json".toMediaType())).build()
}
