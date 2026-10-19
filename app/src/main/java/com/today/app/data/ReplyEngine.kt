package com.today.app.data

/**
 * Simulated friend replies in Korean or English.
 * Language = the setting, or (AUTO) whatever the user just typed in.
 */
object ReplyEngine {

    enum class Lang { KO, EN }

    private val hangul = Regex("[\\u1100-\\u11FF\\u3130-\\u318F\\uAC00-\\uD7A3]")
    private val latin = Regex("[A-Za-z]")

    /** Hangul anywhere -> Korean; Latin letters only -> English; emoji/numbers only -> [fallback]. */
    fun detect(text: String, fallback: Lang = Lang.KO): Lang = when {
        hangul.containsMatchIn(text) -> Lang.KO
        latin.containsMatchIn(text) -> Lang.EN
        else -> fallback
    }

    fun resolve(setting: ReplyLanguage, text: String, fallback: Lang): Lang = when (setting) {
        ReplyLanguage.KOREAN -> Lang.KO
        ReplyLanguage.ENGLISH -> Lang.EN
        ReplyLanguage.AUTO -> detect(text, fallback)
    }

    private class Rule(val keywords: List<String>, val replies: List<String>)

    private val koRules = listOf(
        Rule(listOf("안녕", "하이", "ㅎㅇ", "반가"), listOf("안녕! 오늘 퀘스트 몇 개 했어? 😊", "하이~ 오늘도 레벨업 하는 중?")),
        Rule(listOf("퀘스트", "할일", "할 일", "루틴"), listOf("난 오늘 퀘스트 3개 남았어 😂", "퀘스트 다 깨면 스트릭 올라가잖아, 같이 하자!")),
        Rule(listOf("레벨", "렙", "lv"), listOf("나 지금 LV {friendLevel}! 너는 LV {myLevel}이네 👀", "LV {myLevel}? 금방 따라잡겠다 ㅋㅋ")),
        Rule(listOf("스트릭", "연속", "🔥"), listOf("스트릭 끊기면 너무 아까워 🔥", "나도 스트릭 이어가는 중!")),
        Rule(listOf("운동", "조깅", "달리", "헬스", "러닝"), listOf("오 운동 대박 💪 나도 이따 뛰러 갈 거야", "내일 아침에 같이 조깅 ㄱ?")),
        Rule(listOf("공부", "코틀린", "kotlin", "코딩", "개발"), listOf("Compose 진짜 재밌지 않아? 📘", "공부 퀘스트 +40 XP 꿀이야 ㅋㅋ")),
        Rule(listOf("옷", "스타일", "코디", "옷장"), listOf("옷장에서 사진 찍으면 도트 옷 만들어 주는 거 해봤어? 👕", "오 새 스타일 궁금하다!")),
        Rule(listOf("고마", "땡큐", "감사"), listOf("별말씀을 😆", "언제든지!")),
        Rule(listOf("잘자", "굿밤", "자러"), listOf("잘 자! 내일도 퀘스트 화이팅 🌙", "굿밤~ 내일 스트릭 잊지 마!")),
    )
    private val koDefault = listOf("오 대박 👍", "ㅋㅋㅋ 인정", "나도 오늘 퀘스트 다 깰 거야!", "같이 스트릭 이어가자 🔥", "좋다 좋다 ✨", "화이팅 💪", "헐 진짜?")
    private val koQuestion = listOf("음… 좋은 질문인데? 🤔", "글쎄, 너는 어떻게 생각해?", "아마 그럴걸? ㅎㅎ")

    private val enRules = listOf(
        Rule(listOf("hi", "hello", "hey", "yo", "sup"), listOf("Hey! How many quests did you clear today? 😊", "Hi there! Leveling up today?")),
        Rule(listOf("quest", "todo", "to-do", "routine", "task"), listOf("I still have 3 quests left 😂", "Clear them all and your streak goes up — let's do it!")),
        Rule(listOf("level", "lv"), listOf("I'm LV {friendLevel}! You're LV {myLevel} 👀", "LV {myLevel}? You'll catch up in no time.")),
        Rule(listOf("streak", "🔥"), listOf("Don't break the streak! 🔥", "I'm keeping my streak alive too!")),
        Rule(listOf("run", "jog", "gym", "workout", "exercise"), listOf("Nice workout 💪 I'm going for a run later.", "Morning jog together tomorrow?")),
        Rule(listOf("study", "kotlin", "code", "coding", "compose"), listOf("Compose is so fun, right? 📘", "Study quests are +40 XP, easy win 😎")),
        Rule(listOf("outfit", "style", "clothes", "wardrobe"), listOf("Have you tried the photo-to-pixel outfit thing? 👕", "Ooh, show me your new style!")),
        Rule(listOf("thanks", "thank you", "thx"), listOf("Anytime! 😆", "No problem!")),
        Rule(listOf("good night", "gn", "sleep"), listOf("Good night! Crush your quests tomorrow 🌙", "Sleep well — don't forget your streak!")),
    )
    private val enDefault = listOf("Nice 👍", "lol true", "I'm finishing all my quests today too!", "Let's keep the streak going 🔥", "Love it ✨", "You got this 💪", "No way, really?")
    private val enQuestion = listOf("Hmm, good question 🤔", "Not sure — what do you think?", "Probably, haha")

    fun reply(text: String, lang: Lang, friendLevel: Int, myLevel: Int): String {
        val lower = text.lowercase()
        val rules = if (lang == Lang.KO) koRules else enRules
        val rule = rules.firstOrNull { r -> r.keywords.any { k -> matches(lower, k, lang) } }
        val pool = rule?.replies
            ?: if (lower.trimEnd().endsWith("?")) (if (lang == Lang.KO) koQuestion else enQuestion)
            else (if (lang == Lang.KO) koDefault else enDefault)
        return pool.random()
            .replace("{friendLevel}", friendLevel.toString())
            .replace("{myLevel}", myLevel.toString())
    }

    /** English short keywords must match whole words ("hi" shouldn't match "this"). */
    private fun matches(text: String, keyword: String, lang: Lang): Boolean =
        if (lang == Lang.EN && keyword.all { it.isLetter() } && keyword.length <= 4) {
            Regex("\\b${Regex.escape(keyword)}\\b").containsMatchIn(text)
        } else {
            text.contains(keyword)
        }
}
