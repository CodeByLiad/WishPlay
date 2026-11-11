package com.jinatra.wishplay.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
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

/** Every color role follows the selected palette, including Material surface containers. */
fun wishPlayColorScheme(dark: Boolean, accent: AccentColor): androidx.compose.material3.ColorScheme {
    val base = if (dark) DarkColorScheme else LightColorScheme
    val ground = if (dark) Color(0xFF101211) else Color(0xFFFFFDF9)
    val ink = if (dark) Color(0xFFF4F1EB) else Color(0xFF1B1D1B)
    val primary = if (dark) accent.color else when (accent) {
        AccentColor.GOLD -> Color(0xFF765600)
        AccentColor.VIOLET -> Color(0xFF6043A7)
        AccentColor.TEAL -> Color(0xFF006B5A)
        AccentColor.CORAL -> Color(0xFF9A432D)
    }
    fun surface(amount: Float) = lerp(ground, accent.color, amount)
    return base.copy(primary = primary, onPrimary = if (dark) accent.onColor else Color.White,
        primaryContainer = accent.color, onPrimaryContainer = accent.onColor,
        secondary = primary, onSecondary = if (dark) accent.onColor else Color.White,
        secondaryContainer = surface(if (dark) .24f else .26f), onSecondaryContainer = ink,
        tertiary = primary, onTertiary = if (dark) accent.onColor else Color.White,
        tertiaryContainer = surface(.18f), onTertiaryContainer = ink,
        background = surface(.065f), onBackground = ink,
        surface = surface(.065f), onSurface = ink,
        surfaceDim = surface(if (dark) .025f else .16f), surfaceBright = surface(if (dark) .18f else .01f),
        surfaceContainerLowest = ground, surfaceContainerLow = surface(.13f),
        surfaceContainer = surface(.16f), surfaceContainerHigh = surface(.21f), surfaceContainerHighest = surface(.26f),
        surfaceVariant = surface(.16f), onSurfaceVariant = if (dark) surface(.90f) else lerp(ink, primary, .20f),
        outline = if (dark) surface(.55f) else lerp(ink, ground, .48f),
        outlineVariant = surface(if (dark) .30f else .36f), surfaceTint = primary,
        inverseSurface = ink, inverseOnSurface = ground, inversePrimary = if (dark) accent.onColor else accent.color)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WishPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: AccentColor = AccentColor.GOLD,
    content: @Composable () -> Unit
) {
    val colorScheme = wishPlayColorScheme(darkTheme, accentColor)

    val customColors = WishPlayCustomColors(
        pageBackground = colorScheme.background,
        surfaceContainer = colorScheme.surfaceContainer,
        surfaceContainerHigh = colorScheme.surfaceContainerHigh,
        surfaceContainerHighest = colorScheme.surfaceContainerHighest,
        outlineVariant = colorScheme.outlineVariant,
        success = if (darkTheme) SuccessGreenDark else SuccessGreenLight,
        successContainer = if (darkTheme) SuccessGreenContainerDark else SuccessGreenContainerLight,
        accent = accentColor.color,
        onAccent = accentColor.onColor,
        track = colorScheme.surfaceContainerHighest,
        scrim = if (darkTheme) ScrimDark else ScrimLight,
        error = if (darkTheme) ErrorDark else ErrorLight,
        errorContainer = if (darkTheme) ErrorContainerDark else ErrorContainerLight
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var windowContext = view.context
            while (windowContext is android.content.ContextWrapper && windowContext !is Activity) {
                windowContext = windowContext.baseContext
            }
            val window = (windowContext as? Activity)?.window
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
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(36.dp)),
            typography = WishPlayTypography,
            content = content
        )
    }
}
