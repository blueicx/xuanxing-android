# X3 玄师陪伴五项优化执行账本

## 2026-10-19 占卜学习与 CI 续补

- [done] 塔罗实际牌位/花色/正逆位观察，原始牌阵主动保存与日历回看；卢恩三符完整展示、24 符目录/位置对照/主动保存。
- [done] 基础日期、姓名五数及周期的实际归约步骤；拒绝将汉字/未支持字母按 ASCII 偏移计值，明确不同约定与项目自编评论边界。
- [verified] 571 JVM、10 Node、lint 0 error/75 warning，Debug/AndroidTest/Regression 构建；78 项 Debug 交互为 75 通过 + 3 隔离保护跳过；三阶段真实进程恢复通过。
- [verified] XZ2 保留数据更新 1.1.1（3），设备哈希一致、冷启动正常、当前进程 crash buffer 为空。未做人工视觉/TalkBack。
- [done] 修复远端 SDK 废弃 tools 包，回归契约先失败后通过。远端构建任务已通过，模拟器任务仍以最终回执为准；总清单仍有第 6、8–12 包未完成。

## 2026-10-19 库存采购与餐单条件草稿

- [done] 结构化手填库存数量/单位/期限/分类，独立已买采购，明确/幂等入库及同名替换确认；未知量不推算，无单位转换或自动扣减。
- [done] 期限筛选与默认关闭的摘要提醒；餐单草稿显式保存、冷开/旋转恢复、当前候选重新核验及失效锁定阻止保存；按功能清除。
- [done] DailyLife v5 / 备份 v6，兼容旧版并保留旧备份没有的本机新字段；库存/采购/草稿冲突预览和失败回滚测试，备份界面明确披露分区。
- [verified] 558 JVM、10 Node、lint 0 error/75 warning、Debug/AndroidTest/Regression 构建；全量 UI 73 声明中 70 实际通过、3 隔离保护跳过，0 失败。修复快速入库保留焦点造成的可见性回归，保留失败证据。
- [verified] 独立应用三阶段恢复通过，PID 5519→5568，逐字段核对库存、采购、草稿的真实进程恢复与其他档案隔离；最终 crash buffer 为空，模拟器已关闭、磁盘保留。
- [pending overall] 12 包其余厨房条件、日记、详情/占卜/测验、角色任务/游戏课程、首页/分享/工程仍未完成。最新交接 `docs/superpowers/handoffs/2026-10-19-pantry-and-weekly-drafts-handoff.md`。
- [not run by scope] 手机安装、人工视觉/大字号/TalkBack、真实通知等待、远端 CI、提交/推送；只用隔离模拟器和合成资料。

## 2026-10-18 日常与陪伴 12 包深化（整体未完成）

- [done] 七日餐单组合：当前真实候选过滤/评分、锁定、重复惩罚/食材复用、每人参考预算、缺餐说明，预览后整批保存 21 餐；不当作未来运势或营养方案。
- [done] 安排时长/跨日区间冲突、空闲起点、有限批量重复与整日延期；重叠、容量、损坏保护不部分写入，已完成条目不覆盖。
- [done] 日期范围 `.ics` 导出，默认无备注，不含出生/聊天；DailyLife v4、备份 v5，旧文档/备份兼容。
- [verified] 当前源码 543 JVM、10 Node、lint 0 error/75 warning、Debug/AndroidTest 构建；新增 3 交互通过，完整 70 项声明中 67 实际通过、3 隔离保护跳过，0 失败。
- [verified] 独立 Regression 构建与三阶段真实应用恢复通过，PID 5232→5282；最终模拟器 crash buffer 为空，本轮启动的模拟器已关闭，磁盘保留。证据见当日交接。
- [pending] 12 包剩余：厨房条件、结构化采购库存、周计划草稿、主动日记、探索详情/占卜/测验、角色任务/游戏课程、首页/小组件/分享、工程与正式签名；不得用本切片门禁代替整体完成。
- [not run by scope] 手机安装、人工视觉、大字号、TalkBack、远端 CI、提交/推送。交接 `docs/superpowers/handoffs/2026-10-18-daily-companion-deepening-handoff.md`。

## X3 全量扩充（2026-10-13）

- [verified latest 2026-10-15] 重开/清除统一递增token、取消pending并清持久化线程，保留其他角色及共享主动记忆；新增失败先行回归。整理20处primitive state、6处Modifier、两处命名和冗余API分支。515 JVM、十项契约、lint 0 error/75 warning/0 information、Debug/AndroidTest/Regression构建、59实际Debug交互（另3保护跳过）、三个独立应用恢复阶段通过。最新APK `E909CA79…`及证据见`docs/superpowers/handoffs/2026-10-15-thread-safety-and-lint.md`。不安装手机、不提交/推送；以下数字和哈希保留为历史。

- [verified final 2026-10-15] 补齐24城气候参考（明确测站/网格差异）、全部城市/单城依据展开与实例恢复；808回复增加真实行动/画像内容断言；移除进程级线程缓存并迁出局部状态模块。514 JVM、十项契约、lint 0 error/84 warning、Debug/AndroidTest/Regression构建、59实际Debug UI（另3隔离保护跳过）及三个独立恢复阶段通过。证据及待安装APK `F724646B…`见`docs/superpowers/handoffs/2026-10-15-full-expansion-final.md`。预算/节奏缺资料，不编现实排序；无手机安装/提交/推送/远端CI。下文“18城/510”等保留为历史。

- [verified closeout 2026-10-15] 地区独立事实对比（18 JMA 测站；缺失维度不计分）、真实四角色交接/四游戏推进与保存/合成档案隔离、连续应用内存与重复舞台基准完成本轮实现。510 JVM、十项契约、lint 0 error/84 warning、57 实际 Debug UI（另3隔离保护跳过）与三个 regression 阶段通过。实体低端机/手机安装/远端 CI 未运行。当前产物和证据见 `docs/superpowers/handoffs/2026-10-15-remaining-work-closeout.md`；后文 remaining 标记保留为当时的历史状态。

- [verified application 2026-10-15] 真实页面新增临时食材/时间条件、第二项追问、具体候选依据、长期偏好不自动写入及条件/引用进程恢复；校验器“分钟→分数”误判与解释引用重置先复现后修复。508 JVM、九项契约、lint/构建通过；完整 Debug UI 56 实际通过/2 隔离保护跳过，扩展两阶段另行通过。详细证据与剩余边界见 `docs/superpowers/handoffs/2026-10-15-reference-followup-handoff.md`。

- [implemented core] 波次 1：64 道菜、32 活动、16 环境、24 城市；同规则过滤替代品，贡献分合计一致。今日情境可主动选择；时间紧同时限制备餐与活动。
- [implemented core] 波次 2：档案隔离的安排增改删/完成撤销、购物清单、原始结果快照日历、显式提醒和按功能清除。
- [implemented core] 波次 3：60 术语、16 自编短解说、本地搜索和真实目的页；旧详情正文保留，避免依据重复显示。
- [implemented core] 波次 4：临时条件、候选指代及手动交接；202 输入 × 4 角色 = 808 次真实引擎评估。指代回复与行动卡指向同一候选。
- [partially implemented] 波次 5：诗句 24 关、星图 24 关、丝路 12 固定任务、游戏存档 v5 迁移；九城坐标地图、多分支、三类资源可行路线比较、逐站确认/撤销/规划恢复；象棋真实半步提示和复盘。地区真实条件比较仍缺可靠资料。
- [implemented core] 波次 6：备份 v2、新旧协议读取、超限拒绝、暂存校验后 CAS 原子写入、恢复日志/回滚；新增/合并/覆盖/遗漏预览随最终分区勾选更新，备注独立比较，不误判未勾选的私人备注。收藏与反馈覆盖须确认，相同游戏进度合并。
- [in progress] 波次 7：CI 执行全部 AndroidTest，新增扩充契约、JVM/lint 报告和 APK SHA256；性能测量与状态进一步拆分仍待完成。
- [implemented core] 临时条件、引用/选中候选、当前动作、依据展开、角色游戏入口接入 MysticSessionState reducer 与会话快照 v2；兼容 v1，旧 token 丢弃，按角色/档案恢复，跨日清理旧生活建议并保留游戏入口。卡片从真实数据重新构建，不保存盘面副本。
- [implemented core] 对话安排路由接入真实候选，并要求用户主动确认时间/备注后保存；图片在 IO 线程解码、最大边 1600px、共享 24MiB LRU 缓存，四张实际素材自动测试通过。
- [implemented core] 舞台检查点按档案/日期隔离，角色与抽屉恢复；真实 DataStore 重新打开、Compose 保存实例恢复、返回先收起再关闭测试。后台棋局取消、提示继承取消上下文、代次屏蔽旧发布；搜索节点预算 128/256/1024。
- [measured component] 四素材解码合计 12 MiB；舞台就绪与 PSS 点采样、四局真实搜索有日志。普通密集开局单次样本从 3831ms/872 节点变为 714ms/256 节点，减少了搜索分支，不宣称等强度或全设备保证。
- [verified application 2026-10-14] 独立 regression 包 + 合成资料运行真实 MainActivity：输入饮食问题，回复/卡片与真实仓储一致；滑动到纳迪尔、丝路推进敦煌、保存；实际 force-stop 后 PID 改变，角色/抽屉/游戏恢复；返回浮球再召回。两阶段通过，证据 `work/.dispatch-backup/20261014-application-recovery/2026-10-14T11-54-46-892Z/`。修复控制区超过抽屉时不可滚动；CI 已接入隔离链路，未推送。
- [remaining] 多条件/指代/交接和其他游戏的完整应用组合覆盖；连续图片内存峰值/实体低端机/重复样本；地区现实条件比较的数据来源。
- [verified 2026-10-14] 九项 Node 契约、505 JVM、lint 0 error/84 warning/20 information、Debug/AndroidTest/Regression 构建通过；完整 Debug 模拟器报告 58 项中 56 实际通过、2 按包名保护跳过，隔离 regression 两阶段另行通过。待安装正式 APK SHA256 `153E6746ECEB69D65BA7BD2460A1858BBDDCFF2DF7DF8BE582E31BE6DDCAEE67`，26891201 bytes；未安装手机。
- [verified latest] 8 项 Node 契约、505 JVM、lint 0 error/84 warning/20 information、Debug/AndroidTest 构建通过；隔离模拟器 56/56（94.607 秒），增加实例重建、返回键、生产 DataStore 舞台恢复、真实素材/棋局基准；日志 `stage-immutable-full-final.log`。取消/预算/提示继承与旧发布阻断先失败后修复，保留 45 条 golden。
- [device smoke latest] USB Xperia XZ2 覆盖安装 Success，冷启动 1615 ms，PID 23633 正常，该 PID 的 AndroidRuntime error 检查无输出；设备 APK SHA256 与本地 `BA91B54A61FD2919303927DAE07B93CF96B9A49C3556722086E04EDF1F895D6B` 一致。APK 26891756 bytes，清单 `stage-build-manifest.json`。未清除数据、不操作用户游戏存档，不等于完整设备功能验收。
- [verified previous] 8 项 Node 契约、497 JVM，隔离模拟器 53/53（78.577 秒），日志 `map-storage-uid-final.log`；测试目录 UID 不匹配修正后全套通过，未访问既有资料。旧手机 APK `9C678D7AD2E4A25DA2CF79EA64B76FEC4681CC6A1A3B3C4561E879FE409A0FE5` 已被本轮覆盖。
- [verified] 2026-10-13 最新源码：8 项 Node 契约、489 JVM、lint 0 error/84 warning、Debug/AndroidTest 构建通过；隔离 API35 模拟器 49/49（76.153 秒）真实交互通过，含会话条件/依据恢复和预览统计断言。
- [measured] 隔离模拟器首页进程冷启动 925/1093/1051 ms，PSS 111190 KB；权限弹窗造成的零耗时弃用。不是舞台、图片峰值、棋局或真机性能验收。
- [device smoke] USB Xperia XZ2 已覆盖安装并冷启动，安装 APK 哈希与本地一致；进程正常，未观察到启动崩溃。未清除数据、不操作用户游戏存档，不等于完整真机功能验收。
- [not run by scope] 人工视觉/大字号/TalkBack；不提交、不推送

## X3 可信与个性化收口（2026-10-11）

- [done] 规格与计划：`docs/superpowers/specs/2026-10-11-xuanji-trust-and-personalization-design.md`、`docs/superpowers/plans/2026-10-11-xuanji-trust-and-personalization.md`
- [done] 资料完整度：显示用户已录入资料及其用途，不改算法、不补猜输入
- [done] 行动偏好：本机按档案保存用户明确选择的活动约束并用于离线计划
- [done] 备份加密：新增 AES-GCM/PBKDF2 可选加密包装，旧格式仍可导入
- [done] 周/月回顾：仅聚合收藏与明确行动反馈
- [done] 定向拆分：迁出对话生成器数据模型与舞台纯展示组件
- [done] 契约/JVM/lint/debug/AndroidTest 构建验证：7 项 Node 契约、441 项 JVM、lint 0 error、Debug/AndroidTest APK 均通过
- [done] 隔离 API 35 模拟器：Compose `connectedDebugAndroidTest` 45/45 通过；使用专用 ADB 5038，不触碰用户手机
- [not run by scope] 真机安装、人工视觉/TalkBack/大字号验收；不提交、不推送

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

## X3 运势阅读扩充（2026-10-03）

- [done] 任务 1–5：详情问玄师、收藏回看、周期/一生命盘解说、行动反馈排序已分任务提交；以 `docs/superpowers/plans/2026-10-02-xuanji-reading-experience-expansion.md` 为准
- [done] 任务 6 代码：四角色对话读取当前 profile 的有效游戏 checkpoint；进度依据标为本机存档；缺档诚实回退；游戏结束摘要仅在用户主动操作后收藏
- [pending] 任务 6 Compose 运行：AndroidTest 源码已编译；本机未配置 AVD，未运行 connected 测试，也未触碰真机
- [done] 任务 7 文档与本机门禁：README/系统概览/技术债/交接已同步；六项 Node 契约通过；JVM 415 项通过；lint 0 Error、84 Warning、16 Information；Debug 与 AndroidTest APK 构建成功
- [pending] 任务 7 Compose 运行：无 AVD；新增 AndroidTest 未运行，不连接/安装手机

## X3 稳定可信与本地回顾扩充（2026-10-03）

- [done] 范围确认：用户确认完成交互链路、100+ 对话回归、本地回顾时间线、选择性备份恢复、三款角色游戏深化
- [done] 规格与实施计划：`docs/superpowers/specs/2026-10-03-xuanji-reliability-review-design.md`、`docs/superpowers/plans/2026-10-03-xuanji-reliability-review.md`
- [done] 任务 1：106 条输入 × 4 角色 = 424 次真实离线分析/路由/回复评估；修复嵌入问候短语导致的话题误分类
- [done] 任务 2：近期选择时间线模型、仓储、筛选与收藏/行动反馈 Compose 交互测试
- [done] 任务 3：选择性备份/恢复、SHA-256 校验、只读预览、档案不匹配阻断、显式冲突覆盖；内存存储 Compose 交互通过
- [done] 任务 4：诗句/星图固定关卡、提示、撤销、重开/完成摘要；丝路资源回算与逐段回放；v1→v2 进度迁移
- [done] 任务 5：七项 Node 契约、JVM 430 项、lint 0 error、Debug/AndroidTest APK、隔离 API 35 模拟器 Compose 21/21 通过；交接文档完成
- [not run by scope] 真机、视觉截图、TalkBack、大字号验收；本轮不连接、不安装用户手机

## X3 日常与陪伴扩充（2026-10-16）

- [done] 按现有目录与分类补库存、1–20 份餐食、保存时目录预算、本周餐单、复制/延期和同时间冲突确认；购物清单标记库存且保留菜品来源。
- [done] 软记忆 v2：有效期、编辑/撤回/清除，显式编辑及撤回不被后续推断覆盖；损坏数据不由普通写入覆盖，过期标签仅留管理页。
- [done] DailyLife v3 与备份 v4：兼容旧版本，未知价格不补猜，库存/安排备注均遵循备注选择，恢复保留未选本机备注。
- [done] 安排通知完成/延后操作、唯一工作键和档案/事项/时间复核；构建版本/HEAD/dirty/UTC/APK SHA256 清单接入 CI 上传。
- [verified] 最终源码十项 Node 契约、524 项 JVM、lint 0 error/75 warning；Debug/AndroidTest 与隔离 Regression 构建通过。新增七项交互全过，最终完整 Debug UI 67 项中 64 通过、3 项隔离保护跳过、0 失败。
- [done] 三阶段独立包真实应用进程恢复通过，PID 6022→6071；四角色/游戏/另一档案隔离通过。交接：`docs/superpowers/handoffs/2026-10-16-everyday-expansion-handoff.md`，APK SHA256 `D3D0EC499EFE2B83996BB8438602B80A976694F833BAF9FAD6B87E296C6EFAAF`。
- [not run by scope] 手机安装、人工视觉、大字号及 TalkBack；未提交/推送。只在新建隔离模拟器使用合成资料。
# 2026-10-19 推荐条件与记录回顾续补

## 1.1.2 完整收口

- [done] 探索计算/相邻周期/四柱敏感性、九类测验私有草稿与复核、档案分数历史和逐条授权、角色任务继续/交接、四游戏学习、首页重排/阅读位置/桌面入口、实际字段选择及全文图片分享。
- [verified] 596 JVM、11 Node、0 lint error（76 warning）、Debug/AndroidTest 和独立 Regression 构建通过；87 项完整 UI 为 82 实际通过 + 5 保护跳过，独立包另跑 3 恢复阶段及 2 入口导航。
- [delivered] Xperia XZ2 保留数据升级 1.1.2（4）、冷启动存活且所查 crash buffer 无本包崩溃；APK hash、性能基线和边界见 `docs/superpowers/handoffs/2026-10-19-deepening-completion-handoff.md`。
- [boundary] 正式签名密钥、人工视觉/大字号/TalkBack、第三方系统接收器和长期省电提醒不伪称通过；远端 CI 独立核对。以下保留此前切片记录。

- [done] 设备/难度/便当、显式库存优先、每日/替代/周餐单统一过滤，文化分数与现实排序分离。
- [done] 主动日记/心情/实际执行、周/月描述统计、单日删除、分区清除与备注分区可选备份；DailyLife v6 / 备份 v8 旧版兼容。
- [done] 数字个人周期大师数过早拆位缺陷：失败断言复现后统一归约器与可见计算链。
- [verified] 新增两项隔离 Compose 交互修正后均通过；最后全门禁、全量交互与真实进程恢复另记交接。
- [verified] Debug 1.1.0（2）源码门禁通过，已保留数据安装 Xperia XZ2 并冷启动；最终 76 项交互为 73 通过 + 3 隔离保护跳过。进程恢复与 Git 交付另见交接；整体 12 包剩余范围保持未完成。
