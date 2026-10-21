package com.khajabad.app.ui.events

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khajabad.app.data.events.Dates
import com.khajabad.app.data.events.Event
import com.khajabad.app.data.events.EventsState
import com.khajabad.app.ui.components.ShimmerBox
import com.khajabad.app.ui.components.rememberPressScale
import com.khajabad.app.ui.theme.KhIcons
import com.khajabad.app.ui.theme.KhIcon
import com.khajabad.app.ui.theme.Khaja
import com.khajabad.app.ui.theme.Motion
import com.khajabad.app.ui.theme.Type
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * The "next event" hero card on Home.
 *
 * Deep-ink surface (so it reads as a different material next to the mint tiles),
 * a date block, a live countdown that ticks every second, and a freshness dot.
 */
@Composable
fun NextEventCard(
    state: EventsState,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Khaja.colors
    val shape = RoundedCornerShape(32.dp)

    when (state) {
        is EventsState.Loading, EventsState.Idle -> {
            Box(
                modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(shape)
                    .background(colors.ink.copy(alpha = 0.06f)),
            ) {
                ShimmerBox(Modifier.fillMaxSize(), shape)
            }
        }

        is EventsState.Failed -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.paperAlt)
                    .border(1.dp, colors.line, shape)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 18.dp, vertical = 20.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.mintSoft),
                ) {
                    KhIcon(KhIcons.Refresh, Modifier.size(18.dp), colors.mintDeep)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = "അടുത്ത പരിപാടികൾ",
                        style = Type.MalLabel.copy(color = colors.ink, fontSize = 13.5.sp),
                    )
                    Spacer(Modifier.height(2.dp))
                    BasicText(
                        text = "ലോഡ് ചെയ്യാനായില്ല · വീണ്ടും ശ്രമിക്കാൻ ടാപ്പ് ചെയ്യുക",
                        style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 11.sp),
                        maxLines = 2,
                    )
                }
                KhIcon(KhIcons.Refresh, Modifier.size(15.dp), colors.inkSoft)
            }
        }

        else -> {
            val next = state.next
            if (next == null) {
                Box(
                    modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(colors.paperAlt)
                        .padding(20.dp),
                ) {
                    BasicText(
                        text = "വരാനിരിക്കുന്ന പരിപാടികൾ ഒന്നുമില്ല",
                        style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 12.sp),
                    )
                }
                return
            }
            EventHero(
                event = next,
                state = state,
                onClick = onOpen,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun EventHero(
    event: Event,
    state: EventsState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Khaja.colors
    val shape = RoundedCornerShape(32.dp)
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.985f)
    val fresh = (state as? EventsState.Live)?.fetchedAt ?: (state as? EventsState.Stale)?.fetchedAt ?: 0L

    val transition = rememberInfiniteTransition(label = "eventGlow")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF0D2436), colors.ink)))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        // soft mint bloom in the corner
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.mint.copy(alpha = 0.22f + pulse * 0.06f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.92f, size.height * 0.08f),
                    radius = size.width * 0.55f,
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.92f, size.height * 0.08f),
            )
            drawLine(
                color = colors.mint.copy(alpha = 0.16f),
                start = Offset(0f, size.height - 22f),
                end = Offset(size.width, size.height - 22f),
                strokeWidth = 1.5f,
            )
        }

        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot(
                    live = state is EventsState.Live,
                    label = if (state is EventsState.Live) "NEXT EVENT"
                    else "SAVED · ${Dates.relativeShort(fresh)}",
                    accent = colors.mint,
                )
                Spacer(Modifier.weight(1f))
                if (event.highlighted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100))
                            .background(colors.gold.copy(alpha = 0.18f))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        KhIcon(KhIcons.Star8, Modifier.size(9.dp), colors.gold)
                        Spacer(Modifier.width(4.dp))
                        BasicText(
                            text = "FEATURED",
                            style = Type.Label.copy(color = colors.gold, fontSize = 7.5.sp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.Top) {
                DateBlock(event.start, dark = true)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = event.title,
                        style = Type.MalTitle.copy(color = colors.paper, fontSize = 16.sp, lineHeight = 23.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(5.dp))
                    val meta = listOfNotNull(
                        event.start?.let { Dates.timeOnly(it) },
                        event.location,
                    ).joinToString(" · ")
                    if (meta.isNotBlank()) {
                        BasicText(
                            text = meta,
                            style = Type.Small.copy(
                                color = colors.paper.copy(alpha = 0.62f),
                                fontSize = 11.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                CountdownChip(event.start)
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(colors.mint.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    BasicText(
                        text = "കാണുക",
                        style = Type.MalLabel.copy(color = colors.mint, fontSize = 11.5.sp),
                    )
                    Spacer(Modifier.width(3.dp))
                    KhIcon(KhIcons.ChevronRight, Modifier.size(12.dp), colors.mint)
                }
            }
        }
    }
}

@Composable
private fun LiveDot(live: Boolean, label: String, accent: Color) {
    val transition = rememberInfiniteTransition(label = "dot")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotAlpha",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (live) alpha else 0.5f)),
        )
        Spacer(Modifier.width(7.dp))
        BasicText(text = label, style = Type.Label.copy(color = accent.copy(alpha = 0.9f), fontSize = 8.5.sp))
    }
}

@Composable
fun DateBlock(start: Long?, dark: Boolean = false, large: Boolean = true) {
    val colors = Khaja.colors
    val day = start?.let { Dates.dayMonth(it).first } ?: "--"
    val month = start?.let { Dates.dayMonth(it).second } ?: ""
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(width = if (large) 58.dp else 46.dp, height = if (large) 62.dp else 50.dp)
            .clip(RoundedCornerShape(if (large) 18.dp else 14.dp))
            .background(if (dark) colors.mint else colors.mintSoft),
    ) {
        BasicText(
            text = day,
            style = Type.Display.copy(
                color = if (dark) colors.onMint else colors.mintDeep,
                fontSize = if (large) 24.sp else 19.sp,
                lineHeight = if (large) 26.sp else 21.sp,
                letterSpacing = (-0.6).sp,
            ),
        )
        BasicText(
            text = month,
            style = Type.Label.copy(
                color = if (dark) colors.onMint.copy(alpha = 0.72f) else colors.mintDeep.copy(alpha = 0.8f),
                fontSize = if (large) 8.sp else 7.sp,
                letterSpacing = 1.sp,
            ),
        )
    }
}

/** mm:ss / hh:mm / days — recomposes once per second via [Clock]. */
@Composable
fun CountdownChip(start: Long?) {
    val colors = Khaja.colors
    val now = rememberClock(start)
    val text = when {
        start == null -> "തീയതി ഉടൻ"
        start <= now -> "ഇപ്പോൾ നടക്കുന്നു"
        else -> countdownText(start - now)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(colors.mint.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        KhIcon(KhIcons.Info, Modifier.size(11.dp), colors.mint)
        Spacer(Modifier.width(5.dp))
        BasicText(
            text = text,
            style = Type.Small.copy(color = colors.mint, fontSize = 10.5.sp),
        )
    }
}

fun countdownText(remaining: Long): String {
    val days = TimeUnit.MILLISECONDS.toDays(remaining)
    val hours = TimeUnit.MILLISECONDS.toHours(remaining) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
    return when {
        days >= 2 -> "ഇനി $days ദിവസം"
        days >= 1 -> "ഇനി 1 ദിവസം ${hours} മണിക്കൂർ"
        hours >= 1 -> "ഇനി ${hours}h ${minutes}m"
        else -> "ഇനി ${minutes}m ${seconds}s"
    }
}

/** A 1-second tick that only runs while composable is on screen. */
@Composable
fun rememberClock(key: Any? = null): Long {
    var now by remember(key) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(key) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    return now
}
