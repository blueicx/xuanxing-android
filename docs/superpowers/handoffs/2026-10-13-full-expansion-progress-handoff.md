# X3 全量扩充进行中交接（2026-10-13）

## 状态与边界

整份计划仍未全部完成。本轮不使用子代理，不清理、不提交、不推送；用户通知手机连接后已覆盖安装并启动，未清除数据。人工视觉、大字号和 TalkBack 按用户要求排除。保留原工作区成果，直接在 `codex/reading-expansion` 继续。HEAD 为 `0456228af1cc061d97acec98e24201943a580cc7`，APK 包含未提交源码，不能用 HEAD 单独重现。

计划：`docs/superpowers/plans/2026-10-13-full-expansion.md`。逐波状态：`.superpowers/dispatch-progress.md`。基线和日志：`work/.dispatch-backup/20261013-full-expansion/`。

## 当前已实现

- 64 道菜、32 活动、16 环境、24 城市静态标签；配方、步骤、预算与时间独立，替代菜同规则过滤。四类权重贡献分合计与总分一致；今日情境主动选择，时间紧约束备餐/活动。
- `DailyLife` 模型、Codec、档案隔离 Store、安排时间/备注/完成撤销/修改删除、购物清单、原始快照与日历。提醒显式开启、按星期检查、取消通知和工作；点击进入日期。隐私页按功能清除。
- 60 术语、16 自编短解说、真实本地搜索入口与收藏；原详情保留，依据不重复显示。来源限定为项目自编规则说明，不假称专业原典。
- 临时条件直接重算真实计划；主动点保存才持久化。第二个等指代回复和行动卡一致，不改变原始排序；四角色可手动交接摘要。202 条输入 × 4 角色 = 808 次真实分析/引擎/路由/安全评估，原 45 条 golden 保留。
- 诗句/星图各 24 固定关、丝路 12 固定任务、来源/提示/撤销/重玩、进度 v5 迁移；旧 5 关完成语义保留。九城坐标投影、多分支、资源可行的三类完整路线、逐站确认/改道/撤销/规划恢复与缩放/拖动/全屏；旧四站结算不变。画像补充任务/技能/环境、穿搭/桌面/房间说明。
- 对话加入安排路由指向真实当前候选，由用户确认时间/备注后保存；象棋提示/复盘加入规则校验的实际半步解释，不声称必胜证明。角色素材 IO 下采样到最大 1600 px、24 MiB LRU，仍有 Canvas 回退。
- 备份 v2 支持新分区及 v1、加密包装。完整校验后暂存再原子 CAS；失败日志/回滚/中断恢复；同档案冲突确认、超限拒绝，不静默截断。生产 DataStore 单次 edit，内存测试支持失败注入。
- CI 不再限定 AndroidTest 类名单；增加扩充契约、测试/lint 报告与 APK SHA256/源码提交/版本元数据产物。仅本地修改，远端 CI 未触发。

## 最新舞台恢复与搜索收口

- `MysticStageLayout` 原 `remember` 抽屉丢失问题已用保存实例恢复测试复现并修复；舞台接入按实际档案/日期的检查点，保存角色、打开状态和独立抽屉。主动关闭当前抽屉退回 Peek，系统返回先收起再关闭；正常默认仍是浮球。
- `BoardWorkRunner` 管理后台棋局及发布代次，切角色/档案/关闭时取消。旧非协作任务禁止发布；失败释放忙碌状态。提示的阻塞接缝继承父任务取消；搜索逐节点检查取消，计数预算易/普通/困难为 128/256/1024，保留深度上限 2/3/4。不是等强度提速或必胜证明。
- 最新门禁：8 项 Node 契约、505 JVM、lint 0 error / 84 warning / 20 information、Debug/AndroidTest 构建、diff 检查通过。隔离 API35 全套 56/56，94.607 秒；`stage-immutable-full-final.log`、`stage-immutable-source-final.log`。
- 新增真实素材/棋局性能宿主：四图解码合计 12 MiB；PSS 是点采样而非连续峰值。一次普通密集开局从 3831ms/872 节点变为 714ms/256 节点，搜索分支减少，不能宣传同强度提速。每轮原始样本保留于 `stage-*-perf-observations.log`；后续执行仍有正常负载波动。
- 最新 APK 26891756 bytes，SHA256 `BA91B54A61FD2919303927DAE07B93CF96B9A49C3556722086E04EDF1F895D6B`；路径 `app/build/outputs/apk/debug/app-debug.apk`。版本仍 1 / 1.0.0，包含未提交成果，未提交/推送。
- USB Xperia XZ2 已覆盖安装 Success，COLD 启动 1615ms，PID 23633；该 PID AndroidRuntime error 查询无输出，设备 base.apk SHA 与本地一致。日志 `xz2-stage-*.log`、`stage-build-manifest.json`。仅安装/启动检查，未读写私人游戏进度，不是完整设备功能验收。
- 仍需：真实应用导航与操作系统杀进程恢复、连续内存峰值/实体低端机和多轮统计、地区现实条件可靠来源与匹配；已有组件或存储恢复测试不替代这些验收。

## 前一轮地图收口验证与设备产物

- 八项 Node 契约、497 JVM（0 failures/errors）、lint 0 error / 84 warning / 20 information、Debug/AndroidTest 构建全部通过；`git diff --check` 无差异格式错误。
- 隔离 API35 全套真实 instrumentation：53/53、78.577 秒，专用 ADB 5038、显式目标 `127.0.0.1:5581`。新增地图触控/复位/完整路线确认、四张真实资源下采样与缓存、生产 DataStore 原子 CAS/拒绝错误预期/关闭后重新打开/删除测试；使用全新合成测试缓存文件，不读既有资料。日志 `map-storage-uid-final.log`。
- 首次资源受限路线用例发现最优方案漏掉可行分支，已修复并通过；旧路线 UI 测试改为滚动到实际按钮后点选，地图文本断言改用显式 substring。DataStore 测试首次目录 UID 不匹配已定位修正，全套重跑通过。
- 当前 APK：`app/build/outputs/apk/debug/app-debug.apk`，26123651 bytes，SHA256 `9C678D7AD2E4A25DA2CF79EA64B76FEC4681CC6A1A3B3C4561E879FE409A0FE5`。versionCode 1 / versionName 1.0.0，包含 dirty tree，未提交/推送。
- USB Xperia XZ2 `QV7017NH1F` 已覆盖安装 Success，启动 Status ok / COLD / 1523 ms，PID 21983；设备 base.apk 哈希与上述一致，限定该 PID 的 AndroidRuntime error 查询无输出，后续进程仍存在。仅安装/启动检查，不是完整功能或私人存档验收，未清除或卸载。
- 产物清单 `map-build-manifest.json`；源码门禁日志 `map-source-gates-final.log`；安装/启动/哈希/崩溃检查分别为 `xz2-map-*.log`，均位于 `work/.dispatch-backup/20261013-full-expansion/`。

## 前一轮实际验证（历史记录）

- 八项 Node 契约通过，含新增 full_expansion；原行动契约的 8 道菜是保留的旧 fixture，不是当前完整目录规模。
- `:app:testDebugUnitTest`：489 tests，0 failures/errors（含恢复预览与新增角色交互状态/跨日/旧 token/档案隔离测试）。
- `:app:lintDebug`：0 errors，84 warnings（分级见技术债）。
- `:app:assembleDebug`、`:app:assembleDebugAndroidTest` 成功。
- 专用 ADB 5038、隔离 API35（本轮显式连接别名 `127.0.0.1:5581`，已验证 ro.kernel.qemu=1）全套 instrumentation：49/49，76.153 秒，含恢复预览与条件/依据通过真实 reducer/Codec/Store 恢复的点击测试；最新日志 `interaction-crossday-final.log`。测试不访问手机私人存档。
- 首次 47 项中两项因新增依据重复渲染失败；修复后 13 项定向回归及最终 48 项全量均通过。日志 `instrumentation-final.log`，不是仅编译或节点存在。
- 首页进程冷启动 925/1093/1051 ms，PSS 111190 KB；`runtime-baseline.json`。首次权限弹窗干扰的零耗时弃用，后仅在模拟器授予通知权限并重测。没有以此宣称舞台/图片/棋局/实体机性能通过。

## 前一轮已安装 Debug 产物（不是最新源码）

- 路径：`app/build/outputs/apk/debug/app-debug.apk`
- applicationId：`com.xuanji.app`；versionCode 1 / versionName 1.0.0（未变更发布版本）。
- 大小：26281797 bytes。
- SHA256：`9D894C45B991478E478D7835F885D0C40926243D4263F269C5046F6BB0F43CF0`
- USB Xperia XZ2 覆盖安装 Success，冷启动 Status ok / 1533 ms；设备 base.apk SHA256 与上述值一致，应用 PID 12269 仍在，未观察到该进程 AndroidRuntime 崩溃。日志 `xz2-install.log`、`xz2-launch.log`、`xz2-installed-apk-hash.log`。未清除数据/未卸载，不代表全部真机功能已验收。
- 本地清单：`work/.dispatch-backup/20261013-full-expansion/build-manifest.json`。

## 下一轮必须继续，不可宣传为全部完成

1. 临时条件、引用、当前动作、依据及游戏入口已接入 session reducer/快照 v2，兼容 v1，跨日清理旧生活条件但保留游戏。对话安排入口已补，仍需组合指代/交接及完整应用进程终止/旋转/导航验收；测试存储重建不替代这些验收。
2. 恢复预览的最终分区统计已完成；继续扩大失败与真实生产存储交互覆盖。统计单位为记录、偏好项与单独备注字段；未选备注不误判冲突，相同游戏状态合并，收藏/反馈差异覆盖也须确认。
3. 地区真实气候/节奏/预算比较尚无可靠数据；当前只诚实呈现用户条件。象棋实际半步说明已补，但不是强制胜法课程。
4. 丝路九城、多分支与实际触控已补；仍需完整应用/进程恢复验收。完整路线比较不预支事件收益；不伪造现实导航，长安采用现代西安参考点，所有坐标来源在 `SilkRoadGeography.kt` 中保留。
5. 进一步拆分大文件；舞台/棋局组件基线和取消已补，连续图片峰值与实体低端机/重复统计仍待验证，不用点采样冒充峰值。
6. USB 手机本轮已在线并完成覆盖安装/启动；测试仍在隔离模拟器运行，不操作用户行程或游戏存档。后续源码变动须重新跑门禁并记录新 APK 哈希。默认 5037 仍被系统进程占用，不停止该进程；模拟器实际运行的 headless 子进程通过本机 5581 连接到专用 5038。
