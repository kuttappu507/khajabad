package com.khajabad.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.style.TextAlign
import com.khajabad.app.core.Book
import com.khajabad.app.core.PdfPages
import com.khajabad.app.ui.theme.KhIcon
import com.khajabad.app.ui.theme.KhIcons
import com.khajabad.app.ui.theme.Khaja
import com.khajabad.app.ui.theme.Motion
import com.khajabad.app.ui.theme.Type
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/* -------------------------------------------------------------- press feel */

@Composable
fun rememberPressScale(
    interaction: MutableInteractionSource,
    pressed: Float = 0.955f,
): Float {
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = Motion.snappy(),
        label = "press",
    )
    return scale
}

fun Modifier.softShadow(elevation: Float = 12f, radius: Float = 28f, color: Color = Color(0x22000000)) =
    this.then(
        Modifier.graphicsLayer {
            shadowElevation = elevation.dp.toPx()
            shape = RoundedCornerShape(radius.dp)
            clip = false
            ambientShadowColor = color
            spotShadowColor = color
        }
    )

/* ------------------------------------------------------------------ atoms */

@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Type.Label,
    color: Color = Khaja.colors.inkSoft,
    maxLines: Int = 1,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun KhajaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = true,
    enabled: Boolean = true,
    contentColor: Color = if (filled) Khaja.colors.onMint else Khaja.colors.ink,
    bg: Color = if (filled) Khaja.colors.mint else Color.Transparent,
    style: TextStyle = Type.Title,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    val shape = RoundedCornerShape(100)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.45f }
            .clip(shape)
            .background(bg)
            .then(
                if (filled) Modifier
                else Modifier.border(1.5.dp, Khaja.colors.line, shape)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) {
        if (icon != null) {
            KhIcon(icon, Modifier.size(18.dp), contentColor)
            Spacer(Modifier.width(10.dp))
        }
        BasicText(text = text, style = style.copy(color = contentColor), maxLines = 1)
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 44,
    tint: Color = Khaja.colors.ink,
    background: Color = Khaja.colors.card.copy(alpha = 0.72f),
    borderColor: Color = Khaja.colors.line,
    description: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(background)
            .border(1.dp, borderColor, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        KhIcon(icon, Modifier.size((size * 0.42f).dp), tint, description)
    }
}

/** Entrance animation used across screens: rise + fade, index-staggered. */
@Composable
fun Revealed(
    index: Int = 0,
    delayStep: Int = 55,
    distance: Float = 26f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index.toLong() * delayStep)
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = Motion.bouncy(),
        label = "reveal",
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.coerceIn(0f, 1f)
            translationY = (1f - progress) * distance
        },
    ) { content() }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(
            text = title,
            style = Type.H2.copy(color = Khaja.colors.ink),
        )
        if (action != null) {
            val interaction = remember { MutableInteractionSource() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(100))
                    .clickable(interactionSource = interaction, indication = null, onClick = onAction)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                BasicText(text = action, style = Type.Small.copy(color = Khaja.colors.inkSoft))
                Spacer(Modifier.width(2.dp))
                KhIcon(KhIcons.ChevronRight, Modifier.size(14.dp), Khaja.colors.inkSoft)
            }
        }
    }
}

/* ----------------------------------------------------------------- shimmer */

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(24.dp)) {
    val colors = Khaja.colors
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shift",
    )
    Box(
        modifier
            .clip(shape)
            .background(colors.paperAlt)
            .drawWithContent {
                drawContent()
                val w = size.width
                val start = shift * w
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            colors.card.copy(alpha = 0.9f),
                            Color.Transparent,
                        ),
                        start = Offset(start, 0f),
                        end = Offset(start + w * 0.6f, size.height),
                    )
                )
            }
    )
}

/* ------------------------------------------------------------------- pages */

private sealed interface PageState {
    data object Loading : PageState
    data class Ready(val image: ImageBitmap) : PageState
    data object Failed : PageState
}

private val memoPages = object : LinkedHashMap<String, ImageBitmap>() {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean = size > 3
}

@Composable
fun PdfPageImage(
    book: Book,
    page: Int,
    modifier: Modifier = Modifier,
    targetWidth: Int = 1080,
    contentScale: ContentScale = ContentScale.Fit,
    showPlaceholder: Boolean = true,
    onLoaded: ((Float) -> Unit)? = null,
) {
    val context = LocalContext.current
    val state by produceState<PageState>(PageState.Loading, book.id, page, targetWidth) {
        val cached = memoPages["${book.id}:$page"]
        if (cached != null) {
            value = PageState.Ready(cached)
            onLoaded?.invoke(cached.width.toFloat() / cached.height.toFloat())
            return@produceState
        }
        val image = PdfPages.render(context, book, page, targetWidth)
        value = if (image != null) {
            memoPages["${book.id}:$page"] = image
            onLoaded?.invoke(image.width.toFloat() / image.height.toFloat())
            PageState.Ready(image)
        } else {
            PageState.Failed
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (val s = state) {
            is PageState.Ready -> Image(
                bitmap = s.image,
                contentDescription = "Page ${page + 1}",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
            PageState.Loading -> if (showPlaceholder) {
                ShimmerBox(Modifier.fillMaxSize(), RoundedCornerShape(18.dp))
            }
            PageState.Failed -> if (showPlaceholder) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    KhIcon(KhIcons.Info, Modifier.size(22.dp), Khaja.colors.inkSoft)
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        text = "പേജ് ലഭ്യമല്ല",
                        style = Type.MalLabel.copy(color = Khaja.colors.inkSoft),
                    )
                }
            }
        }
    }
}

/** Fills width, sized by the page aspect ratio once known. */
@Composable
fun PdfPageCard(
    book: Book,
    page: Int,
    modifier: Modifier = Modifier,
    corner: Int = 20,
    targetWidth: Int = 1200,
) {
    var ratio by remember(book.id, page) { mutableStateOf(0.72f) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner.dp))
            .background(Color.White),
    ) {
        PdfPageImage(
            book = book,
            page = page,
            modifier = Modifier.fillMaxSize(),
            targetWidth = targetWidth,
            contentScale = ContentScale.Fit,
            onLoaded = { r -> if (r > 0f) ratio = r },
        )
    }
}

/* -------------------------------------------------------------- bottom bar */

data class BarDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun KhajaBottomBar(
    destinations: List<BarDestination>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Khaja.colors
    val shape = RoundedCornerShape(34.dp)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp)
            .softShadow(elevation = 24f, radius = 34f)
            .clip(shape)
            .background(colors.card.copy(alpha = 0.97f))
            .border(1.dp, colors.line, shape)
            .padding(7.dp),
    ) {
        val itemWidth = maxWidth / destinations.size
        val indicator by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = Motion.fluid(),
            label = "bar",
        )
        Box(
            Modifier
                .offset(x = itemWidth * indicator)
                .width(itemWidth)
                .height(54.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(colors.mint),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            destinations.forEachIndexed { index, dest ->
                val selected = index == selectedIndex
                val interaction = remember { MutableInteractionSource() }
                val scale = rememberPressScale(interaction, 0.9f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .width(itemWidth)
                        .height(54.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(26.dp))
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = { onSelect(index) },
                        ),
                ) {
                    KhIcon(
                        dest.icon,
                        Modifier.size(20.dp),
                        if (selected) colors.onMint else colors.inkSoft,
                    )
                    Spacer(Modifier.height(2.dp))
                    BasicText(
                        text = dest.label,
                        style = Type.Small.copy(
                            color = if (selected) colors.onMint else colors.inkSoft,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ toast */

@Composable
fun ToastPill(text: String, modifier: Modifier = Modifier) {
    val colors = Khaja.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(100))
            .background(colors.ink)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        KhIcon(KhIcons.Check, Modifier.size(15.dp), colors.mint)
        Spacer(Modifier.width(9.dp))
        BasicText(
            text = text,
            style = Type.MalLabel.copy(color = colors.paper, fontSize = 13.sp),
        )
    }
}

/** Rotating eight-point star used as a decorative accent. */
@Composable
fun SpinningStar(
    modifier: Modifier = Modifier,
    color: Color = Khaja.colors.gold,
    durationMillis: Int = 26000,
) {
    val transition = rememberInfiniteTransition(label = "star")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis, easing = androidx.compose.animation.core.LinearEasing)),
        label = "angle",
    )
    KhIcon(KhIcons.Star8, modifier.graphicsLayer { rotationZ = angle }, color)
}
