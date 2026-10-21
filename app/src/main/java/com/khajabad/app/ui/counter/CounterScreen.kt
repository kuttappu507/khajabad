package com.khajabad.app.ui.counter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khajabad.app.ui.components.Revealed
import com.khajabad.app.ui.components.RoundIconButton
import com.khajabad.app.ui.theme.KhIcons
import com.khajabad.app.ui.theme.KhIcon
import com.khajabad.app.ui.theme.Khaja
import com.khajabad.app.ui.theme.Motion
import com.khajabad.app.ui.theme.Type
import kotlin.math.roundToInt

@Composable
fun CounterScreen(
    count: Int,
    target: Int,
    onTap: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onTarget: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Khaja.colors
    val haptics = LocalHapticFeedback.current
    val ripples = remember { mutableStateListOf<Long>() }

    val animated by animateFloatAsState(
        targetValue = count.toFloat(),
        animationSpec = Motion.fluid(),
        label = "count",
    )
    val inCycle = if (target > 0) count % target else count
    val cycles = if (target > 0) count / target else 0
    val fraction = if (target > 0) inCycle.toFloat() / target else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.paper)
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BasicText(text = "Counter", style = Type.H1.copy(color = colors.ink))
                Spacer(Modifier.height(3.dp))
                BasicText(
                    text = "ദിക്ർ കൗണ്ടർ · ഓഫ്‌ലൈൻ",
                    style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 12.sp),
                )
            }
            RoundIconButton(
                KhIcons.Refresh,
                onReset,
                size = 44,
                background = colors.card,
            )
        }

        Spacer(Modifier.weight(0.55f))

        Revealed(index = 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(304.dp),
                contentAlignment = Alignment.Center,
            ) {
                // expanding ripples
                val activeRipples = ripples.toList()
                for (key in activeRipples) {
                    TapRipple(
                        key = key,
                        color = colors.mint,
                        onFinished = { ripples.remove(key) },
                    )
                }

                Canvas(Modifier.size(292.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.mint.copy(alpha = 0.26f), Color.Transparent),
                            center = center,
                            radius = size.minDimension * 0.5f,
                        ),
                        radius = size.minDimension * 0.5f,
                    )
                    drawCircle(
                        color = colors.ink.copy(alpha = 0.06f),
                        radius = size.minDimension * 0.395f,
                        style = Stroke(7.dp.toPx()),
                    )
                    drawArc(
                        color = colors.mintDeep,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(
                            center.x - size.minDimension * 0.395f,
                            center.y - size.minDimension * 0.395f,
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            size.minDimension * 0.79f,
                            size.minDimension * 0.79f,
                        ),
                    )
                }

                TapSurface(
                    onTap = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        ripples.add(System.currentTimeMillis())
                        onTap()
                    },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BasicText(
                            text = "${animated.roundToInt()}",
                            style = Type.Display.copy(
                                color = colors.paper,
                                fontSize = 66.sp,
                                lineHeight = 70.sp,
                            ),
                        )
                        Spacer(Modifier.height(2.dp))
                        BasicText(
                            text = if (target > 0) "$inCycle / $target" else "∞",
                            style = Type.Small.copy(
                                color = colors.mint,
                                fontSize = 12.sp,
                                letterSpacing = 1.4.sp,
                            ),
                        )
                        Spacer(Modifier.height(16.dp))
                        BasicText(
                            text = "ടാപ്പ് ചെയ്യുക",
                            style = Type.MalLabel.copy(
                                color = colors.paper.copy(alpha = 0.55f),
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            BasicText(
                text = "സൈക്കിൾ ${cycles + 1} · ${target.coerceAtLeast(1)} എണ്ണം",
                style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 12.sp),
            )
        }

        Spacer(Modifier.weight(0.55f))

        Revealed(index = 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PresetChip("33", target == 33, Modifier.weight(1f)) { onTarget(33) }
                PresetChip("99", target == 99, Modifier.weight(1f)) { onTarget(99) }
                PresetChip("100", target == 100, Modifier.weight(1f)) { onTarget(100) }
                PresetChip("∞", target == 0, Modifier.weight(1f)) { onTarget(0) }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ActionPill(
                label = "ഒന്ന് കുറയ്ക്കുക",
                icon = KhIcons.Minus,
                onClick = onUndo,
                modifier = Modifier.weight(1f),
            )
            ActionPill(
                label = "പുനഃസജ്ജമാക്കുക",
                icon = KhIcons.Refresh,
                onClick = onReset,
                modifier = Modifier.weight(1f),
                filled = true,
            )
        }

        Spacer(Modifier.height(118.dp))
    }
}

@Composable
private fun TapSurface(onTap: () -> Unit, content: @Composable () -> Unit) {
    val colors = Khaja.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = Motion.snappy(),
        label = "tapScale",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(236.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(colors.ink, Color(0xFF07131F)),
                    radius = 420f,
                )
            )
            .border(1.dp, colors.mint.copy(alpha = 0.25f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onTap),
    ) { content() }
}

@Composable
private fun TapRipple(key: Long, color: Color, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(key) {
        progress.animateTo(1f, tween(760, easing = LinearEasing))
        onFinished()
    }
    val p = progress.value
    Box(
        Modifier
            .size(236.dp)
            .graphicsLayer {
                scaleX = 1f + p * 0.55f
                scaleY = 1f + p * 0.55f
                alpha = (1f - p) * 0.55f
            }
            .clip(CircleShape)
            .border(2.dp, color, CircleShape),
    )
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = Khaja.colors
    val interaction = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (interaction.collectIsPressedAsState().value) 0.94f else 1f,
        animationSpec = Motion.snappy(),
        label = "chip",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(100))
            .background(if (selected) colors.ink else Color.Transparent)
            .border(
                1.2.dp,
                if (selected) Color.Transparent else colors.line,
                RoundedCornerShape(100),
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        BasicText(
            text = label,
            style = Type.Title.copy(
                color = if (selected) colors.mint else colors.inkSoft,
                fontSize = 13.sp,
            ),
        )
    }
}

@Composable
private fun ActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    val colors = Khaja.colors
    val interaction = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (interaction.collectIsPressedAsState().value) 0.96f else 1f,
        animationSpec = Motion.snappy(),
        label = "action",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(100))
            .background(if (filled) colors.ink else colors.card)
            .border(1.dp, if (filled) Color.Transparent else colors.line, RoundedCornerShape(100))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        KhIcon(icon, Modifier.size(15.dp), if (filled) colors.mint else colors.inkSoft)
        Spacer(Modifier.size(8.dp))
        BasicText(
            text = label,
            style = Type.MalLabel.copy(
                color = if (filled) colors.paper else colors.inkSoft,
                fontSize = 11.sp,
            ),
            maxLines = 1,
        )
    }
}
