package com.nuvetrix.wishplay.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Tokens
val PageLight = Color(0xFFE6E2F0)
val FrameLight = Color(0xFF16132E)
val SurfaceLight = Color(0xFFFBF9FF)
val SurfaceContainerLight = Color(0xFFF1EEF8)
val SurfaceContainerHighLight = Color(0xFFE8E4F2)
val SurfaceContainerHighestLight = Color(0xFFDDD8EC)
val OnLight = Color(0xFF16132E)
val OnVariantLight = Color(0xFF4A4663)
val OutlineLight = Color(0xFF77728F)
val OutlineVariantLight = Color(0xFFCAC5DA)
val PrimaryLight = Color(0xFF2E2766)
val OnPrimaryLight = Color(0xFFFFFFFF)
val SecondaryLight = Color(0xFFE4DFFF)
val OnSecondaryLight = Color(0xFF231A5C)
val TertiaryLight = Color(0xFF5B3FE0)
val TertiaryContainerLight = Color(0xFFE6DEFF)
val OnTertiaryContainerLight = Color(0xFF21105E)
val ErrorLight = Color(0xFFB3261E)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)
val SuccessGreenLight = Color(0xFF1E6E47)
val SuccessGreenContainerLight = Color(0xFFCDEFD9)
val TrackLight = Color(0xFFD9D4E8)
val ScrimLight = Color(0x7A16132E)

// Dark Theme Tokens
val PageDark = Color(0xFF0B0A17)
val FrameDark = Color(0xFF2F2C4B)
val SurfaceDark = Color(0xFF131124)
val SurfaceContainerDark = Color(0xFF1C1A33)
val SurfaceContainerHighDark = Color(0xFF25223F)
val SurfaceContainerHighestDark = Color(0xFF302D4C)
val OnDark = Color(0xFFEDEAF7)
val OnVariantDark = Color(0xFFC3BED7)
val OutlineDark = Color(0xFF938EAB)
val OutlineVariantDark = Color(0xFF46425F)
val PrimaryDark = Color(0xFFFFB938) // Gold primary in dark mode
val OnPrimaryDark = Color(0xFF2B1C00)
val SecondaryDark = Color(0xFF342D6B)
val OnSecondaryDark = Color(0xFFE4DFFF)
val TertiaryDark = Color(0xFFB9A8FF)
val TertiaryContainerDark = Color(0xFF3C2C8F)
val OnTertiaryContainerDark = Color(0xFFE6DEFF)
val ErrorDark = Color(0xFFF2B8B5)
val ErrorContainerDark = Color(0xFF601410)
val OnErrorContainerDark = Color(0xFFF9DEDC)
val SuccessGreenDark = Color(0xFF7ED3A4)
val SuccessGreenContainerDark = Color(0xFF0F3D27)
val TrackDark = Color(0xFF3A3657)
val ScrimDark = Color(0x99000000)

// Accent Colors (Pro & Free default Gold)
enum class AccentColor(
    val color: Color,
    val onColor: Color,
    val label: String
) {
    GOLD(Color(0xFFFFB938), Color(0xFF2B1C00), "Gold"),
    VIOLET(Color(0xFFB9A8FF), Color(0xFF21105E), "Violet"),
    TEAL(Color(0xFF6ED6C4), Color(0xFF00382F), "Teal"),
    CORAL(Color(0xFFFF9E8A), Color(0xFF4A0E00), "Coral");

    companion object {
        fun fromKey(key: String): AccentColor =
            entries.find { it.name.equals(key, ignoreCase = true) } ?: GOLD
    }
}
