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
import com.today.app.data.FaceFeature
import com.today.app.data.HairStyle
import com.today.app.data.Mood
import com.today.app.data.Outfit
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** The character is drawn on a 32 x 48 grid (art in CharacterArt.kt). */
const val SPRITE_W = 32
const val SPRITE_H = 48
private const val EYE_BOTTOM = 14

private val Outline = Color(0xFF1B1530)
private val Eye = Color(0xFF2B2140)
private val Blush = Color(0xFFF59AA0)
private val MouthLine = Color(0xFF8E3B46)
private val MouthInside = Color(0xFFC4505E)
private val HeartPink = Color(0xFFFF4F8B)
private val Tear = Color(0xFF6EC8FF)
private val FreckleTint = Color(0xFF8C5032)

/** Resolved colors + shape options for one draw call (colors can be animated). */
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
    val hairStyle: HairStyle = HairStyle.SHORT,
    val features: Set<FaceFeature> = emptySet(),
    /** Accessory ids, drawn in this order (back first). */
    val accessories: List<String> = emptyList(),
    val mood: Mood = Mood.NORMAL,
)

fun Outfit.toSpriteStyle(mood: Mood = Mood.NORMAL) = SpriteStyle(
    Color(hair), Color(skin), Color(shirt), Color(stripe), Color(accent), Color(pants), Color(shoes),
    striped, shortSleeve, shorts, style, features,
    listOf(back, head, face).filter { it.isNotEmpty() },
    mood,
)

/** Everything that decides which code sits in which cell (colors don't), so grids can be cached. */
private data class GridKey(
    val hairStyle: HairStyle,
    val features: Set<FaceFeature>,
    val striped: Boolean,
    val shortSleeve: Boolean,
    val shorts: Boolean,
    val mood: Mood,
    val blink: Boolean,
    val hideAbove: Int,
)

private val gridCache = HashMap<GridKey, Array<CharArray>>()

private fun Map<Int, String>.paintOnto(g: Array<CharArray>, onlyOver: String? = null) {
    for ((y, row) in this) {
        if (y !in g.indices) continue
        for (x in 0 until SPRITE_W) {
            val ch = row.getOrNull(x) ?: '.'
            if (ch == '.') continue
            if (onlyOver != null && g[y][x] !in onlyOver) continue
            g[y][x] = ch
        }
    }
}

/** Builds the code grid: back hair, body (with garment options), front hair, face, features. */
private fun buildGrid(k: GridKey): Array<CharArray> {
    val g = Array(SPRITE_H) { CharArray(SPRITE_W) { '.' } }
    val hair = HAIR_ART.getValue(k.hairStyle)
    hair.back.paintOnto(g)
    for (y in 0 until SPRITE_H) for (x in 0 until SPRITE_W) {
        var ch = BASE_ART[y][x]
        if (ch == '.') continue
        if (k.striped && y in STRIPE_ROWS) ch = when (ch) { 't', 'u' -> 'r'; 'T' -> 'R'; else -> ch }
        if (k.shortSleeve && y in SLEEVE_ROWS && (x in 3..5 || x in 26..28) && ch in "tTu") ch = 's'
        if (k.shorts && y in SHORTS_ROWS) ch = when (ch) { 'p' -> 's'; 'P' -> 'S'; else -> ch }
        g[y][x] = ch
    }
    hair.front.paintOnto(g)
    hair.shine.paintOnto(g, onlyOver = "hHd")
    FACE_ART[k.mood]?.paintOnto(g)
    for (f in k.features) FEATURE_ART[f]?.paintOnto(g)
    if (k.blink) {
        // Open eyes (rows 12..14) close to a line on their lowest row; already-closed eyes stay.
        for (x in 0 until SPRITE_W) {
            if (g[EYE_BOTTOM][x] !in "ew") continue
            g[EYE_BOTTOM][x] = 'e'
            for (y in EYE_BOTTOM - 2 until EYE_BOTTOM) if (g[y][x] in "ew") g[y][x] = 's'
        }
    }
    for (y in 0 until k.hideAbove.coerceAtMost(SPRITE_H)) g[y].fill('.')
    return g
}

private fun SpriteStyle.grid(blink: Boolean): Array<CharArray> {
    val hide = accessories.maxOfOrNull { ACCESSORY_ART[it]?.hideAbove ?: 0 } ?: 0
    val key = GridKey(hairStyle, features, striped, shortSleeve, shorts, mood, blink, hide)
    return synchronized(gridCache) { gridCache.getOrPut(key) { buildGrid(key) } }
}

/** True when some worn accessory moves, so the avatar needs a running clock. */
private fun SpriteStyle.hasAnimatedAccessory() = accessories.any { ACCESSORY_ART[it]?.animated == true }

/** Accessory falling into place: [id] drawn [offset] cells above its spot. */
data class AccessoryDrop(val id: String, val offset: Float)

/**
 * Draws the character with its top-left at [topLeft], each grid cell being [cell] canvas pixels.
 * [time] (0..1, looping) drives sparkles and cape sway.
 */
fun DrawScope.drawPixelSprite(
    topLeft: Offset,
    cell: Float,
    style: SpriteStyle,
    blink: Boolean = false,
    time: Float = 0f,
    drop: AccessoryDrop? = null,
) {
    val hairShine = lerp(style.hair, Color.White, 0.35f)
    val hairDark = lerp(style.hair, Color.Black, 0.3f)
    val skinShadow = lerp(style.skin, Color.Black, 0.14f)
    val freckle = lerp(style.skin, FreckleTint, 0.45f)
    val shirtShade = lerp(style.shirt, Color.Black, 0.25f)
    val shirtLight = lerp(style.shirt, Color.White, 0.2f)
    val stripeShade = lerp(style.stripe, Color.Black, 0.25f)
    val accentShade = lerp(style.accent, Color.Black, 0.25f)
    val pantsShade = lerp(style.pants, Color.Black, 0.3f)
    val shoeLight = lerp(style.shoes, Color.White, 0.3f)
    // overdraw a hair of a pixel to avoid seams between blocks
    val size = Size(cell + 0.6f, cell + 0.6f)
    val g = style.grid(blink)
    for (y in 0 until SPRITE_H) for (x in 0 until SPRITE_W) {
        val color = when (g[y][x]) {
            'k' -> Outline
            'h' -> style.hair
            'H' -> hairShine
            'd' -> hairDark
            's' -> style.skin
            'S' -> skinShadow
            'f' -> freckle
            'e' -> Eye
            'w' -> Color.White
            'b' -> Blush
            'm' -> MouthLine
            'M' -> MouthInside
            'L' -> HeartPink
            'q' -> Tear
            't' -> style.shirt
            'T' -> shirtShade
            'u' -> shirtLight
            'r' -> style.stripe
            'R' -> stripeShade
            'a' -> style.accent
            'A' -> accentShade
            'p' -> style.pants
            'P' -> pantsShade
            'o' -> style.shoes
            'O' -> shoeLight
            else -> null
        } ?: continue
        drawRect(color, Offset(topLeft.x + x * cell, topLeft.y + y * cell), size)
    }
    drawAccessories(topLeft, cell, style, g, hairShine, skinShadow, time, drop)
}

/** Second pass: accessories (back first). */
private fun DrawScope.drawAccessories(
    topLeft: Offset,
    cell: Float,
    style: SpriteStyle,
    grid: Array<CharArray>,
    hairShine: Color,
    skinShadow: Color,
    time: Float,
    drop: AccessoryDrop?,
) {
    val size = Size(cell + 0.6f, cell + 0.6f)
    // Translucent cells (glass, glow) must not overlap, or the seams show darker.
    val exact = Size(cell, cell)
    for (id in style.accessories) {
        val art = ACCESSORY_ART[id] ?: continue
        val top = topLeft.y + if (drop?.id == id) -drop.offset * cell else 0f
        // Cape hem swings up to 2 cells, the shoulders stay put.
        val swing = if (art.sway) sin(time * 2 * PI).toFloat() else 0f
        for ((layer, behind) in listOf(art.behind to true, art.front to false)) {
            for ((y, row) in layer) for (x in 0 until SPRITE_W) {
                val dx = if (art.sway && y > 28) (swing * (y - 28) / 8f).roundToInt() else 0
                val code = row.getOrNull(x - dx) ?: '.'
                if (code == '.') continue
                if (behind && grid.getOrNull(y)?.getOrNull(x) != '.') continue
                val color = art.palette[code] ?: when (code) {
                    'k' -> Outline
                    'h' -> style.hair
                    'H' -> hairShine
                    's' -> style.skin
                    'S' -> skinShadow
                    else -> null
                } ?: continue
                drawRect(color, Offset(topLeft.x + x * cell, top + y * cell), if (color.alpha < 1f) exact else size)
            }
        }
        art.sparkles.forEachIndexed { i, (x, y) ->
            // Each point glints for a quarter of the loop, one after another.
            val local = (time - i.toFloat() / art.sparkles.size + 1f) % 1f
            if (local < 0.25f) drawSparkle(topLeft.x + x * cell, top + y * cell, cell, sin(local / 0.25f * PI).toFloat())
        }
    }
}

/** 4-point twinkle centred on the cell at ([x], [y]); [a] = brightness 0..1. */
private fun DrawScope.drawSparkle(x: Float, y: Float, cell: Float, a: Float) {
    val size = Size(cell, cell)
    drawRect(Color.White.copy(alpha = a), Offset(x, y), size)
    val reach = if (a > 0.6f) 2 else 1
    for (d in 1..reach) {
        val c = SparkleColor.copy(alpha = a * if (d == 1) 0.8f else 0.4f)
        for ((dx, dy) in listOf(d to 0, -d to 0, 0 to d, 0 to -d)) drawRect(c, Offset(x + dx * cell, y + dy * cell), size)
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
        val cell = size.width / SPRITE_W
        drawPixelSprite(
            Offset.Zero, cell,
            target.copy(hair = hair, skin = skin, shirt = shirt, stripe = stripe, accent = accent, pants = pants, shoes = shoes),
            blink,
            time = if (moving) time.value else 0f,
            drop = if (dropId.isNotEmpty() && drop.value != 0f) AccessoryDrop(dropId, drop.value) else null,
        )
    }
}

/**
 * The character as a plain bitmap ([cell] px per grid cell), for places without Compose
 * such as the home-screen widget.
 */
fun renderSpriteBitmap(outfit: Outfit, mood: Mood = Mood.NORMAL, cell: Int = 3): Bitmap {
    val image = ImageBitmap(SPRITE_W * cell, SPRITE_H * cell)
    CanvasDrawScope().draw(
        Density(1f),
        LayoutDirection.Ltr,
        GraphicsCanvas(image),
        Size(image.width.toFloat(), image.height.toFloat()),
    ) {
        drawPixelSprite(Offset.Zero, cell.toFloat(), outfit.toSpriteStyle(mood))
    }
    return image.asAndroidBitmap()
}
