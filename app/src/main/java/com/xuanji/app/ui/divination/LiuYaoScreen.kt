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
import com.xuanji.app.domain.divination.LiuYao
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import com.xuanji.app.ui.viewmodel.LiuYaoViewModel
import com.xuanji.app.ui.xuanjiViewModel

private val POS_NAME = listOf("初", "二", "三", "四", "五", "上")

@Composable
fun LiuYaoScreen() {
    val viewModel = xuanjiViewModel { LiuYaoViewModel(AppModule.liuYaoRepository) }
    val result by viewModel.result.collectAsStateWithLifecycle()
    val question by viewModel.question.collectAsStateWithLifecycle()
    val reading by viewModel.reading.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("六爻", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "三枚铜钱起卦，纳甲装爻、定六亲世应。先在下方写下你的问题，再点「重新摇卦」；摇出结果后会有针对性的现代解说。结果仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = question,
            onValueChange = viewModel::setQuestion,
            label = { Text("先写下你的问题") },
            placeholder = { Text("例如：找丢失的钥匙 / 事业能否升职 / 感情走向") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(12.dp)
        )

        Button(onClick = { viewModel.cast() }, modifier = Modifier.fillMaxWidth()) {
            Text("重新摇卦")
        }

        result?.let { res ->
            FortuneCard {
                SectionTitle("本卦 · 《${res.original.name}》")
                Spacer(Modifier.height(4.dp))
                Text(
                    "${res.original.palace}宫 · 世在${res.shiYao}爻 · 应在${res.yingYao}爻",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                res.lines.reversed().forEach { line ->
                    HexLineRow(line)
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("卦辞：${res.original.judgment}", style = MaterialTheme.typography.bodyMedium)
            }

            res.changed?.let { ch ->
                FortuneCard {
                    SectionTitle("变卦 · 《${ch.name}》")
                    Spacer(Modifier.height(8.dp))
                    Text("卦辞：${ch.judgment}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            FortuneCard {
                SectionTitle("断语")
                Spacer(Modifier.height(8.dp))
                Text(res.reading, style = MaterialTheme.typography.bodyMedium)
            }

            reading?.let { rd ->
                FortuneCard {
                    SectionTitle("现代解说 · ${rd.category.label}")
                    Spacer(Modifier.height(8.dp))
                    if (rd.question.isNotBlank()) {
                        Text(
                            "问题：${rd.question}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(rd.general, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(rd.specific, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        SystemExplanation("liuyao")
    }
}

@Composable
private fun HexLineRow(line: LiuYao.LineInfo) {
    val yaoLabel = POS_NAME[line.pos - 1] + if (line.yang) "九" else "六"
    val lineColor = if (line.changing) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // 爻线图（阳=整条，阴=断开两节）
        Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (line.yang) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(lineColor)
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(lineColor)
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(lineColor)
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(yaoLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "${line.diZhi} · ${line.liuQin}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val marks = buildList {
            if (line.isShi) add("世")
            if (line.isYing) add("应")
            if (line.changing) add(if (line.yang) "〇" else "✕")
        }
        if (marks.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(marks.joinToString(" "), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
