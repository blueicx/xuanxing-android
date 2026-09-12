package com.xuanji.app.domain

/** Pure topic answer wording. Facts are supplied by the caller; this object never invents scores. */
object MysticTopicAnswerTemplates {
    fun answer(
        scholar: Boolean,
        topicKey: String,
        label: String,
        focusScore: Int,
        highLabel: String,
        lowLabel: String,
        testName: String
    ): String {
        val strong = focusScore >= 65
        val mid = focusScore >= 35 && !strong
        val opener = when {
            strong -> "「$label」有空间"
            mid -> "「$label」适合小步走"
            else -> "「$label」要先减负"
        }
        return if (scholar) {
            val tail = when (topicKey) {
                "composite" -> "把注意力放在「$highLabel」，同时给「$lowLabel」留缓冲。"
                "career" -> "挑一件最重要的事推进，沟通时把需求说清楚，比同时开五个头更有力。"
                "love" -> "少一点猜测，多一点具体表达；关系里的安全感的来源之一是把话说开。"
                "wealth" -> "先守住必要支出，再考虑尝试；金额越小，决策越清醒。"
                "study" -> "把目标切成二十五分钟的小段，先完成一次回顾，再谈突破。"
                "health" -> "优先睡眠、饮食和活动量；身体信号值得被认真对待。"
                "test" -> "可以把「${testName.ifBlank { "最近测试" }}」当自我观察材料，与命盘互相参照，不单独下结论。"
                else -> "结合「$highLabel」推进，同时照看「$lowLabel」。"
            }
            "$opener。$tail"
        } else {
            val tail = when (topicKey) {
                "composite" -> "「$highLabel」举火把，「$lowLabel」坐轿子，路线已经很清楚啦！"
                "career" -> "主打一招，别十八般武艺同时抡；把关键话说漂亮，胜过加班到冒烟。"
                "love" -> "直球可以扔，阴阳怪气快收起来；具体说想要什么，才不会被误会的云雾罩住。"
                "wealth" -> "钱包系好绳，小额定投快乐可以，大额冲动先冷冻三天。"
                "study" -> "番茄钟启动！先把最烦的那块啃一小口，成就感会自动续杯。"
                "health" -> "仙体也要保养：早点躺，好好吃，动一动，别和沙发签订永久契约。"
                "test" -> "「${testName.ifBlank { "最近测试" }}」只是镜子，不是审判书；拿来认识自己刚刚好。"
                else -> "让「$highLabel」打头阵，别把「$lowLabel」丢在后山。"
            }
            "$opener！$tail"
        }
    }
}
