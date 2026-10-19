package com.today.app.data

/**
 * Tile map for the free-roam town.
 *  T tree   G grass   F flower   P path   W water
 *  R roof   H house wall   D door   S sign
 */
object WorldMap {
    val tiles: List<String> = listOf(
        "TTTTTTTTTTTTTT",
        "TGGGGFGGGGGGGT",
        "TGRRRGGGGRRRGT",
        "TGHDHGGGGHDHGT",
        "TGGPGGGGGGPGGT",
        "TGGPPPPPPPPGGT",
        "TGFGGGPGGGGGFT",
        "TGGGGGPGGWWWGT",
        "TPPPPPPGGWWWGT",
        "TGGSGGPGGWWWGT",
        "TGGGGGPGGGGGGT",
        "TGRRRGPGGFGGGT",
        "TGHDHGPPPPPPGT",
        "TGGPGGGGGGGPGT",
        "TGGPPPPPPPPPGT",
        "TGFGGGGGGGGFGT",
        "TGGGGGGGGGGGGT",
        "TTTTTTTTTTTTTT",
    )

    val cols = tiles.first().length
    val rows = tiles.size

    data class Npc(val name: String, val x: Int, val y: Int, val outfit: Outfit, val line: String)

    val npcs = listOf(
        Npc("코치 강", 7, 9, Outfits.presets[1], "오늘도 달려볼까? 조깅 퀘스트를 깨면 XP가 쏟아진다고!"),
        Npc("사서 윤", 5, 13, Outfits.presets[3], "꾸준함이 최고의 레벨업이야. 책 한 페이지라도 읽어봐."),
        Npc("낚시꾼 박", 12, 9, Outfits.presets[2], "물 8잔 마셨어? 연못처럼 촉촉하게 살아야지~"),
    )

    /** Messages shown when interacting with doors (keyed by "x,y") and signs. */
    val doorMessages = mapOf(
        "3,3" to "🏋️ 체육관 — 오늘의 운동 퀘스트를 완료하면 문이 열립니다!",
        "10,3" to "☕ 카페 — 코딩하기 좋은 자리가 남아 있어요.",
        "3,12" to "📚 도서관 — 조용히! 독서 퀘스트 진행 중인 사람들이 있어요.",
    )
    const val SIGN_MESSAGE = "🪧 TODAY 마을 — 오늘 할 일을 하면 내일의 내가 강해진다."

    const val START_X = 6
    const val START_Y = 10

    fun tileAt(x: Int, y: Int): Char = tiles.getOrNull(y)?.getOrNull(x) ?: 'T'

    fun isWalkable(x: Int, y: Int): Boolean =
        tileAt(x, y) in "GFP" && npcs.none { it.x == x && it.y == y }
}
