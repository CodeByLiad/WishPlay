package com.jinatra.wishplay

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jinatra.wishplay.ui.components.ReleaseDateField
import com.jinatra.wishplay.ui.profile.DonationSection
import android.content.ClipboardManager
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.jinatra.wishplay.ui.theme.WishPlayTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UxComponentRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun optionalDonationShowsAndCopiesOnlyTheRequestedNumber() {
        compose.setContent {
            WishPlayTheme(darkTheme = false) {
                DonationSection()
            }
        }
        compose.onNodeWithText("01978900129").assertIsDisplayed()
        compose.onNodeWithText("Copy bKash number").performClick()
        compose.onNodeWithText("Number copied").assertIsDisplayed()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val clipboard = instrumentation.targetContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals("01978900129", clipboard.primaryClip!!.getItemAt(0).text.toString())
        }
        compose.onNodeWithText("Donations are optional and don't unlock extra features.").assertIsDisplayed()
    }

    @Test fun calendarCancelPreservesInputAndConfirmationPreservesDate() {
        var value = "2027-01-01"
        compose.setContent {
            var date by remember { mutableStateOf(value) }
            WishPlayTheme(darkTheme = true) {
                ReleaseDateField(value = date, onValueChange = { date = it; value = it })
            }
        }
        compose.onNodeWithContentDescription("Choose Release date").performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals("2027-01-01", value)
        compose.onNodeWithContentDescription("Choose Release date").performClick()
        compose.onNodeWithText("Use date").performClick()
        assertEquals("2027-01-01", value)
        compose.onNode(hasSetTextAction()).performTextReplacement("2027-02-30")
        compose.onNodeWithText("Use YYYY-MM-DD or choose a date from the calendar.").assertIsDisplayed()
    }
}
