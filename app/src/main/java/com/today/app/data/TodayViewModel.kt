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
import com.today.app.i18n.Strings
import com.today.app.i18n.strings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
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

    /** Text in the currently selected app language. */
    val strings: Strings get() = settings.language.strings()

    // ---------------- Outfit recognition ----------------
    var isAnalyzingStyle by mutableStateOf(false)
        private set

    /** -1 = loading the photo, otherwise an index into Strings.analyzeSteps. */
    var analyzeStep by mutableIntStateOf(-1)
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

    /** @param keepLanguage used by "reset all" so the app doesn't jump back to the default language. */
    private fun loadFromStorage(keepLanguage: AppLanguage? = null) {
        val saved = storage.load()
        character = saved.character
        settings = if (keepLanguage == null) saved.settings
        else AppSettings(nickname = keepLanguage.strings().defaultNickname, language = keepLanguage)
        questsByDate.clear()
        questsByDate.putAll(saved.questsByDate)
        todayDate = LocalDate.now()
        selectedDate = todayDate
        clipboard = null
        lastChatLang = if (settings.language == AppLanguage.KOREAN) ReplyEngine.Lang.KO else ReplyEngine.Lang.EN
        enforceXpCap()
        ensureTodayQuests()
        persist()
    }

    /** Quests saved before the 30 XP cap: clamp them, keeping total XP consistent for today's done ones. */
    private fun enforceXpCap() {
        val today = todayDate.toString()
        questsByDate.keys.toList().forEach { date ->
            val list = questsByDate[date] ?: return@forEach
            if (list.none { it.xp > MAX_QUEST_XP }) return@forEach
            questsByDate[date] = list.map { q ->
                if (q.xp <= MAX_QUEST_XP) return@map q
                // Today's completed quests can still be unchecked, so give back the extra XP now.
                if (q.done && date == today) addXp(MAX_QUEST_XP - q.xp, announce = false)
                q.copy(xp = MAX_QUEST_XP)
            }
        }
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
            questsByDate[key] = DummyData.defaultQuests(settings.language, ::newId)
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
            showMessage(if (date.isBefore(todayDate)) strings.msgPastLocked else strings.msgFutureCheck)
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
            if (streak > streakBefore) showMessage(strings.msgStreak(streak))
        } else {
            addXp(-quest.xp)
        }
        persist()
    }

    /** @return null on success, otherwise the (localized) reason it failed. */
    fun addQuest(date: LocalDate, title: String, emoji: String, xp: Int): String? {
        if (!isEditable(date)) return strings.errAddPast
        val list = questsFor(date)
        if (list.size >= MAX_QUESTS_PER_DAY) return strings.errLimit(MAX_QUESTS_PER_DAY)
        val cleanTitle = title.trim().take(MAX_QUEST_TITLE)
        if (cleanTitle.isEmpty()) return strings.errEmptyTitle
        setQuests(date, list + Quest(newId(), cleanTitle, emoji.ifBlank { "⭐" }, xp.coerceIn(MIN_QUEST_XP, MAX_QUEST_XP)))
        persist()
        return null
    }

    fun updateQuest(date: LocalDate, id: String, title: String, emoji: String, xp: Int): String? {
        if (!isEditable(date)) return strings.errEditPast
        val list = questsFor(date)
        val old = list.firstOrNull { it.id == id } ?: return strings.errNotFound
        val cleanTitle = title.trim().take(MAX_QUEST_TITLE)
        if (cleanTitle.isEmpty()) return strings.errEmptyTitle
        val newXp = xp.coerceIn(MIN_QUEST_XP, MAX_QUEST_XP)
        setQuests(date, list.map { if (it.id == id) it.copy(title = cleanTitle, emoji = emoji.ifBlank { "⭐" }, xp = newXp) else it })
        // Already completed today: keep total XP consistent with the new reward.
        if (old.done && newXp != old.xp) addXp(newXp - old.xp)
        persist()
        return null
    }

    fun deleteQuest(date: LocalDate, id: String) {
        if (!isEditable(date)) {
            showMessage(strings.errDeletePast)
            return
        }
        val list = questsFor(date)
        val quest = list.firstOrNull { it.id == id } ?: return
        setQuests(date, list.filterNot { it.id == id })
        if (quest.done) addXp(-quest.xp) // no XP farming by check -> delete -> re-add
        persist()
        showMessage(strings.msgDeleted(quest.title))
    }

    // ======================= Copy / paste =======================

    fun copyDay(date: LocalDate) {
        val quests = questsFor(date)
        if (quests.isEmpty()) {
            showMessage(strings.msgNothingToCopy)
            return
        }
        clipboard = QuestClipboard(date, quests)
        showMessage(strings.msgCopied(date, quests.size))
    }

    fun clearClipboard() {
        clipboard = null
    }

    fun pasteTo(date: LocalDate) {
        val clip = clipboard ?: return
        if (!isEditable(date)) {
            showMessage(strings.errPastePast)
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
        showMessage(strings.pasteResult(toAdd.size, duplicates, overLimit, MAX_QUESTS_PER_DAY))
    }

    private fun setQuests(date: LocalDate, quests: List<Quest>) {
        questsByDate[date.toString()] = quests
    }

    // ======================= XP =======================

    fun consumeLevelUp() {
        levelUpEvent = null
    }

    private fun addXp(delta: Int, announce: Boolean = true) {
        var level = character.level
        var xp = character.xp + delta
        while (xp >= CharacterState.xpNeededFor(level)) {
            xp -= CharacterState.xpNeededFor(level)
            level++
            if (announce) levelUpEvent = level
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
            analyzeStep = -1
            var ticker: Job? = null
            try {
                val bitmap = withContext(Dispatchers.IO) { PhotoDecoder.decode(getApplication(), uri) }
                if (bitmap == null) {
                    showMessage(strings.msgPhotoLoadFail)
                    return@launch
                }
                stylePhoto = bitmap
                styleResult = null

                val started = System.currentTimeMillis()
                ticker = launch {
                    var i = 0
                    while (true) {
                        analyzeStep = i % strings.analyzeSteps.size
                        i++
                        delay(550)
                    }
                }
                val result = analyzer.analyze(bitmap, character.outfit, Outfits.PHOTO_PREFIX + (photoStyleCount + 1))
                // Keep the "AI" animation visible for a moment even on fast devices.
                val elapsed = System.currentTimeMillis() - started
                if (elapsed < 1600) delay(1600 - elapsed)

                photoStyleCount++
                styleResult = result
                applyOutfit(result.outfit)
                showMessage(strings.styleSummary(result))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                showMessage(strings.msgStyleError(e.message))
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
        val clean = name.trim().take(MAX_NICKNAME)
        if (clean.isEmpty()) return
        settings = settings.copy(nickname = clean)
        persist()
    }

    /**
     * Switches every UI string. Built-in content that the user never renamed (default quest titles,
     * default nickname) follows the language too; anything the user typed stays as written.
     */
    fun updateLanguage(language: AppLanguage) {
        if (language == settings.language) return
        val oldDefaultNick = settings.language.strings().defaultNickname
        settings = settings.copy(
            language = language,
            nickname = if (settings.nickname == oldDefaultNick) language.strings().defaultNickname else settings.nickname,
        )
        questsByDate.keys.toList().forEach { date ->
            val list = questsByDate[date] ?: return@forEach
            questsByDate[date] = list.map { q -> DummyData.translateDefaultTitle(q.title, language)?.let { q.copy(title = it) } ?: q }
        }
        clipboard = clipboard?.let { c ->
            c.copy(quests = c.quests.map { q -> DummyData.translateDefaultTitle(q.title, language)?.let { q.copy(title = it) } ?: q })
        }
        lastChatLang = if (language == AppLanguage.KOREAN) ReplyEngine.Lang.KO else ReplyEngine.Lang.EN
        persist()
    }

    fun updateAutoCarryOver(enabled: Boolean) {
        settings = settings.copy(autoCarryOver = enabled)
        persist()
    }

    fun resetAll() {
        val language = settings.language
        storage.clear()
        clearStylePhoto()
        photoStyleCount = 0
        loadFromStorage(keepLanguage = language)
        resetChats()
        showMessage(strings.msgReset)
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
                DummyData.openingMessages[f.id].orEmpty().forEachIndexed { i, (ko, en) ->
                    add(ChatMessage(chatMessageId++, false, ko, LocalTime.of(9, i), textEn = en))
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
        list += ChatMessage(chatMessageId++, true, clean, LocalTime.now())

        // Friends answer in the language you typed (emoji-only keeps the previous language).
        val lang = ReplyEngine.detect(clean, lastChatLang)
        lastChatLang = lang
        val reply = ReplyEngine.reply(clean, lang, friend.level, character.level)
        viewModelScope.launch {
            delay(600)
            typing[friendId] = true
            delay(1000L + (0..1200).random())
            typing[friendId] = false
            chats[friendId]?.add(ChatMessage(chatMessageId++, false, reply, LocalTime.now()))
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

    companion object {
        const val MAX_NICKNAME = 12
    }
}
