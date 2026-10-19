# X3 推荐条件、主动记录与 1.1.0 交付（2026-10-19）

## 当前完成范围

本轮补齐总清单第 1、5 包，并将此前已有证据的第 4 包同步为完成。**整体 12 包仍未全部完成：第 6–12 包保留未完成状态。** 不以本轮测试或安装成功替代剩余功能实现。用户已授权安装手机及 Git 推送；全程单代理，未清理或回退原成果。

- 饮食偏好支持可用设备、简单做法、便当形式与显式库存优先。64 项菜谱的做法条件进入每日过滤、替代候选及周餐单候选池；库存排序不改文化分数，不推算用量或储运安全。库存为零、已过手填期限的条目不用于排序；没有自动扣库存。周餐单仍采用原贪心组合目标，不宣称核算整周库存够用。
- 主动日记、所选心情和实际执行在安排日历中保存/修改/确认删除，按选定日期所在自然周/月展示记录天数、心情次数和安排完成数；没有心情平均值、人格诊断、运势因果分析或聊天自动写入。
- 日记按档案隔离，最多 366 个日期，并受既有 2 MB 文档保护；超限拒绝，不截断旧记录。新增单独清除，清除备注也清除日记；完整清除保持整档案删除。
- DailyLife v6、偏好 v2、备份 v8 向后读取。日记仅进入显式勾选的备注分区，同日期修改先预览/确认，写入仍走暂存、CAS 与回滚；旧备份缺失日记时不清除本机日记。
- 未知版本、未知设备枚举或损坏偏好不再默默使用无约束默认值生成饮食推荐。真实仓库暂停每日/周候选，UI 提示不可读并禁用默认保存，原数据保留；清除由用户主动执行。
- 修复个人数字周期把输入 11/22/33 过早拆位的错误，复用 `NumerologyReduction` 并展示实际组件与归约步骤；明确项目规则版本和文化参考边界。此项不代表塔罗、卢恩与完整姓名推导包整体完成。

## 本机实际验收

- 10 项 Node 契约通过，45 条 wording 保留；202 输入 × 4 角色的实际引擎断言继续由 JVM 执行。
- 567 项 JVM，0 失败、0 错误、0 跳过。
- lint：0 error、75 warning；Debug APK、Debug AndroidTest、独立 Regression APK/AndroidTest 构建通过。
- 最终 Debug 交互声明 76 项：73 实际通过、3 项包名/模拟器保护跳过，0 失败，219.34 秒；不可读偏好、实际勾选保存、日记编辑/旋转恢复/保存/回顾/删除均有操作断言。
- 三阶段独立包真实应用恢复通过：先实际导航/输入/推进，再由脚本终止操作系统进程，最后验证重启与浮球召回。新增真实仓库的不可读偏好拒绝每日/周候选断言，日记字段重启后保持。只在隔离模拟器写合成资料，未在手机运行测试宿主。
- 失败证据保留：最初新交互 2 项失败，原因是偏好文本与复选框未统一点击语义，以及回顾断言误用整段匹配。修正后两项通过，最终全套再执行；没有删断言或减少测试数量。

本机日志在 `work/.dispatch-backup/20261019-practical-constraints/`：`delivery-gates.log`、`regression-build.log`、`final-full-ui.log`、`new-ui.log`、`new-ui-fixed.log`、`application-recovery.log`。真实进程恢复证据目录为 `work/.dispatch-backup/20261014-application-recovery/2026-10-19T10-15-08-879Z/`。回滚副本、日志、模拟器磁盘和设备证据保留本地，不加入本次 Git 交付。

复现源码门禁：

```powershell
Get-ChildItem _dev -Filter '*contract_test.js' | ForEach-Object { node $_.FullName; if ($LASTEXITCODE -ne 0) { throw $_.Name } }
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:writeDebugApkManifest :app:assembleDebugAndroidTest
.\gradlew.bat -PxuanjiRegression=true :app:assembleRegression :app:assembleRegressionAndroidTest
```

交互与系统进程恢复使用已配置的隔离模拟器和 `_dev/isolated_application_recovery.js`；该脚本拒绝实体机及正式包数据目录。

## 手机与产物

Debug 版本 **1.1.0（2）**，包名 `com.xuanji.app`，APK：`app/build/outputs/apk/debug/app-debug.apk`。

SHA256：`0FCCF63569C2EAC607340B91C005F944C0E1F50738FA882B2E2B6E98EDDC30CC`。

Xperia XZ2 通过 `install -r` 更新，返回 Success，未卸载或清数据；版本确认 1.1.0 / 2，安装后设备 APK 哈希与上述一致。`am start -W` 冷启动 Status ok，TotalTime 1620 ms，之后进程仍存活，当前进程 crash buffer 为空。这是单次 Debug 启动记录，不是全机性能基准或全功能真机验收。

人工视觉、大字号、TalkBack 和长期通知调度仍在此前排除/未验收范围。不宣称正式 Release 签名；没有新增正式发布密钥。

## Git 与后续

交付分支为 `codex/reading-expansion`，提交包含本轮和此前积累的项目源码、测试、配置、文档，不包含本地回滚/设备证据。推送在本文保存和提交后执行；以实际远端 HEAD 和 Git 回执核对，不用本机提交代替推送。该分支已加入既有 CI 触发范围，未重复建立第二套 UI 测试任务；本机通过不等于远端 CI 已通过。

剩余范围以 [总清单](../plans/2026-10-18-daily-companion-deepening.md) 和 [技术债](../../TECHNICAL_DEBT.md) 顶部为准：探索详情/相邻周期/时间敏感性、占卜深化、测验草稿/复核/引用授权、角色任务交接深化、游戏课程、首页配置与小组件、选择性图片分享及工程/正式签名收口。未完成项没有改成完成。
