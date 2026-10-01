# X3 玄师陪伴五项优化执行账本

- [done] 领域状态：Peek/Expanded 抽屉、角色状态保留、旧 token 丢弃
- [done] 视觉契约：完整场景图与透明前景图分流
- [done] 对话动作：今日行动、人生画像、依据、游戏路由
- [done] 游戏存档：诗句、星图、丝路按档案/角色/游戏隔离
- [done] 测试与门禁：JVM、AndroidTest 编译、Node 契约、lint、Debug APK
- [pending] 手机安装、TalkBack、Logcat、真实截图（按用户通知执行）
- [done] 运势板块独立详情页：卡片可点入全屏阅读并返回，今日行动三类建议含字段化详情；Compose 交互测试与 JVM 模型测试通过
- [done] 详情闭环补强（2026-10-01）：行动详情展示实际依据来源与加权参考，偏好过滤明确标注不计分；详情内可切换吃什么/做什么/去哪玩；原卡片正文同一内容闭包用于预览和详情
- [done] Compose 测试修复（2026-10-01）：四角色舞台用单一 Compose 宿主覆盖，使用语义 testTag 避免角色画廊与标题重名导致断言歧义；隔离 API 35 模拟器完整 28 项通过
- [done] 详情收口最终门禁（2026-10-01）：JVM、lint、Debug/AndroidTest 编译通过；4 项 Node 契约通过；API 35 隔离模拟器 connectedDebugAndroidTest 28/28 通过
