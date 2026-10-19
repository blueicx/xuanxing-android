package com.xuanji.app.ui.test

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.model.TestRecord
import com.xuanji.app.di.AppModule
import com.google.gson.Gson
import com.xuanji.app.ui.components.SectionTitle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * 测试记录：持久化每次测试结果，按「职业 / 性格 / 趣味」分组展示在测试 tab 顶部。
 */

/** 便捷保存入口：各测试结果页调用，带去重（同一结果只存一次，防重组重复保存）。 */
object TestRecordRecorder {

    /** @param resultKey 结果的唯一标识（如 "MBTI|INTJ"），用于去重 */
    fun save(testName: String, category: String, resultCode: String, resultName: String, resultKey: String, rawScores: Map<String, Int> = emptyMap()) {
        if (resultKey.isBlank()) return
        val owner = AppModule.repository.userProfileState.value?.let(AppModule.actionRepository::profileKey) ?: return
        val epoch = com.xuanji.app.data.local.ProfileTestHistoryStore.epoch(owner)
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch { AppModule.testRecordRepository.addRecord(testName, category, resultCode, resultName, owner, rawScores, epoch) }
    }

    fun reset() = Unit // Result effects and store equality protect recomposition, not a process-global personality cache.
}

private data class TestRecordExport(
    val app: String,
    val type: String,
    val version: Int,
    val exportedAt: String,
    val records: List<TestRecord>
)

private val exportGson = Gson()

/** 测试记录区块：显示在测试 tab 顶部，按职业/性格/趣味分组。 */
@Composable
fun TestRecordsSection() {
    val currentRecords = profileTestRecords()
    val legacyRecords by AppModule.testRecordRepository.records.collectAsStateWithLifecycle(initialValue = emptyList())
    var showLegacy by remember { mutableStateOf(false) }
    val records = if (showLegacy) legacyRecords else currentRecords
    val recordingError by AppModule.testRecordRepository.recordingError.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val payload = TestRecordExport(
                    app = "玄星",
                    type = "test_records",
                    version = 1,
                    exportedAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    records = records
                )
                val output = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("Unable to open export destination")
                output.use { stream ->
                    stream.write(exportGson.toJson(payload).toByteArray(Charsets.UTF_8))
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "已导出测试记录", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    recordingError?.let { Text(it) }
    if (legacyRecords.isNotEmpty()) androidx.compose.material3.TextButton(onClick = { showLegacy = !showLegacy }) {
        Text(if (showLegacy) "返回当前档案记录" else "查看旧版未归档记录（${legacyRecords.size}）")
    }
    if (showLegacy) Text("旧版记录没有档案归属和完整原始分；只读保留，不擅自归到当前档案，也不自动授权玄师。下方导出仅导出这批旧记录。")
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "导出",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.clickable {
                            val stamp = LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))
                            exportLauncher.launch("xuanji-test-records-$stamp.json")
                        }
                    )
                    Text(
                        if (expanded) "收起 ⌃" else "展开 ⌄",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                (listOf("职业", "性格", "趣味", "自我探索") + records.map { it.category }).distinct().forEach { cat ->
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
                            if (r.rawScores.orEmpty().isNotEmpty()) Text("原始/项目分：" + r.rawScores.orEmpty().entries.joinToString { "${it.key}=${it.value}" }, style = MaterialTheme.typography.bodySmall)
                            val previous = records.filter { it.testName == r.testName && it.date < r.date }.maxByOrNull { it.date }
                            previous?.let { before ->
                                val differences = com.xuanji.app.domain.TestHistoryComparison.differences(before, r)
                                Text(if (differences.isEmpty()) "与上次标签对照：${before.resultCode} → ${r.resultCode}；旧记录没有可比较的同版本分数。"
                                    else "较上次同版项目分变化：" + differences.entries.joinToString { "${it.key} ${if (it.value >= 0) "+" else ""}${it.value}" }, style = MaterialTheme.typography.bodySmall)
                            }
                            if (!showLegacy) TestReferenceConsentButton(r, records)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Text(
                    if (showLegacy) "旧版记录保持原样；导出不包含出生档案。" else "当前档案最多保存100条，不静默覆盖旧结果；分数变化不等于心理改善、恶化或人格诊断。导出不包含出生档案。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
