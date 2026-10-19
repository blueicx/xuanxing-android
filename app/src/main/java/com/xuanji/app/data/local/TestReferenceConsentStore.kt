package com.xuanji.app.data.local

import com.xuanji.app.data.model.TestRecord
import com.xuanji.app.domain.action.StableScore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Only a specific user-selected result may be used; new results never inherit consent. */
class TestReferenceConsentStore(private val bridge: PreferenceBridge) {
    private fun key(owner: String): String {
        require(owner.isNotBlank())
        return "test_reference_consent_" + StableScore.sha256(owner)
    }
    private fun fingerprint(record: TestRecord): String = StableScore.sha256(
        listOf(record.testName, record.category, record.resultCode, record.resultName, record.date,
            record.scoringVersion.orEmpty(), record.rawScores.orEmpty().toSortedMap().entries.joinToString { "${it.key}:${it.value}" })
            .joinToString("") { "${it.length}:$it" })
    suspend fun authorized(owner: String, records: List<TestRecord>): TestRecord? {
        val consent = bridge.read(key(owner)) ?: return null
        return records.firstOrNull { "v1:" + fingerprint(it) == consent }
    }
    suspend fun grant(owner: String, record: TestRecord) {
        bridge.write(key(owner), "v1:" + fingerprint(record)); changes.value++
    }
    suspend fun revoke(owner: String) { bridge.delete(key(owner)); changes.value++ }
    companion object {
        private val changes = MutableStateFlow(0L)
        val revision = changes.asStateFlow()
    }
}
