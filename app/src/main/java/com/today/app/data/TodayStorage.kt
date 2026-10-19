package com.today.app.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Saved snapshot of everything that should survive an app restart. */
data class SavedState(
    val character: CharacterState,
    /** ISO date ("2026-10-19") -> that day's quests. */
    val questsByDate: Map<String, List<Quest>>,
    val settings: AppSettings,
)

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
                    val date = prefs.getString("questDate", null) ?: LocalDate.now().toString()
                    questsByDate[date] = questsFromJson(JSONArray(json))
                }
            }
        }

        val language = runCatching { AppLanguage.valueOf(prefs.getString("appLanguage", null) ?: "KOREAN") }
            .getOrDefault(AppLanguage.KOREAN)

        return SavedState(
            character = CharacterState(
                level = prefs.getInt("level", 1).coerceAtLeast(1),
                xp = prefs.getInt("xp", 0).coerceAtLeast(0),
                outfit = outfit,
            ),
            questsByDate = questsByDate,
            settings = AppSettings(
                nickname = prefs.getString("nickname", null) ?: AppSettings().nickname,
                language = language,
                autoCarryOver = prefs.getBoolean("autoCarry", true),
            ),
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
            putString("nickname", s.settings.nickname)
            putString("appLanguage", s.settings.language.name)
            putBoolean("autoCarry", s.settings.autoCarryOver)
            // v1 keys are no longer used
            listOf("quests", "questDate", "streak", "lastCompleteDate", "prevCompleteDate", "worldX", "worldY", "steps", "replyLanguage")
                .forEach { remove(it) }
        }
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private fun questsToJson(quests: List<Quest>) = JSONArray().apply {
        quests.forEach { q ->
            put(JSONObject().put("id", q.id).put("title", q.title).put("emoji", q.emoji).put("xp", q.xp).put("done", q.done))
        }
    }

    private fun questsFromJson(arr: JSONArray): List<Quest> = List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        Quest(o.getString("id"), o.getString("title"), o.optString("emoji", "⭐"), o.optInt("xp", 20), o.optBoolean("done", false))
    }

    private fun outfitToJson(o: Outfit) = JSONObject()
        .put("name", o.name).put("hair", o.hair).put("shirt", o.shirt)
        .put("accent", o.accent).put("pants", o.pants).put("shoes", o.shoes)
        .put("skin", o.skin).put("stripe", o.stripe).put("striped", o.striped)
        .put("shortSleeve", o.shortSleeve).put("shorts", o.shorts).put("longHair", o.longHair)

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
        )
    }
}
