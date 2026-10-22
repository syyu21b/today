package com.today.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.today.app.data.Mood
import com.today.app.data.Outfit
import com.today.app.data.PetSpecies
import com.today.app.data.PetStage

/**
 * The full-body character with the pet standing right beside its feet. The avatar's art box has wide
 * empty margins around the figure, so the pet is tucked into the right one instead of sitting next to
 * the whole box. [height] is the character's height; the pet scales with it.
 */
@Composable
fun AvatarWithPet(
    outfit: Outfit,
    species: PetSpecies?,
    stage: PetStage,
    height: Dp,
    modifier: Modifier = Modifier,
    mood: Mood = Mood.NORMAL,
    avatarModifier: Modifier = Modifier,
    onPet: (() -> Unit)? = null,
) {
    val avatarW = height * AVATAR_ASPECT
    if (species == null) {
        Box(modifier.size(avatarW, height)) { PixelAvatar(outfit, avatarModifier.size(avatarW, height), mood = mood) }
        return
    }
    val petW = height * 0.46f
    // The figure's right side is ~75% across its box; the animal starts ~22% into the pet's box.
    val petX = avatarW * 0.74f - petW * 0.2f
    // Soles sit 4% above the bottom of the avatar box.
    val petLift = height * 0.035f
    val totalW = petX + petW
    Box(modifier.size(totalW, height)) {
        PixelAvatar(outfit, avatarModifier.size(avatarW, height).align(Alignment.TopStart), mood = mood)
        PixelPet(
            species, stage,
            Modifier.width(petW).align(Alignment.BottomStart).offset(x = petX, y = -petLift),
            onPet = onPet,
        )
    }
}
