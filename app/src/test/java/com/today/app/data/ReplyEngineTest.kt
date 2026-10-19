package com.today.app.data

import com.today.app.data.ReplyEngine.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyEngineTest {

    private val hangul = Regex("[\\uAC00-\\uD7A3]")

    @Test
    fun detectsKorean() {
        assertEquals(Lang.KO, ReplyEngine.detect("안녕하세요"))
        assertEquals(Lang.KO, ReplyEngine.detect("ㅋㅋㅋ"))
        assertEquals(Lang.KO, ReplyEngine.detect("오늘 Kotlin 공부했어")) // mixed -> Korean
    }

    @Test
    fun detectsEnglish() {
        assertEquals(Lang.EN, ReplyEngine.detect("hello there"))
        assertEquals(Lang.EN, ReplyEngine.detect("What level are you?"))
    }

    @Test
    fun emojiOnlyKeepsPreviousLanguage() {
        assertEquals(Lang.EN, ReplyEngine.detect("🔥🔥", fallback = Lang.EN))
        assertEquals(Lang.KO, ReplyEngine.detect("👍", fallback = Lang.KO))
    }

    @Test
    fun koreanRepliesAreKorean() {
        repeat(50) {
            val r = ReplyEngine.reply("안녕! 오늘 퀘스트 했어?", Lang.KO, friendLevel = 9, myLevel = 3)
            assertTrue("expected Korean: $r", hangul.containsMatchIn(r))
        }
    }

    @Test
    fun englishRepliesAreEnglish() {
        repeat(50) {
            val r = ReplyEngine.reply("hello, did you finish your quests?", Lang.EN, friendLevel = 9, myLevel = 3)
            assertFalse("expected English: $r", hangul.containsMatchIn(r))
        }
    }

    @Test
    fun levelPlaceholdersAreFilled() {
        repeat(30) {
            val ko = ReplyEngine.reply("너 레벨 몇이야", Lang.KO, friendLevel = 12, myLevel = 4)
            assertFalse(ko.contains("{"))
            assertTrue(ko.contains("12") || ko.contains("4"))
            val en = ReplyEngine.reply("what's your level", Lang.EN, friendLevel = 12, myLevel = 4)
            assertFalse(en.contains("{"))
            assertTrue(en.contains("12") || en.contains("4"))
        }
    }

    @Test
    fun shortEnglishKeywordsMatchWholeWordsOnly() {
        // "this" contains "hi" but must not trigger the greeting rule
        val greetings = setOf("Hey! How many quests did you clear today? 😊", "Hi there! Leveling up today?")
        repeat(30) {
            assertFalse(ReplyEngine.reply("this is fine", Lang.EN, 1, 1) in greetings)
        }
    }
}
