package com.carpet.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.carpet.app.ui.theme.Palette
import kotlin.math.cos
import kotlin.math.sin

private val Gold = Color(0xFFFFD66B)
private const val COUNT = 14

/**
 * Sparkles around a newly hatched friend. A burst plays once; rare and legendary
 * friends keep twinkling.
 */
@Composable
fun Sparkles(keepTwinkling: Boolean, modifier: Modifier = Modifier) {
    val burst = remember { Animatable(0f) }
    LaunchedEffect(Unit) { burst.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
    val t = rememberInfiniteTransition(label = "twinkle")
    val tw by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "tw")

    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height * 0.55f)
        val radius = size.minDimension * 0.5f
        for (i in 0 until COUNT) {
            val angle = Math.toRadians((i * 360.0 / COUNT) + i * 7.0).toFloat()
            val dist = radius * (0.55f + 0.45f * burst.value) * (if (i % 2 == 0) 1f else 0.8f)
            val p = center + Offset(cos(angle) * dist, sin(angle) * dist * 0.9f)
            val alpha = if (keepTwinkling) 0.5f + 0.5f * (if (i % 2 == 0) tw else 1f - tw) else 1f - burst.value
            val s = size.minDimension * 0.022f * (if (i % 3 == 0) 1.4f else 1f)
            val color = (if (i % 2 == 0) Palette.Fox else Gold).copy(alpha = alpha.coerceIn(0f, 1f))
            drawPath(star(p, s), color)
        }
    }
}

/** Four-point star centred at [p]. */
private fun star(p: Offset, s: Float) = Path().apply {
    moveTo(p.x, p.y - s * 2)
    lineTo(p.x + s * 0.5f, p.y - s * 0.5f)
    lineTo(p.x + s * 2, p.y)
    lineTo(p.x + s * 0.5f, p.y + s * 0.5f)
    lineTo(p.x, p.y + s * 2)
    lineTo(p.x - s * 0.5f, p.y + s * 0.5f)
    lineTo(p.x - s * 2, p.y)
    lineTo(p.x - s * 0.5f, p.y - s * 0.5f)
    close()
}
