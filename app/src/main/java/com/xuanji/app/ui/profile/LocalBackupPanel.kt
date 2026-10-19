package com.xuanji.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.xuanji.app.data.local.LocalBackupImportPreview
import com.xuanji.app.data.local.LocalBackupProfileKeys
import com.xuanji.app.data.local.LocalBackupRepositoryError
import com.xuanji.app.data.local.LocalBackupRestoreStatus
import com.xuanji.app.data.local.LocalDataBackupRepository
import com.xuanji.app.data.local.statistics
import com.xuanji.app.data.local.selectedConflicts
import com.xuanji.app.domain.LocalBackupSelection
import com.xuanji.app.domain.LocalDataBackupCodec
import com.xuanji.app.domain.EncryptedLocalBackupCodec
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LocalBackupPanel(repository: LocalDataBackupRepository, profileKeys: LocalBackupProfileKeys) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var exportFinished by remember { mutableStateOf<((Boolean?) -> Unit)?>(null) }
    var documentSelected by remember { mutableStateOf<((String?) -> Unit)?>(null) }

    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val content = pendingExportJson
        val finish = exportFinished
        pendingExportJson = null
        exportFinished = null
        if (uri == null) {
            finish?.invoke(null)
        } else if (content == null) {
            finish?.invoke(false)
        } else scope.launch {
            val saved = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(content) }
                        ?: error("open_failed")
                }
            }.isSuccess
            finish?.invoke(saved)
        }
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val finish = documentSelected
        documentSelected = null
        if (uri != null) scope.launch {
            val raw = runCatching {
                withContext(Dispatchers.IO) { readBoundedText(context.contentResolver.openInputStream(uri)) }
            }.getOrNull()
            finish?.invoke(raw)
        }
    }

    LocalBackupPanelContent(
        repository = repository,
        profileKeys = profileKeys,
        onCreateDocument = { name, json, finish ->
            pendingExportJson = json
            exportFinished = finish
            createDocument.launch(name)
        },
        onOpenDocument = { finish ->
            documentSelected = finish
            openDocument.launch(arrayOf("application/json", "text/*"))
        }
    )
}

@Composable
internal fun LocalBackupPanelContent(
    repository: LocalDataBackupRepository,
    profileKeys: LocalBackupProfileKeys,
    onCreateDocument: (name: String, json: String, onFinished: (Boolean?) -> Unit) -> Unit,
    onOpenDocument: (onSelected: (String?) -> Unit) -> Unit
) {
    var exportSelection by remember { mutableStateOf(LocalBackupSelection(favorites = true, actionFeedback = true)) }
    var importJson by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<LocalBackupImportPreview?>(null) }
    var importSelection by remember { mutableStateOf(LocalBackupSelection()) }
    var confirmOverwrite by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var encryptExport by remember { mutableStateOf(true) }
    var exportPassword by remember { mutableStateOf("") }
    var exportPasswordConfirm by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    var encryptedImportRequiresPassword by remember { mutableStateOf(false) }
    var importIsEncrypted by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    fun checkImport(raw: String, passphrase: CharArray? = null) {
        isWorking = true
        scope.launch {
            val checked = runCatching {
                withContext(Dispatchers.Default) { repository.preview(raw, profileKeys, passphrase) }
            }.getOrElse { LocalBackupImportPreview(error = LocalBackupRepositoryError.InvalidBackup) }
            passphrase?.fill('\u0000')
            preview = checked
            checked.backup?.let { backup ->
                encryptedImportRequiresPassword = false
                importSelection = backup.selection
            }
            isWorking = false
        }
    }
    fun acceptImport(raw: String?) {
        isWorking = true
        if (raw == null) {
            preview = LocalBackupImportPreview(error = LocalBackupRepositoryError.InvalidBackup)
            importJson = null
            isWorking = false
        } else {
            importJson = raw
            importPassword = ""
            importIsEncrypted = EncryptedLocalBackupCodec.isEncrypted(raw)
            encryptedImportRequiresPassword = importIsEncrypted
            if (encryptedImportRequiresPassword) {
                preview = null
                message = "这是加密备份；输入密码后才会解密并进行只读校验。"
                isWorking = false
            } else {
                checkImport(raw)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().testTag("local-backup-panel")) {
        Text("选择性本地备份", style = MaterialTheme.typography.titleMedium)
        Text(
            "可选收藏、偏好、今日安排与备注、库存/采购、餐单草稿、历史结果、行动反馈和游戏进度；不包含出生资料、聊天、长期记忆或象棋战绩。提醒不会随备份启用。文件不会上传。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LocalBackupCheckRow("运势与游戏收藏", exportSelection.favorites, "local-backup-export-favorites") {
            exportSelection = exportSelection.copy(favorites = it)
        }
        LocalBackupCheckRow("行动反馈", exportSelection.actionFeedback, "local-backup-export-feedback") {
            exportSelection = exportSelection.copy(actionFeedback = it)
        }
        LocalBackupCheckRow("诗句、星图与丝路游戏进度", exportSelection.companionGames, "local-backup-export-games") {
            exportSelection = exportSelection.copy(companionGames = it)
        }
        LocalBackupCheckRow("当前中国象棋棋谱", exportSelection.xiangqiSave, "local-backup-export-xiangqi") {
            exportSelection = exportSelection.copy(xiangqiSave = it)
        }
        LocalBackupCheckRow("饮食偏好", exportSelection.foodPreferences, "local-backup-export-food") { exportSelection = exportSelection.copy(foodPreferences = it) }
        LocalBackupCheckRow("活动偏好", exportSelection.activityPreferences, "local-backup-export-activity") { exportSelection = exportSelection.copy(activityPreferences = it) }
        LocalBackupCheckRow("安排、库存、采购与餐单草稿", exportSelection.agenda, "local-backup-export-agenda") { exportSelection = exportSelection.copy(agenda = it, notes = exportSelection.notes && it) }
        if (exportSelection.agenda) LocalBackupCheckRow("包含安排/库存备注及主动日记", exportSelection.notes, "local-backup-export-notes") { exportSelection = exportSelection.copy(notes = it) }
        LocalBackupCheckRow("保存的原始结果快照", exportSelection.snapshots, "local-backup-export-snapshots") { exportSelection = exportSelection.copy(snapshots = it) }
        LocalBackupCheckRow("使用密码加密备份（推荐）", encryptExport, "local-backup-encrypt") {
            encryptExport = it
            exportPassword = ""
            exportPasswordConfirm = ""
        }
        if (encryptExport) {
            OutlinedTextField(
                value = exportPassword,
                onValueChange = { exportPassword = it.take(128) },
                modifier = Modifier.fillMaxWidth().testTag("local-backup-export-password"),
                label = { Text("备份密码（至少 ${EncryptedLocalBackupCodec.MIN_PASSWORD_LENGTH} 位）") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            OutlinedTextField(
                value = exportPasswordConfirm,
                onValueChange = { exportPasswordConfirm = it.take(128) },
                modifier = Modifier.fillMaxWidth().testTag("local-backup-export-password-confirm"),
                label = { Text("再次输入备份密码") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Text("密码只用于本次文件加密，不会保存；忘记密码后无法恢复备份。", style = MaterialTheme.typography.bodySmall)
            if (exportPassword.isNotEmpty() && exportPassword.length < EncryptedLocalBackupCodec.MIN_PASSWORD_LENGTH) {
                Text("密码至少需要 ${EncryptedLocalBackupCodec.MIN_PASSWORD_LENGTH} 个字符。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            } else if (exportPasswordConfirm.isNotEmpty() && exportPassword != exportPasswordConfirm) {
                Text("两次输入的密码不一致。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        } else {
            Text("当前将导出明文文件；文件内容可被查看，请妥善保存。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = profileKeys.valid && exportSelection != LocalBackupSelection() && !isWorking &&
                    (!encryptExport || (exportPassword.length >= EncryptedLocalBackupCodec.MIN_PASSWORD_LENGTH && exportPassword == exportPasswordConfirm)),
                modifier = Modifier.weight(1f).testTag("local-backup-export"),
                onClick = {
                    scope.launch {
                        isWorking = true
                        val passphrase = if (encryptExport) exportPassword.toCharArray() else null
                        val result = runCatching {
                            withContext(Dispatchers.Default) { repository.export(profileKeys, exportSelection, passphrase = passphrase) }
                        }.getOrElse { com.xuanji.app.data.local.LocalBackupExportResult(error = LocalBackupRepositoryError.InvalidBackup) }
                        passphrase?.fill('\u0000')
                        exportPassword = ""
                        exportPasswordConfirm = ""
                        message = result.error?.let(::errorLabel)
                        isWorking = false
                        result.json?.let { json ->
                            onCreateDocument("xuanji-local-backup-${LocalDate.now()}.json", json) { saved ->
                                message = when (saved) {
                                    true -> "已保存选择的本地数据备份。"
                                    false -> "保存失败；原有本地数据没有改变。"
                                    null -> null
                                }
                            }
                        }
                    }
                }
            ) { Text("导出备份") }
            OutlinedButton(
                enabled = profileKeys.valid && !isWorking,
                modifier = Modifier.weight(1f).testTag("local-backup-import"),
                onClick = { onOpenDocument(::acceptImport) }
            ) { Text("选择备份文件") }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (!profileKeys.valid) Text("请先保存出生档案，备份会与当前档案指纹绑定。", style = MaterialTheme.typography.labelSmall)

        if (encryptedImportRequiresPassword) {
            OutlinedTextField(
                value = importPassword,
                onValueChange = { importPassword = it.take(128) },
                modifier = Modifier.fillMaxWidth().testTag("local-backup-import-password"),
                label = { Text("输入备份密码") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !isWorking && importPassword.length >= EncryptedLocalBackupCodec.MIN_PASSWORD_LENGTH,
                    modifier = Modifier.testTag("local-backup-unlock"),
                    onClick = { importJson?.let { raw -> checkImport(raw, importPassword.toCharArray()) } }
                ) { Text(if (isWorking) "正在校验…" else "解锁并预览") }
                TextButton(onClick = {
                    preview = null
                    importJson = null
                    importPassword = ""
                    encryptedImportRequiresPassword = false
                    importIsEncrypted = false
                }) { Text("取消") }
            }
        }

        preview?.let { value ->
            Card(Modifier.fillMaxWidth().testTag("local-backup-preview")) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("恢复预览", style = MaterialTheme.typography.titleSmall)
                    if (!value.valid) {
                        Text(errorLabel(value.error ?: LocalBackupRepositoryError.InvalidBackup), color = MaterialTheme.colorScheme.error)
                        Text("校验未通过，不会写入任何本地数据。", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(
                            if (importIsEncrypted) "密码认证、档案指纹与完整性校验均通过。"
                            else "档案指纹与校验和匹配；明文文件未加密，校验和不代表来源认证。",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("收藏 ${value.favoriteCount} 条 · 行动反馈 ${value.feedbackCount} 条 · 游戏进度 ${value.companionGameCount} 份 · 象棋棋谱 ${if (value.hasXiangqiSave) "1" else "0"} 份")
                        Text("安排 ${value.agendaCount} 条 · 库存 ${value.backup?.payload?.pantry?.size ?: 0} 种 · 原始快照 ${value.snapshotCount} 条；同 ID 冲突先确认，超限将阻止恢复。")
                        Text("主动日记 ${value.backup?.payload?.journal?.size ?: 0} 天；只在勾选备注分区后恢复，同日期修改需确认。")
                        Text("采购 ${value.backup?.payload?.shoppingPurchases?.size ?: 0} 条 · 餐单草稿 ${if (value.backup?.payload?.weeklyMenuDraft == null) 0 else 1} 份；手填数量/单位/期限随库存恢复，不启用提醒。")
                        val statistics = value.statistics(importSelection)
                        Text("新增 ${statistics.added} · 合并保留 ${statistics.merged} · 覆盖 ${statistics.overwritten} · 本次遗漏 ${statistics.omitted}",
                            modifier = Modifier.testTag("local-backup-preview-statistics"))
                        Text("按记录、偏好项及单独备注字段统计；遗漏表示本次未勾选，并非删除。相同内容合并保留。预览只读，确认时再次校验。", style = MaterialTheme.typography.bodySmall)
                        LocalBackupCheckRow("恢复收藏", importSelection.favorites, "local-backup-import-favorites") {
                            importSelection = importSelection.copy(favorites = it)
                        }
                        LocalBackupCheckRow("恢复行动反馈", importSelection.actionFeedback, "local-backup-import-feedback") {
                            importSelection = importSelection.copy(actionFeedback = it)
                        }
                        LocalBackupCheckRow("恢复角色游戏进度", importSelection.companionGames, "local-backup-import-games") {
                            importSelection = importSelection.copy(companionGames = it)
                        }
                        LocalBackupCheckRow("恢复象棋棋谱", importSelection.xiangqiSave, "local-backup-import-xiangqi") {
                            importSelection = importSelection.copy(xiangqiSave = it)
                        }
                        if (value.backup?.selection?.foodPreferences == true) LocalBackupCheckRow("恢复饮食偏好", importSelection.foodPreferences, "local-backup-import-food") { importSelection = importSelection.copy(foodPreferences = it) }
                        if (value.backup?.selection?.activityPreferences == true) LocalBackupCheckRow("恢复活动偏好", importSelection.activityPreferences, "local-backup-import-activity") { importSelection = importSelection.copy(activityPreferences = it) }
                        if (value.backup?.selection?.agenda == true) LocalBackupCheckRow("恢复安排、库存、采购与餐单草稿", importSelection.agenda, "local-backup-import-agenda") { importSelection = importSelection.copy(agenda = it, notes = importSelection.notes && it) }
                        if (importSelection.agenda && value.backup?.selection?.notes == true) LocalBackupCheckRow("包含安排/库存备注及主动日记", importSelection.notes, "local-backup-import-notes") { importSelection = importSelection.copy(notes = it) }
                        if (value.backup?.selection?.snapshots == true) LocalBackupCheckRow("恢复原始快照", importSelection.snapshots, "local-backup-import-snapshots") { importSelection = importSelection.copy(snapshots = it) }
                        val conflicts = value.selectedConflicts(importSelection)
                        if (conflicts.isNotEmpty()) {
                            Text("检测到同名数据：${conflicts.joinToString("、")}。冲突内容默认不覆盖。", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            enabled = importJson != null && importSelection != LocalBackupSelection() && !isWorking,
                            modifier = Modifier.fillMaxWidth().testTag("local-backup-restore"),
                            onClick = {
                                val selectedOverwriteConflicts = selectedConflicts(value, importSelection)
                                if (selectedOverwriteConflicts.isNotEmpty()) confirmOverwrite = true
                                else runRestore(repository, importJson.orEmpty(), profileKeys, importSelection, emptySet(), scope,
                                    importPassword.takeIf { importIsEncrypted }?.toCharArray()) { result ->
                                    message = restoreLabel(result.status)
                                    preview = null
                                    importJson = null
                                    importPassword = ""
                                    encryptedImportRequiresPassword = false
                                    importIsEncrypted = false
                                }
                            }
                        ) { Text("确认恢复所选项目") }
                        TextButton(
                            onClick = {
                                preview = null; importJson = null; importPassword = ""
                                encryptedImportRequiresPassword = false; importIsEncrypted = false
                            },
                            modifier = Modifier.testTag("local-backup-cancel-import")
                        ) { Text("取消导入") }
                    }
                }
            }
        }
    }

    if (confirmOverwrite) {
        val conflicts = preview?.let { selectedConflicts(it, importSelection) }.orEmpty()
        AlertDialog(
            onDismissRequest = { confirmOverwrite = false },
            title = { Text("明确覆盖冲突数据？") },
            text = { Text("将覆盖这些同档案数据：${conflicts.joinToString("、")}。其他内容保留；写入失败会回滚。请确认后继续。") },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("local-backup-confirm-overwrite"),
                    onClick = {
                        confirmOverwrite = false
                        val raw = importJson.orEmpty()
                        runRestore(repository, raw, profileKeys, importSelection, conflicts, scope,
                            importPassword.takeIf { importIsEncrypted }?.toCharArray()) { result ->
                            message = restoreLabel(result.status)
                            preview = null
                            importJson = null
                            importPassword = ""
                            encryptedImportRequiresPassword = false
                            importIsEncrypted = false
                        }
                    }
                ) { Text("确认覆盖并恢复") }
            },
            dismissButton = { TextButton(onClick = { confirmOverwrite = false }) { Text("返回") } }
        )
    }
}

@Composable
private fun LocalBackupCheckRow(label: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(42.dp).testTag(tag)) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.testTag("$tag-checkbox")
        )
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

private fun runRestore(
    repository: LocalDataBackupRepository,
    json: String,
    keys: LocalBackupProfileKeys,
    selection: LocalBackupSelection,
    overwrite: Set<String>,
    scope: kotlinx.coroutines.CoroutineScope,
    passphrase: CharArray? = null,
    onFinished: (com.xuanji.app.data.local.LocalBackupRestoreResult) -> Unit
) {
    scope.launch {
        val result = runCatching {
            withContext(Dispatchers.Default) { repository.restore(json, keys, selection, overwrite, passphrase) }
        }.getOrElse {
            com.xuanji.app.data.local.LocalBackupRestoreResult(
                status = LocalBackupRestoreStatus.Failed,
                error = LocalBackupRepositoryError.WriteFailed
            )
        }
        passphrase?.fill('\u0000')
        onFinished(result)
    }
}

private fun selectedConflicts(preview: LocalBackupImportPreview, selection: LocalBackupSelection): Set<String> {
    return preview.selectedConflicts(selection)
}

private fun readBoundedText(input: java.io.InputStream?): String? {
    input ?: return null
    input.use { stream ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            if (output.size() + count > EncryptedLocalBackupCodec.MAX_CONTAINER_BYTES) return null
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }
}

private fun errorLabel(error: LocalBackupRepositoryError): String = when (error) {
    LocalBackupRepositoryError.InvalidProfile -> "当前档案不可用。"
    LocalBackupRepositoryError.NoSectionsSelected -> "至少选择一个备份分区。"
    LocalBackupRepositoryError.CorruptFavorites -> "收藏数据无法安全读取，未覆盖原数据。"
    LocalBackupRepositoryError.CorruptFeedback -> "行动反馈无法安全读取，未覆盖原数据。"
    LocalBackupRepositoryError.InvalidBackup -> "文件格式、版本或完整性校验不通过。"
    LocalBackupRepositoryError.ProfileMismatch -> "该备份属于其他档案，不能恢复到当前档案。"
    LocalBackupRepositoryError.SelectionNotIncluded -> "所选分区不在该备份中。"
    LocalBackupRepositoryError.ConfirmationRequired -> "请先确认是否覆盖冲突存档。"
    LocalBackupRepositoryError.WriteFailed -> "操作失败；请检查可用空间与文件权限。"
    LocalBackupRepositoryError.PasswordRequired -> "这是加密备份，请输入备份密码。"
    LocalBackupRepositoryError.PasswordTooShort -> "备份密码长度不足。"
    LocalBackupRepositoryError.EncryptedBackupInvalid -> "密码不正确或加密文件已损坏；没有写入数据。"
}

private fun restoreLabel(status: LocalBackupRestoreStatus): String = when (status) {
    LocalBackupRestoreStatus.Completed -> "恢复完成；收藏和行动反馈已按 ID 合并。"
    LocalBackupRestoreStatus.ConfirmationRequired -> "检测到数据冲突，请确认覆盖。"
    LocalBackupRestoreStatus.InvalidBackup -> "恢复被阻止：备份校验或分区选择不正确。"
    LocalBackupRestoreStatus.Failed -> "恢复未能完整完成；请检查存储空间。"
}
