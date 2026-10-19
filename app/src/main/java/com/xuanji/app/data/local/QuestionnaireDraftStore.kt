package com.xuanji.app.data.local

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.xuanji.app.domain.action.StableScore
import com.xuanji.app.domain.test.QuestionnaireDraft
import com.xuanji.app.domain.test.QuestionnaireSpec
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Private answers remain outside backups, search and companion memory. */
class QuestionnaireDraftStore(private val bridge: PreferenceBridge) {
    private fun key(owner: String, spec: QuestionnaireSpec): String {
        require(owner.isNotBlank())
        return "questionnaire_draft_" + StableScore.sha256(owner) + "_" + spec.id
    }
    suspend fun load(owner: String, spec: QuestionnaireSpec): QuestionnaireDraft {
        val raw = bridge.read(key(owner, spec)) ?: return QuestionnaireDraft()
        return runCatching {
            require(raw.length <= 32_000)
            val json = JsonParser.parseString(raw).asJsonObject
            require(json["version"].asInt == 1 && json["owner"].asString == StableScore.sha256(owner))
            require(json["fingerprint"].asString == spec.fingerprint)
            val answers = json["answers"].asJsonArray.map { require(it.isJsonPrimitive && it.asJsonPrimitive.isString); it.asString }
            val draft = QuestionnaireDraft(json["index"].asInt, answers, json["pick"]?.takeUnless { it.isJsonNull }?.asString)
            require(spec.valid(draft))
            draft
        }.getOrElse { QuestionnaireDraft(readable = false) }
    }
    suspend fun save(owner: String, spec: QuestionnaireSpec, draft: QuestionnaireDraft, expectedEpoch: Long? = null) = LocalDataEraseFence.writes.withLock { mutex.withLock {
        check(expectedEpoch == null || expectedEpoch == epoch(owner)) { "questionnaire_autosave_expired" }
        require(spec.valid(draft))
        check(load(owner, spec).readable) { "questionnaire_draft_unreadable" }
        bridge.write(key(owner, spec), Gson().toJson(mapOf("version" to 1, "owner" to StableScore.sha256(owner),
            "fingerprint" to spec.fingerprint, "index" to draft.index, "answers" to draft.answers, "pick" to draft.pick)))
    } }
    suspend fun clear(owner: String, spec: QuestionnaireSpec) = mutex.withLock { bridge.delete(key(owner, spec)) }
    suspend fun clearProfile(owner: String) = mutex.withLock {
        require(owner.isNotBlank())
        val prefix = "questionnaire_draft_" + StableScore.sha256(owner) + "_"
        val expected = com.xuanji.app.domain.test.QuestionnaireCatalog.ids.associate { id -> (prefix + id).let { it to bridge.read(it) } }
        check(bridge.compareAndWriteBatch(expected, expected.mapValues { null })) { "questionnaire_clear_conflict" }
        val hash = StableScore.sha256(owner)
        epochs.update { it + (hash to ((it[hash] ?: 0) + 1)) }
    }
    companion object {
        private val mutex = Mutex()
        private val epochs = kotlinx.coroutines.flow.MutableStateFlow<Map<String, Long>>(emptyMap())
        val revisions = epochs.asStateFlow()
        fun epoch(owner: String): Long = (epochs.value[StableScore.sha256(owner)] ?: 0) + (epochs.value["*"] ?: 0)
        internal fun invalidateAll() { epochs.update { it + ("*" to ((it["*"] ?: 0) + 1)) } }
    }
}
