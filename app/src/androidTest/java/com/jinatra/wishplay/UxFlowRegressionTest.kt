package com.jinatra.wishplay

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.jinatra.wishplay.alerts.BootReceiverEntryPoint
import com.jinatra.wishplay.data.local.prefs.UserPreferences
import com.jinatra.wishplay.data.repository.WishlistRepository
import com.jinatra.wishplay.ui.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class UxFlowRegressionTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun calendarCustomGameAndRemovalHaveRecoverableFlows() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, BootReceiverEntryPoint::class.java)
        val repository = WishlistRepository(entry.wishlistDao(), entry.alertScheduler(), entry.userPreferences())
        val title = "Project Aurora ${System.currentTimeMillis().toString().takeLast(4)}"
        runBlocking {
            UserPreferences(context).setOnboardingDone(true)
            UserPreferences(context).setNotificationsAsked(true)
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.waitUntil(20000) { compose.onAllNodesWithText("My wishlist").fetchSemanticsNodes().isNotEmpty() }
                screenshot("wishlist-empty")
                compose.onNodeWithText("Search").performClick()
                compose.onNodeWithText("Find games").assertIsDisplayed()
                screenshot("search")
                compose.onNodeWithText("Profile").performClick()
                compose.onNodeWithText("Wishlist backups").assertIsDisplayed()
                compose.onNodeWithText("Admin panel").assertDoesNotExist()
                compose.onNodeWithText("WishPlay Pro").assertDoesNotExist()
                compose.onNodeWithText("01978900129").performScrollTo().assertIsDisplayed()
                screenshot("profile")
                compose.onNodeWithContentDescription("Violet accent").performScrollTo().performClick()
                compose.waitUntil(5000) {
                    compose.onAllNodes(hasContentDescription("Violet accent") and isSelected()).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithContentDescription("Gold accent").performClick()
                compose.waitUntil(5000) {
                    compose.onAllNodes(hasContentDescription("Gold accent") and isSelected()).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithText("Alerts").performClick()
                screenshot("alerts")
                compose.onNodeWithContentDescription("Open the release calendar").performClick()
                val month = YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
                compose.onNodeWithText(month).assertIsDisplayed()
                screenshot("calendar")
                compose.onNodeWithContentDescription("Next month").performClick()
                compose.onNodeWithText("This month").performClick()
                compose.onNodeWithText(month).assertIsDisplayed()
                compose.onNodeWithContentDescription("Back").performClick()
                compose.onNodeWithText("Wishlist").performClick()
                if (compose.onAllNodesWithContentDescription("Add game").fetchSemanticsNodes().isNotEmpty()) {
                    compose.onNodeWithContentDescription("Add game").performClick()
                }
                compose.onNodeWithText("Add a custom game").performClick()
                compose.onNodeWithText("e.g. Mango Rush").performTextInput(title)
                compose.onNodeWithContentDescription("Back").performClick()
                compose.onNodeWithText("Discard this game?").assertIsDisplayed()
                compose.onNodeWithText("Keep editing").performClick()
                screenshot("custom-form")
                compose.onNodeWithContentDescription("Choose Release date").performScrollTo().performClick()
                compose.onNodeWithText("Use date").assertIsDisplayed()
                compose.onNodeWithText("Cancel").performClick()
                // Saving stays available without scrolling to the end of the form.
                compose.onNodeWithText("Save to wishlist").assertIsDisplayed().performClick()
                compose.waitUntil(15000) { compose.onAllNodesWithText("You added this yourself").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(title).assertIsDisplayed()
                screenshot("custom-saved-details")
                compose.onNodeWithContentDescription("Back").performClick()
                screenshot("wishlist-populated")
                compose.onAllNodesWithText(title).onFirst().performClick()
                compose.onNodeWithText("Remove from list").performClick()
                compose.onNodeWithText("Remove from wishlist?").assertIsDisplayed()
                compose.onNodeWithText("Keep game").performClick()
                compose.onNodeWithText("Remove from list").assertIsDisplayed()
                compose.onNodeWithText("Remove from list").performClick()
                compose.onNodeWithText("Remove", substring = false).performClick()
                compose.waitUntil(5000) { compose.onAllNodesWithText("Add to wishlist").fetchSemanticsNodes().isNotEmpty() }
            }
        } finally {
            runBlocking { repository.wishlistGames.first().filter { it.title == title }.forEach {
                repository.removeGameFromWishlist(it.id)
            } }
        }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(1000)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "qa/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
