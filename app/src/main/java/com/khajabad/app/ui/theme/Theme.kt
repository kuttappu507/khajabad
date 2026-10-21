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
    val paper: Color,
    val paperAlt: Color,
    val card: Color,
    val ink: Color,
    val inkSoft: Color,
    val mint: Color,
    val mintDeep: Color,
    val mintSoft: Color,
    val onMint: Color,
    val gold: Color,
    val line: Color,
    val scrim: Color,
    val isDark: Boolean,
)

val LightKhaja = KhajaColors(
    paper = Color(0xFFFBF6ED),
    paperAlt = Color(0xFFF4ECDF),
    card = Color(0xFFFFFDF8),
    ink = Color(0xFF0B1E33),
    inkSoft = Color(0xFF63727F),
    mint = Color(0xFF65D9B8),
    mintDeep = Color(0xFF1BA98A),
    mintSoft = Color(0xFFD7F5EA),
    onMint = Color(0xFF0B1E33),
    gold = Color(0xFFE5B364),
    line = Color(0x140B1E33),
    scrim = Color(0xFF04121A),
    isDark = false,
)

val NightKhaja = KhajaColors(
    paper = Color(0xFF07151A),
    paperAlt = Color(0xFF0B2028),
    card = Color(0xFF0E2530),
    ink = Color(0xFFF3EFE5),
    inkSoft = Color(0xFF9AAEB2),
    mint = Color(0xFF5FD8B6),
    mintDeep = Color(0xFF3AD6AE),
    mintSoft = Color(0xFF103A33),
    onMint = Color(0xFF04211B),
    gold = Color(0xFFEAC57D),
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
