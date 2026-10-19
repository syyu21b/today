package com.today.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.today.app.data.CharacterState
import com.today.app.data.Quest
import com.today.app.data.TodayViewModel
import com.today.app.ui.components.CelebrationOverlay
import com.today.app.ui.components.PixelAvatar
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.Mint
import com.today.app.ui.theme.NightSurfaceHigh
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.TextMuted
import com.today.app.ui.theme.Violet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(vm: TodayViewModel, modifier: Modifier = Modifier) {
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Quest?>(null) }
    val doneCount = vm.quests.count { it.done }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header() }
            item { CharacterCard(vm.character, doneCount, vm.quests.size) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("오늘의 퀘스트", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    FilledTonalButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("추가")
                    }
                }
            }
            items(vm.quests, key = { it.id }) { quest ->
                QuestRow(
                    quest = quest,
                    onToggle = { vm.toggleQuest(quest.id) },
                    onLongPress = { pendingDelete = quest },
                    modifier = Modifier.animateItem(),
                )
            }
            item {
                Text(
                    "길게 누르면 퀘스트를 삭제할 수 있어요 · 모든 퀘스트 완료 시 스트릭 +1",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                )
            }
        }

        CelebrationOverlay(trigger = vm.celebrationTick)
    }

    if (showAdd) {
        AddQuestDialog(
            onDismiss = { showAdd = false },
            onAdd = { title, emoji, xp -> vm.addQuest(title, emoji, xp); showAdd = false },
        )
    }

    pendingDelete?.let { q ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("퀘스트 삭제") },
            text = { Text("'${q.title}' 퀘스트를 삭제할까요?") },
            confirmButton = { TextButton(onClick = { vm.removeQuest(q.id); pendingDelete = null }) { Text("삭제") } },
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
private fun Header() {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))
    Column {
        Text("Today", style = MaterialTheme.typography.headlineMedium)
        Text(date, color = TextMuted)
    }
}

@Composable
private fun CharacterCard(c: CharacterState, done: Int, total: Int) {
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
                    Text(
                        "LV ${c.level}",
                        fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Gold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text("🔥 ${c.streak}일", fontFamily = PixelFont, fontWeight = FontWeight.Bold)
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
                Text(c.outfit.name, style = MaterialTheme.typography.labelSmall, color = Violet)
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
        val filled = (progress.coerceIn(0f, 1f) * segments)
        repeat(segments) { i ->
            val on = i < filled
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(if (on) (if (i % 2 == 0) Mint else Mint.copy(alpha = 0.85f)) else NightSurfaceHigh),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuestRow(quest: Quest, onToggle: () -> Unit, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(
        if (quest.done) Mint.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface, label = "bg",
    )
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
            .combinedClickable(onClick = onToggle, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(2.dp, if (quest.done) Mint else TextMuted, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Check, contentDescription = null, tint = Mint,
                modifier = Modifier.size(20.dp).scale(checkScale),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(quest.emoji, fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            quest.title,
            style = MaterialTheme.typography.titleMedium,
            textDecoration = if (quest.done) TextDecoration.LineThrough else null,
            color = if (quest.done) TextMuted else Color.White,
            modifier = Modifier.weight(1f),
        )
        Text(
            "+${quest.xp} XP",
            fontFamily = PixelFont, fontWeight = FontWeight.Bold,
            color = if (quest.done) Mint else Gold,
        )
    }
}

@Composable
private fun AddQuestDialog(onDismiss: () -> Unit, onAdd: (String, String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("⭐") }
    var xp by remember { mutableStateOf("20") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 퀘스트") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("할 일") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(emoji, { emoji = it.take(2) }, label = { Text("이모지") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        xp, { xp = it.filter(Char::isDigit).take(3) }, label = { Text("XP (5~100)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("🏃", "📘", "💼", "💧", "🧘", "🛏️", "🥗").forEach { e ->
                        Text(
                            e, fontSize = 22.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { emoji = e }
                                .padding(4.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(title, emoji, xp.toIntOrNull() ?: 20) }, enabled = title.isNotBlank()) { Text("추가") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
