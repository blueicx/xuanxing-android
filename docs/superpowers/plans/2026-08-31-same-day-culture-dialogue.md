# 同日生内容、B+C 角色与对话承接实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在不改变离线默认的前提下，为同日生页增加可折叠长评语与公版音乐/诗歌卡，并用 B+C 文化道具/场景和上下文承接修复角色交流体验。

**架构：** 领域层提供确定性的作品目录与文化视觉规格；HistoryScreen 只负责折叠/展示；MysticFloatingGuide 根据 `skinId` 渲染道具和场景；MysticDialogueEngine 将当前输入与最近回合合成为可解释的承接上下文。所有输出继续经过现有安全边界，不接入网络模型。

**技术栈：** Kotlin/JUnit4、Jetpack Compose Material3、Canvas、现有离线确定性 hash。

**执行状态（2026-08-31）：** 任务 1–5 已完成；Android 48 项单测、lint（0 errors）和 debug assemble 已通过。手机复测按需求暂缓，未将旧设备截图当作本轮证据。

---

### 任务 1：作品目录与同日生模型

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/domain/SameDayWorks.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/SameDayBirth.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/SameDayWorksTest.kt`

- [ ] **步骤 1：编写失败测试**

```kotlin
@Test fun works_are_deterministic_and_include_public_domain_music_and_poetry() {
    val first = SameDayWorks.forDate(LocalDate.of(2026, 8, 31))
    assertEquals(first, SameDayWorks.forDate(LocalDate.of(2026, 8, 31)))
    assertTrue(first.any { it.kind == WorkKind.MUSIC })
    assertTrue(first.any { it.kind == WorkKind.POETRY && it.publicDomain })
}

@Test fun copyrighted_work_never_exposes_excerpt() {
    SameDayWorks.CATALOG.filterNot { it.publicDomain }.forEach { assertEquals(null, it.excerpt) }
}
```

- [ ] **步骤 2：运行测试确认失败**

运行：`.\gradlew.bat :app:testDebugUnitTest --tests com.xuanji.app.domain.SameDayWorksTest --no-daemon --console=plain`
预期：FAIL，报错 `Unresolved reference: SameDayWorks`。

- [ ] **步骤 3：实现最少目录与模型**

```kotlin
enum class WorkKind { MUSIC, POETRY }
data class SameDayWork(
    val monthDay: String, val kind: WorkKind, val title: String,
    val creator: String, val year: Int?, val style: String,
    val publicDomain: Boolean, val excerpt: String? = null, val note: String
)

object SameDayWorks {
    val CATALOG = listOf(
        SameDayWork("08-31", WorkKind.MUSIC, "月光", "德彪西", 1890, "印象主义钢琴", true, null, "适合把一天收束下来。"),
        SameDayWork("08-31", WorkKind.POETRY, "天真与经验之歌", "威廉·布莱克", 1794, "英国浪漫主义", true, "诗集以童真与经验的对照观察人心。", "公版短摘录。"),
        SameDayWork("08-31", WorkKind.MUSIC, "夜曲 Op.9 No.2", "肖邦", 1832, "浪漫主义钢琴", true, null, "轻声聆听即可，不必把它当成答案。")
    )
    fun forDate(date: LocalDate): List<SameDayWork> {
        val key = date.format(DateTimeFormatter.ofPattern("MM-dd"))
        val matched = CATALOG.filter { it.monthDay == key }
        return if (matched.isNotEmpty()) matched else CATALOG.take(2)
    }
}
```

`SameDayBirth.Figure` 增加 `summary: String = note`，旧构造调用保持兼容。

- [ ] **步骤 4：运行测试确认通过**

运行同上；预期：2 tests passed。

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/xuanji/app/domain/SameDayWorks.kt app/src/main/java/com/xuanji/app/domain/SameDayBirth.kt app/src/test/kotlin/com/xuanji/app/domain/SameDayWorksTest.kt
git commit -m "feat: add deterministic same-day music and poetry catalog"
```

### 任务 2：同日生页面折叠评语与作品卡

**文件：**
- 修改：`app/src/main/java/com/xuanji/app/ui/history/HistoryScreen.kt`
- 创建：`app/src/main/java/com/xuanji/app/ui/history/HistoryCopy.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/SameDayWorksTest.kt`

- [ ] **步骤 1：先增加纯函数显示规则测试**

```kotlin
@Test fun summary_is_shorter_than_full_note() {
    val text = "第一句摘要。第二句补充。第三句展开细节。"
    assertEquals("第一句摘要。", HistoryCopy.summary(text))
}
```

- [ ] **步骤 2：运行确认失败**

运行：`.\gradlew.bat :app:testDebugUnitTest --tests com.xuanji.app.domain.SameDayWorksTest --no-daemon --console=plain`
预期：FAIL，报错 `Unresolved reference: HistoryCopy`。

- [ ] **步骤 3：实现摘要纯函数、Compose 折叠和作品区域**

新增 `HistoryCopy.summary(text)`，以首个完整句作为折叠摘要并处理空文本；使用 `rememberSaveable(fig.name, fig.date) { mutableStateOf(false) }` 保存展开状态；全文放进 `AnimatedVisibility(visible = expanded)`，按钮使用 `TextButton` 且 contentDescription 为“展开评语/收起评语”。在同日生列表后调用 `SameDayWorks.forDate(sameDayDate).forEach { SameDayWorkCard(it) }`，公版 `excerpt` 才渲染正文，非公版仅渲染元数据。

- [ ] **步骤 4：运行单测与编译**

运行：`.\gradlew.bat :app:testDebugUnitTest :app:compileDebugKotlin --no-daemon --console=plain`
预期：BUILD SUCCESSFUL，测试 0 failures。

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/xuanji/app/ui/history/HistoryScreen.kt app/src/main/java/com/xuanji/app/ui/history/HistoryCopy.kt app/src/test/kotlin/com/xuanji/app/domain/SameDayWorksTest.kt
git commit -m "feat: collapse same-day notes and show works"
```

### 任务 3：B+C 文化道具与场景层

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/ui/components/MysticCultureSpec.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/components/MysticFloatingGuide.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/MysticCultureSpecTest.kt`

- [ ] **步骤 1：编写失败测试**

```kotlin
@Test fun every_skin_has_structural_prop_and_scene() {
    MysticCultureSpec.ALL_SKINS.forEach { spec ->
        assertTrue(spec.structuralProp.isNotBlank())
        assertTrue(spec.scene.isNotBlank())
        assertNotEquals(spec.structuralProp, spec.garmentOnlyLabel)
    }
}
```

- [ ] **步骤 2：运行确认失败**

运行：`.\gradlew.bat :app:testDebugUnitTest --tests com.xuanji.app.domain.MysticCultureSpecTest --no-daemon --console=plain`
预期：FAIL，报错 `Unresolved reference: MysticCultureSpec`。

- [ ] **步骤 3：实现规格与 Canvas 分支**

`MysticCultureSpec` 为 8 个现有 `skinId` 提供 `structuralProp`、`scene`、`gesture`。`MysticImmersiveStage` 在人物后方调用 `drawCultureScene(scene, ...)`；`drawCulturalCostume` 增加可辨识几何结构（交领折线、翻领三角、披肩弧线、头巾带、鼓/经卷/沙漏轮廓），保留颜色仅作辅助手段。场景 alpha 不超过 0.18，关闭按钮和对话区使用更高 zIndex；`rememberReducedMotion()` 为 true 时不绘制位移和旋转动画。

- [ ] **步骤 4：运行测试和编译**

运行：`.\gradlew.bat :app:testDebugUnitTest :app:lintDebug --no-daemon --console=plain`
预期：BUILD SUCCESSFUL，lint 0 errors。

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/xuanji/app/ui/components/MysticCultureSpec.kt app/src/main/java/com/xuanji/app/ui/components/MysticFloatingGuide.kt app/src/test/kotlin/com/xuanji/app/domain/MysticCultureSpecTest.kt
git commit -m "feat: add cultural props and stage scenes"
```

### 任务 4：对话承接与答所问约束

**文件：**
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticDialogueEngine.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticIntentClassifier.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticGuideGenerator.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueEngineTest.kt`

- [ ] **步骤 1：编写失败测试**

```kotlin
@Test fun follow_up_uses_recent_topic_before_fortune_dump() {
    val reply = engine.reply(context.copy(recentTurns = listOf(MysticTurn("最近工作怎么样", "先看工作", "career"))), "那我该先做什么？")
    assertEquals(MysticIntent.Action, reply.intent)
    assertTrue(reply.text.contains("事业") || reply.text.contains("工作"))
}

@Test fun explicit_topic_switch_drops_old_topic() {
    val reply = engine.reply(context.copy(recentTurns = listOf(MysticTurn("最近工作怎么样", "先看工作", "career"))), "换个话题，今天运势呢")
    assertEquals(MysticIntent.Fortune, reply.intent)
    assertFalse(reply.text.contains("只谈事业"))
}
```

- [ ] **步骤 2：运行确认失败**

运行：`.\gradlew.bat :app:testDebugUnitTest --tests com.xuanji.app.domain.MysticDialogueEngineTest --no-daemon --console=plain`
预期：至少一项 FAIL，因为当前 `reply` 没有读取 `recentTurns`。

- [ ] **步骤 3：实现承接解析**

新增 `resolveIntent(context, input)`：先用 `MysticIntentClassifier` 分类；若结果为 `Chat`/`Action` 且输入含“那/然后/接着/先做什么”等承接词，则读取最近一条 `MysticTurn.kind` 作为主题；若输入含“换个话题/不说这个”则跳过历史主题。把解析后的 `MysticIntent` 传给 `customAnswer`，并在 `actionAnswer` 前加入主题标签。回复首句必须包含输入主题词或对应维度标签；问候/感谢继续不回显原句。

- [ ] **步骤 4：运行回归测试**

运行：`.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain`
预期：全部测试通过，健康/财富用例仍不出现诊断、保证收益或买卖建议。

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/xuanji/app/domain/MysticDialogueEngine.kt app/src/main/java/com/xuanji/app/domain/MysticIntentClassifier.kt app/src/main/java/com/xuanji/app/domain/MysticGuideGenerator.kt app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueEngineTest.kt
git commit -m "fix: make mystic replies context-aware"
```

### 任务 5：整体验证与文档

**文件：**
- 修改：`docs/SYSTEMS_OVERVIEW.md`
- 修改：`docs/TECHNICAL_DEBT.md`

- [ ] **步骤 1：补充离线/版权/视觉边界文档**

写明作品目录的公版摘录规则、B+C 文化表现为视觉演绎、对话承接不代表真实占断；记录手机复测暂缓。

- [ ] **步骤 2：运行完整验证**

运行：`.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon --console=plain`
预期：BUILD SUCCESSFUL；测试无 failures；lint 0 errors。随后运行小程序既有 7 套测试，确认契约未回归。

- [ ] **步骤 3：检查变更并 Commit**

```bash
git diff --check
git add docs/SYSTEMS_OVERVIEW.md docs/TECHNICAL_DEBT.md
git commit -m "docs: record same-day and B+C dialogue boundaries"
```
