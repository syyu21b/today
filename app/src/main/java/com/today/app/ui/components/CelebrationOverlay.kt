package com.today.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.today.app.R

/**
 * Plays the confetti Lottie (res/raw/confetti.json) once every time [trigger] changes.
 * Draw it on top of the screen; it doesn't consume touches.
 */
@Composable
fun CelebrationOverlay(trigger: Int, modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
    val animatable = rememberLottieAnimatable()

    LaunchedEffect(trigger, composition) {
        if (trigger > 0 && composition != null) {
            animatable.animate(composition, iterations = 1)
        }
    }

    if (animatable.isPlaying) {
        LottieAnimation(
            composition = composition,
            progress = { animatable.progress },
            modifier = modifier.fillMaxSize(),
        )
    }
}
