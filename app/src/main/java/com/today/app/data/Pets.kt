package com.today.app.data

/** The pets on offer, most commonly kept first. Art lives in assets/pet_art.json (tools/character_art/pets.py), names in Strings. */
enum class PetSpecies(val emoji: String) {
    DOG("🐶"), CAT("🐱"), FISH("🐠"), HAMSTER("🐹"), PARROT("🦜"), HEDGEHOG("🦔"), LIZARD("🦎"),
}

/** Growth stages: the pet hatches and grows as the player earns XP. [scale] = drawn size. */
enum class PetStage(val minGrowth: Int, val scale: Float) {
    EGG(0, 0.7f), BABY(50, 0.6f), CHILD(300, 0.8f), ADULT(1000, 1f),
}

data class PetState(
    val species: PetSpecies,
    val name: String,
    /** XP earned since adoption (goes down again when a quest is unchecked). */
    val growth: Int = 0,
) {
    val stage: PetStage get() = PetStage.entries.last { growth >= it.minGrowth }
    val nextStage: PetStage? get() = PetStage.entries.getOrNull(stage.ordinal + 1)

    /** 0..1 towards the next stage (1 when fully grown). */
    val stageProgress: Float
        get() {
            val next = nextStage ?: return 1f
            return (growth - stage.minGrowth).toFloat() / (next.minGrowth - stage.minGrowth)
        }
}

const val MAX_PET_NAME = 10

object Pets {
    /** Applies [delta] growth; the second value is the stage just reached (only when it went up). */
    fun grow(pet: PetState, delta: Int): Pair<PetState, PetStage?> {
        val next = pet.copy(growth = (pet.growth + delta).coerceAtLeast(0))
        return next to next.stage.takeIf { it.ordinal > pet.stage.ordinal }
    }
}
