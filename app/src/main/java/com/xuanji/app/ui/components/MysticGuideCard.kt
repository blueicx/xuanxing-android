package com.xuanji.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.Gson
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.local.dataStore
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.MysticClarifierOption
import com.xuanji.app.domain.MysticInteraction
import com.xuanji.app.domain.MysticInteractionOption
import com.xuanji.app.domain.MysticGuideGenerator
import com.xuanji.app.domain.DefaultMysticDialogueEngine
import com.xuanji.app.domain.DialogueContext
import com.xuanji.app.domain.DialogueProvider
import com.xuanji.app.domain.DialogueRequest
import com.xuanji.app.domain.OfflineDialogueProvider
import com.xuanji.app.domain.ProviderResult
import com.xuanji.app.domain.MysticEvent
import com.xuanji.app.domain.MysticSessionState
import com.xuanji.app.domain.reduce
import com.xuanji.app.domain.MysticOpeningCheckin
import com.xuanji.app.domain.MysticOpeningOption
import com.xuanji.app.domain.MysticGuestCameo
import com.xuanji.app.domain.MysticGuestChoice
import com.xuanji.app.domain.MysticGuestExit
import com.xuanji.app.domain.MysticRhythmCheckin
import com.xuanji.app.domain.MysticSkin
import com.xuanji.app.domain.MysticVisitMemory
import com.xuanji.app.domain.MysticMemoryNote as DomainMysticMemoryNote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.MessageDigest

private data class MysticTurn(
    val key: String,
    val question: String,
    val answer: String,
    val reaction: String = "",
    val kind: String = "ask",
    val aside: MysticGuestCameo? = null,
    val asidePrompt: String = "",
    val asideChoice: String = "",
    val asideReply: String = "",
    val asideExit: MysticGuestExit? = null,
    val asideWrapup: String = ""
)

private data class MysticMemoryNote(
    val id: String,
    val text: String
)

private data class StoredMysticVisit(
    val dateKey: String = "",
    val topicKey: String = "",
    val action: String = ""
)

private class MysticVisitStore(private val context: android.content.Context) {
    private val gson = Gson()

    suspend fun read(profileKey: String): MysticVisitMemory {
        val preferences = context.dataStore.data.first()
        val stored = preferences[stringPreferencesKey("mystic_visit_${fingerprint(profileKey)}")]
            ?.let { json ->
                runCatching { gson.fromJson(json, StoredMysticVisit::class.java) }.getOrNull()
            }
        return MysticVisitMemory(
            lastDateKey = stored?.dateKey.orEmpty(),
            lastTopicKey = stored?.topicKey.orEmpty(),
            lastAction = stored?.action.orEmpty()
        )
    }

    suspend fun save(profileKey: String, memory: MysticVisitMemory) {
        val key = stringPreferencesKey("mystic_visit_${fingerprint(profileKey)}")
        context.dataStore.edit { preferences ->
            preferences[key] = gson.toJson(
                StoredMysticVisit(
                    dateKey = canonicalDate(memory.lastDateKey),
                    topicKey = memory.lastTopicKey,
                    action = memory.lastAction
                )
            )
        }
    }

    private fun fingerprint(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun canonicalDate(value: String): String {
        val parts = value.split("-")
        if (parts.size != 3) return value
        val year = parts[0].toIntOrNull() ?: return value
        val month = parts[1].toIntOrNull() ?: return value
        val day = parts[2].toIntOrNull() ?: return value
        return "$year-$month-$day"
    }
}

private class MysticCompanionState(initialMode: String, initialTopic: String) {
    var mode by mutableStateOf(initialMode)
    var topic by mutableStateOf(initialTopic)
    var interactionCarryoverOption by mutableStateOf<String?>(null)
    var pendingHandoff by mutableStateOf<String?>(null)
    var pendingHandoffEcho by mutableStateOf<String?>(null)
    var rhythmAnswered by mutableStateOf(false)
    var completedRhythmKey by mutableStateOf<String?>(null)
    var guestCameo by mutableStateOf<MysticGuestCameo?>(null)
    var guestExit by mutableStateOf<MysticGuestExit?>(null)
    var selectedGuestChoice by mutableStateOf("")
    var guestReply by mutableStateOf("")
    var guestQuestion by mutableStateOf("")
    var pendingGuest by mutableStateOf(false)
    var guestChoiceCarryoverKey by mutableStateOf<String?>(null)
    var pendingGuestChoiceEcho by mutableStateOf<String?>(null)
    var memoryNotes by mutableStateOf(emptyList<MysticMemoryNote>())
    var memorySequence by mutableStateOf(0)
    var memoryExpanded by mutableStateOf(false)
    var skinId by mutableStateOf("")
}

private val mysticCompanionStates = mutableMapOf<String, MysticCompanionState>()

private fun mysticCompanionState(
    key: String,
    fortune: CompositeDailyFortune
): MysticCompanionState = mysticCompanionStates.getOrPut(key) {
    val topic = "composite"
    MysticCompanionState(MysticGuideGenerator.suggestedMode(topic, fortune), topic).also {
        it.skinId = MysticGuideGenerator.defaultMysticSkin(it.mode, fortune).id
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MysticGuideCard(
    bazi: BaziFull,
    fortune: CompositeDailyFortune,
    immersive: Boolean = false,
    onStageModeChange: (String) -> Unit = {},
    onStageSkinChange: (String) -> Unit = {},
    stageCostumeRequest: Pair<String, String>? = null,
    onStageCostumeConsumed: () -> Unit = {}
) {
    val records by AppModule.testRecordRepository.records.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val visitStore = remember(context) { MysticVisitStore(context) }
    val coroutineScope = rememberCoroutineScope()
    val visitProfile = remember(bazi) { "bazi|${bazi.chart.display}" }
    val companionKey = "${bazi.hashCode()}|${fortune.hashCode()}"
    val companion = remember(companionKey) { mysticCompanionState(companionKey, fortune) }
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
    var visitReady by remember(visitProfile) { mutableStateOf(false) }
    var persistedVisitAction by remember(visitProfile) { mutableStateOf("") }
    LaunchedEffect(companionKey) {
        guestExit = null
        memoryNotes = emptyList()
        memorySequence = 0
        memoryExpanded = false
    }
    val latestTest = records.maxByOrNull { it.date }
    val guide = remember(mode, topic, bazi, fortune, latestTest, companion.skinId) {
        MysticGuideGenerator.generate(mode, topic, bazi, fortune, latestTest, skinId = companion.skinId)
    }
    val dialogueEngine = remember { DefaultMysticDialogueEngine() }
    val dialogueProvider: DialogueProvider = remember { OfflineDialogueProvider(dialogueEngine) }
    var sessionState by remember { mutableStateOf(MysticSessionState()) }
    var gameSession by remember { mutableStateOf(com.xuanji.app.domain.game.GameSessionState()) }
    var gameReply by remember { mutableStateOf("") }
    var gameInputEcho by remember { mutableStateOf<String?>(null) }
    val gameBridge = remember { com.xuanji.app.domain.game.GameDialogueBridge() }
    var pendingCustom by remember(guide) { mutableStateOf<String?>(null) }
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
    var evidenceOpen by remember(guide) { mutableStateOf(false) }
    val conversation = remember(guide) { mutableStateListOf<MysticTurn>() }
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
    var interactionCount by remember(guide) { mutableStateOf(0) }
    var customCount by remember(guide) { mutableStateOf(0) }
    val askCounts = remember(guide) { mutableStateMapOf<String, Int>() }
    var interactionRound by remember(guide) { mutableStateOf(0) }
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

    fun rememberMemory(kind: String, detail: String) {
        val text = MysticGuideGenerator.memoryNote(mode, guide.styleKey, kind, detail)
        if (text.isBlank()) return
        val note = MysticMemoryNote("memory-$kind-$memorySequence", text)
        memoryNotes = (listOf(note) + memoryNotes).take(3)
        sessionState = reduce(sessionState, MysticEvent.Remember(DomainMysticMemoryNote(note.id, note.text)))
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
        memoryNotes = (listOf(note) + memoryNotes).take(3)
        sessionState = reduce(sessionState, MysticEvent.Remember(DomainMysticMemoryNote(note.id, note.text)))
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
        sessionState = reduce(sessionState, MysticEvent.SendInput(cleanQuestion))
        pendingCustom = cleanQuestion
        customQuestion = ""
    }

    fun submitPanelInput(text: String) {
        // Game path first: board-game intents bypass the generic pendingCustom reply so
        // character game commentary never mixes with fortune template wording.
        val cleanText = text.trim().take(200)
        val gameIntent = com.xuanji.app.domain.MysticIntentClassifier.classify(cleanText) ==
            com.xuanji.app.domain.MysticIntent.Game ||
            gameBridge.activeGame(gameSession)
        if (gameIntent) {
            val result = gameBridge.handle(gameSession, cleanText)
            gameSession = result.state
            if (result.reply.isNotBlank()) gameReply = result.reply
            gameInputEcho = if (result.event != null || result.grounded) cleanText else null
            return
        }
        customQuestion = text
        submitCustom()
    }

    fun cancelPanelReply() {
        val pending = sessionState.requestState as? com.xuanji.app.domain.MysticRequestState.Pending ?: return
        pendingCustom = null
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
    }

    LaunchedEffect(pendingCustom, guide) {
        val question = pendingCustom ?: return@LaunchedEffect
        val requestToken = sessionState.sessionToken
        val requestTurnId = (sessionState.requestState as? com.xuanji.app.domain.MysticRequestState.Pending)?.turnId
            ?: return@LaunchedEffect
        kotlinx.coroutines.delay(430)
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
            memoryNotes = sessionState.memoryNotes,
            skinId = companion.skinId,
            question = question
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
    }
    val accent by animateColorAsState(
        targetValue = if (mode == "half") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = tween(260),
        label = "mysticAccent"
    )
    val skins = remember(mode) { MysticGuideGenerator.mysticSkins(mode) }
    var skinIndex by remember(mode, fortune) {
        mutableStateOf(skins.indexOf(MysticGuideGenerator.defaultMysticSkin(mode, fortune)))
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
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 86.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                        Text(
                            if (turn.reaction.isBlank()) turn.answer else "${turn.answer}\n\n${turn.reaction}",
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 21.sp,
                            color = Color(0xFFEFE6D7)
                        )
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
                    .background(Color(0xFF0D0817).copy(alpha = 0.97f))
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 18.dp),
                color = Color.Transparent
            ) {
                Column {
                    if (gameBridge.activeGame(gameSession)) {
                        com.xuanji.app.ui.components.game.GameBoardCard(
                            position = gameSession.position,
                            onSquareTap = { tap ->
                                val from = tap.first
                                val to = tap.second
                                val move = com.xuanji.app.domain.game.BoardMove(
                                    from = from,
                                    to = to,
                                    notation = "",
                                    player = gameSession.position.sideToMove
                                )
                                val result = gameBridge.handle(gameSession, com.xuanji.app.domain.game.XiangqiNotation.format(move, gameSession.position))
                                gameSession = result.state
                                if (result.reply.isNotBlank()) gameReply = result.reply
                            },
                            onUndo = {
                                val result = gameBridge.handle(gameSession, "悔棋")
                                gameSession = result.state
                                if (result.reply.isNotBlank()) gameReply = result.reply
                            },
                            onHint = {
                                val result = gameBridge.handle(gameSession, "给我提示")
                                if (result.reply.isNotBlank()) gameReply = result.reply
                            },
                            onExit = {
                                val result = gameBridge.handle(gameSession, "退出棋局")
                                gameSession = result.state
                                gameReply = result.reply
                            },
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
                    MysticConversationPanel(
                        state = sessionState,
                        onSend = ::submitPanelInput,
                        onQuickPrompt = ::submitPanelInput,
                        onCancel = ::cancelPanelReply,
                        onRetry = ::retryPanelReply,
                        accent = accent,
                        showMessages = false
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
                        if (mode == "half") "半" else "玄",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (mode == "half") "半仙" else "玄学家",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${guide.styleName} · ${guide.styleIntro}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "服饰 · ${skin.label} · ${skin.detail}",
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

            AnimatedVisibility(visible = revisitLine.isNotBlank()) {
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
                            "回访",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Text(
                            revisitLine,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

            if (memoryNotes.isNotEmpty()) {
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
                    "玄学家",
                    "心理按摩",
                    mode == "scholar",
                    scholarAccent,
                    Modifier.weight(1f)
                ) { switchPersona("scholar") }
                MysticPersonaButton(
                    "半仙",
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
                                    onClick = {
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
                                    },
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
                            accent = accent,
                            showMessages = false
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

@Composable
private fun MysticStageSpeech(
    text: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        color = accent.copy(alpha = 0.13f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 21.sp,
            color = Color(0xFFEFE6D7)
        )
    }
}

@Composable
private fun MysticPersonaButton(
    title: String,
    subtitle: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
