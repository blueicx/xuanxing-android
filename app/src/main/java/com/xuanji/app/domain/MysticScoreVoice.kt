package com.xuanji.app.domain

/** Legacy score wording, separate from input analysis, factual grounding and character identity. */
internal object MysticScoreVoice {
    fun scholarHeadline(score: Int, label: String): String = when {
        score >= 80 -> "${label}有势能，你可以安心接住"
        score >= 65 -> "${label}方向清楚，节奏可以温柔些"
        score >= 50 -> "${label}正在蓄力，不必逼它开花"
        score >= 35 -> "${label}需要小步确认，而不是大步证明"
        else -> "先把${label}安顿好，再安排世界"
    }
    fun halfHeadline(score: Int, label: String): String = when {
        score >= 80 -> "不得了！${label}直接踩着祥云起飞"
        score >= 65 -> "${label}火力在线，神仙都要侧目"
        score >= 50 -> "${label}稳如老君炉，别慌"
        score >= 35 -> "${label}有点闹脾气，得哄"
        else -> " ${label}暂时躲进云里充电了"
    }
    fun bandSentence(score: Int): String = when {
        score >= 80 -> "现在的关键不是怀疑机会，而是把注意力放在能让你稳定发挥的选择上。"
        score >= 65 -> "推进是合适的，只是把期待拆成几个可完成的小节点，会更轻松。"
        score >= 50 -> "平稳不代表平淡，它给你空间整理节奏、修补细节。"
        score >= 35 -> "低分不是否定，而是身体和情绪在提醒你收缩战线。"
        else -> "此刻最有效的行动是休息、求助和把任务缩小到不会吓跑自己的程度。"
    }
    fun halfBandSentence(score: Int): String = when {
        score >= 80 -> "这分数都快溢出八卦炉了，好运追着你跑，记得留个门！"
        score >= 65 -> "运势小火苗烧得很旺，适合把计划端上桌，别让它干等！"
        score >= 50 -> "不惊不喜，像一碗温吞仙汤，喝完照样能走路带风。"
        score >= 35 -> "星星在天上挤眉弄眼：今天别硬闯，绕个路更灵光！"
        else -> "云层信号有点差，宜躺平回血，不宜跟命运掰手腕！"
    }
}
