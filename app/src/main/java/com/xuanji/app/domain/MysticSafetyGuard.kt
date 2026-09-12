package com.xuanji.app.domain

/** 只有这两类提问会踩到「给结论」红线；其余一律放行。 */
enum class SafetyDomain { None, Health, Finance }

/**
 * 自由提问里的医疗与财务结论拦截。
 *
 * 词表刻意不复用 MysticIntentClassifier：分类器按主题分流，「我该吃什么药」会命中
 * 句里的「吃」而落到 Daily（MysticIntentClassifier.kt:61），红线不能押在路由运气上。
 */
object MysticSafetyGuard {

    const val HEALTH_DISCLAIMER = "不构成医疗建议，如有不适请咨询专业人士。"
    const val FINANCE_DISCLAIMER = "不构成投资建议，请量力而行。"

    /** 助手不得替用户下的结论，无论问题怎么问。 */
    val FORBIDDEN: List<String> = listOf(
        "你患有", "你已经得", "确诊", "诊断为", "处方", "剂量", "建议服用",
        "保证收益", "稳赚", "推荐买", "建议买入", "全仓", "必赢"
    )

    private val HEALTH_MARKERS = listOf(
        "病", "症", "癌", "肿瘤", "健康", "症状", "发烧", "感冒", "咳嗽", "头疼", "头痛",
        "疼", "痛", "血压", "血糖", "甲状腺", "抑郁", "焦虑症", "体检", "医院", "药",
        "处方", "剂量", "确诊", "诊断", "怀孕"
    )
    private val HEALTH_VERDICTS = listOf(
        "是不是", "得了", "什么药", "吃什么", "吃药", "用药", "剂量", "该不该吃", "要不要去",
        "怎么治", "治好", "根治", "严重吗", "确诊", "诊断", "处方"
    )
    private val FINANCE_MARKERS = listOf(
        "股票", "基金", "期货", "比特币", "币圈", "买房", "投资", "理财", "财运", "赚钱",
        "仓位", "全仓", "加仓", "杠杆", "贷款", "生意", "开店", "本金", "收益"
    )
    private val FINANCE_VERDICTS = listOf(
        "买哪", "能不能买", "该不该买", "会不会涨", "能赚", "赚多少", "翻倍", "稳赚", "保证",
        "推荐", "全仓", "加仓", "清仓", "上车", "该不该投", "值不值得投", "借给"
    )

    /** 这句落在哪个敏感域：只管「要不要补免责句」，不管要不要拒答。 */
    fun domainOf(question: String): SafetyDomain {
        val text = question.trim()
        if (text.isEmpty()) return SafetyDomain.None
        val healthHits = HEALTH_MARKERS.count { text.contains(it) }
        val financeHits = FINANCE_MARKERS.count { text.contains(it) }
        return when {
            healthHits == 0 && financeHits == 0 -> SafetyDomain.None
            healthHits > financeHits -> SafetyDomain.Health
            financeHits > healthHits -> SafetyDomain.Finance
            // 命中数相同则按先出现的一方处理，保持同一问题永远同一判定。
            else -> if (firstHitAt(text, HEALTH_MARKERS) <= firstHitAt(text, FINANCE_MARKERS))
                SafetyDomain.Health else SafetyDomain.Finance
        }
    }

    /**
     * 用户要的是结论还是参考？结论式提问整句换成拒答。
     *
     * 词必须成对命中（域词 + 结论词），否则「我是不是该辞职」这种日常犹豫也会被当成看病。
     */
    fun verdictDomainOf(question: String): SafetyDomain {
        val text = question.trim()
        val healthAsk = HEALTH_MARKERS.any { text.contains(it) } && HEALTH_VERDICTS.any { text.contains(it) }
        val financeAsk = FINANCE_MARKERS.any { text.contains(it) } && FINANCE_VERDICTS.any { text.contains(it) }
        return when {
            healthAsk && !financeAsk -> SafetyDomain.Health
            financeAsk && !healthAsk -> SafetyDomain.Finance
            !healthAsk -> SafetyDomain.None
            // 两边都在要结论，按先开口的那一类拒。
            else -> if (firstHitAt(text, HEALTH_VERDICTS) <= firstHitAt(text, FINANCE_VERDICTS))
                SafetyDomain.Health else SafetyDomain.Finance
        }
    }

    fun disclaimerFor(domain: SafetyDomain): String = when (domain) {
        SafetyDomain.Health -> HEALTH_DISCLAIMER
        SafetyDomain.Finance -> FINANCE_DISCLAIMER
        SafetyDomain.None -> ""
    }

    /** 结论式提问整句换成拒答；两句一组，同一问题永远得到同一句。 */
    fun refusal(mode: String, domain: SafetyDomain, variant: Int): String {
        val scholar = mode != "half"
        val half = variant % 2 == 1
        val line = when {
            scholar && domain == SafetyDomain.Health && !half -> "这句我不接——判病不是我的活儿。"
            scholar && domain == SafetyDomain.Health -> "药和结论都得医生签字，我只看着盘面。"
            scholar && !half -> "钱的事我不给结论，也不替谁背书。"
            scholar -> "涨跌我说了不算，你也不该听别人的。"
            !scholar && domain == SafetyDomain.Health && !half -> "看病开方不在我这一行，街口能治百病的都别信。"
            !scholar && domain == SafetyDomain.Health -> "我看的是势，不是你的病历。"
            !scholar && !half -> "包赚的话别人抢着说，我这儿没有。"
            else -> "让我荐一只，等于把刀递给你，这钱我不挣。"
        }
        return line + disclaimerFor(domain)
    }

    /** customAnswer 的唯一出口：先整句拒结论，再给非结论的健康/财务回答补上免责句。 */
    fun enforce(mode: String, question: String, variant: Int, draft: String): String {
        val verdict = verdictDomainOf(question)
        if (verdict != SafetyDomain.None) return refusal(mode, verdict, variant)
        val disclaimer = disclaimerFor(domainOf(question))
        return if (disclaimer.isEmpty() || draft.contains(disclaimer)) draft else draft + disclaimer
    }

    private fun firstHitAt(text: String, words: List<String>): Int =
        words.mapNotNull { text.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
}
