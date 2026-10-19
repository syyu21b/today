package com.today.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import com.today.app.data.Outfit

/** Sprite area used for the face logo: rows 1..11 (head + collar), columns 1..14 (room for long hair). */
const val FACE_COLS = 14
const val FACE_ROWS = 11

/**
 * Head-and-collar crop of the pixel avatar in the user's current outfit — the same framing as
 * the launcher icon, but drawn live so photo styles and fine-tuned colors show up exactly.
 */
@Composable
fun PixelFace(outfit: Outfit, modifier: Modifier = Modifier) {
    val style = outfit.toSpriteStyle()
    Canvas(modifier.aspectRatio(FACE_COLS / FACE_ROWS.toFloat())) {
        val px = size.width / FACE_COLS
        clipRect {
            drawPixelSprite(Offset(-1 * px, -1 * px), px, style)
        }
    }
}
