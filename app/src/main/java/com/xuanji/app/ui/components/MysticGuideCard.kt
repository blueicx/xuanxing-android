package com.xuanji.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.MysticClarifierOption
import com.xuanji.app.domain.MysticInteraction
import com.xuanji.app.domain.MysticInteractionOption
import com.xuanji.app.domain.MysticGuideGenerator
import com.xuanji.app.domain.MysticOpeningCheckin
import com.xuanji.app.domain.MysticOpeningOption
import com.xuanji.app.domain.MysticGuestCameo
import com.xuanji.app.domain.MysticGuestChoice
import com.xuanji.app.domain.MysticGuestExit
import com.xuanji.app.domain.MysticRhythmCheckin
import com.xuanji.app.domain.MysticSkin

private data class MysticTurn(
    val key: String,
    val question: String,
    val answer: String,
    val reaction: String = "",
    val kind: String = "ask"
)

private data class MysticMemoryNote(
    val id: String,
    val text: String
)

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
}

private val mysticCompanionStates = mutableMapOf<String, MysticCompanionState>()

private fun mysticCompanionState(
    key: String,
    fortune: CompositeDailyFortune
): MysticCompanionState = mysticCompanionStates.getOrPut(key) {
    val topic = "composite"
    MysticCompanionState(MysticGuideGenerator.suggestedMode(topic, fortune), topic)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MysticGuideCard(
    bazi: BaziFull,
    fortune: CompositeDailyFortune
) {
    val records by AppModule.testRecordRepository.records.collectAsStateWithLifecycle(initialValue = emptyList())
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
    LaunchedEffect(companionKey) {
        guestExit = null
        memoryNotes = emptyList()
        memorySequence = 0
        memoryExpanded = false
    }
    val latestTest = records.maxByOrNull { it.date }
    val guide = remember(mode, topic, bazi, fortune, latestTest) {
        MysticGuideGenerator.generate(mode, topic, bazi, fortune, latestTest)
    }
    var selectedFollowUp by remember(guide) { mutableStateOf("") }
    var evidenceOpen by remember(guide) { mutableStateOf(false) }
    val conversation = remember(guide) { mutableStateListOf<MysticTurn>() }
    var arrivalVisible by remember(guide) { mutableStateOf(false) }
    var pendingFollowUp by remember(guide) { mutableStateOf<String?>(null) }
    var pendingInteraction by remember(guide) { mutableStateOf<MysticInteractionOption?>(null) }
    var pendingCustom by remember(guide) { mutableStateOf<String?>(null) }
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
    val interaction = remember(mode, topic, fortune, interactionRound) {
        MysticGuideGenerator.interaction(mode, topic, fortune, interactionRound)
    }
    val opening: MysticOpeningCheckin? = remember(mode, guide.topicKey, guide.styleKey, fortune) {
        MysticGuideGenerator.openingCheckin(
            mode,
            guide.topicKey,
            guide.styleKey,
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
            pendingClarify != null
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
            pendingClarify != null
        ) return
        val cleanQuestion = customQuestion.trim().take(60)
        if (cleanQuestion.isEmpty()) return
        pendingCustom = cleanQuestion
        customQuestion = ""
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
            pendingClarify != null
        ) return
        selectedClarifier = option.key
        pendingClarify = option
    }

    fun switchPersona(targetMode: String) {
        if (
            pendingFollowUp != null ||
            pendingInteraction != null ||
            pendingHandoff != null ||
            pendingCustom != null ||
            pendingOpening != null ||
            pendingRhythm != null ||
            pendingGuest ||
            pendingClarify != null
        ) return
        interactionCarryoverOption = null
        guestChoiceCarryoverKey = null
        guestExit = null
        pendingGuestChoiceEcho = null
        selectedClarifier = null
        pendingClarify = null
        memoryNotes = emptyList()
        memorySequence = 0
        memoryExpanded = false
        mode = targetMode
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
            pendingClarify != null
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
            pendingClarify != null
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
            pendingClarify != null
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
        kotlinx.coroutines.delay(430)
        conversation.add(
            MysticTurn(
                key = "custom-$customCount-${question.hashCode()}",
                question = question,
                answer = MysticGuideGenerator.customAnswer(
                    mode,
                    guide.topicKey,
                    question,
                    fortune,
                    latestTest
                ),
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
                            MysticGuideGenerator.interactionReaction(mode, guide.styleKey)
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
    val skin = skins[skinIndex.coerceIn(skins.indices)]

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
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
                        "现场 · ${MysticGuideGenerator.presenceState(mode, guide.styleKey, conversation.size)}",
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
                                selectedClarifier != null
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
                                pendingClarify != null
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

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customQuestion,
                                onValueChange = { if (it.length <= 60) customQuestion = it },
                                placeholder = { Text("问一句今天的事") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                            OutlinedButton(
                                onClick = ::submitCustom,
                                enabled = customQuestion.isNotBlank() &&
                                    pendingFollowUp == null &&
                                    pendingInteraction == null &&
                                    pendingHandoff == null &&
                                    pendingCustom == null &&
                                    pendingOpening == null &&
                                    pendingRhythm == null &&
                                    !pendingGuest &&
                                    pendingClarify == null
                            ) {
                                Text("问")
                            }
                        }

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
                                                enabled = pendingClarify == null && selectedClarifier == null,
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
                                    MysticGuideGenerator.thinkingLine(
                                        current.mode,
                                        current.styleKey,
                                        when {
                                            pendingInteraction != null -> "game"
                                            pendingHandoff != null -> "handoff"
                                            pendingOpening != null -> "opening"
                                            pendingRhythm != null -> "rhythm"
                                            pendingClarify != null -> "ask"
                                            else -> "ask"
                                        }
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
                                        pendingClarify != null
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
                                            pendingClarify == null
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
