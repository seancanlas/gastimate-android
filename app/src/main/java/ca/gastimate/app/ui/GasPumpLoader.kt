package ca.gastimate.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.min

/**
 * Loading icon (NC-10): a gas pump with a hose; animated green dashes inside
 * the hose read as gas flowing from the pump to the nozzle. Drawn in a 1×1
 * logical space, so any square [modifier] size works. Theme colors by
 * default; override both on colored surfaces (e.g. filled buttons).
 */
@Composable
fun GasPumpLoader(
    modifier: Modifier = Modifier,
    contentDescription: String = "Loading",
    frameColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    gasColor: Color = MaterialTheme.colorScheme.primary,
) {
    val phase by rememberInfiniteTransition(label = "gas-flow")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
            label = "dash-phase",
        )
    val hose = remember { Path() }
    val dashIntervals = remember { floatArrayOf(1f, 1f) }
    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        val s = min(size.width, size.height)
        // Stroke floors: sub-pixel strokes vanish on mdpi screens.
        val minStroke = 1.dp.toPx()
        val frame = frameColor
        val gas = gasColor

        // Base.
        drawRoundRect(
            color = frame.copy(alpha = 0.25f),
            topLeft = Offset(0.04f * s, 0.82f * s),
            size = Size(0.50f * s, 0.10f * s),
            cornerRadius = CornerRadius(0.02f * s),
        )
        // Body: tinted fill + outline.
        drawRoundRect(
            color = gas.copy(alpha = 0.10f),
            topLeft = Offset(0.08f * s, 0.06f * s),
            size = Size(0.44f * s, 0.76f * s),
            cornerRadius = CornerRadius(0.06f * s),
        )
        drawRoundRect(
            color = frame,
            topLeft = Offset(0.08f * s, 0.06f * s),
            size = Size(0.44f * s, 0.76f * s),
            cornerRadius = CornerRadius(0.06f * s),
            style = Stroke((0.035f * s).coerceAtLeast(minStroke)),
        )
        // Screen + panel bar.
        drawRoundRect(
            color = gas.copy(alpha = 0.25f),
            topLeft = Offset(0.15f * s, 0.14f * s),
            size = Size(0.30f * s, 0.22f * s),
            cornerRadius = CornerRadius(0.03f * s),
        )
        drawRoundRect(
            color = frame,
            topLeft = Offset(0.15f * s, 0.14f * s),
            size = Size(0.30f * s, 0.22f * s),
            cornerRadius = CornerRadius(0.03f * s),
            style = Stroke((0.028f * s).coerceAtLeast(minStroke)),
        )
        drawRoundRect(
            color = frame.copy(alpha = 0.5f),
            topLeft = Offset(0.15f * s, 0.44f * s),
            size = Size(0.30f * s, 0.035f * s),
            cornerRadius = CornerRadius(0.017f * s),
        )

        // Hose: exits the body's right side, arcs right and down to the nozzle.
        // Path rebuilt per draw from the current canvas size.
        hose.rewind()
        hose.apply {
            moveTo(0.50f * s, 0.18f * s)
            cubicTo(0.86f * s, 0.18f * s, 0.92f * s, 0.58f * s, 0.70f * s, 0.66f * s)
        }
        drawPath(hose, frame, style = Stroke(width = (0.06f * s).coerceAtLeast(minStroke), cap = StrokeCap.Round))
        // Gas: dashes flowing toward the nozzle, one dash period per cycle.
        val dash = 0.055f * s
        dashIntervals[0] = dash
        dashIntervals[1] = dash
        drawPath(
            hose,
            gas,
            style = Stroke(
                width = (0.03f * s).coerceAtLeast(minStroke),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    dashIntervals,
                    phase = -phase * 2f * dash,
                ),
            ),
        )

        // Nozzle head at the hose tip + spout.
        drawRoundRect(
            color = frame,
            topLeft = Offset(0.66f * s, 0.60f * s),
            size = Size(0.14f * s, 0.18f * s),
            cornerRadius = CornerRadius(0.02f * s),
        )
        drawLine(
            color = frame,
            start = Offset(0.66f * s, 0.76f * s),
            end = Offset(0.59f * s, 0.84f * s),
            strokeWidth = (0.045f * s).coerceAtLeast(minStroke),
            cap = StrokeCap.Round,
        )
    }
}
