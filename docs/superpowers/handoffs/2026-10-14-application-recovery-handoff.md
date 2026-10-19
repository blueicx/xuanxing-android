# X3 真实应用恢复回归交接（2026-10-14）

整份扩充计划尚未全部完成。本轮没有子代理、清理、提交、推送或手机操作。保留现有 dirty tree；正式 APK 尚未安装本轮改动。人工视觉、大字号和 TalkBack 继续排除。

## 本轮实现与证据

- 新增 `regression` 构建类型与独立包 `com.xuanji.app.regression`，共享真实生产实现、独立应用数据目录。测试只允许模拟器与合成资料，拒绝非合成已有档案。
- `ApplicationRecoveryJourneyTest` 不使用 setContent 假宿主：实际 MainActivity、输入框、离线引擎、ActionRepository、角色会话存储和丝路进度参与。问题“今天吃什么”的回复及行动卡核对真实仓储同一道菜，而非仅检查节点存在。
- 游戏通过真实按钮推进敦煌后保存；运行器实际结束该包进程并核实 PID 改变，再验证角色/Expanded 抽屉/资源进度恢复，返回 Peek、浮球和重新召回。
- 本机 API 35 隔离模拟器两阶段各 1 项通过。最新证据目录 `work/.dispatch-backup/20261014-application-recovery/2026-10-14T11-57-44-671Z/`，实际进程 8343 → 8390；清单包含应用与测试 APK SHA256。不是手机验收。
- 真实测试发现控制区超出抽屉、输入不可达；采用抽屉约束高度及滚动修复。输入/行动卡增加稳定测试标签。测试使用未合并语义定位嵌套滚动，避免误点后台页面。
- 系统沉浸模式教育提示抢占焦点，通过 dumpsys window 找到根因。运行器仅在模拟器/包名校验后确认该系统提示；不更改手机设置。
- 九项 Node 契约通过；JVM 505 项、0 failure/error；lint 0 error、84 warning、20 information；Debug/AndroidTest/Regression 构建通过。完整 Debug Compose 测试报告 58 项、0 failure/error、2 skipped，即 56 项实际通过；跳过的两阶段用例已在独立 regression 包单独执行通过，不能算作 Debug 的 58 项通过。
- CI 本地配置增加第九契约，原模拟器步骤先执行全部 Debug 测试，再执行两阶段隔离恢复；新增 Node 20 与证据上传。没有推送，远端运行未验证。

## 复现

只对独立模拟器执行；默认 ADB 端口 5038，可用环境变量修改。CI 使用 5037。

```powershell
.\gradlew.bat -PxuanjiRegression=true :app:assembleRegression :app:assembleRegressionAndroidTest
$env:XUANJI_ADB='C:/Users/blueice/AppData/Local/Android/Sdk/platform-tools/adb.exe'
$env:XUANJI_ADB_PORT='5038'
node _dev/isolated_application_recovery.js 127.0.0.1:5581
```

普通门禁不加 regression 参数，默认两阶段用例跳过，防止读写正式资料；两阶段由受保护的运行器独立执行。运行器保留安装、两阶段、启动日志及哈希清单；失败不会清数据、卸载或回退。

## 下一步及边界

1. 完整应用组合覆盖临时条件、指代、角色交接、其他游戏和切档案，当前仅已闭环饮食/纳迪尔/丝路这一条链路。
2. 地区现实气候/预算比较仍缺可靠可离线使用资料，不编造匹配数据。
3. 连续图片内存峰值、低端实体机和重复基准尚未闭环，不以单点 PSS 或组件样本宣称完成。
4. 手机安装单独执行；此前手机安装证据仍以 2026-10-13 交接为历史记录。本轮 ADB 只发现隔离模拟器。

本轮待安装正式 Debug APK：`app/build/outputs/apk/debug/app-debug.apk`，SHA256 `153E6746ECEB69D65BA7BD2460A1858BBDDCFF2DF7DF8BE582E31BE6DDCAEE67`。包含未提交源码，Git HEAD 不能单独重现该产物。
