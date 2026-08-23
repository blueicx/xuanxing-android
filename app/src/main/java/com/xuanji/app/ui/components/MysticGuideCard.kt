package com.xuanji.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.MysticInteraction
import com.xuanji.app.domain.MysticInteractionOption
import com.xuanji.app.domain.MysticGuideGenerator

private data class MysticTurn(
    val key: String,
    val question: String,
    val answer: String,
    val reaction: String = "",
    val kind: String = "ask"
)

private class MysticCompanionState(initialMode: String, initialTopic: String) {
    var mode by mutableStateOf(initialMode)
    var topic by mutableStateOf(initialTopic)
    var interactionCarryoverOption by mutableStateOf<String?>(null)
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
    val companionKey = listOf(
        bazi.chart.display,
        bazi.strength.level,
        fortune.dateKey,
        fortune.overallScore,
        fortune.luckyNumber
    ).joinToString("|")
    val companion = remember(companionKey) { mysticCompanionState(companionKey, fortune) }
    var topic by remember(companion) { companion::topic }
    var mode by remember(companion) { companion::mode }
    var interactionCarryoverOption by remember(companion) { companion::interactionCarryoverOption }
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
    var pendingHandoff by remember { mutableStateOf<String?>(null) }
    var pendingHandoffEcho by remember { mutableStateOf<String?>(null) }
    var pendingCustom by remember(guide) { mutableStateOf<String?>(null) }
    var customQuestion by remember(guide) { mutableStateOf("") }
    var interactionCount by remember(guide) { mutableStateOf(0) }
    var customCount by remember(guide) { mutableStateOf(0) }
    val askCounts = remember(guide) { mutableStateMapOf<String, Int>() }
    var interactionRound by remember(guide) { mutableStateOf(0) }
    var selectedInteraction by remember(guide, interactionRound) { mutableStateOf<MysticInteractionOption?>(null) }
    val interaction = remember(mode, topic, fortune, interactionRound) {
        MysticGuideGenerator.interaction(mode, topic, fortune, interactionRound)
    }

    LaunchedEffect(guide) {
        if (pendingHandoff == null) {
            interactionCarryoverOption = null
        }
    }

    LaunchedEffect(guide) {
        kotlinx.coroutines.delay(180)
        arrivalVisible = true
    }

    fun selectFollowUp(key: String) {
        if (
            guide.followUps.none { it.key == key } ||
            pendingFollowUp != null ||
            pendingInteraction != null ||
            pendingHandoff != null ||
            pendingCustom != null
        ) return
        pendingFollowUp = key
    }

    fun submitCustom() {
        if (pendingFollowUp != null || pendingInteraction != null || pendingHandoff != null || pendingCustom != null) return
        val cleanQuestion = customQuestion.trim().take(60)
        if (cleanQuestion.isEmpty()) return
        pendingCustom = cleanQuestion
        customQuestion = ""
    }

    fun switchPersona(targetMode: String) {
        if (
            pendingFollowUp != null ||
            pendingInteraction != null ||
            pendingHandoff != null ||
            pendingCustom != null
        ) return
        interactionCarryoverOption = null
        mode = targetMode
    }

    fun selectTopic(key: String) {
        if (
            key == topic ||
            pendingFollowUp != null ||
            pendingInteraction != null ||
            pendingHandoff != null ||
            pendingCustom != null
        ) return
        val previousTopic = topic
        val carryover = MysticGuideGenerator.interactionCarryover(
            mode,
            guide.styleKey,
            interactionCarryoverOption.orEmpty()
        )
        selectedInteraction = null
        pendingFollowUp = null
        pendingInteraction = null
        topic = key
        pendingHandoff = previousTopic
        pendingHandoffEcho = carryover
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
                    pendingHandoffEcho.orEmpty(),
                    MysticGuideGenerator.handoffReaction(mode, guide.styleKey)
                ),
                kind = "handoff"
            )
        )
        while (conversation.size > 5) conversation.removeAt(0)
        interactionCarryoverOption = null
        pendingHandoff = null
        pendingHandoffEcho = null
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
                    MysticGuideGenerator.interactionCarryover(
                        mode,
                        guide.styleKey,
                        interactionCarryoverOption.orEmpty()
                    ),
                    MysticGuideGenerator.customReaction(mode, guide.styleKey, question)
                ),
                kind = "ask"
            )
        )
        while (conversation.size > 5) conversation.removeAt(0)
        customCount += 1
        interactionCarryoverOption = null
        pendingCustom = null
    }

    LaunchedEffect(pendingInteraction) {
        val option = pendingInteraction ?: return@LaunchedEffect
        kotlinx.coroutines.delay(380)
        conversation.add(
            MysticTurn(
                key = "game-$interactionCount-${option.label}",
                question = option.label,
                answer = option.feedback,
                reaction = MysticGuideGenerator.interactionReaction(mode, guide.styleKey),
                kind = "game"
            )
        )
        while (conversation.size > 5) conversation.removeAt(0)
        interactionCount += 1
        interactionCarryoverOption = option.label
        pendingInteraction = null
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
            MysticGuideGenerator.interactionCarryover(
                mode,
                guide.styleKey,
                interactionCarryoverOption.orEmpty()
            ),
            MysticGuideGenerator.reaction(mode, action, askedCount, guide.styleKey)
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
        pendingFollowUp = null
    }
    val accent by animateColorAsState(
        targetValue = if (mode == "half") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = tween(260),
        label = "mysticAccent"
    )

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
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accent.copy(alpha = 0.18f)
                ) {
                    Text(
                        if (mode == "half") "半" else "玄",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                        "现场 · ${MysticGuideGenerator.presenceState(mode, guide.styleKey, conversation.size)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.copy(alpha = 0.86f)
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

                        if (
                            pendingFollowUp != null ||
                            pendingInteraction != null ||
                            pendingHandoff != null ||
                            pendingCustom != null
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
                                "继续问",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (conversation.isNotEmpty()) {
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
                                        customQuestion = ""
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
                                    pendingCustom == null
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
                                        pendingCustom != null
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
                                            pendingCustom == null
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
