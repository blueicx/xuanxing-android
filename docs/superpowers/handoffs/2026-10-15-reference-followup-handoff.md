# X3 临时条件与指代追问交接（2026-10-15）

继续执行全量扩充计划，不使用子代理、不清理、不提交、不推送、不操作手机。整份计划尚未全部完成；人工视觉、大字号、TalkBack 继续排除。

## 修复与验证链

真实 `ApplicationRecoveryJourneyTest` 增加输入“不要豆腐，只有十五分钟 → 第二个 → 为什么”。合成资料只保存在独立 regression 包，读取真实 ActionRepository、离线 Provider 和会话 Store，不 mock 回复或路由。

1. 失败证据：`2026-10-14T19-44-13-228Z/prepare_navigation_and_progress.log`，第二项卡片正确，回复却变成泛泛回退。合成线程显示引用 ID 正确；沿 Provider 追踪到校验器把 `15 分钟` 当成虚构 `15 分`。JVM 最小复现先失败，修复分数正则后通过；同一句中虚构 99 分仍拒绝。
2. 后续失败：`2026-10-14T19-47-25-757Z/prepare_navigation_and_progress.log`，依据卡没有所选第二项。UI 的通用动作分支在“为什么”时把引用重置成第一项，且依据卡只引用通用盘面。现仅新行动候选更新引用；解释不重置。依据卡通过纯 Kotlin resolver 读取所选候选依据，再保留原盘面依据；临时预算/食材/时间单列为“不计匹配分”，不伪造来源或权重。
3. 扩展两阶段真实应用回归通过：`work/.dispatch-backup/20261014-application-recovery/2026-10-14T19-52-58-935Z/`，PID 5288 → 5348，清单包含应用/测试 APK 哈希和本次约束/引用验证项。约束作用到候选、第二项回复/卡片/依据一致，长期饮食偏好未写入；进程重启后条件、引用、角色、抽屉及敦煌进度恢复。

目录时间为运行器 UTC；本交接日期按用户 Asia/Shanghai，为 2026-10-15。日志只涉及隔离合成档案。

## 自动门禁

九项 Node 契约通过，原 45 条 golden 与 808 次真实引擎评估保留。JVM 508 项、0 failure/error；lint 0 error、84 warning、20 information；Debug/AndroidTest/Regression 构建通过。完整 Debug 模拟器报告 58 项中 56 实际通过、2 按隔离包保护跳过，0 failure/error；两阶段回归另行通过，不计作普通 Debug 的 58 项通过。

正式待安装 APK：`app/build/outputs/apk/debug/app-debug.apk`，26891201 bytes，SHA256 `32916E150022F5BB50EDDB3A2F7371CDE98C9D34873D4546178B72BC54E72178`，本轮未安装手机。CI 沿用现有隔离链路，无新增任务或推送。

## 复现与剩余项

沿用 [上一轮复现命令](2026-10-14-application-recovery-handoff.md)。同一运行器现在验证临时条件、引用、依据与恢复；完整重跑不清除正式包资料。

剩余：角色交接、多档案与其他游戏的完整应用组合覆盖；地区现实条件比较的可靠离线资料；连续图片内存峰值、实体低端机与重复基准。不能把这一条追问链路当作全部组合验收。
