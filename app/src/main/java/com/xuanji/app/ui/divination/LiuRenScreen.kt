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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.divination.LiuRen
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun LiuRenScreen() {
    val now = LocalDate.now()
    val res = LiuRen.calculate(now.year, now.monthValue, now.dayOfMonth, 12)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("大六壬（四课三传）", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "古代「三式」之一，此简化版以当日年月日时推四课三传。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SystemExplanation("liuren")
        FortuneCard {
            SectionTitle("今日四课")
            Spacer(Modifier.height(8.dp))
            res.kes.forEachIndexed { i, ke ->
                Text("第${i + 1}课：上神 ${ke.upper}，下神 ${ke.lower}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        FortuneCard {
            SectionTitle("三传")
            Spacer(Modifier.height(8.dp))
            Text("初传：${res.first}（事情起始）", style = MaterialTheme.typography.bodyMedium)
            Text("中传：${res.second}（事情发展）", style = MaterialTheme.typography.bodyMedium)
            Text("末传：${res.third}（事情结果）", style = MaterialTheme.typography.bodyMedium)
        }
        FortuneCard {
            SectionTitle("五行生克简析")
            Spacer(Modifier.height(8.dp))
            Text(res.verdict, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "综合断语：初传${res.first}，中传${res.second}，末传${res.third}，暗示事情需经历先难后易的过程，最终可成。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
