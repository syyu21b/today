package com.today.app.data

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class TodayViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = TodayStorage(app)

    // ---------------- Character & quests ----------------
    var character by mutableStateOf(CharacterState())
        private set
    val quests: SnapshotStateList<Quest> = mutableStateListOf()

    /** Incremented every time a quest is completed -> triggers the Lottie confetti. */
    var celebrationTick by mutableIntStateOf(0)
        private set

    /** Non-null while a "LEVEL UP" dialog should be shown. */
    var levelUpEvent by mutableStateOf<Int?>(null)
        private set

    private var questDate: String? = null
    private var lastCompleteDate: String? = null
    private var prevCompleteDate: String? = null

    // ---------------- Outfit / style transfer mock ----------------
    var isAnalyzingStyle by mutableStateOf(false)
        private set
    var analyzeStep by mutableStateOf("")
        private set
    var extractedSwatches by mutableStateOf<List<Int>>(emptyList())
        private set
    private var photoStyleCount = 0

    // ---------------- World ----------------
    var playerX by mutableIntStateOf(WorldMap.START_X)
        private set
    var playerY by mutableIntStateOf(WorldMap.START_Y)
        private set
    var facing by mutableStateOf(Facing.DOWN)
        private set
    var worldDialog by mutableStateOf<String?>(null)
        private set
    var steps by mutableIntStateOf(0)
        private set

    // ---------------- Community ----------------
    val friends = DummyData.friends
    val chats = mutableStateMapOf<String, SnapshotStateList<ChatMessage>>()
    val typing = mutableStateMapOf<String, Boolean>()
    val unread = mutableStateMapOf<String, Boolean>()
    var openChatId by mutableStateOf<String?>(null)
        private set
    private var messageId = 0L

    init {
        val saved = storage.load()
        val today = today()
        character = saved.character
        lastCompleteDate = saved.lastCompleteDate
        prevCompleteDate = saved.prevCompleteDate
        // New day -> quests reset; missed a day -> streak breaks.
        quests += if (saved.questDate == today) saved.quests else saved.quests.map { it.copy(done = false) }
        questDate = today
        if (lastCompleteDate != today && lastCompleteDate != yesterday()) {
            character = character.copy(streak = 0)
        }
        playerX = saved.worldX
        playerY = saved.worldY
        steps = saved.steps

        friends.forEach { f ->
            chats[f.id] = mutableStateListOf<ChatMessage>().apply {
                DummyData.openingMessages[f.id].orEmpty().forEach { add(ChatMessage(messageId++, false, it, "오전 9:0${size}")) }
            }
            unread[f.id] = true
        }
        persist()
    }

    // ======================= Quests =======================

    fun toggleQuest(id: String) {
        val index = quests.indexOfFirst { it.id == id }
        if (index < 0) return
        val quest = quests[index]
        val nowDone = !quest.done
        quests[index] = quest.copy(done = nowDone)

        if (nowDone) {
            addXp(quest.xp)
            celebrationTick++
        } else {
            addXp(-quest.xp)
        }
        updateStreak()
        persist()
    }

    fun addQuest(title: String, emoji: String, xp: Int) {
        if (title.isBlank()) return
        quests += Quest(UUID.randomUUID().toString(), title.trim(), emoji.ifBlank { "⭐" }, xp.coerceIn(5, 100))
        updateStreak()
        persist()
    }

    fun removeQuest(id: String) {
        quests.removeAll { it.id == id }
        updateStreak()
        persist()
    }

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

    /** Streak +1 the first time all quests are done today; undone if a quest is unchecked again. */
    private fun updateStreak() {
        val today = today()
        val allDone = quests.isNotEmpty() && quests.all { it.done }
        if (allDone && lastCompleteDate != today) {
            val newStreak = if (lastCompleteDate == yesterday()) character.streak + 1 else 1
            prevCompleteDate = lastCompleteDate
            lastCompleteDate = today
            character = character.copy(streak = newStreak)
        } else if (!allDone && lastCompleteDate == today) {
            lastCompleteDate = prevCompleteDate
            character = character.copy(streak = (character.streak - 1).coerceAtLeast(0))
        }
    }

    // ======================= Outfit =======================

    fun applyOutfit(outfit: Outfit) {
        character = character.copy(outfit = outfit)
        persist()
    }

    fun applyStyleFromUri(uri: Uri) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodeSampled(uri) } ?: return@launch
            applyStyleFromBitmap(bitmap)
        }
    }

    /**
     * "AI style transfer" mock: extracts the photo's dominant palette and maps it onto outfit slots.
     * The fake delay + step labels make it feel like a model is running.
     */
    fun applyStyleFromBitmap(bitmap: Bitmap) {
        if (isAnalyzingStyle) return
        viewModelScope.launch {
            isAnalyzingStyle = true
            extractedSwatches = emptyList()
            val steps = listOf("사진 분석 중…", "색감 추출 중…", "스타일 매칭 중…", "의상 생성 중…")
            for (s in steps) {
                analyzeStep = s
                delay(450)
            }
            val outfit = withContext(Dispatchers.Default) { outfitFromPalette(bitmap) }
            extractedSwatches = listOf(outfit.hair, outfit.shirt, outfit.accent, outfit.pants, outfit.shoes)
            applyOutfit(outfit)
            isAnalyzingStyle = false
        }
    }

    private fun outfitFromPalette(bitmap: Bitmap): Outfit {
        val p = Palette.from(bitmap).maximumColorCount(16).generate()
        val fallback = Outfits.presets.first()
        val dominant = p.getDominantColor(fallback.shirt)
        val shirt = p.getVibrantColor(dominant)
        val accent = p.getLightVibrantColor(p.getLightMutedColor(ColorUtils.blendARGB(shirt, 0xFFFFFFFF.toInt(), 0.5f)))
        val pants = p.getDarkMutedColor(p.getDarkVibrantColor(ColorUtils.blendARGB(dominant, 0xFF000000.toInt(), 0.5f)))
        val hair = p.getDarkVibrantColor(ColorUtils.blendARGB(pants, 0xFF000000.toInt(), 0.3f))
        val shoes = ColorUtils.blendARGB(pants, 0xFF000000.toInt(), 0.55f)
        photoStyleCount++
        return Outfit("포토 스타일 #$photoStyleCount", hair, shirt, accent, pants, shoes)
    }

    private fun decodeSampled(uri: Uri, target: Int = 256): Bitmap? = runCatching {
        val resolver = getApplication<Application>().contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }.getOrNull()

    // ======================= World =======================

    fun move(dir: Facing) {
        facing = dir
        worldDialog = null
        val (nx, ny) = when (dir) {
            Facing.UP -> playerX to playerY - 1
            Facing.DOWN -> playerX to playerY + 1
            Facing.LEFT -> playerX - 1 to playerY
            Facing.RIGHT -> playerX + 1 to playerY
        }
        if (!WorldMap.isWalkable(nx, ny)) return
        playerX = nx
        playerY = ny
        steps++
        // Walking bonus: every 50 steps in town = +5 XP
        if (steps % 50 == 0) {
            addXp(5)
            worldDialog = "🚶 산책 보너스! 50걸음 달성 +5 XP"
        }
        persist()
    }

    /** "A" button: talk to whatever is in front of the avatar. */
    fun interact() {
        val (fx, fy) = when (facing) {
            Facing.UP -> playerX to playerY - 1
            Facing.DOWN -> playerX to playerY + 1
            Facing.LEFT -> playerX - 1 to playerY
            Facing.RIGHT -> playerX + 1 to playerY
        }
        val npc = WorldMap.npcs.firstOrNull { it.x == fx && it.y == fy }
        worldDialog = when {
            npc != null -> "${npc.name}: ${npc.line}"
            WorldMap.tileAt(fx, fy) == 'D' -> WorldMap.doorMessages["$fx,$fy"]
            WorldMap.tileAt(fx, fy) == 'S' -> WorldMap.SIGN_MESSAGE
            WorldMap.tileAt(fx, fy) == 'W' -> "💧 맑은 연못이다. 물 마시기 퀘스트가 생각난다…"
            WorldMap.tileAt(fx, fy) == 'T' -> "🌳 커다란 나무다."
            else -> "아무것도 없다."
        }
    }

    fun dismissWorldDialog() {
        worldDialog = null
    }

    // ======================= Community =======================

    fun openChat(friendId: String?) {
        openChatId = friendId
        if (friendId != null) unread[friendId] = false
    }

    fun sendMessage(friendId: String, text: String) {
        if (text.isBlank()) return
        val list = chats[friendId] ?: return
        list += ChatMessage(messageId++, true, text.trim(), nowTime())
        // Simulated friend reply: typing indicator, then an answer.
        viewModelScope.launch {
            delay(600)
            typing[friendId] = true
            delay(1200L + (0..1200).random())
            typing[friendId] = false
            list += ChatMessage(messageId++, false, DummyData.autoReplies.random(), nowTime())
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
        storage.save(
            SavedState(character, quests.toList(), questDate, lastCompleteDate, prevCompleteDate, playerX, playerY, steps)
        )
    }

    private fun today() = LocalDate.now().toString()
    private fun yesterday() = LocalDate.now().minusDays(1).toString()
    private fun nowTime(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("a h:mm"))
}
