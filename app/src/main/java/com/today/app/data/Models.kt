package com.today.app.data

/** A single daily routine turned into a quest. */
data class Quest(
    val id: String,
    val title: String,
    val emoji: String,
    val xp: Int,
    val done: Boolean = false,
)

const val MAX_QUESTS_PER_DAY = 10
const val MAX_QUEST_TITLE = 30
const val MIN_QUEST_XP = 5
const val MAX_QUEST_XP = 100

const val DEFAULT_SKIN = 0xFFF5C9A3.toInt()

/**
 * Everything needed to paint the pixel avatar: colors (ARGB ints) + garment shape flags.
 * The photo analyzer fills all of these from a picture.
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
)

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
enum class DayStatus { EMPTY, PLANNED, PARTIAL, COMPLETE, MISSED }

/** Quests copied from one day, waiting to be pasted onto another. */
data class QuestClipboard(val sourceDate: java.time.LocalDate, val quests: List<Quest>)

enum class ReplyLanguage(val label: String) { AUTO("자동 감지"), KOREAN("한국어"), ENGLISH("English") }

data class AppSettings(
    val nickname: String = "모험가",
    val replyLanguage: ReplyLanguage = ReplyLanguage.AUTO,
    val autoCarryOver: Boolean = true,
)

/** One-shot snackbar message; the id makes identical texts re-trigger. */
data class UiMessage(val id: Long, val text: String)

data class Friend(
    val id: String,
    val name: String,
    val handle: String,
    val outfit: Outfit,
    val online: Boolean,
    val level: Int,
)

data class ChatMessage(
    val id: Long,
    val fromMe: Boolean,
    val text: String,
    val time: String,
    val liked: Boolean = false,
)

object Outfits {
    val presets = listOf(
        Outfit("기본", hair = 0xFF3B2A20.toInt(), shirt = 0xFF6C4BF4.toInt(), accent = 0xFFFFD34E.toInt(), pants = 0xFF2D3A5C.toInt(), shoes = 0xFF1E1E28.toInt()),
        Outfit(
            "스트릿", hair = 0xFF111111.toInt(), shirt = 0xFFF2F2F2.toInt(), accent = 0xFFE94B5A.toInt(), pants = 0xFF2B2B2B.toInt(), shoes = 0xFFF2F2F2.toInt(),
            stripe = 0xFF1B1B1B.toInt(), striped = true,
        ),
        Outfit(
            "오션", hair = 0xFFD9A441.toInt(), shirt = 0xFF2E9BDA.toInt(), accent = 0xFFB6F0FF.toInt(), pants = 0xFFE8DCC2.toInt(), shoes = 0xFF35506B.toInt(),
            shortSleeve = true, shorts = true,
        ),
        Outfit("포레스트", hair = 0xFF5A3B22.toInt(), shirt = 0xFF3F8F4E.toInt(), accent = 0xFFD8C27A.toInt(), pants = 0xFF6B4F35.toInt(), shoes = 0xFF3A2A1C.toInt()),
        Outfit(
            "선셋", hair = 0xFFB4462E.toInt(), shirt = 0xFFFF8C42.toInt(), accent = 0xFFFFE066.toInt(), pants = 0xFF5B2A4E.toInt(), shoes = 0xFF2A1A26.toInt(),
            longHair = true, shortSleeve = true,
        ),
        Outfit(
            "네온", hair = 0xFFFF4FD8.toInt(), shirt = 0xFF1B1B2F.toInt(), accent = 0xFF3DFFB4.toInt(), pants = 0xFF3D2EFF.toInt(), shoes = 0xFF3DFFB4.toInt(),
            stripe = 0xFF3DFFB4.toInt(), striped = true, longHair = true,
        ),
    )

    /** Palette offered in the manual fine-tune editor. */
    val swatches = listOf(
        0xFF111111, 0xFF3B2A20, 0xFF7A4E2D, 0xFFD9A441, 0xFFF2F2F2, 0xFF9E9E9E,
        0xFFE94B5A, 0xFFFF8C42, 0xFFFFD34E, 0xFF3F8F4E, 0xFF2E9BDA, 0xFF2D3A5C,
        0xFF6C4BF4, 0xFFFF4FD8, 0xFFE8DCC2, 0xFF5B2A4E,
    ).map { it.toInt() }

    val skinTones = listOf(0xFFFBE0CC, 0xFFF5C9A3, 0xFFE0A985, 0xFFC68A5E, 0xFF8D5A3B, 0xFF5C3A24).map { it.toInt() }
}

object DummyData {
    val defaultQuests = listOf(
        Quest("q1", "아침 조깅 20분", "🏃", 30),
        Quest("q2", "Kotlin 공부 1시간", "📘", 40),
        Quest("q3", "업무 집중 2시간", "💼", 40),
        Quest("q4", "물 8잔 마시기", "💧", 20),
        Quest("q5", "독서 20분", "📖", 25),
    )

    val friends = listOf(
        Friend("f1", "민지", "minji.daily", Outfits.presets[2], online = true, level = 12),
        Friend("f2", "준호", "junho_dev", Outfits.presets[3], online = true, level = 8),
        Friend("f6", "Alex", "alex.levelup", Outfits.presets[1], online = true, level = 9),
        Friend("f3", "서연", "seoyeon.run", Outfits.presets[4], online = false, level = 15),
        Friend("f4", "도윤", "doyoon.gg", Outfits.presets[5], online = true, level = 5),
        Friend("f5", "하은", "haeun_reads", Outfits.presets[0], online = false, level = 10),
    )

    val openingMessages = mapOf(
        "f1" to listOf("오늘 조깅 퀘스트 했어? 🏃‍♀️", "나 벌써 3개 끝냄 ㅎㅎ"),
        "f2" to listOf("Compose 공부 어디까지 했어?"),
        "f6" to listOf("Hey! Did you finish your quests today? 💪", "한국어로 말해도 돼요 ㅎㅎ"),
        "f3" to listOf("스트릭 15일 달성!! 🔥"),
        "f4" to listOf("새 옷 맞췄어? 👕"),
        "f5" to listOf("이번 주 책 추천해줄게 📚"),
    )
}
