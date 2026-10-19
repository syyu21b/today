package com.today.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.today.app.data.Outfit
import kotlinx.coroutines.delay

/**
 * 16 x 24 human sprite.
 *  k outline  h hair  H hair shine  s skin  S skin shadow  e eye  b blush  m mouth
 *  t shirt  T shirt shade  a accent (collar/belt)  p pants  P pants shade  o shoes
 */
private val SPRITE = listOf(
    "................",
    "....kkkkkkkk....",
    "...khhHHhhhhk...",
    "..khhhhhhhhhhk..",
    "..khhhhhhhhhhk..",
    "..khhsssssshhk..",
    "..khssessesshk..",
    "..khsbssssbshk..",
    "..kssssmmssssk..",
    "...kSSSSSSSSk...",
    "......kssk......",
    "..kttttaattttk..",
    ".kttTttaattTttk.",
    ".ktkTttttttTktk.",
    ".ktkTttttttTktk.",
    ".kskTttttttTksk.",
    "...kaaaaaaaak...",
    "...kppppppppk...",
    "...kpppPPpppk...",
    "...kpppkkpppk...",
    "...kppPkkPppk...",
    "...kppPkkPppk...",
    "..kooookkooook..",
    "..kkkkkkkkkkkk..",
)

/** Long-hair overlay for rows 4..11 (' ' = keep the base pixel). */
private val LONG_HAIR = mapOf(
    4 to ".kh          hk.",
    5 to ".kh          hk.",
    6 to ".kh          hk.",
    7 to ".kh          hk.",
    8 to ".kh          hk.",
    9 to ".khh        hhk.",
    10 to "..khhh    hhhk..",
    11 to "..kh        hk..",
)

const val SPRITE_W = 16
const val SPRITE_H = 24

private val Outline = Color(0xFF1B1530)
private val Eye = Color(0xFF2B2140)
private val Blush = Color(0xFFF59AA0)
private val Mouth = Color(0xFFB8505A)

/** Resolved colors + shape flags for one draw call (colors can be animated). */
data class SpriteStyle(
    val hair: Color,
    val skin: Color,
    val shirt: Color,
    val stripe: Color,
    val accent: Color,
    val pants: Color,
    val shoes: Color,
    val striped: Boolean,
    val shortSleeve: Boolean,
    val shorts: Boolean,
    val longHair: Boolean,
)

fun Outfit.toSpriteStyle() = SpriteStyle(
    Color(hair), Color(skin), Color(shirt), Color(stripe), Color(accent), Color(pants), Color(shoes),
    striped, shortSleeve, shorts, longHair,
)

/** Which sprite code ends up at (x, y) once the garment options are applied. */
private fun spriteCode(x: Int, y: Int, style: SpriteStyle, blink: Boolean): Char {
    var ch = SPRITE[y][x]
    if (style.longHair) LONG_HAIR[y]?.get(x)?.takeIf { it != ' ' }?.let { ch = it }
    if (blink && ch == 'e') ch = 's'
    // Stripes on every other torso row
    if (style.striped && (y == 12 || y == 14)) {
        if (ch == 't') ch = 'r' else if (ch == 'T') ch = 'R'
    }
    // Short sleeves: forearms show skin
    if (style.shortSleeve && (y == 13 || y == 14) && (x == 2 || x == 13)) ch = 's'
    // Shorts: lower legs show skin
    if (style.shorts && (y == 20 || y == 21) && (ch == 'p' || ch == 'P')) ch = if (ch == 'p') 's' else 'S'
    return ch
}

/** Draws the sprite with its top-left at [topLeft], each sprite pixel being [px] canvas pixels. */
fun DrawScope.drawPixelSprite(topLeft: Offset, px: Float, style: SpriteStyle, blink: Boolean = false) {
    val hairShine = lerp(style.hair, Color.White, 0.35f)
    val skinShadow = lerp(style.skin, Color.Black, 0.12f)
    val shirtShade = lerp(style.shirt, Color.Black, 0.25f)
    val stripeShade = lerp(style.stripe, Color.Black, 0.25f)
    val pantsShade = lerp(style.pants, Color.Black, 0.3f)
    // overdraw a hair of a pixel to avoid seams between blocks
    val cell = Size(px + 0.6f, px + 0.6f)
    for (y in 0 until SPRITE_H) for (x in 0 until SPRITE_W) {
        val color = when (spriteCode(x, y, style, blink)) {
            'k' -> Outline
            'h' -> style.hair
            'H' -> hairShine
            's' -> style.skin
            'S' -> skinShadow
            'e' -> Eye
            'b' -> Blush
            'm' -> Mouth
            't' -> style.shirt
            'T' -> shirtShade
            'r' -> style.stripe
            'R' -> stripeShade
            'a' -> style.accent
            'p' -> style.pants
            'P' -> pantsShade
            'o' -> style.shoes
            else -> null
        } ?: continue
        drawRect(color, Offset(topLeft.x + x * px, topLeft.y + y * px), cell)
    }
}

/** Stand-alone avatar with smooth color transitions and idle blinking. */
@Composable
fun PixelAvatar(outfit: Outfit, modifier: Modifier = Modifier, animateColors: Boolean = true) {
    val target = outfit.toSpriteStyle()
    val spec = tween<Color>(durationMillis = if (animateColors) 700 else 0)
    val hair by animateColorAsState(target.hair, spec, label = "hair")
    val skin by animateColorAsState(target.skin, spec, label = "skin")
    val shirt by animateColorAsState(target.shirt, spec, label = "shirt")
    val stripe by animateColorAsState(target.stripe, spec, label = "stripe")
    val accent by animateColorAsState(target.accent, spec, label = "accent")
    val pants by animateColorAsState(target.pants, spec, label = "pants")
    val shoes by animateColorAsState(target.shoes, spec, label = "shoes")

    var blink by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800)
            blink = true
            delay(140)
            blink = false
        }
    }

    Canvas(modifier.aspectRatio(SPRITE_W / SPRITE_H.toFloat())) {
        val px = size.width / SPRITE_W
        drawPixelSprite(
            Offset.Zero, px,
            target.copy(hair = hair, skin = skin, shirt = shirt, stripe = stripe, accent = accent, pants = pants, shoes = shoes),
            blink,
        )
    }
}
