package com.nuvetrix.wishplay.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class CookieShapeType(val lobes: Int, val amplitude: Float) {
    C4(4, 0.10f),
    C6(6, 0.07f),
    C9(9, 0.06f),
    C12(12, 0.04f),
    CIRCLE(1, 0.0f),
    SQUIRCLE(0, 0.0f);

    companion object {
        fun fromKey(key: String): CookieShapeType = when (key.lowercase()) {
            "c4" -> C4
            "c6" -> C6
            "c9" -> C9
            "c12" -> C12
            "circle" -> CIRCLE
            "sq", "squircle" -> SQUIRCLE
            else -> C9
        }
    }
}

class CookieShape(val type: CookieShapeType) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val width = size.width
        val height = size.height
        val minDim = min(width, height)

        if (type == CookieShapeType.SQUIRCLE) {
            val cornerRadius = minDim * 0.28f
            path.addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = width,
                    bottom = height,
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            )
            return Outline.Generic(path)
        }

        val centerX = width / 2f
        val centerY = height / 2f
        val baseRadius = minDim / 2f
        val amp = type.amplitude
        val n = type.lobes

        for (i in 0..360 step 3) {
            val theta = (i * PI / 180.0).toFloat()
            val r = baseRadius * (1f - amp + amp * cos(n * theta))
            val x = centerX + r * cos(theta)
            val y = centerY + r * sin(theta)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}
