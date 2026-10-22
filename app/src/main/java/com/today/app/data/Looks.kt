package com.today.app.data

import kotlin.random.Random

/** A ready-made face + hair + body in the character creator (clothes and accessories stay). */
data class Look(
    val body: BodyType,
    val face: FaceShape,
    val eyes: EyeStyle,
    val brows: BrowStyle,
    val hairStyle: HairStyle,
    val hair: Long,
    val skin: Long,
    val iris: Long,
    val lip: Long = 0xFFE8707E,
    val blush: Boolean = false,
    val features: Set<FaceFeature> = emptySet(),
) {
    fun applyTo(o: Outfit) = o.copy(
        body = body, faceShape = face, eyeStyle = eyes, browStyle = brows, hairStyle = hairStyle, longHair = hairStyle.long,
        hair = hair.toInt(), skin = skin.toInt(), eyeColor = iris.toInt(), lipColor = lip.toInt(), blush = blush, features = features,
        tune = emptyMap(),
    )

    /** Same face and hair as [o]? (drives the selected preset tile) */
    fun matches(o: Outfit) = applyTo(o) == o
}

object Looks {
    val presets = listOf(
        Look(BodyType.SLIM, FaceShape.OVAL, EyeStyle.ALMOND, BrowStyle.NATURAL, HairStyle.LONG, 0xFF3B2A3E, 0xFFF6D5C3, 0xFF6B4FD0),
        Look(BodyType.SLIM, FaceShape.ROUND, EyeStyle.ROUND, BrowStyle.ARCHED, HairStyle.TWINTAIL, 0xFFE8B45A, 0xFFFBE0CC, 0xFF3E6FB8, blush = true),
        Look(BodyType.SLIM, FaceShape.SHARP, EyeStyle.SHARP, BrowStyle.STRAIGHT, HairStyle.BOB, 0xFF151218, 0xFFF5C9A3, 0xFFE94B5A, lip = 0xFFD94F6A),
        Look(BodyType.SLIM, FaceShape.OVAL, EyeStyle.GENTLE, BrowStyle.NATURAL, HairStyle.PONYTAIL, 0xFFB4462E, 0xFFE8B98F, 0xFF3F8F4E),
        Look(BodyType.SLIM, FaceShape.OVAL, EyeStyle.ALMOND, BrowStyle.ARCHED, HairStyle.BUN, 0xFFD8D8E0, 0xFFFFEDE0, 0xFF9E9EA8, lip = 0xFFFF8FA3),
        Look(BodyType.SLIM, FaceShape.ROUND, EyeStyle.ROUND, BrowStyle.NATURAL, HairStyle.CURLY, 0xFF3B2A20, 0xFF8D5A3B, 0xFF8A5A2B, lip = 0xFFC97A7A),
        Look(BodyType.SLIM, FaceShape.OVAL, EyeStyle.SHARP, BrowStyle.ARCHED, HairStyle.LONG, 0xFFFF7FC8, 0xFFFBE0CC, 0xFF6B4FD0, blush = true),
        Look(BodyType.BROAD, FaceShape.SQUARE, EyeStyle.SHARP, BrowStyle.THICK, HairStyle.SHORT, 0xFF2A2430, 0xFFE9C2A6, 0xFF3E6FB8, lip = 0xFFC97A7A),
        Look(BodyType.BROAD, FaceShape.OVAL, EyeStyle.ALMOND, BrowStyle.STRAIGHT, HairStyle.SPIKY, 0xFFD9A441, 0xFFF5C9A3, 0xFF2E9BDA, lip = 0xFFC97A7A),
        Look(BodyType.BROAD, FaceShape.SQUARE, EyeStyle.GENTLE, BrowStyle.THICK, HairStyle.BUZZ, 0xFF111111, 0xFF5C3A24, 0xFF3B2A20, lip = 0xFFB8505A),
        Look(BodyType.BROAD, FaceShape.OVAL, EyeStyle.GENTLE, BrowStyle.NATURAL, HairStyle.SHORT, 0xFF9E9EA8, 0xFFE8B98F, 0xFF8A5A2B, lip = 0xFFC97A7A,
            features = setOf(FaceFeature.WRINKLES, FaceFeature.MUSTACHE)),
        Look(BodyType.BROAD, FaceShape.SQUARE, EyeStyle.ALMOND, BrowStyle.THICK, HairStyle.CURLY, 0xFF7A4E2D, 0xFFC68A5E, 0xFF3F8F4E, lip = 0xFFB8505A,
            features = setOf(FaceFeature.BEARD)),
        Look(BodyType.SLIM, FaceShape.ROUND, EyeStyle.ROUND, BrowStyle.NATURAL, HairStyle.SHORT, 0xFF5A3B22, 0xFFFBE0CC, 0xFF3B2A20, blush = true,
            features = setOf(FaceFeature.FRECKLES)),
        Look(BodyType.SLIM, FaceShape.OVAL, EyeStyle.GENTLE, BrowStyle.NATURAL, HairStyle.BOB, 0xFFF4F4F8, 0xFFF5C9A3, 0xFF9E9EA8,
            features = setOf(FaceFeature.WRINKLES)),
        Look(BodyType.BROAD, FaceShape.SHARP, EyeStyle.SHARP, BrowStyle.STRAIGHT, HairStyle.LONG, 0xFF1B1B2F, 0xFFE0A985, 0xFFE94B5A, lip = 0xFFC97A7A),
    )

    /** "무작위 외형": any face, hair and colours; clothes and accessories are left alone. */
    fun random(o: Outfit, r: Random = Random.Default): Outfit = o.copy(
        body = BodyType.entries.random(r),
        faceShape = FaceShape.entries.random(r),
        eyeStyle = EyeStyle.entries.random(r),
        browStyle = BrowStyle.entries.random(r),
        hairStyle = HairStyle.entries.random(r),
        hair = Outfits.hairColors.random(r),
        skin = Outfits.skinTones.random(r),
        eyeColor = Outfits.eyeColors.random(r),
        lipColor = Outfits.lipColors.random(r),
        blush = r.nextBoolean(),
        features = emptySet(),
        // A few gentle slider nudges so random faces differ in more than the presets do.
        tune = Tune.entries.shuffled(r).take(6).associateWith { (r.nextFloat() * 1.2f - 0.6f) },
    ).let { it.copy(longHair = it.style.long) }

    /**
     * Friends made before the creator existed all share the default face: give each one a face preset
     * picked from its id (stable across launches). Faces someone already chose are kept.
     */
    fun forFriend(o: Outfit, id: String): Outfit {
        val untouched = o.hairStyle == null && o.faceShape == FaceShape.OVAL && o.eyeStyle == EyeStyle.ALMOND && o.browStyle == BrowStyle.NATURAL &&
            o.tune.isEmpty() && o.eyeColor == DEFAULT_EYE_COLOR
        if (!untouched) return o
        val look = presets[Math.floorMod(id.hashCode(), presets.size)]
        // Keep the friend's own hair colour and length so their outfit preset still reads the same.
        val hair = if (o.longHair == look.hairStyle.long) look.hairStyle else if (o.longHair) HairStyle.LONG else HairStyle.SHORT
        return look.copy(hairStyle = hair, hair = o.hair.toLong() and 0xFFFFFFFFL).applyTo(o)
    }

    /** "전체 초기화": the first preset on default clothes. */
    fun reset(o: Outfit): Outfit = presets.first().applyTo(Outfits.presets.first().withAccessoriesOf(o))
}
