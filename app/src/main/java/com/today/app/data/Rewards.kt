package com.today.app.data

import java.time.LocalDate
import kotlin.random.Random

// ======================= Accessories =======================

enum class AccessorySlot { HEAD, FACE, BACK }

/** How special an item is; [boxWeight] = relative chance among box-only items. Colors live in the UI. */
enum class Rarity(val boxWeight: Int) { COMMON(6), RARE(4), EPIC(2), LEGENDARY(1) }

/** Wearable item drawn over the sprite. [unlockLevel] null = only from reward boxes. */
data class Accessory(
    val id: String,
    val slot: AccessorySlot,
    val emoji: String,
    val unlockLevel: Int? = null,
    val rarity: Rarity = Rarity.COMMON,
)

object Accessories {
    val all = listOf(
        Accessory("cap", AccessorySlot.HEAD, "🧢", 2),
        Accessory("glasses", AccessorySlot.FACE, "👓", 3),
        Accessory("headphones", AccessorySlot.HEAD, "🎧", 5, Rarity.RARE),
        Accessory("cape", AccessorySlot.BACK, "🦸", 7, Rarity.EPIC),
        Accessory("crown", AccessorySlot.HEAD, "👑", 10, Rarity.LEGENDARY),
        Accessory("sunglasses", AccessorySlot.FACE, "🕶️", rarity = Rarity.RARE),
        Accessory("catears", AccessorySlot.HEAD, "🐱", rarity = Rarity.EPIC),
        Accessory("halo", AccessorySlot.HEAD, "😇", rarity = Rarity.LEGENDARY),
    )
    val boxOnly = all.filter { it.unlockLevel == null }

    /** A box-only item, rarer ones less often. */
    fun rollBoxItem(random: Random): Accessory {
        var roll = random.nextInt(boxOnly.sumOf { it.rarity.boxWeight })
        return boxOnly.first { roll -= it.rarity.boxWeight; roll < 0 }
    }

    fun byId(id: String) = all.firstOrNull { it.id == id }
    fun unlockedAt(level: Int) = all.filter { it.unlockLevel == level }

    /** The next level-unlock the player hasn't reached yet (for the "next reward" preview). */
    fun nextLevelUnlock(reachedLevel: Int) =
        all.filter { (it.unlockLevel ?: 0) > reachedLevel }.minByOrNull { it.unlockLevel!! }
}

// ======================= Backdrops =======================

/** Scene behind the character (home card + wardrobe stage), unlocked by level. Drawn in Backdrop.kt. */
data class BackdropDef(val id: String, val emoji: String, val unlockLevel: Int)

object Backdrops {
    const val DEFAULT = "default"

    val all = listOf(
        BackdropDef(DEFAULT, "🟪", 1),
        BackdropDef("sky", "☁️", 3),
        BackdropDef("sunset", "🌇", 5),
        BackdropDef("night", "🌙", 7),
        BackdropDef("space", "🪐", 10),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }
    fun unlockedAt(level: Int) = all.filter { it.unlockLevel == level && it.unlockLevel > 1 }
}

// ======================= Titles =======================

/** What titles are judged on. Everything here only grows, so an earned title stays earned. */
data class Progress(val maxLevel: Int, val bestStreak: Int, val totalCompleted: Int, val perfectDay: Boolean)

/** [earned] null = only from reward boxes. Names/descriptions live in Strings. */
data class TitleDef(val id: String, val emoji: String, val earned: ((Progress) -> Boolean)?)

object Titles {
    const val DEFAULT = "rookie"

    val all = listOf(
        TitleDef(DEFAULT, "🌱") { true },
        TitleDef("first_quest", "👣") { it.totalCompleted >= 1 },
        TitleDef("streak_3", "🔥") { it.bestStreak >= 3 },
        TitleDef("streak_7", "⚔️") { it.bestStreak >= 7 },
        TitleDef("streak_30", "🏆") { it.bestStreak >= 30 },
        TitleDef("level_5", "🌟") { it.maxLevel >= 5 },
        TitleDef("level_10", "💎") { it.maxLevel >= 10 },
        TitleDef("quests_50", "📜") { it.totalCompleted >= 50 },
        TitleDef("perfect_day", "💯") { it.perfectDay },
        TitleDef("lucky", "🍀", null),
        TitleDef("starlight", "✨", null),
    )
    val boxOnly = all.filter { it.earned == null }

    fun byId(id: String) = all.firstOrNull { it.id == id }

    /** Ids whose condition is met by [p] (box-only titles never are). */
    fun earnedBy(p: Progress): Set<String> = all.filter { it.earned?.invoke(p) == true }.map { it.id }.toSet()
}

// ======================= Reward state =======================

data class RewardState(
    /** Highest level whose rewards were handed out (= highest level ever reached). */
    val rewardedLevel: Int = 1,
    val shields: Int = 0,
    /** ISO dates protected by a streak shield. */
    val shieldedDates: Set<String> = emptySet(),
    val boxes: Int = 0,
    val stardust: Int = 0,
    /** Box-only accessories owned. */
    val items: Set<String> = emptySet(),
    /** Titles owned (kept even if a quest is unchecked later). */
    val titles: Set<String> = setOf(Titles.DEFAULT),
    val equippedTitle: String = Titles.DEFAULT,
    val backdrop: String = Backdrops.DEFAULT,
)

/** What came out of a reward box. */
sealed interface BoxReward {
    data object Shield : BoxReward
    data class Item(val id: String, val duplicate: Boolean) : BoxReward
    data class Title(val id: String, val duplicate: Boolean) : BoxReward
    data class Stardust(val amount: Int) : BoxReward
}

/** Shown by the level-up dialog; [rewarded] is false when re-reaching a level after unchecking. */
data class LevelUpEvent(val level: Int, val rewarded: Boolean)

/** Pure reward rules (no Android), so they are unit-tested directly. */
object RewardEngine {
    const val DUPLICATE_STARDUST = 2
    const val SHIELD_PRICE = 5

    /** One shield + one box for every level above the last rewarded one. Re-reaching a level gives nothing. */
    fun grantLevels(state: RewardState, level: Int): RewardState {
        if (level <= state.rewardedLevel) return state
        val n = level - state.rewardedLevel
        return state.copy(rewardedLevel = level, shields = state.shields + n, boxes = state.boxes + n)
    }

    /** Opens one box: 35% shield, 30% rare accessory, 15% rare title, 20% stardust. Duplicates turn into stardust. */
    fun openBox(state: RewardState, random: Random = Random.Default): Pair<RewardState, BoxReward>? {
        if (state.boxes <= 0) return null
        val s = state.copy(boxes = state.boxes - 1)
        val roll = random.nextInt(100)
        return when {
            roll < 35 -> s.copy(shields = s.shields + 1) to BoxReward.Shield
            roll < 65 -> {
                val id = Accessories.rollBoxItem(random).id
                if (id in s.items) s.copy(stardust = s.stardust + DUPLICATE_STARDUST) to BoxReward.Item(id, duplicate = true)
                else s.copy(items = s.items + id) to BoxReward.Item(id, duplicate = false)
            }
            roll < 80 -> {
                val id = Titles.boxOnly.random(random).id
                if (id in s.titles) s.copy(stardust = s.stardust + DUPLICATE_STARDUST) to BoxReward.Title(id, duplicate = true)
                else s.copy(titles = s.titles + id) to BoxReward.Title(id, duplicate = false)
            }
            else -> {
                val amount = 1 + random.nextInt(3)
                s.copy(stardust = s.stardust + amount) to BoxReward.Stardust(amount)
            }
        }
    }

    /** [SHIELD_PRICE] stardust -> one shield; null when there isn't enough. */
    fun exchangeStardust(state: RewardState): RewardState? =
        if (state.stardust < SHIELD_PRICE) null
        else state.copy(stardust = state.stardust - SHIELD_PRICE, shields = state.shields + 1)
}

// ======================= Streaks =======================

/**
 * A day "keeps" the streak when all its quests are done or it is shielded.
 * Only completed days add to the count; shielded days just bridge the gap.
 */
object Streaks {
    /** Streak ending at [day] (0 if [day] itself doesn't keep it). */
    fun endingAt(day: LocalDate, complete: (LocalDate) -> Boolean, shielded: (LocalDate) -> Boolean): Int {
        var d = day
        var count = 0
        while (complete(d) || shielded(d)) {
            if (complete(d)) count++
            d = d.minusDays(1)
        }
        return count
    }

    /** Ending today, or yesterday while today is still in progress. */
    fun current(today: LocalDate, complete: (LocalDate) -> Boolean, shielded: (LocalDate) -> Boolean): Int {
        val start = if (complete(today) || shielded(today)) today else today.minusDays(1)
        return endingAt(start, complete, shielded)
    }

    // From saved data (widget / reminders, no ViewModel): shielded and planned rest days bridge.
    private fun completeIn(byDate: Map<String, List<Quest>>) = { d: LocalDate -> byDate[d.toString()].orEmpty().completeOn(d) }
    private fun bridgedIn(byDate: Map<String, List<Quest>>, shieldedDates: Set<String>) =
        { d: LocalDate -> d.toString() in shieldedDates || byDate[d.toString()].orEmpty().isRestDayOn(d) }

    fun currentFor(byDate: Map<String, List<Quest>>, shieldedDates: Set<String>, today: LocalDate): Int =
        current(today, completeIn(byDate), bridgedIn(byDate, shieldedDates))

    fun endingAtFor(byDate: Map<String, List<Quest>>, shieldedDates: Set<String>, day: LocalDate): Int =
        endingAt(day, completeIn(byDate), bridgedIn(byDate, shieldedDates))

    /** Longest streak ever, from the set of completed days and shielded days. */
    fun best(completeDays: Set<LocalDate>, shieldedDays: Set<LocalDate>): Int {
        val kept = (completeDays + shieldedDays).sorted()
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in kept) {
            if (prev == null || d != prev.plusDays(1)) run = 0
            if (d in completeDays) run++
            best = maxOf(best, run)
            prev = d
        }
        return best
    }
}
