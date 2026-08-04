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
import com.xuanji.app.domain.divination.CelticTreeCalendar
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun CelticTreeCalendarScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("凯尔特树历", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "凯尔特树历（Celtic Tree Calendar）以 13 个树月对应欧甘字母（Ogham）与守护动物，12 月 23 日为神秘的无名日。本页基于罗伯特·格雷夫斯的流行化重构，确定性离线计算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val p = profile
        val birth = p?.let { LocalDate.of(it.birthYear, it.birthMonth, it.birthDay) }
        if (birth == null) {
            FortuneCard { Text("尚未设置出生信息，请先到「我的」填写。", style = MaterialTheme.typography.bodyMedium) }
        } else {
            val r = CelticTreeCalendar.treeSign(birth)
            FortuneCard {
                SectionTitle("本命树月 · ${r.name}")
                Spacer(Modifier.height(8.dp))
                InfoRow("欧甘字母", r.ogham)
                InfoRow("元素", r.element)
                InfoRow("守护动物", r.animal)
                if (r.personality.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("【性格】${r.personality}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("【命运】${r.fate}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("【灵性指引】${r.guidance}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("幸运色：${r.luckyColor}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(r.verdict, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Spacer(Modifier.height(6.dp))
                    Text(r.personality.ifBlank { "这一天属于凯尔特树历中的无名日，适合反思与灵性探索。" }, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        FortuneCard {
            SectionTitle("十三树月一览")
            Spacer(Modifier.height(8.dp))
            CelticTreeCalendar.treeTable().forEach { (name, ogham, extra) ->
                Text(
                    "$name　$ogham　—　$extra",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
        SystemExplanation("celtic")
    }
}
