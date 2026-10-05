package com.nuvetrix.wishplay.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvetrix.wishplay.data.repository.BkashConfig
import com.nuvetrix.wishplay.ui.components.ButtonStyle
import com.nuvetrix.wishplay.ui.components.CookieShape
import com.nuvetrix.wishplay.ui.components.CookieShapeType
import com.nuvetrix.wishplay.ui.components.ExpressiveButton
import com.nuvetrix.wishplay.ui.theme.WishPlayThemeColors

private val PRO_FEATURES = listOf(
    "Unlimited wishlist (free = 25 games)",
    "Price-drop alerts for PC games",
    "Accent color themes",
    "Home-screen widget",
    "Cloud sync across all your phones"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProPaymentSheet(
    bkashConfig: BkashConfig,
    isBkashSubmitting: Boolean,
    paddleCheckoutUrl: String,
    onDismiss: () -> Unit,
    onBkashSubmit: (trxId: String, senderNumber: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val customColors = WishPlayThemeColors
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Card/Paddle, 1 = bKash

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(customColors.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CookieShape(CookieShapeType.C9))
                        .background(customColors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = customColors.onAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Text(
                        text = "WishPlay Pro",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "One payment, yours for life",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Feature list
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PRO_FEATURES.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(customColors.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = customColors.accent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Price badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(customColors.surfaceContainer)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$2.99",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 36.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "USD",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "/ 349 BDT",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            // Payment method tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = customColors.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = customColors.accent
                    )
                },
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Card / PayPal",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "bKash (৳349)",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "payment_tab_content"
            ) { tab ->
                when (tab) {
                    0 -> PaddlePaymentTab(
                        checkoutUrl = paddleCheckoutUrl,
                        onOpenCheckout = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    )
                    1 -> BkashPaymentTab(
                        config = bkashConfig,
                        isSubmitting = isBkashSubmitting,
                        onSubmit = { trxId, sender -> onBkashSubmit(trxId, sender) }
                    )
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = "Maybe later",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PaddlePaymentTab(
    checkoutUrl: String,
    onOpenCheckout: (String) -> Unit
) {
    val customColors = WishPlayThemeColors

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Pay securely with card, PayPal, or Apple Pay via Paddle's hosted checkout.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ExpressiveButton(
            onClick = { onOpenCheckout(checkoutUrl) },
            text = "Pay \$2.99 with Card / PayPal",
            style = ButtonStyle.GOLD,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Secured by Paddle · No card details touch WishPlay",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BkashPaymentTab(
    config: BkashConfig,
    isSubmitting: Boolean,
    onSubmit: (trxId: String, senderNumber: String) -> Unit
) {
    val customColors = WishPlayThemeColors
    var trxId by remember { mutableStateOf("") }
    var senderNumber by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Step instructions
        Text(
            text = "How to pay with bKash",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        val steps = if (config.recipient.isNotBlank()) listOf(
            "Open bKash → Send Money",
            "Send ${config.amount_bdt} BDT to ${config.recipient}",
            "Reference: WishPlay Pro",
            "Paste the TrxID below"
        ) else listOf(
            "Open bKash → Send Money",
            "Send 349 BDT to our number (loading…)",
            "Reference: WishPlay Pro",
            "Paste the TrxID below"
        )

        steps.forEachIndexed { index, step ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(customColors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = customColors.onAccent
                    )
                }
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // TrxID field
        OutlinedTextField(
            value = trxId,
            onValueChange = { trxId = it.uppercase(); error = null },
            label = { Text("bKash TrxID") },
            placeholder = { Text("e.g. A1B2C3D4E5") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = customColors.accent,
                unfocusedBorderColor = customColors.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Sender number field
        OutlinedTextField(
            value = senderNumber,
            onValueChange = { senderNumber = it; error = null },
            label = { Text("Your bKash number") },
            placeholder = { Text("01XXXXXXXXX") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { submitBkash(trxId, senderNumber, { error = it }, onSubmit) }
            ),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = customColors.accent,
                unfocusedBorderColor = customColors.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Text(
                text = error!!,
                style = MaterialTheme.typography.bodySmall,
                color = customColors.error
            )
        }

        if (isSubmitting) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = customColors.accent, modifier = Modifier.size(28.dp))
            }
        } else {
            ExpressiveButton(
                onClick = { submitBkash(trxId, senderNumber, { error = it }, onSubmit) },
                text = "Submit TrxID",
                style = ButtonStyle.GOLD,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            text = "Your payment is reviewed manually within 24 hours. Pro activates as soon as it's confirmed.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun submitBkash(
    trxId: String,
    senderNumber: String,
    onError: (String) -> Unit,
    onSubmit: (String, String) -> Unit
) {
    val cleanTrx = trxId.trim()
    val cleanSender = senderNumber.trim()

    if (cleanTrx.length < 6) {
        onError("Please enter a valid TrxID (at least 6 characters).")
        return
    }
    if (!cleanSender.matches(Regex("^01[3-9]\\d{8}$"))) {
        onError("Enter a valid Bangladesh mobile number (e.g. 01712345678).")
        return
    }
    onSubmit(cleanTrx, cleanSender)
}
