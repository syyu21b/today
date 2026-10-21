package com.today.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.today.app.R
import com.today.app.data.ThemeMode

/** Every color the screens use, in a dark and a light version. */
@Immutable
class TodayColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val textMuted: Color,
    /** Main text / icon color. */
    val ink: Color,
    /** Base for faint borders and dividers (used with a low alpha). */
    val hairline: Color,
    val violet: Color,
    val gold: Color,
    val mint: Color,
    val coral: Color,
    val sky: Color,
    /** Top of the profile card gradient. */
    val heroTop: Color,
)

val DarkColors = TodayColors(
    isDark = true,
    background = Color(0xFF14121F),
    surface = Color(0xFF1F1B2E),
    surfaceHigh = Color(0xFF2A2540),
    textMuted = Color(0xFFA8A3C2),
    ink = Color.White,
    hairline = Color.White,
    violet = Color(0xFF8B6CFF),
    gold = Color(0xFFFFD34E),
    mint = Color(0xFF3DDC97),
    coral = Color(0xFFFF5C77),
    sky = Color(0xFF6EC8FF),
    heroTop = Color(0xFF3A2F6B),
)

val LightColors = TodayColors(
    isDark = false,
    background = Color(0xFFF5F3FA),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFECE8F6),
    textMuted = Color(0xFF6B6680),
    ink = Color(0xFF1E1A2E),
    hairline = Color(0xFF1E1A2E),
    violet = Color(0xFF6C4BF4),
    // Deeper accents so text in them stays readable on white.
    gold = Color(0xFFE0A100),
    mint = Color(0xFF14A06A),
    coral = Color(0xFFE23D5B),
    sky = Color(0xFF2B8FD6),
    heroTop = Color(0xFFE2D9FF),
)

val LocalTodayColors = staticCompositionLocalOf { DarkColors }

// Short names used across the screens; they follow the current theme.
val Night: Color @Composable @ReadOnlyComposable get() = LocalTodayColors.current.background
val NightSurface: Color @Composable @ReadOnlyComposable get() = LocalTodayColors.current.surface
val NightSurfaceHigh: Color @Composable @ReadOnlyComposable get() = LocalTodayColors.current.surfaceHigh
val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalTodayColors.current.textMuted
val Ink: Color @Composable @ReadOnlyComposable get() = LocalTodayColors.current.ink
val Hairline: Color @Composable @ReadOnlyComposable get() = LocalTodayColors.current.hairline
val Violet: Color @Composable @ReadOnlyComposable get() = LocalTodayColors.current.violet
val Gold: Color @Composable @ReadOnlyComposable get() = LocalTodayColors.current.gold
val Mint: Color @Composable @ReadOnlyComposable get() = LocalTodayColors.current.mint
val Coral: Color @Composable @ReadOnlyComposable get() = LocalTodayColors.current.coral
val Sky: Color @Composable @ReadOnlyComposable get() = LocalTodayColors.current.sky

/** Instagram-style DM gradient for "my" bubbles and story rings. */
val DmGradient = Brush.linearGradient(listOf(Color(0xFF7B3FF2), Color(0xFFB13CE0), Color(0xFF3D7BFF)))
val StoryGradient = Brush.sweepGradient(listOf(Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF), Color(0xFF4F5BD5), Color(0xFFFEDA75)))

/** Galmuri11 (SIL OFL 1.1): a Korean + Latin pixel font for headings, levels and numbers. */
val PixelFont = FontFamily(
    Font(R.font.galmuri11, FontWeight.Normal),
    Font(R.font.galmuri11_bold, FontWeight.Bold),
)

private fun scheme(c: TodayColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.violet, onPrimary = Color.White, secondary = c.gold, onSecondary = c.background, tertiary = c.mint,
        background = c.background, onBackground = c.ink, surface = c.surface, onSurface = c.ink,
        surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.textMuted, surfaceContainer = c.surface,
        surfaceContainerHigh = c.surfaceHigh, error = c.coral,
    )
} else {
    lightColorScheme(
        primary = c.violet, onPrimary = Color.White, secondary = c.gold, onSecondary = Color.White, tertiary = c.mint,
        background = c.background, onBackground = c.ink, surface = c.surface, onSurface = c.ink,
        surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.textMuted, surfaceContainer = c.surface,
        surfaceContainerHigh = c.surfaceHigh, error = c.coral,
    )
}

private val typography = Typography(
    headlineMedium = TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = PixelFont, fontSize = 11.sp),
)

/** Resolves [mode] (SYSTEM follows the phone) to dark or light. */
@Composable
fun isDarkTheme(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}

@Composable
fun TodayTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val colors = if (isDarkTheme(mode)) DarkColors else LightColors
    CompositionLocalProvider(LocalTodayColors provides colors) {
        MaterialTheme(colorScheme = scheme(colors), typography = typography, content = content)
    }
}
