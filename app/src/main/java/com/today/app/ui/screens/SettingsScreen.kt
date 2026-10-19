package com.today.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.today.app.data.AppLanguage
import com.today.app.data.TodayViewModel
import com.today.app.i18n.LocalStrings
import com.today.app.ui.components.PixelAvatar
import com.today.app.ui.theme.Coral
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.TextMuted
import com.today.app.ui.theme.Violet

@Composable
fun SettingsScreen(vm: TodayViewModel, onOpenOutfit: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    var editingName by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val c = vm.character
    val xpProgress by animateFloatAsState(c.progress, tween(600), label = "xp")

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(s.settingsTitle, style = MaterialTheme.typography.headlineMedium)

        // ---- Profile (same outfit state as the quest card -> updates instantly) ----
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF3A2F6B), MaterialTheme.colorScheme.surface)))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PixelAvatar(c.outfit, Modifier.height(150.dp))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vm.settings.nickname, style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { editingName = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = s.editNickname, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
            Text(s.outfitName(c.outfit.name), color = Violet, fontFamily = PixelFont, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("LV ${c.level}", fontFamily = PixelFont, fontWeight = FontWeight.Bold, color = Gold, fontSize = 18.sp)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) { PixelProgressBar(xpProgress) }
                Spacer(Modifier.width(8.dp))
                Text("${c.xp}/${c.xpToNext}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat("🔥", s.days(vm.streak), s.statStreak)
                Stat("✅", "${vm.totalCompleted}", s.statCompleted)
                Stat("🗓️", s.days(vm.recordedDays), s.statDays)
            }
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(onClick = onOpenOutfit) {
                Icon(Icons.Filled.Checkroom, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(s.customizeOutfit)
            }
        }

        // ---- Language (whole app) ----
        Section(s.sectionLanguage) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Language, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(s.languageHint, color = TextMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                AppLanguage.entries.forEachIndexed { i, lang ->
                    SegmentedButton(
                        selected = vm.settings.language == lang,
                        onClick = { vm.updateLanguage(lang) },
                        shape = SegmentedButtonDefaults.itemShape(i, AppLanguage.entries.size),
                        // Always shown in its own language so it's findable from either side.
                        label = { Text(lang.label) },
                    )
                }
            }
        }

        // ---- Quests ----
        Section(s.sectionQuests) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.autoCarryTitle)
                    Text(s.autoCarryDesc, color = TextMuted, fontSize = 12.sp)
                }
                Switch(checked = vm.settings.autoCarryOver, onCheckedChange = vm::updateAutoCarryOver)
            }
        }

        // ---- Data ----
        Section(s.sectionData) {
            Text(s.dataDesc, color = TextMuted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                Text(s.resetButton, color = Coral)
            }
        }

        Section(s.sectionAbout) {
            Row {
                Text("Today", modifier = Modifier.weight(1f))
                Text("v1.2", color = TextMuted, fontFamily = PixelFont)
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.06f))
            Text(s.aboutDesc, color = TextMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
    }

    if (editingName) {
        var name by remember { mutableStateOf(vm.settings.nickname) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text(s.editNickname) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(TodayViewModel.MAX_NICKNAME) },
                    singleLine = true,
                    label = { Text(s.nicknameLabel(TodayViewModel.MAX_NICKNAME)) },
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.updateNickname(name); editingName = false }, enabled = name.isNotBlank()) { Text(s.save) }
            },
            dismissButton = { TextButton(onClick = { editingName = false }) { Text(s.cancel) } },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(s.resetTitle) },
            text = { Text(s.resetBody) },
            confirmButton = { TextButton(onClick = { vm.resetAll(); confirmReset = false }) { Text(s.resetConfirm, color = Coral) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(s.cancel) } },
        )
    }
}

@Composable
private fun Stat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(value, fontFamily = PixelFont, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Violet)
        Spacer(Modifier.height(10.dp))
        content()
    }
}
