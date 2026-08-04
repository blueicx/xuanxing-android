package com.xuanji.app.ui.divination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.Onmyodo
import com.xuanji.app.domain.divination.OnmyodoProfile
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation

@Composable
fun OnmyodoScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("阴阳道 · 本命星", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "日本阴阳道融合中国阴阳五行与北斗信仰：以出生年地支定「本命星」（北斗七星），以虚岁推「属星」（九曜）。本页基于确定性算法离线计算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val p = profile
        if (p == null) {
            FortuneCard {
                Text("尚未设置出生信息，请先到「我的」填写。", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            val result = Onmyodo.profile(p.birthYear, p.birthMonth)
            ProfileCard(result)
        }

        FortuneCard {
            SectionTitle("九曜属星（当年星）周期")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    listOf("日", "月", "火", "水", "木").forEach { s ->
                        Text("$s　—　${OnmyodoZokuseiMeaning(s)}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    listOf("金", "土", "罗睺", "计都").forEach { s ->
                        Text("$s　—　${OnmyodoZokuseiMeaning(s)}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        FortuneCard {
            SectionTitle("十二生肖 · 本命星对照表")
            Spacer(Modifier.height(8.dp))
            val table = Onmyodo.honmeiTable()
            table.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    row.forEach { (label, star, full) ->
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("$star（$full）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        SystemExplanation("onmyodo")
    }
}

@Composable
private fun ProfileCard(r: OnmyodoProfile) {
    FortuneCard {
        SectionTitle("本命星 · ${r.honmei.star}")
        Spacer(Modifier.height(8.dp))
        Text(
            "生肖：${r.honmei.zodiac}（${r.honmei.branch}）　计算年份：${r.honmei.adjustedYear}年" +
                if (r.honmei.adjustedYear != r.honmei.birthYear) "（生日在立春前，按前一年）" else "",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(4.dp))
        Text("本命星全称：${r.honmei.starFull}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "本命星解读：${r.honmei.meaning}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "属星（当年星）：${r.zokusei.star}　·　虚岁 ${r.zokusei.age} 岁",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(r.zokusei.meaning, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        Text(
            "六维解读",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(4.dp))
        Text(r.verdict, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun OnmyodoZokuseiMeaning(star: String): String = when (star) {
    "日" -> "光明权威，主名声"
    "月" -> "温和滋养，主人缘"
    "火" -> "行动果决，主竞争"
    "水" -> "聪慧善言，主学习"
    "木" -> "仁厚广博，主贵人"
    "金" -> "优雅富足，主审美"
    "土" -> "稳重担当，主责任"
    "罗睺" -> "隐伏多变，主突变"
    "计都" -> "业力收束，主转化"
    else -> ""
}
