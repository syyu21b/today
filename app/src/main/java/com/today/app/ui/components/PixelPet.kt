package com.today.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.drawscope.withTransform
import com.today.app.data.PetSpecies
import com.today.app.data.PetStage
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * The pet at its growth [stage]: an egg that wobbles, or the animal hopping and wagging
 * (two frames of vector art, see PetArt). Smaller stages are drawn smaller inside the same box, standing on its bottom.
 * [onPet] makes it tappable: it jumps and hearts float up.
 */
@Composable
fun PixelPet(
    species: PetSpecies,
    stage: PetStage,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    onPet: (() -> Unit)? = null,
) {
    val clock = rememberInfiniteTransition(label = "pet")
    val t by clock.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val jump = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val hearts = remember { mutableStateListOf<Heart>() }
    var heartId by remember { mutableIntStateOf(0) }

    LaunchedEffect(hearts.size) {
        // Drop hearts once they've floated away.
        if (hearts.isNotEmpty()) {
            kotlinx.coroutines.delay(1100)
            if (hearts.isNotEmpty()) hearts.removeAt(0)
        }
    }

    val tap = Modifier.then(
        if (onPet != null) {
            Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onPet()
                hearts += Heart(heartId++, System.currentTimeMillis())
                scope.launch {
                    jump.snapTo(0f)
                    jump.animateTo(1f, tween(380))
                    jump.snapTo(0f)
                }
            }
        } else Modifier,
    )

    val context = LocalContext.current
    val art = remember { PetArt.get(context.applicationContext).getValue(species) }
    Box(modifier.aspectRatio(PET_W / PET_H).then(tap)) {
        Canvas(Modifier.fillMaxSize()) {
            val time = if (animated) t else 0f
            // Movement is measured in tenths of the art box width (the old pixel grid).
            val cell = size.width / 24f
            val scale = stage.scale
            // Hop: a little bounce twice per loop (fish float instead), plus a big jump when petted.
            val hop = when {
                stage == PetStage.EGG -> 0f
                species == PetSpecies.FISH -> sin(time * 2 * PI).toFloat() * 0.6f
                else -> (-kotlin.math.abs(sin(time * 2 * PI * 2)).toFloat() * 0.5f).coerceAtMost(0f)
            }
            val jumpUp = sin(jump.value * PI).toFloat() * 4f
            val pivot = Offset(size.width / 2, size.height)
            val k = size.width / PET_W
            withTransform({
                scale(scale, scale, pivot)
                translate(0f, (hop - jumpUp) * cell)
                if (stage == PetStage.EGG) rotate(sin(time * 2 * PI * 2).toFloat() * 6f, pivot)
                scale(k, k, Offset.Zero)
            }) {
                val layers = if (stage == PetStage.EGG) art.egg else art.frames[if (animated && time % 0.5f > 0.25f) 1 else 0]
                layers.forEach { drawArtLayer(it) }
            }
            val now = System.currentTimeMillis()
            hearts.forEach { h -> drawHeart(h, now) }
        }
    }
}

private class Heart(val id: Int, val born: Long)

private val HeartFill = Color(0xFFFF4F8B)
private val HeartLine = Color(0xFFB8205A)

private fun DrawScope.drawHeart(h: Heart, now: Long) {
    val age = ((now - h.born) / 1000f).coerceIn(0f, 1f)
    if (age >= 1f) return
    val x = size.width * (0.6f + 0.18f * sin(h.id * 1.7f))
    val y = size.height * (0.4f - 0.35f * age)
    val alpha = 1f - age
    val w = size.width * 0.05f
    val heart = Path().apply {
        moveTo(x, y + w * 0.9f)
        cubicTo(x - w * 1.5f, y - w * 0.1f, x - w * 0.9f, y - w * 1.2f, x, y - w * 0.45f)
        cubicTo(x + w * 0.9f, y - w * 1.2f, x + w * 1.5f, y - w * 0.1f, x, y + w * 0.9f)
        close()
    }
    drawPath(heart, HeartFill, alpha = alpha)
    drawPath(heart, HeartLine, alpha = alpha, style = Stroke(w * 0.18f))
    drawCircle(Color.White.copy(alpha = alpha * 0.8f), w * 0.2f, Offset(x - w * 0.55f, y - w * 0.35f))
}
