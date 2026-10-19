package com.xuanji.app.ui.components
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.local.ConversationMemoryStore
import com.xuanji.app.data.local.DataStorePreferenceBridge
import com.xuanji.app.data.local.MysticCharacterSessionStore
import com.xuanji.app.data.local.CompanionGameProgressStore
import com.xuanji.app.data.local.softMemoryTagStore
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.MysticClarifierOption
import com.xuanji.app.domain.MysticInteraction
import com.xuanji.app.domain.MysticInteractionOption
import com.xuanji.app.domain.MysticDetailContext
import com.xuanji.app.domain.MysticGuideGenerator
import com.xuanji.app.domain.MysticCharacterCatalog
import com.xuanji.app.domain.MysticCharacterEvent
import com.xuanji.app.domain.MysticCharacterId
import com.xuanji.app.domain.MysticCharacterSessionState
import com.xuanji.app.domain.DefaultMysticDialogueEngine
import com.xuanji.app.domain.DialogueContext
import com.xuanji.app.domain.DialogueProvider
import com.xuanji.app.domain.DialogueRequest
import com.xuanji.app.domain.OfflineDialogueProvider
import com.xuanji.app.domain.ProviderResult
import com.xuanji.app.domain.MysticEvent
import com.xuanji.app.domain.MysticSessionState
import com.xuanji.app.domain.reduce
import com.xuanji.app.domain.reduceCharacterSession
import com.xuanji.app.domain.MysticOpeningCheckin
import com.xuanji.app.domain.MysticOpeningOption
import com.xuanji.app.domain.MysticGuestChoice
import com.xuanji.app.domain.MysticRhythmCheckin
import com.xuanji.app.domain.MysticSkin
import com.xuanji.app.domain.MysticVisitMemory
import com.xuanji.app.domain.ConversationMemory
import com.xuanji.app.domain.RecallFacts
import com.xuanji.app.domain.RecollectionCodec
import com.xuanji.app.domain.RecollectionKind
import com.xuanji.app.domain.SoftMemorySource
import com.xuanji.app.domain.SoftMemoryTag
import com.xuanji.app.domain.DefaultMysticDialogueAnalyzer
import com.xuanji.app.domain.MysticMemoryNote as DomainMysticMemoryNote
import com.xuanji.app.domain.MysticCompanionAction
import com.xuanji.app.domain.MysticCompanionActionRouter
import com.xuanji.app.domain.MysticGameContextFactory
import com.xuanji.app.domain.MysticGameContext
import com.xuanji.app.domain.MysticGameProgressQuestion
import com.xuanji.app.domain.CompanionGameFavoriteFactory
import com.xuanji.app.domain.ReadingFavoriteDataState
import com.xuanji.app.domain.action.DailyActionPlan
import com.xuanji.app.domain.action.LifeProfile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.launch
import java.time.LocalDate


private fun mysticCompanionState(
    key: String,
    fortune: CompositeDailyFortune
): MysticCompanionState {
    // remember owns this scope. Once disposed, only the revocable DataStore record may restore it.
    require(key.isNotBlank())
    val topic = "composite"
    return MysticCompanionState(MysticGuideGenerator.suggestedMode(topic, fortune), topic).also {
        it.skinId = MysticGuideGenerator.defaultMysticSkin(it.mode, fortune).id
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MysticGuideCard(
    bazi: BaziFull,
    fortune: CompositeDailyFortune,
    immersive: Boolean = false,
    characterId: MysticCharacterId = MysticCharacterId.ShenYanzhou,
    onStageModeChange: (String) -> Unit = {},
    onStageSkinChange: (String) -> Unit = {},
    stageCostumeRequest: Pair<String, String>? = null,
    onStageCostumeConsumed: () -> Unit = {},
    stageActionRequest: String? = null,
    stageDetailContext: MysticDetailContext? = null,
    onStageActionConsumed: () -> Unit = {},
    onRequestCharacterHandoff: ((MysticCharacterId, MysticDetailContext) -> Unit)? = null
) {
    val records = com.xuanji.app.ui.test.profileTestRecords()
    val context = LocalContext.current
    val visitStore = remember(context) { MysticVisitStore(context) }
    val memoryStore = remember(context) { ConversationMemoryStore(DataStorePreferenceBridge(context)) }
    val characterSessionStore = remember(context) { MysticCharacterSessionStore(DataStorePreferenceBridge(context)) }
    val softMemoryStore = remember(context) { context.softMemoryTagStore() }
    val coroutineScope = rememberCoroutineScope()
    val visitProfile = remember(bazi) { "bazi|${bazi.chart.display}" }
    val readingFavoriteStore = LocalReadingFavoriteStore.current
    val readingFavoriteProfileKey = LocalReadingFavoriteProfileKey.current ?: visitProfile
    val readingMetadata = LocalMysticReadingMetadata.current
    val companionKey = "${bazi.hashCode()}|${fortune.hashCode()}"
    val companion = remember(companionKey) { mysticCompanionState(companionKey, fortune) }
    val characterProfile = remember(characterId) { MysticCharacterCatalog.byId(characterId) }
    var topic by remember(companion) { companion::topic }
    var mode by remember(companion) { companion::mode }
    var interactionCarryoverOption by remember(companion) { companion::interactionCarryoverOption }
    var pendingHandoff by remember(companion) { companion::pendingHandoff }
    var pendingHandoffEcho by remember(companion) { companion::pendingHandoffEcho }
    var rhythmAnswered by remember(companion) { companion::rhythmAnswered }
    var completedRhythmKey by remember(companion) { companion::completedRhythmKey }
    var guestCameo by remember(companion) { companion::guestCameo }
    var guestExit by remember(companion) { companion::guestExit }
    var selectedGuestChoice by remember(companion) { companion::selectedGuestChoice }
    var guestReply by remember(companion) { companion::guestReply }
    var guestQuestion by remember(companion) { companion::guestQuestion }
    var pendingGuest by remember(companion) { companion::pendingGuest }
    var guestChoiceCarryoverKey by remember(companion) { companion::guestChoiceCarryoverKey }
    var pendingGuestChoiceEcho by remember(companion) { companion::pendingGuestChoiceEcho }
    var memoryNotes by remember(companion) { companion::memoryNotes }
    var memorySequence by remember(companion) { companion::memorySequence }
    var memoryExpanded by remember(companion) { companion::memoryExpanded }
    var revisitLine by remember(visitProfile) { mutableStateOf("") }
    var recollectionLine by remember(visitProfile) { mutableStateOf("") }
    var storedMemory by remember(visitProfile) { mutableStateOf(ConversationMemory()) }
    var recollectionBroken by remember(visitProfile) { mutableStateOf(false) }
    var softMemoryTags by remember(visitProfile) { mutableStateOf(emptyList<SoftMemoryTag>()) }
    var memoryClearFailed by remember(visitProfile) { mutableStateOf(false) }
    var visitReady by remember(visitProfile) { mutableStateOf(false) }
    var persistedVisitAction by remember(visitProfile) { mutableStateOf("") }
    LaunchedEffect(companionKey) {
        guestExit = null
        memoryNotes = emptyList()
        memorySequence = 0
        memoryExpanded = false
    }
    val latestTest = com.xuanji.app.ui.test.authorizedCompanionTest(records)
    var sessionState by remember(visitProfile, characterId) { mutableStateOf(companion.sessionFor(characterId)) }
    var characterSessionsLoaded by remember(visitProfile) { mutableStateOf(false) }
    fun currentInteraction() = sessionState.interaction.forDate(fortune.dateKey)
    fun updateInteraction(transform: (com.xuanji.app.domain.MysticInteractionState) -> com.xuanji.app.domain.MysticInteractionState) {
        if (!characterSessionsLoaded) return
        sessionState = reduce(sessionState, MysticEvent.UpdateInteraction(sessionState.sessionToken, transform(currentInteraction())))
    }
    var dailyActionPlan by remember(visitProfile, fortune.dateKey) { mutableStateOf<DailyActionPlan?>(null) }
    var lifeProfile by remember(visitProfile) { mutableStateOf<LifeProfile?>(null) }
    var routedAction by MysticInteractionProperty(
        read = { currentInteraction().restoreAction(dailyActionPlan, lifeProfile) },
        write = { action -> updateInteraction { it.withAction(action) } }
    )
    var temporaryConditions by MysticInteractionProperty(
        read = { currentInteraction().conditions }, write = { value -> updateInteraction { it.copy(conditions = value) } }
    )
    var referenceCategory by MysticInteractionProperty(
        read = { currentInteraction().referenceCategory }, write = { value -> updateInteraction { it.copy(referenceCategory = value) } }
    )
    var currentReferenceId by MysticInteractionProperty(
        read = { currentInteraction().referenceId }, write = { value -> updateInteraction { it.copy(referenceId = value) } }
    )
    var temporaryStatus by remember(characterId) { mutableStateOf<String?>(null) }
    var actionRequestSequence by remember(characterId) { mutableLongStateOf(0L) }
    LaunchedEffect(visitProfile, characterId, fortune.dateKey, temporaryConditions, characterSessionsLoaded) {
        if (!characterSessionsLoaded) return@LaunchedEffect
        val restoredPlan = runCatching { AppModule.actionRepository.loadToday(java.time.LocalDate.parse(fortune.dateKey), temporaryConditions) }.getOrNull()
        if (!isActive) return@LaunchedEffect
        dailyActionPlan = restoredPlan
        lifeProfile = runCatching { AppModule.actionRepository.loadLifeProfile() }.getOrNull()
    }
    val guide = remember(mode, topic, bazi, fortune, latestTest, companion.skinId) {
        MysticGuideGenerator.generate(mode, topic, bazi, fortune, latestTest, skinId = companion.skinId)
    }
    val dialogueEngine = remember { DefaultMysticDialogueEngine() }
    val dialogueProvider: DialogueProvider = remember { OfflineDialogueProvider(dialogueEngine) }
    LaunchedEffect(visitProfile) {
        val restored = runCatching { characterSessionStore.load(visitProfile) }.getOrNull()
        if (restored != null) {
            restored.sessions.forEach { (id, state) ->
                companion.characterSessionStates[id] = state
                val thread = companion.threadFor(id)
                if (thread.isEmpty()) thread.addAll(restoreCharacterThread(state.messages))
            }
            companion.sharedMemoryNotes = restored.sharedMemoryNotes
            sessionState = restored.session(characterId)
        }
        characterSessionsLoaded = true
    }
    LaunchedEffect(sessionState, characterId) {
        companion.characterSessionStates[characterId] = sessionState
    }
    LaunchedEffect(sessionState, characterId, companion.sharedMemoryNotes, characterSessionsLoaded) {
        if (!characterSessionsLoaded) return@LaunchedEffect
        val snapshot = com.xuanji.app.domain.MysticCharacterSessionState(
            activeCharacterId = characterId,
            sessions = MysticCharacterId.entries.associateWith { if (it == characterId) sessionState else companion.sessionFor(it) },
            sharedMemoryNotes = companion.sharedMemoryNotes
        )
        runCatching { characterSessionStore.save(visitProfile, snapshot) }
    }
    LaunchedEffect(characterId) {
        companion.mode = characterProfile.dialogueMode
        companion.skinId = characterProfile.legacySkinId
    }
    LaunchedEffect(characterId, characterSessionsLoaded) {
        if (!characterSessionsLoaded) return@LaunchedEffect
        val previous = companion.activeCharacterId
        if (previous != null && previous != characterId) {
            val base = MysticCharacterSessionState(
                activeCharacterId = previous,
                sessions = MysticCharacterId.entries.associateWith { companion.sessionFor(it) },
                sharedMemoryNotes = companion.sharedMemoryNotes
            )
            val switched = reduceCharacterSession(base, MysticCharacterEvent.Switch(characterId))
            switched.sessions.forEach { (id, state) -> companion.characterSessionStates[id] = state }
            companion.sharedMemoryNotes = switched.sharedMemoryNotes
            sessionState = switched.session(characterId)
            val handoffKey = "character-handoff-${previous.key}-${characterId.key}"
            val targetThread = companion.threadFor(characterId)
            if (targetThread.none { it.key == handoffKey }) {
                targetThread.add(
                    MysticTurn(
                        key = handoffKey,
                        question = "角色交接",
                        answer = MysticCharacterCatalog.byId(characterId).handoffLine,
                        kind = "handoff"
                    )
                )
                while (targetThread.size > 5) targetThread.removeAt(0)
            }
        }
        companion.activeCharacterId = characterId
    }
    var gameSession by remember { mutableStateOf(com.xuanji.app.domain.game.GameSessionState()) }
    var gameReply by remember { mutableStateOf("") }
    var gameInputEcho by remember { mutableStateOf<String?>(null) }
    var gameThinking by remember { mutableStateOf(false) }
    val boardWork = remember(coroutineScope) { com.xuanji.app.domain.game.BoardWorkRunner(coroutineScope) }
    DisposableEffect(visitProfile, characterId, boardWork) {
        onDispose { boardWork.cancel(); gameThinking = false }
    }
    var gameViewPly by remember { mutableStateOf<Int?>(null) }
    var companionGameId by MysticInteractionProperty(
        read = { currentInteraction().gameId?.takeUnless { it == "xiangqi" } },
        write = { value -> updateInteraction { it.copy(gameId = value, actionKey = if (value == null && it.actionKey == "game") "" else it.actionKey) } }
    )
    val gameArchiveStore = remember(context) { com.xuanji.app.data.local.GameArchiveStore(context) }
    val companionGameProgressStore = remember(context) {
        CompanionGameProgressStore(DataStorePreferenceBridge(context))
    }
    var gameRecord by remember { mutableStateOf(com.xuanji.app.domain.game.GameRecord()) }
    var gameArchive by remember { mutableStateOf<com.xuanji.app.domain.game.GameSave?>(null) }
    var gameRecordedToken by remember { mutableStateOf<Long?>(null) }
    var xiangqiFavoriteNotice by remember(visitProfile, characterId) { mutableStateOf("") }
    val gameBridge = remember {
        com.xuanji.app.domain.game.GameDialogueBridge(recordOf = { gameRecord })
    }

    suspend fun readCurrentGameContext(forCharacterId: MysticCharacterId): MysticGameContext? = runCatching {
        val game = com.xuanji.app.domain.game.CompanionGameCatalog.forCharacter(forCharacterId)
        val xiangqiSave = if (forCharacterId == MysticCharacterId.MoHeng) {
            gameArchiveStore.load(visitProfile)
        } else null
        val companionProgress = if (forCharacterId == MysticCharacterId.MoHeng) {
            null
        } else {
            companionGameProgressStore.load(visitProfile, forCharacterId, game.id)
        }
        MysticGameContextFactory.fromSaved(forCharacterId, companionProgress, xiangqiSave)
    }.getOrNull()
    var pendingCustom by remember(guide) { mutableStateOf<String?>(null) }
    var pendingCustomDetailContext by remember(guide) { mutableStateOf<MysticDetailContext?>(null) }
    LaunchedEffect(visitProfile) {
        gameRecord = runCatching { gameArchiveStore.loadRecord(visitProfile) }
            .getOrElse { com.xuanji.app.domain.game.GameRecord() }
        gameArchive = runCatching { gameArchiveStore.load(visitProfile) }.getOrNull()
    }
    LaunchedEffect(visitProfile) {
        // null = 存过但读不出来（或读盘失败）；空记忆 = 本机从没记过。两句话不一样。
        val loaded = runCatching { memoryStore.load(visitProfile) }.getOrNull()
        recollectionBroken = loaded == null
        storedMemory = loaded ?: ConversationMemory()
        // 开场句只反映进来时本机已存的内容；这次访问新记的东西不改写它。
        recollectionLine = MysticGuideGenerator.recallLine(
            mode,
            guide.styleKey,
            if (loaded == null) RecallFacts(unreadable = true) else RecollectionCodec.facts(loaded)
        )
        softMemoryTags = runCatching { softMemoryStore.read(visitProfile).tags }.getOrDefault(emptyList())
    }
    LaunchedEffect(guide) {
        sessionState = reduce(
            sessionState,
            MysticEvent.ChangeContext(
                mode = mode,
                topicKey = guide.topicKey,
                styleKey = guide.styleKey,
                skinId = companion.skinId
            )
        )
        // Context changes invalidate any in-flight custom request.
        pendingCustom = null
    }
    LaunchedEffect(visitProfile, fortune.dateKey) {
        val previousVisit = runCatching { visitStore.read(visitProfile) }.getOrNull()
        val previousMemory = previousVisit ?: MysticVisitMemory("", "", "")
        revisitLine = MysticGuideGenerator.revisitGreeting(
            mode,
            guide.styleKey,
            fortune,
            previousMemory
        )
        persistedVisitAction = previousMemory.lastAction
        visitReady = true
        runCatching {
            visitStore.save(
                visitProfile,
                MysticVisitMemory(fortune.dateKey, topic, previousMemory.lastAction)
            )
        }
    }
    var selectedFollowUp by remember(guide) { mutableStateOf("") }
    var evidenceOpen by MysticInteractionProperty(
        read = { currentInteraction().evidenceOpen }, write = { value -> updateInteraction { it.copy(evidenceOpen = value) } }
    )
    val conversation = companion.threadFor(characterId)
    val expandedBubbles = remember(characterId) { mutableStateMapOf<String, Boolean>() }
    var arrivalVisible by remember(guide) { mutableStateOf(false) }
    val openAsideTurnKey = conversation.firstOrNull { turn ->
        turn.aside != null && turn.asideExit == null
    }?.key
    var pendingFollowUp by remember(guide) { mutableStateOf<String?>(null) }
    var pendingInteraction by remember(guide) { mutableStateOf<MysticInteractionOption?>(null) }
    var pendingOpening by remember(guide) { mutableStateOf<MysticOpeningOption?>(null) }
    var openingAnswered by remember(guide) { mutableStateOf(false) }
    var selectedRhythm by remember(guide) { mutableStateOf<String?>(null) }
    var pendingRhythm by remember(guide) { mutableStateOf<String?>(null) }
    var customQuestion by remember(guide) { mutableStateOf("") }
    var interactionCount by remember(guide) { mutableIntStateOf(0) }
    var customCount by remember(guide) { mutableIntStateOf(0) }
    val askCounts = remember(guide) { mutableStateMapOf<String, Int>() }
    var interactionRound by remember(guide) { mutableIntStateOf(0) }
    var selectedInteraction by remember(guide, interactionRound) { mutableStateOf<MysticInteractionOption?>(null) }
    var selectedClarifier by remember(guide) { mutableStateOf<String?>(null) }
    var pendingClarify by remember(guide) { mutableStateOf<MysticClarifierOption?>(null) }
    var pendingAsideTurnKey by remember(guide) { mutableStateOf<String?>(null) }
    var pendingAsideActionTurnKey by remember(guide) { mutableStateOf<String?>(null) }
    var presenceOverride by remember(guide) { mutableStateOf<String?>(null) }
    val interaction = remember(mode, topic, fortune, interactionRound, companion.skinId) {
        MysticGuideGenerator.interaction(
            mode,
            topic,
            fortune,
            interactionRound,
            companion.skinId
        )
    }
    val opening: MysticOpeningCheckin? = remember(mode, guide.topicKey, guide.styleKey, companion.skinId, fortune) {
        MysticGuideGenerator.openingCheckin(
            mode,
            guide.topicKey,
            guide.styleKey,
            companion.skinId,
            fortune
        )
    }
    val rhythm: MysticRhythmCheckin? = remember(mode, guide.topicKey, guide.styleKey, fortune) {
        MysticGuideGenerator.rhythmCheckin(
            mode,
            guide.topicKey,
            guide.styleKey,
            fortune
        )
    }

    LaunchedEffect(guide) {
        if (pendingHandoff == null) {
            interactionCarryoverOption = null
        }
    }

    LaunchedEffect(guide) {
        guestCameo = null
        guestExit = null
        selectedGuestChoice = ""
        guestReply = ""
        guestQuestion = ""
        pendingGuest = false
        selectedClarifier = null
        pendingClarify = null
    }

    LaunchedEffect(guide) {
        kotlinx.coroutines.delay(180)
        arrivalVisible = true
    }

    /**
     * Long-term memory only ever holds what the user typed or tapped on this device.
     * A blank/nothing-said input produces no entry at all, and a failed write leaves the
     * visible snapshot untouched rather than pretending the note landed.
     */
    fun rememberLongTerm(kind: RecollectionKind, text: String, intent: String = topic) {
        val entry = RecollectionCodec.entryOf(fortune.dateKey, kind, text, intent) ?: return
        coroutineScope.launch {
            val updated = runCatching { memoryStore.remember(visitProfile, entry) }.getOrNull()
                ?: return@launch
            storedMemory = updated
        }
    }

    fun rememberMemory(kind: String, detail: String) {
        val text = MysticGuideGenerator.memoryNote(mode, guide.styleKey, kind, detail)
        if (text.isBlank()) return
        val note = MysticMemoryNote("memory-$kind-$memorySequence", text)
        val sharedNote = DomainMysticMemoryNote(note.id, note.text)
        memoryNotes = (listOf(note) + memoryNotes).take(3)
        companion.sharedMemoryNotes = (companion.sharedMemoryNotes + sharedNote)
            .distinctBy { it.id }
            .takeLast(12)
        sessionState = reduce(sessionState, MysticEvent.Remember(sharedNote))
        memorySequence += 1
    }

    fun rememberAsideMemory(choiceKey: String, detail: String) {
        val text = MysticGuideGenerator.asideMemoryNote(
            mode,
            guide.styleKey,
            choiceKey,
            detail
        )
        if (text.isBlank()) return
        val note = MysticMemoryNote("memory-aside-$memorySequence", text)
        val sharedNote = DomainMysticMemoryNote(note.id, note.text)
        memoryNotes = (listOf(note) + memoryNotes).take(3)
        companion.sharedMemoryNotes = (companion.sharedMemoryNotes + sharedNote)
            .distinctBy { it.id }
            .takeLast(12)
        sessionState = reduce(sessionState, MysticEvent.Remember(sharedNote))
        memorySequence += 1
    }

    fun selectFollowUp(key: String) {
        if (
            guide.followUps.none { it.key == key } ||
            pendingFollowUp != null ||
            pendingInteraction != null ||
            pendingHandoff != null ||
            pendingCustom != null ||
            pendingOpening != null ||
            pendingRhythm != null ||
            pendingGuest ||
            pendingClarify != null ||
            pendingAsideTurnKey != null ||
            pendingAsideActionTurnKey != null ||
            openAsideTurnKey != null
        ) return
        pendingFollowUp = key
    }

    fun rememberSoftTopic(text: String) {
        val analysis = DefaultMysticDialogueAnalyzer().analyze(
            text,
            DialogueContext(
                mode = mode,
                styleKey = guide.styleKey,
                topicKey = topic,
                fortune = fortune,
                latestTest = latestTest,
                recentTurns = sessionState.recentTurns,
                memoryNotes = companion.sharedMemoryNotes,
                skinId = companion.skinId,
                question = text,
                characterName = characterProfile.displayName,
                characterId = characterProfile.id
            )
        )
        val topicKey = analysis.topicKey ?: return
        if (analysis.intent == com.xuanji.app.domain.MysticIntent.Game) return
        val tag = SoftMemoryTag(
            id = "topic-$topicKey",
            key = "topic",
            label = "最近常聊",
            value = analysis.entities["topic_label"] ?: topicKey,
            source = SoftMemorySource.Inferred,
            createdAt = fortune.dateKey,
            lastSeenAt = fortune.dateKey,
            expiresAtDate = runCatching { LocalDate.parse(fortune.dateKey).plusDays(30).toString() }.getOrNull()
        )
        coroutineScope.launch {
            val updated = runCatching { softMemoryStore.upsertInferred(visitProfile, tag) }.getOrNull()
            if (updated != null) softMemoryTags = updated.tags
        }
    }

    fun submitCustom() {
        if (
            pendingFollowUp != null ||
            pendingInteraction != null ||
            pendingHandoff != null ||
            pendingCustom != null ||
            pendingOpening != null ||
            pendingRhythm != null ||
            pendingGuest ||
            pendingClarify != null ||
            pendingAsideTurnKey != null ||
            pendingAsideActionTurnKey != null ||
            openAsideTurnKey != null
        ) return
        val cleanQuestion = customQuestion.trim().take(200)
        if (cleanQuestion.isEmpty()) return
        pendingCustomDetailContext = stageDetailContext
        rememberLongTerm(RecollectionKind.USER_INPUT, cleanQuestion)
        rememberSoftTopic(cleanQuestion)
        sessionState = reduce(sessionState, MysticEvent.SendInput(cleanQuestion))
        pendingCustom = cleanQuestion
        customQuestion = ""
    }

    /**
     * Run one board action off the main thread, then keep playing until the human is to
     * move again. The local search blocks while it evaluates, and spectating hands both
     * sides to the engine, so a single command can produce several replies. Board state
     * only changes through a bridge result — every move shown already passed the rules
     * re-check inside the reducer.
     */
    fun driveGame(
        echo: String? = null,
        action: suspend (com.xuanji.app.domain.game.GameSessionState) -> com.xuanji.app.domain.game.GameDialogueBridge.Result
    ) {
        if (gameThinking) return
        gameThinking = true
        gameViewPly = null
        val initialBoard = gameSession
        boardWork.launch(onFinished = { gameThinking = false }, onError = {
            gameReply = "棋局计算暂时未完成，已落子局面保留，可重试。"
        }) {
            val ticket = this
            suspend fun fulfil(
                result: com.xuanji.app.domain.game.GameDialogueBridge.Result
            ): com.xuanji.app.domain.game.GameDialogueBridge.Result = when (result.archive) {
                com.xuanji.app.domain.game.GameDialogueBridge.ArchiveRequest.SAVE -> {
                    val save = com.xuanji.app.domain.game.GameArchive.saveOf(
                        result.state,
                        savedAt = System.currentTimeMillis()
                    )
                    if (runCatching { gameArchiveStore.save(visitProfile, save) }.isSuccess) {
                        ticket.publish { gameArchive = save }
                        result
                    } else {
                        result.copy(reply = "本机存储不可用，这一局没能保存。")
                    }
                }
                com.xuanji.app.domain.game.GameDialogueBridge.ArchiveRequest.RESUME -> {
                    val save = runCatching { gameArchiveStore.load(visitProfile) }.getOrNull()
                    gameBridge.resumeWith(result.state, save)
                }
                null -> result
            }
            suspend fun publish(result: com.xuanji.app.domain.game.GameDialogueBridge.Result) {
                ticket.publish {
                    if (result.event is com.xuanji.app.domain.game.GameEvent.Start) {
                        gameRecordedToken = null
                    }
                    gameSession = result.state
                    if (result.reply.isNotBlank()) gameReply = result.reply
                    if (echo != null && (result.event != null || result.grounded)) gameInputEcho = echo
                }
            }
            var result = fulfil(action(initialBoard))
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            publish(result)
            while (result.awaitEngine && kotlinx.coroutines.currentCoroutineContext().isActive) {
                result = gameBridge.engineReply(result.state)
                publish(result)
            }
            val settled = gameBridge.settledResult(result.state)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val token = result.state.sessionToken
            if (settled != null && gameRecordedToken != token) {
                gameRecordedToken = token
                rememberLongTerm(
                    RecollectionKind.SETTLED_GAME_RESULT,
                    com.xuanji.app.domain.game.GameRecord.settledNote(settled),
                    intent = ""
                )
                val updated = runCatching { gameArchiveStore.record(visitProfile, settled) }.getOrNull()
                if (updated != null) {
                    ticket.publish { gameRecord = updated }
                }
            }
        }
    }

    fun runGameText(text: String, echo: String = text) {
        driveGame(echo) { gameBridge.handle(it, text, kotlinx.coroutines.currentCoroutineContext()) }
    }

    fun submitPanelInput(text: String) {
        // Game path first: board-game intents bypass the generic pendingCustom reply so
        // character game commentary never mixes with fortune template wording.
        val cleanText = text.trim().take(200)
        actionRequestSequence++
        val conditions = com.xuanji.app.domain.TemporaryActionConditions.parse(cleanText)
        if (conditions.active) {
            val token = sessionState.sessionToken
            val sequence = actionRequestSequence
            coroutineScope.launch {
                val budget = if (conditions.cheaper) dailyActionPlan?.meals?.firstOrNull()?.estimatedPriceCents?.let { (it - 500).coerceAtLeast(100) } else null
                val next = temporaryConditions.merge(conditions.copy(maxBudgetCents = conditions.maxBudgetCents ?: budget))
                val recalculated = AppModule.actionRepository.loadToday(java.time.LocalDate.parse(fortune.dateKey), next)
                if (token != sessionState.sessionToken || sequence != actionRequestSequence) return@launch
                temporaryConditions = next
                dailyActionPlan = recalculated
                referenceCategory = if (conditions.excludedIngredients.isNotEmpty() || conditions.maxBudgetCents != null || conditions.cheaper) "meal"
                    else if (referenceCategory == "outing") "outing" else "activity"
                currentReferenceId = com.xuanji.app.domain.MysticReferenceResolver.fromPlan(recalculated, referenceCategory).firstOrNull()?.id
                routedAction = recalculated?.let { when (referenceCategory) {
                    "meal" -> MysticCompanionAction.ShowTodayMeal(it)
                    "outing" -> MysticCompanionAction.ShowTodayOuting(it)
                    else -> MysticCompanionAction.ShowTodayActivity(it)
                } }
                temporaryStatus = "本次条件已用于重新筛选；长期偏好尚未改动。"
                customQuestion = cleanText
                submitCustom()
            }
            return
        }
        if (MysticGameProgressQuestion.isResumeRequest(cleanText)) {
            val requestedCharacterId = characterId
            val requestToken = sessionState.sessionToken
            coroutineScope.launch {
                val saved = readCurrentGameContext(requestedCharacterId)
                if (requestToken != sessionState.sessionToken || requestedCharacterId != characterId) return@launch
                if (saved?.isResumableFor(requestedCharacterId) == true) {
                    val game = com.xuanji.app.domain.game.CompanionGameCatalog.forCharacter(requestedCharacterId)
                    if (game.id == "xiangqi") runGameText("继续棋局") else companionGameId = game.id
                } else {
                    // Let the offline engine explain the missing/ended save; never open a fresh game
                    // under a command that explicitly asked to resume.
                    customQuestion = cleanText
                    submitCustom()
                }
            }
            return
        }
        if (MysticGameProgressQuestion.isProgressQuestion(cleanText)) {
            customQuestion = cleanText
            submitCustom()
            return
        }
        com.xuanji.app.domain.MysticReferenceResolver.requestedIndex(cleanText)?.let { index ->
            currentReferenceId = com.xuanji.app.domain.MysticReferenceResolver.fromPlan(dailyActionPlan, referenceCategory).getOrNull(index - 1)?.id
            val selectedPlan = com.xuanji.app.domain.MysticReferenceResolver.selectPlan(dailyActionPlan, referenceCategory, currentReferenceId)
            routedAction = selectedPlan?.let { selected -> when (referenceCategory) {
                "meal" -> MysticCompanionAction.ShowTodayMeal(selected)
                "activity" -> MysticCompanionAction.ShowTodayActivity(selected)
                "outing" -> MysticCompanionAction.ShowTodayOuting(selected)
                else -> null
            } }
            customQuestion = cleanText
            submitCustom()
            return
        }
        val action = MysticCompanionActionRouter.route(
            cleanText,
            dailyActionPlan,
            lifeProfile,
            gameArchive != null,
            referenceCategory,
            currentReferenceId
        )
        if (action != null) {
            routedAction = action
            referenceCategory = when (action) {
                is MysticCompanionAction.ShowTodayMeal -> "meal"
                is MysticCompanionAction.ShowTodayActivity -> "activity"
                is MysticCompanionAction.ShowTodayOuting -> "outing"
                else -> referenceCategory
            }
            if (action is MysticCompanionAction.ShowTodayMeal || action is MysticCompanionAction.ShowTodayActivity || action is MysticCompanionAction.ShowTodayOuting) {
                currentReferenceId = com.xuanji.app.domain.MysticReferenceResolver.fromPlan(dailyActionPlan, referenceCategory).firstOrNull()?.id
            }
            when (action) {
                MysticCompanionAction.ShowEvidence -> evidenceOpen = true
                MysticCompanionAction.ResumeXiangqi -> runGameText("继续棋局")
                is MysticCompanionAction.OpenGame -> companionGameId = action.gameId
                MysticCompanionAction.OpenConversation -> Unit
                is MysticCompanionAction.ShowTodayMeal,
                is MysticCompanionAction.ShowTodayActivity,
                is MysticCompanionAction.ShowTodayOuting,
                is MysticCompanionAction.ShowLifeProfile -> Unit
            }
            if (action !is MysticCompanionAction.OpenGame && action != MysticCompanionAction.ResumeXiangqi) {
                // Keep the natural-language answer in the thread while the card renders the same data.
                customQuestion = cleanText
                submitCustom()
            }
            return
        }
        val companionGameIdFromInput = com.xuanji.app.domain.game.CompanionGameCatalog
            .gameIdForInput(cleanText)
        if (companionGameIdFromInput != null && companionGameIdFromInput != "xiangqi") {
            rememberLongTerm(RecollectionKind.USER_INPUT, cleanText, intent = "")
            companionGameId = companionGameIdFromInput
            return
        }
        val gameIntent = com.xuanji.app.domain.MysticIntentClassifier.classify(cleanText) ==
            com.xuanji.app.domain.MysticIntent.Game ||
            gameBridge.activeGame(gameSession)
        if (gameIntent) {
            // intent 留空：棋桌上的话不该被召回句算成「聊过某个命理主题」。
            rememberLongTerm(RecollectionKind.USER_INPUT, cleanText, intent = "")
            runGameText(cleanText)
            return
        }
        customQuestion = text
        submitCustom()
    }

    LaunchedEffect(stageActionRequest, characterId, characterSessionsLoaded) {
        if (!characterSessionsLoaded) return@LaunchedEffect
        val action = stageActionRequest ?: return@LaunchedEffect
        if (action.startsWith("__companion_game__:")) {
            companionGameId = action.substringAfter(":").takeIf { it.isNotBlank() }
        } else if (action == MYSTIC_DETAIL_QUESTION_ACTION) {
            stageDetailContext?.let { detail ->
                val task = com.xuanji.app.domain.MysticTaskReference(detail, characterId)
                if (task.valid()) updateInteraction { it.copy(task = task) }
            }
            val title = stageDetailContext?.title?.takeIf(String::isNotBlank) ?: "当前详情"
            submitPanelInput("请结合我当前打开的「$title」继续解读")
        } else {
            submitPanelInput(action)
        }
        onStageActionConsumed()
    }

    fun resetCurrentThread() {
        actionRequestSequence++
        sessionState = reduce(sessionState, MysticEvent.Clear)
        pendingCustomDetailContext = null
        conversation.clear()
        selectedFollowUp = ""
        selectedInteraction = null
        interactionCarryoverOption = null
        askCounts.clear()
        pendingFollowUp = null
        pendingInteraction = null
        pendingHandoff = null
        pendingHandoffEcho = null
        pendingCustom = null
        pendingOpening = null
        openingAnswered = false
        selectedRhythm = null
        pendingRhythm = null
        completedRhythmKey = null
        rhythmAnswered = false
        guestCameo = null
        guestExit = null
        guestChoiceCarryoverKey = null
        pendingGuestChoiceEcho = null
        memoryNotes = emptyList()
        memorySequence = 0
        memoryExpanded = false
        selectedGuestChoice = ""
        guestReply = ""
        guestQuestion = ""
        pendingGuest = false
        customQuestion = ""
        selectedClarifier = null
        pendingClarify = null
        presenceOverride = null
        pendingAsideTurnKey = null
        pendingAsideActionTurnKey = null
        companion.characterSessionStates[characterId] = sessionState
        coroutineScope.launch { runCatching { characterSessionStore.clearCharacter(visitProfile, characterId) } }
    }

    fun revokeSoftTag(id: String) {
        coroutineScope.launch {
            val updated = runCatching { softMemoryStore.revoke(visitProfile, id) }.getOrNull()
            if (updated != null) softMemoryTags = updated.tags
        }
    }

    fun clearSoftMemory() {
        coroutineScope.launch {
            runCatching { softMemoryStore.clear(visitProfile) }
                .onSuccess { softMemoryTags = emptyList() }
        }
    }

    fun cancelPanelReply() {
        actionRequestSequence++
        val pending = sessionState.requestState as? com.xuanji.app.domain.MysticRequestState.Pending ?: return
        pendingCustom = null
        pendingCustomDetailContext = null
        sessionState = reduce(sessionState, MysticEvent.CancelReply(pending.sessionToken, pending.turnId))
    }

    fun retryPanelReply() {
        val failed = sessionState.requestState as? com.xuanji.app.domain.MysticRequestState.Failed ?: return
        sessionState = reduce(sessionState, MysticEvent.RetryTurn)
        pendingCustom = failed.input
    }

    fun selectClarifier(option: MysticClarifierOption) {
        val customTurn = conversation.lastOrNull()?.takeIf { it.kind == "custom" } ?: return
        val available = MysticGuideGenerator.customClarifier(
            mode,
            guide.styleKey,
            customTurn.question,
            fortune,
            latestTest
        )?.options?.any { it == option } == true
        if (
            !available ||
            pendingFollowUp != null ||
            pendingInteraction != null ||
            pendingHandoff != null ||
            pendingCustom != null ||
            pendingOpening != null ||
            pendingRhythm != null ||
            pendingGuest ||
            pendingClarify != null ||
            pendingAsideTurnKey != null ||
            pendingAsideActionTurnKey != null ||
            openAsideTurnKey != null
        ) return
        selectedClarifier = option.key
        pendingClarify = option
    }

    fun requestAside(key: String) {
        val turn = conversation.firstOrNull { it.key == key } ?: return
        if (
            turn.aside != null ||
            pendingFollowUp != null ||
            pendingInteraction != null ||
            pendingHandoff != null ||
            pendingCustom != null ||
            pendingOpening != null ||
            pendingRhythm != null ||
            pendingGuest ||
            pendingClarify != null ||
            selectedClarifier != null ||
            openAsideTurnKey != null ||
            pendingAsideTurnKey != null
        ) return
        pendingAsideTurnKey = key
    }

    LaunchedEffect(pendingAsideTurnKey, guide) {
        val key = pendingAsideTurnKey ?: return@LaunchedEffect
        kotlinx.coroutines.delay(400)
        val turnIndex = conversation.indexOfFirst { it.key == key }
        if (turnIndex < 0 || conversation[turnIndex].aside != null || pendingAsideTurnKey != key) {
            if (pendingAsideTurnKey == key) pendingAsideTurnKey = null
            return@LaunchedEffect
        }
        val turn = conversation[turnIndex]
        val aside = MysticGuideGenerator.asideInvite(
            mode,
            guide.styleKey,
            guide.topicKey,
            turn.kind,
            fortune,
            turn.question
        )
        if (aside == null) {
            pendingAsideTurnKey = null
            return@LaunchedEffect
        }
        conversation[turnIndex] = turn.copy(aside = aside)
        pendingAsideTurnKey = null
    }

    fun selectAsideChoice(key: String, choice: MysticGuestChoice) {
        val turnIndex = conversation.indexOfFirst { it.key == key }
        if (
            turnIndex < 0 ||
            openAsideTurnKey != key ||
            pendingFollowUp != null ||
            pendingInteraction != null ||
            pendingHandoff != null ||
            pendingCustom != null ||
            pendingOpening != null ||
            pendingRhythm != null ||
            pendingGuest ||
            pendingClarify != null ||
            selectedClarifier != null ||
            pendingAsideTurnKey != null ||
            pendingAsideActionTurnKey != null
        ) return

        val turn = conversation[turnIndex]
        if (
            turn.aside == null ||
            turn.asideChoice.isNotEmpty() ||
            MysticGuideGenerator.asideChoices().none { it.key == choice.key }
        ) return

        conversation[turnIndex] = turn.copy(
            asidePrompt = choice.label,
            asideChoice = choice.key
        )
        pendingAsideActionTurnKey = key
    }

    LaunchedEffect(pendingAsideActionTurnKey, guide) {
        val key = pendingAsideActionTurnKey ?: return@LaunchedEffect
        kotlinx.coroutines.delay(320)
        var turnIndex = conversation.indexOfFirst { it.key == key }
        var turn = conversation.getOrNull(turnIndex)
        if (turn == null || turn.aside == null || turn.asideChoice.isEmpty() || pendingAsideActionTurnKey != key) {
            if (pendingAsideActionTurnKey == key) pendingAsideActionTurnKey = null
            return@LaunchedEffect
        }

        val reply = MysticGuideGenerator.asideResponse(
            mode,
            guide.styleKey,
            guide.topicKey,
            turn.kind,
            fortune,
            turn.question,
            turn.asideChoice
        )
        if (reply.isBlank()) {
            pendingAsideActionTurnKey = null
            return@LaunchedEffect
        }
        conversation[turnIndex] = turn.copy(asideReply = reply)

        kotlinx.coroutines.delay(480)
        turnIndex = conversation.indexOfFirst { it.key == key }
        turn = conversation.getOrNull(turnIndex)
        if (turn == null || turn.aside == null || turn.asideChoice.isEmpty() || pendingAsideActionTurnKey != key) {
            if (pendingAsideActionTurnKey == key) pendingAsideActionTurnKey = null
            return@LaunchedEffect
        }

        conversation[turnIndex] = turn.copy(
            asideExit = MysticGuideGenerator.guestExitCameo(
                mode,
                guide.styleKey,
                fortune,
                turn.asideChoice
            ),
            asideWrapup = MysticGuideGenerator.asideHostWrapup(
                mode,
                guide.styleKey,
                guide.topicKey,
                turn.kind,
                fortune,
                turn.question,
                turn.asideChoice
            )
        )
        presenceOverride = MysticGuideGenerator.asidePresenceState(mode, guide.styleKey)
        rememberAsideMemory(turn.asideChoice, turn.question)
        pendingAsideActionTurnKey = null
    }

    fun switchPersona(targetMode: String, targetSkinId: String? = null) {
        if (
            pendingFollowUp != null ||
            pendingInteraction != null ||
            pendingHandoff != null ||
            pendingCustom != null ||
            pendingOpening != null ||
            pendingRhythm != null ||
            pendingGuest ||
            pendingClarify != null ||
            pendingAsideTurnKey != null ||
            pendingAsideActionTurnKey != null ||
            openAsideTurnKey != null
        ) return
        interactionCarryoverOption = null
        guestChoiceCarryoverKey = null
        guestExit = null
        pendingGuestChoiceEcho = null
        selectedClarifier = null
        pendingClarify = null
        presenceOverride = null
        memoryNotes = emptyList()
        memorySequence = 0
        memoryExpanded = false
        mode = targetMode
        companion.skinId = targetSkinId
            ?: MysticGuideGenerator.defaultMysticSkin(targetMode, fortune).id
        onStageModeChange(targetMode)
    }

    fun selectTopic(key: String) {
        if (
            key == topic ||
            pendingFollowUp != null ||
            pendingInteraction != null ||
            pendingHandoff != null ||
            pendingCustom != null ||
            pendingOpening != null ||
            pendingRhythm != null ||
            pendingGuest ||
            pendingClarify != null ||
            pendingAsideTurnKey != null ||
            pendingAsideActionTurnKey != null ||
            openAsideTurnKey != null
        ) return
        val previousTopic = topic
        val carryover = MysticGuideGenerator.interactionCarryover(
            mode,
            guide.styleKey,
            interactionCarryoverOption.orEmpty()
        )
        val guestEcho = MysticGuideGenerator.guestChoiceCarryover(
            mode,
            guide.styleKey,
            guestChoiceCarryoverKey.orEmpty()
        )
        selectedInteraction = null
        pendingFollowUp = null
        pendingInteraction = null
        selectedClarifier = null
        pendingClarify = null
        topic = key
        pendingHandoff = previousTopic
        pendingHandoffEcho = carryover
        pendingGuestChoiceEcho = guestEcho
        guestChoiceCarryoverKey = null
        guestExit = null
        if (visitReady) {
            coroutineScope.launch {
                runCatching {
                    visitStore.save(
                        visitProfile,
                        MysticVisitMemory(fortune.dateKey, key, persistedVisitAction)
                    )
                }
            }
        }
    }

    LaunchedEffect(pendingHandoff, guide) {
        val fromTopic = pendingHandoff ?: return@LaunchedEffect
        kotlinx.coroutines.delay(420)
        conversation.add(
            MysticTurn(
                key = "handoff-$fromTopic-${guide.topicKey}",
                question = "刚才在看「${MysticGuideGenerator.topicLabel(fromTopic)}」",
                answer = MysticGuideGenerator.topicHandoff(
                    mode,
                    guide.styleKey,
                    fromTopic,
                    guide.topicKey
                ),
                reaction = MysticGuideGenerator.composeReaction(
                    MysticGuideGenerator.rhythmCarryover(
                        mode,
                        guide.styleKey,
                        completedRhythmKey.orEmpty()
                    ),
                    MysticGuideGenerator.composeReaction(
                        pendingHandoffEcho.orEmpty(),
                        MysticGuideGenerator.composeReaction(
                            pendingGuestChoiceEcho.orEmpty(),
                            MysticGuideGenerator.handoffReaction(mode, guide.styleKey)
                        )
                    )
                ),

                kind = "handoff"
            )
        )
        while (conversation.size > 5) conversation.removeAt(0)
        interactionCarryoverOption = null
        completedRhythmKey = null
        pendingHandoff = null
        pendingHandoffEcho = null
        pendingGuestChoiceEcho = null
        pendingOpening = null
        rememberMemory("handoff", MysticGuideGenerator.topicLabel(fromTopic))
        rememberLongTerm(RecollectionKind.USER_CHOICE, MysticGuideGenerator.topicLabel(topic))
    }

    fun selectOpening(option: MysticOpeningOption) {
        if (
            openingAnswered ||
            pendingFollowUp != null ||
            pendingInteraction != null ||
            pendingHandoff != null ||
            pendingCustom != null ||
            pendingOpening != null ||
            pendingRhythm != null ||
            pendingGuest ||
            pendingClarify != null ||
            pendingAsideTurnKey != null ||
            pendingAsideActionTurnKey != null ||
            openAsideTurnKey != null ||
            opening?.options?.none { it.key == option.key } != false
        ) return
        pendingOpening = option
    }

    LaunchedEffect(pendingOpening, guide) {
        val option = pendingOpening ?: return@LaunchedEffect
        kotlinx.coroutines.delay(360)
        val expectedOption = MysticGuideGenerator.openingCheckin(
            mode,
            guide.topicKey,
            guide.styleKey,
            companion.skinId,
            fortune
        )?.options?.firstOrNull { it.key == option.key }
        val currentStyleKey = MysticGuideGenerator.styleKeyFor(mode, guide.topicKey, fortune)
        val guideMatches = guide.mode == mode &&
            guide.topicKey == topic &&
            guide.styleKey == currentStyleKey
        if (!guideMatches || expectedOption == null || pendingOpening?.key != option.key) {
            pendingOpening = null
            return@LaunchedEffect
        }
        conversation.add(
            MysticTurn(
                key = "opening-${guide.topicKey}-${expectedOption.key}",
                question = expectedOption.label,
                answer = expectedOption.response,
                reaction = MysticGuideGenerator.openingReaction(mode, guide.styleKey),
                kind = "opening"
            )
        )
        while (conversation.size > 5) conversation.removeAt(0)
        openingAnswered = true
        pendingOpening = null
        rememberMemory("opening", expectedOption.label)
        rememberLongTerm(RecollectionKind.USER_CHOICE, expectedOption.label)
        persistedVisitAction = expectedOption.label
        runCatching {
            visitStore.save(
                visitProfile,
                MysticVisitMemory(fortune.dateKey, topic, expectedOption.label)
            )
        }
    }

    fun selectRhythm(key: String) {
        if (
            !openingAnswered ||
            rhythmAnswered ||
            rhythm?.options?.none { it.key == key } != false ||
            pendingFollowUp != null ||
            pendingInteraction != null ||
            pendingHandoff != null ||
            pendingCustom != null ||
            pendingOpening != null ||
            pendingRhythm != null ||
            pendingGuest ||
            pendingClarify != null ||
            pendingAsideTurnKey != null ||
            pendingAsideActionTurnKey != null ||
            openAsideTurnKey != null
        ) return
        selectedRhythm = key
        pendingRhythm = key
    }

    LaunchedEffect(pendingRhythm, guide) {
        val key = pendingRhythm ?: return@LaunchedEffect
        kotlinx.coroutines.delay(370)
        val expectedOption = MysticGuideGenerator.rhythmCheckin(
            mode,
            guide.topicKey,
            guide.styleKey,
            fortune
        )?.options?.firstOrNull { it.key == key }
        val currentStyleKey = MysticGuideGenerator.styleKeyFor(mode, guide.topicKey, fortune)
        val guideMatches = guide.mode == mode &&
            guide.topicKey == topic &&
            guide.styleKey == currentStyleKey
        if (!guideMatches || expectedOption == null || pendingRhythm != key) {
            selectedRhythm = null
            pendingRhythm = null
            return@LaunchedEffect
        }
        conversation.add(
            MysticTurn(
                key = "rhythm-${guide.topicKey}-$key",
                question = expectedOption.label,
                answer = expectedOption.response,
                reaction = MysticGuideGenerator.rhythmReaction(mode, guide.styleKey),
                kind = "rhythm"
            )
        )
        while (conversation.size > 5) conversation.removeAt(0)
        completedRhythmKey = key
        rhythmAnswered = true
        guestCameo = guestCameo ?: MysticGuideGenerator.guestCameo(
            mode,
            guide.topicKey,
            fortune,
            key
        )
        pendingRhythm = null
        rememberMemory("rhythm", expectedOption.label)
        rememberLongTerm(RecollectionKind.USER_CHOICE, expectedOption.label)
    }

    fun requestGuestReply(key: String) {
        val choice = MysticGuideGenerator.guestChoices().firstOrNull { it.key == key }
        if (
            guestCameo == null ||
            choice == null ||
            guestReply.isNotBlank() ||
            pendingFollowUp != null ||
            pendingInteraction != null ||
            pendingHandoff != null ||
            pendingCustom != null ||
            pendingOpening != null ||
            pendingRhythm != null ||
            pendingGuest ||
            pendingClarify != null ||
            pendingAsideTurnKey != null ||
            pendingAsideActionTurnKey != null ||
            openAsideTurnKey != null
        ) return
        selectedGuestChoice = key
        pendingGuest = true
    }

    LaunchedEffect(pendingGuest, guestCameo, guide) {
        if (!pendingGuest || guestCameo == null) return@LaunchedEffect
        kotlinx.coroutines.delay(320)
        if (!pendingGuest || guestCameo == null) return@LaunchedEffect
        val choice = MysticGuideGenerator.guestChoices().firstOrNull { it.key == selectedGuestChoice }
        if (choice == null) {
            selectedGuestChoice = ""
            pendingGuest = false
            return@LaunchedEffect
        }
        val rhythmKey = completedRhythmKey.orEmpty()
        guestQuestion = choice.label
        val reply = MysticGuideGenerator.guestReply(
            mode,
            guide.topicKey,
            fortune,
            rhythmKey,
            choice.key
        )
        guestReply = reply
        guestExit = if (reply.isBlank()) {
            null
        } else {
            MysticGuideGenerator.guestExitCameo(
                mode,
                guide.styleKey,
                fortune,
                choice.key
            )
        }
        conversation.add(
            MysticTurn(
                key = "guest-${guide.topicKey}-${choice.key}",
                question = choice.label,
                answer = MysticGuideGenerator.guestHostWrapup(
                    mode,
                    guide.styleKey,
                    guide.topicKey,
                    fortune,
                    rhythmKey,
                    choice.key
                ),
                reaction = MysticGuideGenerator.composeReaction(
                    MysticGuideGenerator.rhythmCarryover(mode, guide.styleKey, rhythmKey),
                    MysticGuideGenerator.interactionCarryover(
                        mode,
                        guide.styleKey,
                        interactionCarryoverOption.orEmpty()
                    )
                ),
                kind = "ask"
            )
        )
        while (conversation.size > 5) conversation.removeAt(0)
        interactionCarryoverOption = null
        completedRhythmKey = null
        pendingGuest = false
        guestChoiceCarryoverKey = choice.key
        rememberMemory("guest", choice.label)
        rememberLongTerm(RecollectionKind.USER_CHOICE, choice.label)
    }

    LaunchedEffect(pendingCustom, pendingCustomDetailContext, guide, characterId) {
        val question = pendingCustom ?: return@LaunchedEffect
        val requestToken = sessionState.sessionToken
        val requestTurnId = (sessionState.requestState as? com.xuanji.app.domain.MysticRequestState.Pending)?.turnId
            ?: return@LaunchedEffect
        kotlinx.coroutines.delay(430)
        if (requestToken != sessionState.sessionToken) return@LaunchedEffect
        val gameContext = if (
            MysticGameProgressQuestion.isProgressQuestion(question) ||
            MysticGameProgressQuestion.isResumeRequest(question)
        ) {
            readCurrentGameContext(characterId)
        } else null
        if (requestToken != sessionState.sessionToken) return@LaunchedEffect
        val dialogueContext = DialogueContext(
            profileKey = visitProfile,
            dateKey = fortune.dateKey,
            mode = mode,
            styleKey = guide.styleKey,
            topicKey = guide.topicKey,
            fortune = fortune,
            latestTest = latestTest,
            recentTurns = sessionState.recentTurns,
            memoryNotes = companion.sharedMemoryNotes,
            skinId = companion.skinId,
            question = question,
            characterName = characterProfile.displayName,
            characterId = characterProfile.id,
            dailyActionPlan = dailyActionPlan,
            lifeProfile = lifeProfile,
            detailContext = pendingCustomDetailContext,
            gameContext = gameContext,
            temporaryConditions = temporaryConditions,
            referenceCandidates = com.xuanji.app.domain.MysticReferenceResolver.fromPlan(dailyActionPlan, referenceCategory),
            currentReferenceId = currentReferenceId,
            referenceCategory = referenceCategory
        )
        val providerResult = dialogueProvider.complete(
            DialogueRequest(dialogueContext, question, requestToken)
        )
        if (requestToken != sessionState.sessionToken) {
            pendingCustom = null
            return@LaunchedEffect
        }
        val dialogueText = when (providerResult) {
            is ProviderResult.Success -> providerResult.text
            is ProviderResult.Failure -> "我先把这句记下了，盘面暂时没能接上；稍后再问一次就好。"
        }
        conversation.add(
            MysticTurn(
                key = "custom-$customCount-${question.hashCode()}",
                question = question,
                answer = dialogueText,
                reaction = MysticGuideGenerator.composeReaction(
                    MysticGuideGenerator.rhythmCarryover(
                        mode,
                        guide.styleKey,
                        completedRhythmKey.orEmpty()
                    ),
                    MysticGuideGenerator.composeReaction(
                        MysticGuideGenerator.interactionCarryover(
                            mode,
                            guide.styleKey,
                            interactionCarryoverOption.orEmpty()
                        ),
                        MysticGuideGenerator.composeReaction(
                            MysticGuideGenerator.guestChoiceCarryover(
                                mode,
                                guide.styleKey,
                                guestChoiceCarryoverKey.orEmpty()
                            ),
                            MysticGuideGenerator.customReaction(mode, guide.styleKey, question)
                        )
                    )
                ),

                kind = "custom"
            )
        )
        while (conversation.size > 5) conversation.removeAt(0)
        sessionState = reduce(
            sessionState,
            if (providerResult is ProviderResult.Success) {
                MysticEvent.ReplySucceeded(
                    requestToken,
                    requestTurnId,
                    com.xuanji.app.domain.DialogueReply(dialogueEngine.classify(question), "", dialogueText)
                )
            } else {
                MysticEvent.ReplyFailed(
                    requestToken,
                    requestTurnId,
                    (providerResult as ProviderResult.Failure).reason
                )
            }
        )
        customCount += 1
        interactionCarryoverOption = null
        completedRhythmKey = null
        guestChoiceCarryoverKey = null
        pendingCustom = null
        if (providerResult is ProviderResult.Success) pendingCustomDetailContext = null
        rememberMemory("ask", question)
    }

    LaunchedEffect(pendingClarify, guide) {
        val option = pendingClarify ?: return@LaunchedEffect
        kotlinx.coroutines.delay(370)
        val customTurn = conversation.lastOrNull()?.takeIf { it.kind == "custom" }
        if (customTurn == null || pendingClarify != option) {
            selectedClarifier = null
            pendingClarify = null
            return@LaunchedEffect
        }
        val expectedOption = MysticGuideGenerator.customClarifier(
            mode,
            guide.styleKey,
            customTurn.question,
            fortune,
            latestTest
        )?.options?.firstOrNull { it.key == option.key }
        if (expectedOption == null) {
            selectedClarifier = null
            pendingClarify = null
            return@LaunchedEffect
        }
        conversation.add(
            MysticTurn(
                key = "clarify-$customCount-${option.key}-${customTurn.question.hashCode()}",
                question = expectedOption.label,
                answer = expectedOption.answer,
                reaction = MysticGuideGenerator.customReaction(mode, guide.styleKey, expectedOption.label),
                kind = "clarify"
            )
        )
        while (conversation.size > 5) conversation.removeAt(0)
        guestCameo = guestCameo ?: MysticGuideGenerator.clarifierGuestCameo(
            mode,
            guide.topicKey,
            fortune,
            option.key
        )
        selectedClarifier = null
        pendingClarify = null
    }

    LaunchedEffect(pendingInteraction) {
        val option = pendingInteraction ?: return@LaunchedEffect
        kotlinx.coroutines.delay(380)
        conversation.add(
            MysticTurn(
                key = "game-$interactionCount-${option.label}",
                question = option.label,
                answer = option.feedback,
                reaction = MysticGuideGenerator.composeReaction(
                    MysticGuideGenerator.rhythmCarryover(
                        mode,
                        guide.styleKey,
                        completedRhythmKey.orEmpty()
                    ),
                    MysticGuideGenerator.composeReaction(
                        MysticGuideGenerator.interactionCarryover(
                            mode,
                            guide.styleKey,
                            interactionCarryoverOption.orEmpty()
                        ),
                        MysticGuideGenerator.composeReaction(
                            MysticGuideGenerator.guestChoiceCarryover(
                                mode,
                                guide.styleKey,
                                guestChoiceCarryoverKey.orEmpty()
                            ),
                            MysticGuideGenerator.interactionReaction(
                                mode,
                                guide.styleKey,
                                companion.skinId
                            )
                        )
                    )
                ),
                kind = "game"
            )
        )
        while (conversation.size > 5) conversation.removeAt(0)
        interactionCount += 1
        interactionCarryoverOption = option.label
        completedRhythmKey = null
        guestChoiceCarryoverKey = null
        pendingInteraction = null
        rememberMemory("game", option.label)
        rememberLongTerm(RecollectionKind.USER_CHOICE, option.label, intent = "")
    }

    LaunchedEffect(pendingFollowUp) {
        val key = pendingFollowUp ?: return@LaunchedEffect
        kotlinx.coroutines.delay(400)
        val item = guide.followUps.firstOrNull { it.key == key } ?: run {
            pendingFollowUp = null
            return@LaunchedEffect
        }
        val askedCount = (askCounts[key] ?: 0) + 1
        val action = when {
            selectedFollowUp == key -> "repeat"
            conversation.isNotEmpty() -> "branch"
            else -> "ask"
        }
        val reactionLine = MysticGuideGenerator.composeReaction(
            MysticGuideGenerator.rhythmCarryover(
                mode,
                guide.styleKey,
                completedRhythmKey.orEmpty()
            ),
            MysticGuideGenerator.composeReaction(
                MysticGuideGenerator.interactionCarryover(
                    mode,
                    guide.styleKey,
                    interactionCarryoverOption.orEmpty()
                ),
                MysticGuideGenerator.composeReaction(
                    MysticGuideGenerator.guestChoiceCarryover(
                        mode,
                        guide.styleKey,
                        guestChoiceCarryoverKey.orEmpty()
                    ),
                    MysticGuideGenerator.reaction(mode, action, askedCount, guide.styleKey)
                )
            ),
        )
        if (action == "repeat" && conversation.lastOrNull()?.key == key) {
            conversation[conversation.lastIndex] = conversation.last().copy(reaction = reactionLine)
        } else {
            val branchIndex = conversation.indexOfFirst { it.key == key }
            val kept = if (branchIndex >= 0) conversation.take(branchIndex) else conversation.toList()
            conversation.clear()
            conversation.addAll(kept.takeLast(3))
            conversation.add(MysticTurn(key, item.question, item.answer, reactionLine))
        }
        askCounts[key] = askedCount
        selectedFollowUp = key
        interactionCarryoverOption = null
        completedRhythmKey = null
        guestChoiceCarryoverKey = null
        pendingFollowUp = null
        rememberMemory("ask", item.question)
        rememberLongTerm(RecollectionKind.USER_CHOICE, item.question)
    }
    val accent by animateColorAsState(
        targetValue = if (mode == "half") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = tween(260),
        label = "mysticAccent"
    )
    val skins = remember(mode) { MysticGuideGenerator.mysticSkins(mode) }
    var skinIndex by remember(mode, fortune) {
        mutableIntStateOf(skins.indexOf(MysticGuideGenerator.defaultMysticSkin(mode, fortune)))
    }
    var skinSource by remember(mode, fortune) { mutableStateOf("daily") }
    LaunchedEffect(skinIndex) {
        companion.skinId = skins.getOrNull(skinIndex)?.id.orEmpty()
    }
    val skin = skins[skinIndex.coerceIn(skins.indices)]
    var pendingSkinId by remember { mutableStateOf<String?>(null)
    }

    LaunchedEffect(mode, skin.id) {
        onStageModeChange(mode)
        onStageSkinChange(skin.id)
    }
    LaunchedEffect(stageCostumeRequest, mode, skins) {
        val request = stageCostumeRequest ?: return@LaunchedEffect
        val (targetMode, targetSkinId) = request
        if (targetMode != mode) {
            pendingSkinId = targetSkinId
            switchPersona(targetMode, targetSkinId)
        } else {
            val index = skins.indexOfFirst { it.id == targetSkinId }
            if (index >= 0) {
                skinIndex = index
                companion.skinId = targetSkinId
            }
        }
        onStageCostumeConsumed()
    }
    LaunchedEffect(mode, pendingSkinId, skins) {
        val targetSkinId = pendingSkinId ?: return@LaunchedEffect
        val index = skins.indexOfFirst { it.id == targetSkinId }
        if (index >= 0) {
            skinIndex = index
            companion.skinId = targetSkinId
        }
        pendingSkinId = null
    }

    if (immersive) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            val availableControlHeight = maxHeight
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 86.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (recollectionLine.isNotBlank()) {
                    MysticStageSpeech(recollectionLine, accent)
                }
                if (revisitLine.isNotBlank()) {
                    MysticStageSpeech(revisitLine, accent)
                }
                MysticStageSpeech(guide.arrival, accent)

                conversation.forEach { turn ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.10f)
                        ) {
                            Text(
                                turn.question,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF4EEE5)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                        color = accent.copy(alpha = 0.13f)
                    ) {
                        val bubble = MysticDialogueBubbleModel(
                            fullText = if (turn.reaction.isBlank()) turn.answer else "${turn.answer}\n\n${turn.reaction}",
                            expanded = expandedBubbles[turn.key] == true
                        )
                        Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                            Text(
                                bubble.preview,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 21.sp,
                                color = Color(0xFFEFE6D7)
                            )
                            if (bubble.shouldCollapse) {
                                OutlinedButton(
                                    onClick = { expandedBubbles[turn.key] = !bubble.expanded },
                                    modifier = Modifier.semantics {
                                        contentDescription = if (bubble.expanded) "收起${characterProfile.displayName}的长回复" else "展开${characterProfile.displayName}的长回复"
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(if (bubble.expanded) "收起" else "展开全文", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                if (pendingCustom != null) {
                    Text(
                        "正在推演···",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.copy(alpha = 0.78f)
                    )
                }
            }

            Surface(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = availableControlHeight)
                    .background(Color(0xFF0D0817).copy(alpha = 0.97f))
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 18.dp),
                color = Color.Transparent
            ) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    currentInteraction().task?.takeUnless { it.completed }?.let { task ->
                        Text("未完成任务 · ${task.detail.title}（${task.detail.dateKey}）", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = {
                                cancelPanelReply()
                                val question = "继续解释已保存的「${task.detail.title}」"
                                sessionState = reduce(sessionState, MysticEvent.SendInput(question))
                                pendingCustomDetailContext = task.detail
                                pendingCustom = question
                            }, modifier = Modifier.testTag("mystic-resume-task")) { Text("继续此项") }
                            OutlinedButton(onClick = { updateInteraction { it.copy(task = task.copy(completed = true)) } }) { Text("标记完成") }
                            OutlinedButton(onClick = { updateInteraction { it.copy(task = null) } }) { Text("移除") }
                        }
                    }
                    companionGameId?.let { gameId ->
                        CompanionGameCard(
                            gameId = gameId,
                            characterId = characterProfile.id,
                            profileKey = visitProfile,
                            onClose = { companionGameId = null },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (gameBridge.activeGame(gameSession)) {
                        com.xuanji.app.ui.components.game.GameBoardCard(
                            position = gameSession.positionAt(gameViewPly ?: gameSession.history.size),
                            history = gameSession.history,
                            viewPly = gameViewPly,
                            outcome = gameSession.outcome,
                            lineColor = Color(skin.garment),
                            boardColor = Color(skin.back),
                            difficulty = gameSession.difficulty,
                            thinking = gameThinking,
                            canRedo = gameSession.redo.isNotEmpty(),
                            onSquareTap = { tap ->
                                driveGame {
                                    gameBridge.applySquareMove(it, from = tap.first, to = tap.second)
                                }
                            },
                            onUndo = { runGameText("悔棋") },
                            onRedo = { runGameText("重做这一手") },
                            onHint = { runGameText("给我提示") },
                            onExit = { runGameText("退出棋局") },
                            onRestart = { runGameText("来一盘象棋") },
                            onDifficultyChange = { level ->
                                runGameText(com.xuanji.app.domain.game.SmartBoardEngine.labelOf(level))
                            },
                            onStep = { ply ->
                                gameViewPly = ply.takeIf { it < gameSession.history.size }
                            },
                            gameTheme = mysticGameThemePalette(characterProfile.gameTheme),
                            footer = if (gameReply.isNotBlank()) {
                                {
                                    Text(
                                        text = gameReply,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFEFE6D7),
                                        modifier = Modifier.semantics {
                                            contentDescription = "基于当前局面的棋局解说"
                                        }
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                    val xiangqiSummary = CompanionGameFavoriteFactory.xiangqiSummary(
                        outcome = gameSession.outcome,
                        halfMoves = gameSession.history.size,
                        title = gameSession.title
                    )
                    if (xiangqiSummary != null) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val store = readingFavoriteStore
                                    val favorite = CompanionGameFavoriteFactory.fromResult(
                                        id = "game-summary-${System.currentTimeMillis()}",
                                        characterId = characterProfile.id,
                                        gameId = "xiangqi",
                                        dateKey = readingMetadata.dateKey,
                                        summary = xiangqiSummary,
                                        sourceNote = "来自本机已复核的象棋棋谱；用户主动收藏。这里只记录棋局结果，不保存对话内容。"
                                    )
                                    if (store == null || readingFavoriteProfileKey.isBlank() || favorite == null) {
                                        xiangqiFavoriteNotice = "收藏暂不可用；棋谱存档不受影响。"
                                    } else {
                                        coroutineScope.launch {
                                            val saved = runCatching {
                                                store.upsert(readingFavoriteProfileKey, favorite)
                                            }.getOrNull()
                                            xiangqiFavoriteNotice = if (saved?.state == ReadingFavoriteDataState.Ready) {
                                                "本局摘要已收藏到「我的收藏」。"
                                            } else {
                                                "本局摘要未能保存；棋谱存档不受影响。"
                                            }
                                        }
                                    }
                                },
                                enabled = !gameThinking,
                                modifier = Modifier.testTag("xiangqi-game-favorite")
                            ) { Text("收藏本局摘要") }
                            if (xiangqiFavoriteNotice.isNotBlank()) {
                                Text(xiangqiFavoriteNotice, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    val archiveLive = gameBridge.activeGame(gameSession)
                    if (archiveLive || gameArchive != null || gameRecord.games > 0) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = gameRecord.summaryText(),
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = "棋局战绩" },
                                style = MaterialTheme.typography.labelSmall,
                                color = accent.copy(alpha = 0.72f)
                            )
                            if (gameSession.history.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { runGameText("保存棋局") },
                                    enabled = !gameThinking,
                                    modifier = Modifier.testTag("game-save")
                                ) {
                                    Text("保存棋局", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (!archiveLive && gameArchive != null) {
                                OutlinedButton(
                                    onClick = { runGameText("继续棋局") },
                                    enabled = !gameThinking,
                                    modifier = Modifier.testTag("game-resume")
                                ) {
                                    Text("继续棋局", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    routedAction?.let { action ->
                        MysticCompanionActionCard(
                            action = action,
                            evidenceLines = com.xuanji.app.domain.MysticReferenceResolver.evidenceLines(
                                dailyActionPlan, referenceCategory, currentReferenceId, temporaryConditions, guide.evidence),
                            onDismiss = { routedAction = null },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    temporaryStatus?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                    if (temporaryConditions.active) {
                        OutlinedButton(onClick = {
                            val key = dailyActionPlan?.profileKey ?: return@OutlinedButton
                            coroutineScope.launch {
                                temporaryStatus = runCatching {
                                    val old = AppModule.actionRepository.readFoodPreference(key)
                                    AppModule.actionRepository.saveFoodPreference(key, temporaryConditions.apply(old))
                                    val activity = AppModule.actionRepository.readActivityPreference(key)
                                    AppModule.actionRepository.saveActivityPreference(key, activity.copy(
                                        indoorOnly = temporaryConditions.indoorOnly ?: activity.indoorOnly,
                                        maxDurationMinutes = temporaryConditions.maxMinutes ?: activity.maxDurationMinutes
                                    ))
                                }.fold({ "已按你的选择保存为此档案的偏好。" }, { "偏好保存未完成，请重试；临时条件仍可使用。" })
                            }
                        }) { Text("保存为偏好") }
                    }
                    onRequestCharacterHandoff?.let { handoff ->
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MysticCharacterCatalog.all.filter { it.id != characterId }.forEach { target ->
                                OutlinedButton(onClick = {
                                    val latest = sessionState.recentTurns.lastOrNull()
                                    val detail = currentInteraction().task?.takeUnless { it.completed }?.detail ?: MysticDetailContext(mode = "角色交接", period = "今日", dateKey = fortune.dateKey,
                                        title = latest?.question ?: "当前问题", result = latest?.answer.orEmpty(),
                                        evidence = guide.evidence.joinToString("；"), sourceStatus = "当前会话交接摘要",
                                        algorithmVersion = com.xuanji.app.domain.MysticEvidenceBuilder.ALGORITHM_VERSION)
                                    cancelPanelReply()
                                    handoff(target.id, detail)
                                }) { Text("转交${target.displayName}") }
                            }
                        }
                    }
                    MysticConversationPanel(
                        state = sessionState,
                        onSend = ::submitPanelInput,
                        onQuickPrompt = ::submitPanelInput,
                        onCancel = ::cancelPanelReply,
                        onRetry = ::retryPanelReply,
                        onClarifierSelected = ::submitPanelInput,
                        softMemoryTags = softMemoryTags,
                        onRevokeSoftTag = ::revokeSoftTag,
                        onClearSoftMemory = ::clearSoftMemory,
                        accent = accent,
                        showMessages = false,
                        gamePrompt = "开始${com.xuanji.app.domain.game.CompanionGameCatalog.forCharacter(characterProfile.id).title}"
                    )
                }
            }
        }
        return
    }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (immersive) Transparent else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (immersive) 0.dp else 1.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(if (immersive) 0.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(skin.back)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(19.dp)
                            .background(Color(skin.garment))
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 13.dp)
                            .height(2.dp)
                            .background(Color(skin.trim))
                    )
                    Text(
                        characterProfile.displayName.take(1),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        characterProfile.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${characterProfile.title} · ${characterProfile.cultureLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "语气 · ${guide.styleName} · ${guide.styleIntro}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "形象 · ${skin.label} · ${skin.detail}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.copy(alpha = 0.86f)
                    )
                    Text(
                        "口吻 · ${skin.voiceLabel} · ${skin.voiceIntro}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.copy(alpha = 0.86f)
                    )
                    Text(
                        "现场 · ${presenceOverride ?: MysticGuideGenerator.presenceState(
                            mode,
                            guide.styleKey,
                            conversation.size
                        )}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.copy(alpha = 0.86f)
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skins.forEachIndexed { index, item ->
                    val isSelected = index == skinIndex
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(item.garment))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) accent else Color(item.trim),
                                shape = CircleShape
                            )
                            .clickable { skinIndex = index }
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("daily" to "今日随机", "custom" to "指定服饰").forEach { (source, label) ->
                    Surface(
                        onClick = {
                            if (source == "daily") {
                                skinIndex = skins.indexOf(
                                    MysticGuideGenerator.defaultMysticSkin(mode, fortune)
                                )
                            }
                            skinSource = source
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = if (skinSource == source) {
                            accent.copy(alpha = 0.20f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                        }
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (skinSource == source) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(visible = revisitLine.isNotBlank() || recollectionLine.isNotBlank()) {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = 0.08f)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            if (revisitLine.isNotBlank()) "回访" else "本机记忆",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        if (recollectionLine.isNotBlank()) {
                            Text(
                                recollectionLine,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (revisitLine.isNotBlank()) {
                            Text(
                                revisitLine,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = arrivalVisible) {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = accent.copy(alpha = 0.12f)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("✦", style = MaterialTheme.typography.titleSmall, color = accent)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "${guide.roleName}今天陪你看盘",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                guide.arrival,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            guestCameo?.let { cameo ->
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                cameo.roleName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "路过 · 客串一句",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent.copy(alpha = 0.86f)
                            )
                        }
                        Text(
                            cameo.line,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (guestReply.isBlank()) {
                            val guestChoicesLocked = pendingGuest ||
                                pendingClarify != null ||
                                selectedClarifier != null ||
                                pendingAsideTurnKey != null
                            FlowRow(
                                Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MysticGuideGenerator.guestChoices().forEach { choice ->
                                    Surface(
                                        onClick = { requestGuestReply(choice.key) },
                                        enabled = !guestChoicesLocked,
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (selectedGuestChoice == choice.key) {
                                            accent.copy(alpha = 0.22f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                                        }
                                    ) {
                                        Text(
                                            choice.label,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            lineHeight = 15.sp,
                                            color = if (guestChoicesLocked) {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                            if (pendingGuest) {
                                Text(
                                    "客串正在接话···",
                                    modifier = Modifier.padding(top = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                guestQuestion,
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                guestReply,
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 19.sp,
                                color = accent.copy(alpha = 0.90f)
                            )
                            guestExit?.let { exit ->
                                Text(
                                    exit.roleName,
                                    modifier = Modifier.padding(top = 10.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
                                )
                                Text(
                                    exit.line,
                                    modifier = Modifier.padding(top = 2.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                                )
                            }
                        }
                    }
                }
            }

            val storedCount = storedMemory.entries.size
            val hasStoredMemory = recollectionBroken || storedCount > 0 || storedMemory.dropped > 0
            if (memoryNotes.isNotEmpty() || hasStoredMemory) {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "现场手记",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (memoryNotes.size > 1) {
                                Surface(
                                    onClick = { memoryExpanded = !memoryExpanded },
                                    color = Color.Transparent
                                ) {
                                    Text(
                                        if (memoryExpanded) "收起手记" else "展开手记",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accent
                                    )
                                }
                            }
                        }
                        memoryNotes.forEachIndexed { index, note ->
                            if (index == 0 || memoryExpanded) {
                                Text(
                                    note.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 19.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (hasStoredMemory) {
                            Text(
                                "本机长期记忆",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "这里只存你自己打的字、点过的选项和终局结果；角色说的话不留档。",
                                style = MaterialTheme.typography.labelSmall,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                            )
                            if (recollectionBroken) {
                                Text(
                                    "本机记录读不出来，清除后从头记。",
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 19.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            storedMemory.entries.asReversed().take(3).forEach { entry ->
                                Text(
                                    "${entry.dateKey.ifBlank { "未记日期" }} · " +
                                        recollectionKindLabel(entry.kind) + " · " + entry.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 19.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (storedCount > 0 || storedMemory.dropped > 0) {
                                val summary = buildString {
                                    append("本机共 $storedCount 条，最多留 ${RecollectionCodec.MAX_ENTRIES} 条。")
                                    if (storedMemory.dropped > 0) {
                                        append("更早的 ${storedMemory.dropped} 条已自动清理。")
                                    }
                                }
                                Text(
                                    summary,
                                    style = MaterialTheme.typography.labelSmall,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    memoryClearFailed = false
                                    coroutineScope.launch {
                                        if (runCatching { memoryStore.clear(visitProfile) }.isSuccess) {
                                            storedMemory = ConversationMemory()
                                            recollectionBroken = false
                                            recollectionLine = ""
                                        } else {
                                            memoryClearFailed = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .semantics { contentDescription = "清除本机长期记忆" },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("清除本机长期记忆", style = MaterialTheme.typography.labelMedium, color = accent)
                            }
                            if (memoryClearFailed) {
                                Text(
                                    "清除失败，记忆仍留在本机。",
                                    style = MaterialTheme.typography.labelSmall,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            OutlinedButton(
                                onClick = ::resetCurrentThread,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .semantics { contentDescription = "清除${characterProfile.displayName}当前线程" },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("清除${characterProfile.displayName}当前线程", style = MaterialTheme.typography.labelMedium, color = accent)
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val scholarAccent = MaterialTheme.colorScheme.primary
                val halfAccent = MaterialTheme.colorScheme.tertiary
                MysticPersonaButton(
                    "理性口吻",
                    "心理按摩",
                    mode == "scholar",
                    scholarAccent,
                    Modifier.weight(1f)
                ) { switchPersona("scholar") }
                MysticPersonaButton(
                    "俏皮口吻",
                    "浮夸吐槽",
                    mode == "half",
                    halfAccent,
                    Modifier.weight(1f)
                ) { switchPersona("half") }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MysticGuideGenerator.topicLabels().forEach { (key, label) ->
                    Surface(
                        onClick = { selectTopic(key) },
                        shape = RoundedCornerShape(999.dp),
                        color = if (topic == key) accent.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (topic == key) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val readingShape = RoundedCornerShape(16.dp)
            Surface(
                Modifier.fillMaxWidth().background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.16f), Color.Transparent, accent.copy(alpha = 0.07f)),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    ),
                    readingShape
                ),
                shape = readingShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f)
            ) {
                Crossfade(targetState = guide, label = "mysticReading") { current ->
                    Column(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✦", style = MaterialTheme.typography.titleMedium, color = accent)
                            Text(
                                "${current.roleName} · ${current.headline}",
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            current.body,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(onClick = { evidenceOpen = !evidenceOpen }, color = Color.Transparent) {
                            Text(
                                if (evidenceOpen) "收起盘面依据" else "展开盘面依据",
                                modifier = Modifier.padding(vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        }

                        AnimatedVisibility(visible = evidenceOpen) {
                            Surface(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f)
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    current.evidence.forEach { fact ->
                                        Text(
                                            fact,
                                            style = MaterialTheme.typography.bodySmall,
                                            lineHeight = 19.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (!openingAnswered && opening != null) {
                            Column(
                                Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    opening.prompt,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 19.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    opening.options.forEach { option ->
                                        Surface(
                                            onClick = { selectOpening(option) },
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (pendingOpening == option) {
                                                accent
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                                            }
                                        ) {
                                            Text(
                                                option.label,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                lineHeight = 17.sp,
                                                color = if (pendingOpening == option) {
                                                    Color.White
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (openingAnswered && !rhythmAnswered && rhythm != null) {
                            Column(
                                Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    rhythm.prompt,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 19.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rhythm.options.forEach { option ->
                                        Surface(
                                            onClick = { selectRhythm(option.key) },
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (selectedRhythm == option.key) {
                                                accent
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                                            }
                                        ) {
                                            Text(
                                                option.label,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                lineHeight = 17.sp,
                                                color = if (selectedRhythm == option.key) {
                                                    Color.White
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "继续问",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (
                                conversation.isNotEmpty() ||
                                pendingFollowUp != null ||
                                pendingInteraction != null ||
                                pendingHandoff != null ||
                                pendingCustom != null ||
                                pendingOpening != null ||
                                pendingRhythm != null ||
                                pendingGuest ||
                                pendingClarify != null ||
                                pendingAsideTurnKey != null
                            ) {
                                Surface(
                                    onClick = ::resetCurrentThread,
                                    color = Color.Transparent
                                ) {
                                    Text(
                                        "重开对话",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accent
                                    )
                                }
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            current.followUps.forEach { item ->
                                Surface(
                                    onClick = { selectFollowUp(item.key) },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (conversation.lastOrNull()?.key == item.key) {
                                        accent
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                                    }
                                ) {
                                    Text(
                                        item.question,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (conversation.lastOrNull()?.key == item.key) {
                                            Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }

                        MysticConversationPanel(
                            state = sessionState,
                            onSend = ::submitPanelInput,
                            onQuickPrompt = ::submitPanelInput,
                            onCancel = ::cancelPanelReply,
                            onRetry = ::retryPanelReply,
                            onClarifierSelected = ::submitPanelInput,
                            softMemoryTags = softMemoryTags,
                            onRevokeSoftTag = ::revokeSoftTag,
                            onClearSoftMemory = ::clearSoftMemory,
                            accent = accent,
                            showMessages = false,
                            gamePrompt = "开始${com.xuanji.app.domain.game.CompanionGameCatalog.forCharacter(characterProfile.id).title}"
                        )

                        if (conversation.isNotEmpty()) {
                            Column(
                                Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                conversation.forEach { turn ->
                                    val followUp = current.followUps.firstOrNull { it.key == turn.key }
                                    val userLine = followUp?.question ?: turn.question
                                    val mysticLine = followUp?.answer ?: turn.answer
                                    run {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                                            ) {
                                                Text(
                                                    userLine,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        if (turn.reaction.isNotBlank()) {
                                            Text(
                                                turn.reaction,
                                                style = MaterialTheme.typography.labelSmall,
                                                lineHeight = 18.sp,
                                                color = accent
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = accent.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                mysticLine,
                                                modifier = Modifier.padding(12.dp),
                                                style = MaterialTheme.typography.bodySmall,
                                                lineHeight = 21.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        if (turn.aside == null) {
                                            Surface(
                                                onClick = { requestAside(turn.key) },
                                                enabled = pendingAsideTurnKey == null &&
                                                    openAsideTurnKey == null &&
                                                    pendingAsideActionTurnKey == null &&
                                                    pendingFollowUp == null &&
                                                    pendingInteraction == null &&
                                                    pendingHandoff == null &&
                                                    pendingCustom == null &&
                                                    pendingOpening == null &&
                                                    pendingRhythm == null &&
                                                    !pendingGuest &&
                                                    pendingClarify == null &&
                                                    selectedClarifier == null,
                                                shape = RoundedCornerShape(999.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                                            ) {
                                                Text(
                                                    "请对面搭腔",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    lineHeight = 15.sp,
                                                    color = if (pendingAsideTurnKey != null) {
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }
                                        if (pendingAsideTurnKey == turn.key) {
                                            Text(
                                                "对面正在接话···",
                                                style = MaterialTheme.typography.labelSmall,
                                                lineHeight = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        turn.aside?.let { aside ->
                                            Surface(
                                                Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                                            ) {
                                                Column(
                                                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            aside.roleName,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            "跟着看盘 · 搭腔",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = accent.copy(alpha = 0.86f)
                                                        )
                                                    }
                                                    Text(
                                                        aside.line,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        lineHeight = 19.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        if (
                                            turn.aside != null &&
                                            turn.asideChoice.isEmpty() &&
                                            openAsideTurnKey == turn.key &&
                                            pendingAsideActionTurnKey == null
                                        ) {
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                MysticGuideGenerator.asideChoices().forEach { choice ->
                                                    Surface(
                                                        onClick = { selectAsideChoice(turn.key, choice) },
                                                        shape = RoundedCornerShape(999.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                                                    ) {
                                                        Text(
                                                            choice.label,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            lineHeight = 17.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (turn.asidePrompt.isNotBlank()) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                                                ) {
                                                    Text(
                                                        turn.asidePrompt,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        lineHeight = 19.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                        if (pendingAsideActionTurnKey == turn.key && turn.asideReply.isBlank()) {
                                            Text(
                                                "对面正在回话···",
                                                style = MaterialTheme.typography.labelSmall,
                                                lineHeight = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (turn.asideReply.isNotBlank()) {
                                            Surface(
                                                Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                                            ) {
                                                Column(
                                                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            turn.aside?.roleName ?: "",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            "跟着看盘 · 回应",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = accent.copy(alpha = 0.86f)
                                                        )
                                                    }
                                                    Text(
                                                        turn.asideReply,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        lineHeight = 19.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        turn.asideExit?.let { exit ->
                                            Surface(
                                                Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                                            ) {
                                                Column(
                                                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            exit.roleName,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            "跟着看盘 · 离席",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = accent.copy(alpha = 0.86f)
                                                        )
                                                    }
                                                    Text(
                                                        exit.line,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        lineHeight = 19.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        if (turn.asideWrapup.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = accent.copy(alpha = 0.16f)
                                            ) {
                                                Text(
                                                    turn.asideWrapup,
                                                    modifier = Modifier.padding(12.dp),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    lineHeight = 21.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val customTurn = conversation.lastOrNull()?.takeIf { it.kind == "custom" }
                        if (customTurn != null) {
                            val clarifier = remember(
                                mode,
                                guide.styleKey,
                                customTurn.question,
                                fortune,
                                latestTest
                            ) {
                                MysticGuideGenerator.customClarifier(
                                    mode,
                                    guide.styleKey,
                                    customTurn.question,
                                    fortune,
                                    latestTest
                                )
                            }
                            clarifier?.let { item ->
                                Column(
                                    Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 19.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        item.options.forEach { option ->
                                            Surface(
                                                onClick = { selectClarifier(option) },
                                                enabled = pendingClarify == null &&
                                                    selectedClarifier == null &&
                                                    pendingAsideTurnKey == null,
                                                shape = RoundedCornerShape(999.dp),
                                                color = if (selectedClarifier == option.key) {
                                                    accent
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                                                }
                                            ) {
                                                Text(
                                                    option.label,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    lineHeight = 17.sp,
                                                    color = if (selectedClarifier == option.key) {
                                                        Color.White
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (
                            pendingFollowUp != null ||
                            pendingInteraction != null ||
                            pendingHandoff != null ||
                            pendingCustom != null ||
                            pendingOpening != null ||
                            pendingRhythm != null ||
                            pendingClarify != null
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    MysticGuideGenerator.thinkingBeat(
                                        mode,
                                        current.styleKey,
                                        companion.skinId,
                                        when {
                                            pendingInteraction != null -> "game"
                                            pendingHandoff != null -> "handoff"
                                            pendingOpening != null -> "opening"
                                            pendingRhythm != null -> "rhythm"
                                            pendingClarify != null -> "ask"
                                            else -> "ask"
                                        },
                                        conversation.size
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "···",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "互动小游戏",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                onClick = {
                                    if (
                                        pendingFollowUp != null ||
                                        pendingInteraction != null ||
                                        pendingHandoff != null ||
                                        pendingCustom != null ||
                                        pendingOpening != null ||
                                        pendingRhythm != null ||
                                        pendingGuest ||
                                        pendingClarify != null ||
                                        pendingAsideTurnKey != null
                                    ) {
                                        // Keep the current game visible until its pending reply lands.
                                    } else {
                                        selectedInteraction = null
                                        pendingInteraction = null
                                        interactionRound += 1
                                    }
                                },
                                color = Color.Transparent
                            ) {
                                Text(
                                    "再来一局",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            }
                        }
                        Text(
                            "${interaction.title} · ${interaction.description}",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            interaction.options.forEach { option ->
                                Surface(
                                    onClick = {
                                        if (
                                            pendingFollowUp == null &&
                                            pendingInteraction == null &&
                                            pendingHandoff == null &&
                                            pendingCustom == null &&
                                            pendingOpening == null &&
                                            pendingRhythm == null &&
                                            !pendingGuest &&
                                            pendingClarify == null &&
                                            pendingAsideTurnKey == null
                                        ) {
                                            selectedInteraction = option
                                            pendingInteraction = option
                                        }
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (selectedInteraction == option) {
                                        accent
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                                    }
                                ) {
                                    Text(
                                        option.label,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selectedInteraction == option) {
                                            Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                        Text(
                            current.signature,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }
            }
        }
    }
}
