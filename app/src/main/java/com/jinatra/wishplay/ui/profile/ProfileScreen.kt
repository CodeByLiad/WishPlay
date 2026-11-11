package com.jinatra.wishplay.ui.profile

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
import androidx.compose.material3.SegmentedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
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
import com.jinatra.wishplay.R
import com.jinatra.wishplay.domain.model.Game
import com.jinatra.wishplay.domain.model.SyncState
import com.jinatra.wishplay.ui.alerts.sheets.BackgroundWorkSheet
import com.jinatra.wishplay.ui.alerts.sheets.NotificationPrimingSheet
import com.jinatra.wishplay.ui.components.ButtonStyle
import com.jinatra.wishplay.ui.components.CookieShape
import com.jinatra.wishplay.ui.components.CookieShapeType
import com.jinatra.wishplay.ui.components.ExpressiveButton
import com.jinatra.wishplay.ui.components.GameLogo
import com.jinatra.wishplay.ui.theme.WishPlayThemeColors

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineLarge)
            // Header / Tabhead
            ProfileHeader(
                name = uiState.user.name,
                email = uiState.user.email,
                initials = uiState.user.initials,
                isGuest = uiState.user.isGuest,
            )


            // Look and Feel Section
            LookAndFeelSection(
                selectedTheme = uiState.themeMode,
                onSelectTheme = { viewModel.setTheme(it) },
                selectedAccent = uiState.accentColor,
                onSelectAccent = viewModel::setAccent
            )

            WishlistBackupSection(viewModel)

            // Guest Banner (if guest)
            if (uiState.user.isGuest) {
                GuestBanner(
                    onSignInClick = { viewModel.openSignInSheet() }
                )
            }


            // Home Screen Widget Section
            WidgetPreviewSection(
                nextDropGame = uiState.nextDropGame,
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
                        putExtra(Intent.EXTRA_TEXT, "My WishPlay wishlist\n\n" + uiState.wishlistGames.joinToString("\n") { it.title })
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share your wishlist"))
                }
            )

            // Account Section
            AccountSection(
                isGuest = uiState.user.isGuest,
                syncState = uiState.syncState,
                lastSyncedTime = uiState.lastSyncedTime,
                onSyncClick = { viewModel.triggerSync() },
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

            DonationSection()
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
) {
    val customColors = WishPlayThemeColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar cookie wrapper
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CookieShape(CookieShapeType.C12))
                .background(MaterialTheme.colorScheme.secondaryContainer),
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
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )


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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LookAndFeelSection(selectedTheme: String, onSelectTheme: (String) -> Unit,
    selectedAccent: String, onSelectAccent: (String) -> Unit) {
    androidx.compose.material3.Card(shape = RoundedCornerShape(28.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Look and feel", style = MaterialTheme.typography.titleLarge)
            Text("Make WishPlay yours", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val modes = listOf("light" to "Light", "dark" to "Dark", "system" to "System")
            androidx.compose.material3.SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, (key, label) ->
                    SegmentedButton(selected = selectedTheme == key,
                        onClick = { onSelectTheme(key) },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index, modes.size)) { Text(label) }
                }
            }
            Text("Accent color", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.jinatra.wishplay.ui.theme.AccentColor.entries.forEach { accent ->
                    val key = accent.name.lowercase()
                    val selected = selectedAccent.equals(key, true)
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                        .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                        .semantics { contentDescription = "${accent.label} accent"; this.selected = selected }
                        .clickable { onSelectAccent(key) }.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(36.dp).clip(if (selected) CookieShape(CookieShapeType.C12) else CircleShape)
                            .background(accent.color), contentAlignment = Alignment.Center) {
                            if (selected) Icon(Icons.Default.Check, null, tint = accent.onColor, modifier = Modifier.size(20.dp))
                        }
                        Text(accent.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetPreviewSection(nextDropGame: Game?) {
    val context = LocalContext.current
    androidx.compose.material3.Card(shape = RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        ProfileListItem(icon = Icons.Default.PhoneAndroid, title = "Home screen widget",
            subtitle = nextDropGame?.let { "Next up: ${it.title}" } ?: "Keep your next release on your home screen",
            onClick = {
                val manager = android.appwidget.AppWidgetManager.getInstance(context)
                if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(
                    android.content.ComponentName(context, com.jinatra.wishplay.widget.NextDropWidgetReceiver::class.java), null, null)
                else android.widget.Toast.makeText(context, "Touch and hold your home screen, then choose Widgets and WishPlay.", android.widget.Toast.LENGTH_LONG).show()
            })
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
    syncState: SyncState,
    lastSyncedTime: Long,
    onSyncClick: () -> Unit,
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



            ProfileDivider()

            ProfileListItem(
                icon = Icons.Default.Download,
                title = "Check for updates",
                subtitle = "Version ${com.jinatra.wishplay.BuildConfig.VERSION_NAME} · Check now",
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
                subtitle = "WishPlay is made by Jinatra",
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
                        text = "Removes your wishlist and profile from our servers. Cannot be undone.",
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
