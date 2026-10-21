package com.today.app.data

import java.time.LocalDate
import java.time.MonthDay
import kotlin.random.Random

// ======================= Daily missions =======================

/** [goal] is how many times it must happen today; [xp] is the bonus for claiming it. */
enum class MissionType(val goal: Int, val xp: Int) {
    COMPLETE_THREE(3, 15),
    COMPLETE_ALL(1, 25),
    MORNING(1, 15),
    PET(3, 10),
    CHAT(1, 10),
    ADD_QUEST(1, 10),
    STATUS(1, 10),
}

/** Today's counters behind the missions; a new date starts from zero. */
data class MissionDay(
    val date: String,
    val petted: Int = 0,
    val chats: Int = 0,
    val added: Int = 0,
    /** A quest was checked before noon. */
    val morning: Boolean = false,
    /** The profile status was updated. */
    val status: Boolean = false,
    val claimed: Set<MissionType> = emptySet(),
    /** The all-clear bonus (box + event day) was handed out. */
    val bonusClaimed: Boolean = false,
)

object Missions {
    const val PER_DAY = 3
    const val BONUS_BOXES = 1

    /** Same three missions all day: one quest mission plus two others, picked from the date. */
    fun forDate(date: LocalDate, hasPet: Boolean): List<MissionType> {
        val r = Random(date.toEpochDay())
        val main = if (r.nextBoolean()) MissionType.COMPLETE_ALL else MissionType.COMPLETE_THREE
        val pool = buildList {
            addAll(listOf(MissionType.MORNING, MissionType.CHAT, MissionType.ADD_QUEST, MissionType.STATUS))
            if (hasPet) add(MissionType.PET)
        }
        return listOf(main) + pool.shuffled(r).take(PER_DAY - 1)
    }

    /** With fewer than three quests today, "complete 3" asks for all of them. */
    fun goal(type: MissionType, activeQuests: Int) =
        if (type == MissionType.COMPLETE_THREE) activeQuests.coerceIn(1, type.goal) else type.goal

    fun progress(type: MissionType, day: MissionDay, done: Int, active: Int): Int = when (type) {
        MissionType.COMPLETE_THREE -> done
        MissionType.COMPLETE_ALL -> if (active > 0 && done == active) 1 else 0
        MissionType.MORNING -> if (day.morning) 1 else 0
        MissionType.PET -> day.petted
        MissionType.CHAT -> day.chats
        MissionType.ADD_QUEST -> day.added
        MissionType.STATUS -> if (day.status) 1 else 0
    }.coerceAtMost(goal(type, active))

    fun isDone(type: MissionType, day: MissionDay, done: Int, active: Int) =
        (type != MissionType.COMPLETE_THREE || active > 0) && progress(type, day, done, active) >= goal(type, active)
}

// ======================= Season events =======================

/** A limited-time event: clear every daily mission on [SeasonEvents.GOAL_DAYS] days to get [itemId]. */
data class SeasonEvent(val id: String, val emoji: String, val start: MonthDay, val end: MonthDay, val itemId: String) {
    fun isActive(date: LocalDate) = MonthDay.from(date) in start..end
    fun key(date: LocalDate) = "$id-${date.year}"
    fun lastDay(date: LocalDate): LocalDate = end.atYear(date.year)
}

object SeasonEvents {
    const val GOAL_DAYS = 3
    /** Owning the item already (from an earlier year): stardust instead. */
    const val REPEAT_STARDUST = 5

    val all = listOf(
        SeasonEvent("newyear", "🎉", MonthDay.of(1, 1), MonthDay.of(1, 31), "partyhat"),
        SeasonEvent("spring", "🌸", MonthDay.of(3, 15), MonthDay.of(4, 30), "flowercrown"),
        SeasonEvent("summer", "🌊", MonthDay.of(7, 1), MonthDay.of(8, 31), "strawhat"),
        SeasonEvent("halloween", "🎃", MonthDay.of(10, 15), MonthDay.of(11, 7), "witchhat"),
        SeasonEvent("christmas", "🎄", MonthDay.of(12, 1), MonthDay.of(12, 31), "santahat"),
    )

    fun active(date: LocalDate) = all.firstOrNull { it.isActive(date) }
    fun byId(id: String) = all.firstOrNull { it.id == id }

    /** What one all-clear day during [event] gives. */
    sealed interface DayResult {
        data class Progress(val days: Int) : DayResult
        data class Item(val id: String) : DayResult
        data class Stardust(val amount: Int) : DayResult
        data object AlreadyFinished : DayResult
    }

    fun addDay(state: RewardState, event: SeasonEvent, date: LocalDate): Pair<RewardState, DayResult> {
        val key = event.key(date)
        val before = state.eventProgress[key] ?: 0
        if (before >= GOAL_DAYS) return state to DayResult.AlreadyFinished
        val days = before + 1
        val s = state.copy(eventProgress = state.eventProgress + (key to days))
        return when {
            days < GOAL_DAYS -> s to DayResult.Progress(days)
            event.itemId in s.items -> s.copy(stardust = s.stardust + REPEAT_STARDUST) to DayResult.Stardust(REPEAT_STARDUST)
            else -> s.copy(items = s.items + event.itemId) to DayResult.Item(event.itemId)
        }
    }
}
