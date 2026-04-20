package com.ashutosh.mindfultennis.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Animated splash screen that draws the app icon (court + fern) path-by-path.
 *
 * Animation sequence:
 *  1. Court boundary rectangle traces around
 *  2. Net/center line sweeps across
 *  3. Fern stem grows from bottom to top
 *  4. Lower pair of fern leaves unfurl from the stem
 *  5. Upper pair of fern leaves unfurl from the stem
 *  6. Brief hold, then [onAnimationComplete] fires
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {},
) {
    val backgroundColor = Color(0xFF1B6B4D)

    val courtProgress = remember { Animatable(0f) }
    val netProgress = remember { Animatable(0f) }
    val stemProgress = remember { Animatable(0f) }
    val lowerLeavesProgress = remember { Animatable(0f) }
    val upperLeavesProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        courtProgress.animateTo(1f, tween(700, easing = EaseOut))
        netProgress.animateTo(1f, tween(400, easing = EaseInOut))
        stemProgress.animateTo(1f, tween(500, easing = EaseOut))
        lowerLeavesProgress.animateTo(1f, tween(400, easing = EaseOut))
        upperLeavesProgress.animateTo(1f, tween(400, easing = EaseOut))
        delay(500)
        onAnimationComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val s = size.width / 108f // scale from 108-viewport to canvas

            // Court boundary rectangle
            if (courtProgress.value > 0f) {
                val path = Path().apply {
                    moveTo(22f * s, 22f * s)
                    lineTo(86f * s, 22f * s)
                    lineTo(86f * s, 86f * s)
                    lineTo(22f * s, 86f * s)
                    lineTo(22f * s, 22f * s)
                }
                drawAnimatedPath(path, Color(0x66FFFFFF), 2f * s, courtProgress.value)
            }

            // Net / center line
            if (netProgress.value > 0f) {
                val path = Path().apply {
                    moveTo(14f * s, 54f * s)
                    lineTo(94f * s, 54f * s)
                }
                drawAnimatedPath(path, Color(0x99FFFFFF), 2f * s, netProgress.value)
            }

            // Fern stem (bottom → top)
            if (stemProgress.value > 0f) {
                val path = Path().apply {
                    moveTo(54f * s, 89f * s)
                    lineTo(54f * s, 34f * s)
                }
                drawAnimatedPath(path, Color.White, 3f * s, stemProgress.value, StrokeCap.Round)
            }

            // Lower fern leaves (pair)
            if (lowerLeavesProgress.value > 0f) {
                val left = Path().apply {
                    moveTo(54f * s, 74f * s)
                    cubicTo(44f * s, 74f * s, 39f * s, 66f * s, 39f * s, 66f * s)
                }
                val right = Path().apply {
                    moveTo(54f * s, 74f * s)
                    cubicTo(64f * s, 74f * s, 69f * s, 66f * s, 69f * s, 66f * s)
                }
                drawAnimatedPath(left, Color.White, 2f * s, lowerLeavesProgress.value, StrokeCap.Round)
                drawAnimatedPath(right, Color.White, 2f * s, lowerLeavesProgress.value, StrokeCap.Round)
            }

            // Upper fern leaves (pair)
            if (upperLeavesProgress.value > 0f) {
                val left = Path().apply {
                    moveTo(54f * s, 59f * s)
                    cubicTo(46f * s, 59f * s, 44f * s, 52f * s, 44f * s, 52f * s)
                }
                val right = Path().apply {
                    moveTo(54f * s, 59f * s)
                    cubicTo(62f * s, 59f * s, 64f * s, 52f * s, 64f * s, 52f * s)
                }
                drawAnimatedPath(left, Color.White, 2f * s, upperLeavesProgress.value, StrokeCap.Round)
                drawAnimatedPath(right, Color.White, 2f * s, upperLeavesProgress.value, StrokeCap.Round)
            }
        }
    }
}

/**
 * Draws a [path] progressively: at [progress] == 0 nothing is visible,
 * at 1 the full stroke is shown. Uses [PathMeasure.getSegment] for smooth trimming.
 */
private fun DrawScope.drawAnimatedPath(
    path: Path,
    color: Color,
    strokeWidth: Float,
    progress: Float,
    cap: StrokeCap = StrokeCap.Butt,
) {
    if (progress <= 0f) return
    if (progress >= 1f) {
        drawPath(path, color, style = Stroke(width = strokeWidth, cap = cap))
        return
    }
    val measure = PathMeasure().apply { setPath(path, false) }
    val trimmedPath = Path()
    measure.getSegment(0f, measure.length * progress, trimmedPath, startWithMoveTo = true)
    drawPath(trimmedPath, color, style = Stroke(width = strokeWidth, cap = cap))
}
