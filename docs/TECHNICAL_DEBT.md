# X3 玄机技术债台账

本台账记录全量审计后仍值得继续处理、但不应阻塞离线版本交付的工程债务。现有未提交源码、截图、UI dump 和脚本均视为用户成果，本台账不要求清理它们。

## 当前基线

- Android 门禁：`testDebugUnitTest`、`lintDebug`、`assembleDebug` 已建立并在最近一轮通过；lint 当前无 error，剩余主要是既有 unused 参数/变量和 SDK XML 版本提示。
- Android 测试：纯 Kotlin domain/generator 测试已存在，覆盖对话分类、确定性、离线 provider、会话 token 和生成器空输入。
- 小程序：结构 lint 与 7 项引擎/题库测试已分开执行并通过；双端契约位于 `_dev/dialogue_contract.json`。
- 设备证据：曾完成 `com.xuanji.app` AVD 安装、启动、综合/东方/西方浮球、召回舞台和关闭回浮球截图，证据保存在 `.superpowers/round46-*`。没有把当前无在线设备误报为实体机验证。
- 同日生增强：`SameDayWorks` 已加入确定性音乐/诗歌卡，`HistoryCopy` 与 `AnimatedVisibility` 支持长评语折叠；后续可继续扩充经过版权核验的作品元数据。
- B+C 视觉：`MysticCultureSpec` 已为 8 个皮肤提供结构化道具和舞台场景；后续仍需设备上检查人物比例、遮挡和不同屏幕密度的视觉细节。
- 对话承接：`MysticDialogueContinuity` 已让省略式追问继承最近主题；后续应继续扩充中英文标点、连续 5 轮、换 persona/皮肤和跨端 golden wording。

## P1：继续拆分大文件

| 文件 | 当前规模（约） | 已完成 | 下一步 |
| --- | ---: | --- | --- |
| `MysticGuideGenerator.kt` | 3.4k 行 | 对话 seam、intent classifier 已抽离 | 将模板选择与安全约束抽成纯 Kotlin 文件，保持确定性 hash 不变 |
| `MysticGuideCard.kt` | 2.4k 行 | provider/session 接入 | 将输入栏/快捷问题和会话渲染拆成独立 composable 文件，先保持参数和状态提升方式不变 |
| `MysticFloatingGuide.kt` | 2.6k 行 | `MysticOrb.kt` 已拆出；舞台仍在原文件 | 将舞台外壳与人物绘制分开；人物绘制 helper 需继续保持同一 skin/mood 输入 |

拆分规则：一次只移动一个稳定边界；不改变公共 API、资源 ID、角色 seed 或默认离线行为；每次移动后必须跑编译、单测、lint 和 debug assemble。

## P1：对话回归矩阵

继续扩充 Android 与小程序的 golden wording，至少保持以下类别：问候、感谢/告别、身份、闲聊、情绪、健康、财务、连续追问、换主题/角色/皮肤、空输入与超长输入。所有回复必须经过本地 persona/safety guard，不能伪造命盘事实或给出医疗/投资结论。

交流面板已新增统一的消息/请求状态控件，后续只需继续扩充 golden wording，不再为每个入口维护独立的输入状态。

## P2：可访问性与运动偏好

- 浮球保持 52dp 视觉尺寸和可点击语义；继续用 UI dump 或 TalkBack 实机检查焦点顺序。
- reduced-motion 已关闭浮球位移和持续旋转；后续检查完整舞台呼吸动画、键盘导航和旋转/返回键状态恢复。
- 复测 safe area：浮球不遮挡分数卡、底部导航和系统手势区。
- 本轮手机复测暂缓，恢复时优先覆盖同日生折叠按钮的 TalkBack 标签、作品卡阅读顺序和舞台文化场景的对比度。

## P2：Provider seam

`DialogueProvider` 当前只提供离线实现。在线 provider 仅允许作为未来 seam：需要显式同意、超时、取消、失败回退和本地安全复核；本轮不接厂商、不写入密钥、不改变默认离线行为。

## P1：真实语料与官方常模接入条件

- Ifá Ese 经文、纳迪叶脉原文：代码已提供 Provider、来源、许可证和置信度字段；在取得传承方/持有者授权与可追溯数据前，应用只能显示名称/索引或“离线模拟”。
- MMPI、Raven、16PF、MBTI：官方题目、计分键、手册和常模属于授权材料；当前仅开放 IPIP Big Five-50 可实际计分，未配置常模时不显示官方百分位。
- 农历：1900–2100 表驱动换算已内置并有闰月 golden case；若需要更早/更晚年份，应新增经核验历表版本，而不是外推。

## 完成定义

一项债务只有在有源码变更、对应测试和可复现命令输出时才标记完成。截图、旧日志或“代码看起来已接线”不能替代运行时证据。
