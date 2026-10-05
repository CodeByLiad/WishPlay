package com.nuvetrix.wishplay.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nuvetrix.wishplay.ui.theme.WishPlayMotion
import com.nuvetrix.wishplay.ui.theme.WishPlayThemeColors

enum class GroupPosition {
    ONLY,
    FIRST,
    MIDDLE,
    LAST
}

@Composable
fun GroupedItemContainer(
    position: GroupPosition,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = WishPlayThemeColors.surfaceContainer,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressedRadius = 20.dp
    val normalOuterRadius = 26.dp
    val normalInnerRadius = 8.dp

    val topStartRadius by animateDpAsState(
        targetValue = when {
            isPressed -> pressedRadius
            position == GroupPosition.FIRST || position == GroupPosition.ONLY -> normalOuterRadius
            else -> normalInnerRadius
        },
        animationSpec = WishPlayMotion.expressiveSpring(),
        label = "ts_radius"
    )

    val topEndRadius by animateDpAsState(
        targetValue = when {
            isPressed -> pressedRadius
            position == GroupPosition.FIRST || position == GroupPosition.ONLY -> normalOuterRadius
            else -> normalInnerRadius
        },
        animationSpec = WishPlayMotion.expressiveSpring(),
        label = "te_radius"
    )

    val bottomStartRadius by animateDpAsState(
        targetValue = when {
            isPressed -> pressedRadius
            position == GroupPosition.LAST || position == GroupPosition.ONLY -> normalOuterRadius
            else -> normalInnerRadius
        },
        animationSpec = WishPlayMotion.expressiveSpring(),
        label = "bs_radius"
    )

    val bottomEndRadius by animateDpAsState(
        targetValue = when {
            isPressed -> pressedRadius
            position == GroupPosition.LAST || position == GroupPosition.ONLY -> normalOuterRadius
            else -> normalInnerRadius
        },
        animationSpec = WishPlayMotion.expressiveSpring(),
        label = "be_radius"
    )

    val shape = RoundedCornerShape(
        topStart = topStartRadius,
        topEnd = topEndRadius,
        bottomStart = bottomStartRadius,
        bottomEnd = bottomEndRadius
    )

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isPressed) WishPlayThemeColors.surfaceContainerHigh else backgroundColor)
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        content()
    }
}
