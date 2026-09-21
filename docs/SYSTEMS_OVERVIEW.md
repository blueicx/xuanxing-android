# 玄星 X3 系统概览

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

文化人物视觉现在按 `MysticSkin.visualStyleId` 切换四套用户确认的人物：三联图左侧江南书生、单独截图的老玄学家、三联图中间学院星象学者、三联图右侧丝路沙海占星师。`MysticFigureAsset` 从本地 `drawable-nodpi` WebP 取图，`MysticFigureCanvas` 保留资源异常时的 Canvas 回退；`MysticCultureSpec` 仍负责水榭、档案室、驿站、云台和沙海等背景。它是文化视觉演绎，不宣称还原真实服饰、仪式或族群身份。

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

## 本轮验证边界

本轮已完成 Android 纯 Kotlin 测试、编译、lint 与 debug assemble 的代码级验证；手机复测按需求暂缓，后续需重新执行安装、启动、截图、旋转/返回键、TalkBack 与 reduced-motion 检查，不以旧截图替代当前版本证据。
