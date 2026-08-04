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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.Numerology
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation

@Composable
fun NumerologyScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    var name by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("数字命理学", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "基础数字由出生年月日推得；输入英文/拼音全名，可得完整五数：生命路径、命运、灵魂渴望、人格与成熟数。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val p = profile
        if (p == null) {
            Text("尚未设置出生信息，请先到「我的」填写。")
        } else {
            val items = Numerology.calculate(p)
            FortuneCard {
                SectionTitle("基础数字（出生日期）")
                Spacer(Modifier.height(8.dp))
                items.forEach { it ->
                    InfoRow(if (it.isMaster) "${it.label}（大师数）" else it.label, "${it.number}")
                    Text(it.trait, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(it.verdict, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
            }

            FortuneCard {
                SectionTitle("完整五数（姓名）")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("英文 / 拼音全名，如 JOHN MICHAEL SMITH") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (name.trim().isNotEmpty()) {
                    Numerology.calculateFull(p, name).forEach { f ->
                        InfoRow(f.label, "${f.number}")
                        Text(f.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(f.verdict, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                    }
                } else {
                    Text("输入姓名后展示完整五数解读。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "生命数字由出生年月日各数位累加化简而得，大师数 11/22/33 不进一步化简；命运/灵魂渴望/人格数由姓名（A=1…Z=26）计算，成熟数 = 生命路径 + 命运。仅供娱乐参考。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SystemExplanation("numerology")
    }
}
