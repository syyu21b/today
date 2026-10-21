package com.today.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.today.app.data.Backdrops
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pixel-art scene behind the character ([id] from [Backdrops]). Clouds drift, stars twinkle,
 * and space gets the odd shooting star; [animated] = false keeps it still (small previews).
 */
@Composable
fun Backdrop(
    id: String,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val clock = rememberInfiniteTransition(label = "backdrop")
    val time = clock.animateFloat(0f, 1f, infiniteRepeatable(tween(12_000, easing = LinearEasing)), label = "time")
    Box(
        modifier.drawBehind {
            val t = if (animated && id != Backdrops.DEFAULT) time.value else 0.3f
            when (id) {
                "sky" -> drawSky(t)
                "sunset" -> drawSunset()
                "night" -> drawNight(t)
                "space" -> drawSpace(t)
                else -> drawRect(Brush.verticalGradient(listOf(Color(0xFF3A2F6B), Color(0xFF1C1830))))
            }
        },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** One "pixel" of the scene, so a small card and the big stage look equally chunky. */
private fun DrawScope.unit() = size.minDimension / 26f

/** Fills the cell at grid ([gx], [gy]) where the grid starts at ([ox], [oy]) px. */
private fun DrawScope.cell(ox: Float, oy: Float, gx: Int, gy: Int, u: Float, color: Color) =
    drawRect(color, Offset(ox + gx * u, oy + gy * u), Size(u + 0.5f, u + 0.5f))

/** Pattern rows of '#' (main) / '+' (shade), top-left at ([x], [y]) px. */
private fun DrawScope.pattern(rows: List<String>, x: Float, y: Float, u: Float, main: Color, shade: Color = main) {
    rows.forEachIndexed { gy, row ->
        row.forEachIndexed { gx, ch ->
            when (ch) {
                '#' -> cell(x, y, gx, gy, u, main)
                '+' -> cell(x, y, gx, gy, u, shade)
            }
        }
    }
}

private fun DrawScope.disc(cx: Float, cy: Float, r: Int, u: Float, color: Color, keep: (Int, Int) -> Boolean = { _, _ -> true }) {
    for (gy in -r..r) for (gx in -r..r) {
        if (gx * gx + gy * gy <= r * r + r / 2 && keep(gx, gy)) cell(cx, cy, gx, gy, u, color)
    }
}

/** Fixed star field: (x, y) as fractions of the size plus a twinkle phase. */
private class Star(val x: Float, val y: Float, val phase: Float, val big: Boolean)

private fun stars(seed: Int, n: Int, maxY: Float): List<Star> {
    val r = Random(seed)
    return List(n) { Star(r.nextFloat(), r.nextFloat() * maxY, r.nextFloat(), r.nextInt(5) == 0) }
}

private val NightStars = stars(7, 26, 0.7f)
private val SpaceStars = stars(11, 40, 1f)

private fun DrawScope.drawStars(list: List<Star>, t: Float, u: Float) {
    for (s in list) {
        val a = 0.35f + 0.65f * (0.5f + 0.5f * sin(2 * PI * (t * 4 + s.phase)).toFloat())
        val x = floor(s.x * size.width / u) * u
        val y = floor(s.y * size.height / u) * u
        val c = Color(0xFFFFF6D8).copy(alpha = a)
        drawRect(c, Offset(x, y), Size(u * 0.6f, u * 0.6f))
        if (s.big && a > 0.8f) {
            val arm = c.copy(alpha = (a - 0.8f) * 3f)
            drawRect(arm, Offset(x - u * 0.6f, y), Size(u * 0.6f, u * 0.6f))
            drawRect(arm, Offset(x + u * 0.6f, y), Size(u * 0.6f, u * 0.6f))
            drawRect(arm, Offset(x, y - u * 0.6f), Size(u * 0.6f, u * 0.6f))
            drawRect(arm, Offset(x, y + u * 0.6f), Size(u * 0.6f, u * 0.6f))
        }
    }
}

private val Cloud = listOf(
    "...###.....",
    "..#####.##.",
    ".##########",
    "+++++++++++",
)

private fun DrawScope.drawSky(t: Float) {
    val u = unit()
    drawRect(Brush.verticalGradient(listOf(Color(0xFF4FA8F0), Color(0xFF9AD6FF), Color(0xFFD8F1FF))))
    // Sun with a soft ring
    val sx = size.width - u * 5
    disc(sx, u * 4, 4, u, Color(0x33FFF6B0))
    disc(sx, u * 4, 3, u, Color(0xFFFFE58A))
    disc(sx, u * 4, 2, u, Color(0xFFFFF3C4))
    // Clouds drift right and wrap around
    val span = size.width + u * 12
    listOf(0f to 0.18f, 0.45f to 0.38f, 0.75f to 0.08f).forEachIndexed { i, (start, y) ->
        val speed = if (i == 1) 1f else 2f
        val x = ((start + t * speed) % 1f) * span - u * 12
        pattern(Cloud, x, size.height * y, u, Color.White, Color(0xFFCFE6F7))
    }
    // Grass with a darker rim and a few tufts
    val ground = size.height - u * 3
    drawRect(Color(0xFF5DBB5A), Offset(0f, ground), Size(size.width, u * 3 + 1))
    drawRect(Color(0xFF3E9A47), Offset(0f, ground), Size(size.width, u * 0.7f))
    var gx = u
    while (gx < size.width) {
        drawRect(Color(0xFF7FD36E), Offset(gx, ground + u * 1.4f), Size(u * 0.6f, u * 0.6f))
        gx += u * 3.3f
    }
}

private fun DrawScope.drawSunset() {
    val u = unit()
    drawRect(Brush.verticalGradient(listOf(Color(0xFF2E2462), Color(0xFF8C3D86), Color(0xFFF0607A), Color(0xFFFFA65C))))
    // Retro sun sliced by horizontal gaps, sitting behind the hills
    val cx = floor(size.width / 2f / u) * u
    val cy = size.height - u * 6
    disc(cx, cy, 7, u, Color(0x40FFD27A))
    disc(cx, cy, 6, u, Color(0xFFFFC25C)) { _, gy -> !(gy > 0 && gy % 2 == 0) }
    disc(cx, cy, 4, u, Color(0xFFFFE08A)) { _, gy -> gy < -1 }
    // Two layers of stepped hills
    val far = Color(0xFF5B2A63)
    val near = Color(0xFF2A1638)
    val cols = (size.width / u).toInt() + 1
    for (gx in 0..cols) {
        val hFar = 5 + (2.5f * sin(gx * 0.35f) + 1.5f * sin(gx * 0.9f + 1f)).toInt()
        val hNear = 3 + (1.5f * sin(gx * 0.5f + 2f) + sin(gx * 1.3f)).toInt()
        drawRect(far, Offset(gx * u, size.height - hFar * u), Size(u + 0.5f, hFar * u))
        drawRect(near, Offset(gx * u, size.height - hNear * u), Size(u + 0.5f, hNear * u))
    }
    // A pair of far-off birds
    pattern(listOf("#.#", ".#."), u * 4, u * 5, u * 0.7f, Color(0xFF2A1638))
    pattern(listOf("#.#", ".#."), u * 7, u * 3.5f, u * 0.6f, Color(0xFF2A1638))
}

private fun DrawScope.drawNight(t: Float) {
    val u = unit()
    drawRect(Brush.verticalGradient(listOf(Color(0xFF070C24), Color(0xFF14224F), Color(0xFF26407A))))
    drawStars(NightStars, t, u)
    // Crescent moon with a faint halo
    val mx = size.width - u * 6
    val my = u * 5
    disc(mx, my, 5, u, Color(0x22FFF6D0))
    disc(mx, my, 3, u, Color(0xFFFFF2B8)) { gx, gy -> (gx - 2) * (gx - 2) + (gy + 1) * (gy + 1) > 5 }
    // Sleeping town: rooftops with a few lit windows
    val cols = (size.width / u).toInt() + 1
    val roof = Color(0xFF0B1230)
    for (gx in 0..cols) {
        val h = 3 + ((gx / 3) * 7 % 4)
        drawRect(roof, Offset(gx * u, size.height - h * u), Size(u + 0.5f, h * u))
        if (gx % 5 == 1 && h > 3) {
            val glow = 0.6f + 0.4f * sin(2 * PI * (t * 2 + gx * 0.13f)).toFloat()
            drawRect(Color(0xFFFFD36B).copy(alpha = glow), Offset(gx * u, size.height - (h - 1) * u), Size(u * 0.7f, u * 0.7f))
        }
    }
}

private fun DrawScope.nebula(color: Color, center: Offset, radius: Float) =
    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center, radius), radius, center)

private fun DrawScope.drawSpace(t: Float) {
    val u = unit()
    drawRect(Brush.verticalGradient(listOf(Color(0xFF05030D), Color(0xFF1A0B3A), Color(0xFF34155C))))
    // Nebula haze
    nebula(Color(0x55B04BFF), Offset(size.width * 0.2f, size.height * 0.8f), size.minDimension * 0.6f)
    nebula(Color(0x3340C8FF), Offset(size.width * 0.7f, size.height * 0.25f), size.minDimension * 0.5f)
    drawStars(SpaceStars, t, u)
    // Ringed planet: back half of the ring, planet, front half
    // Top right, clear of the outfit name in the corner
    val px = floor((size.width - u * 9) / u) * u
    val py = u * 6
    val ring = Color(0xFFFFC98A)
    for (gx in (-7..-5) + (5..7)) cell(px, py, gx, 0, u, ring)
    disc(px, py, 4, u, Color(0xFF7D5CFF))
    disc(px, py, 4, u, Color(0xFFA48BFF)) { gx, gy -> gx + gy < -2 }
    for (gx in -5..5) cell(px, py, gx, 1, u, ring)
    // A shooting star crosses once per loop
    val p = (t - 0.55f) / 0.08f
    if (p in 0f..1f) {
        val sx = size.width * (0.95f - 0.6f * p)
        val sy = size.height * (0.08f + 0.3f * p)
        for (i in 0..5) {
            drawRect(Color.White.copy(alpha = 1f - i / 6f), Offset(sx + i * u * 0.8f, sy - i * u * 0.4f), Size(u * 0.7f, u * 0.7f))
        }
    }
}
