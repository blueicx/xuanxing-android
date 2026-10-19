# X3 稳定可信与本地回顾扩充交接（2026-10-03）

- 工作目录：`F:\huny\xuanji-android`
- 分支：`codex/reading-expansion`
- 方案：[规格](../specs/2026-10-03-xuanji-reliability-review-design.md) · [实施计划](../plans/2026-10-03-xuanji-reliability-review.md)
- Git：按要求未提交、未推送；工作树修改均保留。

## 已完成

1. **对话相关性矩阵**：106 条本地自拟输入逐一由四角色执行，共 424 次真实离线分析、路由与确定性回复评估。扩展了问候、行动、生活画像、多轮、缺资料和安全边界。矩阵捕获并修复“我暗恋的人最近怎么样”被嵌入式“最近怎么样”误判为问候的问题。既有 45 条 dialogue golden 未减少。
2. **近期选择时间线**：记录页只从用户主动收藏与明确行动反馈生成时间线；提供筛选，保留原历史/同日生/收藏功能，不从聊天或被动行为推断偏好。
3. **选择性本地备份恢复**：支持用户选收藏、行动反馈、角色小游戏进度和中国象棋棋谱。版本化 JSON 带 profile 指纹与 SHA-256 完整性校验（不是加密）；预览阶段不写数据，跨档案/损坏文件阻断；收藏和反馈按 ID 合并，游戏存档冲突需要明确确认覆盖。出生资料、对话、长期记忆和象棋战绩不进入备份。
4. **角色游戏深化**：诗句接龙、星图观测扩为五个确定性关卡，提供提示、撤销、重开与完成摘要；丝路路线按已走路径重算天数/补给并提供逐段回放。游戏进度 codec 从 v1 升到 v2，旧有效状态可迁移。
5. **交互验证**：近期选择、导入预览/取消/分区恢复/档案不匹配、详情问玄师、行动反馈和三款角色游戏均有真实 Compose 交互用例；CI API 34 job 已纳入相关测试类和 Node 契约。

## 验证证据

- Node：7 项契约全部通过；对话 relevance 为 106 × 4 = 424；dialogue golden 仍为 45。
- JVM：`:app:testDebugUnitTest` 430 项通过（0 失败、0 error、0 skipped）。
- Lint：`:app:lintDebug` 通过，0 Error / 84 Warning / 16 Information。
- 构建：`:app:assembleDebug`、`:app:assembleDebugAndroidTest` 通过。
- Compose：显式 SDK ADB、独立 server port `5038`、隔离 API 35 AVD `emulator-5556`；`connectedDebugAndroidTest` 21 项通过（0 失败、0 error、0 skipped）。XML 报告：`app/build/outputs/androidTest-results/connected/debug/TEST-X3CodexIsolatedApi35_20261003(AVD) - 15-_app-.xml`。
- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`，24,934,696 bytes，SHA-256 `86244BD8AA32F2D7693C9D674A1676E86109A134A856BA9D7B20556B6CBE62DA`。AndroidTest APK：1,158,912 bytes。
- `git diff --check` 通过；只报告仓库既有 LF/CRLF 转换提示，无 whitespace error。

## 未执行与边界

- 本分支没有推送，远端 GitHub Actions 尚无覆盖本轮的运行证据。
- 未连接、安装或修改用户手机；21 项是隔离模拟器自动交互验证，不是实体机验收。
- 按用户限定，本轮不做截图视觉比较、人工 TalkBack、键盘/减少动态效果或大字号验收。
- 未清理 `.superpowers/`、截图、dump、脚本或其他未跟踪工作；无提交或推送。

## 后续建议

如果用户另行通知手机可测，再单独进行实体机安装/启动/Logcat 与视觉检查；否则无需为了本轮完成状态重跑设备验收。远端 CI 只有在用户授权推送后才能获得本轮远端运行结果。
