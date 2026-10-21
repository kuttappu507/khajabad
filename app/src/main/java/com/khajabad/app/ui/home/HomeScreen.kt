package com.khajabad.app.ui.home

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khajabad.app.core.Book
import com.khajabad.app.core.Category
import com.khajabad.app.core.Catalog
import com.khajabad.app.data.PrefsState
import com.khajabad.app.data.events.EventsState
import com.khajabad.app.ui.events.NextEventCard
import com.khajabad.app.ui.theme.KhIcon
import com.khajabad.app.ui.components.Revealed
import com.khajabad.app.ui.components.RoundIconButton
import com.khajabad.app.ui.components.SectionHeader
import com.khajabad.app.ui.components.rememberPressScale
import com.khajabad.app.ui.theme.KhIcons
import com.khajabad.app.ui.theme.Khaja
import com.khajabad.app.ui.theme.Motion
import com.khajabad.app.ui.theme.Type

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
    onCategory: (Category) -> Unit,
    events: EventsState,
    onOpenEvents: () -> Unit,
    onRefreshEvents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Khaja.colors
    val listState = rememberLazyListState()
    val heroLift by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else 420f
        }
    }

    Box(modifier.fillMaxSize().background(colors.paper)) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 132.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                HomeTopBar(onMenu = onMenu, onBookmarks = onBookmarks, onShare = onShare)
            }
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .graphicsLayer {
                            translationY = heroLift * 0.28f
                            alpha = (1f - heroLift / 520f).coerceIn(0.15f, 1f)
                            scaleX = 1f - (heroLift / 520f) * 0.08f
                            scaleY = scaleX
                        },
                ) {
                    HeroMark()
                }
            }
            item {
                SectionHeader(
                    title = "Recent",
                    action = "See all",
                    onAction = onSeeAll,
                    modifier = Modifier.padding(start = 22.dp, end = 18.dp, top = 2.dp, bottom = 14.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(recent) { index, book ->
                        Revealed(index = index, delayStep = 45) {
                            RecentChip(book = book, onClick = { onOpenBook(book) })
                        }
                    }
                    item {
                        Revealed(index = recent.size, delayStep = 45) {
                            GhostChip(onClick = onSeeAll)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
            item {
                Revealed(index = 1) {
                    val featured = recent.firstOrNull() ?: books.firstOrNull()
                    if (featured != null) {
                        FeaturedCard(
                            book = featured,
                            page = prefs.pageOf(featured.id),
                            onClick = { onOpenBook(featured) },
                            modifier = Modifier.padding(horizontal = 22.dp),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
            item {
                SectionHeader(
                    title = "Next events",
                    action = "All events",
                    onAction = onOpenEvents,
                    modifier = Modifier.padding(start = 22.dp, end = 18.dp, bottom = 12.dp),
                )
            }
            item {
                Revealed(index = 2) {
                    NextEventCard(
                        state = events,
                        onOpen = onOpenEvents,
                        onRetry = onRefreshEvents,
                        modifier = Modifier.padding(horizontal = 22.dp),
                    )
                }
            }
            item { Spacer(Modifier.height(26.dp)) }
            item {
                BasicText(
                    text = "Browse",
                    style = Type.Label.copy(color = colors.inkSoft),
                    modifier = Modifier.padding(start = 24.dp, bottom = 14.dp),
                )
            }
            items(3) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .padding(bottom = 20.dp),
                ) {
                    repeat(3) { col ->
                        val index = row * 3 + col
                        val category = Catalog.categories.getOrNull(index)
                        if (category != null) {
                            Box(Modifier.weight(1f)) {
                                CategoryTile(
                                    category = category,
                                    index = index,
                                    onClick = { onCategory(category) },
                                )
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                Footer(lastRead = recent.firstOrNull(), prefs = prefs)
            }
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
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 2.dp),
    ) {
        val interaction = remember { MutableInteractionSource() }
        val scale = rememberPressScale(interaction)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(colors.card.copy(alpha = 0.6f))
                .clickable(interactionSource = interaction, indication = null, onClick = onMenu),
        ) {
            KhIcon(KhIcons.Menu, Modifier.size(21.dp), colors.ink)
        }
        Spacer(Modifier.weight(1f))
        RoundIconButton(KhIcons.Heart, onBookmarks, size = 46, background = colors.card.copy(alpha = 0.6f))
        Spacer(Modifier.width(8.dp))
        RoundIconButton(KhIcons.Share, onShare, size = 46, background = colors.card.copy(alpha = 0.6f))
    }
}

@Composable
private fun HeroMark() {
    val colors = Khaja.colors
    val transition = rememberInfiniteTransition(label = "hero")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(42000, easing = LinearEasing)),
        label = "spin",
    )
    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(4200, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(280.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.mint.copy(alpha = 0.32f), Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.5f,
                ),
                radius = size.minDimension * 0.5f,
            )
            // dashed orbit
            rotate(spin) {
                drawCircle(
                    color = colors.mintDeep.copy(alpha = 0.35f),
                    radius = size.minDimension * 0.44f,
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 11f), 0f),
                    ),
                )
            }
            // slow breathing halo
            drawCircle(
                color = colors.mint.copy(alpha = 0.12f + breathe * 0.06f),
                radius = size.minDimension * (0.30f + breathe * 0.03f),
            )

            // arch
            val w = size.width
            val arch = Path().apply {
                moveTo(w * 0.30f, w * 0.80f)
                lineTo(w * 0.30f, w * 0.48f)
                cubicTo(w * 0.30f, w * 0.30f, w * 0.41f, w * 0.19f, w * 0.5f, w * 0.19f)
                cubicTo(w * 0.59f, w * 0.19f, w * 0.70f, w * 0.30f, w * 0.70f, w * 0.48f)
                lineTo(w * 0.70f, w * 0.80f)
            }
            drawPath(arch, colors.mintDeep, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(6.dp))
            KhIcon(KhIcons.Book, Modifier.size(40.dp), colors.ink)
            Spacer(Modifier.height(58.dp))
            BasicText(
                text = "khajabad",
                style = Type.Display.copy(color = colors.ink, fontSize = 38.sp),
            )
            Spacer(Modifier.height(4.dp))
            BasicText(
                text = "ഖജാബാദ് · ഓഫ്‌ലൈൻ പുസ്തകശാല",
                style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 13.sp),
            )
        }

        KhIcon(
            KhIcons.Star8,
            Modifier
                .align(Alignment.Center)
                .offset(y = (-84).dp)
                .size(18.dp),
            colors.gold,
        )
    }
}

@Composable
private fun RecentChip(book: Book, onClick: () -> Unit) {
    val colors = Khaja.colors
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(100))
            .background(colors.mint)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        BasicText(
            text = book.title,
            style = Type.MalLabel.copy(color = colors.onMint, fontSize = 13.5.sp),
            maxLines = 1,
        )
    }
}

@Composable
private fun GhostChip(onClick: () -> Unit) {
    val colors = Khaja.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(100))
            .border(1.2.dp, colors.line, RoundedCornerShape(100))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
    ) {
        KhIcon(KhIcons.ChevronRight, Modifier.size(16.dp), colors.inkSoft)
    }
}

@Composable
private fun FeaturedCard(book: Book, page: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Khaja.colors
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.98f)
    val progress = ((page + 1).toFloat() / book.pages.coerceAtLeast(1)).coerceIn(0f, 1f)

    val transition = rememberInfiniteTransition(label = "sheen")
    val sweep by transition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
        label = "sweep",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(102.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(colors.mint, colors.mint.copy(alpha = 0.86f))))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
                    start = Offset(size.width * sweep, 0f),
                    end = Offset(size.width * (sweep + 0.22f), size.height),
                )
            )
            drawLine(
                color = colors.onMint.copy(alpha = 0.14f),
                start = Offset(0f, size.height - 10f),
                end = Offset(size.width, size.height - 10f),
                strokeWidth = 2f,
            )
            drawLine(
                color = colors.onMint,
                start = Offset(0f, size.height - 10f),
                end = Offset(size.width * progress, size.height - 10f),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.onMint.copy(alpha = 0.10f)),
            ) {
                KhIcon(KhIcons.Book, Modifier.size(25.dp), colors.onMint)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                BasicText(
                    text = book.title,
                    style = Type.MalTitle.copy(color = colors.onMint, fontSize = 15.sp),
                    maxLines = 1,
                )
                Spacer(Modifier.height(3.dp))
                BasicText(
                    text = if (page > 0) "വായന തുടരുക · പേജ് ${page + 1} / ${book.pages}"
                    else "${book.pages} പേജ് · ${book.latin}",
                    style = Type.MalLabel.copy(
                        color = colors.onMint.copy(alpha = 0.72f),
                        fontSize = 11.5.sp,
                    ),
                    maxLines = 1,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.onMint.copy(alpha = 0.12f)),
            ) {
                KhIcon(KhIcons.ChevronRight, Modifier.size(16.dp), colors.onMint)
            }
        }
    }
}

@Composable
private fun CategoryTile(category: Category, index: Int, onClick: () -> Unit) {
    val colors = Khaja.colors
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Revealed(index = index + 2, delayStep = 40, distance = 30f) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.mint.copy(alpha = 0.98f),
                                colors.mint.copy(alpha = 0.82f),
                            )
                        )
                    )
                    .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(size.width * 0.24f, size.height * 0.16f),
                            radius = size.maxDimension * 0.6f,
                        ),
                        radius = size.maxDimension * 0.6f,
                        center = Offset(size.width * 0.24f, size.height * 0.16f),
                    )
                }
                KhIcon(category.icon, Modifier.size(34.dp), colors.onMint)
            }
        }
        Spacer(Modifier.height(8.dp))
        BasicText(
            text = category.label,
            style = Type.MalLabel.copy(
                color = colors.ink,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Footer(lastRead: Book?, prefs: PrefsState) {
    val colors = Khaja.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        KhIcon(KhIcons.Star8, Modifier.size(14.dp), colors.mintDeep.copy(alpha = 0.6f))
        Spacer(Modifier.height(10.dp))
        BasicText(
            text = "khajabad · ഓഫ്‌ലൈൻ വായന",
            style = Type.Label.copy(color = colors.inkSoft, fontSize = 9.sp),
        )
        if (lastRead != null) {
            Spacer(Modifier.height(4.dp))
            BasicText(
                text = "അവസാനം വായിച്ചത് · ${lastRead.title}",
                style = Type.MalLabel.copy(color = colors.inkSoft.copy(alpha = 0.7f), fontSize = 10.sp),
            )
        }
    }
}
