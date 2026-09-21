package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune

/** Stable identifiers for the four user-confirmed companion figures. */
enum class MysticCharacterId(val key: String) {
    ShenYanzhou("shen_yanzhou"),
    MoHeng("mo_heng"),
    EvelynNova("evelyn_nova"),
    NadirRashid("nadir_rashid")
}

enum class MysticCharacterAvailability {
    Available,
    CulturalReference,
    NotEnabled
}

data class MysticCharacterSpecialty(
    val key: String,
    val label: String,
    val systemIds: List<String>,
    val sourceLabel: String,
    val ritual: String,
    val availability: MysticCharacterAvailability = MysticCharacterAvailability.Available
)

data class MysticCharacterProfile(
    val id: MysticCharacterId,
    val displayName: String,
    val title: String,
    val cultureLabel: String,
    val visualStyleId: String,
    val sceneId: String,
    val voiceProfile: String,
    val dialogueMode: String,
    val legacySkinId: String,
    val intro: String,
    val handoffLine: String,
    val specialties: List<MysticCharacterSpecialty>,
    val gameTheme: String
)

/**
 * Character identity is deliberately separate from the old clothing/voice skins.
 * The catalog is a curation layer: calculators remain the source of truth and a
 * specialty never claims cultural or professional authority by itself.
 */
object MysticCharacterCatalog {
    val all: List<MysticCharacterProfile> = listOf(
        MysticCharacterProfile(
            id = MysticCharacterId.ShenYanzhou,
            displayName = "沈砚舟",
            title = "江南书生",
            cultureLabel = "江南水乡 · 笔墨与诗意",
            visualStyleId = "jiangnan_scholar",
            sceneId = "jiangnan_triptych",
            voiceProfile = "jiangnan_gentle",
            dialogueMode = "scholar",
            legacySkinId = "jiangnan-robe",
            intro = "我把盘面摊在水榭边，先替你看清纹理，再谈下一步。",
            handoffLine = "沈砚舟把折扇合上：这段路，我先替你记在册上。",
            specialties = listOf(
                MysticCharacterSpecialty(
                    key = "bazi",
                    label = "八字与五行",
                    systemIds = listOf("bazi", "composite"),
                    sourceLabel = "东方命理计算",
                    ritual = "以一行五行摘要开场，再给出可执行的小步行动。"
                ),
                MysticCharacterSpecialty(
                    key = "ziwei",
                    label = "紫微斗数",
                    systemIds = listOf("ziwei"),
                    sourceLabel = "紫微排盘页",
                    ritual = "翻一页宫位札记，只挑与你当前问题有关的宫位。"
                ),
                MysticCharacterSpecialty(
                    key = "poetry",
                    label = "诗歌与生活",
                    systemIds = listOf("same_day_works", "action"),
                    sourceLabel = "同日生作品与行动建议",
                    ritual = "先给一行诗意旁白，再落到今天能做的具体选择。",
                    availability = MysticCharacterAvailability.CulturalReference
                )
            ),
            gameTheme = "jiangnan_wood"
        ),
        MysticCharacterProfile(
            id = MysticCharacterId.MoHeng,
            displayName = "墨衡",
            title = "灰发水墨老法师",
            cultureLabel = "水墨案头 · 记忆与残局",
            visualStyleId = "elder_ink",
            sceneId = "ink_elder",
            voiceProfile = "ink_steady",
            dialogueMode = "scholar",
            legacySkinId = "northland-mantle",
            intro = "别急着求一个响亮答案。把证据、落子和你真正想守住的东西摆上来。",
            handoffLine = "墨衡轻敲杖尖：你刚才留下的那句话，我替你接着看。",
            specialties = listOf(
                MysticCharacterSpecialty(
                    key = "composite",
                    label = "综合合参",
                    systemIds = listOf("composite", "bazi", "western"),
                    sourceLabel = "东方与西方合参结论",
                    ritual = "先列共同信号，再把分歧写在纸边，不把两套体系揉成假共识。"
                ),
                MysticCharacterSpecialty(
                    key = "oracle",
                    label = "签文与批注",
                    systemIds = listOf("today_oracle", "divination"),
                    sourceLabel = "离线签库文化参考",
                    ritual = "抽到的只是一条象征线索，最后仍回到你的现实选择。",
                    availability = MysticCharacterAvailability.CulturalReference
                ),
                MysticCharacterSpecialty(
                    key = "memory",
                    label = "现场手记",
                    systemIds = listOf("conversation_memory"),
                    sourceLabel = "用户主动记忆",
                    ritual = "只记录你主动说过或选择过的内容，随时可以撤回。"
                )
            ),
            gameTheme = "ink_paper"
        ),
        MysticCharacterProfile(
            id = MysticCharacterId.EvelynNova,
            displayName = "伊芙琳·诺瓦",
            title = "学院星象师",
            cultureLabel = "学院档案 · 星图与观测",
            visualStyleId = "academy_astral",
            sceneId = "academy_triptych",
            voiceProfile = "academy_clear",
            dialogueMode = "scholar",
            legacySkinId = "academy-gown",
            intro = "我们先定义问题，再看哪一条证据真的支持它。不要让漂亮的象征替代事实。",
            handoffLine = "伊芙琳把星图推回桌面：我会保留刚才的观测，换一套角度继续。",
            specialties = listOf(
                MysticCharacterSpecialty(
                    key = "western_astrology",
                    label = "西方占星",
                    systemIds = listOf("western", "classical", "hellenistic"),
                    sourceLabel = "西方占星计算页",
                    ritual = "用一条行运或星座事实开场，明确哪些是计算、哪些是解释。"
                ),
                MysticCharacterSpecialty(
                    key = "tarot",
                    label = "塔罗反思",
                    systemIds = listOf("tarot"),
                    sourceLabel = "塔罗牌阵与牌义",
                    ritual = "先说明牌阵位置，再把牌面转成一个可验证的问题。"
                ),
                MysticCharacterSpecialty(
                    key = "numerology",
                    label = "数字命理",
                    systemIds = listOf("numerology"),
                    sourceLabel = "生命数字周期",
                    ritual = "展示计算步骤，不把数字象征说成确定的命运结论。"
                ),
                MysticCharacterSpecialty(
                    key = "psychology",
                    label = "心理测验",
                    systemIds = listOf("big_five", "mbti"),
                    sourceLabel = "自我探索测验",
                    ritual = "先说明不是临床诊断，再把结果变成可讨论的倾向。",
                    availability = MysticCharacterAvailability.CulturalReference
                )
            ),
            gameTheme = "academy_star"
        ),
        MysticCharacterProfile(
            id = MysticCharacterId.NadirRashid,
            displayName = "纳迪尔·拉希德",
            title = "丝路占星师",
            cultureLabel = "丝路驿站 · 星盘与远行",
            visualStyleId = "silkroad_astrologer",
            sceneId = "silkroad_triptych",
            voiceProfile = "silkroad_pragmatic",
            dialogueMode = "scholar",
            legacySkinId = "silkroad-robe",
            intro = "路要分段走，星也要标明出处。先看你现在站在哪一站，再谈要不要上路。",
            handoffLine = "纳迪尔收起星盘：你的行李我已看见，下一站从最小的一步开始。",
            specialties = listOf(
                MysticCharacterSpecialty(
                    key = "arabic_astrology",
                    label = "阿拉伯占星",
                    systemIds = listOf("arabic"),
                    sourceLabel = "阿拉伯点与周期页",
                    ritual = "先标注来源与算法范围，再给出象征性的方向提示。"
                ),
                MysticCharacterSpecialty(
                    key = "persian_astrology",
                    label = "波斯占星",
                    systemIds = listOf("persian"),
                    sourceLabel = "波斯周期系统页",
                    ritual = "以周期和阶段作地图，不把阶段性趋势说成不可改变的命运。"
                ),
                MysticCharacterSpecialty(
                    key = "babylonian_astrology",
                    label = "巴比伦星历",
                    systemIds = listOf("babylonian"),
                    sourceLabel = "巴比伦星历文化页",
                    ritual = "把古老星象当作文化镜子，最后落回今天的路线和选择。",
                    availability = MysticCharacterAvailability.CulturalReference
                ),
                MysticCharacterSpecialty(
                    key = "place",
                    label = "地点与远行",
                    systemIds = listOf("place", "weather"),
                    sourceLabel = "地点/天气扩展入口",
                    ritual = "联网能力未启用时只给离线建议，并明确提示数据边界。"
                )
            ),
            gameTheme = "silkroad_copper"
        )
    )

    fun byId(id: MysticCharacterId): MysticCharacterProfile =
        all.first { it.id == id }

    fun recommend(topicKey: String, fortune: CompositeDailyFortune): MysticCharacterProfile =
        recommend(topicKey, fortune.dateKey, fortune.overallScore, fortune.luckyNumber)

    /** Stable daily recommendation: same topic/date/fortune always selects the same guest. */
    fun recommend(
        topicKey: String,
        dateKey: String,
        overallScore: Int,
        luckyNumber: Int
    ): MysticCharacterProfile {
        val source = "$topicKey|$dateKey|$overallScore|$luckyNumber|mystic-character-v1"
        var hash = 5381L
        source.forEach { char -> hash = (hash * 33L + char.code) % 2_147_483_647L }
        return all[(hash and Long.MAX_VALUE).rem(all.size.toLong()).toInt()]
    }
}

/** Per-character threads over a shared set of user-authored memory notes. */
data class MysticCharacterSessionState(
    val activeCharacterId: MysticCharacterId,
    val sessions: Map<MysticCharacterId, MysticSessionState>,
    val sharedMemoryNotes: List<MysticMemoryNote> = emptyList()
) {
    fun session(id: MysticCharacterId): MysticSessionState = sessions[id] ?: MysticSessionState()

    companion object {
        fun initial(activeCharacterId: MysticCharacterId): MysticCharacterSessionState =
            MysticCharacterSessionState(
                activeCharacterId = activeCharacterId,
                sessions = MysticCharacterId.entries.associateWith { MysticSessionState() }
            )
    }
}

sealed interface MysticCharacterEvent {
    data class Switch(val characterId: MysticCharacterId) : MysticCharacterEvent
    data class Session(val characterId: MysticCharacterId, val event: MysticEvent) : MysticCharacterEvent
    data class RememberShared(val note: MysticMemoryNote) : MysticCharacterEvent
}

fun reduceCharacterSession(
    state: MysticCharacterSessionState,
    event: MysticCharacterEvent
): MysticCharacterSessionState = when (event) {
    is MysticCharacterEvent.Switch -> {
        if (event.characterId == state.activeCharacterId) {
            state
        } else {
            val oldId = state.activeCharacterId
            val oldSession = state.session(oldId)
            val invalidatedOld = oldSession.invalidatePendingCharacterReply()
            val target = state.session(event.characterId)
            val profile = MysticCharacterCatalog.byId(event.characterId)
            val handoff = MysticMessage(
                turnId = target.nextTurnId,
                sessionToken = target.sessionToken,
                role = MysticMessageRole.System,
                text = profile.handoffLine
            )
            state.copy(
                activeCharacterId = event.characterId,
                sessions = state.sessions +
                    (oldId to invalidatedOld) +
                    (event.characterId to target.copy(messages = target.messages + handoff))
            )
        }
    }
    is MysticCharacterEvent.Session -> {
        val current = state.session(event.characterId)
        val updated = if (event.event is MysticEvent.Remember) {
            current
        } else {
            reduce(current, event.event)
        }
        state.copy(sessions = state.sessions + (event.characterId to updated))
    }
    is MysticCharacterEvent.RememberShared -> state.copy(
        sharedMemoryNotes = (state.sharedMemoryNotes + event.note)
            .distinctBy { it.id }
            .takeLast(12)
    )
}

private fun MysticSessionState.invalidatePendingCharacterReply(): MysticSessionState = copy(
    sessionToken = sessionToken + 1,
    pendingInput = null,
    requestState = MysticRequestState.Idle
)
