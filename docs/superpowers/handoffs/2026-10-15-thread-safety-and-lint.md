# 会话清除与工程整理（2026-10-15）

继续执行原全量扩充计划的状态/隐私和工程收口，不新增占卜体系，不使用子代理、不提交、不推送、不安装手机。上一轮功能清单及来源见[全量扩充交接](2026-10-15-full-expansion-final.md)。

## 修复依据

原线程清除将`sessionState`赋为默认状态，token归零；原“重开对话”只清可见列表，未清持久化消息。静态契约和`MysticThreadClearTest`先失败，确认这两个入口没有统一失效旧请求。现在两个入口共用`resetCurrentThread`：通过`MysticEvent.Clear`递增token、清空当前消息/交互、清除pending请求与临时UI，并写回当前角色线程。存储层单角色清除也走同一reducer，保留其他角色消息、共享主动记忆和单独保存的游戏进度。

新增JVM回归检查清除前后token、nextTurnId、旧回复丢弃、其他角色线程、共享主动记忆与档案隔离；不读写用户真实DataStore。原45条golden及202输入×4角色矩阵均保留。

## lint整理范围

- 六处Compose Modifier改为第一个可选参数，调用点经编译核对。
- 修正两处Compose命名；纯Brush工厂移除不必要的Composable声明。
- 二十处primitive state使用对应Int/Long/Float/Double专用类型；今日签的两处`.value`改为`.intValue`，不改变数值和确定性规则。不宣称量化性能提升。
- 移除minSdk 26下冗余的API 26运行时分支，通知频道仍照常创建。
- 未屏蔽lint，也未删除用户素材/资源；依赖版本、target SDK、方向锁定、图标和Application级容器等问题保持显式记录，需要各自兼容性或设计验证。

剩余75项warning分布：46个UnusedResources、16个GradleDependency、5个StaticFieldLeak、各2个IconLocation/MonochromeLauncherIcon，以及DiscouragedApi、LockedOrientationActivity、ObsoleteSdkInt、OldTargetApi各1个。无error、无information。StaticFieldLeak所指向的容器是否仅持Application需要继续逐项核实，不能直接视为5个已证实的Activity泄漏；依赖“有新版”也不是可无风险批量升级的证据。资源没有执行删除，方向锁定没有在缺少横屏验收时擅自放开。

当前`XuanjiApplication.onCreate`向这些仓库传入的是Application实例；本轮没有发现该路径持有Activity的证据。构造函数仍接受通用Context，未来新增调用者仍可能破坏这一约定，因此保留警告、不加SuppressLint，后续可收紧构造边界并补专门测试。

## 验证边界

隔离模拟器使用ADB 5038，AVD为`XuanjiIsolatedApi35_20261011`；不操作手机。真实应用恢复测试继续使用`com.xuanji.app.regression`独立包和合成资料。人工视觉、大字号、TalkBack仍排除；远端CI未推送触发。

## 最终结果与待安装产物

- 十项Node契约全部通过，保留45条golden；515项JVM单测，0失败/错误/跳过。包含新增线程清除回归及202输入×4角色的真实离线引擎矩阵。
- `lintDebug`、`assembleDebug`、`assembleDebugAndroidTest`、`assembleRegression`和`assembleRegressionAndroidTest`通过；lint为0 error / 75 warning / 0 information。
- 完整Debug模拟器报告62项、0失败/错误、3项包名保护跳过，即59项实际通过，用时191.301秒。不是62项全部执行。
- 三阶段真实应用恢复各1项通过，证据`work/.dispatch-backup/20261014-application-recovery/2026-10-15T10-52-16-367Z/`；实际进程4452→4506。第三阶段运行器清单记录81个PSS样本，观察最大298298 KiB、中位数254520 KiB；有Debug/instrumentation开销，不是实体低端机或绝对峰值，不作跨次性能优化百分比。
- 复现沿用[全量交接中的命令](2026-10-15-full-expansion-final.md)，本轮恢复链路安装的是独立regression包，不是正式手机应用；没有卸载或清除正式包数据。

最新待安装文件：`app/build/outputs/apk/debug/app-debug.apk`，包名`com.xuanji.app`，25759595 bytes，versionName 1.0.0/versionCode 1；应按哈希辨别新旧：

```text
E909CA7951BBAAA02DFEAA1601D1842B3C7608CE7E3B2C1C83CF3F983843E709
```

机器可读清单：`work/.dispatch-backup/20261015-thread-safety-lint/final-build-manifest.json`。工作树未提交，HEAD `0456228af1cc061d97acec98e24201943a580cc7`不能单独重现此APK。没有手机安装、提交、推送或远端CI记录。数据授权/现实预算节奏资料不足等原有边界未改变，剩余警告不宣称已全部清零。
