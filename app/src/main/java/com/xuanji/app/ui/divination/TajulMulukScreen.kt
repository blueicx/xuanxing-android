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
import com.xuanji.app.domain.divination.TajulMuluk
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation

@Composable
fun TajulMulukScreen() {
    var husband by rememberSaveable { mutableStateOf("") }
    var wife by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Tajul Muluk 合婚", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "源自班贾尔族 Babilangan 婚配仪式的姓名合婚法：以 Abjadiyyah 字母数值系统计算双方姓名数值，对 7 取余后查配对解读。支持阿拉伯字母或拉丁字母转写。本页基于确定性算法离线计算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FortuneCard {
            SectionTitle("输入双方姓名")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = husband,
                onValueChange = { husband = it },
                label = { Text("男方姓名（拉丁或阿拉伯字母）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = wife,
                onValueChange = { wife = it },
                label = { Text("女方姓名（拉丁或阿拉伯字母）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        if (husband.isNotBlank() && wife.isNotBlank()) {
            val r = TajulMuluk.compute(husband, wife)
            FortuneCard {
                SectionTitle("配对结果 · ${r.interpretation.level}")
                Spacer(Modifier.height(8.dp))
                Text(
                    "${r.husbandName}（数值 ${r.husbandSum}，余 ${r.husbandRemainder}）× ${r.wifeName}（数值 ${r.wifeSum}，余 ${r.wifeRemainder}）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "等级：${r.interpretation.level}　寓意：${r.interpretation.omen}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (r.interpretation.level in setOf("极佳", "上等")) MaterialTheme.colorScheme.primary
                            else if (r.interpretation.level == "中等") MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
                Text(r.interpretation.text, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "传统建议：${r.interpretation.advice}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        FortuneCard {
            SectionTitle("Abjadiyyah 数值（部分字母）")
            Spacer(Modifier.height(8.dp))
            Text(
                "ا=1 ب=2 ج=3 د=4 ه=5 و=6 ز=7 ح=8 ط=9 ي=10 ك=20 ل=30 م=40 ن=50 س=60 ع=70 ف=80 ص=90 ق=100 ر=200 ش=300 ت=400…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SystemExplanation("tajulmuluk")
    }
}
