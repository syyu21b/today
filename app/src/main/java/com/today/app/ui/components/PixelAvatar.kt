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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import com.today.app.data.FaceFeature
import com.today.app.data.Mood
import com.today.app.data.Outfit
import com.today.app.data.TopStyle
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/** Width : height of the full-body character. */
const val AVATAR_ASPECT = CharacterArt.W / CharacterArt.H

/** Crops of the art box (art coordinates) used for portraits. */
enum class HeadBox(val rect: Rect) {
    /** Face and hair, square. */
    FACE(Rect(42f, 4f, 158f, 120f)),
    /** Head with room for tall hats. */
    HAT(Rect(28f, -45f, 172f, 147f)),
    /** Head and upper body. */
    BUST(Rect(22f, -22f, 178f, 228f)),
    /** Close-up of the eyes and brows. */
    EYES(Rect(62f, 60f, 138f, 106f)),
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

/** Part keys in drawing order. Accessory parts are tagged so a drop-in can move them. */
private fun partKeys(o: Outfit, mood: Mood, blink: Boolean): List<Pair<String, String?>> {
    val bt = o.body.name
    val accs = listOf(o.back, o.head, o.face).filter { it.isNotEmpty() }
    val top = o.topStyle.name.lowercase()
    val sleeve = if (o.topStyle == TopStyle.TEE && o.shortSleeve) "short" else "long"
    val keys = ArrayList<Pair<String, String?>>()
    keys += "hairBack.${o.style.name.lowercase()}" to null
    accs.forEach { keys += "accBack.$it" to it }
    keys += "body.$bt" to null
    keys += "arms.$bt" to null
    if (o.topStyle != TopStyle.DRESS) keys += "bottom.$bt.${o.bottom.name.lowercase()}" to null
    keys += "shoes.$bt.${o.shoeStyle.name.lowercase()}" to null
    keys += "top.$bt.$top.$sleeve" to null
    if (o.striped) keys += "stripes.$bt.$top.$sleeve" to null
    keys += "head.${o.faceShape.name.lowercase()}" to null
    keys += "blush.${if (o.blush) 1 else 0}" to null
    keys += "nose" to null
    for (f in FaceFeature.entries) {
        if (f !in o.features || (f == FaceFeature.MUSTACHE && FaceFeature.BEARD in o.features)) continue
        keys += "feature.${f.name.lowercase()}" to null
    }
    keys += "mouth.${moodKey(mood)}" to null
    keys += "eyes.$bt.${o.eyeStyle.name.lowercase()}.${if (blink && mood in OPEN_EYES) "blink" else moodKey(mood)}" to null
    keys += "hairFront.${o.style.name.lowercase()}" to null
    keys += "brows.${o.browStyle.name.lowercase()}.${moodKey(mood)}" to null
    accs.forEach { keys += "accFront.$it" to it }
    return keys
}

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

/**
 * Draws the character so the art box [box] (art coordinates) fills this draw scope.
 * [time] (0..1, looping) makes crowns and halos glint.
 */
private fun DrawScope.drawCharacter(
    parts: Map<String, List<ArtLayer>>,
    outfit: Outfit,
    pal: Palette,
    mood: Mood,
    blink: Boolean,
    box: Rect = Rect(0f, CharacterArt.TOP, CharacterArt.W, CharacterArt.TOP + CharacterArt.H),
    time: Float = 0f,
    drop: AccessoryDrop? = null,
) {
    val k = size.width / box.width
    clipRect {
        scale(k, k, pivot = Offset.Zero) {
            translate(-box.left, -box.top) {
                for ((key, accId) in partKeys(outfit, mood, blink)) {
                    val layers = parts[key] ?: continue
                    val dy = if (drop != null && accId == drop.id) -drop.offset else 0f
                    translate(0f, dy) { layers.forEach { drawLayer(it, pal) } }
                }
                if (outfit.head == "crown" || outfit.head == "halo") drawGlints(if (outfit.head == "crown") CROWN_GLINTS else HALO_GLINTS, time)
            }
        }
    }
}

private val CROWN_GLINTS = listOf(Offset(76f, 0f), Offset(100f, -8f), Offset(124f, 0f))
private val HALO_GLINTS = listOf(Offset(76f, -8f), Offset(124f, -5f), Offset(100f, -12f))

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
    CharacterCanvas(outfit, modifier.aspectRatio(AVATAR_ASPECT), animateColors, mood, dropIn) { FULL }
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
    CharacterCanvas(outfit, modifier, animateColors = true, mood = mood, dropIn = null) { size ->
        val aspect = size.width / size.height
        val h = 570f + (175f - 570f) * zoom
        val cy = (CharacterArt.TOP + CharacterArt.H / 2 - 6f) + (55f - (CharacterArt.TOP + CharacterArt.H / 2 - 6f)) * zoom
        fit(100f, cy, h, aspect)
    }
}

/** Portrait crop of the character ([box]), e.g. for tiles and profile pictures. */
@Composable
fun AvatarHead(outfit: Outfit, modifier: Modifier = Modifier, mood: Mood = Mood.NORMAL, box: HeadBox = HeadBox.FACE, dropIn: String? = null) {
    CharacterCanvas(outfit, modifier.aspectRatio(box.rect.width / box.rect.height), animateColors = false, mood, dropIn) { box.rect }
}

@Composable
private fun CharacterCanvas(outfit: Outfit, modifier: Modifier, animateColors: Boolean, mood: Mood, dropIn: String?, boxFor: (Size) -> Rect) {
    val context = LocalContext.current
    val parts = remember { CharacterArt.parts(context.applicationContext) }
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

    Canvas(modifier) {
        val pal = Palette(skin, hair, top, stripe, acc, bottom, shoes, iris, lip)
        drawCharacter(
            parts, outfit, pal, mood, blink,
            box = boxFor(size),
            time = if (glinting) time.value else 0f,
            drop = if (dropId.isNotEmpty() && drop.value != 0f) AccessoryDrop(dropId, drop.value) else null,
        )
    }
}

/** The character as a plain bitmap [width] px wide, for places without Compose (home-screen widget). */
fun renderAvatarBitmap(context: Context, outfit: Outfit, mood: Mood = Mood.NORMAL, width: Int = 160, box: HeadBox? = null): Bitmap {
    val rect = box?.rect ?: Rect(0f, CharacterArt.TOP, CharacterArt.W, CharacterArt.TOP + CharacterArt.H)
    val height = (width * rect.height / rect.width).toInt()
    val image = ImageBitmap(width, height)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, GraphicsCanvas(image), Size(width.toFloat(), height.toFloat())) {
        drawCharacter(CharacterArt.parts(context.applicationContext), outfit, Palette.of(outfit), mood, blink = false, box = rect)
    }
    return image.asAndroidBitmap()
}
