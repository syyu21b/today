package com.today.app.data

import com.today.app.i18n.EnStrings
import com.today.app.i18n.KoStrings
import com.today.app.i18n.josa
import com.today.app.notify.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FeaturesTest {

    private var ids = 0
    private fun id() = "q${ids++}"

    // ---------------- missions ----------------

    @Test
    fun missionsAreStableForADayAndStartWithAQuestMission() {
        val day = LocalDate.of(2026, 10, 21)
        val a = Missions.forDate(day, hasPet = true)
        assertEquals(a, Missions.forDate(day, hasPet = true))
        assertEquals(Missions.PER_DAY, a.size)
        assertEquals(a.size, a.toSet().size)
        assertTrue(a.first() in setOf(MissionType.COMPLETE_ALL, MissionType.COMPLETE_THREE))
        // Without a pet, the pet mission never shows up.
        repeat(60) { i -> assertFalse(MissionType.PET in Missions.forDate(day.plusDays(i.toLong()), hasPet = false)) }
    }

    @Test
    fun missionProgressCountsAndCaps() {
        val day = MissionDay("2026-10-21", petted = 5)
        assertEquals(3, Missions.progress(MissionType.PET, day, done = 0, active = 4))
        assertTrue(Missions.isDone(MissionType.PET, day, 0, 4))
        // "Complete 3" with only two quests asks for both.
        assertEquals(2, Missions.goal(MissionType.COMPLETE_THREE, 2))
        assertTrue(Missions.isDone(MissionType.COMPLETE_THREE, day, done = 2, active = 2))
        assertFalse(Missions.isDone(MissionType.COMPLETE_THREE, day, done = 0, active = 0))
        assertFalse(Missions.isDone(MissionType.COMPLETE_ALL, day, done = 0, active = 0))
        assertTrue(Missions.isDone(MissionType.COMPLETE_ALL, day, done = 3, active = 3))
    }

    // ---------------- season events ----------------

    @Test
    fun eventsCoverTheirDatesOnly() {
        assertEquals("halloween", SeasonEvents.active(LocalDate.of(2026, 10, 21))?.id)
        assertEquals("christmas", SeasonEvents.active(LocalDate.of(2026, 12, 25))?.id)
        assertEquals("newyear", SeasonEvents.active(LocalDate.of(2027, 1, 1))?.id)
        assertNull(SeasonEvents.active(LocalDate.of(2026, 6, 1)))
        // Every event item exists and is never in reward boxes.
        SeasonEvents.all.forEach { e ->
            val item = Accessories.byId(e.itemId)!!
            assertEquals(e.id, item.event)
            assertFalse(item in Accessories.boxOnly)
        }
    }

    @Test
    fun threeAllClearDaysGiveTheItemOnce() {
        val event = SeasonEvents.byId("halloween")!!
        val date = LocalDate.of(2026, 10, 21)
        var state = RewardState()
        repeat(2) { i ->
            val (next, result) = SeasonEvents.addDay(state, event, date)
            state = next
            assertEquals(SeasonEvents.DayResult.Progress(i + 1), result)
        }
        val (third, result) = SeasonEvents.addDay(state, event, date)
        assertEquals(SeasonEvents.DayResult.Item("witchhat"), result)
        assertTrue("witchhat" in third.items)
        assertEquals(SeasonEvents.DayResult.AlreadyFinished, SeasonEvents.addDay(third, event, date).second)
        // Next year, owning it already turns the reward into stardust.
        var nextYear = third
        repeat(SeasonEvents.GOAL_DAYS) { nextYear = SeasonEvents.addDay(nextYear, event, date.plusYears(1)).first }
        assertEquals(third.stardust + SeasonEvents.REPEAT_STARDUST, nextYear.stardust)
    }

    // ---------------- pets ----------------

    @Test
    fun petHatchesAndGrowsWithXp() {
        var pet = PetState(PetSpecies.HAMSTER, "Peanut")
        assertEquals(PetStage.EGG, pet.stage)
        val (hatched, reached) = Pets.grow(pet, 60)
        assertEquals(PetStage.BABY, reached)
        pet = hatched
        assertEquals(PetStage.BABY, pet.stage)
        assertNull(Pets.grow(pet, 10).second)
        assertEquals(PetStage.ADULT, Pets.grow(pet, 5000).first.stage)
        assertEquals(1f, Pets.grow(pet, 5000).first.stageProgress)
        // Unchecking can't push growth below zero.
        assertEquals(0, Pets.grow(pet, -9999).first.growth)
        assertEquals(7, PetSpecies.entries.size)
    }

    // ---------------- routine templates ----------------

    @Test
    fun templatesSkipDuplicatesAndRespectTheDayLimit() {
        val morning = Templates.byId("morning")!!
        val existing = listOf(Quest("a", morning.quests[0].ko, "⏰", 20))
        val added = Templates.questsFrom(listOf(morning), existing, AppLanguage.KOREAN, ::id)
        assertEquals(morning.quests.size - 1, added.size)
        val full = List(MAX_QUESTS_PER_DAY - 1) { Quest("x$it", "t$it", "⭐", 10) }
        assertEquals(1, Templates.questsFrom(Templates.all, full, AppLanguage.ENGLISH, ::id).size)
        // Every pack has a name and description in both languages, and every title translates.
        Templates.all.forEach { t ->
            assertTrue(KoStrings.templateName(t.id) != t.id && EnStrings.templateName(t.id) != t.id)
            assertTrue(EnStrings.templateDesc(t.id).isNotEmpty())
            t.quests.forEach { q ->
                assertEquals(q.en, DummyData.translateDefaultTitle(q.ko, AppLanguage.ENGLISH))
                assertTrue(q.xp in MIN_QUEST_XP..MAX_QUEST_XP)
                assertTrue(q.ko.length <= MAX_QUEST_TITLE && q.en.length <= MAX_QUEST_TITLE)
            }
        }
    }

    // ---------------- quest reminders ----------------

    @Test
    fun dueQuestsAreAnnouncedOncePerWindow() {
        val quests = listOf(
            Quest("1", "약", "💊", 20, remindAt = 8 * 60),
            Quest("2", "산책", "🚶", 20, remindAt = 9 * 60),
            Quest("3", "done", "✅", 20, done = true, remindAt = 8 * 60),
            Quest("4", "none", "⭐", 20),
        )
        assertEquals(listOf("1"), ReminderScheduler.dueQuests(quests, -1, 8 * 60 + 5).map { it.id })
        // Already announced up to 8:05: only the 9:00 one is new at 9:03.
        assertEquals(listOf("2"), ReminderScheduler.dueQuests(quests, 8 * 60 + 5, 9 * 60 + 3).map { it.id })
    }

    // ---------------- outfit body ----------------

    @Test
    fun presetsKeepTheBody() {
        val me = Outfits.presets[0].copy(hairStyle = HairStyle.BUN, features = setOf(FaceFeature.FRECKLES), skin = 0xFF8D5A3B.toInt(), head = "cap")
        val applied = Outfits.presets[2].withBodyOf(me)
        assertEquals(HairStyle.BUN, applied.style)
        assertEquals(setOf(FaceFeature.FRECKLES), applied.features)
        assertEquals(me.skin, applied.skin)
        assertEquals("cap", applied.head)
        assertEquals(Outfits.presets[2].shirt, applied.shirt)
        // Without a chosen style, the preset's hair length decides.
        assertEquals(HairStyle.LONG, Outfits.presets[4].style)
    }

    // ---------------- Korean particles ----------------

    @Test
    fun josaFollowsTheLastSyllable() {
        assertEquals("가", josa("나비", "이", "가"))
        assertEquals("이", josa("용용", "이", "가"))
        assertEquals("을", josa("용용", "을", "를"))
        assertEquals("로", josa("아기", "으로", "로"))
        assertEquals("으로", josa("어른", "으로", "로"))
        assertEquals("로", josa("물", "으로", "로"))
        assertEquals("이", josa("Buddy", "이", "가"))
    }

    // ---------------- character creator ----------------

    @Test
    fun looksChangeTheFaceButKeepTheClothes() {
        val me = Outfits.presets[1].copy(head = "cap", topStyle = TopStyle.JACKET)
        Looks.presets.forEach { look ->
            val o = look.applyTo(me)
            assertTrue(look.matches(o))
            assertEquals(me.shirt, o.shirt)
            assertEquals("cap", o.head)
            assertEquals(TopStyle.JACKET, o.topStyle)
            assertEquals(look.hairStyle.long, o.longHair)
        }
        val r = Looks.random(me, kotlin.random.Random(3))
        assertEquals(me.shirt, r.shirt)
        assertEquals(r.style.long, r.longHair)
        // Presets put on a look keep the face (body type, eyes...) too.
        val styled = Looks.presets[7].applyTo(me)
        assertEquals(styled.body, Outfits.presets[3].withBodyOf(styled).body)
        assertEquals(styled.eyeStyle, Outfits.presets[3].withBodyOf(styled).eyeStyle)
    }

    @Test
    fun fineTuneSlidersClampDropZeroAndSurvivePresets() {
        val me = Outfits.presets[0]
        val tuned = me.withTune(Tune.JAW, 3f).withTune(Tune.EYE_SIZE, -0.4f)
        assertEquals(1f, tuned.tuned(Tune.JAW))
        assertEquals(-0.4f, tuned.tuned(Tune.EYE_SIZE))
        // Back to (almost) zero = untouched again, so the creator sees no change.
        assertEquals(me, tuned.withTune(Tune.JAW, 0.01f).withTune(Tune.EYE_SIZE, 0f))
        // Outfit presets keep the sliders, face presets reset them.
        assertEquals(tuned.tune, Outfits.presets[2].withBodyOf(tuned).tune)
        assertTrue(Looks.presets[1].applyTo(tuned).tune.isEmpty())
        assertTrue(Looks.random(me, kotlin.random.Random(1)).tune.values.all { it in -1f..1f })
        // Every slider has a label in both languages and each art morph is named once.
        Tune.entries.forEach { assertTrue(KoStrings.tuneName(it).isNotEmpty() && EnStrings.tuneName(it).isNotEmpty()) }
        assertEquals(Tune.entries.mapNotNull { it.art }.size, Tune.entries.mapNotNull { it.art }.toSet().size)
    }

    @Test
    fun friendsGetAStableFaceUnlessTheyChoseOne() {
        val plain = Outfits.presets[2]
        val a = Looks.forFriend(plain, "f1")
        assertEquals(a, Looks.forFriend(plain, "f1"))
        assertEquals(plain.shirt, a.shirt)
        assertEquals(plain.hair, a.hair)
        assertEquals(plain.longHair, a.style.long)
        // Different friends don't all look alike.
        assertTrue(DummyData.friends.map { it.outfit.faceShape to it.outfit.eyeStyle }.toSet().size > 2)
        val chosen = plain.copy(eyeStyle = EyeStyle.ROUND)
        assertEquals(chosen, Looks.forFriend(chosen, "f1"))
    }
}
