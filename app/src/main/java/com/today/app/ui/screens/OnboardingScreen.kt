package com.today.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.today.app.data.AppLanguage
import com.today.app.data.HairStyle
import com.today.app.data.MAX_PET_NAME
import com.today.app.data.Outfits
import com.today.app.data.PetSpecies
import com.today.app.data.PetStage
import com.today.app.data.Templates
import com.today.app.data.TodayViewModel
import com.today.app.data.MAX_QUESTS_PER_DAY
import com.today.app.i18n.LocalStrings
import com.today.app.ui.components.Backdrop
import com.today.app.ui.components.PixelAvatar
import com.today.app.ui.components.PixelPet
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.Hairline
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.TextMuted
import com.today.app.ui.theme.Violet

private const val STEPS = 5

/** First-run guide: welcome → character → goals (routine packs) → pet → ready. */
@Composable
fun OnboardingScreen(vm: TodayViewModel, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var nickname by rememberSaveable { mutableStateOf("") }
    var hair by rememberSaveable { mutableStateOf(vm.character.outfit.style) }
    var skin by rememberSaveable { mutableIntStateOf(vm.character.outfit.skin) }
    var packs by rememberSaveable { mutableStateOf(listOf<String>()) }
    var species by rememberSaveable { mutableStateOf<PetSpecies?>(PetSpecies.DOG) }
    var petName by rememberSaveable { mutableStateOf("") }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importData(uri)
    }
    BackHandler(enabled = step > 0) { step-- }

    val outfit = vm.character.outfit.copy(hairStyle = hair, skin = skin)
    val chosen = packs.mapNotNull(Templates::byId)
    val questCount = Templates.questsFrom(chosen, emptyList(), vm.settings.language) { "" }.size

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        // Progress dots + language switch
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            repeat(STEPS) { i ->
                Box(
                    Modifier
                        .padding(end = 6.dp)
                        .size(width = if (i == step) 22.dp else 8.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(if (i <= step) Violet else Hairline.copy(alpha = 0.15f)),
                )
            }
            Spacer(Modifier.weight(1f))
            AppLanguage.entries.forEach { lang ->
                Text(
                    lang.label,
                    fontSize = 12.sp,
                    color = if (vm.settings.language == lang) Violet else TextMuted,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { vm.updateLanguage(lang) }.padding(6.dp),
                )
            }
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState > initialState) slideInHorizontally { it / 3 } + fadeIn() togetherWith fadeOut()
                else fadeIn() togetherWith slideOutHorizontally { it / 3 } + fadeOut()
            },
            modifier = Modifier.weight(1f),
            label = "onboarding",
        ) { current ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (current) {
                    0 -> WelcomeStep(outfit)
                    1 -> ProfileStep(nickname, { nickname = it }, outfit, hair, { hair = it }, skin, { skin = it })
                    2 -> GoalsStep(packs, { id -> packs = if (id in packs) packs - id else packs + id }, questCount)
                    3 -> PetStep(species, { species = it }, petName, { petName = it })
                    else -> ReadyStep(outfit, species, nickname.ifBlank { vm.settings.nickname }, vm.backdrop)
                }
            }
        }

        // Bottom buttons
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (step < STEPS - 1) step++
                else vm.finishOnboarding(nickname, hair, skin, chosen, species, petName)
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            Text(
                when (step) {
                    0 -> s.obStart
                    STEPS - 1 -> s.obFinish
                    else -> s.obNext
                },
                fontSize = 17.sp,
            )
        }
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            when (step) {
                0 -> TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(s.obRestore, color = TextMuted)
                }
                else -> {
                    TextButton(onClick = { step-- }) { Text(s.obBack, color = TextMuted) }
                    Spacer(Modifier.weight(1f))
                    if (step == 3) TextButton(onClick = { species = null; step++ }) { Text(s.obPetSkip, color = TextMuted) }
                    Text(s.obStep(step, STEPS - 1), color = TextMuted, fontFamily = PixelFont, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.WelcomeStep(outfit: com.today.app.data.Outfit) {
    val s = LocalStrings.current
    Backdrop(
        "sky",
        Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(28.dp)),
    ) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 30.dp)) {
            PixelAvatar(outfit, Modifier.height(200.dp))
            PixelPet(PetSpecies.DOG, PetStage.BABY, Modifier.width(90.dp).padding(bottom = 6.dp))
        }
    }
    Text(s.obWelcomeTitle, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text(s.obWelcomeBody, color = TextMuted, textAlign = TextAlign.Center, fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ProfileStep(
    nickname: String,
    onNickname: (String) -> Unit,
    outfit: com.today.app.data.Outfit,
    hair: HairStyle,
    onHair: (HairStyle) -> Unit,
    skin: Int,
    onSkin: (Int) -> Unit,
) {
    val s = LocalStrings.current
    Text(s.obProfileTitle, style = MaterialTheme.typography.titleLarge)
    Text(s.obProfileHint, color = TextMuted, fontSize = 14.sp)
    Backdrop("default", Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp))) {
        PixelAvatar(outfit, Modifier.height(170.dp))
    }
    OutlinedTextField(
        value = nickname,
        onValueChange = { onNickname(it.take(TodayViewModel.MAX_NICKNAME)) },
        singleLine = true,
        label = { Text(s.nicknameLabel(TodayViewModel.MAX_NICKNAME)) },
        placeholder = { Text(s.defaultNickname) },
        modifier = Modifier.fillMaxWidth(),
    )
    Text(s.hairStyleTitle, style = MaterialTheme.typography.titleMedium)
    HairStyleChips(hair, onHair)
    Text(s.slotSkin, style = MaterialTheme.typography.titleMedium)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Outfits.skinTones.forEach { c ->
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .border(if (c == skin) 3.dp else 1.dp, if (c == skin) Gold else Hairline.copy(alpha = 0.2f), CircleShape)
                    .clickable { onSkin(c) },
            )
        }
    }
}

/** Hair styles as chips (shared by the guide and the wardrobe editor). */
@Composable
fun HairStyleChips(selected: HairStyle, onSelect: (HairStyle) -> Unit) {
    val s = LocalStrings.current
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HairStyle.entries.forEach { h ->
            FilterChip(selected = h == selected, onClick = { onSelect(h) }, label = { Text(s.hairStyleName(h)) })
        }
    }
}

@Composable
private fun GoalsStep(packs: List<String>, onToggle: (String) -> Unit, questCount: Int) {
    val s = LocalStrings.current
    Text(s.obGoalsTitle, style = MaterialTheme.typography.titleLarge)
    Text(s.obGoalsHint, color = TextMuted, fontSize = 14.sp)
    Text(
        s.obGoalsCount(questCount) + if (questCount >= MAX_QUESTS_PER_DAY) " (MAX)" else "",
        color = Violet, fontSize = 13.sp, fontFamily = PixelFont,
    )
    Templates.all.forEach { t -> TemplateRow(t, selected = t.id in packs) { onToggle(t.id) } }
}

@Composable
private fun PetStep(species: PetSpecies?, onSpecies: (PetSpecies) -> Unit, name: String, onName: (String) -> Unit) {
    val s = LocalStrings.current
    Text(s.obPetTitle, style = MaterialTheme.typography.titleLarge)
    Text(s.obPetHint, color = TextMuted, fontSize = 14.sp)
    PetPicker(species, onSpecies)
    if (species != null) {
        OutlinedTextField(
            value = name,
            onValueChange = { onName(it.take(MAX_PET_NAME)) },
            singleLine = true,
            label = { Text(s.petNameLabel) },
            placeholder = { Text(s.petDefaultName(species)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ReadyStep(outfit: com.today.app.data.Outfit, species: PetSpecies?, nickname: String, backdrop: String) {
    val s = LocalStrings.current
    Backdrop(if (backdrop == "default") "sunset" else backdrop, Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(28.dp))) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)) {
            PixelAvatar(outfit, Modifier.height(190.dp), mood = com.today.app.data.Mood.EXCITED)
            if (species != null) PixelPet(species, PetStage.EGG, Modifier.width(80.dp))
        }
    }
    Text(s.obReadyTitle, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text(s.obReadyBody(nickname), color = TextMuted, textAlign = TextAlign.Center, fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
}
