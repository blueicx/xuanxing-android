# 棋局集成说明（BOARD_GAME_INTEGRATION）

> 更新日期：2026-09-01。本文档说明人物对话面板中真实棋局能力的运行模式、扩展接口、离线行为、开源许可边界与排障。

## 1. 当前支持范围

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| 中国象棋（Xiangqi） | **已启用** | 纯 Kotlin 规则核心 + 确定性离线应手，无网络、无引擎二进制依赖 |
| 围棋（Go） | 预留 | 无 GTP provider 时明确返回 `go_provider_not_enabled`，不显示模拟棋盘 |
| 国际象棋（Chess） | 预留 | UCI 契约已定义，无真实引擎时明确返回 `chess_provider_not_enabled` |
| Pikafish 原生引擎 | 预留 | UCI 协议 parser 已就绪；未打包 native 二进制，未启用 NDK/CMake |
| 在线 provider / 大模型 | 不支持 | 产品边界：棋局事实不交给外部模型 |

## 2. 运行模式

- **默认模式：离线规则 + 确定性应手**。`OfflineBoardEngine` 以 FNV-1a 稳定哈希从合法走法中选子；
  相同局面 + 颜色 + 难度 + token 必出同一手。UI 明确标注「离线应手」，不显示胜率或等级分。
- **引擎回退**：`PikafishEngine` 当前所有实例均以 `native_engine_not_packaged` 标记不可用，
  `bestMove` 全量回退到 `OfflineBoardEngine`。接入原生引擎后，任何 `bestmove` 仍须再次通过
  `XiangqiRules` 校验，非法即回退。
- **对话 grounding**：角色解说的棋子、坐标、吃子、将军、胜负一律读取 `BoardMove` / `RuleResult` /
  `GameOutcome`；运势数据不参与棋局结论（「运势好必赢」一类输入会被拒绝并引导回合法走法）。
- **保存边界**：「保存棋局」只保存局面与走法序列；角色评语不写入长期记忆。

## 3. 会话与 token 规则

- `GameSessionState` 携带 `sessionToken`；任何事件 token 不匹配即原样返回（旧异步回包丢弃）。
- persona / skin / topic 切换通过上层 `ChangeContext` 递增 token，等价取消进行中的棋局请求。
- `Undo` 一次回退一整回合（玩家 + 应手各一手），从初始局面重放重建，保证被吃子精确恢复。
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
| 提示看起来弱 | 离线应手为确定性降级实现，非强度引擎；原生引擎接入后自动增强 |
| 悔棋后悔再多手 | 每次悔棋回退一整回合；连按可连续回退 |
| 测试 | `.\gradlew.bat :app:testDebugUnitTest --console=plain`；契约：`node _dev\dialogue_contract_test.js` |
