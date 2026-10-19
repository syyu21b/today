package com.today.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Saved snapshot of everything that should survive an app restart. */
data class SavedState(
    val character: CharacterState,
    val quests: List<Quest>,
    val questDate: String?,
    val lastCompleteDate: String?,
    val prevCompleteDate: String?,
    val worldX: Int,
    val worldY: Int,
    val steps: Int,
)

/** Tiny SharedPreferences + JSON persistence (enough for a 2–3 day project, no Room needed). */
class TodayStorage(context: Context) {
    private val prefs = context.getSharedPreferences("today_prefs", Context.MODE_PRIVATE)

    fun load(): SavedState {
        val outfit = prefs.getString("outfit", null)?.let { outfitFromJson(JSONObject(it)) } ?: Outfits.presets.first()
        val quests = prefs.getString("quests", null)?.let { json ->
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Quest(o.getString("id"), o.getString("title"), o.getString("emoji"), o.getInt("xp"), o.getBoolean("done"))
            }
        } ?: DummyData.defaultQuests
        return SavedState(
            character = CharacterState(
                level = prefs.getInt("level", 1),
                xp = prefs.getInt("xp", 0),
                streak = prefs.getInt("streak", 0),
                outfit = outfit,
            ),
            quests = quests,
            questDate = prefs.getString("questDate", null),
            lastCompleteDate = prefs.getString("lastCompleteDate", null),
            prevCompleteDate = prefs.getString("prevCompleteDate", null),
            worldX = prefs.getInt("worldX", WorldMap.START_X),
            worldY = prefs.getInt("worldY", WorldMap.START_Y),
            steps = prefs.getInt("steps", 0),
        )
    }

    fun save(s: SavedState) {
        val quests = JSONArray().apply {
            s.quests.forEach { q ->
                put(JSONObject().put("id", q.id).put("title", q.title).put("emoji", q.emoji).put("xp", q.xp).put("done", q.done))
            }
        }
        prefs.edit()
            .putInt("level", s.character.level)
            .putInt("xp", s.character.xp)
            .putInt("streak", s.character.streak)
            .putString("outfit", outfitToJson(s.character.outfit).toString())
            .putString("quests", quests.toString())
            .putString("questDate", s.questDate)
            .putString("lastCompleteDate", s.lastCompleteDate)
            .putString("prevCompleteDate", s.prevCompleteDate)
            .putInt("worldX", s.worldX)
            .putInt("worldY", s.worldY)
            .putInt("steps", s.steps)
            .apply()
    }

    private fun outfitToJson(o: Outfit) = JSONObject()
        .put("name", o.name).put("hair", o.hair).put("shirt", o.shirt)
        .put("accent", o.accent).put("pants", o.pants).put("shoes", o.shoes)

    private fun outfitFromJson(j: JSONObject) = Outfit(
        j.getString("name"), j.getInt("hair"), j.getInt("shirt"),
        j.getInt("accent"), j.getInt("pants"), j.getInt("shoes"),
    )
}
