package com.xuanji.app.ui.divination

import androidx.compose.foundation.background
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
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.viewmodel.ReferenceViewModel
import com.xuanji.app.ui.xuanjiViewModel

@Composable
fun ReferenceScreen(key: String) {
    val viewModel = xuanjiViewModel { ReferenceViewModel(AppModule.referenceRepository, key) }
    val entry by viewModel.entry.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (entry == null) {
            Text("未找到该体系资料。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            return@Column
        }
        val e = entry!!

        // 醒目免责横幅
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(12.dp)
        ) {
            Text(
                "⚠ 资料卡 · 不可离线算法化",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "本体系依赖专门历表、师承或仪式，无法在本机用确定性算法推算。以下仅为文化资料介绍，不含任何测算结果。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        Text(e.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("所属文明：${e.region}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        FortuneCard {
            SectionTitle("概述")
            Spacer(Modifier.height(8.dp))
            Text(e.summary, style = MaterialTheme.typography.bodyMedium)
        }
        FortuneCard {
            SectionTitle("核心要点")
            Spacer(Modifier.height(8.dp))
            e.concepts.forEach { c ->
                Text("· $c", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
            }
        }
        FortuneCard {
            SectionTitle("说明")
            Spacer(Modifier.height(8.dp))
            Text(e.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
