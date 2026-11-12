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

class SearchKeyboardRegressionTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun fastNativeKeyboardClearAndRecentSelectionKeepTheExactQuery() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        runBlocking {
            val prefs = UserPreferences(instrumentation.targetContext)
            prefs.setOnboardingDone(true); prefs.setNotificationsAsked(true); prefs.setPreferredPlatforms(emptySet())
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntil(15000) { compose.onAllNodesWithText("Search").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Search").performClick()
            compose.onNodeWithText("Android").performClick()
            compose.onNode(hasSetTextAction()).performClick()
            try { compose.waitUntil(5000) {
                var shown = false
                scenario.onActivity { activity ->
                    shown = androidx.core.view.ViewCompat.getRootWindowInsets(activity.window.decorView)
                        ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
                }
                shown
            } } catch (error: Throwable) {
                scenario.onActivity { activity ->
                    android.util.Log.e("WishPlayKeyboard", "windowFocus=${activity.hasWindowFocus()} viewFocus=${activity.currentFocus} imeVisible=${androidx.core.view.ViewCompat.getRootWindowInsets(activity.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())}")
                }
                val folder = instrumentation.targetContext.getExternalFilesDir(null)!!
                instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                    java.io.File(folder, "native-keyboard-failure.png").outputStream().use {
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                    }
                }
                val root = instrumentation.uiAutomation.rootInActiveWindow
                val text = StringBuilder()
                fun visit(node: android.view.accessibility.AccessibilityNodeInfo, depth: Int) {
                    if (depth > 30) return
                    text.appendLine("${node.className} text=${node.text} focused=${node.isFocused}")
                    for (index in 0 until node.childCount) node.getChild(index)?.let { visit(it, depth + 1) }
                }
                root?.let { visit(it, 0) }
                java.io.File(folder, "native-keyboard-failure.txt").writeText(text.toString())
                throw error
            }
            // Individual native key events exercise the path that an atomic semantics edit misses.
            instrumentation.sendStringSync("off-road league online")
            compose.onNode(hasSetTextAction()).assertTextEquals("off-road league online")
            compose.onNode(hasSetTextAction()).performImeAction()
            compose.waitUntil(15000) { compose.onAllNodesWithText("Offroad League Online").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Clear search").performClick()
            compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
            compose.waitUntil(5000) { compose.onAllNodesWithText("off-road league online").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("off-road league online").performClick()
            compose.onNode(hasSetTextAction()).assertTextEquals("off-road league online")
        }
    }
}
