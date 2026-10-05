package com.nuvetrix.wishplay.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object WishPlayMotion {
    // Easing from prototype: cubic-bezier(.2, 1.35, .3, 1)
    val ExpressiveSpringEasing = CubicBezierEasing(0.2f, 1.35f, 0.3f, 1.0f)

    fun <T> expressiveSpring(
        dampingRatio: Float = 0.55f,
        stiffness: Float = Spring.StiffnessMediumLow
    ): AnimationSpec<T> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )
}
