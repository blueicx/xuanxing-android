package com.xuanji.app.domain

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.text.Normalizer

/**
 * 一句话是怎么上记录的。角色的文案没有对应的 kind —— 红线因此在类型上就成立，
 * 而不是靠调用方自觉。
 */
enum class RecollectionKind(val wire: String) {
    /** 用户自己打出来的原话。 */
    USER_INPUT("user_input"),

    /** 用户在卡面上主动选中的选项。 */
    USER_CHOICE("user_choice"),

    /** 棋盘规则已经判定结束的棋局结果。 */
    SETTLED_GAME_RESULT("settled_game_result");

    companion object {
        fun fromWire(value: String): RecollectionKind? = values().firstOrNull { it.wire == value }
    }
}

/** 一条长期记录：[intent] 只存话题键（如 career），不存生成出来的句子。 */
data class RecollectionEntry(
    val dateKey: String = "",
    val kind: RecollectionKind,
    val text: String,
    val intent: String = ""
)

data class ConversationMemory(
    val version: Int = RecollectionCodec.VERSION,
    val entries: List<RecollectionEntry> = emptyList(),
    /** 本机已经不再持有的记录数：超出上限被挤掉的，以及读不出来的。 */
    val dropped: Int = 0
) {
    val isEmpty: Boolean get() = entries.isEmpty() && dropped == 0
}

/**
 * 文案唯一的输入：只有能从记录里数出来的事实，没有任何推断。
 * [unreadable] 与 [dropped] 分开，因为「这次什么都没读到」和「更早的记录本机已清理」是两件事。
 */
data class RecallFacts(
    val dates: List<String> = emptyList(),
    val userTopics: List<String> = emptyList(),
    val results: List<String> = emptyList(),
    val dropped: Int = 0,
    val unreadable: Boolean = false
) {
    /** 空 = 无话可说；读不出来不是空，那件事必须交代。 */
    val isEmpty: Boolean
        get() = !unreadable && dates.isEmpty() && userTopics.isEmpty() && results.isEmpty() && dropped == 0
    val lastDate: String get() = dates.lastOrNull().orEmpty()
}

object RecollectionCodec {

    const val VERSION = 1
    const val MAX_ENTRIES = 20
    const val MAX_TEXT_CODEPOINTS = 40
    const val MAX_INTENT_CODEPOINTS = 24

    private const val ELLIPSIS = "…"

    private val INVISIBLE = Regex("[\\u0000-\\u0008\\u000B-\\u001F\\u007F-\\u009F\\u200B-\\u200F\\uFEFF]")
    private val SPACES = Regex("[\\s\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000]+")
    private val DATE_KEY = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    private val PAST_CUES = listOf(
        "上次", "上回", "上一次", "之前", "以前", "此前", "昨天", "前天", "那天", "早前", "早先", "早些"
    )
    private val SPEECH_CUES = listOf("聊", "唠", "说", "讲", "问", "提", "谈", "记", "答", "话")
    private val INTERROGATIVES = listOf("什么", "啥", "来着", "哪些", "哪句", "哪回", "吗", "呢")

    /** NFC、去不可见字符、压空白、按码点截断——只做裁剪，不做改写。 */
    fun cleanText(value: String, maxCodePoints: Int = MAX_TEXT_CODEPOINTS): String {
        var text = Normalizer.normalize(value, Normalizer.Form.NFC)
        text = text.replace(INVISIBLE, "")
        text = text.replace(SPACES, " ").trim()
        if (text.codePointCount(0, text.length) > maxCodePoints) {
            text = text.substring(0, text.offsetByCodePoints(0, maxCodePoints)) + ELLIPSIS
        }
        return text
    }

    fun cleanIntent(value: String): String = cleanText(value, MAX_INTENT_CODEPOINTS).lowercase()

    /** 日期键只接受 ISO 形状，其余一律留空：没有日期就宁可不说日期。 */
    fun cleanDateKey(value: String): String {
        val text = cleanText(value, 10)
        return if (DATE_KEY.matches(text)) text else ""
    }

    /** 用户没有真的说过话时返回 null，而不是返回一条空记录。 */
    fun entryOf(dateKey: String, kind: RecollectionKind, text: String, intent: String = ""): RecollectionEntry? {
        val cleaned = cleanText(text)
        if (cleaned.isEmpty()) return null
        return RecollectionEntry(cleanDateKey(dateKey), kind, cleaned, cleanIntent(intent))
    }

    /** 追加一条：同话题同一天同原话不会重复入库，溢出只挤掉最旧的一条并计数。 */
    fun append(memory: ConversationMemory, entry: RecollectionEntry): ConversationMemory {
        val normalized = entryOf(entry.dateKey, entry.kind, entry.text, entry.intent) ?: return memory
        val kept = memory.entries.filterNot {
            it.kind == normalized.kind && it.dateKey == normalized.dateKey && it.text == normalized.text
        }
        val evicted = (kept.size + 1 - MAX_ENTRIES).coerceAtLeast(0)
        return ConversationMemory(
            version = VERSION,
            entries = (kept + normalized).takeLast(MAX_ENTRIES),
            dropped = (memory.dropped + evicted).coerceAtLeast(0)
        )
    }

    /** 去重不记损失（没有东西被丢掉），挤上限才记损失。 */
    fun reduced(entries: List<RecollectionEntry>, dropped: Int = 0): ConversationMemory {
        val deduped = ArrayList<RecollectionEntry>(entries.size)
        val seen = HashSet<String>()
        for (entry in entries.asReversed()) {
            if (seen.add("${entry.dateKey}|${entry.kind.wire}|${entry.text}")) deduped += entry
        }
        deduped.reverse()
        val evicted = (deduped.size - MAX_ENTRIES).coerceAtLeast(0)
        return ConversationMemory(
            version = VERSION,
            entries = deduped.takeLast(MAX_ENTRIES),
            dropped = (dropped + evicted).coerceAtLeast(0)
        )
    }

    fun encode(memory: ConversationMemory): String {
        val root = JsonObject()
        root.addProperty("version", VERSION)
        root.addProperty("dropped", memory.dropped)
        val entries = JsonArray()
        for (entry in memory.entries) {
            val item = JsonObject()
            item.addProperty("dateKey", entry.dateKey)
            item.addProperty("kind", entry.kind.wire)
            item.addProperty("text", entry.text)
            item.addProperty("intent", entry.intent)
            entries.add(item)
        }
        root.add("entries", entries)
        return root.toString()
    }

    /** 整块读不出来返回 null；单条读不出来计进 dropped。 */
    fun decode(json: String?): ConversationMemory? {
        if (json == null || json.isBlank()) return null
        val root = runCatching { JsonParser.parseString(json) }.getOrNull()
        if (root == null || !root.isJsonObject) return null
        val obj = root.asJsonObject
        if (obj.intMember("version", VERSION) > VERSION) return null
        val array = obj.get("entries")
        if (array == null || !array.isJsonArray) return null
        var dropped = obj.intMember("dropped", 0)
        val kept = ArrayList<RecollectionEntry>()
        for (element in array.asJsonArray) {
            if (!element.isJsonObject) {
                dropped += 1
                continue
            }
            val item = element.asJsonObject
            val kind = RecollectionKind.fromWire(item.stringMember("kind"))
            val text = cleanText(item.stringMember("text"))
            if (kind == null || text.isEmpty()) {
                dropped += 1
                continue
            }
            kept += RecollectionEntry(
                dateKey = cleanDateKey(item.stringMember("dateKey")),
                kind = kind,
                text = text,
                intent = cleanIntent(item.stringMember("intent"))
            )
        }
        return reduced(kept, dropped)
    }

    fun facts(memory: ConversationMemory): RecallFacts {
        val dates = LinkedHashSet<String>()
        val topics = LinkedHashSet<String>()
        val results = LinkedHashSet<String>()
        for (entry in memory.entries) {
            if (entry.dateKey.isNotEmpty()) dates += entry.dateKey
            if (entry.kind != RecollectionKind.SETTLED_GAME_RESULT && entry.intent.isNotEmpty()) {
                topics += entry.intent
            }
            if (entry.kind == RecollectionKind.SETTLED_GAME_RESULT) results += entry.text
        }
        return RecallFacts(
            dates = dates.toList(),
            userTopics = topics.toList(),
            results = results.toList(),
            dropped = memory.dropped
        )
    }

    /** 没存过是空的，存了但读坏了才是不可用——两种情况文案不同。 */
    fun factsOf(json: String?): RecallFacts {
        if (json == null || json.isBlank()) return RecallFacts()
        val memory = decode(json) ?: return RecallFacts(unreadable = true)
        return facts(memory)
    }

    /** 「上次聊过什么」这类召回问句；陈述自己说过什么不算。 */
    fun rememberOf(text: String): Boolean {
        val input = cleanText(text)
        if (input.isEmpty()) return false
        val asksContent = SPEECH_CUES.any { input.contains(it) } &&
            INTERROGATIVES.any { input.contains(it) }
        if (!asksContent) return false
        return PAST_CUES.any { input.contains(it) } || input.contains("我")
    }

    private fun JsonObject.stringMember(name: String): String = runCatching {
        val element: JsonElement? = get(name)
        if (element != null && element.isJsonPrimitive) element.asString.orEmpty() else ""
    }.getOrDefault("")

    private fun JsonObject.intMember(name: String, fallback: Int): Int = runCatching {
        val element: JsonElement? = get(name)
        if (element != null && element.isJsonPrimitive) element.asInt else fallback
    }.getOrNull()?.coerceAtLeast(0) ?: fallback
}
