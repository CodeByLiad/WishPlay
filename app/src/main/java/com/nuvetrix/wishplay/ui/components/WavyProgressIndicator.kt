package com.nuvetrix.wishplay.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val infiniteTransition = rememberInfiniteTransition(label = "wavy_progress")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = -16f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f
        val amp = 3.2f * density
        val wavelength = 16f * density
        val strokeWidth = 4f * density
        val px = (clampedProgress * w).coerceIn(10f * density, w - 14f * density)

        // Draw active animated wave clipped to progress
        val wavePath = Path()
        var first = true
        var x = -wavelength + waveOffset * density
        while (x <= w + wavelength) {
            val y = midY + amp * sin((x / wavelength) * (2f * PI.toFloat()))
            if (first) {
                wavePath.moveTo(x, y)
                first = false
            } else {
                wavePath.lineTo(x, y)
            }
            x += 2f * density
        }

        clipRect(left = 0f, top = 0f, right = px, bottom = h) {
            drawPath(
                path = wavePath,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Draw remaining track
        if (px + 9f * density < w - 3f * density) {
            drawLine(
                color = color.copy(alpha = 0.28f),
                start = Offset(px + 9f * density, midY),
                end = Offset(w - 3f * density, midY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Terminal dot at the end
        drawCircle(
            color = color,
            radius = 3f * density,
            center = Offset(w - 3f * density, midY)
        )
    }
}
