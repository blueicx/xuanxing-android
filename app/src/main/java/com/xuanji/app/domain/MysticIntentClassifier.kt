package com.xuanji.app.domain

/** Shared, deterministic intent normalization for Android and the offline dialogue seam. */
object MysticIntentClassifier {
    fun classify(question: String): MysticIntent {
        val q = question.trim().lowercase()
        val normalized = q.trimEnd('.', ',', '，', '。', '!', '！', '?', '？', '~', '～')
        return casual(normalized) ?: topic(q)
    }

    private fun casual(q: String): MysticIntent? = when {
        q in setOf("hi", "hello", "yo", "嗨", "哈喽", "哈罗", "你好", "您好", "在吗", "在么", "在不在") ||
            q.length <= 5 && setOf("你好", "您好", "哈喽", "哈罗", "嗨").any(q::startsWith) ||
            setOf("你好呀", "您好呀", "早上好", "早安", "午安", "下午好", "晚上好", "晚安", "最近怎么样", "最近如何").any(q::contains) -> MysticIntent.Greeting
        q == "走了" || setOf("拜拜", "再见", "回见", "睡了", "去忙").any(q::contains) -> MysticIntent.Farewell
        setOf("谢谢", "多谢", "感谢", "辛苦了", "thanks", "thank you").any(q::contains) -> MysticIntent.Thanks
        setOf("你是谁", "你叫什么", "叫什么名字", "什么名字", "介绍一下自己", "你是什么人", "你是神仙吗").any(q::contains) -> MysticIntent.Identity
        setOf("吃了吗", "干嘛呢", "在干嘛", "在忙吗", "无聊", "陪我聊", "陪我聊聊", "会说吗", "随便聊聊", "陪我说话", "今天心情").any(q::contains) -> MysticIntent.Smalltalk
        else -> null
    }

    private fun topic(q: String): MysticIntent = when {
        containsAny(q, "运势", "运气", "占卜", "算命", "算一卦", "起卦", "盘面", "综合分", "今天运") -> MysticIntent.Fortune
        containsAny(q, "焦虑", "压力", "害怕", "担心", "难过", "崩溃", "很累", "内耗") -> MysticIntent.Mood
        containsAny(q, "感情", "恋爱", "对象", "复合", "暗恋", "表白", "桃花", "分手", "他", "她") -> MysticIntent.Love
        containsAny(q, "财", "钱", "赚钱", "投资", "生意", "消费", "钱包") -> MysticIntent.Wealth
        containsAny(q, "工作", "上班", "事业", "老板", "同事", "面试", "升职", "跳槽") -> MysticIntent.Career
        containsAny(q, "学习", "考试", "复习", "作业", "论文", "背", "题") -> MysticIntent.Study
        containsAny(q, "健康", "身体", "睡觉", "睡眠", "失眠", "生病", "累") -> MysticIntent.Health
        containsAny(q, "为什么", "怎么来", "怎么算", "依据", "来源", "多少分") -> MysticIntent.Why
        containsAny(q, "留意", "注意", "风险", "小心", "避免", "坑") -> MysticIntent.Care
        containsAny(q, "能不能", "会不会", "可不可以", "行不行", "成不成", "该不该") -> MysticIntent.Outcome
        containsAny(q, "什么时候", "几点", "哪天", "现在适合", "今天适合", "怎么做", "怎么办", "如何", "建议", "行动", "开始", "计划", "破", "解") -> MysticIntent.Action
        containsAny(q, "饿", "吃", "外卖", "天气", "下雨", "热死", "冷", "困") -> MysticIntent.Daily
        else -> MysticIntent.Chat
    }

    private fun containsAny(value: String, vararg needles: String): Boolean = needles.any(value::contains)
}
