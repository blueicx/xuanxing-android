package com.xuanji.app.ui.test

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.local.DataStorePreferenceBridge
import com.xuanji.app.data.local.TestReferenceConsentStore
import com.xuanji.app.data.model.TestRecord
import com.xuanji.app.di.AppModule
import kotlinx.coroutines.launch

@Composable
fun profileTestRecords(): List<TestRecord> {
    val profile by AppModule.repository.userProfileState.collectAsStateWithLifecycle()
    val owner = profile?.let(AppModule.actionRepository::profileKey)
    return key(owner) {
        val flow = remember(owner) { AppModule.testRecordRepository.recordsForProfile(owner) }
        val records by flow.collectAsStateWithLifecycle(initialValue = emptyList())
        if (owner == null) emptyList() else records
    }
}

@Composable
fun authorizedCompanionTest(records: List<TestRecord>): TestRecord? {
    val profile by AppModule.repository.userProfileState.collectAsStateWithLifecycle()
    val owner = profile?.let(AppModule.actionRepository::profileKey)
    val context = LocalContext.current.applicationContext
    val store = remember(context) { TestReferenceConsentStore(DataStorePreferenceBridge(context)) }
    val revision by TestReferenceConsentStore.revision.collectAsStateWithLifecycle()
    // Key by records and consent revision: revocation immediately hides a previously loaded result.
    var result by remember(owner, records, revision) { mutableStateOf<TestRecord?>(null) }
    LaunchedEffect(owner, records, revision) {
        result = owner?.let { runCatching { store.authorized(it, records) }.getOrNull() }
    }
    return result
}

@Composable
internal fun TestReferenceConsentButton(record: TestRecord, records: List<TestRecord>) {
    val context = LocalContext.current.applicationContext
    val profile by AppModule.repository.userProfileState.collectAsStateWithLifecycle()
    val owner = profile?.let(AppModule.actionRepository::profileKey)
    val current = authorizedCompanionTest(records)
    val store = remember(context) { TestReferenceConsentStore(DataStorePreferenceBridge(context)) }
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf(false) }
    TextButton(enabled = owner != null, onClick = {
        scope.launch {
            runCatching {
                if (current == record) store.revoke(requireNotNull(owner)) else store.grant(requireNotNull(owner), record)
            }.onFailure { error = true }
        }
    }) { Text(if (current == record) "撤回玄师引用授权" else "允许玄师引用此条结果摘要") }
    if (error) Text("授权更新失败，未改变原授权。")
}
