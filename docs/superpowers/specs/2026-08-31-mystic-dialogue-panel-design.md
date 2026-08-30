# 角色交流板块增强设计

**日期：** 2026-08-31  
**范围：** Android 角色交流面板；保持现有微光浮球、角色舞台和离线默认行为。

## 目标

让交流板块同时具备更自然的连续对话和可恢复的交互 UI：用户能够从问候进入追问、从闲聊转到运势、切换主题/角色/皮肤时不会串入旧回复；发送中、失败、重试和取消状态清晰可见。

## 非目标

- 本轮不接入在线大模型、不改变默认离线 Provider。
- 不重画角色骨架、不改浮球召回逻辑。
- 不把生成回复写入持久化长期记忆。

## 状态模型

`MysticSessionState` 作为唯一状态源，包含：当前 `sessionToken`、主题、persona、skin、消息列表、当前手记、输入草稿和请求状态。所有变更通过 `MysticEvent` reducer 完成：`SendInput`、`QuickPrompt`、`ReplyStarted`、`ReplySucceeded`、`ReplyFailed`、`CancelReply`、`SwitchContext`、`ClearSession`、`RetryTurn`。

每次请求携带 session/turn token。Reducer 仅接受与当前 token 匹配的异步结果；切换 persona、skin 或 topic 后，旧结果变为 stale 并丢弃。

消息类型为 `User`、`Mystic`、`System`，字段包括 turnId、sessionToken、intent、文本、时间戳、pending/error 标记。时间戳用于展示和排序，不参与离线回复 seed，确保确定性。

## 对话流程

1. 输入规范化：去首尾空白、统一标点、长度限制 200 字符；空输入不创建消息。
2. 意图识别：问候、感谢/告别、身份、闲聊、情绪、健康、财务、今日运势、连续追问、换主题。
3. 回复生成：沿用 `MysticDialogueEngine` 的 persona/style/skin 选择与确定性 hash。
4. 安全过滤：健康只给一般性关怀与就医建议，财务只给风险提示；不伪造命盘事实，不声称拥有未保存的长期记忆。
5. 成功后写入当前会话手记；清空会话时一并删除手记。

## UI 设计

- 空状态：角色一句短欢迎语 + 五个快捷入口：今日运势、继续说、换个话题、解释刚才、我只是想聊聊。
- 消息区：用户消息右对齐，角色消息左对齐；角色消息显示 persona 名称；System 消息用于“已切换主题/回复已取消”等状态。
- 输入区：发送按钮在输入为空时禁用；发送中显示取消按钮和轻量占位，不使用持续旋转动画。
- 失败态：保留用户消息，角色侧显示错误原因和“重试”；重试复用原输入但生成新的 turnId。
- 上下文切换：切换 persona/skin/topic 后插入一条 System 消息，说明旧请求已取消，保留历史消息但不再接受旧结果。
- 无障碍：消息列表使用可读语义；发送、取消、重试和快捷入口均有 content description；状态变化通过 LiveRegion 宣告。

## 测试与验收

- reducer：发送、成功、失败、取消、重试、清空、切换上下文和 stale token。
- engine：你好/早安/晚安、谢谢/再见、你是谁/在吗/无聊、情绪/健康/财务、问候后追问、连续 5 轮。
- UI 状态：空输入、超长输入、重复发送、失败重试、取消后恢复、切换 persona/skin/topic。
- 确定性：相同日期、角色、皮肤、盘面、输入产生相同回复；时间戳变化不影响回复。
- 安全：不出现医疗诊断、投资结论、虚构盘面事实或虚构长期记忆。

## 兼容与迁移

现有 `MysticGuideCard` 的公开 Composable 签名保持不变。先在 domain 层扩展 reducer 和消息模型，再将 Compose 中的 `pending*` 局部状态逐步映射到统一状态；每一步都保留旧快捷入口行为并通过现有测试门禁。
