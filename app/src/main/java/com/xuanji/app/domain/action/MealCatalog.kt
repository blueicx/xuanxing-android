package com.xuanji.app.domain.action

import com.xuanji.app.data.model.Element

/** Self-authored home-cooking examples. Prices are ingredient estimates, not merchant quotations. */
internal object MealCatalog {
    private val breakfast = """
        oat-berry|燕麦蓝莓酸奶碗|燕麦,蓝莓,原味酸奶|900|8|WOOD|拌开燕麦与酸奶；洗净蓝莓后铺在上方
        lotus-lily-porridge|莲子百合小米粥|莲子,百合,小米|700|35|EARTH|莲子提前浸泡；与小米煮软后加入百合
        pumpkin-millet|南瓜小米粥|南瓜,小米|600|25|EARTH|南瓜切小块；与洗净的小米同煮至软
        egg-toast|鸡蛋全麦吐司|鸡蛋,全麦面包,番茄|800|10|FIRE|煎熟鸡蛋；夹入面包并搭配洗净番茄
        soy-oats|豆乳燕麦碗|豆乳,燕麦,香蕉|700|8|WOOD|燕麦与豆乳加热；加入香蕉片
        corn-sweet-potato|蒸玉米红薯|玉米,红薯|700|25|EARTH|洗净玉米红薯；蒸至完全熟透
        spinach-egg-roll|菠菜鸡蛋卷|菠菜,鸡蛋,面粉|800|15|WOOD|菠菜焯熟切碎；拌入蛋糊后煎熟卷起
        apple-yogurt|苹果酸奶麦片|苹果,原味酸奶,麦片|900|5|METAL|苹果洗净切丁；与酸奶麦片混合
        tomato-rice-soup|番茄米饭汤|番茄,熟米饭,青菜|600|12|FIRE|番茄加水煮开；加入米饭青菜煮透
        black-sesame-bun|黑芝麻馒头早餐|馒头,黑芝麻,牛奶|800|8|WATER|馒头蒸热；配黑芝麻和加热牛奶
        pear-oat-porridge|雪梨燕麦粥|雪梨,燕麦,温水|700|15|METAL|雪梨洗净切丁；与燕麦同煮至软
        mushroom-egg-sandwich|菌菇鸡蛋三明治|蘑菇,鸡蛋,全麦面包|1200|15|EARTH|蘑菇炒熟、鸡蛋煎熟；夹入全麦面包
        cucumber-rice-roll|黄瓜紫菜饭卷|黄瓜,紫菜,熟米饭|800|12|WATER|黄瓜洗净切条；将米饭铺在紫菜上包卷
        sweet-potato-yogurt|红薯酸奶碗|红薯,原味酸奶|800|25|EARTH|红薯蒸熟放凉；切块后拌入酸奶
        chicken-congee|鸡丝青菜粥|鸡胸肉,大米,青菜|1300|35|WOOD|鸡肉煮熟撕丝；加入煮软米粥及青菜煮透
        banana-pancake|香蕉鸡蛋松饼|香蕉,鸡蛋,面粉|800|15|EARTH|香蕉压泥拌蛋与面粉；小火煎至两面熟透
    """.trimIndent()
    private val lunch = """
        tomato-egg-noodle|番茄鸡蛋面|番茄,鸡蛋,面条,青菜|1200|15|FIRE|鸡蛋炒熟、番茄炒软；加水煮面与青菜
        halal-lamb-rice|孜然羊肉饭|羊肉,米饭,洋葱,孜然|2800|30|FIRE|羊肉洋葱炒熟；撒孜然后配熟米饭
        mushroom-tofu|菌菇豆腐煲|香菇,豆腐,青菜|1300|20|WATER|香菇炒香后加水；加入豆腐青菜炖熟
        chicken-potato-rice|土豆鸡肉饭|土豆,鸡胸肉,米饭|1800|25|EARTH|鸡肉土豆炖至熟软；盛到熟米饭上
        broccoli-egg-rice|西兰花鸡蛋饭|西兰花,鸡蛋,米饭|1100|15|WOOD|西兰花焯熟；与炒熟鸡蛋、米饭翻炒
        carrot-beef-noodle|胡萝卜牛肉面|胡萝卜,牛肉,面条|2400|35|EARTH|牛肉胡萝卜炖熟；加入面条煮透
        eggplant-chickpea|茄子鹰嘴豆饭|茄子,熟鹰嘴豆,米饭|1300|20|WATER|茄子炒熟；加入熟鹰嘴豆后配米饭
        corn-tofu-rice|玉米豆腐饭|玉米,豆腐,米饭|1000|15|EARTH|玉米豆腐炒熟；配热米饭
        shrimp-noodle|鲜虾青菜面|虾,青菜,面条|2200|20|WATER|虾与青菜煮熟；加入面条煮透
        lentil-tomato-rice|番茄扁豆饭|番茄,扁豆,米饭|1100|30|FIRE|扁豆煮至熟透；与炒软番茄搭配米饭
        cabbage-chicken-noodle|白菜鸡肉面|白菜,鸡胸肉,面条|1500|20|METAL|鸡肉煮熟；加入白菜和面条煮透
        pumpkin-chickpea|南瓜鹰嘴豆饭|南瓜,熟鹰嘴豆,米饭|1200|20|EARTH|南瓜蒸熟；加入熟鹰嘴豆后配米饭
        spinach-mushroom-rice|菠菜菌菇饭|菠菜,蘑菇,米饭|1000|18|WOOD|蘑菇炒熟；加入菠菜炒透后配米饭
        sesame-cold-noodle|芝麻黄瓜凉面|芝麻,黄瓜,面条|1100|15|METAL|面条煮熟放凉；拌洗净黄瓜与芝麻
        cabbage-egg-noodle|包菜鸡蛋炒面|包菜,鸡蛋,面条|1000|15|WOOD|面条煮熟沥水；与炒熟鸡蛋包菜翻炒
        chicken-soup-rice|鸡肉蔬菜汤饭|鸡胸肉,胡萝卜,米饭|1600|25|EARTH|鸡肉胡萝卜煮熟；加入熟米饭煮开
    """.trimIndent()
    private val dinner = """
        salmon-greens|柠檬三文鱼配烤蔬菜|三文鱼,西兰花,柠檬,南瓜|3500|30|WATER|鱼与蔬菜烤至熟透；上桌前加少量柠檬汁
        pepper-chicken|青椒鸡丁糙米饭|鸡胸肉,青椒,糙米|1900|30|WOOD|糙米煮熟；鸡肉青椒炒熟后配饭
        tofu-greens|青菜豆腐汤|豆腐,青菜,温水|900|12|WOOD|豆腐加水煮开；加入青菜煮透
        tomato-potato-soup|番茄土豆汤|番茄,土豆,温水|900|25|EARTH|番茄炒软；与土豆加水煮至熟软
        steamed-fish|清蒸鱼配青菜|鱼,青菜,生姜|2800|25|WATER|鱼加入姜片蒸熟；青菜另行焯熟
        mushroom-vermicelli|菌菇粉丝煲|蘑菇,粉丝,白菜|1100|20|WATER|粉丝泡软；与菌菇白菜加水煮透
        pumpkin-rice|南瓜焖饭|南瓜,大米,玉米|1000|30|EARTH|洗净大米；放入南瓜玉米加水焖熟
        cabbage-beef|包菜牛肉配饭|包菜,牛肉,米饭|2500|25|METAL|牛肉包菜炒熟；配熟米饭
        lemon-tofu|柠檬豆腐配烤蔬菜|豆腐,西兰花,柠檬,南瓜|1600|30|METAL|豆腐蔬菜烤熟；拌少量柠檬汁
        egg-corn-soup|鸡蛋玉米汤|鸡蛋,玉米,温水|800|15|EARTH|玉米加水煮熟；倒入蛋液煮至完全凝固
        zucchini-noodle|西葫芦鸡蛋面|西葫芦,鸡蛋,面条|1200|18|WOOD|西葫芦与鸡蛋炒熟；加入煮熟面条
        chickpea-vegetable-soup|鹰嘴豆蔬菜汤|熟鹰嘴豆,胡萝卜,白菜|1200|20|EARTH|胡萝卜白菜煮软；加入熟鹰嘴豆煮开
        potato-mushroom|土豆蘑菇焖饭|土豆,蘑菇,大米|1100|30|EARTH|大米洗净；与土豆蘑菇加水焖熟
        chicken-broccoli|西兰花鸡胸肉饭|鸡胸肉,西兰花,米饭|1800|20|WOOD|鸡胸肉煎熟；搭配焯熟西兰花与米饭
        tofu-cabbage-noodle|豆腐白菜汤面|豆腐,白菜,面条|1100|15|METAL|白菜豆腐加水煮开；加入面条煮透
        tomato-lentil-soup|番茄红扁豆汤|番茄,红扁豆,胡萝卜|1200|30|FIRE|红扁豆煮透；加入番茄胡萝卜同煮至软
    """.trimIndent()
    private val drinks = """
        ginger-tea|姜枣热饮|生姜,红枣,温水|400|12|FIRE|红枣洗净去核；与姜片加水煮开
        lemon-water|柠檬水|柠檬,饮用水|300|3|METAL|柠檬洗净切片；放入饮用水
        pear-drink|雪梨热饮|雪梨,温水|500|15|METAL|雪梨洗净切块；加水煮软
        apple-cinnamon|苹果肉桂饮|苹果,肉桂,温水|500|15|FIRE|苹果洗净切块；加少量肉桂与水煮开
        mint-water|薄荷清水饮|薄荷,饮用水|300|5|WOOD|薄荷洗净；放入饮用水
        cucumber-water|黄瓜水|黄瓜,饮用水|200|3|WOOD|黄瓜洗净切片；浸入饮用水
        soy-milk|原味豆浆|豆浆|400|5|EARTH|使用已煮熟豆浆；按个人口味加热
        oat-milk|燕麦饮|燕麦饮|500|3|EARTH|选无酒精燕麦饮；摇匀后饮用
        warm-milk|温牛奶|牛奶|500|4|METAL|牛奶倒入杯中；温热后饮用
        rooibos|路易波士茶|路易波士茶,温水|400|8|FIRE|茶叶放入茶具；加热水浸泡后滤出
        barley-drink|大麦茶|大麦茶,温水|300|8|EARTH|大麦茶用热水浸泡；滤出后饮用
        hibiscus|洛神花饮|洛神花,温水|500|8|FIRE|洛神花热水浸泡；滤出后饮用
        orange-water|橙片水|橙子,饮用水|400|3|FIRE|橙子洗净切片；放入饮用水
        berry-water|蓝莓果饮|蓝莓,饮用水|700|5|WATER|蓝莓洗净略压碎；加饮用水搅匀
        peach-drink|桃子果饮|桃子,饮用水|600|5|WOOD|桃子洗净去核；与饮用水搅拌
        plain-water|温水|饮用水|100|2|WATER|取可直接饮用的水；按个人习惯调温
    """.trimIndent()

    val all: List<MealCandidate> = listOf(
        MealSlot.Breakfast to breakfast, MealSlot.Lunch to lunch,
        MealSlot.Dinner to dinner, MealSlot.Drink to drinks
    ).flatMap { (slot, rows) -> rows.lines().filter(String::isNotBlank).map { line ->
        val f = line.trim().split('|')
        val ingredients = f[2].split(',')
        val element = Element.valueOf(f[5])
        val allergens = buildSet {
            if (ingredients.any { it.contains("牛奶") || it.contains("酸奶") }) add("乳")
            if (ingredients.any { it.contains("鸡蛋") }) add("蛋")
            if (ingredients.any { it.contains("豆腐") || it.contains("豆乳") || it.contains("豆浆") }) add("大豆")
            if (ingredients.any { it.contains("面") || it.contains("麦片") || it.contains("大麦") }) add("麸质")
            if (ingredients.any { it.contains("鱼") }) add("鱼")
            if (ingredients.any { it.contains("虾") }) add("甲壳类")
            if (ingredients.any { it.contains("芝麻") }) add("芝麻")
        }
        MealCandidate(
            f[0], slot, f[1], ingredients, "当前约束下无同餐次替代候选", listOf(f[1]) + ingredients,
            setOf(element), setOf(element), seasons(element),
            vegetarian = ingredients.none { it in setOf("鸡胸肉", "羊肉", "牛肉", "三文鱼", "鱼", "虾") },
            halalCompatible = true, spicy = false, alcohol = false,
            estimatedPriceCents = f[3].toInt(), prepMinutes = f[4].toInt(), allergenTags = allergens,
            steps = f[6].split('；'),
            requiredEquipment = when (f[0]) {
                in coldPreparation -> emptySet()
                "salmon-greens", "lemon-tofu" -> setOf(KitchenEquipment.Oven)
                "corn-sweet-potato", "black-sesame-bun", "sweet-potato-yogurt", "steamed-fish", "pumpkin-chickpea" -> setOf(KitchenEquipment.Steamer)
                "pumpkin-rice", "potato-mushroom" -> setOf(KitchenEquipment.RiceCooker)
                else -> setOf(KitchenEquipment.Stove)
            },
            difficulty = if (f[0] in moderatePreparation) CookingDifficulty.Moderate else CookingDifficulty.Easy,
            lunchboxSuitable = f[0] in lunchboxMeals
        )
    } }

    // Editorial metadata for these exact recipes, not inferred user abilities or food-safety claims.
    private val coldPreparation get() = setOf("oat-berry", "apple-yogurt", "cucumber-rice-roll", "lemon-water",
        "mint-water", "cucumber-water", "oat-milk", "orange-water", "berry-water", "peach-drink", "plain-water")
    private val moderatePreparation get() = setOf("spinach-egg-roll", "banana-pancake", "halal-lamb-rice",
        "carrot-beef-noodle", "salmon-greens", "steamed-fish", "cucumber-rice-roll")
    private val lunchboxMeals get() = setOf("egg-toast", "corn-sweet-potato", "apple-yogurt", "mushroom-egg-sandwich",
        "cucumber-rice-roll", "banana-pancake", "halal-lamb-rice", "chicken-potato-rice", "broccoli-egg-rice",
        "eggplant-chickpea", "corn-tofu-rice", "lentil-tomato-rice", "pumpkin-chickpea", "spinach-mushroom-rice",
        "cabbage-egg-noodle", "pepper-chicken", "pumpkin-rice", "cabbage-beef", "potato-mushroom", "chicken-broccoli")

    private fun seasons(element: Element): Set<String> = when (element) {
        Element.WOOD -> setOf("spring", "summer")
        Element.FIRE -> setOf("summer", "autumn")
        Element.EARTH -> setOf("autumn", "winter")
        Element.METAL -> setOf("autumn", "spring")
        Element.WATER -> setOf("winter", "summer")
    }
}
