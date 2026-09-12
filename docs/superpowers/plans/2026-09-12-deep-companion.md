# X3 深陪伴体验实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 `superpowers:executing-plans` 逐任务实现本计划；每个任务完成后先验证，再创建一个原子提交。

**目标：** 在默认离线的前提下，完成 A 方案人物主舞台、克制东方人形、结构化对话理解、可控本地软标签和可验证的 Provider 扩展接缝，消除人物辨识度不足、文化皮肤像换色、对话答非所问三个问题。

**架构：** 领域层新增 `MysticDialogueAnalyzer`、回复校验 Module 和软标签 Codec/Store；UI 层把舞台人物、文化背景、消息列表和记忆面板从两个超大 Composable 文件中按稳定 seam 拆出。离线 Provider 继续是默认实现，在线 Provider 只有显式注入时才参与，所有外部文本先经过本地事实与安全校验。

**技术栈：** Kotlin/JVM、Jetpack Compose、DataStore Preferences、JUnit4/Kotlin Test、Compose UI Test、Node.js 契约脚本、GitHub Actions。

---

## 任务 1：建立本轮基线并修正文档契约数字

**文件：**
- 修改：`docs/SYSTEMS_OVERVIEW.md`
- 修改：`docs/TECHNICAL_DEBT.md`
- 修改：`_dev/dialogue_contract.json`
- 测试：`_dev/dialogue_contract_test.js`

- [ ] **步骤 1：先写会失败的文档契约断言**

在 `_dev/dialogue_contract_test.js` 增加断言，要求 Android 文档中引用的 golden 数量与 `dialogue_contract.json.golden_wording.length` 相同，并要求当前值为 45：

```js
const overview = fs.readFileSync(path.join(__dirname, '..', 'docs', 'SYSTEMS_OVERVIEW.md'), 'utf8');
assert(overview.includes('45 条 golden wording'), 'SYSTEMS_OVERVIEW must report the current golden count');
```

- [ ] **步骤 2：运行契约确认失败**

运行：

```powershell
node _dev/dialogue_contract_test.js
```

预期：FAIL，报错 `SYSTEMS_OVERVIEW must report the current golden count`，因为概览仍写着 37 条。

- [ ] **步骤 3：同步文档和契约说明**

把 `SYSTEMS_OVERVIEW.md` 的 37 改成 45，补充“本阶段不新增占卜体系、先治理陪伴体验”的边界；在 `TECHNICAL_DEBT.md` 增加本计划的入口和未验证设备边界。契约 JSON 只补充 `dialogue_analysis`、`soft_memory`、`stage_title` 三个字段，不修改已有 seed、persona、safety 和棋局字段。

- [ ] **步骤 4：运行契约确认通过**

运行：

```powershell
node _dev/dialogue_contract_test.js
```

预期：`dialogue contract: PASS (45 golden entries)`。

- [ ] **步骤 5：提交**

```powershell
git add docs/SYSTEMS_OVERVIEW.md docs/TECHNICAL_DEBT.md _dev/dialogue_contract.json _dev/dialogue_contract_test.js
git commit -m "docs(契约): 同步深陪伴阶段边界与黄金文案数量"
```

## 任务 2：新增结构化对话分析 Module

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/domain/MysticDialogueAnalyzer.kt`
- 创建：`app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueAnalyzerTest.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticDialogueEngine.kt`

- [ ] **步骤 1：编写失败测试**

新增以下测试，先引用尚不存在的类型：

```kotlin
@Test
fun multi_topic_input_keeps_one_primary_topic_and_one_clarifier() {
    val result = DefaultMysticDialogueAnalyzer().analyze(
        "工作和感情都想问，先看哪个？",
        testDialogueContext()
    )
    assertEquals(MysticIntent.Career, result.intent)
    assertEquals("career", result.topicKey)
    assertTrue(result.entities.containsKey("secondary_topic"))
    assertTrue(result.needsClarification)
}

@Test
fun explicit_follow_up_inherits_previous_topic_and_entity() {
    val result = DefaultMysticDialogueAnalyzer().analyze(
        "那这个呢？",
        testDialogueContext(recentTurns = listOf(MysticTurn("最近工作压力很大", "先把工作拆开", "career")))
    )
    assertEquals(MysticIntent.Career, result.intent)
    assertTrue(result.confidence >= 60)
}
```

- [ ] **步骤 2：运行定向测试确认失败**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.MysticDialogueAnalyzerTest" --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
```

预期：FAIL，报错 `Unresolved reference: DefaultMysticDialogueAnalyzer`。

- [ ] **步骤 3：实现最小分析接口**

新增稳定数据结构：

```kotlin
data class DialogueAnalysis(
    val intent: MysticIntent,
    val topicKey: String?,
    val entities: Map<String, String> = emptyMap(),
    val confidence: Int,
    val needsClarification: Boolean
)

interface MysticDialogueAnalyzer {
    fun analyze(input: String, context: DialogueContext): DialogueAnalysis
}
```

`DefaultMysticDialogueAnalyzer` 依次执行 Unicode/标点规范化、棋局意图、安全词扫描、寒暄、明确主题、追问承接和普通闲聊；多主题只取第一个稳定主主题，把第二主题放入 `secondary_topic`；低于 60 的置信度设置 `needsClarification=true`。分析器不计算运势分数、不写入记忆。

- [ ] **步骤 4：让现有 Engine 复用分析器**

在 `MysticDialogueEngine.kt` 中让 `classify()` 返回分析器的 `intent`，保留旧公开方法；`DialogueContext` 的默认值和 `MysticIntent` 枚举不删除。

- [ ] **步骤 5：运行通过并提交**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.MysticDialogueAnalyzerTest" --tests "com.xuanji.app.domain.MysticDialogueEngineTest" --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
git add app/src/main/java/com/xuanji/app/domain/MysticDialogueAnalyzer.kt app/src/main/java/com/xuanji/app/domain/MysticDialogueEngine.kt app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueAnalyzerTest.kt
git commit -m "feat(对话): 增加主题实体与置信度分析"
```

预期：定向测试通过。

## 任务 3：把“先答核心 + 澄清入口”接入回复和会话状态

**文件：**
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticDialogueEngine.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticDialogueContinuity.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticDialogueCoordinator.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticSessionState.kt`
- 修改：`app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueEngineTest.kt`
- 修改：`app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueContinuityTest.kt`
- 修改：`app/src/test/kotlin/com/xuanji/app/domain/MysticSessionStateTest.kt`

- [ ] **步骤 1：先锁定回复扩展的失败测试**

```kotlin
@Test
fun ambiguous_reply_answers_primary_topic_and_exposes_at_most_two_clarifiers() {
    val reply = engine.reply(testDialogueContext(), "工作和感情都想问")
    assertEquals(MysticIntent.Career, reply.intent)
    assertTrue(reply.text.isNotBlank())
    assertTrue(reply.clarifiers.size <= 2)
}

@Test
fun stale_provider_reply_is_dropped_after_context_change() {
    val pending = reduce(MysticSessionState(), MysticEvent.SendInput("工作怎么办"))
    val changed = reduce(pending, MysticEvent.ChangeContext(topicKey = "love"))
    val stale = reduce(
        changed,
        MysticEvent.ReplySucceeded(changed.sessionToken - 1, 1, DialogueReply(MysticIntent.Career, "", "旧回复"))
    )
    assertTrue(stale.messages.none { it.text == "旧回复" })
}
```

- [ ] **步骤 2：运行确认旧接口未满足新行为**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.MysticDialogueEngineTest" --tests "com.xuanji.app.domain.MysticSessionStateTest" --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
```

预期：FAIL，因为 `DialogueReply` 尚无 `clarifiers`，且协调器没有把分析结果传给回复。

- [ ] **步骤 3：扩展回复数据结构并保持兼容**

将 `DialogueReply` 扩展为：

```kotlin
data class DialogueReply(
    val intent: MysticIntent,
    val prefix: String,
    val text: String,
    val clarifiers: List<String> = emptyList(),
    val source: ReplySource = ReplySource.Offline,
    val groundedFacts: List<String> = emptyList()
)

enum class ReplySource { Offline, OnlineFallback, OnlineValidated }
```

引擎先生成主回答，再由分析器产生 0–2 个澄清入口；问候、感谢、告别继续不显示原问题回显。

- [ ] **步骤 4：扩展会话事件**

新增 `MysticEvent.ClarifierSelected`，其内容复用 `beginInput()`；`MysticSessionState` 不新增第二套 pending 状态，旧 token 丢弃逻辑保持单一来源。

- [ ] **步骤 5：运行测试并提交**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.MysticDialogueEngineTest" --tests "com.xuanji.app.domain.MysticDialogueContinuityTest" --tests "com.xuanji.app.domain.MysticSessionStateTest" --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
git add app/src/main/java/com/xuanji/app/domain/MysticDialogueEngine.kt app/src/main/java/com/xuanji/app/domain/MysticDialogueContinuity.kt app/src/main/java/com/xuanji/app/domain/MysticDialogueCoordinator.kt app/src/main/java/com/xuanji/app/domain/MysticSessionState.kt app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueEngineTest.kt app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueContinuityTest.kt app/src/test/kotlin/com/xuanji/app/domain/MysticSessionStateTest.kt
git commit -m "feat(对话): 增加核心回答与澄清入口"
```

## 任务 4：实现本地软标签 Codec、Store 和撤回

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/domain/SoftMemoryTag.kt`
- 创建：`app/src/main/java/com/xuanji/app/data/local/SoftMemoryTagStore.kt`
- 创建：`app/src/test/kotlin/com/xuanji/app/domain/SoftMemoryTagTest.kt`
- 创建：`app/src/test/kotlin/com/xuanji/app/data/local/SoftMemoryTagStoreTest.kt`
- 修改：`app/src/main/java/com/xuanji/app/data/local/DataStoreModule.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticRecollection.kt`

- [ ] **步骤 1：写失败测试锁定长期保存规则**

```kotlin
@Test
fun inferred_tag_round_trips_with_source_and_can_be_revoked() {
    val tag = SoftMemoryTag("work", "topic", "最近常聊", "工作", SoftMemorySource.Inferred, "2026-09-12", "2026-09-12")
    val decoded = SoftMemoryTagCodec.decode(SoftMemoryTagCodec.encode(listOf(tag)))
    assertEquals(tag, decoded.single())
    assertTrue(SoftMemoryTagCodec.revoke(decoded, "work").single().revoked)
}
```

- [ ] **步骤 2：运行确认失败**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.SoftMemoryTagTest" --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
```

预期：FAIL，类型和 Codec 尚不存在。

- [ ] **步骤 3：实现版本化 Codec**

使用 Gson 写入 `version=1`、`profileKey` 摘要和 tag 列表；未知版本、字段缺失或 JSON 损坏统一返回空列表并暴露 `unreadable=true`，不阻塞对话。

- [ ] **步骤 4：实现 Store 与 DataStore key**

`SoftMemoryTagStore` 使用 `soft_memory_<sha256(profileKey)>`，与 `talk_memory_`、`game_save_`、`user_profile` 互斥；提供 `read()`、`upsertInferred()`、`revoke(id)`、`clear()` 四个方法。自动保存不需要二次确认，但 UI 必须显示“由对话推断”，用户可以立即撤回。

- [ ] **步骤 5：运行并提交**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.SoftMemoryTagTest" --tests "com.xuanji.app.data.local.SoftMemoryTagStoreTest" --tests "com.xuanji.app.data.local.ConversationMemoryStoreTest" --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
git add app/src/main/java/com/xuanji/app/domain/SoftMemoryTag.kt app/src/main/java/com/xuanji/app/data/local/SoftMemoryTagStore.kt app/src/main/java/com/xuanji/app/data/local/DataStoreModule.kt app/src/main/java/com/xuanji/app/domain/MysticRecollection.kt app/src/test/kotlin/com/xuanji/app/domain/SoftMemoryTagTest.kt app/src/test/kotlin/com/xuanji/app/data/local/SoftMemoryTagStoreTest.kt
git commit -m "feat(记忆): 增加可撤回的本地软标签"
```

## 任务 5：接入 Provider 校验和在线失败回退接缝

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/domain/DialogueReplyValidator.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/DialogueProvider.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticDialogueCoordinator.kt`
- 创建：`app/src/test/kotlin/com/xuanji/app/domain/DialogueReplyValidatorTest.kt`
- 修改：`app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueCoordinatorTest.kt`

- [ ] **步骤 1：写失败测试**

```kotlin
@Test
fun provider_text_that_invents_a_score_is_rejected_to_offline_fallback() {
    val result = DialogueReplyValidator.validate(
        "你的综合分是 99 分，保证升职。",
        testDialogueContext(fortune = fortune(overallScore = 72))
    )
    assertEquals(ValidationResult.Reject("ungrounded_fact"), result)
}

@Test
fun provider_failure_returns_offline_reply_without_changing_default_provider() = runTest {
    val reply = coordinator(FailingProvider()).complete(state(), context(), "今天运势")
    assertTrue(reply.any { it is MysticEvent.ReplySucceeded })
}
```

- [ ] **步骤 2：运行确认失败**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.DialogueReplyValidatorTest" --tests "com.xuanji.app.domain.MysticDialogueCoordinatorTest" --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
```

- [ ] **步骤 3：实现本地校验**

校验器只允许引用 `DialogueContext.fortune`、`latestTest`、最近一回合和用户输入中存在的事实；发现新分数、医疗/财务结论或未授权长期记忆时返回拒绝。协调器将拒绝/超时/取消统一降级到 `OfflineDialogueProvider`，成功文本标记 `OnlineValidated`，回退文本标记 `OnlineFallback`。

- [ ] **步骤 4：运行并提交**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.DialogueReplyValidatorTest" --tests "com.xuanji.app.domain.MysticDialogueCoordinatorTest" --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
git add app/src/main/java/com/xuanji/app/domain/DialogueReplyValidator.kt app/src/main/java/com/xuanji/app/domain/DialogueProvider.kt app/src/main/java/com/xuanji/app/domain/MysticDialogueCoordinator.kt app/src/test/kotlin/com/xuanji/app/domain/DialogueReplyValidatorTest.kt app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueCoordinatorTest.kt
git commit -m "feat(provider): 增加本地事实校验与离线回退"
```

## 任务 6：接入软标签和澄清入口到对话卡

**文件：**
- 修改：`app/src/main/java/com/xuanji/app/ui/components/MysticGuideCard.kt`
- 创建：`app/src/main/java/com/xuanji/app/ui/components/MysticClarifierRow.kt`
- 创建：`app/src/main/java/com/xuanji/app/ui/components/MysticSoftMemoryPanel.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/components/MysticConversationPanel.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/components/MysticGuideCard.kt`

- [ ] **步骤 1：先在 Compose 测试中钉住语义**

在 `app/src/androidTest/kotlin/com/xuanji/app/ui/components/MysticConversationPanelTest.kt` 添加：

```kotlin
@Test
fun clarifier_and_soft_memory_controls_have_accessible_labels() {
    composeTestRule.onNodeWithText("由对话推断").assertExists()
    composeTestRule.onNodeWithContentDescription("撤回软标签：工作").assertExists()
}
```

- [ ] **步骤 2：运行编译确认测试尚未满足**

```powershell
.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
```

预期：FAIL，新的测试与 UI 语义尚不存在。

- [ ] **步骤 3：实现入口与面板**

回复气泡下方最多显示两个 `clarifiers`；点击后派发 `MysticEvent.ClarifierSelected`。软标签面板显示标签值、来源、最后出现日期和单条撤回/全部清除；清除用户原话记忆与软标签继续使用同一个明确的清除入口，并显示清除范围。

- [ ] **步骤 4：运行编译并提交**

```powershell
.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
git add app/src/main/java/com/xuanji/app/ui/components/MysticGuideCard.kt app/src/main/java/com/xuanji/app/ui/components/MysticClarifierRow.kt app/src/main/java/com/xuanji/app/ui/components/MysticSoftMemoryPanel.kt app/src/main/java/com/xuanji/app/ui/components/MysticConversationPanel.kt app/src/androidTest/kotlin/com/xuanji/app/ui/components/MysticConversationPanelTest.kt
git commit -m "feat(交流面板): 展示澄清入口与软标签控制"
```

## 任务 7：拆出 A 方案舞台布局和人物 Canvas

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/ui/components/MysticStageLayout.kt`
- 创建：`app/src/main/java/com/xuanji/app/ui/components/MysticFigureCanvas.kt`
- 创建：`app/src/main/java/com/xuanji/app/ui/components/MysticCultureBackdrop.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/components/MysticFloatingGuide.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/components/MysticCultureSpec.kt`
- 创建：`app/src/test/kotlin/com/xuanji/app/ui/components/MysticCultureVoiceTest.kt`

- [ ] **步骤 1：先写文化差异的纯 Kotlin 测试**

```kotlin
@Test
fun each_skin_has_prop_gesture_and_voice_differences() {
    val specs = MysticCultureSpec.all()
    assertEquals(8, specs.size)
    assertEquals(8, specs.map { it.prop }.toSet().size)
    assertEquals(8, specs.map { it.gesture }.toSet().size)
    assertEquals(8, specs.map { it.voiceLexicon }.toSet().size)
}
```

- [ ] **步骤 2：运行确认新字段不存在**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.ui.components.MysticCultureVoiceTest" --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
```

- [ ] **步骤 3：实现舞台拆分**

将 `MysticFloatingGuide.kt` 中的 `MysticImmersiveStage`、`StageFigure`、`drawMysticFigure`、`StageBackdrop` 和文化道具绘制按上述文件迁移。`MysticStageLayout` 的公开输入只包含 `mode`、`skinId`、`moodLevel`、`onClose` 和内容 Slot；人物 Canvas 不接触 `CompositeDailyFortune`。舞台标题只调用 `MysticGuideGenerator.personaName(mode)`，不显示 `sceneLabel`。

- [ ] **步骤 4：调整人物比例和动画**

人物区域占舞台上半区 55%–65%；底部对话面板使用 `navigationBarsPadding()`、`imePadding()` 和滚动容器。`reduced-motion` 时禁用位移、旋转、持续呼吸和面部微动，仅保留静态光晕。

- [ ] **步骤 5：运行定向测试、编译并提交**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.ui.components.MysticCultureVoiceTest" --tests "com.xuanji.app.ui.components.MysticCultureSpecTest" :app:compileDebugKotlin --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
git add app/src/main/java/com/xuanji/app/ui/components/MysticStageLayout.kt app/src/main/java/com/xuanji/app/ui/components/MysticFigureCanvas.kt app/src/main/java/com/xuanji/app/ui/components/MysticCultureBackdrop.kt app/src/main/java/com/xuanji/app/ui/components/MysticFloatingGuide.kt app/src/main/java/com/xuanji/app/ui/components/MysticCultureSpec.kt app/src/test/kotlin/com/xuanji/app/ui/components/MysticCultureVoiceTest.kt
git commit -m "refactor(舞台): 拆分人物画布并统一文化差异"
```

## 任务 8：拆分卡面消息列表和记忆面板

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/ui/components/MysticMessageList.kt`
- 创建：`app/src/main/java/com/xuanji/app/ui/components/MysticMemorySection.kt`
- 创建：`app/src/main/java/com/xuanji/app/ui/components/MysticConversationStateHost.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/components/MysticGuideCard.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/components/MysticConversationPanel.kt`

- [ ] **步骤 1：为消息列表写纯 UI Model 测试**

```kotlin
@Test
fun stale_and_system_messages_keep_distinct_roles() {
    val model = MysticMessageListModel.from(
        listOf(
            MysticMessage(1, 2, MysticMessageRole.User, "工作怎么办"),
            MysticMessage(1, 2, MysticMessageRole.System, "旧回复已取消")
        )
    )
    assertEquals(listOf(MysticMessageRole.User, MysticMessageRole.System), model.roles)
}
```

- [ ] **步骤 2：实现 UI Model 和 Composable**

消息列表只消费 `List<MysticMessage>` 和 `List<String>` 澄清入口；记忆区只消费 `SoftMemoryTag` 与现有 `RecallFacts`，不从生成器读取隐式状态。

- [ ] **步骤 3：替换卡面内联实现**

保留 `MysticGuideCard` 的公共参数、`MysticSessionState` 提升方式和 `stageCostumeRequest` 回调；把消息、输入、软标签面板和舞台内容通过 Slot 接回。

- [ ] **步骤 4：运行全部 domain/UI 模型测试并提交**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.MysticSessionStateTest" --tests "com.xuanji.app.ui.components.MysticCultureSpecTest" :app:compileDebugKotlin --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
git add app/src/main/java/com/xuanji/app/ui/components/MysticMessageList.kt app/src/main/java/com/xuanji/app/ui/components/MysticMemorySection.kt app/src/main/java/com/xuanji/app/ui/components/MysticConversationStateHost.kt app/src/main/java/com/xuanji/app/ui/components/MysticGuideCard.kt app/src/main/java/com/xuanji/app/ui/components/MysticConversationPanel.kt
git commit -m "refactor(卡面): 拆分消息列表与记忆面板"
```

## 任务 9：继续拆分 Generator 主题回答并保护确定性

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/domain/MysticDialogueTopicTemplates.kt`
- 创建：`app/src/main/java/com/xuanji/app/domain/MysticCultureVoice.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticGuideGenerator.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticDialogueTemplates.kt`
- 创建：`app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueDeterminismTest.kt`

- [ ] **步骤 1：先写确定性和边界失败测试**

```kotlin
@Test
fun same_context_and_input_keep_the_same_reply_after_template_split() {
    val first = engine.reply(testDialogueContext(skinId = "academy-gown"), "今天工作怎么办")
    val second = engine.reply(testDialogueContext(skinId = "academy-gown"), "今天工作怎么办")
    assertEquals(first, second)
}

@Test
fun generated_reply_never_claims_a_new_fortune_score() {
    val reply = engine.reply(testDialogueContext(fortune = fortune(overallScore = 72)), "今天工作怎么办")
    assertFalse(reply.text.contains("99 分"))
}
```

- [ ] **步骤 2：迁移主题回答和语气表**

把 `topicAnswer`、文化 skin 的 voice suffix、fortune/daily/chat 主题模板按纯 Kotlin Module 迁移；`MysticGuideGenerator.customAnswer` 只保留分析、路由和唯一 `MysticSafetyGuard.enforce` 调用。

- [ ] **步骤 3：同步契约扫描**

让 `_dev/dialogue_contract_test.js` 扫描新的模板文件，并继续禁止第二个未经过 guard 的 `return when (intent)`。

- [ ] **步骤 4：运行并提交**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.xuanji.app.domain.MysticDialogueDeterminismTest" --tests "com.xuanji.app.domain.MysticGuideGeneratorTest" --tests "com.xuanji.app.domain.MysticSafetyGuardTest" --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
node _dev/dialogue_contract_test.js
git add app/src/main/java/com/xuanji/app/domain/MysticDialogueTopicTemplates.kt app/src/main/java/com/xuanji/app/domain/MysticCultureVoice.kt app/src/main/java/com/xuanji/app/domain/MysticGuideGenerator.kt app/src/main/java/com/xuanji/app/domain/MysticDialogueTemplates.kt app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueDeterminismTest.kt _dev/dialogue_contract_test.js
git commit -m "refactor(生成器): 拆分主题回答并保持确定性"
```

## 任务 10：扩充契约与测试矩阵

**文件：**
- 修改：`_dev/dialogue_contract.json`
- 修改：`_dev/dialogue_contract_test.js`
- 修改：`app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueEngineTest.kt`
- 修改：`app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueContinuityTest.kt`
- 修改：`app/src/test/kotlin/com/xuanji/app/domain/SoftMemoryTagTest.kt`
- 修改：`app/src/androidTest/kotlin/com/xuanji/app/ui/components/MysticConversationPanelTest.kt`

- [ ] **步骤 1：增加 golden 输入**

加入多主题、实体、低置信度、两个澄清入口、软标签撤回、在线失败回退、舞台标题和新文化语气；每条必须引用真实的 `Suite.case`。

- [ ] **步骤 2：强化契约脚本**

增加以下交叉校验：

```js
assert(contract.stage_title === 'persona-only');
assert(contract.dialogue_analysis.primary_topic_required === true);
assert(contract.soft_memory.auto_save === true);
assert(contract.soft_memory.user_revoke === true);
```

并扫描源码，确保舞台标题未拼接 `sceneLabel`，软标签不会写入 `RecollectionKind.UserInput`。

- [ ] **步骤 3：运行契约和全量 JVM 测试**

```powershell
node _dev/dialogue_contract_test.js
.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
```

预期：契约 PASS；全量 JVM 测试通过，失败数为 0。

- [ ] **步骤 4：提交**

```powershell
git add _dev/dialogue_contract.json _dev/dialogue_contract_test.js app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueEngineTest.kt app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueContinuityTest.kt app/src/test/kotlin/com/xuanji/app/domain/SoftMemoryTagTest.kt app/src/androidTest/kotlin/com/xuanji/app/ui/components/MysticConversationPanelTest.kt
git commit -m "test(陪伴): 扩充对话澄清与记忆契约矩阵"
```

## 任务 11：完成工程门禁、文档和交接

**文件：**
- 修改：`README.md`
- 修改：`docs/SYSTEMS_OVERVIEW.md`
- 修改：`docs/TECHNICAL_DEBT.md`
- 创建：`docs/superpowers/handoffs/2026-09-12-deep-companion-implementation-handoff.md`
- 修改：`.github/workflows/build.yml`

- [ ] **步骤 1：同步产品边界**

README 和系统概览明确：默认离线、在线 Provider 仅显式扩展、软标签仅本机、舞台标题只显示角色名、当前不新增占卜体系。技术债台账记录三大文件的新行数和未执行真机验证。

- [ ] **步骤 2：确保 CI 顺序**

保持 Node 契约在 Gradle 前执行；工作流继续运行 `testDebugUnitTest lintDebug assembleDebug`。不把 `connectedDebugAndroidTest` 写进无设备的 GitHub runner。

- [ ] **步骤 3：执行最终门禁**

```powershell
git diff --check
node _dev/authentic_systems_contract_test.js
node _dev/dialogue_contract_test.js
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
```

预期：两个 Node 契约 PASS；三项 Gradle 任务 `BUILD SUCCESSFUL`；Lint error 为 0。生成 APK：`app/build/outputs/apk/debug/app-debug.apk`。

- [ ] **步骤 4：写交接并提交**

交接文档必须列出：本轮提交、测试命令和结果、APK 路径、远端 CI 是否已重跑、未跟踪用户成果未清理、手机/TalkBack/Logcat 仍等待用户通知。

```powershell
git add README.md docs/SYSTEMS_OVERVIEW.md docs/TECHNICAL_DEBT.md docs/superpowers/handoffs/2026-09-12-deep-companion-implementation-handoff.md .github/workflows/build.yml
git commit -m "docs(陪伴): 记录深陪伴实现门禁与交接"
```

## 整体验收矩阵

| 领域 | 通过条件 | 证据边界 |
| --- | --- | --- |
| 对话相关性 | 多主题选主答、低置信度澄清、连续 5 轮承接、旧 token 丢弃 | JVM + golden；无设备不宣称 UI 观感通过 |
| 角色形象 | A 舞台人物比例、标题仅角色名、文化道具/动作/语气均有差异 | Compose 编译/语义测试；真机截图等用户通知后执行 |
| 软记忆 | 自动保存、来源可见、单条撤回、全部清除、损坏降级 | JVM/DataStore seam；跨进程磁盘行为需设备验证 |
| 安全 | 在线文本不能覆盖分数、命盘、棋局、健康/财务边界 | `DialogueReplyValidatorTest`、`MysticSafetyGuardTest`、契约扫描 |
| 工程 | Node 契约、单元测试、Lint、Debug APK 全通过 | 本机证据 + 推送后远端 CI；不把本机结果写成远端结果 |
| 内容边界 | 不新增体系，不伪造 Ifá/Nadi/商业心理常模 | 文档、`authentic_systems_contract_test.js` 和源码 Provider 边界 |
