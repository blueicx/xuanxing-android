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
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.MysticCharacterId
import com.xuanji.app.domain.MysticCharacterProfile

/**
 * Full-screen character stage. The scene plate owns the background for the
 * three reference triptychs; the elder ink figure remains a transparent
 * foreground over the generated ink backdrop.
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
    val gold = Color(0xFFD9C58B)
    val characterUi = MysticCharacterUiModel.from(character)
    var specialtiesOpen by remember(character.id) { mutableStateOf(false) }

    Surface(
        Modifier.fillMaxSize(),
        color = Color(0xFF0D0817),
        contentColor = Color(0xFFF4EEE5)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (characterUi.usesScenePlate) {
                MysticFigureAsset(
                    styleId = character.visualStyleId,
                    contentDescription = "${character.displayName}人物场景",
                    modifier = Modifier.fillMaxSize().alpha(.96f),
                    contentScale = ContentScale.Crop
                ) {
                    MysticCultureBackdrop(skinId, gold, moodLevel)
                    MysticFigureCanvas(
                        mode = "scholar",
                        skinId = skinId,
                        styleId = character.visualStyleId,
                        garment = garment,
                        trimColor = trimColor,
                        moodLevel = moodLevel,
                        phase = phase,
                        reducedMotion = reducedMotion,
                        modifier = Modifier.fillMaxSize(.82f)
                    )
                }
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
            } else {
                MysticCultureBackdrop(skinId, gold, moodLevel)
                Box(
                    Modifier.fillMaxWidth().fillMaxHeight(.60f).padding(top = 40.dp),
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
                        modifier = Modifier.fillMaxSize(.82f)
                    )
                }
            }

            Column(Modifier.fillMaxSize()) {
                if (characterUi.usesScenePlate) {
                    Box(Modifier.fillMaxWidth().fillMaxHeight(.57f))
                }
                Column(
                    Modifier.fillMaxWidth().weight(1f)
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
                        }
                    )
                    Text(
                        "${character.title} · ${character.cultureLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE7D9EE)
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
                        onConversation = onConversation,
                        onSpecialties = { specialtiesOpen = !specialtiesOpen },
                        onGame = onStartGame
                    )
                    if (specialtiesOpen) {
                        Surface(
                            Modifier.fillMaxWidth(),
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
                    MaterialTheme(colorScheme = darkColorScheme(primary = gold, tertiary = Color(0xFFE3A579))) {
                        content()
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
                    .semantics { contentDescription = "关闭玄师台" }
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "关闭玄师台",
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
