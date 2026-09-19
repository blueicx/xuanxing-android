# X3 玄机：角色讲棋与本机长期记忆交接文档

**交接日期：** 2026-09-02
**项目目录：** `F:\huny\xuanji-android`
**当前分支：** `codex/system-consistency`
**交接范围：** 角色能按规则解释盘面（为什么这步弱、哪个子白送、换个更稳的一手），以及可回忆、可见、可一键清除的本机长期记忆。
**当前状态：** 计划 7 步全部交付并通过本机门禁；**设备验证尚未开始，等待用户明确通知**。

配套计划：`shining-valley-mullet.md`（Qoder 会话内计划文件，不在仓库内）
上一轮交接：[`2026-09-01-board-game-dialogue-handoff.md`](./2026-09-01-board-game-dialogue-handoff.md)

## 1. 给下一位执行者的第一句话

本轮代码已完成，不要重做。先跑 `node _dev\dialogue_contract_test.js` 和
`.\gradlew.bat :app:testDebugUnitTest --console=plain -Pkotlin.compiler.execution.strategy=in-process`
确认基线仍然绿，再动下面第 7 节列出的任一切片。两条硬红线不得为了「显得更有用」而让步：
棋局回答不出现胜率/等级分/任何强度数字；长期记忆只存用户自己的话，不存角色生成文案。
工作树里的截图、UI dump、`.superpowers/` 与历史交接文档一律保留，不删除、不 `git add`。

## 2. 本轮已交付

### 讲棋（Slice 1，纯 `domain/game`）

- `domain/game/BoardExplanation.kt`：`exposed` / `critique` / `safest` 三个事实出口。
  回吃判定是「先把攻击方挪到目标格，再问 `XiangqiRules.legalMoves` 己方能否合法吃回」，
  因此被牵死的保护子不算有根，规则不产出的着法也不会被当成保护；将/帅永不被报成可吃子。
- `GameDialogueBridge` 新增两条棋局内命令：`WHY`「这步为什么不好」（优先评价玩家自己最后一手，
  看不出问题就明确说「按当前规则看不出问题」）与 `SAFER`「换个稳一点的走法」（扫描序取前 N 个
  合法着法，只比「走完后己方被盯住的子数」，平手取先出现的）。`THREATS` 与走子后评注改为
  区分「有子能回吃」与「没人能吃回，属于白送」；难度只决定话量（easy 无评注 / normal 一句 /
  hard 多报有根数），不改变事实。
- 引擎分值到不了文案：`SmartBoardEngine.evaluate` 仍是 `private`，契约脚本对此有断言；
  `BoardExplanation.kt` 内不出现 `evaluate`。

### 本机长期记忆（Slice 2）

- `domain/MysticRecollection.kt`：`RecollectionKind` 只有 `user_input` / `user_choice` /
  `settled_game_result` 三种，**生成文案在类型上无处可放**；`RecollectionCodec` 负责 NFC 清洗、
  码点截断、ISO 日期键、20 条上限、`dropped` 计数与损坏即诚实降级；`RecallFacts` 是召回文案的
  唯一输入，且不含用户原话。
- `data/local/PreferenceBridge.kt`（接口 + `DataStorePreferenceBridge(context)`）与
  `ConversationMemoryStore.kt`：key `talk_memory_<sha256(profileKey)>`，摘要显式使用 UTF-8。
  有了这个 seam，JVM 侧用内存假桥就能验证读写与清除，而不只验证 JSON 编解码。
- `MysticGuideGenerator.recallLine(mode, styleKey, facts)`：只有玄学家/半仙两族六个风格有召回句，
  空记忆返回空串，`unreadable` 说「读不出来，这次不引旧话」，`dropped` 追加清理数而不吞掉现存事实。
- `MysticGuideCard.kt`：进面板按档案载入；8 处用户动作写入（原话、面板输入、终局结算、handoff、
  开场选择、节奏选择、访客选择、棋局内选择、追问）；「现场手记」面板新增「本机长期记忆」区，
  列最近 3 条 + 总数与上限说明 + 「清除本机长期记忆」按钮（≥48dp、带 TalkBack 描述）。
  开场召回句在进面板时算一次，本次访问新记的东西不改写它；「重开对话」与切换 persona 只清会话状态。
- `GameRecord.settledNote(result)`：终局一行与战绩板共用同一张令牌表，没结算返回空串。

### 契约与文档

- `_dev/dialogue_contract.json` 新增 `explanation` 与 `conversation_memory` 两段，golden wording 22 → 26。
- `_dev/dialogue_contract_test.js` 改为「从源码解析出来再比对」：种类枚举、20 条上限、键前缀与
  邻居前缀互斥、召回措辞金句、`rememberLongTerm` 调用点不含生成文案、48dp 与 TalkBack 标签、
  `evaluate` 私有性、禁评分正则；`verify` 引用现在还要求点名一个仍然存在的测试方法。
- 文档同步：`BOARD_GAME_INTEGRATION.md` 新增 §2.3；`SYSTEMS_OVERVIEW.md` 新增「本机长期记忆」一节；
  `TECHNICAL_DEBT.md` 记录切片 5/6、边界与待办。

## 3. 提交切片（7 个，各自独立可回退）

```text
763bd4d feat: explain the board with rules-derived hanging-piece facts
694296b feat: let the character explain the board it really sees
d6f15d3 feat: keep a long-term recollection the user alone can write to
9961dca feat: put long-term recollection behind a testable preference seam
5e8bbea feat: voice stored recollection without adding a verdict
fabe847 feat: store and clear the companion's on-device recollection
774b329 docs: contract the explanation and recollection wording with tests
```

改动规模：19 个文件、约 2147 行新增（含测试与文档）。

## 4. 门禁与真实证据

| 命令 | 观察结果 |
| --- | --- |
| `:app:testDebugUnitTest` | BUILD SUCCESSFUL，264 项通过，0 失败 / 0 错误 / 0 跳过 |
| `:app:lintDebug` | 0 error，仅既有 `AutoboxingStateCreation` info 提示（非本轮引入） |
| `:app:assembleDebug` / `:app:assembleDebugAndroidTest` | 均成功，`compileDebugKotlin` 与 `compileDebugAndroidTestKotlin` 实际执行 |
| `node _dev\dialogue_contract_test.js` | `PASS (26 golden entries)`；反向验证过：给 `kinds` 加第四种即失败 |

测试套件构成：`BoardExplanationTest` 8、`GameExplanationTest` 17、`MysticRecollectionTest` 16、
`ConversationMemoryStoreTest` 9（全新四套），`MysticGuideGeneratorTest` 扩到 13，`GameArchiveTest` 新增 1。

**这些全部是本机 JVM / 编译证据。** 判定棋盘前提用的是「先构造局面再断言」（空盘摆子、
被牵死的車、能回吃的俥），不是假设；确定性用的是同局面重复回答与两个独立 bridge 实例字符串全等。

## 5. 明确未验证（不得当作已完成）

| 项 | 为什么 JVM 证不了 | 恢复设备验证时的最小动作 |
| --- | --- | --- |
| DataStore 真机往返与跨进程存活 | 假桥只证明读写与清除逻辑正确 | 记一条→杀进程→重开面板，确认开场召回句与列表都在 |
| 清除是否真的释放磁盘记录 | `delete` 被 fake 记账，非真实 preference 文件 | 清除后重启应用应回空态；必要时 `adb shell run-as` 查看 prefs |
| 召回句在气泡里的排布 | 无渲染证据 | 舞台与回访卡两处各截一张图，确认不挤压 `revisitLine` |
| 清除按钮 TalkBack 与 ≥48dp | 尺寸与语义只写在源码里 | TalkBack 逐项朗读顺序 + Layout Inspector 量命中区 |
| 困难档 `safest` 低端机耗时 | 扫描成本上限是设计值，未实测 | 困难档连续点「换个稳一点的」计时，Logcat 记录 |
| 12 项棋盘 Compose 交互用例 | `assembleDebugAndroidTest` 只证明可编译 | `connectedDebugAndroidTest` 跑一次 |

## 6. 本轮冻结的取舍（不要悄悄改回来）

- 不动 `BoardEngine` 接口：新增方法会牵动 `PikafishEngine` / `OfflineBoardEngine` seam。
- 不暴露 `pseudoLegal`，不做开局名与战术术语表（无来源即编造），不做 0–100 局面条。
- 不重构既有三个偏好键构造器；`talk_memory_` 与邻居前缀的互斥由存储测试与契约脚本共同守住。
- 长期记忆是 20 条滚动缓冲，只有整体清除；同 kind+日期+文本去重，因此它不是第二个战绩板。
- 不新增素材、不接厂商 SDK、不联网，因此本轮无 NOTICE 工作。

## 7. 建议的下一片（按优先级，一片一提交一门禁）

1. **设备验证复测**（当前唯一被用户挡住的项，需明确放行）：按第 5 节动作逐项采集证据，
   更新 `TECHNICAL_DEBT.md` 的「未验证」条目，不拿旧截图替代。
2. **persona 命名漂移收口**：同两个模式在卡面叫「玄学家/半仙」、浮球舞台叫「慈翁/魔师」、
   TalkBack 里还有第三种「玄师」，且浮球动画速度靠字符串比较 `"魔师"` 分支。
   收口方式：一个枚举 + 一份 label 表，动画按枚举分支。门禁：
   `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`。
3. **记忆逐条删除**：面板列表已按条渲染，补「只删这一条」与撤销，仍走 `PreferenceBridge` seam，
   契约段同步 `conversation_memory`。
4. **解释的下一步深化**：仅在能继续用规则事实支撑的前提下扩展（例如一次回看两手的成因链），
   不做任何需要引擎评分才能成立的表述。
5. 明确不在近程计划内：落子音效（仓库无音频资源）、字体捆绑（缺许可核验）、在线 provider、
   Pikafish 原生包（GPLv3 NOTICE 与 source offer 未就绪）。

## 8. 环境与工作树备忘

- 本机 Gradle 必须带 `-Pkotlin.compiler.execution.strategy=in-process`，否则编译进程会被杀掉；
  构建日志写到 `/tmp`，不要写进仓库（会改变未跟踪产物数量）。
- `git status --short` 基线：29 项未跟踪，本轮前后不变 —— `.superpowers/`（1）、
  `device-*-20260831.xml`（8）、`ui*.xml`（18，含 `uiE/uiW/uiW2`）、上一轮计划（1）与交接文档（1）。
  本文件是本轮新写并单独按路径提交的，不占这 29 项。
- 提交一律按显式路径 `git add`，禁止 `git add -A`；提交后核对未跟踪数量。

## 9. 交付定义（接手时同样适用）

一项能力只有同时具备源码、能证伪它的测试、以及可复现命令输出，才可称为完成。
运行时与设备类结论必须有真机证据；没有设备时报告「未验证」，不以编译通过或旧截图替代。
