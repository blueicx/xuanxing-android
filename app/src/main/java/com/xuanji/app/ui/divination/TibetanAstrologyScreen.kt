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
import com.xuanji.app.domain.divination.TibetanAstrology
import com.xuanji.app.domain.divination.TibetanFiveElements
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun TibetanAstrologyScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    val today = LocalDate.now()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("西藏占星", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "藏历时轮历融合印度与中原历法，以星曜日、太阴日、月宿与五行元素推演每日吉凶。本页基于确定性算法离线计算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 今日运势
        val todayReading = TibetanAstrology.dailyReading(today)
        DailyReadingCard(todayReading.date, todayReading.elements, todayReading.interpretation, todayReading.score, todayReading.band, todayReading.verdict)

        // 生日五要素（若有资料）
        val p = profile
        if (p != null) {
            val birth = LocalDate.of(p.birthYear, p.birthMonth, p.birthDay)
            val birthElements = TibetanAstrology.fiveElements(birth)
            FortuneCard {
                SectionTitle("本命五要素 · ${birth.year}年${birth.monthValue}月${birth.dayOfMonth}日")
                Spacer(Modifier.height(8.dp))
                ElementGrid(birthElements)
                Spacer(Modifier.height(12.dp))
                Text(
                    "本命底色（${birthElements.nakshatraName} · ${birthElements.nakshatraElement}性）：${TibetanAstrology.elementNature(birthElements.nakshatraElement)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "出生日气场（${birthElements.weekdayName} · ${birthElements.weekdayElement}）：${TibetanAstrology.elementNature(birthElements.weekdayElement)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SystemExplanation("tibet")
    }
}

@Composable
private fun DailyReadingCard(
    date: LocalDate,
    e: TibetanFiveElements,
    interpretation: String,
    score: Int,
    band: String,
    verdict: String
) {
    FortuneCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionTitle("今日 · ${date.monthValue}月${date.dayOfMonth}日")
            }
            Text(
                "$band $score 分",
                style = MaterialTheme.typography.titleMedium,
                color = if (score >= 65) MaterialTheme.colorScheme.primary
                        else if (score >= 50) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(8.dp))
        ElementGrid(e)
        Spacer(Modifier.height(12.dp))
        Text(
            "元素组合：${e.weekdayElement}（${e.weekdayName}） × ${e.nakshatraElement}（${e.nakshatraName}）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(interpretation, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        Text(
            "六维解读",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(4.dp))
        Text(verdict, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ElementGrid(e: TibetanFiveElements) {
    val rows = listOf(
        "星曜日" to "${e.weekdayName}（${e.weekdayElement}）",
        "太阴日" to "${e.lunarDate}",
        "月宿" to "${e.nakshatraName}（${e.nakshatraElement}）",
        "结合期" to e.conjunctionName,
        "运动期" to e.motionName
    )
    rows.forEach { (label, value) ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(64.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
