package com.xuanji.app.ui.divination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.NineStars
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun NineStarsScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("九星気学", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "日本版八字：以立春为年度分界算「本命星」，再据五行生克推荐参拜神社与吉利方位。仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val p = profile
        if (p == null) {
            Text("尚未设置出生信息，请先到「我的」填写。")
        } else {
            val res = NineStars.calculate(LocalDate.of(p.birthYear, p.birthMonth, p.birthDay))
            FortuneCard {
                SectionTitle("本命星 · ${res.star.name}")
                Spacer(Modifier.height(8.dp))
                Text(
                    "计算年份：${res.adjustedYear}（生日${if (res.birthYear != res.adjustedYear) "在立春前，按前一年" else "在立春后"}）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text("五行：${res.star.element}　本位方位：${res.star.direction}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text(res.star.summary, style = MaterialTheme.typography.bodyMedium)
            }

            FortuneCard {
                SectionTitle("生克匹配要点")
                Spacer(Modifier.height(8.dp))
                Text(
                    "生我（${shengWo(res.star.element)}）·大吉　同我（${res.star.element}）·吉",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "克我（${keWo(res.star.element)}）·宜回避",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            FortuneCard {
                SectionTitle("六维解读")
                Spacer(Modifier.height(8.dp))
                Text(
                    res.verdict,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val recommend = res.matches.filter {
                it.relation.startsWith("生我") || it.relation.startsWith("同我")
            }
            val avoid = res.matches.filter { it.relation.contains("回避") }

            FortuneCard {
                SectionTitle("推荐参拜（生我 / 同我）")
                Spacer(Modifier.height(8.dp))
                if (recommend.isEmpty()) {
                    Text("（暂无匹配）", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    recommend.forEach { m ->
                        Text(
                            "${m.shrine.name}（祭神 ${m.shrine.deity} · ${m.shrine.element} · ${m.shrine.direction}）— ${m.relation}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            if (avoid.isNotEmpty()) {
                FortuneCard {
                    SectionTitle("宜回避（克我）")
                    Spacer(Modifier.height(8.dp))
                    avoid.forEach { m ->
                        Text(
                            "${m.shrine.name}（${m.shrine.element} · ${m.shrine.direction}）",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
        SystemExplanation("ninestars")
    }
}

// 生我：该元素生我（如 水生木 → 木的生我是水）
private fun shengWo(wx: String): String = when (wx) {
    "水" -> "金"
    "木" -> "水"
    "火" -> "木"
    "土" -> "火"
    "金" -> "土"
    else -> ""
}

// 克我：该元素克我（如 金克木 → 木的克我是金）
private fun keWo(wx: String): String = when (wx) {
    "水" -> "土"
    "木" -> "金"
    "火" -> "水"
    "土" -> "木"
    "金" -> "火"
    else -> ""
}
