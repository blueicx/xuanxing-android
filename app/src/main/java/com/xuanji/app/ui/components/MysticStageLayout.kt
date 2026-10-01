package com.xuanji.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.MysticCharacterId
import com.xuanji.app.domain.MysticCharacterProfile
import com.xuanji.app.domain.MysticDrawerState
import androidx.compose.runtime.mutableStateMapOf

/**
 * Unified full-screen stage. Every role goes through the same five layers:
 * scene plate, scrim, figure, stage header/gallery and companion drawer.
 */
@Composable
fun MysticStageLayout(
    character: MysticCharacterProfile,
    skinId: String,
    garment: Color,
    trimColor: Color,
    moodLevel: Float,
    onCharacterSelected: (MysticCharacterId) -> Unit,
    onConversation: () -> Unit,
    onStartGame: () -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotion()
    val phase by if (reducedMotion) {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "stageMotion").animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
            label = "stagePhase"
        )
    }
    val characterUi = MysticCharacterUiModel.from(character)
    val scene = characterUi.sceneSpec
    val assetSpec = MysticVisualAssetCatalog.forCharacter(character)
    val gold = Color(scene.accentColorArgb)
    var specialtiesOpen by remember(character.id) { mutableStateOf(false) }
    val drawerStates = remember { mutableStateMapOf<MysticCharacterId, MysticDrawerState>() }
    val drawerState = drawerStates[character.id] ?: MysticDrawerState.Peek

    fun expandDrawer() { drawerStates[character.id] = MysticDrawerState.Expanded }
    fun collapseDrawer() { drawerStates[character.id] = MysticDrawerState.Peek }

    Surface(
        Modifier.fillMaxSize(),
        color = Color(0xFF0D0817),
        contentColor = Color(0xFFF4EEE5)
    ) {
        Box(Modifier.fillMaxSize()) {
            // Complete-scene artwork owns its background; only transparent artwork gets a backdrop.
            if (assetSpec.renderMode == MysticAssetRenderMode.CompleteScene) {
                MysticCompleteSceneAsset(
                    styleId = character.visualStyleId,
                    contentDescription = "${character.displayName}完整文化场景",
                    modifier = Modifier.fillMaxSize(),
                    fallback = {
                        MysticCultureBackdrop(scene, gold, moodLevel, Modifier.fillMaxSize())
                    }
                )
            } else {
                MysticCultureBackdrop(scene, gold, moodLevel, Modifier.fillMaxSize())
            }

            // Layer 2: one consistent scrim keeps text and controls readable.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x330D0817),
                            Color.Transparent,
                            Color(0xF20D0817)
                        )
                    )
                )
            )

            // Layer 3: the local character artwork or deterministic Canvas fallback.
            if (assetSpec.renderMode == MysticAssetRenderMode.ForegroundOnScene) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(.72f)
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MysticFigureCanvas(
                        mode = "scholar",
                        skinId = skinId,
                        styleId = character.visualStyleId,
                        garment = garment,
                        trimColor = trimColor,
                        moodLevel = moodLevel,
                        phase = phase,
                        reducedMotion = reducedMotion,
                        modifier = Modifier.fillMaxSize(.94f)
                    )
                }
            }

            // Layer 4 + 5: one information header and one bottom companion drawer.
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().fillMaxHeight(if (drawerState == MysticDrawerState.Peek) .68f else .50f))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xE60D0817), Color(0xFF0D0817))
                            )
                        )
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        character.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = gold,
                        modifier = Modifier.semantics {
                            contentDescription = "当前角色：${character.displayName}，${character.title}"
                        }.testTag("stage-current-character-name")
                    )
                    Text(
                        "${character.title} · ${scene.title}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE7D9EE),
                        modifier = Modifier.testTag("stage-current-character-title")
                    )
                    Text(
                        "当前专长 · ${characterUi.primarySpecialty}",
                        style = MaterialTheme.typography.labelSmall,
                        color = gold.copy(alpha = .88f),
                        modifier = Modifier.semantics {
                            contentDescription = "当前专长：${characterUi.primarySpecialty}"
                        }
                    )
                    MysticCharacterActionBar(
                        character = character,
                        onConversation = { expandDrawer(); onConversation() },
                        onSpecialties = { specialtiesOpen = !specialtiesOpen; expandDrawer() },
                        onGame = { expandDrawer(); onStartGame() }
                    )
                    if (drawerState == MysticDrawerState.Peek) {
                        Surface(
                            onClick = { expandDrawer() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stage-drawer-peek")
                                .semantics { contentDescription = "对话抽屉，已收起；向上展开" },
                            color = Color(0x661A1029),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("对话抽屉 · 已收起", style = MaterialTheme.typography.labelMedium, color = gold)
                                Text("点击展开消息、今日行动、依据和游戏", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD2C4DE))
                            }
                        }
                    }
                    if (specialtiesOpen) {
                        Surface(
                            Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "${character.displayName}的专长列表" },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xAA211433),
                            border = BorderStroke(1.dp, gold.copy(alpha = .24f))
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(
                                    "${character.displayName}的专长入口",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = gold
                                )
                                character.specialties.forEach { specialty ->
                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(
                                            specialty.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFFF4EEE5)
                                        )
                                        Text(
                                            "${specialty.sourceLabel} · ${specialty.ritual}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFD2C4DE)
                                        )
                                    }
                                }
                                Text(
                                    "角色负责策展与解释，盘面仍以对应体系的计算结果为准。",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBB6D7)
                                )
                            }
                        }
                    }
                    if (drawerState == MysticDrawerState.Expanded) {
                        Surface(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("stage-drawer-expanded")
                                .semantics { contentDescription = "对话抽屉，已展开；向下收起" },
                            color = Color(0xDD0D0817),
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                            border = BorderStroke(1.dp, gold.copy(alpha = .24f))
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                Surface(
                                    onClick = { collapseDrawer() },
                                    color = Color.Transparent,
                                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "收起对话抽屉" }
                                ) {
                                    Text("收起对话", Modifier.padding(horizontal = 14.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium, color = gold)
                                }
                                MaterialTheme(colorScheme = darkColorScheme(primary = gold, tertiary = Color(0xFFE3A579))) {
                                    Box(Modifier.fillMaxSize()) { content() }
                                }
                            }
                        }
                    }
                }
            }

            MysticCharacterGallery(
                selectedId = character.id,
                onSelected = onCharacterSelected,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp)
            )
            Surface(
                onClick = onClose,
                shape = CircleShape,
                color = Color(0xFF241731).copy(alpha = .86f),
                border = BorderStroke(1.dp, gold.copy(alpha = .38f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 12.dp, end = 14.dp)
                    .size(48.dp)
                    .semantics { contentDescription = "关闭玄师台" }
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "关闭玄师台",
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
