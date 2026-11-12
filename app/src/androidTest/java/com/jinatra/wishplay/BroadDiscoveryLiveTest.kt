package com.jinatra.wishplay

import android.util.Log
import com.jinatra.wishplay.data.remote.api.WishPlayApiServiceImpl
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test

/** Real providers and multiple unrelated titles; never fulfilled by bundled demo records. */
class BroadDiscoveryLiveTest {
    @Test fun nintendoAndMicrosoftGamesCanBeFoundAndOpenedWithoutRawg() = runBlocking<Unit> {
        val api = WishPlayApiServiceImpl(OkHttpClient())
        val cases = listOf(
            Triple("zelda", "Switch", "Breath of the Wild"),
            Triple("Mario Kart", "Switch", "Mario Kart 8"),
            Triple("Animal Crossing", "Switch", "New Horizons"),
            Triple("Mario Kart World", "Switch 2", "Mario Kart World"),
            Triple("Donkey Kong Bananza", "Switch 2", "Donkey Kong Bananza"),
            Triple("Halo Infinite", "Xbox Series", "Halo Infinite"),
            Triple("Forza Horizon 5", "Xbox Series", "Forza Horizon 5"),
            Triple("Minecraft", "PC", "Minecraft: Java & Bedrock Edition for PC"),
            Triple("Sea of Thieves", "PC", "Sea of Thieves"),
            Triple("Astro Bot", "PS5", "ASTRO BOT"),
            Triple("Bloodborne", "PS4", "Bloodborne"),
            Triple("Stellar Blade", "PS5", "Stellar Blade"),
            Triple("Fortnite", "PC", "Fortnite"),
            Triple("Alan Wake 2", "PC", "Alan Wake 2"),
            Triple("Valorant", "PC", "VALORANT"),
            Triple("Genshin Impact", "PC", "Genshin Impact")
        )
        val failures = mutableListOf<String>()
        for ((query, platform, expected) in cases) {
            try {
                val start = System.nanoTime()
                var firstMillis: Long? = null
                var matchMillis: Long? = null
                var games = emptyList<com.jinatra.wishplay.data.remote.dto.GameDto>()
                val prefix = when { query in listOf("Fortnite", "Alan Wake 2", "Valorant", "Genshin Impact") -> "epic_"; platform.startsWith("Switch") -> "nintendo_"; platform.startsWith("PS") -> "ps_"; else -> "microsoft_" }
                api.searchGamesUpdates(query, platform, 1).collect { page ->
                    if (page.games.isNotEmpty() && firstMillis == null) firstMillis = (System.nanoTime() - start) / 1_000_000
                    if (matchMillis == null && page.games.any { it.id.startsWith(prefix) && it.title.contains(expected, true) && platform in it.platforms })
                        matchMillis = (System.nanoTime() - start) / 1_000_000
                    games = page.games
                }
                val match = games.firstOrNull { it.id.startsWith(prefix) && it.title.contains(expected, true) && platform in it.platforms }
                assertNotNull("$query / $platform missing $expected; got ${games.map { it.title }}", match)
                // A new client mirrors reopening a saved entry after process death.
                val details = WishPlayApiServiceImpl(OkHttpClient()).getGameDetails(match!!.id, match.title)
                assertNotNull("$query details unavailable", details)
                assertEquals(match.id, details!!.id)
                assertTrue("$query platform lost on details", platform in details.platforms)
                assertTrue(details.releaseEvidence[platform].orEmpty().any { it.url?.startsWith("https://") == true })
                Log.i("WishPlayCoverage", "$platform | $query | ${match.title} | ${match.id} | first=${firstMillis}ms | match=${matchMillis}ms | count=${games.size}")
            } catch (e: Exception) { failures.add("$platform / $query: ${e.message}") }
            catch (e: AssertionError) { failures.add("$platform / $query: ${e.message}") }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test fun unrelatedAndroidGamesAppearFromRealPlayListings() = runBlocking<Unit> {
        val api = WishPlayApiServiceImpl(OkHttpClient())
        val failures = mutableListOf<String>()
        val queries = listOf("Offroad League Online", "PUBG MOBILE", "Free Fire", "Mobile Legends", "Call of Duty", "Roblox",
            "Clash of Clans", "Candy Crush", "Minecraft", "Balatro", "Vampire Survivors", "Stardew Valley", "Monument Valley", "Genshin Impact")
        for (query in queries) {
            try {
                val games = api.searchGamesPage(query, "Android", 1).games
                val expected = com.jinatra.wishplay.domain.model.catalogTitleKey(query)
                val game = games.firstOrNull { it.id.startsWith("play_") && com.jinatra.wishplay.domain.model.catalogTitleKey(it.title).contains(expected) }
                assertNotNull("$query missing; got ${games.map { it.title }}", game)
                Log.i("WishPlayCoverage", "Android | $query | ${game!!.id} | count=${games.size}")
            } catch (e: Exception) { failures.add("$query: ${e.message}") }
            catch (e: AssertionError) { failures.add("$query: ${e.message}") }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
