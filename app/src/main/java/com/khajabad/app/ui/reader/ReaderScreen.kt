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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khajabad.app.core.Book
import com.khajabad.app.core.PdfPages
import com.khajabad.app.ui.components.PdfPageImage
import com.khajabad.app.ui.components.RoundIconButton
import com.khajabad.app.ui.components.ShimmerBox
import com.khajabad.app.ui.theme.KhIcon
import com.khajabad.app.ui.theme.KhIcons
import com.khajabad.app.ui.theme.Khaja
import com.khajabad.app.ui.theme.Motion
import com.khajabad.app.ui.theme.Type
import kotlinx.coroutines.launch

/**
 * The reader: one continuous, full-bleed vertical scroll of PDF pages.
 *
 * This is the "native document" model rather than a page-flip carousel — you
 * flick through the book the way you scroll a web page, the pages fill the
 * screen edge to edge, and the chrome fades away when you start reading.
 *
 * Gestures:
 *  * single tap     — show / hide the chrome
 *  * pinch          — zoom (1×–4×); the scroll locks while zoomed
 *  * one finger     — pan while zoomed, scroll while not
 *  * double tap     — jump between 1× and 2.5×
 */
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
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val pageCount = book.pages.coerceAtLeast(1)
    val startPage = initialPage.coerceIn(0, pageCount - 1)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startPage)

    var chrome by remember { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showPages by remember { mutableStateOf(false) }
    var daylight by remember { mutableStateOf(false) }

    val current by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val zoomed = scale > 1.01f

    // Render at the real screen width so text is crisp at 1× without decoding
    // anything larger than the device can show.
    val targetWidth = with(density) {
        (configuration.screenWidthDp.dp.toPx()).toInt().coerceIn(720, 2000)
    }

    val pageBg = if (daylight) Color(0xFFE8E8EF) else Color(0xFF07070D)
    val fg = if (daylight) Color(0xFF14142A) else Color(0xFFF3F3FA)
    val fgSoft = if (daylight) Color(0x9914142A) else Color(0x99F3F3FA)

    LaunchedEffect(current) {
        onPageChanged(current)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        // Neighbour prefetch keeps the scroll from showing placeholders.
        listOf(current + 1, current - 1).forEach { index ->
            if (index in 0 until pageCount) PdfPages.render(context, book, index, targetWidth)
        }
    }

    LaunchedEffect(zoomed) {
        if (!zoomed) offset = Offset.Zero
    }

    fun setZoom(target: Float, focus: Offset = Offset.Zero) {
        val next = target.coerceIn(1f, 4f)
        scale = next
        offset = if (next <= 1.01f) Offset.Zero else focus
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(pageBg),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        fun clampPan(value: Offset, at: Float): Offset {
            val maxX = (widthPx * (at - 1f)) / 2f
            val maxY = (heightPx * (at - 1f)) / 2f
            return Offset(
                value.x.coerceIn(-maxX.coerceAtLeast(0f), maxX.coerceAtLeast(0f)),
                value.y.coerceIn(-maxY.coerceAtLeast(0f), maxY.coerceAtLeast(0f)),
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        ) {
            LazyColumn(
                state = listState,
                userScrollEnabled = !zoomed,
                contentPadding = PaddingValues(vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(pageCount) { index ->
                    ReaderPage(
                        book = book,
                        index = index,
                        targetWidth = targetWidth,
                        zoomed = zoomed,
                        onTap = { chrome = !chrome },
                        onDoubleTap = {
                            if (zoomed) setZoom(1f) else setZoom(2.5f)
                        },
                        onPinch = { factor ->
                            val next = (scale * factor).coerceIn(1f, 4f)
                            scale = next
                            offset = clampPan(offset, next)
                        },
                        onPan = { delta -> offset = clampPan(offset + delta, scale) },
                    )
                }
            }
        }

        // Hairline progress, always visible.
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(fg.copy(alpha = 0.10f)),
        ) {
            val progress by animateFloatAsState(
                targetValue = ((current + 1).toFloat() / pageCount).coerceIn(0f, 1f),
                animationSpec = Motion.fluid(),
                label = "readProgress",
            )
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(2.dp)
                    .background(colors.accent),
            )
        }

        AnimatedVisibility(
            visible = chrome,
            enter = fadeIn(tween(220)) + slideInVertically(tween(320)) { -it / 3 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(260)) { -it / 3 },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(pageBg.copy(alpha = 0.96f), pageBg.copy(alpha = 0.72f), Color.Transparent),
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(
                        KhIcons.Back,
                        onBack,
                        size = 42,
                        tint = fg,
                        background = fg.copy(alpha = 0.10f),
                        borderColor = Color.Transparent,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            text = book.title,
                            style = Type.MalTitle.copy(color = fg, fontSize = 14.sp),
                            maxLines = 1,
                        )
                        BasicText(
                            text = "${book.latin} · പേജ് ${current + 1} / $pageCount",
                            style = Type.Small.copy(color = fgSoft, fontSize = 10.sp),
                            maxLines = 1,
                        )
                    }
                    RoundIconButton(
                        KhIcons.Grid,
                        { showPages = true },
                        size = 42,
                        tint = fg,
                        background = fg.copy(alpha = 0.10f),
                        borderColor = Color.Transparent,
                    )
                    Spacer(Modifier.width(8.dp))
                    RoundIconButton(
                        if (daylight) KhIcons.Moon else KhIcons.Sun,
                        { daylight = !daylight },
                        size = 42,
                        tint = fg,
                        background = fg.copy(alpha = 0.10f),
                        borderColor = Color.Transparent,
                    )
                    Spacer(Modifier.width(8.dp))
                    RoundIconButton(
                        if (bookmarked(current)) KhIcons.HeartFull else KhIcons.Heart,
                        { onToggleBookmark(current) },
                        size = 42,
                        tint = if (bookmarked(current)) colors.amber else fg,
                        background = fg.copy(alpha = 0.10f),
                        borderColor = Color.Transparent,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = chrome,
            enter = fadeIn(tween(220)) + slideInVertically(tween(320)) { it / 2 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(240)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, pageBg.copy(alpha = 0.82f), pageBg.copy(alpha = 0.97f)),
                        )
                    )
                    .padding(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        text = "${current + 1}",
                        style = Type.H2.copy(color = fg, fontSize = 18.sp),
                    )
                    BasicText(
                        text = " / $pageCount",
                        style = Type.Small.copy(color = fgSoft, fontSize = 11.sp),
                    )
                    Spacer(Modifier.weight(1f))
                    BasicText(
                        text = if (zoomed) "സൂം ${(scale * 100).toInt()}% · സമ്പൂർണ്ണ വീതി"
                        else "തുടർച്ചയായ സ്ക്രോൾ · സമ്പൂർണ്ണ വീതി",
                        style = Type.MalLabel.copy(color = fgSoft, fontSize = 9.5.sp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                PageSeek(
                    page = current,
                    pages = pageCount,
                    accent = colors.accent,
                    track = fg.copy(alpha = 0.16f),
                    thumb = pageBg,
                    onSeek = { target ->
                        scope.launch { listState.scrollToItem(target.coerceIn(0, pageCount - 1)) }
                    },
                )
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
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showPages = false },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.74f)
                        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                        .background(colors.surface)
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
                            style = Type.MalH2.copy(color = colors.text, fontSize = 17.sp),
                        )
                        Spacer(Modifier.weight(1f))
                        BasicText(
                            text = "${current + 1} / $pageCount",
                            style = Type.Small.copy(color = colors.accentDeep),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(pageCount) { index ->
                            val active = index == current
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(96.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .border(
                                            width = if (active) 2.dp else 1.dp,
                                            color = if (active) colors.accent else colors.line,
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                        .clickable {
                                            showPages = false
                                            scope.launch { listState.animateScrollToItem(index) }
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
                                        color = if (active) colors.accentDeep else colors.textSoft,
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
 * One page of the book: full width, height from the real PDF aspect ratio, with
 * tap / pinch / pan routing.
 */
@Composable
private fun ReaderPage(
    book: Book,
    index: Int,
    targetWidth: Int,
    zoomed: Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onPinch: (Float) -> Unit,
    onPan: (Offset) -> Unit,
) {
    val context = LocalContext.current
    var ratio by remember(book.id) { mutableFloatStateOf(DEFAULT_RATIO) }

    val image by produceState<ImageBitmap?>(initialValue = null, book.id, index, targetWidth) {
        val bitmap = PdfPages.render(context, book, index, targetWidth)
        if (bitmap != null && bitmap.height > 0) {
            ratio = (bitmap.width.toFloat() / bitmap.height.toFloat()).coerceIn(0.2f, 2f)
        }
        value = bitmap
    }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .background(Color.White)
            .pointerInput(zoomed) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var event = awaitPointerEvent()
                    do {
                        val pressed = event.changes.count { it.pressed }
                        if (pressed > 1) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            if (zoomChange != 1f) {
                                onPinch(zoomChange)
                                event.changes.forEach { it.consume() }
                            } else if (panChange != Offset.Zero) {
                                onPan(panChange)
                                event.changes.forEach { it.consume() }
                            }
                        } else if (pressed == 1 && zoomed) {
                            val panChange = event.calculatePan()
                            if (panChange != Offset.Zero) {
                                onPan(panChange)
                                event.changes.forEach { it.consume() }
                            }
                        }
                        event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { onDoubleTap() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ShimmerBox(Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
        }
    }
}

/** Thin seek bar used under the reader; tap or drag to jump pages. */
@Composable
private fun PageSeek(
    page: Int,
    pages: Int,
    accent: Color,
    track: Color,
    thumb: Color,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val denominator = (pages - 1).coerceAtLeast(1)
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val base = if (pages <= 1) 0f else page.toFloat() / denominator
    val fraction = if (dragFraction >= 0f) dragFraction else base

    BoxWithConstraints(
        modifier = modifier
            .height(34.dp)
            .pointerInput(pages) {
                detectTapGestures { offset ->
                    val f = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((f * denominator).toInt().coerceIn(0, pages - 1))
                }
            }
            .pointerInput(pages) {
                androidx.compose.foundation.gestures.detectDragGestures(
                    onDragStart = { offset ->
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
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
                .offset(x = maxWidth * fraction.coerceIn(0f, 1f) - 6.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(accent)
                .border(2.dp, thumb, CircleShape),
        )
    }
}

private const val DEFAULT_RATIO = 0.707f
