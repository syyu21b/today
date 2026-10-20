package com.today.app.i18n

import com.today.app.data.AppLanguage
import com.today.app.data.DummyData
import com.today.app.data.MAX_QUEST_XP
import com.today.app.data.Outfits
import com.today.app.data.StyleNote
import org.junit.Assert.assertEquals
import com.today.app.data.Accessories
import com.today.app.data.BoxReward
import com.today.app.data.Titles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class StringsTest {

    private val hangul = Regex("[\\uAC00-\\uD7A3]")
    private val date = LocalDate.of(2026, 10, 19)

    @Test
    fun englishTextHasNoKorean() {
        val e = EnStrings
        val samples = listOf(
            e.headerDate(date), e.shortDate(date), e.monthTitle(YearMonth.of(2026, 10)), e.chatTime(LocalTime.of(9, 5)),
            e.greeting("Sam"), e.clipboardBanner(date, 3), e.msgCopied(date, 1), e.pasteResult(2, 1, 1, 10),
            e.footer(10), e.deleteQuestBody("Run", true, 30), e.levelReached(3), e.msgStreak(2),
            e.weekdayShort(date.dayOfWeek), e.outfitName("ocean"), e.outfitName("포토 스타일 #2"),
        ) + e.analyzeSteps + StyleNote.entries.map { e.note(it) } + e.presetNames.values + listOf(
            e.notifSummary(true, "9:00 AM"), e.friendsSummary(3, 1), e.messagesSummary("Medium", true),
            e.questsSummary(true, false), e.dataSummary(4, 29), e.questReminderBody(2), e.streakReminderBody(1),
            e.msgContactsImported(3), e.groupFriendList(9), e.blockConfirmTitle("Sam"), e.deleteFriendTitle("Sam"),
            e.languageName(AppLanguage.KOREAN), e.notifPermissionDenied, e.contactsCardBody, e.privacyBody,
        ) + com.today.app.data.FriendSource.entries.map { e.friendSource(it) } +
            com.today.app.data.ChatFontSize.entries.map { e.fontSizeLabel(it) }
        samples.forEach { assertFalse("Korean in English text: $it", hangul.containsMatchIn(it)) }
    }

    @Test
    fun rewardTextExistsInBothLanguages() {
        val rewards = listOf(
            BoxReward.Shield, BoxReward.Stardust(2),
            BoxReward.Item("halo", duplicate = false), BoxReward.Item("halo", duplicate = true),
            BoxReward.Title("lucky", duplicate = false), BoxReward.Title("lucky", duplicate = true),
        )
        val e = EnStrings
        val english = rewards.map { e.boxRewardText(it) } + Titles.all.flatMap { listOf(e.titleLabel(it.id), e.titleHow(it.id), e.msgNewTitle(it)) } +
            Accessories.all.flatMap { listOf(e.accessoryLabel(it.id), e.accessoryLock(it), e.msgAccessoryLocked(it), e.newUnlock(it)) } +
            listOf(
                e.shieldDialogBody, e.msgNoStreakToProtect, e.shieldCount(2), e.rewardsTitle, e.statShields, e.statBoxes,
                e.statStardust, e.statTitles, e.statItems, e.boxesLeft(0), e.boxesLeft(1), e.boxesLeft(3), e.titleCollection(2, 11), e.stardustProgress(3, 5),
                e.shieldedDaysTitle, e.noShieldedDays,
                e.monthDelta(null), e.monthDelta(5), e.monthDelta(-3), e.monthDelta(0), e.dayDetail(date, 2, 5), e.dayDetail(date, 0, 0),
                e.bestWeekday(java.time.DayOfWeek.TUESDAY, 80), e.statsIntro, e.questRatesEmpty, e.statsTapHint, e.gestureHint, e.reorder, e.reorderDone, e.reorderHint,
                e.msgRestored("Run"), e.msgRestDay, e.widgetTodayLabel, e.widgetNoQuests, e.widgetOpen,
                e.rowWidget, e.widgetRowDesc, e.msgWidgetUnsupported, e.repeatLabel(emptySet()),
                e.repeatLabel(setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.FRIDAY)),
            ) + com.today.app.data.Mood.entries.map { e.moodGreeting("Sam", it) } + listOf( e.msgShieldUsed(3), e.rescueBanner(4), e.useShieldAction(1),
                e.stardustLine(2, 5), e.msgNotEnoughStardust(5), e.boxBanner(1), e.boxBanner(2), e.openAnother(2), e.levelRewards,
            )
        english.forEach { assertFalse("Korean in English text: $it", hangul.containsMatchIn(it)) }
        // Every id has a real name (the `else -> id` fallback is never hit).
        listOf(KoStrings, EnStrings).forEach { s ->
            Titles.all.forEach { assertNotEquals(it.id, s.titleName(it.id)) }
            Accessories.all.forEach { assertNotEquals(it.id, s.accessoryName(it.id)) }
        }
        assertEquals("🧢 캡모자", KoStrings.accessoryLabel("cap"))
        val mwf = setOf(java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY)
        assertEquals("월·수·금", KoStrings.repeatLabel(mwf))
        assertEquals("Mon·Wed·Fri", EnStrings.repeatLabel(mwf))
        assertEquals("매일", KoStrings.repeatLabel(emptySet()))
        assertEquals("10월 20일 (화) · 5개 중 2개 완료", KoStrings.dayDetail(java.time.LocalDate.of(2026, 10, 20), 2, 5))
    }

    @Test
    fun koreanDatesUseKorean() {
        assertEquals("10월 19일 (월)", KoStrings.shortDate(date))
        assertEquals("2026년 10월", KoStrings.monthTitle(YearMonth.of(2026, 10)))
        assertEquals("Mon, Oct 19", EnStrings.shortDate(date))
        assertEquals("October 2026", EnStrings.monthTitle(YearMonth.of(2026, 10)))
    }

    @Test
    fun bothLanguagesHaveSameShape() {
        assertEquals(KoStrings.analyzeSteps.size, EnStrings.analyzeSteps.size)
        assertEquals(KoStrings.presetNames.keys, EnStrings.presetNames.keys)
        assertEquals(Outfits.presets.map { it.name }.toSet(), EnStrings.presetNames.keys)
    }

    @Test
    fun legacyOutfitNamesAreLocalized() {
        assertEquals("ocean", Outfits.normalizeName("오션"))
        assertEquals("photo:3", Outfits.normalizeName("포토 스타일 #3"))
        assertEquals("Ocean", EnStrings.outfitName("오션"))
        assertEquals("Photo Style #3", EnStrings.outfitName("photo:3"))
        assertEquals("포토 스타일 #3", KoStrings.outfitName("photo:3"))
    }

    @Test
    fun pluralsInEnglish() {
        assertEquals("1 day", EnStrings.days(1))
        assertEquals("5 days", EnStrings.days(5))
        assertEquals("Nothing to paste", EnStrings.pasteResult(0, 0, 0, 10))
        assertEquals("Pasted 9 · skipped 1 over the 10-a-day limit", EnStrings.pasteResult(9, 0, 1, 10))
    }

    @Test
    fun defaultQuestsFollowLanguageAndXpCap() {
        var n = 0
        val ko = DummyData.defaultQuests(AppLanguage.KOREAN) { "${n++}" }
        val en = DummyData.defaultQuests(AppLanguage.ENGLISH) { "${n++}" }
        assertTrue(ko.all { hangul.containsMatchIn(it.title) || it.title.contains("Kotlin") })
        assertTrue(en.none { hangul.containsMatchIn(it.title) })
        assertTrue((ko + en).all { it.xp <= MAX_QUEST_XP })
        assertEquals("Read 20 min", DummyData.translateDefaultTitle("독서 20분", AppLanguage.ENGLISH))
        assertEquals("독서 20분", DummyData.translateDefaultTitle("Read 20 min", AppLanguage.KOREAN))
        assertNull(DummyData.translateDefaultTitle("My own quest", AppLanguage.KOREAN))
    }
}
