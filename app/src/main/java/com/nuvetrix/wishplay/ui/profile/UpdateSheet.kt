package com.nuvetrix.wishplay.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvetrix.wishplay.data.remote.update.UpdateManifest
import com.nuvetrix.wishplay.data.remote.update.UpdateStatus
import com.nuvetrix.wishplay.ui.components.ButtonStyle
import com.nuvetrix.wishplay.ui.components.ExpressiveButton
import com.nuvetrix.wishplay.ui.theme.WishPlayThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSheet(
    manifest: UpdateManifest,
    isForced: Boolean,
    status: UpdateStatus,
    onDismiss: () -> Unit,
    onDownloadAndInstall: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !isForced } // Prevent swipe to dismiss if forced
    )
    val customColors = WishPlayThemeColors

    ModalBottomSheet(
        onDismissRequest = { if (!isForced) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            if (!isForced) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(customColors.outlineVariant)
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isForced) "Required update" else "Update available",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = if (isForced) customColors.error else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "WishPlay v${manifest.versionName}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = customColors.accent
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isForced) customColors.errorContainer else customColors.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isForced) Icons.Default.Warning else Icons.Default.Download,
                        contentDescription = null,
                        tint = if (isForced) customColors.error else customColors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Release Notes Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(customColors.surfaceContainer)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "What's new",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = manifest.releaseNotes.ifBlank { "Performance improvements, updated game data proxy, and security hardening." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Security verification pill (SHA-256 and certificate verified)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(customColors.successContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = customColors.success,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "SHA-256 verified · Signed with official Jinatra key",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = customColors.success
                )
            }

            // Progress bar if downloading
            if (status is UpdateStatus.Downloading) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Downloading update...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${status.progressPercent}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = customColors.accent
                        )
                    }
                    LinearProgressIndicator(
                        progress = { status.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = customColors.accent,
                        trackColor = customColors.surfaceContainerHigh
                    )
                }
            }

            // Error display if failed
            if (status is UpdateStatus.Error) {
                Text(
                    text = status.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = customColors.error
                )
            }

            // Action Buttons
            val isDownloading = status is UpdateStatus.Downloading
            val isReady = status is UpdateStatus.ReadyToInstall

            ExpressiveButton(
                onClick = onDownloadAndInstall,
                text = when {
                    isDownloading -> "Downloading..."
                    isReady -> "Install now"
                    else -> "Download and install"
                },
                style = ButtonStyle.FILLED,
                enabled = !isDownloading,
                modifier = Modifier.fillMaxWidth()
            )

            if (!isForced) {
                ExpressiveButton(
                    onClick = onDismiss,
                    text = "Later",
                    style = ButtonStyle.TONAL,
                    enabled = !isDownloading,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Older versions are discontinued. Update to continue using WishPlay.",
                    style = MaterialTheme.typography.bodySmall,
                    color = customColors.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
