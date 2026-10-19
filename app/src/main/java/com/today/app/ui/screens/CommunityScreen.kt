package com.today.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.today.app.data.ChatMessage
import com.today.app.data.Friend
import com.today.app.data.TodayViewModel
import com.today.app.ui.components.PixelAvatar
import com.today.app.ui.theme.Coral
import com.today.app.ui.theme.DmGradient
import com.today.app.ui.theme.Mint
import com.today.app.ui.theme.NightSurfaceHigh
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.StoryGradient
import com.today.app.ui.theme.TextMuted

@Composable
fun CommunityScreen(vm: TodayViewModel, modifier: Modifier = Modifier) {
    val openFriend = vm.friends.firstOrNull { it.id == vm.openChatId }
    BackHandler(enabled = openFriend != null) { vm.openChat(null) }

    AnimatedContent(
        targetState = openFriend,
        transitionSpec = {
            if (targetState != null) slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 } + fadeOut()
            else slideInHorizontally { -it / 3 } + fadeIn() togetherWith slideOutHorizontally { it }
        },
        modifier = modifier.fillMaxSize(),
        label = "dm",
    ) { friend ->
        if (friend == null) FriendList(vm) else DmChat(vm, friend)
    }
}

// ============================ Friend list ============================

@Composable
private fun FriendList(vm: TodayViewModel) {
    LazyColumn(contentPadding = PaddingValues(vertical = 16.dp)) {
        item {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("메시지", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.Search, null, tint = TextMuted)
            }
            Spacer(Modifier.height(16.dp))
        }
        // Story-style row of friends' avatars
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(vm.friends, key = { it.id }) { f ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { vm.openChat(f.id) },
                    ) {
                        FriendAvatar(f, 64.dp, storyRing = true)
                        Spacer(Modifier.height(4.dp))
                        Text(f.name, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = NightSurfaceHigh)
        }
        items(vm.friends, key = { "row-${it.id}" }) { f ->
            val last = vm.chats[f.id]?.lastOrNull()
            val isTyping = vm.typing[f.id] == true
            val isUnread = vm.unread[f.id] == true
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.openChat(f.id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FriendAvatar(f, 56.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(f.name, fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium)
                    Text(
                        when {
                            isTyping -> "입력 중…"
                            last == null -> "LV ${f.level} · 새 대화를 시작해 보세요"
                            last.fromMe -> "나: ${last.text}"
                            else -> last.text
                        },
                        color = if (isUnread || isTyping) Color.White else TextMuted,
                        fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp,
                    )
                }
                if (isUnread) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF3D7BFF)))
                } else {
                    Icon(Icons.Filled.CameraAlt, null, tint = TextMuted)
                }
            }
        }
    }
}

/** Round avatar showing the friend's pixel character, with optional IG story ring + online dot. */
@Composable
private fun FriendAvatar(friend: Friend, size: Dp, storyRing: Boolean = false) {
    Box {
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
        if (friend.online) {
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

// ============================ DM chat ============================

@Composable
private fun DmChat(vm: TodayViewModel, friend: Friend) {
    val messages = vm.chats[friend.id].orEmpty()
    val isTyping = vm.typing[friend.id] == true
    // TextFieldState handles IME composition (Korean 조합 중 글자) correctly when cleared.
    val input = rememberTextFieldState()
    val listState = rememberLazyListState()

    // Keep the newest message in view (list is reversed, so index 0 = bottom).
    LaunchedEffect(messages.size, isTyping) { listState.animateScrollToItem(0) }

    Column(Modifier.fillMaxSize().imePadding()) {
        // ---- Top bar ----
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.openChat(null) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
            }
            FriendAvatar(friend, 40.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(friend.name, fontWeight = FontWeight.Bold)
                Text(
                    if (isTyping) "입력 중…" else if (friend.online) "활동 중" else "@${friend.handle}",
                    fontSize = 12.sp, color = if (friend.online || isTyping) Mint else TextMuted,
                )
            }
        }
        HorizontalDivider(color = NightSurfaceHigh)

        // ---- Messages ----
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (isTyping) item(key = "typing") { TypingBubble(friend) }
            items(messages.asReversed(), key = { it.id }) { m ->
                MessageBubble(m, friend, onDoubleTap = { vm.toggleLike(friend.id, m.id) })
            }
            item(key = "profile") { ChatProfileHeader(friend) }
        }

        // ---- Input ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(NightSurfaceHigh)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(DmGradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            val send = {
                vm.sendMessage(friend.id, input.text.toString())
                input.clearText()
            }
            BasicTextField(
                state = input,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(Color.White),
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                onKeyboardAction = { send() },
                modifier = Modifier.weight(1f),
                decorator = { inner ->
                    if (input.text.isEmpty()) Text("메시지 보내기… / Message…", color = TextMuted)
                    inner()
                },
            )
            AnimatedVisibility(input.text.isNotBlank(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                IconButton(onClick = send) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "보내기", tint = Color(0xFF8E7BFF))
                }
            }
        }
    }
}

@Composable
private fun ChatProfileHeader(friend: Friend) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FriendAvatar(friend, 88.dp)
        Spacer(Modifier.height(8.dp))
        Text(friend.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("@${friend.handle} · LV ${friend.level}", color = TextMuted, fontFamily = PixelFont, fontSize = 12.sp)
    }
}

@Composable
private fun MessageBubble(m: ChatMessage, friend: Friend, onDoubleTap: () -> Unit) {
    val shape = if (m.fromMe) RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    Row(
        Modifier.fillMaxWidth().padding(bottom = if (m.liked) 12.dp else 0.dp),
        horizontalArrangement = if (m.fromMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!m.fromMe) {
            FriendAvatar(friend, 28.dp)
            Spacer(Modifier.width(8.dp))
        }
        Box {
            Column(
                Modifier
                    .widthIn(max = 260.dp)
                    .clip(shape)
                    .then(if (m.fromMe) Modifier.background(DmGradient) else Modifier.background(NightSurfaceHigh))
                    .pointerInput(m.id) { detectTapGestures(onDoubleTap = { onDoubleTap() }) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(m.text, color = Color.White, fontSize = 15.sp)
                Text(
                    m.time, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
            if (m.liked) {
                Box(
                    Modifier
                        .align(if (m.fromMe) Alignment.BottomStart else Alignment.BottomEnd)
                        .offset(y = 14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, NightSurfaceHigh, CircleShape)
                        .padding(3.dp),
                ) {
                    Icon(Icons.Filled.Favorite, "좋아요", tint = Coral, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
private fun TypingBubble(friend: Friend) {
    val t = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.Bottom) {
        FriendAvatar(friend, 28.dp)
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(NightSurfaceHigh)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(3) { i ->
                val a by t.animateFloat(
                    initialValue = 0.3f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(500, delayMillis = i * 150), RepeatMode.Reverse),
                    label = "dot$i",
                )
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = a)))
            }
        }
    }
}
