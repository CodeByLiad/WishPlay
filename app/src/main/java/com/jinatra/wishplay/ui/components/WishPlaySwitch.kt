package com.jinatra.wishplay.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

/** Standard Material switch: distinct thumb, track, and checked indicator. */
@Composable
fun WishPlaySwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors()) {
    Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled,
        colors = colors, thumbContent = if (checked) {
            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
        } else null)
}
