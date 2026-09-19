# X3 深陪伴体验设计规格

**状态：** 已获用户确认，进入实现计划前的书面审查。

**目标：** 在保持离线优先和事实安全边界的前提下，重做角色舞台、对话理解和可控记忆，解决人物辨识度不足、文化皮肤像换色、对话答非所问三个核心问题。

**产品选择：** 两位核心角色 + 文化皮肤；结构化离线对话为默认；在线 Provider 仅作为用户主动开启的扩展；本阶段不新增占卜体系。

---

## 1. 用户体验方向

### 1.1 角色与舞台

- 角色模式仍为 `scholar`（玄学家）和 `half`（半仙）。舞台标题只显示角色名，不显示文化场景名和分隔点。
- 完整舞台采用“人物主舞台”布局：人物占上半区，底部为可滚动对话面板；综合、东方、西方页面仍以微光浮球作为默认入口。
- 人物采用“克制东方人形”：统一头身骨架，重新校准头部比例、五官间距、肩线、手势、坐姿和服饰层次。文化差异通过道具、背景纹理、动作节奏和语气词表达，而不是只更换颜色。
- 舞台关闭后回到微光浮球；浮球继续满足最小触控尺寸、内容描述、键盘可达性、`reduced-motion` 和系统安全区要求。
- 文化场景可以作为视觉和无障碍描述的一部分，但不作为舞台主标题；避免标题重复和跨文化标签化。

### 1.2 对话原则

- 回复策略是“先答核心，再给一个澄清入口”。识别出明显意图时先回应最小有用内容，并提供一个短问题或最多两个快捷入口。
- 多主题输入先选择置信度最高的主主题；其余主题进入澄清入口，不把多个模板拼成一段泛泛长文。
- 低置信度输入只返回简短承接和澄清问题，不伪造已理解的事实。
- 连续追问继承上一回合的主题和实体；明确出现新主题时切换上下文；切换 persona、皮肤或主题时使旧异步结果失效。
- 棋局回复仍只引用棋盘事实；运势、健康和财务回复继续经过本地安全约束。

### 1.3 记忆原则

- 现有三类长期记忆继续保留：用户原话、用户主动选择、已结算棋局结果。
- 新增本地软标签，例如“最近常聊工作”。软标签允许自动生成并长期保留，但必须带来源标记“由对话推断”，在本机长期记忆面板中可见、可单条撤回、可全部清除。
- 软标签不能被角色说成用户明确承诺、诊断或事实；引用时使用低断言表达，例如“我猜你最近更常提到……”。
- 软标签不上传、不进入在线 Provider 请求，除非用户在未来的在线设置中明确授权；默认离线行为不变。

## 2. 领域与接口设计

### 2.1 对话分析 Module

在现有 `MysticIntentClassifier` 和 `MysticDialogueContinuity` 之上增加一个小接口，隐藏关键词、实体和置信度计算：

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

实现约束：

- 先规范化 Unicode、大小写、标点和长度，再执行游戏/安全/直接寒暄/主题/追问排序。
- 主题词和实体词分开维护；“工作和感情都想问”保留两个候选，但只选择一个主答复。
- `confidence` 只用于是否澄清，不作为运势分数或心理结论。
- 旧 `MysticDialogueEngine.classify()` 保持兼容，内部转调分析 Module。

### 2.2 回复接口扩展

在 `DialogueReply` 上增加默认值字段，保持旧调用方源码兼容：

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

- `clarifiers` 最多两个，空列表表示无需澄清。
- `groundedFacts` 只允许列出当前盘面、棋盘或用户显式输入中已有的事实，不允许保存为长期记忆。
- 在线返回必须先经过本地事实校验和 `MysticSafetyGuard`；校验失败转为 `OnlineFallback`，回到确定性离线答复。

### 2.3 软标签模型

```kotlin
data class SoftMemoryTag(
    val id: String,
    val key: String,
    val label: String,
    val value: String,
    val source: SoftMemorySource = SoftMemorySource.Inferred,
    val createdAt: String,
    val lastSeenAt: String,
    val revoked: Boolean = false
)

enum class SoftMemorySource { Inferred, UserConfirmed }
```

- 软标签写入独立于现有 `RecollectionKind`，避免把推断内容伪装成用户原话。
- `SoftMemoryTag` 使用版本化 Codec 落入同一 profile 隔离的 DataStore；读取损坏时明确降级，不阻塞对话。
- 列表只展示最近可读的标签；撤回操作写入 `revoked` 或删除该条，全部清除同时清除软标签和现有长期记忆。

## 3. UI 与工程拆分

### 3.1 舞台拆分

从 `MysticFloatingGuide.kt` 按稳定 seam 拆出：

- 舞台布局与系统栏处理；
- `MysticFigureCanvas` 人物骨架、五官、手势和表情；
- 文化背景、道具和场景图层；
- 服饰/角色切换控件。

人物 Canvas 只接收 `mode`、`skinId`、`moodLevel` 和 reduced-motion 状态，不读取运势生成器内部状态，也不自行决定角色称谓。

### 3.2 对话卡拆分

从 `MysticGuideCard.kt` 按稳定 seam 拆出消息列表、输入状态宿主、软标签面板和对话快捷入口。状态仍由 `MysticSessionState`/`reduce` 管理，所有异步回包继续携带 `sessionToken` 和 `turnId`。

### 3.3 Generator 拆分

继续把 `MysticGuideGenerator.kt` 的主题回答、运势回答和文化语气表抽到纯 Kotlin Module；保持确定性 hash、persona/style key 和 `MysticSafetyGuard.enforce` 唯一接线点不变。

## 4. 验证与验收

### 4.1 Domain 测试

- 中文、英文、全半角标点、空白和 200 字符截断。
- 单主题、多主题、实体识别、低置信度澄清和“先答核心 + 澄清入口”。
- 连续 5 轮追问、明确换主题、换 persona/皮肤期间旧 token 丢弃。
- 软标签自动生成、来源标记、长期读取、单条撤回、全部清除和损坏降级。
- 在线成功、超时、取消、事实校验失败以及离线回退；默认 provider 仍为离线。
- 健康/财务边界、盘面事实和棋局事实不被在线文本覆盖。

### 4.2 UI 测试

- 舞台标题只显示角色名，不显示文化场景名或分隔点。
- 人物占比、底部对话面板、滚动和输入法安全区不互相遮挡。
- 浮球召回、舞台关闭、切换角色/皮肤、返回键和旋转恢复状态。
- 键盘导航、TalkBack 内容描述、软标签来源/撤回控件和 `reduced-motion`。

### 4.3 工程门禁

- `node _dev/dialogue_contract_test.js` 继续作为 CI 阻断门禁，并同步修正文档中 37/45 条 golden 的旧数字。
- `:app:testDebugUnitTest`、`:app:lintDebug`、`:app:assembleDebug` 必须通过；Lint error 阻断，warning 分级记录。
- Android instrumented 测试、真机安装、TalkBack、Logcat 和视觉截图等到用户通知手机可测后执行，不用旧截图替代新证据。

## 5. 明确不做的事情

- 本阶段不新增占卜体系、不接入具体在线厂商、不写入密钥。
- 不把 Ifá Ese、纳迪叶文、商业心理测验常模或未经授权的传统文本硬编码进应用。
- 不把软标签发送到网络，不把生成式回复伪装成长期记忆。
- 不改变默认离线行为、现有角色 seed/hash、棋局规则或健康/财务安全边界。
