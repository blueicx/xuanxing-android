# X3 玄机：人物对话真实棋局交接文档

**交接日期：** 2026-09-01  
**项目目录：** `F:\huny\xuanji-android`  
**当前分支：** `codex/system-consistency`  
**交接范围：** 在人物交流面板中加入真实中国象棋，并为围棋/国际象棋保留可验证的扩展 seam。  
**当前状态：** 计划已完成，代码实现尚未开始。

## 1. 给下一位执行者的第一句话

先读取本文件和配套计划，再从任务 1 的失败测试开始。不要直接修改 `MysticGuideGenerator.kt`、`MysticGuideCard.kt` 或 `MysticFloatingGuide.kt` 的大段旧逻辑，也不要删除工作树中的截图、UI dump 或 `.superpowers` 资料。棋局必须成为独立的 domain 模块，角色回复只能读取真实棋局事件。

配套计划：[`2026-09-01-board-game-dialogue.md`](../plans/2026-09-01-board-game-dialogue.md)

## 2. 当前仓库基线

### Git 和工作树

- `HEAD`：`eb4c3bb docs: 标记同日生与角色优化计划完成`。
- 当前分支：`codex/system-consistency`。
- tracked 源码当前没有待提交修改。
- `git status --short` 当前有 28 项未跟踪内容：
  - `.superpowers/`：历史计划、视觉评审和过程资料，必须保留。
  - `device-*-20260831.xml`：此前设备 UI dump，必须保留，不能当作当前棋局功能证据。
  - `ui1.xml` 至 `ui15.xml`、`uiE.xml`、`uiW.xml`、`uiW2.xml`：历史 UI dump，必须保留。
  - `docs/superpowers/plans/2026-09-01-board-game-dialogue.md`：本功能实现计划，必须保留。
- 本轮没有删除文件，没有执行 `git reset --hard`、`git clean` 或覆盖用户产物。

### 最近已完成的 X3 能力

- `MysticDialogueEngine`、`MysticDialogueContinuity`、`MysticSessionState` 和 `DialogueProvider` 已存在。
- 默认离线 provider，不接在线模型、不写密钥。
- 对话已有 session/turn token，切换 persona、skin 或主题时应丢弃旧异步结果。
- 综合、东方、西方页均已挂载统一微光浮球和可召回舞台。
- 默认状态为微光浮球可见、完整人物舞台关闭；完全隐藏入口已存在。
- 同日生页面已有音乐/诗歌作品卡和评语折叠。
- `MysticCultureSpec` 已提供 8 个皮肤的文化道具和舞台场景。
- 对话已支持省略式追问承接最近主题，但棋局意图尚未加入。

### 最近一次质量证据

上一轮已记录 Android 以下门禁通过：

```text
./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon --console=plain
```

- 纯 Kotlin 单元测试：48 项通过，0 失败。
- `lintDebug`：0 error。
- `assembleDebug`：通过。
- Debug APK：`F:\huny\xuanji-android\app\build\outputs\apk\debug\app-debug.apk`。

这些是棋局代码开始前的历史基线。新增棋局代码后必须重新运行，不得把旧输出当作新功能证据。

### 设备验证边界

用户已明确要求手机复测等通知再做。本阶段不得主动运行 `adb devices`、安装 APK、截图或 Logcat。历史 AVD 证据只用于了解浮球/舞台现状，不能证明当前棋盘功能可用。

## 3. 已冻结的产品和架构决策

### 产品范围

1. “象棋”默认按中国象棋（Xiangqi）理解。
2. 第一交付必须支持真实合法走法、吃子、将军/将死、悔棋、提示和复盘摘要。
3. 角色解说只能基于当前棋盘、上一手和规则结果；不得根据运势分数声称“必胜”。
4. 规则错误、引擎超时、取消和 provider 缺失都必须可见且可测试。
5. 没有真实 provider 时，围棋和国际象棋只能显示“尚未启用”，不能返回假棋局。
6. 用户主动保存时只保存局面与走法，不把角色生成文案写成长期记忆。

### 技术范围

- 先实现纯 Kotlin 中国象棋规则，保证无 NDK 也能构建、测试和运行。
- 默认使用确定性离线应手，作为测试和原生引擎不可用时的降级实现。
- Pikafish 作为可选 UCI adapter；原生引擎阶段才启用 NDK/CMake 和 `arm64-v8a`。
- 围棋预留 GTP 契约；国际象棋预留 UCI 契约；两者不阻塞中国象棋交付。
- 不引入联网 provider，不让外部模型覆盖棋盘事实、命盘事实、健康边界或财务边界。

## 4. 目标数据流

```text
人物输入
  ↓
MysticIntentClassifier
  ↓
GameDialogueBridge
  ↓
GameSessionState + reduceGame
  ↓
BoardRules（中国象棋规则）
  ↓
BoardEngine（OfflineBoardEngine / PikafishEngine）
  ↓
GameEvent + GameDialogueResult(grounded=true)
  ↓
GameBoardCard + 角色解说
```

任何异步引擎结果都必须携带 `sessionToken`；token 不匹配时 reducer 原样返回当前状态。

## 5. 计划文件落点

### Domain

- `app/src/main/java/com/xuanji/app/domain/game/GameTypes.kt`
- `app/src/main/java/com/xuanji/app/domain/game/XiangqiBoard.kt`
- `app/src/main/java/com/xuanji/app/domain/game/XiangqiRules.kt`
- `app/src/main/java/com/xuanji/app/domain/game/XiangqiNotation.kt`
- `app/src/main/java/com/xuanji/app/domain/game/GameSessionState.kt`
- `app/src/main/java/com/xuanji/app/domain/game/GameEngine.kt`
- `app/src/main/java/com/xuanji/app/domain/game/GameDialogueBridge.kt`
- `app/src/main/java/com/xuanji/app/domain/game/GoContracts.kt`

### UI

- `app/src/main/java/com/xuanji/app/ui/components/game/GameBoardCard.kt`
- `app/src/main/java/com/xuanji/app/ui/components/game/XiangqiPieceGlyphs.kt`
- `app/src/main/java/com/xuanji/app/ui/components/MysticGuideCard.kt`
- `app/src/main/java/com/xuanji/app/ui/components/MysticFloatingGuide.kt`

### Native engine seam

- `app/src/main/cpp/CMakeLists.txt`
- `app/src/main/cpp/pikafish_bridge.cpp`
- `app/src/main/java/com/xuanji/app/domain/game/PikafishEngine.kt`
- `NOTICE-THIRD-PARTY.md`

### Tests and契约

- `app/src/test/kotlin/com/xuanji/app/domain/game/`
- `app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueGameIntentTest.kt`
- `_dev/dialogue_contract.json`

## 6. 执行顺序和检查点

### 检查点 A：规则核心

完成任务 1–2 后，必须证明：

- 初始局面含 32 子。
- 车、马、炮、象、士、将、兵规则均有测试。
- 蹩马腿、炮隔子、河界、九宫、将军和自杀将均被拒绝或正确判定。
- 局面编码稳定，走法可从中文 notation 往返解析。

### 检查点 B：会话和对话

完成任务 3–4 后，必须证明：

- 相同局面、颜色、难度和 seed 返回相同离线应手。
- persona/skin/topic 切换会丢弃旧引擎回复。
- “来一盘象棋”“走炮二平五”“悔棋”“提示”“复盘”“退出”均进入游戏路径。
- 普通问候、运势、健康和财务表达不被误判为棋局操作。
- 回复中的棋子、坐标、吃子和胜负全部来自状态对象或规则结果。

### 检查点 C：UI

完成任务 5 后，必须证明：

- 交流面板可打开棋盘卡片，棋盘为 9×10 网格。
- 非法目标格不可提交，合法目标格可提交。
- 浮球关闭/召回不重置棋局。
- 悔棋、提示、退出有 48dp 以上触控区域和 TalkBack 描述。
- reduced-motion 下无持续位移或旋转。

### 检查点 D：原生引擎

完成任务 6 后，必须证明：

- UCI parser 覆盖 `uciok`、`readyok`、`bestmove`、超时、取消和进程退出。
- `bestmove` 再次通过本地 `XiangqiRules` 校验。
- Pikafish 不可用时自动回退 `OfflineBoardEngine`。
- 构建不依赖未打包的引擎二进制。
- GPLv3 文本、版本、版权和完整源码获取方式已写入 NOTICE。

## 7. 当前风险和处理方式

| 风险 | 处理方式 |
| --- | --- |
| 现有三个 Compose 超大文件继续膨胀 | 新增 `domain/game` 和 `ui/components/game`，只在入口做薄接线 |
| 规则实现与引擎坐标不一致 | 统一 `Square`、局面编码和 `XiangqiNotation`，引擎回包再次过规则校验 |
| 异步旧结果污染新 persona/skin | 使用独立游戏 token，并复用现有 session token 失效策略 |
| 离线降级被误称为强引擎 | UI 明确标记“离线应手”，不显示未计算的胜率或等级 |
| GPL 依赖不完整 | 原生打包前完成 NOTICE、许可证和 source offer；否则只交付纯 Kotlin 版本 |
| 用户工作树成果被误删 | 任何清理前先列 exact path；本交接阶段不清理任何历史产物 |
| 手机验证被提前执行 | 遵守用户指示，设备验证留到明确通知后 |

## 8. 推荐的下一次提交切片

每个切片独立可回滚，且每次都运行相应测试：

1. `feat: freeze board game domain contracts`
2. `feat: add tested xiangqi rules core`
3. `feat: add deterministic game session reducer`
4. `feat: ground character dialogue in game state`
5. `feat: mount xiangqi board in companion dialogue`
6. `feat: add optional pikafish uci adapter`
7. `feat: reserve go and chess engine seams`
8. `docs: document board game dialogue scope and verification`

不要把 8 个切片压成一次大提交；每个提交后保留测试输出和失败原因。

## 9. 交付定义

中国象棋功能只有同时满足以下条件才可称为完成：

- Android `testDebugUnitTest`、`lintDebug`、`assembleDebug` 全部通过。
- 小程序结构 lint、现有 7 项引擎/题库测试和新增游戏 golden matrix 分开通过。
- 规则、会话、grounding、旧 token 丢弃和安全边界都有真实测试。
- 未启用的围棋、国际象棋、在线 provider 和未完成设备验证均明确标记。
- 当前设备证据在用户允许后重新采集，包含 `adb devices`、安装、启动、截图和 Logcat；没有设备时报告“未验证”。
- README、`docs/SYSTEMS_OVERVIEW.md`、`docs/TECHNICAL_DEBT.md` 和本交接文档保持一致。

## 10. 交接结束状态

本次交接只新增本文件，不代表棋局功能已经实现。下一位执行者应从计划任务 1 开始，先写失败测试，再实现最小规则模型；在规则核心和测试通过前，不接入 NDK、不改 UI、不运行手机复测。

