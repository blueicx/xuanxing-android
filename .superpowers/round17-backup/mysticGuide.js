// services/mysticGuide.js — 双面角色解读（对齐 Android MysticGuideGenerator.kt）
// 确定性输出：同命盘、同日期、同模式、同问题必然得到同一文案。
'use strict';

const TOPICS = [
  ['composite', '综合'],
  ['career', '事业'],
  ['love', '感情'],
  ['wealth', '财富'],
  ['study', '学习'],
  ['health', '健康'],
  ['test', '测试'],
];

function topicLabels() {
  return TOPICS.map(([key, label]) => ({ key, label }));
}

function topicLabel(key) {
  const item = TOPICS.find(([topicKey]) => topicKey === key);
  return item ? item[1] : '综合';
}

/** 同一天、同一命盘默认由同一位玄师陪伴；好运坏运不预设谁来接话。 */
function suggestedMode(topicKey, fortune) {
  return companionSeed(fortune) % 2 === 0 ? 'scholar' : 'half';
}

/** 作风仍按话题微调：同一个人换到不同话题，会有不同的讲解姿势。 */
function style(scholar, topicKey, fortune) {
  const index = Math.floor((presenceSeed(topicKey, fortune) / 7) % 3);
  if (scholar) {
    return [
      ['archive', '档案室学者', '翻页很轻 · 先核对再说话'],
      ['harbor', '灯下倾听者', '先接情绪 · 再看盘面'],
      ['compass', '慢速罗盘', '给方向 · 不催你出发'],
    ][index];
  }
  return [
    ['herald', '天庭司仪', '开场锣鼓 · 顺便阴阳'],
    ['alley', '街口半仙', '嘴硬心软 · 人间气'],
    ['intern', '云端的实习生', '法术不稳 · 态度很好'],
  ][index];
}

function presenceSeed(topicKey, fortune) {
  const source = `${canonicalDateKey(fortune.dateKey)}|${topicKey}|${fortune.overallScore}|${fortune.luckyNumber}`;
  let hash = 5381;
  for (let i = 0; i < source.length; i += 1) {
    hash = (hash * 33 + source.charCodeAt(i)) % 2147483647;
  }
  return hash;
}

function companionSeed(fortune) {
  const source = `${canonicalDateKey(fortune.dateKey)}|${fortune.overallScore}|${fortune.luckyNumber}`;
  let hash = 5381;
  for (let i = 0; i < source.length; i += 1) {
    hash = (hash * 33 + source.charCodeAt(i)) % 2147483647;
  }
  return hash;
}

function canonicalDateKey(value) {
  const parts = String(value || '').split('-');
  if (parts.length !== 3) return value;
  const year = Number(parts[0]);
  const month = Number(parts[1]);
  const day = Number(parts[2]);
  if (!Number.isFinite(year) || !Number.isFinite(month) || !Number.isFinite(day)) return value;
  return `${year}-${month}-${day}`;
}

function arrivalLine(scholar, score, seed) {
  let lines;
  if (scholar && score >= 65) {
    lines = [
      '我刚看完这页盘面。数不错，你可以少怀疑自己一点。',
      '路过看见这个分，先坐下来替你说一句：它值得高兴。',
      '今天这份势能是真的，不过别急着把它一天用完。',
    ];
  } else if (scholar && score < 45) {
    lines = [
      '我在旁边看了一会儿。先别骂自己，这只是提醒，不是结论。',
      '低分的意思是该收着走，不是你不行的证据。',
      '我把盘面又核了一遍。慢一点，先把最要紧的一件事照顾好。',
    ];
  } else if (scholar) {
    lines = [
      '我刚好经过，看了一眼。平稳也是一种能继续走的状态。',
      '不用逼它开花，今天的节奏适合把细节捋顺。',
      '我在这里陪你看一会儿，有疑问就慢慢问。',
    ];
  } else if (score >= 65) {
    lines = [
      '哟，这么体面？看来本半仙准备的符水要放凉了。',
      '行啊你，这排面都敢摆出来；别得意，本半仙还盯着呢。',
      '啧，好运用得挺熟练啊，记得留一点明天阴阳我。',
    ];
  } else if (score < 45) {
    lines = [
      '咳，这盘面有点害羞。笑什么，本半仙又不是来收你加班费的。',
      '别看我锣敲得响，今天只准你退三步，不准你认输。',
      '这信号确实闹脾气了；听半仙一句，先躺平回血再说。',
    ];
  } else {
    lines = [
      '本半仙掐指一算：不惊不喜，适合把琐事一个个收拾掉。',
      '温吞仙汤一碗，喝了不惊艳，但至少不会烫嘴。',
      '我路过闻了闻，今天没有大雷，也没有免费馅饼。',
    ];
  }
  return lines[Math.floor((seed / 11) % lines.length)];
}

function round(value) {
  return Math.round(value);
}

function contextualGames(scholar, fortune) {
  const high = fortune.dimensions.reduce((best, item) => (!best || item.score > best.score ? item : best), fortune.dimensions[0]);
  const low = fortune.dimensions.reduce((best, item) => (!best || item.score < best.score ? item : best), fortune.dimensions[0]);
  if (scholar) {
    return [
      {
        title: '强弱接力',
        description: `今天「${high.label}」 ${high.score} 分，「${low.label}」 ${low.score} 分。选一个衔接方式。`,
        options: [
          { label: '用强项带一带', feedback: `好。「${high.label}」的 ${high.score} 分不是拿来炫耀的，拿它给「${low.label}」开个头。` },
          { label: '先照顾弱项', feedback: `对，${low.score} 分只需要一个小动作；别逼它今天变成满分。` },
          { label: '只观察不改动', feedback: '可以，记录本身就是校准；明天的对照会更清楚。' },
        ],
      },
      {
        title: '开关实验',
        description: `幸运开关是「${fortune.luckyColor}」和「${fortune.luckyDirection}」。选一个轻量实验。`,
        options: [
          { label: '带上幸运色', feedback: '把它当作提醒，不是护身符：看到颜色就回到那件小事。' },
          { label: '顺吉利方向走走', feedback: '如果顺路就走一段；重点是换气，不是改命。' },
          { label: '定一个十分钟提醒', feedback: `十分钟后回看「${low.label}」，只问一句：现在能做的最小步是什么？` },
        ],
      },
    ];
  }
  return [
    {
      title: '仙家调配室',
      description: `「${high.label}」举着 ${high.score} 分，「${low.label}」只有 ${low.score} 分！选个调法。`,
      options: [
        { label: '抽高项借火力', feedback: `批准借用！把「${high.label}」的劲头挪一点去暖「${low.label}」。` },
        { label: '给低项加云朵棉', feedback: `安排！软处理不丢人，${low.score} 分也能慢慢爬坡。` },
        { label: '先盖休息章', feedback: '章已盖好！神仙也得充电，别拿硬撑当法术。' },
      ],
    },
    {
      title: '幸运快递·定制',
      description: `今日包裹按盘面打包：${fortune.luckyColor}、${fortune.luckyDirection}，签一样！`,
      options: [
        { label: `${fortune.luckyColor}便签`, feedback: '签收！上面写着：颜色只是开关，真正动手的还是你。' },
        { label: `${fortune.luckyDirection}绕路券`, feedback: '券已生效！顺路就绕一小段，不顺路就原地做小事。' },
        { label: `${high.label}试用装`, feedback: '发货啦！先用一小时，别贪多；用完写一句哪里顺手。' },
      ],
    },
  ];
}

function interaction(mode, topicKey, fortune, gameRound) {
  const scholar = mode !== 'half';
  const staticGames = scholar
    ? [
        {
          title: '六十秒校准',
          description: '先别改命盘，只给接下来一小时定一个方向。',
          options: [
            { label: '写下最重要的一件', feedback: '好，把它放在视线里；其他事先排队。' },
            { label: '把干扰挪远一点', feedback: '对，给注意力留一条干净的通道。' },
            { label: '做三分钟热身', feedback: '很好，启动比完美更能带走停滞感。' },
          ],
        },
        {
          title: '可控分拣',
          description: '把心里盘旋的事放进三只匣子。',
          options: [
            { label: '现在就能做', feedback: '这只匣子最轻，先从这里拿回掌控感。' },
            { label: '今晚再处理', feedback: '可以，给它一个具体时间就不算悬着。' },
            { label: '其实可以先放下', feedback: '承认不必做，也是一种很干净的整理。' },
          ],
        },
        {
          title: '最小一步',
          description: '为最需要照看的地方挑一件小事。',
          options: [
            { label: '十分钟整理', feedback: '十分钟后停下即可；小承诺更容易守住。' },
            { label: '说一句真话', feedback: '表达清楚需求，关系里的雾会散掉一些。' },
            { label: '先休息一下', feedback: '低电量时，休息不是偷懒，是校准。' },
          ],
        },
        {
          title: '两栏笔记',
          description: '把今天的事分成“我能做”和“我只能等”。',
          options: [
            { label: '我能做的一件', feedback: '很好，把它放到下一个二十五分钟里。' },
            { label: '只能等的一件', feedback: '写下来就够了；等待也可以被安放。' },
            { label: '先划掉一件', feedback: '少一件事，盘面会立刻清爽一点。' },
          ],
        },
        {
          title: '三分钟观察站',
          description: '选一个信号，接下来只观察它。',
          options: [
            { label: '呼吸的节奏', feedback: '先让身体成为参照物，答案会慢下来。' },
            { label: '最常打开的软件', feedback: '看清注意力的去向，不做批评，只做记录。' },
            { label: '今天的低电量时刻', feedback: '找到它，明天就能提前设一个休息点。' },
          ],
        },
      ]
    : [
        {
          title: '仙家三宝',
          description: '本半仙打开云柜，你只能摸一样！',
          options: [
            { label: '摸锦囊', feedback: '锦囊里没有天机，只有一句话：先把最难的事啃一小口。' },
            { label: '摇小铃铛', feedback: '叮！这是提醒信号，不是催命符；该问就去问。' },
            { label: '抱云朵枕', feedback: '抱紧了。软一点没关系，今天允许你边回血边推进。' },
          ],
        },
        {
          title: '天庭弹幕',
          description: '选一条弹幕挂在你头顶护体。',
          options: [
            { label: '稳住，能赢', feedback: '弹幕已置顶！但赢的定义由你来定，不用硬撑给别人看。' },
            { label: '退一步不丢人', feedback: '这条弹幕很贵。绕路不是输，是聪明的神仙都会用的导航。' },
            { label: '先吃口热的', feedback: '天庭认证！胃暖了，脑子的仙气才会通。' },
          ],
        },
        {
          title: '云朵点名',
          description: '点一位仙官来值班。',
          options: [
            { label: '财神候场', feedback: '他只管机会，不管冲动账单；小额清醒花，别让他打瞌睡。' },
            { label: '月老探头', feedback: '他递来的不是红线，是话筒：把想要什么说清楚。' },
            { label: '太白记笔记', feedback: '老头子写下四个字：少开五个头。专一比热闹灵光。' },
          ],
        },
        {
          title: '云上签筒',
          description: '抽一支不吓人的仙家提示！',
          options: [
            { label: '先做小事', feedback: '签文只有四个字：小事先行。别小看它，这招最灵。' },
            { label: '说清楚点', feedback: '上签！把需求讲明白，神仙都省得猜谜。' },
            { label: '歇一小会儿', feedback: '云朵盖章：休息不是偷懒，是给仙气充电。' },
          ],
        },
        {
          title: '仙界快递',
          description: '本半仙给你寄个包裹，选一个！',
          options: [
            { label: '一盒勇气', feedback: '已发货！用量说明：先用于那件拖了很久的小事。' },
            { label: '一条边界', feedback: '签收成功。今天可以对多余的任务说：仙鹤也要下班。' },
            { label: '十分钟安静', feedback: '包裹有点轻，效果不小；安静完记得回来。' },
          ],
        },
      ];

  const games = [...staticGames, ...contextualGames(scholar, fortune)];

  const source = `${canonicalDateKey(fortune.dateKey)}|${mode}|${topicKey}|${fortune.overallScore}|0`;
  let hash = 52711;
  for (let i = 0; i < source.length; i += 1) {
    hash = (hash * 37 + source.charCodeAt(i)) % 2147483647;
  }
  const safeRound = Math.max(0, gameRound || 0);
  const picked = (Math.floor(hash / 19) + safeRound) % games.length;
  return games[picked];
}

function interactionReaction(mode, styleKey) {
  const scholar = mode !== 'half';
  if (scholar) {
    if (styleKey === 'archive') return '我先把这项记在旁边；做法比说法更重要。';
    if (styleKey === 'harbor') return '好，你选的这一步我已经接住了。';
    return '这个方向够小，适合真的走一步。';
  }
  if (styleKey === 'herald') return '恭喜！这条选择已经敲锣送进云海！';
  if (styleKey === 'alley') return '行，就按这个来；咱不整虚的。';
  return '已登记工单！实习生保证不把它弄丢。';
}

function topicLabel(key) {
  return topicLabels().find((item) => item.key === key)?.label || '综合';
}

function thinkingLine(mode, styleKey, kind) {
  const scholar = mode !== 'half';
  if (scholar) {
    if (styleKey === 'archive') {
      return kind === 'game' ? '正在把你的选择抄进档案' : kind === 'handoff' ? '正在把上一页夹好' : '正在翻对应的那一页';
    }
    if (styleKey === 'harbor') {
      return kind === 'game' ? '正在看你选出的那一步' : kind === 'handoff' ? '正在给话题换个坐姿' : '先接住这句话';
    }
    return kind === 'game' ? '正在核对最小一步' : kind === 'handoff' ? '罗盘准备只转一格' : '指针正在慢慢对齐';
  }
  if (styleKey === 'herald') {
    return kind === 'game' ? '锣鼓小队正在验票' : kind === 'handoff' ? '换场锣鼓正在调音' : '天庭司仪正翻到那一页';
  }
  if (styleKey === 'alley') {
    return kind === 'game' ? '半仙正在给你递签' : kind === 'handoff' ? '大碗茶先挪个位置' : '街口半仙正在打听';
  }
  return kind === 'game' ? '云上工单正在登记' : kind === 'handoff' ? '云端工单正在改派' : '实习生法术加载中';
}

function handoffReaction(mode, styleKey) {
  const scholar = mode !== 'half';
  if (scholar) {
    if (styleKey === 'archive') return '旧线索已夹进书页，不会丢。';
    if (styleKey === 'harbor') return '刚才的话题先放在垫子上，随时可以回来。';
    return '方向记下了；现在只把指针转向新的一格。';
  }
  if (styleKey === 'herald') return '换场锣鼓已响，旧话题在后台候场！';
  if (styleKey === 'alley') return '行，大碗茶不撤；咱先聊新来的这摊。';
  return '改派成功！旧话题挂起，新工单置顶。';
}

function topicHandoff(mode, styleKey, fromTopicKey, toTopicKey) {
  const from = topicLabel(fromTopicKey);
  const to = topicLabel(toTopicKey);
  const scholar = mode !== 'half';
  if (scholar) {
    if (styleKey === 'archive') return `我把「${from}」那页先夹好，现在翻到「${to}」。两条线索可以互相参照。`;
    if (styleKey === 'harbor') return `「${from}」先放在旁边歇一会儿；我们轻轻转到「${to}」，不用把它关门外。`;
    return `「${from}」的方向已经记下。罗盘只转一格，先看「${to}」最清楚的位置。`;
  }
  if (styleKey === 'herald') return `换场！「${from}」先去后台候着，「${to}」带着盘面登台！`;
  if (styleKey === 'alley') return `换得挺快啊？行，「${from}」的茶还温着；咱先看看「${to}」这摊。`;
  return `工单已改派：「${from}」暂时挂起，「${to}」置顶。实习生保证旧话题不弄丢！`;
}

function customReaction(mode, styleKey, question) {
  const scholar = mode !== 'half';
  const variant = Number(customPulse(question) % 2n);
  if (scholar) {
    if (styleKey === 'archive') {
      return variant === 0 ? '我把这句话放在这一页旁边看。' : '先留住你的原话，再对照盘面。';
    }
    if (styleKey === 'harbor') {
      return variant === 0 ? '这句话我接住了；我们慢慢拆。' : '嗯，这里可以不用急着要答案。';
    }
    return variant === 0 ? '问题收到了；罗盘只按这一句转。' : '先把范围收窄，再看它指向哪里。';
  }
  if (styleKey === 'herald') {
    return variant === 0 ? '锣鼓轻一点，这句我听清了！' : '好胆量！当面问得这么直接！';
  }
  if (styleKey === 'alley') {
    return variant === 0 ? '哟，这话够直；大碗茶先放下。' : '行，咱不绕弯子，直接看这摊。';
  }
  return variant === 0 ? '工单已登记！实习生不弄丢这句。' : '收到收到！法术加载中，态度拉满！';
}

function customAnswer(mode, topicKey, question, fortune, test = null) {
  const label = topicLabel(topicKey);
  let focus;
  if (topicKey === 'test') {
    focus = fortune.dimensions.reduce((best, item) => (!best || item.score > best.score ? item : best), null);
  } else {
    focus = fortune.dimensions.find((item) => item.key === topicKey)
      || fortune.dimensions.find((item) => item.key === 'emotion' && topicKey === 'love')
      || fortune.dimensions[0];
  }
  const high = fortune.dimensions.reduce((best, item) => (!best || item.score > best.score ? item : best), null);
  const low = fortune.dimensions.reduce((best, item) => (!best || item.score < best.score ? item : best), null);
  const intent = customIntent(question);
  const scholar = mode !== 'half';
  const cleanCautions = String(fortune.cautions || '').replace(/\s*[\r\n]+\s*/g, ' ');

  if (intent === 'mood') {
    return scholar
      ? `今天综合 ${fortune.overallScore} 分，最需要照看的是「${low.label}」 ${low.score} 分。`
        + '先把睡眠、吃饭和一件最小的事安排好；情绪紧的时候，判断可以晚一点再做。'
      : `综合 ${fortune.overallScore} 分，「${low.label}」只有 ${low.score} 分，仙界都不催你现在硬撑！`
        + '先喝口热的、歇十分钟，再把最麻烦的事切成一小块。';
  }
  if (intent === 'love') {
    const dim = fortune.dimensions.find((item) => item.key === (question.includes('桃花') ? 'peach' : 'emotion')) || focus;
    return scholar
      ? `「${dim.label}」当前 ${dim.score} 分。比起猜结果，今天更适合把想说的一件事说清楚；`
        + '关系里的安全感来自具体表达，不是反复试探。'
      : `「${dim.label}」 ${dim.score} 分！别把话筒扔给对方猜，想要什么直接讲；`
        + '暧昧让神仙算账都费劲，直球省电！';
  }
  if (intent === 'wealth') {
    const dim = fortune.dimensions.find((item) => item.key === 'wealth') || focus;
    return scholar
      ? `「财富」 ${dim.score} 分。今天优先守住必要支出；若要尝试，金额小到失败也不影响生活。`
        + `幸运色「${fortune.luckyColor}」可以当作提醒自己冷静消费的开关。`
      : `财库信号 ${dim.score} 分！小额快乐可以投喂，大额冲动先冷冻三天；`
        + `往「${fortune.luckyDirection}」挪一挪，不如先打开记账本！`;
  }
  if (intent === 'career') {
    const dim = fortune.dimensions.find((item) => item.key === 'career') || focus;
    return scholar
      ? `「事业」 ${dim.score} 分。今天挑一件最重要的事推进；沟通时把需求、时间和需要的支持说清楚，`
        + '比同时开五个头更能建立可信度。'
      : `事业炉火 ${dim.score} 分！主打一招，别十八般武艺一起抡；`
        + '把关键话说漂亮，胜过加班到冒烟。';
  }
  if (intent === 'study') {
    const dim = fortune.dimensions.find((item) => item.key === 'study') || focus;
    return scholar
      ? `「学习」 ${dim.score} 分。把它切成二十五分钟的小段：先回顾一次，再处理最难的一块；`
        + '完成比完美更容易带走停滞感。'
      : `文昌香火 ${dim.score} 分！番茄钟启动，先把最烦的那块啃一小口；`
        + '成就感会自动续杯，别靠焦虑续命。';
  }
  if (intent === 'health') {
    const dim = fortune.dimensions.find((item) => item.key === 'health') || focus;
    return scholar
      ? `「健康」 ${dim.score} 分。优先睡眠、饮食和活动量；身体信号值得认真对待，`
        + '持续不舒服时请优先休息或寻求专业帮助。'
      : `健康炉温 ${dim.score} 分！早点躺、好好吃、动一动；`
        + '别和沙发签永久契约，真不舒服也别硬撑成苦瓜。';
  }
  if (intent === 'why') {
    return scholar
      ? `你问的这句落在「${label}」 ${focus.score} 分；全天综合 ${fortune.overallScore} 分。`
        + `最强是「${high.label}」 ${high.score}，最需照看是「${low.label}」 ${low.score}。这是现有盘面算法的参照，不是命运判决。`
      : `别急，本半仙把账摊开：「${label}」 ${focus.score} 分，综合 ${fortune.overallScore} 分！`
        + `「${high.label}」举火把，「${low.label}」坐轿子；数字来自既有算法，不是拍脑袋。`;
  }
  if (intent === 'care') return careAnswer(scholar, low.label, cleanCautions);
  if (intent === 'outcome') {
    return scholar
      ? `我不替未来盖章。当前能看见的是：「${high.label}」 ${high.score} 可用，「${low.label}」 ${low.score} 要照看。`
        + '把可控的一步做完，结果会比空等更清楚。'
      : '天机不打包票，打包票的都是卖符的！不过'
        + `「${high.label}」 ${high.score} 在线，「${low.label}」 ${low.score} 别硬闯；先做小事，再谈成不成。`;
  }
  if (intent === 'action') return actionAnswer(scholar, high.label, low.label, fortune.luckyColor, fortune.luckyDirection);
  return topicAnswer(
    scholar,
    topicKey,
    label,
    focus.score,
    high.label,
    low.label,
    test ? (test.name || test.testName || '最近测试') : ''
  );
}

function customIntent(question) {
  const q = String(question || '').trim().toLowerCase();
  const hit = (words) => words.some((word) => q.includes(word));
  if (hit(['焦虑', '压力', '害怕', '担心', '难过', '崩溃', '很累', '内耗'])) return 'mood';
  if (hit(['感情', '恋爱', '对象', '复合', '暗恋', '表白', '桃花', '分手', '他', '她'])) return 'love';
  if (hit(['财', '钱', '赚钱', '投资', '生意', '消费', '钱包'])) return 'wealth';
  if (hit(['工作', '上班', '事业', '老板', '同事', '面试', '升职', '跳槽'])) return 'career';
  if (hit(['学习', '考试', '复习', '作业', '论文', '背', '题'])) return 'study';
  if (hit(['健康', '身体', '睡觉', '睡眠', '失眠', '生病', '累'])) return 'health';
  if (hit(['为什么', '怎么来', '怎么算', '依据', '来源', '多少分'])) return 'why';
  if (hit(['留意', '注意', '风险', '小心', '避免', '坑'])) return 'care';
  if (hit(['能不能', '会不会', '可不可以', '行不行', '成不成', '该不该'])) return 'outcome';
  if (hit(['什么时候', '几点', '哪天', '现在适合', '今天适合'])) return 'action';
  if (hit(['怎么做', '怎么办', '如何', '建议', '行动', '开始', '计划', '破', '解'])) return 'action';
  return 'topic';
}

function customPulse(value) {
  let hash = 5381;
  const text = String(value || '').trim();
  for (let i = 0; i < text.length; i += 1) {
    hash = (hash * 33 + text.charCodeAt(i)) % 2147483647;
  }
  return hash;
}

function reaction(mode, action, askedCount, styleKey = '') {
  const scholar = mode !== 'half';
  const count = Math.max(1, Number(askedCount) || 1);
  if (scholar) {
    if (action === 'branch') return '先接住刚才那句；这条我们分开看，不急着混在一起。';
    if (action === 'repeat') {
      return count === 2
        ? '你又问了一遍。我猜不是没听懂，是这句话还没落进心里。'
        : '还在想这件事？那我们把入口再缩小一点。';
    }
  if (styleKey === 'archive') {
    if (count % 3 === 0) return '我把这一页又翻了一遍，你问到点子上了。';
    if (count % 3 === 1) return '这个问题我先归档；慢慢拆，不急着下结论。';
    return '嗯，档案里最稳的线索还是盘面。';
  }
  if (styleKey === 'harbor') {
    if (count % 3 === 0) return '好，这句话我听见了；我们把它的来路拆开。';
    if (count % 3 === 1) return '可以慢慢问，这里不用赶时间。';
    return '我先陪你把情绪放稳，再看数字怎么走。';
  }
  if (count % 3 === 0) return '这个问得好，罗盘可以先指一个小方向。';
  if (count % 3 === 1) return '我们只转一格，看看哪里最先清楚。';
  return '方向要能落地；我来帮你收窄一点。';
  }

  if (action === 'branch') return '喂喂，话题拐弯也要给云朵一点反应时间！';
  if (action === 'repeat') {
    return count === 2
      ? '又问？行，本半仙就喜欢你这份不死心。'
      : '还惦记着呢？好吧，仙界给你加播一次。';
  }
  if (styleKey === 'herald') {
    if (count % 3 === 0) return '锣鼓已响！这个问题有点锋利，本司仪先垫块云！';
    if (count % 3 === 1) return '好问题！开场词都替你想好了！';
    return '稍等，天庭司仪正在翻盘面！';
  }
  if (styleKey === 'alley') {
    if (count % 3 === 0) return '哟，这话够直接；本半仙先给你沏口大碗茶。';
    if (count % 3 === 1) return '好问题！街口消息灵通，但咱不吓人。';
    return '稍等，半仙正在跟云朵打听！';
  }
  if (count % 3 === 0) return '这个问题有点锋利，实习生小本本已掏出来！';
  if (count % 3 === 1) return '好问题！法术不稳，态度先拉满。';
  return '稍等，云端工单正在流转！';
}

function scholarHeadline(score, label) {
  if (score >= 80) return `${label}有势能，你可以安心接住`;
  if (score >= 65) return `${label}方向清楚，节奏可以温柔些`;
  if (score >= 50) return `${label}正在蓄力，不必逼它开花`;
  if (score >= 35) return `${label}需要小步确认，而不是大步证明`;
  return `先把${label}安顿好，再安排世界`;
}

function halfHeadline(score, label) {
  if (score >= 80) return `不得了！${label}直接踩着祥云起飞`;
  if (score >= 65) return `${label}火力在线，神仙都要侧目`;
  if (score >= 50) return `${label}稳如老君炉，别慌`;
  if (score >= 35) return `${label}有点闹脾气，得哄`;
  return ` ${label}暂时躲进云里充电了`;
}

function bandSentence(score) {
  if (score >= 80) return '现在的关键不是怀疑机会，而是把注意力放在能让你稳定发挥的选择上。';
  if (score >= 65) return '推进是合适的，只是把期待拆成几个可完成的小节点，会更轻松。';
  if (score >= 50) return '平稳不代表平淡，它给你空间整理节奏、修补细节。';
  if (score >= 35) return '低分不是否定，而是身体和情绪在提醒你收缩战线。';
  return '此刻最有效的行动是休息、求助和把任务缩小到不会吓跑自己的程度。';
}

function halfBandSentence(score) {
  if (score >= 80) return '这分数都快溢出八卦炉了，好运追着你跑，记得留个门！';
  if (score >= 65) return '运势小火苗烧得很旺，适合把计划端上桌，别让它干等！';
  if (score >= 50) return '不惊不喜，像一碗温吞仙汤，喝完照样能走路带风。';
  if (score >= 35) return '星星在天上挤眉弄眼：今天别硬闯，绕个路更灵光！';
  return '云层信号有点差，宜躺平回血，不宜跟命运掰手腕！';
}

function whyAnswer(scholar, label, focusScore, eastScore, westScore) {
  const gap = Math.abs(eastScore - westScore);
  const stronger = eastScore >= westScore ? '东方盘' : '西方盘';
  const weaker = eastScore >= westScore ? '西方盘' : '东方盘';
  if (scholar) {
    return `${label}的 ${focusScore} 分来自两侧交叉核对：东方 ${eastScore} 分，西方 ${westScore} 分。`
      + (gap >= 20
        ? `${stronger}更给力，${weaker}偏保守；不必硬选一边，先让稳的那边带路。`
        : '两边口径接近，说明这个判断比较稳，可以放心当作今天的参照。');
  }
  return `别看只是一个 ${focusScore}，背后可是东方 ${eastScore} 分、西方 ${westScore} 分在开会！`
    + (gap >= 20
      ? `${stronger}嗓门最大，${weaker}在旁边泼温水；先听强的，也别把弱的锁门外。`
      : '两边意见罕见一致，这信号可信度直接拉满！');
}

function actionAnswer(scholar, highLabel, lowLabel, luckyColor, direction) {
  if (scholar) {
    return `先给「${lowLabel}」十分钟的照看，再做一件能让「${highLabel}」落地的小事。`
      + `今天可用「${luckyColor}」和「${direction}」当状态开关：换颜色、调座位或出门方向，都是提醒自己切换节奏。`;
  }
  return `给「${lowLabel}」递杯仙气水，再让「${highLabel}」冲锋！`
    + `记得带上「${luckyColor}」，往「${direction}」挪一挪；这不是魔法命令，是给你换个心理档位。`;
}

function careAnswer(scholar, lowLabel, cautions) {
  const cleanCaution = String(cautions || '').trim() || '保持规律，别把日程塞太满';
  if (scholar) {
    return `盘面提醒的重点是「${lowLabel}」：${cleanCaution}。`
      + '这些是倾向描述，不是判决；如果状态持续不舒服，请优先休息或寻求专业帮助。';
  }
  return `天界小黑板写的是「${lowLabel}」：${cleanCaution}！`
    + '半仙只负责敲锣，不负责吓人；真不舒服就去休息，别硬撑成苦瓜。';
}

function topicAnswer(scholar, topicKey, label, focusScore, highLabel, lowLabel, testName) {
  const strong = focusScore >= 65;
  const mid = focusScore >= 35 && !strong;
  const opener = strong
    ? `「${label}」有空间`
    : mid ? `「${label}」适合小步走` : `「${label}」要先减负`;
  if (scholar) {
    const tail = {
      composite: `把注意力放在「${highLabel}」，同时给「${lowLabel}」留缓冲。`,
      career: '挑一件最重要的事推进，沟通时把需求说清楚，比同时开五个头更有力。',
      love: '少一点猜测，多一点具体表达；关系里的安全感的来源之一是把话说开。',
      wealth: '先守住必要支出，再考虑尝试；金额越小，决策越清醒。',
      study: '把目标切成二十五分钟的小段，先完成一次回顾，再谈突破。',
      health: '优先睡眠、饮食和活动量；身体信号值得被认真对待。',
      test: `可以把「${testName || '最近测试'}」当自我观察材料，与命盘互相参照，不单独下结论。`,
    }[topicKey] || `结合「${highLabel}」推进，同时照看「${lowLabel}」。`;
    return `${opener}。${tail}`;
  }
  const tail = {
    composite: `「${highLabel}」举火把，「${lowLabel}」坐轿子，路线已经很清楚啦！`,
    career: '主打一招，别十八般武艺同时抡；把关键话说漂亮，胜过加班到冒烟。',
    love: '直球可以扔，阴阳怪气快收起来；具体说想要什么，才不会被误会的云雾罩住。',
    wealth: '钱包系好绳，小额定投快乐可以，大额冲动先冷冻三天。',
    study: '番茄钟启动！先把最烦的那块啃一小口，成就感会自动续杯。',
    health: '仙体也要保养：早点躺，好好吃，动一动，别和沙发签订永久契约。',
    test: `「${testName || '最近测试'}」只是镜子，不是审判书；拿来认识自己刚刚好。`,
  }[topicKey] || `让「${highLabel}」打头阵，别把「${lowLabel}」丢在后山。`;
  return `${opener}！${tail}`;
}

function generate(mode, topicKey, chartFull, fortune, test = null, divinationSummary = null) {
  const label = topicLabel(topicKey);
  let focus;
  if (topicKey === 'test') {
    focus = fortune.dimensions.reduce((best, item) => (!best || item.score > best.score ? item : best), null);
  } else {
    focus = fortune.dimensions.find((item) => item.key === topicKey)
      || fortune.dimensions.find((item) => item.key === 'emotion' && topicKey === 'love')
      || fortune.dimensions[0];
  }

  const eastScore = {
    career: fortune.eastern.careerScore,
    wealth: fortune.eastern.wealthScore,
    health: fortune.eastern.healthScore,
    study: round((fortune.eastern.careerScore + fortune.eastern.overallScore) / 2),
    composite: fortune.eastern.overallScore,
  }[topicKey] || fortune.eastern.loveScore;
  const westScore = {
    career: fortune.western.careerScore,
    wealth: fortune.western.wealthScore,
    health: fortune.western.healthScore,
    study: round((fortune.western.careerScore + fortune.western.overallScore) / 2),
    composite: fortune.western.overallScore,
  }[topicKey] || fortune.western.loveScore;

  const high = fortune.dimensions.reduce((best, item) => (!best || item.score > best.score ? item : best), null);
  const low = fortune.dimensions.reduce((best, item) => (!best || item.score < best.score ? item : best), null);
  const facts = [
    `东方盘：${chartFull.dayMaster}（${chartFull.dayMasterWx}），${chartFull.strength.level}；今日${fortune.eastern.dayPillarText}，喜${chartFull.yongJi.use.join('、')}。`,
    `西方盘：太阳${fortune.western.sign}，今日整体 ${fortune.western.overallScore} 分。`,
    `综合盘：${fortune.overallScore} 分；最强是${high.label} ${high.score}，最需照看是${low.label} ${low.score}。`,
    `今日开关：幸运数字 ${fortune.luckyNumber}，幸运色${fortune.luckyColor}，吉利方位${fortune.luckyDirection}。`,
  ];
  if (divinationSummary && String(divinationSummary).trim()) facts.push(`占卜参照：${String(divinationSummary).trim()}`);
  if (test) {
    const resultCode = test.resultCode || test.title || '完成';
    const resultName = test.resultName || test.title || '完成';
    facts.push(`最近测试：${test.name} → ${resultCode}（${resultName}）`);
  }

  const scholar = mode !== 'half';
  const [styleKey, styleName, styleIntro] = style(scholar, topicKey, fortune);
  const arrival = arrivalLine(scholar, fortune.overallScore, presenceSeed(topicKey, fortune));
  const headline = scholar ? scholarHeadline(focus.score, label) : halfHeadline(focus.score, label);
  let body;
  if (scholar) {
    body = `我把「${label}」放回完整命盘看：综合 ${focus.score} 分，东方 ${eastScore} 分，西方 ${westScore} 分。`
      + bandSentence(focus.score)
      + (focus.score >= low.score && low.score < 55
        ? `真正想被照顾的是「${low.label}」，今天给它一个十分钟的小承诺就够了。`
        : '你不需要立刻变成另一个人，只要让已有的稳定继续发生。');
  } else {
    body = `天界吐槽频道已锁定「${label}」：综合 ${focus.score} 分，东方 ${eastScore} 分，西方 ${westScore} 分！`
      + halfBandSentence(focus.score)
      + (high.score >= 65
        ? `「${high.label}」简直在冒仙气，别端着了，赶紧去接住这波排面！`
        : '连半仙都看不下去啦，先别硬冲，留点力气明天封神！');
  }

  const followUps = [
    {
      key: 'why',
      question: '这个数怎么来？',
      answer: whyAnswer(scholar, label, focus.score, eastScore, westScore),
    },
    {
      key: 'action',
      question: '现在怎么做？',
      answer: actionAnswer(scholar, high.label, low.label, fortune.luckyColor, fortune.luckyDirection),
    },
    {
      key: 'care',
      question: '要留意什么？',
      answer: careAnswer(scholar, low.label, fortune.cautions),
    },
    {
      key: 'focus',
      question: `${label}怎么破？`,
      answer: topicAnswer(
        scholar,
        topicKey,
        label,
        focus.score,
        high.label,
        low.label,
        test ? (test.name || '最近测试') : ''
      ),
    },
  ];

  return {
    mode,
    topicKey,
    roleName: scholar ? '玄学家' : '半仙',
    styleKey,
    styleName,
    styleIntro,
    signature: scholar ? '只讲盘面依据 · 仅供娱乐参考' : '浮夸但讲逻辑 · 仅供娱乐参考',
    arrival,
    headline,
    body,
    evidence: facts,
    followUps,
  };
}

module.exports = {
  generate,
  suggestedMode,
  topicLabels,
  topicLabel,
  interaction,
  interactionReaction,
  handoffReaction,
  thinkingLine,
  topicHandoff,
  reaction,
  customAnswer,
  customReaction,
};
