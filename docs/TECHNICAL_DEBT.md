# X3 玄机技术债台账

本台账记录全量审计后仍值得继续处理、但不应阻塞离线版本交付的工程债务。现有未提交源码、截图、UI dump 和脚本均视为用户成果，本台账不要求清理它们。

## 当前基线

- Android 门禁：`testDebugUnitTest`、`lintDebug`、`assembleDebug` 已建立并在最近一轮通过；lint 当前无 error，剩余主要是既有 unused 参数/变量和 SDK XML 版本提示。
- CI（`.github/workflows/build.yml`，2026-09-02 才可用）：此前 `Build Debug APK` 在 `eb4c3bb`/`887e1ee`/`7dc3b52` 连续失败且 Gradle 从未启动，因此**没有任何历史 CI 证据可引用**。两个根因已分别修复：`1dbe42f` 去掉 `sdkmanager` 中不存在的包 `build-tools;34.0`（真实包为 `34.0.0`，未知包会使该步骤退出码 1）；`457dc7a` 为 `.gitignore` 的笼统 `*.jar` 添加 `!gradle/wrapper/gradle-wrapper.jar` 例外并入库 wrapper jar——在此之前仓库从未 tracked 任何 jar，任何 fresh clone 运行 `./gradlew` 都会 `ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain`，这不止是 CI 问题。`457dc7a` 的运行结果为 pass（7m20s，`BUILD SUCCESSFUL in 6m 41s`，产出 `xuanxing-debug-apk`）。2026-09-02 本分支推到 `f89f2f1` 后再跑一次（run #17）：同样 pass，`BUILD SUCCESSFUL in 6m 30s`，`52 actionable tasks: 52 executed`（无一项来自缓存），`testDebugUnitTest` / `assembleDebug` / `lintDebug` 三个任务都在干净 runner 上真实执行并成功，APK 已上传。注意两点边界：Gradle 成功日志不打印逐条测试数，因此各轮记的「N 项通过 / 0 跳过」仍是本机计数；workflow 里没有 `node` 步骤，`_dev/dialogue_contract_test.js` 至今没在 CI 跑过一次。CI 覆盖范围仅 `testDebugUnitTest` + `lintDebug` + `assembleDebug`；instrumented `app/src/androidTest`、真机与 DataStore 运行时行为不在其中，仍按下列条目视为未验证。
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
  7. 称谓收口：`MysticGuideGenerator.personaName(mode)` 成为双面角色对外称谓的唯一来源（scholar→「玄学家」、half→「半仙」），「玄师」退回为这套陪伴功能的统称（舞台关闭键、会话气泡、设置项标题），浮球舞台那套「慈翁 / 魔师」全部退役；`MysticOrb` 的漂移周期与幅度改为按 `half` 分支，不再比较显示字符串，`MysticImmersiveStage` 中从未被读取的 `roleName` 形参删除；`identityAnswer` 六条自我介绍改为每条只自称一次（原先「半仙，街口半仙」这类重复是把「魔师」换成「半仙」后才会显形，已并入作风名）。契约新增 `persona` 段：逐行扫描 `app/src/main/java/com/xuanji/app` 下的 `.kt` 断言退役名 0 命中（测试源码只在断言里把它们当反例列出，不在扫描范围内），并钉住 `personaName` 的实现、浮球按模式分支的写法、六条自我介绍与统称「玄师」。
  8. `styleKeyFor` 回归：该函数自 `f3ad6a8`（2026-08-24，「feat: add mystic opening check-in」）起返回 `style(...).second`，即作风**显示名**「档案室学者」而非作风**键** `archive`；下游一律按键匹配，因此有两处可见后果——`customAnswer` 里所有 `when (styleKey)` 永远落到 `else`（学者只会说「慢速罗盘」的话、半仙只会说「云端的实习生」的话，而卡面标题却按 `generate()` 的正确键显示「档案室学者」等六种作风），以及 `MysticGuideCard` 两处 `guide.styleKey == styleKeyFor(...)` 守卫永远为假、签到与节奏选择后的延后应答每次都被静默丢弃。已改为返回 `.first`。
  证据：`:app:testDebugUnitTest` 266 项通过（新增 `personaName_labels_the_two_modes_and_never_the_umbrella`、`identity_answers_name_the_persona_exactly_once`；后者遍历 5 组盘面 × 7 个话题 × 2 个模式，断言六种作风全部可达且每条身份回答只自称一次）、`:app:lintDebug` 0 error、`:app:assembleDebug` 重新产出 `app-debug.apk`、`node _dev/dialogue_contract_test.js` PASS。拆成两个提交后，只有单测（`--rerun` 强制重跑）与契约脚本在最终树上重新执行过，lint 与 APK 是同内容拆分前那次门禁的产物；`styleKeyFor` 那一笔没有在隔离状态下单独跑过测试——它相对最终树只少了称谓改名，而既有断言与契约都没有钉过受影响的那几句文案，因此判断为安全，但这是推理而非观测。`assembleDebugAndroidTest` 判定 UP-TO-DATE，但 `compileDebugAndroidTestKotlin` 已针对改动后的 main 源码重新编译通过——12 项棋盘交互用例仍未在任何设备或 CI 上执行。**以上均为本机 JVM / 静态扫描 / 编译证据**：实机上浮球与舞台的称谓渲染、TalkBack 实际播报、换装标签在窄屏是否截断，以及签到 / 节奏延后应答修复后是否真的出现在界面上，都未验证。
  同类残留已收口（见下方「当前基线」第 10 条）：`TodayOracle` 曾以显示名作查询键——`oracleRole` 返回「玄学家 / 半仙」，`when (role)`、`if (firstRole == "玄学家")` 与 12 条对照表都依赖这两个字面量。称谓改名不会像浮球动画那样静默走错分支（`when` 不匹配会落到 `else`），但那是「落进另一张嘴的口吻」，比走错分支更难发现，因此改为按稳定的 role 键分支。

- 医疗/财务红线与契约去空转（2026-09-02）：一个切片。
  9. `MysticSafetyGuard`（纯 Kotlin，无 Android 依赖）接在 `customAnswer` 的返回处：原先 `return when (intent) { … }` 改为先收成 `draft`，再由 `enforce(mode, question, variant, draft)` 决定整条回复。命中「领域词 + 结论词」成对出现时**整句换成拒答**（两个模式 × 两个领域 × 两个变体 = 8 句，各带本领域免责句），只命中领域词时保留原回答并在句尾补一次免责句、已有则不重复。变体由既有私有 `customPulse(question) % 2L` 选出，该 hash 始终非负，因此同一问题永远得到同一句。守卫自带词表而不复用 `MysticIntentClassifier`：分类器按主题分流，「我该吃什么药」因句中的「吃」落到 `Daily`（`MysticIntentClassifier.kt:61`），Health 的 needle 里从来没有「药」——红线不能押在路由运气上。成对命中也保证「最近睡眠不好」这类陈述不会被当成问诊。
  契约同时被去掉三处空转：`requireVerify` 以前只要求写出测试**文件名**且用 `.includes()` 比对方法名（截断的方法名前缀也能通过），现在强制 `Suite.case` 两级并用 `\bfun\s+case\b` 校验真实方法；10 处只写了 suite 的 verify 全部补成具体用例；`golden_wording` 从 26 扩到 37，补上此前完全无契约约束的 `继续` / `那工作怎么办` / `重开对话` / 纯空白输入 / `你是谁？` 与四条红线用例，`everyday_guards` 每条改为逐字钉住测试里的 `assertEquals(MysticIntent.X, classify("input"))` 断言。这一笔当时还多写了一句假话：把「超长输入」算进了新增项，而 37 条里只有纯空白那一条，超长路径一条都没有——该缺口在下一轮（第 10 条）补齐，现为 45 条。顺带修掉两处既有假话：契约原写「车厘子好吃吗 → chat」，实际分类器返回 `Daily`，`MysticDialogueGameIntentTest` 里那句 `assertTrue(cherry != MysticIntent.Game)` 已换成对 `Daily` 的确切断言，改坏任一侧都会失败。空输入与超长输入按用户决定**维持现状**（reducer 里 trim / 200 字符截断 / 纯空白丢弃），没有新增界面文案。
  证据：`:app:testDebugUnitTest --rerun` 278 项通过（0 失败 0 跳过，较上一轮 +12 = `MysticSafetyGuardTest` 11 项 + `MysticDialogueEngineTest.custom_answer_routes_every_question_through_the_safety_guard` 1 项）、`:app:lintDebug` 0 errors / 81 warnings（均为既有）、`:app:assembleDebug` 重新产出 `app-debug.apk`、`:app:assembleDebugAndroidTest` 的 `compileDebugAndroidTestKotlin` 针对改动后的 main 源码重新编译通过、`node _dev/dialogue_contract_test.js` PASS（37 条 golden）。五项都在最终树上跑过。为确认新断言不是空转做过两次反证：把 `customAnswer` 临时改回直接 `return when (intent)`，结果只有那一条接线用例失败（10 项中 1 failed），恢复后全绿；把一处 verify 的方法名截成前缀（`…_round_trip` 对应实际的 `…_round_trips`），旧脚本按 `.includes()` 放行、新脚本按预期报错。本切片已随分支推送到 `f89f2f1`，PR #1 的 run #17 覆盖了第 9、10 两条所在的树：`testDebugUnitTest` / `lintDebug` / `assembleDebug` 在干净 Linux runner 上全部真实执行且 job pass（`52 actionable tasks: 52 executed`，无缓存）。但 278 项这个计数与 `node _dev/dialogue_contract_test.js` 仍只有本机证据——CI 日志不打印逐条测试数，workflow 里也没有 node 步骤。**以上均为本机 JVM / 静态扫描 / 编译证据**：8 句拒答与免责声明在实机气泡里的换行、窄屏截断与 TalkBack 播报，以及两种作风下拒答语气的实际观感，均未验证（已列入契约 `safety.unverified`）。

- 今日灵签改按稳定键分支（2026-09-02）：一个切片。
  10. `TodayOracle` 不再用显示名当查询键：新增 `OracleTier { High, Mid, Low }` 与 `OracleRole(modeKey) { Scholar("scholar"), Half("half") }`，`oracleRole(draw)` 返回枚举，对外称谓取 `role.label = MysticGuideGenerator.personaName(modeKey)`，因此 `roleName` 仍是同一个字符串、界面取首字的头像和「某某离席」那句都不必改。`private fun oracleTier(String)` 删除，12 条签面在 `OraclePoem` 表上逐条声明自己的 tier，`TIER_BY_LEVEL` 由该表 `associate` 得出；`tier` 只做 `OracleResult` 的计算属性（getter，无 backing field）——这个对象经 `TodayOracleCache` 用 Gson 写进 DataStore，多一个构造字段会让当天已缓存的旧 JSON 反序列化出 null，Kotlin 默认值不生效，等于静默改掉用户正在看的那支签。`when (role)`、`when (role to choiceKey)`、三段 relay、两张 tier×role 表和 `if (firstRole == "玄学家")`（改为 `firstRole.other`）全部按枚举重排；四个公开函数签名与 `observerRelayGate` 的 7 个哈希输入未动，relay 的触发时机因此不变。
  行为差异要写清：旧 `oracleTier` 是 `else -> "low"`，任何没见过的 level 都静默当低签；现在 `TIER_BY_LEVEL.getValue(level)` 会抛 `NoSuchElementException`。全仓 `level` 只可能取签库里那 5 个值，所以唯一可达路径是「同一天内签库被换掉」这种构建期不一致——换来的是不再悄悄换成另一张嘴的口吻。回归矩阵此前缺的类别一并补上（37→45）：问候 / 感谢 / 告别 / 情绪各一条，超长输入两条（对话侧 200 字截断、棋局侧超长命令），换上下文两条（迟到的旧 token 回复整条作废、请求在飞时切 persona 回 Idle 并追加系统行）——上一轮那句把「超长输入」算进新增项的话由此变成事实。
  证据：`:app:testDebugUnitTest --rerun` 284 项通过（0 失败 0 跳过，较上一轮 +6 = `TodayOracleTest` 全量）、`:app:lintDebug` 0 errors / 81 warnings（均为既有）、`:app:assembleDebug` 重新产出 `app-debug.apk`（23,845,578 字节）、`:app:compileDebugAndroidTestKotlin` 针对改动后的 main 源码重新编译通过但 `packageDebugAndroidTest` 判定 UP-TO-DATE，磁盘上的 androidTest APK 仍是 01:07 那一份、`node _dev/dialogue_contract_test.js` PASS（45 条 golden）。四项都在最终树上跑过。文案不变做过机械核对：把两版文件里含标点的中文字面量取 multiset 比对，78 条完全一致，消失的只有 24×`"玄学家"`、14×`"半仙"`、7×`"high"`、7×`"mid"`、3×`"low"` 与 `oracleTier` 里的 3 个 level 名，新增只有 `"scholar"` 与 `"half"`。新门禁也做过反证：往 `TodayOracle.kt` 塞回一行 `private val PROBE_BANNED = "玄学家"`，脚本按预期以 `TodayOracle must never key a branch on "玄学家": …` 退出 1，删掉后恢复 PASS。路上真失败过两次，都记在这里：`the_cached_oracle_shape_is_unchanged` 首跑失败，因为 Compose 的 `@StabilityInferred` 会给 data class 加一个 `public static final int $stable`（javap flags 0x0019，并未标 ACC_SYNTHETIC），断言改成按 Gson 自己的口径过滤 static / transient；另有 `assertNull` 漏 import、以及 `Field.isTransient` 在本模块不能作为属性访问，改用 `Modifier.isTransient(...)`。**以上计数与静态扫描均为本机证据**；gradle 三项另在覆盖这一笔的干净 runner 上真实执行过一次（run #17，详见第 9 条同段），而 `_dev/dialogue_contract_test.js` 至今不在 CI 里。契约 `today_oracle.unverified` 记的仍是设备面：灵签页实际渲染、两种口吻在气泡里的排布与 TalkBack 播报，以及「同一天内签库变化」这个抛错窗口在真机上无从观察。

## P1：继续拆分大文件

| 文件 | 当前规模 | 已完成 | 下一步 |
| --- | ---: | --- | --- |
| `MysticGuideGenerator.kt` | 3468 行 | 对话 seam、intent classifier、医疗/财务红线（`MysticSafetyGuard.kt`，109 行）均已抽离 | 按 intent 把模板与语气表拆成纯 Kotlin 文件，保持确定性 hash 与 `customAnswer` 唯一 `enforce` 调用点不变 |
| `MysticGuideCard.kt` | 2719 行 | provider/session 接入；输入栏与快捷问题已抽到 `MysticConversationPanel.kt`（130 行，卡面两处调用点复用同一 `submitPanelInput`） | 继续拆会话气泡与卡面主体渲染，先保持参数和状态提升方式不变 |
| `MysticFloatingGuide.kt` | 2683 行 | `MysticOrb.kt` 已拆出；舞台仍在原文件 | 将舞台外壳与人物绘制分开；人物绘制 helper 需继续保持同一 skin/mood 输入 |

拆分规则：一次只移动一个稳定边界；不改变公共 API、资源 ID、角色 seed 或默认离线行为；每次移动后必须跑编译、单测、lint 和 debug assemble。

## P1：对话回归矩阵

继续扩充 Android 与小程序的 golden wording，至少保持以下类别：问候、感谢/告别、身份、闲聊、情绪、健康、财务、连续追问、换主题/角色/皮肤、空输入与超长输入。Android 侧这些类别现已全部落到 `_dev/dialogue_contract.json` 的 45 条 golden 里，每条都指到一个真实执行过的 `Suite.case`；其中「换皮肤 / 换 persona」两条记的是会话状态事件而非用户输入，且换皮肤只钉到 reducer 层（旧回复作废、请求回 Idle），没有钉到界面渲染出的新风格；小程序侧的 golden 本轮未重新核对。

红线的真实边界（本轮收口，见「当前基线」第 9 条）：`MysticGuideGenerator.customAnswer` 是唯一随用户输入变化的应答生产者，它的 `when (intent)` 结果现在统一过 `MysticSafetyGuard.enforce`（`MysticGuideGenerator.kt:2818`，全仓唯一调用点）；开场签到、本机召回句与过渡语是固定模板，不经这道关——它们本来也不回答用户的提问。因此「所有回复都过 safety guard」这句旧描述不准确，准确的表述是：**任何由用户文本决定的回复都必须过 `enforce`，模板句由各自的 golden 约束**。空输入与超长输入维持 reducer 现有行为（trim、200 字符截断、纯空白丢弃），不再假设界面上会出现新的提示文案。

交流面板已新增统一的消息/请求状态控件，后续只需继续扩充 golden wording，不再为每个入口维护独立的输入状态。

persona 命名漂移（已收口，见「当前基线」第 7 条）：同两个模式曾分别在 `MysticGuideCard.kt` 叫「玄学家 / 半仙」、在 `MysticFloatingGuide.kt` 叫「慈翁 / 魔师」，`MysticOrb.kt` 的 TalkBack 里还有第三种「玄师」，且浮球动画速度靠字符串比较 `"魔师"` 分支。现在称谓只有 `MysticGuideGenerator.personaName` 一个来源，「玄师」固定为统称，动画按模式分支，退役名由 `_dev/dialogue_contract_test.js` 逐行扫描 main 源码树（`app/src/main/java/com/xuanji/app`）把关。同源问题的最后一处 `TodayOracle` 也已收口（见第 10 条），契约 `today_oracle` 段现直接禁止这些写法：`TodayOracle.kt` 任何一行出现被引号包住的 `"玄学家"` / `"半仙"` / `"high"` / `"mid"` / `"low"`、重新出现 `private fun oracleTier(`、或给 `OracleResult` 加了第七个持久化字段以外的构造参数——任一情况都会让契约脚本失败。

## P2：可访问性与运动偏好

- 浮球保持 52dp 视觉尺寸和可点击语义；继续用 UI dump 或 TalkBack 实机检查焦点顺序。
- reduced-motion 已关闭浮球位移和持续旋转；后续检查完整舞台呼吸动画、键盘导航和旋转/返回键状态恢复。
- 复测 safe area：浮球不遮挡分数卡、底部导航和系统手势区。
- 本机长期记忆面板：召回句在气泡里的排布、「清除本机长期记忆」的焦点顺序与实机 48dp 命中区、清除后是否回到空态，均只有源码级证据；恢复设备验证时按 TalkBack 顺序复测，不以截图替代。
- 医疗/财务拒答与免责句：8 句拒答（两种作风 × 两个领域 × 两个变体）在气泡里的换行、窄屏是否截断、TalkBack 是否完整念出免责句，均只有 JVM 断言；恢复设备时逐句复测，抽查一句不能代替全部 8 句。
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
