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

const companionSessions = {};

function companionSessionKey(chartFull, fortune) {
  const pillars = Array.isArray(chartFull && chartFull.pillars) ? chartFull.pillars : [];
  return [
    canonicalDateKey(fortune.dateKey),
    fortune.overallScore,
    fortune.luckyNumber,
    pillars.map((pillar) => `${pillar.gan || ''}${pillar.zhi || ''}`).join(''),
  ].join('|');
}

function readCompanionSession(chartFull, fortune) {
  const key = companionSessionKey(chartFull, fortune);
  const stored = companionSessions[key];
  if (stored) return { ...stored, key };
  const topic = 'composite';
  return { mode: suggestedMode(topic, fortune), topic, key };
}

function rememberCompanionSession(chartFull, fortune, mode, topic) {
  const cleanMode = mode === 'half' ? 'half' : 'scholar';
  const cleanTopic = TOPICS.some(([key]) => key === topic) ? topic : 'composite';
  const key = companionSessionKey(chartFull, fortune);
  companionSessions[key] = { mode: cleanMode, topic: cleanTopic };
  return key;
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

function openingReaction(mode, styleKey) {
  const scholar = mode !== 'half';
  if (scholar) {
    if (styleKey === 'archive') return '好，这一句我先放进今天的档案。';
    if (styleKey === 'harbor') return '嗯，你选的门我看见了；慢慢说。';
    return '罗盘先停在这里，我们把这条线看清楚。';
  }
  if (styleKey === 'herald') return '开场签到收到！锣鼓先收半个音！';
  if (styleKey === 'alley') return '行，就从这个茬开聊；茶给你续上。';
  return '工单已接！实习生这就翻开对应页！';
}

function openingHighLow(fortune) {
  const dimensions = Array.isArray(fortune.dimensions) ? fortune.dimensions : [];
  const high = dimensions.reduce((best, item) => (!best || item.score > best.score ? item : best), null);
  const low = dimensions.reduce((best, item) => (!best || item.score < best.score ? item : best), null);
  return { high, low };
}

function openingPrompt(scholar, styleKey, topicKey) {
  const topic = topicLabel(topicKey);
  if (scholar) {
    if (styleKey === 'archive') {
      return {
        composite: '综合档案已摊平，先用哪一条做今天的书签？',
        career: '事业卷宗有一处折角，你想从哪里核对？',
        love: '感情这页写着具体的事；先翻哪一行？',
        wealth: '钱袋账册摆在右手边，先看哪一栏？',
        study: '学习笔记还留着一页空白，先补哪里？',
        health: '身体档案不催促人；先记录哪一项？',
      }[topicKey] || '最近测试只是材料；先取哪一面镜子？';
    }
    if (styleKey === 'harbor') {
      return {
        composite: `灯亮了，${topic}这件事想先从哪头靠岸？`,
        career: '事业的潮水不急着赶；你想先卸下哪一件？',
        love: '感情这片水面很安静；先说哪一句？',
        wealth: '钱袋的小船系着呢；先看哪里吃水？',
        study: '学习像整理行囊；先放下哪本书？',
        health: '身体也需要泊位；先让它歇在哪一处？',
      }[topicKey] || '测试结果不是判决；先坐下来照哪一面？';
    }
    return {
      composite: `${topic}盘面已经归位；罗盘先对哪一格？`,
      career: '事业方向有几条并排；先确认哪条小路？',
      love: '感情指针很轻；先停在哪个词上？',
      wealth: '钱袋路线可以慢走；先核哪一个路标？',
      study: '学习是一格一格推进；先转哪一页？',
      health: '身体坐标值得看清；先校准哪一项？',
    }[topicKey] || '测试材料已经编号；先读哪一段注脚？';
  }
  if (styleKey === 'herald') {
    return {
      composite: `锣鼓轻一点！${topic}开场签到，先递哪张名帖？`,
      career: '事业大幕拉开一条缝；先报哪个节目？',
      love: '感情舞台不打追光；先点哪盏小灯？',
      wealth: '钱袋账本已呈上来；先翻哪页奏折？',
      study: '文昌香火已备好；先点哪炷？',
      health: '仙体保养司就位；先验哪件行李？',
    }[topicKey] || '测试榜单暂不宣读；先挑哪面镜子？';
  }
  if (styleKey === 'alley') {
    return {
      composite: `大碗茶放好了；${topic}这摊先唠哪句？`,
      career: '班还是得上；先把哪件事摆上桌？',
      love: '感情这事不猜谜；先从哪句实话开始？',
      wealth: '钱包不用晒；先看哪个口子？',
      study: '书山有近道也有远路；先迈哪步？',
      health: '神仙也怕硬熬；先顾哪一块？',
    }[topicKey] || '测试单别吓自己；先拿哪面照照？';
  }
  return {
    composite: `云端签到页打开啦；${topic}先勾哪个框？`,
    career: '事业工单已建号；先处理哪条备注？',
    love: '感情信号稳定；先发送哪句草稿？',
    wealth: '钱包云账本同步中；先核对哪一笔？',
    study: '学习进度条不催人；先点亮哪格？',
    health: '仙体巡检开始啦；先贴哪张便签？',
  }[topicKey] || '测试报告已脱敏；先展开哪段摘要？';
}

function openingResponse(scholar, styleKey, topicKey, strength, label, score) {
  const topic = topicLabel(topicKey);
  const position = strength ? '较强' : '较需照看';
  if (scholar) {
    if (styleKey === 'archive') {
      return `${topic}档案里，「${label}」${position}（${score} 分）。先把它当作参照，不改结论，也不急着定义今天。`;
    }
    if (styleKey === 'harbor') {
      return `「${label}」现在${position}，${score} 分。我先把这句话放在桌上；它值得被慢慢说清。`;
    }
    return `${topic}的「${label}」${position}（${score} 分）。罗盘只标这个位置，下一步仍由你选。`;
  }
  if (styleKey === 'herald') {
    return `报——「${label}」${position}，${score} 分！这不是判决，只是今天${topic}的开场字幕！`;
  }
  if (styleKey === 'alley') {
    return `「${label}」${position}，${score} 分。咱把话摊开：它能当线索，不能替你过日子。`;
  }
  return `${topic}工单显示「${label}」${position}（${score} 分）。已登记！用法说明：观察优先，不吹法术。`;
}

function openingSwitchResponse(scholar, styleKey, topicKey, source, value) {
  const topic = topicLabel(topicKey);
  if (scholar) {
    if (styleKey === 'archive') return `${source}「${value}」可以夹进${topic}那一页；它适合当提醒，不适合当保证。`;
    if (styleKey === 'harbor') return `把${source}「${value}」放在顺手的地方；${topic}需要时，让它帮你想起歇一口气。`;
    return `${source}「${value}」先当作${topic}的小路标；走到哪儿、歇多久，都由你定。`;
  }
  if (styleKey === 'herald') return `${source}「${value}」已盖章！${topic}专用提醒送达，但它不包办结局！`;
  if (styleKey === 'alley') return `${source}「${value}」给你压在茶杯底下；${topic}忙起来时看一眼就行，别迷信。`;
  return `${source}「${value}」已写进${topic}便签！功能只有一条：提醒你回来照顾自己。`;
}

function openingCheckin(mode, topicKey, styleKey, fortune) {
  const { high: strong, low: weak } = openingHighLow(fortune);
  if (!strong || !weak) return null;
  const scholar = mode !== 'half';
  const useColor = Number(fortune.overallScore) % 2 === 0;
  const switchLabel = useColor ? fortune.luckyColor : fortune.luckyDirection;
  const switchSource = useColor ? '幸运色' : '吉利方位';

  return {
    prompt: openingPrompt(scholar, styleKey, topicKey),
    options: [
      {
        key: 'strength',
        label: scholar ? `从「${strong.label}」聊起` : `看看「${strong.label}」的排面`,
        response: openingResponse(scholar, styleKey, topicKey, true, strong.label, strong.score),
      },
      {
        key: 'pressure',
        label: scholar ? `先照看「${weak.label}」` : `给「${weak.label}」搭个梯子`,
        response: openingResponse(scholar, styleKey, topicKey, false, weak.label, weak.score),
      },
      {
        key: 'switch',
        label: scholar
          ? `用${switchSource}「${switchLabel}」提醒自己`
          : `领「${switchLabel}」${switchSource}便签`,
        response: openingSwitchResponse(scholar, styleKey, topicKey, switchSource, switchLabel),
      },
    ],
  };
}

function cleanCaution(fortune) {
  return String((fortune && fortune.cautions) || '').trim().replace(/\s+/g, ' ');
}

function rhythmPrompt(scholar, styleKey, topicKey) {
  const topic = topicLabel(topicKey);
  if (scholar) {
    if (styleKey === 'archive') return `${topic}档案旁多了一栏状态；今天你选哪一档？`;
    if (styleKey === 'harbor') return `灯先留着；${topic}之外，你的节奏是哪一种？`;
    return '盘面归位了；先标一下你今天的速度。';
  }
  if (styleKey === 'herald') return '开场登记补充项！今天的节奏档位报一个！';
  if (styleKey === 'alley') return '先别急着上茶；今天你是稳、累还是赶？';
  return '云端表单新增一行：今日节奏选哪个？';
}

function steadyResponse(scholar, styleKey, topicKey, strong, source, value) {
  const topic = topicLabel(topicKey);
  if (scholar) {
    if (styleKey === 'archive') {
      return `${topic}档案收到「稳稳推进」。最强项是「${strong.label}」，${strong.score} 分；`
        + `${source}「${value}」可以当作提醒。`;
    }
    if (styleKey === 'harbor') {
      return `稳着来很好。「${strong.label}」现在有 ${strong.score} 分；`
        + `${source}「${value}」放在顺手处就好。`;
    }
    return `${topic}的速度标成稳档。「${strong.label}」 ${strong.score} 分，`
      + `${source}「${value}」只作小路标。`;
  }
  if (styleKey === 'herald') {
    return `稳速档批准！「${strong.label}」 ${strong.score} 分；`
      + `${source}「${value}」小旗已举起！`;
  }
  if (styleKey === 'alley') {
    return `行，稳住就行。「${strong.label}」有 ${strong.score} 分；`
      + `${source}「${value}」压在茶杯边。`;
  }
  return `云端备注：稳速推进。「${strong.label}」 ${strong.score} 分，`
    + `${source}「${value}」便签已贴好！`;
}

function rhythmCautionLine(scholar, caution) {
  return caution ? (scholar ? ` 盘面提醒：${caution}。` : ` 小黑板写着：${caution}！`) : '';
}

function tiredResponse(scholar, styleKey, topicKey, weak, caution) {
  const topic = topicLabel(topicKey);
  const cautionLine = rhythmCautionLine(scholar, caution);
  if (scholar) {
    if (styleKey === 'archive') {
      return `${topic}档案记下「有点累」。最需照看的是「${weak.label}」，${weak.score} 分。`
        + `${cautionLine}先做十分钟最小的一步。`;
    }
    if (styleKey === 'harbor') {
      return `累了就先承认这件事。「${weak.label}」 ${weak.score} 分。`
        + `${cautionLine}先做十分钟最小的一步。`;
    }
    return `速度降一档也没关系。「${weak.label}」 ${weak.score} 分。`
      + `${cautionLine}先做十分钟最小的一步。`;
  }
  if (styleKey === 'herald') {
    return `低电量档登记！「${weak.label}」只有 ${weak.score} 分。`
      + `${cautionLine}先回血，锣鼓调小声！`;
  }
  if (styleKey === 'alley') {
    return `累就直说，挺好。「${weak.label}」才 ${weak.score} 分。`
      + `${cautionLine}先回血，别硬扛！`;
  }
  return `云端状态：需要休息。「${weak.label}」 ${weak.score} 分。`
    + `${cautionLine}先回血十分钟！`;
}

function rushedResponse(scholar, styleKey, topicKey, caution, source, value) {
  const topic = topicLabel(topicKey);
  const cautionLine = rhythmCautionLine(scholar, caution);
  if (scholar) {
    if (styleKey === 'archive') {
      return `${topic}这一页被催出了折角。${cautionLine}清单先砍成一步；`
        + `${source}「${value}」用来换挡。`;
    }
    if (styleKey === 'harbor') {
      return `被赶着走时，先给自己留个泊位。${cautionLine}清单砍成一步；`
        + `${source}「${value}」帮你换气。`;
    }
    return `急速指针需要慢半拍。${cautionLine}把清单砍成一步；`
      + `${source}「${value}」当换挡提醒。`;
  }
  if (styleKey === 'herald') {
    return `急档收到！场务都别催了！${cautionLine}先刹车三分钟；`
      + `${source}「${value}」便签送上！`;
  }
  if (styleKey === 'alley') {
    return `谁把你催成这样？${cautionLine}先刹车三分钟；`
      + `${source}「${value}」压在清单上面。`;
  }
  return `云端提示：速度过载。${cautionLine}刹车三分钟；`
    + `${source}「${value}」便签已弹出！`;
}

function rhythmCheckin(mode, topicKey, styleKey, fortune) {
  if (!fortune) return null;
  const { high: strong, low: weak } = openingHighLow(fortune);
  if (!strong || !weak) return null;
  const scholar = mode !== 'half';
  const useColor = Number(fortune.overallScore) % 2 === 0;
  const value = useColor ? fortune.luckyColor : fortune.luckyDirection;
  const source = useColor ? '幸运色' : '吉利方位';
  const caution = cleanCaution(fortune);

  return {
    prompt: rhythmPrompt(scholar, styleKey, topicKey),
    options: [
      { key: 'steady', label: '稳稳推进', response: steadyResponse(scholar, styleKey, topicKey, strong, source, value) },
      { key: 'tired', label: '有点累', response: tiredResponse(scholar, styleKey, topicKey, weak, caution) },
      { key: 'rushed', label: '被赶着走', response: rushedResponse(scholar, styleKey, topicKey, caution, source, value) },
    ],
  };
}

function rhythmReaction(mode, styleKey) {
  const scholar = mode !== 'half';
  if (scholar) {
    if (styleKey === 'archive') return '好，节奏这一栏我先记下。';
    if (styleKey === 'harbor') return '嗯，你的节奏我听见了；不用赶。';
    return '速度先标在这里；路线仍由你调。';
  }
  if (styleKey === 'herald') return '节奏档位登记完毕！锣鼓跟着你收放！';
  if (styleKey === 'alley') return '行，今天按这个劲儿来；茶不催你。';
  return '表单提交成功！实习生帮你把节奏置顶！';
}

function rhythmCarryover(mode, styleKey, rhythmKey) {
  if (mode !== 'half') {
    if (styleKey === 'archive') {
      if (rhythmKey === 'steady') return '档案边角补了一笔：你今天选了稳速。';
      if (rhythmKey === 'tired') return '档案边角记着：你今天有点累。';
      if (rhythmKey === 'rushed') return '档案边角记着：你今天被催得紧。';
    }
    if (styleKey === 'harbor') {
      if (rhythmKey === 'steady') return '我记得你说今天还稳得住。';
      if (rhythmKey === 'tired') return '我记得你说今天有些累。';
      if (rhythmKey === 'rushed') return '我记得你说今天被人推着走。';
    }
    if (rhythmKey === 'steady') return '罗盘旁留了个标记：今天的速度是稳的。';
    if (rhythmKey === 'tired') return '罗盘旁留了个标记：今天要省一点力。';
    if (rhythmKey === 'rushed') return '罗盘旁留了个标记：今天的速度偏急。';
    return '';
  }
  if (styleKey === 'herald') {
    if (rhythmKey === 'steady') return '后台字幕已记：今日稳速前进！';
    if (rhythmKey === 'tired') return '后台字幕已记：今日电量偏低！';
    if (rhythmKey === 'rushed') return '后台字幕已记：今日场务别乱催！';
  }
  if (styleKey === 'alley') {
    if (rhythmKey === 'steady') return '你刚说今天还算稳，咱记着这茬。';
    if (rhythmKey === 'tired') return '你刚说今天累，咱不装没听见。';
    if (rhythmKey === 'rushed') return '你刚说今天赶，咱先把这事记下。';
  }
  if (rhythmKey === 'steady') return '工单备注：今日节奏稳定。';
  if (rhythmKey === 'tired') return '工单备注：今日需要省电。';
  if (rhythmKey === 'rushed') return '工单备注：今日外部催促较多。';
  return '';
}

function guestCameo(mode, topicKey, fortune, rhythmKey = '') {
  if (!fortune || !Array.isArray(fortune.dimensions) || !fortune.dimensions.length) return null;

  const scholar = mode !== 'half';
  const source = [
    canonicalDateKey(fortune.dateKey),
    'guest',
    mode,
    topicKey,
    fortune.overallScore,
    fortune.luckyNumber,
    String(rhythmKey || ''),
  ].join('|');
  let hash = 733;
  for (let i = 0; i < source.length; i += 1) {
    hash = (hash * 41 + source.charCodeAt(i)) % 2147483647;
  }
  if (hash % 5 !== 0) return null;

  const high = fortune.dimensions.reduce((best, item) => (!best || item.score > best.score ? item : best), null);
  const low = fortune.dimensions.reduce((best, item) => (!best || item.score < best.score ? item : best), null);
  const focus = topicKey === 'test'
    ? high
    : fortune.dimensions.find((item) => item.key === topicKey)
      || fortune.dimensions.find((item) => item.key === 'emotion' && topicKey === 'love')
      || fortune.dimensions[0];
  const score = Number(fortune.overallScore);
  const useColor = score % 2 === 0;
  const value = useColor ? fortune.luckyColor : fortune.luckyDirection;
  const sourceName = useColor ? '幸运色' : '吉利方位';
  let lines;

  if (scholar) {
    if (score >= 65) {
      lines = [
        `哟，「${focus.label}」 ${focus.score} 分？行，今天不用本半仙救场，我就在旁边看你得意。`,
        `好家伙，「${high.label}」 ${high.score} 都冒仙气了！先别谢天，明天记得也这么精神。`,
        `${sourceName}「${value}」都来捧场了；小心走太快，仙鹤也要看红绿灯。`,
      ];
    } else if (score < 45) {
      lines = [
        `咳，「${low.label}」 ${low.score} 是有点蔫；本半仙不吓你，先把饭吃热、觉睡够。`,
        `别硬撑，「${low.label}」 ${low.score} 只是让你收着走；留三分力气，明天还能翻云。`,
        `这页我看过了，不算完蛋。「${low.label}」要小步走，${sourceName}「${value}」就当个提醒。`,
      ];
    } else {
      lines = [
        `我探头看了看，「${focus.label}」 ${focus.score} 分；温吞也有温吞的走法，别自己吓自己。`,
        `这盘不惊不喜，「${low.label}」 ${low.score} 先照顾好；大戏改天再唱。`,
        `${sourceName}「${value}」路过递个提醒：今天把琐事收拾干净就够体面了。`,
      ];
    }
    return { roleName: '半仙', line: lines[Math.floor((hash / 13) % lines.length)] };
  }

  if (score >= 65) {
    lines = [
      `我从旁边瞄了一眼：「${focus.label}」 ${focus.score} 分，确实值得高兴；别把这份稳当成必须表演的戏。`,
      `路过替你记一笔：「${high.label}」 ${high.score} 在线。锣鼓可以听，别跟着把自己催热。`,
      `这盘面不算差。${sourceName}「${value}」只是提醒，你已经有能接住它的节奏。`,
    ];
  } else if (score < 45) {
    lines = [
      `我在旁边看了一会儿：「${low.label}」 ${low.score} 分只是提醒，不是给你定性的结论。`,
      `路过核对了一遍，「${low.label}」 ${low.score} 要收着照顾；先做最小的一件就好。`,
      `${sourceName}「${value}」可以当停顿记号；「${low.label}」需要休息，不需要责备。`,
    ];
  } else {
    lines = [
      `我看了一眼这页：「${focus.label}」 ${focus.score} 分，适合慢慢整理，不必逼它立刻开花。`,
      `路过留下一句：「${low.label}」 ${low.score} 分值得照看；小事做完就可以停下。`,
      `这盘面平稳。${sourceName}「${value}」当提醒就好，路线还是由你定。`,
    ];
  }
  return { roleName: '玄学家', line: lines[Math.floor((hash / 13) % lines.length)] };
}

function guestChoices() {
  return [
    { key: 'why', label: '问依据：这个判断从哪来？' },
    { key: 'accept', label: '接一句：我先收下提醒。' },
    { key: 'pushback', label: '拦一句：别俩人一起看我。' },
  ];
}

function guestReply(mode, topicKey, fortune, rhythmKey = '', choiceKey = 'why') {
  if (!fortune || !Array.isArray(fortune.dimensions) || !fortune.dimensions.length) return '';
  const choice = guestChoices().find((item) => item.key === choiceKey) || guestChoices()[0];

  const source = [
    canonicalDateKey(fortune.dateKey),
    'guest-answer',
    mode,
    topicKey,
    fortune.overallScore,
    fortune.luckyNumber,
    String(rhythmKey || ''),
    choice.key,
  ].join('|');
  let hash = 937;
  for (let i = 0; i < source.length; i += 1) {
    hash = (hash * 43 + source.charCodeAt(i)) % 2147483647;
  }

  const high = fortune.dimensions.reduce((best, item) => (!best || item.score > best.score ? item : best), null);
  const low = fortune.dimensions.reduce((best, item) => (!best || item.score < best.score ? item : best), null);
  const focus = topicKey === 'test'
    ? high
    : fortune.dimensions.find((item) => item.key === topicKey)
      || fortune.dimensions.find((item) => item.key === 'emotion' && topicKey === 'love')
      || fortune.dimensions[0];
  const score = Number(fortune.overallScore);
  const useColor = score % 2 === 0;
  const value = useColor ? fortune.luckyColor : fortune.luckyDirection;
  const sourceName = useColor ? '幸运色' : '吉利方位';
  const rhythmNote = rhythmKey === 'steady'
    ? '你选的稳速还摆在桌上'
    : rhythmKey === 'tired'
      ? '你说过的累也摆在桌上'
      : rhythmKey === 'rushed'
        ? '你说的被催着走也摆在桌上'
        : '节奏先按刚才那页记着';
  const scholarMain = mode !== 'half';
  let answer;

  if (scholarMain) {
    if (score >= 65) {
      if (choice.key === 'accept') {
        answer = `算你会接。「${focus.label}」 ${focus.score} 分先用在小处；${rhythmNote}，${sourceName}「${value}」当个记号。`;
      } else if (choice.key === 'pushback') {
        answer = `本半仙退后半步；可「${focus.label}」 ${focus.score} 分摆在这儿，${rhythmNote}，你别装没看见。`;
      } else {
        answer = `「${focus.label}」 ${focus.score} 是明面证据，「${high.label}」 ${high.score} 在后面撑着；${sourceName}「${value}」只是路标，${rhythmNote}。`;
      }
    } else if (score < 45) {
      if (choice.key === 'accept') {
        answer = `收下就行。「${low.label}」 ${low.score} 分先照顾一口饭、一觉觉；${rhythmNote}，${sourceName}「${value}」不用背锅。`;
      } else if (choice.key === 'pushback') {
        answer = `我这就少说两句；但「${low.label}」 ${low.score} 分还在页面上，${rhythmNote}，先做最小一件。`;
      } else {
        answer = `我看的是「${low.label}」 ${low.score} 分，它只说今天要省力；${rhythmNote}，${sourceName}「${value}」不是判决。`;
      }
    } else if (choice.key === 'accept') {
      answer = `稳稳接住就够。「${focus.label}」 ${focus.score} 分适合小步走；${rhythmNote}，${sourceName}「${value}」当便签。`;
    } else if (choice.key === 'pushback') {
      answer = `行行行，我不围观点评；可「${low.label}」 ${low.score} 分得照看，${rhythmNote}，大戏改天再唱。`;
    } else {
      answer = `「${focus.label}」 ${focus.score} 分是主线索：温吞不是坏事；「${high.label}」能借力，「${low.label}」要照顾，${rhythmNote}。`;
    }
  } else if (score >= 65) {
    if (choice.key === 'accept') {
      answer = `好，那就轻轻收下。「${focus.label}」 ${focus.score} 分值得用一次小行动；${rhythmNote}，${sourceName}「${value}」当提醒。`;
    } else if (choice.key === 'pushback') {
      answer = `我往旁边挪一步。「${high.label}」 ${high.score} 还亮着，「${low.label}」也要留口气；${rhythmNote}。`;
    } else {
      answer = `主证据是「${focus.label}」 ${focus.score} 分，「${high.label}」 ${high.score} 在旁证；${sourceName}「${value}」不是护身符，${rhythmNote}。`;
    }
  } else if (score < 45) {
    if (choice.key === 'accept') {
      answer = `先收下这句：「${low.label}」 ${low.score} 分需要休息；${rhythmNote}，${sourceName}「${value}」帮你停一下。`;
    } else if (choice.key === 'pushback') {
      answer = `好，我不多站了；但「${low.label}」 ${low.score} 分值得照看，${rhythmNote}，先吃饭睡觉。`;
    } else {
      answer = `我看到的是「${low.label}」 ${low.score} 分；它只是状态页，不是结论。${sourceName}「${value}」当暂停记号，${rhythmNote}。`;
    }
  } else if (choice.key === 'accept') {
    answer = `收得很稳。「${focus.label}」 ${focus.score} 分不急不缓；${rhythmNote}，${sourceName}「${value}」放在手边即可。`;
  } else if (choice.key === 'pushback') {
    answer = `我退到门边。「${high.label}」能搭把手，「${low.label}」 ${low.score} 别硬压；${rhythmNote}。`;
  } else {
    answer = `我把线捋过了：「${focus.label}」 ${focus.score} 分；${sourceName}「${value}」只作参照，${rhythmNote}。`;
  }

  const leadIndex = Math.floor((hash / 19) % 3);
  const lead = leadIndex === 1 ? '我又看了一眼；' : leadIndex === 2 ? '按这一页说；' : '';
  return lead + answer;
}

function guestHostWrapup(mode, styleKey, topicKey, fortune, rhythmKey = '', choiceKey = 'why') {
  if (!fortune || !Array.isArray(fortune.dimensions) || !fortune.dimensions.length) return '';

  const high = fortune.dimensions.reduce((best, item) => (!best || item.score > best.score ? item : best), null);
  const low = fortune.dimensions.reduce((best, item) => (!best || item.score < best.score ? item : best), null);
  const focus = topicKey === 'test'
    ? high
    : fortune.dimensions.find((item) => item.key === topicKey)
      || fortune.dimensions.find((item) => item.key === 'emotion' && topicKey === 'love')
      || fortune.dimensions[0];
  const score = Number(fortune.overallScore);
  const useColor = score % 2 === 0;
  const value = useColor ? fortune.luckyColor : fortune.luckyDirection;
  const sourceName = useColor ? '幸运色' : '吉利方位';
  const rhythmNote = rhythmKey === 'steady'
    ? '稳速那栏先合上'
    : rhythmKey === 'tired'
      ? '累的那栏先合上'
      : rhythmKey === 'rushed'
        ? '被催那栏先合上'
        : '节奏栏先按刚才记着';
  const scholar = mode !== 'half';
  const choiceNote = choiceKey === 'accept'
    ? '你接得住。'
    : choiceKey === 'pushback'
      ? '好，都退半步。'
      : '问得对。';
  let body;

  if (scholar) {
    if (styleKey === 'harbor') {
      if (score >= 65) {
        body = `客串的话我先接住。${focus.label}有 ${focus.score} 分，${low.label}也留着位置；${rhythmNote}。`;
      } else if (score < 45) {
        body = `这里不用急着翻页。${low.label} ${low.score} 分先被看见，${rhythmNote}；${sourceName}「${value}」放门口就好。`;
      } else {
        body = `灯还留着。${focus.label} ${focus.score} 分可以慢慢走，${rhythmNote}；${sourceName}「${value}」只作提醒。`;
      }
    } else if (styleKey === 'compass') {
      if (score >= 65) {
        body = `方向没有变大，只是更清楚。${focus.label} ${focus.score} 分可用一小步验证；${rhythmNote}。`;
      } else if (score < 45) {
        body = `先把针放慢。${low.label} ${low.score} 分需要照顾，${rhythmNote}；${sourceName}「${value}」当暂停点。`;
      } else {
        body = `指针停在这里就够了。${focus.label} ${focus.score} 分宜整理，${rhythmNote}；${sourceName}「${value}」留作参照。`;
      }
    } else if (score >= 65) {
      body = `我把客串那句夹进档案。${focus.label} ${focus.score} 分是入口，${low.label}做备注；${rhythmNote}。`;
    } else if (score < 45) {
      body = `档案里补一行：${low.label} ${low.score} 分需要休息，不是定罪；${rhythmNote}。`;
    } else {
      body = `这一页归档为观察项。${focus.label} ${focus.score} 分先小步走；${rhythmNote}，${sourceName}「${value}」当便签。`;
    }
  } else if (styleKey === 'alley') {
    if (score >= 65) {
      body = `茶先放下！${focus.label} ${focus.score} 分是真排面；${low.label}也带一口，${rhythmNote}。`;
    } else if (score < 45) {
      body = `咱不唱衰。${low.label} ${low.score} 分先歇口气，${rhythmNote}；${sourceName}「${value}」压在杯底当提醒。`;
    } else {
      body = `街口风不大。${focus.label} ${focus.score} 分慢慢晃过去就行，${rhythmNote}；${sourceName}「${value}」顺手看一眼。`;
    }
  } else if (styleKey === 'intern') {
    if (score >= 65) {
      body = `工单备注：${focus.label} ${focus.score} 分可用在一件小事上；${low.label}另开一栏，${rhythmNote}。`;
    } else if (score < 45) {
      body = `工单已降速：${low.label} ${low.score} 分优先休息；${rhythmNote}，${sourceName}「${value}」设成暂停标签。`;
    } else {
      body = `云端记录：${focus.label} ${focus.score} 分保持小步推进；${rhythmNote}，${sourceName}「${value}」仅作提示。`;
    }
  } else if (score >= 65) {
    body = `锣鼓停半拍！${focus.label} ${focus.score} 分确实亮眼；给${low.label}留口气，${rhythmNote}。`;
  } else if (score < 45) {
    body = `场务别催！${low.label} ${low.score} 分先回血，${rhythmNote}；${sourceName}「${value}」只是台侧暗号。`;
  } else {
    body = `今日戏码平稳。${focus.label} ${focus.score} 分按小段演，${rhythmNote}；${sourceName}「${value}」当道具提示。`;
  }
  return choiceNote + body;
}

function guestChoiceCarryover(mode = '', styleKey = '', choiceKey = '') {
  if (mode === 'scholar') {
    if (styleKey === 'archive') {
      if (choiceKey === 'why') return '客串退场后，档案页边多了一行：依据已被当面问过。';
      if (choiceKey === 'accept') return '客串退场后，档案里夹了一张便签：提醒已经由你收下。';
      if (choiceKey === 'pushback') return '客串退场后，档案合上半页：围观已经被你叫停。';
    }
    if (styleKey === 'harbor') {
      if (choiceKey === 'why') return '水面安静下来，你把依据这件事稳稳放上了岸。';
      if (choiceKey === 'accept') return '提醒被你先接住，泊位边少了一件悬着的事。';
      if (choiceKey === 'pushback') return '你拦住了围拢的视线，水面重新留给你自己。';
    }
    if (styleKey === 'compass') {
      if (choiceKey === 'why') return '罗盘指针在「依据」一格停留过，来源核对已经发生。';
      if (choiceKey === 'accept') return '罗盘旁留了一个小记号：那句提醒已被收进手边。';
      if (choiceKey === 'pushback') return '罗盘让出中心位置，两人围观的状态已经解除。';
    }
    return '';
  }

  if (mode === 'half') {
    if (styleKey === 'herald') {
      if (choiceKey === 'why') return '锣鼓停了半拍：依据问题已经递到台前！';
      if (choiceKey === 'accept') return '台侧记下一笔：提醒先被稳稳接住！';
      if (choiceKey === 'pushback') return '幕布收窄半尺：两个人一起看戏的状态被你叫停！';
    }
    if (styleKey === 'alley') {
      if (choiceKey === 'why') return '这茬摆在茶碗边上：依据你已经当面问过了。';
      if (choiceKey === 'accept') return '提醒接住了，咱先把它放在顺手的地方。';
      if (choiceKey === 'pushback') return '你一句话拦住了，俩人不再一起围着你看了。';
    }
    if (styleKey === 'intern') {
      if (choiceKey === 'why') return '工单状态更新为「依据已问过」。';
      if (choiceKey === 'accept') return '工单状态更新为「提醒已接收」。';
      if (choiceKey === 'pushback') return '工单状态更新为「围观已暂停」。';
    }
  }
  return '';
}

function presenceState(mode, styleKey, exchangeCount) {
  const count = Math.max(0, Number(exchangeCount) || 0);
  const band = count <= 0 ? 0 : count <= 2 ? 1 : count <= 4 ? 2 : 3;
  const states = {
    archive: ['档案刚翻开', '档案翻了几页', '档案桌上线索渐多', '档案桌正忙着核对'],
    harbor: ['灯刚点起来', '垫子上放进了几句话', '港口正在换气', '潮声来回正热闹'],
    compass: ['罗盘刚归位', '罗盘微调过一格', '指针还在慢慢对齐', '几条方向并排摆着'],
    herald: ['锣鼓正在候场', '台词已经排开', '场记单渐渐变厚', '舞台正处在换场节奏'],
    alley: ['大碗茶刚放下', '茶已经续了一轮', '街口聊出了热气', '几摊话题一起开着'],
    intern: ['云端工单刚新建', '小本本添了几行', '便签开始排起队', '多张工单并行流转'],
  };
  const styleStates = states[styleKey] || (mode === 'half' ? states.intern : states.archive);
  return styleStates[band];
}

function interactionCarryover(mode, styleKey, optionLabel) {
  const label = String(optionLabel || '').trim();
  if (!label) return '';
  if (styleKey === 'archive') return `档案边角先记一笔：你刚才选了「${label}」。`;
  if (styleKey === 'harbor') return `我记得你刚才选了「${label}」，先把它放在手边。`;
  if (styleKey === 'compass') return `你刚才选的「${label}」，我当作一个参照点留着。`;
  if (styleKey === 'herald') return `刚才那句「${label}」已在后台登记！`;
  if (styleKey === 'alley') return `行，你刚挑的是「${label}」，咱记着这茬。`;
  if (styleKey === 'intern') return `工单备注：你刚才选了「${label}」。`;
  if (mode !== 'half') return `你刚才选的「${label}」，我当作一个参照点留着。`;
  return `工单备注：你刚才选了「${label}」。`;
}

function composeReaction(carryover, reactionLine) {
  const prefix = String(carryover || '').trim();
  const base = String(reactionLine || '').trim();
  if (!prefix) return base;
  if (!base) return prefix;
  return `${prefix} ${base}`;
}

function memoryNote(mode, styleKey, kind, detail) {
  const familyStyles = {
    scholar: ['archive', 'harbor', 'compass'],
    half: ['herald', 'alley', 'intern'],
  }[mode];
  const kinds = ['opening', 'rhythm', 'game', 'guest', 'ask', 'handoff'];
  if (!familyStyles || !familyStyles.includes(styleKey) || !kinds.includes(kind)) return '';

  let clean = String(detail || '').normalize('NFC')
    .replace(/[\u0000-\u0008\u000B-\u001F\u007F-\u009F\u200B-\u200F\uFEFF]/g, '')
    .replace(/[\s\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]+/g, ' ')
    .trim();
  if (!clean) return '';
  if (Array.from(clean).length > 24) clean = `${Array.from(clean).slice(0, 24).join('')}…`;

  const notes = {
    archive: {
      opening: `我把开场这一步记进档案：${clean}。`,
      rhythm: `节奏栏补了一笔：${clean}。`,
      game: `小游戏这一步留下记录：${clean}。`,
      guest: `客串那阵的立场已记下：${clean}。`,
      ask: `你问的这句被页边折角保留：${clean}。`,
      handoff: `从${clean}换页，旧线索仍夹在原处。`,
    },
    harbor: {
      opening: `我把开场这一步放到灯下：${clean}。`,
      rhythm: `你的节奏是${clean}，我先替你收着。`,
      game: `小游戏里的${clean}，我摆在容易看见的地方。`,
      guest: `客串退开后，你的立场是${clean}。`,
      ask: `你问到的${clean}，我先把这句接稳。`,
      handoff: `从${clean}走过来，水面还留着刚才的痕迹。`,
    },
    compass: {
      opening: `开场签到成为第一个参照点：${clean}。`,
      rhythm: `我把${clean}标进今天的速度栏。`,
      game: `小游戏选过${clean}，指针旁留了个标记。`,
      guest: `客串立场停在罗盘边缘：${clean}。`,
      ask: `这句问题钉在当前方位：${clean}。`,
      handoff: `从${clean}转页，旧标记没有抹掉。`,
    },
    herald: {
      opening: `开场锣鼓收到：${clean}已入场！`,
      rhythm: `节奏台本补一笔：${clean}登记完毕！`,
      game: `小游戏这一手挂上侧幕：${clean}！`,
      guest: `客串台词痕迹留下：${clean}！`,
      ask: `这句被递到台前，场记先收好：${clean}！`,
      handoff: `从${clean}换幕，旧场记继续跟着！`,
    },
    alley: {
      opening: `开场这句咱先搁茶碗边：${clean}。`,
      rhythm: `今天这个劲儿我给你记着：${clean}。`,
      game: `小游戏挑的这茬留下了：${clean}。`,
      guest: `客串那茬你的接法是：${clean}。`,
      ask: `你问的这句先摆桌面：${clean}。`,
      handoff: `从${clean}挪过来，旧茬还在茶边放着。`,
    },
    intern: {
      opening: `开场记录已提交：${clean}。`,
      rhythm: `节奏工单更新为：${clean}。`,
      game: `小游戏结果已归档：${clean}。`,
      guest: `客串互动备注：${clean}。`,
      ask: `问题已加入待核对清单：${clean}。`,
      handoff: `从${clean}交接，旧标签继续保留。`,
    },
  };
  return notes[styleKey][kind];
}

function thinkingLine(mode, styleKey, kind) {
  const scholar = mode !== 'half';
  if (kind === 'opening') {
    if (scholar) {
      if (styleKey === 'archive') return '正在给签到句找位置';
      if (styleKey === 'harbor') return '正在接住开场那句';
      return '罗盘正在对准入口';
    }
    if (styleKey === 'herald') return '开场名帖正在登记';
    if (styleKey === 'alley') return '开场这茬正摆上桌';
    return '签到表单提交中';
  }
  if (kind === 'rhythm') {
    if (scholar) {
      if (styleKey === 'archive') return '正在把节奏栏补上';
      if (styleKey === 'harbor') return '正在接住你的节奏';
      return '指针正按你的速度调整';
    }
    if (styleKey === 'herald') return '节奏档位正在登记';
    if (styleKey === 'alley') return '半仙正在掂量你的劲儿';
    return '节奏表单提交中';
  }
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
  const variant = customPulse(question) % 2;
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

function customClarifier(mode, styleKey, question, fortune, test = null) {
  if (!fortune || !Array.isArray(fortune.dimensions) || !fortune.dimensions.length) return null;

  const intent = customIntent(question);
  const family = {
    mood: 'reflect',
    care: 'reflect',
    health: 'reflect',
    love: 'reflect',
    career: 'act',
    wealth: 'act',
    study: 'act',
    action: 'act',
    topic: 'act',
    why: 'check',
    outcome: 'check',
  }[intent];
  if (!family) return null;

  const persona = mode === 'half' ? 'half' : 'scholar';
  const titles = {
    'scholar/archive/reflect': '档案页边有个小问号',
    'scholar/harbor/reflect': '灯下想轻轻问一句',
    'scholar/compass/reflect': '罗盘停在一个岔口',
    'scholar/archive/act': '档案里还差一行注记',
    'scholar/harbor/act': '先把船桨放稳一点',
    'scholar/compass/act': '指针想再校一次方向',
    'scholar/archive/check': '这份记录还要对个来源',
    'scholar/harbor/check': '浪头下面先看一眼锚点',
    'scholar/compass/check': '北针先确认读数',
    'half/herald/reflect': '锣鼓暂停，司仪要补一句',
    'half/alley/reflect': '大碗茶边上冒出个问题',
    'half/intern/reflect': '工单备注栏亮了一下',
    'half/herald/act': '登台前先对一遍台本',
    'half/alley/act': '动手前咱把袖口掸一掸',
    'half/intern/act': '执行前先补一张便签',
    'half/herald/check': '谢幕前先核对节目单',
    'half/alley/check': '这摊账得翻两页看看',
    'half/intern/check': '归档前先跑一次自检',
  };
  const title = titles[`${persona}/${styleKey}/${family}`];
  if (!title) return null;

  const highSource = fortune.dimensions.reduce(
    (best, item) => (!best || item.score > best.score ? item : best),
    null
  );
  const lowSource = fortune.dimensions.reduce(
    (best, item) => (!best || item.score < best.score ? item : best),
    null
  );
  if (!highSource || !lowSource) return null;
  const high = { ...highSource, score: Number(highSource.score) };
  const low = { ...lowSource, score: Number(lowSource.score) };

  const answers = {
    scholar: {
      reflect: {
        low: `先看「${low.label}」 ${low.score} 分；它不是判决，只是今天最需要照看的信号。综合 ${fortune.overallScore} 分，给它留一点余量就够了。`,
        specific: `把刚才的问题落到一件事上：综合 ${fortune.overallScore} 分，「${high.label}」 ${high.score} 可用，「${low.label}」 ${low.score} 要照看。越具体，越不容易被情绪带偏。`,
        pause: `今天综合 ${fortune.overallScore} 分，幸运色「${fortune.luckyColor}」可以当休息提醒。「${low.label}」 ${low.score} 需要照看，先停十分钟不丢人。`,
      },
      act: {
        small: `从「${high.label}」 ${high.score} 借力，挑一件最小、今天一定能完成的事；综合 ${fortune.overallScore} 分，完成比铺开更有用。`,
        guard: `「${low.label}」 ${low.score} 是今天的护栏位。金额、承诺和睡眠先设上限，幸运色「${fortune.luckyColor}」只当冷静开关。`,
        timing: `综合 ${fortune.overallScore} 分说明今天适合分批推进。先用「${high.label}」 ${high.score} 开场，遇到「${low.label}」 ${low.score} 的环节放到状态好一点的时候。`,
      },
      check: {
        source: `来源是现有盘面：综合 ${fortune.overallScore} 分，「${high.label}」 ${high.score} 最强，「${low.label}」 ${low.score} 最需照看。它是参照，不是命运盖章。`,
        risk: `最该留意「${low.label}」 ${low.score}；综合 ${fortune.overallScore} 分时，风险常藏在过度承诺和忽略身体信号里。先把边界写清楚。`,
        next: `看完这一格，先回到「${high.label}」 ${high.score} 能推动的小事；「${low.label}」 ${low.score} 只安排照看动作，不用反复占卜。`,
      },
    },
    half: {
      reflect: {
        low: `「${low.label}」 ${low.score} 分在打盹，综合 ${fortune.overallScore} 分还没塌！别把全部力气都押上去，先护住这块就行。`,
        specific: `别问天机，问具体事！综合 ${fortune.overallScore} 分，「${high.label}」 ${high.score} 在线，「${low.label}」 ${low.score} 爱闹。说清一件事，本半仙才好帮你拆。`,
        pause: `${fortune.overallScore} 分还想硬冲？「${low.label}」 ${low.score} 都举白旗了！用「${fortune.luckyColor}」提醒自己歇口气，神仙也讲究可持续摸鱼。`,
      },
      act: {
        small: `别摆十八般武艺！综合 ${fortune.overallScore} 分，「${high.label}」 ${high.score} 是你的趁手家伙；先做一小步，功劳簿也好记账。`,
        guard: `「${low.label}」 ${low.score} 爱挖坑，综合 ${fortune.overallScore} 分也别浪！大额、大话和大熬夜都先拦住；「${fortune.luckyColor}」是刹车贴纸，不是护身符。`,
        timing: `什么时候出手？先看「${high.label}」 ${high.score} 什么时候在线！综合 ${fortune.overallScore} 分，别等黄道吉时等成搁浅；小事现在就能动。`,
      },
      check: {
        source: `账本在这：综合 ${fortune.overallScore} 分，「${high.label}」 ${high.score} 举火把，「${low.label}」 ${low.score} 坐轿子。数字来自算法，不是本半仙半夜编的。`,
        risk: `「${low.label}」 ${low.score} 爱使绊子，综合 ${fortune.overallScore} 分时最怕嘴上答应太快、身体电量太低。先留退路，别硬闯。`,
        next: `别赖在签筒前啦！综合 ${fortune.overallScore} 分，「${high.label}」 ${high.score} 已备好；回去做一件小事，比再抽十次都灵。`,
      },
    },
  }[persona][family];

  const labels = {
    reflect: [
      ['low', '最想护住哪块？'],
      ['specific', '能不能说得更具体？'],
      ['pause', '要不要先歇一步？'],
    ],
    act: [
      ['small', '最小一步选哪个？'],
      ['guard', '哪里需要先设护栏？'],
      ['timing', '什么时候出手合适？'],
    ],
    check: [
      ['source', '这个判断从哪来？'],
      ['risk', '眼下最该防什么？'],
      ['next', '看完后往哪走？'],
    ],
  }[family];

  return {
    title,
    options: labels.map(([key, label]) => ({ key, label, answer: answers[key] })),
  };
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
    const testName = test.testName || test.name || '最近测试';
    facts.push(`最近测试：${testName} → ${resultCode}（${resultName}）`);
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
        test ? (test.testName || test.name || '') : ''
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
  openingCheckin,
  openingReaction,
  rhythmCheckin,
  rhythmReaction,
  rhythmCarryover,
  presenceState,
  interactionCarryover,
  composeReaction,
  memoryNote,
  handoffReaction,
  thinkingLine,
  topicHandoff,
  reaction,
  customAnswer,
  customReaction,
  customClarifier,
  guestCameo,
  guestChoices,
  guestReply,
  guestHostWrapup,
  guestChoiceCarryover,
  readCompanionSession,
  rememberCompanionSession,
};
