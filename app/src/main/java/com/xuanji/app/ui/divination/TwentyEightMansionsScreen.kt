package com.xuanji.app.ui.divination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.xuanji.app.domain.divination.TwentyEightMansions
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun TwentyEightMansionsScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    val today = LocalDate.now()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("二十八星宿", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "以 2000-01-07（角木蛟）为基准，每 28 日循环一次。含四象、五行、吉凶与详细性格/运势解读。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SystemExplanation("ershiba")

        val birthMansion = profile?.let {
            TwentyEightMansions.byDate(LocalDate.of(it.birthYear, it.birthMonth, it.birthDay))
        }
        if (birthMansion != null) {
            MansionCard("本命星宿", birthMansion)
        } else {
            Text("尚未设置出生信息，请先在「我的」中填写生日以查看本命星宿。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        MansionCard("今日值日星宿", TwentyEightMansions.byDate(today))

        FortuneCard {
            SectionTitle("二十八宿总览（四象分组）")
            Spacer(Modifier.height(8.dp))
            listOf("青龙", "玄武", "白虎", "朱雀").forEach { group ->
                Text(group + "七宿：", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                val names = TwentyEightMansions.byConstellation()[group]?.joinToString(" → ") { "${it.name}(${it.lucky})" } ?: ""
                Text(names, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
            }
        }
        FortuneCard {
            SectionTitle("说明")
            Spacer(Modifier.height(8.dp))
            Text(
                "本页为二十八星宿体系的文化演绎，算法基准以 2000 年 1 月 7 日为角木蛟起点，每 28 日循环。仅供自我探索与娱乐参考。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MansionCard(tag: String, m: TwentyEightMansions.Mansion) {
    val luckSymbol = when (m.lucky) {
        "吉" -> "🟢"
        "中" -> "🟡"
        else -> "🔴"
    }
    FortuneCard {
        SectionTitle("$tag · ${m.name}（${m.pinyin}）")
        Spacer(Modifier.height(8.dp))
        InfoRow("四象", m.constellation)
        InfoRow("动物", m.animal)
        InfoRow("五行", m.element)
        InfoRow("方位", m.direction)
        InfoRow("吉凶", "$luckSymbol ${m.lucky}")
        Spacer(Modifier.height(8.dp))
        Text("性格：${m.personality}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Text("事业：${m.career}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Text("财运：${m.wealth}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Text("爱情：${m.love}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Text("健康：${m.health}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Text("幸运色：${m.luckyColor}　·　幸运数字：${m.luckyNumber}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Text("建议：${m.advice}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Text(m.summary, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}
