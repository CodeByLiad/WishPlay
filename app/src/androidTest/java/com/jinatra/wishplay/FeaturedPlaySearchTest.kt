package com.jinatra.wishplay

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.jinatra.wishplay.alerts.BootReceiverEntryPoint
import com.jinatra.wishplay.data.local.prefs.UserPreferences
import com.jinatra.wishplay.data.remote.api.GooglePlayCatalog
import com.jinatra.wishplay.data.remote.api.WishPlayApiServiceImpl
import com.jinatra.wishplay.data.repository.WishlistRepository
import com.jinatra.wishplay.ui.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Base64

class FeaturedPlaySearchTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun featuredExactMatchSurvivesFortyEarlierRecommendations() {
        val records = JSONArray()
        repeat(45) { index -> records.put(JSONArray().put(JSONArray().put("com.example.game$index"))
            .put(JSONArray().put(null).put(null).put(null).put(JSONArray().put(null).put(null).put("https://play-lh.googleusercontent.com/icon")))
            .put(null).put("Other game $index").put(null).put("Action")) }
        records.put(featured("com.example.featured", "Racing League", "GAME_RACING"))
        val games = GooglePlayCatalog(OkHttpClient()).parseSearch(payload(records), "racing-league")
        assertEquals("Racing League", games.first().title)
        assertEquals(40, games.size)
    }

    @Test fun featuredNonGamesAreNotMisclassifiedAsGames() {
        val games = GooglePlayCatalog(OkHttpClient()).parseSearch(payload(JSONArray().put(
            featured("com.example.editor", "Photo editor", "PHOTOGRAPHY"))), "Photo editor")
        assertTrue(games.isEmpty())
    }

    @Test fun liveOffroadSpellingsReturnTheRealGameFirstWithArtworkAndDetails() = runBlocking {
        val client = OkHttpClient()
        val api = WishPlayApiServiceImpl(client)
        var id: String? = null
        for (query in listOf("off-road league online", "offroad league online", "off road league online")) {
            val first = api.searchGamesPage(query, "Android", 1).games.first()
            assertEquals("Offroad League Online", first.title)
            assertEquals(id ?: first.id, first.id)
            id = first.id
            assertEquals(setOf("Android"), first.platforms.keys)
            assertTrue(first.releaseEvidence["Android"]!!.single().url.contains("com.RoosterGames.OffroadLeague"))
        }
        val details = requireNotNull(api.getGameDetails(requireNotNull(id)))
        assertEquals("Rooster Games", details.developer)
        client.newCall(Request.Builder().url(requireNotNull(details.coverUrl)).build()).execute().use {
            assertTrue(it.isSuccessful)
            assertTrue(it.header("Content-Type").orEmpty().startsWith("image/"))
            assertTrue(it.body!!.bytes().size > 100)
        }
    }

    @Test fun offroadCanBeFoundOpenedAndSavedInTheRealApp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, BootReceiverEntryPoint::class.java)
        val repository = WishlistRepository(entry.wishlistDao(), entry.alertScheduler(), entry.userPreferences())
        val id = "play_" + Base64.getUrlEncoder().withoutPadding().encodeToString("com.RoosterGames.OffroadLeague".toByteArray())
        val saved = runBlocking { repository.observeGameById(id).first()?.inWishlist == true }
        runBlocking {
            val prefs = UserPreferences(context)
            prefs.setOnboardingDone(true); prefs.setNotificationsAsked(true); prefs.setPreferredPlatforms(emptySet())
        }
        fun screenshot(name: String) {
            compose.waitForIdle()
            val file = File(context.getExternalFilesDir(null), "offroad/$name.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.waitUntil(15000) { compose.onAllNodesWithText("Search").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("Search").performClick()
                compose.onNodeWithText("Android").performClick()
                compose.onNode(hasSetTextAction()).performTextInput("off-road league online")
                compose.waitUntil(15000) { compose.onAllNodesWithText("Offroad League Online").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("Offroad League Online").assertIsDisplayed()
                screenshot("search")
                compose.onNodeWithText("Offroad League Online").performClick()
                compose.waitUntil(10000) { compose.onAllNodesWithText("Rooster Games").fetchSemanticsNodes().isNotEmpty() }
                screenshot("details")
                if (!saved) {
                    compose.onAllNodesWithText("Add to wishlist").onFirst().performScrollTo().performClick()
                    compose.waitUntil(5000) { runBlocking { repository.observeGameById(id).first()?.inWishlist == true } }
                }
            }
        } finally { if (!saved) runBlocking { repository.removeGameFromWishlist(id) } }
    }

    private fun payload(records: JSONArray) = "AF_initDataCallback({key: 'ds:4', data:$records, sideChannel: {}});"
    private fun featured(pkg: String, name: String, category: String): JSONArray {
        val app = JSONArray().put(0, JSONArray().put(name))
            .put(79, JSONArray().put(JSONArray().put(JSONArray().put("Racing").put(null).put(category))))
            .put(95, JSONArray().put(JSONArray().put(null).put(null).put(null).put(JSONArray().put(null).put(null).put("https://play-lh.googleusercontent.com/featured"))))
        return JSONArray().put(null).put(null).put(app).put(JSONObject().put("12", JSONArray().put(JSONArray().put(pkg).put(7))))
    }
}
