# v14 卡片表面统一实现计划

> **面向 AI 代理的工作者：** 使用 `superpowers:executing-plans` 在当前会话内逐任务实现；不启动子代理。步骤使用复选框跟踪。保留当前 dirty tree，不提交或推送。

**目标：** 让玄星普通内容卡、趋势卡、维度卡与今日行动卡遵循 v14 的统一表面语言。

**架构：** 在 Compose UI 层集中定义颜色、边线、圆角和渐变 token。`FortuneCard` 负责通用卡片材质；趋势、维度和行动组件只引用其语义变体，不重复声明局部色值。

**技术栈：** Kotlin、Jetpack Compose Material 3、Node 源码契约、Gradle JVM/lint/debug 构建、ADB 实机截图。

---

## 文件清单

- 创建 `app/src/main/java/com/xuanji/app/ui/components/FortuneSurfaceTokens.kt`：集中保存 v14 卡片颜色、描边、尺寸与渐变。
- 修改 `app/src/main/java/com/xuanji/app/ui/components/FortuneComponents.kt`：将 `FortuneCard` 的 Material 默认凸起表面替换为普通内容卡 token。
- 修改 `app/src/main/java/com/xuanji/app/ui/composite/CompositeFortuneScreen.kt`：将趋势主卡、摘要标签和维度小卡改用语义 token。
- 修改 `app/src/main/java/com/xuanji/app/ui/components/DailyActionCards.kt`：让饮食、活动、出行使用原型对应的细腻渐变与描边。
- 修改 `_dev/xuanji_v14_shell_contract_test.js`：约束 token 集中定义和角色映射，防止卡片样式再次分散。

## 任务 1：先扩展回归契约

**文件：** 修改 `_dev/xuanji_v14_shell_contract_test.js`。

- [x] **步骤 1：写失败断言**：断言集中 token 文件与通用卡、趋势卡、行动卡的引用，包括描边宽度和渐变映射。
- [x] **步骤 2：运行确认失败**：先后观察到 token 文件缺失、组件未接入及描边宽度未集中导致的预期失败；实现后契约通过。

## 任务 2：建立单一视觉 token 源

**文件：** 创建 `app/src/main/java/com/xuanji/app/ui/components/FortuneSurfaceTokens.kt`。

- [x] **步骤 1：实现 token**：定义普通卡 `#251D34/#382F4A/12dp/0dp elevation`、紧凑卡 `#29213A/#342A48/10dp`、集中 1dp 描边宽度、趋势 radial gradient、饮食暖色 gradient 与活动/出行深色 gradient 及描边；趋势与分类卡圆角分别为 17dp、15dp、13dp。
- [x] **步骤 2：运行契约**：`node _dev/xuanji_v14_shell_contract_test.js` 通过，验证 token 定义及组件引用。

## 任务 3：统一通用卡表面

**文件：** 修改 `app/src/main/java/com/xuanji/app/ui/components/FortuneComponents.kt`。

- [x] **步骤 1：应用普通卡 token**：将 `FortuneCard` 的 16dp 圆角、Material surface 和 4dp elevation 替换为 12dp 统一圆角、普通卡填充、1dp 细描边与 0dp elevation；保留编辑态、卡片重排、分享、详情打开和 `previewContent` 行为。
- [x] **步骤 2：编译**：`:app:compileDebugKotlin` 返回 `BUILD SUCCESSFUL`。

## 任务 4：统一复合页专属层级

**文件：** 修改 `app/src/main/java/com/xuanji/app/ui/composite/CompositeFortuneScreen.kt`。

- [x] **步骤 1：替换局部样式**：趋势主视觉使用 token 的 radial gradient 与 17dp 圆角；摘要 chip 和维度 tile 改用紧凑卡色板、统一细描边及规定圆角；保留当前评分、旧版完整总评详情和每个维度点入详情。
- [x] **步骤 2：运行契约和单测**：v14 shell contract 与 `:app:testDebugUnitTest` 均通过。

## 任务 5：统一今日行动语义变体

**文件：** 修改 `app/src/main/java/com/xuanji/app/ui/components/DailyActionCards.kt`。

- [x] **步骤 1：替换局部样式**：饮食用暖色渐变和暖色细描边，活动/出行用共享暗色系渐变与各自低饱和描边；统一 13–15dp 圆角、padding、标题和说明间距。仅保留类别图标、标题和边线的色差。
- [x] **步骤 2：确认契约**：v14 shell contract 通过，四类表面映射与集中描边宽度均受约束。

## 任务 6：完整验证与实机复核

**文件：** 不再新增源码文件；仅生成正常构建产物与本地截图。

- [x] **步骤 1：运行 Node 契约**：五项 Node 契约均退出码 0。
- [x] **步骤 2：运行 Android 门禁**：`testDebugUnitTest`、`lintDebug`、`assembleDebug`、`assembleDebugAndroidTest` 均通过。
- [x] **步骤 3：安装和检查**：对在线 Xperia XZ2 使用 `adb install -r` 保留数据；检查综合、东方、西方、行动卡和详情页截图，进程存活，所查 Logcat 窗口无 FATAL/ANR。
