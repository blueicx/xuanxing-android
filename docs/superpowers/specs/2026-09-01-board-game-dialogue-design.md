# 2026-09-01 人物对话真实棋局：接口、交互与安全决策

> 本文件在实现开始前冻结。后续变更必须先修改本文件并说明理由，再动代码。

## 1. 接口冻结

### 1.1 值对象（`domain/game/GameTypes.kt`）

- `GameType { XIANGQI, CHESS, GO }`：棋种类别。第一交付只实现 XIANGQI。
- `PlayerColor { RED, BLACK, WHITE }`：RED/BLACK 用于中国象棋，WHITE 预留给国际象棋。
- `Square(file, rank)`：`file = 0..8`（列），`rank = 0..9`（行）；`rank 0` 为黑方底线，`rank 9` 为红方底线。UI 不直接操作数组下标。
- `BoardMove(from, to, notation, captured?, player)`：`from` 可为空（预留虚着/ pass），`notation` 为中文纵线着法（如 `炮二平五`）。
- `EngineEvaluation(centipawns?, mateIn?, depth)` 与 `EngineTurn(move, evaluation?)`：仅真实引擎填充；离线应手必须为 `evaluation = null`，UI 不显示胜率。
- `GoPosition` / `GoPlaceMove` / `GoRulesAvailability`：围棋只保留契约。无 GTP provider 时所有查询返回 `unavailable("go_provider_not_enabled")`，禁止伪造落子、胜负或形势判断。

### 1.2 规则与引擎 seam（`XiangqiRules` / `GameEngine.kt`）

- `BoardRules.legalMoves(position, color)`：先生成伪合法走法，再应用走法并拒绝己方被将军状态。
- `BoardRules.apply(position, move)` 返回 `RuleResult.Applied | Rejected`；错误码固定为
  `from_empty`、`wrong_turn`、`illegal_move`、`self_check`、`game_over`。UI 与对话共用同一错误码集合。
- `BoardRules.outcome(position)` 返回 `GameOutcome`；`isIllegalPosition` 只用于“双将对脸”等非法局面判定。
- `BoardEngine.bestMove(position, color, token)`：挂起函数；任何实现（离线/ Pikafish/ 未来 GTP、UCI）
  必须携带 `token`，且返回的走法必须能再次通过本地 `XiangqiRules` 校验。

### 1.3 会话（`GameSessionState` / `reduceGame`）

- `GameSessionState(sessionToken, gameType, position, history, redo, request, outcome)`。
- 事件：`Start / ApplyMove / EngineReply / Undo / Cancel / Exit`。
- token 不匹配时 reducer 原样返回当前状态（丢弃旧异步回包）。
- `Undo` 一次回退一整回合（玩家+引擎各一手），`redo` 保存被回退的走法；新走法清空 `redo`。
- `Exit` 重置为初始空会话；上层 persona/skin/topic 切换通过 `ChangeContext` 递增 token，等价于丢弃棋局异步请求。

## 2. 交互决策

1. “象棋”默认按中国象棋（Xiangqi）理解；围棋、国际象棋在 UI 中显示“尚未启用”，不显示模拟棋盘。
2. 交流面板输入“来一盘象棋 / 走炮二平五 / 悔棋 / 提示 / 复盘 / 退出”进入棋局路径；
   问候、运势、健康、财务表达优先级高于棋局误判词（如“车”不单独触发）。
3. 棋盘卡片固定 9×10 网格；点击己方棋子后只显示合法落子提示；非法目标格不可提交。
4. 悔棋、提示、退出按钮 ≥ 48dp，带 `contentDescription`（撤销上一手 / 请求提示 / 退出棋局）。
5. 浮球关闭/召回与跨页（综合/东方/西方）切换不重建棋局，只复用同一会话 token。
6. reduced-motion 下棋盘无持续位移或旋转动画。

## 3. 安全与 grounding 边界

1. 只有 `grounded=true` 的 `GameDialogueResult` 可以写入角色消息流。
2. 回复中的棋子、坐标、吃子、将军、胜负一律读取 `BoardMove` / `RuleResult` / `GameOutcome`，禁止模板编造。
3. 跨域结论（如“运势好所以必胜”）一律改写为“局面信息不足，先看合法走法”。
4. persona 只改变语气与称呼，不改变棋局事实。
5. 用户主动保存棋局时只保存局面与走法序列；角色评价不写入长期记忆。
6. 不引入联网 provider；外部模型不得覆盖棋盘事实、命盘事实、健康边界或财务边界。

## 4. 交付与验证边界

- 纯 Kotlin 规则核心不依赖 Android `Context`、Compose 或引擎进程。
- 默认离线确定性应手；Pikafish 仅作为可选 UCI adapter，未打包原生引擎时 APK 必须可构建。
- 设备验证（adb/安装/截图/Logcat）等用户明确通知后执行；无设备时报告“未验证”。
- 文档基线：README、`docs/SYSTEMS_OVERVIEW.md`、`docs/TECHNICAL_DEBT.md`、`docs/BOARD_GAME_INTEGRATION.md` 与本规格保持一致。
