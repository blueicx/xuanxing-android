package com.xuanji.app.ui.divination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.ZiweiCalculator
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation

@Composable
fun ZiweiScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    var school by remember { mutableStateOf("中州派") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("紫微斗数 · 三派排盘", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "中州派（王亭之口传）、北派（河洛四化）、闽派（《全书》庚干阳武同阴）。切换派别可对比三派不同的四化规则与解读。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 派别选择
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("中州派", "北派", "闽派").forEach { s ->
                FilterChip(
                    selected = school == s,
                    onClick = { school = s },
                    label = { Text(s) }
                )
            }
        }

        val p = profile
        if (p == null) {
            Text(
                "尚未设置出生信息，请先到「我的」填写。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val chart = ZiweiCalculator.calculate(
                p.birthYear, p.birthMonth, p.birthDay, p.birthHour, p.birthMinute,
                gender = if (p.gender == "女") "female" else "male",
                school = school
            )
            FortuneCard {
                SectionTitle("基本信息 · ${chart.school}")
                Spacer(Modifier.height(8.dp))
                Text("出生：${chart.year}年${chart.month}月${chart.day}日 ${chart.hour}时", style = MaterialTheme.typography.bodyMedium)
                Text("年干支：${chart.yearGan}${chart.yearZhi}　五行局：${chart.bureau}", style = MaterialTheme.typography.bodyMedium)
                Text("命宫：${chart.lifePalace}　身宫：${chart.bodyPalace}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(chart.schoolInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            FortuneCard {
                SectionTitle("十二宫命盘")
                Spacer(Modifier.height(4.dp))
                chart.palaces.forEach { palace ->
                    val tag = when {
                        palace.isLife && palace.isBody -> "（命·身）"
                        palace.isLife -> "（命）"
                        palace.isBody -> "（身）"
                        else -> ""
                    }
                    val starStr = when {
                        palace.mainStars.isNotEmpty() -> palace.mainStars.joinToString(" ")
                        else -> "空宫"
                    }
                    val aux = buildList {
                        if (palace.luckyStars.isNotEmpty()) add(palace.luckyStars.joinToString(" "))
                        if (palace.badStars.isNotEmpty()) add(palace.badStars.joinToString(" "))
                        if (palace.hua.isNotEmpty()) add(palace.hua.joinToString(" "))
                    }.joinToString(" ｜ ")
                    Text(
                        "${palace.gan}${palace.branch} $starStr",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (palace.isLife || palace.isBody) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (aux.isNotEmpty()) {
                        Text(
                            aux,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                        )
                    }
                    Text(
                        "　${palace.name}$tag",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            FortuneCard {
                SectionTitle("生年四化（${chart.school}）")
                Spacer(Modifier.height(8.dp))
                chart.fourTrans.forEach { t ->
                    Text(t, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
                }
            }

            // 重点宫位深度解读
            FortuneCard {
                SectionTitle("重点宫位深度解读")
                Spacer(Modifier.height(8.dp))
                listOf("命宫", "夫妻", "事业", "财帛", "迁移").forEach { name ->
                    val palace = chart.palaces.firstOrNull { it.name == name } ?: return@forEach
                    Text("【$name】", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(ZiweiCalculator.palaceMeaning(name) ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (palace.mainStars.isEmpty()) {
                        Text("空宫，可借对宫星曜参考。", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
                    } else {
                        palace.mainStars.forEach { star ->
                            val info = ZiweiCalculator.starInfo(star)
                            if (info != null) {
                                Text(
                                    "$star 在$name",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("  性格：${info["性格"]}", style = MaterialTheme.typography.bodySmall)
                                Text("  事业：${info["事业"]}", style = MaterialTheme.typography.bodySmall)
                                Text("  爱情：${info["爱情"]}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
                            }
                        }
                    }
                }
            }

            Text(chart.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SystemExplanation("ziwei")
    }
}
