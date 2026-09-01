# 交接文档：棋局对话 + 引擎增强（2026-09-01）

> 本文档交接 xuanji-android 棋局功能的最新状态。前一版交接见同目录（棋局 8 切片基础版）；本文覆盖其后两个增强提交。**凭据红线：对话中出现过的 GitHub 凭据一律 [REDACTED]，严禁写入任何文件、命令或提交。**

## 1. 当前完成状态

### 提交链（本分支 codex/system-consistency）

| Commit | 内容 |
|---|---|
| `beb20af` → `f269f55`（11 个） | 棋局基础版：规则核心 / reducer / 对话 grounding / Compose 棋盘 / Pikafish UCI seam / 围棋国际象棋预留 / 文档（详见前版交接文档） |
| `2eb1c9c` | 棋盘 UI 重构：传统棋盘线 + 楚河汉界 + 九宫斜点、选中/合法落点/上一步/将军脉冲高亮、吃子记录、触感反馈、`applySquareMove` 直点走子（绕过记谱解析，消除同 file 双车歧义）+ 5 项测试 |
| `aaeadda` | 引擎增强：SmartBoardEngine（alpha-beta 搜索，难度 easy/normal/hard = 深度 2/3/4）+ 开局书（当头炮）+ 三次重复判和 + 60 半回合无吃子限着（`GameOutcome.Draw`）+ `BoardAnalysis.threatsAgainst` 威胁提示 + `EndgameCatalog` 3 关残局 + 11 项测试 |

### 测试与门禁（真实执行结果，2026-09-01）

- **全量单测 158 项 / 0 失败**（`gradlew :app:testDebugUnitTest`，后台模式运行）
- **lintDebug 0 error**
- assembleDebug APK 生成正常（基础版时 23.8MB，增量后未重新量测）
- `node _dev/dialogue_contract_test.js` PASS；`_dev/authentic_systems_contract_test.js` PASS

## 2. 本轮新增的关键实现

### SmartBoardEngine（`domain/game/SmartBoardEngine.kt`）
- 根视角固定符号的 minimax + alpha-beta 剪枝，深度按难度 2/3/4；着法排序吃子优先
- 开局书：标准开局局面 encode 命中时红方走炮二平五 (7,7)→(4,7)，着法经 legalMoves 验证后才返回
- **终局防御**：`bestMove` 开头检查 outcome，终局局面（将死/困毙/和棋）直接返回 `NoMove("game_over")`——否则 reducer 会以 game_over 拒绝，契约失真
- 确定性：同局面+难度必出同手，无随机

### 规则层新增语义（`XiangqiRules.kt`）
- **禁止吃将着法**：`pseudoLegal` 新增 `allowKingCapture` 参数（默认 false），`legalMoves` 不再生成「吃对方将」的着法；`isGeneralAttacked` 传 `allowKingCapture = true` 保持打将检测语义。真实对局永远不该出现吃将（对手不会送将），但手工摆位 fixture 曾触发此洞
- **和棋**：`GameOutcome.Draw`（data object）；三次重复（按 encode 计数）+ 60 半回合无吃子；`apply` 对 Draw 局面拒绝后续着法（game_over）

### BoardAnalysis + EndgameCatalog（`domain/game/BoardAnalysis.kt`）
- `threatsAgainst(position, defender)`：列出 defender 每个正被攻击的子及攻击者，来自真实 legalMoves
- `pieceName(piece)`：棋子中文全名（domain 侧，避免依赖 ui 包的 XiangqiPieceGlyphs）
- `EndgameCatalog`：3 关残局（单车必胜/车破双士/空头炮），FEN-like 编码，测试验证引擎对下可解

### 排障沉淀（本次踩坑实录）
1. **手工摆位三要素**：将/帅必须各在九宫（file 3..5）、**两将不得同 file 无遮挡（将脸=非法局面）**、不要摆出「已被将军」的中间局面。多个测试失败全部源于此
2. FEN-like 编码每行数字和必须 = 9，共 10 行
3. `BoardMove.from` 是 `Square?`，直接 `incoming.from` 传入非空参数会类型不匹配，需局部变量收窄
4. PS 5.1 脚本里对原生工具（gradlew）不要设 `$ErrorActionPreference='Stop'`——stderr 警告会变成 terminating NativeCommandError；用 `$LASTEXITCODE` 判断
5. gradle 一律 background + `process wait`（前台曾 exit 130 被中断）
6. commit 后必须 `git status --short` 检查漏文件（历史上 activeGame 曾漏提）

## 3. 未完成 / 显式暂停（用户指示「后边任务暂时不做」）

以下为「全部完成」清单中尚未做的项，**未经用户指令不得自行恢复**：

- [ ] B 组 UI 接线：GameBoardCard 难度选择/回放控制/平移动画/reduced-motion 豁免/棋子字体；MysticGuideCard 自动应手（玩家执黑时引擎先走——**当前执黑仍是纯文案，引擎不会先手**）/续局存档（「保存棋局」仍是空承诺）/战绩统计/皮肤联动主题/音效
- [ ] C 组：androidTest 交互测试、`_dev/dialogue_contract.json` 扩充（威胁/和棋/残局话术）、文档同步（README / SYSTEMS_OVERVIEW / TECHNICAL_DEBT / BOARD_GAME_INTEGRATION 需补：搜索引擎、Draw 规则、威胁提示、残局、禁吃将语义）
- [ ] 设备实机验证（adb/安装/截图）——一直等用户明确通知
- [ ] Bridge 未接的部分：SmartBoardEngine 尚未替换 GameDialogueBridge 里的 OfflineBoardEngine 默认应手；威胁提示/残局/观战命令无对话入口（Bridge 的 engineReply/难度/残局命令未实现）

**最小接线建议**（下次开工）：`GameDialogueBridge` 里 `hint` 与引擎应手处把 `OfflineBoardEngine` 换成 `SmartBoardEngine(difficulty)`，难度作为 Bridge 构造参数或会话状态；outcome 分支加 `Draw` 文案「双方不变作和/无吃子限着判和」；`GameBoardUiModel.outcomeText` 同步加 Draw 文案（当前 UI 不认识 Draw，显示为无状态）。

## 4. 快速上手

```powershell
# 全量单测（必须后台跑）
.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain
# 统计
Get-ChildItem app\build\test-results\testDebugUnitTest -Filter "*.xml" | ForEach-Object { [xml]$x = Get-Content $_.FullName -Encoding UTF8; "{0}: {1}/{2} failed" -f $x.testsuite.name, $x.testsuite.failures, $x.testsuite.tests }
# lint
.\gradlew.bat :app:lintDebug --no-daemon --console=plain
Select-String -Path app\build\reports\lint-results-debug.xml -Pattern 'severity="Error"'
# 契约
node _dev\dialogue_contract_test.js
```

关键文件：规则 `XiangqiRules.kt`（364 行）、棋盘 `XiangqiBoard.kt`、引擎 `SmartBoardEngine.kt` / `GameEngine.kt`（旧离线应手，仍在用）、会话 `GameSessionState.kt`、对话 `GameDialogueBridge.kt`（290 行）、UI `ui/components/game/GameBoardCard.kt`（453 行）+ `MysticGuideCard.kt`（挂载点 ~L1259）、威胁/残局 `BoardAnalysis.kt`。

—— 交接完毕。分支 `codex/system-consistency`，最新 `aaeadda`。
