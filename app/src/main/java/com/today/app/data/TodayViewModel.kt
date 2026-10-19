package com.today.app.data

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class TodayViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = TodayStorage(app)
    private val analyzer = StyleAnalyzer()

    // ---------------- Character ----------------
    var character by mutableStateOf(CharacterState())
        private set

    /** Incremented every time a quest is completed -> triggers the Lottie confetti. */
    var celebrationTick by mutableIntStateOf(0)
        private set

    /** Non-null while a "LEVEL UP" dialog should be shown. */
    var levelUpEvent by mutableStateOf<Int?>(null)
        private set

    // ---------------- Quests & calendar ----------------
    /** ISO date -> quests. Lists are immutable; every change replaces the list. */
    private val questsByDate = mutableStateMapOf<String, List<Quest>>()

    var todayDate by mutableStateOf(LocalDate.now())
        private set
    var selectedDate by mutableStateOf(LocalDate.now())
        private set
    var clipboard by mutableStateOf<QuestClipboard?>(null)
        private set

    // ---------------- Settings / messages ----------------
    var settings by mutableStateOf(AppSettings())
        private set
    var message by mutableStateOf<UiMessage?>(null)
        private set
    private var messageCounter = 0L

    // ---------------- Outfit recognition ----------------
    var isAnalyzingStyle by mutableStateOf(false)
        private set
    var analyzeStep by mutableStateOf("")
        private set
    var stylePhoto by mutableStateOf<Bitmap?>(null)
        private set
    var styleResult by mutableStateOf<StyleResult?>(null)
        private set
    private var photoStyleCount = 0

    // ---------------- Community ----------------
    val friends = DummyData.friends
    val chats = mutableStateMapOf<String, SnapshotStateList<ChatMessage>>()
    val typing = mutableStateMapOf<String, Boolean>()
    val unread = mutableStateMapOf<String, Boolean>()
    var openChatId by mutableStateOf<String?>(null)
        private set
    private var chatMessageId = 0L
    private var lastChatLang = ReplyEngine.Lang.KO

    init {
        loadFromStorage()
        resetChats()
    }

    private fun loadFromStorage() {
        val saved = storage.load()
        character = saved.character
        settings = saved.settings
        questsByDate.clear()
        questsByDate.putAll(saved.questsByDate)
        todayDate = LocalDate.now()
        selectedDate = todayDate
        clipboard = null
        ensureTodayQuests()
        persist()
    }

    // ======================= Calendar =======================

    /** Call on resume: handles the app being left open across midnight. */
    fun refreshDay() {
        val now = LocalDate.now()
        if (now == todayDate) return
        val followToday = selectedDate == todayDate
        todayDate = now
        if (followToday) selectedDate = now
        ensureTodayQuests()
        persist()
    }

    fun selectDate(date: LocalDate) {
        selectedDate = date
    }

    fun questsFor(date: LocalDate): List<Quest> = questsByDate[date.toString()].orEmpty()

    fun isEditable(date: LocalDate) = !date.isBefore(todayDate)
    fun isCheckable(date: LocalDate) = date == todayDate

    fun dayStatus(date: LocalDate): DayStatus {
        val quests = questsFor(date)
        val done = quests.count { it.done }
        return when {
            quests.isEmpty() -> DayStatus.EMPTY
            done == quests.size -> DayStatus.COMPLETE
            date.isAfter(todayDate) -> DayStatus.PLANNED
            date.isBefore(todayDate) -> DayStatus.MISSED
            done > 0 -> DayStatus.PARTIAL
            else -> DayStatus.PLANNED
        }
    }

    /** Consecutive fully-completed days ending today (or yesterday if today isn't finished yet). */
    val streak: Int
        get() {
            fun complete(d: LocalDate) = questsFor(d).let { it.isNotEmpty() && it.all { q -> q.done } }
            var day = if (complete(todayDate)) todayDate else todayDate.minusDays(1)
            var count = 0
            while (complete(day)) {
                count++
                day = day.minusDays(1)
            }
            return count
        }

    val totalCompleted: Int get() = questsByDate.values.sumOf { list -> list.count { it.done } }
    val recordedDays: Int get() = questsByDate.count { (_, list) -> list.isNotEmpty() }

    /** New day without quests: carry over the most recent day's quests (unchecked), or defaults on first run. */
    private fun ensureTodayQuests() {
        val key = todayDate.toString()
        if (questsByDate.containsKey(key)) return
        if (questsByDate.isEmpty()) {
            questsByDate[key] = DummyData.defaultQuests.map { it.copy(id = newId()) }
            return
        }
        if (!settings.autoCarryOver) return
        val previous = questsByDate.keys.filter { it < key }.sortedDescending()
            .firstNotNullOfOrNull { k -> questsByDate[k]?.takeIf { it.isNotEmpty() } }
            ?: return
        questsByDate[key] = previous.take(MAX_QUESTS_PER_DAY).map { it.copy(id = newId(), done = false) }
    }

    // ======================= Quest CRUD =======================

    fun toggleQuest(date: LocalDate, id: String) {
        if (!isCheckable(date)) {
            showMessage(if (date.isBefore(todayDate)) "지난 날의 기록은 바꿀 수 없어요" else "예정된 퀘스트는 그날 체크할 수 있어요")
            return
        }
        val list = questsFor(date)
        val quest = list.firstOrNull { it.id == id } ?: return
        val nowDone = !quest.done
        val streakBefore = streak
        setQuests(date, list.map { if (it.id == id) it.copy(done = nowDone) else it })
        if (nowDone) {
            addXp(quest.xp)
            celebrationTick++
            if (streak > streakBefore) showMessage("🔥 오늘의 퀘스트 완료! 스트릭 ${streak}일")
        } else {
            addXp(-quest.xp)
        }
        persist()
    }

    /** @return null on success, otherwise the reason it failed. */
    fun addQuest(date: LocalDate, title: String, emoji: String, xp: Int): String? {
        if (!isEditable(date)) return "지난 날에는 퀘스트를 추가할 수 없어요"
        val list = questsFor(date)
        if (list.size >= MAX_QUESTS_PER_DAY) return "하루에 최대 ${MAX_QUESTS_PER_DAY}개까지 추가할 수 있어요"
        val cleanTitle = title.trim().take(MAX_QUEST_TITLE)
        if (cleanTitle.isEmpty()) return "할 일을 입력해 주세요"
        setQuests(date, list + Quest(newId(), cleanTitle, emoji.ifBlank { "⭐" }, xp.coerceIn(MIN_QUEST_XP, MAX_QUEST_XP)))
        persist()
        return null
    }

    fun updateQuest(date: LocalDate, id: String, title: String, emoji: String, xp: Int): String? {
        if (!isEditable(date)) return "지난 날의 퀘스트는 수정할 수 없어요"
        val list = questsFor(date)
        val old = list.firstOrNull { it.id == id } ?: return "퀘스트를 찾을 수 없어요"
        val cleanTitle = title.trim().take(MAX_QUEST_TITLE)
        if (cleanTitle.isEmpty()) return "할 일을 입력해 주세요"
        val newXp = xp.coerceIn(MIN_QUEST_XP, MAX_QUEST_XP)
        setQuests(date, list.map { if (it.id == id) it.copy(title = cleanTitle, emoji = emoji.ifBlank { "⭐" }, xp = newXp) else it })
        // Already completed today: keep total XP consistent with the new reward.
        if (old.done && newXp != old.xp) addXp(newXp - old.xp)
        persist()
        return null
    }

    fun deleteQuest(date: LocalDate, id: String) {
        if (!isEditable(date)) {
            showMessage("지난 날의 퀘스트는 삭제할 수 없어요")
            return
        }
        val list = questsFor(date)
        val quest = list.firstOrNull { it.id == id } ?: return
        setQuests(date, list.filterNot { it.id == id })
        if (quest.done) addXp(-quest.xp) // no XP farming by check -> delete -> re-add
        persist()
        showMessage("'${quest.title}' 삭제됨")
    }

    // ======================= Copy / paste =======================

    fun copyDay(date: LocalDate) {
        val quests = questsFor(date)
        if (quests.isEmpty()) {
            showMessage("복사할 퀘스트가 없어요")
            return
        }
        clipboard = QuestClipboard(date, quests)
        showMessage("${formatDate(date)} 퀘스트 ${quests.size}개 복사됨 · 붙여넣을 날짜를 고르세요")
    }

    fun clearClipboard() {
        clipboard = null
    }

    fun pasteTo(date: LocalDate) {
        val clip = clipboard ?: return
        if (!isEditable(date)) {
            showMessage("지난 날에는 붙여넣을 수 없어요")
            return
        }
        val existing = questsFor(date)
        val existingTitles = existing.map { it.title }.toSet()
        val candidates = clip.quests.filter { it.title !in existingTitles }
        val duplicates = clip.quests.size - candidates.size
        val slots = MAX_QUESTS_PER_DAY - existing.size
        val toAdd = candidates.take(slots.coerceAtLeast(0))
        val overLimit = candidates.size - toAdd.size

        if (toAdd.isNotEmpty()) {
            setQuests(date, existing + toAdd.map { it.copy(id = newId(), done = false) })
            persist()
        }
        val parts = buildList {
            if (toAdd.isNotEmpty()) add("${toAdd.size}개 붙여넣음")
            if (duplicates > 0) add("중복 ${duplicates}개 제외")
            if (overLimit > 0) add("하루 최대 ${MAX_QUESTS_PER_DAY}개 제한으로 ${overLimit}개 제외")
        }
        showMessage(if (toAdd.isEmpty() && parts.isEmpty()) "붙여넣을 퀘스트가 없어요" else parts.joinToString(" · "))
    }

    private fun setQuests(date: LocalDate, quests: List<Quest>) {
        questsByDate[date.toString()] = quests
    }

    // ======================= XP =======================

    fun consumeLevelUp() {
        levelUpEvent = null
    }

    private fun addXp(delta: Int) {
        var level = character.level
        var xp = character.xp + delta
        while (xp >= CharacterState.xpNeededFor(level)) {
            xp -= CharacterState.xpNeededFor(level)
            level++
            levelUpEvent = level
        }
        while (xp < 0 && level > 1) {
            level--
            xp += CharacterState.xpNeededFor(level)
        }
        character = character.copy(level = level, xp = xp.coerceAtLeast(0))
    }

    // ======================= Outfit =======================

    fun applyOutfit(outfit: Outfit) {
        character = character.copy(outfit = outfit)
        persist()
    }

    /** Manual fine-tuning from the wardrobe editor. */
    fun editOutfit(transform: (Outfit) -> Outfit) {
        applyOutfit(transform(character.outfit))
    }

    fun analyzePhoto(uri: Uri) {
        if (isAnalyzingStyle) return
        viewModelScope.launch {
            isAnalyzingStyle = true
            analyzeStep = "사진 불러오는 중…"
            var ticker: kotlinx.coroutines.Job? = null
            try {
                val bitmap = withContext(Dispatchers.IO) { PhotoDecoder.decode(getApplication(), uri) }
                if (bitmap == null) {
                    showMessage("사진을 불러오지 못했어요")
                    return@launch
                }
                stylePhoto = bitmap
                styleResult = null

                val started = System.currentTimeMillis()
                ticker = launch {
                    val steps = listOf("사람 찾는 중…", "머리·상의·하의 영역 나누는 중…", "옷 색감 추출 중…", "도트 의상 생성 중…")
                    var i = 0
                    while (true) {
                        analyzeStep = steps[i % steps.size]
                        i++
                        delay(550)
                    }
                }
                val result = analyzer.analyze(bitmap, character.outfit, "포토 스타일 #${photoStyleCount + 1}")
                // Keep the "AI" animation visible for a moment even on fast devices.
                val elapsed = System.currentTimeMillis() - started
                if (elapsed < 1600) delay(1600 - elapsed)

                photoStyleCount++
                styleResult = result
                applyOutfit(result.outfit)
                showMessage(result.summary)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                showMessage("스타일 분석 중 문제가 생겼어요: ${e.message ?: "알 수 없는 오류"}")
            } finally {
                ticker?.cancel()
                isAnalyzingStyle = false
            }
        }
    }

    fun clearStylePhoto() {
        stylePhoto = null
        styleResult = null
    }

    // ======================= Settings =======================

    fun updateNickname(name: String) {
        val clean = name.trim().take(12)
        if (clean.isEmpty()) return
        settings = settings.copy(nickname = clean)
        persist()
    }

    fun updateReplyLanguage(lang: ReplyLanguage) {
        settings = settings.copy(replyLanguage = lang)
        persist()
    }

    fun updateAutoCarryOver(enabled: Boolean) {
        settings = settings.copy(autoCarryOver = enabled)
        persist()
    }

    fun resetAll() {
        storage.clear()
        clearStylePhoto()
        photoStyleCount = 0
        loadFromStorage()
        resetChats()
        showMessage("모든 데이터가 초기화되었어요")
    }

    fun showMessage(text: String) {
        message = UiMessage(++messageCounter, text)
    }

    fun consumeMessage(id: Long) {
        if (message?.id == id) message = null
    }

    // ======================= Community =======================

    private fun resetChats() {
        openChatId = null
        chats.clear()
        typing.clear()
        unread.clear()
        friends.forEach { f ->
            chats[f.id] = mutableStateListOf<ChatMessage>().apply {
                DummyData.openingMessages[f.id].orEmpty().forEachIndexed { i, text ->
                    add(ChatMessage(chatMessageId++, false, text, "오전 9:0$i"))
                }
            }
            unread[f.id] = true
        }
    }

    fun openChat(friendId: String?) {
        openChatId = friendId
        if (friendId != null) unread[friendId] = false
    }

    fun sendMessage(friendId: String, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val list = chats[friendId] ?: return
        val friend = friends.firstOrNull { it.id == friendId } ?: return
        list += ChatMessage(chatMessageId++, true, clean, nowTime())

        val lang = ReplyEngine.resolve(settings.replyLanguage, clean, lastChatLang)
        lastChatLang = lang
        val reply = ReplyEngine.reply(clean, lang, friend.level, character.level)
        // Simulated friend reply: typing indicator, then an answer.
        viewModelScope.launch {
            delay(600)
            typing[friendId] = true
            delay(1000L + (0..1200).random())
            typing[friendId] = false
            chats[friendId]?.add(ChatMessage(chatMessageId++, false, reply, nowTime()))
            if (openChatId != friendId) unread[friendId] = true
        }
    }

    fun toggleLike(friendId: String, messageId: Long) {
        val list = chats[friendId] ?: return
        val i = list.indexOfFirst { it.id == messageId }
        if (i >= 0) list[i] = list[i].copy(liked = !list[i].liked)
    }

    // ======================= Helpers =======================

    private fun persist() {
        storage.save(SavedState(character, questsByDate.toMap(), settings))
    }

    override fun onCleared() {
        analyzer.close()
    }

    private fun newId() = UUID.randomUUID().toString()
    private fun nowTime(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN))

    companion object {
        fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN))
    }
}
