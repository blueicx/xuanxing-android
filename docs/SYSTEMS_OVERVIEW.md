# 玄星 X3 系统概览

## 2026-10-19 占卜学习与可复核数字

`DivinationLearning` 是纯观察/解释模块：塔罗仅统计实际抽到的牌、正逆位、重复花色和相邻位置，不生成额外预测；`TarotLearningPanel` 可主动保存牌阵原文/种子/规则版本到已有记录日历。日期在本次抽牌时固定，切换档案时清除旧牌阵，不把旧牌面记到新档案。

`RuneLearningPanel` 展示本次三处位置对照和可收起的 24 符目录，明确是现有目录的项目现代评论。主界面同时完整展示第二、第三符，不再只呈现第一符；主动随机抽签清除旧稳定结果，恢复今日符文同时清除两种抽取状态。稳定结果可主动保存到原始快照，心得由日记另行填写。

`Numerology.fullCalculationSteps` 与实际姓名五数共用同一推导路径；输入只允许 A–Z/空格/连字符/撇号，不对汉字、带变音字母或其他文字猜测转写，不再用 Unicode `isLetter` 减去 ASCII 偏移。A=1…Z=26，AEIOU 为元音、Y 为辅音；没有该类字母时为零并明确说明。基础日期各数位相加与完整生命路径的分组件归约都保持已有结果，页面分别公开步骤并注明大师数可能导致差异。没有新增常模、诊断或传统权威文献。

## 2026-10-19 推荐现实条件与主动记录

`FoodPreference` v2 的可用设备为空集合时只保留无需加热做法；null 是不限制。设备/难度/便当为硬过滤，替代菜和周餐单沿用同一候选池。库存优先只计算未过期、非零且同名的用户库存数量项，置于文化分数之外排序，不换算用量、不承诺食品安全。UI 与推荐仓库共享 `DailyLifeStore`，主动选择库存优先后，库存修订会刷新推荐。

`DailyJournalEntry` 仅含用户文本、主动选择的心情、实际执行和日期。`DailyJournalPanel` 在日历中编辑、保存、删除，按所选日期所在自然周/月统计记录天数、心情次数及安排完成数，不评价未记录日期、不作相关性或人格推断。最多 366 个日期；超限拒绝写入而不是删旧记录。

DailyLife v6 接受 v1–v6；备份 v8 接受 v1–v8，新增日记放入明确勾选的备注分区，同日期修改走冲突预览、确认和既有原子回滚。旧备份没有日记时保留本机日记；清除备注包含日记，也可单独清除主动日记；完整清除沿用整个档案存储删除。

`NumerologyReduction` 为项目的 `master-preserving-reduction-v2` 约定：保留输入或中间结果 11/22/33，记录拆位过程。个人年/月/日显示实际组件与归约步骤；这一规则差异明确标为文化约定，不宣称科学或唯一流派。

## 2026-10-19 采购、库存与餐单草稿

`DailyLifeCodec` v5 接受 v1–v5：`PantryItem` 新增手填 quantity/unit/expiresOn/category；`ShoppingPurchase` 以日期 + 规范化食材名隔离已买、采购数量和是否已入库；`WeeklyMenuDraft` 仅保存用户主动确认的起始日期、预算、重排序号与锁定 key。旧库存不从备注推断数量或期限，缺失数量保持未知。

`DailyLifeStore.transferPurchaseToPantry` 在同一互斥更新中检查已买状态、同名替换同意与字段合法性，再同时写库存和入库标记；重复确认不再加记。不同单位不换算、不相加，完成餐单不自动扣库存。采购和库存可分别清除，删除采购不倒扣库存；清除安排同时清除其采购/草稿，但不删除库存。所有数据仍按档案散列键隔离。

库存期限仅为手填记录：已超过目标日期或记录数量为零的库存不计购物“可用”标记；临期筛选按今天展示三天内或已超过记录期限的食材，不作食品安全判断。`ReminderPreferences.pantry` 默认 false，沿用明确的星期/时间、WorkManager 和通知权限；只选择库存提醒也可调度，数量为零或未填期限不加入该摘要。旧偏好自动保持关闭；备份不恢复提醒开关。

`PantryShoppingPanel` 从原日历组件拆出，保留食材输入 tag 与原购物来源文案；采购详情、已买 checkbox、入库确认、同名替换确认、库存编辑与期限筛选使用真实 Store。快速入库成功后清除输入焦点/键盘，避免焦点滚动重新遮挡已保存结果；保存失败保留输入。

餐单草稿需点击“保存草稿”，没有自动写入长期推断记忆。冷启动加载已保存条件，旋转保存未提交界面状态；当前候选重新过滤，旧锁定失效则提示并阻止直接加入。支持取消全部锁定、选择新起始日和清除草稿。保存的是条件而非未来盘面/整份固定推荐快照，所以跨日或偏好改变可能改变未锁定候选。

备份 v6 接受 v1–v6。安排分区明确包含库存、采购和草稿，预览分别计数并列出冲突；备注仍单独选择。恢复旧 v1–v5 时保留本机新增结构化库存元数据、采购和草稿。新版本恢复的数量/期限/草稿差异必须确认，写入失败沿用暂存、CAS 和日志回滚，测试覆盖新字段一起回滚。详情与证据以[当日交接](superpowers/handoffs/2026-10-19-pantry-and-weekly-drafts-handoff.md)为准；下节保留历史描述。

## 2026-10-18 七日餐单与安排深化

`DailyActionPlanner.mealPool` 与每日推荐共用完整过滤、实际评分、替代候选和反馈规则。`WeeklyMealPlanner` 以当前候选作离线贪心组合：保留锁定、降低重复、复用食材、核对每人整周目录预算。它不是全局最优求解器，也不把当前盘面当成未来七天预测。缺候选或锁定失效会明确显示，只有完整 21 餐才可从记录日历确认保存；不修改原有日/周/月/年运势页面。

`DailyLifeCodec` v4 接受 v1–v4；`durationMinutes=0` 表示未知，旧记录不补猜时长。已知时长使用左闭右开区间，能检测跨午夜的相邻日期；未知时长只作为起点参与检查，不虚构结束时间。空闲时段使用同一冲突规则，每 15 分钟检查 08:00–20:00。`AgendaPlanning.repeat` 生成 1–12 周的有限独立安排，不开启后台无限续排；确定性 ID 使重复确认不覆盖既有完成状态。

`DailyLifeStore.addAgendaBatch` / `moveAgendaDate` 在互斥区内整体校验、检查冲突并一次写入；容量不足、损坏或任一冲突不写部分结果。整体延期保留 ID、备注、份数与时长，只移动未完成项。备份 v5 新增时长，兼容 v1–v4，其余分区/备注/校验/回滚边界沿用。

`AgendaCalendarExport` 导出用户选择日期范围的安排，默认排除备注；没有出生信息、聊天、库存或运势快照。使用浮动本地时间、UTF-8 安全折行、CRLF 文本转义和不包含原始 ID 的 UID；未知时长不输出推测结束时间。无时间安排输出全天事件。文件经系统保存选择器保存，不请求系统日历权限，不建立双向同步、不写入提醒。

12 个工作包的整体完成状态以[当前清单](superpowers/plans/2026-10-18-daily-companion-deepening.md)为准，不将本节切片宣称为全部完成。

## 2026-10-16 日常安排与记忆管理

`DailyLifeCodec` v3 接受 v1–v3：旧安排份数补为 1，缺失预算保持未知，旧库存补为空。每条餐食保存 1–20 份及保存当时的每份目录预算参考，展示乘份数的粗略估算；配方没有标准重量，购物清单不推算克数。`PantryItem` 按食材名称与用户备注保存，购物清单保留菜名来源并标记库存已有。本周餐单由用户主动保存的安排派生，可复制未完成餐食到另一日期，保留份数/备注并生成新 ID；不从当前运势生成未来预测。

同日同一非空时间的未完成安排会触发冲突提示。新增、编辑和延期须显式允许重叠；餐单复制若遇冲突整次不写入。通知操作仅针对已显示的事项和档案，支持完成或约一小时后提醒；延后任务有唯一键、档案/事项/时间复核，关闭提醒及切档案取消延后通知。调度受系统限制，不能保证精确时间。

`SoftMemoryTagCodec` v2 兼容旧 v1 无期限标签；新推断标签默认 30 天，过期不在对话提示中展示，隐私管理页仍可查看、编辑、改为 7/30/90 天或无期限。仅修改文字时保留原期限；显式编辑标为用户确认，后续推断不覆盖确认或撤回记录。损坏文档阻止普通写入，清除需显式操作。

备份 v4 包括份数、保存时的目录估价与库存；v1–v3 仍可读，旧备份不会擦除已有库存。库存备注和安排备注遵循同一可选备注分区，未勾选不导出，导入未选时保留本机原备注。恢复继续使用已有校验、冲突预览和原子写入/回滚机制。

`:app:writeDebugApkManifest` 依赖 Debug 构建，生成同目录 `build-manifest.json`：包名、版本、HEAD、dirty 状态、记录时间、APK 大小与 SHA256。任务每次显式调用重新生成清单；HEAD 无法单独重现未提交工作区。CI 上传该文件。自动门禁与待安装产物以[当日交接](superpowers/handoffs/2026-10-16-everyday-expansion-handoff.md)的实际结果为准。

会话重开和单角色清除统一调用`MysticEvent.Clear`，递增token而非归零；取消临时pending工作并清除当前线程的持久化消息。共享主动记忆、其他角色线程及独立游戏存档不随单线程清除。2026-10-15后续证据见`docs/superpowers/handoffs/2026-10-15-thread-safety-and-lint.md`。

## 剩余源码收口

`OfflineRegionComparison` 覆盖24城：20个JMA测站、1个KNMI测站、3个NASA POWER/MERRA-2网格参考点，均为1991–2020期，明确标记不同方法及空间限制。12月简单平均的气候阈值比较不合入文化权重；预算/生活节奏缺少可比事实数据，不参与现实排序。来源与阈值边界见 `docs/data/JMA_CLIMATE_NORMALS.md`。`RegionPreferenceComparisonSection` 可展开全部城市与单城依据，保存实例状态按档案区分。

角色线程不再由进程级map跨界面持有；Compose负责当前作用域，关闭后从DataStore恢复，避免删除持久化数据后拾起旧内存线程。局部状态、访问存储及线程复原已迁至`MysticGuideLocalState.kt`。808次对话评估同时注入行动方案/画像，并断言具体内容与路由一致，不以非空文本代替相关性。角色Pager用settledPage接收稳定选择；显式交接展开目标抽屉。隔离应用回归覆盖四角色游戏/档案流程，采样器只读独立包的PSS；暖缓存组件与整应用观测分开。当前证据见`docs/superpowers/handoffs/2026-10-15-full-expansion-final.md`。

## 2026-10-15 当前候选解释

分数校验排除“分钟”时长，仍拦截不在实际盘面内的分数。解释路由保留用户选中的候选 ID，不重置至第一项；候选依据从实际计划重新读取，临时过滤与匹配依据分别展示。真实应用回归覆盖临时条件、序数追问、卡片/回复/依据一致、不自动写长期偏好及进程恢复；详情以 2026-10-15 交接为准。

## 2026-10-14 真实应用恢复回归

`regression` 构建类型继承 Debug，实现与正式应用相同，但包名为 `com.xuanji.app.regression`，拥有独立的数据目录。仅显式使用 `-PxuanjiRegression=true` 时 AndroidTest 才指向它。测试通过真实 MainActivity、导航、仓储、离线引擎和动作路由运行，不替换 Compose 页面或模拟回复。运行器先核实模拟器属性与 APK 清单包名，才安装、确认模拟器系统沉浸提示并终止隔离进程；两阶段检查实际 PID 改变及持久化恢复。正式包和手机不参与此回归。

真实页面回归暴露控制区超出抽屉且不可滚动，现按抽屉约束高度并允许滚动；新增输入框和行动卡稳定标签。CI 在现有模拟器任务中继续执行全部 Debug 测试，然后单独执行两阶段隔离恢复测试，不用固定类列表筛选普通测试。远端尚未触发。

## 2026-10-13 日常扩充实现边界

行动目录现为 64 道菜（四类各 16）、32 个活动、16 类环境、24 城市静态标签。目录内容为项目自编生活灵感；价格和准备时间是参考，不是实时商家或营养数据。先按明确约束过滤，再以五行/星座/当日盘面/季节或城市标签进行确定性排序；替代菜使用同一过滤集合，匹配贡献分与总分一致。今日情境支持在家、上班、周末、时间紧，不自动写入长期偏好。

`DailyLifeStore` 按档案保存安排、备注、原始结果快照和默认关闭的事项提醒。购物清单合并食材但保留菜品来源；历史快照读取保存时正文和算法版本，不重新计算。提醒采用 WorkManager，不保证精确时刻；关闭提醒会取消排队工作和现有通知。相关数据可从隐私页按功能清除。

学习目录有 60 个术语和 16 篇项目自编短说明，标签明确其简化规则和适用范围。本地搜索覆盖入口、学习和当前档案收藏，不索引私密聊天。对话临时条件只调整本次真实计划，主动点击保存才写入长期偏好；序数引用同时调整展示卡，不改变原始算法排序。

备份协议 v2 增加饮食/活动偏好、安排、可选备注和快照；仍排除出生资料和聊天。明文 SHA256 只校验完整性，不代表来源认证；加密文件另以 AES-GCM 认证。生产恢复通过暂存桥完整校验后以 DataStore 原子 CAS 提交，冲突须确认；顺序写入测试边界使用日志与回滚。超限拒绝，不静默截断。游戏进度 v5 保留旧五关诗句完成语义、固定任务 ID，并迁移旧丝路存档的空规划路线。

恢复预览按最终选择实时统计新增、相同记录合并保留、覆盖及本次未选项目；备注字段独立统计，不会因省略备注而误判安排冲突。收藏、反馈、安排、偏好与游戏等不同内容的覆盖须确认；未选分区不写入，相同游戏状态不重复询问覆盖。

`MysticInteractionState` 保存当前角色的临时条件、候选 ID、动作类型、依据展开和游戏入口，归约事件携带 session token。会话快照 v2 兼容 v1；跨日清理旧生活建议但保留当前游戏，重建卡片仍调用真实行动与画像数据。此为可恢复代码与自动测试，不替代完整应用级进程终止/导航验收。

舞台另有按档案隔离的 `MysticStageCheckpointStore`：同一阅读日期恢复角色、打开状态和各自抽屉；跨日、未知角色、损坏或未来格式不恢复。默认仍为浮球且舞台收起，只有已有用户操作检查点才恢复舞台。抽屉使用可保存实例状态；返回键先收起展开区，再关闭舞台；主动关闭把当前角色抽屉退回 Peek。系统保存实例重建和真实 DataStore 关闭后重新打开均有自动测试，但尚不等于整应用杀进程验收。

象棋计算通过 `BoardWorkRunner` 在后台运行，切角色/档案/关闭所属组件时取消；即使旧任务不响应取消，代次校验也禁止发布旧回复。提示继承调用者取消上下文，CPU 搜索逐节点检查取消。易/普通/困难深度上限保留 2/3/4 层，增加确定性节点上限 128/256/1024，不采用会随设备速度改变结果的时间预算。大棋树可能只搜索部分分支，界面明确标为有限搜索，不宣称必胜或强度评级；所有实际落子仍经真实规则校验。

丝路九城按 GeoNames 参考坐标投影，长安使用现代西安参考点。坐标采用 CC BY 4.0，底图和连接线为项目自绘游戏廊道，不是历史道路或现实导航。距离为城市参考点直线距离，天数和补给是独立游戏规则；三类完整方案先按当前资源筛出可行路线，逐站确认才结算，改道撤销旧规划，旧四站结算不变。地图支持缩放、拖动、复位、方向按钮、站点替代操作和全屏入口。

对话“加入今日安排”先打开当前真实候选的行动卡，由用户再确认时间和备注，不自动写入。象棋提示/复盘解释规则校验的实际半步、起终点、吃子与将军状态，不伪造必胜证明。角色素材在 IO 线程解码，最大边 1600 px、共享 24 MiB LRU 缓存，资源缺失仍用 Canvas 回退。

扩充仍在进行。地区现实条件匹配、完整应用导航和性能收口不得按已完成宣传；详见技术债及执行账本。[GeoNames 数据与授权](https://www.geonames.org/about.html)。

## 默认陪伴形态

综合、东方和西方运势页默认只显示 52dp 微光浮球（`OrbVisible`）。点击浮球才打开完整玄师舞台；关闭舞台后回到浮球。用户可在「我的 → 悬浮法师开关」关闭浮球，入口会保留用于恢复。

浮球和舞台都遵循系统导航栏/输入法安全区；系统开启“移除动画”时，浮球取消位移、旋转和面部微动。

浮球实现独立在 `MysticOrb.kt`；舞台外壳、文化背景、人物资源选择和 Canvas 回退分别由 `MysticStageLayout.kt`、`MysticCultureBackdrop.kt`、`MysticFigureAsset.kt`、`MysticFigureCanvas.kt` 承担，`MysticFloatingGuide.kt` 只保留挂载与状态桥接。

## 对话与会话

`MysticDialogueEngine` 负责输入分类和确定性本地回复，`MysticSessionState`/`reduce` 负责会话 token、上下文切换及旧异步结果丢弃。跨会话的「本机长期记忆」由 `RecollectionKind` 限定种类，只有用户原话、用户主动选择与已结算棋局结果三种，生成文案在类型上无处可放，因此「不把生成内容伪装成长期记忆」不再依赖调用方自觉（见「本机长期记忆」一节）。

意图规范化已抽到 `MysticIntentClassifier`，供 generator 与 engine 共用，避免两套关键词表继续漂移。

对话输入先经过 `MysticDialogueAnalyzer`：统一 NFKC、标点和大小写，给出主主题、次主题实体、置信度与上一轮承接。`MysticTopicAnswerTemplates` 只接收已经计算出的盘面事实，`MysticCultureVoice` 维护文化皮肤的动作/语汇；因此换作风不会重新计算分数，也不会把文化装饰冒充事实。

交流面板由 `MysticMessageList`、`MysticConversationInput`、`MysticClarifierRow` 和 `MysticSoftMemoryPanel` 组成。低置信度或多主题时最多给两个澄清入口，用户点选后仍走同一个 session token。软标签存于独立的 `soft_memory_<sha256(profileKey)>` 命名空间，显示“由对话推断”，可逐条撤回或全部清除，不进入 `RecollectionKind` 的长期访问记录。

同日生页的音乐/诗歌目录由 `SameDayWorks` 提供确定性结果：公版作品才允许显示短摘录，非公版只显示标题、作者、年份与风格元数据。长评语默认折叠为首句，用户主动展开后才显示全文。作品与人物内容均属于人文陪伴，不是占断证据。

对话承接由 `MysticDialogueContinuity` 读取最近回合的主题；“继续”“这个呢”“那怎么办”等省略式输入会继承上一主题，明确出现新主题时以当前输入为准。承接只改善表达相关性，不改变盘面算法，也不把生成内容写入长期记忆。

文化人物视觉现在由 `MysticCharacterCatalog` 与 `MysticSceneCatalog` 管理四位独立来客，而不是把角色当成服饰皮肤：沈砚舟（江南书生）、墨衡（灰发水墨老法师）、伊芙琳·诺瓦（学院星象师）和纳迪尔·拉希德（丝路占星师）。`MysticStageLayout` 固定使用场景底板、统一遮罩、人物层、顶部信息和底部陪伴抽屉五层结构；江南水榭、水墨案头、学院观测台、丝路驿站分别绘制月桥/灯笼、宣纸/墨砚、星图/档案桌、拱门/铜盘等文化道具，墨衡不再是透明背景例外。`MysticFigureAsset` 仍优先读取本地 `drawable-nodpi` WebP，`MysticFigureCanvas` 在资源缺失时回退到本地人物与水墨静室；未知场景 ID 也回退到非空水墨场景。它是文化视觉演绎，不宣称还原真实服饰、仪式或族群身份。

角色舞台由 `MysticStageLayout` 承载全屏 Horizontal Pager 画廊，`MysticCharacterGallery` 提供左右拖拽与 TalkBack 角色目标，统一的动作栏提供「进入对话 / 查看专长 / 进入当前角色游戏」。舞台有明确的 `Peek/Expanded` 抽屉状态：打开时只显示角色摘要和动作栏，明确操作后才显示消息、行动卡、依据和游戏；每位角色在当前舞台中保留自己的抽屉状态，旧 token 不能重新打开已切换的角色。角色专长只做已有计算体系的策展入口：`MysticCharacterSpecialty.systemIds` 指向实际系统，`sourceLabel` 与 `availability` 明示文化参考或未启用边界，不生成伪造经文、叶脉文本或心理测验常模。浮球显示当前角色首字与剪影，推荐由 `topicKey + dateKey + fortune` 稳定派生，用户手动选择后当天沿用。

视觉资源有显式 `MysticAssetRenderMode` 契约：沈砚舟、伊芙琳·诺瓦和纳迪尔·拉希德的 WebP 是完整场景图，直接铺满场景；墨衡的 WebP 是透明前景，只叠加水墨背景。`MysticVisualAssetCatalog` 负责声明这一区别，完整场景不会再收到第二层文化底板；资源缺失时统一回到 Canvas 场景与人物回退。

角色状态由 `MysticCharacterSessionState` 按角色保存独立消息线程，共享盘面事实与用户主动记忆；切换时追加交接语，并递增旧线程 token，使迟到的异步回复丢弃。`MysticDialogueEngine` 现在把 `characterId` 交给 `MysticCharacterVoiceAdapter`：它按意图选择角色专长、加入可辨识的解释语气，再回到本地安全校验，不改变盘面计算。「玄学家 / 半仙」只保留为内部语气模式，界面身份统一显示四位来客。

线程快照由 `MysticSessionSnapshotCodec` 和 `MysticCharacterSessionStore` 以 `mystic_character_sessions_<sha256(profileKey)>` 写入 DataStore。快照只恢复稳定消息、最近主题和用户手记，进行中的 provider 请求强制恢复为 `Idle`；支持当前角色清除、全部清除和“我的 → 隐私与数据 → 复制本地数据摘要”。

棋局仍是四位角色共用的真实中国象棋规则与会话引擎，`mysticGameThemePalette` 只替换木案、水墨宣纸、学院星盘、丝路铜盘四套视觉材质；沈砚舟、伊芙琳和纳迪尔分别提供自有短句接龙、星图连线逻辑题和离线丝路资源路线游戏。围棋和国际象棋继续显示 provider 未启用，不渲染假棋局。

## 四角色游戏与依据

`CompanionGameCatalog` 是角色与游戏的单一映射，`CompanionGameEngine<S,E,R>` 约束每个游戏使用独立 reducer、结果和重开路径。诗句、星图和路线都明确标为文化参考/逻辑游戏；中国象棋标为已启用并复用既有规则。`gameIdForInput` 与 `MysticCompanionActionRouter` 共用一组窄词快捷入口，输入「开始诗句接龙」「开始星图观测」或「开始丝路路线规划」会打开当前角色游戏；普通提问不会被误开。`CompanionGameCard` 只在当前角色的动作栏、快捷气泡或舞台按钮召回，不把游戏结果写成占卜结论。

`CompanionGameProgressStore` 使用 `companion_game_progress_<sha256(profileKey)>_<character>_<game>` 独立键保存三个逻辑小游戏的版本化状态；`CompanionGameProgressCodec` 对损坏、未知版本和错误角色安全降级。中国象棋继续由 `GameArchiveStore` 保存局面和棋谱，两套存档互不混用。对话输入经 `MysticCompanionActionRouter` 直接打开真实的今日饮食/行动/出游、人生画像、依据抽屉或游戏；缺少计算数据时保持对话回退，不生成空的具体结论。

角色问“还剩几天 / 多少补给 / 下一站怎么选 / 继续游戏”时，`MysticGuideCard` 只读取当前角色、当前 `visitProfile` 对应的存档：诗句、星图与丝路读取 `CompanionGameProgressStore`，象棋读取 `GameArchiveStore`。`MysticGameContextFactory` 将有效进度压缩为最多 240 字的本轮 checkpoint；它验证三款小游戏的路线/关卡状态，并通过 `GameArchive.restore` 复核象棋棋谱。完整游戏状态不会复制进对话线程或生成式长期记忆。路线问题列出当前存档实际相邻的站点与游戏内消耗；没有有效存档时诚实回退，“继续”不会暗中创建新局。

游戏问题的 `MysticEvidenceBuilder` 仅在存档有效时列出 `local_game_archive`，权重为 100，并将依据标为“本机存档”；不附带综合运势、角色专长算法或对应的运势分数。无有效存档时 confidence 为 0、体系与权重为空，并标明缺少本机存档。棋局、诗句、星图或丝路结束后，摘要只有在用户主动点“收藏本局摘要”时才写入 profile 隔离的收藏 store；保存的是简短游戏结果，不是角色生成文案或整个棋局 state。

`MysticEvidenceBuilder` 为对话和综合盘面生成依据 trace：输入摘要、体系、权重、算法版本、确定性 seed、计算/用户输入/文化参考/未启用标签、置信度和缺失输入。天气没有用户授权时会明确记录“未启用能力”，不会从盘面猜实时天气。依据层和 `MysticPrivacyExportCodec` 均可离线运行。

## 本地回顾、个性化与选择性备份（2026-10-11）

`LocalReviewTimelineRepository` 只合并当前 profile 的用户主动收藏快照与明确提交的行动反馈，并由 `LocalReviewTimeline` 按时间排序、筛选和去重；它不读取聊天线程、不自动把中途游戏进度列为回顾，也不推断人格或偏好。历史、同日生和收藏页保持原结构，近期选择只是额外分区。

近期回顾的 `LocalReviewTimeline.summarize` 按 ISO 周或自然月汇总收藏、采纳、不合适、换掉四类显式记录。周期只是用户可切换的本地视图，不根据比例生成偏好或人格结论。

`BirthProfileReadinessEvaluator` 仅检查出生日期、时间、地点、经纬度、性别五类资料是否已保存，并为每项说明用途；它不生成置信分、不改运势算法，也不补猜用户资料。`ActionConstraintStore` 以档案指纹隔离用户明确保存的活动精力、室内优先和最长时长偏好；这些约束用于今日活动候选过滤，与饮食偏好分开保存。没有用户选择时维持默认计划。

`LocalDataBackupCodec` 生成有版本、profile 指纹和 SHA-256 校验和的 JSON，大小上限 2 MB。用户逐项选择收藏、行动反馈、三款角色小游戏进度与一份中国象棋棋谱；出生资料、聊天、长期记忆和象棋战绩不进入文件。SHA-256 只能发现意外损坏，不是加密或身份认证。`LocalBackupPanel` 通过 Android 系统文档选择器读写；选文件后的预览只读，profile 不匹配、格式/校验错误会阻止恢复。收藏和反馈按 ID 合并；同角色同游戏或象棋存档冲突只有用户再次确认覆盖才写入。

加密包装 `EncryptedLocalBackupCodec` 使用随机 16 字节 salt、12 字节 nonce、PBKDF2-HMAC-SHA256（210,000 次）派生 256-bit 密钥，并由 AES-256-GCM 认证加密；容器限制为原备份上限的两倍，解密明文再经过既有版本、档案指纹和 SHA-256 校验。密码最少 12 个字符，仅在本次操作内存中使用，导出后清空界面状态，不写入偏好或日志。显式关闭加密才导出明文；旧明文格式继续支持导入，界面会提示其未加密。错误密码或损坏容器在预览阶段失败，不写入目标存储。

`MysticGuideModels.kt` 与 `MysticStageDecorations.kt` 分别承接从生成器和舞台卡片机械迁出的数据声明及展示组件；不改变生成算法、回复措辞或舞台结构，后续大文件拆分仍需小步进行。

角色游戏 codec 当前为版本 2，可读取版本 1 的有效进度并保留当前关卡/棋路。诗句接龙和星图观测提供固定关卡、提示、撤销、重开与完成摘要；丝路撤销按已走路线重新计算剩余天数和资源，并可查看逐段回放。三者都保持离线、确定性与游戏定位，不把关卡结果解释成占卜结论。

## 棋局会话（2026-09）

交流面板已接入真实中国象棋：`domain/game` 提供纯 Kotlin 规则核心（`XiangqiBoard`/`XiangqiRules`/`XiangqiNotation`）、会话级分析（`BoardAnalysis` 威胁扫描、`EndgameCatalog` 残局）、搜索引擎（`SmartBoardEngine`）、`GameSessionState`/`reduceGame` 会话 reducer 与 `GameDialogueBridge` 意图桥。快捷区现在直接提供「来一盘象棋」，游戏意图（`MysticIntent.Game`）优先于通用运势分类；游戏回复走独立卡片路径，不经过 `pendingCustom` 文本模板。角色棋局话术只引用 `BoardMove`/`RuleResult`/`GameOutcome` 中的事实，运势数据不参与棋局结论，反之亦然。Android 侧 `_dev/dialogue_contract.json` 由 `_dev/dialogue_contract_test.js` 直接与 Kotlin 源码交叉校验（事件枚举、错误码、判和措辞、存档字段、棋盘 UI 定位符与状态文案，以及 `explanation`、`conversation_memory`、`persona`、`safety`、`visual_companion` 四段），45 条 golden wording 每条都点名其验证用例，文档措辞与代码漂移会导致契约测试失败。

本阶段不新增占卜体系，也不把未授权的 Ifá、纳迪、心理测验常模或在线模型包装成已接入能力；先治理角色陪伴、回答相关性、可撤回本地记忆和内容边界。默认仍为离线回复，在线 Provider 只作为显式扩展接缝。

棋局异步纪律与会话一致：`GameEvent` 携带 token，token 不匹配即原样丢弃；悔棋一次回退一整回合并从初始局面重放恢复（含被吃子），重做沿 `redo` 列表逐手回放。默认引擎是 `SmartBoardEngine`（纯 Kotlin alpha-beta，难度对应 2/3/4 层搜索，红方开局走内置开局库），玩家走完后由 `Result.awaitEngine` 串接自动应手，「观战」模式下引擎走双方；`OfflineBoardEngine` 只保留为 `PikafishEngine` 的降级回退与测试接缝，`PikafishEngine` 的 UCI seam 已就绪但未打包原生引擎，所有请求显式回退。判和是会话级规则（三次重复局面 / 连续 60 个无吃子半回合，见 `GameSessionState.drawReason()`），`XiangqiRules.outcome` 本身不返回和棋。「保存棋局 / 继续棋局 / 战绩」由 `GameArchive`（FEN 起点 + UCI 棋谱）与 `GameArchiveStore` 落到 DataStore，恢复时逐手重放过规则校验、被拒的尾部手数显式回报；存档只含局面与棋谱，不含角色评语。围棋（GTP）与国际象棋（UCI）仅保留契约与 adapter，无 provider 时明确返回「尚未启用」。运行模式、指令清单、许可边界与排障见 `docs/BOARD_GAME_INTEGRATION.md`。

棋局解释（2026-09-02）：`BoardExplanation` 只从 `XiangqiRules` 取事实——哪个子被谁盯住、攻击方落子后己方能否合法回吃——角色据此说清「这步为什么不好」「换个稳一点的走法」，措辞里不含胜率、等级分或任何强度数字，`SmartBoardEngine.evaluate` 保持 `private` 由契约脚本守住。细节见 `docs/BOARD_GAME_INTEGRATION.md` §2.3。

## 本机长期记忆（2026-09-02）

- **存什么**：只存用户自己打的字（`user_input`）、用户在卡面上主动点过的选项（`user_choice`）、以及棋盘规则已判定结束的棋局结果（`settled_game_result`）。角色生成的评语、现场手记文本一律不进这条路径。
- **存哪里**：共享 `preferencesDataStore("xuanji_prefs")`，key 为 `talk_memory_<sha256(profileKey)>`（UTF-8 摘要），与 `game_save_` / `game_record_` / `mystic_visit_` / `card_layout_` / `user_profile` 互斥，换命盘读不到别人的记录。读写经 `PreferenceBridge` seam，因此 JVM 侧能用内存假桥验证存取与清除，而不只验证 JSON 编解码。
- **上限与诚实降级**：本机最多留 20 条，超出挤掉最旧一条并把数量计入随记忆一起落盘的 `dropped`；「从没存过」与「存了但读不出来」是两种状态，后者必须明说「本机记录读不出来，这次不引旧话」，不假装什么都没丢。
- **说什么**：召回文案的唯一输入是 `RecallFacts`（日期、话题键、终局结果、清理数、可读性），里面不含用户原话，所以角色只会说「你聊过「事业、财富」」而不会复述原句；开场句只陈述进来时本机已有的内容，本次访问新记的东西不改写它。
- **可见与可清**：「现场手记」面板新增「本机长期记忆」区，列出最近 3 条与总数、说明只存哪三类，并提供「清除本机长期记忆」按钮（≥48dp，带 TalkBack 描述）；清除只删自己那一个键并立即回空态。卡面上的「重开对话」与切换 persona 只清会话状态，不动长期记忆。
- **未验证**：真机上 DataStore 往返与跨进程存活、清除是否真的释放磁盘记录、召回句在气泡里的排布、按钮的实机触摸目标与播报，目前只有 JVM 与编译证据。

## 对话红线：不作医疗与投资建议（2026-09-02）

- **hook 在哪**：`MysticGuideGenerator.customAnswer` 是唯一随用户输入变化的应答生产者，它的 `when (intent)` 结果统一交给 `MysticSafetyGuard.enforce(mode, question, variant, draft)`（全仓唯一调用点）。开场签到、本机召回句、转场语是固定模板，不经这道关——它们本来也不回答用户的提问。
- **怎么判**：领域词与结论词**成对命中**（如「药」+「该不该/吃什么」）才整句换成拒答；只命中领域词（如「最近体检要注意什么」）保留原回答、句尾补一次免责句，已有则不重复。因此「最近睡眠不好」这类陈述不会被当成问诊。词表由守卫自带，不复用 `MysticIntentClassifier`：「我该吃什么药」在分类器里因句中的「吃」落到 `Daily`，红线不能押在路由运气上。
- **怎么说**：两个模式 × 两个领域 × 两个变体共 8 句拒答，变体由问题文本的 `customPulse` hash 选出，同一问题永远同一句。免责句沿用应用里已有的那两条（与 `domain/divination/IChing.kt` 同一句），不新造口径。
- **守住什么**：不诊断、不评药与剂量、不荐股、不承诺收益，也不换成一套「替代诊断」；`_dev/dialogue_contract_test.js` 逐字镜像 13 项禁词并断言守卫不含 Android 依赖、`enforce` 调用点仍在。
- **未验证**：8 句拒答与免责句在实机气泡里的换行、窄屏截断与 TalkBack 播报；语音转写出的医学/理财词是否与原话同域。

## Provider seam

`DialogueProvider` 与 `OfflineDialogueProvider` 只提供扩展接口；当前默认实现完全离线，不请求网络、不写入密钥，也不改变现有盘面、健康和财务边界。未来接入在线 provider 时，结果先由 `DialogueReplyValidator` 校验分数是否来自当前 `CompositeDailyFortune`、是否越过记忆/安全红线，再标记 `OnlineValidated`；失败、超时或校验拒绝统一回到 `OnlineFallback` 的本地生成器，不允许直接返回未校验文本。

## 占卜扩展与可复现输入

`domain/divination/DivinationQuery` 是塔罗、卢恩和数字命理的共同输入契约。`DeterministicDraw` 用 SHA-256 派生种子，组合算法版本、档案摘要、日期、问题和牌阵；同样输入得到同样结果，不保存原始姓名。塔罗由 `DeterministicTarot` 支持单张/三张/五张，卢恩由 `Rune.read` 支持单符/三符，`NumerologyCycles` 输出个人年/月/日周期与计算步骤。随机按钮仍保留为用户主动的另一种体验。`DivinationHistoryStore` 只保存用户主动确认的结果摘要，支持按档案删除。

## 天气、地点与联网边界

`domain/external` 提供 `OfflineContextProvider` 和可选的 `OpenMeteoContextProvider`。应用默认不联网、不读取 GPS；用户在「我的 → 隐私与数据」打开开关并手动输入城市后，才会发起一次地理编码或天气请求。请求失败、超时或关闭开关时回退到离线城市目录，不猜测实时天气。地图按钮只调用手机现有地图应用的 `geo:` 深链，不内嵌瓦片、不做后台定位。联网结果只用于显示天气/地点上下文，不能覆盖盘面、医疗或投资护栏。

对外接口约束：Open-Meteo 地理编码与预报接口分别要求城市名或经纬度；OSM/Nominatim 若未来作为地图 provider，必须遵守手动触发、限速、缓存、User-Agent、署名与不做自动补全等政策，不在本轮内置瓦片服务。

## 今日行动约束与反馈

`FoodPreference` 现在可保存过敏原、单餐预算与准备时长；`DailyActionPlanner` 在五行/星座/盘面/季节权重排序前先过滤这些硬约束，过滤为空时明确说明而不伪造菜名。`DailyActionCards` 展示具体食材、替代菜、时间和“换一个/已采纳/不合适”按钮；反馈是用户主动选择，不转化为隐式性格或健康推断。行动卡仍只出现在日周期。

## 安全响应护栏

`SafetyResponseGuard` 位于离线与可选 provider 文本之后：健康结论式问题转为安全拒答和专业帮助入口；投资结论式问题不输出买卖、仓位或收益承诺；人格只接受 `ExplicitAssessment`（用户主动完成并确认的测验），聊天内容一律为 `Unknown`。provider 结果先过 `DialogueReplyValidator`，不合格则回退到本地安全文案。

## 今日行动与人生画像

`domain/action` 是独立的纯 Kotlin 离线模块。`DailyActionPlanner` 先按 profileKey 的本地饮食偏好过滤候选，再用 0.40 五行、0.25 星座、0.20 今日盘面、0.15 季节/城市标签计算分数；`LifeProfilePlanner` 使用命盘和太阳星座作为必需来源，测试记录只作为显式增强输入。主结果不使用随机数，同分才用 `profileKey|dateKey|candidateKey` 稳定 hash 排序。

综合页的“今日行动”卡与对话引擎共享同一个 `DailyActionPlan`，我的页与对话共享同一个 `LifeProfile`。结果同时展示匹配分、来源链、置信度和边界说明。城市候选来自本地静态目录，最多显示 3 个匹配示例，不写“命中注定”“最适合移民”等绝对结论；食品建议不构成医疗、营养或过敏建议。

两端各有一份同名但不同职责的契约：小程序 `_dev/dialogue_contract.json` 是双端共享的对话契约（意图枚举、规范化、seed 组成、session token、安全边界）；Android `_dev/dialogue_contract.json` 早已不止棋局（事件、判和、存档、棋盘 UI、讲棋、本机记忆、称谓与医疗/财务红线），只随本仓库的 Kotlin 源码演进，两者不互为副本。

## 体系一致性分级

- **历法/天文近似**：八字节气经验表、紫微公历近似、JPL 开普勒根数、玛雅 GMT 584283、古典占星地点/昼夜盘。代码保留可复现的计算边界，但不宣称高精度星历或完整传统排盘。
- **传统简化演示**：易经（64 卦名、部分卦辞）、奇门、风水/瓦斯图、Ifá、纳迪、姓名学、灵签等。页面明确说明缺失的仪式、经典库、笔画或住宅输入，不把哈希/示例文案伪装成正统结果。
- **文化娱乐/自编问卷**：MBTI、Big Five、16PF、MMPI 风格、文字推理、趣味人格等。结果仅作自我探索，不具备标准化常模、临床诊断或能力鉴定效力。

本版本默认离线；算法一致性回归样例包括 2000-01-07 日柱、子初换日、2012-12-21 玛雅长纪历、昼夜幸运点、星盘时区以及 Ifá 十六主 Odu 唯一性。

剩余拆分、无障碍复测和 warning 分级记录在 `docs/TECHNICAL_DEBT.md`。

## 真实数据边界（2026-08）

- 紫微农历：`TableLunisolarCalendarProvider` 使用 1900–2100 年表驱动换算，闰月显式表示；超出表范围返回不支持，不静默猜测。旧公历入口仍保留为近似兼容路径。
- Ifá：16×16 共 256 个 Odu 组合已建立稳定索引（`outer * 16 + inner`）。Ese 经文正文必须来自受权传承语料，缺省 Provider 不提供伪造正文。
- 纳迪：真实叶脉文本必须由 `NadiCorpusProvider` 注入并携带来源、授权和置信度；默认 `OfflineNadiSimulationProvider` 明确标记为模拟。
- 心理测验：IPIP Big Five-50 使用公开题库和透明计分；MMPI、Raven、16PF、MBTI 题目/手册/官方常模不随应用内置，常模缺失时只展示原始分数。

核验入口：香港天文台公历/农历转换表（1901–2100）；IPIP 官方站点（题目与量表为公版、常模需自行说明）；联合国教科文组织 Ifá 传承说明（256 个 Odu 组合与口传 Ese 体系）。

## 本轮验证（2026-10-11）

本地七项 Node 契约通过；`:app:testDebugUnitTest` 441/441 通过；`:app:lintDebug` 0 Error、84 Warning、16 Information；`:app:assembleDebug` 与 `:app:assembleDebugAndroidTest` 成功。专用 ADB 5038 与隔离 API 35 模拟器上的 `connectedDebugAndroidTest` 45/45 通过，测试数据均为合成资料和内存存储。

本轮没有连接/安装用户手机，也未做人工视觉、TalkBack、大字号或减少动态效果验收。没有推送，因此不记录本轮 GitHub Actions 为通过。

## 历史验证边界（2026-10-03）

2026-10-03 本机完整门禁通过：`:app:testDebugUnitTest` 415 项通过（0 失败、0 error、0 skipped），`:app:lintDebug` 0 Error / 84 Warning / 16 Information，`:app:assembleDebug` 和 `:app:assembleDebugAndroidTest` 成功；六项 Node 契约全部通过。当前环境没有配置 AVD，因此 AndroidTest 仅编译/打包，未运行 Compose 交互；本轮也未连接或安装手机。GitHub Actions 已配置 API 34 的 `MysticDialogueEndToEndTest` 作业，但当前分支未推送，尚无覆盖这次提交的 CI 运行结果。
