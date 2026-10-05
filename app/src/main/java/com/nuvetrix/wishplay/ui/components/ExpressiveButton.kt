package com.nuvetrix.wishplay.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvetrix.wishplay.ui.theme.WishPlayMotion
import com.nuvetrix.wishplay.ui.theme.WishPlayThemeColors

enum class ButtonStyle {
    FILLED,
    GOLD,
    TONAL,
    OUTLINED,
    TEXT,
    DANGER
}

@Composable
fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.FILLED,
    isLarge: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    text: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val defaultRadius = if (isLarge) 30.dp else 24.dp
    val pressedRadius = if (isLarge) 16.dp else 12.dp

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) pressedRadius else defaultRadius,
        animationSpec = WishPlayMotion.expressiveSpring(),
        label = "btn_corner_radius"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = WishPlayMotion.expressiveSpring(),
        label = "btn_scale"
    )

    val customColors = WishPlayThemeColors

    val (containerColor, contentColor, border) = when (style) {
        ButtonStyle.FILLED -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            null
        )
        ButtonStyle.GOLD -> Triple(
            customColors.accent,
            customColors.onAccent,
            null
        )
        ButtonStyle.TONAL -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            null
        )
        ButtonStyle.OUTLINED -> Triple(
            Color.Transparent,
            MaterialTheme.colorScheme.onSurface,
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        )
        ButtonStyle.TEXT -> Triple(
            Color.Transparent,
            MaterialTheme.colorScheme.primary,
            null
        )
        ButtonStyle.DANGER -> Triple(
            Color.Transparent,
            MaterialTheme.colorScheme.error,
            BorderStroke(1.dp, customColors.outlineVariant)
        )
    }

    val height = if (isLarge) 60.dp else 48.dp
    val horizontalPadding = if (isLarge) 28.dp else 22.dp

    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.38f),
            disabledContentColor = contentColor.copy(alpha = 0.38f)
        ),
        border = border,
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 0.dp),
        interactionSource = interactionSource
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = if (isLarge) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.labelLarge
            )
        }
    }
}
