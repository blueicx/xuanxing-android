# 棋局集成说明（BOARD_GAME_INTEGRATION）

> 更新日期：2026-09-01。本文档说明人物对话面板中真实棋局能力的运行模式、扩展接口、离线行为、开源许可边界与排障。

## 1. 当前支持范围

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| 中国象棋（Xiangqi） | **已启用** | 纯 Kotlin 规则核心 + 本地 alpha-beta 搜索引擎，无网络、无引擎二进制依赖 |
| 围棋（Go） | 预留 | 无 GTP provider 时明确返回 `go_provider_not_enabled`，不显示模拟棋盘 |
| 国际象棋（Chess） | 预留 | UCI 契约已定义，无真实引擎时明确返回 `chess_provider_not_enabled` |
| Pikafish 原生引擎 | 预留 | UCI 协议 parser 已就绪；未打包 native 二进制，未启用 NDK/CMake |
| 在线 provider / 大模型 | 不支持 | 产品边界：棋局事实不交给外部模型 |

## 2. 运行模式

- **默认引擎：`SmartBoardEngine`**。纯 Kotlin alpha-beta 搜索，真实博弈树展开 `XiangqiRules`，
  难度决定搜索深度（轻松 2 层 / 普通 3 层 / 困难 4 层），评估为子力价值 + 位置加成，
  红方开局走法命中内置开局库，搜索跑在 `Dispatchers.Default`。完全离线且确定性：
  相同局面 + 颜色 + 难度必出同一手。不显示胜率、等级分或任何强度评级。
- **应手回退**：`OfflineBoardEngine`（FNV-1a 稳定哈希选子）不再是默认应手，只保留为
  `PikafishEngine` 的降级回退和单元测试注入的确定性接缝。
- **原生引擎回退**：`PikafishEngine` 当前所有实例均以 `native_engine_not_packaged` 标记不可用，
  `bestMove` 全量回退。接入原生引擎后，任何 `bestmove` 仍须再次通过
  `XiangqiRules` 校验，非法即回退。
- **已结束局面不再应手**：引擎对将死 / 困毙 / 和棋返回 `EngineResult.NoMove("game_over")`，
  reducer 同样拒绝这类走法，双向保证不会出现「赢了还在走」。
- **对话 grounding**：角色解说的棋子、坐标、吃子、将军、胜负一律读取 `BoardMove` / `RuleResult` /
  `GameOutcome`；运势数据不参与棋局结论（「运势好必赢」一类输入会被拒绝并引导回合法走法）。
- **UI 状态文案**：只有「引擎推演中···」与「回放第 n/m 手」两种状态提示，不出现强度类措辞。
- **动效豁免**：落子滑动动画由 `rememberReducedMotion()` 控制，它读取
  `Settings.Global.ANIMATOR_DURATION_SCALE`，系统把动画时长缩放设为 0 时直接落位、不做滑动。

## 2.1 对话指令清单

| 指令 | 触发语 | 说明 |
| --- | --- | --- |
| 开局 | 「来一盘象棋」「陪我下象棋」 | 支持「我执黑」直接换色 |
| 难度 | 「难一点」「换个难度」「困难」 | `SmartBoardEngine.parseLabel` 映射，未知措辞只提示不改动 |
| 颜色 / 观战 | 「我执黑」「我执红」「观战」「替我下」 | 观战 = `playerColor = WHITE`，双方均由引擎应手 |
| 走子 | 「走炮二平五」「马8进7」/ 点格子 | 中文纵线记法与 UCI 均可 |
| 合法性询问 | 「马八进七这步能走吗」 | 只查规则，不消耗行棋权 |
| 威胁 | 「有哪些威胁」「会被吃吗」 | `BoardAnalysis.threatsAgainst` 逐子扫描 |
| 提示 / 复盘 | 「给我提示」「复盘刚才那步」 | 提示为真实合法走法 + 搜索层数，非强度评级 |
| 悔棋 / 重做 | 「悔棋」「重做」 | 悔棋回退一整回合；重做沿 `redo` 列表逐手回放 |
| 残局 | 「来局残局」 | `EndgameCatalog` 3 则已验证可胜局面 |
| 保存 / 继续 | 「保存棋局」「继续棋局」 | 见 §2.2 |
| 战绩 | 「战绩」「比分」 | 只读已结算结果 |
| 退出 | 「退出棋局」 | 重置会话 |

## 2.2 续局存档与战绩

- **编码**：`GameSave(version, start, moves, playerColor, difficulty, title, savedAt)`。
  `start` 用 `XiangqiBoard.encode` 的 FEN-like，`moves` 用 UCI 半回合序列。
  选 UCI 而非中文记法，是因为同纵线双车 / 双马的中文着法本身有歧义，无法无损回放。
- **恢复即校验**：`GameArchive.restore` 不做任何乐观假设，逐手回放到
  `reduceGame(GameEvent.ApplyMove)`，吃子、将军、判和计数与胜负全部重算；
  遇到第一条被规则拒绝的半回合即停，回退到最后一个合法局面并回报
  `dropped = moves.size - index`（对话中显示「n 手未通过规则校验」）。
- **失效输入降级**：版本号不符、`start` 无法解码、将军缺失 / 双将对面 → `Rejected(reason)`，
  会话保持原样；未知难度字符串统一回落到「普通」。
- **战绩只记已结算对局**：`GameRecord(wins, losses, draws, lastResult)` 仅在
  `settledResult` 返回非空时累加，且以 `sessionToken` 去重，同一局重复结算只记一次。
- **存储边界**：「保存棋局」只保存局面与走法序列（`GameArchive` 内不出现角色、评语或运势字段）；
  角色评语不写入长期记忆。数据落在共享 `preferencesDataStore("xuanji_prefs")`，
  key 为 `game_save_<sha256(profileKey)>` / `game_record_<sha256(profileKey)>`，
  `profileKey` 由档案生辰信息派生，因此换档案不会读到别人的棋局。
- **桥接层不碰 Android**：`GameDialogueBridge` 只在 `Result.archive` 上标 `SAVE` / `RESUME`，
  真正的 DataStore 读写由 Compose 侧完成；写失败回复「本机存储不可用，这一局没能保存。」

## 3. 会话与 token 规则

- `GameSessionState` 携带 `sessionToken`；任何事件 token 不匹配即原样返回（旧异步回包丢弃）。
- persona / skin / topic 切换通过上层 `ChangeContext` 递增 token，等价取消进行中的棋局请求。
- `Undo` 一次回退一整回合（玩家 + 应手各一手），从初始局面重放重建，保证被吃子精确恢复。
- `Redo` 沿 `redo` 列表逐手回放；任何新走法都会清空重做栈，避免分叉。
- **判和是会话级规则**：`XiangqiRules.outcome` 只看当前局面，永远不会返回和棋；
  三次重复局面（`positionLog` 计数 >= 3）与连续 60 个无吃子半回合（`NO_CAPTURE_LIMIT`）
  由 `GameSessionState.drawReason()` 返回 `"repetition"` / `"no_capture_limit"`，
  对话措辞分别为「和棋（双方不变作和）」与「和棋（无吃子限着判和）」。
- **自动应手**：玩家走完后 `shouldAskEngine` 为真 → `Result.awaitEngine`，UI 在同一协程里
  串接引擎回包，直到轮回玩家或对局结束；观战模式下双方均由引擎走。引擎回包用第一人称
  （「我走「炮二平五」」），与玩家着法可区分。
- 浮球关闭 / 召回、综合/东方/西方页切换不重建棋局，仅复用同一 `GameSessionState`。

## 4. 坐标与编码

- `Square(file, rank)`：`file = 0..8`，`rank = 0..9`；rank 0 黑方底线，rank 9 红方底线。
- FEN-like：黑方顶行到底行、大写红 / 小写黑、行棋方后缀 ` r` / ` b`。初始局面：
  `rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR r`
- UCI：`file 字母 a..i`（file 0 = a）+ 行数字 `'9' - rank`（rank 9 → '0'）。
- 中文着法：红方纵线 九…一（file 0 = 九），黑方 1..9；马/相/仕 进退记目标纵线；
  同线双子用 前/后 前缀并省略起始纵线。

## 5. 许可与 source offer（接入原生引擎前必读）

- Pikafish 采用 **GPLv3**。打包其源码或二进制之前，必须在本目录旁边提供：
  1. `NOTICE-THIRD-PARTY.md` 中写明版本号、上游地址、修改说明；
  2. GPLv3 完整文本位置；
  3. 对应完整源码的获取方式（source offer）。
- 当前交付**不含**任何 Pikafish 源码/二进制，因此无需附 GPLv3 文本；
  release 检查不得将无许可文件的原生引擎打进 APK。
- 未来接入 Stockfish（GPLv3）/ KataGo（各版本许可不同）时按同一流程单独记录。

## 6. 排障

| 现象 | 处理 |
| --- | --- |
| 说「来一盘象棋」无反应 | 检查输入是否被引号/表情打断；先说「退出棋局」再重开 |
| 着法无识别 | 用「走炮二平五」纵线记法；或点棋盘格子直接走子 |
| 提示看起来弱 | 默认搜索深度按难度 2/3/4 层，说「换个难度，困难」可提高层数；本机性能不足时应手会变慢但仍确定性 |
| 引擎长时间「推演中」 | 困难=4 层搜索，低端机上耗时较长；可切回「普通」，token 不匹配的旧回包会被丢弃 |
| 悔棋后悔再多手 | 每次悔棋回退一整回合；连按可连续回退，之后可用「重做」逐手回放 |
| 「继续棋局」说没有存档 | 存档按档案 key 分仓，换档案后读不到上一档案的棋局；先在原档案里「保存棋局」 |
| 恢复后提示「n 手未通过规则校验」 | 存档尾部被截断或篡改，已回退到最后合法一手；从该手继续下即可，不会静默丢弃整局 |
| 保存后没有声音/落子音效 | **未实现**：当前未打包音频资源，属已知范围限制，不是开关被关掉 |
| 棋子字形 | 只用系统字体渲染 `XiangqiPieceGlyphs.glyph` 的单个汉字，未捆绑字体，也没有缺字时的可视回退；`description` 仅作为 TalkBack 内容描述 |
| 测试 | `.\gradlew.bat :app:testDebugUnitTest --console=plain`；契约：`node _dev\dialogue_contract_test.js`（含棋盘 UI 定位符与状态文案交叉校验）；棋盘交互用例在 `app/src/androidTest`，`assembleDebugAndroidTest` 只证明可编译，需接上设备跑 `connectedDebugAndroidTest` 才算运行时证据（当前未执行），其前提由 `BoardUiFixtureTest` 在 JVM 侧钉住 |
