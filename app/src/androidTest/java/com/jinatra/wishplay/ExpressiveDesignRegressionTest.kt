package com.jinatra.wishplay

import android.graphics.Bitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.jinatra.wishplay.data.local.prefs.UserPreferences
import com.jinatra.wishplay.data.remote.api.StoreCatalog
import com.jinatra.wishplay.data.remote.api.WishPlayApiServiceImpl
import com.jinatra.wishplay.data.repository.WishlistRepository
import com.jinatra.wishplay.data.repository.toDomainModel
import com.jinatra.wishplay.alerts.BootReceiverEntryPoint
import com.jinatra.wishplay.ui.MainActivity
import com.jinatra.wishplay.ui.theme.*
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ExpressiveDesignRegressionTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun everyPaletteHasReadableTextAndDistinctSwitchThumb() {
        for (dark in listOf(false, true)) {
            val backgrounds = mutableSetOf<Int>()
            val primaryColors = mutableSetOf<Int>()
            for (accent in AccentColor.entries) {
                val c = wishPlayColorScheme(dark, accent)
                backgrounds += c.background.toArgb(); primaryColors += c.primary.toArgb()
                for ((foreground, background) in listOf(c.onSurface to c.surface, c.onSurfaceVariant to c.surfaceContainer,
                    c.onPrimary to c.primary, c.onPrimaryContainer to c.primaryContainer,
                    c.onSecondaryContainer to c.secondaryContainer)) {
                    assertTrue("Unreadable $accent dark=$dark", ColorUtils.calculateContrast(foreground.toArgb(), background.toArgb()) >= 4.5)
                }
                assertTrue("Invisible switch thumb $accent", ColorUtils.calculateContrast(c.onPrimary.toArgb(), c.primary.toArgb()) >= 4.5)
            }
            assertEquals(4, backgrounds.size); assertEquals(4, primaryColors.size)
        }
    }

    @Test fun steamRequirementsHandleMarkupAndMissingValuesHonestly() {
        val parser = StoreCatalog(OkHttpClient())
        val (level, storage) = parser.parseRequirements("<strong>Minimum:</strong><ul><li><strong>OS *:</strong> Windows 10 &amp; 11</li><li><strong>Processor:</strong> Intel Core i5<br>or AMD Ryzen 5</li><li><strong>Memory:</strong> 8 GB RAM</li><li><strong>Graphics:</strong> NVIDIA GTX 1060</li><li><strong>Storage:</strong> 25 GB available space</li></ul>")
        assertEquals("Windows 10 & 11", level!!.os)
        assertEquals("Intel Core i5 or AMD Ryzen 5", level.cpu)
        assertEquals("8 GB RAM", level.ram)
        assertEquals("NVIDIA GTX 1060", level.gpu)
        assertEquals("25 GB available space", storage)
        assertNull(parser.parseRequirements("").first)
        assertNull(parser.parseRequirements("<p>Requires a 64-bit processor.</p>").first)
    }

    @Test fun liveWorldSeekerHasMinimumRecommendedAndStorage() = runBlocking {
        val game = requireNotNull(WishPlayApiServiceImpl(OkHttpClient()).getGameDetails("steam_755500"))
        assertTrue(game.title.contains("WORLD SEEKER", true))
        assertTrue(requireNotNull(game.requirements?.min?.os).contains("Windows", true))
        assertFalse(game.requirements?.min?.cpu.isNullOrBlank())
        assertFalse(game.requirements?.rec?.gpu.isNullOrBlank())
        assertTrue(requireNotNull(game.storageSizes["PC"]).contains("GB"))
    }

    @Test fun settingsApplyAndPersistAllPalettesAndLibraryUsesRealArtwork() {
        val prefs = UserPreferences(context)
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, BootReceiverEntryPoint::class.java)
        val repository = WishlistRepository(entry.wishlistDao(), entry.alertScheduler(), entry.userPreferences())
        val api = WishPlayApiServiceImpl(OkHttpClient())
        val added = mutableListOf<String>()
        runBlocking {
            prefs.setOnboardingDone(true); prefs.setNotificationsAsked(true); prefs.setPreferredPlatforms(emptySet())
            for (id in listOf("steam_755500", "steam_1245620", "steam_1086940")) {
                val previous = repository.observeGameById(id).first()?.inWishlist == true
                val game = requireNotNull(api.getGameDetails(id)).toDomainModel()
                repository.saveGame(game)
                if (!previous) { repository.addGameToWishlist(id).getOrThrow(); added += id }
            }
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                compose.waitUntil(20000) { compose.onAllNodesWithText("My wishlist").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("Profile").performClick()
                compose.onNodeWithText("Settings").assertIsDisplayed()
                for (dark in listOf(false, true)) {
                    compose.onNodeWithText(if (dark) "Dark" else "Light").performScrollTo().performClick()
                    for (accent in AccentColor.entries) {
                        compose.onNodeWithContentDescription("${accent.label} accent").performScrollTo().performClick()
                        compose.waitUntil(5000) { runBlocking { prefs.accentColor.first() == accent.name.lowercase() } }
                        compose.waitForIdle()
                        val expected = wishPlayColorScheme(dark, accent).surface.toArgb()
                        val title = compose.onNodeWithText("Settings").fetchSemanticsNode().boundsInWindow
                        val screenshot = instrumentation.uiAutomation.takeScreenshot()
                        val actual = screenshot.getPixel(3, title.center.y.toInt().coerceAtLeast(1))
                        assertEquals("Palette did not reach the screen: $accent dark=$dark", expected, actual)
                        if (accent == AccentColor.TEAL || accent == AccentColor.VIOLET) screenshot("settings-${accent.name.lowercase()}-${if (dark) "dark" else "light"}")
                    }
                }
                runBlocking { prefs.setAccentColor("teal"); prefs.setThemeMode("light") }
                scenario.recreate()
                compose.onNodeWithText("Wishlist").performClick()
                compose.waitUntil(10000) { compose.onAllNodesWithText("ONE PIECE World Seeker", substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty() }
                screenshot("library-teal-light")
                compose.onNodeWithText("ONE PIECE World Seeker", substring = true, ignoreCase = true).performScrollTo().performClick()
                compose.onNodeWithText("System requirements").performScrollTo()
                compose.onNodeWithText("Minimum").performScrollTo().performClick()
                compose.onNodeWithText("Windows 7 64-bit SP1").assertExists()
                compose.onNodeWithText("25 GB available space").performScrollTo()
                screenshot("requirements-minimum")
                compose.onNodeWithText("Recommended").performScrollTo().performClick()
                compose.onNodeWithText("Windows 10").assertExists()
                compose.onNodeWithText("25 GB available space").performScrollTo()
                screenshot("requirements-recommended")
                compose.onNodeWithContentDescription("Back").performScrollTo().performClick()
                compose.waitUntil(10000) { compose.onAllNodesWithText("Alerts").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("Alerts").performClick()
                compose.onNodeWithText("Delivery").performScrollTo()
                screenshot("alerts-switches")
            }
        } finally { runBlocking { added.forEach { repository.removeGameFromWishlist(it) } } }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        assertTrue("System overlay blocks visual verification", instrumentation.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("isn't responding").isNullOrEmpty())
        val output = File(context.getExternalFilesDir(null), "expressive/$name.png")
        output.parentFile!!.mkdirs()
        output.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
