# X3 稳定可信与本地回顾扩充实现计划

> **面向 AI 代理的工作者：** 使用 `superpowers:executing-plans` 在当前分支逐任务实现。不开子代理；步骤使用复选框记录；不得提交、推送或连接用户手机。

**目标：** 完成真实交互验证、对话回归、本地回顾、选择性备份恢复和现有三款角色游戏深化。

**架构：** 先落地共同对话 fixture 与纯 Kotlin 游戏/备份/时间线模型，再接入既有 DataStore stores 和 Compose 页面，最后用隔离模拟器运行真实交互并更新文档。所有用户资料处理保持 profile 隔离、显式选择和确认。

**技术栈：** Kotlin/JVM、Jetpack Compose、现有 PreferenceBridge/DataStore、Gson、JUnit、Compose UI Test、Node.js 契约、Gradle Android Emulator Runner。

---

## 文件职责

- 创建 `app/src/test/resources/dialogue_relevance_cases.json`：106 条自拟对话输入，逐条由四角色执行（424 次评估），共享给 JVM 与 Node 校验。
- 创建 `app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueRelevanceMatrixTest.kt`：运行真实对话分析、路由、离线回复和边界检查。
- 创建 `_dev/dialogue_relevance_contract_test.js`：校验用例数、ID 唯一性、persona/意图覆盖并与 JSON fixture 对齐。
- 创建 `app/src/main/java/com/xuanji/app/domain/LocalReviewTimeline.kt`、`app/src/main/java/com/xuanji/app/data/local/LocalReviewTimelineRepository.kt`：定义明确来源的回顾项并合并用户收藏与主动行动反馈。
- 修改 `app/src/main/java/com/xuanji/app/ui/history/HistoryScreen.kt`：在记录页加入紧凑的「近期选择」筛选列表，保留旧历史和收藏行为。
- 创建 `app/src/main/java/com/xuanji/app/domain/LocalDataBackup.kt`：选择性备份 envelope、checksum、版本解码及预览模型。
- 创建 `app/src/main/java/com/xuanji/app/data/local/LocalDataBackupRepository.kt`：协调现有收藏、行动反馈、三款角色游戏进度和 `GameArchiveStore` 棋谱的读取、导入预览和确认写入；不导入象棋战绩。
- 创建 `app/src/main/java/com/xuanji/app/ui/profile/LocalBackupPanel.kt` 并修改 `ProfileScreen.kt`：系统文档选择器、勾选项、导入预览、冲突确认与结果提示。
- 修改 `app/src/main/java/com/xuanji/app/domain/game/CompanionGameEngines.kt`、`CompanionGameProgress.kt` 与 `app/src/main/java/com/xuanji/app/ui/components/CompanionGameCard.kt`：诗句/星图关卡、撤销与摘要，丝路回退/重算/回放以及存档迁移。
- 创建或扩展 `app/src/test/kotlin/com/xuanji/app/domain/LocalReviewTimelineTest.kt`、`app/src/test/kotlin/com/xuanji/app/data/local/LocalReviewTimelineRepositoryTest.kt`、`LocalDataBackupCodecTest.kt`、`LocalDataBackupRepositoryTest.kt` 和游戏引擎测试。
- 扩展 `app/src/androidTest/kotlin/com/xuanji/app/ui/history/ReadingFavoritesInteractionTest.kt`、`app/src/androidTest/kotlin/com/xuanji/app/ui/components/MysticDialogueEndToEndTest.kt`，并为游戏/备份导入交互添加隔离 Compose 用例。
- 修改 `.github/workflows/build.yml` 加入相关性 fixture 契约并将隔离 API 34 job 纳入本轮交互用例。
- 更新 `README.md`、`docs/SYSTEMS_OVERVIEW.md`、`docs/TECHNICAL_DEBT.md`，新建 dated handoff；进度写入 `.superpowers/dispatch-progress.md`，目标文件原件备份到 `.superpowers/work/.dispatch-backup/`。

## 任务 1：对话相关性评测集

- [x] 建立 106 条自拟 JSON 用例；每条由四角色执行，四角色与 greet/daily/life_profile/evidence/game/clarify/safety 均有样例。
- [x] 编写真实 JVM 矩阵测试，逐条调用现有 `MysticDialogueAnalyzer`、`MysticCompanionActionRouter` 和 `MysticDialogueEngine`，断言类别、动作路由及缺资料/医疗/财务边界。
- [x] 首轮矩阵准确发现“我暗恋的人最近怎么样”误判为问候；将问候短语限制在独立/句首表达后，回归通过。
- [x] 实现 fixture loader 与确定性分类/路由修复；行为回归先补样例再改生产代码。
- [x] 新建 Node contract，断言 106 个唯一 id、四角色齐全与目标意图覆盖，并加入 CI；Node 与 JVM 矩阵通过。

## 任务 2：本地回顾时间线

- [x] 测试先定义时间线 item 类型、来源标签、时间降序、筛选、同 ID 去重和空数据行为。
- [x] 新增纯 Kotlin timeline builder；来源只允许主动收藏快照和主动行动反馈，不读取隐式人格/聊天，也不把未主动收藏的自动存档列为回顾项。
- [x] 编写 repository 测试，覆盖收藏/反馈合并、同 profile 读取、跨 profile 隔离和缺失 store 回退。
- [x] 在记录页加入「近期选择」紧凑分区和过滤；旧历史、收藏搜索/清除流程保持不变。
- [x] 扩展 Compose 测试，真实点击筛选、打开/移除收藏并验证主动反馈条目内容；JVM 与模拟器交互通过。

## 任务 3：选择性本地备份恢复

- [x] 写 codec 用例：分类选择、版本、SHA-256、截断/非法字段、损坏内容与档案指纹匹配。
- [x] 新增版本化 backup DTO/codec，默认不包含出生资料、对话线程或长期记忆；保留收藏、反馈、角色游戏和 Xiangqi `GameSave`；不备份象棋战绩。
- [x] 测试 repository 对已确认前后的 stores：预览/校验不得写入；确认后收藏和反馈按 ID 幂等合并；游戏存档冲突只在用户明确覆盖后写入。
- [x] 新增 repository，注入既有 stores 和内存 PreferenceBridge 测试 seam，不创建另一套 profile key。
- [x] 新增 Profile 本地备份面板：系统文档选择器、导入预览/分区选择、档案不匹配阻断和冲突覆盖二次确认。
- [x] Compose 测试验证导出选择、预览、取消不变更、确认后选择性恢复及跨档案备份阻断；JVM 覆盖损坏文件与冲突写入门槛。

## 任务 4：角色游戏撤销、关卡与回放

- [x] 为诗句、星图、丝路每个引擎先添加失败测试：撤销、重开、终局摘要、确定性关卡推进和资源重算。
- [x] 扩充自有诗句与星图逻辑题至 5 个固定关卡，提供稳定提示和可验证目标；只按固定顺序推进。
- [x] 诗句状态增加可迁移答案历史；星图与丝路按已记录路径重算撤销后的当前点/资源，并保留旧 progress envelope 的有效存档。
- [x] 进度 codec 升级到版本 2 并迁移旧 JSON；未知版本或损坏数据安全降级，旧 v1 有效进度不静默重置。
- [x] UI 增加关卡/提示/撤销/重开/回放摘要控件，沿用现有 game host，不新增首页入口。
- [x] AndroidTest 真实触发撤销、关卡推进、终局摘要与存档恢复；JVM 与隔离模拟器运行通过。

## 任务 5：隔离运行、文档与最终验收

- [x] 使用显式 SDK `adb.exe`、专用 server port 5038 和明确的 `emulator-5556`，未连接或部署到用户手机。
- [x] 扩展 API 34 Android Emulator CI job 覆盖本轮对话、收藏、游戏、详情与备份交互类；七项 `_dev` Node contracts 全部通过。
- [x] 运行 `:app:testDebugUnitTest`、`:app:lintDebug`、`:app:assembleDebug`、`:app:assembleDebugAndroidTest`。
- [x] 在隔离模拟器执行 `connectedDebugAndroidTest`：21 项通过，0 失败/错误/跳过，并保留 XML 报告。
- [x] 更新 README、系统概览、技术债、dated handoff 与 progress ledger；区分本机 JVM、模拟器、未执行的远端 CI 与真机证据。
- [x] 执行 `git diff --check` 和最终 `git status --short`；不清理、不提交、不推送、不安装用户手机。
