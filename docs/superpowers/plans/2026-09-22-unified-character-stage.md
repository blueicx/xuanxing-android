# X3 玄师统一角色舞台实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans）逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将四位来客统一到同一套全屏角色舞台中，让沈砚舟、墨衡、伊芙琳·诺瓦、纳迪尔·拉希德都拥有完整且可辨识的文化场景，同时保留现有对话、角色线程、棋局、默认隐藏浮球、折叠长回复与无障碍行为。

**架构：** 舞台固定为五层：场景底板、统一遮罩、人物层、顶部角色信息、底部陪伴抽屉。场景由 `MysticCharacterProfile.sceneId` 驱动，人物资源缺失时回退到同一角色配色的 Canvas 形象；不再以 `usesScenePlate` 对墨衡走另一套布局。角色目录、盘面算法、对话生成器、DataStore 和游戏规则不改职责，舞台只消费角色资料与回调。

**技术栈：** Kotlin 1.9.22、Jetpack Compose Material 3、Compose UI Test、JUnit 4、现有本地 drawable/Canvas 资源；继续支持 API 26、reduced-motion、状态栏/导航栏/IME insets 和离线默认行为。

**设计依据：** `docs/superpowers/specs/2026-09-22-unified-character-stage-design.md`。该规格已由用户确认；本计划只实现统一舞台，不新增占卜体系、联网能力、模型调用或长期记忆规则。

---

## 任务 1：建立统一场景模型与回退契约

**目标文件：**

- 新增 `app/src/main/java/com/xuanji/app/ui/components/MysticSceneSpec.kt`
- 新增 `app/src/test/kotlin/com/xuanji/app/ui/components/MysticSceneSpecTest.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/components/MysticCharacterUiModel.kt`
- 修改 `app/src/test/kotlin/com/xuanji/app/ui/components/MysticCharacterUiModelTest.kt`

### 步骤

- [ ] 先在 `MysticSceneSpecTest` 写失败测试：四个 `sceneId` 必须映射到唯一的稳定场景；未知 ID 必须回退到 `ink_fallback`；每个场景必须有非空标题、文化标签、调色板和场景语义描述。
- [ ] 写失败测试约束 `MysticCharacterUiModel.from(profile)` 暴露 `sceneId`/场景规格，而不是 `usesScenePlate` 布尔分支；四角色均声明 `hasCompleteBackdrop=true`。
- [ ] 运行 `./gradlew :app:testDebugUnitTest --tests '*MysticSceneSpecTest' --tests '*MysticCharacterUiModelTest'`，确认新断言先失败且失败原因是缺少统一场景映射。
- [ ] 实现 `MysticSceneSpec`：定义 `SceneId`、标题、文化灵感、背景层类型、低对比度回退颜色、可访问描述和角色强调色；为 `jiangnan_triptych`、`ink_elder`、`academy_triptych`、`silkroad_triptych` 提供四条资料，为未知值提供稳定水墨回退。
- [ ] 将 `MysticCharacterUiModel` 的 `usesScenePlate` 替换为 `sceneSpec` 或等价的稳定场景引用，保留现有 actions、specialties 和 game 主题字段，避免改变角色推荐、seed、线程和棋局行为。
- [ ] 重新运行上述两组测试，确认绿色；再运行 `./gradlew :app:testDebugUnitTest`，确认现有 JVM 测试无回归。
- [ ] 提交小步变更：`test(stage): 锁定四角色统一场景与回退契约`，再提交实现：`refactor(stage): 引入角色场景规格`。

**验收证据：** 测试能证明四个角色都有完整场景，未知资源不会得到透明/纯黑空白层，且旧 `MysticCharacterUiModel` 动作与角色游戏断言保持通过。

## 任务 2：实现统一五层舞台与四套文化背景

**目标文件：**

- 修改 `app/src/main/java/com/xuanji/app/ui/components/MysticStageLayout.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/components/MysticCultureBackdrop.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/components/MysticFigureAsset.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/components/MysticFigureCanvas.kt`
- 必要时新增 `app/src/main/java/com/xuanji/app/ui/components/MysticScenePlate.kt`
- 必要时新增 `app/src/main/java/com/xuanji/app/ui/components/MysticStageScrim.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/components/MysticFloatingGuide.kt`（仅删除舞台重复 helper 的调用或转发，不改浮球状态机）

### 步骤

- [ ] 先为 `MysticStageLayout` 增加 Compose 语义测试用例骨架（见任务 3），并在实现前确认当前测试能复现墨衡与其他角色的布局分支差异。
- [ ] 把场景渲染抽成 `MysticScenePlate`：根据 `MysticSceneSpec` 画出统一的底色、文化地平线、主道具和低对比度纹理；四套场景都必须占满舞台，不依赖人物 PNG 自带背景。
- [ ] 为沈砚舟绘制水榭、月桥、灯笼和远山；为墨衡绘制完整案头、宣纸、砚台和山屏；为伊芙琳绘制观测台、星图刻度和档案桌；为纳迪尔绘制驿站拱门、沙丘、铜盘和灯火。所有元素都使用场景规格的 palette，不能只更换背景色。
- [ ] 保留 `MysticFigureAsset` 的本地位图优先策略，但让 PNG 作为人物层而不是场景底板；资源缺失时调用 `MysticFigureCanvas`，且人物层仍有相同的语义描述和最小可读对比度。
- [ ] 在 `MysticStageLayout` 中移除 `if (usesScenePlate)` 两套布局，固定按五层顺序渲染：`ScenePlate`、`SceneScrim`、`CharacterLayer`、`StageHeader`、`CompanionDrawer`。人物高度、底部安全区和信息抽屉使用统一约束；不再允许墨衡独自缩进或悬空。
- [ ] 将顶部标题、文化灵感、当前专长、关闭按钮和画廊放入统一 header 区；底部动作栏、专长展开内容和 `content()` 保持抽屉层，不能压住人物关键五官、分数卡或棋盘。
- [ ] 使用 `statusBarsPadding()`、`navigationBarsPadding()`、`imePadding()` 与 `WindowInsets` 组合，确保窄屏、横屏和键盘打开时场景仍有可见人物与完整关闭入口；reduced-motion 时不启用场景位移和持续旋转。
- [ ] 收敛 `MysticFloatingGuide.kt` 中与新场景重复的 `drawCulturalScene`/`StageBackdrop` 路径，只保留未被新舞台引用的历史浮球绘制逻辑，避免同一舞台叠两层背景；保持 `MysticImmersiveStage` 的窗口 inset 和回调签名。
- [ ] 运行 `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`，确认 Kotlin 编译和纯 Kotlin 回归通过；提交 `feat(stage): 统一四角色场景与人物层`。

**验收证据：** 四角色都进入同一布局路径；墨衡拥有与其他角色等价的完整场景；PNG、Canvas 回退和场景背景各自只有一层，不出现背景重复或纯透明舞台。

## 任务 3：统一信息密度、悬浮气泡与无障碍语义

**目标文件：**

- 修改 `app/src/main/java/com/xuanji/app/ui/components/MysticStageLayout.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/components/MysticCharacterUiModel.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/components/MysticGuideCard.kt`（仅在舞台容器需要时调整气泡/折叠入口）
- 新增或修改 `app/src/androidTest/kotlin/com/xuanji/app/ui/components/MysticStageLayoutTest.kt`
- 修改 `app/src/androidTest/kotlin/com/xuanji/app/ui/components/MysticCharacterGalleryTest.kt`
- 修改 `app/src/test/kotlin/com/xuanji/app/ui/components/MysticCharacterUiModelTest.kt`

### 步骤

- [ ] 先写 Compose UI 测试：舞台能找到当前角色姓名、文化灵感、专长描述、关闭按钮、画廊语义和三个动作按钮；长回复默认显示预览并能通过明确的“展开完整回复”语义展开。
- [ ] 写测试覆盖四角色切换：切换只触发 `onCharacterSelected`，不丢失动作入口或改变 `CompanionGameCatalog` 里的游戏；关闭按钮只调用 `onClose`，不清除线程或软记忆。
- [ ] 写窄屏语义测试（使用 `setContent` 与固定小尺寸）：角色姓名、关闭入口和至少一个主要动作仍存在；测试不依赖像素截图，不把未执行设备测试冒充通过。
- [ ] 实现底部 `CompanionDrawer` 的统一视觉：默认只显示标题、文化灵感、主专长和三项动作；专长列表按点击展开，长对话沿用 `MysticDialogueBubbleModel` 的折叠状态；依据、分数、每日行动和棋盘内容继续由现有 `content()` 管理并放在抽屉滚动区域。
- [ ] 为关闭按钮补足至少 48dp 触控尺寸、`contentDescription = "关闭玄师台"`、键盘可达语义和角色姓名朗读顺序；为画廊卡和动作按钮保留稳定 test tag/描述，TalkBack 不朗读重复的装饰性背景。
- [ ] 明确 reduced-motion 分支：不改变布局、不持续旋转、不使用人物位移，只保留静态场景和必要透明度变化；保留现有浮球 reduced-motion 行为。
- [ ] 运行 `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin`；若 Compose 测试需要真机运行，先确保编译通过并在计划验收记录中标记“设备待用户通知”。提交 `test(stage): 覆盖统一舞台语义与窄屏契约`，再提交 `feat(stage): 收敛陪伴抽屉信息密度`。

**验收证据：** JVM 与 AndroidTest 编译均通过；测试明确锁定标题、关闭、动作、展开/折叠和小屏存在性；不会因为舞台视觉重构而删除对话、游戏或记忆入口。

## 任务 4：清理旧分支、契约与文档同步

**目标文件：**

- 修改 `app/src/main/java/com/xuanji/app/ui/components/MysticCharacterUiModel.kt`
- 修改 `app/src/test/kotlin/com/xuanji/app/ui/components/MysticCharacterUiModelTest.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/components/MysticStageLayout.kt`
- 修改 `app/src/main/java/com/xuanji/app/ui/components/MysticFloatingGuide.kt`
- 修改 `_dev/dialogue_contract_test.js` 或新增 `_dev/unified_stage_contract_test.js`（仅当现有契约入口无法表达场景约束）
- 修改 `README.md`
- 修改 `SYSTEMS_OVERVIEW.md`
- 修改 `docs/TECHNICAL_DEBT.md`

### 步骤

- [ ] 更新单测，把原先断言“前三个使用 scene plate、墨衡不使用”的测试改成“四个角色都使用统一场景规格、墨衡 sceneId 为 `ink_elder`、未知 ID 有水墨回退”。
- [ ] 全仓搜索并移除生产代码对 `usesScenePlate` 的读取；若为兼容既有测试而保留字段，必须让它成为由统一场景规格派生的只读值，禁止继续作为布局分支。
- [ ] 清理确认无引用的旧舞台背景 helper；每次删除前用 `rg` 验证调用点，不能删除仍被默认浮球或旧回退使用的函数。
- [ ] 将 README、系统总览和技术债台账写明：四角色统一全屏场景、墨衡不再是透明例外、图片缺失回退、设备截图/TalkBack/Logcat 仍需用户通知后完成；不宣称未执行的设备验收。
- [ ] 若新增 Node 契约，锁定四个 `sceneId`、统一五层顺序、旧布尔分支不再参与布局、四个角色的场景标题和回退文案；运行 `node _dev/unified_stage_contract_test.js`。
- [ ] 运行 `rg -n "usesScenePlate|if \(.*usesScenePlate" app/src/main app/src/test app/src/androidTest _dev README.md SYSTEMS_OVERVIEW.md docs/TECHNICAL_DEBT.md`，确认生产代码不再以旧布尔值分支布局；另行检查本次改动没有留下未完成标记。
- [ ] 提交 `docs(stage): 同步统一角色舞台契约与边界`。

**验收证据：** 生产代码不再以角色身份走两套舞台；文档和契约能区分源码门禁与尚未执行的设备视觉验收。

## 任务 5：完整门禁与设备验收记录

**目标文件：**

- 不新增源码文件；将命令输出和结果更新到 `docs/TECHNICAL_DEBT.md` 与本轮交接文档（如仓库已有对应路径，则更新最新文件）

### 步骤

- [ ] 运行 `node _dev/authentic_systems_contract_test.js`。
- [ ] 运行 `node _dev/dialogue_contract_test.js`。
- [ ] 运行 `./gradlew :app:testDebugUnitTest --rerun`，记录实际测试数和失败数。
- [ ] 运行 `./gradlew :app:lintDebug`，记录 error/warning；error 必须为 0，既有 warning 分级记录。
- [ ] 运行 `./gradlew :app:assembleDebug`，记录 APK 绝对路径、文件大小和 SHA-256。
- [ ] 运行 `./gradlew :app:compileDebugAndroidTestKotlin`，确保新增 Compose 测试可编译；没有设备时不得把编译当作 UI 通过。
- [ ] 如用户之后通知手机复测，再执行 `adb devices`、确认包名 `com.xuanji.app`、安装最新 APK、启动、截图四角色舞台和关闭/浮球回退路径、收集 Logcat，并单独检查 TalkBack、reduced-motion、旋转、返回键和窄屏遮挡；设备未连接时在文档写明“未验证”。
- [ ] 最后用 `git status --short` 检查只包含本计划涉及的源码/文档和用户已有成果，不清理 `.superpowers/`、截图、UI dump、`work/` 或其他未跟踪文件；提交 `chore(stage): 完成统一角色舞台门禁记录`。

**最终验收：** Node 契约、JVM 单测、lint、Debug APK 和 AndroidTest 编译全部通过；四角色场景、统一布局、动作入口、长回复折叠与无障碍语义有源码/测试证据；真实手机视觉、TalkBack、Logcat 只有在用户通知后执行并单独报告。

## 执行纪律

- 每个任务先写失败测试，再写最小实现；测试绿后再做重构和绘制细化。
- 每个小任务单独提交，提交前只检查相关文件，绝不回退或清理用户已有 dirty tree。
- 不修改角色算法、出生资料、盘面事实、对话 seed、DataStore schema、棋局规则或网络权限。
- 不下载新图片、不加入在线模型、不改变默认离线行为；已有 PNG 仅作为人物层，缺失时用本地 Canvas 回退。
- 任何设备、TalkBack、Logcat、真实触控和截图结论都必须有当次命令或设备输出证据。
