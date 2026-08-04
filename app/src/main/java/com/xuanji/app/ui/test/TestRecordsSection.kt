package com.xuanji.app.ui.test

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.model.TestRecord
import com.xuanji.app.di.AppModule
import com.xuanji.app.ui.components.SectionTitle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 测试记录：持久化每次测试结果，按「职业 / 性格 / 趣味」分组展示在测试 tab 顶部。
 */

/** 便捷保存入口：各测试结果页调用，带去重（同一结果只存一次，防重组重复保存）。 */
object TestRecordRecorder {
    private val savedKeys = mutableSetOf<String>()

    /** @param resultKey 结果的唯一标识（如 "MBTI|INTJ"），用于去重 */
    fun save(testName: String, category: String, resultCode: String, resultName: String, resultKey: String) {
        if (!savedKeys.add(resultKey)) return   // 已保存过该结果，跳过
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch { AppModule.testRecordRepository.addRecord(testName, category, resultCode, resultName) }
    }

    fun reset() { savedKeys.clear() }
}

/** 测试记录区块：显示在测试 tab 顶部，按职业/性格/趣味分组。 */
@Composable
fun TestRecordsSection() {
    val records by AppModule.testRecordRepository.records.collectAsStateWithLifecycle(initialValue = emptyList())
    var expanded by remember { mutableStateOf(false) }

    if (records.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("测试记录（${records.size}）")
                Text(
                    if (expanded) "收起 ⌃" else "展开 ⌄",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                listOf("职业", "性格", "趣味").forEach { cat ->
                    val catRecords = records.filter { it.category == cat }
                    if (catRecords.isNotEmpty()) {
                        Text("【$cat】", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                        catRecords.forEach { r ->
                            Text(
                                "${r.date} · ${r.testName} → ${r.resultCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Text(
                    "结果保存在本机，仅供回顾。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
