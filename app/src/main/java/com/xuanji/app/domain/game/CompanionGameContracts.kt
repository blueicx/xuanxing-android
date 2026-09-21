package com.xuanji.app.domain.game

import com.xuanji.app.domain.MysticCharacterId

enum class GameAvailability {
    Enabled,
    CulturalReference,
    NotEnabled
}

data class CompanionGameProfile(
    val id: String,
    val characterId: MysticCharacterId,
    val title: String,
    val availability: GameAvailability,
    val themeKey: String,
    val description: String,
    val accessibilityLabel: String
)

interface CompanionGameEngine<S, E, R> {
    fun reduce(state: S, event: E): S
    fun result(state: S): R
}

object CompanionGameCatalog {
    val all: List<CompanionGameProfile> = listOf(
        CompanionGameProfile(
            id = "poetry_chain",
            characterId = MysticCharacterId.ShenYanzhou,
            title = "诗句接龙",
            availability = GameAvailability.CulturalReference,
            themeKey = "jiangnan_wood",
            description = "用自有短句库做韵脚和结构练习，不冒充诗人原文。",
            accessibilityLabel = "诗句接龙，本地韵脚和结构逻辑游戏"
        ),
        CompanionGameProfile(
            id = "xiangqi",
            characterId = MysticCharacterId.MoHeng,
            title = "中国象棋",
            availability = GameAvailability.Enabled,
            themeKey = "ink_paper",
            description = "使用已验证的中国象棋规则、AI、残局和棋谱存档。",
            accessibilityLabel = "中国象棋，真实规则棋局"
        ),
        CompanionGameProfile(
            id = "star_map_observation",
            characterId = MysticCharacterId.EvelynNova,
            title = "星图观测",
            availability = GameAvailability.CulturalReference,
            themeKey = "academy_star",
            description = "观察连线和顺序的本地逻辑题，不作为占星或心理结论。",
            accessibilityLabel = "星图观测，本地连线逻辑游戏"
        ),
        CompanionGameProfile(
            id = "silkroad_route",
            characterId = MysticCharacterId.NadirRashid,
            title = "丝路路线规划",
            availability = GameAvailability.CulturalReference,
            themeKey = "silkroad_copper",
            description = "在离线城市图上取舍预算、时间和资源，不代表现实旅行安全。",
            accessibilityLabel = "丝路路线规划，离线资源取舍游戏"
        )
    )

    fun forCharacter(characterId: MysticCharacterId): CompanionGameProfile =
        all.first { it.characterId == characterId }

    fun byId(id: String): CompanionGameProfile? = all.firstOrNull { it.id == id }

    /**
     * Maps explicit dialogue shortcuts to a local game id. The wording is intentionally
     * narrow so an ordinary question about a poem, constellation or route cannot open a game.
     */
    fun gameIdForInput(input: String): String? {
        val q = input.trim().lowercase()
        return when {
            q.contains("诗句接龙") || q.contains("对仗校对") ||
                q.contains("诗歌游戏") || q.contains("玩诗句") || q == "接下一句" ->
                "poetry_chain"
            q.contains("星图观测") || q.contains("观测星图") ||
                q.contains("星图游戏") || q.contains("星座连线游戏") ->
                "star_map_observation"
            q.contains("丝路路线规划") || q.contains("路线规划游戏") ||
                q.contains("规划丝路") || q.contains("驿站路线游戏") ->
                "silkroad_route"
            q.contains("象棋") && (
                q.contains("来一盘") || q.contains("来一局") || q.contains("下一盘") ||
                    q.contains("下一局") || q.contains("开一盘") || q.contains("开一局") ||
                    q.contains("陪我下") || q.contains("开始")
                ) -> "xiangqi"
            else -> null
        }
    }
}
