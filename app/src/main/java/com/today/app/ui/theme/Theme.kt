package com.today.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Night = Color(0xFF14121F)
val NightSurface = Color(0xFF1F1B2E)
val NightSurfaceHigh = Color(0xFF2A2540)
val Violet = Color(0xFF8B6CFF)
val Gold = Color(0xFFFFD34E)
val Mint = Color(0xFF3DDC97)
val Coral = Color(0xFFFF5C77)
val TextMuted = Color(0xFFA8A3C2)

/** Instagram-style DM gradient for "my" bubbles and story rings. */
val DmGradient = Brush.linearGradient(listOf(Color(0xFF7B3FF2), Color(0xFFB13CE0), Color(0xFF3D7BFF)))
val StoryGradient = Brush.sweepGradient(listOf(Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF), Color(0xFF4F5BD5), Color(0xFFFEDA75)))

/** Monospace = cheap but effective "pixel game" look for numbers and headings. */
val PixelFont = FontFamily.Monospace

private val colors = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = Night,
    tertiary = Mint,
    background = Night,
    onBackground = Color.White,
    surface = NightSurface,
    onSurface = Color.White,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = TextMuted,
    surfaceContainer = NightSurface,
    error = Coral,
)

private val typography = Typography(
    headlineMedium = TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = PixelFont, fontSize = 11.sp),
)

@Composable
fun TodayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
