package com.today.app.ui.screens

import android.icu.text.BreakIterator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.today.app.data.CharacterState
import com.today.app.data.MAX_QUESTS_PER_DAY
import com.today.app.data.MAX_QUEST_TITLE
import com.today.app.data.MAX_QUEST_XP
import com.today.app.data.MIN_QUEST_XP
import com.today.app.data.Mood
import com.today.app.data.Quest
import com.today.app.data.TodayViewModel
import com.today.app.i18n.LocalStrings
import com.today.app.ui.components.CelebrationOverlay
import com.today.app.ui.components.PixelAvatar
import com.today.app.ui.components.WeekCalendar
import com.today.app.ui.theme.Coral
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.Mint
import com.today.app.ui.theme.NightSurfaceHigh
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.Sky
import com.today.app.ui.theme.TextMuted
import com.today.app.ui.theme.Violet
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Which quest editor is open: a new quest, or an existing one. */
private sealed interface Editor {
    data object New : Editor
    data class Existing(val quest: Quest) : Editor
}

@Composable
fun HomeScreen(vm: TodayViewModel, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    var editor by remember { mutableStateOf<Editor?>(null) }
    var pendingDelete by remember { mutableStateOf<Quest?>(null) }
    var showShield by remember { mutableStateOf(false) }
    var showTitles by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    // Drag-to-reorder: which row is lifted and how far it has moved since its last swap.
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val rowGap = with(density) { 12.dp.toPx() }
    val scrollEdge = with(density) { 72.dp.toPx() }
    val scrollStep = with(density) { 14.dp.toPx() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val date = vm.selectedDate
    val quests = vm.questsFor(date)
    val editable = vm.isEditable(date)
    val todayQuests = vm.todayActive
    val clipboard = vm.clipboard

    // "순서 변경" mode: drag handles instead of menus. Leaves when the day changes or can't be reordered.
    var reorderMode by rememberSaveable { mutableStateOf(false) }
    val canReorder = editable && quests.size > 1
    LaunchedEffect(date, canReorder) { if (!canReorder) reorderMode = false }

    /** Moves the lifted row by [dy]; swaps past half a neighbor's height and scrolls near the edges. */
    fun dragBy(questId: String, dy: Float) {
        dragOffset += dy
        val list = vm.questsFor(date)
        val index = list.indexOfFirst { it.id == questId }
        if (index < 0) return
        val visible = listState.layoutInfo.visibleItemsInfo
        fun heightOf(id: String) = visible.firstOrNull { it.key == id }?.size
        val own = heightOf(questId) ?: return
        if (dragOffset > 0 && index < list.lastIndex) {
            val next = heightOf(list[index + 1].id) ?: own
            if (dragOffset > next / 2f) {
                vm.moveQuest(date, index, index + 1)
                dragOffset -= next + rowGap
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        } else if (dragOffset < 0 && index > 0) {
            val prev = heightOf(list[index - 1].id) ?: own
            if (-dragOffset > prev / 2f) {
                vm.moveQuest(date, index, index - 1)
                dragOffset += prev + rowGap
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        }
        // Near the top/bottom of the screen: scroll, and keep the row under the finger.
        val me = visible.firstOrNull { it.key == questId } ?: return
        val top = me.offset + dragOffset
        val layout = listState.layoutInfo
        val delta = when {
            top + me.size > layout.viewportEndOffset - scrollEdge -> scrollStep
            top < layout.viewportStartOffset + scrollEdge -> -scrollStep
            else -> 0f
        }
        if (delta != 0f) {
            scope.launch {
                val used = listState.scrollBy(delta)
                if (draggingId == questId) dragOffset += used
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") { Header(vm.todayDate, s.moodGreeting(vm.settings.nickname, vm.mood)) }
            item(key = "character") {
                CharacterCard(
                    vm.character, vm.streak, todayQuests.count { it.done }, todayQuests.size,
                    shields = vm.rewards.shields,
                    titleId = vm.rewards.equippedTitle,
                    mood = vm.mood,
                    onShield = { showShield = true },
                    onTitle = { showTitles = true },
                )
            }
            vm.rescuableStreak?.let { saved ->
                item(key = "rescue") {
                    RewardBanner("🛡", s.rescueBanner(saved), s.rescueButton, Sky) { vm.useShield(vm.todayDate.minusDays(1)) }
                }
            }
            if (vm.rewards.boxes > 0) {
                item(key = "box") {
                    RewardBanner("🎁", s.boxBanner(vm.rewards.boxes), s.openBox, Gold, vm::openBox)
                }
            }
            item(key = "calendar") {
                WeekCalendar(
                    selected = date,
                    today = vm.todayDate,
                    statusOf = vm::dayStatus,
                    onSelect = vm::selectDate,
                    weekStartsMonday = vm.settings.weekStartsMonday,
                )
            }
            if (clipboard != null) {
                item(key = "clipboard") {
                    ClipboardBanner(
                        text = s.clipboardBanner(clipboard.sourceDate, clipboard.quests.size),
                        canPaste = editable,
                        onPaste = { vm.pasteTo(date) },
                        onCancel = vm::clearClipboard,
                    )
                }
            }
            item(key = "dayHeader") {
                DayHeader(
                    date = date,
                    today = vm.todayDate,
                    count = quests.size,
                    editable = editable,
                    onAdd = {
                        if (quests.size >= MAX_QUESTS_PER_DAY) vm.showMessage(s.errLimit(MAX_QUESTS_PER_DAY))
                        else editor = Editor.New
                    },
                    onCopy = { vm.copyDay(date) },
                    canReorder = canReorder,
                    reorderMode = reorderMode,
                    onReorder = { reorderMode = !reorderMode },
                )
            }
            if (quests.isEmpty()) {
                item(key = "empty") { EmptyDay(editable) }
            }
            items(quests, key = { it.id }) { quest ->
                val isDragging = draggingId == quest.id
                QuestRow(
                    quest = quest,
                    date = date,
                    editable = editable,
                    checkable = vm.isCheckable(date),
                    dragging = isDragging,
                    reorderMode = reorderMode,
                    onToggle = { vm.toggleQuest(date, quest.id) },
                    onEdit = { editor = Editor.Existing(quest) },
                    // Menu and swipe both ask first (the dialog below), then Undo is still offered.
                    onDelete = { pendingDelete = quest },
                    onDragStart = {
                        draggingId = quest.id
                        dragOffset = 0f
                    },
                    onDrag = { dy -> dragBy(quest.id, dy) },
                    onDragEnd = {
                        draggingId = null
                        dragOffset = 0f
                        vm.commitOrder()
                    },
                    modifier = if (isDragging) Modifier.graphicsLayer { translationY = dragOffset } else Modifier.animateItem(),
                )
            }
            item(key = "footer") {
                Text(
                    (if (editable && quests.isNotEmpty() && !reorderMode) s.gestureHint + "\n" else "") + s.footer(MAX_QUESTS_PER_DAY),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                )
            }
        }

        if (vm.settings.celebrationEffects) CelebrationOverlay(trigger = vm.celebrationTick)
    }

    editor?.let { e ->
        val existing = (e as? Editor.Existing)?.quest
        QuestEditorDialog(
            initial = existing,
            defaultXp = vm.settings.defaultQuestXp,
            weekStartsMonday = vm.settings.weekStartsMonday,
            onDismiss = { editor = null },
            onSave = { title, emoji, xp, days ->
                val error = if (existing == null) vm.addQuest(date, title, emoji, xp, days)
                else vm.updateQuest(date, existing.id, title, emoji, xp, days)
                if (error == null) editor = null else vm.showMessage(error)
            },
        )
    }

    pendingDelete?.let { q ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(s.deleteQuestTitle) },
            text = { Text(s.deleteQuestBody(q.title, q.done, q.xp)) },
            confirmButton = { TextButton(onClick = { vm.deleteQuest(date, q.id); pendingDelete = null }) { Text(s.delete, color = Coral) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(s.cancel) } },
        )
    }

    if (showShield) ShieldDialog(vm, onDismiss = { showShield = false })
    if (showTitles) TitlePickerDialog(vm, onDismiss = { showTitles = false })
    vm.levelUpEvent?.let { LevelUpDialog(vm, it) }
    // After the level-up dialog, so a box opened from it isn't hidden behind.
    vm.boxResult?.let { BoxDialog(vm, it) }
}

@Composable
private fun Header(today: LocalDate, greeting: String) {
    val s = LocalStrings.current
    Column {
        Text("Today", style = MaterialTheme.typography.headlineMedium)
        Text("${s.headerDate(today)} · $greeting", color = TextMuted)
    }
}

@Composable
private fun CharacterCard(
    c: CharacterState,
    streak: Int,
    done: Int,
    total: Int,
    shields: Int,
    titleId: String,
    mood: Mood,
    onShield: () -> Unit,
    onTitle: () -> Unit,
) {
    val s = LocalStrings.current
    val xpProgress by animateFloatAsState(c.progress, tween(600), label = "xp")
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF3A2F6B), Color(0xFF1F1B2E)))),
                contentAlignment = Alignment.Center,
            ) {
                PixelAvatar(c.outfit, Modifier.height(92.dp), mood = mood)
                // Mood badge in the corner; the face itself changes too.
                Text(mood.emoji, fontSize = 16.sp, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("LV ${c.level}", fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Gold)
                    Spacer(Modifier.weight(1f))
                    Text("🔥 ${s.days(streak)}", fontFamily = PixelFont, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                PixelProgressBar(xpProgress)
                Text(
                    "${c.xp} / ${c.xpToNext} XP",
                    style = MaterialTheme.typography.labelSmall, color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(s.todayProgress(done, total), style = MaterialTheme.typography.bodyMedium)
                Text(
                    s.outfitName(c.outfit.name),
                    style = MaterialTheme.typography.labelSmall, color = Violet, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    RewardPill("🛡 $shields", Sky, onShield)
                    RewardPill(s.titleLabel(titleId), Gold, onTitle, Modifier.weight(1f, fill = false))
                }
            }
        }
    }
}

/** Segmented, game-style XP bar. */
@Composable
fun PixelProgressBar(progress: Float, segments: Int = 20) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(14.dp)
            .border(2.dp, Color(0xFF0E0C16), RoundedCornerShape(3.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val filled = progress.coerceIn(0f, 1f) * segments
        repeat(segments) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(if (i < filled) Mint else NightSurfaceHigh),
            )
        }
    }
}

@Composable
private fun DayHeader(
    date: LocalDate,
    today: LocalDate,
    count: Int,
    editable: Boolean,
    onAdd: () -> Unit,
    onCopy: () -> Unit,
    canReorder: Boolean,
    reorderMode: Boolean,
    onReorder: () -> Unit,
) {
    val s = LocalStrings.current
    val tag = when {
        date == today -> s.tagToday
        date.isBefore(today) -> s.tagPast
        else -> s.tagPlanned
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.shortDate(date), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(8.dp))
            Text(
                tag, fontSize = 12.sp, color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (date == today) Violet else NightSurfaceHigh)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Spacer(Modifier.weight(1f))
            Text("$count / $MAX_QUESTS_PER_DAY", fontFamily = PixelFont, color = if (count >= MAX_QUESTS_PER_DAY) Coral else TextMuted)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = onCopy,
                enabled = count > 0,
                label = { Text(s.copyDay) },
                leadingIcon = { Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp)) },
            )
            if (editable) {
                AssistChip(
                    onClick = onAdd,
                    label = { Text(s.addQuest) },
                    leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Violet.copy(alpha = 0.2f)),
                )
                // Right of "Add": toggles reorder mode; reads "Done" while active.
                AssistChip(
                    onClick = onReorder,
                    enabled = canReorder,
                    label = { Text(if (reorderMode) s.reorderDone else s.reorder) },
                    leadingIcon = {
                        Icon(if (reorderMode) Icons.Filled.Check else Icons.Filled.SwapVert, null, Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (reorderMode) Violet else Color.Transparent,
                        labelColor = Color.White,
                        leadingIconContentColor = Color.White,
                    ),
                )
            }
        }
        when {
            reorderMode -> Hint(s.reorderHint)
            date.isBefore(today) -> Hint(s.hintPast)
            date.isAfter(today) -> Hint(s.hintFuture)
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun ClipboardBanner(text: String, canPaste: Boolean, onPaste: () -> Unit, onCancel: () -> Unit) {
    val s = LocalStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Gold.copy(alpha = 0.15f))
            .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("📋", fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (canPaste) {
            TextButton(onClick = onPaste) {
                Icon(Icons.Filled.ContentPaste, null, Modifier.size(16.dp), tint = Gold)
                Spacer(Modifier.width(4.dp))
                Text(s.pasteHere, color = Gold)
            }
        }
        IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = s.cancelCopy, tint = TextMuted) }
    }
}

@Composable
private fun EmptyDay(editable: Boolean) {
    val s = LocalStrings.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🗓️", fontSize = 32.sp)
        Spacer(Modifier.height(8.dp))
        Text(if (editable) s.emptyEditable else s.emptyPast, color = TextMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun QuestRow(
    quest: Quest,
    date: LocalDate,
    editable: Boolean,
    checkable: Boolean,
    dragging: Boolean,
    reorderMode: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val haptic = LocalHapticFeedback.current
    val active = quest.activeOn(date)
    var menuOpen by remember { mutableStateOf(false) }
    var width by remember { mutableIntStateOf(1) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    // Reorder mode: only the handle moves rows, so taps and swipes are off.
    val canComplete = checkable && active && !reorderMode // swipe right
    val canDelete = editable && !reorderMode // swipe left
    // pointerInput outlives recompositions: always call the latest callbacks.
    val latestDragStart by rememberUpdatedState(onDragStart)
    val latestDrag by rememberUpdatedState(onDrag)
    val latestDragEnd by rememberUpdatedState(onDragEnd)

    val bg by animateColorAsState(if (quest.done) Mint.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface, label = "bg")
    val checkScale by animateFloatAsState(
        if (quest.done) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "check",
    )
    val toggle = {
        if (canComplete && !quest.done) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        onToggle()
    }

    Box(
        modifier
            .fillMaxWidth()
            .onSizeChanged { width = it.width }
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                if (dragging) {
                    scaleX = 1.03f
                    scaleY = 1.03f
                }
            },
    ) {
        // Revealed behind the row while swiping: check on the left, trash on the right.
        if (offsetX != 0f) {
            val right = offsetX > 0
            val tint = if (right) Mint else Coral
            Row(
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tint.copy(alpha = 0.25f))
                    .padding(horizontal = 22.dp),
                horizontalArrangement = if (right) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (right) Icons.Filled.Check else Icons.Filled.Delete, contentDescription = null, tint = tint)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offsetX }
                .clip(RoundedCornerShape(16.dp))
                .background(if (dragging) NightSurfaceHigh else bg)
                .then(if (reorderMode) Modifier.border(1.dp, Violet.copy(alpha = 0.5f), RoundedCornerShape(16.dp)) else Modifier)
                .then(
                    if (editable && !reorderMode) {
                        Modifier.pointerInput(quest.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    latestDragStart()
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    latestDrag(amount.y)
                                },
                                onDragEnd = { latestDragEnd() },
                                onDragCancel = { latestDragEnd() },
                            )
                        }
                    } else {
                        Modifier
                    },
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = canComplete || canDelete,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(
                            if (canDelete) -width.toFloat() else 0f,
                            if (canComplete) width.toFloat() else 0f,
                        )
                    },
                    onDragStopped = {
                        val threshold = width * 0.35f
                        when {
                            offsetX > threshold -> {
                                toggle()
                                animate(offsetX, 0f) { v, _ -> offsetX = v }
                            }
                            offsetX < -threshold -> {
                                // Don't delete yet: spring back and ask in the confirm dialog.
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDelete()
                                animate(offsetX, 0f) { v, _ -> offsetX = v }
                            }
                            else -> animate(offsetX, 0f) { v, _ -> offsetX = v }
                        }
                    },
                )
                .clickable(enabled = !reorderMode, onClick = toggle)
                .alpha(if (active) 1f else 0.5f)
                .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(2.dp, if (quest.done) Mint else if (canComplete) TextMuted else TextMuted.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Mint, modifier = Modifier.size(20.dp).scale(checkScale))
            }
            Spacer(Modifier.width(14.dp))
            Text(quest.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (quest.done) TextDecoration.LineThrough else null,
                    color = if (quest.done) TextMuted else Color.White,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (quest.days.isNotEmpty()) {
                    Text(
                        "🔁 ${s.repeatLabel(quest.days)}" + if (active) "" else " · ${s.restDay}",
                        fontSize = 12.sp, color = if (active) Violet else TextMuted,
                    )
                }
            }
            Text("+${quest.xp} XP", fontFamily = PixelFont, fontWeight = FontWeight.Bold, color = if (quest.done) Mint else Gold)
            if (reorderMode) {
                // Drag handle: moves the row immediately (no long press needed).
                Icon(
                    Icons.Filled.DragHandle,
                    contentDescription = s.reorder,
                    tint = Violet,
                    modifier = Modifier
                        .size(48.dp)
                        .pointerInput(quest.id) {
                            detectDragGestures(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    latestDragStart()
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    latestDrag(amount.y)
                                },
                                onDragEnd = { latestDragEnd() },
                                onDragCancel = { latestDragEnd() },
                            )
                        }
                        .padding(12.dp),
                )
            } else if (editable) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = s.questMenu, tint = TextMuted)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(s.edit) },
                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                            onClick = { menuOpen = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text(s.delete, color = Coral) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Coral) },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

/** First user-perceived character (keeps multi-codepoint emoji like 🏃‍♀️ intact). */
private fun firstGrapheme(s: String): String {
    if (s.isEmpty()) return s
    val it = BreakIterator.getCharacterInstance()
    it.setText(s)
    val end = it.next()
    return if (end == BreakIterator.DONE) s else s.substring(0, end)
}

/** Last grapheme typed — so typing into a filled emoji box replaces it. */
private fun lastGrapheme(s: String): String {
    if (s.isEmpty()) return s
    val it = BreakIterator.getCharacterInstance()
    it.setText(s)
    val end = it.last()
    val start = it.previous()
    return if (start == BreakIterator.DONE) s else s.substring(start, end)
}

@Composable
private fun QuestEditorDialog(
    initial: Quest?,
    defaultXp: Int,
    weekStartsMonday: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Set<DayOfWeek>) -> Unit,
) {
    val s = LocalStrings.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var days by remember { mutableStateOf(initial?.days ?: emptySet()) }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "⭐") }
    var xpText by remember { mutableStateOf((initial?.xp ?: defaultXp).coerceIn(MIN_QUEST_XP, MAX_QUEST_XP).toString()) }
    val xp = xpText.toIntOrNull()
    val xpValid = xp != null && xp in MIN_QUEST_XP..MAX_QUEST_XP
    val titleValid = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) s.newQuest else s.editQuest) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(MAX_QUEST_TITLE) },
                    label = { Text(s.fieldTask) },
                    singleLine = true,
                    supportingText = { Text("${title.length} / $MAX_QUEST_TITLE") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { v -> emoji = if (v.length > emoji.length) lastGrapheme(v) else firstGrapheme(v) },
                        label = { Text(s.fieldEmoji) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = xpText,
                        onValueChange = { xpText = it.filter(Char::isDigit).take(2) },
                        label = { Text(s.fieldXp) },
                        singleLine = true,
                        isError = !xpValid,
                        supportingText = { Text("$MIN_QUEST_XP~$MAX_QUEST_XP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("🏃", "📘", "💼", "💧", "🧘", "🛏️", "🥗", "🎧").forEach { e ->
                        Text(
                            e, fontSize = 22.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (emoji == e) Violet.copy(alpha = 0.3f) else Color.Transparent)
                                .clickable { emoji = e }
                                .padding(4.dp),
                        )
                    }
                }
                Text(s.fieldRepeat, fontSize = 12.sp, color = TextMuted)
                WeekdayPicker(days, weekStartsMonday) { days = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, emoji, xp ?: defaultXp, days) }, enabled = titleValid && xpValid) {
                Text(if (initial == null) s.add else s.save)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

/** Seven round toggles; none selected = every day. */
@Composable
private fun WeekdayPicker(selected: Set<DayOfWeek>, mondayFirst: Boolean, onChange: (Set<DayOfWeek>) -> Unit) {
    val s = LocalStrings.current
    val order = if (mondayFirst) DayOfWeek.entries else listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.dropLast(1)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        order.forEach { day ->
            val on = day in selected
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (on) Violet else NightSurfaceHigh)
                    .clickable { onChange(if (on) selected - day else selected + day) },
                contentAlignment = Alignment.Center,
            ) {
                Text(s.weekdayShort(day).take(3), fontSize = 12.sp, color = if (on) Color.White else TextMuted, fontWeight = if (on) FontWeight.Bold else null)
            }
        }
    }
}
