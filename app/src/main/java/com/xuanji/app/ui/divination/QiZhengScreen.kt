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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.QiZheng
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import com.xuanji.app.ui.viewmodel.QiZhengViewModel
import com.xuanji.app.ui.xuanjiViewModel

@Composable
fun QiZhengScreen() {
    val viewModel = xuanjiViewModel { QiZhengViewModel(AppModule.repository) }
    val result by viewModel.result.collectAsStateWithLifecycle()
    val hasProfile by viewModel.hasProfile.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("七政四余", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "中式星命：日、月、五曜（七政）与罗睺、计都、月孛、紫气（四余）所落黄道十二宫。按你的出生信息推算。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SystemExplanation("qizheng")

        if (!hasProfile) {
            Text("尚未设置出生信息，请先在「我的」中填写生日。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            return@Column
        }

        result?.let { res ->
            val seven = res.bodies.filter { it.kind == "七政" }
            val four = res.bodies.filter { it.kind == "四余" }
            FortuneCard {
                SectionTitle("七政")
                Spacer(Modifier.height(8.dp))
                BodyGrid(seven)
            }
            FortuneCard {
                SectionTitle("四余")
                Spacer(Modifier.height(8.dp))
                BodyGrid(four)
            }
            FortuneCard {
                SectionTitle("综合解读")
                Spacer(Modifier.height(8.dp))
                Text(res.verdict, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            FortuneCard {
                SectionTitle("说明")
                Spacer(Modifier.height(8.dp))
                Text(res.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BodyGrid(bodies: List<QiZheng.Body>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        bodies.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { b ->
                    Row(Modifier.weight(1f)) {
                        Text(b.symbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text("${b.name} · ${b.palace}", style = MaterialTheme.typography.bodyMedium)
                            Text("${b.degreeInPalace}°（${b.kind}）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                // 补足奇数行
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
