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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.HumanDesign
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import com.xuanji.app.ui.viewmodel.HumanDesignViewModel
import com.xuanji.app.ui.xuanjiViewModel

@Composable
fun HumanDesignScreen() {
    val viewModel = xuanjiViewModel { HumanDesignViewModel(AppModule.repository) }
    val result by viewModel.result.collectAsStateWithLifecycle()
    val hasProfile by viewModel.hasProfile.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("人类图（近似）", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "由你的出生星盘推演类型与权威（离线简化近似，非完整人类图计算）。结果仅供娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SystemExplanation("humandesign")

        if (!hasProfile) {
            Text("尚未设置出生信息，请先在「我的」中填写生日。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            return@Column
        }

        result?.let { res ->
            FortuneCard {
                SectionTitle("六维解读")
                Spacer(Modifier.height(8.dp))
                Text(res.verdict, style = MaterialTheme.typography.bodyMedium)
            }
            FortuneCard {
                SectionTitle("类型 · ${res.type}")
                Spacer(Modifier.height(8.dp))
                Text(res.typeDesc, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("人生策略 · ${res.strategy}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                Text("非自己主题 · ${res.notSelf}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("权威 · ${res.authority}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                Text(res.authorityDesc, style = MaterialTheme.typography.bodyMedium)
            }
            FortuneCard {
                SectionTitle("人生角色 · ${res.profile}")
                Spacer(Modifier.height(8.dp))
                Text(res.profileDesc, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                SectionTitle("定义类型 · ${res.definition}")
                Spacer(Modifier.height(4.dp))
                Text(res.definitionDesc, style = MaterialTheme.typography.bodyMedium)
                if (res.channels.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    SectionTitle("激活通道")
                    Spacer(Modifier.height(4.dp))
                    res.channels.forEach { ch ->
                        Text("• $ch", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            FortuneCard {
                SectionTitle("九大能量中心")
                Spacer(Modifier.height(8.dp))
                res.centers.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { c ->
                            CenterChip(c)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
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
private fun CenterChip(c: HumanDesign.Center) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 8.dp, bottom = 4.dp)
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (c.defined) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "${c.nameCn}${if (c.defined) "·有定义" else "·开放"}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
