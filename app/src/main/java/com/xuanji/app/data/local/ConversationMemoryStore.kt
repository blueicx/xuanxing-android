package com.xuanji.app.data.local

import com.xuanji.app.domain.ConversationMemory
import com.xuanji.app.domain.RecallFacts
import com.xuanji.app.domain.RecollectionCodec
import com.xuanji.app.domain.RecollectionEntry
import java.security.MessageDigest

/**
 * 一个命盘一份长期记忆。清除只删自己那一个键，前缀与棋局存档、战绩、当日回访记录互斥。
 */
class ConversationMemoryStore(private val bridge: PreferenceBridge) {

    /** 读不出来返回 null，没存过返回空记忆 —— 文案要分清这两件事。 */
    suspend fun load(profileKey: String): ConversationMemory? {
        val json = bridge.read(memoryKey(profileKey)) ?: return ConversationMemory()
        return RecollectionCodec.decode(json)
    }

    suspend fun facts(profileKey: String): RecallFacts =
        RecollectionCodec.factsOf(bridge.read(memoryKey(profileKey)))

    /** 追加一条用户自己的话。空白内容不会写盘，也不会顺带清掉已有记录。 */
    suspend fun remember(profileKey: String, entry: RecollectionEntry): ConversationMemory {
        val key = memoryKey(profileKey)
        val stored = bridge.read(key)
        val current = RecollectionCodec.decode(stored) ?: ConversationMemory()
        val next = RecollectionCodec.append(current, entry)
        if (next == current) return current
        // 读不出的旧记录会在下一次真的写下内容时被替换；在那之前它必须被报成「本机记录不可用」
        bridge.write(key, RecollectionCodec.encode(next))
        return next
    }

    suspend fun clear(profileKey: String) {
        bridge.delete(memoryKey(profileKey))
    }

    fun memoryKey(profileKey: String): String = KEY_PREFIX + fingerprint(profileKey)

    companion object {
        const val KEY_PREFIX = "talk_memory_"

        fun fingerprint(value: String): String =
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
