package com.nuvetrix.wishplay.ui.profile

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nuvetrix.wishplay.R
import com.nuvetrix.wishplay.domain.model.Game
import com.nuvetrix.wishplay.domain.model.SyncState
import com.nuvetrix.wishplay.ui.alerts.sheets.BackgroundWorkSheet
import com.nuvetrix.wishplay.ui.alerts.sheets.NotificationPrimingSheet
import com.nuvetrix.wishplay.ui.components.ButtonStyle
import com.nuvetrix.wishplay.ui.components.CookieShape
import com.nuvetrix.wishplay.ui.components.CookieShapeType
import com.nuvetrix.wishplay.ui.components.ExpressiveButton
import com.nuvetrix.wishplay.ui.components.GameLogo
import com.nuvetrix.wishplay.ui.theme.WishPlayThemeColors

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToAdmin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val customColors = WishPlayThemeColors
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh notification status on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNotificationStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Permission launcher for POST_NOTIFICATIONS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshNotificationStatus()
    }

    // Snackbar triggers
    LaunchedEffect(uiState.snackMessage) {
        uiState.snackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnack()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header / Tabhead
            ProfileHeader(
                name = uiState.user.name,
                email = uiState.user.email,
                initials = uiState.user.initials,
                isGuest = uiState.user.isGuest,
                isPro = uiState.user.isPro,
                role = uiState.user.role
            )

            // Guest Banner (if guest)
            if (uiState.user.isGuest) {
                GuestBanner(
                    onSignInClick = { viewModel.openSignInSheet() }
                )
            }

            // Pro Upsell Hero Banner (if not Pro)
            if (!uiState.user.isPro) {
                ProUpsellCard(
                    onGetProClick = { viewModel.openProSheet() }
                )
            }

            // Admin Panel Entrypoint (Admin only)
            if (uiState.user.role == "admin") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(customColors.surfaceContainer)
                ) {
                    ProfileListItem(
                        icon = Icons.Default.Security,
                        iconBg = customColors.accent,
                        iconTint = customColors.onAccent,
                        title = "Admin panel",
                        subtitle = "Codes and bKash payments",
                        onClick = onNavigateToAdmin
                    )
                }
            }

            // Look and Feel Section
            LookAndFeelSection(
                selectedTheme = uiState.themeMode,
                onSelectTheme = { viewModel.setTheme(it) },
                selectedAccent = uiState.accentColor,
                onSelectAccent = { accentKey ->
                    if (!uiState.user.isPro && accentKey != "gold") {
                        viewModel.openProSheet()
                    } else {
                        viewModel.setAccent(accentKey)
                    }
                },
                isPro = uiState.user.isPro
            )

            // Home Screen Widget Section
            WidgetPreviewSection(
                nextDropGame = uiState.nextDropGame,
                isPro = uiState.user.isPro
            )

            // Alerts and Sharing Section
            AlertsAndSharingSection(
                notificationsAllowed = uiState.isNotificationsAllowed,
                onNotificationsClick = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !uiState.isNotificationsAllowed) {
                        viewModel.openPrimingSheet()
                    } else {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                },
                onBackgroundWorkClick = { viewModel.openBatterySheet() },
                onShareListClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "My WishPlay Wishlist")
                        putExtra(Intent.EXTRA_TEXT, "Check out my upcoming game wishlist on WishPlay! https://wishplay.app/l/tanvir")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share your wishlist"))
                }
            )

            // Account Section
            AccountSection(
                isGuest = uiState.user.isGuest,
                isPro = uiState.user.isPro,
                syncState = uiState.syncState,
                lastSyncedTime = uiState.lastSyncedTime,
                onSyncClick = { viewModel.triggerSync() },
                onRedeemClick = { viewModel.openRedeemSheet() },
                onCheckUpdateClick = { viewModel.checkForUpdates(isManual = true) },
                onPrivacyClick = { viewModel.openPrivacySheet() },
                onSignOutClick = { viewModel.signOut() }
            )

            // Danger Zone Section (if signed in)
            if (!uiState.user.isGuest) {
                DangerZoneSection(
                    onDeleteAccountClick = { viewModel.openDeleteSheet() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    // Modal Bottom Sheets
    if (uiState.isDeleteSheetOpen) {
        DeleteAccountSheet(
            onDismiss = { viewModel.closeDeleteSheet() },
            onConfirmDelete = { viewModel.deleteAccount() }
        )
    }

    if (uiState.isSignInSheetOpen) {
        val activityContext = LocalContext.current
        GoogleSignInSheet(
            onDismiss = { viewModel.closeSignInSheet() },
            onSignIn = { viewModel.signInWithGoogle(activityContext) }
        )
    }

    if (uiState.isRedeemSheetOpen) {
        RedeemCodeSheet(
            onDismiss = { viewModel.closeRedeemSheet() },
            onRedeem = { code -> viewModel.redeemCode(code) }
        )
    }

    if (uiState.isPrivacySheetOpen) {
        PrivacyPolicySheet(
            onDismiss = { viewModel.closePrivacySheet() }
        )
    }

    if (uiState.isBatterySheetOpen) {
        BackgroundWorkSheet(
            onDismiss = { viewModel.closeBatterySheet() }
        )
    }

    if (uiState.isPrimingSheetOpen) {
        NotificationPrimingSheet(
            onDismiss = { viewModel.closePrimingSheet() },
            onAllowClicked = {
                viewModel.closePrimingSheet()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
    }

    if (uiState.isProSheetOpen) {
        ProPaymentSheet(
            bkashConfig = uiState.bkashConfig,
            isBkashSubmitting = uiState.isBkashSubmitting,
            paddleCheckoutUrl = uiState.paddleCheckoutUrl,
            onDismiss = { viewModel.closeProSheet() },
            onBkashSubmit = { trxId, sender -> viewModel.submitBkash(trxId, sender) }
        )
    }

    if (uiState.isUpdateSheetOpen && uiState.updateManifest != null) {
        UpdateSheet(
            manifest = uiState.updateManifest!!,
            isForced = uiState.isUpdateForced,
            status = uiState.updateStatus,
            onDismiss = { viewModel.closeUpdateSheet() },
            onDownloadAndInstall = { viewModel.downloadAndInstallUpdate() }
        )
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    email: String,
    initials: String,
    isGuest: Boolean,
    isPro: Boolean,
    role: String
) {
    val customColors = WishPlayThemeColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar cookie wrapper
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CookieShape(CookieShapeType.C12))
                .background(if (isPro) customColors.accent else MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (isGuest) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = if (isPro) customColors.onAccent else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isPro) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(customColors.accent)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CookieShape(CookieShapeType.C9))
                            .background(customColors.onAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = customColors.accent,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Text(
                        text = if (role == "admin") "Admin · Pro for life" else "Pro for life",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = customColors.onAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun GuestBanner(
    onSignInClick: () -> Unit
) {
    val customColors = WishPlayThemeColors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(customColors.surfaceContainer)
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Keep your list safe",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Sign in to back up your wishlist and sync it to any phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.onSurface)
                    .clickable { onSignInClick() }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4285F4),
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "Sign in with Google",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProUpsellCard(
    onGetProClick: () -> Unit
) {
    val customColors = WishPlayThemeColors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 12.dp, bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(customColors.accent)
            .clickable { onGetProClick() }
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WishPlay Pro",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    ),
                    color = customColors.onAccent
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CookieShape(CookieShapeType.C9))
                        .background(customColors.onAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = customColors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = "Unlimited list, price-drop alerts, themes and widgets. One payment, yours for life.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = customColors.onAccent
            )
        }
    }
}

@Composable
private fun LookAndFeelSection(
    selectedTheme: String,
    onSelectTheme: (String) -> Unit,
    selectedAccent: String,
    onSelectAccent: (String) -> Unit,
    isPro: Boolean
) {
    val customColors = WishPlayThemeColors

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Look and feel",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Segmented theme control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(customColors.surfaceContainer)
                .padding(4.dp)
        ) {
            listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (key, label) ->
                val isSelected = selectedTheme == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { onSelectTheme(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Accent color row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Accent color",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!isPro) {
                LockPillTag()
            }
        }

        // Swatches
        val accents = listOf(
            "gold" to Color(0xFFFFB938),
            "violet" to Color(0xFFB9A8FF),
            "teal" to Color(0xFF6ED6C4),
            "coral" to Color(0xFFFF9E8A)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            accents.forEach { (key, color) ->
                val isSelected = selectedAccent == key
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onSelectAccent(key) }
                        .then(
                            if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface).let {
                                Modifier.border(it, CircleShape)
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF16132E),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetPreviewSection(
    nextDropGame: Game?,
    isPro: Boolean
) {
    val customColors = WishPlayThemeColors

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Home screen widget",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!isPro) {
                LockPillTag()
            }
        }

        // Widget Preview Card matching user's real next drop game
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(customColors.surfaceContainer)
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (nextDropGame != null) {
                    val gameColor = try {
                        Color(android.graphics.Color.parseColor(nextDropGame.hueHex))
                    } catch (_: Exception) {
                        Color(0xFF1F7A6E)
                    }
                    val days = nextDropGame.daysUntilRelease()
                    val whenLabel = when {
                        days == null -> "Date TBA"
                        days == 0 -> "Out today"
                        days == 1 -> "Out tomorrow"
                        days > 1 -> "Out in $days days"
                        else -> "Out now"
                    }

                    GameLogo(
                        title = nextDropGame.title,
                        hue = gameColor,
                        shapeType = CookieShapeType.fromKey(nextDropGame.shapeKey),
                        size = 48.dp,
                        imageUrl = nextDropGame.logoUrl ?: nextDropGame.coverUrl
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nextDropGame.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = whenLabel,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = days?.toString() ?: "—",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 38.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CookieShape(CookieShapeType.C9))
                            .background(customColors.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = customColors.onAccent
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Next drop preview",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Add games to track releases",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "—",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 38.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertsAndSharingSection(
    notificationsAllowed: Boolean,
    onNotificationsClick: () -> Unit,
    onBackgroundWorkClick: () -> Unit,
    onShareListClick: () -> Unit
) {
    val customColors = WishPlayThemeColors

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Alerts and sharing",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(customColors.surfaceContainer)
        ) {
            ProfileListItem(
                icon = if (notificationsAllowed) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                iconTint = if (notificationsAllowed) customColors.success else customColors.error,
                iconBg = if (notificationsAllowed) customColors.successContainer else customColors.errorContainer,
                title = "Notifications",
                subtitle = if (notificationsAllowed) "Allowed for all four channels" else "Blocked in Android settings",
                onClick = onNotificationsClick
            )

            ProfileDivider()

            ProfileListItem(
                icon = Icons.Default.BatteryChargingFull,
                title = "Background work",
                subtitle = "Keep the daily check running",
                onClick = onBackgroundWorkClick
            )

            ProfileDivider()

            ProfileListItem(
                icon = Icons.Default.Share,
                title = "Share your wishlist",
                subtitle = "A read-only link. Nobody can change your list.",
                onClick = onShareListClick
            )
        }
    }
}

@Composable
private fun AccountSection(
    isGuest: Boolean,
    isPro: Boolean,
    syncState: SyncState,
    lastSyncedTime: Long,
    onSyncClick: () -> Unit,
    onRedeemClick: () -> Unit,
    onCheckUpdateClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val customColors = WishPlayThemeColors

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Account",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(customColors.surfaceContainer)
        ) {
            // Cloud Sync item
            if (isGuest) {
                ProfileListItem(
                    icon = Icons.Default.PhoneAndroid,
                    title = "This phone only",
                    subtitle = "Your list is not backed up and is lost if you uninstall",
                    showChevron = false
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSyncClick() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(customColors.accent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (syncState == SyncState.SYNCING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = customColors.accent,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = customColors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (syncState == SyncState.SYNCING) "Syncing..." else "Synced",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Your list is backed up to your Google account",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(customColors.accent.copy(alpha = 0.15f))
                            .clickable { onSyncClick() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Sync now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = customColors.accent
                        )
                    }
                }
            }

            ProfileDivider()

            if (isPro) {
                ProfileListItem(
                    icon = Icons.Default.Sync,
                    title = "Restore purchase",
                    subtitle = "Pull Pro back after a reinstall",
                    onClick = onSyncClick
                )
            } else {
                ProfileListItem(
                    icon = Icons.Default.Redeem,
                    title = "Redeem a code",
                    subtitle = "Giveaway or discount codes",
                    onClick = onRedeemClick
                )
            }

            ProfileDivider()

            ProfileListItem(
                icon = Icons.Default.Download,
                title = "Check for updates",
                subtitle = "Version ${com.nuvetrix.wishplay.BuildConfig.VERSION_NAME} · Check now",
                onClick = onCheckUpdateClick
            )

            ProfileDivider()

            ProfileListItem(
                icon = Icons.Default.Security,
                title = "Privacy policy",
                subtitle = "What we store and what we never collect",
                onClick = onPrivacyClick
            )

            ProfileDivider()

            ProfileListItem(
                icon = Icons.Default.Info,
                title = "About",
                subtitle = "WishPlay is made by Nuvetrix",
                showChevron = false
            )

            if (!isGuest) {
                ProfileDivider()

                ProfileListItem(
                    icon = Icons.Default.ExitToApp,
                    title = "Sign out",
                    subtitle = "Switch to guest mode",
                    onClick = onSignOutClick
                )
            }
        }
    }
}

@Composable
private fun DangerZoneSection(
    onDeleteAccountClick: () -> Unit
) {
    val customColors = WishPlayThemeColors

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Danger zone",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(BorderStroke(1.5.dp, customColors.outlineVariant), RoundedCornerShape(24.dp))
                .clickable { onDeleteAccountClick() }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(customColors.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = customColors.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Delete your account",
                        fontWeight = FontWeight.Bold,
                        color = customColors.error
                    )
                    Text(
                        text = "Removes your list and your Pro record from our servers. Cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    iconBg: Color = MaterialTheme.colorScheme.surfaceVariant,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ProfileDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(WishPlayThemeColors.surfaceContainerHigh)
    )
}

@Composable
private fun LockPillTag() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "PRO",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
