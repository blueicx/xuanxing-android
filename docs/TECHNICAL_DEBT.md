# X3 玄机技术债台账

## 2026-10-19 占卜学习补齐与 CI 修复

- 塔罗关系观察、用户主动保存原始牌阵/规则/种子和日历回看；卢恩三符完整显示、24 符目录、位置对照和主动保存；基础日期/姓名/个人周期均公开真实步骤。新学习提示标明项目自编，不增加伪经文或命运结论。
- 修复姓名 `isLetter()` 将汉字等映射为 ASCII 偏移的错误；只接受明确支持的英文/拼音字母，不自动转写。基础日期与完整生命路径的不同约定分别说明，不悄悄改掉历史结果。
- 远端首跑失败发生在 SDK 初始化，原因是 setup-android v3 默认安装废弃 `tools` 包；两个 job 显式改装 `platform-tools`，增加会先失败的契约回归。修复提交 `bd9eeee` 已推送，远端构建 job 已通过；整个远端模拟器任务仍须以最终回执为准。
- 总清单完成 1–5、7；未完成 6、8–12：探索式详情、测验完整恢复/复核/引用同意、四角色任务深化、游戏课程、首页配置/小组件，以及分享/工程与正式签名收口。下文旧列表是历史切片，不作为当前完成状态。

## 2026-10-19 推荐条件、记录回顾与交付

- 本次补齐设备/难度/便当过滤和显式库存优先，保持四类文化贡献分不变；主动日记/心情/实际执行及周月描述回顾接入日历、清除、可选备份、冲突确认和回滚。存储 DailyLife v6 / 备份 v8，旧版缺失日记不会擦除本机日记。
- 个人周期曾先把 11/22/33 拆位，违反其声明约定；失败断言已复现，统一归约器修复并展示逐步过程。仅此数字解释切片，不代表现有占卜包已整体完成。
- 整体 12 包仍未全部完成：6 探索详情/相邻周期/时间敏感性；7 塔罗关系/卢恩学习及姓名归约全链；8 测验草稿/复核/历史比较/引用同意；9 角色任务交接深化；10 游戏课程；11 首页排序/固定/续玩小组件；12 图片选择分享、大文件/性能/lint 分级与正式签名。没有正式发布密钥，当前交付仅 Debug，不宣称 Release。
- 用户本轮已授权安装与推送。实际自动化、手机安装/启动和提交证据独立记录；人工视觉、大字号与 TalkBack 仍在排除范围。以下为历史记录，不能当作当前缺口或当前验收。

## 2026-10-19 库存采购与草稿收口（此前切片）

- 已接真实界面/存储/备份/清除：独立采购、手填数量单位期限与分类、明确且幂等入库、同名替换确认、期限筛选、默认关闭的期限摘要提醒、餐单条件草稿。旧备份不会擦除本机新元数据。
- 条件草稿不是固定整周推荐快照，恢复后依当前偏好重新校验，不绕过新过敏/排除条件。没有自动库存扣减、用量/营养推算、单位换算或食品安全保证；库存尚未参与推荐排序。
- 提醒的内容选择和调度入口有自动测试；操作系统省电、实际长期等待和通知触控不是本轮人工验收范围。文件选择器/第三方日历导入沿用未人工验收状态。
- 总体 12 包未完成。后续仍包括厨房条件/库存优先、主动日记和执行回顾、探索式详情、既有占卜/测验深化、角色任务交接、游戏课程、首页与小组件、图片分享，以及大文件/性能/lint 和正式签名。
- 验证计数、产物与真实设备边界见 `docs/superpowers/handoffs/2026-10-19-pantry-and-weekly-drafts-handoff.md`；不把下面历史缺口继续当作本轮当前状态。

## 2026-10-18 当前深化切片与剩余范围

- 已实现：真实过滤候选驱动的七日餐单组合、锁定与参考预算；时长/跨日区间冲突、空闲时段、有限重复、整体延期；选择性 `.ics` 导出；DailyLife v4、备份 v5 的旧版读取。
- 重复安排为有限批量生成的独立项，不是无限 recurrence；修改源条目不会改写其他已生成日期。餐单锁定与未保存预览仅在当前界面内有效，未实现可持久化周计划草稿；保存后各餐食按现有安排独立恢复。
- 七日组合是确定性贪心，不保证全局预算最优或营养完整；当前五行/星座评分只作为当前文化匹配，不为未来各日重算运势。菜谱用量仍无标准克数。
- `.ics` 导出器的范围、转义、折行与隐私边界有 JVM 测试；系统文件选择器与第三方日历实际导入未人工验收。无时区浮动时间跨时区导入应自行核对。
- 12 个工作包仍有未完成项：烹饪设备/难度/便当条件、结构化采购/库存期限、主动日记、探索式详情、现有占卜深化、测验草稿/引用同意、四角色任务交接深化、游戏课程、首页配置与小组件、分享图片及正式 Release 签名。对应清单保留未完成状态，不因自动门禁通过而标完成。
- 未做手机安装、人工视觉、大字号或 TalkBack 验收；未提交/推送。验证与当前产物见 `docs/superpowers/handoffs/2026-10-18-daily-companion-deepening-handoff.md`，下文是历史记录。

## 2026-10-16 日常扩充

- 源码新增库存/购物清单核对、1–20 份餐食与保存时目录预算参考、本周餐单及复制；同时间冲突须确认，延期保留资料，复制冲突不部分写入。
- 软标签支持编辑、保留期限或 7/30/90 天/无限期；过期仅留管理页。显式编辑和撤回不被推断重建；损坏数据保留至用户主动清除。
- 备份 v4 兼容 v1–v3；库存备注沿用备注分区，未选不导出、不覆盖本机原备注。旧安排默认 1 份，未知价格不补猜。
- 安排通知的完成与延后操作已接 WorkManager；重新核对档案、事项、待办和原时间，切档案/关闭提醒取消。系统调度、真实通知触控和实体设备仍须单独记录实际验收。
- `writeDebugApkManifest` 生成版本/HEAD/dirty/UTC/APK SHA256 清单并接 CI 上传；尚未推送，因此不能称远端 CI 已覆盖本轮。
- 本轮门禁和产物以 `docs/superpowers/handoffs/2026-10-16-everyday-expansion-handoff.md` 为准；下方旧计数是历史证据。库存没有结构化用量、菜谱没有标准重量，当前只标名称覆盖，不能声称自动计算剩余克数。

## 2026-10-15 剩余源码收口现状

- 后续会话清除修复与lint整理见`docs/superpowers/handoffs/2026-10-15-thread-safety-and-lint.md`，其验证和APK优先于下文历史产物。重开/清除统一reducer，不把token重置为零，保留其他角色和共享主动记忆。
- lint从84 warning/20 information降至75 warning/0 information，没有屏蔽或删除资源：参数/命名/装箱/冗余API分支已整理；剩余资源、依赖、图标、方向/SDK和容器引用按最新交接分类，不把所有warning称为已修复。

- 四角色真实交接、诗句/星图/象棋/丝路推进与存档、合成档案隔离、进程恢复已补真实应用回归；修复途中页覆盖目标角色和交接未展开目标抽屉。代表流程通过，不视为穷尽所有输入与棋局。
- 地区气候参考已覆盖24城：20个JMA测站、1个KNMI测站和3个NASA POWER网格参考点，同为1991–2020期；显示不同方法及空间限制，不把网格估计称作测站实测。全部城市和单城依据可展开，实例恢复有交互测试。预算/节奏缺少可比事实，仍不参与现实排序。
- 12 次暖缓存舞台测量和连续 PSS 采样通过；实际应用四角色/游戏/档案流程 55 个样本，观察最大 PSS 302197 KiB。记录 Debug/instrumentation 负载，不代表绝对峰值或 Release/实体低端机。
- 808次对话评估增加真实行动/画像上下文及具体内容断言；删除进程级角色线程map，关闭后只从可清除存储恢复，作用域不复用回归先失败后修复。局部状态/访问存储迁出`MysticGuideLocalState.kt`。
- 最新门禁、产物和复现见 `docs/superpowers/handoffs/2026-10-15-full-expansion-final.md`。实体机低端性能与远端CI未运行；无手机安装、提交或推送。下方保留历史阶段记录，不作为当前未完成项清单。

2026-10-15：已补真实应用“临时条件 → 第二个 → 为什么 → 进程恢复”，修复分钟校验误判、依据路由重置引用及候选依据缺失。508 JVM、九项契约、lint/构建通过；更多角色交接/档案/游戏组合及连续内存峰值、地区资料仍未闭环，不宣称全量完成。

## 2026-10-13 阶段剩余项（历史记录，现状见上方）

- 角色临时条件、引用对象、当前动作、依据展开与非象棋游戏入口已接入统一 session reducer 和快照 v2，按角色/档案持久化。2026-10-14 新增真实 MainActivity 输入/饮食联动、纳迪尔丝路推进及实际结束进程后的角色/抽屉/游戏恢复，独立包名隔离合成资料。组合指代/交接、其余游戏与多档案的完整应用验收仍需扩大，不能把这一条链路视为全部组合完成。
- 202 输入 × 4 角色真实评估已落地，但完整应用导航、临时条件/指代/交接组合的 UI 链路覆盖仍需扩大，不能用节点存在或模型计数代替。
- 备份 v2、暂存校验、原子 CAS 与中断恢复已落地；预览已按最终勾选显示新增、保留/合并、覆盖和遗漏数量。备注独立统计与确认，不影响未勾选的本机备注；还需扩大真实生产存储交互覆盖。
- 地区条件当前只显示用户明确选择和数据不足说明，不宣称已实现真实气候/预算匹配排名。象棋已有实际半步提示与复盘解释，但仍不是强制胜法证明或完整残局课程。
- 丝路已补九城坐标地图、多分支、完整路线比较、逐站确认与 v5 规划存档迁移；2026-10-14 已通过真实应用推进敦煌、持久化与进程恢复、返回浮球和再召回。全部分支及所有角色组合仍非完整应用覆盖。
- 启动诊断脚本限制到隔离模拟器，避免误操作手机。角色图片已有后台下采样和共享缓存；现已有四场景就绪/PSS 采样、四局真实搜索基线与协作式取消/旧结果阻断测试。连续内存峰值、实体低端机、重复采样中位数与整应用基准仍需深化。
- CI 已取消固定 AndroidTest 类名单并添加扩充契约、报告与 APK 哈希记录；远端 CI 没有推送触发，不能声称已跑通。
- 人工视觉、大字号、TalkBack 按用户要求排除；手机安装已获后续授权，覆盖安装/启动与完整设备功能验收仍分别记录。

当前 lint 0 error、84 warning：46 个 UnusedResources、16 个 GradleDependency、6 个 ModifierParameter、5 个 StaticFieldLeak，其余命名/图标/兼容性等 11 个。Context 静态持有优先核实是否均为 applicationContext；资源不能未经引用与视觉核实直接删除；依赖升级须独立验证。未对 warning 做全局压制。

隔离模拟器首页启动基线（Debug，2026-10-13）：925/1093/1051 ms，PSS 111190 KB。脚本拒绝实体机 serial 和零耗时/错误 Activity 测量；实际权限弹窗拦截后的零耗时已弃用。此基线不覆盖舞台图片内存峰值或象棋计算。

舞台/棋局新增基线：隔离 Debug 组件宿主中，四张解码素材合计 12 MiB（512×1024 三张、1024×1536 一张），不等于进程内存或峰值。首轮舞台就绪样本 123–207 ms；后续全套测试中的样本受预热与宿主负载影响，须按日志原值读取，不视为回归阈值。普通密集开局的单次搜索样本由 3831 ms / 872 节点变为 714 ms / 256 节点；这是限制部分分支后的结果，不是同搜索强度的提速保证。`MysticPerformanceBaselineTest` 记录真实规则校验、节点数、PSS 采样和缓存预热边界；日志为 `stage-perf-observations.log` / `stage-budget-perf-observations.log`，位于本轮基线目录。

本台账记录全量审计后仍值得继续处理、但不应阻塞离线版本交付的工程债务。现有未提交源码、截图、UI dump 和脚本均视为用户成果，本台账不要求清理它们。

## 当前状态（2026-10-11）

- 本机 Android 门禁通过：`:app:testDebugUnitTest` 441 项（0 失败、0 error、0 skipped）；`:app:lintDebug` 0 Error / 84 Warning / 16 Information；`:app:assembleDebug` 与 `:app:assembleDebugAndroidTest` 成功。
- 七项 Node 契约全部通过：authentic systems、dialogue（45 条既有 golden）、dialogue relevance（106 条输入 × 4 角色 = 424 次评估）、unified stage、action/profile、v14 shell、reading favorites。
- 专用 ADB server port 5038、隔离 API 35 AVD `XuanjiIsolatedApi35_20261011` 的 `connectedDebugAndroidTest` 45/45 通过（0 失败、0 error、0 skipped）。报告：`app/build/outputs/androidTest-results/connected/debug/TEST-XuanjiIsolatedApi35_20261011(AVD) - 15-_app-.xml`。设备只运行测试 APK；没有连接或安装到用户手机。
- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`（25,614,848 bytes，SHA-256 `BE0C13B6DF54199B1F29C81565F6E406D45B21A4A80823BDF49689CB11E91625`）；AndroidTest APK 1,149,458 bytes。
- 七项 Node 契约、Android 单测、lint、APK 构建和隔离模拟器 Compose 交互均为本机证据。工作区尚未提交/推送，因此无本轮远端 CI 运行记录；真机安装、人工视觉截图、TalkBack、大字号与减少动态效果验收未执行，也不以模拟器交互代替这些人工验收。

### 2026-10-11 可信与个性化收口

- 已实现出生资料完整度说明、按档案隔离的显式活动偏好、可选 AES-GCM/PBKDF2 选择性备份、周/月显式行动回顾，以及两个低风险文件拆分；算法不改分、缺项不猜、对话不作人格推断。
- 新增 JVM 与 Compose 交互覆盖。备份支持旧明文导入，但新导出默认加密；SHA-256 只作损坏检测，不能替代加密。
- 尚存工程债务：lint 的 84 项 warning 与 16 项 information、其他超大文件继续渐进拆分；真机和人工视觉/无障碍验收依旧待用户安排。

## 既有基线记录（滚动历史）

以下保留此前记录时的状态与证据；涉及 CI 覆盖、APK 和设备执行的旧描述不代表当前运行状态，以本节上方的最新验证及各自日期为准。

- Android 门禁：`testDebugUnitTest`、`lintDebug`、`assembleDebug` 已建立并在最近一轮通过；lint 当前无 error，剩余主要是既有 unused 参数/变量和 SDK XML 版本提示。
- CI（`.github/workflows/build.yml`）：此前 `Build Debug APK` 在 `eb4c3bb`/`887e1ee`/`7dc3b52` 连续失败且 Gradle 从未启动；`1dbe42f`、`457dc7a` 已修复 SDK 包名与 wrapper jar，run #17（`f89f2f1`）已在干净 runner 通过 `testDebugUnitTest` / `assembleDebug` / `lintDebug`。本轮新增 Node 20 步骤并在 Gradle 前执行 `node _dev/dialogue_contract_test.js`，使 45 条 golden wording 成为 CI 阻断门禁；本机最终树验证为 `dialogue contract: PASS (45 golden entries)`，但本轮改动尚未推送，因此远端尚未产生新的 CI run。Gradle 成功日志不打印逐条测试数，instrumented `app/src/androidTest`、真机与 DataStore 运行时行为仍不在 CI 覆盖范围内。
- Android 测试：纯 Kotlin domain/generator 测试已存在，覆盖对话分类、确定性、离线 provider、会话 token、生成器空输入与棋局规则 / 引擎 / 存档 / 解释事实，以及本机长期记忆的编解码与存储（`PreferenceBridge` 用内存假桥，不需 Robolectric 即可验证读写与清除）；`app/src/androidTest` 源码集已建立（棋盘 12 项 Compose 交互用例），能被 `assembleDebugAndroidTest` 编译，但**未在设备或 CI 上执行**。
- 小程序：结构 lint 与 7 项引擎/题库测试已分开执行并通过；双端契约位于 `_dev/dialogue_contract.json`。
- 今日行动与人生画像（2026-09-19）：`domain/action` 已落地五行 + 星座必需来源、固定权重、静态食物/城市目录、按档案隔离的饮食偏好和对话复用；`_dev/action_profile_contract_test.js` 锁定离线与同分排序边界。静态目录覆盖范围、设备窄屏展示和 TalkBack 仍需用户通知后复测，不把评分当成医疗、职业能力或迁居结论。
- 设备证据：曾完成 `com.xuanji.app` AVD 安装、启动、综合/东方/西方浮球、召回舞台和关闭回浮球截图，证据保存在 `.superpowers/round46-*`。没有把当前无在线设备误报为实体机验证。
- 同日生增强：`SameDayWorks` 已加入确定性音乐/诗歌卡，`HistoryCopy` 与 `AnimatedVisibility` 支持长评语折叠；后续可继续扩充经过版权核验的作品元数据。
- 文化人物视觉：四位独立来客已由 `MysticCharacterCatalog`、`MysticSceneCatalog`、`MysticCharacterSessionState` 和 `MysticCharacterGallery` 接入；`MysticStageLayout` 固定使用场景底板、遮罩、人物、顶部信息和底部抽屉五层结构，江南水榭、水墨案头、学院观测台、丝路驿站各有独立道具和光色，墨衡不再是透明人物例外。`MysticFigureCanvas` 保留资源异常回退，未知场景会回退到非空水墨静室。新增 JVM 场景契约与 Compose 舞台测试；真实设备比例、遮挡、滑动焦点、TalkBack 和不同密度仍需单独复测。
- 对话承接：`MysticDialogueContinuity` 已让省略式追问继承最近主题；后续应继续扩充中英文标点、连续 5 轮、换 persona/皮肤和跨端 golden wording。
- 深陪伴计划（2026-09-12）：实现入口见 [`docs/superpowers/plans/2026-09-12-deep-companion.md`](superpowers/plans/2026-09-12-deep-companion.md)，本阶段只治理现有陪伴体验，不新增占卜体系。默认离线；在线 Provider 仅保留显式扩展接缝，外部文本仍需本地事实/安全校验。设备、TalkBack、Logcat 和真实视觉复测等待用户通知，不能用 JVM/编译结果替代。
- 深陪伴实现（本分支）：已完成分析器、澄清入口、旧 token 丢弃、软标签撤回/清除、Provider 本地事实校验与离线回退；舞台拆为 `MysticStageLayout` / `MysticCultureBackdrop` / `MysticFigureAsset` / `MysticFigureCanvas`，交流面板拆为消息列表与输入状态，主题回答和文化语气表已从生成器抽出；棋局快捷入口已显式显示。最终门禁仍以本轮最后一次命令输出为准，设备、TalkBack、Logcat 和真实视觉复测等待用户通知。
- 四角色融入（本分支，2026-09-22）：新增稳定角色目录、专长来源映射、独立角色会话线程、角色切换交接语、旧 token 丢弃、全屏画廊、悬浮对话气泡折叠和角色棋盘主题。UI 不再把「玄学家 / 半仙」当人物身份；它们只保留为内部语气模式。`MysticCharacterGalleryTest` 补上四角色名称/文化标签、画廊可访问描述和「进入对话 / 查看专长 / 来一盘象棋」入口的 Compose 编译测试。真实设备滑动、TalkBack 播报、人物比例、旋转/返回与 reduced-motion 仍未执行。
- 统一角色舞台（本分支，2026-09-22）：`MysticSceneCatalog` 为四个 `sceneId` 提供可测试的标题、文化道具、调色板和可访问描述；`MysticStageLayout` 移除 `usesScenePlate` 的双路布局，四角色都通过同一五层场景/人物/抽屉结构。新增 `MysticStageLayoutTest` 覆盖四角色标题、场景语义、关闭入口、动作栏和窄屏存在性；只完成 JVM 与 AndroidTest 编译证据，未将设备视觉、TalkBack、Logcat 或截图复测冒充已通过。
- 棋局功能（2026-09-01）：四个切片已交付并通过门禁。
  1. 引擎与对话：`SmartBoardEngine`（alpha-beta，难度 2/3/4 层 + 开局库）成为默认应手，走子后自动串接引擎回包，新增难度切换、换色 / 观战、威胁扫描、残局目录、重做与和棋措辞。
  2. 棋盘 UI：难度选择、回放控件、落子滑动动画（系统动画时长为 0 时跳过）、吃子记录与 TalkBack 描述。
  3. 持久化：「保存棋局 / 继续棋局 / 战绩」经 `GameArchive` + `GameArchiveStore` 落 DataStore，恢复时逐手过规则校验并回报被丢弃的尾部手数，战绩按 session token 去重且只记已结算对局；存档不含角色评语。
  4. 交互测试：新增 `app/src/androidTest` 源码集与 Compose UI-Test 依赖，`GameBoardCardTest` 用坐标 `testTag` 驱动选子 / 落子 / 取消 / 难度 / 回放 / 思考锁 / 吃子记录 12 项用例；其前提由 `BoardUiFixtureTest` 在 JVM 上钉住，`_dev/dialogue_contract.json` 新增 `board_ui` 段交叉校验定位符与状态文案。
  证据：`:app:testDebugUnitTest` 201 项通过（其中棋局相关 153 项）、`:app:lintDebug` 0 error、`:app:assembleDebug` 与 `:app:assembleDebugAndroidTest` 均产出 APK、`node _dev/dialogue_contract_test.js` PASS（22 条 golden wording，含 UI 定位符交叉校验，改坏一处状态文案即失败）。**以上均为本机 JVM/编译证据，12 项棋盘交互用例未在设备执行。**
  Pikafish UCI 协议 parser 与显式降级 seam 已交付。

- 对话模板与 CI 契约（2026-09-08）：本轮完成一个可回滚切片。`.github/workflows/build.yml` 增加 Node 20 与 `_dev/dialogue_contract_test.js` 步骤；契约脚本现在同时扫描 `MysticGuideGenerator.kt` 与 `MysticDialogueTemplates.kt`，避免模板抽离后身份文案约束失效。新增纯 Kotlin `MysticDialogueTemplates.kt`，把 greeting/farewell/thanks/identity/smalltalk/daily/chat 模板和确定性 pulse hash 从生成器移出，`MysticGuideGenerator.kt` 从 3468 行降到 3191 行，`MysticDialogueTemplatesTest` 钉住 persona 标签和告别模板。用户输入决定的回复仍由生成器统一经过 `MysticSafetyGuard.enforce`，离线默认与 seed/hash 不变。证据：`node _dev/dialogue_contract_test.js` PASS（45 条）；`:app:testDebugUnitTest`、`:app:lintDebug`、`:app:assembleDebug` 同一轮本机执行均成功，lint 无 error（仅既有 warning）。尚未推送，远端 CI 尚未重跑；设备/UI/TalkBack 仍按要求暂缓。

- 棋局解释与陪伴记忆（2026-09-02）：两个切片已交付并通过门禁。
  5. 讲棋：`BoardExplanation` 用 `XiangqiRules.legalMoves` 做回吃判定（先把攻击方挪到目标格再问能否合法吃回），`GameDialogueBridge` 新增 `WHY`「这步为什么不好」与 `SAFER`「换个稳一点的走法」，威胁报告与走子后评注改为区分「有子能回吃」与「没人能吃回，属于白送」，难度只决定话量；契约新增 `explanation` 段，扫描措辞禁词并断言 `SmartBoardEngine.evaluate` 仍为 `private`。
  6. 本机长期记忆：`RecollectionCodec`（三种 `RecollectionKind`、20 条上限、`dropped` 计数、损坏即诚实降级）+ `PreferenceBridge` seam + `ConversationMemoryStore`（key `talk_memory_<sha256>`，UTF-8 摘要）+ `MysticGuideGenerator.recallLine` + `MysticGuideCard` 接线（按档案载入、8 处用户动作写入、现场手记面板列表与「清除本机长期记忆」按钮）。契约新增 `conversation_memory` 段，把种类枚举、上限、键前缀互斥、召回措辞与 48dp/TalkBack 全部对齐源码。
  证据：`:app:testDebugUnitTest` 264 项通过（0 失败 0 跳过）、`:app:lintDebug` 0 error（仅既有 `AutoboxingStateCreation` info 提示）、`:app:assembleDebug` 与 `:app:assembleDebugAndroidTest` 均成功、`node _dev/dialogue_contract_test.js` PASS（26 条 golden wording）。其中 `testDebugUnitTest` / `lintDebug` / `assembleDebug` 已在 `457dc7a` 的干净 Linux runner 上重跑通过（见上方 CI 条目），`assembleDebugAndroidTest` 与契约脚本仍只有本机证据。二者都只是 JVM/编译证据：DataStore 真机往返与跨进程存活、清除是否真的释放磁盘记录、召回句在气泡里的排布、清除按钮的实机触摸目标与 TalkBack 播报、困难档 `safest` 在低端机上的耗时，均未验证。

- 身份称谓与作风键（2026-09-02）：两个独立提交。
  7. 称谓收口：`MysticGuideGenerator.personaName(mode)` 成为双面角色对外称谓的唯一来源（scholar→「玄学家」、half→「半仙」），「玄师」退回为这套陪伴功能的统称（舞台关闭键、会话气泡、设置项标题），浮球舞台那套「慈翁 / 魔师」全部退役；`MysticOrb` 的漂移周期与幅度改为按 `half` 分支，不再比较显示字符串，`MysticImmersiveStage` 中从未被读取的 `roleName` 形参删除；`identityAnswer` 六条自我介绍改为每条只自称一次（原先「半仙，街口半仙」这类重复是把「魔师」换成「半仙」后才会显形，已并入作风名）。契约新增 `persona` 段：逐行扫描 `app/src/main/java/com/xuanji/app` 下的 `.kt` 断言退役名 0 命中（测试源码只在断言里把它们当反例列出，不在扫描范围内），并钉住 `personaName` 的实现、浮球按模式分支的写法、六条自我介绍与统称「玄师」。
  8. `styleKeyFor` 回归：该函数自 `f3ad6a8`（2026-08-24，「feat: add mystic opening check-in」）起返回 `style(...).second`，即作风**显示名**「档案室学者」而非作风**键** `archive`；下游一律按键匹配，因此有两处可见后果——`customAnswer` 里所有 `when (styleKey)` 永远落到 `else`（学者只会说「慢速罗盘」的话、半仙只会说「云端的实习生」的话，而卡面标题却按 `generate()` 的正确键显示「档案室学者」等六种作风），以及 `MysticGuideCard` 两处 `guide.styleKey == styleKeyFor(...)` 守卫永远为假、签到与节奏选择后的延后应答每次都被静默丢弃。已改为返回 `.first`。
  证据：`:app:testDebugUnitTest` 266 项通过（新增 `personaName_labels_the_two_modes_and_never_the_umbrella`、`identity_answers_name_the_persona_exactly_once`；后者遍历 5 组盘面 × 7 个话题 × 2 个模式，断言六种作风全部可达且每条身份回答只自称一次）、`:app:lintDebug` 0 error、`:app:assembleDebug` 重新产出 `app-debug.apk`、`node _dev/dialogue_contract_test.js` PASS。拆成两个提交后，只有单测（`--rerun` 强制重跑）与契约脚本在最终树上重新执行过，lint 与 APK 是同内容拆分前那次门禁的产物；`styleKeyFor` 那一笔没有在隔离状态下单独跑过测试——它相对最终树只少了称谓改名，而既有断言与契约都没有钉过受影响的那几句文案，因此判断为安全，但这是推理而非观测。`assembleDebugAndroidTest` 判定 UP-TO-DATE，但 `compileDebugAndroidTestKotlin` 已针对改动后的 main 源码重新编译通过——12 项棋盘交互用例仍未在任何设备或 CI 上执行。**以上均为本机 JVM / 静态扫描 / 编译证据**：实机上浮球与舞台的称谓渲染、TalkBack 实际播报、换装标签在窄屏是否截断，以及签到 / 节奏延后应答修复后是否真的出现在界面上，都未验证。
  同类残留已收口（见下方「当前基线」第 10 条）：`TodayOracle` 曾以显示名作查询键——`oracleRole` 返回「玄学家 / 半仙」，`when (role)`、`if (firstRole == "玄学家")` 与 12 条对照表都依赖这两个字面量。称谓改名不会像浮球动画那样静默走错分支（`when` 不匹配会落到 `else`），但那是「落进另一张嘴的口吻」，比走错分支更难发现，因此改为按稳定的 role 键分支。

- 医疗/财务红线与契约去空转（2026-09-02）：一个切片。
  9. `MysticSafetyGuard`（纯 Kotlin，无 Android 依赖）接在 `customAnswer` 的返回处：原先 `return when (intent) { … }` 改为先收成 `draft`，再由 `enforce(mode, question, variant, draft)` 决定整条回复。命中「领域词 + 结论词」成对出现时**整句换成拒答**（两个模式 × 两个领域 × 两个变体 = 8 句，各带本领域免责句），只命中领域词时保留原回答并在句尾补一次免责句、已有则不重复。变体由既有私有 `customPulse(question) % 2L` 选出，该 hash 始终非负，因此同一问题永远得到同一句。守卫自带词表而不复用 `MysticIntentClassifier`：分类器按主题分流，「我该吃什么药」因句中的「吃」落到 `Daily`（`MysticIntentClassifier.kt:61`），Health 的 needle 里从来没有「药」——红线不能押在路由运气上。成对命中也保证「最近睡眠不好」这类陈述不会被当成问诊。
  契约同时被去掉三处空转：`requireVerify` 以前只要求写出测试**文件名**且用 `.includes()` 比对方法名（截断的方法名前缀也能通过），现在强制 `Suite.case` 两级并用 `\bfun\s+case\b` 校验真实方法；10 处只写了 suite 的 verify 全部补成具体用例；`golden_wording` 从 26 扩到 37，补上此前完全无契约约束的 `继续` / `那工作怎么办` / `重开对话` / 纯空白输入 / `你是谁？` 与四条红线用例，`everyday_guards` 每条改为逐字钉住测试里的 `assertEquals(MysticIntent.X, classify("input"))` 断言。这一笔当时还多写了一句假话：把「超长输入」算进了新增项，而 37 条里只有纯空白那一条，超长路径一条都没有——该缺口在下一轮（第 10 条）补齐，现为 45 条。顺带修掉两处既有假话：契约原写「车厘子好吃吗 → chat」，实际分类器返回 `Daily`，`MysticDialogueGameIntentTest` 里那句 `assertTrue(cherry != MysticIntent.Game)` 已换成对 `Daily` 的确切断言，改坏任一侧都会失败。空输入与超长输入按用户决定**维持现状**（reducer 里 trim / 200 字符截断 / 纯空白丢弃），没有新增界面文案。
  证据：`:app:testDebugUnitTest --rerun` 278 项通过（0 失败 0 跳过，较上一轮 +12 = `MysticSafetyGuardTest` 11 项 + `MysticDialogueEngineTest.custom_answer_routes_every_question_through_the_safety_guard` 1 项）、`:app:lintDebug` 0 errors / 81 warnings（均为既有）、`:app:assembleDebug` 重新产出 `app-debug.apk`、`:app:assembleDebugAndroidTest` 的 `compileDebugAndroidTestKotlin` 针对改动后的 main 源码重新编译通过、`node _dev/dialogue_contract_test.js` PASS（37 条 golden）。五项都在最终树上跑过。为确认新断言不是空转做过两次反证：把 `customAnswer` 临时改回直接 `return when (intent)`，结果只有那一条接线用例失败（10 项中 1 failed），恢复后全绿；把一处 verify 的方法名截成前缀（`…_round_trip` 对应实际的 `…_round_trips`），旧脚本按 `.includes()` 放行、新脚本按预期报错。本切片已随分支推送到 `f89f2f1`，PR #1 的 run #17 覆盖了第 9、10 两条所在的树：`testDebugUnitTest` / `lintDebug` / `assembleDebug` 在干净 Linux runner 上全部真实执行且 job pass（`52 actionable tasks: 52 executed`，无缓存）。但 278 项这个计数与 `node _dev/dialogue_contract_test.js` 仍只有本机证据——CI 日志不打印逐条测试数，workflow 里也没有 node 步骤。**以上均为本机 JVM / 静态扫描 / 编译证据**：8 句拒答与免责声明在实机气泡里的换行、窄屏截断与 TalkBack 播报，以及两种作风下拒答语气的实际观感，均未验证（已列入契约 `safety.unverified`）。

- 今日灵签改按稳定键分支（2026-09-02）：一个切片。
  10. `TodayOracle` 不再用显示名当查询键：新增 `OracleTier { High, Mid, Low }` 与 `OracleRole(modeKey) { Scholar("scholar"), Half("half") }`，`oracleRole(draw)` 返回枚举，对外称谓取 `role.label = MysticGuideGenerator.personaName(modeKey)`，因此 `roleName` 仍是同一个字符串、界面取首字的头像和「某某离席」那句都不必改。`private fun oracleTier(String)` 删除，12 条签面在 `OraclePoem` 表上逐条声明自己的 tier，`TIER_BY_LEVEL` 由该表 `associate` 得出；`tier` 只做 `OracleResult` 的计算属性（getter，无 backing field）——这个对象经 `TodayOracleCache` 用 Gson 写进 DataStore，多一个构造字段会让当天已缓存的旧 JSON 反序列化出 null，Kotlin 默认值不生效，等于静默改掉用户正在看的那支签。`when (role)`、`when (role to choiceKey)`、三段 relay、两张 tier×role 表和 `if (firstRole == "玄学家")`（改为 `firstRole.other`）全部按枚举重排；四个公开函数签名与 `observerRelayGate` 的 7 个哈希输入未动，relay 的触发时机因此不变。
  行为差异要写清：旧 `oracleTier` 是 `else -> "low"`，任何没见过的 level 都静默当低签；现在 `TIER_BY_LEVEL.getValue(level)` 会抛 `NoSuchElementException`。全仓 `level` 只可能取签库里那 5 个值，所以唯一可达路径是「同一天内签库被换掉」这种构建期不一致——换来的是不再悄悄换成另一张嘴的口吻。回归矩阵此前缺的类别一并补上（37→45）：问候 / 感谢 / 告别 / 情绪各一条，超长输入两条（对话侧 200 字截断、棋局侧超长命令），换上下文两条（迟到的旧 token 回复整条作废、请求在飞时切 persona 回 Idle 并追加系统行）——上一轮那句把「超长输入」算进新增项的话由此变成事实。
  证据：`:app:testDebugUnitTest --rerun` 284 项通过（0 失败 0 跳过，较上一轮 +6 = `TodayOracleTest` 全量）、`:app:lintDebug` 0 errors / 81 warnings（均为既有）、`:app:assembleDebug` 重新产出 `app-debug.apk`（23,845,578 字节）、`:app:compileDebugAndroidTestKotlin` 针对改动后的 main 源码重新编译通过但 `packageDebugAndroidTest` 判定 UP-TO-DATE，磁盘上的 androidTest APK 仍是 01:07 那一份、`node _dev/dialogue_contract_test.js` PASS（45 条 golden）。四项都在最终树上跑过。文案不变做过机械核对：把两版文件里含标点的中文字面量取 multiset 比对，78 条完全一致，消失的只有 24×`"玄学家"`、14×`"半仙"`、7×`"high"`、7×`"mid"`、3×`"low"` 与 `oracleTier` 里的 3 个 level 名，新增只有 `"scholar"` 与 `"half"`。新门禁也做过反证：往 `TodayOracle.kt` 塞回一行 `private val PROBE_BANNED = "玄学家"`，脚本按预期以 `TodayOracle must never key a branch on "玄学家": …` 退出 1，删掉后恢复 PASS。路上真失败过两次，都记在这里：`the_cached_oracle_shape_is_unchanged` 首跑失败，因为 Compose 的 `@StabilityInferred` 会给 data class 加一个 `public static final int $stable`（javap flags 0x0019，并未标 ACC_SYNTHETIC），断言改成按 Gson 自己的口径过滤 static / transient；另有 `assertNull` 漏 import、以及 `Field.isTransient` 在本模块不能作为属性访问，改用 `Modifier.isTransient(...)`。**以上计数与静态扫描均为本机证据**；gradle 三项另在覆盖这一笔的干净 runner 上真实执行过一次（run #17，详见第 9 条同段），而 `_dev/dialogue_contract_test.js` 至今不在 CI 里。契约 `today_oracle.unverified` 记的仍是设备面：灵签页实际渲染、两种口吻在气泡里的排布与 TalkBack 播报，以及「同一天内签库变化」这个抛错窗口在真机上无从观察。

## 2026-10-03 运势阅读扩充状态

- 任务 1–6 已按 `docs/superpowers/plans/2026-10-02-xuanji-reading-experience-expansion.md` 分任务提交；当前第 6 项提交为 `9469e84`。角色对话只读本角色、本档案的有效游戏 checkpoint；游戏依据标记为本机存档，缺档不编进度，续局请求不会静默开始新局。完成摘要必须由用户主动收藏。
- 当前源码回归：六项 Node 契约、45 条 dialogue golden、游戏上下文/摘要收藏/进度意图 JVM 测试通过；`compileDebugAndroidTestKotlin` 通过。
- 设备边界：当前环境未配置 Android AVD；本轮未运行 `connectedDebugAndroidTest`，未连接、安装或操作用户手机。新增 Compose 交互用例只有编译证据，没有模拟器或真机运行时证据。

## P1：继续拆分大文件

| 文件 | 当前规模 | 已完成 | 下一步 |
| --- | ---: | --- | --- |
| `MysticGuideGenerator.kt` | 3124 行（2026-10-15） | 对话 seam、intent classifier、安全守卫、主题模板及文化语气 seam | 仍为大文件；继续按稳定边界拆分，不宣称已消除全部工程债 |
| `MysticGuideCard.kt` | 3067 行（2026-10-15） | provider/session、消息/输入/记忆控件及局部状态/访问存储独立；移除不可撤销的进程线程缓存 | 仍为大文件；后续继续缩小编排层，保持已验证存储与token边界 |
| `MysticFloatingGuide.kt` | 399 行（2026-10-15） | 浮球、舞台布局、背景与人物Canvas拆出，舞台检查点及交接接线 | 人工视觉/大字号/TalkBack按用户范围排除，不能把自动化当作人工验收 |

拆分规则：一次只移动一个稳定边界；不改变公共 API、资源 ID、角色 seed 或默认离线行为；每次移动后必须跑编译、单测、lint 和 debug assemble。

## P1：对话回归矩阵

Android保留45条golden，并扩充至202输入×4角色的真实引擎矩阵；新增行动方案/人生画像注入和具体回复内容断言。换角色、临时条件、指代、交接和四游戏由隔离真实应用流程补充，不视为所有输入的穷尽证明。小程序本轮未重新核对；Node只验证契约，不能代替JVM实际调用引擎。

红线的真实边界（本轮收口，见「当前基线」第 9 条）：`MysticGuideGenerator.customAnswer` 是唯一随用户输入变化的应答生产者，它的 `when (intent)` 结果现在统一过 `MysticSafetyGuard.enforce`（`MysticGuideGenerator.kt:2818`，全仓唯一调用点）；开场签到、本机召回句与过渡语是固定模板，不经这道关——它们本来也不回答用户的提问。因此「所有回复都过 safety guard」这句旧描述不准确，准确的表述是：**任何由用户文本决定的回复都必须过 `enforce`，模板句由各自的 golden 约束**。空输入与超长输入维持 reducer 现有行为（trim、200 字符截断、纯空白丢弃），不再假设界面上会出现新的提示文案。

交流面板已新增统一的消息/请求状态控件，后续只需继续扩充 golden wording，不再为每个入口维护独立的输入状态。

persona 命名漂移（已收口，见「当前基线」第 7 条）：同两个模式曾分别在 `MysticGuideCard.kt` 叫「玄学家 / 半仙」、在 `MysticFloatingGuide.kt` 叫「慈翁 / 魔师」，`MysticOrb.kt` 的 TalkBack 里还有第三种「玄师」，且浮球动画速度靠字符串比较 `"魔师"` 分支。现在称谓只有 `MysticGuideGenerator.personaName` 一个来源，「玄师」固定为统称，动画按模式分支，退役名由 `_dev/dialogue_contract_test.js` 逐行扫描 main 源码树（`app/src/main/java/com/xuanji/app`）把关。同源问题的最后一处 `TodayOracle` 也已收口（见第 10 条），契约 `today_oracle` 段现直接禁止这些写法：`TodayOracle.kt` 任何一行出现被引号包住的 `"玄学家"` / `"半仙"` / `"high"` / `"mid"` / `"low"`、重新出现 `private fun oracleTier(`、或给 `OracleResult` 加了第七个持久化字段以外的构造参数——任一情况都会让契约脚本失败。

## P2：可访问性与运动偏好

- 浮球保持 52dp 视觉尺寸和可点击语义；继续用 UI dump 或 TalkBack 实机检查焦点顺序。
- reduced-motion 已关闭浮球位移和持续旋转；后续检查完整舞台呼吸动画、键盘导航和旋转/返回键状态恢复。
- 复测 safe area：浮球不遮挡分数卡、底部导航和系统手势区。
- 本机长期记忆面板：召回句在气泡里的排布、「清除本机长期记忆」的焦点顺序与实机 48dp 命中区、清除后是否回到空态，均只有源码级证据；恢复设备验证时按 TalkBack 顺序复测，不以截图替代。
- 医疗/财务拒答与免责句：8 句拒答（两种作风 × 两个领域 × 两个变体）在气泡里的换行、窄屏是否截断、TalkBack 是否完整念出免责句，均只有 JVM 断言；恢复设备时逐句复测，抽查一句不能代替全部 8 句。
- 本轮手机复测暂缓，恢复时优先覆盖同日生折叠按钮的 TalkBack 标签、作品卡阅读顺序和舞台文化场景的对比度。

## P2：Provider seam

`DialogueProvider` 当前只提供离线实现。在线 provider 仅允许作为未来 seam：需要显式同意、超时、取消、失败回退和本地安全复核；本轮不接厂商、不写入密钥、不改变默认离线行为。

## P2：棋局引擎与扩展（真实状态）

- **Pikafish 原生包**：UCI 行协议 parser（`UciProtocolParser`）与降级 seam（`PikafishEngine` → `OfflineBoardEngine`）已完成并有 11 项协议测试；NDK/CMake、`arm64-v8a` 构建与引擎二进制**未打包**。接入前置条件：GPLv3 NOTICE、许可文本与 source offer 齐全（见 `NOTICE-THIRD-PARTY.md`），接入后 `bestmove` 仍须过 `XiangqiRules` 校验。
- **围棋 GTP/KataGo**：仅有 `GoSessionAdapter` 契约（无 provider 返回 `go_provider_not_enabled`）；未实现规则、未接引擎、未显示模拟棋盘。
- **国际象棋 Stockfish**：仅有 `ChessEngineAdapter` 契约（返回 `chess_provider_not_enabled`）与 FEN 起始局面值对象；未实现规则。
- **落子音效**：**未实现**。仓库无音频资源，未接 `SoundPool`，也不引入在线素材；需要设计确认音源与许可后再补。
- **棋子字形**：直接用系统字体渲染 `XiangqiPieceGlyphs.glyph` 的单个汉字，未捆绑字体资源，也没有缺字时的可视回退——`description` 只作为 TalkBack 内容描述，不参与绘制。若需保证跨机型一致，需要引入开源中文字体子集并核对许可。
- **引擎强度上限**：`SmartBoardEngine` 只有 alpha-beta + 简单子力/位置评估，无置换表、无静态搜索（quiescence）、无迭代加深，深度上限 4 层；horizon effect 与末端漏算未解决，不宣称任何等级分。
- **UI交互测试执行**：本机隔离模拟器已实际运行完整Debug交互集，三个受包名保护的恢复阶段另在`.regression`执行；最新计数与证据见顶部交接。不再只有编译证据；远端CI和人工焦点/视觉验收仍未执行。
- **解释的边界**：`safest` 只按「走完后己方被盯住的子数」挑一手，看一步、无静态搜索、候选数按难度封顶（6/14/40），低端机耗时未实测；不做开局名与战术术语表（无来源即编造），不显示任何强度数字。
- **记忆的边界**：本机长期记忆是 20 条滚动缓冲；角色线程已经支持按角色清除，隐私页可复制脱敏本地数据摘要，但长期记忆逐条撤回仍未做成独立按钮。同 kind+日期+文本会去重，因此同一天重复的终局结果只记一次——它不是第二个战绩板。
- **设备复测**：棋盘 UI（浮球关闭/召回不丢局、TalkBack、reduced-motion）尚无实机证据；恢复设备验证时需重新采集 `adb devices`、安装、启动、截图与 Logcat，不以旧截图替代。

## P1：真实语料与官方常模接入条件

- Ifá Ese 经文、纳迪叶脉原文：代码已提供 Provider、来源、许可证和置信度字段；在取得传承方/持有者授权与可追溯数据前，应用只能显示名称/索引或“离线模拟”。
- MMPI、Raven、16PF、MBTI：官方题目、计分键、手册和常模属于授权材料；当前仅开放 IPIP Big Five-50 可实际计分，未配置常模时不显示官方百分位。
- 农历：1900–2100 表驱动换算已内置并有闰月 golden case；若需要更早/更晚年份，应新增经核验历表版本，而不是外推。

## 2026-09-22 全量扩充切片

- **角色相关性**：`MysticCharacterVoiceAdapter` 已把 `characterId`、意图和专长映射接到 `DefaultMysticDialogueEngine`，四位角色对相同问题使用不同的解释入口，同时保留当前盘面分数与安全护栏。新增 4 组 JVM 回归用例；仍需把 30–50 条多语言真实问句扩到契约 JSON 并在小程序同步。
- **线程恢复**：`MysticSessionSnapshotCodec` + `MysticCharacterSessionStore` 以独立 key 保存四角色线程和共享用户手记，损坏/未来版本拒绝加载，pending 请求恢复 Idle。已覆盖 JVM 编解码、角色清除和全部清除；旋转、进程重启和 DataStore 真机往返仍待设备验收。
- **依据与隐私**：`MysticEvidenceBuilder` 生成算法版本、seed、体系、权重、状态、置信度和缺失输入；Profile 的“复制本地数据摘要”调用 `MysticPrivacyExportCodec`，不上传也不把生成内容当长期记忆。依据抽屉还未统一挂到所有运势结果卡，当前综合卡和对话 trace 是主要 seam。
- **角色游戏**：`CompanionGameCatalog` / `CompanionGameEngine` 已落地诗句接龙、星图观测、丝路路线三个确定性本地引擎，中国象棋复用原引擎；舞台动作栏按角色显示游戏名。新三类游戏的本地存档和 androidTest 尚未补齐，暂不宣称设备完成。
- **每日行动**：卡片现在展示具体食材、预计价格、准备分钟数，按钮的采纳/不合适/换一个会写入 `ActionFeedbackDataStore`；今日行动仍仅日周期，周/月/年不挂载。反馈尚未用于任何自动人格推断。
- **舞台**：角色画廊替换为 Compose `HorizontalPager`，保留窄屏、TalkBack 和 reduced-motion 语义；真机滑动、横屏和焦点顺序未验证。

## 完成定义

一项债务只有在有源码变更、对应测试和可复现命令输出时才标记完成。截图、旧日志或“代码看起来已接线”不能替代运行时证据。
