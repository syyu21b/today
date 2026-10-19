package com.today.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.today.app.data.Outfit
import com.today.app.data.Outfits
import com.today.app.data.TodayViewModel
import com.today.app.ui.components.PixelAvatar
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.TextMuted
import com.today.app.ui.theme.Violet

@Composable
fun OutfitScreen(vm: TodayViewModel, modifier: Modifier = Modifier) {
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // System photo picker: no storage permission required.
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            photoUri = uri
            photoBitmap = null
            vm.applyStyleFromUri(uri)
        }
    }
    // System camera app returning a small preview bitmap: no CAMERA permission required.
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            photoBitmap = bmp
            photoUri = null
            vm.applyStyleFromBitmap(bmp)
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("옷장", style = MaterialTheme.typography.headlineMedium)
        Text("사진을 찍거나 올리면 AI가 사진의 분위기에 맞춰 의상을 바꿔줘요.", color = TextMuted)

        // ---- Avatar stage ----
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF3A2F6B), Color(0xFF1A1628)))),
            contentAlignment = Alignment.Center,
        ) {
            // floor shadow
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 26.dp)
                    .size(width = 120.dp, height = 14.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f)),
            )
            FloatingAvatar(vm.character.outfit)
            Text(
                vm.character.outfit.name,
                fontFamily = PixelFont, color = Gold,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            )
            androidx.compose.animation.AnimatedVisibility(vm.isAnalyzingStyle, enter = fadeIn(), exit = fadeOut()) {
                ScanOverlay(vm.analyzeStep)
            }
        }

        // ---- Photo buttons ----
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { cameraLauncher.launch(null) },
                enabled = !vm.isAnalyzingStyle,
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Icon(Icons.Filled.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text("사진 찍기")
            }
            OutlinedButton(
                onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !vm.isAnalyzingStyle,
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Icon(Icons.Filled.PhotoLibrary, null)
                Spacer(Modifier.width(8.dp))
                Text("앨범에서")
            }
        }

        // ---- Source photo + extracted palette ----
        if (photoUri != null || photoBitmap != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val photoModifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                when {
                    photoUri != null -> AsyncImage(
                        model = photoUri, contentDescription = "선택한 사진",
                        contentScale = ContentScale.Crop, modifier = photoModifier,
                    )
                    photoBitmap != null -> Image(
                        bitmap = photoBitmap!!.asImageBitmap(), contentDescription = "찍은 사진",
                        contentScale = ContentScale.Crop, modifier = photoModifier,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Violet, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("추출된 스타일 팔레트", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (vm.extractedSwatches.isEmpty()) {
                            Text("분석 중…", color = TextMuted)
                        }
                        vm.extractedSwatches.forEach { c ->
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(c))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                            )
                        }
                    }
                    Text("머리 · 상의 · 포인트 · 하의 · 신발", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }

        // ---- Presets ----
        Text("프리셋 의상", style = MaterialTheme.typography.titleLarge)
        Outfits.presets.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { outfit ->
                    PresetCard(
                        outfit = outfit,
                        selected = vm.character.outfit == outfit,
                        onClick = { vm.applyOutfit(outfit) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FloatingAvatar(outfit: Outfit) {
    val t = rememberInfiniteTransition(label = "float")
    val dy by t.animateFloat(
        0f, -8f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "dy",
    )
    PixelAvatar(outfit, Modifier.height(200.dp).graphicsLayer { translationY = dy.dp.toPx() })
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
        Text(outfit.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.Bold else null)
    }
}
