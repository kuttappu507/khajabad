package com.khajabad.app.ui.home

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khajabad.app.core.Book
import com.khajabad.app.data.PrefsState
import com.khajabad.app.data.events.EventsState
import com.khajabad.app.ui.components.PdfPageImage
import com.khajabad.app.ui.components.Revealed
import com.khajabad.app.ui.components.RoundIconButton
import com.khajabad.app.ui.components.SectionHeader
import com.khajabad.app.ui.components.rememberPressScale
import com.khajabad.app.ui.events.NextEventCard
import com.khajabad.app.ui.theme.KhIcon
import com.khajabad.app.ui.theme.KhIcons
import com.khajabad.app.ui.theme.Khaja
import com.khajabad.app.ui.theme.Motion
import com.khajabad.app.ui.theme.Type

/**
 * Home.
 *
 * Khajabad ships two books, so home is simply: a greeting, both books with their
 * reading progress, and the next event from the website. No fake catalogue.
 */
@Composable
fun HomeScreen(
    books: List<Book>,
    recent: List<Book>,
    prefs: PrefsState,
    onMenu: () -> Unit,
    onBookmarks: () -> Unit,
    onShare: () -> Unit,
    onOpenBook: (Book) -> Unit,
    onSeeAll: () -> Unit,
    events: EventsState,
    onOpenEvents: () -> Unit,
    onRefreshEvents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Khaja.colors
    val listState = rememberLazyListState()

    Box(modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 132.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { HomeTopBar(onMenu = onMenu, onBookmarks = onBookmarks, onShare = onShare) }

            item {
                Revealed(index = 0) {
                    HeroBanner(
                        bookCount = books.size,
                        pages = books.sumOf { it.pages },
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Your books",
                    action = "Library",
                    onAction = onSeeAll,
                    modifier = Modifier.padding(start = 22.dp, end = 18.dp, top = 22.dp, bottom = 12.dp),
                )
            }

            items(books.size) { index ->
                val book = books[index]
                Revealed(index = index + 1, delayStep = 60) {
                    BookRow(
                        book = book,
                        page = prefs.pageOf(book.id),
                        onClick = { onOpenBook(book) },
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 5.dp),
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Next events",
                    action = "All events",
                    onAction = onOpenEvents,
                    modifier = Modifier.padding(start = 22.dp, end = 18.dp, top = 26.dp, bottom = 12.dp),
                )
            }
            item {
                Revealed(index = 3) {
                    NextEventCard(
                        state = events,
                        onOpen = onOpenEvents,
                        onRetry = onRefreshEvents,
                        modifier = Modifier.padding(horizontal = 22.dp),
                    )
                }
            }

            item { Spacer(Modifier.height(26.dp)) }
            item { Footer(lastRead = recent.firstOrNull()) }
        }
    }
}

@Composable
private fun HomeTopBar(onMenu: () -> Unit, onBookmarks: () -> Unit, onShare: () -> Unit) {
    val colors = Khaja.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp),
    ) {
        val interaction = remember { MutableInteractionSource() }
        val scale = rememberPressScale(interaction)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.line, CircleShape)
                .clickable(interactionSource = interaction, indication = null, onClick = onMenu),
        ) {
            KhIcon(KhIcons.Menu, Modifier.size(20.dp), colors.text)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = "khajabad",
                style = Type.Title.copy(color = colors.text, fontSize = 17.sp),
            )
            BasicText(
                text = "ഓഫ്‌ലൈൻ പുസ്തകശാല",
                style = Type.MalLabel.copy(color = colors.textSoft, fontSize = 10.5.sp),
            )
        }
        RoundIconButton(
            KhIcons.Heart,
            onBookmarks,
            size = 44,
            tint = colors.text,
            background = colors.surface,
        )
        Spacer(Modifier.width(8.dp))
        RoundIconButton(
            KhIcons.Share,
            onShare,
            size = 44,
            tint = colors.text,
            background = colors.surface,
        )
    }
}

/**
 * The one piece of brand theatre on this screen: a violet mesh card that drifts
 * slowly, with the wordmark and a one-line summary of what is inside.
 */
@Composable
private fun HeroBanner(bookCount: Int, pages: Int, modifier: Modifier = Modifier) {
    val colors = Khaja.colors
    val transition = rememberInfiniteTransition(label = "hero")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )
    val width = LocalConfiguration.current.screenWidthDp.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(198.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(colors.accent, colors.accentDeep))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(size.width * (0.18f + drift * 0.10f), size.height * 0.14f),
                    radius = size.maxDimension * 0.62f,
                ),
                radius = size.maxDimension * 0.62f,
                center = Offset(size.width * (0.18f + drift * 0.10f), size.height * 0.14f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.amber.copy(alpha = 0.42f), Color.Transparent),
                    center = Offset(size.width * (0.92f - drift * 0.08f), size.height * 0.92f),
                    radius = size.maxDimension * 0.55f,
                ),
                radius = size.maxDimension * 0.55f,
                center = Offset(size.width * (0.92f - drift * 0.08f), size.height * 0.92f),
            )
            // two calm orbit rings, hand-offset
            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = size.minDimension * (0.36f + drift * 0.02f),
                center = Offset(size.width * 0.84f, size.height * 0.20f),
                style = Stroke(1.2.dp.toPx()),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = size.minDimension * 0.52f,
                center = Offset(size.width * 0.84f, size.height * 0.20f),
                style = Stroke(1.dp.toPx()),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                ) {
                    KhIcon(KhIcons.Book, Modifier.size(19.dp), colors.onAccent)
                }
                Spacer(Modifier.width(11.dp))
                BasicText(
                    text = "ഖജാബാദ്",
                    style = Type.MalTitle.copy(color = colors.onAccent, fontSize = 15.sp),
                )
                Spacer(Modifier.weight(1f))
                KhIcon(KhIcons.Star8, Modifier.size(15.dp), colors.amber)
            }

            Column {
                BasicText(
                    text = "വായന,\nഎപ്പോഴും കൂടെ",
                    style = Type.MalDisplay.copy(
                        color = colors.onAccent,
                        fontSize = (width.value * 0.072f).coerceIn(24f, 31f).sp,
                        lineHeight = (width.value * 0.085f).coerceIn(30f, 37f).sp,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetaPill("$bookCount പുസ്തകം", colors.onAccent)
                    Spacer(Modifier.width(8.dp))
                    MetaPill("$pages പേജ്", colors.onAccent)
                    Spacer(Modifier.width(8.dp))
                    MetaPill("ഓഫ്‌ലൈൻ", colors.onAccent)
                }
            }
        }
    }
}

@Composable
private fun MetaPill(text: String, color: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 11.dp, vertical = 5.dp),
    ) {
        BasicText(
            text = text,
            style = Type.MalLabel.copy(color = color, fontSize = 10.5.sp),
            maxLines = 1,
        )
    }
}

@Composable
private fun BookRow(book: Book, page: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Khaja.colors
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.98f)
    val progress = ((page + 1).toFloat() / book.pages.coerceAtLeast(1)).coerceIn(0f, 1f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(24.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(12.dp),
    ) {
        Box(
            Modifier
                .width(62.dp)
                .height(84.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White),
        ) {
            PdfPageImage(
                book = book,
                page = 0,
                targetWidth = 240,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = book.title,
                style = Type.MalTitle.copy(color = colors.text, fontSize = 15.sp),
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            BasicText(
                text = "${book.latin} · ${book.pages} പേജ്",
                style = Type.Small.copy(color = colors.textSoft, fontSize = 10.5.sp),
                maxLines = 1,
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.bgAlt),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(if (page > 0) progress else 0.02f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
            }
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = if (page > 0) "പേജ് ${page + 1} / ${book.pages} · തുടരുക"
                else "${book.subtitle}",
                style = Type.MalLabel.copy(color = colors.textSoft, fontSize = 10.sp),
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
        ) {
            KhIcon(KhIcons.ChevronRight, Modifier.size(15.dp), colors.accentDeep)
        }
    }
}

@Composable
private fun Footer(lastRead: Book?) {
    val colors = Khaja.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KhIcon(KhIcons.Star8, Modifier.size(12.dp), colors.amber)
            Spacer(Modifier.width(7.dp))
            BasicText(
                text = "khajabad",
                style = Type.Label.copy(color = colors.textSoft, fontSize = 9.sp),
            )
        }
        Spacer(Modifier.height(5.dp))
        BasicText(
            text = if (lastRead != null) "അവസാനം വായിച്ചത് · ${lastRead.title}"
            else "ഓഫ്‌ലൈൻ വായന · രണ്ട് പുസ്തകം",
            style = Type.MalLabel.copy(color = colors.textSoft, fontSize = 10.sp),
        )
    }
}
