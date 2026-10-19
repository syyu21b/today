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
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.today.app.i18n.LocalStrings
import com.today.app.ui.components.FriendAvatar
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
    val s = LocalStrings.current
    LazyColumn(contentPadding = PaddingValues(vertical = 16.dp)) {
        item {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(s.messagesTitle, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.Search, s.search, tint = TextMuted)
            }
            Spacer(Modifier.height(16.dp))
        }
        // Story-style row of friends' avatars
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(vm.visibleFriends, key = { it.id }) { f ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { vm.openChat(f.id) },
                    ) {
                        FriendAvatar(f, 64.dp, storyRing = true)
                        Spacer(Modifier.height(4.dp))
                        Text(s.friendName(f), fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = NightSurfaceHigh)
        }
        if (vm.visibleFriends.isEmpty()) {
            item { Text(s.noFriends, color = TextMuted, modifier = Modifier.padding(24.dp)) }
        }
        items(vm.visibleFriends, key = { "row-${it.id}" }) { f ->
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
                    Text(s.friendName(f), fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium)
                    Text(
                        when {
                            isTyping -> s.typing
                            last == null -> s.startChat(f.level)
                            last.fromMe -> s.youPrefix(s.messageText(last))
                            else -> s.messageText(last)
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

// ============================ DM chat ============================

@Composable
private fun DmChat(vm: TodayViewModel, friend: Friend) {
    val s = LocalStrings.current
    val st = vm.settings
    var menu by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val messages = vm.chats[friend.id].orEmpty()
    val isTyping = vm.typing[friend.id] == true
    // TextFieldState handles IME composition (e.g. Hangul syllables in progress) correctly when cleared.
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
            }
            FriendAvatar(friend, 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(s.friendName(friend), fontWeight = FontWeight.Bold)
                Text(
                    if (isTyping) s.typing else if (friend.online) s.activeNow else "@${friend.handle}",
                    fontSize = 12.sp, color = if (friend.online || isTyping) Mint else TextMuted,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = s.friendMenu) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(s.block) },
                        leadingIcon = { Icon(Icons.Filled.Block, null) },
                        onClick = { menu = false; confirmBlock = true },
                    )
                    DropdownMenuItem(
                        text = { Text(s.deleteFriend, color = Coral) },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Coral) },
                        onClick = { menu = false; confirmDelete = true },
                    )
                }
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
            val lastMine = messages.lastOrNull { it.fromMe }?.id
            items(messages.asReversed(), key = { it.id }) { m ->
                MessageBubble(
                    m, friend, fontSize = st.chatFontSize.sp,
                    showRead = st.readReceipts && m.id == lastMine && m.read,
                    onDoubleTap = { vm.toggleLike(friend.id, m.id) },
                )
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
                textStyle = TextStyle(color = Color.White, fontSize = (st.chatFontSize.sp + 1).sp),
                cursorBrush = SolidColor(Color.White),
                lineLimits = if (st.enterToSend) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(maxHeightInLines = 4),
                keyboardOptions = KeyboardOptions(imeAction = if (st.enterToSend) ImeAction.Send else ImeAction.Default),
                onKeyboardAction = if (st.enterToSend) KeyboardActionHandler { send() } else null,
                modifier = Modifier.weight(1f),
                decorator = { inner ->
                    if (input.text.isEmpty()) Text(s.messagePlaceholder, color = TextMuted)
                    inner()
                },
            )
            AnimatedVisibility(input.text.isNotBlank(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                IconButton(onClick = send) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = s.send, tint = Color(0xFF8E7BFF))
                }
            }
        }
    }

    ChatConfirmDialogs(vm, friend, confirmBlock, confirmDelete, onDismiss = { confirmBlock = false; confirmDelete = false })
}

@Composable
private fun ChatConfirmDialogs(
    vm: TodayViewModel,
    friend: Friend,
    confirmBlock: Boolean,
    confirmDelete: Boolean,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    val name = s.friendName(friend)
    if (confirmBlock) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(s.blockConfirmTitle(name)) },
            text = { Text(s.blockConfirmBody) },
            confirmButton = { TextButton(onClick = { onDismiss(); vm.setBlocked(friend.id, true) }) { Text(s.block, color = Coral) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(s.deleteFriendTitle(name)) },
            text = { Text(s.deleteFriendBody) },
            confirmButton = { TextButton(onClick = { onDismiss(); vm.removeFriend(friend.id) }) { Text(s.delete, color = Coral) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
        )
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
        Text(LocalStrings.current.friendName(friend), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("@${friend.handle} · LV ${friend.level}", color = TextMuted, fontFamily = PixelFont, fontSize = 12.sp)
    }
}

@Composable
private fun MessageBubble(m: ChatMessage, friend: Friend, fontSize: Int, showRead: Boolean, onDoubleTap: () -> Unit) {
    val s = LocalStrings.current
    val shape = if (m.fromMe) RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
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
                Text(s.messageText(m), color = Color.White, fontSize = fontSize.sp)
                Text(
                    s.chatTime(m.time), fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f),
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
                    Icon(Icons.Filled.Favorite, s.liked, tint = Coral, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
    if (showRead) {
        Text(s.read, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, end = 4.dp))
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
