package com.today.app.data

import kotlin.random.Random

/** A ready-made face + hair + body in the character creator (clothes and accessories stay). */
data class Look(
    val gender: Gender,
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
    val body: BodyType = BodyType.SLIM,
) {
    fun applyTo(o: Outfit) = o.copy(
        gender = gender, body = body, faceShape = face, eyeStyle = eyes, browStyle = brows, hairStyle = hairStyle, longHair = hairStyle.long,
        hair = hair.toInt(), skin = skin.toInt(), eyeColor = iris.toInt(), lipColor = lip.toInt(), blush = blush, features = features,
        tune = emptyMap(),
    ).fitGender()

    /** Same face and hair as [o]? (drives the selected preset tile) */
    fun matches(o: Outfit) = applyTo(o) == o
}

/** Natural lip colour for male looks: the art only uses it for the mouth line. */
private const val LIP_M = 0xFFC08080

object Looks {
    val female = listOf(
        Look(Gender.FEMALE, FaceShape.OVAL, EyeStyle.ALMOND, BrowStyle.NATURAL, HairStyle.LONG, 0xFF4A3028, 0xFFF6D5C3, 0xFF6B4FD0),
        Look(Gender.FEMALE, FaceShape.OVAL, EyeStyle.ROUND, BrowStyle.STRAIGHT, HairStyle.HUSH, 0xFF2B2430, 0xFFFBE0CC, 0xFF3E3A5A, lip = 0xFFE07A86),
        Look(Gender.FEMALE, FaceShape.OVAL, EyeStyle.GENTLE, BrowStyle.NATURAL, HairStyle.BUILD, 0xFF6A4632, 0xFFF5D2B8, 0xFF8A5A2B, blush = true),
        Look(Gender.FEMALE, FaceShape.ROUND, EyeStyle.ALMOND, BrowStyle.ARCHED, HairStyle.WAVY, 0xFFB08A64, 0xFFF6D5C3, 0xFF3E6FB8, lip = 0xFFE8707E),
        Look(Gender.FEMALE, FaceShape.SHARP, EyeStyle.SHARP, BrowStyle.STRAIGHT, HairStyle.HIME, 0xFF151218, 0xFFF5C9A3, 0xFFE94B5A, lip = 0xFFD94F6A),
        Look(Gender.FEMALE, FaceShape.ROUND, EyeStyle.ROUND, BrowStyle.NATURAL, HairStyle.BOB, 0xFF3B2A20, 0xFFFBE0CC, 0xFF3B2A20, blush = true),
        Look(Gender.FEMALE, FaceShape.OVAL, EyeStyle.SHARP, BrowStyle.ARCHED, HairStyle.PIXIE, 0xFFD8C4A8, 0xFFFFEDE0, 0xFF6B4FD0, lip = 0xFFFF8FA3),
        Look(Gender.FEMALE, FaceShape.OVAL, EyeStyle.GENTLE, BrowStyle.NATURAL, HairStyle.PONYTAIL, 0xFF8A3A2E, 0xFFE8B98F, 0xFF3F8F4E),
        Look(Gender.FEMALE, FaceShape.ROUND, EyeStyle.ROUND, BrowStyle.NATURAL, HairStyle.CURLY, 0xFF3B2A20, 0xFF8D5A3B, 0xFF8A5A2B, lip = 0xFFC97A7A),
        Look(Gender.FEMALE, FaceShape.OVAL, EyeStyle.GENTLE, BrowStyle.NATURAL, HairStyle.BUN, 0xFFF4F4F8, 0xFFF5C9A3, 0xFF9E9EA8,
            features = setOf(FaceFeature.WRINKLES)),
    )

    val male = listOf(
        Look(Gender.MALE, FaceShape.OVAL, EyeStyle.ALMOND, BrowStyle.NATURAL, HairStyle.COMMA, 0xFF2A2430, 0xFFF0CDB0, 0xFF3E3A5A, lip = LIP_M),
        Look(Gender.MALE, FaceShape.SQUARE, EyeStyle.SHARP, BrowStyle.THICK, HairStyle.SHORT, 0xFF2A2430, 0xFFE9C2A6, 0xFF3E6FB8, lip = LIP_M, body = BodyType.BROAD),
        Look(Gender.MALE, FaceShape.SHARP, EyeStyle.SHARP, BrowStyle.STRAIGHT, HairStyle.PARTED, 0xFF5A3B22, 0xFFF5D2B8, 0xFF8A5A2B, lip = LIP_M),
        Look(Gender.MALE, FaceShape.OVAL, EyeStyle.GENTLE, BrowStyle.NATURAL, HairStyle.CURTAIN, 0xFF3B2A20, 0xFFF5C9A3, 0xFF3B2A20, lip = LIP_M),
        Look(Gender.MALE, FaceShape.OVAL, EyeStyle.ROUND, BrowStyle.NATURAL, HairStyle.LEAF, 0xFF8A6A50, 0xFFFBE0CC, 0xFF3E6FB8, lip = LIP_M),
        Look(Gender.MALE, FaceShape.SQUARE, EyeStyle.ALMOND, BrowStyle.THICK, HairStyle.CROP, 0xFF111111, 0xFFE0A985, 0xFF3B2A20, lip = LIP_M, body = BodyType.BROAD),
        Look(Gender.MALE, FaceShape.SHARP, EyeStyle.SHARP, BrowStyle.STRAIGHT, HairStyle.WOLF, 0xFFB8B8C4, 0xFFFBE0CC, 0xFFE94B5A, lip = LIP_M),
        Look(Gender.MALE, FaceShape.SQUARE, EyeStyle.GENTLE, BrowStyle.THICK, HairStyle.SLICKED, 0xFF1E1A22, 0xFFE9C2A6, 0xFF3B2A20, lip = LIP_M, body = BodyType.BROAD),
        Look(Gender.MALE, FaceShape.ROUND, EyeStyle.ROUND, BrowStyle.NATURAL, HairStyle.CURLY, 0xFF7A4E2D, 0xFFC68A5E, 0xFF3F8F4E, lip = 0xFFA0525A),
        Look(Gender.MALE, FaceShape.SQUARE, EyeStyle.ALMOND, BrowStyle.THICK, HairStyle.BUZZ, 0xFF3B2A20, 0xFF5C3A24, 0xFF3F8F4E, lip = 0xFFA0525A,
            features = setOf(FaceFeature.BEARD), body = BodyType.BROAD),
    )

    val presets = female + male

    fun forGender(g: Gender) = if (g == Gender.MALE) male else female

    /**
     * "남자 / 여자": the other gender's first look on the same clothes and accessories; what that gender
     * doesn't wear (a dress for him...) is swapped.
     */
    fun switchGender(o: Outfit, g: Gender): Outfit = if (o.gender == g) o else forGender(g).first().applyTo(o)

    /** "무작위 외형": any face, hair and colours of the current gender; clothes and accessories are left alone. */
    fun random(o: Outfit, r: Random = Random.Default): Outfit = o.copy(
        body = BodyType.entries.random(r),
        faceShape = FaceShape.entries.random(r),
        eyeStyle = EyeStyle.entries.random(r),
        browStyle = BrowStyle.entries.random(r),
        hairStyle = HairStyle.forGender(o.gender).random(r),
        hair = Outfits.hairColors.random(r),
        skin = Outfits.skinTones.random(r),
        eyeColor = Outfits.eyeColors.random(r),
        lipColor = if (o.gender == Gender.MALE) LIP_M.toInt() else Outfits.lipColors.random(r),
        blush = o.gender == Gender.FEMALE && r.nextBoolean(),
        features = emptySet(),
        // A few gentle slider nudges so random faces differ in more than the presets do.
        tune = Tune.entries.shuffled(r).take(6).associateWith { (r.nextFloat() * 1.2f - 0.6f) },
    ).let { it.copy(longHair = it.style.long) }

    /**
     * Friends made before the creator existed all share the default face: give each one a face preset
     * of their gender picked from its id (stable across launches). Faces someone already chose are kept.
     */
    fun forFriend(o: Outfit, id: String): Outfit {
        val untouched = o.hairStyle == null && o.faceShape == FaceShape.OVAL && o.eyeStyle == EyeStyle.ALMOND && o.browStyle == BrowStyle.NATURAL &&
            o.tune.isEmpty() && o.eyeColor == DEFAULT_EYE_COLOR
        if (!untouched) return o
        val list = forGender(o.gender)
        val look = list[Math.floorMod(id.hashCode(), list.size)]
        // Keep the friend's own hair colour so their outfit preset still reads the same.
        return look.copy(hair = o.hair.toLong() and 0xFFFFFFFFL).applyTo(o)
    }

    /** "전체 초기화": the gender's first look on default clothes. */
    fun reset(o: Outfit): Outfit = forGender(o.gender).first().applyTo(Outfits.presets.first().withAccessoriesOf(o))
}
