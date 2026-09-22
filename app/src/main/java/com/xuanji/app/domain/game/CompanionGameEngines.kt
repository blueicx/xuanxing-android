package com.xuanji.app.domain.game

/** Self-owned short lines: the game checks structure/rhyme, not authorship. */
data class PoetryPrompt(
    val id: String,
    val line: String,
    val expectedRhyme: String,
    val options: List<String>
)

data class PoetryChainState(
    val promptIndex: Int = 0,
    val selected: String? = null,
    val score: Int = 0,
    val completed: Boolean = false,
    val feedback: String = ""
)

sealed interface PoetryChainEvent {
    data class Choose(val line: String) : PoetryChainEvent
    data object Restart : PoetryChainEvent
}

data class PoetryChainResult(
    val score: Int,
    val completed: Boolean,
    val feedback: String,
    val corpusLabel: String = "自有短句库"
)

object PoetryChainEngine : CompanionGameEngine<PoetryChainState, PoetryChainEvent, PoetryChainResult> {
    private val prompts = listOf(
        PoetryPrompt("p1", "小桥收晚风", "ong", listOf("灯影落江中", "星子过长空", "纸鸢入云中")),
        PoetryPrompt("p2", "一盏照书夜", "e", listOf("半窗听雨声", "轻舟向远波", "新叶带春风")),
        PoetryPrompt("p3", "竹影移窗慢", "an", listOf("茶烟入砚寒", "山色入衣宽", "灯火过长安"))
    )

    fun prompt(state: PoetryChainState): PoetryPrompt = prompts[state.promptIndex.mod(prompts.size)]

    override fun reduce(state: PoetryChainState, event: PoetryChainEvent): PoetryChainState = when (event) {
        PoetryChainEvent.Restart -> PoetryChainState()
        is PoetryChainEvent.Choose -> {
            if (state.completed) state else {
                val current = prompt(state)
                val accepted = event.line in current.options && rhymeMatches(event.line, current.expectedRhyme)
                val nextScore = state.score + if (accepted) 1 else 0
                val nextIndex = state.promptIndex + 1
                state.copy(
                    promptIndex = nextIndex,
                    selected = event.line,
                    score = nextScore,
                    completed = nextIndex >= prompts.size,
                    feedback = if (accepted) "韵脚和结构都接上了。" else "这句可以读，但没有接上本题的韵脚；再试一条。"
                )
            }
        }
    }

    override fun result(state: PoetryChainState): PoetryChainResult =
        PoetryChainResult(state.score, state.completed, state.feedback)

    private fun rhymeMatches(line: String, expected: String): Boolean = when (expected) {
        "ong" -> line.endsWith("中") || line.endsWith("空")
        "e" -> line.endsWith("声") || line.endsWith("波")
        "an" -> line.endsWith("寒") || line.endsWith("宽") || line.endsWith("安")
        else -> false
    }
}

data class StarMapPuzzle(
    val id: String,
    val start: String,
    val target: String,
    val edges: Map<String, Set<String>>
)

data class StarMapState(
    val puzzleIndex: Int = 0,
    val current: String = "",
    val path: List<String> = emptyList(),
    val completed: Boolean = false,
    val feedback: String = ""
)

sealed interface StarMapEvent {
    data class Move(val node: String) : StarMapEvent
    data object Restart : StarMapEvent
}

data class StarMapResult(
    val completed: Boolean,
    val path: List<String>,
    val feedback: String,
    val rulesLabel: String = "观测逻辑题，不是占星结论"
)

object StarMapPuzzleEngine : CompanionGameEngine<StarMapState, StarMapEvent, StarMapResult> {
    private val puzzles = listOf(
        StarMapPuzzle("s1", "织女", "天狼", mapOf("织女" to setOf("天津"), "天津" to setOf("织女", "天狼"), "天狼" to setOf("天津"))),
        StarMapPuzzle("s2", "北斗一", "北斗三", mapOf("北斗一" to setOf("北斗二"), "北斗二" to setOf("北斗一", "北斗三"), "北斗三" to setOf("北斗二")))
    )

    fun puzzle(state: StarMapState): StarMapPuzzle = puzzles[state.puzzleIndex.mod(puzzles.size)]

    override fun reduce(state: StarMapState, event: StarMapEvent): StarMapState = when (event) {
        StarMapEvent.Restart -> initial()
        is StarMapEvent.Move -> {
            val puzzle = puzzle(state)
            val origin = state.current.ifBlank { puzzle.start }
            val valid = event.node in (puzzle.edges[origin].orEmpty())
            if (!valid || state.completed) state.copy(feedback = "这颗星与当前位置没有直接连线。")
            else {
                val path = state.path.ifEmpty { listOf(puzzle.start) } + event.node
                val done = event.node == puzzle.target
                state.copy(
                    current = event.node,
                    path = path,
                    completed = done,
                    feedback = if (done) "观测路径完成。" else "连线有效，继续寻找目标。"
                )
            }
        }
    }

    fun initial(): StarMapState {
        val puzzle = puzzles.first()
        return StarMapState(current = puzzle.start, path = listOf(puzzle.start))
    }

    fun options(state: StarMapState): List<String> {
        val puzzle = puzzle(state)
        return (puzzle.edges[state.current.ifBlank { puzzle.start }].orEmpty() + puzzle.target)
            .distinct()
            .sorted()
    }

    override fun result(state: StarMapState): StarMapResult =
        StarMapResult(state.completed, state.path, state.feedback)
}

data class RouteCity(val id: String, val label: String, val days: Int, val cost: Int)

data class SilkRoadState(
    val current: String = "长安",
    val target: String = "撒马尔罕",
    val daysLeft: Int = 8,
    val budgetLeft: Int = 12,
    val path: List<String> = listOf("长安"),
    val completed: Boolean = false,
    val failed: Boolean = false,
    val feedback: String = ""
)

sealed interface SilkRoadEvent {
    data class Travel(val cityId: String) : SilkRoadEvent
    data object Restart : SilkRoadEvent
}

data class SilkRoadResult(
    val completed: Boolean,
    val failed: Boolean,
    val path: List<String>,
    val daysLeft: Int,
    val budgetLeft: Int,
    val feedback: String,
    val rulesLabel: String = "离线资源取舍游戏，不是现实旅行建议"
)

object SilkRoadRouteEngine : CompanionGameEngine<SilkRoadState, SilkRoadEvent, SilkRoadResult> {
    private val cities = mapOf(
        "长安" to RouteCity("长安", "长安", 0, 0),
        "敦煌" to RouteCity("敦煌", "敦煌", 2, 3),
        "喀什" to RouteCity("喀什", "喀什", 3, 4),
        "撒马尔罕" to RouteCity("撒马尔罕", "撒马尔罕", 3, 5)
    )
    private val links = mapOf(
        "长安" to setOf("敦煌"),
        "敦煌" to setOf("长安", "喀什"),
        "喀什" to setOf("敦煌", "撒马尔罕"),
        "撒马尔罕" to setOf("喀什")
    )

    fun destinations(state: SilkRoadState): List<RouteCity> = links[state.current].orEmpty().mapNotNull(cities::get)

    override fun reduce(state: SilkRoadState, event: SilkRoadEvent): SilkRoadState = when (event) {
        SilkRoadEvent.Restart -> SilkRoadState()
        is SilkRoadEvent.Travel -> {
            if (state.completed || state.failed) state else {
                val city = cities[event.cityId]
                val linked = event.cityId in links[state.current].orEmpty()
                when {
                    city == null || !linked -> state.copy(feedback = "这条路线当前没有相邻驿站。")
                    state.daysLeft < city.days || state.budgetLeft < city.cost -> state.copy(
                        failed = true,
                        feedback = "资源不够抵达下一站；路线规划结束。"
                    )
                    else -> state.copy(
                        current = city.id,
                        daysLeft = state.daysLeft - city.days,
                        budgetLeft = state.budgetLeft - city.cost,
                        path = state.path + city.id,
                        completed = city.id == state.target,
                        feedback = if (city.id == state.target) "抵达目标，路线完成。" else "抵达${city.label}，继续规划下一段。"
                    )
                }
            }
        }
    }

    override fun result(state: SilkRoadState): SilkRoadResult = SilkRoadResult(
        state.completed, state.failed, state.path, state.daysLeft, state.budgetLeft, state.feedback
    )
}
