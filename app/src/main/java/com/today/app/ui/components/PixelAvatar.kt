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
const val SPRITE_W = 16
const val SPRITE_H = 24

private val Outline = Color(0xFF1B1530)
private val Skin = Color(0xFFF5C9A3)
private val SkinShadow = Color(0xFFE0A985)
private val Eye = Color(0xFF2B2140)
private val Blush = Color(0xFFF59AA0)
private val Mouth = Color(0xFFB8505A)

/** Resolved colors for one draw call (lets us animate outfit changes). */
data class SpriteColors(val hair: Color, val shirt: Color, val accent: Color, val pants: Color, val shoes: Color)

fun Outfit.toSpriteColors() = SpriteColors(Color(hair), Color(shirt), Color(accent), Color(pants), Color(shoes))

/**
 * Draws the sprite with its top-left at [topLeft], each sprite pixel being [px] canvas pixels.
 * [step] alternates leg frames for a walking animation; [back] shows the back of the head.
 */
fun DrawScope.drawPixelSprite(
    topLeft: Offset,
    px: Float,
    colors: SpriteColors,
    blink: Boolean = false,
    step: Int = 0,
    back: Boolean = false,
) {
    val hairShine = lerp(colors.hair, Color.White, 0.35f)
    val shirtShade = lerp(colors.shirt, Color.Black, 0.25f)
    val pantsShade = lerp(colors.pants, Color.Black, 0.3f)
    // overdraw a hair of a pixel to avoid seams between blocks
    val cell = Size(px + 0.6f, px + 0.6f)
    SPRITE.forEachIndexed { y, row ->
        row.forEachIndexed { x, c ->
            var ch = c
            if (blink && ch == 'e') ch = 's'
            if (back && y in 5..8 && ch in "sSebm") ch = 'h'
            // walking: lift one leg (rows 19-23) by shifting it up a pixel
            var dy = 0f
            if (step != 0 && y >= 19) {
                val leftLeg = x < SPRITE_W / 2
                if ((step == 1 && leftLeg) || (step == 2 && !leftLeg)) dy = -px
            }
            val color = when (ch) {
                'k' -> Outline
                'h' -> colors.hair
                'H' -> hairShine
                's' -> Skin
                'S' -> SkinShadow
                'e' -> Eye
                'b' -> Blush
                'm' -> Mouth
                't' -> colors.shirt
                'T' -> shirtShade
                'a' -> colors.accent
                'p' -> colors.pants
                'P' -> pantsShade
                'o' -> colors.shoes
                else -> null
            } ?: return@forEachIndexed
            drawRect(color, Offset(topLeft.x + x * px, topLeft.y + y * px + dy), cell)
        }
    }
}

/** Stand-alone avatar with smooth color transitions and idle blinking. */
@Composable
fun PixelAvatar(outfit: Outfit, modifier: Modifier = Modifier, animateColors: Boolean = true) {
    val target = outfit.toSpriteColors()
    val spec = tween<Color>(durationMillis = if (animateColors) 700 else 0)
    val hair by animateColorAsState(target.hair, spec, label = "hair")
    val shirt by animateColorAsState(target.shirt, spec, label = "shirt")
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
        drawPixelSprite(Offset.Zero, px, SpriteColors(hair, shirt, accent, pants, shoes), blink)
    }
}
