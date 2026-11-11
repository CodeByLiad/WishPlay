package com.jinatra.wishplay

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.app.NotificationManagerCompat
import com.jinatra.wishplay.data.local.prefs.UserPreferences
import com.jinatra.wishplay.ui.MainActivity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import com.jinatra.wishplay.alerts.BootReceiverEntryPoint
import com.jinatra.wishplay.data.repository.WishlistRepository
import dagger.hilt.android.EntryPointAccessors

class SearchAndPermissionUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun onePieceAndroidLoadsRealResultsAndArtwork() {
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, BootReceiverEntryPoint::class.java)
        val repository = WishlistRepository(entry.wishlistDao(), entry.alertScheduler(), entry.userPreferences())
        val gameId = "play_" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("com.bandainamcoent.opbrww".toByteArray())
        val previouslySaved = runBlocking { repository.observeGameById(gameId).first()?.inWishlist == true }
        runBlocking {
            UserPreferences(context).setOnboardingDone(true)
            UserPreferences(context).setNotificationsAsked(true)
        }
        try { ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(20000) { compose.onAllNodesWithText("Search").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Search").performClick()
            compose.onNodeWithText("Android").performClick()
            compose.onNode(hasSetTextAction()).performTextInput("one piece")
            compose.waitUntil(20000) { compose.onAllNodesWithText("ONE PIECE Bounty Rush").fetchSemanticsNodes().isNotEmpty() }
            compose.waitUntil(15000) { compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Artwork loaded"), true).fetchSemanticsNodes().isNotEmpty() }
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            screenshot("one-piece-live-results")
            compose.onNodeWithText("ONE PIECE Bounty Rush").performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithText(if (previouslySaved) "Remove from wishlist" else "Add to wishlist").fetchSemanticsNodes().isNotEmpty() }
            screenshot("one-piece-live-details")
            if (!previouslySaved) {
                compose.onAllNodesWithText("Add to wishlist").onFirst().performScrollTo().performClick()
                compose.waitUntil(5000) { runBlocking { repository.observeGameById(gameId).first()?.inWishlist == true } }
            }
        } } finally { if (!previouslySaved) runBlocking { repository.removeGameFromWishlist(gameId) } }
    }

    /** Run with POST_NOTIFICATIONS revoked and its user flags cleared before instrumentation. */
    @Test fun onboardingIconsAndRealAndroidPermissionAllow() {
        val prefs = UserPreferences(context)
        assertFalse("Revoke permission before this test", NotificationManagerCompat.from(context).areNotificationsEnabled())
        runBlocking { prefs.setOnboardingDone(false); prefs.setNotificationsAsked(false); prefs.setThemeMode("light") }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(20000) { compose.onAllNodesWithText("Continue as guest").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Continue as guest").performClick()
            compose.onNodeWithText("PC").performClick()
            compose.onNodeWithText("PS5").performClick()
            compose.onNodeWithText("PS5").assertIsOn()
            screenshot("platform-icons-light")
            runBlocking { prefs.setThemeMode("dark") }
            compose.waitForIdle()
            screenshot("platform-icons-dark")
            compose.onNodeWithText("Continue with 2 platforms").performClick()
            compose.onNodeWithText("Start my wishlist").performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText("Turn on notifications").fetchSemanticsNodes().isNotEmpty() }
            screenshot("notification-explanation")
            compose.onNodeWithText("Turn on notifications").performClick()
            var allow: AccessibilityNodeInfo? = null
            compose.waitUntil(10000) {
                fun findAllow(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
                    if (node == null) return null
                    if (node.viewIdResourceName?.endsWith(":id/permission_allow_button") == true) return node
                    for (i in 0 until node.childCount) findAllow(node.getChild(i))?.let { return it }
                    return null
                }
                allow = findAllow(instrumentation.uiAutomation.rootInActiveWindow)
                allow != null
            }
            screenshot("android-notification-permission")
            assertTrue(allow!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            compose.waitUntil(10000) { NotificationManagerCompat.from(context).areNotificationsEnabled() && runBlocking { prefs.notificationsAsked.first() && prefs.notificationsEnabled.first() } }
        }
        runBlocking { prefs.setPreferredPlatforms(emptySet()) }
    }

    @Test fun returningUserCanDeclineWithoutRepeatedPrompt() {
        val prefs = UserPreferences(context)
        assertFalse(NotificationManagerCompat.from(context).areNotificationsEnabled())
        runBlocking { prefs.setOnboardingDone(true); prefs.setNotificationsAsked(false) }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntil(15000) { compose.onAllNodesWithText("Not now").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Not now").performClick()
            compose.waitUntil(5000) { runBlocking { prefs.notificationsAsked.first() && !prefs.notificationsEnabled.first() } }
            scenario.recreate()
            compose.waitForIdle()
            compose.onNodeWithText("Turn on notifications").assertDoesNotExist()
            compose.onNodeWithText("Search").performClick()
            compose.onNodeWithText("Find games").assertIsDisplayed()
        }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val output = File(context.getExternalFilesDir(null), "search-permission/$name.png")
        output.parentFile!!.mkdirs()
        output.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
