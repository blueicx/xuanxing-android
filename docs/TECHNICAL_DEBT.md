# X3 玄机技术债台账

本台账记录全量审计后仍值得继续处理、但不应阻塞离线版本交付的工程债务。现有未提交源码、截图、UI dump 和脚本均视为用户成果，本台账不要求清理它们。

## 当前基线

- Android 门禁：`testDebugUnitTest`、`lintDebug`、`assembleDebug` 已建立并在最近一轮通过；lint 当前无 error，剩余主要是既有 unused 参数/变量和 SDK XML 版本提示。
- CI（`.github/workflows/build.yml`，2026-09-02 才可用）：此前 `Build Debug APK` 在 `eb4c3bb`/`887e1ee`/`7dc3b52` 连续失败且 Gradle 从未启动，因此**没有任何历史 CI 证据可引用**。两个根因已分别修复：`1dbe42f` 去掉 `sdkmanager` 中不存在的包 `build-tools;34.0`（真实包为 `34.0.0`，未知包会使该步骤退出码 1）；`457dc7a` 为 `.gitignore` 的笼统 `*.jar` 添加 `!gradle/wrapper/gradle-wrapper.jar` 例外并入库 wrapper jar——在此之前仓库从未 tracked 任何 jar，任何 fresh clone 运行 `./gradlew` 都会 `ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain`，这不止是 CI 问题。`457dc7a` 的运行结果为 pass（7m20s，`BUILD SUCCESSFUL in 6m 41s`，产出 `xuanxing-debug-apk`）。CI 覆盖范围仅 `testDebugUnitTest` + `lintDebug` + `assembleDebug`；instrumented `app/src/androidTest`、真机与 DataStore 运行时行为不在其中，仍按下列条目视为未验证。
- Android 测试：纯 Kotlin domain/generator 测试已存在，覆盖对话分类、确定性、离线 provider、会话 token、生成器空输入与棋局规则 / 引擎 / 存档 / 解释事实，以及本机长期记忆的编解码与存储（`PreferenceBridge` 用内存假桥，不需 Robolectric 即可验证读写与清除）；`app/src/androidTest` 源码集已建立（棋盘 12 项 Compose 交互用例），能被 `assembleDebugAndroidTest` 编译，但**未在设备或 CI 上执行**。
- 小程序：结构 lint 与 7 项引擎/题库测试已分开执行并通过；双端契约位于 `_dev/dialogue_contract.json`。
- 设备证据：曾完成 `com.xuanji.app` AVD 安装、启动、综合/东方/西方浮球、召回舞台和关闭回浮球截图，证据保存在 `.superpowers/round46-*`。没有把当前无在线设备误报为实体机验证。
- 同日生增强：`SameDayWorks` 已加入确定性音乐/诗歌卡，`HistoryCopy` 与 `AnimatedVisibility` 支持长评语折叠；后续可继续扩充经过版权核验的作品元数据。
- B+C 视觉：`MysticCultureSpec` 已为 8 个皮肤提供结构化道具和舞台场景；后续仍需设备上检查人物比例、遮挡和不同屏幕密度的视觉细节。
- 对话承接：`MysticDialogueContinuity` 已让省略式追问继承最近主题；后续应继续扩充中英文标点、连续 5 轮、换 persona/皮肤和跨端 golden wording。
- 棋局功能（2026-09-01）：四个切片已交付并通过门禁。
  1. 引擎与对话：`SmartBoardEngine`（alpha-beta，难度 2/3/4 层 + 开局库）成为默认应手，走子后自动串接引擎回包，新增难度切换、换色 / 观战、威胁扫描、残局目录、重做与和棋措辞。
  2. 棋盘 UI：难度选择、回放控件、落子滑动动画（系统动画时长为 0 时跳过）、吃子记录与 TalkBack 描述。
  3. 持久化：「保存棋局 / 继续棋局 / 战绩」经 `GameArchive` + `GameArchiveStore` 落 DataStore，恢复时逐手过规则校验并回报被丢弃的尾部手数，战绩按 session token 去重且只记已结算对局；存档不含角色评语。
  4. 交互测试：新增 `app/src/androidTest` 源码集与 Compose UI-Test 依赖，`GameBoardCardTest` 用坐标 `testTag` 驱动选子 / 落子 / 取消 / 难度 / 回放 / 思考锁 / 吃子记录 12 项用例；其前提由 `BoardUiFixtureTest` 在 JVM 上钉住，`_dev/dialogue_contract.json` 新增 `board_ui` 段交叉校验定位符与状态文案。
  证据：`:app:testDebugUnitTest` 201 项通过（其中棋局相关 153 项）、`:app:lintDebug` 0 error、`:app:assembleDebug` 与 `:app:assembleDebugAndroidTest` 均产出 APK、`node _dev/dialogue_contract_test.js` PASS（22 条 golden wording，含 UI 定位符交叉校验，改坏一处状态文案即失败）。**以上均为本机 JVM/编译证据，12 项棋盘交互用例未在设备执行。**
  Pikafish UCI 协议 parser 与显式降级 seam 已交付。

- 棋局解释与陪伴记忆（2026-09-02）：两个切片已交付并通过门禁。
  5. 讲棋：`BoardExplanation` 用 `XiangqiRules.legalMoves` 做回吃判定（先把攻击方挪到目标格再问能否合法吃回），`GameDialogueBridge` 新增 `WHY`「这步为什么不好」与 `SAFER`「换个稳一点的走法」，威胁报告与走子后评注改为区分「有子能回吃」与「没人能吃回，属于白送」，难度只决定话量；契约新增 `explanation` 段，扫描措辞禁词并断言 `SmartBoardEngine.evaluate` 仍为 `private`。
  6. 本机长期记忆：`RecollectionCodec`（三种 `RecollectionKind`、20 条上限、`dropped` 计数、损坏即诚实降级）+ `PreferenceBridge` seam + `ConversationMemoryStore`（key `talk_memory_<sha256>`，UTF-8 摘要）+ `MysticGuideGenerator.recallLine` + `MysticGuideCard` 接线（按档案载入、8 处用户动作写入、现场手记面板列表与「清除本机长期记忆」按钮）。契约新增 `conversation_memory` 段，把种类枚举、上限、键前缀互斥、召回措辞与 48dp/TalkBack 全部对齐源码。
  证据：`:app:testDebugUnitTest` 264 项通过（0 失败 0 跳过）、`:app:lintDebug` 0 error（仅既有 `AutoboxingStateCreation` info 提示）、`:app:assembleDebug` 与 `:app:assembleDebugAndroidTest` 均成功、`node _dev/dialogue_contract_test.js` PASS（26 条 golden wording）。其中 `testDebugUnitTest` / `lintDebug` / `assembleDebug` 已在 `457dc7a` 的干净 Linux runner 上重跑通过（见上方 CI 条目），`assembleDebugAndroidTest` 与契约脚本仍只有本机证据。二者都只是 JVM/编译证据：DataStore 真机往返与跨进程存活、清除是否真的释放磁盘记录、召回句在气泡里的排布、清除按钮的实机触摸目标与 TalkBack 播报、困难档 `safest` 在低端机上的耗时，均未验证。

- 身份称谓与作风键（2026-09-02）：两个独立提交。
  7. 称谓收口：`MysticGuideGenerator.personaName(mode)` 成为双面角色对外称谓的唯一来源（scholar→「玄学家」、half→「半仙」），「玄师」退回为这套陪伴功能的统称（舞台关闭键、会话气泡、设置项标题），浮球舞台那套「慈翁 / 魔师」全部退役；`MysticOrb` 的漂移周期与幅度改为按 `half` 分支，不再比较显示字符串，`MysticImmersiveStage` 中从未被读取的 `roleName` 形参删除；`identityAnswer` 六条自我介绍改为每条只自称一次（原先「半仙，街口半仙」这类重复是把「魔师」换成「半仙」后才会显形，已并入作风名）。契约新增 `persona` 段：全量扫描 `app/src` 下所有 `.kt` 断言退役名 0 命中，并钉住 `personaName` 的实现、浮球按模式分支的写法、六条自我介绍与统称「玄师」。
  8. `styleKeyFor` 回归：该函数自 `f3ad6a8`（2026-08-24，「feat: add mystic opening check-in」）起返回 `style(...).second`，即作风**显示名**「档案室学者」而非作风**键** `archive`；下游一律按键匹配，因此有两处可见后果——`customAnswer` 里所有 `when (styleKey)` 永远落到 `else`（学者只会说「慢速罗盘」的话、半仙只会说「云端的实习生」的话，而卡面标题却按 `generate()` 的正确键显示「档案室学者」等六种作风），以及 `MysticGuideCard` 两处 `guide.styleKey == styleKeyFor(...)` 守卫永远为假、签到与节奏选择后的延后应答每次都被静默丢弃。已改为返回 `.first`。
  证据：`:app:testDebugUnitTest` 266 项通过（新增 `personaName_labels_the_two_modes_and_never_the_umbrella`、`identity_answers_name_the_persona_exactly_once`；后者遍历 5 组盘面 × 7 个话题 × 2 个模式，断言六种作风全部可达且每条身份回答只自称一次）、`:app:lintDebug` 0 error、`:app:assembleDebug` 重新产出 `app-debug.apk`、`node _dev/dialogue_contract_test.js` PASS。`assembleDebugAndroidTest` 判定 UP-TO-DATE，但 `compileDebugAndroidTestKotlin` 已针对改动后的 main 源码重新编译通过——12 项棋盘交互用例仍未在任何设备或 CI 上执行。**以上均为本机 JVM / 静态扫描 / 编译证据**：实机上浮球与舞台的称谓渲染、TalkBack 实际播报、换装标签在窄屏是否截断，以及签到 / 节奏延后应答修复后是否真的出现在界面上，都未验证。
  同类残留（未在本轮处理）：`TodayOracle` 仍以显示名作查询键——`oracleRole` 返回「玄学家 / 半仙」，`when (role)`、`if (firstRole == "玄学家")` 与 12 条对照表都依赖这两个字面量；称谓改名不会像浮球动画那样静默走错分支（`when` 不匹配会落到 `else`），但仍属「行为依赖显示字符串」，若要收口应改为稳定的 role 键。

## P1：继续拆分大文件

| 文件 | 当前规模（约） | 已完成 | 下一步 |
| --- | ---: | --- | --- |
| `MysticGuideGenerator.kt` | 3.4k 行 | 对话 seam、intent classifier 已抽离 | 将模板选择与安全约束抽成纯 Kotlin 文件，保持确定性 hash 不变 |
| `MysticGuideCard.kt` | 2.4k 行 | provider/session 接入 | 将输入栏/快捷问题和会话渲染拆成独立 composable 文件，先保持参数和状态提升方式不变 |
| `MysticFloatingGuide.kt` | 2.6k 行 | `MysticOrb.kt` 已拆出；舞台仍在原文件 | 将舞台外壳与人物绘制分开；人物绘制 helper 需继续保持同一 skin/mood 输入 |

拆分规则：一次只移动一个稳定边界；不改变公共 API、资源 ID、角色 seed 或默认离线行为；每次移动后必须跑编译、单测、lint 和 debug assemble。

## P1：对话回归矩阵

继续扩充 Android 与小程序的 golden wording，至少保持以下类别：问候、感谢/告别、身份、闲聊、情绪、健康、财务、连续追问、换主题/角色/皮肤、空输入与超长输入。所有回复必须经过本地 persona/safety guard，不能伪造命盘事实或给出医疗/投资结论。

交流面板已新增统一的消息/请求状态控件，后续只需继续扩充 golden wording，不再为每个入口维护独立的输入状态。

persona 命名漂移（已收口，见「当前基线」第 7 条）：同两个模式曾分别在 `MysticGuideCard.kt` 叫「玄学家 / 半仙」、在 `MysticFloatingGuide.kt` 叫「慈翁 / 魔师」，`MysticOrb.kt` 的 TalkBack 里还有第三种「玄师」，且浮球动画速度靠字符串比较 `"魔师"` 分支。现在称谓只有 `MysticGuideGenerator.personaName` 一个来源，「玄师」固定为统称，动画按模式分支，退役名由 `_dev/dialogue_contract_test.js` 全量扫描 `app/src` 把关。仍开放的同源问题：`TodayOracle` 用显示名当查询键。

## P2：可访问性与运动偏好

- 浮球保持 52dp 视觉尺寸和可点击语义；继续用 UI dump 或 TalkBack 实机检查焦点顺序。
- reduced-motion 已关闭浮球位移和持续旋转；后续检查完整舞台呼吸动画、键盘导航和旋转/返回键状态恢复。
- 复测 safe area：浮球不遮挡分数卡、底部导航和系统手势区。
- 本机长期记忆面板：召回句在气泡里的排布、「清除本机长期记忆」的焦点顺序与实机 48dp 命中区、清除后是否回到空态，均只有源码级证据；恢复设备验证时按 TalkBack 顺序复测，不以截图替代。
- 本轮手机复测暂缓，恢复时优先覆盖同日生折叠按钮的 TalkBack 标签、作品卡阅读顺序和舞台文化场景的对比度。

## P2：Provider seam

`DialogueProvider` 当前只提供离线实现。在线 provider 仅允许作为未来 seam：需要显式同意、超时、取消、失败回退和本地安全复核；本轮不接厂商、不写入密钥、不改变默认离线行为。

## P2：棋局引擎与扩展（真实状态）

- **Pikafish 原生包**：UCI 行协议 parser（`UciProtocolParser`）与降级 seam（`PikafishEngine` → `OfflineBoardEngine`）已完成并有 11 项协议测试；NDK/CMake、`arm64-v8a` 构建与引擎二进制**未打包**。接入前置条件：GPLv3 NOTICE、许可文本与 source offer 齐全（见 `NOTICE-THIRD-PARTY.md`），接入后 `bestmove` 仍须过 `XiangqiRules` 校验。
- **围棋 GTP/KataGo**：仅有 `GoSessionAdapter` 契约（无 provider 返回 `go_provider_not_enabled`）；未实现规则、未接引擎、未显示模拟棋盘。
- **国际象棋 Stockfish**：仅有 `ChessEngineAdapter` 契约（返回 `chess_provider_not_enabled`）与 FEN 起始局面值对象；未实现规则。
- **落子音效**：**未实现**。仓库无音频资源，未接 `SoundPool`，也不引入在线素材；需要设计确认音源与许可后再补。
- **棋子字形**：直接用系统字体渲染 `XiangqiPieceGlyphs.glyph` 的单个汉字，未捆绑字体资源，也没有缺字时的可视回退——`description` 只作为 TalkBack 内容描述，不参与绘制。若需保证跨机型一致，需要引入开源中文字体子集并核对许可。
- **引擎强度上限**：`SmartBoardEngine` 只有 alpha-beta + 简单子力/位置评估，无置换表、无静态搜索（quiescence）、无迭代加深，深度上限 4 层；horizon effect 与末端漏算未解决，不宣称任何等级分。
- **UI 交互测试执行**：`app/src/androidTest` 源码集与依赖已建立，`assembleDebugAndroidTest` 只能证明可编译；12 项棋盘用例尚未在任何设备或 CI 上跑过一次，真实点击结果、焦点顺序与动画豁免仍需运行时证据。
- **解释的边界**：`safest` 只按「走完后己方被盯住的子数」挑一手，看一步、无静态搜索、候选数按难度封顶（6/14/40），低端机耗时未实测；不做开局名与战术术语表（无来源即编造），不显示任何强度数字。
- **记忆的边界**：本机长期记忆是 20 条滚动缓冲，只有整体清除一个动作，没有逐条删除与导出；同 kind+日期+文本会去重，因此同一天重复的终局结果只记一次——它不是第二个战绩板。
- **设备复测**：棋盘 UI（浮球关闭/召回不丢局、TalkBack、reduced-motion）尚无实机证据；恢复设备验证时需重新采集 `adb devices`、安装、启动、截图与 Logcat，不以旧截图替代。

## P1：真实语料与官方常模接入条件

- Ifá Ese 经文、纳迪叶脉原文：代码已提供 Provider、来源、许可证和置信度字段；在取得传承方/持有者授权与可追溯数据前，应用只能显示名称/索引或“离线模拟”。
- MMPI、Raven、16PF、MBTI：官方题目、计分键、手册和常模属于授权材料；当前仅开放 IPIP Big Five-50 可实际计分，未配置常模时不显示官方百分位。
- 农历：1900–2100 表驱动换算已内置并有闰月 golden case；若需要更早/更晚年份，应新增经核验历表版本，而不是外推。

## 完成定义

一项债务只有在有源码变更、对应测试和可复现命令输出时才标记完成。截图、旧日志或“代码看起来已接线”不能替代运行时证据。
