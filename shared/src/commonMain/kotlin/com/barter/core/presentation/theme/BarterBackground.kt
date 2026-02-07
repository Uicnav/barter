package com.barter.core.presentation.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private val BgDark = Color(0xFF060F0D)
private val BgMid = Color(0xFF0A1E18)
private val BgTealCore = Color(0xFF0E3E30)
private val HexLineColor = Color(0xFF1AE6B4)

/**
 * Futuristic edge-to-edge background with radial gradient and hexagonal grid.
 * Draws behind [content] as a full-bleed layer.
 */
@Composable
fun BarterBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRadialGradient()
            drawHexGrid()
        }
        content()
    }
}

private fun DrawScope.drawRadialGradient() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val maxR = size.width.coerceAtLeast(size.height) * 0.85f

    // Full dark fill
    drawRect(BgDark)

    // Radial gradient: dark edge → mid → teal core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BgTealCore, BgMid, BgDark),
            center = Offset(cx, cy),
            radius = maxR,
        ),
        radius = maxR,
        center = Offset(cx, cy),
    )
}

private fun DrawScope.drawHexGrid() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val hexR = min(size.width, size.height) * 0.035f
    val h = hexR * sqrt(3f)
    val w = hexR * 2f
    val maxDist = min(size.width, size.height) * 0.7f

    val cols = (size.width / (w * 0.75f)).toInt() + 4
    val rows = (size.height / h).toInt() + 4

    for (row in -2..rows) {
        for (col in -2..cols) {
            val hx = col * w * 0.75f
            val hy = row * h + (col % 2) * h * 0.5f

            val dx = hx - cx
            val dy = hy - cy
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > maxDist) continue

            val fade = (1f - (dist / maxDist)).coerceIn(0f, 1f)
            val alpha = 0.07f * fade * fade
            if (alpha < 0.005f) continue

            val path = Path()
            for (i in 0..5) {
                val angle = ((60.0 * i - 30.0) * kotlin.math.PI / 180.0).toFloat()
                val px = hx + hexR * cos(angle)
                val py = hy + hexR * sin(angle)
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()

            drawPath(
                path = path,
                color = HexLineColor.copy(alpha = alpha),
                style = Stroke(width = 1f),
            )
        }
    }
}
