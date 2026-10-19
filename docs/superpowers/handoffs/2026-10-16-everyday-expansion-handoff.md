# X3 日常使用与陪伴扩充交接（2026-10-16）

本轮在原有未提交工作区内实施，不使用子代理，不清理旧成果，不提交或推送。Android 生产 APK 已构建，手机安装仍由用户另行通知。以下功能基于当前源码；旧交接中的 APK 哈希与测试计数不代表本轮产物。

## 用户现在可以做什么

- 在饮食详情或玄师真实饮食卡中，将菜品加入安排，选择时间、备注和 1–20 份。安排保留保存时的每份目录估价，按份数展示参考预算；没有估价的旧记录显示未知。预算是项目目录参考，不是实时价格。
- 在记录日历查看本周主动保存的餐单，将某日未完成餐食复制到另一日。副本生成新 ID，保留份数、备注和估价，完成状态重置；原记录保留。复制遇同一时段冲突整次不写入，不会部分覆盖。
- 新增、编辑或延期安排时，看到同日同一非空时间的其他待办；可改时间、取消，或再次点击明确保留重叠安排。延期移动原项目，原日不留副本。
- 在日历记录食材库存和备注；购物清单标出“库存已有/待补充”并保留菜名来源。库存按名称核对；没有统一菜谱重量，不生成食材克数、营养或剩余数量。库存与安排可独立清除。
- 开启安排提醒后，从通知中完成当前事项或约一小时后再提醒。延后任务按档案/事项去重，执行前重查档案、待办及原日期/时间；改期、完成或删除后的旧动作不会改写其他事项。切档案或关闭提醒取消延后通知；系统省电可能造成调度延迟。
- 在“我的 → 隐私与数据 → 记忆管理”查看、修改、撤回软标签；保持原期限或改为 7/30/90 天、无期限。旧标签保持无期限，新推断标签默认 30 天。过期标签保留在管理页，对话提示隐藏；用户编辑/撤回不会被后续主题识别覆盖。损坏数据保留，普通编辑/推断不覆盖它。

原有目录、详情分类、确定性五行/星座推荐、60 术语/16 篇学习内容、本地搜索、四角色线程、808 次对话矩阵和四款游戏继续沿用。今日行动只在日周期挂载；周餐单是用户安排工具，不据当天运势生成未来预测。

## 数据与恢复

| 协议 | 当前版本 | 迁移与边界 |
| --- | --- | --- |
| DailyLife | 3 | 接受 v1–v3；旧安排补 1 份，缺失价格保持未知，旧库存为空 |
| 本地备份 | 4 | 接受 v1–v4；备份 v3 起有库存，v4 含每份价格；旧备份导入不删除本机库存 |
| 软标签 | 2 | v1 保持无期限；v2 日期严格解析，错误到期字段不降级为永久有效 |

库存和安排共享“安排”备份分区，备注单独选择。未勾选备注时不导出库存/安排备注，恢复时保留本机原备注；库存备注的冲突归到 Notes，不因取消备注而要求无关覆盖。恢复沿用完整校验、暂存、CAS、日志和回滚。按功能清除支持库存，清除备注同时清空库存备注，其他档案不受影响。

`SoftMemoryTagStore` 的写操作统一互斥。显式编辑只改变文字、期限和用户确认标记，保留 ID、原创建记录和撤回状态。聊天里的清除仅在存储删除成功后更新界面。

## 构建产物

生产候选：`app/build/outputs/apk/debug/app-debug.apk`，包名 `com.xuanji.app`，Debug，版本 `1.0.0 / 1`，26,145,714 bytes。

```text
SHA256 D3D0EC499EFE2B83996BB8438602B80A976694F833BAF9FAD6B87E296C6EFAAF
```

新增 `:app:writeDebugApkManifest`，依赖 `assembleDebug`，输出同目录 `build-manifest.json`。记录包名、版本、HEAD、dirty、记录时间、APK 大小及 SHA256；每次显式执行重新生成。CI 使用该任务并上传清单。HEAD 为 `0456228af1cc061d97acec98e24201943a580cc7`，当前工作区有未提交改动，不能只靠 HEAD 重现 APK。版本号未变，应使用清单/哈希识别。

## 本轮验证记录

- 十项 Node 契约通过，保留 45 条 wording golden；202 条输入 × 4 角色的 808 次真实回复评估由 JVM 执行，Node 只检查契约。
- JVM 524 项，0 失败、0 error、0 skipped。新增覆盖旧安排迁移、份数/价格校验、冲突确认与整批复制不部分写入、库存持久化与档案隔离、备份备注选择与旧 v2 导入、软标签到期/显式编辑/撤回保护。
- `lintDebug`：0 error、75 warning。Debug、AndroidTest、Regression 和对应 AndroidTest 构建通过。
- 七项定向 Android 交互全部通过，包含份数保存、冲突二次确认、库存实际输入联动、过期隐藏、修改文字保留有效期/改成无期限，以及通知 PendingIntent 的档案/事项/动作/改期隔离。
- 最终源码完整 Debug UI 67 项声明、64 实际通过、3 项独立包保护跳过、0 失败，105.894 秒；`am instrument` 的 `OK (67 tests)` 包含跳过项，因此不写成 67 项全部实际通过。
- 三阶段真实应用恢复全部通过：真实饮食对话和临时条件、第二候选/依据一致、丝路推进、终止进程后恢复、四角色交接与各游戏存档、另一合成档案隔离。PID 实测 6022 → 6071。Debug/instrumentation 下 PSS 41 次采样，观察最大 313406 KiB、中位数 268402 KiB，是本次观察值，不能作为实体低端机或 Release 性能结论。
- 验收后读取隔离模拟器的 crash buffer，结果为空，日志保存为 `emulator-crash-buffer.log`；此记录不代表手机 Logcat 验收。

测试环境为新建 API 35 AVD `XuanjiEverydayIsolated20261016`，ADB 5038，serial `emulator-5582`，每次安装先验证 `ro.kernel.qemu=1`。使用显式 serial 的 `adb install` / `am instrument`，防止同时在线的实体手机被选中。普通 Debug UI 和 `com.xuanji.app.regression` 进程恢复分开运行，使用合成资料。

日志：`work/.dispatch-backup/20261016-everyday-expansion/`，含构建日志、七项交互、最终完整 UI 和验收清单；AVD 磁盘已加入任务级忽略规则，保留在本机且不作为源码。恢复证据位于 `work/.dispatch-backup/20261014-application-recovery/2026-10-16T09-18-24-448Z/`。三阶段由 `_dev/isolated_application_recovery.js` 实际执行，使用独立包名和合成档案。

Debug AndroidTest APK SHA256：`CA54167388FCBB3BC880480EF9B354C2C0CE372A5BCD7346ED8DD2B08D331A9B`。最终 Debug、AndroidTest 与 Regression 门禁日志对应上述源码；构建清单 SHA256 已与磁盘 APK 再次比对一致。

复现源码门禁：

```powershell
$contracts = @(rg --files _dev -g '*contract_test.js')
foreach ($contract in $contracts) {
    node $contract
    if ($LASTEXITCODE -ne 0) { throw "Contract failed: $contract" }
}
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:writeDebugApkManifest :app:assembleDebugAndroidTest
.\gradlew.bat -PxuanjiRegression=true :app:assembleRegression :app:assembleRegressionAndroidTest
```

本轮完整 UI 使用经过模拟器属性校验的显式设备命令；运行前须安装对应的 Debug 与 AndroidTest APK：

```powershell
& 'C:\Users\blueice\AppData\Local\Android\Sdk\platform-tools\adb.exe' -P 5038 -s emulator-5582 shell am instrument -w -r 'com.xuanji.app.test/androidx.test.runner.AndroidJUnitRunner'
$env:XUANJI_ADB = 'C:\Users\blueice\AppData\Local\Android\Sdk\platform-tools\adb.exe'
$env:XUANJI_ADB_PORT = '5038'
node _dev/isolated_application_recovery.js emulator-5582
```

未把本轮直接 instrumentation 运行写成 Gradle `connectedDebugAndroidTest` 执行；CI 仍使用原有模拟器任务执行全部 AndroidTest。

## 验证范围

本轮未安装或操作手机，未进行人工视觉、大字号或 TalkBack 验收，未推送触发远端 CI。通知身份隔离有运行测试；操作系统实际等待一小时后的通知送达与省电延迟未作真实时间验收。库存没有自动克数/保质期解析，城市预算和节奏资料不足仍沿用原边界。没有新增在线地图、模型、医疗/投资结论或未授权文化原文。

本轮调试记录：首次新增 Gradle 任务因 `java` 扩展名遮蔽标准库引用失败，改为显式导入后通过；旧用例的刻意重叠安排改为显式允许，损坏软标签用例调整为保留原数据至主动清除；新增回调的 SuspiciousIndentation 已按报告修正，未通过抑制 lint 绕过。
