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
import com.xuanji.app.domain.divination.ThirteenMoon
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation

@Composable
fun ThirteenMoonScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("十三月亮历", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "20 图腾 × 13 调性 = 260 个 Kin 的卓尔金历。按你的出生日期计算星系印记、出生波符与五大天赋力量。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SystemExplanation("dreamspell")

        val p = profile
        if (p == null) {
            Text("尚未设置出生信息，请先在「我的」中填写生日。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            return@Column
        }

        val res = ThirteenMoon.cast(java.time.LocalDate.of(p.birthYear, p.birthMonth, p.birthDay))
        FortuneCard {
            SectionTitle("星系印记 · ${res.tone}的${res.sign}")
            Spacer(Modifier.height(8.dp))
            Text("Kin ${res.kin}　·　颜色 ${res.color}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(8.dp))
            Text("调性「${res.tone}」：${res.toneKeyword}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("代表问题：${res.toneQuestion}", style = MaterialTheme.typography.bodyMedium)
        }
        FortuneCard {
            SectionTitle("六维解读")
            Spacer(Modifier.height(8.dp))
            Text(res.verdict, style = MaterialTheme.typography.bodyMedium)
        }
        FortuneCard {
            SectionTitle("图腾深度解读 · ${res.sign}")
            Spacer(Modifier.height(8.dp))
            Text("关键词 · ${res.signKeyword}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text("性格：${res.personality}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("天赋：${res.talent}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("挑战：${res.challenge}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("灵性指引：${res.guidance}", style = MaterialTheme.typography.bodyMedium)
        }
        FortuneCard {
            SectionTitle("五大天赋力量")
            Spacer(Modifier.height(8.dp))
            res.totems.forEach { (role, t) ->
                Text("• $role：${t.name}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
            }
        }
        FortuneCard {
            SectionTitle("出生波符（13 个生命课题）")
            Spacer(Modifier.height(8.dp))
            res.wave.forEachIndexed { i, w ->
                Text(
                    "${i + 1}. ${w.tone}的${w.sign} — ${w.keyword}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("　　❓ ${w.question}", style = MaterialTheme.typography.bodySmall)
            }
        }
        FortuneCard {
            SectionTitle("说明")
            Spacer(Modifier.height(8.dp))
            Text(
                "本页为 Dreamspell（何西·阿圭列斯）体系的确定性离线实现：Kin 由年份/月份数值表与日期算出。仅供文化娱乐参考。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
