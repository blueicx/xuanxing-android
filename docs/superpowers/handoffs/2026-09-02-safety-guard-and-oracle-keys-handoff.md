# X3 玄机：对话红线与灵签键化交接文档

**交接日期：** 2026-09-02
**项目目录：** `F:\huny\xuanji-android`
**当前分支：** `codex/system-consistency`
**交接范围：** 两片同属一类问题——门禁给的是假信心。`fb17087` 立起医疗/财务红线并掏空契约里的空转断言；`bef5fbb` 把 `TodayOracle` 的分支依据从显示名换成稳定键。
**当前状态：** 两笔已提交并随分支推送（`f26c876..f89f2f1`）；本机四项门禁全绿，干净 runner 上 PR #1 的 run #17 也 pass。但契约脚本仍不在 CI 内（见第 5 节），设备面全部未验证。

## 1. 给下一位执行者的第一句话

先读 `docs/TECHNICAL_DEBT.md`「当前基线」第 9、10 条，那两段区分了「观测到的」和「推理出来的」。这轮暴露的规律是：**一个只检查文件存在、或只断言某几个词没出现的门禁，比没有门禁更危险**。下一片最该动的不是功能，是把契约脚本接进 CI（第 5 节）。不要在这轮里夹带设备验证、在线 provider、第三方语料或棋局引擎。

## 2. 本轮已交付

### 红线与安全网（`fb17087`）

- 新增 `domain/MysticSafetyGuard.kt`（109 行，纯 Kotlin，无 `android.*` import，因此 JVM 可测）。公开面：`domainOf` / `verdictDomainOf` / `disclaimerFor` / `refusal(mode, domain, variant)` / `enforce(mode, question, variant, draft)`，以及 `FORBIDDEN` 词表与两域免责句。
- 接线只有一处：`MysticGuideGenerator.kt:2818`，`customAnswer` 的返回处。原 `return when (intent) { … }` 先收成 `draft`，再由 `enforce` 决定整条回复。**这是全仓唯一随用户文本变化的应答生产者**——开场签到、本机召回句、转场语是模板，不经此路，也不回答用户提问。
- 规则：领域词与结论词**成对**命中才整句换成拒答（2 模式 × 2 领域 × 2 变体 = 8 句，各带本域免责句）；只命中领域词则保留原答案并在句尾补一次免责句，已有不重复。变体由既有私有 `customPulse(question) % 2L` 选出，该 hash 始终非负，同一问题永远同一句。
- 守卫自带词表，**不复用** `MysticIntentClassifier`：「我该吃什么药」因句中的「吃」落到 `Daily`（`MysticIntentClassifier.kt:61`），Health 的 needle 里从来没有「药」。红线不能押在路由运气上——这是本轮最实质的一处判断。
- 免责句复用应用内既有措辞（`IChing.kt:74` 的「不构成医疗建议…」「不构成投资建议…」）。刻意避开「不能替代医学诊断」那句，因为 `诊断` 二字早已是 `MysticDialogueEngineTest.kt:114` 的禁词。
- 新增 `MysticSafetyGuardTest.kt`（163 行 / 11 项）与 `MysticDialogueEngineTest.custom_answer_routes_every_question_through_the_safety_guard`。
- 契约去空转：`requireVerify` 以前只要求写出测试**文件名**并用 `.includes()` 比对方法名（截断前缀也能过），现在强制 `Suite.case` 两级 + `\bfun\s+case\b` 校验真实方法；10 条只写了 suite 的 golden 全部落到真实用例。新增 `safety` 段。`golden_wording` 26 → 37。
- `everyday_guards` 从「只查条数」变成逐条核对：js 用正则读出分类器各分支实际返回值来验 `expectIntent`；顺带修掉契约里的假话「车厘子好吃吗 → chat」，实际是 `Daily`，测试里那句 `assertTrue(cherry != Game)` 换成对 `Daily` 的确切断言，改坏任一侧都失败。

### 灵签键化（`bef5fbb`）

- `OracleTier { High, Mid, Low }` 与 `OracleRole(modeKey) { Scholar("scholar"), Half("half") }`；对外称谓取 `role.label = MysticGuideGenerator.personaName(modeKey)`，所以 `roleName` 仍是同一个字符串，界面取首字的头像与「某某离席」那句都不必改。
- `private fun oracleTier(String)` 删除；12 条签面在 `OraclePoem` 表上各自声明 tier，`TIER_BY_LEVEL` 由该表 `associate` 得出。`tier` 只做计算属性（getter，无 backing field）。
- 为什么不能加字段：`OracleResult` 经 `TodayOracleCache` 用 Gson 写进 DataStore，多一个构造参数会让当天已缓存的旧 JSON 反序列化成 null（Kotlin 默认值不生效），等于静默改掉用户正在看的那支签。`the_cached_oracle_shape_is_unchanged` 专门兜这条。
- 文案一字未动，且是机械核对过的：两版文件里含标点的中文字面量取 multiset 比对，78 条完全一致；消失的只有 24×`"玄学家"`、14×`"半仙"`、7×`"high"`、7×`"mid"`、3×`"low"` 和 `oracleTier` 里的 3 个 level 名，新增只有 `"scholar"` / `"half"`。
- 行为差异（刻意）：旧 `oracleTier` 是 `else -> "low"`，没见过的 level 静默当低签；现在 `TIER_BY_LEVEL.getValue(level)` 抛 `NoSuchElementException`。全仓 `level` 只可能取签库那 5 个值，唯一可达路径是「同一天内签库被换掉」这种构建期不一致。
- 契约新增 `today_oracle` 段：`TodayOracle.kt` 任何一行出现被引号包住的 `"玄学家"` / `"半仙"` / `"high"` / `"mid"` / `"low"`、重新出现 `private fun oracleTier(`、或 `OracleResult` 构造函数多出第七个持久化字段，都直接失败；另断言 relay 门的哈希输入仍不含 tier、签面 12 条不变。
- golden 37 → 45：补上上一轮欠的问候 / 感谢 / 告别 / 情绪 / 超长输入（对话侧与棋局侧各一条）/ 换上下文（迟到回复作废、请求在飞时切 persona）六条，全部指向真实执行过的用例。

## 3. 门禁与真实证据（都在最终树上跑过）

- `node _dev/dialogue_contract_test.js` → PASS，45 条 golden。
- `./gradlew :app:testDebugUnitTest --rerun -Pkotlin.compiler.execution.strategy=in-process` → exit 0，284 项 / 0 失败 / 0 跳过（266 → 278 → 284）。
- `./gradlew :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` → BUILD SUCCESSFUL，lint 0 errors / 81 warnings（均既有），`app-debug.apk` 重新产出（23,845,578 B）。`compileDebugAndroidTestKotlin` 针对改动后的 main 重新编译通过，但 `packageDebugAndroidTest` 判定 UP-TO-DATE，磁盘上的 androidTest APK 仍是旧的。
- 两次反证，都真失败过：把 `customAnswer` 临时改回直接 `return when (intent)`，只有那条接线用例失败（10 项中 1 failed）；往 `TodayOracle.kt` 塞回一行 `private val PROBE_BANNED = "玄学家"`，脚本以 `TodayOracle must never key a branch on "玄学家": …` 退出 1。还原后恢复全绿。
- 推送后 CI：run #17（`Assemble debug APK`，干净 Linux runner，head `f89f2f1`）**pass**，`BUILD SUCCESSFUL in 6m 30s`、`52 actionable tasks: 52 executed`（无缓存复用），`testDebugUnitTest` / `assembleDebug` / `lintDebug` 均真实执行，APK 已上传。边界：Gradle 成功日志不打印逐条测试数，所以 284 / 0 / 0 仍是本机计数；workflow 里没有 node 步骤，契约脚本依旧没在 CI 跑过。
- 路上真实踩到的两个坑，别重复：`the_cached_oracle_shape_is_unchanged` 首跑失败，因为 Compose 的 `@StabilityInferred` 会给 data class 加一个 `public static final int $stable`（javap flags 0x0019，**未**标 ACC_SYNTHETIC），断言必须按 Gson 自己的口径过滤 static / transient；另外 `Field.isTransient` 在本模块不能作为属性访问，要用 `Modifier.isTransient(...)`。

## 4. 明确未验证（不得当作已完成）

- 8 句拒答与两域免责句在气泡里的换行、窄屏截断、TalkBack 实际播报——只有 JVM 断言，界面上一眼都没看过。抽查一句不能代替全部 8 句。
- 灵签页在两种口吻下的实际渲染，以及「同一天内签库变化」这个抛错窗口在真机上是否可观察。
- 12 项棋盘交互 androidTest 用例从未在设备或 CI 上执行。
- 契约脚本本身从未在 CI 上执行过（第 5 节）——它现在只是本机门禁。
- 上一轮写进账本的一句假话已就地改正并留痕：第 9 条曾把「超长输入」算进新增 golden，实际那 37 条里只有纯空白一条。

设备类工作按用户要求暂缓，恢复时先补第 4 节。

## 5. 最大的已知缺口

`.github/workflows/build.yml:39` 只跑 `./gradlew testDebugUnitTest lintDebug assembleDebug`，**没有 `node _dev/dialogue_contract_test.js`**。也就是说本轮新立的最强门禁（契约对 `customAnswer` 函数体断言 `enforce` 存在、逐行扫描显示名、核对每条 golden 的用例真实存在）在远端一次都没执行过，绕过它不会被 CI 拦住。下一片第一优先是加一个 node 步骤（`actions/setup-node` + `node _dev/dialogue_contract_test.js`），并确认它失败时真的能让 job 变红。

## 6. 本轮冻结的取舍（不要悄悄改回来）

- 空输入与 >200 字输入**维持现状**（reducer 里 trim / 200 字符截断 / 纯空白丢弃），用户已明确选择「只把契约补真，不加界面文案」。契约与测试钉的是「现在就是这样」。
- 红线只在 `customAnswer` 一处接线，模板句由各自的 golden 约束，不给它们套守卫。
- 「玄师」= 功能统称，永不作为任一模式的标签。
- 本轮只登记不动手：`WesternFortuneGenerator.kt:155`、`EasternScreen.kt:303,308`、`PhysiognomyScreen`、`ClassicalAstrologyScreen` 与 `GameDialogueBridge` 按钮文案回灌路由器，属同型但当前值仍对得上（潜伏，不是现役）。

## 7. 建议的下一片（按优先级，一片一提交一门禁）

1. 把契约脚本接进 CI（第 5 节），并做一次「故意改坏一处措辞 → CI 变红」的反证。
2. `MysticGuideGenerator.kt` 继续拆分（P1 表）：按 intent 把模板与语气表抽成纯 Kotlin，保持确定性 hash 与 `customAnswer` 唯一 `enforce` 调用点不变。
3. 设备验证窗口开启后：8 句拒答的排布与播报、灵签页渲染、TalkBack 焦点顺序。

## 8. 环境与工作树备忘

- 本机 Gradle 需 `-Pkotlin.compiler.execution.strategy=in-process`；日志写 `/tmp`，不要落在仓库里。注意后台命令的 exit code 可能被管道吃掉，判断成败要看 Gradle 自己写在日志尾的行。
- `git status --porcelain` 有 29 项未跟踪内容（`.superpowers/`、`device-*-20260831.xml`、`ui*.xml`、两份 plan/handoff 文档），一律按显式路径 stage，提交后复核数量不变。
- 远端是 `blueicx/xuanxing-android`，PR #1 是本分支的常驻 PR；CI 只在 `457dc7a` 之后可用。push 需逐次授权，本轮已获授权并推送成功：`f26c876..f89f2f1`（2026-09-02 10:12Z，共 6 笔）。

## 9. 交付定义（接手时同样适用）

每片一个独立可回退提交；契约脚本 + `testDebugUnitTest` + `lintDebug` + `assembleDebug` 全绿；文档同步真实状态；JVM/编译证据不得写成实机或运行时证据；新增断言必须先证明它会失败。
