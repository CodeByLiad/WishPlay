package com.jinatra.wishplay

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jinatra.wishplay.domain.model.Game
import com.jinatra.wishplay.ui.search.SearchResultRow
import com.jinatra.wishplay.ui.theme.WishPlayTheme
import org.junit.Rule
import org.junit.Test

class SearchListingUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectedPlatformShowsStoreAndDoesNotCallMissingDatesTba() {
        compose.setContent {
            WishPlayTheme(darkTheme = false) {
                Column {
                    SearchResultRow(Game("epic_one", "A shared title", platforms = mapOf("PC" to "2023-10-27")),
                        selectedPlatform = "PC", onClick = {}, onToggleWishlist = {})
                    SearchResultRow(Game("steam_123", "A shared title", platforms = mapOf("PC" to null)),
                        selectedPlatform = "PC", onClick = {}, onToggleWishlist = {})
                }
            }
        }
        compose.onNodeWithText("Epic Games Store · PC\nOut now").assertIsDisplayed()
        compose.onNodeWithText("Steam · PC\nDate not listed").assertIsDisplayed()
        compose.onAllNodesWithText("Date TBA", substring = true).assertCountEquals(0)
    }
}
