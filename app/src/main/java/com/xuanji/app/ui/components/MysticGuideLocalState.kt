package com.xuanji.app.ui.components
import androidx.compose.runtime.mutableIntStateOf

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.xuanji.app.data.local.dataStore
import com.xuanji.app.domain.MysticCharacterId
import com.xuanji.app.domain.MysticGuestCameo
import com.xuanji.app.domain.MysticGuestExit
import com.xuanji.app.domain.MysticMessage
import com.xuanji.app.domain.MysticMessageRole
import com.xuanji.app.domain.MysticSessionState
import com.xuanji.app.domain.MysticVisitMemory
import com.xuanji.app.domain.RecollectionKind
import com.xuanji.app.domain.MysticMemoryNote as DomainMysticMemoryNote
import kotlinx.coroutines.flow.first
import java.security.MessageDigest

internal data class MysticTurn(
    val key: String,
    val question: String,
    val answer: String,
    val reaction: String = "",
    val kind: String = "ask",
    val aside: MysticGuestCameo? = null,
    val asidePrompt: String = "",
    val asideChoice: String = "",
    val asideReply: String = "",
    val asideExit: MysticGuestExit? = null,
    val asideWrapup: String = ""
)

internal data class MysticMemoryNote(
    val id: String,
    val text: String
)

/** Rehydrates the rich transcript from the persisted, provider-agnostic message log. */
internal fun restoreCharacterThread(messages: List<MysticMessage>): List<MysticTurn> {
    val restored = mutableListOf<MysticTurn>()
    var pendingUser: MysticMessage? = null
    messages.takeLast(20).forEach { message ->
        when (message.role) {
            MysticMessageRole.User -> {
                if (pendingUser != null) {
                    restored += MysticTurn(
                        key = "restored-user-${pendingUser!!.turnId}",
                        question = pendingUser!!.text,
                        answer = "这句已记下；上一轮回复没有完整保存。",
                        kind = "restored"
                    )
                }
                pendingUser = message
            }
            MysticMessageRole.Mystic -> {
                val user = pendingUser
                restored += MysticTurn(
                    key = "restored-${message.turnId}",
                    question = user?.text ?: "角色回复",
                    answer = message.text,
                    kind = "restored"
                )
                pendingUser = null
            }
            MysticMessageRole.System -> {
                restored += MysticTurn(
                    key = "restored-system-${message.turnId}",
                    question = "角色交接",
                    answer = message.text,
                    kind = "handoff"
                )
            }
        }
    }
    pendingUser?.let { user ->
        restored += MysticTurn(
            key = "restored-user-${user.turnId}",
            question = user.text,
            answer = "这句已记下；回复仍可重新提问。",
            kind = "restored"
        )
    }
    return restored.takeLast(5)
}
/** 记忆列表里那条是哪个动作；穷举写，新增种类时编译期就得补。 */
internal fun recollectionKindLabel(kind: RecollectionKind): String = when (kind) {
    RecollectionKind.USER_INPUT -> "原话"
    RecollectionKind.USER_CHOICE -> "选择"
    RecollectionKind.SETTLED_GAME_RESULT -> "终局"
}

private data class StoredMysticVisit(
    val dateKey: String = "",
    val topicKey: String = "",
    val action: String = ""
)

internal class MysticVisitStore(private val context: android.content.Context) {
    private val gson = Gson()

    suspend fun read(profileKey: String): MysticVisitMemory {
        val preferences = context.dataStore.data.first()
        val stored = preferences[stringPreferencesKey("mystic_visit_${fingerprint(profileKey)}")]
            ?.let { json ->
                runCatching { gson.fromJson(json, StoredMysticVisit::class.java) }.getOrNull()
            }
        return MysticVisitMemory(
            lastDateKey = stored?.dateKey.orEmpty(),
            lastTopicKey = stored?.topicKey.orEmpty(),
            lastAction = stored?.action.orEmpty()
        )
    }

    suspend fun save(profileKey: String, memory: MysticVisitMemory) {
        val key = stringPreferencesKey("mystic_visit_${fingerprint(profileKey)}")
        context.dataStore.edit { preferences ->
            preferences[key] = gson.toJson(
                StoredMysticVisit(
                    dateKey = canonicalDate(memory.lastDateKey),
                    topicKey = memory.lastTopicKey,
                    action = memory.lastAction
                )
            )
        }
    }

    private fun fingerprint(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun canonicalDate(value: String): String {
        val parts = value.split("-")
        if (parts.size != 3) return value
        val year = parts[0].toIntOrNull() ?: return value
        val month = parts[1].toIntOrNull() ?: return value
        val day = parts[2].toIntOrNull() ?: return value
        return "$year-$month-$day"
    }
}

internal class MysticCompanionState(initialMode: String, initialTopic: String) {
    var mode by mutableStateOf(initialMode)
    var topic by mutableStateOf(initialTopic)
    var interactionCarryoverOption by mutableStateOf<String?>(null)
    var pendingHandoff by mutableStateOf<String?>(null)
    var pendingHandoffEcho by mutableStateOf<String?>(null)
    var rhythmAnswered by mutableStateOf(false)
    var completedRhythmKey by mutableStateOf<String?>(null)
    var guestCameo by mutableStateOf<MysticGuestCameo?>(null)
    var guestExit by mutableStateOf<MysticGuestExit?>(null)
    var selectedGuestChoice by mutableStateOf("")
    var guestReply by mutableStateOf("")
    var guestQuestion by mutableStateOf("")
    var pendingGuest by mutableStateOf(false)
    var guestChoiceCarryoverKey by mutableStateOf<String?>(null)
    var pendingGuestChoiceEcho by mutableStateOf<String?>(null)
    var memoryNotes by mutableStateOf(emptyList<MysticMemoryNote>())
    /** User-authored notes are shared across character threads; generated replies are not. */
    var sharedMemoryNotes by mutableStateOf(emptyList<DomainMysticMemoryNote>())
    var memorySequence by mutableIntStateOf(0)
    var memoryExpanded by mutableStateOf(false)
    var skinId by mutableStateOf("")
    var activeCharacterId by mutableStateOf<MysticCharacterId?>(null)
    val characterThreads = mutableMapOf<MysticCharacterId, SnapshotStateList<MysticTurn>>()
    val characterSessionStates = mutableStateMapOf<MysticCharacterId, MysticSessionState>()

    fun threadFor(characterId: MysticCharacterId): SnapshotStateList<MysticTurn> =
        characterThreads.getOrPut(characterId) { mutableStateListOf() }

    fun sessionFor(characterId: MysticCharacterId): MysticSessionState =
        characterSessionStates[characterId] ?: MysticSessionState()
}
