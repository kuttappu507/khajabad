package com.khajabad.app.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khajabad.app.R

/**
 * khajabad design system.
 *
 * Deliberately hand-rolled: no Material theme, no Material components.
 * Everything below is plain Compose (foundation + ui + animation).
 */

@Immutable
data class KhajaColors(
    /** Page background. */
    val bg: Color,
    /** Slightly recessed background for grouped rows and shelves. */
    val bgAlt: Color,
    /** Raised surfaces: sheets, cards, the nav bar. */
    val surface: Color,
    /** Primary text. */
    val text: Color,
    /** Secondary text, icons, captions. */
    val textSoft: Color,
    /** Brand accent — indigo/violet. */
    val accent: Color,
    val accentDeep: Color,
    /** Tinted accent background (chips, selected rows). */
    val accentSoft: Color,
    /** Foreground drawn on top of [accent]. */
    val onAccent: Color,
    /** Warm secondary accent, used sparingly for dates and stars. */
    val amber: Color,
    val line: Color,
    val scrim: Color,
    val isDark: Boolean,
)

/**
 * "Indigo paper" — a cool, modern light theme. Not the cream/mint of the old
 * reference: near-white lilac paper, inky indigo type, one confident violet
 * accent and a peach highlight.
 */
val LightKhaja = KhajaColors(
    bg = Color(0xFFF5F5FA),
    bgAlt = Color(0xFFEBEBF4),
    surface = Color(0xFFFFFFFF),
    text = Color(0xFF12132A),
    textSoft = Color(0xFF6A6D8A),
    accent = Color(0xFF6C5CE7),
    accentDeep = Color(0xFF5344C9),
    accentSoft = Color(0xFFEBE8FE),
    onAccent = Color(0xFFFFFFFF),
    amber = Color(0xFFF5A46B),
    line = Color(0xFFE2E2EE),
    scrim = Color(0xFF07081A),
    isDark = false,
)

/** "Midnight glass" — deep indigo-black with a brighter violet accent. */
val NightKhaja = KhajaColors(
    bg = Color(0xFF090A14),
    bgAlt = Color(0xFF121322),
    surface = Color(0xFF171827),
    text = Color(0xFFF2F2F8),
    textSoft = Color(0xFF9698B4),
    accent = Color(0xFF8E80FF),
    accentDeep = Color(0xFFB0A5FF),
    accentSoft = Color(0xFF232046),
    onAccent = Color(0xFF0C0B1C),
    amber = Color(0xFFFFC29A),
    line = Color(0x1FFFFFFF),
    scrim = Color(0xFF000000),
    isDark = true,
)

val LocalKhaja = staticCompositionLocalOf { LightKhaja }

object Khaja {
    val colors: KhajaColors
        @Composable @ReadOnlyComposable get() = LocalKhaja.current
}

@Composable
fun KhajabadTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalKhaja provides if (dark) NightKhaja else LightKhaja) {
        content()
    }
}

/* ------------------------------------------------------------------ type */

val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)

val NotoMalayalam = FontFamily(
    Font(R.font.noto_malayalam_regular, FontWeight.Normal),
    Font(R.font.noto_malayalam_medium, FontWeight.Medium),
    Font(R.font.noto_malayalam_semibold, FontWeight.SemiBold),
    Font(R.font.noto_malayalam_bold, FontWeight.Bold),
)

object Type {
    val Display = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.Bold,
        fontSize = 42.sp, lineHeight = 44.sp, letterSpacing = (-1.4).sp,
    )
    val H1 = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.6).sp,
    )
    val H2 = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp,
    )
    val Title = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 21.sp,
    )
    val Body = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    )
    val Small = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp,
    )
    val Label = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.6.sp,
    )

    // Malayalam counterparts (Noto Sans Malayalam)
    val MalDisplay = TextStyle(
        fontFamily = NotoMalayalam, fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp, lineHeight = 38.sp,
    )
    val MalH2 = TextStyle(
        fontFamily = NotoMalayalam, fontWeight = FontWeight.Medium,
        fontSize = 20.sp, lineHeight = 30.sp,
    )
    val MalTitle = TextStyle(
        fontFamily = NotoMalayalam, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 24.sp,
    )
    val MalBody = TextStyle(
        fontFamily = NotoMalayalam, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 22.sp,
    )
    val MalLabel = TextStyle(
        fontFamily = NotoMalayalam, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 20.sp,
    )
}

/* ---------------------------------------------------------------- motion */

object Motion {
    /** Soft, water-like settling. */
    fun <T> fluid() = spring<T>(dampingRatio = 0.78f, stiffness = 320f)

    /** Heavier, slower settle for large surfaces. */
    fun <T> gentle() = spring<T>(dampingRatio = 0.9f, stiffness = 180f)

    /** Quick and decisive — buttons, chips, toggles. */
    fun <T> snappy() = spring<T>(dampingRatio = 0.55f, stiffness = 900f)

    fun <T> bouncy() = spring<T>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

    /**
     * "Stop motion" reveal: the value hops through discrete poses instead of
     * gliding. Used for splash letters, section headers and the counter digits.
     */
    fun stopMotion(durationMillis: Int = 520, delayMillis: Int = 0): FiniteAnimationSpec<Float> {
        val d = durationMillis
        return keyframes {
            this.durationMillis = d
            this.delayMillis = delayMillis
            0f at 0
            0.12f at (d * 0.16f).toInt()
            0.12f at (d * 0.20f).toInt()
            0.58f at (d * 0.44f).toInt()
            0.58f at (d * 0.50f).toInt()
            0.88f at (d * 0.72f).toInt()
            0.88f at (d * 0.78f).toInt()
            1f at d
        }
    }
}

/* ---------------------------------------------------------------- shapes */

object Dimens {
    val gutter = 22.dp
    val tileRadius = 28.dp
    val pillRadius = 100.dp
    val cardRadius = 32.dp
    val bottomInset = 96.dp
}
