# 角色交流板块增强实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在不破坏现有角色舞台和离线确定性回复的前提下，统一交流状态、补齐发送中/失败/取消/重试与快捷入口，并覆盖连续对话和旧请求丢弃。

**架构：** 领域层扩展不可变消息与请求状态，所有事件由 reducer 处理；Compose 层通过一个新的轻量交流面板渲染状态，并将现有 `MysticGuideCard` 的发送入口逐步接入。回复请求携带 session/turn token，切换上下文立即使旧结果失效。

**技术栈：** Kotlin/JVM、Compose Material3、Kotlin coroutines、JUnit4。

---

### 任务 1：扩展会话状态模型

**文件：**
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticSessionState.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/MysticSessionStateTest.kt`

- [x] 添加 `MysticMessageRole`、`MysticMessage`、`MysticRequestState`，保留现有 `MysticTurn` 兼容字段。
- [x] 添加 `MysticEvent.SendInput`、`QuickPrompt`、`ReplyStarted`、`ReplySucceeded`、`ReplyFailed`、`CancelReply`、`RetryTurn`，并让旧 `Input/Reply` 事件继续工作。
- [x] reducer 对 token、空输入、200 字符限制、重复发送和清空行为给出确定结果。
- [x] 先写并运行失败测试，再实现并运行通过测试。

### 任务 2：统一对话请求协调器

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/domain/MysticDialogueCoordinator.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueCoordinatorTest.kt`

- [x] 定义纯 Kotlin `DialogueCoordinator`，输入 `DialogueContext`、当前状态和 `DialogueProvider`，输出事件流/结果。
- [x] 为发送、取消、重试生成递增 turnId；上下文变更后旧结果返回 stale 而不是写入消息。
- [x] Provider 失败映射为可重试错误，不吞掉用户消息。
- [x] 覆盖连续五轮、重复输入、失败重试和旧 token 丢弃。

### 任务 3：交流 UI 状态组件

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/ui/components/MysticConversationPanel.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/components/MysticGuideCard.kt`

- [x] 新组件渲染空状态、消息气泡、角色名、发送中占位、取消、失败重试和快捷入口。
- [x] 快捷入口固定为“今日运势、继续说、换个话题、解释刚才、我只是想聊聊”。
- [x] 输入框限制 200 字符；发送按钮在空输入/请求中禁用；状态变化使用语义描述。
- [x] 在 `MysticGuideCard` 的 compact 与 immersive 两种模式复用该组件，保留现有 persona/skin/topic 控件。

### 任务 4：接入异步 token 与上下文切换

**文件：**
- 修改：`app/src/main/java/com/xuanji/app/ui/components/MysticGuideCard.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticSessionState.kt`

- [x] 自定义输入、快捷问题和重试统一走新的会话事件；旧的 `pendingCustom` 路径保留兼容。
- [x] persona、skin、topic 切换 dispatch `ChangeContext`，插入系统提示并取消旧请求。
- [x] 清空会话同时清除当前手记、草稿和请求状态；不删除持久化回访记录。
- [x] 运行 UI 编译与 domain 测试，确认现有舞台关闭/召回状态不变。

### 任务 5：文案、安全与回归门禁

**文件：**
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticDialogueEngine.kt`
- 修改：`docs/SYSTEMS_OVERVIEW.md`
- 修改：`docs/TECHNICAL_DEBT.md`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueEngineTest.kt`

- [x] 问候/感谢/闲聊回复不回显“你问：「…」”；健康与财务保持边界文案。
- [x] 增加 golden wording 与安全断言，确保不伪造盘面事实或长期记忆。
- [x] 运行 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`、`git diff --check`，记录 ADB 设备状态。
