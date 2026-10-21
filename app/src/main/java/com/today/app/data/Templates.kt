package com.today.app.data

/** One quest inside a routine pack: Korean and English titles. */
data class TemplateQuest(val emoji: String, val ko: String, val en: String, val xp: Int) {
    fun title(language: AppLanguage) = if (language == AppLanguage.KOREAN) ko else en
}

/**
 * Ready-made routine packs, from kids to seniors. Picked in the first-run guide (as goals)
 * and from the quest list ("Templates"). Names and descriptions live in Strings.
 */
data class RoutineTemplate(val id: String, val emoji: String, val quests: List<TemplateQuest>)

object Templates {
    val all = listOf(
        RoutineTemplate(
            "morning", "🌅",
            listOf(
                TemplateQuest("⏰", "7시 전에 일어나기", "Wake up before 7", 20),
                TemplateQuest("💧", "일어나서 물 한 잔", "A glass of water after waking", 10),
                TemplateQuest("🧘", "아침 스트레칭 10분", "Morning stretch 10 min", 15),
                TemplateQuest("📝", "오늘 할 일 3가지 적기", "Write down 3 things for today", 15),
            ),
        ),
        RoutineTemplate(
            "workout", "💪",
            listOf(
                TemplateQuest("🏋️", "스쿼트 30개", "30 squats", 20),
                TemplateQuest("🧱", "플랭크 1분", "Plank 1 min", 20),
                TemplateQuest("💪", "푸시업 20개", "20 push-ups", 20),
                TemplateQuest("🏃", "30분 걷기 또는 달리기", "Walk or run 30 min", 30),
            ),
        ),
        RoutineTemplate(
            "study", "📚",
            listOf(
                TemplateQuest("📖", "개념 복습 1시간", "Review notes 1 hour", 30),
                TemplateQuest("✏️", "문제 30개 풀기", "Solve 30 problems", 25),
                TemplateQuest("📒", "오답 노트 정리", "Go over mistakes", 20),
                TemplateQuest("🔤", "단어 30개 외우기", "Learn 30 words", 20),
            ),
        ),
        RoutineTemplate(
            "health", "🥗",
            listOf(
                TemplateQuest("💧", "물 8잔 마시기", "Drink 8 glasses of water", 20),
                TemplateQuest("🥦", "채소 한 접시 먹기", "Eat a plate of vegetables", 15),
                TemplateQuest("🚶", "만 보 걷기", "Walk 10,000 steps", 30),
                TemplateQuest("💊", "영양제 챙겨 먹기", "Take vitamins", 10),
            ),
        ),
        RoutineTemplate(
            "sleep", "🌙",
            listOf(
                TemplateQuest("📵", "자기 1시간 전 폰 내려놓기", "No phone 1 hour before bed", 20),
                TemplateQuest("🍵", "따뜻한 차 한 잔", "A cup of warm tea", 10),
                TemplateQuest("📔", "감사 일기 쓰기", "Write a gratitude note", 15),
                TemplateQuest("🛏️", "12시 전에 잠들기", "Asleep before midnight", 25),
            ),
        ),
        RoutineTemplate(
            "mind", "🧘",
            listOf(
                TemplateQuest("🧘", "명상 10분", "Meditate 10 min", 20),
                TemplateQuest("🌳", "바깥 공기 쐬며 산책", "A walk outside", 20),
                TemplateQuest("🎵", "좋아하는 음악 듣기", "Listen to music you love", 10),
                TemplateQuest("😊", "오늘 좋았던 일 하나 떠올리기", "Recall one good thing today", 10),
            ),
        ),
        RoutineTemplate(
            "work", "💼",
            listOf(
                TemplateQuest("💼", "업무 집중 2시간", "Deep work 2 hours", 30),
                TemplateQuest("📥", "받은 메일 정리", "Clear the inbox", 15),
                TemplateQuest("🥪", "점심 먹고 10분 산책", "10-min walk after lunch", 15),
                TemplateQuest("📅", "내일 할 일 미리 정리", "Plan tomorrow", 15),
            ),
        ),
        RoutineTemplate(
            "kids", "🧒",
            listOf(
                TemplateQuest("🪥", "하루 세 번 양치하기", "Brush teeth 3 times", 15),
                TemplateQuest("📓", "숙제 끝내기", "Finish homework", 25),
                TemplateQuest("🧸", "내 방 정리하기", "Tidy my room", 15),
                TemplateQuest("📚", "책 15분 읽기", "Read for 15 min", 15),
            ),
        ),
        RoutineTemplate(
            "senior", "🌿",
            listOf(
                TemplateQuest("💊", "약 챙겨 먹기", "Take my medicine", 20),
                TemplateQuest("🩺", "혈압 재기", "Check blood pressure", 15),
                TemplateQuest("🚶", "동네 산책 30분", "30-min walk around the block", 25),
                TemplateQuest("📞", "가족에게 안부 전화", "Call the family", 15),
            ),
        ),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }

    /** Every built-in title (packs + first-run defaults), for following the language switch. */
    val allQuests: List<TemplateQuest> by lazy {
        all.flatMap { it.quests } + DummyData.defaultQuestTemplates.map { TemplateQuest(it[0], it[1], it[2], it[3].toInt()) }
    }

    /**
     * Quests from [packs] for a day that already has [existing]: duplicates (same title) are skipped
     * and the day limit is respected.
     */
    fun questsFrom(packs: List<RoutineTemplate>, existing: List<Quest>, language: AppLanguage, newId: () -> String): List<Quest> {
        val taken = existing.map { it.title }.toMutableSet()
        val room = (MAX_QUESTS_PER_DAY - existing.size).coerceAtLeast(0)
        return packs.flatMap { it.quests }
            .filter { taken.add(it.title(language)) }
            .take(room)
            .map { Quest(newId(), it.title(language), it.emoji, it.xp.coerceIn(MIN_QUEST_XP, MAX_QUEST_XP)) }
    }
}
