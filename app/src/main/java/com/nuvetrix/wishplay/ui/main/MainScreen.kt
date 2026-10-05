package com.nuvetrix.wishplay.ui.main

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.nuvetrix.wishplay.ui.navigation.NavigationTab
import com.nuvetrix.wishplay.ui.theme.WishPlayMotion
import com.nuvetrix.wishplay.ui.theme.WishPlayThemeColors
import com.nuvetrix.wishplay.ui.wishlist.WishlistScreen
import com.nuvetrix.wishplay.ui.wishlist.WishlistViewModel

@Composable
fun MainScreen(
    onNavigateToDetails: (String) -> Unit,
    onNavigateToCustom: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToAdmin: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.WISHLIST) }
    val wishlistViewModel: WishlistViewModel = hiltViewModel()
    val searchViewModel: com.nuvetrix.wishplay.ui.search.SearchViewModel = hiltViewModel()
    val alertsViewModel: com.nuvetrix.wishplay.ui.alerts.AlertsViewModel = hiltViewModel()
    val profileViewModel: com.nuvetrix.wishplay.ui.profile.ProfileViewModel = hiltViewModel()

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
                .padding(bottom = 84.dp)
        ) {
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
                    com.nuvetrix.wishplay.ui.search.SearchScreen(
                        viewModel = searchViewModel,
                        onNavigateToDetails = onNavigateToDetails,
                        onNavigateToCustom = onNavigateToCustom
                    )
                }
                NavigationTab.ALERTS -> {
                    com.nuvetrix.wishplay.ui.alerts.AlertsScreen(
                        viewModel = alertsViewModel,
                        onNavigateToDetails = onNavigateToDetails,
                        onNavigateToCalendar = onNavigateToCalendar
                    )
                }
                NavigationTab.PROFILE -> {
                    com.nuvetrix.wishplay.ui.profile.ProfileScreen(
                        viewModel = profileViewModel,
                        onNavigateToAdmin = onNavigateToAdmin
                    )
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
    val customColors = WishPlayThemeColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(customColors.surfaceContainer)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavigationTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            val icon = when (tab) {
                NavigationTab.WISHLIST -> Icons.Default.List
                NavigationTab.SEARCH -> Icons.Default.Search
                NavigationTab.ALERTS -> Icons.Default.Notifications
                NavigationTab.PROFILE -> Icons.Default.Person
            }

            val indicatorWidth by animateDpAsState(
                targetValue = if (isSelected) 64.dp else 56.dp,
                animationSpec = WishPlayMotion.expressiveSpring(),
                label = "nav_width"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelectTab(tab) }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = indicatorWidth, height = 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.title,
                        tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = tab.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SearchPlaceholderTab(
    onAddCustom: () -> Unit,
    onOpenDetails: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Find games",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Full online IGDB and Steam search connects in Phase 2. In Phase 1, you can add any game directly as a custom game.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(WishPlayThemeColors.surfaceContainer)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Add a custom game",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Save any game WishPlay doesn't know yet, set the date, and your countdown alerts will fire automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                com.nuvetrix.wishplay.ui.components.ExpressiveButton(
                    onClick = onAddCustom,
                    style = com.nuvetrix.wishplay.ui.components.ButtonStyle.GOLD,
                    text = "Add custom game"
                )
            }
        }
    }
}

@Composable
fun AlertsPlaceholderTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Alerts",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Background WorkManager alerts and notification channels activate in Phase 3. Your scheduled dates from Phase 1 will automatically be hooked up.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProfilePlaceholderTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(WishPlayThemeColors.surfaceContainer)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Guest Mode",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "This phone only · Wishlist is saved in local encrypted database (SQLCipher). Sign-in sync arrives in Phase 4.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
