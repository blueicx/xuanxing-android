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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import com.xuanji.app.ui.viewmodel.LenormandViewModel
import com.xuanji.app.ui.xuanjiViewModel

@Composable
fun LenormandScreen() {
    val viewModel = xuanjiViewModel { LenormandViewModel() }
    val result by viewModel.result.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("雷诺曼", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "36 张符号卡牌，抽 3 张对应过去 / 现在 / 未来。静心默念后点「抽牌」。仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = { viewModel.draw() }, modifier = Modifier.fillMaxWidth()) {
            Text("抽牌")
        }

        result?.let { res ->
            FortuneCard {
                SectionTitle("三牌总览")
                Spacer(Modifier.height(8.dp))
                Text(res.verdict, style = MaterialTheme.typography.bodyMedium)
            }
            res.draws.forEach { d ->
                FortuneCard {
                    SectionTitle("${d.position} · 第 ${d.card.num} 张 ${d.card.nameCn}")
                    Spacer(Modifier.height(8.dp))
                    Text(d.card.nameEn, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(d.card.meaning, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(d.card.verdict, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        SystemExplanation("lenormand")
    }
}
