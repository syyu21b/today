package com.today.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class RewardsTest {

    private val d = LocalDate.of(2026, 10, 20)

    @Test
    fun levelRewardsAreGrantedOncePerNewLevel() {
        val s = RewardEngine.grantLevels(RewardState(), 2)
        assertEquals(2, s.rewardedLevel)
        assertEquals(1, s.shields)
        assertEquals(1, s.boxes)
        // Dropping back and re-reaching LV 2 (uncheck -> check) gives nothing.
        assertEquals(s, RewardEngine.grantLevels(s, 2))
        assertEquals(s, RewardEngine.grantLevels(s, 1))
        // Skipping levels grants each one.
        val jumped = RewardEngine.grantLevels(s, 5)
        assertEquals(4, jumped.shields)
        assertEquals(4, jumped.boxes)
    }

    @Test
    fun openingBoxesKeepsStateConsistent() {
        var s = RewardState(boxes = 500)
        val random = Random(42)
        repeat(500) {
            val before = s
            val (next, reward) = RewardEngine.openBox(s, random)!!
            assertEquals(before.boxes - 1, next.boxes)
            when (reward) {
                BoxReward.Shield -> assertEquals(before.shields + 1, next.shields)
                is BoxReward.Item -> {
                    assertTrue(Accessories.byId(reward.id)!!.unlockLevel == null)
                    if (reward.duplicate) assertEquals(before.stardust + RewardEngine.DUPLICATE_STARDUST, next.stardust)
                    else assertTrue(reward.id !in before.items && reward.id in next.items)
                }
                is BoxReward.Title -> {
                    assertTrue(Titles.byId(reward.id)!!.earned == null)
                    if (reward.duplicate) assertEquals(before.stardust + RewardEngine.DUPLICATE_STARDUST, next.stardust)
                    else assertTrue(reward.id !in before.titles && reward.id in next.titles)
                }
                is BoxReward.Stardust -> {
                    assertTrue(reward.amount in 1..3)
                    assertEquals(before.stardust + reward.amount, next.stardust)
                }
            }
            s = next
        }
        // 500 boxes are enough to own every box-only item and title.
        assertEquals(Accessories.boxOnly.map { it.id }.toSet(), s.items)
        assertTrue(s.titles.containsAll(Titles.boxOnly.map { it.id }))
        assertNull(RewardEngine.openBox(s.copy(boxes = 0)))
    }

    @Test
    fun stardustExchange() {
        assertNull(RewardEngine.exchangeStardust(RewardState(stardust = RewardEngine.SHIELD_PRICE - 1)))
        val s = RewardEngine.exchangeStardust(RewardState(stardust = RewardEngine.SHIELD_PRICE + 1, shields = 2))!!
        assertEquals(1, s.stardust)
        assertEquals(3, s.shields)
    }

    @Test
    fun shieldBridgesTheStreakWithoutCounting() {
        // Done on 16, 17, 19; 18 shielded; today (20) in progress.
        val complete = setOf(d.minusDays(4), d.minusDays(3), d.minusDays(1))
        val shielded = setOf(d.minusDays(2))
        assertEquals(3, Streaks.current(d, { it in complete }, { it in shielded }))
        assertEquals(1, Streaks.current(d, { it in complete }, { false }))
        // Shielding today keeps it without adding.
        assertEquals(3, Streaks.current(d, { it in complete }, { it in shielded || it == d }))
        assertEquals(3, Streaks.best(complete, shielded))
        assertEquals(2, Streaks.best(complete, emptySet()))
        assertEquals(0, Streaks.endingAt(d, { false }, { false }))
    }

    @Test
    fun titlesAndAccessoriesAreWellFormed() {
        assertEquals(Titles.all.size, Titles.all.map { it.id }.toSet().size)
        assertEquals(Accessories.all.size, Accessories.all.map { it.id }.toSet().size)
        assertEquals(setOf(Titles.DEFAULT), Titles.earnedBy(Progress(1, 0, 0, false)))
        val p = Progress(maxLevel = 10, bestStreak = 7, totalCompleted = 50, perfectDay = true)
        assertTrue(Titles.earnedBy(p).containsAll(listOf("level_10", "streak_7", "quests_50", "perfect_day")))
        assertTrue("streak_30" !in Titles.earnedBy(p))
        assertEquals("cap", Accessories.nextLevelUnlock(1)?.id)
        assertEquals("headphones", Accessories.nextLevelUnlock(3)?.id)
        assertNull(Accessories.nextLevelUnlock(10))
    }

    @Test
    fun accessoriesFollowTheCharacter() {
        val worn = Outfits.presets[0].withAccessory(AccessorySlot.HEAD, "crown").withAccessory(AccessorySlot.FACE, "glasses")
        val ocean = Outfits.presets[2].withAccessoriesOf(worn)
        assertEquals("crown", ocean.head)
        assertEquals("glasses", ocean.face)
        assertEquals(Outfits.presets[2], ocean.withoutAccessories())
        assertEquals("ocean", AppIconManager.iconKeyFor(ocean))
    }
}
