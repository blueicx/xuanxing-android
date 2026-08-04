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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.divination.KabbalahAstrology
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun KabbalahAstrologyScreen() {
    var name by rememberSaveable { mutableStateOf("") }
    val today = LocalDate.now()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("犹太占星 · 卡巴拉星象", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "基于《创世之书》（Sefer Yetzirah）与生命之树传统：希伯来十二个月各有对应星座（Mazalot）、支派与感官，蕴含卡巴拉深意；姓名可经 Gematria 字母数值对应生命之树。本页以确定性算法离线计算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val r = KabbalahAstrology.calculate(today, name)
        FortuneCard {
            SectionTitle("今日卡巴拉星象")
            Spacer(Modifier.height(8.dp))
            InfoRow("希伯来历", "${r.hebrewYear}年 ${r.monthName} ${r.hebrewDay}日")
            InfoRow("星座", "${r.zodiacSign}（${r.hebrewZodiacName}）")
            InfoRow("支派", r.tribe)
            InfoRow("感官", r.sense)
            Spacer(Modifier.height(6.dp))
            Text(r.kabbalahMeaning, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "灵性课题：${r.spiritualTheme}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(r.verdict, style = MaterialTheme.typography.bodyMedium)
        }

        FortuneCard {
            SectionTitle("Gematria 姓名数值")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("输入姓名（汉字自动转写，或拉丁/希伯来字母）") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            if (name.isNotBlank()) {
                val g = KabbalahAstrology.gematria(name)
                Spacer(Modifier.height(8.dp))
                Text("转写拉丁：${g.transliteration}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("希伯来：${g.hebrewSequence}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text("数值：${g.total}　数根：${g.digitRoot}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(g.text, style = MaterialTheme.typography.bodyMedium)
            }
        }

        FortuneCard {
            SectionTitle("生命之树（Sefirot）与行星")
            Spacer(Modifier.height(8.dp))
            KabbalahAstrology.sefirot().forEachIndexed { i, (n, planet, symbol) ->
                Text(
                    "${i + 1}. $n　$planet（$symbol）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        FortuneCard {
            SectionTitle("十二个月 · 星座对应")
            Spacer(Modifier.height(8.dp))
            KabbalahAstrology.monthTable().forEach { (m, z, extra) ->
                Text(
                    "$m → $z",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
        SystemExplanation("jewish")
    }
}
