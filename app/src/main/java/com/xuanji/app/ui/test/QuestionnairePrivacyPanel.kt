package com.xuanji.app.ui.test

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.local.*
import com.xuanji.app.di.AppModule
import kotlinx.coroutines.launch

@Composable
fun QuestionnairePrivacyPanel() {
    val profile by AppModule.repository.userProfileState.collectAsStateWithLifecycle()
    val owner = profile?.let(AppModule.actionRepository::profileKey) ?: return
    val context = LocalContext.current.applicationContext
    val bridge = remember(context) { DataStorePreferenceBridge(context) }
    val scope = rememberCoroutineScope()
    var section by remember(owner) { mutableStateOf<String?>(null) }
    var status by remember(owner) { mutableStateOf("") }
    Text("答题草稿、分数历史与玄师授权仅在本机，默认不进入备份、搜索或长期记忆。")
    listOf("答题草稿", "测验历史", "玄师测验授权").forEach { title ->
        TextButton(onClick = { section = title }) { Text("清除当前档案$title") }
    }
    if (status.isNotBlank()) Text(status)
    section?.let { title -> AlertDialog(onDismissRequest = { section = null }, title = { Text("清除$title？") },
        text = { Text("仅清除当前档案的$title，不影响其他档案、出生资料和旧版未归档记录。不能撤销；清除历史会同时撤回引用授权。") },
        confirmButton = { TextButton(onClick = { scope.launch {
            status = runCatching {
                when (title) {
                    "答题草稿" -> QuestionnaireDraftStore(bridge).clearProfile(owner)
                    "测验历史" -> { ProfileTestHistoryStore(bridge).clear(owner); TestReferenceConsentStore(bridge).revoke(owner) }
                    else -> TestReferenceConsentStore(bridge).revoke(owner)
                }
            }.fold({ "已清除$title。" }, { "清除未完成，请重试；未显示为成功。" })
            section = null
        } }) { Text("确认清除") } }, dismissButton = { TextButton(onClick = { section = null }) { Text("取消") } }) }
}
