package com.jinatra.wishplay

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.jinatra.wishplay.data.local.prefs.UserPreferences
import com.jinatra.wishplay.ui.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ConsoleDiscoveryUiTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun playStationAndSwitch2SearchOpenRealDetails() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        runBlocking {
            UserPreferences(instrumentation.targetContext).apply {
                setOnboardingDone(true); setNotificationsAsked(true); setPreferredPlatforms(emptySet())
            }
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(20000) { compose.onAllNodesWithText("Search").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Search").performClick()
            compose.onNodeWithText("PS5").performScrollTo().performClick()
            compose.onNode(hasSetTextAction()).performTextInput("Astro Bot")
            compose.waitUntil(20000) { compose.onAllNodesWithText("ASTRO BOT").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("ASTRO BOT").performClick()
            compose.waitUntil(20000) { compose.onAllNodesWithText("2024-09-06", substring = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithText("Switch 2").performScrollTo().performClick()
            compose.onNode(hasSetTextAction()).performTextReplacement("Mario Kart World")
            compose.waitUntil(20000) { compose.onAllNodesWithText("Mario Kart World", useUnmergedTree = false).fetchSemanticsNodes().size > 1 }
            compose.onAllNodesWithText("Mario Kart World").filter(!hasSetTextAction()).onFirst().performClick()
            compose.waitUntil(20000) { compose.onAllNodesWithText("2025-06-05", substring = true).fetchSemanticsNodes().isNotEmpty() }
        }
    }
}
