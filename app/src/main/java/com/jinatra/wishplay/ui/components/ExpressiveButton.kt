@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.jinatra.wishplay.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jinatra.wishplay.ui.theme.WishPlayThemeColors

enum class ButtonStyle { FILLED, GOLD, TONAL, OUTLINED, TEXT, DANGER }

@Composable
fun ExpressiveButton(onClick: () -> Unit, modifier: Modifier = Modifier, style: ButtonStyle = ButtonStyle.FILLED,
    isLarge: Boolean = false, enabled: Boolean = true, leadingIcon: (@Composable () -> Unit)? = null, text: String) {
    val colors = MaterialTheme.colorScheme
    val content: @Composable RowScope.() -> Unit = {
        if (leadingIcon != null) { leadingIcon(); Spacer(Modifier.width(8.dp)) }
        Text(text, style = if (isLarge) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge)
    }
    val control = modifier.heightIn(min = if (isLarge) 60.dp else 48.dp)
    when (style) {
        ButtonStyle.TONAL -> FilledTonalButton(onClick, control, enabled = enabled, content = content)
        ButtonStyle.OUTLINED, ButtonStyle.DANGER -> OutlinedButton(onClick, control, enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (style == ButtonStyle.DANGER) colors.error else colors.primary),
            content = content)
        ButtonStyle.TEXT -> TextButton(onClick, control, enabled = enabled, content = content)
        else -> Button(onClick = onClick, modifier = control, enabled = enabled, shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (style == ButtonStyle.GOLD) WishPlayThemeColors.accent else colors.primary,
                contentColor = if (style == ButtonStyle.GOLD) WishPlayThemeColors.onAccent else colors.onPrimary), content = content)
    }
}
