package com.nuvetrix.wishplay.ui.admin

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvetrix.wishplay.domain.model.AdminBkashItem
import com.nuvetrix.wishplay.domain.model.AdminPromoCode
import com.nuvetrix.wishplay.ui.theme.WishPlayThemeColors

@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val customColors = WishPlayThemeColors
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Enforce screenshot blocking on Admin Panel (PRD line 286)
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    LaunchedEffect(state.snackMessage) {
        state.snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Admin panel",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── 1. Stats Row ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = "${state.stats.activeCodes}",
                    label = "Active codes",
                    isHighlighted = false
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = "${state.stats.redeemed}",
                    label = "Redeemed",
                    isHighlighted = false
                )
                StatCard(
                    modifier = Modifier.weight(1.2f),
                    value = "${state.stats.pendingBkashCount}",
                    label = "bKash to check",
                    isHighlighted = true
                )
            }

            // ── 2. bKash payments to check ────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "bKash payments to check",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (state.pendingBkash.isEmpty()) {
                    Text(
                        text = "All caught up. New bKash payments will show here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(customColors.surfaceContainer),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        state.pendingBkash.forEach { item ->
                            BkashPendingRow(
                                item = item,
                                onApprove = { viewModel.approveBkash(item) },
                                onReject = { viewModel.rejectBkash(item) }
                            )
                        }
                    }
                }
            }

            // ── 3. Create a code ──────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Create a code",
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
                        .background(customColors.surfaceContainer)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Code type segmented toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(customColors.surfaceContainerHigh)
                                .padding(4.dp)
                        ) {
                            listOf("free" to "Free Pro", "pct" to "Discount").forEach { (type, label) ->
                                val isSelected = state.selectedCodeType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                        .clickable { viewModel.setCodeType(type) },
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

                        // Discount percentage (if discount type selected)
                        if (state.selectedCodeType == "pct") {
                            OutlinedTextField(
                                value = state.discountPct.toString(),
                                onValueChange = { str ->
                                    str.toIntOrNull()?.let { viewModel.setDiscountPct(it) }
                                },
                                label = { Text("Discount (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = customColors.accent,
                                    focusedLabelColor = customColors.accent
                                )
                            )
                        }

                        // Max uses and Expires row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = state.maxUses.toString(),
                                onValueChange = { str ->
                                    str.toIntOrNull()?.let { viewModel.setMaxUses(it) }
                                },
                                label = { Text("Max uses") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = customColors.accent,
                                    focusedLabelColor = customColors.accent
                                )
                            )

                            OutlinedTextField(
                                value = state.expiresAt,
                                onValueChange = { viewModel.setExpiresAt(it) },
                                label = { Text("Expires") },
                                modifier = Modifier.weight(1.3f),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = customColors.accent,
                                    focusedLabelColor = customColors.accent
                                )
                            )
                        }

                        // Create code button
                        Button(
                            onClick = { viewModel.createCode() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = customColors.accent,
                                contentColor = customColors.onAccent
                            )
                        ) {
                            Text(
                                text = "+ Create code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // ── 4. Your codes ─────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Your codes",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Search field
                OutlinedTextField(
                    value = state.codeSearchQuery,
                    onValueChange = { viewModel.setCodeSearchQuery(it) },
                    placeholder = { Text("Find a code") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (state.codeSearchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setCodeSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = customColors.accent,
                        unfocusedContainerColor = customColors.surfaceContainer,
                        focusedContainerColor = customColors.surfaceContainer
                    )
                )

                // Code List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(customColors.surfaceContainer),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    state.filteredCodes.forEach { codeItem ->
                        PromoCodeRow(
                            item = codeItem,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Promo Code", codeItem.code)
                                clipboard.setPrimaryClip(clip)
                            },
                            onToggle = { viewModel.toggleCode(codeItem.code, codeItem.active) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    isHighlighted: Boolean
) {
    val customColors = WishPlayThemeColors
    val bg = if (isHighlighted) customColors.accent else customColors.surfaceContainer
    val contentColor = if (isHighlighted) customColors.onAccent else MaterialTheme.colorScheme.onSurface
    val labelColor = if (isHighlighted) customColors.onAccent.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = labelColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BkashPendingRow(
    item: AdminBkashItem,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val customColors = WishPlayThemeColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = item.trxId,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "From ${item.senderNumber} · ${item.amountBdt} BDT · ${item.createdAt ?: "Recent"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onApprove,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = customColors.accent,
                    contentColor = customColors.onAccent
                )
            ) {
                Text(text = "Approve", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onReject,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, customColors.outlineVariant)
            ) {
                Text(text = "Reject", fontWeight = FontWeight.Bold, color = customColors.error)
            }
        }
    }
}

@Composable
private fun PromoCodeRow(
    item: AdminPromoCode,
    onCopy: () -> Unit,
    onToggle: () -> Unit
) {
    val customColors = WishPlayThemeColors
    val isUsedUp = item.uses >= item.maxUses
    val isLive = item.active && !isUsedUp
    val isFree = item.type == "lifetime"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isLive) 1f else 0.62f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isFree) customColors.accent else customColors.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFree) Icons.Default.CardGiftcard else Icons.Default.LocalOffer,
                contentDescription = null,
                tint = if (isFree) customColors.onAccent else customColors.accent,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.code,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            val desc = if (isFree) "Free Pro" else "${item.discountPct}% off"
            val statusDesc = if (isUsedUp) "used up" else "ends ${item.expiresAt ?: "2026-12-31"}"
            Text(
                text = "$desc, ${item.uses} of ${item.maxUses} used, $statusDesc",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy code",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        if (!isUsedUp) {
            Switch(
                checked = item.active,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = customColors.onAccent,
                    checkedTrackColor = customColors.accent
                )
            )
        }
    }
}
