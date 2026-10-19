package com.today.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.today.app.R
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.Night
import com.today.app.ui.theme.PixelFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Picks up exactly where the system splash leaves off (same 160dp gold circle with the face,
 * centered on the same background), then lifts the logo and fades the app name in below it.
 */
@Composable
fun IntroSplash(onFinished: () -> Unit) {
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
        // System splash geometry: 240dp icon drawable, inner 160dp circle visible.
        Box(
            Modifier
                .offset { IntOffset(0, logoY.value.dp.roundToPx()) }
                .size(160.dp)
                .clip(CircleShape)
                .background(Gold),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.requiredSize(240.dp))
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
