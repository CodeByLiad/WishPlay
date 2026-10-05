package com.nuvetrix.wishplay.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = AccentColor.GOLD.color,
    onPrimaryContainer = AccentColor.GOLD.onColor,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = OnSecondaryLight,
    tertiary = TertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = SurfaceLight,
    onBackground = OnLight,
    surface = SurfaceLight,
    onSurface = OnLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = OnVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark, // Gold in dark mode per prototype
    onPrimary = OnPrimaryDark,
    primaryContainer = AccentColor.GOLD.color,
    onPrimaryContainer = AccentColor.GOLD.onColor,
    secondaryContainer = SecondaryDark,
    onSecondaryContainer = OnSecondaryDark,
    tertiary = TertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = SurfaceDark,
    onBackground = OnDark,
    surface = SurfaceDark,
    onSurface = OnDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = OnVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = ScrimDark
)

@Composable
fun WishPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: AccentColor = AccentColor.GOLD,
    content: @Composable () -> Unit
) {
    val baseColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    // Override primaryContainer/onPrimaryContainer with the chosen accent color
    val colorScheme = baseColorScheme.copy(
        primaryContainer = accentColor.color,
        onPrimaryContainer = accentColor.onColor
    )

    val customColors = WishPlayCustomColors(
        pageBackground = if (darkTheme) PageDark else PageLight,
        surfaceContainer = if (darkTheme) SurfaceContainerDark else SurfaceContainerLight,
        surfaceContainerHigh = if (darkTheme) SurfaceContainerHighDark else SurfaceContainerHighLight,
        surfaceContainerHighest = if (darkTheme) SurfaceContainerHighestDark else SurfaceContainerHighestLight,
        outlineVariant = if (darkTheme) OutlineVariantDark else OutlineVariantLight,
        success = if (darkTheme) SuccessGreenDark else SuccessGreenLight,
        successContainer = if (darkTheme) SuccessGreenContainerDark else SuccessGreenContainerLight,
        accent = accentColor.color,
        onAccent = accentColor.onColor,
        track = if (darkTheme) TrackDark else TrackLight,
        scrim = if (darkTheme) ScrimDark else ScrimLight,
        error = if (darkTheme) ErrorDark else ErrorLight,
        errorContainer = if (darkTheme) ErrorContainerDark else ErrorContainerLight
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalWishPlayColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WishPlayTypography,
            content = content
        )
    }
}
