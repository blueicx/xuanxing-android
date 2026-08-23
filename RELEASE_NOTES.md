# Release Notes

## 2026-08-23 · 今日吉时与双面角色 / Hourly Guides & Dual-Persona Readings

### 中文说明

- 新增「今日吉时」：东方页按五鼠遁推算十二时辰干支，结合日主五行生克、喜忌修正得到确定性分数；安卓提供可点击十二时辰盘，小程序提供时辰盘和详情卡。
- 新增「玄学家 / 半仙」双面解读：综合页可在两种角色间切换，并按综合、事业、感情、财富、学习、健康、最近测试七个入口查看；解读事实行同时引用东方盘、西方盘、综合维度、确定性今日灵签和本地测试记录。
- 「玄学家」基于现有东方盘、西方盘、综合维度做温和心理按摩；「半仙」用浮夸语气调侃同一组结果。两者都不使用随机数。
- 解读只引用已存在的命盘、运势和本地测试记录；未完成的占卜或测试不会被虚构引用。
- 同一命盘、同一天、同一模式与问题在安卓和小程序中保持核心数值与文案一致。小程序同步优化了角色卡动画、横向问题选择和高分/低分色彩反馈，并把今日吉时的阴影兼容到更多微信环境。

### English Notes

- Added "Today's Lucky Hours": the Eastern page derives twelve day-pillars with the Five Rats Escape method, then scores each hour from day-master element relations and favorable-element adjustments. Android uses an interactive circular clock; WeChat Mini Program uses a clock grid with detail cards.
- Added the dual-persona "Mystic Scholar / Half Immortal" reading on the Composite page. Users can switch personas and select Composite, Career, Love, Wealth, Study, Health, or Latest Test. Fact lines reference the Bazi chart, Western chart, composite dimensions, deterministic daily oracle, and local test records.
- The Mystic Scholar offers supportive psychological framing based on existing Bazi, Western, and composite dimensions. The Half Immortal playfully exaggerates the same deterministic data. Neither persona uses random generation.
- Readings reference only existing charts, fortune results, and local test records. Missing divination or test activity is never invented.
- With identical profile, date, persona, and topic, Android and Mini Program keep core scores and wording aligned. The Mini Program also receives animated persona styling, horizontal topic selection, clearer score colors, and a compatibility-safe glow for lucky hours.

### Verification

- Android: `assembleDebug` passed.
- Mini Program: lint and all 7 test suites passed.
