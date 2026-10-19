package com.xuanji.app.domain.action

import com.xuanji.app.data.model.Element

internal object ExpandedActionCatalog {
    val activities: List<ActivityCandidate> = """
        desk-reset|整理桌面的一角|5|15|METAL|Low|true|上午|career
        short-reading|读一篇短文并记一句话|5|15|METAL|Low|true|午后|study
        tea-break|泡一杯茶并离屏休息|5|15|WATER|Low|true|午后|emotion
        stretch-break|做舒适范围内的轻柔伸展|5|10|WOOD|Low|true|下午|health
        music-listen|专心听一首器乐曲|5|10|WATER|Low|true|晚间|emotion
        gratitude-note|写下一件值得感谢的小事|5|10|EARTH|Low|true|晚间|emotion
        tomorrow-list|列出明天最重要的三件事|5|15|METAL|Low|true|晚间|career
        window-rest|在窗边观察远处景物|5|10|WOOD|Low|true|午后|health
        plant-care|给盆栽做一次日常检查|10|20|WOOD|Low|true|上午|emotion
        photo-sort|整理一组自己拍摄的照片|10|25|METAL|Low|true|下午|study
        poem-copy|抄写一首公版诗并读出节奏|10|20|WATER|Low|true|晚间|study
        skill-review|复习一个熟悉的技能要点|15|25|METAL|Medium|true|上午|study
        meal-prep|准备一份简单家常餐|15|30|EARTH|Medium|true|午后|health
        music-practice|练习一段熟悉的旋律|15|30|FIRE|Medium|true|下午|study
        paper-craft|完成一件小型纸艺|15|30|WOOD|Medium|true|下午|emotion
        bookshelf-sort|按主题整理一层书架|15|30|METAL|Medium|true|午后|career
        letter-write|给熟人写一段真诚的话|10|25|EARTH|Low|true|晚间|love
        board-study|研究一局棋的关键转折|20|40|METAL|Medium|true|晚间|study
        neighborhood-walk|在熟悉街区慢走|15|30|WOOD|Low|false|傍晚|health
        park-sketch|在公园画一张观察速写|20|45|WOOD|Medium|false|下午|emotion
        garden-visit|观察一处花园的季节变化|20|40|WOOD|Medium|false|上午|emotion
        market-walk|逛一段本地市集|20|45|EARTH|Medium|false|上午|emotion
        cycle-easy|在熟悉安全路段轻松骑行|30|50|FIRE|High|false|上午|health
        hill-walk|选择熟悉步道做短途步行|40|80|WOOD|High|false|上午|health
        group-project|与伙伴完成一项小协作|30|60|EARTH|High|true|下午|career
        long-practice|完成一段专注创作练习|45|90|FIRE|High|true|下午|study
    """.trimIndent().lines().map { line ->
        val f = line.trim().split('|')
        val element = Element.valueOf(f[4])
        ActivityCandidate(f[0], f[1], f[2].toInt()..f[3].toInt(), f[7],
            "按可用时间和舒适程度调整，可随时停止", setOf(element), setOf(element), setOf(f[8]),
            setOf("spring", "summer", "autumn", "winter"), EnergyLevel.valueOf(f[5]), f[6].toBooleanStrict())
    }

    val outings: List<OutingCandidate> = listOf(
        venue("library", "公共图书馆阅读区", "quiet", Element.METAL, true, "适合用一段安静时间阅读和整理"),
        venue("tea-room", "安静茶室", "tea", Element.WATER, true, "可以留出缓冲时间与熟人轻松交流"),
        venue("art-gallery", "室内艺术展厅", "culture", Element.FIRE, true, "以一两件作品作为观察主题"),
        venue("craft-room", "室内手作空间", "culture", Element.WOOD, true, "把好奇心转成一件具体的小作品"),
        venue("music-room", "室内音乐空间", "culture", Element.FIRE, true, "用完整聆听代替赶场打卡"),
        venue("covered-market", "有顶棚的市集", "food", Element.EARTH, true, "观察当地日常生活与食材"),
        venue("historic-lane", "历史街巷", "history", Element.EARTH, false, "按步行时间选择短段街区"),
        venue("garden", "园林庭院", "green", Element.WOOD, false, "留意植物与空间层次的变化"),
        venue("coastal-path", "海滨步道", "coast", Element.WATER, false, "选择熟悉的短段步道放慢节奏"),
        venue("city-square", "城市公共广场", "urban", Element.FIRE, false, "给日常路线增加一个停留点")
    )

    private fun venue(key: String, title: String, tag: String, element: Element, indoor: Boolean, reason: String) =
        OutingCandidate(key, title, reason, setOf(tag), setOf(element), setOf(element),
            setOf("spring", "summer", "autumn", "winter"), indoor)
}
