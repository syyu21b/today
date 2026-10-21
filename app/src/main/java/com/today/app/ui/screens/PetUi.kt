package com.today.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.today.app.data.MAX_PET_NAME
import com.today.app.data.PetSpecies
import com.today.app.data.PetStage
import com.today.app.data.TodayViewModel
import com.today.app.i18n.LocalStrings
import com.today.app.ui.components.Backdrop
import com.today.app.ui.components.PixelPet
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.Hairline
import com.today.app.ui.theme.Mint
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.TextMuted
import com.today.app.ui.theme.Violet

/** Wardrobe card: the pet big on the backdrop, its growth, petting, rename / adopt another. */
@Composable
fun PetCard(vm: TodayViewModel) {
    val s = LocalStrings.current
    val haptic = LocalHapticFeedback.current
    val pet = vm.pet
    var adopting by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmChange by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<PetSpecies?>(null) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(s.petCardTitle, style = MaterialTheme.typography.titleMedium)
        if (pet == null) {
            Text(s.petAdoptHint, color = TextMuted, fontSize = 12.sp)
            PetPicker(selected = null, onSelect = { picked = it; adopting = true })
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Backdrop(vm.backdrop, Modifier.width(130.dp).height(110.dp).clip(RoundedCornerShape(16.dp))) {
                PixelPet(
                    pet.species, pet.stage,
                    Modifier.width(96.dp).align(Alignment.BottomCenter).padding(bottom = 6.dp),
                    onPet = {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        vm.petThePet()
                    },
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pet.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.clickable { renaming = true })
                    Spacer(Modifier.width(6.dp))
                    Text("${pet.species.emoji} ${s.petStageName(pet.stage)}", color = Violet, fontSize = 12.sp, fontFamily = PixelFont)
                }
                PixelProgressBar(pet.stageProgress, segments = 12)
                val next = pet.nextStage
                Text(
                    if (next == null) s.petFullyGrown else s.petGrowthLeft(next.minGrowth - pet.growth, next),
                    color = if (next == null) Mint else TextMuted, fontSize = 12.sp,
                )
                Text(if (pet.stage == PetStage.EGG) s.petEggHint else s.petTapHint, color = TextMuted, fontSize = 11.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { renaming = true }, modifier = Modifier.weight(1f)) { Text(s.petRename, maxLines = 1) }
            OutlinedButton(onClick = { confirmChange = true }, modifier = Modifier.weight(1f)) { Text(s.petChange, maxLines = 1, fontSize = 13.sp) }
        }
    }

    if (adopting) {
        PetAdoptDialog(initial = picked, onDismiss = { adopting = false }) { species, name ->
            vm.adoptPet(species, name)
            adopting = false
        }
    }
    if (renaming && pet != null) {
        PetNameDialog(pet.name, title = s.petRename, onDismiss = { renaming = false }) {
            vm.renamePet(it)
            renaming = false
        }
    }
    if (confirmChange && pet != null) {
        AlertDialog(
            onDismissRequest = { confirmChange = false },
            title = { Text(s.petChange) },
            text = { Text(s.petChangeBody(pet.name)) },
            confirmButton = { TextButton(onClick = { confirmChange = false; picked = null; adopting = true }) { Text(s.obNext) } },
            dismissButton = { TextButton(onClick = { confirmChange = false }) { Text(s.cancel) } },
        )
    }
}

/** Seven tiles (most popular first): animated preview + name. */
@Composable
fun PetPicker(selected: PetSpecies?, onSelect: (PetSpecies) -> Unit) {
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PetSpecies.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { species ->
                    val on = species == selected
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (on) Gold.copy(alpha = 0.15f) else Hairline.copy(alpha = 0.04f))
                            .border(2.dp, if (on) Gold else Hairline.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .clickable { onSelect(species) }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PixelPet(species, PetStage.ADULT, Modifier.fillMaxWidth(), animated = on)
                        Text(s.petName(species), fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else null, maxLines = 1)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun PetAdoptDialog(initial: PetSpecies?, onDismiss: () -> Unit, onAdopt: (PetSpecies, String) -> Unit) {
    val s = LocalStrings.current
    var species by remember { mutableStateOf(initial ?: PetSpecies.DOG) }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.petAdoptTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PetPicker(species) { species = it }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_PET_NAME) },
                    singleLine = true,
                    label = { Text(s.petNameLabel) },
                    placeholder = { Text(s.petDefaultName(species)) },
                )
            }
        },
        confirmButton = { TextButton(onClick = { onAdopt(species, name) }) { Text(s.petAdopt) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

@Composable
private fun PetNameDialog(initial: String, title: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val s = LocalStrings.current
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it.take(MAX_PET_NAME) }, singleLine = true, label = { Text(s.petNameLabel) })
        },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(s.save) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
