package com.jinatra.wishplay.ui.components

import androidx.compose.material3.toShape
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@androidx.compose.runtime.Composable
fun CookieShape(type: CookieShapeType): Shape {
    return when (type) {
        CookieShapeType.C4 -> androidx.compose.material3.MaterialShapes.Cookie4Sided
        CookieShapeType.C6 -> androidx.compose.material3.MaterialShapes.Cookie6Sided
        CookieShapeType.C9 -> androidx.compose.material3.MaterialShapes.Cookie9Sided
        CookieShapeType.C12 -> androidx.compose.material3.MaterialShapes.Cookie12Sided
        CookieShapeType.CIRCLE -> androidx.compose.material3.MaterialShapes.Circle
        CookieShapeType.SQUIRCLE -> androidx.compose.material3.MaterialShapes.Square
    }.toShape()
}
