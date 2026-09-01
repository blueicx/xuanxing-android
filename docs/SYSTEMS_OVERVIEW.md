# 玄星 X3 系统概览

## 默认陪伴形态

综合、东方和西方运势页默认只显示 52dp 微光浮球（`OrbVisible`）。点击浮球才打开完整玄师舞台；关闭舞台后回到浮球。用户可在「我的 → 悬浮玄师」关闭浮球，入口会保留用于恢复。

浮球和舞台都遵循系统导航栏/输入法安全区；系统开启“移除动画”时，浮球取消位移、旋转和面部微动。

浮球实现已独立在 `MysticOrb.kt`，舞台与人物绘制仍保留在 `MysticFloatingGuide.kt`，后续可继续按同一边界渐进拆分。

## 对话与会话

`MysticDialogueEngine` 负责输入分类和确定性本地回复，`MysticSessionState`/`reduce` 负责会话 token、上下文切换及旧异步结果丢弃。跨会话的「本机长期记忆」由 `RecollectionKind` 限定种类，只有用户原话、用户主动选择与已结算棋局结果三种，生成文案在类型上无处可放，因此「不把生成内容伪装成长期记忆」不再依赖调用方自觉（见「本机长期记忆」一节）。

意图规范化已抽到 `MysticIntentClassifier`，供 generator 与 engine 共用，避免两套关键词表继续漂移。

同日生页的音乐/诗歌目录由 `SameDayWorks` 提供确定性结果：公版作品才允许显示短摘录，非公版只显示标题、作者、年份与风格元数据。长评语默认折叠为首句，用户主动展开后才显示全文。作品与人物内容均属于人文陪伴，不是占断证据。

对话承接由 `MysticDialogueContinuity` 读取最近回合的主题；“继续”“这个呢”“那怎么办”等省略式输入会继承上一主题，明确出现新主题时以当前输入为准。承接只改善表达相关性，不改变盘面算法，也不把生成内容写入长期记忆。

B+C 视觉方案采用统一人物骨架加文化道具和场景层：每个 `skinId` 都映射到独立的 `MysticCultureSpec`，舞台会绘制对应的水榭、档案室、驿站、火塘、云台、城市夜景、沙海或节庆院落。它是文化视觉演绎，不宣称还原真实服饰、仪式或族群身份。

## 棋局会话（2026-09）

交流面板已接入真实中国象棋：`domain/game` 提供纯 Kotlin 规则核心（`XiangqiBoard`/`XiangqiRules`/`XiangqiNotation`）、会话级分析（`BoardAnalysis` 威胁扫描、`EndgameCatalog` 残局）、搜索引擎（`SmartBoardEngine`）、`GameSessionState`/`reduceGame` 会话 reducer 与 `GameDialogueBridge` 意图桥。游戏意图（`MysticIntent.Game`）优先于通用运势分类；游戏回复走独立卡片路径，不经过 `pendingCustom` 文本模板。角色棋局话术只引用 `BoardMove`/`RuleResult`/`GameOutcome` 中的事实，运势数据不参与棋局结论，反之亦然。Android 侧 `_dev/dialogue_contract.json` 由 `_dev/dialogue_contract_test.js` 直接与 Kotlin 源码交叉校验（事件枚举、错误码、判和措辞、存档字段、棋盘 UI 定位符与状态文案、`explanation` 与 `conversation_memory` 两段），文档措辞与代码漂移会导致契约测试失败。

棋局异步纪律与会话一致：`GameEvent` 携带 token，token 不匹配即原样丢弃；悔棋一次回退一整回合并从初始局面重放恢复（含被吃子），重做沿 `redo` 列表逐手回放。默认引擎是 `SmartBoardEngine`（纯 Kotlin alpha-beta，难度对应 2/3/4 层搜索，红方开局走内置开局库），玩家走完后由 `Result.awaitEngine` 串接自动应手，「观战」模式下引擎走双方；`OfflineBoardEngine` 只保留为 `PikafishEngine` 的降级回退与测试接缝，`PikafishEngine` 的 UCI seam 已就绪但未打包原生引擎，所有请求显式回退。判和是会话级规则（三次重复局面 / 连续 60 个无吃子半回合，见 `GameSessionState.drawReason()`），`XiangqiRules.outcome` 本身不返回和棋。「保存棋局 / 继续棋局 / 战绩」由 `GameArchive`（FEN 起点 + UCI 棋谱）与 `GameArchiveStore` 落到 DataStore，恢复时逐手重放过规则校验、被拒的尾部手数显式回报；存档只含局面与棋谱，不含角色评语。围棋（GTP）与国际象棋（UCI）仅保留契约与 adapter，无 provider 时明确返回「尚未启用」。运行模式、指令清单、许可边界与排障见 `docs/BOARD_GAME_INTEGRATION.md`。

棋局解释（2026-09-02）：`BoardExplanation` 只从 `XiangqiRules` 取事实——哪个子被谁盯住、攻击方落子后己方能否合法回吃——角色据此说清「这步为什么不好」「换个稳一点的走法」，措辞里不含胜率、等级分或任何强度数字，`SmartBoardEngine.evaluate` 保持 `private` 由契约脚本守住。细节见 `docs/BOARD_GAME_INTEGRATION.md` §2.3。

## 本机长期记忆（2026-09-02）

- **存什么**：只存用户自己打的字（`user_input`）、用户在卡面上主动点过的选项（`user_choice`）、以及棋盘规则已判定结束的棋局结果（`settled_game_result`）。角色生成的评语、现场手记文本一律不进这条路径。
- **存哪里**：共享 `preferencesDataStore("xuanji_prefs")`，key 为 `talk_memory_<sha256(profileKey)>`（UTF-8 摘要），与 `game_save_` / `game_record_` / `mystic_visit_` / `card_layout_` / `user_profile` 互斥，换命盘读不到别人的记录。读写经 `PreferenceBridge` seam，因此 JVM 侧能用内存假桥验证存取与清除，而不只验证 JSON 编解码。
- **上限与诚实降级**：本机最多留 20 条，超出挤掉最旧一条并把数量计入随记忆一起落盘的 `dropped`；「从没存过」与「存了但读不出来」是两种状态，后者必须明说「本机记录读不出来，这次不引旧话」，不假装什么都没丢。
- **说什么**：召回文案的唯一输入是 `RecallFacts`（日期、话题键、终局结果、清理数、可读性），里面不含用户原话，所以角色只会说「你聊过「事业、财富」」而不会复述原句；开场句只陈述进来时本机已有的内容，本次访问新记的东西不改写它。
- **可见与可清**：「现场手记」面板新增「本机长期记忆」区，列出最近 3 条与总数、说明只存哪三类，并提供「清除本机长期记忆」按钮（≥48dp，带 TalkBack 描述）；清除只删自己那一个键并立即回空态。卡面上的「重开对话」与切换 persona 只清会话状态，不动长期记忆。
- **未验证**：真机上 DataStore 往返与跨进程存活、清除是否真的释放磁盘记录、召回句在气泡里的排布、按钮的实机触摸目标与播报，目前只有 JVM 与编译证据。

## Provider seam

`DialogueProvider` 与 `OfflineDialogueProvider` 只提供扩展接口；当前默认实现完全离线，不请求网络、不写入密钥，也不改变现有盘面、健康和财务边界。未来接入在线 provider 时，结果仍需经过本地 persona/safety guard。

两端各有一份同名但不同职责的契约：小程序 `_dev/dialogue_contract.json` 是双端共享的对话契约（意图枚举、规范化、seed 组成、session token、安全边界）；Android `_dev/dialogue_contract.json` 是棋局契约（事件、判和、存档、golden wording），只随本仓库的 Kotlin 源码演进，两者不互为副本。

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
