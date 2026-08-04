package com.xuanji.app.ui.divination

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.GuaCommons
import com.xuanji.app.domain.divination.MeiHua
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import com.xuanji.app.ui.viewmodel.MeiHuaViewModel
import com.xuanji.app.ui.xuanjiViewModel

@Composable
fun MeiHuaScreen() {
    val viewModel = xuanjiViewModel { MeiHuaViewModel(AppModule.repository) }
    val result by viewModel.result.collectAsStateWithLifecycle()
    val benming by viewModel.benming.collectAsStateWithLifecycle()
    val question by viewModel.question.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("梅花易数", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "以年月日时起卦（先天八卦数），观体用生克断吉凶。可先写下问题，再点「重新起卦」。结果仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = question,
            onValueChange = viewModel::setQuestion,
            label = { Text("先写下你的问题（可留空）") },
            placeholder = { Text("例如：此事能否成 / 寻人方位") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(12.dp)
        )
        Button(onClick = { viewModel.recast() }, modifier = Modifier.fillMaxWidth()) {
            Text("重新起卦")
        }

        benming?.let { bm ->
            FortuneCard {
                SectionTitle("本命卦（按生日起卦，参考）")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TrigramColumn(bm.upper, "上")
                    Spacer(Modifier.width(16.dp))
                    TrigramColumn(bm.lower, "下")
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(bm.name, style = MaterialTheme.typography.titleMedium)
                        Text("${bm.upper.wx} / ${bm.lower.wx}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        result?.let { res ->
            FortuneCard {
                SectionTitle("本卦")
                Spacer(Modifier.height(8.dp))
                GuaRow(res.original)
            }
            FortuneCard {
                SectionTitle("互卦 · 变卦")
                Spacer(Modifier.height(8.dp))
                Text("互卦", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                GuaRow(res.mutual)
                Spacer(Modifier.height(10.dp))
                Text("变卦（动在${listOf("初", "二", "三", "四", "五", "上")[res.movingLine - 1]}爻）", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                GuaRow(res.changed)
            }
            FortuneCard {
                SectionTitle("体用与解说")
                Spacer(Modifier.height(8.dp))
                Text(
                    "体卦：${res.ti.cn}（${res.ti.wx}）  用卦：${res.yong.cn}（${res.yong.wx}）",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(res.tiYongRelation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
                Text(res.reading, style = MaterialTheme.typography.bodyMedium)
            }
        }
        SystemExplanation("meihua")
    }
}

@Composable
private fun GuaRow(gua: MeiHua.GuaView) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TrigramColumn(gua.upper, "上")
        Spacer(Modifier.width(16.dp))
        TrigramColumn(gua.lower, "下")
        Spacer(Modifier.width(16.dp))
        Column {
            Text(gua.name, style = MaterialTheme.typography.titleMedium)
            Text("${gua.upper.wx} / ${gua.lower.wx}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TrigramColumn(t: GuaCommons.Trigram, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        t.lines.forEach { yang ->
            Box(
                Modifier
                    .width(36.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (yang) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.height(3.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text("$label ${t.cn}${t.symbol}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
