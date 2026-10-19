package com.xuanji.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.isActive
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.MysticGuideGenerator
import com.xuanji.app.domain.MysticCharacterCatalog
import com.xuanji.app.domain.MysticCharacterId
import com.xuanji.app.domain.MysticCharacterProfile
import com.xuanji.app.domain.MysticDetailContext
import com.xuanji.app.domain.game.CompanionGameCatalog
import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import kotlin.math.sin


/** 外层 Scaffold 读取此状态，沉浸舞台打开时隐藏底部导航栏。 */
val LocalImmersiveStageOpen = compositionLocalOf { mutableStateOf(false) }
val LocalMysticExternalOpenRequest = compositionLocalOf { mutableStateOf<Long?>(null) }
val LocalMysticResumeEligible = compositionLocalOf { false }

/** 全局微光浮球显示状态；默认开启，完整人物仅在点击浮球后出现。 */
val LocalMysticGuideVisible = compositionLocalOf { mutableStateOf(true) }

/** 面部微动作输入：眨眼、视线与嘴角轻变化都从同一位相推导，同输入永远同输出。 */
private data class MysticFaceMotion(
    val blink: Float,
    val gazeX: Float,
    val microSmile: Float
)

@Composable
private fun rememberMysticFaceMotion(half: Boolean, reducedMotion: Boolean): MysticFaceMotion {
    if (reducedMotion) return MysticFaceMotion(blink = 0f, gazeX = 0f, microSmile = 0f)
    val phase by rememberInfiniteTransition(label = "mysticFace").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (half) 4300 else 6400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mysticPhase"
    )
    val p = ((phase % 1f) + 1f) % 1f
    val tau = (2 * Math.PI).toFloat()
    val blink = if (half) {
        maxOf(
            blinkPulse(p, 0.27f, 0.020f),
            blinkPulse(p, 0.71f, 0.016f),
            blinkPulse(p, 0.93f, 0.012f)
        )
    } else {
        maxOf(blinkPulse(p, 0.34f, 0.018f), blinkPulse(p, 0.82f, 0.015f))
    }.coerceIn(0f, 1f)
    val gazeX = if (half) {
        (0.46f * sin(p * tau * 2f + 0.7f) + 0.16f * sin(p * tau * 5f + 2.1f)).coerceIn(-1f, 1f)
    } else {
        (0.24f * sin(p * tau * 2f + 1.9f) + 0.08f * sin(p * tau * 5f + 0.4f)).coerceIn(-1f, 1f)
    }
    val microSmile = 0.06f * sin(p * tau * 3f + if (half) 2.4f else 0.8f)
    return MysticFaceMotion(blink, gazeX, microSmile)
}

private fun blinkPulse(phase: Float, center: Float, halfWidth: Float): Float {
    val distance = kotlin.math.abs(phase - center)
    val wrapped = minOf(distance, 1f - distance)
    return if (wrapped < halfWidth) 1f - wrapped / halfWidth else 0f
}

/** 当日分数折成表情倾向：+1 明快，-1 低落；中间平滑过渡，不改变算法结论。 */
private fun mysticMoodLevel(score: Int): Float = ((score - 55f) / 40f).coerceIn(-1f, 1f)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
 }

@Composable
fun MysticFloatingGuide(
    bazi: BaziFull?,
    fortune: CompositeDailyFortune?,
    modifier: Modifier = Modifier,
    readingMode: String = "综合",
    readingPeriod: String? = null,
    readingDateKey: String? = null,
    content: @Composable (ScrollState) -> Unit
) {
    val guideAvailable = bazi != null && fortune != null
    val favoriteStore = AppModule.readingFavoriteStore
    val readingProfileKey = remember(bazi) { bazi?.let { "bazi|${it.chart.display}" } }
    val activeProfile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    val checkpointProfileKey = activeProfile?.let(AppModule.actionRepository::profileKey) ?: readingProfileKey.orEmpty()
    val companionKey = remember(bazi, fortune, checkpointProfileKey) {
        if (bazi == null || fortune == null) "unavailable" else "$checkpointProfileKey|${fortune.dateKey}"
    }
    val suggestedCharacter = if (guideAvailable) {
        remember(companionKey) {
            MysticCharacterCatalog.recommend(
                topicKey = "composite",
                fortune = fortune!!
            )
        }
    } else {
        MysticCharacterCatalog.byId(MysticCharacterId.ShenYanzhou)
    }
    var stageCharacterKey by rememberSaveable(companionKey) { mutableStateOf(suggestedCharacter.id.key) }
    val stageCharacter = remember(stageCharacterKey) {
        MysticCharacterCatalog.all.firstOrNull { it.id.key == stageCharacterKey } ?: suggestedCharacter
    }
    var stageMode by rememberSaveable(companionKey) { mutableStateOf(suggestedCharacter.dialogueMode) }
    var stageSkinId by rememberSaveable(companionKey) { mutableStateOf(suggestedCharacter.legacySkinId) }
    var stageActionRequest by rememberSaveable(companionKey) { mutableStateOf<String?>(null) }
    var stageDetailContext by remember(companionKey) { mutableStateOf<MysticDetailContext?>(null) }
    var stageDetailReturn by remember(companionKey) { mutableStateOf<(() -> Unit)?>(null) }
    val skin = if (guideAvailable) {
        MysticGuideGenerator.mysticSkinVoice(stageMode, stageSkinId)
            ?: MysticGuideGenerator.defaultMysticSkin(stageMode, fortune!!)
    } else null
    var detailOpen by rememberSaveable(companionKey) { mutableStateOf(false) }
    var savedDrawers by rememberSaveable(companionKey) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var checkpointLoaded by rememberSaveable(companionKey) { mutableStateOf(false) }
    val checkpointContext = androidx.compose.ui.platform.LocalContext.current
    val checkpointStore = remember(checkpointContext) {
        com.xuanji.app.data.local.MysticStageCheckpointStore(com.xuanji.app.data.local.DataStorePreferenceBridge(checkpointContext))
    }
    LaunchedEffect(checkpointProfileKey, fortune?.dateKey, guideAvailable) {
        if (checkpointLoaded || !guideAvailable || checkpointProfileKey.isBlank()) return@LaunchedEffect
        val saved = runCatching { checkpointStore.load(checkpointProfileKey, fortune!!.dateKey) }.getOrNull()
        if (!kotlinx.coroutines.currentCoroutineContext().isActive) return@LaunchedEffect
        if (saved != null) {
            val profile = MysticCharacterCatalog.byId(saved.characterId)
            stageCharacterKey = profile.id.key
            stageMode = profile.dialogueMode
            stageSkinId = profile.legacySkinId
            detailOpen = saved.stageOpen
            savedDrawers = saved.drawers.entries.associate { it.key.key to it.value.name }
            stageActionRequest = null // Never replay an unfinished game/start request.
        }
        checkpointLoaded = true
    }
    LaunchedEffect(checkpointProfileKey, fortune?.dateKey, stageCharacterKey, detailOpen, savedDrawers, checkpointLoaded) {
        if (!checkpointLoaded || !guideAvailable || checkpointProfileKey.isBlank()) return@LaunchedEffect
        val drawers = savedDrawers.mapNotNull { (key, value) ->
            val id = MysticCharacterId.entries.firstOrNull { it.key == key }
            val state = com.xuanji.app.domain.MysticDrawerState.entries.firstOrNull { it.name == value }
            if (id != null && state != null) id to state else null
        }.toMap()
        runCatching { checkpointStore.save(checkpointProfileKey, com.xuanji.app.domain.MysticStageCheckpoint(
            fortune!!.dateKey, stageCharacter.id, detailOpen, drawers)) }
    }
    val guideVisible = LocalMysticGuideVisible.current
    val pageScroll = rememberScrollState()
    val stageOpenState = LocalImmersiveStageOpen.current
    val externalOpenRequest = LocalMysticExternalOpenRequest.current
    val resumeEligible = LocalMysticResumeEligible.current
    LaunchedEffect(externalOpenRequest.value, guideAvailable, checkpointLoaded, resumeEligible) {
        if (resumeEligible && externalOpenRequest.value != null && guideAvailable && checkpointLoaded) {
            detailOpen = true
            externalOpenRequest.value = null
        }
    }
    LaunchedEffect(detailOpen) { stageOpenState.value = detailOpen }
    val companionUiState = CompanionUiState(
        presence = if (guideVisible.value) CompanionPresence.OrbVisible else CompanionPresence.FullyHidden,
        stageOpen = detailOpen
    )

    fun selectCharacter(id: MysticCharacterId) {
        val profile = MysticCharacterCatalog.byId(id)
        stageCharacterKey = profile.id.key
        stageMode = profile.dialogueMode
        stageSkinId = profile.legacySkinId
        stageActionRequest = null
        stageDetailContext = null
    }

    Box(modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalMysticReadingMetadata provides MysticReadingMetadata(
                mode = readingMode,
                period = readingPeriod ?: when (fortune?.period) {
                    "week" -> "本周"
                    "month" -> "本月"
                    "year" -> "本年"
                    else -> "今日"
                },
                dateKey = readingDateKey ?: fortune?.dateKey.orEmpty()
            ),
            LocalMysticDetailQuestion provides { selectedDetail, returnToDetail ->
                stageDetailContext = selectedDetail
                stageDetailReturn = returnToDetail
                stageActionRequest = MYSTIC_DETAIL_QUESTION_ACTION
                detailOpen = true
            },
            LocalReadingFavoriteStore provides favoriteStore,
            LocalReadingFavoriteProfileKey provides readingProfileKey
        ) {
            content(pageScroll)
        }

        if (skin != null && companionUiState.presence == CompanionPresence.OrbVisible && !companionUiState.stageOpen) {
            MysticOrb(
                roleName = stageCharacter.displayName,
                characterInitial = stageCharacter.displayName.take(1),
                half = stageMode == "half",
                color = Color(skin.garment),
                trimColor = Color(skin.trim),
                onClick = { detailOpen = true },
                scrollValue = pageScroll.value,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .imePadding()
            )
        }

        if (detailOpen && bazi != null && fortune != null && skin != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .zIndex(100f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* consume clicks so they don't fall through */ }
            ) {
                MysticImmersiveStage(
                    character = stageCharacter,
                    skinId = skin.id,
                    garment = Color(skin.garment),
                    trimColor = Color(skin.trim),
                    moodLevel = mysticMoodLevel(fortune!!.overallScore),
                    onCharacterSelected = ::selectCharacter,
                    onConversation = { stageActionRequest = "我只是想聊聊" },
                    onStartGame = {
                        val game = CompanionGameCatalog.forCharacter(stageCharacter.id)
                        stageActionRequest = if (game.id == "xiangqi") {
                            "来一盘象棋"
                        } else {
                            "__companion_game__:${game.id}"
                        }
                    },
                    onClose = {
                        detailOpen = false
                        savedDrawers = savedDrawers + (stageCharacter.id.key to com.xuanji.app.domain.MysticDrawerState.Peek.name)
                        val returnToDetail = stageDetailReturn
                        stageDetailReturn = null
                        stageDetailContext = null
                        returnToDetail?.invoke()
                    },
                    drawerState = savedDrawers[stageCharacter.id.key]?.let { com.xuanji.app.domain.MysticDrawerState.valueOf(it) }
                        ?: com.xuanji.app.domain.MysticDrawerState.Peek,
                    onDrawerStateChanged = { value -> savedDrawers = savedDrawers + (stageCharacter.id.key to value.name) },
                ) {
                    MysticGuideCard(
                        bazi,
                        fortune,
                        immersive = true,
                        characterId = stageCharacter.id,
                        onRequestCharacterHandoff = { target, detail ->
                            selectCharacter(target)
                            // A handoff is a conversation action, not merely gallery selection.
                            savedDrawers = savedDrawers + (target.key to com.xuanji.app.domain.MysticDrawerState.Expanded.name)
                            stageDetailContext = detail
                            stageActionRequest = MYSTIC_DETAIL_QUESTION_ACTION
                        },
                        onStageModeChange = { stageMode = it },
                        onStageSkinChange = { stageSkinId = it },
                        stageActionRequest = stageActionRequest,
                        stageDetailContext = stageDetailContext,
                        onStageActionConsumed = {
                            stageActionRequest = null
                            stageDetailContext = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MysticImmersiveStage(
    character: MysticCharacterProfile,
    skinId: String,
    garment: Color,
    trimColor: Color,
    moodLevel: Float,
    onCharacterSelected: (MysticCharacterId) -> Unit,
    onConversation: () -> Unit,
    onStartGame: () -> Unit,
    onClose: () -> Unit,
    drawerState: com.xuanji.app.domain.MysticDrawerState,
    onDrawerStateChanged: (com.xuanji.app.domain.MysticDrawerState) -> Unit,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    DisposableEffect(view) {
        try {
            val win = view.context.findActivity()?.window
            if (win != null) {
                WindowCompat.setDecorFitsSystemWindows(win, false)
                val controller = WindowCompat.getInsetsController(win, win.decorView)
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            }
        } catch (_: Exception) { }
        onDispose {
            try {
                val win = view.context.findActivity()?.window
                if (win != null) {
                    val controller = WindowCompat.getInsetsController(win, win.decorView)
                    controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                    WindowCompat.setDecorFitsSystemWindows(win, false)
                }
            } catch (_: Exception) { }
        }
    }
    MysticStageLayout(
        character = character,
        skinId = skinId,
        garment = garment,
        trimColor = trimColor,
        moodLevel = moodLevel,
        onCharacterSelected = onCharacterSelected,
        onConversation = onConversation,
        onStartGame = onStartGame,
        onClose = onClose,
        drawerStateOverride = drawerState,
        onDrawerStateChanged = onDrawerStateChanged,
        content = content
    )
}

