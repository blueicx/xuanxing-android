package com.xuanji.app.domain.mbti

/**
 * MBTI 风格性格自我探索（40 题自编简版；不是官方 MBTI 测评）。
 * 40 题，四维度（E/I, S/N, T/F, J/P）各 10 题，16 种人格类型详细解读。
 * 全离线、确定性计分，供职业性格参考。
 */

data class MbtiOption(val key: String, val text: String, val dimension: String)
data class MbtiQuestion(val q: String, val options: List<MbtiOption>)

data class MbtiType(
    val code: String,
    val name: String,
    val description: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val careers: List<String>,
    val relationships: String,
    val growth: String,
    val famous: String
)

object Mbti {
    val QUESTIONS: List<MbtiQuestion> = listOf(
        // E/I 1-10
        MbtiQuestion("在聚会或社交场合中，你通常：", listOf(
            MbtiOption("A", "喜欢成为焦点，主动与人交谈", "E"),
            MbtiOption("B", "更愿意观察他人，享受安静交流", "I"))),
        MbtiQuestion("当你需要放松时，你更倾向于：", listOf(
            MbtiOption("A", "约朋友一起外出或参加活动", "E"),
            MbtiOption("B", "独自在家看书、看电影或做手工", "I"))),
        MbtiQuestion("在团队讨论中，你通常：", listOf(
            MbtiOption("A", "积极发言，分享自己的想法", "E"),
            MbtiOption("B", "先倾听他人，再谨慎表达", "I"))),
        MbtiQuestion("你更享受哪种沟通方式？", listOf(
            MbtiOption("A", "面对面交流，实时互动", "E"),
            MbtiOption("B", "书面沟通（邮件/消息），有时间思考", "I"))),
        MbtiQuestion("你更喜欢的工作环境是：", listOf(
            MbtiOption("A", "开放、协作、充满人际交流", "E"),
            MbtiOption("B", "安静、独立、有足够私人空间", "I"))),
        MbtiQuestion("你通常如何处理新信息？", listOf(
            MbtiOption("A", "说出来或写下来，与他人分享", "E"),
            MbtiOption("B", "在内心反复思考，独自消化", "I"))),
        MbtiQuestion("你更喜欢哪种社交规模？", listOf(
            MbtiOption("A", "大型聚会，认识新朋友", "E"),
            MbtiOption("B", "小型深度交流，少数几个朋友", "I"))),
        MbtiQuestion("在空余时间，你通常：", listOf(
            MbtiOption("A", "外出活动，探索新鲜事物", "E"),
            MbtiOption("B", "待在家里，进行个人爱好", "I"))),
        MbtiQuestion("你觉得哪种情况更耗费你的精力？", listOf(
            MbtiOption("A", "长时间独处，缺少人际互动", "E"),
            MbtiOption("B", "长时间社交，需要频繁互动", "I"))),
        MbtiQuestion("在人群中，你更倾向于：", listOf(
            MbtiOption("A", "主动发起对话和活动", "E"),
            MbtiOption("B", "等待他人接近或加入", "I"))),

        // S/N 11-20
        MbtiQuestion("你更容易注意到：", listOf(
            MbtiOption("A", "眼前的具体细节和事实", "S"),
            MbtiOption("B", "整体模式和潜在可能性", "N"))),
        MbtiQuestion("当你阅读时，你更关注：", listOf(
            MbtiOption("A", "字面意思和具体描述", "S"),
            MbtiOption("B", "字里行间的含义和象征", "N"))),
        MbtiQuestion("你更喜欢哪种类型的信息？", listOf(
            MbtiOption("A", "实际、可验证、基于经验", "S"),
            MbtiOption("B", "抽象、理论、富有想象力", "N"))),
        MbtiQuestion("在解决问题时，你通常：", listOf(
            MbtiOption("A", "依赖已有经验和标准方法", "S"),
            MbtiOption("B", "寻找创新方法和新思路", "N"))),
        MbtiQuestion("你更喜欢哪种娱乐方式？", listOf(
            MbtiOption("A", "亲自体验（如运动、手工）", "S"),
            MbtiOption("B", "幻想、构思、文学艺术", "N"))),
        MbtiQuestion("在描述事件时，你倾向于：", listOf(
            MbtiOption("A", "按照时间顺序和具体细节", "S"),
            MbtiOption("B", "突出整体印象和意义", "N"))),
        MbtiQuestion("你更相信：", listOf(
            MbtiOption("A", "亲眼所见，亲耳所闻", "S"),
            MbtiOption("B", "直觉和预感", "N"))),
        MbtiQuestion("在学习新事物时，你倾向于：", listOf(
            MbtiOption("A", "逐步实践，按部就班", "S"),
            MbtiOption("B", "先了解理论框架，再深入", "N"))),
        MbtiQuestion("你更擅长：", listOf(
            MbtiOption("A", "关注眼下，处理具体事务", "S"),
            MbtiOption("B", "设想未来，规划长期目标", "N"))),
        MbtiQuestion("你更容易被什么吸引？", listOf(
            MbtiOption("A", "现实中有趣的事物", "S"),
            MbtiOption("B", "脑海中的奇思妙想", "N"))),

        // T/F 21-30
        MbtiQuestion("在做决策时，你更依赖：", listOf(
            MbtiOption("A", "逻辑分析和客观事实", "T"),
            MbtiOption("B", "个人价值观和他人感受", "F"))),
        MbtiQuestion("你通常如何评价别人的意见？", listOf(
            MbtiOption("A", "根据合理性和一致性", "T"),
            MbtiOption("B", "根据真诚性和情感表达", "F"))),
        MbtiQuestion("在争论中，你更注重：", listOf(
            MbtiOption("A", "谁的观点更符合逻辑", "T"),
            MbtiOption("B", "如何保持和谐关系", "F"))),
        MbtiQuestion("你更容易被以下哪种方式说服？", listOf(
            MbtiOption("A", "清晰的推理和证据", "T"),
            MbtiOption("B", "感人的故事和情感共鸣", "F"))),
        MbtiQuestion("你更喜欢哪种领导风格？", listOf(
            MbtiOption("A", "公正客观，基于绩效", "T"),
            MbtiOption("B", "关心团队，注重人文关怀", "F"))),
        MbtiQuestion("在处理冲突时，你倾向于：", listOf(
            MbtiOption("A", "分析是非对错，理性解决", "T"),
            MbtiOption("B", "照顾各方情绪，寻求共识", "F"))),
        MbtiQuestion("你认为以下哪个更重要？", listOf(
            MbtiOption("A", "真理与公正", "T"),
            MbtiOption("B", "善良与体谅", "F"))),
        MbtiQuestion("在人际交往中，你更看重：", listOf(
            MbtiOption("A", "思想的准确性和深度", "T"),
            MbtiOption("B", "情感的真挚和亲密度", "F"))),
        MbtiQuestion("你更容易被什么触动？", listOf(
            MbtiOption("A", "一个逻辑严谨的论证", "T"),
            MbtiOption("B", "一段温暖人心的故事", "F"))),
        MbtiQuestion("你更欣赏哪种品质？", listOf(
            MbtiOption("A", "理性、客观、独立思考", "T"),
            MbtiOption("B", "同理心、温柔、善解人意", "F"))),

        // J/P 31-40
        MbtiQuestion("你更喜欢哪种生活方式？", listOf(
            MbtiOption("A", "有计划、有组织、按时间表", "J"),
            MbtiOption("B", "灵活、随性、适应变化", "P"))),
        MbtiQuestion("在旅行前，你通常：", listOf(
            MbtiOption("A", "制定详细行程，预订一切", "J"),
            MbtiOption("B", "只定大方向，边走边看", "P"))),
        MbtiQuestion("你更喜欢哪种工作方式？", listOf(
            MbtiOption("A", "有条理地完成任务清单", "J"),
            MbtiOption("B", "根据灵感即兴发挥", "P"))),
        MbtiQuestion("你对 deadline 的态度是：", listOf(
            MbtiOption("A", "提前规划，确保按时完成", "J"),
            MbtiOption("B", "最后时刻集中精力冲刺", "P"))),
        MbtiQuestion("你更偏好哪种环境？", listOf(
            MbtiOption("A", "整洁、有序、有明确规则", "J"),
            MbtiOption("B", "自由、随意、有创意空间", "P"))),
        MbtiQuestion("在做决定时，你倾向于：", listOf(
            MbtiOption("A", "尽快做出决定，避免拖延", "J"),
            MbtiOption("B", "保留选择余地，保持开放", "P"))),
        MbtiQuestion("你更享受哪种生活方式？", listOf(
            MbtiOption("A", "规律性强的日常安排", "J"),
            MbtiOption("B", "充满新奇和变化的日常", "P"))),
        MbtiQuestion("你更擅长：", listOf(
            MbtiOption("A", "计划和遵守计划", "J"),
            MbtiOption("B", "即兴调整和适应变化", "P"))),
        MbtiQuestion("你更容易感到压力：", listOf(
            MbtiOption("A", "当计划被打乱时", "J"),
            MbtiOption("B", "当被要求严格遵守规则时", "P"))),
        MbtiQuestion("你更倾向于哪种决策风格？", listOf(
            MbtiOption("A", "确定、闭环、达成一致", "J"),
            MbtiOption("B", "灵活、探索、开放讨论", "P")))
    )

    val TYPES: Map<String, MbtiType> = mapOf(
        "ISTJ" to MbtiType("ISTJ", "检查者型 / 物流师",
            "安静、严谨、负责、有条理。注重事实和细节，坚持标准和规则，是可靠的组织者和执行者。",
            listOf("高度负责，信守承诺", "条理清晰，善于组织和规划", "注重实际，执行力强", "冷静理智，善于应对压力"),
            listOf("过于保守，抗拒改变", "可能显得固执和刻板", "不善于表达情感，给人距离感", "有时过于完美主义"),
            listOf("会计", "审计师", "法官", "行政管理人员", "军警", "工程技术人员", "图书馆管理员"),
            "忠诚可靠，是值得依赖的伴侣和朋友。但需要学会表达情感，理解他人的灵活性。",
            "尝试开放接受新观点，练习分享情感，适度放松标准。",
            "乔治·华盛顿，赫拉克利特，约翰·洛克，丹泽尔·华盛顿"),
        "ISFJ" to MbtiType("ISFJ", "保护者型 / 守卫者",
            "安静、友善、有责任感、关怀他人。重视和谐与传统，默默奉献，是可靠的守护者。",
            listOf("体贴入微，善解人意", "坚守传统，有始有终", "细致周到，善于照顾他人需求", "忠诚且勤奋"),
            listOf("过度自我牺牲，忽视自身需求", "过于敏感，易受批评伤害", "不善于表达不满，容易积压情绪", "抗拒变化，对新事物有疑虑"),
            listOf("护士", "教师", "心理咨询师", "社会工作者", "行政助理", "图书馆员", "医护人员"),
            "温暖体贴，是理想的伴侣和父母。需学会明确表达自己的边界和需求。",
            "学习说'不'，为自己留出空间，培养接受变化的意愿。",
            "特蕾莎修女，圣雄甘地，乔治·哈里森，克里斯汀·斯图尔特"),
        "INFJ" to MbtiType("INFJ", "倡导者型 / 提倡者",
            "富有洞察力、理想主义、有使命感的愿景家。深刻理解他人，渴望为世界带来积极改变。",
            listOf("深具同理心，洞察他人", "富有远见和创造力", "坚定执着，为理想奋斗", "善于激励他人"),
            listOf("过于理想化，易失望", "高敏感，易受他人情绪影响", "过于神秘，难以被完全理解", "容易过度劳累"),
            listOf("作家", "心理咨询师", "教育工作者", "人力资源", "灵性导师", "社会改革者"),
            "渴望深刻、有意义的连接，是忠诚且有深度的伴侣。需要学会平衡给予与接受。",
            "学会放下对完美的执着，接受现实的不完美，定期给自己充电。",
            "马丁·路德·金，纳尔逊·曼德拉，尼采，莎士比亚，歌德"),
        "INTJ" to MbtiType("INTJ", "战略家型 / 建筑师",
            "独立、果断、战略思维强。擅长深度思考和长期规划，追求知识、效率和自我实现。",
            listOf("战略思维，规划长远", "高度理性，客观决策", "自信独立，不畏挑战", "持续学习，追求卓越"),
            listOf("过于挑剔，难以容忍低效", "不擅社交，给人冷漠印象", "过于完美主义，易拖延", "固执己见，不轻易妥协"),
            listOf("科学家", "工程师", "律师", "战略顾问", "创业家", "系统分析师"),
            "独立自主，需要伴侣理解其空间需求。感情上忠诚但表达有限。",
            "练习表达情感，接受不同观点，培养灵活性和耐心。",
            "艾萨克·牛顿，史蒂芬·霍金，艾隆·马斯克，玛丽·居里"),
        "ISTP" to MbtiType("ISTP", "冒险家型 / 鉴赏家",
            "冷静、好奇、务实、善于操作。喜欢动手实践，追求刺激和自由，是出色的问题解决者。",
            listOf("反应敏捷，善于应对突发", "动手能力强，喜欢探索", "灵活变通，不拘一格", "客观冷静，不情绪化"),
            listOf("太过随性，缺乏计划性", "可能显得冷漠，不关心他人", "容易冲动，追求过度刺激", "对长期承诺有困难"),
            listOf("机械师", "飞行员", "外科医生", "技术人员", "赛车手", "危机管理"),
            "重视自由和独立，需要伴侣理解其节奏。感情真诚但不愿被约束。",
            "培养责任感，学习承诺的重要性，关注他人情感需求。",
            "刘易斯·汉密尔顿，史蒂夫·麦奎因，克林特·伊斯特伍德，汤姆·克鲁斯"),
        "ISFP" to MbtiType("ISFP", "探险家型 / 探险家",
            "安静、随和、富有艺术气质。活在当下，欣赏美和感官体验，善于感知他人的情感。",
            listOf("艺术天赋，创造力强", "温柔体贴，善于共情", "灵活适应，随遇而安", "真诚自然，不矫揉造作"),
            listOf("不善规划，缺乏长远目标", "避免冲突，可能压抑自己", "容易自我贬低，不自信", "过于敏感，易受伤"),
            listOf("艺术家", "音乐家", "设计师", "摄影师", "美容师", "园艺师"),
            "细腻浪漫，追求心灵的契合。需要伴侣给予肯定和安全感。",
            "培养自信心，学习表达需求，尝试制定小目标并坚持。",
            "迈克尔·杰克逊，文森特·梵高，玛丽莲·梦露，肖邦"),
        "INFP" to MbtiType("INFP", "治疗师型 / 调停者",
            "理想主义、浪漫、充满同理心。追求意义和真实性，富有创造力和共情力，是和平的倡导者。",
            listOf("想象力丰富，创造力强", "深具同理心，善解人意", "忠诚于价值观，有原则", "善于倾听和支持他人"),
            listOf("过于理想化，易挫败", "回避冲突，压抑情感", "决策困难，优柔寡断", "容易自责，自我怀疑"),
            listOf("作家", "心理咨询师", "社工", "教师", "艺术家", "人力资源", "公益人士"),
            "浪漫主义，渴望灵魂伴侣。重视精神连接，需要信任和理解。",
            "学会设定底线，将理想转化为小行动，接受不完美。",
            "威廉·莎士比亚，约翰·列侬，村上春树，科特·柯本，鲁米"),
        "INTP" to MbtiType("INTP", "逻辑学家型 / 逻辑学家",
            "理性、好奇、善于分析。热衷于理论和体系，喜欢探索复杂问题，是真正的思考者。",
            listOf("分析力强，逻辑清晰", "创新思维，善于提出新颖构想", "独立客观，保持中立", "学习能力强，知识渊博"),
            listOf("过于理论化，脱离现实", "不善社交，给人孤僻感", "优柔寡断，难以决策", "过于挑剔，习惯性怀疑"),
            listOf("科学家", "程序员", "研究员", "系统架构师", "数学教师", "分析师"),
            "需要思想共鸣，伴侣应能理解其独立空间和智力追求。",
            "实践社交技巧，学习将想法付诸行动，接受情感的重要性。",
            "阿尔伯特·爱因斯坦，查尔斯·达尔文，比尔·盖茨，柏拉图"),
        "ESTP" to MbtiType("ESTP", "创业者型 / 企业家",
            "精力充沛、冒险、机智、善于社交。活在当下，善于抓住机会，是天生的行动派。",
            listOf("行动力强，果断敢拼", "灵活适应，善于应变", "人际魅力，社交能手", "实际务实，高效解决问题"),
            listOf("冲动，缺乏耐心", "追求刺激，容易分心", "可能忽视他人感受", "难以遵循规则和纪律"),
            listOf("企业家", "销售", "经纪人", "新闻记者", "警察", "消防员", "特技演员"),
            "热烈直接，需要伴侣能跟上其节奏和冒险精神。",
            "培养耐心，学习关注长期目标，注重沟通中的情感层面。",
            "托马斯·爱迪生，温斯顿·丘吉尔，欧内斯特·海明威，麦当娜"),
        "ESFP" to MbtiType("ESFP", "表演者型 / 表演者",
            "热情、乐观、友善、善于表现。享受生活，喜欢成为注意中心，是真正的快乐使者。",
            listOf("阳光开朗，感染力强", "善于人际，适应力好", "实践能力强，动手积极", "同理心强，乐于助人"),
            listOf("注意力易分散，三分钟热度", "过分追求即时快乐，忽略长期", "过于情绪化，易被影响", "可能缺乏深度和计划性"),
            listOf("演员", "主持人", "老师", "销售", "导游", "活动策划", "艺人"),
            "浪漫热情，需要伴侣给予肯定和共同享乐。",
            "培养专注力，制定可行计划，学会独处和内在成长。",
            "埃尔维斯·普雷斯利，玛丽亚·凯莉，罗宾·威廉姆斯，艾米·怀恩豪斯"),
        "ENFP" to MbtiType("ENFP", "激励者型 / 竞选者",
            "充满热情、创造力和激情。善于洞察人与事物之间的联系，是富有感染力的梦想家。",
            listOf("热情洋溢，激励他人", "创造力强，善于联想", "善于沟通，建立连接", "灵活开放，拥抱变化"),
            listOf("过于理想化，易受打击", "缺乏条理，难以专注", "容易过度承诺，导致忙碌", "情感波动大"),
            listOf("培训师", "作家", "导演", "社会活动家", "公关", "心理咨询师"),
            "热情浪漫，寻求充满活力和意义的关系。需要伴侣理解其多样性。",
            "学习设定优先级，提高执行力，关注细节。",
            "罗伯特·弗罗斯特，沃尔特·迪士尼，马克·吐温，小罗伯特·唐尼"),
        "ENTP" to MbtiType("ENTP", "发明家型 / 辩论家",
            "聪明、好奇、富有挑战精神。喜欢争论和探索新观点，是充满机智的思想刺激者。",
            listOf("创造性思维，解决问题", "快节奏，灵活应变", "善于辩论，表达清晰", "开放心态，追求新知"),
            listOf("好辩，可能冒犯他人", "缺乏坚持，三分钟热度", "过于理智，忽视情感", "不守纪律，难以遵循常规"),
            listOf("科学家", "律师", "记者", "发明家", "产品经理", "咨询顾问"),
            "喜欢智力上的较量，需要能跟上其思维节奏的伴侣。",
            "学习倾听和共情，专注完成项目，培养情感敏感度。",
            "本杰明·富兰克林，尼古拉·特斯拉，斯蒂夫·乔布斯，昆汀·塔伦蒂诺"),
        "ESTJ" to MbtiType("ESTJ", "监督者型 / 总经理",
            "果断、务实、有组织、有责任心。是天生的领导者，注重秩序和效率，善于管理事务。",
            listOf("领导力强，决策果断", "组织能力出色，高效执行", "诚实正直，言行一致", "负责任，值得信赖"),
            listOf("固执，听不进意见", "过于强势，缺乏变通", "忽视他人情感，显得冷漠", "工作狂，难放松"),
            listOf("高级管理", "项目经理", "法官", "军官", "公务员", "会计师"),
            "忠诚可靠，但需学习表达柔情和关注伴侣需求。",
            "学会倾听，适当放权，培养同理心。",
            "富兰克林·罗斯福，亨利·福特，美国前总统杜鲁门，碧昂丝"),
        "ESFJ" to MbtiType("ESFJ", "供给者型 / 执政官",
            "友善、合作、有责任感、注重和谐。善于关照他人，是社区和家庭的核心维系者。",
            listOf("乐于助人，善于关怀", "社交能力强，善于协调", "有组织，责任心强", "信守承诺，忠诚可靠"),
            listOf("过于取悦他人，忽视自身", "对批评敏感，容易受伤", "避免冲突，可能压抑", "过于传统，抗拒变化"),
            listOf("教师", "护士", "人力资源", "社工", "社区管理", "酒店管理"),
            "关怀备至，喜欢照顾伴侣和家庭。需要学会设立边界。",
            "练习自我关怀，接受不完美，尝试新事物。",
            "桑德拉·布洛克，米歇尔·奥巴马，艾尔莎·帕托，比尔·克林顿"),
        "ENFJ" to MbtiType("ENFJ", "教育家型 / 主人公",
            "富有感染力、关怀他人、有远见。善于激励和引导，是天生的领导者和变革者。",
            listOf("激励他人，富有说服力", "善解人意，同理心强", "有远见，推动变革", "善于沟通，组织能力强"),
            listOf("过于理想化，易失望", "可能过度干预他人", "难以拒绝，易承压", "对批评敏感"),
            listOf("教师", "培训师", "心理咨询师", "公关", "人力资源", "公益领袖"),
            "热情、忠诚、愿意付出。需要伴侣回应其关怀并给予支持。",
            "学会放手，尊重他人独立性，平衡付出与自我。",
            "马丁·路德·金，奥普拉·温弗瑞，巴拉克·奥巴马，居里夫人"),
        "ENTJ" to MbtiType("ENTJ", "指挥官型 / 指挥官",
            "果断、战略、自信、执行力强。是天生的领导者，善于规划和组织，追求效率和成就。",
            listOf("战略思维，长远规划", "决策果断，执行力强", "自信坚定，鼓舞团队", "善于管理，资源整合"),
            listOf("过于强硬，可能咄咄逼人", "忽视情感，人际关系紧张", "工作狂，难以放松", "缺乏耐心，对低效容忍度低"),
            listOf("高管", "创始人", "战略顾问", "将军", "律师", "政治家"),
            "事业型伴侣，需要对方独立且能接受其高强度精力。",
            "练习倾听和同理，学会授权，平衡工作与生活。",
            "尤利乌斯·凯撒，史蒂夫·乔布斯，拿破仑，比尔·克林顿")
    )

    val ALL_TYPES: List<MbtiType> = TYPES.entries.sortedBy { it.key }.map { it.value }

    /** 计分并返回类型 */
    fun calculate(answers: List<String>): MbtiType {
        val scores = mutableMapOf("E" to 0, "I" to 0, "S" to 0, "N" to 0, "T" to 0, "F" to 0, "J" to 0, "P" to 0)
        answers.forEachIndexed { i, key ->
            val dim = QUESTIONS[i].options.firstOrNull { it.key == key }?.dimension
            if (dim != null) scores[dim] = scores.getOrDefault(dim, 0) + 1
        }
        fun pick(a: String, b: String): String = if ((scores[a] ?: 0) >= (scores[b] ?: 0)) a else b
        val code = pick("E", "I") + pick("S", "N") + pick("T", "F") + pick("J", "P")
        return TYPES[code] ?: TYPES["INFP"]!!
    }

    /** 返回各维度得分，用于展示 */
    fun scores(answers: List<String>): List<Pair<String, Pair<Int, Int>>> {
        val scores = mutableMapOf("E" to 0, "I" to 0, "S" to 0, "N" to 0, "T" to 0, "F" to 0, "J" to 0, "P" to 0)
        answers.forEachIndexed { i, key ->
            val dim = QUESTIONS[i].options.firstOrNull { it.key == key }?.dimension
            if (dim != null) scores[dim] = scores.getOrDefault(dim, 0) + 1
        }
        return listOf(
            "外向/内向" to (scores["E"]!! to scores["I"]!!),
            "感觉/直觉" to (scores["S"]!! to scores["N"]!!),
            "思考/情感" to (scores["T"]!! to scores["F"]!!),
            "判断/感知" to (scores["J"]!! to scores["P"]!!)
        )
    }
}
