# X3 占卜学习与 1.1.1 交接（2026-10-19）

## 本轮完成

总清单第 7 包的解释层已接入真实入口；**总计划仍未全部完成，保留第 6、8–12 包。** 本轮没有使用子代理，没有清理用户成果，也没有新增占卜体系、在线模型、常模或传统经文。

- 塔罗：观察本次真实抽牌的相邻位置、同花色数量与正逆位数量，提示共同点/冲突及现实证据复盘，不把牌位先后当作必然因果。翻牌完成后显示，复用原卡片设计。
- 用户可明确保存原牌面、正逆位、位置、对应解释、种子和规则版本到已有记录日历；同一结果重复保存沿用稳定 ID。没有自动保存、没有把生成内容当长期记忆。心得另由用户主动写入已有日记。
- 卢恩：稳定三符完整展示第二、第三符，不再只显示第一符；新增三处位置对照、可收起的 24 符学习目录和原始结果保存。随机抽签清除旧稳定结果，“恢复今日符文”清除两种抽取状态。目录解读明确标为项目现代评论，而非古代经文或唯一流派。
- 数字：基础日期、完整姓名五数、个人周期均展示实际中间值；姓名结果与计算链复用同一纯函数，防止解释与计分分叉。只接受 A–Z 英文/拼音、空格、连字符和撇号，最多 120 字符，不猜汉字读音，也不把 Unicode 字母减去 ASCII 偏移；拒绝混合汉字、变音字母、数字与超长姓名。
- 姓名规则沿用 A=1…Z=26，元音为 AEIOU、Y 按辅音。缺少某类字母时为零，页面明确不据此作人格结论。基础日期各数位求和与完整生命路径分组件归约分别说明，保留历史结果，含大师数时可能不同，不悄悄合并两种规则。
- `DivinationLearning` 标为 `draw-observation-v1`，只做当前牌面的观察与项目自编提示。没有增加医疗、投资或未来事件保证。

## 远端 CI 修复

首次推送 `58c31fa` 的两个远端任务在 SDK 初始化失败，未进入测试。日志明确为 `Failed to find package 'tools'`；核对 setup-android v3 的 action 定义后，确认默认 packages 为 `tools platform-tools`。提交 `bd9eeee` 给两个 SDK 步骤显式配置 `packages: 'platform-tools'`，并保留现有平台/build-tools 安装步骤。

新增契约先在旧配置失败、修复后通过。修复后远端 run `39258566623` 的构建任务已通过；模拟器任务结果和本轮后续推送的远端结果应以实际最终回执为准，不能用本机通过代替。

## 本机验证证据

- 571 项 JVM，0 失败、0 错误、0 跳过。新增 4 项纯规则测试覆盖姓名输入/计算链、基础日期推导、实际塔罗位置/保存内容以及完整卢恩目录。
- 10 项 Node 契约通过；45 条 golden wording 保留，202 输入 × 4 角色的真实引擎断言仍在 JVM 中执行。
- `lintDebug`：0 error、75 warning；Debug / AndroidTest 构建、独立 Regression / Regression AndroidTest 构建通过。
- 最终完整 Debug 交互为 78 项：75 实际通过、3 个隔离保护跳过，0 失败，206.702 秒。新增 2 项操作测试验证卢恩三处位置与目录展开/旋转恢复，以及塔罗明确点击保存后原牌面/种子/版本落入隔离历史，其他档案为空。
- 三阶段真实应用进程恢复已通过，证据目录 `work/.dispatch-backup/20261014-application-recovery/2026-10-19T13-31-42-703Z/`。沿用独立 `.regression` 包和合成资料，不读取或修改手机存档。

证据目录为 `work/.dispatch-backup/20261019-practical-constraints/`：`divination-red.log`、`base-number-red.log`、`snapshot-red.log` 为先失败证据；`learning-delivery-gates.log`、`learning-full-ui.log`、`learning-regression-build.log` 为最终门禁；`learning-application-recovery.log` 为真实进程恢复，`learning-phone-*.log` 为设备交付。以上日志与回滚副本保留本地、排除 Git。

复现命令：

```powershell
Get-ChildItem _dev -Filter '*contract_test.js' | ForEach-Object { node $_.FullName; if ($LASTEXITCODE -ne 0) { throw $_.Name } }
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:writeDebugApkManifest :app:assembleDebugAndroidTest
.\gradlew.bat -PxuanjiRegression=true :app:assembleRegression :app:assembleRegressionAndroidTest
```

## 手机与产物

Debug **1.1.1（3）**，包名 `com.xuanji.app`；产物 `app/build/outputs/apk/debug/app-debug.apk`。

SHA256：`CE888028B6B0D5101B749790C7AF8A89B24434E1DC8E1FC6B8F4080576C1AEEA`。

Xperia XZ2 使用 `install -r` 更新成功，未卸载或清数据；设备 APK 哈希与上述一致。冷启动 Status ok，TotalTime 1609 ms，进程存活，当前进程 crash buffer 为空。没有把单次启动当作全功能真机或性能验收。

人工视觉、大字号、TalkBack 和长期通知调度继续沿用此前排除范围；未新增正式发布密钥，当前不是正式签名 Release。

## 剩余范围

第 6 包探索详情/相邻周期/时间敏感性；第 8 包测验草稿、漏答复核、历史比较和角色引用同意；第 9 包角色当前任务交接深化；第 10 包游戏课程；第 11 包首页配置/小组件；第 12 包图片分享、大文件/性能/lint 分级与正式签名。总清单继续保留未完成状态。

旧塔罗完整舞台的所有旋转/跨进程交互、实际姓名输入框测试与真实手机占卜触控未在本轮逐项人工验收；本轮新交互宿主使用合成牌面和内存边界，不冒充整个生产导航测试。
