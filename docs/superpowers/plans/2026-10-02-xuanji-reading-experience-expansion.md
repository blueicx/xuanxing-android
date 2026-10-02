# X3 运势阅读与玄师陪伴扩充实施计划

> **面向 AI 代理的工作者：** 使用 `superpowers:executing-plans` 在当前分支内逐任务实现此计划。不开子代理。步骤使用复选框跟踪；每个任务完成后运行其门禁并单独提交。

**目标：** 在保留既有运势内容与离线边界的基础上，实现详情问玄师、收藏回顾、周期解说、行动反馈闭环和四角色游戏上下文衔接。

**架构：** 详情通过类型化 `MysticDetailContext` 将当前屏幕已经显示的结果带入当前角色对话；收藏通过 profile hash 隔离的版本化本地 store 保存用户主动选取的快照；周期继续消费现有确定性运势；行动反馈仅调整相同类别候选顺序；游戏继续复用现有四角色游戏引擎与存档。

**技术栈：** Kotlin/JVM、Jetpack Compose、DataStore PreferenceBridge、Gson/显式 JSON codec、JUnit、Compose AndroidTest、Node 契约、Gradle。

---

## 文件清单

- `app/src/main/java/com/xuanji/app/domain/MysticDetailContext.kt`：定义模式、周期、日期、详情结果、依据和来源版本；提供安全长度限制和可直接询问的用户输入文本。
- `app/src/main/java/com/xuanji/app/domain/MysticDialogueEngine.kt`、`MysticEvidence.kt`：让离线引擎把详情上下文纳入证据和回复输入。
- `app/src/main/java/com/xuanji/app/ui/components/FortuneDetailPage.kt`、`FortuneComponents.kt`、`DailyActionCards.kt`、`CompositeFortuneScreen.kt`、`EasternScreen.kt`、`WesternScreen.kt`：在已有完整详情页加入“问玄师”和“收藏”；传递对应的真实 `ShareCard`、周期评分、行动细节及依据。
- `app/src/main/java/com/xuanji/app/ui/components/MysticFloatingGuide.kt`、`MysticGuideCard.kt`：打开当前角色舞台，投递类型化详情问题，在既有会话 token 体系下生成回复；关闭舞台回到原详情。
- `app/src/main/java/com/xuanji/app/domain/ReadingFavorite.kt`、`app/src/main/java/com/xuanji/app/data/local/ReadingFavoriteStore.kt`：版本化快照模型、codec、按 profile hash 隔离的增删改查及 profile 清除。
- `app/src/main/java/com/xuanji/app/ui/history/HistoryScreen.kt`：历史内容和我的收藏分段展示，支持分类筛选、搜索、查看快照、取消和确认清空。
- `app/src/main/java/com/xuanji/app/domain/MysticPrivacyExport.kt`、`app/src/main/java/com/xuanji/app/ui/profile/ProfileScreen.kt`、`app/src/main/java/com/xuanji/app/data/repository/FortuneRepository.kt`：导出收藏摘要，并在完整本地清理时删除收藏。
- `app/src/main/java/com/xuanji/app/domain/action/ActionFeedback.kt`、`DailyActionPlanner.kt`、`data/local/ActionFeedbackDataStore.kt`、`ui/viewmodel/ActionViewModel.kt`、`DailyActionCards.kt`：把用户明示反馈用于候选排序，支持撤销最后一次换选。
- `app/src/main/java/com/xuanji/app/ui/XuanjiApp.kt`、`LifeProfileCards.kt`：在现有一生命盘中补充人生概览入口，复用当前画像与 chart-only 数据。
- 对应 `app/src/test/kotlin`、`app/src/androidTest/kotlin` 与 `_dev/*contract_test.js`：分别验证纯逻辑、真实 Compose 操作与既有页面不回退。
- `README.md`、`docs/SYSTEMS_OVERVIEW.md`、`docs/TECHNICAL_DEBT.md` 与新增 dated handoff：记录实施状态和未做的真机验收。

## 任务 1：详情上下文模型和真实问答链路

**文件：**
- 创建 `app/src/test/kotlin/com/xuanji/app/domain/MysticDetailContextTest.kt`
- 创建 `app/src/main/java/com/xuanji/app/domain/MysticDetailContext.kt`
- 修改 `app/src/main/java/com/xuanji/app/domain/MysticDialogueEngine.kt`
- 修改 `app/src/main/java/com/xuanji/app/domain/MysticEvidence.kt`
- 修改 `app/src/main/java/com/xuanji/app/domain/DialogueReplyValidator.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/components/FortuneDetailPage.kt`
- 创建 `app/src/main/java/com/xuanji/app/ui/components/MysticDetailQuestionBridge.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/components/FortuneComponents.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/components/DailyActionCards.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/composite/CompositeFortuneScreen.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/components/MysticFloatingGuide.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/components/MysticGuideCard.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/eastern/EasternScreen.kt` 和 `app/src/main/java/com/xuanji/app/ui/western/WesternScreen.kt`
- 修改 `app/src/androidTest/kotlin/com/xuanji/app/ui/components/MysticDialogueEndToEndTest.kt`

- [x] 先写纯 Kotlin 测试：详情问题文本包含所选模式、周期、日期、标题、真实显示值和依据；截断字段保持在 4000 字符内；空依据如实为空。
- [x] 执行 `.\gradlew.bat :app:testDebugUnitTest --tests '*MysticDetailContextTest' --console=plain`，确认失败原因是 `MysticDetailContext` 尚未定义。
- [x] 实现 `MysticDetailContext(mode, period, dateKey, title, result, evidence, sourceStatus, algorithmVersion)` 和 `toQuestion()`；避免把出生资料全文或未收藏会话数据混入详情上下文。
- [x] 在 `DialogueContext` 增加可选 detail context；证据构造器包含同一 `title/date/period/result/evidence`，离线引擎生成时将其作为当前轮补充输入并保留原安全 guard。
- [x] 用 composition local 或显式回调将 `FortuneDetailPage` 的“问玄师”动作送回 `MysticFloatingGuide`；由 `MysticGuideCard` 在当前角色线程提交对应追问，复用 sessionToken 和 provider 失败回退。
- [x] 为综合维度、通用 ShareCard 和每日行动构造精确 context；东方/西方所有普通卡的结果文本使用其 `ShareCard` 标题、摘要、内容和已有来源，命盘详情不错误标记成每日周期结果。
- [x] 扩展 Compose AndroidTest：打开真实详情、点“问玄师”、断言当前详情和结果被传入对话、关闭舞台后原标题和详情保持可见。
- [x] 运行 `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest --console=plain`。
- [x] 单独提交任务 1，提交信息 `feat(详情): 把当前结果带入玄师对话`（`f25b477`）。

## 任务 2：用户主动收藏的版本化快照

**文件：**
- 创建 `app/src/main/java/com/xuanji/app/domain/ReadingFavorite.kt`
- 创建 `app/src/main/java/com/xuanji/app/data/local/ReadingFavoriteStore.kt`
- 创建 `app/src/test/kotlin/com/xuanji/app/domain/ReadingFavoriteCodecTest.kt`
- 创建 `app/src/test/kotlin/com/xuanji/app/data/local/ReadingFavoriteStoreTest.kt`
- 修改 `app/src/test/kotlin/com/xuanji/app/data/local/FakePreferenceBridge.kt` 或复用现有同类测试实现

- [x] 先写 codec/store 测试：保存并恢复详情快照、类型和 profile 相互隔离、同 ID 更新、取消/删除、未知版本和坏 JSON 安全回退、旧版 envelope 迁移、排序按保存时间倒序。
- [x] 执行 `.\gradlew.bat :app:testDebugUnitTest --tests '*ReadingFavorite*' --console=plain`，确认新类型缺失导致预期失败。
- [x] 实现有版本字段的 `ReadingFavorite`（id、type、title、dateKey、period、mode、characterId、gameId、body、evidence、algorithmVersion、createdAt）；禁止持久化完整出生资料。
- [x] 实现 `ReadingFavoriteCodec`、`ReadingFavoriteStore(PreferenceBridge)`；键使用 `reading_favorites_<sha256(profileKey)>`，单档案上限 200 条，坏数据保留错误状态并提供显式清除方法。
- [x] 补充测试：清档案只删本 profile、清全部 favorites 不影响角色线程和游戏存档。
- [x] 运行 `.\gradlew.bat :app:testDebugUnitTest --tests '*ReadingFavorite*' --console=plain`。
- [ ] 单独提交任务 2，提交信息 `feat(收藏): 增加本地版本化阅读快照`。

## 任务 3：详情收藏入口与记录页回看

**文件：**
- 修改 `FortuneDetailPage.kt`、`FortuneComponents.kt`、`DailyActionCards.kt`、`HistoryScreen.kt`
- 修改 `AppModule.kt`、`FortuneRepository.kt`、`ProfileViewModel.kt`、`MysticPrivacyExport.kt`、`ProfileScreen.kt`
- 创建 `app/src/androidTest/kotlin/com/xuanji/app/ui/history/ReadingFavoritesInteractionTest.kt`
- 修改 `_dev/action_profile_contract_test.js` 或新增 `_dev/reading_favorites_contract_test.js`

- [ ] 先写 Compose 测试：详情点收藏后标签状态变化；切“我的收藏”可筛选、搜索、打开只读快照、取消收藏；空列表、坏数据和清空确认有明确状态。
- [ ] 执行新增 AndroidTest 编译/运行目标，确认失败因为收藏入口/记录页缺少收藏分段。
- [ ] 将收藏 store 注册到应用现有数据入口；详情统一接收收藏当前 context 的回调，区分收藏/已收藏并提供“取消收藏”。
- [ ] 记录页保留「历史上的今天」「同月同日生」全部内容，只增加「我的收藏」段；收藏视图按全部/运势/行动/玄师/游戏筛选，可关键词搜索，打开快照不自动重算。
- [ ] 清空前显示一次系统风格确认；取消操作提供撤销；不增加首页快捷按钮。
- [ ] 在现有隐私导出中加入收藏摘要条目数和快照字段；完整本地数据清除调用 favorites `clear(profileKey)`，不扩大到用户以外 profile。
- [ ] 新增 Node contract 锁定旧历史内容和新收藏入口并存；运行 `node _dev/reading_favorites_contract_test.js`。
- [ ] 运行 `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest --console=plain` 并执行收藏 AndroidTest。
- [ ] 单独提交任务 3，提交信息 `feat(记录): 加入运势和玄师收藏回看`。

## 任务 4：周期趋势与一生命盘概览

**文件：**
- 修改 `CompositeFortuneScreen.kt`、`EasternScreen.kt`、`WesternScreen.kt`、`XuanjiApp.kt`、`LifeProfileCards.kt`
- 修改 `app/src/test/kotlin/com/xuanji/app/ui/composite/CompositePresentationContractTest.kt`
- 修改 `_dev/xuanji_v14_shell_contract_test.js`

- [ ] 先写测试：day/week/month/year 切换后日期/标题对应现有结果；每日行动只出现在 day；无历史序列时不得显示拼造的折线数据；本命盘独立入口保留。
- [ ] 执行 `.\gradlew.bat :app:testDebugUnitTest --tests '*CompositePresentationContractTest' --console=plain`，确认新增趋势契约失败。
- [ ] 使用 `fortune.periodSummary`、`dimensions`、`insights` 和已有东/西方周期字段呈现层级清晰的周期解说及依据；没有跨期观测序列时只呈现当前周期与文字依据，不造图。
- [ ] 把 LifeProfile 摘要作为「一生命盘」的概览 tab；东方本命/西方本命仍调用 chartOnly 实际盘面；不从每日详情跳转排盘。
- [ ] 保留 v14 卡片 token、旧内容、东方农历和公历日期语义；不改变算法计算或分类顺序。
- [ ] 运行 `node _dev/xuanji_v14_shell_contract_test.js` 与 `.\gradlew.bat :app:testDebugUnitTest --tests '*CompositePresentationContractTest' --console=plain`。
- [ ] 单独提交任务 4，提交信息 `feat(运势): 补全周期解说与本命概览`。

## 任务 5：行动反馈参与排序并可撤销

**文件：**
- 修改 `ActionFeedback.kt`、`DailyActionPlanner.kt`、`ActionFeedbackDataStore.kt`、`ActionRepository.kt`、`ActionViewModel.kt`、`DailyActionCards.kt`、`ProfileScreen.kt`
- 创建 `app/src/test/kotlin/com/xuanji/app/domain/action/ActionFeedbackRankingTest.kt`
- 修改 `_dev/action_profile_contract_test.js`

- [ ] 先写纯 Kotlin 测试：Accepted 在下次同类别排序给予上限 +5；NotSuitable 降权上限 -8；Replaced 只暂时排除当日已换候选且可撤销；类别隔离；不能过滤掉硬约束候选的约束检查。
- [ ] 执行 `.\gradlew.bat :app:testDebugUnitTest --tests '*ActionFeedbackRankingTest' --console=plain`，确认排序辅助 API 缺失而失败。
- [ ] 添加纯函数 `applyExplicitFeedback(candidates, feedback, category, dateKey)`；反馈只按 profile/category 候选 ID 聚合，结果可解释，基准五行/星座权重不变。
- [ ] store 增加按 ID 删除最近一条 feedback 的 API；“换一个”记录原排序并撤销只恢复当次候选选择，不删除饮食偏好。
- [ ] UI 显示筛选来源与反馈生效提示；保存/撤销后刷新今日结果，空候选时保留明确空态。
- [ ] 运行 Node action contract、ActionFeedback JVM 测试、`:app:testDebugUnitTest`。
- [ ] 单独提交任务 5，提交信息 `feat(行动): 使用可撤回反馈调整候选顺序`。

## 任务 6：角色对话引用实际游戏存档

**文件：**
- 修改 `MysticGuideCard.kt`、`MysticDialogueEngine.kt`、`MysticEvidence.kt`、`CompanionGameProgressStore.kt`、现有 game dialogue bridge 与测试
- 修改 `_dev/dialogue_contract_test.js`
- 创建或扩展 `app/src/test/kotlin/com/xuanji/app/domain/MysticGameContextTest.kt`

- [ ] 先写测试：四个角色通过同一条游戏上下文接口分别获得正确 characterId/gameId；有档案时回复里引用真实存档 checkpoint；无档案时产生诚实回退；清档案不串读。
- [ ] 执行 `.\gradlew.bat :app:testDebugUnitTest --tests '*MysticGameContextTest' --console=plain`，确认接口/上下文尚未存在。
- [ ] 将当前角色对应的版本化 `CompanionGameProgressState` 作为仅本轮的可选 dialogue context；conversation thread 不复制整个游戏 state，也不把生成回复保存成长记忆。
- [ ] 游戏入口恢复现有状态、结束摘要可主动收藏；继续使用中国象棋 GameArchiveStore 和其他三款现有 store。
- [ ] 运行 dialogue Node contract 与新增 JVM 测试、现有 dialogue AndroidTest。
- [ ] 单独提交任务 6，提交信息 `feat(玄师): 让角色对话读取实际游戏进度`。

## 任务 7：文档与完整门禁

**文件：** `README.md`、`docs/SYSTEMS_OVERVIEW.md`、`docs/TECHNICAL_DEBT.md`、`docs/superpowers/handoffs/2026-10-02-reading-experience-expansion-handoff.md`、相关契约测试。

- [ ] 文档只记已经实现且可由源码验证的事实，区分 Android/JVM/Node/CI/模拟器/真机状态。
- [ ] 运行全部 Node 契约：`node _dev/authentic_systems_contract_test.js`、`node _dev/dialogue_contract_test.js`、`node _dev/unified_stage_contract_test.js`、`node _dev/action_profile_contract_test.js`、`node _dev/xuanji_v14_shell_contract_test.js`、`node _dev/reading_favorites_contract_test.js`。
- [ ] 运行 `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain`。
- [ ] 在在线模拟器上执行新增 Compose 操作测试；用户指定的真机验收仍登记未做。
- [ ] 检查 `git diff --check`、秘钥/私人数据未入库、分支状态，并更新计划复选框。
- [ ] 提交任务 7，提交信息 `docs(交接): 记录扩充实现和验证状态`。
