package com.jinatra.wishplay.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jinatra.wishplay.ui.components.ButtonStyle
import com.jinatra.wishplay.ui.components.ExpressiveButton
import com.jinatra.wishplay.ui.theme.WishPlayThemeColors

const val BKASH_DONATION_NUMBER = "01978900129"

@Composable
fun DonationSection() {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(WishPlayThemeColors.surfaceContainer).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Support WishPlay", style = MaterialTheme.typography.headlineMedium)
        Text("Every feature is free. If you'd like to support development, you can donate any amount through bKash.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("bKash number", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer {
            Text(BKASH_DONATION_NUMBER, style = MaterialTheme.typography.headlineLarge)
        }
        ExpressiveButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("bKash donation number", BKASH_DONATION_NUMBER))
                copied = true
            },
            text = "Copy bKash number", style = ButtonStyle.TONAL,
            modifier = Modifier.fillMaxWidth()
        )
        if (copied) Text("Number copied", style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Text("Donations are optional and don't unlock extra features.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
