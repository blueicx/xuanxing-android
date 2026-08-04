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
import com.xuanji.app.domain.divination.AZTEC_DAY_SIGNS
import com.xuanji.app.domain.divination.AztecAstrology
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun AztecAstrologyScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("阿兹特克占星", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "基于 Tonalpohualli（托纳尔波瓦利）260 天神圣历法：20 日符 × 13 数字 = 260 个独特印记，每符有守护神与方位。以 Alfonso Caso 相关性锚定历法。本页确定性离线计算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val p = profile
        if (p == null) {
            FortuneCard { Text("尚未设置出生信息，请先到「我的」填写。", style = MaterialTheme.typography.bodyMedium) }
        } else {
            val r = AztecAstrology.calculate(LocalDate.of(p.birthYear, p.birthMonth, p.birthDay))
            FortuneCard {
                SectionTitle("Tonalpohualli 印记 · ${r.fullSignature}")
                Spacer(Modifier.height(8.dp))
                InfoRow("日符", "${r.signChinese}（${r.signEnglish}）")
                InfoRow("数字", "${r.number} / 13")
                InfoRow("守护神", r.god)
                InfoRow("方位", r.direction)
                Spacer(Modifier.height(8.dp))
                Text("关键词：${r.interpretation.keywords}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("性格：${r.interpretation.personality}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("天赋：${r.interpretation.talent}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("挑战：${r.interpretation.challenge}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("人生课题：${r.interpretation.theme}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(r.verdict, style = MaterialTheme.typography.bodyMedium)
            }
        }

        FortuneCard {
            SectionTitle("二十日符一览")
            Spacer(Modifier.height(8.dp))
            AZTEC_DAY_SIGNS.forEachIndexed { i, s ->
                Text(
                    "${i + 1}. ${s.chinese}（${s.english}）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
        SystemExplanation("aztec")
    }
}
