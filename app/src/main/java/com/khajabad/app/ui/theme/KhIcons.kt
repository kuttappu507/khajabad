package com.khajabad.app.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * All glyphs are drawn as code — no icon font, no Material icons.
 * Stroke colour is always black so [KhIcon] can tint the whole painter.
 */
private fun lineIcon(name: String, strokeWidth: Float = 1.7f, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }.build()

private fun fillIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black), pathBuilder = block)
    }.build()

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + r, y1 = cy)
    arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - r, y1 = cy)
    close()
}

private fun PathBuilder.star(cx: Float, cy: Float, outer: Float, inner: Float, points: Int) {
    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = -PI / 2 + i * PI / points
        val x = cx + (radius * cos(angle)).toFloat()
        val y = cy + (radius * sin(angle)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

object KhIcons {

    val Menu = lineIcon("menu", 1.8f) {
        moveTo(3.6f, 7.2f); lineTo(20.4f, 7.2f)
        moveTo(3.6f, 12f); lineTo(15.2f, 12f)
        moveTo(3.6f, 16.8f); lineTo(20.4f, 16.8f)
    }

    val Heart = lineIcon("heart") {
        moveTo(12f, 20.5f)
        cubicTo(8.6f, 17.9f, 3f, 13.9f, 3f, 9.4f)
        cubicTo(3f, 6.6f, 5.1f, 4.5f, 7.8f, 4.5f)
        cubicTo(9.6f, 4.5f, 11.2f, 5.5f, 12f, 7f)
        cubicTo(12.8f, 5.5f, 14.4f, 4.5f, 16.2f, 4.5f)
        cubicTo(18.9f, 4.5f, 21f, 6.6f, 21f, 9.4f)
        cubicTo(21f, 13.9f, 15.4f, 17.9f, 12f, 20.5f)
        close()
    }

    val HeartFull = fillIcon("heart_full") {
        moveTo(12f, 20.5f)
        cubicTo(8.6f, 17.9f, 3f, 13.9f, 3f, 9.4f)
        cubicTo(3f, 6.6f, 5.1f, 4.5f, 7.8f, 4.5f)
        cubicTo(9.6f, 4.5f, 11.2f, 5.5f, 12f, 7f)
        cubicTo(12.8f, 5.5f, 14.4f, 4.5f, 16.2f, 4.5f)
        cubicTo(18.9f, 4.5f, 21f, 6.6f, 21f, 9.4f)
        cubicTo(21f, 13.9f, 15.4f, 17.9f, 12f, 20.5f)
        close()
    }

    val Share = lineIcon("share") {
        moveTo(12f, 15.4f); lineTo(12f, 3.8f)
        moveTo(8.2f, 7.4f); lineTo(12f, 3.6f); lineTo(15.8f, 7.4f)
        moveTo(5.6f, 12.4f); lineTo(5.6f, 19.6f); lineTo(18.4f, 19.6f); lineTo(18.4f, 12.4f)
    }

    val Search = lineIcon("search") {
        circle(11f, 11f, 6.6f)
        moveTo(15.9f, 15.9f); lineTo(20.6f, 20.6f)
    }

    val Home = lineIcon("home") {
        moveTo(4f, 10.6f); lineTo(12f, 3.8f); lineTo(20f, 10.6f); lineTo(20f, 20f)
        lineTo(4f, 20f); close()
        moveTo(9.6f, 20f); lineTo(9.6f, 14.4f); lineTo(14.4f, 14.4f); lineTo(14.4f, 20f)
    }

    val Grid = lineIcon("grid") {
        moveTo(3.8f, 3.8f); lineTo(10.2f, 3.8f); lineTo(10.2f, 10.2f); lineTo(3.8f, 10.2f); close()
        moveTo(13.8f, 3.8f); lineTo(20.2f, 3.8f); lineTo(20.2f, 10.2f); lineTo(13.8f, 10.2f); close()
        moveTo(3.8f, 13.8f); lineTo(10.2f, 13.8f); lineTo(10.2f, 20.2f); lineTo(3.8f, 20.2f); close()
        moveTo(13.8f, 13.8f); lineTo(20.2f, 13.8f); lineTo(20.2f, 20.2f); lineTo(13.8f, 20.2f); close()
    }

    val Beads = lineIcon("beads") {
        val cx = 12f; val cy = 12f; val r = 7.2f
        for (i in 0 until 10) {
            val a = -PI / 2 + i * (2 * PI / 10)
            circle(cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat(), 1.35f)
        }
        circle(12f, 12f, 1.5f)
    }

    val Book = lineIcon("book") {
        moveTo(3f, 6.2f)
        cubicTo(6.5f, 4.6f, 9.5f, 5f, 12f, 6.8f)
        cubicTo(14.5f, 5f, 17.5f, 4.6f, 21f, 6.2f)
        lineTo(21f, 18.6f)
        cubicTo(17.5f, 17f, 14.5f, 17.4f, 12f, 19.2f)
        cubicTo(9.5f, 17.4f, 6.5f, 17f, 3f, 18.6f)
        close()
        moveTo(12f, 6.8f); lineTo(12f, 19.2f)
    }

    val Bookmark = lineIcon("bookmark") {
        moveTo(6.6f, 3.8f); lineTo(17.4f, 3.8f); lineTo(17.4f, 20.4f)
        lineTo(12f, 16.2f); lineTo(6.6f, 20.4f); close()
    }

    val BookmarkFull = fillIcon("bookmark_full") {
        moveTo(6.6f, 3.8f); lineTo(17.4f, 3.8f); lineTo(17.4f, 20.4f)
        lineTo(12f, 16.2f); lineTo(6.6f, 20.4f); close()
    }

    val Back = lineIcon("back") {
        moveTo(14.4f, 4.6f); lineTo(6.8f, 12f); lineTo(14.4f, 19.4f)
    }

    val ChevronRight = lineIcon("chevron_right") {
        moveTo(9.4f, 5.4f); lineTo(16f, 12f); lineTo(9.4f, 18.6f)
    }

    val Close = lineIcon("close") {
        moveTo(5.6f, 5.6f); lineTo(18.4f, 18.4f)
        moveTo(18.4f, 5.6f); lineTo(5.6f, 18.4f)
    }

    val Moon = fillIcon("moon") {
        moveTo(20.4f, 14.2f)
        cubicTo(18.9f, 18.9f, 14.1f, 21.6f, 9.4f, 20.1f)
        cubicTo(4.7f, 18.6f, 2f, 13.8f, 3.5f, 9.1f)
        cubicTo(4.7f, 5.4f, 8f, 3.1f, 11.6f, 3f)
        cubicTo(9.4f, 5.4f, 8.8f, 8.9f, 10.2f, 12f)
        cubicTo(11.6f, 15.1f, 14.9f, 16.9f, 18.4f, 16.5f)
        close()
    }

    val Sun = lineIcon("sun") {
        circle(12f, 12f, 4.4f)
        for (i in 0 until 8) {
            val a = i * PI / 4
            val c = cos(a).toFloat(); val s = sin(a).toFloat()
            moveTo(12 + c * 7.2f, 12 + s * 7.2f)
            lineTo(12 + c * 9.4f, 12 + s * 9.4f)
        }
    }

    val Plus = lineIcon("plus") {
        moveTo(12f, 5.4f); lineTo(12f, 18.6f)
        moveTo(5.4f, 12f); lineTo(18.6f, 12f)
    }

    val Minus = lineIcon("minus") { moveTo(5.4f, 12f); lineTo(18.6f, 12f) }

    val Sparkle = fillIcon("sparkle") {
        moveTo(12f, 2.6f)
        cubicTo(12.9f, 8.1f, 15.9f, 11.1f, 21.4f, 12f)
        cubicTo(15.9f, 12.9f, 12.9f, 15.9f, 12f, 21.4f)
        cubicTo(11.1f, 15.9f, 8.1f, 12.9f, 2.6f, 12f)
        cubicTo(8.1f, 11.1f, 11.1f, 8.1f, 12f, 2.6f)
        close()
    }

    val Star8 = fillIcon("star8") { star(12f, 12f, 9.4f, 4.4f, 8) }

    val Dome = lineIcon("dome") {
        moveTo(12f, 2.6f); lineTo(12f, 4.6f)
        moveTo(4.6f, 20.4f); lineTo(19.4f, 20.4f)
        moveTo(6.4f, 20.4f); lineTo(6.4f, 13.6f)
        cubicTo(6.4f, 9.4f, 9.2f, 6.6f, 12f, 6.6f)
        cubicTo(14.8f, 6.6f, 17.6f, 9.4f, 17.6f, 13.6f)
        lineTo(17.6f, 20.4f)
        moveTo(10f, 20.4f); lineTo(10f, 16.6f)
        cubicTo(10f, 15.2f, 10.9f, 14.2f, 12f, 14.2f)
        cubicTo(13.1f, 14.2f, 14f, 15.2f, 14f, 16.6f)
        lineTo(14f, 20.4f)
    }

    val Mihrab = lineIcon("mihrab") {
        moveTo(5.4f, 20.6f); lineTo(5.4f, 11.2f)
        cubicTo(5.4f, 7.4f, 8.3f, 4.6f, 12f, 4.6f)
        cubicTo(15.7f, 4.6f, 18.6f, 7.4f, 18.6f, 11.2f)
        lineTo(18.6f, 20.6f)
        moveTo(8.6f, 20.6f); lineTo(8.6f, 11.6f)
        cubicTo(8.6f, 9.3f, 10.1f, 7.7f, 12f, 7.7f)
        cubicTo(13.9f, 7.7f, 15.4f, 9.3f, 15.4f, 11.6f)
        lineTo(15.4f, 20.6f)
        moveTo(3.4f, 20.6f); lineTo(20.6f, 20.6f)
    }

    val Hands = lineIcon("hands") {
        moveTo(4.2f, 20.6f); lineTo(4.2f, 10.6f)
        cubicTo(4.2f, 9.1f, 5.6f, 8.1f, 7f, 8.8f)
        lineTo(9.5f, 10.3f); lineTo(9.5f, 20.6f)
        moveTo(19.8f, 20.6f); lineTo(19.8f, 10.6f)
        cubicTo(19.8f, 9.1f, 18.4f, 8.1f, 17f, 8.8f)
        lineTo(14.5f, 10.3f); lineTo(14.5f, 20.6f)
        moveTo(4.2f, 14.4f); lineTo(9.5f, 14.4f)
        moveTo(19.8f, 14.4f); lineTo(14.5f, 14.4f)
    }

    val Scroll = lineIcon("scroll") {
        moveTo(6.8f, 3.6f); lineTo(15.6f, 3.6f); lineTo(19.8f, 7.8f)
        lineTo(19.8f, 20.4f); lineTo(6.8f, 20.4f); close()
        moveTo(15.2f, 3.8f); lineTo(15.2f, 8.2f); lineTo(19.6f, 8.2f)
        moveTo(9.6f, 12.4f); lineTo(16.4f, 12.4f)
        moveTo(9.6f, 16f); lineTo(16.4f, 16f)
    }

    val Lantern = lineIcon("lantern") {
        moveTo(12f, 2f); lineTo(12f, 3.4f)
        moveTo(9f, 3.6f); lineTo(15f, 3.6f); lineTo(14.2f, 6f); lineTo(9.8f, 6f); close()
        moveTo(8f, 8.4f); lineTo(16f, 8.4f); lineTo(17.2f, 18f); lineTo(6.8f, 18f); close()
        moveTo(9.6f, 20.6f); lineTo(14.4f, 20.6f)
        moveTo(9.8f, 11.2f); lineTo(9.8f, 15.2f)
        moveTo(12f, 11.2f); lineTo(12f, 15.2f)
        moveTo(14.2f, 11.2f); lineTo(14.2f, 15.2f)
    }

    val Dots = fillIcon("dots") {
        circle(5.6f, 12f, 1.9f)
        circle(12f, 12f, 1.9f)
        circle(18.4f, 12f, 1.9f)
    }

    val Bait = fillIcon("bait") {
        star(11.2f, 11.2f, 8.2f, 3.9f, 8)
        circle(12f, 12f, 4.6f)
    }

    val Refresh = lineIcon("refresh") {
        moveTo(19.6f, 12f)
        arcTo(7.6f, 7.6f, 0f, isMoreThanHalf = true, isPositiveArc = false, x1 = 7.2f, y1 = 5.6f)
        moveTo(6.4f, 2.6f); lineTo(6.4f, 6.4f); lineTo(10.2f, 6.4f)
    }

    val Check = lineIcon("check") {
        moveTo(5f, 12.6f); lineTo(9.6f, 17.2f); lineTo(19.2f, 7.2f)
    }

    val Info = lineIcon("info") {
        circle(12f, 12f, 8.4f)
        moveTo(12f, 11f); lineTo(12f, 16.4f)
        moveTo(12f, 7.9f); lineTo(12f, 8.1f)
    }

    val ArrowUp = lineIcon("arrow_up") {
        moveTo(12f, 19f); lineTo(12f, 5.4f)
        moveTo(6.6f, 10.8f); lineTo(12f, 5.4f); lineTo(17.4f, 10.8f)
    }
}

@Composable
fun KhIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Khaja.colors.ink,
    description: String? = null,
) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = description,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier,
    )
}
