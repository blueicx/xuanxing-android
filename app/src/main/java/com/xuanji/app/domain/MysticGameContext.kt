package com.xuanji.app.domain

import com.xuanji.app.domain.game.CompanionGameCatalog
import com.xuanji.app.domain.game.CompanionGameProgressState
import com.xuanji.app.domain.game.GameArchive
import com.xuanji.app.domain.game.GameOutcome
import com.xuanji.app.domain.game.GameSave
import com.xuanji.app.domain.game.PlayerColor

/** A short, per-turn checkpoint; it never carries the serialized board or complete game state. */
data class MysticGameContext(
    val characterId: MysticCharacterId,
    val gameId: String,
    val gameTitle: String,
    val checkpoint: String,
    val status: MysticGameProgressStatus
) {
    fun isResumableFor(requestedCharacter: MysticCharacterId): Boolean =
        characterId == requestedCharacter && status == MysticGameProgressStatus.InProgress
}

enum class MysticGameProgressStatus { InProgress, Completed, Ended }

object MysticGameContextFactory {
    fun fromSaved(
        characterId: MysticCharacterId,
        companionProgress: CompanionGameProgressState?,
        xiangqiArchive: GameSave?
    ): MysticGameContext? {
        val game = CompanionGameCatalog.forCharacter(characterId)
        if (game.id == "xiangqi") {
            val save = xiangqiArchive ?: return null
            val restored = GameArchive.restore(save, token = 1) as? com.xuanji.app.domain.game.GameRestore.Loaded
                ?: return null
            val side = if (restored.state.position.sideToMove == PlayerColor.RED) "红方" else "黑方"
            val title = save.title.takeIf(String::isNotBlank)?.take(48)?.let { "「$it」" }.orEmpty()
            val dropped = restored.dropped.takeIf { it > 0 }
                ?.let { "另有 $it 个无法复核的棋步已舍弃。" }.orEmpty()
            return MysticGameContext(
                characterId = characterId,
                gameId = game.id,
                gameTitle = game.title,
                checkpoint = "已保存$title 棋局；棋谱复核后有 ${restored.state.history.size} 个有效半回合，当前轮到$side。$dropped",
                status = when (restored.state.outcome) {
                    GameOutcome.InProgress, is GameOutcome.Check -> MysticGameProgressStatus.InProgress
                    is GameOutcome.Checkmate, is GameOutcome.Stalemate, GameOutcome.Draw,
                    is GameOutcome.IllegalPosition -> MysticGameProgressStatus.Ended
                }
            )
        }

        val state = companionProgress?.takeIf { it.gameId == game.id } ?: return null
        val (checkpoint, status) = when (state) {
            is CompanionGameProgressState.Poetry -> {
                if (characterId != MysticCharacterId.ShenYanzhou) return null
                if (!com.xuanji.app.domain.game.PoetryChainEngine.isValidState(state.value)) return null
                val lastChoice = state.value.selected?.take(24)?.let { "；最近选择：$it" }.orEmpty()
                "第 ${state.value.promptIndex + 1} 题；当前得分 ${state.value.score}$lastChoice" to
                    if (state.value.completed) MysticGameProgressStatus.Completed else MysticGameProgressStatus.InProgress
            }
            is CompanionGameProgressState.StarMap -> {
                if (characterId != MysticCharacterId.EvelynNova) return null
                if (!com.xuanji.app.domain.game.StarMapPuzzleEngine.isValidState(state.value)) return null
                val target = com.xuanji.app.domain.game.StarMapPuzzleEngine.puzzle(state.value).target.take(20)
                val path = state.value.path.take(10).joinToString(" → ") { it.take(20) }.ifBlank { "暂无路径" }
                "第 ${state.value.puzzleIndex + 1} 题；路径 $path；目标 $target" to
                    if (state.value.completed) MysticGameProgressStatus.Completed else MysticGameProgressStatus.InProgress
            }
            is CompanionGameProgressState.SilkRoad -> {
                if (characterId != MysticCharacterId.NadirRashid) return null
                val value = state.value
                if (!com.xuanji.app.domain.game.SilkRoadRouteEngine.isValidState(value)) return null
                val path = value.path.take(10).joinToString(" → ") { it.take(20) }.ifBlank { value.current.take(20) }
                val choices = com.xuanji.app.domain.game.SilkRoadRouteEngine.destinations(value)
                    .sortedBy { it.id }
                    .joinToString("；") { city ->
                        "${city.label}（${city.days}天·${city.cost}资源）" +
                            if (value.daysLeft >= city.days && value.budgetLeft >= city.cost) "可达" else "资源不足"
                    }.ifBlank { "暂无相邻驿站" }
                "当前在 ${value.current.take(20)}，目标 ${value.target.take(20)}；剩余 ${value.daysLeft.coerceIn(0, 365)} 天、${value.budgetLeft.coerceIn(0, 1000)} 点资源；路线 $path；可选下一站与消耗：$choices" to
                    when {
                        value.completed -> MysticGameProgressStatus.Completed
                        value.failed -> MysticGameProgressStatus.Ended
                        else -> MysticGameProgressStatus.InProgress
                    }
            }
        }
        return MysticGameContext(characterId, game.id, game.title, checkpoint.take(240), status)
    }
}

object MysticGameProgressQuestion {
    private val progressPhrases = listOf(
        "游戏进度", "游戏到哪", "游戏到哪了", "游戏存档", "玩到哪", "玩到哪里", "上次玩到",
        "棋局进度", "棋谱进度", "还剩几天", "还剩多少天", "剩余几天", "多少补给", "剩余资源",
        "还剩多少资源", "下一站怎么选", "路线走到哪", "当前这一局怎么样"
    )
    private val resumePhrases = listOf("继续游戏", "继续路线", "继续丝路", "继续诗句", "继续接龙", "继续星图", "继续观测")

    fun isProgressQuestion(input: String): Boolean {
        val normalized = input.trim().lowercase()
        return progressPhrases.any(normalized::contains)
    }

    fun isResumeRequest(input: String): Boolean {
        val normalized = input.trim().lowercase()
        return resumePhrases.any(normalized::contains)
    }
}

object MysticGameContextAnswer {
    fun reply(characterId: MysticCharacterId, gameContext: MysticGameContext?, input: String): String {
        val currentGame = CompanionGameCatalog.forCharacter(characterId)
        val saved = gameContext?.takeIf { it.characterId == characterId && it.gameId == currentGame.id }
            ?: return if (MysticGameProgressQuestion.isResumeRequest(input)) {
                "我没有读到当前角色有效的${currentGame.title}存档，所以不会替你新开一局。你可以从角色舞台明确选择开始新局。"
            } else {
                "我这里没有读到当前角色的有效${currentGame.title}存档，所以不猜进度。可以先从角色舞台进入${currentGame.title}；存档损坏时需要在游戏内重新开始。"
            }

        if (MysticGameProgressQuestion.isResumeRequest(input)) {
            if (!saved.isResumableFor(characterId)) {
                val ending = if (saved.status == MysticGameProgressStatus.Completed) "已完成" else "已结束"
                return "本机存档显示这一局$ending：${saved.checkpoint}如想再玩，请在游戏卡中主动选择重新开始。"
            }
            return "我读到本机${saved.gameTitle}存档：${saved.checkpoint}回到${saved.gameTitle}即可接着这份存档继续；这里只报本机进度，不作运势判断。"
        }

        val resourceQuestion = listOf("几天", "多少天", "补给", "资源").any(input::contains)
        if (resourceQuestion && saved.gameId != "silkroad_route") {
            return "当前存档是${saved.gameTitle}，没有行程天数或补给数；${saved.checkpoint}"
        }
        val introduction = when (characterId) {
            MysticCharacterId.ShenYanzhou -> "沈砚舟翻到诗笺上的存档："
            MysticCharacterId.MoHeng -> "墨衡对照本机棋谱："
            MysticCharacterId.EvelynNova -> "伊芙琳打开观测记录夹："
            MysticCharacterId.NadirRashid -> "纳迪尔摊开保存的丝路行程："
        }
        return "$introduction${saved.checkpoint}（${statusLabel(saved.status)}）。这里只复述本机游戏进度，不是运势结论。"
    }

    private fun statusLabel(status: MysticGameProgressStatus): String = when (status) {
        MysticGameProgressStatus.InProgress -> "进行中"
        MysticGameProgressStatus.Completed -> "已完成"
        MysticGameProgressStatus.Ended -> "已结束"
    }
}
