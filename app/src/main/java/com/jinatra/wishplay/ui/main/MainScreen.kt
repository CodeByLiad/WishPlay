package com.jinatra.wishplay.ui.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jinatra.wishplay.ui.navigation.NavigationTab
import com.jinatra.wishplay.ui.theme.WishPlayMotion
import com.jinatra.wishplay.ui.theme.WishPlayThemeColors
import com.jinatra.wishplay.ui.wishlist.WishlistScreen
import com.jinatra.wishplay.ui.wishlist.WishlistViewModel

@Composable
fun MainScreen(
    onNavigateToDetails: (String) -> Unit,
    onNavigateToCustom: () -> Unit,
    onNavigateToCalendar: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(NavigationTab.WISHLIST) }
    val wishlistViewModel: WishlistViewModel = hiltViewModel()
    val searchViewModel: com.jinatra.wishplay.ui.search.SearchViewModel = hiltViewModel()
    val alertsViewModel: com.jinatra.wishplay.ui.alerts.AlertsViewModel = hiltViewModel()
    val profileViewModel: com.jinatra.wishplay.ui.profile.ProfileViewModel = hiltViewModel()

    val tabStateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    androidx.activity.compose.BackHandler(enabled = selectedTab != NavigationTab.WISHLIST) {
        selectedTab = NavigationTab.WISHLIST
    }

    Scaffold(
        bottomBar = {
            WishPlayBottomNav(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            tabStateHolder.SaveableStateProvider(selectedTab.route) {
            when (selectedTab) {
                NavigationTab.WISHLIST -> {
                    WishlistScreen(
                        viewModel = wishlistViewModel,
                        onNavigateToDetails = onNavigateToDetails,
                        onNavigateToSearch = { selectedTab = NavigationTab.SEARCH },
                        onNavigateToCustom = onNavigateToCustom,
                        onNavigateToProfile = { selectedTab = NavigationTab.PROFILE }
                    )
                }
                NavigationTab.SEARCH -> {
                    com.jinatra.wishplay.ui.search.SearchScreen(
                        viewModel = searchViewModel,
                        onNavigateToDetails = onNavigateToDetails,
                        onNavigateToCustom = onNavigateToCustom
                    )
                }
                NavigationTab.ALERTS -> {
                    com.jinatra.wishplay.ui.alerts.AlertsScreen(
                        viewModel = alertsViewModel,
                        onNavigateToDetails = onNavigateToDetails,
                        onNavigateToCalendar = onNavigateToCalendar,
                        onNavigateToSearch = { selectedTab = NavigationTab.SEARCH }
                    )
                }
                NavigationTab.PROFILE -> {
                    com.jinatra.wishplay.ui.profile.ProfileScreen(
                        viewModel = profileViewModel
                    )
                }
            }
        }
        }
    }
}

@Composable
fun WishPlayBottomNav(
    selectedTab: NavigationTab,
    onSelectTab: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.NavigationBar(
        modifier = modifier,
        containerColor = WishPlayThemeColors.surfaceContainer,
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) {
        NavigationTab.entries.forEach { tab ->
            val icon = when (tab) {
                NavigationTab.WISHLIST -> Icons.Default.List
                NavigationTab.SEARCH -> Icons.Default.Search
                NavigationTab.ALERTS -> Icons.Default.Notifications
                NavigationTab.PROFILE -> Icons.Default.Person
            }
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(tab.title) }
            )
        }
    }
}
