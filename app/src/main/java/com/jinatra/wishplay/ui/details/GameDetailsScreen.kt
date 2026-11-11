@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.jinatra.wishplay.ui.details

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jinatra.wishplay.R
import com.jinatra.wishplay.ui.components.ButtonStyle
import com.jinatra.wishplay.ui.components.CookieShapeType
import com.jinatra.wishplay.ui.components.ExpressiveButton
import com.jinatra.wishplay.ui.components.ExpressiveChip
import com.jinatra.wishplay.ui.components.GameLogo
import com.jinatra.wishplay.ui.components.WavyProgressIndicator
import com.jinatra.wishplay.ui.theme.BodyFontFamily
import com.jinatra.wishplay.ui.theme.DisplayFontFamily
import com.jinatra.wishplay.ui.theme.WishPlayThemeColors
import java.time.LocalDate

@Composable
fun GameDetailsScreen(
    viewModel: GameDetailsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val customColors = WishPlayThemeColors
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    var showRemove by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val game = uiState.game
    if (game == null) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            if (uiState.isLoading) CircularProgressIndicator()
            else {
                Text(uiState.error ?: "Game details are unavailable.")
                ExpressiveButton(onClick = viewModel::refresh, text = "Try again")
            }
        }
        return
    }

    if (showRemove) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRemove = false },
            title = { Text("Remove from wishlist?") },
            text = { Text("${game.title} will leave your wishlist and its release reminders will stop.") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = {
                showRemove = false; viewModel.toggleWishlist()
            }) { Text("Remove") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { showRemove = false }) { Text("Keep game") } }
        )
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding() + 40.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val gameColor = try {
                Color(android.graphics.Color.parseColor(game.hueHex))
            } catch (_: Exception) {
                customColors.accent
            }

            // 1. Cover Image Banner with Back Button (extends behind status bar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (game.coverUrl.isNullOrBlank()) 112.dp else 240.dp)
                    .background(gameColor)
            ) {
                if (!game.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = game.coverUrl,
                        contentDescription = "Cover for ${game.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Top gradient scrim for high-contrast status bar icon visibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x99000000), Color.Transparent)
                            )
                        )
                )

                // Scrimmed Back Button with statusBarsPadding so it never clashes with status bar icons
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                                                .padding(start = 16.dp, top = 8.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x8C16132E))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp).clickable { viewModel.refresh() })
            }
            // 2. Logo overlapping Cover
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-38).dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(5.dp)
                ) {
                    GameLogo(
                        title = game.title,
                        hue = gameColor,
                        shapeType = CookieShapeType.fromKey(game.shapeKey),
                        size = 76.dp,
                        imageUrl = game.logoUrl ?: game.coverUrl, fallbackImageUrl = game.coverUrl
                    )
                }
            }

            // 3. Game Title and Developer
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-24).dp)
            ) {
                Text(
                    text = game.title,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (game.isCustom) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Custom game",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You added this yourself",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = game.developer ?: "Unknown developer",
                        fontFamily = BodyFontFamily,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4. Quick Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-14).dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveButton(
                    onClick = { if (game.inWishlist) showRemove = true else viewModel.toggleWishlist() },
                    enabled = !uiState.isUpdating,
                    modifier = Modifier.weight(1f),
                    style = if (game.inWishlist) ButtonStyle.TONAL else ButtonStyle.GOLD,
                    leadingIcon = {
                        Icon(
                            imageVector = if (game.inWishlist) Icons.Default.Delete else Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    text = if (uiState.isUpdating) "Updating…" else if (game.inWishlist) "Remove from list" else "Add to wishlist"
                )

                if (game.inWishlist) {
                    IconButton(
                        onClick = viewModel::toggleAlert,
                        enabled = !uiState.isUpdating,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (game.alertEnabled) customColors.accent else customColors.surfaceContainerHigh)
                    ) {
                        Icon(
                            imageVector = if (game.alertEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = if (game.alertEnabled) "Turn release alerts off" else "Turn release alerts on",
                            tint = if (game.alertEnabled) customColors.onAccent else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Follow ${game.title} on WishPlay!")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share game"))
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(BorderStroke(1.dp, customColors.outlineVariant), RoundedCornerShape(24.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 5. Platform Switcher Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                game.platforms.keys.forEach { p ->
                    val isSelected = uiState.selectedPlatform == p
                    ExpressiveChip(
                        selected = isSelected,
                        onClick = { viewModel.selectPlatform(p) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        label = p
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Release Date Hero Card
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!game.isCustom) Text("Release dates by source", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                game.platforms.forEach { (platform, date) ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.selectPlatform(platform) }.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(platform, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        Text(date ?: "Date TBA", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val evidence = game.releaseEvidence[platform].orEmpty()
                    if (evidence.map { it.date }.distinct().size > 1) Text("Sources differ · linked store date shown above",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    evidence.forEach { source ->
                        Text("${source.source}: ${source.date ?: "Date TBA"}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { runCatching { uriHandler.openUri(source.url) } }.padding(vertical = 8.dp))
                    }
                }
            }
            if (game.inWishlist) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Platforms to track", style = MaterialTheme.typography.headlineMedium)
                    Text("Only selected platforms send reminders.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExpressiveChip(selected = game.trackedPlatforms == null,
                            onClick = { viewModel.trackPlatforms(null) }, label = "All platforms")
                        game.platforms.keys.forEach { platform ->
                            ExpressiveChip(selected = game.trackedPlatforms?.contains(platform) == true,
                                onClick = { viewModel.trackPlatforms(platform) }, label = platform)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            val platformDate = uiState.selectedPlatform?.let { game.platforms[it] }
            val daysUntil = platformDate?.let {
                try {
                    java.time.temporal.ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        LocalDate.parse(it)
                    ).toInt()
                } catch (_: Exception) {
                    null
                }
            }

            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                when {
                    platformDate == null -> {
                        // TBA card (Prototype: 2px dashed outline, "To be announced", "Expected in 2027")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(28.dp))
                                .border(
                                    BorderStroke(2.dp, customColors.outlineVariant),
                                    RoundedCornerShape(28.dp)
                                )
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Release date",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (game.id.startsWith("steam_") || game.id.startsWith("play_") || game.id.startsWith("apple_")) "Date unavailable" else stringResource(R.string.tba_date),
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (game.expectedYear != null) "Expected in ${game.expectedYear}. We'll alert you the moment a date is set."
                                    else "This source has not provided a release date. You can still save this game.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    daysUntil != null && daysUntil <= 0 -> {
                        // Out now card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(28.dp))
                                .background(customColors.successContainer)
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Out now on ${uiState.selectedPlatform}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = customColors.success
                                )
                                Text(
                                    text = "Released $platformDate",
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp,
                                    color = customColors.success
                                )
                            }
                        }
                    }
                    else -> {
                        // Upcoming Hero Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(32.dp))
                                .background(customColors.accent)
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (game.isCustom) "Your date: $platformDate" else "Releases $platformDate",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = customColors.onAccent
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${daysUntil ?: 0}",
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 72.sp,
                                        lineHeight = 64.sp,
                                        color = customColors.onAccent
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (daysUntil == 1) "day to go" else "days to go",
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = customColors.onAccent,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                WavyProgressIndicator(
                                    progress = game.progress,
                                    color = customColors.onAccent
                                )

                                if (!game.movedFromDate.isNullOrBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Update,
                                            contentDescription = null,
                                            tint = customColors.onAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Delayed from ${game.movedFromDate}",
                                            fontFamily = BodyFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = customColors.onAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 7. Notes Section for Custom Game
            if (game.isCustom) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Your notes",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (!game.customNotes.isNullOrBlank()) game.customNotes else "No notes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "WishPlay can't fetch covers, trailers or requirements for custom games. Your alerts still work.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                // 8. Trailer Section
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Trailer",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (game.hasTrailer) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(gameColor)
                                .clickable {
                                    val videoId = game.trailerYoutubeId ?: "dQw4w9WgXcQ"
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.youtube.com/watch?v=$videoId")
                                    )
                                    context.startActivity(intent)
                                }
                        ) {
                            if (!game.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = game.coverUrl,
                                    contentDescription = "Trailer preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Dark overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x5516132E))
                            )

                            // Play Button in center
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(customColors.accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play trailer",
                                    tint = customColors.onAccent,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Watch on YouTube caption at bottom
                            Text(
                                text = "Watch on YouTube",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp)
                            )
                        }
                    } else {
                        // Trailer TBA Card (PRD: "Trailer will be shared later")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .border(
                                    BorderStroke(2.dp, customColors.outlineVariant),
                                    RoundedCornerShape(24.dp)
                                )
                                .padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Trailer unavailable from this source",
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Open the store listing to check available videos.",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val listingUrl = game.releaseEvidence[uiState.selectedPlatform].orEmpty().firstOrNull()?.url
                if (listingUrl != null && listingUrl.startsWith("https://")) {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        ExpressiveButton(text = "Open store listing", style = ButtonStyle.TONAL,
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(listingUrl))) })
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // 9. About Section
                if (!game.about.isNullOrBlank()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = game.about,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 10. System Requirements Section
                val activePlatform = uiState.selectedPlatform ?: "PC"
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (activePlatform == "PC") "System requirements" else "Requirements",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    when (activePlatform) {
                        "PC" -> {
                            androidx.compose.material3.SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                listOf("min" to "Minimum", "rec" to "Recommended").forEachIndexed { index, (mode, label) ->
                                    SegmentedButton(selected = uiState.reqMode == mode, onClick = { viewModel.setReqMode(mode) },
                                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index, 2)) { Text(label) }
                                }
                            }

                            val level = if (uiState.reqMode == "min") game.requirements?.min else game.requirements?.rec
                            val keys = listOf("OS", "CPU", "GPU", "RAM")

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(customColors.surfaceContainer)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                keys.forEach { k ->
                                    val v = when (k) {
                                        "OS" -> level?.os
                                        "CPU" -> level?.cpu
                                        "GPU" -> level?.gpu
                                        "RAM" -> level?.ram
                                        else -> null
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = k,
                                            fontFamily = BodyFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = v ?: "Not provided by source",
                                            modifier = Modifier.weight(1f).padding(start = 20.dp),
                                            fontFamily = BodyFontFamily,
                                            fontWeight = if (v != null) FontWeight.Medium else FontWeight.Normal,
                                            fontSize = 14.sp,
                                            color = if (v != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        "Android" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(customColors.surfaceContainer)
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Android",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Check compatibility on Google Play",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        "iOS" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(customColors.surfaceContainer)
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "iOS",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Check compatibility on the App Store",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        else -> {
                            // Consoles: "Runs on any PS5. No extra hardware needed."
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(customColors.surfaceContainer)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Check the $activePlatform store listing for edition and hardware compatibility.",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 11. Space needed section
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Space needed",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val size = uiState.selectedPlatform?.let { game.storageSizes[it] }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(customColors.surfaceContainer)
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = customColors.accent,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = size ?: "Storage size unavailable",
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = if (size != null) FontWeight.ExtraBold else FontWeight.Normal,
                                    fontSize = if (size != null) 22.sp else 15.sp,
                                    color = if (size != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "On ${uiState.selectedPlatform ?: "selected platform"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 12. Price section (PC & game.price != null)
                if (activePlatform == "PC" && game.price != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Price on ${game.price.store}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(customColors.surfaceContainer)
                                .padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = game.price.now,
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 32.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!game.price.was.isNullOrBlank()) {
                                        Text(
                                            text = game.price.was,
                                            fontFamily = BodyFontFamily,
                                            fontSize = 16.sp,
                                            textDecoration = TextDecoration.LineThrough,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(customColors.successContainer)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "20% off",
                                                fontFamily = BodyFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = customColors.success
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (game.inWishlist) "Alert me when the price drops" else "Add to wishlist to enable price alerts",
                                        fontFamily = BodyFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    ExpressiveButton(
                                        onClick = viewModel::togglePriceAlert,
                                        style = ButtonStyle.GOLD,
                                        enabled = !uiState.isUpdating && game.inWishlist,
                                        text = if (game.priceAlertEnabled) "Turn off" else "Turn on"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 13. Remove Game / Add Game Bottom Button
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                if (game.inWishlist) {
                    ExpressiveButton(
                        onClick = { if (game.inWishlist) showRemove = true else viewModel.toggleWishlist() },
                    enabled = !uiState.isUpdating,
                        modifier = Modifier.fillMaxWidth(),
                        style = ButtonStyle.DANGER,
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        text = "Remove from wishlist"
                    )
                } else {
                    ExpressiveButton(
                        onClick = { if (game.inWishlist) showRemove = true else viewModel.toggleWishlist() },
                    enabled = !uiState.isUpdating,
                        modifier = Modifier.fillMaxWidth(),
                        style = ButtonStyle.GOLD,
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = "Add to wishlist"
                    )
                }
            }
        }
    }
}
