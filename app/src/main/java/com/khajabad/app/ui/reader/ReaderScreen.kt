package com.khajabad.app.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khajabad.app.core.Book
import com.khajabad.app.core.PdfPages
import com.khajabad.app.ui.components.PdfPageImage
import com.khajabad.app.ui.components.RoundIconButton
import com.khajabad.app.ui.components.ShimmerBox
import com.khajabad.app.ui.theme.KhIcons
import com.khajabad.app.ui.theme.KhIcon
import com.khajabad.app.ui.theme.Khaja
import com.khajabad.app.ui.theme.Motion
import com.khajabad.app.ui.theme.Type
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ReaderScreen(
    book: Book,
    initialPage: Int,
    bookmarked: (Int) -> Boolean,
    onBack: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onToggleBookmark: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Khaja.colors
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val pager = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (book.pages - 1).coerceAtLeast(0)),
    ) { book.pages.coerceAtLeast(1) }

    var chrome by remember { mutableStateOf(true) }
    var night by remember { mutableStateOf(true) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var showPages by remember { mutableStateOf(false) }

    val bg = if (night) Color(0xFF07141A) else colors.paperAlt
    val fg = if (night) Color(0xFFF1EDE3) else colors.ink
    val fgSoft = if (night) Color(0x8FF1EDE3) else colors.inkSoft

    LaunchedEffect(pager.currentPage) {
        onPageChanged(pager.currentPage)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        zoom = 1f
        pan = Offset.Zero
    }

    // Prefetch neighbours so page turns feel instant.
    val context = LocalContext.current
    LaunchedEffect(pager.currentPage) {
        val current = pager.currentPage
        listOf(current + 1, current - 1).forEach { index ->
            if (index in 0 until book.pages) {
                PdfPages.render(context, book, index, 900)
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(bg),
    ) {
        HorizontalPager(
            state = pager,
            userScrollEnabled = zoom <= 1.01f,
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            val pageOffset = ((pager.currentPage - index) + pager.currentPageOffsetFraction)
                .coerceIn(-1f, 1f)
            val abs = abs(pageOffset)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 96.dp)
                    .graphicsLayer {
                        rotationY = pageOffset * -16f
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                        cameraDistance = 16f * density
                        alpha = 1f - abs * 0.35f
                        scaleX = 1f - abs * 0.06f
                        scaleY = 1f - abs * 0.06f
                        translationX = -pageOffset * 26f * density
                    },
            ) {
                FittedPage(
                    book = book,
                    index = index,
                    zoom = if (index == pager.currentPage) zoom else 1f,
                    pan = if (index == pager.currentPage) pan else Offset.Zero,
                    onGesture = { z, p ->
                        zoom = (zoom * z).coerceIn(1f, 4.2f)
                        pan += p
                    },
                )
            }
        }

        // Tap zones: toggle chrome, double tap to zoom.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { chrome = !chrome },
                        onDoubleTap = {
                            if (zoom > 1.05f) {
                                zoom = 1f
                                pan = Offset.Zero
                            } else {
                                zoom = 2.2f
                            }
                        },
                    )
                }
                .then(
                    if (zoom > 1.01f) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, drag ->
                                pan += drag
                                change.consume()
                            }
                        }
                    } else Modifier
                ),
        )

        // Hairline reading progress, always visible.
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (night) Color.White.copy(alpha = 0.08f) else colors.ink.copy(alpha = 0.06f)),
        ) {
            val progress by animateFloatAsState(
                targetValue = ((pager.currentPage + 1).toFloat() / book.pages.coerceAtLeast(1)),
                animationSpec = Motion.fluid(),
                label = "readProgress",
            )
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(colors.mint),
            )
        }

        AnimatedVisibility(
            visible = chrome,
            enter = fadeIn(tween(220)) + slideInVertically(tween(320)) { -it / 2 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(260)) { -it / 2 },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                (if (night) Color.Black else colors.paper).copy(alpha = 0.92f),
                                Color.Transparent,
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(
                        KhIcons.Back,
                        onBack,
                        size = 42,
                        tint = fg,
                        background = if (night) Color.White.copy(alpha = 0.08f) else colors.card,
                        borderColor = if (night) Color.White.copy(alpha = 0.10f) else colors.line,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            text = book.title,
                            style = Type.MalTitle.copy(color = fg, fontSize = 14.sp),
                            maxLines = 1,
                        )
                        BasicText(
                            text = "പേജ് ${pager.currentPage + 1} / ${book.pages}",
                            style = Type.Small.copy(color = fgSoft, fontSize = 10.5.sp),
                        )
                    }
                    RoundIconButton(
                        if (bookmarked(pager.currentPage)) KhIcons.BookmarkFull else KhIcons.Bookmark,
                        { onToggleBookmark(pager.currentPage) },
                        size = 42,
                        tint = if (bookmarked(pager.currentPage)) colors.mint else fg,
                        background = if (night) Color.White.copy(alpha = 0.08f) else colors.card,
                        borderColor = if (night) Color.White.copy(alpha = 0.10f) else colors.line,
                    )
                    Spacer(Modifier.width(8.dp))
                    RoundIconButton(
                        if (night) KhIcons.Sun else KhIcons.Moon,
                        { night = !night },
                        size = 42,
                        tint = fg,
                        background = if (night) Color.White.copy(alpha = 0.08f) else colors.card,
                        borderColor = if (night) Color.White.copy(alpha = 0.10f) else colors.line,
                    )
                    Spacer(Modifier.width(8.dp))
                    RoundIconButton(
                        KhIcons.Grid,
                        { showPages = true },
                        size = 42,
                        tint = fg,
                        background = if (night) Color.White.copy(alpha = 0.08f) else colors.card,
                        borderColor = if (night) Color.White.copy(alpha = 0.10f) else colors.line,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = chrome,
            enter = fadeIn(tween(220)) + slideInVertically(tween(340)) { it / 2 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(260)) { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                (if (night) Color.Black else colors.paper).copy(alpha = 0.94f),
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, top = 34.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    RoundIconButton(
                        KhIcons.Back,
                        { scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) } },
                        size = 44,
                        tint = fg,
                        background = if (night) Color.White.copy(alpha = 0.08f) else colors.card,
                        borderColor = if (night) Color.White.copy(alpha = 0.10f) else colors.line,
                    )
                    PageScrubber(
                        page = pager.currentPage,
                        pages = book.pages,
                        accent = colors.mint,
                        track = if (night) Color.White.copy(alpha = 0.14f) else colors.ink.copy(alpha = 0.10f),
                        modifier = Modifier.weight(1f),
                        onSeek = { target -> scope.launch { pager.animateScrollToPage(target) } },
                    )
                    RoundIconButton(
                        KhIcons.ChevronRight,
                        {
                            scope.launch {
                                pager.animateScrollToPage(
                                    (pager.currentPage + 1).coerceAtMost(book.pages - 1)
                                )
                            }
                        },
                        size = 44,
                        tint = fg,
                        background = if (night) Color.White.copy(alpha = 0.08f) else colors.card,
                        borderColor = if (night) Color.White.copy(alpha = 0.10f) else colors.line,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KhIcon(KhIcons.Book, Modifier.size(12.dp), fgSoft)
                    Spacer(Modifier.width(6.dp))
                    BasicText(
                        text = book.latin.uppercase(),
                        style = Type.Label.copy(color = fgSoft, fontSize = 8.5.sp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(fgSoft)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicText(
                        text = if (zoom > 1.05f) "സൂം ${(zoom * 100).toInt()}%" else "ഇരട്ട ടാപ്പ് · സൂം",
                        style = Type.MalLabel.copy(color = fgSoft, fontSize = 9.5.sp),
                    )
                }
            }
        }

        // Thumbnail sheet
        AnimatedVisibility(
            visible = showPages,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(160)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showPages = false },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.72f)
                        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                        .background(if (night) Color(0xFF0C232B) else colors.paper)
                        .clickable(enabled = false) {}
                        .navigationBarsPadding()
                        .padding(vertical = 18.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 6.dp),
                    ) {
                        BasicText(
                            text = "പേജുകൾ",
                            style = Type.MalH2.copy(
                                color = if (night) Color(0xFFF1EDE3) else colors.ink,
                                fontSize = 17.sp,
                            ),
                        )
                        Spacer(Modifier.weight(1f))
                        BasicText(
                            text = "${book.pages}",
                            style = Type.Small.copy(color = colors.mintDeep),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(book.pages) { index ->
                            val active = index == pager.currentPage
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(96.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .border(
                                            width = if (active) 2.dp else 1.dp,
                                            color = if (active) colors.mint else Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                        .clickable {
                                            showPages = false
                                            scope.launch { pager.animateScrollToPage(index) }
                                        },
                                ) {
                                    PdfPageImage(
                                        book = book,
                                        page = index,
                                        targetWidth = 240,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                Spacer(Modifier.height(5.dp))
                                BasicText(
                                    text = "${index + 1}",
                                    style = Type.Small.copy(
                                        color = if (active) colors.mint else Color(0xFF9FB0B5),
                                        fontSize = 10.sp,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Page card that fits the viewport and preserves the PDF page aspect ratio.
 */
@Composable
private fun FittedPage(
    book: Book,
    index: Int,
    zoom: Float,
    pan: Offset,
    onGesture: (Float, Offset) -> Unit,
) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, book.id, index) {
        value = PdfPages.render(context, book, index, 1200)
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var multi = false
                    var event = awaitPointerEvent()
                    do {
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount > 1) multi = true
                        if (multi) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                onGesture(zoomChange, panChange)
                                event.changes.forEach { it.consume() }
                            }
                        }
                        event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val ratio = image?.let { it.width.toFloat() / it.height.toFloat() } ?: 0.72f
        val fitWidth = if (maxWidth.value * ratio > maxHeight.value) maxHeight * ratio else maxWidth
        val cardWidth = fitWidth
        val cardHeight = cardWidth / ratio

        val density = LocalDensity.current
        val maxPanX = with(density) { (cardWidth * (zoom - 1f) / 2f).toPx() }
        val maxPanY = with(density) { (cardHeight * (zoom - 1f) / 2f).toPx() }
        val panX = pan.x.coerceIn(-maxPanX, maxPanX)
        val panY = pan.y.coerceIn(-maxPanY, maxPanY)

        Box(
            Modifier
                .size(cardWidth, cardHeight)
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = panX
                    translationY = panY
                }
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White),
        ) {
            val bitmap = image
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Page ${index + 1}",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ShimmerBox(Modifier.fillMaxSize(), RoundedCornerShape(14.dp))
            }
        }
    }
}

@Composable
private fun PageScrubber(
    page: Int,
    pages: Int,
    accent: Color,
    track: Color,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val denominator = (pages - 1).coerceAtLeast(1)
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val fraction = if (dragFraction >= 0f) dragFraction else page.toFloat() / denominator

    BoxWithConstraints(
        modifier = modifier
            .height(40.dp)
            .pointerInput(pages) {
                detectTapGestures { offset ->
                    val f = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((f * denominator).toInt())
                }
            }
            .pointerInput(pages) {
                detectDragGestures(
                    onDragStart = { offset -> dragFraction = (offset.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = {
                        val target = (dragFraction * denominator).toInt().coerceIn(0, pages - 1)
                        dragFraction = -1f
                        onSeek(target)
                    },
                    onDragCancel = { dragFraction = -1f },
                    onDrag = { change, _ ->
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        change.consume()
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(track),
        )
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(accent),
        )
        Box(
            Modifier
                .offset(x = maxWidth * fraction.coerceIn(0f, 1f) - 7.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(accent)
                .border(2.dp, Color.Black.copy(alpha = 0.15f), CircleShape),
        )
    }
}
