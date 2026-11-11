package com.jinatra.wishplay.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jinatra.wishplay.R
import com.jinatra.wishplay.domain.model.Platform

/** Decorative marks accompany the readable platform label; they aren't read twice by TalkBack. */
@Composable
fun PlatformIcon(platform: Platform, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurface) {
    val iconModifier = modifier.size(24.dp)
    if (platform == Platform.PC) {
        Icon(Icons.Outlined.Computer, contentDescription = null, tint = tint, modifier = iconModifier)
        return
    }
    val resource = when (platform) {
        Platform.PS5, Platform.PS4 -> R.drawable.ic_platform_playstation
        Platform.XBOX_SERIES -> R.drawable.ic_platform_xbox
        Platform.SWITCH, Platform.SWITCH_2 -> R.drawable.ic_platform_nintendoswitch
        Platform.ANDROID -> R.drawable.ic_platform_android
        Platform.IOS -> R.drawable.ic_platform_apple
        Platform.PC -> error("PC uses the monitor icon")
    }
    Icon(painterResource(resource), contentDescription = null, tint = tint, modifier = iconModifier)
}
