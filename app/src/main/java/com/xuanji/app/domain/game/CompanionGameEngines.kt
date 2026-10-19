package com.xuanji.app.domain.game

/** Self-owned short lines: the game checks structure/rhyme, not authorship. */
data class PoetryPrompt(
    val id: String,
    val line: String,
    val expectedRhyme: String,
    val options: List<String>,
    val hint: String = "留意句尾的韵脚。",
    val source: String = "玄星自有短句库 · 简化韵脚规则"
)

data class PoetryStateSnapshot(
    val promptIndex: Int,
    val selected: String?,
    val score: Int,
    val completed: Boolean,
    val feedback: String
)

data class PoetryChainState(
    val promptIndex: Int = 0,
    val selected: String? = null,
    val score: Int = 0,
    val completed: Boolean = false,
    val feedback: String = "",
    val undoHistory: List<PoetryStateSnapshot> = emptyList(),
    val levelCountAtStart: Int = 24
)

sealed interface PoetryChainEvent {
    data class Choose(val line: String) : PoetryChainEvent
    data object Hint : PoetryChainEvent
    data object Undo : PoetryChainEvent
    data object Restart : PoetryChainEvent
}

data class PoetryChainResult(
    val score: Int,
    val completed: Boolean,
    val feedback: String,
    val corpusLabel: String = "自有短句库",
    val summary: String = ""
)

object PoetryChainEngine : CompanionGameEngine<PoetryChainState, PoetryChainEvent, PoetryChainResult> {
    private val prompts = listOf(
        PoetryPrompt("p1", "小桥收晚风", "ong", listOf("灯影落江中", "星子过长空", "纸鸢入云中"), "句尾可用‘中’或‘空’的韵脚。"),
        PoetryPrompt("p2", "一盏照书夜", "e", listOf("半窗听雨声", "轻舟向远波", "新叶带春风"), "读出句尾的开口音，找‘声’或‘波’。"),
        PoetryPrompt("p3", "竹影移窗慢", "an", listOf("茶烟入砚寒", "山色入衣宽", "灯火过长安"), "这一题押‘寒、宽、安’一类韵脚。"),
        PoetryPrompt("p4", "古渡横舟影", "ing", listOf("疏钟入夜城", "秋水照孤星", "远岫藏归云"), "句尾试找‘城’或‘星’的 ing 韵。"),
        PoetryPrompt("p5", "山前灯未歇", "ie", listOf("归人踏月色", "远寺传清越", "松风过石桥"), "句尾试找‘色’或‘越’的 ie 韵。")
    )

    val levelCount: Int get() = prompts.size + ExpandedGameCatalog.poetry.size

    fun prompt(state: PoetryChainState): PoetryPrompt = (prompts + ExpandedGameCatalog.poetry)[state.promptIndex.coerceIn(0, levelCount - 1)]

    fun isValidState(state: PoetryChainState): Boolean {
        if (state.levelCountAtStart !in setOf(5, levelCount) || state.promptIndex !in 0..state.levelCountAtStart ||
            state.score !in 0..state.promptIndex.coerceAtMost(state.levelCountAtStart) ||
            state.completed != (state.promptIndex == state.levelCountAtStart) ||
            (state.undoHistory.isNotEmpty() && state.undoHistory.size != state.promptIndex)
        ) return false
        if (state.promptIndex == 0) return state.selected == null
        val previousPrompt = prompt(state.copy(promptIndex = state.promptIndex - 1))
        return state.selected in previousPrompt.options
    }

    override fun reduce(state: PoetryChainState, event: PoetryChainEvent): PoetryChainState = when (event) {
        PoetryChainEvent.Restart -> PoetryChainState()
        PoetryChainEvent.Hint -> if (state.completed) state else state.copy(feedback = prompt(state).hint)
        PoetryChainEvent.Undo -> {
            val previous = state.undoHistory.lastOrNull()
            if (previous == null) state.copy(feedback = "没有可撤销的答案。")
            else state.copy(
                promptIndex = previous.promptIndex,
                selected = previous.selected,
                score = previous.score,
                completed = previous.completed,
                feedback = "已撤销上一步，可以重新选择。",
                undoHistory = state.undoHistory.dropLast(1)
            )
        }
        is PoetryChainEvent.Choose -> {
            if (state.completed) state else if (event.line !in prompt(state).options) state.copy(feedback = "没有接上当前题目的选项；请选择提供的短句，进度未改变。") else {
                val current = prompt(state)
                val accepted = event.line in current.options && rhymeMatches(event.line, current.expectedRhyme)
                val nextScore = state.score + if (accepted) 1 else 0
                val nextIndex = state.promptIndex + 1
                val previous = PoetryStateSnapshot(state.promptIndex, state.selected, state.score, state.completed, state.feedback)
                state.copy(
                    promptIndex = nextIndex,
                    selected = event.line,
                    score = nextScore,
                    completed = nextIndex >= state.levelCountAtStart,
                    feedback = if (accepted) "韵脚和结构都接上了。" else "这句可以读，但没有接上本题的韵脚；再试一条。",
                    undoHistory = state.undoHistory + previous
                )
            }
        }
    }

    override fun result(state: PoetryChainState): PoetryChainResult = PoetryChainResult(
        score = state.score,
        completed = state.completed,
        feedback = state.feedback,
        summary = if (state.completed) "诗句接龙完成：${state.promptIndex} 题，答对 ${state.score} 题；内容来自自有短句库。" else "已完成 ${state.promptIndex} / ${state.levelCountAtStart} 题。"
    )

    private fun rhymeMatches(line: String, expected: String): Boolean = when (expected) {
        "ong" -> line.endsWith("中") || line.endsWith("空")
        "e" -> line.endsWith("声") || line.endsWith("波")
        "an" -> line.endsWith("寒") || line.endsWith("宽") || line.endsWith("安")
        "ing" -> line.endsWith("城") || line.endsWith("星")
        "ie" -> line.endsWith("色") || line.endsWith("越")
        else -> false
    }
}

data class StarMapPuzzle(
    val id: String,
    val start: String,
    val target: String,
    val edges: Map<String, Set<String>>,
    val solution: List<String> = listOf(start, target),
    val hint: String = "沿着直接连线逐步抵达目标。"
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
    data object Hint : StarMapEvent
    data object Undo : StarMapEvent
    data object NextPuzzle : StarMapEvent
    data object RestartPuzzle : StarMapEvent
    data object Restart : StarMapEvent
}

data class StarMapResult(
    val completed: Boolean,
    val path: List<String>,
    val feedback: String,
    val rulesLabel: String = "观测逻辑题，不是占星结论",
    val summary: String = ""
)

object StarMapPuzzleEngine : CompanionGameEngine<StarMapState, StarMapEvent, StarMapResult> {
    private val puzzles = listOf(
        StarMapPuzzle("s1", "织女", "天狼", mapOf("织女" to setOf("天津"), "天津" to setOf("织女", "天狼"), "天狼" to setOf("天津")), listOf("织女", "天津", "天狼"), "先走到唯一相邻的‘天津’，再前往目标。"),
        StarMapPuzzle("s2", "北斗一", "北斗三", mapOf("北斗一" to setOf("北斗二"), "北斗二" to setOf("北斗一", "北斗三"), "北斗三" to setOf("北斗二")), listOf("北斗一", "北斗二", "北斗三"), "中间点‘北斗二’连接起点和目标。"),
        StarMapPuzzle("s3", "星点甲", "星点丁", mapOf("星点甲" to setOf("星点乙", "星点丙"), "星点乙" to setOf("星点甲", "星点丙"), "星点丙" to setOf("星点乙", "星点丁", "星点甲"), "星点丁" to setOf("星点丙")), listOf("星点甲", "星点丙", "星点丁"), "从两个邻点里选能继续连到‘星点丁’的分支。"),
        StarMapPuzzle("s4", "星点戊", "星点辛", mapOf("星点戊" to setOf("星点己", "星点庚"), "星点己" to setOf("星点戊", "星点辛"), "星点庚" to setOf("星点戊", "星点辛"), "星点辛" to setOf("星点己", "星点庚")), listOf("星点戊", "星点己", "星点辛"), "两条路线都可达，任选一个中间点即可。"),
        StarMapPuzzle("s5", "星点壬", "星点乙十", mapOf("星点壬" to setOf("星点癸"), "星点癸" to setOf("星点壬", "星点甲十"), "星点甲十" to setOf("星点癸", "星点乙十"), "星点乙十" to setOf("星点甲十")), listOf("星点壬", "星点癸", "星点甲十", "星点乙十"), "按连线顺序经过‘星点癸’和‘星点甲十’。")
    )

    val puzzleCount: Int get() = puzzles.size + ExpandedGameCatalog.stars.size

    fun puzzle(state: StarMapState): StarMapPuzzle = (puzzles + ExpandedGameCatalog.stars)[state.puzzleIndex.coerceIn(0, puzzleCount - 1)]

    fun hasNextPuzzle(state: StarMapState): Boolean = state.completed && state.puzzleIndex < puzzleCount - 1

    fun isValidState(state: StarMapState): Boolean {
        if (state.puzzleIndex !in 0 until puzzleCount || state.path.isEmpty() || state.path.size > 20) return false
        val puzzle = puzzle(state)
        return state.current in puzzle.edges &&
            state.path.first() == puzzle.start &&
            state.path.last() == state.current &&
            state.path.zipWithNext().all { (from, to) -> to in puzzle.edges[from].orEmpty() } &&
            state.completed == (state.current == puzzle.target)
    }

    override fun reduce(state: StarMapState, event: StarMapEvent): StarMapState = when (event) {
        StarMapEvent.Restart -> initial()
        StarMapEvent.RestartPuzzle -> initial(state.puzzleIndex)
        StarMapEvent.Hint -> if (state.completed) state else state.copy(feedback = puzzle(state).hint)
        StarMapEvent.NextPuzzle -> if (hasNextPuzzle(state)) initial(state.puzzleIndex + 1) else state
        StarMapEvent.Undo -> {
            if (state.path.size <= 1) state.copy(feedback = "已在本题起点，没有可撤销的连线。")
            else {
                val path = state.path.dropLast(1)
                state.copy(current = path.last(), path = path, completed = false, feedback = "已撤销上一段连线。")
            }
        }
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

    fun initial(puzzleIndex: Int = 0): StarMapState {
        val index = puzzleIndex.coerceIn(0, puzzleCount - 1)
        val puzzle = puzzle(StarMapState(puzzleIndex = index))
        return StarMapState(puzzleIndex = index, current = puzzle.start, path = listOf(puzzle.start))
    }

    fun options(state: StarMapState): List<String> {
        val puzzle = puzzle(state)
        return puzzle.edges[state.current.ifBlank { puzzle.start }].orEmpty().sorted()
    }

    override fun result(state: StarMapState): StarMapResult = StarMapResult(
        state.completed,
        state.path,
        state.feedback,
        summary = if (state.completed) "第 ${state.puzzleIndex + 1} 题完成：${state.path.joinToString(" → ")}。观测逻辑题，不是占星结论。" else "正在进行第 ${state.puzzleIndex + 1} / $puzzleCount 题。"
    )
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
    val feedback: String = "",
    val missionId: String = "silk-01",
    val plannedPath: List<String> = emptyList()
)

sealed interface SilkRoadEvent {
    data class PlanRoute(val path: List<String>) : SilkRoadEvent
    data class SelectMission(val missionId: String) : SilkRoadEvent
    data class Travel(val cityId: String) : SilkRoadEvent
    data object Undo : SilkRoadEvent
    data object Restart : SilkRoadEvent
}

data class SilkRoadLeg(val from: String, val to: String, val days: Int, val cost: Int, val daysSpent: Int, val budgetSpent: Int)

data class SilkRoadResult(
    val completed: Boolean,
    val failed: Boolean,
    val path: List<String>,
    val daysLeft: Int,
    val budgetLeft: Int,
    val feedback: String,
    val rulesLabel: String = "离线资源取舍游戏，不是现实旅行建议",
    val summary: String = ""
)

object SilkRoadRouteEngine : CompanionGameEngine<SilkRoadState, SilkRoadEvent, SilkRoadResult> {
    fun initial(missionId: String = "silk-01"): SilkRoadState {
        val mission = SilkRoadMissionCatalog.find(missionId) ?: SilkRoadMissionCatalog.all.first()
        return SilkRoadState(target = mission.target, daysLeft = mission.days, budgetLeft = mission.supplies, missionId = mission.id)
    }
    fun destinations(state: SilkRoadState): List<RouteCity> = SilkRoadGeography.destinations(state.current)

    fun replay(state: SilkRoadState): List<SilkRoadLeg> {
        var daysSpent = 0
        var budgetSpent = 0
        return state.path.zipWithNext().map { (from, to) ->
            val leg = requireNotNull(SilkRoadGeography.leg(from, to))
            daysSpent += leg.days
            budgetSpent += leg.supplies
            SilkRoadLeg(from, to, leg.days, leg.supplies, daysSpent, budgetSpent)
        }
    }

    fun isValidState(state: SilkRoadState): Boolean {
        val mission = SilkRoadMissionCatalog.find(state.missionId) ?: return false
        if (state.path.isEmpty() || state.path.size > 40 || state.target != mission.target || !SilkRoadGeography.contains(state.current)) return false
        if (state.plannedPath.isNotEmpty() && !SilkRoadGeography.validPlan(state.plannedPath, state.current, state.target)) return false
        val validPath = state.path.first() == "长安" &&
            state.path.last() == state.current &&
            state.path.all(SilkRoadGeography::contains) &&
            state.path.zipWithNext().all { (from, to) -> SilkRoadGeography.leg(from, to) != null }
        if (!validPath) return false
        val travelled = state.path.zipWithNext().map { (from, to) -> SilkRoadGeography.leg(from, to)!! }
        val expectedDays = mission.days - travelled.sumOf { it.days }
        val expectedBudget = mission.supplies - travelled.sumOf { it.supplies }
        return state.daysLeft == expectedDays &&
            state.budgetLeft == expectedBudget &&
            expectedDays >= 0 && expectedBudget >= 0 &&
            state.completed == (state.current == state.target) &&
            !(state.completed && state.failed) &&
            (!state.failed || destinations(state).any { state.daysLeft < it.days || state.budgetLeft < it.cost })
    }

    override fun reduce(state: SilkRoadState, event: SilkRoadEvent): SilkRoadState = when (event) {
        is SilkRoadEvent.PlanRoute -> {
            if (!SilkRoadGeography.validPlan(event.path, state.current, state.target) || state.completed || state.failed) state
            else {
                val legs = event.path.zipWithNext().map { (a, b) -> SilkRoadGeography.leg(a, b)!! }
                if (legs.sumOf { it.days } > state.daysLeft || legs.sumOf { it.supplies } > state.budgetLeft) state.copy(feedback = "当前资源不足以完成该方案；尚未消耗资源。")
                else state.copy(plannedPath = event.path, feedback = "方案已选定，仍需逐站确认前往。")
            }
        }
        SilkRoadEvent.Restart -> initial(state.missionId)
        is SilkRoadEvent.SelectMission -> if (SilkRoadMissionCatalog.find(event.missionId) == null) state else initial(event.missionId)
        SilkRoadEvent.Undo -> {
            if (state.failed) state.copy(failed = false, feedback = "已撤销未执行的资源不足决定，可以重新选路。")
            else if (state.path.size <= 1) state.copy(feedback = "已在起点，没有可撤销的路段。")
            else {
                val path = state.path.dropLast(1)
                val travelled = path.zipWithNext().map { (from, to) -> SilkRoadGeography.leg(from, to)!! }
                state.copy(
                    current = path.last(),
                    daysLeft = initial(state.missionId).daysLeft - travelled.sumOf { it.days },
                    budgetLeft = initial(state.missionId).budgetLeft - travelled.sumOf { it.supplies },
                    path = path,
                    plannedPath = emptyList(),
                    completed = false,
                    failed = false,
                    feedback = "已撤销上一段，剩余天数和资源已重算。"
                )
            }
        }
        is SilkRoadEvent.Travel -> {
            if (state.completed || state.failed || state.path.size >= 40) state else {
                val city = destinations(state).firstOrNull { it.id == event.cityId }
                val linked = city != null
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
                        plannedPath = if (state.plannedPath.getOrNull(1) == city.id) state.plannedPath.drop(1) else emptyList(),
                        completed = city.id == state.target,
                        feedback = if (city.id == state.target) "抵达目标，路线完成。" else "抵达${city.label}，继续规划下一段。"
                    )
                }
            }
        }
    }

    override fun result(state: SilkRoadState): SilkRoadResult = SilkRoadResult(
        completed = state.completed,
        failed = state.failed,
        path = state.path,
        daysLeft = state.daysLeft,
        budgetLeft = state.budgetLeft,
        feedback = state.feedback,
        summary = buildString {
            append("${SilkRoadMissionCatalog.find(state.missionId)?.title.orEmpty()} · ")
            append(if (state.completed) "路线完成" else if (state.failed) "规划结束" else "路线回放")
            append("：${state.path.joinToString(" → ")}。")
            if (state.completed || state.failed) append("剩余 ${state.daysLeft} 天、${state.budgetLeft} 点资源。")
            append("离线示意路线，不是现实旅行或安全建议。")
        }
    )
}
