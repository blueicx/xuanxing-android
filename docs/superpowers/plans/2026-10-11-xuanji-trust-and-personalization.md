# X3 可信与个性化收口实现计划

> **面向 AI 代理的工作者：** 在当前有用户未提交改动的工作区内内联执行；不得清理、覆盖、暂存、提交或推送无关成果。每项行为先写失败测试、确认失败原因，再实现。

**目标：** 完成资料完整度解释、显式行动偏好、可选加密备份、周/月本地回顾和一次低风险的角色代码拆分。

**架构：** 纯 Kotlin domain 负责确定性规则；现有 Repository/DataStore 边界负责按档案隔离；Compose 只编辑和呈现状态。备份加密作为旧 JSON codec 外层包装，保持旧明文备份可导入。

**技术栈：** Kotlin/JVM、AndroidX Compose、PreferenceBridge/DataStore、Gson、JCA AES-GCM 与 PBKDF2、JUnit、Compose UI Test。

> **执行状态（2026-10-11）：** 本计划所有代码任务已完成；七项 Node 契约通过，JVM 441/441、API 35 隔离模拟器 Compose 交互 45/45、lint 与 Debug/AndroidTest 构建均通过。未连接或安装用户手机，也未提交/推送。设备安装、人工视觉与无障碍验收不在本计划范围。

---

## 文件与职责

- 新建 `app/src/main/java/com/xuanji/app/domain/ProfileDataReadiness.kt`：出生资料完整度纯函数与用途说明。
- 新建 `app/src/main/java/com/xuanji/app/data/local/ActionConstraintStore.kt`：按档案保存用户明确选择的行动约束。
- 修改 `ActionRepository.kt`、`ActionViewModel.kt`、`FoodPreferenceEditor.kt`、`ProfileScreen.kt`：偏好读写、计划计算和设置入口。
- 新建 `app/src/main/java/com/xuanji/app/domain/EncryptedLocalBackup.kt`：加密包装协议与稳定错误类型。
- 修改 `LocalBackupPanel.kt`：密码输入、明文/加密选择、加密文件导入预览。
- 修改 `LocalReviewTimeline.kt`、`HistoryScreen.kt`：ISO 周/自然月的明确行为汇总与周期筛选。
- 新建 `MysticGuideModels.kt`、`MysticStageDecorations.kt`：只承载从两个大文件移动出的现有声明。
- 新建 JVM 测试：`ProfileDataReadinessTest.kt`、`ActionConstraintStoreTest.kt`、`EncryptedLocalBackupTest.kt`、`LocalReviewPeriodSummaryTest.kt`。
- 扩展 Compose 测试：Profile 设置偏好和资料提示、LocalBackupPanel 加密往返入口、近期回顾周期切换。
- 更新 `README.md`、`SYSTEMS_OVERVIEW.md`、`TECHNICAL_DEBT.md`、交接文档和 `.superpowers/dispatch-progress.md`。

## 任务 1：资料完整度

- [x] 添加纯 Kotlin readiness 测试：无档案、缺地域/坐标/性别、资料齐全都按字段返回确定性状态。
- [x] 运行对应 JVM 测试，确认因模型/评估器未定义而失败。
- [x] 实现 `ProfileDataReadiness`，文案只声明字段用途，不改变既有算法置信度。
- [x] 在个人档案页显示完整度清单和“完善资料”提示，并增加 Compose 语义断言。
- [x] 运行 readiness JVM 与 profile Compose 测试。

## 任务 2：行动偏好

- [x] 添加 `ActionConstraintStoreTest`：写入后读回、损坏 JSON 安全回默认、profile key 相互隔离。
- [x] 运行测试确认新 store 缺失导致预期失败。
- [x] 实现版本化 `ActionConstraintStore`，只存 `EnergyLevel`、`indoorOnly`、`maxActivityMinutes`，输入进行范围裁剪。
- [x] 先写 Planner 回归测试，证明室内/最长时长过滤候选；确认当前实现失败，再接入 repository 持久值。
- [x] 在设置页增加精力、室内优先和最长时长控件，保存后刷新综合页今日计划。
- [x] 运行 action domain/store 和设置 UI 测试。

## 任务 3：加密备份

- [x] 添加加密 codec 测试：正确密码往返、错误密码区分、字节翻转/截断拒绝、密码不进入编码结果。
- [x] 运行测试确认加密协议缺失而失败。
- [x] 实现有随机 salt/nonce、AES-GCM tag 校验、PBKDF2-HMAC-SHA256 密钥派生和最大输入长度的外层 codec；不持久化密码。
- [x] 在 UI 中添加显式加密开关和密码确认；选择文件后先检测加密头，再要求输入密码，只有成功解密并通过原 profile/checksum 校验才能预览。
- [x] 保留明文旧备份导入；明文导出必须再次显示未加密提示。
- [x] 运行 codec、Repository 与 Compose 交互测试。

## 任务 4：周/月回顾

- [x] 为纯汇总模型添加测试：ISO 周跨年归属、自然月边界、源类别计数、空数据。
- [x] 运行测试确认汇总 API 缺失而失败。
- [x] 实现周期汇总，只消费已有收藏与 `ActionFeedback` 明确记录。
- [x] 在时间线顶部增加“本周 / 本月”切换和汇总卡，不显示推断结论或人格标签。
- [x] 运行 timeline JVM 与 HistoryScreen Compose 测试。

## 任务 5：定向拆分与文档

- [x] 将 `MysticGuideGenerator.kt` 中纯 data class 声明机械移动到 `MysticGuideModels.kt`，保持包名和声明可见性不变。
- [x] 将 `MysticStageSpeech`、`MysticPersonaButton` 机械移动到 `MysticStageDecorations.kt`；补上独立 Compose test 对颜色/文本/点击不变的断言。
- [x] 运行相关 JVM/Compose 编译测试，核对 diff 仅有声明移动和新增功能。
- [x] 更新 README、系统概览、技术债、交接文档和任务账本，标记未做的手机/人工验收。

## 最终验证

- [x] 运行全部 `_dev` Node 契约。
- [x] 运行 `.\gradlew.bat :app:testDebugUnitTest --console=plain`。
- [x] 运行 `.\gradlew.bat :app:lintDebug --console=plain`。
- [x] 运行 `.\gradlew.bat :app:assembleDebug --console=plain` 与 `.\gradlew.bat :app:assembleDebugAndroidTest --console=plain`。
- [x] 核对 `git diff --check`、dirty tree 和 APK 产物；不连接手机、不安装、不提交、不推送。
