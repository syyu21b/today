package com.today.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.today.app.data.Friend
import com.today.app.ui.theme.Mint
import com.today.app.ui.theme.StoryGradient

/** Round avatar showing the friend's pixel character, with optional IG story ring + online dot. */
@Composable
fun FriendAvatar(friend: Friend, size: Dp, storyRing: Boolean = false, dimmed: Boolean = false) {
    Box(Modifier.alpha(if (dimmed) 0.45f else 1f)) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .then(if (storyRing) Modifier.background(StoryGradient) else Modifier)
                .padding(if (storyRing) 3.dp else 0.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .padding(if (storyRing) 2.dp else 0.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF3A2F6B), Color(0xFF241F38)))),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Zoom in on the head/upper body
            PixelAvatar(friend.outfit, Modifier.height(size * 1.3f).offset(y = size * 0.08f), animateColors = false)
        }
        if (friend.online && !dimmed) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(size / 4)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Mint),
            )
        }
    }
}
