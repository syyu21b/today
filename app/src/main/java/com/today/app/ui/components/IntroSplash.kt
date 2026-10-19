package com.today.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.today.app.data.Outfit
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.Night
import com.today.app.ui.theme.PixelFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Launch intro: the user's own character (current outfit, drawn live) on a gold circle, which
 * lifts while the app name fades in below. The system splash before it is a plain background,
 * so there's no jump from a fixed icon to the personalised logo.
 */
@Composable
fun IntroSplash(outfit: Outfit, onFinished: () -> Unit) {
    val logoY = remember { Animatable(0f) }
    val nameAlpha = remember { Animatable(0f) }
    val nameY = remember { Animatable(16f) }

    LaunchedEffect(Unit) {
        delay(150)
        launch { logoY.animateTo(-36f, tween(500, easing = FastOutSlowInEasing)) }
        delay(150)
        launch { nameY.animateTo(0f, tween(450, easing = FastOutSlowInEasing)) }
        nameAlpha.animateTo(1f, tween(450))
        delay(700)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Night)
            // Swallow touches so nothing underneath reacts while the intro plays.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .offset { IntOffset(0, logoY.value.dp.roundToPx()) }
                .size(160.dp)
                .clip(CircleShape)
                .background(Gold),
            contentAlignment = Alignment.Center,
        ) {
            // Same framing as the launcher icon: 50.4 of the 72 visible units.
            PixelFace(outfit, Modifier.width(112.dp).offset(y = 2.dp))
        }
        Text(
            "Today",
            color = Color.White,
            fontFamily = PixelFont,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            modifier = Modifier
                .offset { IntOffset(0, (logoY.value + 80f + 44f + nameY.value).dp.roundToPx()) }
                .alpha(nameAlpha.value),
        )
    }
}
