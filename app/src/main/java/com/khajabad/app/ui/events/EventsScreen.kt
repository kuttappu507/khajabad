package com.khajabad.app.ui.events

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khajabad.app.data.events.Dates
import com.khajabad.app.data.events.Event
import com.khajabad.app.data.events.EventsState
import com.khajabad.app.ui.components.Revealed
import com.khajabad.app.ui.components.RoundIconButton
import com.khajabad.app.ui.components.ShimmerBox
import com.khajabad.app.ui.components.rememberPressScale
import com.khajabad.app.ui.theme.KhIcons
import com.khajabad.app.ui.theme.KhIcon
import com.khajabad.app.ui.theme.Khaja
import com.khajabad.app.ui.theme.Type

@Composable
fun EventsScreen(
    state: EventsState,
    configured: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Khaja.colors
    val context = LocalContext.current
    var selected by remember { mutableStateOf<Event?>(null) }
    var showRaw by rememberSaveable { mutableStateOf(false) }

    val events = state.events

    Box(modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(KhIcons.Back, onBack, size = 42, background = colors.surface)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        BasicText(text = "Events", style = Type.H2.copy(color = colors.text))
                        BasicText(
                            text = headerSubtitle(state, events.size),
                            style = Type.MalLabel.copy(color = colors.textSoft, fontSize = 11.sp),
                        )
                    }
                    RoundIconButton(
                        KhIcons.Refresh,
                        onRefresh,
                        size = 42,
                        background = colors.accentSoft,
                        borderColor = colors.accentSoft,
                        tint = colors.accentDeep,
                    )
                }
            }

            if (state is EventsState.Stale) {
                item {
                    Banner(
                        text = "ഓഫ്‌ലൈൻ · ${Dates.relative(state.fetchedAt)} ലെ ഡാറ്റ കാണിക്കുന്നു",
                        icon = KhIcons.Info,
                    )
                }
            }
            if (!configured) {
                item {
                    Banner(
                        text = "ഈ ബിൽഡിൽ ഇവന്റ് ഫീഡ് കോൺഫിഗർ ചെയ്തിട്ടില്ല (NOTTE_API_KEY)",
                        icon = KhIcons.Info,
                    )
                }
            }
            if (state is EventsState.Failed) {
                item {
                    Banner(text = state.error, icon = KhIcons.Info)
                    if (state.detail.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.bgAlt)
                                .padding(14.dp),
                        ) {
                            BasicText(
                                text = "അവസാന ശ്രമത്തിന്റെ വിശദാംശം",
                                style = Type.MalLabel.copy(color = colors.textSoft, fontSize = 10.sp),
                            )
                            Spacer(Modifier.height(6.dp))
                            BasicText(
                                text = state.detail,
                                style = Type.Small.copy(
                                    color = colors.textSoft,
                                    fontSize = 9.5.sp,
                                    lineHeight = 14.sp,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(100))
                            .background(colors.accent)
                            .clickable(onClick = onRefresh)
                            .padding(horizontal = 18.dp, vertical = 11.dp),
                    ) {
                        BasicText(
                            text = "വീണ്ടും ശ്രമിക്കുക",
                            style = Type.MalLabel.copy(color = colors.onAccent, fontSize = 12.sp),
                        )
                    }
                }
            }

            if (state is EventsState.Loading && events.isEmpty()) {
                items(3) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .clip(RoundedCornerShape(24.dp)),
                    ) {
                        ShimmerBox(Modifier.fillMaxSize(), RoundedCornerShape(24.dp))
                    }
                }
            }

            var lastMonth = ""
            itemsIndexed(events) { index, event ->
                val month = event.start?.let { Dates.monthKey(it) } ?: "Date TBA"
                Column {
                    if (month != lastMonth) {
                        BasicText(
                            text = month.uppercase(),
                            style = Type.Label.copy(color = colors.textSoft, fontSize = 9.sp),
                            modifier = Modifier.padding(start = 6.dp, top = if (index == 0) 8.dp else 18.dp, bottom = 8.dp),
                        )
                        lastMonth = month
                    }
                    Revealed(index = index.coerceAtMost(6), delayStep = 40) {
                        EventRow(
                            event = event,
                            onClick = { selected = event },
                        )
                    }
                }
            }

            if (events.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100))
                            .clickable { showRaw = !showRaw }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                    ) {
                        KhIcon(KhIcons.Dots, Modifier.size(12.dp), colors.textSoft)
                        Spacer(Modifier.width(6.dp))
                        BasicText(
                            text = if (showRaw) "Hide raw fields" else "Show raw fields",
                            style = Type.Small.copy(color = colors.textSoft, fontSize = 10.sp),
                        )
                    }
                }
                if (showRaw) {
                    item {
                        val raw = events.firstOrNull()?.raw.orEmpty()
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.bgAlt)
                                .padding(14.dp),
                        ) {
                            BasicText(
                                text = if (raw.isEmpty()) "No raw fields cached" else raw.entries.joinToString("\n") { "${it.key}: ${it.value}" },
                                style = Type.Small.copy(color = colors.textSoft, fontSize = 9.5.sp, lineHeight = 15.sp),
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selected != null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.scrim.copy(alpha = 0.5f))
                    .clickable { selected = null },
                contentAlignment = Alignment.BottomCenter,
            ) {
                selected?.let { event ->
                    EventSheet(
                        event = event,
                        onClose = { selected = null },
                        onCalendar = { addToCalendar(context, event) },
                        onOpenLink = { openUrl(context, event.url) },
                        onShare = {
                            onShare(
                                "${event.title}\n" +
                                    (event.start?.let { Dates.readable(it) } ?: "") +
                                    (event.location?.let { "\n$it" } ?: "")
                            )
                            selected = null
                        },
                    )
                }
            }
        }
    }
}

private fun headerSubtitle(state: EventsState, count: Int): String = when (state) {
    is EventsState.Live -> "$count പരിപാടികൾ · ${Dates.relative(state.fetchedAt)} അപ്ഡേറ്റ്"
    is EventsState.Stale -> "$count പരിപാടികൾ · ഓഫ്‌ലൈൻ"
    is EventsState.Loading -> "അപ്ഡേറ്റ് ചെയ്യുന്നു…"
    is EventsState.Failed -> "ലഭ്യമല്ല"
    EventsState.Idle -> "$count പരിപാടികൾ"
}

@Composable
private fun Banner(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val colors = Khaja.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.bgAlt)
            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        KhIcon(icon, Modifier.size(14.dp), colors.accentDeep)
        Spacer(Modifier.width(10.dp))
        BasicText(
            text = text,
            style = Type.MalLabel.copy(color = colors.textSoft, fontSize = 11.sp, lineHeight = 17.sp),
        )
    }
}

@Composable
private fun EventRow(event: Event, onClick: () -> Unit) {
    val colors = Khaja.colors
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.985f)
    val today = event.start != null && Dates.startOfDay(event.start) == Dates.startOfDay(System.currentTimeMillis())
    val past = event.start != null && event.start < System.currentTimeMillis() - 60 * 60 * 1000

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (past) 0.55f else 1f }
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(
                1.dp,
                if (today) colors.accent else colors.line,
                RoundedCornerShape(24.dp),
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(12.dp),
    ) {
        DateBlock(event.start, dark = today)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = event.title,
                style = Type.MalTitle.copy(color = colors.text, fontSize = 14.sp, lineHeight = 21.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (today) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(100))
                            .background(colors.accentSoft)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        BasicText(
                            text = "ഇന്ന്",
                            style = Type.MalLabel.copy(color = colors.accentDeep, fontSize = 9.sp),
                        )
                    }
                    Spacer(Modifier.width(7.dp))
                }
                BasicText(
                    text = listOfNotNull(
                        event.start?.let { Dates.timeOnly(it) },
                        event.location,
                    ).joinToString(" · ").ifBlank { "വിശദാംശങ്ങൾ കാണുക" },
                    style = Type.Small.copy(color = colors.textSoft, fontSize = 10.5.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        KhIcon(KhIcons.ChevronRight, Modifier.size(15.dp), colors.textSoft)
    }
}

@Composable
private fun EventSheet(
    event: Event,
    onClose: () -> Unit,
    onCalendar: () -> Unit,
    onOpenLink: () -> Unit,
    onShare: () -> Unit,
) {
    val colors = Khaja.colors
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.74f)
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(colors.surface)
            .clickable(enabled = false) {}
            .navigationBarsPadding(),
    ) {
        Box(
            Modifier
                .padding(top = 10.dp)
                .align(Alignment.CenterHorizontally)
                .size(width = 42.dp, height = 4.dp)
                .clip(CircleShape)
                .background(colors.line),
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 18.dp, bottom = 26.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DateBlock(event.start, large = true)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = event.title,
                        style = Type.MalTitle.copy(color = colors.text, fontSize = 17.sp, lineHeight = 25.sp),
                    )
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        text = event.start?.let { Dates.readable(it) } ?: "തീയതി പ്രഖ്യാപിച്ചിട്ടില്ല",
                        style = Type.Small.copy(color = colors.accentDeep, fontSize = 11.sp),
                    )
                }
                RoundIconButton(
                    KhIcons.Close,
                    onClose,
                    size = 34,
                    background = colors.bgAlt,
                    borderColor = colors.line,
                )
            }

            Spacer(Modifier.height(18.dp))

            event.location?.let {
                InfoRow(KhIcons.Mihrab, "സ്ഥലം", it)
                Spacer(Modifier.height(10.dp))
            }
            if (event.end != null) {
                InfoRow(KhIcons.Info, "അവസാനിക്കുന്നു", Dates.readable(event.end))
                Spacer(Modifier.height(10.dp))
            }
            event.description?.let {
                Spacer(Modifier.height(6.dp))
                BasicText(
                    text = it,
                    style = Type.MalBody.copy(color = colors.textSoft, fontSize = 13.sp, lineHeight = 22.sp),
                )
            }

            Spacer(Modifier.height(22.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SheetAction(
                    label = "കലണ്ടറിൽ ചേർക്കുക",
                    icon = if (event.start != null) KhIcons.Plus else KhIcons.Info,
                    onClick = onCalendar,
                    modifier = Modifier.weight(1f),
                    filled = true,
                )
                if (event.url != null) {
                    SheetAction("ലിങ്ക്", KhIcons.Share, onOpenLink, Modifier.weight(0.46f))
                } else {
                    SheetAction("പങ്കിടുക", KhIcons.Share, onShare, Modifier.weight(0.46f))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    val colors = Khaja.colors
    Row(verticalAlignment = Alignment.Top) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
        ) {
            KhIcon(icon, Modifier.size(13.dp), colors.accentDeep)
        }
        Spacer(Modifier.width(11.dp))
        Column {
            BasicText(text = label, style = Type.Label.copy(color = colors.textSoft, fontSize = 8.5.sp))
            Spacer(Modifier.height(2.dp))
            BasicText(
                text = value,
                style = Type.MalLabel.copy(color = colors.text, fontSize = 12.5.sp, lineHeight = 19.sp),
            )
        }
    }
}

@Composable
private fun SheetAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    val colors = Khaja.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(100))
            .background(if (filled) colors.text else colors.bgAlt)
            .clickable(onClick = onClick),
    ) {
        KhIcon(icon, Modifier.size(14.dp), if (filled) colors.accent else colors.textSoft)
        Spacer(Modifier.width(8.dp))
        BasicText(
            text = label,
            style = Type.MalLabel.copy(
                color = if (filled) colors.bg else colors.textSoft,
                fontSize = 11.sp,
            ),
            maxLines = 1,
        )
    }
}

/* ---------------------------------------------------------------- intents */

private fun addToCalendar(context: Context, event: Event) {
    val start = event.start ?: return
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end ?: (start + 60 * 60 * 1000))
        putExtra(CalendarContract.Events.TITLE, event.title)
        event.location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
        event.description?.let { putExtra(CalendarContract.Events.DESCRIPTION, it) }
    }
    runCatching { context.startActivity(intent) }
}

private fun openUrl(context: Context, url: String?) {
    if (url == null) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    }
}
