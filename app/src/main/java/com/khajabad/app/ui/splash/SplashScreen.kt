package com.khajabad.app.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khajabad.app.ui.theme.Khaja
import com.khajabad.app.ui.theme.KhIcons
import com.khajabad.app.ui.theme.KhIcon
import com.khajabad.app.ui.theme.Motion
import com.khajabad.app.ui.theme.Outfit
import com.khajabad.app.ui.theme.Type
import kotlinx.coroutines.delay

/**
 * Splash: a mihrab arch draws itself, the wordmark arrives letter-by-letter in
 * discrete "stop motion" hops, then a mint sheen sweeps past the mark.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val colors = Khaja.colors
    var started by remember { mutableStateOf(false) }
    var sweep by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        started = true
        delay(250)
        sweep = true
        delay(2250)
        onFinished()
    }

    val draw by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "arch",
    )
    val markScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.86f,
        animationSpec = Motion.gentle(),
        label = "mark",
    )
    val sheen by animateFloatAsState(
        targetValue = if (sweep) 1f else 0f,
        animationSpec = tween(1400, delayMillis = 120, easing = LinearEasing),
        label = "sheen",
    )

    val embers = remember { List(16) { Triple(it, (it * 37 % 100) / 100f, (it * 61 % 100) / 100f) } }
    val drift by rememberInfiniteTransition(label = "embers").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.paper),
        contentAlignment = Alignment.Center,
    ) {
        // Radial mint glow behind the mark.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.mint.copy(alpha = 0.34f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.42f),
                    radius = size.minDimension * 0.62f,
                ),
                radius = size.minDimension * 0.62f,
                center = Offset(size.width * 0.5f, size.height * 0.42f),
            )
            // slow rising dust
            embers.forEach { (seed, fx, fy) ->
                val px = size.width * (0.08f + fx * 0.84f)
                val phase = (fy + drift) % 1f
                val py = size.height * (0.95f - phase * 0.9f)
                val alpha = (1f - kotlin.math.abs(phase - 0.5f) * 2f).coerceIn(0f, 1f) * 0.5f
                drawCircle(
                    color = colors.mintDeep.copy(alpha = alpha * 0.7f),
                    radius = 1.4f + (seed % 3) * 0.9f,
                    center = Offset(px, py),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { scaleX = markScale; scaleY = markScale },
        ) {
            Box(
                Modifier.size(132.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val arch = Path().apply {
                        moveTo(w * 0.22f, w * 0.9f)
                        lineTo(w * 0.22f, w * 0.46f)
                        cubicTo(
                            w * 0.22f, w * 0.22f,
                            w * 0.38f, w * 0.08f,
                            w * 0.5f, w * 0.08f,
                        )
                        cubicTo(
                            w * 0.62f, w * 0.08f,
                            w * 0.78f, w * 0.22f,
                            w * 0.78f, w * 0.46f,
                        )
                        lineTo(w * 0.78f, w * 0.9f)
                    }
                    val measure = PathMeasure().apply { setPath(arch, false) }
                    val segment = Path()
                    measure.getSegment(0f, measure.length * draw.coerceIn(0f, 1f), segment, true)
                    drawPath(
                        segment,
                        color = colors.mintDeep,
                        style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
                    )

                    // sheen sweeping across the arch
                    if (sheen > 0f) {
                        rotate(degrees = -24f, pivot = center) {
                            val x = -w * 0.4f + sheen * w * 1.8f
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        colors.mint.copy(alpha = 0.55f),
                                        Color.Transparent,
                                    ),
                                    start = Offset(x, 0f),
                                    end = Offset(x + w * 0.28f, size.height),
                                ),
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width, size.height),
                            )
                        }
                    }
                }

                KhIcon(
                    KhIcons.Book,
                    Modifier
                        .size(46.dp)
                        .graphicsLayer { alpha = ((draw - 0.45f) / 0.55f).coerceIn(0f, 1f) },
                    colors.ink,
                )

                val starReveal = ((draw - 0.7f) / 0.3f).coerceIn(0f, 1f)
                KhIcon(
                    KhIcons.Star8,
                    Modifier
                        .align(Alignment.TopCenter)
                        .size(18.dp)
                        .graphicsLayer {
                            alpha = starReveal
                            translationY = (1f - starReveal) * 12f - 4f
                            rotationZ = (1f - starReveal) * -40f
                        },
                    colors.gold,
                )
            }

            Spacer(Modifier.height(30.dp))

            Row(horizontalArrangement = Arrangement.Center) {
                "khajabad".forEachIndexed { index, ch ->
                    val letter by animateFloatAsState(
                        targetValue = if (started) 1f else 0f,
                        animationSpec = Motion.stopMotion(durationMillis = 460, delayMillis = 260 + index * 46),
                        label = "letter$index",
                    )
                    BasicText(
                        text = ch.toString(),
                        style = Type.Display.copy(
                            fontFamily = Outfit,
                            fontSize = 44.sp,
                            color = colors.ink,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.graphicsLayer {
                            alpha = letter
                            translationY = (1f - letter) * 18f
                        },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            val subtitle by animateFloatAsState(
                targetValue = if (started) 1f else 0f,
                animationSpec = tween(520, delayMillis = 760),
                label = "sub",
            )
            BasicText(
                text = "ഓഫ്‍ലൈൻ പുസ്തകശാല",
                style = Type.MalLabel.copy(
                    color = colors.inkSoft,
                    fontSize = 16.sp,
                    letterSpacing = 2.sp,
                ),
                modifier = Modifier.graphicsLayer { alpha = subtitle },
            )
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = "OFFLINE PDF LIBRARY",
                style = Type.Label.copy(color = colors.mintDeep, fontSize = 9.5.sp),
                modifier = Modifier.graphicsLayer { alpha = subtitle },
            )
        }

        // Progress hairline pinned to the bottom.
        val progress by animateFloatAsState(
            targetValue = if (started) 1f else 0f,
            animationSpec = tween(2100, easing = LinearEasing),
            label = "progress",
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 84.dp)
                .width(96.dp)
                .height(3.dp)
                .background(colors.line, androidx.compose.foundation.shape.RoundedCornerShape(50)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(listOf(colors.mint, colors.mintDeep)),
                        androidx.compose.foundation.shape.RoundedCornerShape(50),
                    ),
            )
        }
    }
}
