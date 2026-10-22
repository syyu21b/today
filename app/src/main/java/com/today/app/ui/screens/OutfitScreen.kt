package com.today.app.ui.screens

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.today.app.data.FaceFeature
import com.today.app.data.Mood
import com.today.app.data.Outfit
import com.today.app.data.PetSpecies
import com.today.app.data.PetStage
import com.today.app.data.Outfits
import com.today.app.data.StyleResult
import com.today.app.data.TodayViewModel
import com.today.app.i18n.LocalStrings
import com.today.app.i18n.Strings
import com.today.app.ui.components.AvatarWithPet
import com.today.app.ui.components.Backdrop
import com.today.app.ui.components.PixelAvatar
import com.today.app.ui.components.PixelPet
import com.today.app.ui.theme.Hairline
import com.today.app.ui.theme.Ink
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.NightSurfaceHigh
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.TextMuted
import com.today.app.ui.theme.Violet
import java.io.File

/** Outfit color slots the user can fine-tune. */
private enum class Slot { HAIR, SKIN, SHIRT, STRIPE, ACCENT, PANTS, SHOES }

private fun Slot.label(s: Strings) = when (this) {
    Slot.HAIR -> s.slotHair
    Slot.SKIN -> s.slotSkin
    Slot.SHIRT -> s.slotTop
    Slot.STRIPE -> s.slotStripe
    Slot.ACCENT -> s.slotAccent
    Slot.PANTS -> s.slotBottom
    Slot.SHOES -> s.slotShoes
}

private fun Outfit.colorOf(slot: Slot) = when (slot) {
    Slot.HAIR -> hair
    Slot.SKIN -> skin
    Slot.SHIRT -> shirt
    Slot.STRIPE -> stripe
    Slot.ACCENT -> accent
    Slot.PANTS -> pants
    Slot.SHOES -> shoes
}

private fun Outfit.withColor(slot: Slot, c: Int) = when (slot) {
    Slot.HAIR -> copy(hair = c)
    Slot.SKIN -> copy(skin = c)
    Slot.SHIRT -> copy(shirt = c)
    Slot.STRIPE -> copy(stripe = c)
    Slot.ACCENT -> copy(accent = c)
    Slot.PANTS -> copy(pants = c)
    Slot.SHOES -> copy(shoes = c)
}

@Composable
fun OutfitScreen(vm: TodayViewModel, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val context = LocalContext.current
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    // System photo picker: no storage permission required.
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.analyzePhoto(uri)
    }
    // Full-resolution photo written to our FileProvider (better recognition than a thumbnail).
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCameraUri
        if (saved && uri != null) vm.analyzePhoto(uri.toUri())
    }

    val launchCamera: () -> Unit = {
        try {
            val dir = File(context.cacheDir, "camera").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // keep only the latest shot
            val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingCameraUri = uri.toString()
            cameraLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            vm.showMessage(s.cameraNotFound)
        } catch (e: Exception) {
            vm.showMessage(s.cameraError(e.message))
        }
    }

    val outfit = vm.character.outfit

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(s.outfitTitle, style = MaterialTheme.typography.headlineMedium)
        Text(s.outfitIntro, color = TextMuted, fontSize = 14.sp)
        // RPG-style creator: face, eyes, hair, makeup, clothes.
        Box(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF223457), Color(0xFF3A2F6B))))
                .border(1.dp, Color(0xFFD9B77A), RoundedCornerShape(14.dp))
                .clickable(onClick = vm::openCreator),
            contentAlignment = Alignment.Center,
        ) { Text("✨ " + s.creatorOpen, color = Color(0xFFD9B77A), fontWeight = FontWeight.Bold, fontSize = 16.sp) }

        // ---- Avatar stage ----
        Backdrop(
            vm.backdrop,
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(24.dp)),
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
                    .size(width = if (vm.pet != null) 150.dp else 110.dp, height = 14.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f)),
            )
            FloatingAvatar(outfit, vm.mood, vm.pet?.species, vm.pet?.stage, onPet = vm::petThePet, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
            Text(
                s.outfitName(outfit.name),
                fontFamily = PixelFont, color = Gold,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            )
            androidx.compose.animation.AnimatedVisibility(vm.isAnalyzingStyle, enter = fadeIn(), exit = fadeOut()) {
                ScanOverlay(if (vm.analyzeStep < 0) s.analyzeLoading else s.analyzeSteps[vm.analyzeStep])
            }
        }

        // ---- Photo buttons ----
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = launchCamera,
                enabled = !vm.isAnalyzingStyle,
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Icon(Icons.Filled.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text(s.takePhoto)
            }
            OutlinedButton(
                onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !vm.isAnalyzingStyle,
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Icon(Icons.Filled.PhotoLibrary, null)
                Spacer(Modifier.width(8.dp))
                Text(s.fromGallery)
            }
        }
        Text(s.photoTip, color = TextMuted, fontSize = 12.sp)

        // ---- Source photo with recognized regions ----
        vm.stylePhoto?.let { photo ->
            AnalyzedPhoto(photo = photo, result = vm.styleResult, analyzing = vm.isAnalyzingStyle, onClose = vm::clearStylePhoto)
        }

        // ---- Pet ----
        PetCard(vm)

        // ---- Manual fine-tune ----
        FineTuneCard(outfit = outfit, extracted = vm.styleResult?.regions?.map { it.color }.orEmpty(), onChange = vm::editOutfit)

        // ---- Accessories (level / reward-box unlocks) ----
        AccessoriesCard(vm)
        BackdropCard(vm)

        // ---- Presets ----
        Text(s.presetsTitle, style = MaterialTheme.typography.titleLarge)
        Outfits.presets.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { preset ->
                    PresetCard(
                        // Shown on the player's own body (hair style, skin, accessories).
                        outfit = preset.withBodyOf(outfit),
                        selected = preset.withBodyOf(outfit) == outfit,
                        onClick = { vm.applyOutfit(preset) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AnalyzedPhoto(photo: Bitmap, result: StyleResult?, analyzing: Boolean, onClose: () -> Unit) {
    val s = LocalStrings.current
    val textMeasurer = rememberTextMeasurer()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, null, tint = Violet, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (analyzing) s.analyzing else s.analysisResult, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose, enabled = !analyzing) { Icon(Icons.Filled.Close, contentDescription = s.closePhoto, tint = TextMuted) }
        }
        val image = remember(photo) { photo.asImageBitmap() }
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .heightIn(max = 360.dp)
                .aspectRatio(photo.width.toFloat() / photo.height, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            Image(image, contentDescription = s.analyzedPhoto, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            result?.let { r ->
                Canvas(Modifier.fillMaxSize()) {
                    val sx = size.width / photo.width
                    val sy = size.height / photo.height
                    r.regions.forEach { region ->
                        val tl = Offset(region.rect.left * sx, region.rect.top * sy)
                        val sz = Size(region.rect.width() * sx, region.rect.height() * sy)
                        drawRect(Color.Black.copy(alpha = 0.6f), tl, sz, style = Stroke(5f))
                        drawRect(Color.White, tl, sz, style = Stroke(2.5f))
                        val label = textMeasurer.measure(s.bodyPart(region.part), TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        val labelTl = Offset(tl.x.coerceAtLeast(0f), (tl.y - label.size.height - 2).coerceAtLeast(0f))
                        // colored chip = extracted color, then the label text
                        drawRect(Color(region.color), labelTl, Size(14f, label.size.height.toFloat()))
                        drawRect(Color.Black.copy(alpha = 0.65f), labelTl.copy(x = labelTl.x + 14f), Size(label.size.width + 6f, label.size.height.toFloat()))
                        drawText(label, topLeft = labelTl.copy(x = labelTl.x + 17f))
                    }
                }
            }
        }
        result?.let { r ->
            Spacer(Modifier.height(10.dp))
            Text(s.styleSummary(r), fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                r.regions.distinctBy { it.part }.forEach { region ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(region.color))
                                .border(1.dp, Hairline.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                        )
                        Text(s.bodyPart(region.part), fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun FineTuneCard(outfit: Outfit, extracted: List<Int>, onChange: ((Outfit) -> Outfit) -> Unit) {
    val s = LocalStrings.current
    var slot by remember { mutableStateOf(Slot.SHIRT) }
    // If stripes get turned off while editing the stripe color, fall back to the shirt.
    val activeSlot = if (slot == Slot.STRIPE && !outfit.striped) Slot.SHIRT else slot
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(s.fineTuneTitle, style = MaterialTheme.typography.titleMedium)
        Text(s.fineTuneHint, color = TextMuted, fontSize = 12.sp)

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = outfit.shortSleeve, onClick = { onChange { it.copy(shortSleeve = !it.shortSleeve) } }, label = { Text(s.chipShortSleeve) })
            FilterChip(selected = outfit.striped, onClick = { onChange { it.copy(striped = !it.striped) } }, label = { Text(s.chipStriped) })
            FilterChip(selected = outfit.shorts, onClick = { onChange { it.copy(shorts = !it.shorts) } }, label = { Text(s.chipShorts) })
        }
        Text(s.hairStyleTitle, fontSize = 13.sp, color = TextMuted)
        HairStyleChips(outfit.style, outfit.gender) { h -> onChange { it.copy(hairStyle = h, longHair = h.long) } }
        Text(s.featuresTitle, fontSize = 13.sp, color = TextMuted)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FaceFeature.entries.forEach { f ->
                val on = f in outfit.features
                FilterChip(selected = on, onClick = { onChange { it.copy(features = if (on) it.features - f else it.features + f) } }, label = { Text(s.featureName(f)) })
            }
        }

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Slot.entries.filter { it != Slot.STRIPE || outfit.striped }.forEach { slotItem ->
                val isActive = activeSlot == slotItem
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) NightSurfaceHigh else Color.Transparent)
                        .clickable { slot = slotItem }
                        .padding(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(outfit.colorOf(slotItem)))
                            .border(2.dp, if (isActive) Gold else Hairline.copy(alpha = 0.2f), CircleShape),
                    )
                    Text(slotItem.label(s), fontSize = 11.sp, color = if (isActive) Ink else TextMuted)
                }
            }
        }

        val choices = when (activeSlot) {
            Slot.SKIN -> Outfits.skinTones
            Slot.HAIR -> extracted + Outfits.hairColors + Outfits.swatches
            else -> extracted + Outfits.swatches
        }.distinct()
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            choices.forEach { c ->
                val selected = outfit.colorOf(activeSlot) == c
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(c))
                        .border(if (selected) 3.dp else 1.dp, if (selected) Gold else Hairline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .clickable { onChange { it.withColor(activeSlot, c) } },
                )
            }
        }
    }
}

@Composable
private fun FloatingAvatar(outfit: Outfit, mood: Mood, species: PetSpecies?, stage: PetStage?, onPet: () -> Unit, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "float")
    val dy by t.animateFloat(0f, -6f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "dy")
    // The pet stands right next to the character's feet.
    AvatarWithPet(
        outfit, species, stage ?: PetStage.EGG, height = 228.dp, modifier = modifier, mood = mood,
        avatarModifier = Modifier.graphicsLayer { translationY = dy.dp.toPx() }, onPet = onPet,
    )
}

@Composable
private fun ScanOverlay(step: String) {
    val t = rememberInfiniteTransition(label = "scan")
    val pulse by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse")
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.AutoAwesome, null, tint = Gold, modifier = Modifier.size(40.dp).alpha(pulse))
            Spacer(Modifier.height(12.dp))
            Text(step, fontFamily = PixelFont, color = Color.White)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(Modifier.width(160.dp), color = Gold)
        }
    }
}

@Composable
private fun PresetCard(outfit: Outfit, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, if (selected) Gold else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            PixelAvatar(outfit, Modifier.fillMaxHeight().padding(4.dp), animateColors = false)
        }
        Text(LocalStrings.current.outfitName(outfit.name), style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.Bold else null)
    }
}
