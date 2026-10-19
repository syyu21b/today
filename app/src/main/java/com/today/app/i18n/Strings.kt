package com.today.app.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.today.app.data.AppLanguage
import com.today.app.data.BodyPart
import com.today.app.data.ChatMessage
import com.today.app.data.Friend
import com.today.app.data.Outfits
import com.today.app.data.StyleNote
import com.today.app.data.StyleResult
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Every piece of user-visible text. Both languages implement this interface, so a missing
 * translation is a compile error rather than a stray Korean/English string at runtime.
 */
interface Strings {
    val language: AppLanguage
    val locale: Locale

    // ---- common ----
    val cancel: String
    val save: String
    val add: String
    val delete: String
    val close: String
    val back: String

    // ---- tabs ----
    val tabQuests: String
    val tabOutfit: String
    val tabMessages: String
    val tabSettings: String

    // ---- dates ----
    fun headerDate(date: LocalDate): String
    fun shortDate(date: LocalDate): String
    fun monthTitle(month: YearMonth): String
    fun weekdayShort(day: DayOfWeek): String = day.getDisplayName(TextStyle.SHORT, locale)
    fun chatTime(time: LocalTime): String
    fun days(n: Int): String

    // ---- home / quests ----
    fun greeting(nickname: String): String
    fun todayProgress(done: Int, total: Int): String
    val tagToday: String
    val tagPast: String
    val tagPlanned: String
    val copyDay: String
    val addQuest: String
    val hintPast: String
    val hintFuture: String
    fun clipboardBanner(date: LocalDate, count: Int): String
    val pasteHere: String
    val cancelCopy: String
    val emptyEditable: String
    val emptyPast: String
    fun footer(max: Int): String
    val questMenu: String
    val edit: String
    val deleteQuestTitle: String
    fun deleteQuestBody(title: String, done: Boolean, xp: Int): String
    val newQuest: String
    val editQuest: String
    val fieldTask: String
    val fieldEmoji: String
    val fieldXp: String
    fun levelReached(level: Int): String
    val levelUpBody: String
    val levelUpOk: String

    // ---- calendar ----
    val prevWeek: String
    val nextWeek: String
    val prevMonth: String
    val nextMonth: String
    val today: String
    val goToday: String
    val legendComplete: String
    val legendPartial: String
    val legendPlanned: String
    val legendMissed: String

    // ---- quest messages (snackbars / errors) ----
    val msgPastLocked: String
    val msgFutureCheck: String
    fun msgStreak(streak: Int): String
    val errAddPast: String
    fun errLimit(max: Int): String
    val errEmptyTitle: String
    val errEditPast: String
    val errNotFound: String
    val errDeletePast: String
    fun msgDeleted(title: String): String
    val msgNothingToCopy: String
    fun msgCopied(date: LocalDate, count: Int): String
    val errPastePast: String
    fun pasteResult(added: Int, duplicates: Int, overLimit: Int, max: Int): String

    // ---- wardrobe ----
    val outfitTitle: String
    val outfitIntro: String
    val takePhoto: String
    val fromGallery: String
    val photoTip: String
    val analyzing: String
    val analysisResult: String
    val closePhoto: String
    val analyzedPhoto: String
    val analyzeLoading: String
    val analyzeSteps: List<String>
    val fineTuneTitle: String
    val fineTuneHint: String
    val chipShortSleeve: String
    val chipStriped: String
    val chipShorts: String
    val chipLongHair: String
    val slotHair: String
    val slotSkin: String
    val slotTop: String
    val slotStripe: String
    val slotAccent: String
    val slotBottom: String
    val slotShoes: String
    val presetsTitle: String
    val presetNames: Map<String, String>
    fun photoStyleName(n: String): String
    fun bodyPart(part: BodyPart): String
    fun note(note: StyleNote): String
    val noPersonFound: String
    val resultPrefix: String
    val msgPhotoLoadFail: String
    fun msgStyleError(detail: String?): String
    val cameraNotFound: String
    fun cameraError(detail: String?): String

    // ---- messages ----
    val messagesTitle: String
    val search: String
    val typing: String
    fun startChat(level: Int): String
    fun youPrefix(text: String): String
    val activeNow: String
    val send: String
    val liked: String
    val messagePlaceholder: String

    // ---- settings ----
    val settingsTitle: String
    val editNickname: String
    fun nicknameLabel(max: Int): String
    val statStreak: String
    val statCompleted: String
    val statDays: String
    val customizeOutfit: String
    val sectionLanguage: String
    val languageHint: String
    val sectionQuests: String
    val autoCarryTitle: String
    val autoCarryDesc: String
    val sectionData: String
    val dataDesc: String
    val resetButton: String
    val resetTitle: String
    val resetBody: String
    val resetConfirm: String
    val msgReset: String
    val sectionAbout: String
    val aboutDesc: String
    val defaultNickname: String

    // ---- derived helpers (shared by both languages) ----
    fun outfitName(raw: String): String {
        val key = Outfits.normalizeName(raw)
        presetNames[key]?.let { return it }
        if (key.startsWith(Outfits.PHOTO_PREFIX)) return photoStyleName(key.removePrefix(Outfits.PHOTO_PREFIX))
        return raw
    }

    fun styleSummary(result: StyleResult): String =
        if (!result.personDetected) {
            noPersonFound + result.notes.joinToString("") { " · " + note(it) }
        } else {
            resultPrefix + result.notes.joinToString(" · ") { note(it) }
        }

    fun friendName(friend: Friend) = if (language == AppLanguage.KOREAN) friend.name else friend.nameEn

    fun messageText(m: ChatMessage) = if (language == AppLanguage.ENGLISH && m.textEn != null) m.textEn else m.text
}

object KoStrings : Strings {
    override val language = AppLanguage.KOREAN
    override val locale: Locale = Locale.KOREAN

    override val cancel = "취소"
    override val save = "저장"
    override val add = "추가"
    override val delete = "삭제"
    override val close = "닫기"
    override val back = "뒤로"

    override val tabQuests = "퀘스트"
    override val tabOutfit = "옷장"
    override val tabMessages = "메시지"
    override val tabSettings = "설정"

    override fun headerDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", locale))
    override fun shortDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("M월 d일 (E)", locale))
    override fun monthTitle(month: YearMonth) = "${month.year}년 ${month.monthValue}월"
    override fun chatTime(time: LocalTime): String = time.format(DateTimeFormatter.ofPattern("a h:mm", locale))
    override fun days(n: Int) = "${n}일"

    override fun greeting(nickname: String) = "${nickname}님, 오늘도 레벨업!"
    override fun todayProgress(done: Int, total: Int) = "오늘 진행: $done / $total"
    override val tagToday = "오늘"
    override val tagPast = "지난 기록"
    override val tagPlanned = "예정"
    override val copyDay = "이 날 복사"
    override val addQuest = "퀘스트 추가"
    override val hintPast = "지난 기록은 볼 수만 있어요. 복사해서 오늘이나 다른 날에 붙여넣을 수 있어요."
    override val hintFuture = "미리 계획한 퀘스트예요. 완료 체크는 당일에 할 수 있어요."
    override fun clipboardBanner(date: LocalDate, count: Int) = "${shortDate(date)} 퀘스트 ${count}개 복사됨"
    override val pasteHere = "여기에 붙여넣기"
    override val cancelCopy = "복사 취소"
    override val emptyEditable = "아직 퀘스트가 없어요.\n추가하거나 다른 날을 복사해서 붙여넣어 보세요."
    override val emptyPast = "이 날은 기록이 없어요."
    override fun footer(max: Int) = "⋮ 메뉴로 수정·삭제 · 하루 최대 ${max}개 · 모든 퀘스트 완료 시 스트릭 +1"
    override val questMenu = "퀘스트 메뉴"
    override val edit = "수정"
    override val deleteQuestTitle = "퀘스트 삭제"
    override fun deleteQuestBody(title: String, done: Boolean, xp: Int) =
        "'$title' 퀘스트를 삭제할까요?" + if (done) "\n이미 완료한 퀘스트라 받은 $xp XP도 회수돼요." else ""
    override val newQuest = "새 퀘스트"
    override val editQuest = "퀘스트 수정"
    override val fieldTask = "할 일"
    override val fieldEmoji = "이모지"
    override val fieldXp = "XP"
    override fun levelReached(level: Int) = "LV $level 달성!"
    override val levelUpBody = "오늘의 내가 어제의 나보다 강해졌어요."
    override val levelUpOk = "좋아!"

    override val prevWeek = "이전 주"
    override val nextWeek = "다음 주"
    override val prevMonth = "이전 달"
    override val nextMonth = "다음 달"
    override val today = "오늘"
    override val goToday = "오늘로 이동"
    override val legendComplete = "완료"
    override val legendPartial = "진행 중"
    override val legendPlanned = "예정"
    override val legendMissed = "미완료"

    override val msgPastLocked = "지난 날의 기록은 바꿀 수 없어요"
    override val msgFutureCheck = "예정된 퀘스트는 그날 체크할 수 있어요"
    override fun msgStreak(streak: Int) = "🔥 오늘의 퀘스트 완료! 스트릭 ${streak}일"
    override val errAddPast = "지난 날에는 퀘스트를 추가할 수 없어요"
    override fun errLimit(max: Int) = "하루에 최대 ${max}개까지 추가할 수 있어요"
    override val errEmptyTitle = "할 일을 입력해 주세요"
    override val errEditPast = "지난 날의 퀘스트는 수정할 수 없어요"
    override val errNotFound = "퀘스트를 찾을 수 없어요"
    override val errDeletePast = "지난 날의 퀘스트는 삭제할 수 없어요"
    override fun msgDeleted(title: String) = "'$title' 삭제됨"
    override val msgNothingToCopy = "복사할 퀘스트가 없어요"
    override fun msgCopied(date: LocalDate, count: Int) = "${shortDate(date)} 퀘스트 ${count}개 복사됨 · 붙여넣을 날짜를 고르세요"
    override val errPastePast = "지난 날에는 붙여넣을 수 없어요"
    override fun pasteResult(added: Int, duplicates: Int, overLimit: Int, max: Int): String {
        val parts = buildList {
            if (added > 0) add("${added}개 붙여넣음")
            if (duplicates > 0) add("중복 ${duplicates}개 제외")
            if (overLimit > 0) add("하루 최대 ${max}개 제한으로 ${overLimit}개 제외")
        }
        return if (parts.isEmpty()) "붙여넣을 퀘스트가 없어요" else parts.joinToString(" · ")
    }

    override val outfitTitle = "옷장"
    override val outfitIntro = "전신 사진을 찍거나 고르면 머리·상의·하의·신발을 인식해서 도트 의상으로 만들어요. 바꾼 스타일은 퀘스트와 프로필 캐릭터에도 바로 적용돼요."
    override val takePhoto = "사진 찍기"
    override val fromGallery = "앨범에서"
    override val photoTip = "💡 정면 전신 사진 · 밝은 곳 · 단순한 배경일수록 정확해요"
    override val analyzing = "인식 중…"
    override val analysisResult = "인식 결과"
    override val closePhoto = "사진 닫기"
    override val analyzedPhoto = "분석한 사진"
    override val analyzeLoading = "사진 불러오는 중…"
    override val analyzeSteps = listOf("사람 찾는 중…", "머리·상의·하의 영역 나누는 중…", "옷 색감 추출 중…", "도트 의상 생성 중…")
    override val fineTuneTitle = "세부 조정"
    override val fineTuneHint = "인식 결과가 조금 다르면 여기서 바로 고칠 수 있어요"
    override val chipShortSleeve = "반팔"
    override val chipStriped = "줄무늬"
    override val chipShorts = "반바지"
    override val chipLongHair = "긴 머리"
    override val slotHair = "머리"
    override val slotSkin = "피부"
    override val slotTop = "상의"
    override val slotStripe = "줄무늬"
    override val slotAccent = "포인트"
    override val slotBottom = "하의"
    override val slotShoes = "신발"
    override val presetsTitle = "프리셋 의상"
    override val presetNames = mapOf("default" to "기본", "street" to "스트릿", "ocean" to "오션", "forest" to "포레스트", "sunset" to "선셋", "neon" to "네온")
    override fun photoStyleName(n: String) = "포토 스타일 #$n"
    override fun bodyPart(part: BodyPart) = when (part) {
        BodyPart.HAIR -> "머리"
        BodyPart.SKIN -> "피부"
        BodyPart.TOP -> "상의"
        BodyPart.BOTTOM -> "하의"
        BodyPart.SHOES -> "신발"
        BodyPart.OUTFIT -> "의상"
    }
    override fun note(note: StyleNote) = when (note) {
        StyleNote.BACK_VIEW -> "뒷모습"
        StyleNote.FACE_HIDDEN -> "얼굴 미인식"
        StyleNote.LONG_HAIR -> "긴 머리"
        StyleNote.STRIPED -> "줄무늬 상의"
        StyleNote.SHORT_SLEEVE -> "반팔·민소매"
        StyleNote.LONG_SLEEVE -> "긴팔"
        StyleNote.SHORTS -> "반바지"
        StyleNote.LONG_PANTS -> "긴바지"
        StyleNote.UPPER_BODY_ONLY -> "상반신만 인식"
    }
    override val noPersonFound = "사람을 찾지 못해 사진 중앙의 옷 색감으로 만들었어요"
    override val resultPrefix = "인식 결과: "
    override val msgPhotoLoadFail = "사진을 불러오지 못했어요"
    override fun msgStyleError(detail: String?) = "스타일 분석 중 문제가 생겼어요: ${detail ?: "알 수 없는 오류"}"
    override val cameraNotFound = "사용할 수 있는 카메라 앱이 없어요"
    override fun cameraError(detail: String?) = "카메라를 열 수 없어요: ${detail ?: "알 수 없는 오류"}"

    override val messagesTitle = "메시지"
    override val search = "검색"
    override val typing = "입력 중…"
    override fun startChat(level: Int) = "LV $level · 새 대화를 시작해 보세요"
    override fun youPrefix(text: String) = "나: $text"
    override val activeNow = "활동 중"
    override val send = "보내기"
    override val liked = "좋아요"
    override val messagePlaceholder = "메시지 보내기…"

    override val settingsTitle = "설정"
    override val editNickname = "닉네임 수정"
    override fun nicknameLabel(max: Int) = "닉네임 (최대 ${max}자)"
    override val statStreak = "스트릭"
    override val statCompleted = "완료 퀘스트"
    override val statDays = "기록한 날"
    override val customizeOutfit = "옷장에서 꾸미기"
    override val sectionLanguage = "언어"
    override val languageHint = "앱의 모든 글자가 선택한 언어로 바뀌어요. 메시지는 입력한 언어로 친구가 답장해요."
    override val sectionQuests = "퀘스트"
    override val autoCarryTitle = "새로운 날 퀘스트 자동 불러오기"
    override val autoCarryDesc = "오늘 퀘스트가 비어 있으면 가장 최근 날의 퀘스트를 가져와요"
    override val sectionData = "데이터"
    override val dataDesc = "모든 퀘스트 기록, 레벨, 의상, 설정이 기기에만 저장돼요."
    override val resetButton = "모든 데이터 초기화"
    override val resetTitle = "모든 데이터 초기화"
    override val resetBody = "퀘스트 기록, 레벨, 의상, 설정이 모두 지워지고 되돌릴 수 없어요. 계속할까요?"
    override val resetConfirm = "초기화"
    override val msgReset = "모든 데이터가 초기화되었어요"
    override val sectionAbout = "앱 정보"
    override val aboutDesc = "오늘의 루틴을 퀘스트로 바꿔 캐릭터를 키우는 습관 트래커"
    override val defaultNickname = "모험가"
}

object EnStrings : Strings {
    override val language = AppLanguage.ENGLISH
    override val locale: Locale = Locale.ENGLISH

    override val cancel = "Cancel"
    override val save = "Save"
    override val add = "Add"
    override val delete = "Delete"
    override val close = "Close"
    override val back = "Back"

    override val tabQuests = "Quests"
    override val tabOutfit = "Wardrobe"
    override val tabMessages = "Messages"
    override val tabSettings = "Settings"

    override fun headerDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale))
    override fun shortDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))
    override fun monthTitle(month: YearMonth): String = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    override fun chatTime(time: LocalTime): String = time.format(DateTimeFormatter.ofPattern("h:mm a", locale))
    override fun days(n: Int) = if (n == 1) "1 day" else "$n days"

    override fun greeting(nickname: String) = "Level up today, $nickname!"
    override fun todayProgress(done: Int, total: Int) = "Today: $done / $total"
    override val tagToday = "Today"
    override val tagPast = "Past"
    override val tagPlanned = "Planned"
    override val copyDay = "Copy day"
    override val addQuest = "Add quest"
    override val hintPast = "Past days are read-only. Copy them to paste into today or another day."
    override val hintFuture = "Planned ahead. You can check these off on the day itself."
    override fun clipboardBanner(date: LocalDate, count: Int) = "Copied $count ${quests(count)} from ${shortDate(date)}"
    override val pasteHere = "Paste here"
    override val cancelCopy = "Cancel copy"
    override val emptyEditable = "No quests yet.\nAdd one, or copy another day and paste it here."
    override val emptyPast = "Nothing was logged on this day."
    override fun footer(max: Int) = "⋮ menu to edit or delete · up to $max a day · finish all for streak +1"
    override val questMenu = "Quest menu"
    override val edit = "Edit"
    override val deleteQuestTitle = "Delete quest"
    override fun deleteQuestBody(title: String, done: Boolean, xp: Int) =
        "Delete '$title'?" + if (done) "\nIt's already completed, so its $xp XP will be taken back." else ""
    override val newQuest = "New quest"
    override val editQuest = "Edit quest"
    override val fieldTask = "Task"
    override val fieldEmoji = "Emoji"
    override val fieldXp = "XP"
    override fun levelReached(level: Int) = "Reached LV $level!"
    override val levelUpBody = "Today's you is stronger than yesterday's."
    override val levelUpOk = "Awesome!"

    override val prevWeek = "Previous week"
    override val nextWeek = "Next week"
    override val prevMonth = "Previous month"
    override val nextMonth = "Next month"
    override val today = "Today"
    override val goToday = "Go to today"
    override val legendComplete = "Done"
    override val legendPartial = "In progress"
    override val legendPlanned = "Planned"
    override val legendMissed = "Missed"

    override val msgPastLocked = "Past days can't be changed"
    override val msgFutureCheck = "Planned quests can be checked on that day"
    override fun msgStreak(streak: Int) = "🔥 All quests done! Streak: ${days(streak)}"
    override val errAddPast = "You can't add quests to past days"
    override fun errLimit(max: Int) = "You can add up to $max quests a day"
    override val errEmptyTitle = "Please enter a task"
    override val errEditPast = "Past quests can't be edited"
    override val errNotFound = "Quest not found"
    override val errDeletePast = "Past quests can't be deleted"
    override fun msgDeleted(title: String) = "Deleted '$title'"
    override val msgNothingToCopy = "No quests to copy"
    override fun msgCopied(date: LocalDate, count: Int) = "Copied $count ${quests(count)} from ${shortDate(date)} · pick a day to paste"
    override val errPastePast = "You can't paste into past days"
    override fun pasteResult(added: Int, duplicates: Int, overLimit: Int, max: Int): String {
        val parts = buildList {
            if (added > 0) add("Pasted $added")
            if (duplicates > 0) add("skipped $duplicates duplicate${if (duplicates == 1) "" else "s"}")
            if (overLimit > 0) add("skipped $overLimit over the $max-a-day limit")
        }
        return if (parts.isEmpty()) "Nothing to paste" else parts.joinToString(" · ").replaceFirstChar { it.uppercase() }
    }

    override val outfitTitle = "Wardrobe"
    override val outfitIntro = "Take or pick a full-body photo and we'll detect hair, top, bottom and shoes to make a pixel outfit. Changes apply to your quest and profile character right away."
    override val takePhoto = "Take photo"
    override val fromGallery = "From gallery"
    override val photoTip = "💡 Front-facing full-body shots, good light and a plain background work best"
    override val analyzing = "Analyzing…"
    override val analysisResult = "Result"
    override val closePhoto = "Close photo"
    override val analyzedPhoto = "Analyzed photo"
    override val analyzeLoading = "Loading photo…"
    override val analyzeSteps = listOf("Finding the person…", "Splitting hair, top and bottom…", "Extracting outfit colors…", "Building pixel outfit…")
    override val fineTuneTitle = "Fine-tune"
    override val fineTuneHint = "If the result is a bit off, fix it right here"
    override val chipShortSleeve = "Short sleeves"
    override val chipStriped = "Stripes"
    override val chipShorts = "Shorts"
    override val chipLongHair = "Long hair"
    override val slotHair = "Hair"
    override val slotSkin = "Skin"
    override val slotTop = "Top"
    override val slotStripe = "Stripes"
    override val slotAccent = "Accent"
    override val slotBottom = "Bottom"
    override val slotShoes = "Shoes"
    override val presetsTitle = "Preset outfits"
    override val presetNames = mapOf("default" to "Classic", "street" to "Street", "ocean" to "Ocean", "forest" to "Forest", "sunset" to "Sunset", "neon" to "Neon")
    override fun photoStyleName(n: String) = "Photo Style #$n"
    override fun bodyPart(part: BodyPart) = when (part) {
        BodyPart.HAIR -> "Hair"
        BodyPart.SKIN -> "Skin"
        BodyPart.TOP -> "Top"
        BodyPart.BOTTOM -> "Bottom"
        BodyPart.SHOES -> "Shoes"
        BodyPart.OUTFIT -> "Outfit"
    }
    override fun note(note: StyleNote) = when (note) {
        StyleNote.BACK_VIEW -> "back view"
        StyleNote.FACE_HIDDEN -> "face not visible"
        StyleNote.LONG_HAIR -> "long hair"
        StyleNote.STRIPED -> "striped top"
        StyleNote.SHORT_SLEEVE -> "short sleeves / sleeveless"
        StyleNote.LONG_SLEEVE -> "long sleeves"
        StyleNote.SHORTS -> "shorts"
        StyleNote.LONG_PANTS -> "long pants"
        StyleNote.UPPER_BODY_ONLY -> "upper body only"
    }
    override val noPersonFound = "No person found, so we used the clothing colors in the middle of the photo"
    override val resultPrefix = "Detected: "
    override val msgPhotoLoadFail = "Couldn't load the photo"
    override fun msgStyleError(detail: String?) = "Something went wrong while analyzing: ${detail ?: "unknown error"}"
    override val cameraNotFound = "No camera app available"
    override fun cameraError(detail: String?) = "Couldn't open the camera: ${detail ?: "unknown error"}"

    override val messagesTitle = "Messages"
    override val search = "Search"
    override val typing = "typing…"
    override fun startChat(level: Int) = "LV $level · Start a conversation"
    override fun youPrefix(text: String) = "You: $text"
    override val activeNow = "Active now"
    override val send = "Send"
    override val liked = "Liked"
    override val messagePlaceholder = "Message…"

    override val settingsTitle = "Settings"
    override val editNickname = "Edit nickname"
    override fun nicknameLabel(max: Int) = "Nickname (max $max)"
    override val statStreak = "Streak"
    override val statCompleted = "Completed"
    override val statDays = "Days logged"
    override val customizeOutfit = "Customize in wardrobe"
    override val sectionLanguage = "Language"
    override val languageHint = "Changes all text in the app. Friends reply in the language you type."
    override val sectionQuests = "Quests"
    override val autoCarryTitle = "Carry quests over to a new day"
    override val autoCarryDesc = "If today is empty, bring in the most recent day's quests"
    override val sectionData = "Data"
    override val dataDesc = "All quest history, levels, outfits and settings stay on this device."
    override val resetButton = "Reset all data"
    override val resetTitle = "Reset all data"
    override val resetBody = "This erases your quest history, level, outfit and settings. It can't be undone. Continue?"
    override val resetConfirm = "Reset"
    override val msgReset = "All data has been reset"
    override val sectionAbout = "About"
    override val aboutDesc = "A habit tracker that turns today's routines into quests to level up your character"
    override val defaultNickname = "Adventurer"

    private fun quests(n: Int) = if (n == 1) "quest" else "quests"
}

fun AppLanguage.strings(): Strings = when (this) {
    AppLanguage.KOREAN -> KoStrings
    AppLanguage.ENGLISH -> EnStrings
}

/** Current UI strings, provided at the root from the saved language setting. */
val LocalStrings = staticCompositionLocalOf<Strings> { KoStrings }
