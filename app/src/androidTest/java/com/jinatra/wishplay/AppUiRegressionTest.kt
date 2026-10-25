package com.jinatra.wishplay

import android.graphics.Bitmap
import android.view.WindowInsets
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.jinatra.wishplay.data.local.prefs.UserPreferences
import com.jinatra.wishplay.ui.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import android.content.Intent
import com.jinatra.wishplay.domain.model.Game
import com.jinatra.wishplay.data.repository.WishlistRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import com.jinatra.wishplay.alerts.BootReceiverEntryPoint
import com.jinatra.wishplay.alerts.DailyCheckWorkerEntryPoint

class AppUiRegressionTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun mainTabsAndCustomDatesStayInsideSystemBars() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        runBlocking {
            UserPreferences(context).setOnboardingDone(true)
            UserPreferences(context).setNotificationsAsked(true)
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntil(20000) { compose.onAllNodesWithText("My wishlist").fetchSemanticsNodes().isNotEmpty() }
            screenshot("wishlist")
            compose.onNodeWithText("Search", useUnmergedTree = false).performClick()
            compose.onNodeWithText("Find games").assertIsDisplayed()
            var statusTop = 0
            var navigationBottom = 0
            var windowHeight = 0
            scenario.onActivity { activity ->
                val insets = activity.window.decorView.rootWindowInsets.getInsets(WindowInsets.Type.systemBars())
                statusTop = insets.top
                navigationBottom = insets.bottom
                windowHeight = activity.window.decorView.height
            }
            assertTrue(compose.onNodeWithText("Find games").fetchSemanticsNode().boundsInWindow.top >= statusTop)
            assertTrue(compose.onNodeWithText("Profile").fetchSemanticsNode().boundsInWindow.bottom <= windowHeight - navigationBottom)
            screenshot("search")
            compose.onNodeWithText("Android", useUnmergedTree = false).performClick()
            compose.onNode(hasSetTextAction()).performClick().performTextInput("Genshin")
            compose.onNodeWithText("Genshin", useUnmergedTree = true).assertExists()
            compose.waitUntil(40000) {
                compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Artwork loaded"),
                    useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            screenshot("search-keyboard")
            // Native Back closes the keyboard without leaving the tab.
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            compose.onNodeWithText("Alerts", useUnmergedTree = false).performClick()
            compose.onNodeWithText("Nothing scheduled").assertExists()
            screenshot("alerts")
            compose.onNodeWithText("Profile", useUnmergedTree = false).performClick()
            compose.onNodeWithText("Wishlist backups").assertIsDisplayed()
            compose.onNodeWithText("Export").assertIsDisplayed()
            compose.onNodeWithText("Import").assertIsDisplayed()
            compose.onNodeWithText("Keep your list safe").assertExists()
            screenshot("profile")
            compose.onNodeWithText("Wishlist", useUnmergedTree = false).performClick()
            compose.onNodeWithText("Add a custom game").performClick()
            compose.onNodeWithText("Different release dates per platform").assertExists()
            screenshot("custom")
        }
    }

    @Test fun perGamePlatformChoicesPersist() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, BootReceiverEntryPoint::class.java)
        val repository = WishlistRepository(entryPoint.wishlistDao(), entryPoint.alertScheduler(), entryPoint.userPreferences())
        val alertDao = EntryPointAccessors.fromApplication(context.applicationContext, DailyCheckWorkerEntryPoint::class.java).alertDao()
        val game = Game("c_ui_preferences", "Platform choices", isCustom = true,
            platforms = mapOf("PC" to "2027-03-01", "PS5" to "2027-04-01", "Android" to "2027-05-01"))
        runBlocking {
            UserPreferences(context).setOnboardingDone(true)
            repository.saveGame(game)
            repository.addGameToWishlist(game.id).getOrThrow()
        }
        val intent = Intent(context, MainActivity::class.java).putExtra("extra_game_id", game.id)
        try {
            ActivityScenario.launch<MainActivity>(intent).use {
                compose.waitUntil(20000) { compose.onAllNodesWithText("Platforms to track").fetchSemanticsNodes().isNotEmpty() }
                compose.onAllNodesWithText("PC").onLast().performScrollTo().performClick()
                compose.waitUntil(5000) { runBlocking { repository.observeGameById(game.id).first()?.trackedPlatforms == setOf("PC") &&
                    alertDao.getPendingAlertsForGame(game.id).size == 2 } }
                compose.onAllNodesWithText("PS5").onLast().performClick()
                compose.waitUntil(5000) { runBlocking { repository.observeGameById(game.id).first()?.trackedPlatforms == setOf("PC", "PS5") &&
                    alertDao.getPendingAlertsForGame(game.id).size == 4 } }
                screenshot("platform-preferences")
            }
            ActivityScenario.launch<MainActivity>(intent).use {
                compose.waitUntil(20000) { compose.onAllNodesWithText("Platforms to track").fetchSemanticsNodes().isNotEmpty() }
                compose.onAllNodesWithText("PS5").onLast().performScrollTo().assertIsSelected()
            }
        } finally { runBlocking { repository.removeGameFromWishlist(game.id) } }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assertTrue("Emulator has a system ANR dialog", instrumentation.uiAutomation.rootInActiveWindow
            ?.findAccessibilityNodeInfosByText("isn't responding").isNullOrEmpty())
        android.os.SystemClock.sleep(300)
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "qa/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
