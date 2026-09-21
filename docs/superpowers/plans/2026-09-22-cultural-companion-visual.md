# X3 文化皮肤与象棋入口实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将玄师舞台升级为三套真正不同的文化视觉皮肤，并让真实中国象棋在快捷区可发现。

**架构：** `MysticSkin.visualStyleId` 负责领域层稳定映射；`MysticFigureAsset` 负责 UI 资源选择；`MysticFigureCanvas` 负责资源渲染与 Canvas 回退。现有对话、命盘、记忆和棋局状态不改写，象棋快捷按钮只复用已有游戏命令。

**技术栈：** Kotlin、Jetpack Compose、Android drawable-nodpi WebP、纯 Kotlin/JVM 测试、Node 契约脚本、Gradle。

---

### 任务 1：锁定失败契约与资源命名

**文件：**
- 修改：`_dev/dialogue_contract.json`，新增 `visual_companion` 段和快捷入口 golden 项。
- 修改：`_dev/dialogue_contract_test.js`，读取该段并检查皮肤映射、资源文件、快捷文本与 Compose 来源。
- 测试：`_dev/dialogue_contract_test.js`。

- [ ] **步骤 1：编写失败契约**

在 JSON 增加：

```json
"visual_companion": {
  "style_ids": ["ink_scholar", "cel_astrologer", "lowpoly_guardian"],
  "skin_style_map": {
    "jiangnan-robe": "ink_scholar",
    "cloud-daoist": "ink_scholar",
    "academy-gown": "cel_astrologer",
    "street-jacket": "cel_astrologer",
    "festival-costume": "cel_astrologer",
    "silkroad-robe": "lowpoly_guardian",
    "northland-mantle": "lowpoly_guardian",
    "desert-traveler": "lowpoly_guardian"
  },
  "resource_names": ["mystic_figure_ink", "mystic_figure_cel", "mystic_figure_lowpoly"],
  "quick_game_prompt": "来一盘象棋",
  "verify": "MysticSkinTest.visual_styles_are_bounded_and_mapped"
}
```

在脚本中先断言 `MysticFigureAsset.kt`、三个 drawable 文件、`visualStyleId` 和 `quick_game_prompt` 都存在。此时应因文件尚不存在而失败。

运行：`node _dev/dialogue_contract_test.js`

预期：FAIL，报错缺少 `visual_companion` 的实现文件或资源。

- [ ] **步骤 2：运行确认红灯**

运行同一命令并保存失败断言文本；不要修改任何现有用户未跟踪产物。

- [ ] **步骤 3：提交契约的最小实现前置**

只提交 JSON 与 Node 检查，不修改生产 Kotlin；提交：

```bash
git add _dev/dialogue_contract.json _dev/dialogue_contract_test.js
git commit -m "test: contract cultural companion visuals"
```

### 任务 2：增加稳定视觉风格映射

**文件：**
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticGuideGenerator.kt:91-180`。
- 创建：`app/src/test/kotlin/com/xuanji/app/domain/MysticSkinTest.kt`。

- [ ] **步骤 1：编写失败的 JVM 测试**

```kotlin
@Test
fun visual_styles_are_bounded_and_mapped() {
    val allowed = setOf("ink_scholar", "cel_astrologer", "lowpoly_guardian")
    val all = listOf("scholar", "half").flatMap(MysticGuideGenerator::mysticSkins)
    assertEquals(8, all.size)
    assertTrue(all.all { it.visualStyleId in allowed })
    assertEquals("ink_scholar", all.first { it.id == "jiangnan-robe" }.visualStyleId)
    assertEquals("cel_astrologer", all.first { it.id == "academy-gown" }.visualStyleId)
    assertEquals("lowpoly_guardian", all.first { it.id == "desert-traveler" }.visualStyleId)
    assertEquals(3, all.map { it.visualStyleId }.toSet().size)
}
```

运行：`.\gradlew.bat :app:testDebugUnitTest --tests com.xuanji.app.domain.MysticSkinTest`

预期：FAIL，`MysticSkin` 尚无 `visualStyleId`。

- [ ] **步骤 2：增加最小领域字段**

在 `MysticSkin` 末尾增加：

```kotlin
val visualStyleId: String = "ink_scholar"
```

为八个既有构造调用使用命名参数写入任务 1 的三值映射，保持现有 ID、颜色、语气和 deterministic seed 不变。

- [ ] **步骤 3：运行测试确认绿灯**

运行：`.\gradlew.bat :app:testDebugUnitTest --tests com.xuanji.app.domain.MysticSkinTest`

预期：PASS。

- [ ] **步骤 4：提交领域映射**

```bash
git add app/src/main/java/com/xuanji/app/domain/MysticGuideGenerator.kt app/src/test/kotlin/com/xuanji/app/domain/MysticSkinTest.kt
git commit -m "feat: add cultural visual style ids"
```

### 任务 3：生成并放入三套透明角色资源

**文件：**
- 创建：`app/src/main/res/drawable-nodpi/mystic_figure_ink.webp`。
- 创建：`app/src/main/res/drawable-nodpi/mystic_figure_cel.webp`。
- 创建：`app/src/main/res/drawable-nodpi/mystic_figure_lowpoly.webp`。

- [ ] **步骤 1：生成透明资源**

使用内置 image generation，分别生成全身透明 cutout；保留 A 水墨老年玄学家、B 图形化二次元星象角色、C 低多边形沙海守望者的轮廓、道具和材质，不把背景烘进资源。输出复制到上述三个 `drawable-nodpi` 路径。

- [ ] **步骤 2：检查资源约束**

运行：`Get-ChildItem app/src/main/res/drawable-nodpi/mystic_figure_*.webp | Select-Object Name,Length`

预期：三份文件存在且非零；文件名只包含小写字母、数字和下划线，未生成网络 URL 或密钥文件。

- [ ] **步骤 3：提交资源**

```bash
git add app/src/main/res/drawable-nodpi/mystic_figure_ink.webp app/src/main/res/drawable-nodpi/mystic_figure_cel.webp app/src/main/res/drawable-nodpi/mystic_figure_lowpoly.webp
git commit -m "feat: add cultural companion figure assets"
```

### 任务 4：接入资源渲染并保留回退

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/ui/components/MysticFigureAsset.kt`。
- 修改：`app/src/main/java/com/xuanji/app/ui/components/MysticFigureCanvas.kt`。
- 修改：`app/src/main/java/com/xuanji/app/ui/components/MysticStageLayout.kt`。
- 测试：`_dev/dialogue_contract_test.js`。

- [ ] **步骤 1：增加资源选择器**

创建：

```kotlin
internal enum class MysticFigureAssetId { Ink, Cel, LowPoly }

internal fun figureAssetId(styleId: String): MysticFigureAssetId = when (styleId) {
    "cel_astrologer" -> MysticFigureAssetId.Cel
    "lowpoly_guardian" -> MysticFigureAssetId.LowPoly
    else -> MysticFigureAssetId.Ink
}
```

并在同文件提供 `@Composable fun MysticFigureAsset(styleId, contentDescription, modifier)`，用 `painterResource` + `Image(contentScale = ContentScale.Fit)` 显示三份 drawable。

- [ ] **步骤 2：把现有 Canvas 变成回退路径**

将 `MysticFigureCanvas` 增加 `styleId: String` 参数：资源 drawable 能加载时调用 `MysticFigureAsset`，否则调用现有 `drawRestrainedFigure`。不改变 `phase`、`reducedMotion`、`contentDescription` 参数语义。

- [ ] **步骤 3：从舞台传入领域风格**

在 `MysticStageLayout` 从 `MysticGuideGenerator.mysticSkinVoice(mode, skinId)` 读取 `visualStyleId`，把它传给 `MysticFigureCanvas`；人物展示区域由 `fillMaxSize(.72f)` 调到 `fillMaxSize(.82f)`，保持顶部 40dp、底部对话 safe-area 和 `navigationBarsPadding()/imePadding()`。

- [ ] **步骤 4：运行契约与构建**

运行：`node _dev/dialogue_contract_test.js`、`.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`

预期：契约 PASS；Gradle `BUILD SUCCESSFUL`；lint 无 error。

- [ ] **步骤 5：提交渲染接入**

```bash
git add app/src/main/java/com/xuanji/app/ui/components/MysticFigureAsset.kt app/src/main/java/com/xuanji/app/ui/components/MysticFigureCanvas.kt app/src/main/java/com/xuanji/app/ui/components/MysticStageLayout.kt _dev/dialogue_contract_test.js
git commit -m "feat: render distinct cultural companion figures"
```

### 任务 5：显式暴露象棋入口

**文件：**
- 修改：`app/src/main/java/com/xuanji/app/ui/components/MysticConversationPanel.kt:25-80`。
- 创建：`app/src/test/kotlin/com/xuanji/app/domain/MysticGameShortcutTest.kt`。

- [ ] **步骤 1：编写失败测试**

```kotlin
@Test
fun game_shortcut_text_is_classified_as_game() {
    assertEquals(MysticIntent.Game, MysticIntentClassifier.classify("来一盘象棋"))
}
```

并在契约脚本断言 `QUICK_PROMPTS` 包含 `visual_companion.quick_game_prompt`。当前分类测试可能已通过，但 UI 契约应先失败。

- [ ] **步骤 2：增加快捷按钮**

将 `QUICK_PROMPTS` 改为：

```kotlin
private val QUICK_PROMPTS = listOf(
    "今日运势", "来一盘象棋", "继续说", "换个话题", "解释刚才", "我只是想聊聊"
)
```

按钮沿用现有 `onQuickPrompt`、busy 禁用、content description 和 `MysticGuideCard.submitPanelInput` 游戏优先分支。

- [ ] **步骤 3：运行测试确认通过**

运行：`.\gradlew.bat :app:testDebugUnitTest --tests com.xuanji.app.domain.MysticGameShortcutTest` 与 `node _dev/dialogue_contract_test.js`。

预期：PASS；按钮文本与分类器、`GameDialogueBridge` 的 `start_xiangqi` 契约一致。

- [ ] **步骤 4：提交入口**

```bash
git add app/src/main/java/com/xuanji/app/ui/components/MysticConversationPanel.kt app/src/test/kotlin/com/xuanji/app/domain/MysticGameShortcutTest.kt _dev/dialogue_contract_test.js
git commit -m "feat: expose xiangqi shortcut in companion chat"
```

### 任务 6：全量门禁与文档同步

**文件：**
- 修改：`docs/SYSTEMS_OVERVIEW.md`，说明三套视觉 ID、透明资源回退、象棋快捷入口。
- 修改：`docs/TECHNICAL_DEBT.md`，把“人物仍是单一几何 Canvas”更新为资源渲染已交付，并保留未做设备视觉验收的事实。
- 修改：`docs/superpowers/handoffs/2026-09-19-expanded-companion-handoff.md`，追加新 APK hash、文化切换与游戏入口验证栏位。

- [ ] **步骤 1：运行全部验证**

```bash
node _dev/authentic_systems_contract_test.js
node _dev/dialogue_contract_test.js
node _dev/action_profile_contract_test.js
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
git diff --check
```

预期：三个 Node 脚本 PASS，Gradle `BUILD SUCCESSFUL`，lint 0 error，diff check 退出码 0。

- [ ] **步骤 2：同步文档中的事实**

只写已观测的本机证据；把手机截图、TalkBack、旋转和 Logcat 作为待用户通知后执行的设备门，不宣称已经完成。

- [ ] **步骤 3：提交并推送**

```bash
git add docs/SYSTEMS_OVERVIEW.md docs/TECHNICAL_DEBT.md docs/superpowers/handoffs/2026-09-19-expanded-companion-handoff.md
git commit -m "docs: document cultural companion visuals and game entry"
git fetch origin
git push origin main
```

- [ ] **步骤 4：交付检查**

运行 `git status -sb`、`git log -1 --oneline --decorate`、`git rev-list --left-right --count HEAD...origin/main`。确认只剩既有未跟踪截图/dump/宣传图，不把它们加入提交。

