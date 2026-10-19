package com.today.app.ui.screens

import android.icu.text.BreakIterator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.today.app.data.CharacterState
import com.today.app.data.MAX_QUESTS_PER_DAY
import com.today.app.data.MAX_QUEST_TITLE
import com.today.app.data.MAX_QUEST_XP
import com.today.app.data.MIN_QUEST_XP
import com.today.app.data.Quest
import com.today.app.data.TodayViewModel
import com.today.app.ui.components.CelebrationOverlay
import com.today.app.ui.components.PixelAvatar
import com.today.app.ui.components.WeekCalendar
import com.today.app.ui.theme.Coral
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.Mint
import com.today.app.ui.theme.NightSurfaceHigh
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.TextMuted
import com.today.app.ui.theme.Violet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Which quest editor is open: a new quest, or an existing one. */
private sealed interface Editor {
    data object New : Editor
    data class Existing(val quest: Quest) : Editor
}

@Composable
fun HomeScreen(vm: TodayViewModel, modifier: Modifier = Modifier) {
    var editor by remember { mutableStateOf<Editor?>(null) }
    var pendingDelete by remember { mutableStateOf<Quest?>(null) }

    val date = vm.selectedDate
    val quests = vm.questsFor(date)
    val editable = vm.isEditable(date)
    val todayQuests = vm.questsFor(vm.todayDate)
    val clipboard = vm.clipboard

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") { Header(vm.settings.nickname) }
            item(key = "character") {
                CharacterCard(vm.character, vm.streak, todayQuests.count { it.done }, todayQuests.size)
            }
            item(key = "calendar") {
                WeekCalendar(
                    selected = date,
                    today = vm.todayDate,
                    statusOf = vm::dayStatus,
                    onSelect = vm::selectDate,
                )
            }
            if (clipboard != null) {
                item(key = "clipboard") {
                    ClipboardBanner(
                        text = "${TodayViewModel.formatDate(clipboard.sourceDate)} 퀘스트 ${clipboard.quests.size}개 복사됨",
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
                        if (quests.size >= MAX_QUESTS_PER_DAY) vm.showMessage("하루에 최대 ${MAX_QUESTS_PER_DAY}개까지 추가할 수 있어요")
                        else editor = Editor.New
                    },
                    onCopy = { vm.copyDay(date) },
                )
            }
            if (quests.isEmpty()) {
                item(key = "empty") { EmptyDay(editable) }
            }
            items(quests, key = { it.id }) { quest ->
                QuestRow(
                    quest = quest,
                    editable = editable,
                    checkable = vm.isCheckable(date),
                    onToggle = { vm.toggleQuest(date, quest.id) },
                    onEdit = { editor = Editor.Existing(quest) },
                    onDelete = { pendingDelete = quest },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "footer") {
                Text(
                    "⋮ 메뉴로 수정·삭제 · 하루 최대 ${MAX_QUESTS_PER_DAY}개 · 모든 퀘스트 완료 시 스트릭 +1",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                )
            }
        }

        CelebrationOverlay(trigger = vm.celebrationTick)
    }

    editor?.let { e ->
        val existing = (e as? Editor.Existing)?.quest
        QuestEditorDialog(
            initial = existing,
            onDismiss = { editor = null },
            onSave = { title, emoji, xp ->
                val error = if (existing == null) vm.addQuest(date, title, emoji, xp)
                else vm.updateQuest(date, existing.id, title, emoji, xp)
                if (error == null) editor = null else vm.showMessage(error)
            },
        )
    }

    pendingDelete?.let { q ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("퀘스트 삭제") },
            text = {
                Text(
                    "'${q.title}' 퀘스트를 삭제할까요?" +
                        if (q.done) "\n이미 완료한 퀘스트라 받은 ${q.xp} XP도 회수돼요." else ""
                )
            },
            confirmButton = { TextButton(onClick = { vm.deleteQuest(date, q.id); pendingDelete = null }) { Text("삭제", color = Coral) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("취소") } },
        )
    }

    vm.levelUpEvent?.let { level ->
        AlertDialog(
            onDismissRequest = vm::consumeLevelUp,
            title = { Text("⭐ LEVEL UP! ⭐", fontFamily = PixelFont, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    PixelAvatar(vm.character.outfit, Modifier.height(120.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("LV $level 달성!", fontFamily = PixelFont, fontSize = 22.sp, color = Gold)
                    Text("오늘의 내가 어제의 나보다 강해졌어요.", color = TextMuted)
                }
            },
            confirmButton = { TextButton(onClick = vm::consumeLevelUp) { Text("좋아!") } },
        )
    }
}

@Composable
private fun Header(nickname: String) {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))
    Column {
        Text("Today", style = MaterialTheme.typography.headlineMedium)
        Text("$date · ${nickname}님, 오늘도 레벨업!", color = TextMuted)
    }
}

@Composable
private fun CharacterCard(c: CharacterState, streak: Int, done: Int, total: Int) {
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
                PixelAvatar(c.outfit, Modifier.height(92.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("LV ${c.level}", fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Gold)
                    Spacer(Modifier.weight(1f))
                    Text("🔥 ${streak}일", fontFamily = PixelFont, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                PixelProgressBar(xpProgress)
                Text(
                    "${c.xp} / ${c.xpToNext} XP",
                    style = MaterialTheme.typography.labelSmall, color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("오늘 진행: $done / $total", style = MaterialTheme.typography.bodyMedium)
                Text(c.outfit.name, style = MaterialTheme.typography.labelSmall, color = Violet, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
private fun DayHeader(date: LocalDate, today: LocalDate, count: Int, editable: Boolean, onAdd: () -> Unit, onCopy: () -> Unit) {
    val tag = when {
        date == today -> "오늘"
        date.isBefore(today) -> "지난 기록"
        else -> "예정"
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(TodayViewModel.formatDate(date), style = MaterialTheme.typography.titleLarge)
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = onCopy,
                enabled = count > 0,
                label = { Text("이 날 복사") },
                leadingIcon = { Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp)) },
            )
            if (editable) {
                AssistChip(
                    onClick = onAdd,
                    label = { Text("퀘스트 추가") },
                    leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Violet.copy(alpha = 0.2f)),
                )
            }
        }
        when {
            date.isBefore(today) -> Hint("지난 기록은 볼 수만 있어요. 복사해서 오늘이나 다른 날에 붙여넣을 수 있어요.")
            date.isAfter(today) -> Hint("미리 계획한 퀘스트예요. 완료 체크는 당일에 할 수 있어요.")
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun ClipboardBanner(text: String, canPaste: Boolean, onPaste: () -> Unit, onCancel: () -> Unit) {
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
                Text("여기에 붙여넣기", color = Gold)
            }
        }
        IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = "복사 취소", tint = TextMuted) }
    }
}

@Composable
private fun EmptyDay(editable: Boolean) {
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
        Text(
            if (editable) "아직 퀘스트가 없어요.\n추가하거나 다른 날을 복사해서 붙여넣어 보세요." else "이 날은 기록이 없어요.",
            color = TextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun QuestRow(
    quest: Quest,
    editable: Boolean,
    checkable: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val bg by animateColorAsState(if (quest.done) Mint.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface, label = "bg")
    val checkScale by animateFloatAsState(
        if (quest.done) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "check",
    )
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onToggle)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(2.dp, if (quest.done) Mint else if (checkable) TextMuted else TextMuted.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Mint, modifier = Modifier.size(20.dp).scale(checkScale))
        }
        Spacer(Modifier.width(14.dp))
        Text(quest.emoji, fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            quest.title,
            style = MaterialTheme.typography.titleMedium,
            textDecoration = if (quest.done) TextDecoration.LineThrough else null,
            color = if (quest.done) TextMuted else Color.White,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
        )
        Text("+${quest.xp} XP", fontFamily = PixelFont, fontWeight = FontWeight.Bold, color = if (quest.done) Mint else Gold)
        if (editable) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "퀘스트 메뉴", tint = TextMuted)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("수정") },
                        leadingIcon = { Icon(Icons.Filled.Edit, null) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text("삭제", color = Coral) },
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
private fun QuestEditorDialog(initial: Quest?, onDismiss: () -> Unit, onSave: (String, String, Int) -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "⭐") }
    var xpText by remember { mutableStateOf((initial?.xp ?: 20).toString()) }
    val xp = xpText.toIntOrNull()
    val xpValid = xp != null && xp in MIN_QUEST_XP..MAX_QUEST_XP
    val titleValid = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "새 퀘스트" else "퀘스트 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(MAX_QUEST_TITLE) },
                    label = { Text("할 일") },
                    singleLine = true,
                    supportingText = { Text("${title.length} / $MAX_QUEST_TITLE") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { v -> emoji = if (v.length > emoji.length) lastGrapheme(v) else firstGrapheme(v) },
                        label = { Text("이모지") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = xpText,
                        onValueChange = { xpText = it.filter(Char::isDigit).take(3) },
                        label = { Text("XP") },
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
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, emoji, xp ?: 20) }, enabled = titleValid && xpValid) {
                Text(if (initial == null) "추가" else "저장")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
