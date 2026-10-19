package com.today.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.today.app.data.Facing
import com.today.app.data.TodayViewModel
import com.today.app.data.WorldMap
import com.today.app.ui.components.drawPixelSprite
import com.today.app.ui.components.toSpriteColors
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.NightSurfaceHigh
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.TextMuted
import com.today.app.ui.theme.Violet
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun WorldScreen(vm: TodayViewModel, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TODAY 마을", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            Text("👣 ${vm.steps}", fontFamily = PixelFont, color = TextMuted)
        }
        Text("A 버튼으로 주민·건물과 대화해 보세요", color = TextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val tile = minOf(maxWidth / WorldMap.cols, maxHeight / WorldMap.rows)
            Box(
                Modifier
                    .size(tile * WorldMap.cols, tile * WorldMap.rows)
                    .clip(RoundedCornerShape(12.dp))
                    .border(3.dp, Color(0xFF0E0C16), RoundedCornerShape(12.dp)),
            ) {
                WorldCanvas(vm, Modifier.fillMaxSize())
                androidx.compose.animation.AnimatedVisibility(
                    visible = vm.worldDialog != null,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    DialogBox(vm.worldDialog.orEmpty(), onClick = vm::dismissWorldDialog)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DPad(onMove = vm::move)
            Spacer(Modifier.weight(1f))
            ActionButton(onClick = vm::interact)
        }
    }
}

@Composable
private fun WorldCanvas(vm: TodayViewModel, modifier: Modifier) {
    val moveSpec = tween<Float>(durationMillis = 140, easing = LinearEasing)
    val ax by animateFloatAsState(vm.playerX.toFloat(), moveSpec, label = "x")
    val ay by animateFloatAsState(vm.playerY.toFloat(), moveSpec, label = "y")
    val moving = abs(ax - vm.playerX) > 0.01f || abs(ay - vm.playerY) > 0.01f

    val t = rememberInfiniteTransition(label = "world")
    val wave by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "wave")
    val walk by t.animateFloat(0f, 2f, infiniteRepeatable(tween(280, easing = LinearEasing)), label = "walk")
    val bob by t.animateFloat(0f, 2f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "bob")

    val playerColors = vm.character.outfit.toSpriteColors()
    val facing = vm.facing

    Canvas(modifier) {
        val ts = size.width / WorldMap.cols
        for (y in 0 until WorldMap.rows) for (x in 0 until WorldMap.cols) {
            drawTile(WorldMap.tileAt(x, y), x, y, ts, wave)
        }

        // Draw characters back-to-front so lower ones overlap upper ones.
        data class Entity(val x: Float, val y: Float, val draw: (Offset, Float) -> Unit)
        val entities = buildList {
            WorldMap.npcs.forEach { npc ->
                add(Entity(npc.x.toFloat(), npc.y.toFloat()) { tl, px ->
                    val idleBob = if (bob < 1f) 0f else -px
                    drawPixelSprite(tl.copy(y = tl.y + idleBob), px, npc.outfit.toSpriteColors())
                    drawExclamation(Offset(tl.x + ts / 2, tl.y - px * 3), px)
                })
            }
            add(Entity(ax, ay) { tl, px ->
                val step = if (moving) (if (walk < 1f) 1 else 2) else 0
                drawPixelSprite(tl, px, playerColors, step = step, back = facing == Facing.UP)
            })
        }.sortedBy { it.y }

        entities.forEach { e ->
            val px = ts / 16f
            val left = e.x * ts
            val top = (e.y + 1) * ts - px * 24
            // shadow
            drawOval(Color.Black.copy(alpha = 0.25f), Offset(left + px * 2, (e.y + 1) * ts - px * 2.5f), Size(px * 12, px * 3))
            e.draw(Offset(left, top), px)
        }
    }
}

private fun DrawScope.drawExclamation(center: Offset, px: Float) {
    val s = Size(px * 2, px * 2)
    drawRect(Gold, Offset(center.x - px, center.y - px * 7), Size(px * 2, px * 4))
    drawRect(Gold, Offset(center.x - px, center.y - px * 2), s)
}

private fun DrawScope.drawTile(c: Char, x: Int, y: Int, ts: Float, wave: Float) {
    val o = Offset(x * ts, y * ts)
    val full = Size(ts + 0.5f, ts + 0.5f)
    val u = ts / 8f
    fun block(cx: Float, cy: Float, w: Float, h: Float, color: Color) =
        drawRect(color, Offset(o.x + cx * u, o.y + cy * u), Size(w * u, h * u))

    val grass = if ((x + y) % 2 == 0) Color(0xFF5DBB63) else Color(0xFF56B05C)
    when (c) {
        'P' -> {
            drawRect(Color(0xFFD9B97A), o, full)
            block(1f, 2f, 1f, 1f, Color(0xFFC9A766)); block(5f, 5f, 1f, 1f, Color(0xFFC9A766))
        }
        'W' -> {
            drawRect(Color(0xFF3A8EDB), o, full)
            val shift = ((wave * 8 + x + y * 3) % 8)
            block(shift, 2f, 2f, 0.6f, Color(0xFF8CC8FF))
            block((shift + 4) % 8, 5.5f, 2f, 0.6f, Color(0xFF8CC8FF))
        }
        'T' -> {
            drawRect(grass, o, full)
            block(3f, 5f, 2f, 3f, Color(0xFF7A4E2D))
            block(1f, 1f, 6f, 4.5f, Color(0xFF2E7D32))
            block(2f, 0f, 4f, 1f, Color(0xFF2E7D32))
            block(2f, 1.5f, 2f, 1.5f, Color(0xFF43A047))
        }
        'F' -> {
            drawRect(grass, o, full)
            block(1f, 2f, 1f, 1f, Color(0xFFFF7EB6)); block(5f, 1f, 1f, 1f, Color(0xFFFFE066))
            block(3f, 5f, 1f, 1f, Color(0xFFFFFFFF)); block(6f, 6f, 1f, 1f, Color(0xFFFF7EB6))
        }
        'R' -> {
            drawRect(Color(0xFFC0392B), o, full)
            for (i in 0..3) block(0f, i * 2f + 1.5f, 8f, 0.5f, Color(0xFF962D22))
        }
        'H' -> {
            drawRect(Color(0xFFF1E3C6), o, full)
            block(2f, 2f, 4f, 3.5f, Color(0xFF6B4F35))
            block(2.5f, 2.5f, 3f, 2.5f, Color(0xFF8CD3FF))
            block(3.75f, 2.5f, 0.5f, 2.5f, Color(0xFF6B4F35))
        }
        'D' -> {
            drawRect(Color(0xFFF1E3C6), o, full)
            block(2f, 1.5f, 4f, 6.5f, Color(0xFF7A4E2D))
            block(2.5f, 2f, 3f, 6f, Color(0xFF9A6638))
            block(4.6f, 4.8f, 0.7f, 0.7f, Gold)
        }
        'S' -> {
            drawRect(grass, o, full)
            block(3.5f, 4f, 1f, 4f, Color(0xFF7A4E2D))
            block(1f, 1.5f, 6f, 3f, Color(0xFFB07A44))
            block(1.5f, 2.5f, 5f, 0.4f, Color(0xFF7A4E2D)); block(1.5f, 3.4f, 4f, 0.4f, Color(0xFF7A4E2D))
        }
        else -> drawRect(grass, o, full)
    }
}

@Composable
private fun DialogBox(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xF01B1530))
            .border(3.dp, Color.White, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Column {
            Text(text, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
            Text("▼ 탭해서 닫기", color = TextMuted, fontSize = 11.sp, fontFamily = PixelFont, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
private fun DPad(onMove: (Facing) -> Unit) {
    val size = 52.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HoldButton(Icons.Filled.KeyboardArrowUp, size) { onMove(Facing.UP) }
        Row(horizontalArrangement = Arrangement.spacedBy(size)) {
            HoldButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, size) { onMove(Facing.LEFT) }
            HoldButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, size) { onMove(Facing.RIGHT) }
        }
        HoldButton(Icons.Filled.KeyboardArrowDown, size) { onMove(Facing.DOWN) }
    }
}

/** Fires once on press, then repeats while held — like a real game pad. */
@Composable
private fun HoldButton(icon: ImageVector, size: androidx.compose.ui.unit.Dp, onAction: () -> Unit) {
    val scope = rememberCoroutineScope()
    val action by rememberUpdatedState(onAction)
    var pressed by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(if (pressed) Violet else NightSurfaceHigh)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    pressed = true
                    val job = scope.launch {
                        action()
                        delay(260)
                        while (isActive) {
                            action()
                            delay(150)
                        }
                    }
                    waitForUpOrCancellation()
                    job.cancel()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun ActionButton(onClick: () -> Unit) {
    Box(
        Modifier
            .padding(end = 12.dp)
            .size(76.dp)
            .clip(CircleShape)
            .background(Gold)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("A", fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Color(0xFF14121F))
    }
}
