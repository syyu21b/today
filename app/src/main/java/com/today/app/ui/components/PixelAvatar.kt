package com.today.app.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import com.today.app.data.Mood
import com.today.app.data.Outfit
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

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

/**
 * Accessory art on a finer 32 x 48 grid (half a sprite pixel per cell), drawn over the finished sprite.
 * '.' = transparent. Codes come from the art's own [palette]; k/h/H/s/S fall back to the sprite's
 * outline/hair/skin, so e.g. cat ears match the hair. [behind] cells only show where the sprite is empty.
 */
private class AccessoryArt(
    val palette: Map<Char, Color>,
    val front: Map<Int, String> = emptyMap(),
    val behind: Map<Int, String> = emptyMap(),
    /** Twinkle points (x, y on the fine grid), each lighting up in turn. */
    val sparkles: List<Pair<Int, Int>> = emptyList(),
    /** Lower [behind] rows sway sideways (capes). */
    val sway: Boolean = false,
) {
    val animated get() = sparkles.isNotEmpty() || sway
}

private const val ART_W = 32

private val ACCESSORY_ART = mapOf(
    "cap" to AccessoryArt(
        palette = mapOf('C' to Color(0xFFE94B5A), 'L' to Color(0xFFFF8F98), 'c' to Color(0xFFB8323F), 'B' to Color(0xFFD83F4E), 'b' to Color(0xFF7E1F2A), 'Y' to Color(0xFFFFE066)),
        front = mapOf(
            0 to "..............kkkk..............",
            1 to "..........kkkkkcckkkkk..........",
            2 to "........kkCCLLCCCCLLCCkk........",
            3 to "......kkCCCLCCCYYCCCLCCCkk......",
            4 to ".....kCCCCLCCCYYYYCCCLCCCCk.....",
            5 to "....kCCCCCCCCYYYYYYCCCCCCCCk....",
            6 to "....kCCCCCCCCCYYYYCCCCCCCCCk....",
            7 to "...kcCCCCCCCCCCYYCCCCCCCCCCck...",
            8 to "...kcccccccccccccccccccccccck...",
            9 to "...kBBBBBBBBBBBBBBBBBBBBBBBBk...",
            10 to "..kBBBBBBBBBBBBBBBBBBBBBBBBBBk..",
            11 to "..kbbbbbbbbbbbbbbbbbbbbbbbbbbk..",
        ),
    ),
    "glasses" to AccessoryArt(
        palette = mapOf('F' to Color(0xFFE23D78), 'p' to Color(0x99FF7FAE), 'w' to Color(0xCCFFFFFF)),
        front = mapOf(
            10 to "..........FF.FF..FF.FF..........",
            11 to ".......FFFwppppFFppppwFFF.......",
            12 to ".........FpppppFFpppppF.........",
            13 to "..........FpppF..FpppF..........",
            14 to "...........FpF....FpF...........",
            15 to "............F......F............",
        ),
    ),
    "headphones" to AccessoryArt(
        palette = mapOf('d' to Color(0xFF3A3A4A), 'D' to Color(0xFF6A6A80), 'c' to Color(0xFF7C6CF0), 'L' to Color(0xFFB3A8FF), 'S' to Color(0xFF5246B8), 'p' to Color(0xFF2A2A36)),
        front = mapOf(
            0 to ".........kkkkkkkkkkkkkk.........",
            1 to ".......kkDDDDDDDDDDDDDDkk.......",
            2 to "......kddddddddddddddddddk......",
            3 to ".....kddk..............kddk.....",
            4 to "....kddk................kddk....",
            5 to "....kddk................kddk....",
            6 to "...kddk..................kddk...",
            7 to "...kddk..................kddk...",
            8 to ".kkkkkk..................kkkkkk.",
            9 to "kLLccpk..................kpccLLk",
            10 to "kLcccpk..................kpcccLk",
            11 to "kccccpk..................kpcccck",
            12 to "kccccpk..................kpcccck",
            13 to "kccccpk..................kpcccck",
            14 to "kScccpk..................kpcccSk",
            15 to "kSSSSpk..................kpSSSSk",
            16 to ".kkkkkk..................kkkkkk.",
        ),
    ),
    "cape" to AccessoryArt(
        palette = mapOf('v' to Color(0xFFB0283A), 'V' to Color(0xFF7A1A28), 'y' to Color(0xFFFFD34E), 'Y' to Color(0xFFFFF2A8)),
        front = mapOf(
            21 to ".............kkkkkk.............",
            22 to "....vvvvvvvvvkYyyYkvvvvvvvvv....",
            23 to "......kkkvvvvkyyyykvvvvkkk......",
            24 to ".............kkkkkk.............",
        ),
        behind = mapOf(
            20 to "......kVVVVVVVVVVVVVVVVVVk......",
            21 to ".....kvvvvvvvvvvvvvvvvvvvvk.....",
            22 to "..kvvvvvvvvvvvvvvvvvvvvvvvvvvk..",
            23 to ".kvvvvvvvvvvvvvvvvvvvvvvvvvvvvk.",
            24 to "kvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvk",
            25 to "kvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvk",
            26 to "kvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvk",
            27 to "kvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvk",
            28 to "kvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvk",
            29 to "kvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvk",
            30 to "kvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvk",
            31 to "kvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvk",
            32 to "kvvVvvvvvvvvvvvvvvvvvvvvvvvvVvvk",
            33 to "kvvVvvvvvvvvvvvvvvvvvvvvvvvvVvvk",
            34 to "kvvVvvvvvvvvvvvvvvvvvvvvvvvvVvvk",
            35 to "kvvVvvvvvvvvvvvvvvvvvvvvvvvvVvvk",
            36 to "kvvVvvvvvvvvvvvvvvvvvvvvvvvvVvvk",
            37 to "kvvVvvvvvvvvvvvvvvvvvvvvvvvvVvvk",
            38 to "kvvVvvvvvvvvvvvvvvvvvvvvvvvvVvvk",
            39 to "kvvVvvvvvvvvvvvvvvvvvvvvvvvvVvvk",
            40 to "kvvVvvvvvvvvvvvvvvvvvvvvvvvvVvvk",
            41 to "kvVvvvvvvvvvvvvvvvvvvvvvvvvvvVvk",
            42 to "kVVVvVVVVVVVVVVVVVVVVVVVVVVvVVVk",
            43 to ".kkkkkkkkkkkkkkkkkkkkkkkkkkkkkk.",
        ),
        sway = true,
    ),
    "crown" to AccessoryArt(
        palette = mapOf('y' to Color(0xFFFFD34E), 'Y' to Color(0xFFFFF2A8), 'o' to Color(0xFFC8901E), 'r' to Color(0xFFFF4D6D), 'g' to Color(0xFF3DDCFF)),
        front = mapOf(
            0 to "........oY.....YY.....Yo........",
            1 to "........yy.....yy.....yy........",
            2 to ".......oYyo...oyyo...oyYo.......",
            3 to ".......oYyyo.oyyyyo.oyyYo.......",
            4 to "......oyyyyyoyyyyyyoyyyyyo......",
            5 to "......oyyyyyyyyyyyyyyyyyyo......",
            6 to "......oYYYYYYYYYYYYYYYYYYo......",
            7 to "......oyyrryyyggggyyyrryyo......",
            8 to "......oyyrryyyggggyyyrryyo......",
            9 to "......kooooooooooooooooook......",
        ),
        sparkles = listOf(9 to 7, 15 to 1, 22 to 7, 16 to 8),
    ),
    "sunglasses" to AccessoryArt(
        palette = mapOf('F' to Color(0xFF101018), 'd' to Color(0xFF1E2436), 'l' to Color(0xFF3A4A6A), 'w' to Color(0xDDFFFFFF)),
        front = mapOf(
            10 to "......FFFFFFFFFFFFFFFFFFFF......",
            11 to "......FFFddwddF..FddwddFFF......",
            12 to "........FdwdddF..FdwdddF........",
            13 to "........FlllllF..FlllllF........",
            14 to ".........FlllF....FlllF.........",
            15 to "..........FFF......FFF..........",
        ),
    ),
    "catears" to AccessoryArt(
        palette = mapOf('p' to Color(0xFFF59AA0), 'P' to Color(0xFFFFD0D4)),
        front = mapOf(
            0 to "..kk........................kk..",
            1 to "..kHkk....................kkHk..",
            2 to "..kHpphkk..............kkhppHk..",
            3 to "..khppphhkk..........kkhhppphk..",
            4 to "..khpppPphhk........khhpPppphk..",
            5 to "...khppPpphhhh....hhhhppPpphk...",
            6 to "...khhpppphh........hhpppphhk...",
            7 to "....khhhhh............hhhhhk....",
        ),
    ),
    "halo" to AccessoryArt(
        palette = mapOf('l' to Color(0xFFFFF3A0), 'L' to Color(0xFFFFC93C), 'Y' to Color(0xFFFFFFFF), 'q' to Color(0x66FFF3A0)),
        front = mapOf(
            0 to "........qqLLLLLLLLLLLLqq........",
            1 to ".....qlLL..............LLlq.....",
            2 to "......qlllllllYYYYlllllllq......",
            3 to ".........qqqqqqqqqqqqqq.........",
        ),
        sparkles = listOf(11 to 0, 23 to 1, 7 to 1),
    ),
)

/** Face overlays per mood, like accessories (' ' keeps the pixel). q = tear. */
private val MOOD_ART = mapOf(
    // Mouth corners lifted into a smile.
    Mood.HAPPY to mapOf(7 to "      m  m      "),
    // Eyes shut into lines, small mouth.
    Mood.SLEEPY to mapOf(
        6 to "     ee  ee     ",
        8 to "       ms       ",
    ),
    // Frown (corners down) and a tear.
    Mood.SAD to mapOf(
        7 to "     q mm       ",
        8 to "      mssm      ",
    ),
)

private val Tear = Color(0xFF6EC8FF)

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
    /** Accessory ids, drawn in this order (back first). */
    val accessories: List<String> = emptyList(),
    val mood: Mood = Mood.NORMAL,
)

fun Outfit.toSpriteStyle(mood: Mood = Mood.NORMAL) = SpriteStyle(
    Color(hair), Color(skin), Color(shirt), Color(stripe), Color(accent), Color(pants), Color(shoes),
    striped, shortSleeve, shorts, longHair,
    listOf(back, head, face).filter { it.isNotEmpty() },
    mood,
)

/** Which sprite code ends up at (x, y) once the garment options are applied. */
private fun spriteCode(x: Int, y: Int, style: SpriteStyle, blink: Boolean): Char {
    var ch = SPRITE[y][x]
    if (style.longHair) LONG_HAIR[y]?.get(x)?.takeIf { it != ' ' }?.let { ch = it }
    MOOD_ART[style.mood]?.get(y)?.getOrNull(x)?.takeIf { it != ' ' }?.let { ch = it }
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

/** True when some worn accessory moves, so the avatar needs a running clock. */
private fun SpriteStyle.hasAnimatedAccessory() = accessories.any { ACCESSORY_ART[it]?.animated == true }

/** Accessory falling into place: [id] drawn [offset] fine-grid cells above its spot. */
data class AccessoryDrop(val id: String, val offset: Float)

/**
 * Draws the sprite with its top-left at [topLeft], each sprite pixel being [px] canvas pixels.
 * [time] (0..1, looping) drives sparkles and cape sway.
 */
fun DrawScope.drawPixelSprite(
    topLeft: Offset,
    px: Float,
    style: SpriteStyle,
    blink: Boolean = false,
    time: Float = 0f,
    drop: AccessoryDrop? = null,
) {
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
            'q' -> Tear
            else -> null
        } ?: continue
        drawRect(color, Offset(topLeft.x + x * px, topLeft.y + y * px), cell)
    }
    drawAccessories(topLeft, px / 2, style, blink, hairShine, skinShadow, time, drop)
}

/** Second pass: accessories (back first) on the half-pixel grid. */
private fun DrawScope.drawAccessories(
    topLeft: Offset,
    half: Float,
    style: SpriteStyle,
    blink: Boolean,
    hairShine: Color,
    skinShadow: Color,
    time: Float,
    drop: AccessoryDrop?,
) {
    val cell = Size(half + 0.6f, half + 0.6f)
    // Translucent cells (glass, glow) must not overlap, or the seams show darker.
    val exact = Size(half, half)
    for (id in style.accessories) {
        val art = ACCESSORY_ART[id] ?: continue
        val top = topLeft.y + if (drop?.id == id) -drop.offset * half else 0f
        // Cape hem swings up to 2 cells, the shoulders stay put.
        val swing = if (art.sway) sin(time * 2 * PI).toFloat() else 0f
        for ((layer, behind) in listOf(art.behind to true, art.front to false)) {
            for ((y, row) in layer) for (x in 0 until ART_W) {
                val dx = if (art.sway && y > 28) (swing * (y - 28) / 8f).roundToInt() else 0
                val code = row.getOrNull(x - dx) ?: '.'
                if (code == '.') continue
                if (behind && spriteCode(x / 2, y / 2, style, blink) != '.') continue
                val color = art.palette[code] ?: when (code) {
                    'k' -> Outline
                    'h' -> style.hair
                    'H' -> hairShine
                    's' -> style.skin
                    'S' -> skinShadow
                    else -> null
                } ?: continue
                drawRect(color, Offset(topLeft.x + x * half, top + y * half), if (color.alpha < 1f) exact else cell)
            }
        }
        art.sparkles.forEachIndexed { i, (x, y) ->
            // Each point glints for a quarter of the loop, one after another.
            val local = (time - i.toFloat() / art.sparkles.size + 1f) % 1f
            if (local < 0.25f) drawSparkle(topLeft.x + x * half, top + y * half, half, sin(local / 0.25f * PI).toFloat())
        }
    }
}

/** 4-point twinkle centred on the cell at ([x], [y]); [a] = brightness 0..1. */
private fun DrawScope.drawSparkle(x: Float, y: Float, half: Float, a: Float) {
    val size = Size(half, half)
    drawRect(Color.White.copy(alpha = a), Offset(x, y), size)
    val reach = if (a > 0.6f) 2 else 1
    for (d in 1..reach) {
        val c = SparkleColor.copy(alpha = a * if (d == 1) 0.8f else 0.4f)
        for ((dx, dy) in listOf(d to 0, -d to 0, 0 to d, 0 to -d)) drawRect(c, Offset(x + dx * half, y + dy * half), size)
    }
}

private val SparkleColor = Color(0xFFFFF6C8)
private const val DROP_FROM = 14f

/**
 * Stand-alone avatar with smooth color transitions and idle blinking.
 * With [animateColors], an accessory put on falls into place; [dropIn] plays that fall once on first show.
 */
@Composable
fun PixelAvatar(
    outfit: Outfit,
    modifier: Modifier = Modifier,
    animateColors: Boolean = true,
    mood: Mood = Mood.NORMAL,
    dropIn: String? = null,
) {
    val target = outfit.toSpriteStyle(mood)
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

    // Sparkles / cape sway clock; only read (and so only redrawn) when something moves.
    val clock = rememberInfiniteTransition(label = "accessories")
    val time = clock.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "time")
    val moving = target.hasAnimatedAccessory()

    val drop = remember { Animatable(if (dropIn != null) DROP_FROM else 0f) }
    var dropId by remember { mutableStateOf(dropIn.orEmpty()) }
    var worn by remember { mutableStateOf(target.accessories) }
    LaunchedEffect(target.accessories) {
        val added = target.accessories.firstOrNull { it !in worn }
        worn = target.accessories
        if (added != null && animateColors) {
            dropId = added
            drop.snapTo(DROP_FROM)
        }
        if (dropId.isNotEmpty()) drop.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 420f))
    }

    Canvas(modifier.aspectRatio(SPRITE_W / SPRITE_H.toFloat())) {
        val px = size.width / SPRITE_W
        drawPixelSprite(
            Offset.Zero, px,
            target.copy(hair = hair, skin = skin, shirt = shirt, stripe = stripe, accent = accent, pants = pants, shoes = shoes),
            blink,
            time = if (moving) time.value else 0f,
            drop = if (dropId.isNotEmpty() && drop.value != 0f) AccessoryDrop(dropId, drop.value) else null,
        )
    }
}

/**
 * The sprite as a plain bitmap ([pixel] px per sprite pixel), for places without Compose
 * such as the home-screen widget.
 */
fun renderSpriteBitmap(outfit: Outfit, mood: Mood = Mood.NORMAL, pixel: Int = 6): Bitmap {
    val image = ImageBitmap(SPRITE_W * pixel, SPRITE_H * pixel)
    CanvasDrawScope().draw(
        Density(1f),
        LayoutDirection.Ltr,
        GraphicsCanvas(image),
        Size(image.width.toFloat(), image.height.toFloat()),
    ) {
        drawPixelSprite(Offset.Zero, pixel.toFloat(), outfit.toSpriteStyle(mood))
    }
    return image.asAndroidBitmap()
}
