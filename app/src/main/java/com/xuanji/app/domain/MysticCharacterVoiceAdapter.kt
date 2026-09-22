package com.xuanji.app.domain

/**
 * The catalog describes who a guest is; this adapter makes that identity part of
 * the answer contract. It may add framing and wording, but it must not add a
 * score, chart fact, diagnosis, financial conclusion, or invented source.
 */
data class MysticSpecialtySelection(
    val characterId: MysticCharacterId,
    val specialtyKey: String,
    val specialtyLabel: String,
    val sourceLabel: String,
    val availability: MysticCharacterAvailability
)

data class MysticCharacterVoiceResult(
    val text: String,
    val selection: MysticSpecialtySelection
)

object MysticCharacterVoiceAdapter {

    fun selectSpecialty(
        characterId: MysticCharacterId,
        intent: MysticIntent,
        topicKey: String
    ): MysticSpecialtySelection {
        val profile = MysticCharacterCatalog.byId(characterId)
        val preferred = when (characterId) {
            MysticCharacterId.ShenYanzhou -> when {
                intent == MysticIntent.Game -> setOf("poetry")
                intent == MysticIntent.LifeProfile || intent == MysticIntent.TodayMeal ||
                    intent == MysticIntent.TodayActivity || intent == MysticIntent.TodayOuting ||
                    intent == MysticIntent.Action -> setOf("poetry", "bazi")
                topicKey == "ziwei" -> setOf("ziwei")
                else -> setOf("bazi", "ziwei")
            }
            MysticCharacterId.MoHeng -> when {
                intent == MysticIntent.Game -> setOf("xiangqi", "memory")
                intent == MysticIntent.Why -> setOf("composite")
                else -> setOf("composite", "memory", "oracle")
            }
            MysticCharacterId.EvelynNova -> when {
                intent == MysticIntent.Game -> setOf("star_map")
                intent == MysticIntent.Tarot -> setOf("tarot")
                intent == MysticIntent.Numerology -> setOf("numerology")
                intent == MysticIntent.Personality -> setOf("psychology")
                else -> setOf("western_astrology", "tarot", "numerology")
            }
            MysticCharacterId.NadirRashid -> when {
                intent == MysticIntent.Game -> setOf("silkroad_route")
                intent == MysticIntent.Weather || intent == MysticIntent.Place ||
                    intent == MysticIntent.TodayOuting -> setOf("place")
                topicKey.contains("arabic") -> setOf("arabic_astrology")
                topicKey.contains("persian") -> setOf("persian_astrology")
                else -> setOf("babylonian_astrology", "arabic_astrology", "place")
            }
        }
        val ranked = profile.specialties.withIndex().sortedWith(
            compareBy<IndexedValue<MysticCharacterSpecialty>> {
                val index = preferred.indexOf(it.value.key)
                if (index < 0) Int.MAX_VALUE else index
            }.thenBy { it.index }
        )
        val chosen = ranked.firstOrNull()?.value
            ?: MysticCharacterSpecialty(
                key = "general",
                label = "综合入口",
                systemIds = listOf("composite"),
                sourceLabel = "现有盘面计算",
                ritual = "先核对已有结果"
            )
        return MysticSpecialtySelection(
            characterId = characterId,
            specialtyKey = chosen.key,
            specialtyLabel = chosen.label,
            sourceLabel = chosen.sourceLabel,
            availability = chosen.availability
        )
    }

    fun adapt(
        context: DialogueContext,
        analysis: DialogueAnalysis,
        draft: String
    ): MysticCharacterVoiceResult {
        val characterId = context.characterId ?: characterIdFor(context.characterName)
        val selected = selectSpecialty(characterId, analysis.intent, analysis.topicKey ?: context.topicKey)
        val selection = context.characterSpecialtyKey
            ?.let { key ->
                val specialty = MysticCharacterCatalog.byId(characterId).specialties.firstOrNull { it.key == key }
                specialty?.let {
                    MysticSpecialtySelection(characterId, it.key, it.label, it.sourceLabel, it.availability)
                }
            }
            ?: selected
        val profile = MysticCharacterCatalog.byId(characterId)
        val framing = framing(profile, selection, analysis.intent, context.question)
        val text = when {
            draft.isBlank() -> framing
            framing.isBlank() -> draft
            else -> "$framing\n$draft"
        }
        return MysticCharacterVoiceResult(text = text, selection = selection)
    }

    private fun framing(
        profile: MysticCharacterProfile,
        selection: MysticSpecialtySelection,
        intent: MysticIntent,
        input: String
    ): String {
        val variant = stableVariant("${profile.id.key}|${selection.specialtyKey}|$intent|$input")
        return when (profile.id) {
            MysticCharacterId.ShenYanzhou -> when (intent) {
                MysticIntent.Greeting -> if (variant == 0) "沈砚舟把折扇放在案边：你好，先坐一会儿。" else "沈砚舟在水榭边向你点头：你好，今天想从哪一页看起？"
                MysticIntent.Thanks -> "沈砚舟合扇一笑：不必客气，愿这一步对你真有用。"
                MysticIntent.Farewell -> "沈砚舟收好案上的纸页：慢走，想清楚的一小步随时可以回来续上。"
                MysticIntent.Smalltalk -> "沈砚舟给茶盏添了热水：闲聊也算歇脚，不必急着把每句话变成答案。"
                MysticIntent.Identity -> "我是沈砚舟，负责把盘面翻成能落地的文字；算法和边界仍写在依据里。"
                MysticIntent.Game -> "沈砚舟铺开素笺：来做一局诗句接龙，按韵脚和结构慢慢对。"
                else -> "我先从${selection.specialtyLabel}这一页看，${selection.sourceLabel}只作依据，不替你下定论。"
            }
            MysticCharacterId.MoHeng -> when (intent) {
                MysticIntent.Greeting -> "墨衡轻敲杖尖：坐，先把问题和证据摆齐。"
                MysticIntent.Thanks -> "墨衡点了点头：记住真正有用的那一小步就够了。"
                MysticIntent.Farewell -> "墨衡合上批注：去做能验证的那一步，回来再看下一手。"
                MysticIntent.Smalltalk -> "墨衡把茶推近：闲谈也可以记一笔，但我不会替你编长期记忆。"
                MysticIntent.Identity -> "我是墨衡，负责把综合盘面、现场手记和每一步选择分开写清楚。"
                MysticIntent.Game -> "墨衡摆好棋盘：来一盘中国象棋，规则和棋谱会逐手留在棋局里。"
                else -> "墨衡先批注${selection.specialtyLabel}：${selection.sourceLabel}提供线索，分歧也会原样保留。"
            }
            MysticCharacterId.EvelynNova -> when (intent) {
                MysticIntent.Greeting -> "伊芙琳·诺瓦打开观测册：你好。先定义问题，再决定看哪一张图。"
                MysticIntent.Thanks -> "伊芙琳合上记录夹：不客气，能复核的步骤比漂亮的结论更重要。"
                MysticIntent.Farewell -> "伊芙琳收起星图：下次带着新的观测回来，我们继续核对。"
                MysticIntent.Smalltalk -> "伊芙琳抬眼：可以闲聊，但我不会把聊天自动变成固定性格结论。"
                MysticIntent.Identity -> "我是伊芙琳·诺瓦，负责把星图、牌阵和数字步骤分层展示。"
                MysticIntent.Game -> "伊芙琳调亮星图：来做一题观测逻辑挑战，答案属于游戏，不是占星结论。"
                else -> "伊芙琳先看${selection.specialtyLabel}：${selection.sourceLabel}是入口，解释与计算会分开标注。"
            }
            MysticCharacterId.NadirRashid -> when (intent) {
                MysticIntent.Greeting -> "纳迪尔·拉希德在驿站灯下抬头：你好，先确认你现在站在哪一站。"
                MysticIntent.Thanks -> "纳迪尔收起铜盘：不必客气，路线总要分成几段走。"
                MysticIntent.Farewell -> "纳迪尔系紧行囊：一路慢行，下一站仍由你自己选择。"
                MysticIntent.Smalltalk -> "纳迪尔把灯芯拨亮：可以聊两句，天气和地点若要实时数据仍需你主动授权。"
                MysticIntent.Identity -> "我是纳迪尔·拉希德，负责把地域、旅途和文化参考的出处标在地图边上。"
                MysticIntent.Game -> "纳迪尔展开丝路图：来做一局路线规划，预算和时间会决定下一站。"
                else -> "纳迪尔先标出${selection.specialtyLabel}：${selection.sourceLabel}属于${if (selection.availability == MysticCharacterAvailability.CulturalReference) "文化参考" else "现有入口"}，不冒充现实保证。"
            }
        }
    }

    private fun characterIdFor(name: String?): MysticCharacterId =
        MysticCharacterCatalog.all.firstOrNull { it.displayName == name }?.id
            ?: MysticCharacterId.ShenYanzhou

    private fun stableVariant(value: String): Int {
        var hash = 5381L
        value.forEach { hash = (hash * 33L + it.code) % 2_147_483_647L }
        return (hash and Long.MAX_VALUE).rem(2L).toInt()
    }
}
