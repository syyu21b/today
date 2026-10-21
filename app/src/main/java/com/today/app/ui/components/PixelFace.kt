package com.today.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.today.app.data.Outfit

/**
 * Face portrait of the user's current character (same framing as the launcher icon idea),
 * drawn live so every customisation shows up exactly.
 */
@Composable
fun PixelFace(outfit: Outfit, modifier: Modifier = Modifier) {
    AvatarHead(outfit, modifier, box = HeadBox.FACE)
}
