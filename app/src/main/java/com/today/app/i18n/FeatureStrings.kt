package com.today.app.i18n

import com.today.app.data.FaceFeature
import com.today.app.data.HairStyle
import com.today.app.data.MissionType
import com.today.app.data.Mood
import com.today.app.data.PetSpecies
import com.today.app.data.PetStage
import com.today.app.data.SeasonEvent
import com.today.app.data.SeasonEvents
import com.today.app.data.ThemeMode

/**
 * Text for the first-run guide, pets, missions, events, routine packs, profile status,
 * appearance, backup import and privacy. Part of [Strings] (both languages implement it).
 */
interface FeatureStrings {
    // ---- first-run guide ----
    val obWelcomeTitle: String
    val obWelcomeBody: String
    val obStart: String
    val obRestore: String
    val obNext: String
    val obBack: String
    val obFinish: String
    val obProfileTitle: String
    val obProfileHint: String
    val obGoalsTitle: String
    val obGoalsHint: String
    fun obGoalsCount(n: Int): String
    val obPetTitle: String
    val obPetHint: String
    val obPetSkip: String
    val obReadyTitle: String
    fun obReadyBody(nickname: String): String
    fun obWelcomeDone(nickname: String): String
    fun obStep(step: Int, total: Int): String

    // ---- routine packs ----
    val templatesTitle: String
    val templatesHint: String
    val templatesButton: String
    fun templateName(id: String): String
    fun templateDesc(id: String): String
    fun msgTemplateAdded(name: String, n: Int): String
    val msgTemplateNone: String

    // ---- hair & face ----
    val hairStyleTitle: String
    fun hairStyleName(h: HairStyle): String
    val featuresTitle: String
    fun featureName(f: FaceFeature): String

    // ---- profile status ----
    val statusTitle: String
    val statusAuto: String
    fun moodName(m: Mood): String
    val statusMessageLabel: String
    val statusEmpty: String
    val editStatus: String
    val msgStatusSaved: String

    // ---- pets ----
    fun petName(p: PetSpecies): String
    fun petDefaultName(p: PetSpecies): String
    fun petStageName(s: PetStage): String
    val petCardTitle: String
    val petNameLabel: String
    fun petGrowthLeft(xp: Int, next: PetStage): String
    val petFullyGrown: String
    val petTapHint: String
    val petEggHint: String
    val petAdoptTitle: String
    val petAdoptHint: String
    val petAdopt: String
    val petChange: String
    fun petChangeBody(name: String): String
    val petRename: String
    fun msgPetAdopted(name: String): String
    fun msgPetHatched(name: String): String
    fun msgPetGrew(name: String, stage: PetStage): String

    // ---- daily missions & events ----
    val missionsTitle: String
    val missionsHint: String
    fun missionText(type: MissionType, goal: Int): String
    val missionClaim: String
    val missionClaimed: String
    fun msgMissionClaimed(xp: Int): String
    val msgMissionNotDone: String
    val msgMissionsAllClear: String
    fun missionsBonusHint(eventActive: Boolean): String
    fun eventName(id: String): String
    fun eventBanner(event: SeasonEvent, days: Int, goal: Int): String
    fun eventEnds(event: SeasonEvent, month: Int, day: Int): String
    fun eventDayResult(event: SeasonEvent, result: SeasonEvents.DayResult): String

    // ---- quest reminder ----
    val questReminderField: String
    val questReminderNone: String
    val questReminderPick: String
    val questReminderClear: String
    fun questItemReminderTitle(emoji: String, title: String): String
    val questItemReminderBody: String
    val msgQuestReminderNeedsNotifications: String

    // ---- appearance ----
    val rowAppearance: String
    fun themeName(m: ThemeMode): String
    val appearanceHint: String

    // ---- backup import ----
    val importData: String
    val importDesc: String
    val importConfirmTitle: String
    val importConfirmBody: String
    val importConfirm: String
    val importDone: String
    val importFailed: String

    // ---- privacy & crash reports ----
    val rowPrivacy: String
    val privacySummary: String
    val privacyPolicyTitle: String
    val privacyPolicyBody: String
    val crashReportsTitle: String
    val crashReportsDesc: String
    val crashDialogTitle: String
    val crashDialogBody: String
    val crashSend: String
    val crashSubject: String
    val deleteAllTitle: String
    val deleteAllDesc: String
}

/** Picks 이/가, 을/를, 은/는, 와/과, (으)로 by whether [word] ends in a final consonant. */
internal fun josa(word: String, withBatchim: String, without: String): String {
    val last = word.lastOrNull() ?: return without
    if (last !in '가'..'힣') return withBatchim // e.g. Latin names: read with the fuller form
    val batchim = (last - '가') % 28
    // (으)로: ㄹ final consonant takes 로
    if (withBatchim == "으로" && batchim == 8) return without
    return if (batchim != 0) withBatchim else without
}

object KoFeatureStrings : FeatureStrings {
    override val obWelcomeTitle = "Today에 오신 걸 환영해요!"
    override val obWelcomeBody = "매일의 습관을 퀘스트로 바꾸고, 해낼 때마다 캐릭터와 펫이 함께 성장해요."
    override val obStart = "시작하기"
    override val obRestore = "백업 파일로 이어서 하기"
    override val obNext = "다음"
    override val obBack = "이전"
    override val obFinish = "모험 시작!"
    override val obProfileTitle = "나를 닮은 캐릭터 만들기"
    override val obProfileHint = "이름과 헤어스타일, 피부색을 골라요. 옷과 악세사리는 나중에 옷장에서 꾸밀 수 있어요"
    override val obGoalsTitle = "어떤 습관을 만들고 싶나요?"
    override val obGoalsHint = "여러 개 골라도 돼요. 고른 루틴이 오늘의 퀘스트로 들어가요 (하루 최대 10개)"
    override fun obGoalsCount(n: Int) = if (n == 0) "고르지 않으면 기본 퀘스트로 시작해요" else "퀘스트 ${n}개로 시작해요"
    override val obPetTitle = "함께 키울 펫을 골라요"
    override val obPetHint = "퀘스트로 XP를 모으면 알이 부화하고, 아기 → 어린이 → 어른으로 자라요"
    override val obPetSkip = "나중에 고를게요"
    override val obReadyTitle = "준비 완료!"
    override fun obReadyBody(nickname: String) = "${nickname}님의 첫 퀘스트가 기다리고 있어요. 하나씩 완료하며 레벨을 올려 보세요!"
    override fun obWelcomeDone(nickname: String) = "🎉 환영해요, ${nickname}님! 오늘의 퀘스트를 시작해 볼까요?"
    override fun obStep(step: Int, total: Int) = "$step / $total"

    override val templatesTitle = "루틴 템플릿"
    override val templatesHint = "탭 한 번으로 여러 퀘스트를 추가해요. 이미 있는 퀘스트는 건너뛰어요"
    override val templatesButton = "템플릿"
    override fun templateName(id: String) = when (id) {
        "morning" -> "미라클 모닝"
        "workout" -> "홈트레이닝"
        "study" -> "시험 공부"
        "health" -> "건강 습관"
        "sleep" -> "꿀잠 루틴"
        "mind" -> "마음 챙김"
        "work" -> "직장인 루틴"
        "kids" -> "어린이 생활 습관"
        "senior" -> "건강한 하루 (어르신)"
        else -> id
    }
    override fun templateDesc(id: String) = when (id) {
        "morning" -> "일찍 일어나 하루를 계획해요"
        "workout" -> "집에서 하는 간단한 운동"
        "study" -> "복습 · 문제 풀이 · 암기"
        "health" -> "물 · 채소 · 걷기 · 영양제"
        "sleep" -> "잠들기 전 습관으로 숙면"
        "mind" -> "명상과 산책으로 마음 정리"
        "work" -> "집중 업무와 내일 준비"
        "kids" -> "양치 · 숙제 · 정리 · 독서"
        "senior" -> "약 · 혈압 · 산책 · 안부 전화"
        else -> ""
    }
    override fun msgTemplateAdded(name: String, n: Int) = "「$name」에서 퀘스트 ${n}개를 추가했어요"
    override val msgTemplateNone = "추가할 퀘스트가 없어요 (이미 있거나 하루 10개를 채웠어요)"

    override val hairStyleTitle = "헤어스타일"
    override fun hairStyleName(h: HairStyle) = when (h) {
        HairStyle.SHORT -> "숏컷"
        HairStyle.LONG -> "긴 머리"
        HairStyle.BOB -> "단발"
        HairStyle.PONYTAIL -> "포니테일"
        HairStyle.TWINTAIL -> "양갈래"
        HairStyle.BUN -> "올림머리"
        HairStyle.SPIKY -> "삐죽머리"
        HairStyle.CURLY -> "곱슬머리"
        HairStyle.BUZZ -> "짧은 머리"
    }
    override val featuresTitle = "얼굴 디테일"
    override fun featureName(f: FaceFeature) = when (f) {
        FaceFeature.FRECKLES -> "주근깨"
        FaceFeature.MUSTACHE -> "콧수염"
        FaceFeature.BEARD -> "턱수염"
        FaceFeature.WRINKLES -> "주름"
    }

    override val statusTitle = "지금 내 기분"
    override val statusAuto = "자동"
    override fun moodName(m: Mood) = when (m) {
        Mood.HAPPY -> "행복해요"
        Mood.NORMAL -> "평범해요"
        Mood.SLEEPY -> "졸려요"
        Mood.SAD -> "슬퍼요"
        Mood.EXCITED -> "신나요"
        Mood.CALM -> "평온해요"
        Mood.LOVE -> "설레요"
        Mood.ANGRY -> "짜증나요"
        Mood.SICK -> "아파요"
    }
    override val statusMessageLabel = "상태 메시지"
    override val statusEmpty = "상태 메시지를 남겨 보세요"
    override val editStatus = "상태 바꾸기"
    override val msgStatusSaved = "상태를 바꿨어요"

    override fun petName(p: PetSpecies) = when (p) {
        PetSpecies.DOG -> "강아지"
        PetSpecies.CAT -> "고양이"
        PetSpecies.FISH -> "물고기"
        PetSpecies.HAMSTER -> "햄스터"
        PetSpecies.PARROT -> "앵무새"
        PetSpecies.HEDGEHOG -> "고슴도치"
        PetSpecies.LIZARD -> "도마뱀"
    }
    override fun petDefaultName(p: PetSpecies) = when (p) {
        PetSpecies.DOG -> "뭉치"
        PetSpecies.CAT -> "나비"
        PetSpecies.FISH -> "뻐끔이"
        PetSpecies.HAMSTER -> "도토리"
        PetSpecies.PARROT -> "초롱이"
        PetSpecies.HEDGEHOG -> "밤송이"
        PetSpecies.LIZARD -> "용용이"
    }
    override fun petStageName(s: PetStage) = when (s) {
        PetStage.EGG -> "알"
        PetStage.BABY -> "아기"
        PetStage.CHILD -> "어린이"
        PetStage.ADULT -> "어른"
    }
    override val petCardTitle = "내 펫"
    override val petNameLabel = "펫 이름 (최대 10자)"
    override fun petGrowthLeft(xp: Int, next: PetStage) = "${petStageName(next)}까지 $xp XP"
    override val petFullyGrown = "다 자랐어요! 언제나 함께해요 💕"
    override val petTapHint = "펫을 눌러 쓰다듬어 주세요"
    override val petEggHint = "퀘스트를 완료하면 알이 따뜻해져요"
    override val petAdoptTitle = "펫 입양하기"
    override val petAdoptHint = "가장 많이 키우는 반려동물 7종 중에서 골라요. 퀘스트 XP를 모으면 자라요"
    override val petAdopt = "입양하기"
    override val petChange = "다른 펫 입양하기"
    override fun petChangeBody(name: String) = "${name}${josa(name, "과", "와")} 헤어지고 새 펫을 입양할까요? 새 펫은 알부터 다시 시작해요"
    override val petRename = "이름 바꾸기"
    override fun msgPetAdopted(name: String) = "🥚 ${name}${josa(name, "이", "가")} 알에서 기다리고 있어요"
    override fun msgPetHatched(name: String) = "🐣 알이 부화했어요! ${name}${josa(name, "을", "를")} 만나 보세요"
    override fun msgPetGrew(name: String, stage: PetStage) =
        "🌱 ${name}${josa(name, "이", "가")} ${petStageName(stage)}${josa(petStageName(stage), "으로", "로")} 자랐어요!"

    override val missionsTitle = "오늘의 미션"
    override val missionsHint = "매일 바뀌는 미션 3개. 완료하고 보상을 받아요"
    override fun missionText(type: MissionType, goal: Int) = when (type) {
        MissionType.COMPLETE_THREE -> "퀘스트 ${goal}개 완료하기"
        MissionType.COMPLETE_ALL -> "오늘 퀘스트 모두 완료하기"
        MissionType.MORNING -> "오전 12시 전에 퀘스트 하나 완료하기"
        MissionType.PET -> "펫 ${goal}번 쓰다듬기"
        MissionType.CHAT -> "친구에게 메시지 보내기"
        MissionType.ADD_QUEST -> "새 퀘스트 추가하기"
        MissionType.STATUS -> "프로필에서 오늘 기분 남기기"
    }
    override val missionClaim = "받기"
    override val missionClaimed = "완료"
    override fun msgMissionClaimed(xp: Int) = "🎯 미션 보상 +$xp XP"
    override val msgMissionNotDone = "아직 미션을 다 하지 않았어요"
    override val msgMissionsAllClear = "🎁 오늘의 미션을 모두 끝냈어요! 보상 상자 1개를 받았어요"
    override fun missionsBonusHint(eventActive: Boolean) =
        if (eventActive) "모두 완료하면 🎁 보상 상자 + 이벤트 1일" else "모두 완료하면 🎁 보상 상자"
    override fun eventName(id: String) = when (id) {
        "newyear" -> "새해"
        "spring" -> "봄꽃"
        "summer" -> "여름"
        "halloween" -> "할로윈"
        "christmas" -> "크리스마스"
        else -> id
    }
    override fun eventBanner(event: SeasonEvent, days: Int, goal: Int) =
        if (days >= goal) "${event.emoji} ${eventName(event.id)} 이벤트 완료! 한정 악세사리를 옷장에서 확인하세요"
        else "${event.emoji} ${eventName(event.id)} 이벤트: 미션 올클리어 $days / ${goal}일 → 한정 악세사리"
    override fun eventEnds(event: SeasonEvent, month: Int, day: Int) = "${month}월 ${day}일까지"
    override fun eventDayResult(event: SeasonEvent, result: SeasonEvents.DayResult) = when (result) {
        is SeasonEvents.DayResult.Progress -> "🎁 미션 올클리어! 보상 상자 +1 · ${eventName(event.id)} 이벤트 ${result.days}/${SeasonEvents.GOAL_DAYS}일"
        is SeasonEvents.DayResult.Item -> "${event.emoji} 이벤트 완료! 한정 악세사리를 얻었어요 (옷장에서 착용)"
        is SeasonEvents.DayResult.Stardust -> "${event.emoji} 이벤트 완료! 이미 가진 악세사리라 별조각 ${result.amount}개를 받았어요"
        SeasonEvents.DayResult.AlreadyFinished -> "🎁 미션 올클리어! 보상 상자 +1"
    }

    override val questReminderField = "알림 시간"
    override val questReminderNone = "없음"
    override val questReminderPick = "시간 정하기"
    override val questReminderClear = "알림 끄기"
    override fun questItemReminderTitle(emoji: String, title: String) = "$emoji $title"
    override val questItemReminderBody = "지금 할 시간이에요! 완료하고 XP를 받아요"
    override val msgQuestReminderNeedsNotifications = "알림이 꺼져 있어요. 설정 > 알림에서 켜면 이 시간에 알려 드려요"

    override val rowAppearance = "화면 테마"
    override fun themeName(m: ThemeMode) = when (m) {
        ThemeMode.SYSTEM -> "시스템 설정"
        ThemeMode.DARK -> "다크"
        ThemeMode.LIGHT -> "라이트"
    }
    override val appearanceHint = "시스템 설정을 고르면 휴대폰의 다크 모드를 따라가요"

    override val importData = "백업 불러오기"
    override val importDesc = "내보낸 JSON 파일로 기록을 되살려요"
    override val importConfirmTitle = "백업을 불러올까요?"
    override val importConfirmBody = "지금 기기에 있는 기록은 백업 파일 내용으로 바뀌어요. 필요하면 먼저 내보내기로 저장해 두세요."
    override val importConfirm = "불러오기"
    override val importDone = "✅ 백업을 불러왔어요"
    override val importFailed = "Today 백업 파일이 아니거나 읽을 수 없어요"

    override val rowPrivacy = "개인정보 · 데이터"
    override val privacySummary = "처리방침, 오류 리포트, 데이터 삭제"
    override val privacyPolicyTitle = "개인정보 처리방침"
    override val privacyPolicyBody = """
        1. 수집하는 정보: Today는 회원가입이 없고 서버로 정보를 보내지 않아요. 퀘스트, 캐릭터, 펫, 설정, 대화 기록은 모두 이 기기 안에만 저장돼요.
        2. 사진: 의상 인식에 고른 사진은 기기 안에서만 분석하고 저장하지 않아요.
        3. 연락처: 친구 추가를 직접 고를 때만 이름을 읽고, 고른 사람의 이름만 저장해요.
        4. 알림: 알림 권한은 리마인더와 메시지 알림에만 써요.
        5. 오류 리포트: 앱이 비정상 종료되면 오류 내용만 기기에 남기고, 다음 실행 때 직접 공유할지 물어봐요. 공유하지 않으면 어디에도 전송되지 않아요.
        6. 내 데이터 관리: 언제든 백업 파일로 내보내거나, 아래에서 모든 데이터를 삭제할 수 있어요. 앱을 지우면 기기 안의 기록도 함께 지워져요.
    """.trimIndent()
    override val crashReportsTitle = "오류 리포트 제안"
    override val crashReportsDesc = "앱이 비정상 종료되면 다음 실행 때 리포트 공유를 물어봐요"
    override val crashDialogTitle = "앱이 예기치 않게 종료됐어요"
    override val crashDialogBody = "불편을 드려 죄송해요. 오류 리포트를 공유해 주시면 문제를 고치는 데 큰 도움이 돼요. 리포트에는 기기 모델과 오류 내용만 들어 있어요."
    override val crashSend = "리포트 공유"
    override val crashSubject = "Today 오류 리포트"
    override val deleteAllTitle = "모든 데이터 삭제"
    override val deleteAllDesc = "퀘스트, 캐릭터, 펫, 친구, 대화, 오류 기록까지 기기에서 지워요"
}

object EnFeatureStrings : FeatureStrings {
    override val obWelcomeTitle = "Welcome to Today!"
    override val obWelcomeBody = "Turn daily habits into quests. Every one you finish helps your character and your pet grow."
    override val obStart = "Get started"
    override val obRestore = "Continue from a backup file"
    override val obNext = "Next"
    override val obBack = "Back"
    override val obFinish = "Start the adventure!"
    override val obProfileTitle = "Make a character like you"
    override val obProfileHint = "Pick a name, hair style and skin tone. Clothes and accessories can be changed later in the wardrobe"
    override val obGoalsTitle = "Which habits do you want to build?"
    override val obGoalsHint = "Pick as many as you like. They become today's quests (up to 10 a day)"
    override fun obGoalsCount(n: Int) = if (n == 0) "Pick none to start with the default quests" else "Starting with $n quests"
    override val obPetTitle = "Pick a pet to raise"
    override val obPetHint = "Earn XP with quests: the egg hatches, then grows from baby to young to adult"
    override val obPetSkip = "Maybe later"
    override val obReadyTitle = "All set!"
    override fun obReadyBody(nickname: String) = "Your first quests are waiting, $nickname. Finish them one by one to level up!"
    override fun obWelcomeDone(nickname: String) = "🎉 Welcome, $nickname! Ready for today's quests?"
    override fun obStep(step: Int, total: Int) = "$step / $total"

    override val templatesTitle = "Routine templates"
    override val templatesHint = "Add several quests in one tap. Ones you already have are skipped"
    override val templatesButton = "Templates"
    override fun templateName(id: String) = when (id) {
        "morning" -> "Miracle Morning"
        "workout" -> "Home Workout"
        "study" -> "Exam Prep"
        "health" -> "Healthy Habits"
        "sleep" -> "Sleep Well"
        "mind" -> "Mindfulness"
        "work" -> "Workday"
        "kids" -> "Kids' Routine"
        "senior" -> "Healthy Day (Seniors)"
        else -> id
    }
    override fun templateDesc(id: String) = when (id) {
        "morning" -> "Rise early and plan the day"
        "workout" -> "Simple exercises at home"
        "study" -> "Review · practice · memorize"
        "health" -> "Water · veggies · walking · vitamins"
        "sleep" -> "Bedtime habits for deep sleep"
        "mind" -> "Meditation and walks to unwind"
        "work" -> "Focus time and planning ahead"
        "kids" -> "Teeth · homework · tidy up · reading"
        "senior" -> "Medicine · blood pressure · walk · call"
        else -> ""
    }
    override fun msgTemplateAdded(name: String, n: Int) = "Added $n quest${if (n == 1) "" else "s"} from \"$name\""
    override val msgTemplateNone = "Nothing to add (already there, or the day is full)"

    override val hairStyleTitle = "Hair style"
    override fun hairStyleName(h: HairStyle) = when (h) {
        HairStyle.SHORT -> "Short"
        HairStyle.LONG -> "Long"
        HairStyle.BOB -> "Bob"
        HairStyle.PONYTAIL -> "Ponytail"
        HairStyle.TWINTAIL -> "Twin tails"
        HairStyle.BUN -> "Bun"
        HairStyle.SPIKY -> "Spiky"
        HairStyle.CURLY -> "Curly"
        HairStyle.BUZZ -> "Buzz cut"
    }
    override val featuresTitle = "Face details"
    override fun featureName(f: FaceFeature) = when (f) {
        FaceFeature.FRECKLES -> "Freckles"
        FaceFeature.MUSTACHE -> "Mustache"
        FaceFeature.BEARD -> "Beard"
        FaceFeature.WRINKLES -> "Wrinkles"
    }

    override val statusTitle = "How I feel"
    override val statusAuto = "Auto"
    override fun moodName(m: Mood) = when (m) {
        Mood.HAPPY -> "Happy"
        Mood.NORMAL -> "Okay"
        Mood.SLEEPY -> "Sleepy"
        Mood.SAD -> "Sad"
        Mood.EXCITED -> "Excited"
        Mood.CALM -> "Calm"
        Mood.LOVE -> "In love"
        Mood.ANGRY -> "Grumpy"
        Mood.SICK -> "Unwell"
    }
    override val statusMessageLabel = "Status message"
    override val statusEmpty = "Leave a status message"
    override val editStatus = "Update status"
    override val msgStatusSaved = "Status updated"

    override fun petName(p: PetSpecies) = when (p) {
        PetSpecies.DOG -> "Dog"
        PetSpecies.CAT -> "Cat"
        PetSpecies.FISH -> "Fish"
        PetSpecies.HAMSTER -> "Hamster"
        PetSpecies.PARROT -> "Parrot"
        PetSpecies.HEDGEHOG -> "Hedgehog"
        PetSpecies.LIZARD -> "Lizard"
    }
    override fun petDefaultName(p: PetSpecies) = when (p) {
        PetSpecies.DOG -> "Buddy"
        PetSpecies.CAT -> "Luna"
        PetSpecies.FISH -> "Bubbles"
        PetSpecies.HAMSTER -> "Peanut"
        PetSpecies.PARROT -> "Kiwi"
        PetSpecies.HEDGEHOG -> "Chestnut"
        PetSpecies.LIZARD -> "Ziggy"
    }
    override fun petStageName(s: PetStage) = when (s) {
        PetStage.EGG -> "Egg"
        PetStage.BABY -> "Baby"
        PetStage.CHILD -> "Young"
        PetStage.ADULT -> "Adult"
    }
    override val petCardTitle = "My pet"
    override val petNameLabel = "Pet name (up to 10)"
    override fun petGrowthLeft(xp: Int, next: PetStage) = "$xp XP to ${petStageName(next)}"
    override val petFullyGrown = "Fully grown! Always by your side 💕"
    override val petTapHint = "Tap your pet to give it a pat"
    override val petEggHint = "Finishing quests keeps the egg warm"
    override val petAdoptTitle = "Adopt a pet"
    override val petAdoptHint = "Pick one of the 7 most popular pets. It grows with your quest XP"
    override val petAdopt = "Adopt"
    override val petChange = "Adopt a different pet"
    override fun petChangeBody(name: String) = "Say goodbye to $name and adopt a new pet? The new one starts again as an egg."
    override val petRename = "Rename"
    override fun msgPetAdopted(name: String) = "🥚 $name is waiting in its egg"
    override fun msgPetHatched(name: String) = "🐣 The egg hatched! Say hi to $name"
    override fun msgPetGrew(name: String, stage: PetStage) = "🌱 $name grew into a${if (stage == PetStage.ADULT) "n" else ""} ${petStageName(stage).lowercase()}!"

    override val missionsTitle = "Today's missions"
    override val missionsHint = "Three new missions every day. Finish them and claim the reward"
    override fun missionText(type: MissionType, goal: Int) = when (type) {
        MissionType.COMPLETE_THREE -> "Complete $goal quest${if (goal == 1) "" else "s"}"
        MissionType.COMPLETE_ALL -> "Complete all of today's quests"
        MissionType.MORNING -> "Complete a quest before noon"
        MissionType.PET -> "Pat your pet $goal times"
        MissionType.CHAT -> "Send a friend a message"
        MissionType.ADD_QUEST -> "Add a new quest"
        MissionType.STATUS -> "Share today's mood on your profile"
    }
    override val missionClaim = "Claim"
    override val missionClaimed = "Done"
    override fun msgMissionClaimed(xp: Int) = "🎯 Mission reward +$xp XP"
    override val msgMissionNotDone = "This mission isn't finished yet"
    override val msgMissionsAllClear = "🎁 All of today's missions done! You got a reward box"
    override fun missionsBonusHint(eventActive: Boolean) =
        if (eventActive) "Finish all: 🎁 reward box + 1 event day" else "Finish all: 🎁 reward box"
    override fun eventName(id: String) = when (id) {
        "newyear" -> "New Year"
        "spring" -> "Spring Blossom"
        "summer" -> "Summer"
        "halloween" -> "Halloween"
        "christmas" -> "Christmas"
        else -> id
    }
    override fun eventBanner(event: SeasonEvent, days: Int, goal: Int) =
        if (days >= goal) "${event.emoji} ${eventName(event.id)} event complete! Find your limited item in the wardrobe"
        else "${event.emoji} ${eventName(event.id)} event: all missions cleared $days / $goal days → limited accessory"
    override fun eventEnds(event: SeasonEvent, month: Int, day: Int) =
        "Until ${java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)} $day"
    override fun eventDayResult(event: SeasonEvent, result: SeasonEvents.DayResult) = when (result) {
        is SeasonEvents.DayResult.Progress -> "🎁 All missions cleared! +1 box · ${eventName(event.id)} event ${result.days}/${SeasonEvents.GOAL_DAYS} days"
        is SeasonEvents.DayResult.Item -> "${event.emoji} Event complete! You got a limited accessory (wear it in the wardrobe)"
        is SeasonEvents.DayResult.Stardust -> "${event.emoji} Event complete! You already own the item, so here are ${result.amount} stardust"
        SeasonEvents.DayResult.AlreadyFinished -> "🎁 All missions cleared! +1 box"
    }

    override val questReminderField = "Reminder"
    override val questReminderNone = "None"
    override val questReminderPick = "Set a time"
    override val questReminderClear = "No reminder"
    override fun questItemReminderTitle(emoji: String, title: String) = "$emoji $title"
    override val questItemReminderBody = "It's time! Finish it to earn XP"
    override val msgQuestReminderNeedsNotifications = "Notifications are off. Turn them on in Settings > Notifications to get this reminder"

    override val rowAppearance = "Theme"
    override fun themeName(m: ThemeMode) = when (m) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.DARK -> "Dark"
        ThemeMode.LIGHT -> "Light"
    }
    override val appearanceHint = "System follows your phone's dark mode"

    override val importData = "Import backup"
    override val importDesc = "Restore your records from an exported JSON file"
    override val importConfirmTitle = "Import this backup?"
    override val importConfirmBody = "Everything on this device will be replaced with the backup's contents. Export first if you want to keep it."
    override val importConfirm = "Import"
    override val importDone = "✅ Backup imported"
    override val importFailed = "That isn't a Today backup, or it couldn't be read"

    override val rowPrivacy = "Privacy & data"
    override val privacySummary = "Policy, crash reports, delete data"
    override val privacyPolicyTitle = "Privacy policy"
    override val privacyPolicyBody = """
        1. What we collect: Today has no accounts and sends nothing to a server. Quests, character, pet, settings and chats are stored only on this device.
        2. Photos: a photo picked for outfit recognition is analyzed on the device and never saved.
        3. Contacts: names are read only when you choose to add friends, and only the people you pick are saved.
        4. Notifications: used only for reminders and message alerts.
        5. Crash reports: if the app crashes, only the error details are kept on the device and you're asked on the next launch whether to share them. Nothing is sent unless you share it.
        6. Your data: export it as a backup any time, or delete everything below. Uninstalling the app also removes it from the device.
    """.trimIndent()
    override val crashReportsTitle = "Offer crash reports"
    override val crashReportsDesc = "After a crash, ask on the next launch whether to share the report"
    override val crashDialogTitle = "The app closed unexpectedly"
    override val crashDialogBody = "Sorry about that. Sharing the crash report helps a lot in fixing it. It only contains the device model and the error details."
    override val crashSend = "Share report"
    override val crashSubject = "Today crash report"
    override val deleteAllTitle = "Delete all data"
    override val deleteAllDesc = "Removes quests, character, pet, friends, chats and crash logs from this device"
}
