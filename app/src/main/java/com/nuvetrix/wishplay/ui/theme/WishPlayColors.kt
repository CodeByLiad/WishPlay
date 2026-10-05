package com.nuvetrix.wishplay.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class WishPlayCustomColors(
    val pageBackground: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val outlineVariant: Color,
    val success: Color,
    val successContainer: Color,
    val accent: Color,
    val onAccent: Color,
    val track: Color,
    val scrim: Color,
    val error: Color,
    val errorContainer: Color
)

val LocalWishPlayColors = staticCompositionLocalOf {
    WishPlayCustomColors(
        pageBackground = PageLight,
        surfaceContainer = SurfaceContainerLight,
        surfaceContainerHigh = SurfaceContainerHighLight,
        surfaceContainerHighest = SurfaceContainerHighestLight,
        outlineVariant = OutlineVariantLight,
        success = SuccessGreenLight,
        successContainer = SuccessGreenContainerLight,
        accent = AccentColor.GOLD.color,
        onAccent = AccentColor.GOLD.onColor,
        track = TrackLight,
        scrim = ScrimLight,
        error = ErrorLight,
        errorContainer = ErrorContainerLight
    )
}

val WishPlayThemeColors: WishPlayCustomColors
    @Composable
    @ReadOnlyComposable
    get() = LocalWishPlayColors.current
