package com.xuanji.app.ui.test

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.local.DataStorePreferenceBridge
import com.xuanji.app.data.local.QuestionnaireDraftStore
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.test.QuestionnaireDraft
import com.xuanji.app.domain.test.QuestionnaireSpec
import kotlinx.coroutines.launch

class QuestionnaireProgress<T, P>(
    val index: MutableState<Int>,
    val answers: SnapshotStateList<T>,
    val pick: MutableState<P>,
    val spec: QuestionnaireSpec,
    val decode: (String) -> T,
    val emptyPick: P
) {
    var reviewed by mutableStateOf(false)
}

internal data class QuestionnairePersistence(val owner: String, val store: QuestionnaireDraftStore)
internal val LocalQuestionnairePersistence = staticCompositionLocalOf<QuestionnairePersistence?> { null }

@Composable
internal fun rememberIntQuestionnaire(id: String, count: Int, options: Set<Int>, empty: Int) =
    rememberQuestionnaire(id, List(count) { options.map(Int::toString).toSet() }, empty,
        { it.toString() }, { it.toInt() }, { if (it == empty) null else it.toString() }, { it?.toInt() ?: empty })

@Composable
internal fun rememberBooleanQuestionnaire(id: String, count: Int) =
    rememberQuestionnaire<Boolean, Boolean?>(id, List(count) { setOf("true", "false") }, null,
        { it.toString() }, { it.toBooleanStrict() }, { it?.toString() }, { it?.toBooleanStrict() })

@Composable
internal fun rememberStringQuestionnaire(id: String, options: List<Set<String>>) =
    rememberQuestionnaire<String, String?>(id, options, null, { it }, { it }, { it }, { it })

@Composable
private fun <T, P> rememberQuestionnaire(
    id: String, options: List<Set<String>>, empty: P, encode: (T) -> String, decode: (String) -> T,
    encodePick: (P) -> String?, decodePick: (String?) -> P
): QuestionnaireProgress<T, P>? {
    val context = LocalContext.current.applicationContext
    val injected = LocalQuestionnairePersistence.current
    val owner = if (injected != null) injected.owner else {
        val profile by AppModule.repository.userProfileState.collectAsStateWithLifecycle()
        profile?.let(AppModule.actionRepository::profileKey)
    }
    val spec = remember(id) { QuestionnaireSpec(id, options) }
    val store = injected?.store ?: remember(context) { QuestionnaireDraftStore(DataStorePreferenceBridge(context)) }
    val revisions by QuestionnaireDraftStore.revisions.collectAsStateWithLifecycle()
    val epoch = (owner?.let { revisions[com.xuanji.app.domain.action.StableScore.sha256(it)] } ?: 0L) + (revisions["*"] ?: 0L)
    val state = remember(owner, id) { QuestionnaireProgress(mutableStateOf(0), mutableStateListOf<T>(), mutableStateOf(empty), spec, decode, empty) }
    var loaded by remember(owner, id, epoch) { mutableStateOf(false) }
    var blocked by remember(owner, id, epoch) { mutableStateOf(false) }
    var failure by remember(owner, id, epoch) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(owner, id, epoch) {
        runCatching {
            val draft = owner?.let { store.load(it, spec) } ?: QuestionnaireDraft()
            blocked = !draft.readable
            if (draft.readable) {
                state.index.value = draft.index
                state.answers.clear()
                state.answers.addAll(draft.answers.map(decode))
                state.pick.value = decodePick(draft.pick)
                state.reviewed = false
            }
        }.onFailure { failure = true; blocked = true }
        loaded = true
    }
    LaunchedEffect(owner, id, loaded, blocked, epoch) {
        if (owner == null || !loaded || blocked) return@LaunchedEffect
        snapshotFlow { QuestionnaireDraft(state.index.value, state.answers.map(encode), encodePick(state.pick.value)) }
            .collect { draft ->
                if (spec.valid(draft)) {
                    runCatching { store.save(owner, spec, draft, epoch) }.onFailure { failure = true }
                }
            }
    }
    if (!loaded) { Text("正在读取本机答题草稿…"); return null }
    if (blocked) {
        Text("旧草稿损坏或题库版本已变化，未覆盖原数据。清除后可重新答题。")
        TextButton(onClick = { scope.launch {
            runCatching { owner?.let { store.clear(it, spec) } }.onSuccess { blocked = false; failure = false }
        } }) { Text("清除这份答题草稿") }
        return null
    }
    if (failure) Text("草稿保存失败，请勿退出；未伪装为已保存。")
    if (owner == null) Text("尚无出生档案：本次作答不持久保存。")
    if (state.index.value == 0) SideEffect { state.reviewed = false }
    return state
}

/** Review changes in-place: editing question one does not discard the remaining answers. */
@Composable
internal fun <T, P> QuestionnaireReview(progress: QuestionnaireProgress<T, P>, questions: List<String>, labels: Map<String, String> = emptyMap()) {
    Column(Modifier.testTag("questionnaire-review")) {
        Text("计分前复核 · ${progress.answers.size}/${questions.size} 题")
        Text("这些题目是项目自我探索内容，不是正式诊断量表或人口常模。答案不会自动交给玄师。")
        TextButton(onClick = {
            progress.index.value = 0
            progress.answers.clear()
            progress.pick.value = progress.emptyPick
            progress.reviewed = false
        }) { Text("放弃此份答案并重新开始") }
        questions.forEachIndexed { index, question ->
            Text("${index + 1}. $question")
            progress.spec.options[index].sorted().forEach { option ->
                val selected = progress.answers.getOrNull(index) == progress.decode(option)
                OutlinedButton(onClick = {
                    if (index < progress.answers.size) progress.answers[index] = progress.decode(option)
                    else if (index == progress.answers.size) progress.answers.add(progress.decode(option))
                }, modifier = Modifier.fillMaxWidth().testTag("review-$index-$option")) {
                    Text((if (selected) "✓ " else "") + (labels[option] ?: option))
                }
            }
        }
        Button(onClick = { progress.reviewed = true }, enabled = progress.answers.size == questions.size,
            modifier = Modifier.testTag("questionnaire-score")) { Text("确认答案并计分") }
    }
}
