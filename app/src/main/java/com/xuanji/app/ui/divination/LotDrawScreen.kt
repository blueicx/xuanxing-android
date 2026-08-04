package com.xuanji.app.ui.divination

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuanji.app.domain.divination.LotDraw
import com.xuanji.app.domain.divination.LotDraw.LotResult
import com.xuanji.app.domain.divination.LotDraw.LotSystem
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate
import kotlinx.coroutines.delay

/**
 * 抽签占卜页：按 systemKey 显示对应系统的动态交互。
 * 每个系统有专属的「抽取/重抽」动作，点击后播放动画并给出结果。
 * 删除了「选择占卜系统」下拉（各系统在占卜图鉴里已是独立条目）。
 */
@Composable
fun LotDrawScreen(systemKey: String?) {
    val system = LotDraw.ALL.firstOrNull { it.key == systemKey } ?: LotSystem.ChineseKauCim
    val today = LocalDate.now()

    // 抽签次数：每次重抽递增 → 改变结果（原日期种子确定性，重抽换卦）
    var askCount by rememberSaveable { mutableStateOf(0) }
    // 动画进行中：点击后先播对应系统的动画，播完才出结果
    var isDrawing by remember { mutableStateOf(false) }

    LaunchedEffect(isDrawing) {
        if (isDrawing) {
            delay(1400)
            isDrawing = false
            askCount++
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(system.label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            system.description(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SystemExplanation(system.key)

        FortuneCard {
            SectionTitle(system.actionLabel())
            Spacer(Modifier.height(12.dp))
            // 动作符号动画区：点击后按系统类型播放专属动画
            val actionIcon = system.actionIcon()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isDrawing) {
                    val trans = rememberInfiniteTransition(label = "drawAnim")
                    when (system.key) {
                        "chinese_kau_cim" -> {
                            // 摇签：签筒左右快速晃动 + 轻微旋转
                            val shake: Float by trans.animateFloat(
                                initialValue = -14f, targetValue = 14f,
                                animationSpec = infiniteRepeatable(tween(110), RepeatMode.Reverse),
                                label = "shake"
                            )
                            val tilt: Float by trans.animateFloat(
                                initialValue = -9f, targetValue = 9f,
                                animationSpec = infiniteRepeatable(tween(220), RepeatMode.Reverse),
                                label = "tilt"
                            )
                            Text(
                                actionIcon, style = MaterialTheme.typography.displayMedium,
                                modifier = Modifier.graphicsLayer {
                                    translationX = shake
                                    rotationZ = tilt
                                }
                            )
                        }
                        "cowrie" -> {
                            // 投掷贝壳：旋转 + 上下抛动
                            val spin: Float by trans.animateFloat(
                                initialValue = 0f, targetValue = 360f,
                                animationSpec = infiniteRepeatable(tween(320, easing = LinearEasing)),
                                label = "spin"
                            )
                            val hop: Float by trans.animateFloat(
                                initialValue = -18f, targetValue = 6f,
                                animationSpec = infiniteRepeatable(tween(160), RepeatMode.Reverse),
                                label = "hop"
                            )
                            Text(
                                actionIcon, style = MaterialTheme.typography.displayMedium,
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = spin
                                    translationY = hop
                                }
                            )
                        }
                        "bibliomancy" -> {
                            // 翻书：左右翻页（scaleX 脉动）
                            val flip: Float by trans.animateFloat(
                                initialValue = 0.35f, targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(280), RepeatMode.Reverse),
                                label = "flip"
                            )
                            Text(
                                actionIcon, style = MaterialTheme.typography.displayMedium,
                                modifier = Modifier.graphicsLayer { scaleX = flip }
                            )
                        }
                        "mizu_kuji" -> {
                            // 水签：签纸在水中上下沉浮 + 微旋转
                            val sink: Float by trans.animateFloat(
                                initialValue = -14f, targetValue = 10f,
                                animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse),
                                label = "sink"
                            )
                            val rock: Float by trans.animateFloat(
                                initialValue = -6f, targetValue = 6f,
                                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                                label = "rock"
                            )
                            Text(
                                actionIcon, style = MaterialTheme.typography.displayMedium,
                                modifier = Modifier.graphicsLayer {
                                    translationY = sink
                                    rotationZ = rock
                                }
                            )
                        }
                        "greek_oracle" -> {
                            // 凝视：神谕火焰脉动
                            val glow: Float by trans.animateFloat(
                                initialValue = 0.35f, targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(260), RepeatMode.Reverse),
                                label = "glow"
                            )
                            val rise: Float by trans.animateFloat(
                                initialValue = 4f, targetValue = -8f,
                                animationSpec = infiniteRepeatable(tween(260), RepeatMode.Reverse),
                                label = "rise"
                            )
                            Text(
                                actionIcon, style = MaterialTheme.typography.displayMedium,
                                modifier = Modifier.graphicsLayer {
                                    alpha = glow
                                    translationY = rise
                                }
                            )
                        }
                        else -> {
                            // 抽签/掣签等：上下浮动 + 轻微旋转
                            val float: Float by trans.animateFloat(
                                initialValue = -12f, targetValue = 10f,
                                animationSpec = infiniteRepeatable(tween(260), RepeatMode.Reverse),
                                label = "float"
                            )
                            val rot: Float by trans.animateFloat(
                                initialValue = -7f, targetValue = 7f,
                                animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
                                label = "rot"
                            )
                            Text(
                                actionIcon, style = MaterialTheme.typography.displayMedium,
                                modifier = Modifier.graphicsLayer {
                                    translationY = float
                                    rotationZ = rot
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        actionIcon,
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.graphicsLayer { alpha = 1f },
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { if (!isDrawing) isDrawing = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDrawing
            ) {
                Text(
                    when {
                        isDrawing -> "${system.actionLabel()}中…"
                        askCount == 0 -> "开始${system.actionLabel()}"
                        else -> "再${system.actionLabel()}一次（换一签）"
                    }
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "本页为确定性抽取：同一天首次结果固定；点「再抽一次」会重新起卦（模拟摇签桶变化）。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (askCount > 0) {
            val res = LotDraw.draw(system, today, askCount)
            // 按文化风格渲染成「一张签」：签头 + 纸面，文字用深色保证可读
            SignPaper(systemKey = system.key, result = res)
        }
    }
}

/** 签纸视觉样式：不同文化不同配色。 */
private data class LotSignStyle(
    val headColor: Color,      // 顶部签头颜色
    val headText: String,      // 签头标签
    val paperColor: Color,     // 签纸底色
    val borderColor: Color,    // 签纸描边
    val titleColor: Color,     // 标题色
    val textColor: Color       // 正文色
)

private fun lotSignStyle(systemKey: String): LotSignStyle = when (systemKey) {
    // 中国观音灵签：红纸金字
    "chinese_kau_cim" -> LotSignStyle(Color(0xFFC62828), "灵签", Color(0xFFFFF3E0), Color(0xFFC62828), Color(0xFFB23A48), Color(0xFF5D4037))
    // 藏传签卜：橙黄法轮
    "tibetan_div" -> LotSignStyle(Color(0xFFE07A3F), "签喻", Color(0xFFFFF8E1), Color(0xFFE07A3F), Color(0xFFB26A00), Color(0xFF5D4037))
    // 泰国暹罗签：金色神兽
    "thai_siam" -> LotSignStyle(Color(0xFFC9A227), "暹罗签", Color(0xFFFFFBE6), Color(0xFFC9A227), Color(0xFF8A6D00), Color(0xFF5D4037))
    // 古希腊神谕：蓝白大理石门
    "greek_oracle" -> LotSignStyle(Color(0xFF3A6FB0), "神谕", Color(0xFFF0F4FA), Color(0xFF3A6FB0), Color(0xFF2B5C9E), Color(0xFF37474F))
    // 圣经掣签：深蓝庄严
    "bible_lot" -> LotSignStyle(Color(0xFF37474F), "掣签", Color(0xFFECEFF1), Color(0xFF37474F), Color(0xFF2C3E50), Color(0xFF37474F))
    // 非洲贝壳：米白贝色
    "cowrie" -> LotSignStyle(Color(0xFFB08968), "贝卦", Color(0xFFF7F0E4), Color(0xFFB08968), Color(0xFF7A5C3E), Color(0xFF5D4037))
    // 翻书占卜：羊皮纸
    "bibliomancy" -> LotSignStyle(Color(0xFFA1887F), "箴言", Color(0xFFF5EBD8), Color(0xFFA1887F), Color(0xFF6D4C41), Color(0xFF5D4037))
    // 水占卜神签：水蓝
    "mizu_kuji" -> LotSignStyle(Color(0xFF4FC3F7), "水签", Color(0xFFE1F5FE), Color(0xFF4FC3F7), Color(0xFF0288D1), Color(0xFF01579B))
    // 日本御神签：白纸红纹
    "omikuji" -> LotSignStyle(Color(0xFFD32F2F), "御神签", Color(0xFFFFFDF7), Color(0xFFD32F2F), Color(0xFFB71C1C), Color(0xFF4E342E))
    else -> LotSignStyle(Color(0xFF7B1FA2), "签", Color(0xFFF3E5F5), Color(0xFF7B1FA2), Color(0xFF6A1B9A), Color(0xFF4A148C))
}

/** 签纸容器：顶部圆头 + 纸面主体，随文化风格变色。文字用深色保证可读。 */
@Composable
private fun SignPaper(systemKey: String, result: LotResult) {
    val style = lotSignStyle(systemKey)
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部签头（半圆头）
        Box(
            Modifier
                .width(64.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(style.headColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                style.headText,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        // 签纸主体
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(style.paperColor)
                .border(1.5.dp, style.borderColor, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                // 签题（大字深色）
                Text(
                    result.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = style.titleColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "抽取日期 · ${result.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = style.textColor.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(10.dp))
                result.detail.forEachIndexed { idx, (k, v) ->
                    if (idx > 0) Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            k + "：",
                            style = MaterialTheme.typography.titleSmall,
                            color = style.titleColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            v,
                            style = MaterialTheme.typography.bodyMedium,
                            color = style.textColor
                        )
                    }
                }
            }
        }
    }
}

/** 各系统的引导语 */
private fun LotSystem.description(): String = when (this) {
    LotSystem.ChineseKauCim -> "观音灵签 · 在签筒中摇出一支签，依签号查签诗断吉凶。点击「摇签」模拟摇动签筒。"
    LotSystem.TibetanDivination -> "藏传佛教签卜 · 寺院求签，以喻象分吉凶。点击「抽签」抽取一支。"
    LotSystem.ThaiSiamCee -> "泰国暹罗签 · 以神兽象征对应吉凶等级。点击「抽签」抽取一支。"
    LotSystem.GreekOracle -> "德尔斐神谕 · 凝视神谕之镜，浮现箴言。点击「凝视」浮现神谕。"
    LotSystem.BibleLot -> "《圣经》掣签 · 乌陵与土明求问神意。点击「掣签」求取指引。"
    LotSystem.CowrieShell -> "非洲贝壳占卜 · 投掷四枚贝壳成卦。点击「投掷」抛贝断卦。"
    LotSystem.Bibliomancy -> "翻书占卜 · 随机翻开经典取一句启示。点击「翻书」翻开一页。"
    LotSystem.MizuKuji -> "水占卜神签 · 贵船神社水签，签文遇水显现。点击「放水」显现签文。"
    LotSystem.Omikuji -> "日本御神签 · 神社抽签，分项指引运势。点击「抽签」抽取一支。"
}

/** 各系统的动作符号 */
private fun LotSystem.actionIcon(): String = when (this) {
    LotSystem.ChineseKauCim -> "🏮"
    LotSystem.TibetanDivination -> "☸️"
    LotSystem.ThaiSiamCee -> "🐘"
    LotSystem.GreekOracle -> "🏛️"
    LotSystem.BibleLot -> "✝️"
    LotSystem.CowrieShell -> "🐚"
    LotSystem.Bibliomancy -> "📖"
    LotSystem.MizuKuji -> "💧"
    LotSystem.Omikuji -> "⛩️"
}

/** 各系统的动作动词 */
private fun LotSystem.actionLabel(): String = when (this) {
    LotSystem.ChineseKauCim -> "摇签"
    LotSystem.TibetanDivination -> "抽签"
    LotSystem.ThaiSiamCee -> "抽签"
    LotSystem.GreekOracle -> "凝视"
    LotSystem.BibleLot -> "掣签"
    LotSystem.CowrieShell -> "投掷"
    LotSystem.Bibliomancy -> "翻书"
    LotSystem.MizuKuji -> "放水"
    LotSystem.Omikuji -> "抽签"
}
