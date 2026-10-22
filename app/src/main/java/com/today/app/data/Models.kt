package com.today.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** A single daily routine turned into a quest. */
data class Quest(
    val id: String,
    val title: String,
    val emoji: String,
    val xp: Int,
    val done: Boolean = false,
    /** Weekdays it's scheduled on; empty = every day. On other days it rests (shown, not counted). */
    val days: Set<DayOfWeek> = emptySet(),
    /** Minute of the day for this quest's own reminder; null = none. */
    val remindAt: Int? = null,
) {
    fun activeOn(date: LocalDate) = days.isEmpty() || date.dayOfWeek in days
}

/** The quests that count on [date] (rest-day quests are left out). */
fun List<Quest>.activeOn(date: LocalDate) = filter { it.activeOn(date) }

/** Every quest scheduled on [date] is done (nothing scheduled = not complete). */
fun List<Quest>.completeOn(date: LocalDate) = activeOn(date).let { it.isNotEmpty() && it.all { q -> q.done } }

/** The day has quests but none is scheduled: a planned rest day, which doesn't break a streak. */
fun List<Quest>.isRestDayOn(date: LocalDate) = isNotEmpty() && activeOn(date).isEmpty()

const val MAX_QUESTS_PER_DAY = 10
const val MAX_QUEST_TITLE = 30
const val MIN_QUEST_XP = 5
const val MAX_QUEST_XP = 30

const val DEFAULT_SKIN = 0xFFF5C9A3.toInt()

/** The character's sex: face, body, hair styles and some clothes are drawn differently. [code] = art key. */
enum class Gender(val code: String) { FEMALE("F"), MALE("M") }

private val BOTH = setOf(Gender.FEMALE, Gender.MALE)
private val F = setOf(Gender.FEMALE)
private val M = setOf(Gender.MALE)

/** Hair shapes drawn by the avatar (art in character_art.json); [genders] = who the creator offers them to. */
enum class HairStyle(val long: Boolean, val genders: Set<Gender>) {
    // Women (2025-26 trends): long layers + curtain bangs, hush cut, build perm, Elizabeth waves, hime cut, tassel bob, pixie...
    LONG(true, F), HUSH(true, F), BUILD(true, F), WAVY(true, F), HIME(true, F), BOB(true, F), PIXIE(false, F), PONYTAIL(true, F), BUN(false, F), TWINTAIL(true, F),
    // Men: soft two-block, comma hair, 6:4 parted perm, curtain hair, leaf cut, textured crop, wolf cut, slick-back...
    SHORT(false, M), COMMA(false, M), PARTED(false, M), CURTAIN(false, M), LEAF(false, M), CROP(false, M), WOLF(false, M), SLICKED(false, M),
    CURLY(false, BOTH), BUZZ(false, M),
    /** Old saves only; no longer offered. */
    SPIKY(false, emptySet()),
    ;

    companion object {
        fun forGender(g: Gender) = entries.filter { g in it.genders }
    }
}

/** Optional face details, e.g. for an older-looking character. */
enum class FaceFeature { FRECKLES, MUSTACHE, BEARD, WRINKLES }

// Character creator options (art in assets/character_art.json, drawn by PixelAvatar).
/** Build within the chosen [Gender]; BROAD adds to the shoulder / waist / limb sliders. */
enum class BodyType { SLIM, BROAD }
enum class FaceShape { OVAL, ROUND, SHARP, SQUARE }
enum class EyeStyle { ALMOND, ROUND, SHARP, GENTLE }
enum class BrowStyle { NATURAL, ARCHED, STRAIGHT, THICK }
enum class TopStyle(val genders: Set<Gender> = BOTH) { TEE, SHIRT, KNIT, HOODIE, JACKET, DRESS(F) }
enum class BottomStyle(val genders: Set<Gender> = BOTH) { PANTS, SHORTS, SKIRT(F) }
enum class ShoeStyle { SNEAKERS, BOOTS }

/** The hair photo styles and presets without a chosen style get: long or short for [g]. */
fun defaultHair(g: Gender, long: Boolean) = when {
    g == Gender.MALE -> HairStyle.SHORT
    long -> HairStyle.LONG
    else -> HairStyle.PIXIE
}

/** Which creator tab a fine-tune slider sits in. */
enum class TuneGroup { BODY, FACE, EYES, BROWS, NOSE_MOUTH }

/**
 * Fine-tune sliders of the character creator, each -1..1 (0 = as drawn). [art] names the morph target
 * in character_art.json; the rest move, scale or tilt a feature when it is drawn (PixelAvatar.kt).
 */
enum class Tune(val group: TuneGroup, val art: String? = null) {
    HEAD_SIZE(TuneGroup.BODY), SHOULDERS(TuneGroup.BODY, "shoulders"), WAIST(TuneGroup.BODY, "waist"), HIPS(TuneGroup.BODY, "hips"),
    LEGS(TuneGroup.BODY, "legs"), LIMBS(TuneGroup.BODY, "limbs"),
    FACE_WIDTH(TuneGroup.FACE, "faceWidth"), CHEEKS(TuneGroup.FACE, "cheeks"), JAW(TuneGroup.FACE, "jaw"), CHIN(TuneGroup.FACE, "chin"),
    EYE_SIZE(TuneGroup.EYES), EYE_HEIGHT(TuneGroup.EYES), EYE_SPACING(TuneGroup.EYES), EYE_TILT(TuneGroup.EYES),
    BROW_HEIGHT(TuneGroup.BROWS), BROW_TILT(TuneGroup.BROWS), BROW_THICKNESS(TuneGroup.BROWS),
    NOSE_SIZE(TuneGroup.NOSE_MOUTH), NOSE_HEIGHT(TuneGroup.NOSE_MOUTH), MOUTH_SIZE(TuneGroup.NOSE_MOUTH), MOUTH_HEIGHT(TuneGroup.NOSE_MOUTH),
}

const val DEFAULT_EYE_COLOR = 0xFF6B4FD0.toInt()
const val DEFAULT_LIP_COLOR = 0xFFE8707E.toInt()

/**
 * Everything needed to paint the pixel avatar: colors (ARGB ints) + garment shape flags.
 * [name] is a language-neutral key for presets ("ocean") / photo styles ("photo:3"),
 * shown through Strings.outfitName().
 */
data class Outfit(
    val name: String,
    val hair: Int,
    val shirt: Int,
    val accent: Int,
    val pants: Int,
    val shoes: Int,
    val skin: Int = DEFAULT_SKIN,
    val stripe: Int = accent,
    val striped: Boolean = false,
    val shortSleeve: Boolean = false,
    val shorts: Boolean = false,
    val longHair: Boolean = false,
    /** Equipped accessory ids per slot ("" = none), see [Accessories]. */
    val head: String = "",
    val face: String = "",
    val back: String = "",
    /** Chosen hair shape; null = follow [longHair] (presets and photo styles). */
    val hairStyle: HairStyle? = null,
    val features: Set<FaceFeature> = emptySet(),
    // Character creator
    val body: BodyType = BodyType.SLIM,
    val faceShape: FaceShape = FaceShape.OVAL,
    val eyeStyle: EyeStyle = EyeStyle.ALMOND,
    val browStyle: BrowStyle = BrowStyle.NATURAL,
    val eyeColor: Int = DEFAULT_EYE_COLOR,
    val lipColor: Int = DEFAULT_LIP_COLOR,
    /** Stronger cheek blush (makeup). */
    val blush: Boolean = false,
    val topStyle: TopStyle = TopStyle.TEE,
    /** null = follow [shorts] (presets and photo styles). */
    val bottomStyle: BottomStyle? = null,
    val shoeStyle: ShoeStyle = ShoeStyle.SNEAKERS,
    /** Fine-tune slider values (-1..1); missing = 0. */
    val tune: Map<Tune, Float> = emptyMap(),
    val gender: Gender = Gender.FEMALE,
) {
    val style: HairStyle get() = hairStyle ?: defaultHair(gender, longHair)
    val bottom: BottomStyle get() = bottomStyle ?: if (shorts) BottomStyle.SHORTS else BottomStyle.PANTS

    fun tuned(t: Tune): Float = tune[t] ?: 0f

    /** Sets one slider; values near 0 are dropped so an untouched character compares equal. */
    fun withTune(t: Tune, value: Float): Outfit {
        val v = value.coerceIn(-1f, 1f)
        return copy(tune = if (kotlin.math.abs(v) < 0.02f) tune - t else tune + (t to v))
    }

    fun accessoryIn(slot: AccessorySlot) = when (slot) {
        AccessorySlot.HEAD -> head
        AccessorySlot.FACE -> face
        AccessorySlot.BACK -> back
    }

    fun withAccessory(slot: AccessorySlot, id: String) = when (slot) {
        AccessorySlot.HEAD -> copy(head = id)
        AccessorySlot.FACE -> copy(face = id)
        AccessorySlot.BACK -> copy(back = id)
    }

    fun withoutAccessories() = copy(head = "", face = "", back = "")

    /** Presets and photo styles keep whatever the character is already wearing. */
    fun withAccessoriesOf(other: Outfit) = copy(head = other.head, face = other.face, back = other.back)

    /** Clothes from this outfit on [other]'s body: accessories, hair, face and skin stay. */
    fun withBodyOf(other: Outfit) = withAccessoriesOf(other).copy(
        hairStyle = other.hairStyle, features = other.features, skin = other.skin,
        body = other.body, faceShape = other.faceShape, eyeStyle = other.eyeStyle, browStyle = other.browStyle,
        eyeColor = other.eyeColor, lipColor = other.lipColor, blush = other.blush, tune = other.tune, gender = other.gender,
    ).fitGender()

    /** Swaps whatever [gender] can't wear (a dress, a skirt, a hair style) for the closest thing it can. */
    fun fitGender(): Outfit {
        var o = this
        if (gender !in o.topStyle.genders) o = o.copy(topStyle = TopStyle.TEE)
        if (gender !in o.bottom.genders) o = o.copy(bottomStyle = BottomStyle.PANTS, shorts = false)
        if (o.hairStyle != null && gender !in o.hairStyle!!.genders) o = o.copy(hairStyle = defaultHair(gender, o.longHair))
        return o.copy(longHair = o.style.long)
    }
}

data class CharacterState(
    val level: Int = 1,
    val xp: Int = 0,
    val outfit: Outfit = Outfits.presets.first(),
) {
    val xpToNext: Int get() = xpNeededFor(level)
    val progress: Float get() = xp.toFloat() / xpToNext

    companion object {
        fun xpNeededFor(level: Int) = 100 + (level - 1) * 50
    }
}

/** How a calendar day looks: drives the dots in the week strip / month view. */
enum class DayStatus { EMPTY, PLANNED, PARTIAL, COMPLETE, MISSED, SHIELDED }

/** Quests copied from one day, waiting to be pasted onto another. */
data class QuestClipboard(val sourceDate: LocalDate, val quests: List<Quest>)

/** Language of every piece of UI text. Labels are always shown in their own language. */
enum class AppLanguage(val label: String) { KOREAN("한국어"), ENGLISH("English") }

enum class ChatFontSize(val sp: Int) { SMALL(13), MEDIUM(15), LARGE(18) }

/** Every user preference. Times are minutes since midnight. */
data class AppSettings(
    val nickname: String = DummyData.DEFAULT_NICKNAME_KO,
    val language: AppLanguage = AppLanguage.KOREAN,
    // Quests
    val autoCarryOver: Boolean = true,
    val weekStartsMonday: Boolean = false,
    val celebrationEffects: Boolean = true,
    val defaultQuestXp: Int = 20,
    // Notifications (master switch stays off until the user grants permission)
    val notificationsEnabled: Boolean = false,
    val questReminder: Boolean = true,
    val questReminderTime: Int = 9 * 60,
    val streakReminder: Boolean = true,
    val streakReminderTime: Int = 21 * 60,
    val messageNotifications: Boolean = true,
    val messagePreview: Boolean = true,
    // Messages
    val chatFontSize: ChatFontSize = ChatFontSize.MEDIUM,
    val enterToSend: Boolean = true,
    val readReceipts: Boolean = true,
    val autoReplies: Boolean = true,
    val saveChatHistory: Boolean = true,
    // Friends
    val contactsPromptDone: Boolean = false,
    // Profile status: a mood shown on the character (null = automatic) and a short message
    val statusMood: Mood? = null,
    val statusMessage: String = "",
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** First-run guide finished (existing users count as done). */
    val onboardingDone: Boolean = true,
    /** Offer to share a crash report on the next launch. */
    val crashReports: Boolean = true,
)

const val MAX_STATUS_MESSAGE = 40

/** One-shot snackbar message; the id makes identical texts re-trigger. [action] adds a button (e.g. Undo). */
data class UiMessage(val id: Long, val text: String, val action: String? = null)

/**
 * The character's face. The first four follow how today (and yesterday) went; all of them can
 * also be picked as the profile status.
 */
enum class Mood(val emoji: String) {
    HAPPY("😄"), NORMAL("🙂"), SLEEPY("😴"), SAD("😢"),
    EXCITED("🤩"), CALM("😌"), LOVE("🥰"), ANGRY("😤"), SICK("🤒"),
}

/** Light / dark appearance; SYSTEM follows the phone. */
enum class ThemeMode { SYSTEM, DARK, LIGHT }

object Moods {
    /** Proud when today is done, gloomy right after a missed day, sleepy before starting. Shared by the app and the widget. */
    fun of(byDate: Map<String, List<Quest>>, shieldedDates: Set<String>, today: LocalDate): Mood {
        val active = byDate[today.toString()].orEmpty().activeOn(today)
        val done = active.count { it.done }
        val yesterday = today.minusDays(1)
        val yList = byDate[yesterday.toString()].orEmpty()
        val missedYesterday = yList.activeOn(yesterday).isNotEmpty() && !yList.completeOn(yesterday) &&
            yesterday.toString() !in shieldedDates
        return when {
            active.isNotEmpty() && done == active.size -> Mood.HAPPY
            done == 0 && missedYesterday -> Mood.SAD
            done == 0 && active.isNotEmpty() -> Mood.SLEEPY
            else -> Mood.NORMAL
        }
    }
}

enum class FriendSource { DEMO, CONTACT, MANUAL }

data class Friend(
    val id: String,
    val name: String,
    val nameEn: String,
    val handle: String,
    val outfit: Outfit,
    val online: Boolean,
    val level: Int,
    val source: FriendSource = FriendSource.DEMO,
    val blocked: Boolean = false,
    val contactId: Long? = null,
)

const val MAX_FRIENDS = 50

/** [textEn] is set for scripted messages that exist in both languages. */
data class ChatMessage(
    val id: Long,
    val fromMe: Boolean,
    val text: String,
    val time: LocalTime,
    val liked: Boolean = false,
    val textEn: String? = null,
    /** For my messages: the friend has seen it (drives read receipts). */
    val read: Boolean = false,
)

object Outfits {
    val presets = listOf(
        Outfit("default", hair = 0xFF3B2A20.toInt(), shirt = 0xFF6C4BF4.toInt(), accent = 0xFFFFD34E.toInt(), pants = 0xFF2D3A5C.toInt(), shoes = 0xFF1E1E28.toInt()),
        Outfit(
            "street", hair = 0xFF111111.toInt(), shirt = 0xFFF2F2F2.toInt(), accent = 0xFFE94B5A.toInt(), pants = 0xFF2B2B2B.toInt(), shoes = 0xFFF2F2F2.toInt(),
            stripe = 0xFF1B1B1B.toInt(), striped = true,
        ),
        Outfit(
            "ocean", hair = 0xFFD9A441.toInt(), shirt = 0xFF2E9BDA.toInt(), accent = 0xFFB6F0FF.toInt(), pants = 0xFFE8DCC2.toInt(), shoes = 0xFF35506B.toInt(),
            shortSleeve = true, shorts = true,
        ),
        Outfit("forest", hair = 0xFF5A3B22.toInt(), shirt = 0xFF3F8F4E.toInt(), accent = 0xFFD8C27A.toInt(), pants = 0xFF6B4F35.toInt(), shoes = 0xFF3A2A1C.toInt()),
        Outfit(
            "sunset", hair = 0xFFB4462E.toInt(), shirt = 0xFFFF8C42.toInt(), accent = 0xFFFFE066.toInt(), pants = 0xFF5B2A4E.toInt(), shoes = 0xFF2A1A26.toInt(),
            longHair = true, shortSleeve = true,
        ),
        Outfit(
            "neon", hair = 0xFFFF4FD8.toInt(), shirt = 0xFF1B1B2F.toInt(), accent = 0xFF3DFFB4.toInt(), pants = 0xFF3D2EFF.toInt(), shoes = 0xFF3DFFB4.toInt(),
            stripe = 0xFF3DFFB4.toInt(), striped = true, longHair = true,
        ),
    )

    const val PHOTO_PREFIX = "photo:"

    /** Names saved by v1.1 (Korean text) -> language-neutral keys. */
    private val legacyNames = mapOf("기본" to "default", "스트릿" to "street", "오션" to "ocean", "포레스트" to "forest", "선셋" to "sunset", "네온" to "neon")
    private val legacyPhoto = Regex("^포토 스타일 #(\\d+)$")

    fun normalizeName(name: String): String =
        legacyNames[name] ?: legacyPhoto.find(name)?.let { PHOTO_PREFIX + it.groupValues[1] } ?: name

    /** Palette offered in the manual fine-tune editor. */
    val swatches = listOf(
        0xFF111111, 0xFF3B2A20, 0xFF7A4E2D, 0xFFD9A441, 0xFFF2F2F2, 0xFF9E9E9E,
        0xFFE94B5A, 0xFFFF8C42, 0xFFFFD34E, 0xFF3F8F4E, 0xFF2E9BDA, 0xFF2D3A5C,
        0xFF6C4BF4, 0xFFFF4FD8, 0xFFE8DCC2, 0xFF5B2A4E,
    ).map { it.toInt() }

    val skinTones = listOf(0xFFFFEDE0, 0xFFFBE0CC, 0xFFF5C9A3, 0xFFE8B98F, 0xFFE0A985, 0xFFC68A5E, 0xFF8D5A3B, 0xFF5C3A24).map { it.toInt() }

    /** Eye and lip colours offered in the character creator. */
    val eyeColors = listOf(0xFF6B4FD0, 0xFF3E6FB8, 0xFF2E9BDA, 0xFF3F8F4E, 0xFF7BD389, 0xFF8A5A2B, 0xFF3B2A20, 0xFFD9A441, 0xFFE94B5A, 0xFF9E9EA8).map { it.toInt() }
    val lipColors = listOf(0xFFE8707E, 0xFFD94F6A, 0xFFFF8FA3, 0xFFC97A7A, 0xFFB8505A, 0xFFE0A985).map { it.toInt() }

    /** Extra hair colours offered with the hair slot (grey and white for older characters). */
    val hairColors = listOf(0xFF111111, 0xFF3B2A20, 0xFF7A4E2D, 0xFFB4462E, 0xFFD9A441, 0xFFF2D58A, 0xFF9E9E9E, 0xFFD8D8E0, 0xFFF4F4F8).map { it.toInt() }
}

object DummyData {
    const val DEFAULT_NICKNAME_KO = "모험가"
    const val DEFAULT_NICKNAME_EN = "Adventurer"

    /** Built-in quests: (emoji, Korean title, English title, xp). */
    val defaultQuestTemplates = listOf(
        listOf("🏃", "아침 조깅 20분", "Morning jog 20 min", "30"),
        listOf("📘", "Kotlin 공부 1시간", "Study Kotlin 1 hour", "30"),
        listOf("💼", "업무 집중 2시간", "Deep work 2 hours", "30"),
        listOf("💧", "물 8잔 마시기", "Drink 8 glasses of water", "20"),
        listOf("📖", "독서 20분", "Read 20 min", "25"),
    )

    fun defaultQuests(language: AppLanguage, newId: () -> String) = defaultQuestTemplates.map { t ->
        Quest(newId(), if (language == AppLanguage.KOREAN) t[1] else t[2], t[0], t[3].toInt())
    }

    /** Built-in title in the other language, or null for user-written titles. */
    fun translateDefaultTitle(title: String, to: AppLanguage): String? = Templates.allQuests.firstNotNullOfOrNull { t ->
        when {
            to == AppLanguage.ENGLISH && title == t.ko -> t.en
            to == AppLanguage.KOREAN && title == t.en -> t.ko
            else -> null
        }
    }

    val friends = listOf(
        Friend("f1", "민지", "Minji", "minji.daily", demoLook(Outfits.presets[2], Looks.female[1], 0xFF5A3A2C), online = true, level = 12),
        Friend("f2", "준호", "Junho", "junho_dev", demoLook(Outfits.presets[3], Looks.male[0], 0xFF2A2430), online = true, level = 8),
        Friend("f6", "Alex", "Alex", "alex.levelup", demoLook(Outfits.presets[1], Looks.male[3], 0xFF8A6A50), online = true, level = 9),
        Friend("f3", "서연", "Seoyeon", "seoyeon.run", demoLook(Outfits.presets[4], Looks.female[2], 0xFF8A3A2E), online = false, level = 15),
        Friend("f4", "도윤", "Doyoon", "doyoon.gg", demoLook(Outfits.presets[5], Looks.male[2], 0xFF1E1A22), online = true, level = 5),
        Friend("f5", "하은", "Haeun", "haeun_reads", demoLook(Outfits.presets[0], Looks.female[5], 0xFF3B2A20), online = false, level = 10),
    )

    /** A scripted friend: a face preset on an outfit preset, with natural hair colour. */
    private fun demoLook(clothes: Outfit, look: Look, hair: Long) = look.copy(hair = hair).applyTo(clothes)

    fun demoFriend(id: String) = friends.firstOrNull { it.id == id }

    /** Scripted first messages: (Korean, English). */
    val openingMessages = mapOf(
        "f1" to listOf("오늘 조깅 퀘스트 했어? 🏃‍♀️" to "Did you do your jogging quest today? 🏃‍♀️", "나 벌써 3개 끝냄 ㅎㅎ" to "I already finished 3 lol"),
        "f2" to listOf("Compose 공부 어디까지 했어?" to "How far did you get with Compose?"),
        "f6" to listOf("Hey! Did you finish your quests today? 💪" to "Hey! Did you finish your quests today? 💪", "한국어로 말해도 돼요 ㅎㅎ" to "You can text me in Korean too!"),
        "f3" to listOf("스트릭 15일 달성!! 🔥" to "Hit a 15-day streak!! 🔥"),
        "f4" to listOf("새 옷 맞췄어? 👕" to "Got a new outfit? 👕"),
        "f5" to listOf("이번 주 책 추천해줄게 📚" to "Let me recommend a book this week 📚"),
    )
}
