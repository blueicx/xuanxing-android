package com.xuanji.app.domain.test

data class IpipItem(val text: String, val scale: Int, val reverse: Boolean)
data class IpipScaleScore(val scale: String, val raw: Int, val mean: Double?, val percentile: Int?)
data class IpipResult(val scores: List<IpipScaleScore>, val instrumentId: String = "ipip-big5-50")

/** Goldberg 50-item IPIP markers. Items/scales are public-domain; no commercial norm is implied. */
object IpipBigFive {
    private val texts = listOf(
        "Am the life of the party.", "Feel little concern for others.", "Am always prepared.", "Get stressed out easily.", "Have a rich vocabulary.",
        "Don't talk a lot.", "Am interested in people.", "Leave my belongings around.", "Am relaxed most of the time.", "Have difficulty understanding abstract ideas.",
        "Feel comfortable around people.", "Insult people.", "Pay attention to details.", "Worry about things.", "Have a vivid imagination.",
        "Keep in the background.", "Sympathize with others' feelings.", "Make a mess of things.", "Seldom feel blue.", "Am not interested in abstract ideas.",
        "Start conversations.", "Am not interested in other people's problems.", "Get chores done right away.", "Am easily disturbed.", "Have excellent ideas.",
        "Have little to say.", "Have a soft heart.", "Often forget to put things back in their proper place.", "Get upset easily.", "Do not have a good imagination.",
        "Talk to a lot of different people at parties.", "Am not really interested in others.", "Like order.", "Change my mood a lot.", "Am quick to understand things.",
        "Don't like to draw attention to myself.", "Take time out for others.", "Shirk my duties.", "Have frequent mood swings.", "Use difficult words.",
        "Don't mind being the center of attention.", "Feel others' emotions.", "Follow a schedule.", "Get irritated easily.", "Spend time reflecting on things.",
        "Am quiet around strangers.", "Make people feel at ease.", "Am exacting in my work.", "Often feel blue.", "Am full of ideas."
    )
    // scale 1=E, 2=A, 3=C, 4=emotional stability, 5=intellect/imagination
    private val keys = listOf("1+","2-","3+","4-","5+","1-","2+","3-","4+","5-","1+","2-","3+","4-","5+","1-","2+","3-","4+","5-","1+","2-","3+","4-","5+","1-","2+","3-","4-","5-","1+","2-","3+","4-","5+","1-","2+","3-","4-","5+","1+","2+","3+","4-","5+","1-","2+","3+","4-","5+")
    val items: List<IpipItem> = texts.mapIndexed { i, text -> IpipItem(text, keys[i].dropLast(1).toInt(), keys[i].endsWith('+').not()) }
    private val names = mapOf(1 to "外向性", 2 to "宜人性", 3 to "尽责性", 4 to "情绪稳定性", 5 to "智性/想象力")

    fun score(answers: List<Int>, norm: NormativeSample? = null): IpipResult {
        require(answers.size == items.size) { "IPIP-50 需要 50 个答案" }
        require(answers.all { it in 1..5 }) { "IPIP-50 每个答案必须为 1..5" }
        val sums = (1..5).associateWith { 0 }.toMutableMap()
        answers.forEachIndexed { i, answer ->
            val item = items[i]
            sums[item.scale] = sums.getValue(item.scale) + if (item.reverse) 6 - answer else answer
        }
        val scores = (1..5).map { scale ->
            val raw = sums.getValue(scale)
            IpipScaleScore(names.getValue(scale), raw, norm?.meanByScale?.get(names.getValue(scale)), norm?.percentile(names.getValue(scale), raw.toDouble()))
        }
        return IpipResult(scores)
    }
}
