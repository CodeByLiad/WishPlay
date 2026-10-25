package com.jinatra.wishplay.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.jinatra.wishplay.domain.model.forPlatform
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinatra.wishplay.domain.model.Game
import com.jinatra.wishplay.ui.components.ButtonStyle
import com.jinatra.wishplay.ui.components.CookieShapeType
import com.jinatra.wishplay.ui.components.ExpressiveButton
import com.jinatra.wishplay.ui.components.ExpressiveChip
import com.jinatra.wishplay.ui.components.GameLogo
import com.jinatra.wishplay.ui.theme.BodyFontFamily
import com.jinatra.wishplay.ui.theme.DisplayFontFamily
import com.jinatra.wishplay.ui.theme.WishPlayMotion
import com.jinatra.wishplay.ui.theme.WishPlayThemeColors

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToCustom: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val customColors = WishPlayThemeColors
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val platforms = listOf("All", "Android", "PC", "PS5", "PS4", "Xbox Series", "Switch", "iOS")
    val platformShortNames = mapOf(
        "All" to "All",
        "PC" to "PC",
        "PS5" to "PS5",
        "PS4" to "PS4",
        "Xbox Series" to "Xbox",
        "Switch" to "Switch",
        "Android" to "Android",
        "iOS" to "iOS"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Screen Header
            Text(
                text = "Find games",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // Search Bar (Prototype .searchbar: height 58dp, radius 29dp, background var(--sch))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(58.dp)
                    .clip(RoundedCornerShape(29.dp))
                    .background(customColors.surfaceContainerHigh)
                    .padding(start = 18.dp, end = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChanged,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = BodyFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.onSubmitSearch()
                                keyboardController?.hide()
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (uiState.query.isEmpty()) {
                                Text(
                                    text = "Search any game",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (uiState.query.isNotEmpty()) {
                        IconButton(
                            onClick = viewModel::onClearQuery,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Platform Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                platforms.forEach { p ->
                    val isSelected = uiState.selectedPlatform == p
                    ExpressiveChip(
                        selected = isSelected,
                        onClick = { viewModel.onPlatformSelected(p) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        label = platformShortNames[p] ?: p
                    )
                }
            }

            // Main Content Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Before typing: Recent searches and Popular right now
                if (uiState.query.isBlank()) {
                    if (uiState.recentSearches.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent searches",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Clear all",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = customColors.accent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.onClearRecent() }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    uiState.recentSearches.forEach { r ->
                                        RecentSearchChip(
                                            query = r,
                                            onSelect = { viewModel.onSelectRecent(r) },
                                            onRemove = { viewModel.onRemoveRecent(r) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Popular right now
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Try a search",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.popularQueries.forEach { p ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(customColors.surfaceContainerHigh)
                                            .clickable { viewModel.onSelectRecent(p) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = customColors.accent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = p,
                                                fontFamily = BodyFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section Title
                item {
                    val title = when {
                        uiState.query.isNotBlank() -> "Results"
                        uiState.selectedPlatform == "All" -> "Explore all platforms"
                        else -> "Explore ${platformShortNames[uiState.selectedPlatform]} games"
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Loading State
                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = customColors.accent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Empty State (Query has no matches)
                if (!uiState.isLoading && uiState.error == null && uiState.results.isEmpty() &&
                    uiState.otherPlatformResults.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(customColors.surfaceContainer)
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "No match for “${uiState.query}”",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "If it's too new or too niche for the database, add it yourself and set the date.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ExpressiveButton(
                                onClick = onNavigateToCustom,
                                style = ButtonStyle.GOLD,
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                },
                                text = "Add as a custom game"
                            )
                        }
                    }
                }

                // Results list
                uiState.sourceNotice?.let { notice ->
                    item { Text(notice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(uiState.results, key = { it.id }) { game ->
                    SearchResultRow(
                        game = game,
                        selectedPlatform = uiState.selectedPlatform,
                        isUpdating = game.id in uiState.busyGameIds,
                        onClick = { viewModel.openDetails(game, onNavigateToDetails) },
                        onToggleWishlist = { viewModel.toggleWishlist(game) }
                    )
                }

                if (uiState.otherPlatformResults.isNotEmpty()) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = "On other platforms",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val label = platformShortNames[uiState.selectedPlatform] ?: uiState.selectedPlatform
                            Text(
                                text = "Not listed on $label yet — these match your search on other platforms.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(uiState.otherPlatformResults, key = { "other_${it.id}" }) { game ->
                        SearchResultRow(
                            game = game,
                            selectedPlatform = "All",
                            isUpdating = game.id in uiState.busyGameIds,
                            onClick = { viewModel.openDetails(game, onNavigateToDetails) },
                            onToggleWishlist = { viewModel.toggleWishlist(game) }
                        )
                    }
                }

                if (uiState.error != null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(uiState.error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ExpressiveButton(onClick = viewModel::retry, text = "Try again")
                        }
                    }
                } else if (uiState.hasMore && !uiState.isLoading) {
                    item { ExpressiveButton(onClick = viewModel::loadMore, text = "Load more games") }
                }
                item {
                    Column {
                        Text("Sources · store listings are for the US", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            listOf("RAWG" to "https://rawg.io", "Steam" to "https://store.steampowered.com", "App Store" to "https://apps.apple.com").forEach { (name, url) ->
                                Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { uriHandler.openUri(url) }.padding(vertical = 12.dp))
                            }
                        }
                    }
                }

                // Bottom note
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Can't find a game? ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Add it yourself",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = customColors.accent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onNavigateToCustom() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentSearchChip(
    query: String,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(WishPlayThemeColors.surfaceContainerHigh)
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = query,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { onSelect() }
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove $query",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .clickable { onRemove() }
            )
        }
    }
}

@Composable
fun SearchResultRow(
    game: Game,
    selectedPlatform: String = "All",
    isUpdating: Boolean = false,
    onClick: () -> Unit,
    onToggleWishlist: () -> Unit
) {
    val customColors = WishPlayThemeColors
    val gameColor = try {
        Color(android.graphics.Color.parseColor(game.hueHex))
    } catch (_: Exception) {
        customColors.accent
    }

    val releaseGame = game.forPlatform(selectedPlatform)
    val daysUntil = releaseGame.daysUntilRelease()
    val releaseLabel = when {
        daysUntil == null -> if (game.expectedYear != null) "TBA ${game.expectedYear}" else "Date TBA"
        daysUntil < 0 -> "Out now"
        daysUntil == 0 -> "Today"
        daysUntil == 1 -> "Tomorrow"
        daysUntil <= 14 -> "In $daysUntil days"
        else -> releaseGame.releaseDate ?: "Upcoming"
    }

    val sourceTag = when { game.id.startsWith("steam_") -> "Steam · "; game.id.startsWith("apple_") -> "App Store · "; else -> "" }
    val platformsText = sourceTag + game.platforms.keys.joinToString(", ")

    val buttonCornerRadius by animateDpAsState(
        targetValue = if (game.inWishlist) 24.dp else 16.dp,
        animationSpec = WishPlayMotion.expressiveSpring(),
        label = "btn_radius"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(customColors.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameLogo(
                title = game.title,
                hue = gameColor,
                shapeType = CookieShapeType.fromKey(game.shapeKey),
                size = 48.dp,
                imageUrl = game.logoUrl ?: game.coverUrl, fallbackImageUrl = game.coverUrl
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.title,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (game.platforms.values.all { it == null }) "$platformsText\nSee details for release dates"
                        else if (selectedPlatform == "All") "$platformsText\nRelease: $releaseLabel" else "$selectedPlatform · $releaseLabel",
                    fontFamily = BodyFontFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Add button (Prototype .addbtn: 48dp, spring corner radius, checkmark when done)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(buttonCornerRadius))
                    .background(
                        if (game.inWishlist) MaterialTheme.colorScheme.secondaryContainer
                        else customColors.accent
                    )
                    .clickable(
                        enabled = !isUpdating,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleWishlist
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isUpdating) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = customColors.onAccent, strokeWidth = 2.dp)
                else {
                Icon(
                    imageVector = if (game.inWishlist) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = if (game.inWishlist) "In your wishlist" else "Add to wishlist",
                    tint = if (game.inWishlist) MaterialTheme.colorScheme.onSecondaryContainer
                    else customColors.onAccent,
                    modifier = Modifier.size(22.dp)
                )
                }
            }
        }
    }
}
