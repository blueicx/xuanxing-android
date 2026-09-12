# X3 玄机：角色称谓收口与作风键修复交接文档

**交接日期：** 2026-09-02
**项目目录：** `F:\huny\xuanji-android`
**当前分支：** `codex/system-consistency`
**交接范围：** 双面角色对外称谓的统一，以及顺带暴露出的 `styleKeyFor` 键/名回归。
**当前状态：** 两笔改动已提交并通过本机门禁；实机验证仍未做，且被明确要求暂缓。

## 1. 给下一位执行者的第一句话

称谓已经不是问题，但同一类缺陷还在：`TodayOracle` 用显示名当查询键。改它之前先读 `docs/TECHNICAL_DEBT.md`「当前基线」第 7、8 条——那里写清了哪些是观测、哪些只是推理。不要为了「看起来更统一」把「玄师」从统称改成某个模式的名字，也不要在这轮里夹带设备验证、在线 provider 或棋局引擎。

## 2. 本轮已交付

### 称谓收口（`20be2b9`）

- `MysticGuideGenerator.personaName(mode)`：scholar→「玄学家」、half→「半仙」，是对外称谓的唯一来源。
- 「玄师」退回为这套陪伴功能的统称：舞台关闭键「关闭玄师台」、会话气泡、设置项标题保持原样。
- `MysticOrb` 的漂移周期（3500/5200ms）与幅度（3f/2f）改为按 `half` 分支，不再比较 `"魔师"` 字符串；`roleName` 由调用方用 `personaName(stageMode)` 传入。
- `MysticImmersiveStage` 上从未被读取的 `roleName` 形参删除。
- `identityAnswer` 六条自我介绍每条只自称一次；「街口半仙」「云端实习半仙」把称谓并进作风名，避免换名后出现「半仙，街口半仙」。
- 两处「本魔师」文案改为「本半仙」。
- 契约新增 `persona` 段：逐行扫描 `app/src/main/java/com/xuanji/app` 下所有 `.kt`，退役名（慈翁 / 魔师）任何一行命中即失败；另钉住 `personaName` 实现、浮球按模式分支、六条自我介绍、统称「玄师」和 `unverified` 至少两条。
- 新增 JVM 用例 `personaName_labels_the_two_modes_and_never_the_umbrella`、`identity_answers_name_the_persona_exactly_once`（5 组盘面 × 7 个话题 × 2 个模式，断言六种作风全部可达、每条身份回答只自称一次、不借用另一面称谓）。

### 作风键回归（`cf4b491`）

`styleKeyFor` 自 `f3ad6a8`（2026-08-24）起返回 `style(...).second`，即作风显示名而非作风键。下游一律按键匹配，因此：`customAnswer` 里六种作风全部落到 `else`（卡面标题写着「档案室学者」，说的却是「慢速罗盘」的话），`MysticGuideCard` 两处 `guide.styleKey == styleKeyFor(...)` 守卫永远为假，签到与节奏选择后的延后应答每次都被静默丢弃。已改为返回 `.first`。

刻意没动的地方：客串台词与 `asideMemoryNote` 里提到的是**另一面**角色，那是有意的，不是漂移。

## 3. 门禁与真实证据

- `node _dev/dialogue_contract_test.js` → PASS（26 条 golden wording）。
- `./gradlew :app:testDebugUnitTest --rerun` → exit 0，266 项、0 失败、0 跳过（拆提交后在最终树上强制重跑）。
- `./gradlew :app:lintDebug :app:assembleDebug` → BUILD SUCCESSFUL，0 error 级命中，`app/build/outputs/apk/debug/app-debug.apk`（23,845,578 B，2026-09-02 14:46）——同内容拆分前那次门禁的产物。
- `assembleDebugAndroidTest` 判定 UP-TO-DATE，`compileDebugAndroidTestKotlin` 已针对改动后的 main 源码重新编译。

## 4. 明确未验证（不得当作已完成）

- 实机上浮球与舞台的称谓渲染、TalkBack 实际播报内容、换装按钮标签在窄屏是否截断。
- 签到与节奏延后应答修复后是否真的出现在界面上——只证明了守卫的条件在 JVM 侧成立。
- 12 项棋盘交互 androidTest 用例从未在设备或 CI 上执行。
- `cf4b491` 这一笔没有隔离跑过测试：它相对最终树只少了称谓改名，且既有断言与已提交契约都没钉过受影响的那几句文案，因此判断为安全——这是推理，不是观测。

设备类工作按用户要求暂缓，恢复时先补第 4 节。

## 5. 本轮冻结的取舍（不要悄悄改回来）

- 「玄师」= 功能统称，永不作为任一模式的标签。
- 未识别的 mode 一律按学者处理，与 `mysticSkins` / `styleKeyFor` 的 `mode != "half"` 判断保持一致。
- 称谓收口不改动画节奏数值，只改分支依据。

## 6. 建议的下一片（按优先级，一片一提交一门禁）

1. `TodayOracle`：`oracleRole` 返回显示名，`when (role)`、`if (firstRole == "玄学家")` 与 12 条对照表都依赖这两个字面量。改成稳定 role 键，行为不变，契约扫描禁止显示名出现在比较位置。
2. 大文件继续拆分（`docs/TECHNICAL_DEBT.md` P1 表），一次只移动一个稳定边界。
3. 设备验证窗口开启后：TalkBack 焦点顺序、同日生折叠按钮标签、作品卡阅读顺序、舞台文化场景对比度。

## 7. 环境与工作树备忘

- 本机 Gradle 需 `-Pkotlin.compiler.execution.strategy=in-process`；日志写 `/tmp`，不要落在仓库里。
- `git status --short` 当前有 29 项未跟踪内容（`.superpowers/`、`device-*-20260831.xml`、`ui*.xml`、两份 plan/handoff 文档），一律按显式路径 stage，提交后用 `git status --short` 复核数量不变。
- 远端是 `blueicx/xuanxing-android`，PR #1 是本分支的常驻 PR；CI 只在 `457dc7a` 之后可用。push 需要逐次授权，本轮未推送。

## 8. 交付定义（接手时同样适用）

每片一个独立可回退提交；契约脚本 + `testDebugUnitTest` + `lintDebug` + `assembleDebug` 全绿；文档同步真实状态；JVM/编译证据不得写成实机或运行时证据。
