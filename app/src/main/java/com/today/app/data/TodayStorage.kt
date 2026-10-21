package com.today.app.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Saved snapshot of everything that should survive an app restart. */
data class SavedState(
    val character: CharacterState,
    /** ISO date ("2026-10-19") -> that day's quests. */
    val questsByDate: Map<String, List<Quest>>,
    val settings: AppSettings,
    val friends: List<Friend>,
    /** friendId -> messages; null when nothing was saved (history off or first run). */
    val chats: Map<String, List<ChatMessage>>?,
    val rewards: RewardState = RewardState(),
    val pet: PetState? = null,
    /** Mission counters of the last day the app was used. */
    val missions: MissionDay? = null,
)

/**
 * [date]'s quests as the app will show them: the saved list, or - if the app hasn't been opened
 * that day yet - the list it will carry over (unchecked). Used where there's no ViewModel.
 */
fun SavedState.questsOn(date: LocalDate): List<Quest> {
    val key = date.toString()
    questsByDate[key]?.let { return it }
    if (!settings.autoCarryOver) return emptyList()
    return questsByDate.keys.filter { it < key }.sortedDescending()
        .firstNotNullOfOrNull { k -> questsByDate[k]?.takeIf { it.isNotEmpty() } }
        ?.take(MAX_QUESTS_PER_DAY)
        ?.map { it.copy(done = false) }
        .orEmpty()
}

/** Tiny SharedPreferences + JSON persistence (enough for a 2–3 day project, no Room needed). */
class TodayStorage(context: Context) {
    private val prefs = context.getSharedPreferences("today_prefs", Context.MODE_PRIVATE)

    fun load(): SavedState {
        val outfit = runCatching { prefs.getString("outfit", null)?.let { outfitFromJson(JSONObject(it)) } }.getOrNull()
            ?: Outfits.presets.first()

        val questsByDate = mutableMapOf<String, List<Quest>>()
        val byDateJson = prefs.getString("questsByDate", null)
        if (byDateJson != null) {
            runCatching {
                val obj = JSONObject(byDateJson)
                obj.keys().forEach { date -> questsByDate[date] = questsFromJson(obj.getJSONArray(date)) }
            }
        } else {
            // Migrate v1 format (one list + the date it belonged to).
            prefs.getString("quests", null)?.let { json ->
                runCatching {
                    val date = prefs.getString("questDate", null) ?: java.time.LocalDate.now().toString()
                    questsByDate[date] = questsFromJson(JSONArray(json))
                }
            }
        }

        val friends = prefs.getString("friends", null)
            ?.let { json -> runCatching { JSONArray(json).let { arr -> List(arr.length()) { friendFromJson(arr.getJSONObject(it)) } } }.getOrNull() }
            ?: DummyData.friends

        val chats = prefs.getString("chats", null)?.let { json ->
            runCatching {
                val obj = JSONObject(json)
                obj.keys().asSequence().associateWith { id -> messagesFromJson(obj.getJSONArray(id)) }
            }.getOrNull()
        }

        return SavedState(
            character = CharacterState(
                level = prefs.getInt("level", 1).coerceAtLeast(1),
                xp = prefs.getInt("xp", 0).coerceAtLeast(0),
                outfit = outfit,
            ),
            questsByDate = questsByDate,
            settings = loadSettings(),
            friends = friends,
            chats = chats,
            rewards = loadRewards(),
            pet = prefs.getString("pet", null)?.let { runCatching { petFromJson(JSONObject(it)) }.getOrNull() },
            missions = prefs.getString("missions", null)?.let { runCatching { missionsFromJson(JSONObject(it)) }.getOrNull() },
        )
    }

    fun save(s: SavedState) {
        val byDate = JSONObject()
        s.questsByDate.forEach { (date, quests) -> byDate.put(date, questsToJson(quests)) }
        prefs.edit {
            putInt("level", s.character.level)
            putInt("xp", s.character.xp)
            putString("outfit", outfitToJson(s.character.outfit).toString())
            putString("questsByDate", byDate.toString())
            putString("settings", settingsToJson(s.settings).toString())
            putString("friends", JSONArray().apply { s.friends.forEach { put(friendToJson(it)) } }.toString())
            putString("rewards", rewardsToJson(s.rewards).toString())
            if (s.pet == null) remove("pet") else putString("pet", petToJson(s.pet).toString())
            if (s.missions == null) remove("missions") else putString("missions", missionsToJson(s.missions).toString())
            if (s.chats == null) {
                remove("chats")
            } else {
                val obj = JSONObject()
                // Keep the most recent 200 messages per friend so prefs stay small.
                s.chats.forEach { (id, list) -> obj.put(id, messagesToJson(list.takeLast(200))) }
                putString("chats", obj.toString())
            }
            // Keys from older versions that are no longer used
            listOf(
                "quests", "questDate", "streak", "lastCompleteDate", "prevCompleteDate", "worldX", "worldY", "steps",
                "replyLanguage", "nickname", "appLanguage", "autoCarry",
            ).forEach { remove(it) }
        }
    }

    fun clear() {
        prefs.edit { clear() }
    }

    /** Everything the user owns, as one pretty-printed JSON document (Settings > Data > Export). */
    fun exportJson(s: SavedState): String = JSONObject()
        .put("app", "Today")
        .put("exportedAt", java.time.LocalDateTime.now().toString())
        .put("character", JSONObject().put("level", s.character.level).put("xp", s.character.xp).put("outfit", outfitToJson(s.character.outfit)))
        .put("questsByDate", JSONObject().apply { s.questsByDate.toSortedMap().forEach { (d, q) -> put(d, questsToJson(q)) } })
        .put("friends", JSONArray().apply { s.friends.forEach { put(friendToJson(it)) } })
        .put("rewards", rewardsToJson(s.rewards))
        .put("settings", settingsToJson(s.settings))
        .apply { s.pet?.let { put("pet", petToJson(it)) } }
        .toString(2)

    /**
     * Replaces everything with a backup made by [exportJson]. The whole file is checked first,
     * so a broken or foreign file changes nothing. Afterwards [load] validates each value as usual.
     * @return false when [json] isn't a Today backup.
     */
    fun importJson(json: String): Boolean {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return false
        if (root.optString("app") != "Today") return false
        val character = root.optJSONObject("character") ?: return false
        val outfit = runCatching {
            val byDate = root.optJSONObject("questsByDate") ?: JSONObject()
            byDate.keys().forEach { questsFromJson(byDate.getJSONArray(it)) }
            root.optJSONArray("friends")?.let { arr -> List(arr.length()) { friendFromJson(arr.getJSONObject(it)) } }
            root.optJSONObject("pet")?.let { petFromJson(it) }
            outfitFromJson(character.getJSONObject("outfit"))
        }.getOrNull() ?: return false
        prefs.edit {
            clear()
            putInt("level", character.optInt("level", 1))
            putInt("xp", character.optInt("xp", 0))
            putString("outfit", outfitToJson(outfit).toString())
            putString("questsByDate", (root.optJSONObject("questsByDate") ?: JSONObject()).toString())
            root.optJSONArray("friends")?.let { putString("friends", it.toString()) }
            root.optJSONObject("rewards")?.let { putString("rewards", it.toString()) }
            // A restored backup never shows the first-run guide again.
            putString("settings", (root.optJSONObject("settings") ?: JSONObject()).put("onboardingDone", true).toString())
            root.optJSONObject("pet")?.let { putString("pet", it.toString()) }
        }
        return true
    }

    // ------------------------------------------------------------------ settings

    private fun loadSettings(): AppSettings {
        val d = AppSettings()
        val json = prefs.getString("settings", null)?.let { runCatching { JSONObject(it) }.getOrNull() }
        if (json == null) {
            // v1.1/v1.2 stored a few separate keys; nothing at all = a fresh install, which gets the guide.
            return d.copy(
                nickname = prefs.getString("nickname", null) ?: d.nickname,
                language = enumOr(prefs.getString("appLanguage", null), d.language),
                autoCarryOver = prefs.getBoolean("autoCarry", d.autoCarryOver),
                onboardingDone = prefs.contains("level"),
            )
        }
        return AppSettings(
            nickname = json.optString("nickname", d.nickname).ifBlank { d.nickname },
            language = enumOr(json.optString("language"), d.language),
            autoCarryOver = json.optBoolean("autoCarryOver", d.autoCarryOver),
            weekStartsMonday = json.optBoolean("weekStartsMonday", d.weekStartsMonday),
            celebrationEffects = json.optBoolean("celebrationEffects", d.celebrationEffects),
            defaultQuestXp = json.optInt("defaultQuestXp", d.defaultQuestXp).coerceIn(MIN_QUEST_XP, MAX_QUEST_XP),
            notificationsEnabled = json.optBoolean("notificationsEnabled", d.notificationsEnabled),
            questReminder = json.optBoolean("questReminder", d.questReminder),
            questReminderTime = json.optInt("questReminderTime", d.questReminderTime).coerceIn(0, 24 * 60 - 1),
            streakReminder = json.optBoolean("streakReminder", d.streakReminder),
            streakReminderTime = json.optInt("streakReminderTime", d.streakReminderTime).coerceIn(0, 24 * 60 - 1),
            messageNotifications = json.optBoolean("messageNotifications", d.messageNotifications),
            messagePreview = json.optBoolean("messagePreview", d.messagePreview),
            chatFontSize = enumOr(json.optString("chatFontSize"), d.chatFontSize),
            enterToSend = json.optBoolean("enterToSend", d.enterToSend),
            readReceipts = json.optBoolean("readReceipts", d.readReceipts),
            autoReplies = json.optBoolean("autoReplies", d.autoReplies),
            saveChatHistory = json.optBoolean("saveChatHistory", d.saveChatHistory),
            contactsPromptDone = json.optBoolean("contactsPromptDone", d.contactsPromptDone),
            statusMood = json.optString("statusMood").takeIf { it.isNotEmpty() }?.let { runCatching { Mood.valueOf(it) }.getOrNull() },
            statusMessage = json.optString("statusMessage").take(MAX_STATUS_MESSAGE),
            themeMode = enumOr(json.optString("themeMode"), d.themeMode),
            // Settings saved before the guide existed belong to existing users.
            onboardingDone = json.optBoolean("onboardingDone", true),
            crashReports = json.optBoolean("crashReports", d.crashReports),
        )
    }

    private fun settingsToJson(s: AppSettings) = JSONObject()
        .put("nickname", s.nickname)
        .put("language", s.language.name)
        .put("autoCarryOver", s.autoCarryOver)
        .put("weekStartsMonday", s.weekStartsMonday)
        .put("celebrationEffects", s.celebrationEffects)
        .put("defaultQuestXp", s.defaultQuestXp)
        .put("notificationsEnabled", s.notificationsEnabled)
        .put("questReminder", s.questReminder)
        .put("questReminderTime", s.questReminderTime)
        .put("streakReminder", s.streakReminder)
        .put("streakReminderTime", s.streakReminderTime)
        .put("messageNotifications", s.messageNotifications)
        .put("messagePreview", s.messagePreview)
        .put("chatFontSize", s.chatFontSize.name)
        .put("enterToSend", s.enterToSend)
        .put("readReceipts", s.readReceipts)
        .put("autoReplies", s.autoReplies)
        .put("saveChatHistory", s.saveChatHistory)
        .put("contactsPromptDone", s.contactsPromptDone)
        .put("statusMood", s.statusMood?.name ?: "")
        .put("statusMessage", s.statusMessage)
        .put("themeMode", s.themeMode.name)
        .put("onboardingDone", s.onboardingDone)
        .put("crashReports", s.crashReports)

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        runCatching { enumValueOf<E>(name ?: "") }.getOrDefault(default)

    // ------------------------------------------------------------------ rewards

    /** Missing / broken data -> defaults; unknown ids from other versions are dropped. */
    private fun loadRewards(): RewardState {
        val d = RewardState()
        val j = prefs.getString("rewards", null)?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return d
        fun strings(key: String): Set<String> =
            j.optJSONArray(key)?.let { arr -> List(arr.length()) { arr.optString(it) }.toSet() }.orEmpty()
        val titles = strings("titles").filter { Titles.byId(it) != null }.toSet() + Titles.DEFAULT
        return RewardState(
            rewardedLevel = j.optInt("rewardedLevel", d.rewardedLevel).coerceAtLeast(1),
            shields = j.optInt("shields", 0).coerceAtLeast(0),
            shieldedDates = strings("shieldedDates").filter { runCatching { java.time.LocalDate.parse(it) }.isSuccess }.toSet(),
            boxes = j.optInt("boxes", 0).coerceAtLeast(0),
            stardust = j.optInt("stardust", 0).coerceAtLeast(0),
            items = strings("items").filter { Accessories.byId(it) != null }.toSet(),
            titles = titles,
            equippedTitle = j.optString("equippedTitle", Titles.DEFAULT).takeIf { it in titles } ?: Titles.DEFAULT,
            backdrop = j.optString("backdrop", Backdrops.DEFAULT).takeIf { Backdrops.byId(it) != null } ?: Backdrops.DEFAULT,
            eventProgress = j.optJSONObject("eventProgress")?.let { o ->
                o.keys().asSequence().associateWith { o.optInt(it).coerceIn(0, SeasonEvents.GOAL_DAYS) }
            }.orEmpty(),
        )
    }

    private fun rewardsToJson(r: RewardState) = JSONObject()
        .put("rewardedLevel", r.rewardedLevel)
        .put("shields", r.shields)
        .put("shieldedDates", JSONArray(r.shieldedDates.sorted()))
        .put("boxes", r.boxes)
        .put("stardust", r.stardust)
        .put("items", JSONArray(r.items.sorted()))
        .put("titles", JSONArray(r.titles.sorted()))
        .put("equippedTitle", r.equippedTitle)
        .put("backdrop", r.backdrop)
        .put("eventProgress", JSONObject().apply { r.eventProgress.forEach { (k, v) -> put(k, v) } })

    // ------------------------------------------------------------------ pet & missions

    private fun petToJson(p: PetState) = JSONObject().put("species", p.species.name).put("name", p.name).put("growth", p.growth)

    private fun petFromJson(o: JSONObject) = PetState(
        species = PetSpecies.valueOf(o.getString("species")),
        name = o.optString("name").take(MAX_PET_NAME),
        growth = o.optInt("growth", 0).coerceAtLeast(0),
    )

    private fun missionsToJson(m: MissionDay) = JSONObject()
        .put("date", m.date).put("petted", m.petted).put("chats", m.chats).put("added", m.added)
        .put("morning", m.morning).put("status", m.status)
        .put("claimed", JSONArray(m.claimed.map { it.name }.sorted())).put("bonusClaimed", m.bonusClaimed)

    private fun missionsFromJson(o: JSONObject) = MissionDay(
        date = o.getString("date"),
        petted = o.optInt("petted").coerceAtLeast(0),
        chats = o.optInt("chats").coerceAtLeast(0),
        added = o.optInt("added").coerceAtLeast(0),
        morning = o.optBoolean("morning"),
        status = o.optBoolean("status"),
        claimed = o.optJSONArray("claimed")?.let { arr ->
            List(arr.length()) { arr.optString(it) }.mapNotNull { runCatching { MissionType.valueOf(it) }.getOrNull() }.toSet()
        }.orEmpty(),
        bonusClaimed = o.optBoolean("bonusClaimed"),
    )

    // ------------------------------------------------------------------ quests

    private fun questsToJson(quests: List<Quest>) = JSONArray().apply {
        quests.forEach { q ->
            put(
                JSONObject().put("id", q.id).put("title", q.title).put("emoji", q.emoji).put("xp", q.xp).put("done", q.done)
                    .apply { if (q.days.isNotEmpty()) put("days", JSONArray(q.days.map { it.value }.sorted())) }
                    .apply { q.remindAt?.let { put("remindAt", it) } }
            )
        }
    }

    private fun questsFromJson(arr: JSONArray): List<Quest> = List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        Quest(
            o.getString("id"), o.getString("title"), o.optString("emoji", "⭐"), o.optInt("xp", 20), o.optBoolean("done", false),
            days = o.optJSONArray("days")?.let { arr -> List(arr.length()) { arr.optInt(it) }.filter { it in 1..7 }.map(DayOfWeek::of).toSet() }.orEmpty(),
            remindAt = if (o.has("remindAt")) o.optInt("remindAt").takeIf { it in 0 until 24 * 60 } else null,
        )
    }

    // ------------------------------------------------------------------ friends & chats

    private fun friendToJson(f: Friend) = JSONObject()
        .put("id", f.id).put("name", f.name).put("nameEn", f.nameEn).put("handle", f.handle)
        .put("outfit", outfitToJson(f.outfit)).put("online", f.online).put("level", f.level)
        .put("source", f.source.name).put("blocked", f.blocked)
        .apply { f.contactId?.let { put("contactId", it) } }

    private fun friendFromJson(o: JSONObject) = Friend(
        id = o.getString("id"),
        name = o.getString("name"),
        nameEn = o.optString("nameEn", o.getString("name")),
        handle = o.optString("handle"),
        outfit = outfitFromJson(o.getJSONObject("outfit")),
        online = o.optBoolean("online"),
        level = o.optInt("level", 1),
        source = enumOr(o.optString("source"), FriendSource.MANUAL),
        blocked = o.optBoolean("blocked"),
        contactId = if (o.has("contactId")) o.getLong("contactId") else null,
    )

    private fun messagesToJson(list: List<ChatMessage>) = JSONArray().apply {
        list.forEach { m ->
            put(
                JSONObject().put("id", m.id).put("fromMe", m.fromMe).put("text", m.text).put("time", m.time.toString())
                    .put("liked", m.liked).put("read", m.read).apply { m.textEn?.let { put("textEn", it) } }
            )
        }
    }

    private fun messagesFromJson(arr: JSONArray): List<ChatMessage> = List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        ChatMessage(
            id = o.getLong("id"),
            fromMe = o.getBoolean("fromMe"),
            text = o.getString("text"),
            time = runCatching { LocalTime.parse(o.getString("time")) }.getOrDefault(LocalTime.NOON),
            liked = o.optBoolean("liked"),
            textEn = if (o.has("textEn")) o.getString("textEn") else null,
            read = o.optBoolean("read"),
        )
    }

    // ------------------------------------------------------------------ outfit

    /** Only a known accessory of the right slot survives loading. */
    private fun accessoryOr(id: String, slot: AccessorySlot) = if (Accessories.byId(id)?.slot == slot) id else ""

    private fun outfitToJson(o: Outfit) = JSONObject()
        .put("name", o.name).put("hair", o.hair).put("shirt", o.shirt)
        .put("accent", o.accent).put("pants", o.pants).put("shoes", o.shoes)
        .put("skin", o.skin).put("stripe", o.stripe).put("striped", o.striped)
        .put("shortSleeve", o.shortSleeve).put("shorts", o.shorts).put("longHair", o.longHair)
        .put("head", o.head).put("face", o.face).put("back", o.back)
        .put("hairStyle", o.hairStyle?.name ?: "")
        .put("features", JSONArray(o.features.map { it.name }.sorted()))

    private fun outfitFromJson(j: JSONObject): Outfit {
        val accent = j.getInt("accent")
        return Outfit(
            name = Outfits.normalizeName(j.getString("name")),
            hair = j.getInt("hair"),
            shirt = j.getInt("shirt"),
            accent = accent,
            pants = j.getInt("pants"),
            shoes = j.getInt("shoes"),
            skin = j.optInt("skin", DEFAULT_SKIN),
            stripe = j.optInt("stripe", accent),
            striped = j.optBoolean("striped", false),
            shortSleeve = j.optBoolean("shortSleeve", false),
            shorts = j.optBoolean("shorts", false),
            longHair = j.optBoolean("longHair", false),
            head = accessoryOr(j.optString("head"), AccessorySlot.HEAD),
            face = accessoryOr(j.optString("face"), AccessorySlot.FACE),
            back = accessoryOr(j.optString("back"), AccessorySlot.BACK),
            hairStyle = j.optString("hairStyle").takeIf { it.isNotEmpty() }?.let { runCatching { HairStyle.valueOf(it) }.getOrNull() },
            features = j.optJSONArray("features")?.let { arr ->
                List(arr.length()) { arr.optString(it) }.mapNotNull { runCatching { FaceFeature.valueOf(it) }.getOrNull() }.toSet()
            }.orEmpty(),
        )
    }
}
