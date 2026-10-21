package com.today.app.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.today.app.data.Accessories
import com.today.app.data.Accessory
import com.today.app.data.BackdropDef
import com.today.app.data.Rarity
import com.today.app.data.AppLanguage
import com.today.app.data.BodyPart
import com.today.app.data.BoxReward
import com.today.app.data.ChatMessage
import com.today.app.data.Friend
import com.today.app.data.Mood
import com.today.app.data.Outfits
import com.today.app.data.RewardEngine
import com.today.app.data.StyleNote
import com.today.app.data.StyleResult
import com.today.app.data.TitleDef
import com.today.app.data.Titles
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
interface Strings : FeatureStrings {
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

    // ---- settings dashboard ----
    val on: String
    val off: String
    val ok: String
    val groupGeneral: String
    fun languageName(lang: AppLanguage): String
    val groupSocial: String
    val groupActivity: String
    val groupMore: String
    val rowNotifications: String
    val rowFriends: String
    fun notifSummary(enabled: Boolean, questReminderTime: String?): String
    fun friendsSummary(count: Int, blocked: Int): String
    fun messagesSummary(fontSize: String, autoReplies: Boolean): String
    fun questsSummary(autoCarry: Boolean, weekStartsMonday: Boolean): String
    fun dataSummary(days: Int, quests: Int): String
    fun versionSummary(version: String): String

    // ---- notification settings ----
    val notifMaster: String
    val notifMasterDesc: String
    val notifPermissionDenied: String
    val openSystemSettings: String
    val groupReminders: String
    val questReminder: String
    val questReminderDesc: String
    val streakReminder: String
    val streakReminderDesc: String
    val reminderTime: String
    val pickTime: String
    val groupMessageAlerts: String
    val messageNotif: String
    val messageNotifDesc: String
    val messagePreview: String
    val messagePreviewDesc: String
    val testNotification: String
    val channelReminders: String
    val channelMessages: String
    val questReminderTitle: String
    fun questReminderBody(remaining: Int): String
    val streakReminderTitle: String
    fun streakReminderBody(remaining: Int): String
    val newMessageHidden: String
    val testNotificationTitle: String
    val testNotificationBody: String

    // ---- friend settings ----
    val contactsCardTitle: String
    val contactsCardBody: String
    val contactsImport: String
    val contactsLater: String
    val groupContacts: String
    val importFromContacts: String
    val contactsAllowed: String
    val contactsNotAllowed: String
    val contactsDenied: String
    val contactsEmpty: String
    val contactsPickerTitle: String
    val searchName: String
    fun addSelected(n: Int): String
    val alreadyFriend: String
    fun msgContactsImported(n: Int): String
    fun groupFriendList(n: Int): String
    val addFriend: String
    val fieldFriendName: String
    val fieldFriendHandle: String
    val errFriendName: String
    val errFriendDuplicate: String
    fun errFriendLimit(max: Int): String
    fun msgFriendAdded(name: String): String
    fun friendSource(source: com.today.app.data.FriendSource): String
    val friendMenu: String
    val sendMessageAction: String
    val block: String
    val unblock: String
    val deleteFriend: String
    fun blockConfirmTitle(name: String): String
    val blockConfirmBody: String
    fun deleteFriendTitle(name: String): String
    val deleteFriendBody: String
    fun msgBlocked(name: String): String
    fun msgUnblocked(name: String): String
    fun msgFriendDeleted(name: String): String
    fun groupBlocked(n: Int): String
    val noBlocked: String
    val noFriends: String

    // ---- message settings ----
    val groupChat: String
    val chatFontSize: String
    fun fontSizeLabel(size: com.today.app.data.ChatFontSize): String
    val enterToSend: String
    val enterToSendDesc: String
    val readReceipts: String
    val readReceiptsDesc: String
    val read: String
    val autoReplies: String
    val autoRepliesDesc: String
    val groupHistory: String
    val saveHistory: String
    val saveHistoryDesc: String
    val clearChats: String
    val clearChatsBody: String
    val msgChatsCleared: String
    val manageBlocked: String
    val chatPreviewSample: String

    // ---- quest settings ----
    val weekStart: String
    val sunday: String
    val monday: String
    val celebration: String
    val celebrationDesc: String
    val defaultXp: String
    val defaultXpDesc: String
    val questReminderLink: String

    // ---- data settings ----
    val groupStorage: String
    val statQuestsTotal: String
    val statFriends: String
    val statMessages: String
    val exportData: String
    val exportDesc: String
    val exportDone: String
    val exportFailed: String

    // ---- about ----
    val version: String
    val builtWith: String
    val privacyTitle: String
    val privacyBody: String
    val permissionsTitle: String
    val permNotifications: String
    val permContacts: String
    val permCamera: String
    val permCameraDesc: String
    val permGranted: String
    val permDenied: String
    val licenses: String

    // ---- rewards ----
    val shieldName: String
    val shieldDialogBody: String
    fun shieldCount(n: Int): String
    val useShieldToday: String
    val useShieldYesterday: String
    fun msgShieldUsed(streak: Int): String
    val msgNoShield: String
    val msgNoStreakToProtect: String
    val errShieldNotAllowed: String
    fun rescueBanner(streak: Int): String
    val rescueButton: String
    fun useShieldAction(shields: Int): String
    fun stardustLine(n: Int, price: Int): String
    val exchange: String
    val msgExchanged: String
    fun msgNotEnoughStardust(price: Int): String
    val legendShielded: String
    fun boxBanner(n: Int): String
    val openBox: String
    val boxTitle: String
    val boxOpening: String
    fun boxRewardText(r: BoxReward): String
    fun openAnother(n: Int): String
    val levelRewards: String
    fun newUnlock(acc: Accessory): String
    fun nextUnlock(acc: Accessory): String
    fun titleName(id: String): String
    fun titleHow(id: String): String
    val titlePickerTitle: String
    val titleEquipped: String
    fun msgNewTitle(t: TitleDef): String
    val accessoriesTitle: String
    val accessoriesHint: String
    fun accessoryName(id: String): String
    fun accessoryLock(acc: Accessory): String
    fun msgAccessoryLocked(acc: Accessory): String
    fun rarityName(r: Rarity): String
    val backdropTitle: String
    val backdropHint: String
    fun backdropName(id: String): String
    fun newBackdrop(b: BackdropDef): String
    fun msgBackdropLocked(b: BackdropDef): String
    val rewardsTitle: String
    val statShields: String
    val statBoxes: String
    val statStardust: String
    val statTitles: String
    val statItems: String
    fun boxesLeft(n: Int): String
    fun stardustProgress(n: Int, price: Int): String
    fun titleCollection(owned: Int, total: Int): String
    val shieldedDaysTitle: String
    val noShieldedDays: String

    // ---- stats ----
    val tabStats: String
    val statsIntro: String
    val statCurrentStreak: String
    val statBestStreak: String
    val statTotalXp: String
    val statMonthRate: String
    /** [points] = this month minus last month in percentage points; null = no data last month. */
    fun monthDelta(points: Int?): String
    val heatmapTitle: String
    val heatLess: String
    val heatMore: String
    fun dayDetail(date: LocalDate, done: Int, total: Int): String
    val weekChartTitle: String
    fun bestWeekday(day: DayOfWeek, percent: Int): String
    val questRatesTitle: String
    val questRatesEmpty: String
    val statsTapHint: String

    // ---- quest usability ----
    val fieldRepeat: String
    val repeatEveryDay: String
    val restDay: String
    val msgRestDay: String
    val undo: String
    fun msgRestored(title: String): String
    val gestureHint: String
    val reorder: String
    val reorderDone: String
    val reorderHint: String

    // ---- mood ----
    fun moodGreeting(nickname: String, mood: Mood): String

    // ---- widget ----
    val widgetTodayLabel: String
    val widgetNoQuests: String
    val widgetOpen: String
    val rowWidget: String
    val widgetRowDesc: String
    val msgWidgetUnsupported: String

    /** "월·수·금", or "매일" for every day. */
    fun repeatLabel(days: Set<DayOfWeek>): String =
        if (days.isEmpty()) repeatEveryDay else DayOfWeek.entries.filter { it in days }.joinToString("·") { weekdayShort(it) }

    /** "🧢 Cap" */
    fun accessoryLabel(id: String): String = "${Accessories.byId(id)?.emoji.orEmpty()} ${accessoryName(id)}".trim()

    /** "🔥 Getting Steady" */
    fun titleLabel(id: String): String = "${Titles.byId(id)?.emoji.orEmpty()} ${titleName(id)}".trim()

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

object KoStrings : Strings, FeatureStrings by KoFeatureStrings {
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

    override val on = "켜짐"
    override val off = "꺼짐"
    override val ok = "확인"
    override val groupGeneral = "일반"
    override fun languageName(lang: AppLanguage) = if (lang == AppLanguage.KOREAN) "한국어" else "영어"
    override val groupSocial = "소셜"
    override val groupActivity = "활동"
    override val groupMore = "기타"
    override val rowNotifications = "알림"
    override val rowFriends = "친구"
    override fun notifSummary(enabled: Boolean, questReminderTime: String?) =
        if (!enabled) "꺼짐" else if (questReminderTime != null) "켜짐 · 퀘스트 알림 $questReminderTime" else "켜짐"
    override fun friendsSummary(count: Int, blocked: Int) = "친구 ${count}명" + if (blocked > 0) " · 차단 ${blocked}명" else ""
    override fun messagesSummary(fontSize: String, autoReplies: Boolean) = "글자 $fontSize · 자동 답장 ${if (autoReplies) "켜짐" else "꺼짐"}"
    override fun questsSummary(autoCarry: Boolean, weekStartsMonday: Boolean) =
        "자동 불러오기 ${if (autoCarry) "켜짐" else "꺼짐"} · ${if (weekStartsMonday) "월요일" else "일요일"} 시작"
    override fun dataSummary(days: Int, quests: Int) = "기록 ${days}일 · 퀘스트 ${quests}개"
    override fun versionSummary(version: String) = "버전 $version"

    override val notifMaster = "알림 받기"
    override val notifMasterDesc = "퀘스트·스트릭·메시지 알림을 한 번에 켜고 꺼요"
    override val notifPermissionDenied = "알림 권한이 꺼져 있어요. 기기 설정에서 허용해 주세요."
    override val openSystemSettings = "설정 열기"
    override val groupReminders = "리마인더"
    override val questReminder = "퀘스트 알림"
    override val questReminderDesc = "매일 정한 시간에 남은 퀘스트를 알려줘요"
    override val streakReminder = "스트릭 경고"
    override val streakReminderDesc = "퀘스트를 다 못 끝냈으면 저녁에 한 번 더 알려줘요"
    override val reminderTime = "알림 시간"
    override val pickTime = "시간 선택"
    override val groupMessageAlerts = "메시지 알림"
    override val messageNotif = "새 메시지 알림"
    override val messageNotifDesc = "친구에게 답장이 오면 알려줘요"
    override val messagePreview = "메시지 미리보기"
    override val messagePreviewDesc = "알림에 메시지 내용을 보여줘요"
    override val testNotification = "테스트 알림 보내기"
    override val channelReminders = "퀘스트 리마인더"
    override val channelMessages = "메시지"
    override val questReminderTitle = "오늘의 퀘스트 ⚔️"
    override fun questReminderBody(remaining: Int) = "퀘스트 ${remaining}개가 기다리고 있어요. 오늘도 레벨업!"
    override val streakReminderTitle = "스트릭이 끊길 위기! 🔥"
    override fun streakReminderBody(remaining: Int) = "남은 퀘스트 ${remaining}개를 끝내고 스트릭을 지켜요"
    override val newMessageHidden = "새 메시지가 도착했어요"
    override val testNotificationTitle = "Today 알림 테스트"
    override val testNotificationBody = "알림이 잘 도착했어요 🎉"

    override val contactsCardTitle = "연락처에서 친구를 불러올까요?"
    override val contactsCardBody = "연락처의 이름만 기기 안에서 읽고, 어디에도 보내지 않아요. 추가할 사람은 직접 고를 수 있어요."
    override val contactsImport = "불러오기"
    override val contactsLater = "나중에"
    override val groupContacts = "연락처"
    override val importFromContacts = "연락처에서 친구 불러오기"
    override val contactsAllowed = "연락처 권한 허용됨"
    override val contactsNotAllowed = "연락처 권한이 필요해요"
    override val contactsDenied = "연락처 권한이 거부됐어요. 기기 설정에서 허용할 수 있어요."
    override val contactsEmpty = "불러올 연락처가 없어요"
    override val contactsPickerTitle = "친구로 추가할 연락처"
    override val searchName = "이름 검색"
    override fun addSelected(n: Int) = "${n}명 추가"
    override val alreadyFriend = "이미 친구"
    override fun msgContactsImported(n: Int) = if (n == 0) "새로 추가된 친구가 없어요" else "친구 ${n}명을 추가했어요"
    override fun groupFriendList(n: Int) = "친구 목록 ($n)"
    override val addFriend = "친구 추가"
    override val fieldFriendName = "이름"
    override val fieldFriendHandle = "아이디 (선택)"
    override val errFriendName = "이름을 입력해 주세요"
    override val errFriendDuplicate = "이미 있는 아이디예요"
    override fun errFriendLimit(max: Int) = "친구는 최대 ${max}명까지 추가할 수 있어요"
    override fun msgFriendAdded(name: String) = "${name}님을 친구로 추가했어요"
    override fun friendSource(source: com.today.app.data.FriendSource) = when (source) {
        com.today.app.data.FriendSource.DEMO -> "추천 친구"
        com.today.app.data.FriendSource.CONTACT -> "연락처"
        com.today.app.data.FriendSource.MANUAL -> "직접 추가"
    }
    override val friendMenu = "친구 메뉴"
    override val sendMessageAction = "메시지 보내기"
    override val block = "차단"
    override val unblock = "차단 해제"
    override val deleteFriend = "친구 삭제"
    override fun blockConfirmTitle(name: String) = "${name}님을 차단할까요?"
    override val blockConfirmBody = "차단하면 메시지 목록에서 숨겨지고 서로 메시지를 주고받을 수 없어요. 설정 > 친구에서 언제든 해제할 수 있어요."
    override fun deleteFriendTitle(name: String) = "${name}님을 삭제할까요?"
    override val deleteFriendBody = "친구 목록과 대화 기록에서 모두 삭제되고 되돌릴 수 없어요."
    override fun msgBlocked(name: String) = "${name}님을 차단했어요"
    override fun msgUnblocked(name: String) = "${name}님의 차단을 해제했어요"
    override fun msgFriendDeleted(name: String) = "${name}님을 친구에서 삭제했어요"
    override fun groupBlocked(n: Int) = "차단한 친구 ($n)"
    override val noBlocked = "차단한 친구가 없어요"
    override val noFriends = "아직 친구가 없어요. 연락처에서 불러오거나 직접 추가해 보세요."

    override val groupChat = "대화"
    override val chatFontSize = "글자 크기"
    override fun fontSizeLabel(size: com.today.app.data.ChatFontSize) = when (size) {
        com.today.app.data.ChatFontSize.SMALL -> "작게"
        com.today.app.data.ChatFontSize.MEDIUM -> "보통"
        com.today.app.data.ChatFontSize.LARGE -> "크게"
    }
    override val enterToSend = "엔터로 보내기"
    override val enterToSendDesc = "끄면 엔터는 줄바꿈, 보내기 버튼으로 전송해요"
    override val readReceipts = "읽음 표시"
    override val readReceiptsDesc = "친구가 읽은 내 메시지 아래에 '읽음'을 보여줘요"
    override val read = "읽음"
    override val autoReplies = "친구 자동 답장"
    override val autoRepliesDesc = "친구가 내가 입력한 언어로 답장해요 (데모 친구)"
    override val groupHistory = "기록"
    override val saveHistory = "대화 기록 저장"
    override val saveHistoryDesc = "앱을 다시 켜도 대화가 남아 있어요. 끄면 저장된 기록도 지워져요."
    override val clearChats = "대화 기록 모두 삭제"
    override val clearChatsBody = "모든 친구와의 대화가 삭제되고 되돌릴 수 없어요."
    override val msgChatsCleared = "대화 기록을 모두 삭제했어요"
    override val manageBlocked = "차단한 친구 관리"
    override val chatPreviewSample = "오늘 퀘스트 다 했어? 🔥"

    override val weekStart = "한 주 시작 요일"
    override val sunday = "일요일"
    override val monday = "월요일"
    override val celebration = "완료 효과"
    override val celebrationDesc = "퀘스트를 완료하면 축하 애니메이션을 보여줘요"
    override val defaultXp = "새 퀘스트 기본 XP"
    override val defaultXpDesc = "퀘스트를 추가할 때 처음 채워지는 XP예요"
    override val questReminderLink = "퀘스트 알림 설정"

    override val groupStorage = "저장 현황"
    override val statQuestsTotal = "전체 퀘스트"
    override val statFriends = "친구"
    override val statMessages = "메시지"
    override val exportData = "데이터 내보내기"
    override val exportDesc = "퀘스트·의상·친구·설정을 JSON 파일로 저장해요"
    override val exportDone = "데이터를 파일로 저장했어요"
    override val exportFailed = "내보내기에 실패했어요"

    override val version = "버전"
    override val builtWith = "만든 기술"
    override val privacyTitle = "개인정보"
    override val privacyBody = "모든 기록은 이 기기에만 저장돼요. 연락처는 친구를 추가할 때만 기기 안에서 이름을 읽고, 사진은 의상 인식에만 쓰고 저장하지 않아요."
    override val permissionsTitle = "권한"
    override val permNotifications = "알림"
    override val permContacts = "연락처"
    override val permCamera = "카메라"
    override val permCameraDesc = "기기의 카메라 앱을 사용해 권한이 필요 없어요"
    override val permGranted = "허용됨"
    override val permDenied = "허용 안 됨"
    override val licenses = "오픈소스 라이선스"

    // ---- rewards ----
    override val shieldName = "스트릭 보호권"
    override val shieldDialogBody =
        "레벨업할 때마다 1장씩 받아요. 퀘스트를 다 못 끝낸 날(오늘 또는 어제)에 쓰면 스트릭이 끊기지 않아요. " +
            "보호한 날은 일수에 더해지지 않고 스트릭을 이어주기만 해요."
    override fun shieldCount(n: Int) = "보유 ${n}장"
    override val useShieldToday = "오늘 지키기"
    override val useShieldYesterday = "어제 복구"
    override fun msgShieldUsed(streak: Int) = "🛡 보호권 사용! 🔥 스트릭 ${streak}일 유지"
    override val msgNoShield = "보호권이 없어요. 레벨업하거나 별조각으로 교환해요"
    override val msgNoStreakToProtect = "지킬 스트릭이 없어요. 하루를 완료해서 스트릭을 시작해요"
    override val errShieldNotAllowed = "보호권은 아직 완료하지 못한 오늘이나 어제에만 쓸 수 있어요"
    override fun rescueBanner(streak: Int) = "어제 퀘스트를 놓쳤어요! 보호권을 쓰면 🔥 ${streak}일 스트릭이 이어져요"
    override val rescueButton = "복구"
    override fun useShieldAction(shields: Int) = "🛡 보호권 사용 (${shields}장)"
    override fun stardustLine(n: Int, price: Int) = "✨ 별조각 $n / $price · 모으면 보호권 1장과 교환"
    override val exchange = "교환"
    override val msgExchanged = "✨ 별조각 → 🛡 보호권 1장 교환 완료!"
    override fun msgNotEnoughStardust(price: Int) = "별조각이 ${price}개 필요해요"
    override val legendShielded = "보호"
    override fun boxBanner(n: Int) = "보상 상자 ${n}개가 도착했어요"
    override val openBox = "열기"
    override val boxTitle = "🎁 보상 상자"
    override val boxOpening = "두근두근… 무엇이 나올까?"
    override fun boxRewardText(r: BoxReward) = when (r) {
        BoxReward.Shield -> "🛡 스트릭 보호권 +1"
        is BoxReward.Item ->
            if (r.duplicate) "이미 가진 ${accessoryLabel(r.id)} → ✨ 별조각 +${RewardEngine.DUPLICATE_STARDUST}"
            else "새 아이템 ${accessoryLabel(r.id)} 획득! 옷장에서 착용해 보세요"
        is BoxReward.Title ->
            if (r.duplicate) "이미 가진 칭호 「${titleName(r.id)}」 → ✨ 별조각 +${RewardEngine.DUPLICATE_STARDUST}"
            else "희귀 칭호 「${titleLabel(r.id)}」 획득!"
        is BoxReward.Stardust -> "✨ 별조각 +${r.amount}"
    }
    override fun openAnother(n: Int) = "하나 더 열기 ($n)"
    override val levelRewards = "🛡 보호권 +1 · 🎁 보상 상자 +1"
    override fun newUnlock(acc: Accessory) = "🔓 새 아이템 해금: ${accessoryLabel(acc.id)}"
    override fun nextUnlock(acc: Accessory) = "다음 보상: LV ${acc.unlockLevel} ${accessoryLabel(acc.id)}"
    override fun titleName(id: String) = when (id) {
        "rookie" -> "새싹 모험가"
        "first_quest" -> "첫 발걸음"
        "streak_3" -> "꾸준함의 시작"
        "streak_7" -> "일주일의 전사"
        "streak_30" -> "30일의 전설"
        "level_5" -> "성장하는 모험가"
        "level_10" -> "베테랑"
        "quests_50" -> "퀘스트 수집가"
        "perfect_day" -> "완벽주의자"
        "lucky" -> "행운아"
        "starlight" -> "별빛 수집가"
        else -> id
    }
    override fun titleHow(id: String) = when (id) {
        "rookie" -> "기본 칭호"
        "first_quest" -> "퀘스트 1개 완료"
        "streak_3" -> "3일 연속 달성"
        "streak_7" -> "7일 연속 달성"
        "streak_30" -> "30일 연속 달성"
        "level_5" -> "LV 5 달성"
        "level_10" -> "LV 10 달성"
        "quests_50" -> "퀘스트 50개 완료"
        "perfect_day" -> "하루 퀘스트 10개 모두 완료"
        else -> "보상 상자 전용"
    }
    override val titlePickerTitle = "칭호 선택"
    override val titleEquipped = "착용 중"
    override fun msgNewTitle(t: TitleDef) = "🏅 새 칭호 획득: ${titleLabel(t.id)}"
    override val accessoriesTitle = "악세사리"
    override val accessoriesHint = "레벨업으로 해금하고 보상 상자에서 희귀 아이템을 모아요. 탭해서 착용/해제"
    override fun accessoryName(id: String) = when (id) {
        "cap" -> "캡모자"
        "glasses" -> "하트 안경"
        "headphones" -> "헤드폰"
        "cape" -> "영웅 망토"
        "crown" -> "왕관"
        "sunglasses" -> "선글라스"
        "catears" -> "고양이 귀"
        "halo" -> "천사 링"
        "partyhat" -> "새해 파티 모자"
        "flowercrown" -> "봄꽃 화관"
        "strawhat" -> "여름 밀짚모자"
        "witchhat" -> "할로윈 마녀 모자"
        "santahat" -> "산타 모자"
        else -> id
    }
    override fun accessoryLock(acc: Accessory) = acc.unlockLevel?.let { "LV $it" } ?: if (acc.event != null) "🎪 이벤트" else "🎁 상자"
    override val rewardsTitle = "내 보상"
    override val statShields = "보호권"
    override val statBoxes = "상자"
    override val statStardust = "별조각"
    override val statTitles = "칭호"
    override val statItems = "아이템"
    override fun boxesLeft(n: Int) = if (n == 0) "레벨업하면 1개씩 받아요" else "열지 않은 상자 ${n}개"
    override fun titleCollection(owned: Int, total: Int) = "칭호 도감 $owned / $total"
    override fun stardustProgress(n: Int, price: Int) = "$n / $price · ${price}개 모으면 보호권 1장과 교환"
    override val shieldedDaysTitle = "보호권으로 지킨 날"
    override val noShieldedDays = "아직 보호권을 쓴 날이 없어요"

    // ---- stats ----
    override val tabStats = "통계"
    override val statsIntro = "기록이 쌓일수록 나의 습관 패턴이 보여요"
    override val statCurrentStreak = "현재 스트릭"
    override val statBestStreak = "최고 스트릭"
    override val statTotalXp = "누적 XP"
    override val statMonthRate = "이번 달 달성률"
    override fun monthDelta(points: Int?) = when {
        points == null -> "지난달 기록 없음"
        points > 0 -> "지난달보다 +${points}%p"
        points < 0 -> "지난달보다 ${points}%p"
        else -> "지난달과 같아요"
    }
    override val heatmapTitle = "최근 12주"
    override val heatLess = "적음"
    override val heatMore = "많음"
    override fun dayDetail(date: LocalDate, done: Int, total: Int) =
        if (total == 0) "${shortDate(date)} · 할 퀘스트 없음" else "${shortDate(date)} · ${total}개 중 ${done}개 완료"
    override val weekChartTitle = "최근 7일 완료 수"
    override fun bestWeekday(day: DayOfWeek, percent: Int) =
        "📈 ${day.getDisplayName(TextStyle.FULL, locale)}에 가장 꾸준해요 (평균 $percent%)"
    override val questRatesTitle = "퀘스트별 달성률"
    override val questRatesEmpty = "어제까지의 기록이 쌓이면 보여요"
    override val statsTapHint = "칸이나 막대를 누르면 그날 기록을 볼 수 있어요"

    // ---- quest usability ----
    override val fieldRepeat = "반복 요일 (선택 안 하면 매일)"
    override val repeatEveryDay = "매일"
    override val restDay = "쉬는 날"
    override val msgRestDay = "오늘은 쉬는 날로 정한 퀘스트예요"
    override val undo = "실행 취소"
    override fun msgRestored(title: String) = "「$title」 복구했어요"
    override val gestureHint = "👉 밀어서 완료 · 👈 밀어서 삭제 · 순서 변경 버튼이나 길게 눌러 이동"
    override val reorder = "순서 변경"
    override val reorderDone = "완료"
    override val reorderHint = "≡ 를 끌어서 순서를 바꾸고, 다 바꿨으면 완료를 눌러요"

    // ---- mood ----
    override fun moodGreeting(nickname: String, mood: Mood) = when (mood) {
        Mood.HAPPY -> "${nickname}님, 오늘 퀘스트 완료! 최고예요 🎉"
        Mood.SAD -> "${nickname}님, 어제는 아쉬웠지만 오늘 다시 시작해요!"
        Mood.SLEEPY -> "${nickname}님, 오늘의 첫 퀘스트를 시작해 볼까요?"
        Mood.NORMAL -> greeting(nickname)
        Mood.EXCITED -> "${nickname}님, 신나는 하루네요! 기세를 몰아가요 🔥"
        Mood.CALM -> "${nickname}님, 오늘은 차분하게 하나씩 해 봐요"
        Mood.LOVE -> "${nickname}님, 설레는 하루 보내세요 💕"
        Mood.ANGRY -> "${nickname}님, 힘든 날엔 작은 퀘스트 하나부터!"
        Mood.SICK -> "${nickname}님, 무리하지 말고 푹 쉬어요. 쉬는 것도 퀘스트예요"
    }

    // ---- widget ----
    override val widgetTodayLabel = "오늘 진행"
    override val widgetNoQuests = "퀘스트 없음"
    override val widgetOpen = "열기"
    override val rowWidget = "홈 화면 위젯"
    override val widgetRowDesc = "캐릭터와 오늘 진행률을 홈 화면에 한 줄로 두고 바로 열어요"
    override val msgWidgetUnsupported = "이 런처는 바로 추가를 지원하지 않아요. 홈 화면을 길게 눌러 위젯에서 추가해 주세요"
    override fun msgAccessoryLocked(acc: Accessory) =
        acc.unlockLevel?.let { "🔒 ${accessoryName(acc.id)}: LV ${it}에 해금돼요" }
            ?: acc.event?.let { "🔒 ${accessoryName(acc.id)}: ${eventName(it)} 기간에 이벤트 미션으로 얻을 수 있어요" }
            ?: "🔒 ${accessoryName(acc.id)}: 보상 상자에서 얻을 수 있어요"
    override fun rarityName(r: Rarity) = when (r) {
        Rarity.COMMON -> "일반"
        Rarity.RARE -> "희귀"
        Rarity.EPIC -> "영웅"
        Rarity.LEGENDARY -> "전설"
        Rarity.LIMITED -> "한정"
    }
    override val backdropTitle = "배경"
    override val backdropHint = "레벨이 오르면 새 배경이 열려요. 홈 캐릭터 카드와 옷장에 함께 적용돼요"
    override fun backdropName(id: String) = when (id) {
        "sky" -> "맑은 하늘"
        "sunset" -> "노을"
        "night" -> "별밤"
        "space" -> "우주"
        else -> "기본"
    }
    override fun newBackdrop(b: BackdropDef) = "🖼 새 배경 해금: ${b.emoji} ${backdropName(b.id)}"
    override fun msgBackdropLocked(b: BackdropDef) = "🔒 ${backdropName(b.id)}: LV ${b.unlockLevel}에 해금돼요"
}

object EnStrings : Strings, FeatureStrings by EnFeatureStrings {
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

    override val on = "On"
    override val off = "Off"
    override val ok = "OK"
    override val groupGeneral = "General"
    override fun languageName(lang: AppLanguage) = if (lang == AppLanguage.KOREAN) "Korean" else "English"
    override val groupSocial = "Social"
    override val groupActivity = "Activity"
    override val groupMore = "More"
    override val rowNotifications = "Notifications"
    override val rowFriends = "Friends"
    override fun notifSummary(enabled: Boolean, questReminderTime: String?) =
        if (!enabled) "Off" else if (questReminderTime != null) "On · Quest reminder $questReminderTime" else "On"
    override fun friendsSummary(count: Int, blocked: Int) =
        "$count friend${if (count == 1) "" else "s"}" + if (blocked > 0) " · $blocked blocked" else ""
    override fun messagesSummary(fontSize: String, autoReplies: Boolean) = "Text $fontSize · Auto-replies ${if (autoReplies) "on" else "off"}"
    override fun questsSummary(autoCarry: Boolean, weekStartsMonday: Boolean) =
        "Carry-over ${if (autoCarry) "on" else "off"} · Week starts ${if (weekStartsMonday) "Monday" else "Sunday"}"
    override fun dataSummary(days: Int, quests: Int) = "${days(days)} logged · $quests quest${if (quests == 1) "" else "s"}"
    override fun versionSummary(version: String) = "Version $version"

    override val notifMaster = "Allow notifications"
    override val notifMasterDesc = "Turn quest, streak and message alerts on or off at once"
    override val notifPermissionDenied = "Notifications are blocked. Please allow them in system settings."
    override val openSystemSettings = "Open settings"
    override val groupReminders = "Reminders"
    override val questReminder = "Quest reminder"
    override val questReminderDesc = "A daily nudge about the quests you have left"
    override val streakReminder = "Streak alert"
    override val streakReminderDesc = "One more evening reminder if you haven't finished"
    override val reminderTime = "Time"
    override val pickTime = "Choose time"
    override val groupMessageAlerts = "Message alerts"
    override val messageNotif = "New message alerts"
    override val messageNotifDesc = "Get notified when a friend replies"
    override val messagePreview = "Message preview"
    override val messagePreviewDesc = "Show the message text in the notification"
    override val testNotification = "Send test notification"
    override val channelReminders = "Quest reminders"
    override val channelMessages = "Messages"
    override val questReminderTitle = "Today's quests ⚔️"
    override fun questReminderBody(remaining: Int) = "$remaining quest${if (remaining == 1) " is" else "s are"} waiting. Time to level up!"
    override val streakReminderTitle = "Your streak is at risk! 🔥"
    override fun streakReminderBody(remaining: Int) = "Finish the last $remaining quest${if (remaining == 1) "" else "s"} to keep your streak"
    override val newMessageHidden = "You have a new message"
    override val testNotificationTitle = "Today test notification"
    override val testNotificationBody = "Notifications are working 🎉"

    override val contactsCardTitle = "Find friends from your contacts?"
    override val contactsCardBody = "Only names are read, on this device, and never sent anywhere. You choose who to add."
    override val contactsImport = "Import"
    override val contactsLater = "Not now"
    override val groupContacts = "Contacts"
    override val importFromContacts = "Import friends from contacts"
    override val contactsAllowed = "Contacts access allowed"
    override val contactsNotAllowed = "Contacts access needed"
    override val contactsDenied = "Contacts access was denied. You can allow it in system settings."
    override val contactsEmpty = "No contacts to import"
    override val contactsPickerTitle = "Choose contacts to add"
    override val searchName = "Search name"
    override fun addSelected(n: Int) = "Add $n"
    override val alreadyFriend = "Added"
    override fun msgContactsImported(n: Int) = if (n == 0) "No new friends added" else "Added $n friend${if (n == 1) "" else "s"}"
    override fun groupFriendList(n: Int) = "Friends ($n)"
    override val addFriend = "Add friend"
    override val fieldFriendName = "Name"
    override val fieldFriendHandle = "Username (optional)"
    override val errFriendName = "Please enter a name"
    override val errFriendDuplicate = "That username is already taken"
    override fun errFriendLimit(max: Int) = "You can have up to $max friends"
    override fun msgFriendAdded(name: String) = "Added $name as a friend"
    override fun friendSource(source: com.today.app.data.FriendSource) = when (source) {
        com.today.app.data.FriendSource.DEMO -> "Suggested"
        com.today.app.data.FriendSource.CONTACT -> "Contact"
        com.today.app.data.FriendSource.MANUAL -> "Added manually"
    }
    override val friendMenu = "Friend menu"
    override val sendMessageAction = "Message"
    override val block = "Block"
    override val unblock = "Unblock"
    override val deleteFriend = "Remove friend"
    override fun blockConfirmTitle(name: String) = "Block $name?"
    override val blockConfirmBody = "They'll be hidden from Messages and you won't be able to message each other. You can unblock anytime in Settings > Friends."
    override fun deleteFriendTitle(name: String) = "Remove $name?"
    override val deleteFriendBody = "They'll be removed from your friends along with your chat history. This can't be undone."
    override fun msgBlocked(name: String) = "Blocked $name"
    override fun msgUnblocked(name: String) = "Unblocked $name"
    override fun msgFriendDeleted(name: String) = "Removed $name"
    override fun groupBlocked(n: Int) = "Blocked ($n)"
    override val noBlocked = "No blocked friends"
    override val noFriends = "No friends yet. Import from contacts or add someone."

    override val groupChat = "Chat"
    override val chatFontSize = "Text size"
    override fun fontSizeLabel(size: com.today.app.data.ChatFontSize) = when (size) {
        com.today.app.data.ChatFontSize.SMALL -> "Small"
        com.today.app.data.ChatFontSize.MEDIUM -> "Medium"
        com.today.app.data.ChatFontSize.LARGE -> "Large"
    }
    override val enterToSend = "Press Enter to send"
    override val enterToSendDesc = "When off, Enter adds a new line and the button sends"
    override val readReceipts = "Read receipts"
    override val readReceiptsDesc = "Show \"Read\" under messages your friend has seen"
    override val read = "Read"
    override val autoReplies = "Friend auto-replies"
    override val autoRepliesDesc = "Friends reply in the language you type (demo friends)"
    override val groupHistory = "History"
    override val saveHistory = "Save chat history"
    override val saveHistoryDesc = "Keep chats after restarting the app. Turning this off deletes saved history."
    override val clearChats = "Delete all chats"
    override val clearChatsBody = "Every conversation will be deleted. This can't be undone."
    override val msgChatsCleared = "All chats deleted"
    override val manageBlocked = "Manage blocked friends"
    override val chatPreviewSample = "Did you finish today's quests? 🔥"

    override val weekStart = "Week starts on"
    override val sunday = "Sunday"
    override val monday = "Monday"
    override val celebration = "Completion effects"
    override val celebrationDesc = "Play a celebration when you complete a quest"
    override val defaultXp = "Default XP for new quests"
    override val defaultXpDesc = "Pre-filled XP when you add a quest"
    override val questReminderLink = "Quest reminder settings"

    override val groupStorage = "Storage"
    override val statQuestsTotal = "Total quests"
    override val statFriends = "Friends"
    override val statMessages = "Messages"
    override val exportData = "Export data"
    override val exportDesc = "Save quests, outfit, friends and settings as a JSON file"
    override val exportDone = "Data saved to file"
    override val exportFailed = "Export failed"

    override val version = "Version"
    override val builtWith = "Built with"
    override val privacyTitle = "Privacy"
    override val privacyBody = "Everything stays on this device. Contacts are read on-device only when you add friends, and photos are used only for outfit detection and never stored."
    override val permissionsTitle = "Permissions"
    override val permNotifications = "Notifications"
    override val permContacts = "Contacts"
    override val permCamera = "Camera"
    override val permCameraDesc = "Uses your camera app, so no permission is needed"
    override val permGranted = "Allowed"
    override val permDenied = "Not allowed"
    override val licenses = "Open-source licenses"

    // ---- rewards ----
    override val shieldName = "Streak shield"
    override val shieldDialogBody =
        "You get one every level-up. Use it on a day you couldn't finish (today or yesterday) and your streak won't break. " +
            "A shielded day doesn't add to the count, it just keeps the streak going."
    override fun shieldCount(n: Int) = "You have $n"
    override val useShieldToday = "Protect today"
    override val useShieldYesterday = "Save yesterday"
    override fun msgShieldUsed(streak: Int) = "🛡 Shield used! 🔥 Streak kept: ${days(streak)}"
    override val msgNoShield = "No shields left. Level up or trade in stardust"
    override val msgNoStreakToProtect = "No streak to protect yet. Finish a day to start one"
    override val errShieldNotAllowed = "Shields only work on today or yesterday, if it isn't complete"
    override fun rescueBanner(streak: Int) = "You missed yesterday! A shield keeps your 🔥 ${days(streak)} streak alive"
    override val rescueButton = "Save"
    override fun useShieldAction(shields: Int) = "🛡 Use shield ($shields)"
    override fun stardustLine(n: Int, price: Int) = "✨ Stardust $n / $price · trade for 1 shield"
    override val exchange = "Trade"
    override val msgExchanged = "✨ Stardust → 🛡 1 shield!"
    override fun msgNotEnoughStardust(price: Int) = "You need $price stardust"
    override val legendShielded = "Shielded"
    override fun boxBanner(n: Int) = if (n == 1) "A reward box arrived" else "$n reward boxes arrived"
    override val openBox = "Open"
    override val boxTitle = "🎁 Reward box"
    override val boxOpening = "What's inside…?"
    override fun boxRewardText(r: BoxReward) = when (r) {
        BoxReward.Shield -> "🛡 Streak shield +1"
        is BoxReward.Item ->
            if (r.duplicate) "Already have ${accessoryLabel(r.id)} → ✨ Stardust +${RewardEngine.DUPLICATE_STARDUST}"
            else "New item: ${accessoryLabel(r.id)}! Try it on in the wardrobe"
        is BoxReward.Title ->
            if (r.duplicate) "Already have the ${titleName(r.id)} title → ✨ Stardust +${RewardEngine.DUPLICATE_STARDUST}"
            else "Rare title: ${titleLabel(r.id)}!"
        is BoxReward.Stardust -> "✨ Stardust +${r.amount}"
    }
    override fun openAnother(n: Int) = "Open another ($n)"
    override val levelRewards = "🛡 Shield +1 · 🎁 Reward box +1"
    override fun newUnlock(acc: Accessory) = "🔓 Unlocked: ${accessoryLabel(acc.id)}"
    override fun nextUnlock(acc: Accessory) = "Next reward: LV ${acc.unlockLevel} ${accessoryLabel(acc.id)}"
    override fun titleName(id: String) = when (id) {
        "rookie" -> "Sprout Adventurer"
        "first_quest" -> "First Steps"
        "streak_3" -> "Getting Steady"
        "streak_7" -> "Weeklong Warrior"
        "streak_30" -> "30-Day Legend"
        "level_5" -> "Rising Adventurer"
        "level_10" -> "Veteran"
        "quests_50" -> "Quest Collector"
        "perfect_day" -> "Perfectionist"
        "lucky" -> "Lucky One"
        "starlight" -> "Starlight Collector"
        else -> id
    }
    override fun titleHow(id: String) = when (id) {
        "rookie" -> "Starting title"
        "first_quest" -> "Complete 1 quest"
        "streak_3" -> "3-day streak"
        "streak_7" -> "7-day streak"
        "streak_30" -> "30-day streak"
        "level_5" -> "Reach LV 5"
        "level_10" -> "Reach LV 10"
        "quests_50" -> "Complete 50 quests"
        "perfect_day" -> "Finish all 10 quests in a day"
        else -> "Reward box only"
    }
    override val titlePickerTitle = "Choose a title"
    override val titleEquipped = "Equipped"
    override fun msgNewTitle(t: TitleDef) = "🏅 New title: ${titleLabel(t.id)}"
    override val accessoriesTitle = "Accessories"
    override val accessoriesHint = "Unlock them by leveling up and collect rare ones from reward boxes. Tap to wear / remove"
    override fun accessoryName(id: String) = when (id) {
        "cap" -> "Cap"
        "glasses" -> "Heart Glasses"
        "headphones" -> "Headphones"
        "cape" -> "Hero Cape"
        "crown" -> "Crown"
        "sunglasses" -> "Sunglasses"
        "catears" -> "Cat Ears"
        "halo" -> "Halo"
        "partyhat" -> "New Year Party Hat"
        "flowercrown" -> "Spring Flower Crown"
        "strawhat" -> "Summer Straw Hat"
        "witchhat" -> "Halloween Witch Hat"
        "santahat" -> "Santa Hat"
        else -> id
    }
    override fun accessoryLock(acc: Accessory) = acc.unlockLevel?.let { "LV $it" } ?: if (acc.event != null) "🎪 Event" else "🎁 Box"
    override val rewardsTitle = "My rewards"
    override val statShields = "Shields"
    override val statBoxes = "Boxes"
    override val statStardust = "Stardust"
    override val statTitles = "Titles"
    override val statItems = "Items"
    override fun boxesLeft(n: Int) = when (n) {
        0 -> "You get one every level-up"
        1 -> "1 unopened box"
        else -> "$n unopened boxes"
    }
    override fun titleCollection(owned: Int, total: Int) = "Title collection $owned / $total"
    override fun stardustProgress(n: Int, price: Int) = "$n / $price · trade $price for 1 shield"
    override val shieldedDaysTitle = "Days saved by a shield"
    override val noShieldedDays = "No shields used yet"

    // ---- stats ----
    override val tabStats = "Stats"
    override val statsIntro = "Your habit patterns show up as you log more days"
    override val statCurrentStreak = "Current streak"
    override val statBestStreak = "Best streak"
    override val statTotalXp = "Total XP"
    override val statMonthRate = "This month"
    override fun monthDelta(points: Int?) = when {
        points == null -> "No data last month"
        points > 0 -> "+$points pts vs last month"
        points < 0 -> "$points pts vs last month"
        else -> "Same as last month"
    }
    override val heatmapTitle = "Last 12 weeks"
    override val heatLess = "Less"
    override val heatMore = "More"
    override fun dayDetail(date: LocalDate, done: Int, total: Int) =
        if (total == 0) "${shortDate(date)} · nothing scheduled" else "${shortDate(date)} · $done of $total done"
    override val weekChartTitle = "Completed, last 7 days"
    override fun bestWeekday(day: DayOfWeek, percent: Int) =
        "📈 Most consistent on ${day.getDisplayName(TextStyle.FULL, locale)}s (avg $percent%)"
    override val questRatesTitle = "Success rate by quest"
    override val questRatesEmpty = "Shows up once you have past days logged"
    override val statsTapHint = "Tap a cell or bar to see that day"

    // ---- quest usability ----
    override val fieldRepeat = "Repeat on (none = every day)"
    override val repeatEveryDay = "Every day"
    override val restDay = "Rest day"
    override val msgRestDay = "This quest rests today"
    override val undo = "Undo"
    override fun msgRestored(title: String) = "Restored \"$title\""
    override val gestureHint = "👉 swipe to complete · 👈 swipe to delete · Reorder button or long-press to move"
    override val reorder = "Reorder"
    override val reorderDone = "Done"
    override val reorderHint = "Drag ≡ to change the order, then tap Done"

    // ---- mood ----
    override fun moodGreeting(nickname: String, mood: Mood) = when (mood) {
        Mood.HAPPY -> "All done today, $nickname! 🎉"
        Mood.SAD -> "Yesterday slipped, $nickname. Let's go again today!"
        Mood.SLEEPY -> "Ready for your first quest, $nickname?"
        Mood.NORMAL -> greeting(nickname)
        Mood.EXCITED -> "What a day, $nickname! Keep the streak rolling 🔥"
        Mood.CALM -> "Nice and steady today, $nickname"
        Mood.LOVE -> "Have a lovely day, $nickname 💕"
        Mood.ANGRY -> "Tough day, $nickname? Start with one small quest"
        Mood.SICK -> "Rest up, $nickname. Taking a break counts too"
    }

    // ---- widget ----
    override val widgetTodayLabel = "Today"
    override val widgetNoQuests = "No quests"
    override val widgetOpen = "Open"
    override val rowWidget = "Home-screen widget"
    override val widgetRowDesc = "A one-row bar with your character and today's progress that opens the app"
    override val msgWidgetUnsupported = "This launcher can't add it directly. Long-press the home screen and add it from Widgets"
    override fun msgAccessoryLocked(acc: Accessory) =
        acc.unlockLevel?.let { "🔒 ${accessoryName(acc.id)} unlocks at LV $it" }
            ?: acc.event?.let { "🔒 ${accessoryName(acc.id)} comes from ${eventName(it)} event missions" }
            ?: "🔒 ${accessoryName(acc.id)} comes from reward boxes"
    override fun rarityName(r: Rarity) = when (r) {
        Rarity.COMMON -> "Common"
        Rarity.RARE -> "Rare"
        Rarity.EPIC -> "Epic"
        Rarity.LEGENDARY -> "Legendary"
        Rarity.LIMITED -> "Limited"
    }
    override val backdropTitle = "Backgrounds"
    override val backdropHint = "New backgrounds open as you level up. They show on the home character card and in the wardrobe"
    override fun backdropName(id: String) = when (id) {
        "sky" -> "Clear Sky"
        "sunset" -> "Sunset"
        "night" -> "Starry Night"
        "space" -> "Space"
        else -> "Default"
    }
    override fun newBackdrop(b: BackdropDef) = "🖼 New background: ${b.emoji} ${backdropName(b.id)}"
    override fun msgBackdropLocked(b: BackdropDef) = "🔒 ${backdropName(b.id)}: unlocks at LV ${b.unlockLevel}"

    private fun quests(n: Int) = if (n == 1) "quest" else "quests"
}

fun AppLanguage.strings(): Strings = when (this) {
    AppLanguage.KOREAN -> KoStrings
    AppLanguage.ENGLISH -> EnStrings
}

/** Current UI strings, provided at the root from the saved language setting. */
val LocalStrings = staticCompositionLocalOf<Strings> { KoStrings }
