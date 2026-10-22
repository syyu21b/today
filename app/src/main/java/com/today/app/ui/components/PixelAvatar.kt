package com.today.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import com.today.app.data.BodyType
import com.today.app.data.FaceFeature
import com.today.app.data.Gender
import com.today.app.data.Mood
import com.today.app.data.Outfit
import com.today.app.data.TopStyle
import com.today.app.data.Tune
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin

/** Width : height of the full-body character. */
const val AVATAR_ASPECT = CharacterArt.W / CharacterArt.H

/** Crops of the art box (art coordinates) used for portraits; they grow and shrink with the head-size slider. */
enum class HeadBox(val rect: Rect) {
    /** Whole face and hair with a little air around it, square (also fits a circle). */
    FACE(Rect(51f, 34f, 149f, 132f)),
    /** Head with room for tall hats. */
    HAT(Rect(44f, -6f, 156f, 132f)),
    /** Head and upper body, down to the waist. */
    BUST(Rect(8f, 14f, 192f, 262f)),
    /** Close-up of the eyes and brows. */
    EYES(Rect(72f, 72f, 128f, 102f)),
}

private val Ink = Color(0xFF281622)
private val White = Color.White

private fun mix(a: Color, b: Color, t: Float) = lerp(a, b, t)

/** Every colour role the art uses, derived from the outfit. */
private class Palette(skin: Color, hair: Color, top: Color, stripe: Color, acc: Color, bottom: Color, shoes: Color, iris: Color, lip: Color) {
    val roles: Map<String, Color> = mapOf(
        "skin" to skin, "skinL" to mix(skin, White, 0.18f), "skinS" to mix(skin, Color(0xFFC46E6E), 0.28f), "skinD" to mix(skin, Color(0xFF783C3C), 0.5f),
        "line" to mix(skin, Ink, 0.65f),
        "hair" to hair, "hairS" to mix(hair, Ink, 0.35f), "hairD" to mix(hair, Ink, 0.6f), "hairL" to mix(hair, White, 0.35f),
        "iris" to iris, "irisD" to mix(iris, Ink, 0.6f), "irisL" to mix(iris, White, 0.55f), "pupil" to mix(iris, Ink, 0.85f),
        "white" to White, "eyeLine" to Color(0xFF2A1A26), "lip" to lip, "lipLine" to mix(lip, Ink, 0.45f), "blush" to Color(0xFFFF7A8C),
        "top" to top, "topS" to mix(top, Ink, 0.3f), "topL" to mix(top, White, 0.15f), "stripe" to stripe,
        "bottom" to bottom, "bottomS" to mix(bottom, Ink, 0.3f),
        "shoes" to shoes, "shoesL" to mix(shoes, White, 0.25f), "shoesS" to mix(shoes, Ink, 0.35f), "sole" to Color(0xFFEDEDF2),
        "acc" to acc, "accS" to mix(acc, Ink, 0.3f),
    )

    fun color(p: Paint): Color = when (p) {
        is Paint.Role -> roles[p.name] ?: Color.Magenta
        is Paint.Fixed -> p.color
        is Paint.Linear -> color(p.a)
        is Paint.Radial -> color(p.base)
    }

    companion object {
        fun of(o: Outfit) = Palette(Color(o.skin), Color(o.hair), Color(o.shirt), Color(o.stripe), Color(o.accent), Color(o.pants), Color(o.shoes), Color(o.eyeColor), Color(o.lipColor))
    }
}

private fun moodKey(m: Mood) = m.name.lowercase()
private val OPEN_EYES = setOf(Mood.NORMAL, Mood.SAD, Mood.EXCITED, Mood.LOVE, Mood.ANGRY, Mood.SICK)

/** Accessory falling into place: [id] drawn [offset] art units above its spot. */
data class AccessoryDrop(val id: String, val offset: Float)

/** Features the sliders move as a whole, around their anchor in the art. */
private enum class Feature(val anchor: String?) { NONE(null), EYE_R("eyeR"), EYE_L("eyeL"), BROW_R("browR"), BROW_L("browL"), NOSE("nose"), MOUTH("mouth") }

/** One part to draw: [head] parts follow the head-size slider; [accId] tags accessories for the drop-in. */
private class PartRef(val key: String, val head: Boolean, val feature: Feature = Feature.NONE, val accId: String? = null)

/** Parts in drawing order. */
private fun partKeys(o: Outfit, mood: Mood, blink: Boolean): List<PartRef> {
    val bt = o.gender.code
    val accs = listOf(o.back, o.head, o.face).filter { it.isNotEmpty() }
    val top = o.topStyle.name.lowercase()
    val sleeve = if (o.topStyle == TopStyle.TEE && o.shortSleeve) "short" else "long"
    val hair = o.style.name.lowercase()
    val eyes = "eyes.$bt.${o.eyeStyle.name.lowercase()}.${if (blink && mood in OPEN_EYES) "blink" else moodKey(mood)}"
    val brows = "brows.$bt.${o.browStyle.name.lowercase()}.${moodKey(mood)}"
    val keys = ArrayList<PartRef>()
    keys += PartRef("hairBack.$hair", head = true)
    accs.forEach { keys += PartRef("accBack.$it", head = it != "cape", accId = it) }
    keys += PartRef("body.$bt", head = false)
    if (o.topStyle != TopStyle.DRESS) keys += PartRef("bottom.$bt.${o.bottom.name.lowercase()}", head = false)
    keys += PartRef("shoes.$bt.${o.shoeStyle.name.lowercase()}", head = false)
    keys += PartRef("top.$bt.$top.$sleeve", head = false)
    if (o.striped) keys += PartRef("stripes.$bt.$top.$sleeve", head = false)
    // Arms hang in front of the garment's body; its sleeves cover them; hands go over everything.
    keys += PartRef("arms.$bt", head = false)
    keys += PartRef("sleeves.$bt.$top.$sleeve", head = false)
    keys += PartRef("hands.$bt", head = false)
    keys += PartRef("head.$bt.${o.faceShape.name.lowercase()}", head = true)
    // Boys only get rosy cheeks when they ask for them (makeup tab).
    if (o.blush || o.gender == Gender.FEMALE) keys += PartRef("blush.${if (o.blush) 1 else 0}", head = true)
    keys += PartRef("nose.$bt", head = true, feature = Feature.NOSE)
    for (f in FaceFeature.entries) {
        if (f !in o.features || (f == FaceFeature.MUSTACHE && FaceFeature.BEARD in o.features)) continue
        keys += PartRef("feature.${f.name.lowercase()}", head = true)
    }
    keys += PartRef("mouth.$bt.${moodKey(mood)}", head = true, feature = Feature.MOUTH)
    keys += PartRef("$eyes.R", head = true, feature = Feature.EYE_R)
    keys += PartRef("$eyes.L", head = true, feature = Feature.EYE_L)
    keys += PartRef("hairFront.$hair", head = true)
    keys += PartRef("$brows.R", head = true, feature = Feature.BROW_R)
    keys += PartRef("$brows.L", head = true, feature = Feature.BROW_L)
    accs.forEach { keys += PartRef("accFront.$it", head = it != "cape", accId = it) }
    return keys
}

/**
 * The art with [outfit]'s fine-tune sliders applied: morphed layers (made on first use) and the
 * moved anchors the features scale and tilt around.
 */
internal class Posed(private val art: Art, outfit: Outfit) {
    private val values = FloatArray(art.params.size) { i ->
        val name = art.params[i]
        (Tune.entries.firstOrNull { it.art == name }?.let(outfit::tuned) ?: 0f) + if (outfit.body == BodyType.BROAD) BROAD_BUILD[name] ?: 0f else 0f
    }
    private val cache = HashMap<String, List<ArtLayer>?>()

    fun layers(key: String): List<ArtLayer>? = cache.getOrPut(key) { art.parts[key]?.map { it.morphed(values) } }

    private val anchors: Map<String, Offset> = run {
        val layers = layers("anchors.${outfit.gender.code}.${outfit.faceShape.name.lowercase()}").orEmpty()
        art.anchorNames.withIndex().associate { (i, name) ->
            val l = layers.getOrNull(i)
            val c = l?.morph(l.shape, false, values) ?: l?.shape?.coords
            name to if (c != null) Offset(c[0], c[1]) else Offset(100f, 100f)
        }
    }

    fun anchor(name: String) = anchors[name] ?: Offset(100f, 100f)
}

/** Pets use fixed colours only, so any palette works. */
internal fun DrawScope.drawArtLayer(l: ArtLayer) = drawLayer(l, FixedPalette)

private val FixedPalette by lazy { Palette.of(com.today.app.data.Outfits.presets.first()) }

private fun DrawScope.drawLayer(l: ArtLayer, pal: Palette) {
    fun brush(p: Paint): Brush = when (p) {
        is Paint.Linear -> Brush.linearGradient(listOf(pal.color(p.a), pal.color(p.b)), p.start, p.end)
        is Paint.Radial -> {
            val c = pal.color(p.base)
            Brush.radialGradient(listOf(c.copy(alpha = p.alpha), c.copy(alpha = 0f)), p.center, p.radius)
        }
        else -> Brush.linearGradient(listOf(pal.color(p), pal.color(p)))
    }
    val draw: DrawScope.() -> Unit = {
        l.fill?.let { f ->
            if (f is Paint.Linear || f is Paint.Radial) drawPath(l.path, brush(f), alpha = l.alpha)
            else drawPath(l.path, pal.color(f), alpha = l.alpha)
        }
        l.stroke?.let { s ->
            drawPath(l.path, pal.color(s), alpha = l.alpha, style = Stroke(l.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
    val clip = l.clip
    if (clip != null) clipPath(clip) { draw() } else draw()
}

/** The broad build: a sturdier frame on top of whatever the sliders say. */
private val BROAD_BUILD = mapOf("shoulders" to 1.1f, "limbs" to 0.9f, "waist" to 0.9f, "hips" to 0.35f)

/** How much the head is scaled by the head-size slider. */
private fun headScale(o: Outfit) = 1f + 0.1f * o.tuned(Tune.HEAD_SIZE)

/** [box] grown / shrunk with the head so portraits keep the same framing. */
private fun headBox(box: Rect, o: Outfit, pivot: Offset): Rect {
    val k = headScale(o)
    if (k == 1f) return box
    return Rect(pivot.x + (box.left - pivot.x) * k, pivot.y + (box.top - pivot.y) * k, pivot.x + (box.right - pivot.x) * k, pivot.y + (box.bottom - pivot.y) * k)
}

/** Moves, scales and tilts one face feature by the sliders. */
private fun DrawScope.withFeature(f: Feature, o: Outfit, posed: Posed, block: DrawScope.() -> Unit) {
    val name = f.anchor ?: return block()
    val pivot = posed.anchor(name)
    var dx = 0f
    var dy = 0f
    var sx = 1f
    var sy = 1f
    var deg = 0f
    when (f) {
        Feature.EYE_R, Feature.EYE_L -> {
            val side = if (f == Feature.EYE_R) 1f else -1f
            sx = 1f + 0.16f * o.tuned(Tune.EYE_SIZE)
            sy = sx
            dy = -3f * o.tuned(Tune.EYE_HEIGHT)
            dx = side * 2.6f * o.tuned(Tune.EYE_SPACING)
            deg = -side * 9f * o.tuned(Tune.EYE_TILT)
        }
        Feature.BROW_R, Feature.BROW_L -> {
            val side = if (f == Feature.BROW_R) 1f else -1f
            sy = 1f + 0.4f * o.tuned(Tune.BROW_THICKNESS)
            dy = -3f * o.tuned(Tune.BROW_HEIGHT) - 1.5f * o.tuned(Tune.EYE_HEIGHT)
            dx = side * 2f * o.tuned(Tune.EYE_SPACING)
            deg = -side * 12f * o.tuned(Tune.BROW_TILT)
        }
        Feature.NOSE -> {
            sx = 1f + 0.25f * o.tuned(Tune.NOSE_SIZE)
            sy = sx
            dy = -2.5f * o.tuned(Tune.NOSE_HEIGHT)
        }
        Feature.MOUTH -> {
            sx = 1f + 0.28f * o.tuned(Tune.MOUTH_SIZE)
            sy = 1f + 0.12f * o.tuned(Tune.MOUTH_SIZE)
            dy = -3f * o.tuned(Tune.MOUTH_HEIGHT)
        }
        Feature.NONE -> Unit
    }
    if (dx == 0f && dy == 0f && sx == 1f && sy == 1f && deg == 0f) return block()
    withTransform({
        translate(dx, dy)
        rotate(deg, pivot)
        scale(sx, sy, pivot)
    }, block)
}

/** Runs [block] in art coordinates, with the art box [box] filling this draw scope. */
private fun DrawScope.inArtBox(box: Rect, block: DrawScope.() -> Unit) {
    val k = size.width / box.width
    clipRect {
        scale(k, k, pivot = Offset.Zero) {
            translate(-box.left, -box.top, block)
        }
    }
}

/** Draws the character so the art box [box] (art coordinates) fills this draw scope. */
private fun DrawScope.drawCharacter(
    posed: Posed,
    outfit: Outfit,
    pal: Palette,
    mood: Mood,
    blink: Boolean,
    box: Rect = FULL,
    drop: AccessoryDrop? = null,
) {
    val neck = posed.anchor("neck")
    val hs = headScale(outfit)
    inArtBox(box) {
        for (ref in partKeys(outfit, mood, blink)) {
            val layers = posed.layers(ref.key) ?: continue
            val dy = if (drop != null && ref.accId == drop.id) -drop.offset else 0f
            val draw: DrawScope.() -> Unit = { withFeature(ref.feature, outfit, posed) { translate(0f, dy) { layers.forEach { drawLayer(it, pal) } } } }
            if (ref.head && hs != 1f) scale(hs, hs, neck, draw) else draw()
        }
    }
}

/** Crowns and halos glinting over the character at [time] (0..1, looping). */
private fun DrawScope.drawCharacterGlints(posed: Posed, outfit: Outfit, box: Rect, time: Float) {
    val hs = headScale(outfit)
    inArtBox(box) {
        scale(hs, hs, posed.anchor("neck")) { drawGlints(if (outfit.head == "crown") CROWN_GLINTS else HALO_GLINTS, time) }
    }
}

/**
 * Draws [block] into a software bitmap the size of [size] (reusing [into] when it fits).
 * The art has hundreds of gradient fills; drawn straight to the screen every frame, the GPU
 * renderer (seen on the emulator) starts dropping them after a while and characters turn into
 * bare outlines. Rasterising on the CPU once and showing the picture avoids that and is cheaper.
 */
internal fun rasterize(size: Size, into: ImageBitmap? = null, block: DrawScope.() -> Unit): ImageBitmap {
    val w = ceil(size.width).toInt().coerceAtLeast(1)
    val h = ceil(size.height).toInt().coerceAtLeast(1)
    val image = into?.takeIf { it.width == w && it.height == h }?.also { it.asAndroidBitmap().eraseColor(0) } ?: ImageBitmap(w, h)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, GraphicsCanvas(image), size, block)
    return image
}

/** Two bitmaps used in turn, so the picture on screen is never drawn over while the next one is made. */
private class DoubleBuffer {
    private val images = arrayOfNulls<ImageBitmap>(2)
    private var next = 0

    fun draw(size: Size, block: DrawScope.() -> Unit): ImageBitmap =
        rasterize(size, images[next], block).also { images[next] = it; next = 1 - next }
}

private val CROWN_GLINTS = listOf(Offset(83.8f, 38.4f), Offset(100f, 32.7f), Offset(116.2f, 38.4f))
private val HALO_GLINTS = listOf(Offset(83.8f, 32.7f), Offset(116.2f, 34.6f), Offset(100f, 29.8f))

private fun DrawScope.drawGlints(points: List<Offset>, time: Float) {
    points.forEachIndexed { i, p ->
        val local = (time - i.toFloat() / points.size + 1f) % 1f
        if (local > 0.3f) return@forEachIndexed
        val a = sin(local / 0.3f * PI).toFloat()
        val r = 4f * a
        drawLine(Color.White.copy(alpha = a), Offset(p.x - r, p.y), Offset(p.x + r, p.y), strokeWidth = 1.2f, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = a), Offset(p.x, p.y - r), Offset(p.x, p.y + r), strokeWidth = 1.2f, cap = StrokeCap.Round)
        drawCircle(Color(0xFFFFF6C8).copy(alpha = a), 1.4f * a, p)
    }
}

private const val DROP_FROM = 40f

/**
 * The character, full body, with smooth colour changes, idle blinking and glints.
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
    CharacterCanvas(outfit, modifier.aspectRatio(AVATAR_ASPECT), animateColors, mood, dropIn, portrait = false) { FULL }
}

private val FULL = Rect(0f, CharacterArt.TOP, CharacterArt.W, CharacterArt.TOP + CharacterArt.H)

/** [box]'s centre and height, widened / narrowed to [aspect] (width / height). */
private fun fit(centerX: Float, centerY: Float, height: Float, aspect: Float) =
    Rect(centerX - height * aspect / 2, centerY - height / 2, centerX + height * aspect / 2, centerY + height / 2)

/**
 * Character creator stage: the whole body at [zoom] 0, the face at 1 (smoothly in between),
 * fitted to whatever shape the stage has.
 */
@Composable
fun CharacterStage(outfit: Outfit, modifier: Modifier = Modifier, mood: Mood = Mood.NORMAL, zoom: Float = 0f) {
    CharacterCanvas(outfit, modifier, animateColors = true, mood = mood, dropIn = null, portrait = false) { size ->
        val aspect = size.width / size.height
        val h = 580f + (122f - 580f) * zoom
        val cy = (CharacterArt.TOP + CharacterArt.H / 2 - 6f) + (83f - (CharacterArt.TOP + CharacterArt.H / 2 - 6f)) * zoom
        fit(100f, cy, h, aspect)
    }
}

/** Portrait crop of the character ([box]), e.g. for tiles and profile pictures. */
@Composable
fun AvatarHead(outfit: Outfit, modifier: Modifier = Modifier, mood: Mood = Mood.NORMAL, box: HeadBox = HeadBox.FACE, dropIn: String? = null) {
    CharacterCanvas(outfit, modifier.aspectRatio(box.rect.width / box.rect.height), animateColors = false, mood, dropIn, portrait = true) { box.rect }
}

@Composable
private fun CharacterCanvas(outfit: Outfit, modifier: Modifier, animateColors: Boolean, mood: Mood, dropIn: String?, portrait: Boolean, boxFor: (Size) -> Rect) {
    val context = LocalContext.current
    val art = remember { CharacterArt.art(context.applicationContext) }
    val posed = remember(art, outfit.tune, outfit.faceShape, outfit.gender, outfit.body) { Posed(art, outfit) }
    val spec = tween<Color>(durationMillis = if (animateColors) 600 else 0)
    val hair by animateColorAsState(Color(outfit.hair), spec, label = "hair")
    val skin by animateColorAsState(Color(outfit.skin), spec, label = "skin")
    val top by animateColorAsState(Color(outfit.shirt), spec, label = "top")
    val stripe by animateColorAsState(Color(outfit.stripe), spec, label = "stripe")
    val acc by animateColorAsState(Color(outfit.accent), spec, label = "acc")
    val bottom by animateColorAsState(Color(outfit.pants), spec, label = "bottom")
    val shoes by animateColorAsState(Color(outfit.shoes), spec, label = "shoes")
    val iris by animateColorAsState(Color(outfit.eyeColor), spec, label = "iris")
    val lip by animateColorAsState(Color(outfit.lipColor), spec, label = "lip")

    var blink by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3200)
            blink = true
            delay(130)
            blink = false
        }
    }
    val glinting = outfit.head == "crown" || outfit.head == "halo"
    val clock = rememberInfiniteTransition(label = "glint")
    val time = clock.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "time")

    val worn = listOf(outfit.back, outfit.head, outfit.face).filter { it.isNotEmpty() }
    val drop = remember { Animatable(if (dropIn != null) DROP_FROM else 0f) }
    var dropId by remember { mutableStateOf(dropIn.orEmpty()) }
    var lastWorn by remember { mutableStateOf(worn) }
    LaunchedEffect(worn) {
        val added = worn.firstOrNull { it !in lastWorn }
        lastWorn = worn
        if (added != null && animateColors) {
            dropId = added
            drop.snapTo(DROP_FROM)
        }
        if (dropId.isNotEmpty()) drop.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 380f))
    }

    val buffer = remember { DoubleBuffer() }
    // The character is redrawn into a bitmap only when it changes (colours, blink, drop-in, size);
    // the glints are the only thing drawn live.
    Spacer(
        modifier.drawWithCache {
            val pal = Palette(skin, hair, top, stripe, acc, bottom, shoes, iris, lip)
            val box = boxFor(size).let { if (portrait) headBox(it, outfit, posed.anchor("neck")) else it }
            val dropping = if (dropId.isNotEmpty() && drop.value != 0f) AccessoryDrop(dropId, drop.value) else null
            val image = if (size.minDimension >= 1f) buffer.draw(size) { drawCharacter(posed, outfit, pal, mood, blink, box, dropping) } else null
            onDrawBehind {
                image?.let { drawImage(it) }
                if (glinting) drawCharacterGlints(posed, outfit, box, time.value)
            }
        },
    )
}

/** The character as a plain bitmap [width] px wide, for places without Compose (home-screen widget). */
fun renderAvatarBitmap(context: Context, outfit: Outfit, mood: Mood = Mood.NORMAL, width: Int = 160, box: HeadBox? = null): Bitmap {
    val rect = box?.rect ?: Rect(0f, CharacterArt.TOP, CharacterArt.W, CharacterArt.TOP + CharacterArt.H)
    val height = (width * rect.height / rect.width).toInt()
    val posed = Posed(CharacterArt.art(context.applicationContext), outfit)
    return rasterize(Size(width.toFloat(), height.toFloat())) {
        drawCharacter(posed, outfit, Palette.of(outfit), mood, blink = false, box = if (box == null) rect else headBox(rect, outfit, posed.anchor("neck")))
    }.asAndroidBitmap()
}
