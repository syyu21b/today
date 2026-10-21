package com.today.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.today.app.data.BodyType
import com.today.app.data.BottomStyle
import com.today.app.data.BrowStyle
import com.today.app.data.EyeStyle
import com.today.app.data.FaceFeature
import com.today.app.data.FaceShape
import com.today.app.data.HairStyle
import com.today.app.data.Looks
import com.today.app.data.Mood
import com.today.app.data.Outfit
import com.today.app.data.Outfits
import com.today.app.data.ShoeStyle
import com.today.app.data.TodayViewModel
import com.today.app.data.TopStyle
import com.today.app.i18n.LocalStrings
import com.today.app.ui.components.AvatarHead
import com.today.app.ui.components.CharacterStage
import com.today.app.ui.components.HeadBox
import com.today.app.ui.components.PixelAvatar
import com.today.app.ui.theme.PixelFont
import kotlin.math.sin
import kotlin.random.Random

// A dark, game-like palette regardless of the app theme.
private val StageTop = Color(0xFF223457)
private val StageMid = Color(0xFF0F182C)
private val StageBottom = Color(0xFF070A13)
private val PanelBg = Color(0xF20C1322)
private val TileBg = Color(0xFF151E33)
private val TileLine = Color(0xFF2B3A5C)
private val GoldC = Color(0xFFD9B77A)
private val Muted = Color(0xFF8E9AB8)
private val TextC = Color(0xFFEAEFFA)

private val PREVIEW_MOODS = listOf(Mood.NORMAL, Mood.HAPPY, Mood.EXCITED, Mood.CALM, Mood.LOVE, Mood.SLEEPY, Mood.SAD, Mood.ANGRY, Mood.SICK)

/**
 * Full-screen character creator in the style of an RPG: big lit stage, expression preview,
 * five save slots, and a panel of tabs (presets, body, face, eyes, hair, makeup, outfit).
 * Edits go to a draft; "외형 결정" applies it.
 */
@Composable
fun CharacterCreatorScreen(vm: TodayViewModel) {
    val s = LocalStrings.current
    val haptic = LocalHapticFeedback.current
    val original = vm.character.outfit
    var draft by remember { mutableStateOf(original) }
    var tab by remember { mutableIntStateOf(0) }
    var mood by remember { mutableStateOf(Mood.NORMAL) }
    var zoomed by remember { mutableStateOf(false) }
    var slot by remember { mutableIntStateOf(0) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val zoom by animateFloatAsState(if (zoomed) 1f else 0f, tween(450), label = "zoom")
    val edit: ((Outfit) -> Outfit) -> Unit = { f ->
        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        draft = f(draft)
    }
    val leave = { if (draft != original) confirmDiscard = true else vm.closeCreator() }
    BackHandler(onBack = leave)

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(StageTop, StageMid, StageBottom)))
            // Swallow touches so nothing underneath reacts.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            // ---- top bar ----
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = leave) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextC, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.creatorBack, color = TextC, fontSize = 13.sp)
                }
                Text(s.creatorTitle, color = GoldC, fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                TopAction(Icons.Filled.Casino, s.creatorRandom) { edit { Looks.random(it, Random(System.nanoTime())) } }
                TopAction(Icons.Filled.RestartAlt, s.creatorReset) { edit { Looks.reset(it) } }
            }

            // ---- stage ----
            Box(Modifier.fillMaxWidth().weight(1f)) {
                StageGlow()
                CharacterStage(draft, Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 4.dp), mood = mood, zoom = zoom)
                // expression preview (like action previews)
                Column(
                    Modifier.align(Alignment.CenterStart).padding(start = 8.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(s.creatorExpression, color = Muted, fontSize = 10.sp)
                    PREVIEW_MOODS.forEach { m ->
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (m == mood) GoldC.copy(alpha = 0.25f) else TileBg.copy(alpha = 0.85f))
                                .border(1.5.dp, if (m == mood) GoldC else TileLine, CircleShape)
                                .clickable { mood = m },
                            contentAlignment = Alignment.Center,
                        ) { Text(m.emoji, fontSize = 18.sp) }
                    }
                }
                // zoom
                Column(Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(TileBg.copy(alpha = 0.85f)).border(1.5.dp, TileLine, CircleShape).clickable { zoomed = !zoomed },
                        contentAlignment = Alignment.Center,
                    ) { Icon(if (zoomed) Icons.Filled.ZoomOut else Icons.Filled.ZoomIn, null, tint = TextC) }
                    Text(if (zoomed) s.creatorZoomBody else s.creatorZoomFace, color = Muted, fontSize = 10.sp)
                }
                // save slots (hidden while the face is zoomed in)
                if (!zoomed) SaveSlots(vm, slot, onSelect = { slot = it }, onSave = { vm.saveLook(slot, draft) }, onLoad = {
                    val saved = vm.looks.getOrNull(slot)
                    if (saved == null) vm.showMessage(s.msgSlotEmpty) else {
                        draft = saved.withAccessoriesOf(draft)
                        vm.showMessage(s.msgLookLoaded(slot + 1))
                    }
                }, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 6.dp))
            }

            // ---- panel ----
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(330.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(PanelBg)
                    .border(1.dp, TileLine, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
            ) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("✨", "🧍", "🙂", "👁", "💇", "💄", "👕").forEachIndexed { i, icon ->
                        val on = tab == i
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) GoldC.copy(alpha = 0.18f) else TileBg)
                                .border(1.5.dp, if (on) GoldC else TileLine, RoundedCornerShape(12.dp))
                                .clickable { tab = i }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(icon, fontSize = 16.sp)
                            Text(s.creatorTab(i), color = if (on) GoldC else TextC, fontSize = 11.sp, fontWeight = if (on) FontWeight.Bold else null)
                        }
                    }
                }
                // Each tab starts scrolled to the top.
                key(tab) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (tab) {
                        0 -> PresetsTab(draft, edit)
                        1 -> BodyTab(draft, edit)
                        2 -> FaceTab(draft, edit)
                        3 -> EyesTab(draft, edit)
                        4 -> HairTab(draft, edit)
                        5 -> MakeupTab(draft, edit)
                        else -> OutfitTab(draft, edit)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF8A6A34), GoldC, Color(0xFF8A6A34))))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            vm.applyLook(draft)
                        },
                    contentAlignment = Alignment.Center,
                ) { Text(s.creatorConfirm, color = Color(0xFF1A1208), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(s.creatorDiscardTitle) },
            text = { Text(s.creatorDiscardBody) },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; vm.closeCreator() }) { Text(s.creatorDiscard) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(s.cancel) } },
        )
    }
}

@Composable
private fun TopAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = GoldC, modifier = Modifier.size(20.dp))
        Text(label, color = Muted, fontSize = 9.sp)
    }
}

/** Spotlight from above, a glowing floor and slowly rising motes. */
@Composable
private fun BoxScope.StageGlow() {
    val t by rememberInfiniteTransition(label = "motes").animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "t")
    val motes = remember { List(26) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) } }
    Box(
        Modifier.matchParentSize().drawBehind {
            drawCircle(
                Brush.radialGradient(listOf(Color(0x664F7BD8), Color.Transparent), Offset(size.width / 2, size.height * 0.35f), size.height * 0.6f),
                size.height * 0.6f, Offset(size.width / 2, size.height * 0.35f),
            )
            val floorY = size.height * 0.93f
            drawOval(
                Brush.radialGradient(listOf(Color(0x995D8CFF), Color(0x00395AA8)), Offset(size.width / 2, floorY), size.width * 0.32f),
                topLeft = Offset(size.width * 0.18f, floorY - size.height * 0.05f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.1f),
            )
            for ((x, y0, ph) in motes) {
                val y = ((y0 - t * (0.4f + ph * 0.5f)) % 1f + 1f) % 1f
                val a = 0.25f + 0.5f * (0.5f + 0.5f * sin((t * 6.28f * 2 + ph * 6.28f)))
                drawCircle(Color(0xFFBFD4FF).copy(alpha = a * (1f - y * 0.4f)), 1.2f + ph * 1.6f, Offset(size.width * x, size.height * (0.35f + y * 0.62f)))
            }
        },
    )
}

@Composable
private fun SaveSlots(vm: TodayViewModel, slot: Int, onSelect: (Int) -> Unit, onSave: () -> Unit, onLoad: () -> Unit, modifier: Modifier) {
    val s = LocalStrings.current
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(TileBg.copy(alpha = 0.85f)).padding(6.dp), horizontalAlignment = Alignment.End) {
        Text(s.creatorSlots, color = Muted, fontSize = 9.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            vm.looks.forEachIndexed { i, look ->
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2A45))
                        .border(1.5.dp, if (i == slot) GoldC else TileLine, CircleShape)
                        .clickable { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (look != null) AvatarHead(look.withoutAccessories(), Modifier.fillMaxSize())
                    else Text("${i + 1}", color = Muted, fontSize = 12.sp, fontFamily = PixelFont)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SmallButton(s.creatorSave, onSave)
            SmallButton(s.creatorLoad, onLoad)
        }
    }
}

@Composable
private fun SmallButton(label: String, onClick: () -> Unit) {
    Text(
        label, color = TextC, fontSize = 11.sp,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF26355A)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

// ------------------------------------------------------------------ building blocks

@Composable
private fun Section(title: String, hint: String? = null) {
    Column {
        Text(title, color = GoldC, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (hint != null) Text(hint, color = Muted, fontSize = 11.sp)
    }
}

/** Grid of tiles, [columns] per row. */
@Composable
private fun <T> TileGrid(items: List<T>, columns: Int, tile: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { tile(it, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Tile(selected: Boolean, onClick: () -> Unit, modifier: Modifier, label: String? = null, aspect: Float = 1f, content: @Composable BoxScope.() -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1C2742), TileBg)))
                .border(if (selected) 2.dp else 1.dp, if (selected) GoldC else TileLine, RoundedCornerShape(10.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
            content = content,
        )
        if (label != null) Text(label, color = if (selected) GoldC else TextC, fontSize = 10.sp, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun Swatches(colors: List<Int>, selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.distinct().forEach { c ->
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .border(if (c == selected) 3.dp else 1.dp, if (c == selected) GoldC else TileLine, CircleShape)
                    .clickable { onPick(c) },
            )
        }
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        (if (on) "✓ " else "") + label,
        color = if (on) GoldC else TextC, fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (on) GoldC.copy(alpha = 0.15f) else TileBg)
            .border(1.dp, if (on) GoldC else TileLine, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

// ------------------------------------------------------------------ tabs

@Composable
private fun ColumnScope.PresetsTab(d: Outfit, edit: ((Outfit) -> Outfit) -> Unit) {
    val s = LocalStrings.current
    Section(s.creatorTab(0), s.creatorPresetHint)
    TileGrid(Looks.presets, 5) { look, m ->
        val o = look.applyTo(d)
        Tile(look.matches(d), { edit { look.applyTo(it) } }, m) { AvatarHead(o.withoutAccessories(), Modifier.fillMaxSize()) }
    }
}

@Composable
private fun ColumnScope.BodyTab(d: Outfit, edit: ((Outfit) -> Outfit) -> Unit) {
    val s = LocalStrings.current
    Section(s.creatorTab(1))
    TileGrid(BodyType.entries, 4) { b, m ->
        Tile(d.body == b, { edit { it.copy(body = b) } }, m, s.bodyName(b), aspect = 0.6f) { PixelAvatar(d.copy(body = b).withoutAccessories(), Modifier.fillMaxHeight().padding(4.dp), animateColors = false) }
    }
    Section(s.secSkin)
    Swatches(Outfits.skinTones, d.skin) { c -> edit { it.copy(skin = c) } }
}

@Composable
private fun ColumnScope.FaceTab(d: Outfit, edit: ((Outfit) -> Outfit) -> Unit) {
    val s = LocalStrings.current
    Section(s.creatorTab(2))
    TileGrid(FaceShape.entries, 4) { f, m ->
        Tile(d.faceShape == f, { edit { it.copy(faceShape = f) } }, m, s.faceShapeName(f)) { AvatarHead(d.copy(faceShape = f).withoutAccessories(), Modifier.fillMaxSize()) }
    }
    Section(s.featuresTitle)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FaceFeature.entries.forEach { f ->
            val on = f in d.features
            Toggle(s.featureName(f), on) { edit { it.copy(features = if (on) it.features - f else it.features + f) } }
        }
    }
}

@Composable
private fun ColumnScope.EyesTab(d: Outfit, edit: ((Outfit) -> Outfit) -> Unit) {
    val s = LocalStrings.current
    Section(s.creatorTab(3))
    TileGrid(EyeStyle.entries, 4) { e, m ->
        Tile(d.eyeStyle == e, { edit { it.copy(eyeStyle = e) } }, m, s.eyeStyleName(e), aspect = HeadBox.EYES.rect.width / HeadBox.EYES.rect.height) {
            AvatarHead(d.copy(eyeStyle = e).withoutAccessories(), Modifier.fillMaxSize(), box = HeadBox.EYES)
        }
    }
    Section(s.secEyeColor)
    Swatches(Outfits.eyeColors, d.eyeColor) { c -> edit { it.copy(eyeColor = c) } }
    Section(s.secBrows)
    TileGrid(BrowStyle.entries, 4) { b, m ->
        Tile(d.browStyle == b, { edit { it.copy(browStyle = b) } }, m, s.browName(b), aspect = HeadBox.EYES.rect.width / HeadBox.EYES.rect.height) {
            AvatarHead(d.copy(browStyle = b).withoutAccessories(), Modifier.fillMaxSize(), box = HeadBox.EYES)
        }
    }
}

@Composable
private fun ColumnScope.HairTab(d: Outfit, edit: ((Outfit) -> Outfit) -> Unit) {
    val s = LocalStrings.current
    Section(s.creatorTab(4))
    TileGrid(HairStyle.entries, 5) { h, m ->
        Tile(d.style == h, { edit { it.copy(hairStyle = h, longHair = h.long) } }, m, s.hairStyleName(h), aspect = 0.8f) {
            AvatarHead(d.copy(hairStyle = h).withoutAccessories(), Modifier.fillMaxSize(), box = HeadBox.HAT)
        }
    }
    Section(s.secColors)
    Swatches(Outfits.hairColors + Outfits.swatches, d.hair) { c -> edit { it.copy(hair = c) } }
}

@Composable
private fun ColumnScope.MakeupTab(d: Outfit, edit: ((Outfit) -> Outfit) -> Unit) {
    val s = LocalStrings.current
    Section(s.secLip)
    Swatches(Outfits.lipColors, d.lipColor) { c -> edit { it.copy(lipColor = c) } }
    TileGrid(listOf(false, true), 4) { b, m ->
        Tile(d.blush == b, { edit { it.copy(blush = b) } }, m, if (b) s.secBlush else "—") { AvatarHead(d.copy(blush = b).withoutAccessories(), Modifier.fillMaxSize()) }
    }
}

private enum class ColorSlot { TOP, ACCENT, BOTTOM, SHOES }

@Composable
private fun ColumnScope.OutfitTab(d: Outfit, edit: ((Outfit) -> Outfit) -> Unit) {
    val s = LocalStrings.current
    Section(s.secTop)
    TileGrid(TopStyle.entries, 5) { t, m ->
        Tile(d.topStyle == t, { edit { it.copy(topStyle = t) } }, m, s.topStyleName(t), aspect = 0.75f) {
            AvatarHead(d.copy(topStyle = t).withoutAccessories(), Modifier.fillMaxSize(), box = HeadBox.BUST)
        }
    }
    if (d.topStyle == TopStyle.TEE) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Toggle(s.sleeveShort, d.shortSleeve) { edit { it.copy(shortSleeve = true) } }
            Toggle(s.sleeveLong, !d.shortSleeve) { edit { it.copy(shortSleeve = false) } }
            Toggle(s.chipStriped, d.striped) { edit { it.copy(striped = !it.striped) } }
        }
    }
    if (d.topStyle != TopStyle.DRESS) {
        Section(s.secBottom)
        TileGrid(BottomStyle.entries, 5) { b, m ->
            Tile(d.bottom == b, { edit { it.copy(bottomStyle = b, shorts = b == BottomStyle.SHORTS) } }, m, s.bottomStyleName(b), aspect = 0.5f) {
                PixelAvatar(d.copy(bottomStyle = b).withoutAccessories(), Modifier.fillMaxHeight().padding(2.dp), animateColors = false)
            }
        }
    }
    Section(s.secShoes)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ShoeStyle.entries.forEach { sh -> Toggle(s.shoeStyleName(sh), d.shoeStyle == sh) { edit { it.copy(shoeStyle = sh) } } }
    }
    var slot by remember { mutableStateOf(ColorSlot.TOP) }
    Section(s.secColors)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ColorSlot.entries.forEach { c ->
            val label = when (c) {
                ColorSlot.TOP -> s.slotTop
                ColorSlot.ACCENT -> s.slotAccent
                ColorSlot.BOTTOM -> s.slotBottom
                ColorSlot.SHOES -> s.slotShoes
            }
            Toggle(label, slot == c) { slot = c }
        }
    }
    val current = when (slot) {
        ColorSlot.TOP -> d.shirt
        ColorSlot.ACCENT -> d.accent
        ColorSlot.BOTTOM -> d.pants
        ColorSlot.SHOES -> d.shoes
    }
    Swatches(Outfits.swatches, current) { c ->
        edit {
            when (slot) {
                ColorSlot.TOP -> it.copy(shirt = c)
                ColorSlot.ACCENT -> it.copy(accent = c)
                ColorSlot.BOTTOM -> it.copy(pants = c)
                ColorSlot.SHOES -> it.copy(shoes = c)
            }
        }
    }
    Section(s.secOutfitPresets)
    TileGrid(Outfits.presets, 6) { p, m ->
        val o = p.withBodyOf(d).copy(topStyle = d.topStyle, bottomStyle = d.bottomStyle, shoeStyle = d.shoeStyle)
        Tile(o == d, { edit { cur -> p.withBodyOf(cur).copy(topStyle = cur.topStyle, bottomStyle = cur.bottomStyle, shoeStyle = cur.shoeStyle) } }, m, aspect = 0.55f) {
            PixelAvatar(o.withoutAccessories(), Modifier.fillMaxHeight().padding(2.dp), animateColors = false)
        }
    }
}
