package com.nuvetrix.wishplay.ui.wishlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvetrix.wishplay.R
import com.nuvetrix.wishplay.domain.model.FilterOption
import com.nuvetrix.wishplay.domain.model.ReleaseStatus
import com.nuvetrix.wishplay.domain.model.SortOption
import com.nuvetrix.wishplay.ui.components.ButtonStyle
import com.nuvetrix.wishplay.ui.components.CookieShape
import com.nuvetrix.wishplay.ui.components.CookieShapeType
import com.nuvetrix.wishplay.ui.components.ExpressiveButton
import com.nuvetrix.wishplay.ui.components.ExpressiveChip
import com.nuvetrix.wishplay.ui.components.GroupPosition
import com.nuvetrix.wishplay.ui.theme.WishPlayMotion
import com.nuvetrix.wishplay.ui.theme.WishPlayThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    viewModel: WishlistViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCustom: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val customColors = WishPlayThemeColors
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val filteredGames = remember(uiState.games, uiState.selectedFilter) {
        when (uiState.selectedFilter) {
            FilterOption.ALL -> uiState.games
            FilterOption.UPCOMING -> uiState.games.filter { it.status() == ReleaseStatus.UPCOMING }
            FilterOption.OUT_NOW -> uiState.games.filter { it.status() == ReleaseStatus.OUT_NOW }
            FilterOption.TBA -> uiState.games.filter { it.status() == ReleaseStatus.TBA }
        }
    }

    val sortedGames = remember(filteredGames, uiState.selectedSort) {
        WishlistGrouping.sortGames(filteredGames, uiState.selectedSort)
    }

    val groupedGames = remember(sortedGames, uiState.selectedSort) {
        WishlistGrouping.groupGames(sortedGames, uiState.selectedSort)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.surface
        ) { paddingValues ->
            if (uiState.games.isEmpty()) {
                FirstRunEmptyWishlist(
                    onSearch = onNavigateToSearch,
                    onAddCustom = onNavigateToCustom,
                    onProfile = onNavigateToProfile,
                    modifier = Modifier.padding(paddingValues)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Header
                    item {
                        WishlistHeader(
                            totalCount = uiState.games.size,
                            isPro = uiState.isPro,
                            onShare = { /* Phase 1 Share preview */ },
                            onProfile = onNavigateToProfile
                        )
                    }

                    // Next Drop Hero Card
                    item {
                        uiState.nextDropGame?.let { nextGame ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                NextDropHeroCard(
                                    game = nextGame,
                                    onClick = { onNavigateToDetails(nextGame.id) }
                                )
                            }
                        }
                    }

                    // Filter chips
                    item {
                        FilterChipsRow(
                            selectedFilter = uiState.selectedFilter,
                            onSelectFilter = viewModel::setFilter,
                            allCount = uiState.games.size,
                            upcomingCount = uiState.games.count { it.status() == ReleaseStatus.UPCOMING },
                            outCount = uiState.games.count { it.status() == ReleaseStatus.OUT_NOW },
                            tbaCount = uiState.games.count { it.status() == ReleaseStatus.TBA }
                        )
                    }

                    // Sort Control Row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${sortedGames.size} shown",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            ExpressiveChip(
                                selected = false,
                                onClick = viewModel::showSortSheet,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Sort,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = uiState.selectedSort.title
                            )
                        }
                    }

                    // Grouped Game List
                    if (sortedGames.isEmpty()) {
                        item {
                            EmptyFilterView(
                                filter = uiState.selectedFilter,
                                totalCount = uiState.games.size,
                                onShowAll = { viewModel.setFilter(FilterOption.ALL) }
                            )
                        }
                    } else {
                        groupedGames.forEach { group ->
                            if (group.header != null) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = group.header.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${group.games.size}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            itemsIndexed(group.games) { index, game ->
                                val position = when {
                                    group.games.size == 1 -> GroupPosition.ONLY
                                    index == 0 -> GroupPosition.FIRST
                                    index == group.games.lastIndex -> GroupPosition.LAST
                                    else -> GroupPosition.MIDDLE
                                }

                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 1.5.dp)) {
                                    GameRow(
                                        game = game,
                                        position = position,
                                        onClick = { onNavigateToDetails(game.id) }
                                    )
                                }
                            }
                        }
                    }

                    // Free list cap row
                    if (!uiState.isPro) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(
                                        BorderStroke(1.5.dp, customColors.outlineVariant),
                                        RoundedCornerShape(24.dp)
                                    )
                                    .clickable { /* Opens Pro Sheet */ }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${uiState.games.size} of 25 games",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Free lists hold up to 25. Go unlimited with Pro.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB scrim when open
        AnimatedVisibility(
            visible = uiState.isFabExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(customColors.scrim)
                    .clickable { viewModel.closeFab() }
            )
        }

        // Expandable FAB & Menu
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.isFabExpanded) {
                    ExpressiveButton(
                        onClick = {
                            viewModel.closeFab()
                            onNavigateToSearch()
                        },
                        style = ButtonStyle.GOLD,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        text = "Search games"
                    )

                    ExpressiveButton(
                        onClick = {
                            viewModel.closeFab()
                            onNavigateToCustom()
                        },
                        style = ButtonStyle.GOLD,
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        text = "Add a custom game"
                    )
                }

                val fabRotation by animateFloatAsState(
                    targetValue = if (uiState.isFabExpanded) 45f else 0f,
                    animationSpec = WishPlayMotion.expressiveSpring(),
                    label = "fab_rotation"
                )

                FloatingActionButton(
                    onClick = viewModel::toggleFab,
                    containerColor = customColors.accent,
                    contentColor = customColors.onAccent,
                    shape = RoundedCornerShape(if (uiState.isFabExpanded) 32.dp else 22.dp),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add game",
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(fabRotation)
                    )
                }
            }
        }

        // Sort Sheet
        if (uiState.showSortSheet) {
            ModalBottomSheet(
                onDismissRequest = viewModel::hideSortSheet,
                sheetState = rememberModalBottomSheetState()
            ) {
                SortSheetContent(
                    selectedSort = uiState.selectedSort,
                    onSelectSort = viewModel::setSort
                )
            }
        }

        // Notification Priming Sheet
        if (uiState.showNotifPrimingSheet) {
            ModalBottomSheet(
                onDismissRequest = viewModel::hideNotifSheet,
                sheetState = rememberModalBottomSheetState()
            ) {
                NotificationPrimingSheetContent(
                    onAllow = viewModel::onAllowNotifications,
                    onDeny = viewModel::onDenyNotifications
                )
            }
        }
    }
}

@Composable
private fun WishlistHeader(
    totalCount: Int,
    isPro: Boolean,
    onShare: () -> Unit,
    onProfile: () -> Unit
) {
    val customColors = WishPlayThemeColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = stringResource(R.string.my_wishlist),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$totalCount games you're waiting on",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CookieShape(CookieShapeType.C9))
                    .background(if (isPro) customColors.accent else MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(onClick = onProfile),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "G",
                    color = if (isPro) customColors.onAccent else MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: FilterOption,
    onSelectFilter: (FilterOption) -> Unit,
    allCount: Int,
    upcomingCount: Int,
    outCount: Int,
    tbaCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExpressiveChip(
            selected = selectedFilter == FilterOption.ALL,
            onClick = { onSelectFilter(FilterOption.ALL) },
            leadingIcon = if (selectedFilter == FilterOption.ALL) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            label = "All",
            count = allCount
        )

        ExpressiveChip(
            selected = selectedFilter == FilterOption.UPCOMING,
            onClick = { onSelectFilter(FilterOption.UPCOMING) },
            leadingIcon = if (selectedFilter == FilterOption.UPCOMING) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            label = "Upcoming",
            count = upcomingCount
        )

        ExpressiveChip(
            selected = selectedFilter == FilterOption.OUT_NOW,
            onClick = { onSelectFilter(FilterOption.OUT_NOW) },
            leadingIcon = if (selectedFilter == FilterOption.OUT_NOW) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            label = "Out now",
            count = outCount
        )

        ExpressiveChip(
            selected = selectedFilter == FilterOption.TBA,
            onClick = { onSelectFilter(FilterOption.TBA) },
            leadingIcon = if (selectedFilter == FilterOption.TBA) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            label = "Date TBA",
            count = tbaCount
        )
    }
}

@Composable
private fun FirstRunEmptyWishlist(
    onSearch: () -> Unit,
    onAddCustom: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customColors = WishPlayThemeColors

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = stringResource(R.string.my_wishlist),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Nothing on it yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CookieShape(CookieShapeType.C9))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(onClick = onProfile),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "G",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        }

        // Empty card in the center
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(customColors.surfaceContainer)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CookieShape(CookieShapeType.C9))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.empty_wishlist_headline),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.empty_wishlist_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExpressiveButton(
                    onClick = onSearch,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.GOLD,
                    isLarge = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    text = stringResource(R.string.search_games)
                )

                ExpressiveButton(
                    onClick = onAddCustom,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.TEXT,
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    text = stringResource(R.string.add_custom_game)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EmptyFilterView(
    filter: FilterOption,
    totalCount: Int,
    onShowAll: () -> Unit
) {
    val customColors = WishPlayThemeColors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(customColors.surfaceContainer)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Nothing in this filter",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "You have $totalCount games on your list, but none of them are ${filter.label.lowercase()}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ExpressiveButton(
                onClick = onShowAll,
                style = ButtonStyle.TONAL,
                text = "Show all $totalCount"
            )
        }
    }
}

@Composable
private fun SortSheetContent(
    selectedSort: SortOption,
    onSelectSort: (SortOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Sort your list",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        SortOption.entries.forEach { option ->
            val isSelected = selectedSort == option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSort(option) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(
                            BorderStroke(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = option.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "Sorting by release date also groups your list by month.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotificationPrimingSheetContent(
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    val customColors = WishPlayThemeColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Want a heads-up before it drops?",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Android has to ask before WishPlay can send anything. Without it, nothing on your list can reach you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Notification mock preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, customColors.outlineVariant), RoundedCornerShape(20.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(customColors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = customColors.onAccent,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Pocket Kingdoms is out today",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Now on Android and iOS · tap to open",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        ExpressiveButton(
            onClick = onAllow,
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.GOLD,
            isLarge = true,
            text = "Turn on notifications"
        )

        ExpressiveButton(
            onClick = onDeny,
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.TEXT,
            text = "Not now"
        )

        Text(
            text = "You can change this any time in Profile. Each alert type is its own Android channel, so you can mute them one by one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
