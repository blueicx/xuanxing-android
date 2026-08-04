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
import com.xuanji.app.domain.divination.MEDICINE_WHEEL_DIRECTIONS
import com.xuanji.app.domain.divination.MedicineWheel
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun MedicineWheelScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("北美药轮", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "药轮是北美原住民的古老智慧，象征生命的循环与自然的力量。按出生月份定位四大方向（东鹰/南熊/西狼/北野牛），各有元素、颜色、动物图腾与灵性指引。本页确定性离线计算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val p = profile
        if (p == null) {
            FortuneCard { Text("尚未设置出生信息，请先到「我的」填写。", style = MaterialTheme.typography.bodyMedium) }
        } else {
            val d = MedicineWheel.directionFor(LocalDate.of(p.birthYear, p.birthMonth, p.birthDay))
            FortuneCard {
                SectionTitle("药轮位置 · ${d.name}")
                Spacer(Modifier.height(8.dp))
                InfoRow("季节", d.season)
                InfoRow("元素", d.element)
                InfoRow("颜色", d.color)
                InfoRow("动物图腾", d.animal)
                InfoRow("象征", d.symbol)
                Spacer(Modifier.height(8.dp))
                Text("关键词：${d.keywords}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("性格：${d.personality}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("天赋：${d.talent}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("挑战：${d.challenge}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("人生课题：${d.theme}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("灵性指引：${d.guidance}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(d.verdict, style = MaterialTheme.typography.bodyMedium)
            }
        }

        FortuneCard {
            SectionTitle("药轮四大方向")
            Spacer(Modifier.height(8.dp))
            MEDICINE_WHEEL_DIRECTIONS.forEach { d ->
                Text(
                    "${d.name} · ${d.season} · ${d.animal} · ${d.element}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
        SystemExplanation("medicinewheel")
    }
}
