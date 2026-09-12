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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.divination.Ifa
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun IfaScreen() {
    var question by rememberSaveable { mutableStateOf("") }
    var askCount by rememberSaveable { mutableStateOf(0) }
    val today = LocalDate.now()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("艾法预言", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "约鲁巴文明的艾法体系（2008 年列入联合国非遗）依赖棕榈果仪式、求问语境与 Ese 口传。本页仅以「日期 + 求问内容」确定性生成 16 主 Odu 名称索引，不替代实际占卜，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FortuneCard {
            SectionTitle("问卦")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = question, onValueChange = { question = it },
                label = { Text("输入您想问的问题（如：关于我的事业发展）") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { askCount++ }) { Text("重新问卦（换一卦）") }
        }

        val r = Ifa.divine(today, question, askCount)
        FortuneCard {
            SectionTitle("Odu · ${r.combination.canonicalName}")
            Spacer(Modifier.height(8.dp))
            Text("离线 256 组合索引：${r.combination.index}（${r.binaryPattern}）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(r.reading, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text("Ese 经文正文未随应用内置；需要受权传承语料与受训者解读。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        FortuneCard {
            SectionTitle("十六主 Odu 一览")
            Spacer(Modifier.height(8.dp))
            Ifa.oduTable().forEach { (name, reading) ->
                Text(
                    "$name — $reading",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
        SystemExplanation("ifa")
    }
}
