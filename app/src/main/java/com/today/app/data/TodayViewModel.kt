package com.today.app.data

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.today.app.i18n.Strings
import com.today.app.i18n.strings
import com.today.app.notify.Notifier
import com.today.app.notify.ReminderScheduler
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

    /** Set from the activity lifecycle: message alerts are skipped for the chat you're looking at. */
    var appInForeground = false

    /** A notification asked to open this chat; the UI switches to the Messages tab and consumes it. */
    var pendingChatNavigation by mutableStateOf<String?>(null)
        private set

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

    // ---------------- Friends & chats ----------------
    /** All friends including blocked ones (persisted). */
    val friends: SnapshotStateList<Friend> = mutableStateListOf()
    val chats = mutableStateMapOf<String, SnapshotStateList<ChatMessage>>()
    val typing = mutableStateMapOf<String, Boolean>()
    val unread = mutableStateMapOf<String, Boolean>()
    var openChatId by mutableStateOf<String?>(null)
        private set
    private var chatMessageId = 0L
    private var lastChatLang = ReplyEngine.Lang.KO

    /** Friends shown in Messages (blocked ones are hidden everywhere except Settings). */
    val visibleFriends: List<Friend> get() = friends.filter { !it.blocked }
    val blockedFriends: List<Friend> get() = friends.filter { it.blocked }

    /** Date/time/timezone changed in system settings -> re-check "today" right away. */
    private val clockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshDay()
    }

    init {
        loadFromStorage()
        ContextCompat.registerReceiver(
            app,
            clockReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        // App kept open across midnight: roll the day over without needing a resume.
        viewModelScope.launch {
            while (true) {
                delay(DAY_CHECK_INTERVAL_MS)
                refreshDay()
            }
        }
    }

    /** @param keepLanguage used by "reset all" so the app doesn't jump back to the default language. */
    private fun loadFromStorage(keepLanguage: AppLanguage? = null) {
        val saved = storage.load()
        character = saved.character
        settings = if (keepLanguage == null) saved.settings
        else AppSettings(nickname = keepLanguage.strings().defaultNickname, language = keepLanguage)
        // The OS can revoke notification permission at any time: reflect that in the switch.
        if (settings.notificationsEnabled && !Notifier.hasPermission(getApplication())) {
            settings = settings.copy(notificationsEnabled = false)
        }
        questsByDate.clear()
        questsByDate.putAll(saved.questsByDate)
        todayDate = LocalDate.now()
        selectedDate = todayDate
        clipboard = null
        lastChatLang = if (settings.language == AppLanguage.KOREAN) ReplyEngine.Lang.KO else ReplyEngine.Lang.EN
        enforceXpCap()
        ensureTodayQuests()

        friends.clear()
        friends.addAll(saved.friends)
        loadChats(if (settings.saveChatHistory) saved.chats else null)

        persist()
        ReminderScheduler.reschedule(getApplication(), settings)
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
    val totalQuests: Int get() = questsByDate.values.sumOf { it.size }
    val recordedDays: Int get() = questsByDate.count { (_, list) -> list.isNotEmpty() }
    val totalMessages: Int get() = chats.values.sumOf { it.size }

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
        // Everything done: no need for tonight's streak alert.
        if (list.size > 0 && questsFor(date).all { it.done }) Notifier.cancel(getApplication(), Notifier.ID_STREAK)
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

    /** Generic setter for simple preferences; side effects (alarms, history) are handled here. */
    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val old = settings
        val new = transform(old)
        if (new == old) return
        settings = new.copy(defaultQuestXp = new.defaultQuestXp.coerceIn(MIN_QUEST_XP, MAX_QUEST_XP))
        persist()
        if (notificationFieldsChanged(old, settings)) ReminderScheduler.reschedule(getApplication(), settings)
    }

    private fun notificationFieldsChanged(a: AppSettings, b: AppSettings) =
        a.notificationsEnabled != b.notificationsEnabled || a.questReminder != b.questReminder ||
            a.questReminderTime != b.questReminderTime || a.streakReminder != b.streakReminder ||
            a.streakReminderTime != b.streakReminderTime || a.language != b.language

    fun updateNickname(name: String) {
        val clean = name.trim().take(MAX_NICKNAME)
        if (clean.isEmpty()) return
        updateSettings { it.copy(nickname = clean) }
    }

    /**
     * Switches every UI string. Built-in content that the user never renamed (default quest titles,
     * default nickname) follows the language too; anything the user typed stays as written.
     */
    fun updateLanguage(language: AppLanguage) {
        if (language == settings.language) return
        val oldDefaultNick = settings.language.strings().defaultNickname
        questsByDate.keys.toList().forEach { date ->
            val list = questsByDate[date] ?: return@forEach
            questsByDate[date] = list.map { q -> DummyData.translateDefaultTitle(q.title, language)?.let { q.copy(title = it) } ?: q }
        }
        clipboard = clipboard?.let { c ->
            c.copy(quests = c.quests.map { q -> DummyData.translateDefaultTitle(q.title, language)?.let { q.copy(title = it) } ?: q })
        }
        lastChatLang = if (language == AppLanguage.KOREAN) ReplyEngine.Lang.KO else ReplyEngine.Lang.EN
        updateSettings {
            it.copy(
                language = language,
                nickname = if (it.nickname == oldDefaultNick) language.strings().defaultNickname else it.nickname,
            )
        }
        Notifier.ensureChannels(getApplication(), strings) // rename channels in system settings
    }

    /** Master switch. Turning on requires permission, which the UI requests first. */
    fun setNotificationsEnabled(enabled: Boolean) {
        if (enabled && !Notifier.canPost(getApplication())) {
            showMessage(strings.notifPermissionDenied)
            return
        }
        if (enabled) Notifier.ensureChannels(getApplication(), strings)
        updateSettings { it.copy(notificationsEnabled = enabled) }
    }

    fun sendTestNotification() {
        if (!Notifier.canPost(getApplication())) {
            showMessage(strings.notifPermissionDenied)
            return
        }
        Notifier.post(getApplication(), strings, Notifier.ID_TEST, Notifier.CHANNEL_REMINDERS, strings.testNotificationTitle, strings.testNotificationBody)
    }

    fun resetAll() {
        val language = settings.language
        storage.clear()
        clearStylePhoto()
        photoStyleCount = 0
        loadFromStorage(keepLanguage = language)
        showMessage(strings.msgReset)
    }

    /** Writes the export JSON to a document the user picked (Storage Access Framework). */
    fun exportData(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                val json = storage.exportJson(snapshot())
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
                }
            }.isSuccess
            showMessage(if (ok) strings.exportDone else strings.exportFailed)
        }
    }

    fun showMessage(text: String) {
        message = UiMessage(++messageCounter, text)
    }

    fun consumeMessage(id: Long) {
        if (message?.id == id) message = null
    }

    // ======================= Friends =======================

    fun friend(id: String) = friends.firstOrNull { it.id == id }

    fun finishContactsPrompt() = updateSettings { it.copy(contactsPromptDone = true) }

    /** @return null on success, otherwise the (localized) reason it failed. */
    fun addFriend(name: String, handle: String): String? {
        val cleanName = name.trim().take(MAX_FRIEND_NAME)
        if (cleanName.isEmpty()) return strings.errFriendName
        if (friends.size >= MAX_FRIENDS) return strings.errFriendLimit(MAX_FRIENDS)
        val cleanHandle = sanitizeHandle(handle).ifEmpty { generateHandle(cleanName) }
        if (friends.any { it.handle.equals(cleanHandle, ignoreCase = true) }) return strings.errFriendDuplicate
        val f = newFriend("m" + UUID.randomUUID().toString().take(8), cleanName, cleanHandle, FriendSource.MANUAL, null)
        friends += f
        chats[f.id] = mutableStateListOf()
        persist()
        showMessage(strings.msgFriendAdded(cleanName))
        return null
    }

    /** Adds the picked address-book entries (skipping ones already added). Returns how many were added. */
    fun importContacts(entries: List<ContactEntry>): Int {
        var added = 0
        for (c in entries) {
            if (friends.size >= MAX_FRIENDS) break
            if (friends.any { it.contactId == c.id }) continue
            val f = newFriend("c${c.id}", c.name.take(MAX_FRIEND_NAME), generateHandle(c.name), FriendSource.CONTACT, c.id)
            friends += f
            chats[f.id] = mutableStateListOf()
            added++
        }
        updateSettings { it.copy(contactsPromptDone = true) }
        persist()
        showMessage(strings.msgContactsImported(added))
        return added
    }

    fun removeFriend(id: String) {
        val f = friend(id) ?: return
        friends.removeAll { it.id == id }
        chats.remove(id)
        typing.remove(id)
        unread.remove(id)
        if (openChatId == id) openChatId = null
        Notifier.cancel(getApplication(), Notifier.messageId(id))
        persist()
        showMessage(strings.msgFriendDeleted(strings.friendName(f)))
    }

    fun setBlocked(id: String, blocked: Boolean) {
        val i = friends.indexOfFirst { it.id == id }
        if (i < 0) return
        friends[i] = friends[i].copy(blocked = blocked)
        if (blocked) {
            if (openChatId == id) openChatId = null
            unread[id] = false
            typing[id] = false
            Notifier.cancel(getApplication(), Notifier.messageId(id))
        }
        persist()
        val name = strings.friendName(friends[i])
        showMessage(if (blocked) strings.msgBlocked(name) else strings.msgUnblocked(name))
    }

    private fun newFriend(id: String, name: String, handle: String, source: FriendSource, contactId: Long?) = Friend(
        id = id,
        name = name,
        nameEn = name,
        handle = handle,
        outfit = Outfits.presets.random(),
        online = (0..2).random() > 0,
        level = (1..15).random(),
        source = source,
        contactId = contactId,
    )

    private fun sanitizeHandle(raw: String) = raw.trim().removePrefix("@").lowercase().filter { it.isLetterOrDigit() && it.code < 128 || it == '_' || it == '.' }.take(20)

    /** ASCII handle from the name, or "friend1234" for names without Latin letters (e.g. Korean). */
    private fun generateHandle(name: String): String {
        val base = sanitizeHandle(name.replace(' ', '_')).ifEmpty { "friend" }
        var candidate = base
        while (friends.any { it.handle.equals(candidate, ignoreCase = true) }) candidate = base + (1000..9999).random()
        return candidate
    }

    // ======================= Chats =======================

    private fun loadChats(saved: Map<String, List<ChatMessage>>?) {
        openChatId = null
        chats.clear()
        typing.clear()
        unread.clear()
        var maxId = -1L
        friends.forEach { f ->
            val history = saved?.get(f.id)
            chats[f.id] = mutableStateListOf<ChatMessage>().apply {
                if (history != null) {
                    addAll(history)
                } else {
                    // First run (or history off): scripted openers for the demo friends.
                    DummyData.openingMessages[f.id].orEmpty().forEachIndexed { i, (ko, en) ->
                        add(ChatMessage(maxId + 1 + i, false, ko, LocalTime.of(9, i), textEn = en))
                    }
                }
            }
            chats[f.id]?.maxOfOrNull { it.id }?.let { if (it > maxId) maxId = it }
            unread[f.id] = history == null && chats[f.id]?.isNotEmpty() == true
        }
        chatMessageId = maxId + 1
    }

    fun clearAllChats() {
        friends.forEach { f -> chats[f.id] = mutableStateListOf() }
        typing.clear()
        unread.clear()
        persist()
        showMessage(strings.msgChatsCleared)
    }

    fun openChat(friendId: String?) {
        if (friendId != null && friend(friendId)?.blocked != false) return
        openChatId = friendId
        if (friendId != null) {
            unread[friendId] = false
            Notifier.cancel(getApplication(), Notifier.messageId(friendId))
        }
    }

    /** Called when a message notification is tapped. */
    fun openChatFromNotification(friendId: String) {
        if (friend(friendId)?.blocked != false) return
        openChat(friendId)
        pendingChatNavigation = friendId
    }

    fun consumeChatNavigation() {
        pendingChatNavigation = null
    }

    fun sendMessage(friendId: String, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val f = friend(friendId) ?: return
        if (f.blocked) return
        val list = chats.getOrPut(friendId) { mutableStateListOf() }
        val myId = chatMessageId++
        list += ChatMessage(myId, true, clean, LocalTime.now())
        persist()
        if (!settings.autoReplies) return

        // Friends answer in the language you typed (emoji-only keeps the previous language).
        val lang = ReplyEngine.detect(clean, lastChatLang)
        lastChatLang = lang
        val reply = ReplyEngine.reply(clean, lang, f.level, character.level)
        viewModelScope.launch {
            delay(600)
            if (friend(friendId)?.blocked != false) return@launch // blocked/removed meanwhile
            chats[friendId]?.let { msgs -> msgs.indices.filter { msgs[it].fromMe && !msgs[it].read }.forEach { msgs[it] = msgs[it].copy(read = true) } }
            typing[friendId] = true
            delay(1000L + (0..1200).random())
            typing[friendId] = false
            if (friend(friendId)?.blocked != false) return@launch
            chats[friendId]?.add(ChatMessage(chatMessageId++, false, reply, LocalTime.now()))
            val viewing = appInForeground && openChatId == friendId
            if (!viewing) {
                unread[friendId] = true
                notifyMessage(friendId, reply)
            }
            persist()
        }
    }

    private fun notifyMessage(friendId: String, text: String) {
        val s = settings
        if (!s.notificationsEnabled || !s.messageNotifications) return
        val f = friend(friendId) ?: return
        Notifier.post(
            getApplication(), strings, Notifier.messageId(friendId), Notifier.CHANNEL_MESSAGES,
            strings.friendName(f), if (s.messagePreview) text else strings.newMessageHidden, openChatFriendId = friendId,
        )
    }

    fun toggleLike(friendId: String, messageId: Long) {
        val list = chats[friendId] ?: return
        val i = list.indexOfFirst { it.id == messageId }
        if (i >= 0) list[i] = list[i].copy(liked = !list[i].liked)
        persist()
    }

    // ======================= Helpers =======================

    private fun snapshot() = SavedState(
        character, questsByDate.toMap(), settings, friends.toList(),
        if (settings.saveChatHistory) chats.mapValues { it.value.toList() } else null,
    )

    private fun persist() {
        storage.save(snapshot())
    }

    override fun onCleared() {
        analyzer.close()
        getApplication<Application>().unregisterReceiver(clockReceiver)
    }

    private fun newId() = UUID.randomUUID().toString()

    companion object {
        const val MAX_NICKNAME = 12
        const val MAX_FRIEND_NAME = 20
        private const val DAY_CHECK_INTERVAL_MS = 30_000L
    }
}
