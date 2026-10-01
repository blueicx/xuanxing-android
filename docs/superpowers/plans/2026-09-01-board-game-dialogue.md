# 人物对话真实棋局实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在人物交流面板中加入可验证的中国象棋真实对弈，并为国际象棋与围棋保留统一扩展接口，使角色话术只引用真实棋局状态，不伪造棋子、合法性、胜率或结果。

**架构：** 采用“对话意图 → 游戏会话 reducer → 规则模块 → 引擎 adapter → 棋盘 UI/角色解说”的深模块结构。第一阶段以纯 Kotlin 中国象棋规则和本地确定性应手作为可测试基础，第二阶段接入 Pikafish UCI 原生引擎；围棋使用同一会话协议，待中国象棋稳定后再接入 GTP 引擎。现有离线 `MysticDialogueEngine` 仍是默认入口，游戏模块不改变原有运势、占卜和安全边界。

**技术栈：** Kotlin/JVM domain、JUnit 4、Jetpack Compose、协程、Android NDK/CMake、Pikafish UCI adapter；棋局序列化采用 JSON 兼容 DTO，不引入联网服务或密钥。

---

## 文件清单与职责

### 新建文件

- `app/src/main/java/com/xuanji/app/domain/game/GameTypes.kt`：游戏类型、颜色、坐标、走法、局面和引擎结果等稳定值对象。
- `app/src/main/java/com/xuanji/app/domain/game/XiangqiBoard.kt`：初始局面、不可变棋盘、走法应用与 FEN-like 局面编码。
- `app/src/main/java/com/xuanji/app/domain/game/XiangqiRules.kt`：中国象棋合法走法、将军/将死、蹩马腿、炮隔子、九宫和河界规则。
- `app/src/main/java/com/xuanji/app/domain/game/GameSessionState.kt`：通用游戏会话状态、事件和 reducer，负责 session/turn token 及旧异步结果丢弃。
- `app/src/main/java/com/xuanji/app/domain/game/GameEngine.kt`：`BoardRules`、`BoardEngine`、`OfflineBoardEngine` 和 `EngineResult` 接口。
- `app/src/main/java/com/xuanji/app/domain/game/GameDialogueBridge.kt`：把自然语言映射为棋局动作，并根据真实事件生成角色解说。
- `app/src/main/java/com/xuanji/app/domain/game/XiangqiNotation.kt`：坐标、中文着法和 UCI 走法互转，拒绝模糊或非法输入。
- `app/src/main/java/com/xuanji/app/domain/game/GoContracts.kt`：围棋预留的局面、落子、规则与 GTP adapter 契约，不在本阶段伪装成已实现。
- `app/src/main/java/com/xuanji/app/ui/components/game/GameBoardCard.kt`：Compose 棋盘、合法落子提示、当前手、悔棋/提示/退出按钮和无障碍语义。
- `app/src/main/java/com/xuanji/app/ui/components/game/XiangqiPieceGlyphs.kt`：中国象棋棋子字形与主题化绘制，棋盘布局不承担规则判断。
- `app/src/main/cpp/CMakeLists.txt`：仅在原生引擎阶段启用的 arm64-v8a 构建入口。
- `app/src/main/cpp/pikafish_bridge.cpp`：JNI/CLI bridge，负责 UCI 输入输出和取消，不包含棋局规则。
- `docs/BOARD_GAME_INTEGRATION.md`：运行模式、开源许可、引擎打包、离线行为与排障说明。
- `docs/superpowers/specs/2026-09-01-board-game-dialogue-design.md`：实现前冻结的接口、交互和安全决策。

### 修改文件

- `app/build.gradle.kts`：测试依赖、source set、NDK/CMake 开关和 ABI 约束；默认构建不要求原生引擎文件存在。
- `app/src/main/java/com/xuanji/app/domain/MysticDialogueEngine.kt`：若意图枚举仍位于此处，扩展游戏意图并接入 `GameDialogueBridge`。
- `app/src/main/java/com/xuanji/app/domain/MysticIntentClassifier.kt`：识别“来一盘象棋/走炮二平五/悔棋/提示/复盘/退出”等表达，保留原有问候、运势和安全意图优先级。
- `app/src/main/java/com/xuanji/app/domain/MysticSessionState.kt`：增加可选 `gameState` 和游戏事件转发，切换 persona/skin/topic 时递增 token 并清理棋局异步请求。
- `app/src/main/java/com/xuanji/app/domain/DialogueProvider.kt`：增加 `DialogueRequest.gameContext` 可选字段；离线 provider 仍默认，不将棋局交给外部模型。
- `app/src/main/java/com/xuanji/app/ui/components/MysticGuideCard.kt`：在消息流中挂载 `GameBoardCard`，移除游戏输入期间对通用文本回复的误判。
- `app/src/main/java/com/xuanji/app/ui/components/MysticFloatingGuide.kt`：舞台关闭时保存棋局摘要，浮球恢复后回到同一会话；不改变默认浮球可见性。
- `app/src/main/java/com/xuanji/app/ui/composite/CompositeFortuneScreen.kt`、`app/src/main/java/com/xuanji/app/ui/eastern/EasternScreen.kt`、`app/src/main/java/com/xuanji/app/ui/western/WesternScreen.kt`：只传递已有 `MysticGuideCard` 游戏回调，不复制棋局状态。
- `README.md`、`docs/SYSTEMS_OVERVIEW.md`、`docs/TECHNICAL_DEBT.md`：说明真实棋局的当前支持范围、默认离线、未启用的围棋/在线 provider，以及验证命令。
- `LICENSE` 或新增 `NOTICE-THIRD-PARTY.md`：在引入 Pikafish 源码或二进制时附带 GPLv3 文本、版权与对应 source offer。

### 测试文件

- `app/src/test/kotlin/com/xuanji/app/domain/game/XiangqiBoardTest.kt`
- `app/src/test/kotlin/com/xuanji/app/domain/game/XiangqiRulesTest.kt`
- `app/src/test/kotlin/com/xuanji/app/domain/game/XiangqiNotationTest.kt`
- `app/src/test/kotlin/com/xuanji/app/domain/game/GameSessionReducerTest.kt`
- `app/src/test/kotlin/com/xuanji/app/domain/game/GameDialogueBridgeTest.kt`
- `app/src/test/kotlin/com/xuanji/app/domain/game/OfflineBoardEngineTest.kt`
- `app/src/test/kotlin/com/xuanji/app/domain/game/GoContractsTest.kt`
- `app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueGameIntentTest.kt`
- `_dev/dialogue_contract.json`：增加游戏意图、棋局事件和 golden wording。

---

## 任务 1：冻结游戏领域模型与验证边界

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/domain/game/GameTypes.kt`
- 创建：`app/src/main/java/com/xuanji/app/domain/game/GoContracts.kt`
- 创建：`docs/superpowers/specs/2026-09-01-board-game-dialogue-design.md`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/game/GoContractsTest.kt`

- [ ] **步骤 1：编写失败测试，固定通用事件和预留契约**

```kotlin
@Test
fun gameType_and_move_are_serializable_contract_values() {
    val move = BoardMove(from = Square(0, 0), to = Square(0, 1), notation = "车九进一")
    assertEquals(GameType.XIANGQI, GameType.valueOf("XIANGQI"))
    assertEquals("车九进一", move.notation)
}

@Test
fun go_contract_is_explicitly_unavailable_until_provider_is_supplied() {
    val result = GoRulesAvailability.unavailable("go_provider_not_enabled")
    assertFalse(result.available)
    assertEquals("go_provider_not_enabled", result.reason)
}
```

- [ ] **步骤 2：运行测试确认失败**

运行：`./gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.game.GoContractsTest" --no-daemon --console=plain`

预期：FAIL，报错为 `Unresolved reference: GameType` 或 `Unresolved reference: GoRulesAvailability`。

- [ ] **步骤 3：实现最小领域模型**

```kotlin
enum class GameType { XIANGQI, CHESS, GO }
enum class PlayerColor { RED, BLACK, WHITE }
data class Square(val file: Int, val rank: Int)
data class BoardMove(
    val from: Square?,
    val to: Square,
    val notation: String,
    val captured: String? = null,
    val player: PlayerColor = PlayerColor.RED
)
data class EngineEvaluation(val centipawns: Int?, val mateIn: Int?, val depth: Int)
data class EngineTurn(val move: BoardMove, val evaluation: EngineEvaluation? = null)
data class GoRulesAvailability(val available: Boolean, val reason: String?) {
    companion object { fun unavailable(reason: String) = GoRulesAvailability(false, reason) }
}
```

围棋契约只定义值对象和不可用结果，不返回虚构落子或胜率。

- [ ] **步骤 4：运行测试确认通过**

运行：`./gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.game.GoContractsTest" --no-daemon --console=plain`

预期：PASS。

- [ ] **步骤 5：提交**

```bash
git add app/src/main/java/com/xuanji/app/domain/game/GameTypes.kt app/src/main/java/com/xuanji/app/domain/game/GoContracts.kt app/src/test/kotlin/com/xuanji/app/domain/game/GoContractsTest.kt docs/superpowers/specs/2026-09-01-board-game-dialogue-design.md
git commit -m "feat: freeze board game domain contracts"
```

## 任务 2：中国象棋不可变棋盘与规则核心

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/domain/game/XiangqiBoard.kt`
- 创建：`app/src/main/java/com/xuanji/app/domain/game/XiangqiRules.kt`
- 创建：`app/src/main/java/com/xuanji/app/domain/game/XiangqiNotation.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/game/XiangqiBoardTest.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/game/XiangqiRulesTest.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/game/XiangqiNotationTest.kt`

- [ ] **步骤 1：先写规则失败测试**

测试必须覆盖：初始 32 子、红黑回合、兵过河前后走法、马腿、炮隔子、象不过河、士/将九宫、车线阻挡、吃子、将军、将死、重复局面计数和非法自杀将。

```kotlin
@Test fun initial_position_has_32_pieces() =
    assertEquals(32, XiangqiBoard.initial().pieces.count { it != null })

@Test fun horse_cannot_jump_over_blocking_leg() {
    val position = XiangqiFixtures.horseWithBlockedLeg()
    val legal = XiangqiRules.legalMoves(position, PlayerColor.RED)
    assertFalse(legal.any { it.from == Square(1, 2) && it.to == Square(3, 3) })
}

@Test fun cannon_requires_exactly_one_screen_to_capture() {
    val oneScreen = XiangqiFixtures.cannonWithOneScreen()
    val twoScreens = XiangqiFixtures.cannonWithTwoScreens()
    assertTrue(XiangqiRules.legalMoves(oneScreen, PlayerColor.RED).any { it.to == Square(4, 7) })
    assertFalse(XiangqiRules.legalMoves(twoScreens, PlayerColor.RED).any { it.to == Square(4, 7) })
}

@Test fun elephant_cannot_cross_river() {
    val position = XiangqiFixtures.redElephantNearRiver()
    assertFalse(XiangqiRules.legalMoves(position, PlayerColor.RED).any { it.to.rank < 5 })
}

@Test fun generals_cannot_face_each_other() {
    val position = XiangqiFixtures.generalsFacing()
    assertTrue(XiangqiRules.outcome(position).isIllegalPosition)
}

@Test fun notation_round_trip_preserves_move() {
    val move = BoardMove(Square(1, 0), Square(1, 2), "马八进七", player = PlayerColor.RED)
    val text = XiangqiNotation.format(move, XiangqiBoard.initial())
    assertEquals(move, XiangqiNotation.parse(text, XiangqiBoard.initial()))
}
```

- [ ] **步骤 2：运行规则测试确认失败**

运行：`./gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.game.Xiangqi*Test" --no-daemon --console=plain`

预期：FAIL，规则类型和初始局面尚未定义。

- [ ] **步骤 3：实现纯 Kotlin 规则核心**

使用 `List<Piece?>` 或固定数组保存 9×10 棋盘，`XiangqiBoard` 保持不可变；`XiangqiRules.legalMoves(board, color)` 先生成伪合法走法，再应用走法并拒绝将军状态。所有坐标使用 `file=0..8`、`rank=0..9`，UI 不直接操作数组。

```kotlin
interface BoardRules {
    fun legalMoves(position: BoardPosition, color: PlayerColor): List<BoardMove>
    fun apply(position: BoardPosition, move: BoardMove): RuleResult
    fun outcome(position: BoardPosition): GameOutcome
}

sealed interface RuleResult {
    data class Applied(val position: BoardPosition, val move: BoardMove) : RuleResult
    data class Rejected(val code: String) : RuleResult
}
```

规则错误码固定为 `from_empty`、`wrong_turn`、`illegal_move`、`self_check`、`game_over`，供 UI 和对话共同使用。

- [ ] **步骤 4：运行全部规则测试确认通过**

运行：`./gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.game.Xiangqi*Test" --no-daemon --console=plain`

预期：全部 PASS；不得依赖 Android `Context`、Compose 或引擎进程。

- [ ] **步骤 5：提交**

```bash
git add app/src/main/java/com/xuanji/app/domain/game/XiangqiBoard.kt app/src/main/java/com/xuanji/app/domain/game/XiangqiRules.kt app/src/main/java/com/xuanji/app/domain/game/XiangqiNotation.kt app/src/test/kotlin/com/xuanji/app/domain/game/Xiangqi*Test.kt
git commit -m "feat: add tested xiangqi rules core"
```

## 任务 3：通用游戏会话 reducer 与离线应手

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/domain/game/GameSessionState.kt`
- 创建：`app/src/main/java/com/xuanji/app/domain/game/GameEngine.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/game/GameSessionReducerTest.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/game/OfflineBoardEngineTest.kt`

- [ ] **步骤 1：编写 token、悔棋和异步过期失败测试**

```kotlin
@Test fun context_change_drops_old_engine_reply() {
    val state = GameSessionState(sessionToken = 8L)
    val changed = reduceGame(state, GameEvent.Start(GameType.XIANGQI, token = 9L))
    val stale = reduceGame(changed, GameEvent.EngineReply(8L, Fixtures.engineTurn()))
    assertEquals(changed, stale)
}

@Test fun undo_restores_previous_position_without_reusing_future_moves() {
    val state = Fixtures.afterTwoMoves()
    val undone = reduceGame(state, GameEvent.Undo(state.sessionToken))
    assertEquals(1, undone.history.size)
    assertEquals(1, undone.redo.size)
    assertEquals(Fixtures.positionAfterFirstMove(), undone.position)
}

@Test fun offline_engine_returns_only_legal_deterministic_move() = runTest {
    val position = XiangqiBoard.initial().asPosition()
    val first = OfflineBoardEngine("easy").bestMove(position, PlayerColor.RED, 3L)
    val second = OfflineBoardEngine("easy").bestMove(position, PlayerColor.RED, 3L)
    assertEquals(first, second)
    assertTrue(first is EngineResult.Move)
    assertTrue(XiangqiRules.legalMoves(position, PlayerColor.RED).contains((first as EngineResult.Move).turn.move))
}
```

- [ ] **步骤 2：运行测试确认失败**

运行：`./gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.game.*Session*Test" --tests "com.xuanji.app.domain.game.OfflineBoardEngineTest" --no-daemon --console=plain`

预期：FAIL，`GameSessionState`、`reduceGame` 和 `OfflineBoardEngine` 不存在。

- [ ] **步骤 3：实现 reducer 与 engine seam**

```kotlin
data class GameSessionState(
    val sessionToken: Long = 0L,
    val gameType: GameType = GameType.XIANGQI,
    val position: BoardPosition = XiangqiBoard.initial().asPosition(),
    val history: List<BoardMove> = emptyList(),
    val redo: List<BoardMove> = emptyList(),
    val request: GameRequest = GameRequest.Idle,
    val outcome: GameOutcome = GameOutcome.InProgress
)

sealed interface GameEvent {
    data class Start(val type: GameType, val token: Long) : GameEvent
    data class ApplyMove(val token: Long, val move: BoardMove) : GameEvent
    data class EngineReply(val token: Long, val turn: EngineTurn) : GameEvent
    data class Undo(val token: Long) : GameEvent
    data class Cancel(val token: Long) : GameEvent
    data object Exit : GameEvent
}

fun reduceGame(state: GameSessionState, event: GameEvent): GameSessionState = when (event) {
    is GameEvent.Start -> state.copy(
        sessionToken = event.token,
        gameType = event.type,
        position = XiangqiBoard.initial().asPosition(),
        history = emptyList(),
        redo = emptyList(),
        request = GameRequest.Idle,
        outcome = GameOutcome.InProgress
    )
    is GameEvent.ApplyMove -> if (event.token != state.sessionToken) state else state.applyPlayerMove(event.move)
    is GameEvent.EngineReply -> if (event.token != state.sessionToken) state else state.applyEngineTurn(event.turn)
    is GameEvent.Undo -> if (event.token != state.sessionToken) state else state.undoLastPair()
    is GameEvent.Cancel -> if (event.token != state.sessionToken) state else state.copy(request = GameRequest.Idle)
    GameEvent.Exit -> GameSessionState()
}

interface BoardEngine {
    suspend fun bestMove(position: BoardPosition, color: PlayerColor, token: Long): EngineResult
}
```

`EngineReply.token != state.sessionToken` 时返回原状态；`Start`、`Exit` 和上层 `ChangeContext` 都清空未完成请求。`applyPlayerMove` 与 `applyEngineTurn` 必须再次经过 `BoardRules.apply`，拒绝非法走法后保持原局面。

- [ ] **步骤 4：实现确定性离线应手**

`OfflineBoardEngine` 从合法走法按 `stableHash(position.encode() + color + difficulty)` 选择应手；它用于无原生引擎设备、测试和降级，不显示引擎胜率，不宣称强度等级。

- [ ] **步骤 5：运行测试确认通过**

运行：`./gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.game.*Session*Test" --tests "com.xuanji.app.domain.game.OfflineBoardEngineTest" --no-daemon --console=plain`

预期：全部 PASS。

- [ ] **步骤 6：提交**

```bash
git add app/src/main/java/com/xuanji/app/domain/game/GameSessionState.kt app/src/main/java/com/xuanji/app/domain/game/GameEngine.kt app/src/test/kotlin/com/xuanji/app/domain/game/GameSessionReducerTest.kt app/src/test/kotlin/com/xuanji/app/domain/game/OfflineBoardEngineTest.kt
git commit -m "feat: add deterministic game session reducer"
```

## 任务 4：把真实棋局接入人物对话

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/domain/game/GameDialogueBridge.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticIntentClassifier.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticDialogueEngine.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticSessionState.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/DialogueProvider.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/game/GameDialogueBridgeTest.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueGameIntentTest.kt`
- 修改：`_dev/dialogue_contract.json`

- [ ] **步骤 1：写 golden wording 失败测试**

至少覆盖：`来一盘象棋`、`红方`、`走炮二平五`、`这步能走吗`、`悔棋`、`给我提示`、`复盘刚才那步`、`退出棋局`、空输入、超长输入、大小写和全半角标点；每个回复都要包含真实错误码或真实走法，不得把通用运势模板当成棋局回复。

- [ ] **步骤 2：运行测试确认失败**

运行：`./gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.game.GameDialogueBridgeTest" --tests "com.xuanji.app.domain.MysticDialogueGameIntentTest" --no-daemon --console=plain`

预期：FAIL，游戏意图尚未识别。

- [ ] **步骤 3：实现 bridge 和安全约束**

```kotlin
data class GameDialogueResult(
    val event: GameEvent?,
    val reply: String,
    val state: GameSessionState,
    val grounded: Boolean
)

interface GameDialogueBridge {
    fun handle(state: GameSessionState, input: String): GameDialogueResult
}
```

规则：

1. 只有 `grounded=true` 的结果可以写入角色消息流。
2. 回复中的棋子、坐标、将军、吃子、胜负均从 `BoardMove`、`RuleResult` 或 `GameOutcome` 读取。
3. “运势不错所以这步一定赢”一类跨域结论一律改为“局面信息不足，先看合法走法”。
4. 角色 persona 只改变语气、称呼和解释长度，不改变棋局事实。
5. 用户主动选择“保存棋局”时只保存局面和走法，不把生成的角色评价伪装成用户记忆。

- [ ] **步骤 4：同步 Android/小程序契约**

在 `_dev/dialogue_contract.json` 增加 `game_intents`、`game_events`、`board_encoding`、`token_drop_rule`、`grounded_reply_required` 和至少 12 条 golden wording；小程序暂时只执行契约测试，不复制 Android 棋盘实现。

- [ ] **步骤 5：运行对话与既有回归测试**

运行：`./gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain`

预期：既有 48 项及新增游戏测试全部 PASS；对话旧 golden wording 不改变。

- [ ] **步骤 6：提交**

```bash
git add app/src/main/java/com/xuanji/app/domain/game/GameDialogueBridge.kt app/src/main/java/com/xuanji/app/domain/MysticIntentClassifier.kt app/src/main/java/com/xuanji/app/domain/MysticDialogueEngine.kt app/src/main/java/com/xuanji/app/domain/MysticSessionState.kt app/src/main/java/com/xuanji/app/domain/DialogueProvider.kt app/src/test/kotlin/com/xuanji/app/domain/game/GameDialogueBridgeTest.kt app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueGameIntentTest.kt _dev/dialogue_contract.json
git commit -m "feat: ground character dialogue in game state"
```

## 任务 5：Compose 棋盘卡片和交流面板挂载

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/ui/components/game/GameBoardCard.kt`
- 创建：`app/src/main/java/com/xuanji/app/ui/components/game/XiangqiPieceGlyphs.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/components/MysticGuideCard.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/components/MysticFloatingGuide.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/composite/CompositeFortuneScreen.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/eastern/EasternScreen.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/western/WesternScreen.kt`

- [ ] **步骤 1：先增加 Compose 可测试的状态映射测试**

验证棋盘只渲染 `GameSessionState.position`，非法落子按钮不可点击，旧 token 的引擎回包不改变显示；若当前工程没有 Compose UI test 依赖，先只测试 `GameBoardUiModel.from(state)` 的纯 Kotlin 映射。

- [ ] **步骤 2：实现棋盘卡片**

棋盘固定 9×10 网格，横竖线清晰，红黑双方颜色和文字对比度符合现有主题；点击棋子后只显示 `legalMoves`，点击目标格触发 `GameEvent.ApplyMove`。按钮提供 `contentDescription`：`棋盘`、`撤销上一手`、`请求提示`、`退出棋局`，最小触控尺寸 48dp。

- [ ] **步骤 3：接入 MysticGuideCard**

游戏消息使用独立卡片，不再经过普通 `pendingCustom` 文本回复路径；棋盘卡片关闭后保留 `GameSessionState`，退出时显式清空。角色解说显示在最后一手下方，且标记“基于当前局面”。

- [ ] **步骤 4：检查浮球/舞台状态**

浮球关闭和重新召回不得重建棋局；切换综合、东方、西方页时只允许复用当前会话 token，不能复制三个棋局实例。reduced-motion 下棋盘不做持续旋转或位移动画。

- [ ] **步骤 5：运行编译、单测和 lint**

运行：`./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon --console=plain`

预期：测试 PASS、lint 0 error、debug APK 成功生成。

- [ ] **步骤 6：提交**

```bash
git add app/src/main/java/com/xuanji/app/ui/components/game app/src/main/java/com/xuanji/app/ui/components/MysticGuideCard.kt app/src/main/java/com/xuanji/app/ui/components/MysticFloatingGuide.kt app/src/main/java/com/xuanji/app/ui/composite/CompositeFortuneScreen.kt app/src/main/java/com/xuanji/app/ui/eastern/EasternScreen.kt app/src/main/java/com/xuanji/app/ui/western/WesternScreen.kt
git commit -m "feat: mount xiangqi board in companion dialogue"
```

## 任务 6：接入 Pikafish UCI 原生引擎与离线降级

**文件：**
- 修改：`app/build.gradle.kts`
- 创建：`app/src/main/cpp/CMakeLists.txt`
- 创建：`app/src/main/cpp/pikafish_bridge.cpp`
- 创建：`app/src/main/java/com/xuanji/app/domain/game/PikafishEngine.kt`
- 创建：`NOTICE-THIRD-PARTY.md`
- 修改：`docs/BOARD_GAME_INTEGRATION.md`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/game/PikafishProtocolTest.kt`

- [ ] **步骤 1：写 UCI 协议解析失败测试**

测试 `uciok`、`readyok`、`bestmove`、`info depth`、引擎超时、取消和进程退出；测试使用固定字符串，不启动真实 native 进程。

- [ ] **步骤 2：运行测试确认失败**

运行：`./gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.game.PikafishProtocolTest" --no-daemon --console=plain`

预期：FAIL，协议 parser 和 adapter 尚未定义。

- [ ] **步骤 3：实现 UCI seam 和进程生命周期**

`PikafishEngine` 只接受 `BoardPosition`，将局面编码为 UCI position，发送 `go movetime N`，解析第一条合法 `bestmove`，然后再次用 `XiangqiRules` 校验；任何解析失败、超时、取消或非法走法都回退到 `OfflineBoardEngine`。

Android 首个 ABI 只启用 `arm64-v8a`；原生引擎未打包时应用仍可构建和运行。不得从网络下载引擎或模型。

- [ ] **步骤 4：补充许可与 source offer**

在 `NOTICE-THIRD-PARTY.md` 写明 Pikafish 版本、来源、GPLv3 文本位置、修改说明和对应完整源码获取方式；release 检查不得把无许可文件的原生二进制打进 APK。

- [ ] **步骤 5：运行协议测试和完整门禁**

运行：`./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon --console=plain`

预期：全部通过；没有原生引擎时明确记录为离线规则/确定性应手模式。

- [ ] **步骤 6：提交**

```bash
git add app/build.gradle.kts app/src/main/cpp app/src/main/java/com/xuanji/app/domain/game/PikafishEngine.kt app/src/test/kotlin/com/xuanji/app/domain/game/PikafishProtocolTest.kt NOTICE-THIRD-PARTY.md docs/BOARD_GAME_INTEGRATION.md
git commit -m "feat: add optional pikafish uci adapter"
```

## 任务 7：围棋和国际象棋扩展 seam（不改变中国象棋默认体验）

**文件：**
- 修改：`app/src/main/java/com/xuanji/app/domain/game/GoContracts.kt`
- 创建：`app/src/main/java/com/xuanji/app/domain/game/GoSessionAdapter.kt`
- 创建：`app/src/main/java/com/xuanji/app/domain/game/ChessEngineAdapter.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/game/GoContractsTest.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/game/ChessEngineAdapterTest.kt`
- 修改：`docs/BOARD_GAME_INTEGRATION.md`

- [ ] **步骤 1：写 provider 缺失和协议边界测试**

围棋无 GTP provider 时必须返回 `go_provider_not_enabled`；国际象棋 adapter 只接受 UCI 局面，不得误用中国象棋坐标；两个 adapter 的错误码和取消语义与 `BoardEngine` 一致。

- [ ] **步骤 2：实现最小 adapter**

只实现协议 DTO、能力探测、取消和失败回退；没有真实 provider 时 UI 显示“该棋类尚未启用”，不显示模拟棋盘结果。接入 KataGo 或 Stockfish 时沿用同一 `BoardEngine` seam，并单独记录模型/许可证。

- [ ] **步骤 3：运行测试确认通过**

运行：`./gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.game.GoContractsTest" --tests "com.xuanji.app.domain.game.ChessEngineAdapterTest" --no-daemon --console=plain`

预期：PASS；中国象棋现有测试无回归。

- [ ] **步骤 4：提交**

```bash
git add app/src/main/java/com/xuanji/app/domain/game/GoContracts.kt app/src/main/java/com/xuanji/app/domain/game/GoSessionAdapter.kt app/src/main/java/com/xuanji/app/domain/game/ChessEngineAdapter.kt app/src/test/kotlin/com/xuanji/app/domain/game/GoContractsTest.kt app/src/test/kotlin/com/xuanji/app/domain/game/ChessEngineAdapterTest.kt docs/BOARD_GAME_INTEGRATION.md
git commit -m "feat: reserve go and chess engine seams"
```

## 任务 8：端到端验证、文档和交付门禁

**文件：**
- 修改：`README.md`
- 修改：`docs/SYSTEMS_OVERVIEW.md`
- 修改：`docs/TECHNICAL_DEBT.md`
- 修改：`docs/BOARD_GAME_INTEGRATION.md`
- 修改：`_dev/dialogue_contract.json`

- [ ] **步骤 1：运行静态和单元门禁**

运行：`./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon --console=plain`

预期：test PASS、lint 0 error、assembleDebug PASS。

- [ ] **步骤 2：执行小程序契约测试**

运行仓库现有的结构 lint 和 7 项引擎/题库测试命令，并额外执行 `_dev/dialogue_contract.json` 的游戏 golden matrix；结构 lint 与引擎测试分开报告。

- [ ] **步骤 3：执行可用设备检查但不冒充手机复测**

只有用户明确要求恢复设备验证时，才运行 `adb devices`、安装 APK、启动 `com.xuanji.app`、截图和 Logcat。没有设备时在报告中标记“未验证”，不以历史截图替代当前棋盘证据。

- [ ] **步骤 4：更新技术债台账**

记录中国象棋规则、离线应手、对话 grounding 和 UI 状态的完成项；将真实 Pikafish native 包、围棋 GTP/KataGo、国际象棋 Stockfish 和设备复测分别列为可验证状态，不把预留 seam 写成已联网或已启用能力。

- [ ] **步骤 5：提交最终文档变更**

```bash
git add README.md docs/SYSTEMS_OVERVIEW.md docs/TECHNICAL_DEBT.md docs/BOARD_GAME_INTEGRATION.md _dev/dialogue_contract.json
git commit -m "docs: document board game dialogue scope and verification"
```

---

## 验收矩阵

| 类别 | 必须通过的事实 |
| --- | --- |
| 规则 | 初始局面、所有中国象棋特殊规则、将军/将死和非法走法均有纯 Kotlin 测试 |
| 确定性 | 相同局面、颜色、难度和 seed 的离线应手完全一致 |
| 真实 grounding | 角色只引用棋局事件；不存在虚构棋子、胜率、胜负或跨域结论 |
| 会话 | persona/skin/topic 切换会丢弃旧 token；悔棋不会污染 redo/history |
| UI | 综合/东方/西方页都能从交流面板发起；浮球关闭/召回不丢棋局；键盘/TalkBack 可达 |
| 安全 | 空输入、超长输入、未知命令、医疗/投资话题不越界，棋局数据不写入虚构长期记忆 |
| 引擎 | Pikafish 不可用时自动回退离线规则应手；原生引擎未打包时 APK 仍可构建 |
| 双端 | `_dev/dialogue_contract.json` 的意图、事件、编码、token 规则和 golden wording 可被小程序测试读取 |
| 许可 | Pikafish/未来 Stockfish/KataGo 接入前，NOTICE、GPLv3 文本和 source offer 齐全 |
| 工程门禁 | `testDebugUnitTest`、`lintDebug`、`assembleDebug` 全部通过；设备证据按真实执行情况标记 |

## 实施顺序与停止条件

先完成任务 1–4，得到可独立测试的真实规则与对话 grounding；再完成任务 5，得到不依赖 native 的可用 UI；任务 6 只在许可证和构建工具链确认后执行；任务 7 不阻塞中国象棋交付。任何阶段若出现规则测试失败、旧对话回归、原生许可不完整或工作树中的用户成果会被覆盖，立即停止该阶段并保留当前提交，不使用破坏性 git 操作。
