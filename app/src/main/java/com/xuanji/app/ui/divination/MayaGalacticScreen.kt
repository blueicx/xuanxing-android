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
import com.xuanji.app.domain.divination.MayaGalactic
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun MayaGalacticScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("玛雅占星 · 星系印记", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "基于卓尔金历（Tzolk'in）260 天周期：20 图腾 × 13 音阶 = 260 个星系印记（Kin），并推演五大天赋图腾（主/支持/挑战/指引/推动）。本页以确定性算法离线计算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val p = profile
        if (p == null) {
            FortuneCard { Text("尚未设置出生信息，请先到「我的」填写。", style = MaterialTheme.typography.bodyMedium) }
        } else {
            val r = MayaGalactic.calculate(LocalDate.of(p.birthYear, p.birthMonth, p.birthDay))
            FortuneCard {
                SectionTitle("星系印记 · ${r.fullSignature}")
                Spacer(Modifier.height(8.dp))
                InfoRow("KIN", "${r.kin} / 260")
                InfoRow("图腾", "${r.signChinese}（${r.signOriginal}）")
                InfoRow("音阶", "${r.toneChinese}")
                InfoRow("颜色 / 元素", "${r.color} / ${r.element}")
                Spacer(Modifier.height(8.dp))
                Text("五大天赋图腾：", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                Text("主图腾：${r.totems["main"]}（核心天赋与本质）", style = MaterialTheme.typography.bodyMedium)
                Text("支持图腾：${r.totems["support"]}（力量与资源）", style = MaterialTheme.typography.bodyMedium)
                Text("挑战图腾：${r.totems["challenge"]}（需要面对的课题）", style = MaterialTheme.typography.bodyMedium)
                Text("指引图腾：${r.totems["guide"]}（内在指引与方向）", style = MaterialTheme.typography.bodyMedium)
                Text("推动图腾：${r.totems["antipode"]}（潜能的催化剂）", style = MaterialTheme.typography.bodyMedium)
            }
            FortuneCard {
                SectionTitle("深度解读")
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
        SystemExplanation("maya-galactic")
    }
}
