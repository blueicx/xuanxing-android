package com.xuanji.app.domain.action

import com.xuanji.app.data.model.Element

internal data class MealCandidate(
    val key: String,
    val slot: MealSlot,
    val title: String,
    val ingredients: List<String>,
    val substitute: String,
    val deliveryKeywords: List<String>,
    val elementTags: Set<Element>,
    val zodiacTags: Set<Element>,
    val seasonTags: Set<String>,
    val vegetarian: Boolean,
    val halalCompatible: Boolean,
    val spicy: Boolean,
    val alcohol: Boolean,
    val estimatedPriceCents: Int = 5000,
    val prepMinutes: Int = 20,
    val allergenTags: Set<String> = emptySet(),
    val steps: List<String> = emptyList(),
    val requiredEquipment: Set<KitchenEquipment> = emptySet(),
    val difficulty: CookingDifficulty = CookingDifficulty.Easy,
    val lunchboxSuitable: Boolean = false
)

internal data class ActivityCandidate(
    val key: String,
    val title: String,
    val durationMinutes: IntRange,
    val bestPeriod: String,
    val avoid: String?,
    val elementTags: Set<Element>,
    val zodiacTags: Set<Element>,
    val fortuneKeys: Set<String>,
    val seasonTags: Set<String>,
    val energy: EnergyLevel = EnergyLevel.Medium,
    val indoor: Boolean = true
)

internal data class OutingCandidate(
    val key: String,
    val placeType: String,
    val reason: String,
    val cityTags: Set<String>,
    val elementTags: Set<Element>,
    val zodiacTags: Set<Element>,
    val seasonTags: Set<String>,
    val indoor: Boolean = false
)

internal data class CareerCandidate(
    val key: String,
    val label: String,
    val elementTags: Set<Element>,
    val zodiacTags: Set<Element>,
    val testTags: Set<String>,
    val environmentTags: Set<String>
)

internal data class ColorCandidate(
    val key: String,
    val label: String,
    val hex: String,
    val elementTags: Set<Element>,
    val zodiacTags: Set<Element>,
    val testTags: Set<String>
)

object CityProfileCatalog {
    val profiles: List<CityProfile> = listOf(
        CityProfile("shanghai", "上海", setOf("waterfront", "culture", "night", "urban"), "中国", "上海"),
        CityProfile("beijing", "北京", setOf("culture", "history", "urban", "dry"), "中国", "北京"),
        CityProfile("chengdu", "成都", setOf("green", "food", "slow", "culture"), "中国", "成都"),
        CityProfile("hangzhou", "杭州", setOf("waterfront", "green", "tea", "slow"), "中国", "杭州"),
        CityProfile("kyoto", "京都", setOf("culture", "green", "quiet", "history"), "日本", "京都"),
        CityProfile("lisbon", "里斯本", setOf("waterfront", "sun", "culture", "slow"), "葡萄牙", "里斯本"),
        CityProfile("helsinki", "赫尔辛基", setOf("waterfront", "quiet", "forest", "cool"), "芬兰", "赫尔辛基"),
        CityProfile("melbourne", "墨尔本", setOf("culture", "green", "urban", "coast"), "澳大利亚", "墨尔本"),
        CityProfile("suzhou", "苏州", setOf("waterfront", "culture", "green", "slow"), "中国", "苏州"),
        CityProfile("nanjing", "南京", setOf("history", "culture", "green", "urban"), "中国", "南京"),
        CityProfile("xian", "西安", setOf("history", "culture", "dry", "food"), "中国", "西安"),
        CityProfile("guangzhou", "广州", setOf("food", "urban", "green", "warm"), "中国", "广州"),
        CityProfile("shenzhen", "深圳", setOf("urban", "coast", "green", "warm"), "中国", "深圳"),
        CityProfile("qingdao", "青岛", setOf("coast", "waterfront", "culture", "cool"), "中国", "青岛"),
        CityProfile("xiamen", "厦门", setOf("coast", "food", "slow", "warm"), "中国", "厦门"),
        CityProfile("kunming", "昆明", setOf("green", "slow", "culture", "mild"), "中国", "昆明"),
        CityProfile("tokyo", "东京", setOf("urban", "culture", "food", "night"), "日本", "东京"),
        CityProfile("seoul", "首尔", setOf("urban", "culture", "history", "night"), "韩国", "首尔"),
        CityProfile("singapore", "新加坡", setOf("urban", "green", "warm", "food"), "新加坡", "新加坡"),
        CityProfile("vienna", "维也纳", setOf("culture", "history", "quiet", "urban"), "奥地利", "维也纳"),
        CityProfile("prague", "布拉格", setOf("history", "culture", "waterfront", "quiet"), "捷克", "布拉格"),
        CityProfile("amsterdam", "阿姆斯特丹", setOf("waterfront", "culture", "urban", "cool"), "荷兰", "阿姆斯特丹"),
        CityProfile("vancouver", "温哥华", setOf("coast", "forest", "green", "cool"), "加拿大", "温哥华"),
        CityProfile("wellington", "惠灵顿", setOf("coast", "culture", "quiet", "cool"), "新西兰", "惠灵顿")
    )

    val defaultCity: CityProfile get() = profiles.first()

    fun find(keyOrLabel: String?): CityProfile? {
        if (keyOrLabel.isNullOrBlank()) return null
        return profiles.firstOrNull { it.key == keyOrLabel || it.label == keyOrLabel || it.city == keyOrLabel }
    }
}

internal object ActionCatalog {
    val meals: List<MealCandidate> = MealCatalog.all

    val activities: List<ActivityCandidate> = listOf(
        ActivityCandidate("green-walk", "在绿地慢走", 25..40, "傍晚", "避免把散步变成冲刺训练", setOf(Element.WOOD), setOf(Element.WOOD), setOf("health", "emotion"), setOf("spring", "summer"), EnergyLevel.Low, indoor = false),
        ActivityCandidate("focused-reading", "做一段 45 分钟的专注阅读", 35..55, "上午", "先关掉高干扰通知", setOf(Element.METAL), setOf(Element.METAL), setOf("study", "career"), setOf("autumn", "winter")),
        ActivityCandidate("creative-sketch", "完成一页速写或音乐练习", 20..45, "下午", "不追求一次成稿", setOf(Element.WOOD, Element.FIRE), setOf(Element.FIRE), setOf("emotion", "study"), setOf("spring", "summer")),
        ActivityCandidate("declutter", "整理一个抽屉并清掉三件不用物品", 20..35, "午后", "只处理可立即决定的物品", setOf(Element.METAL, Element.EARTH), setOf(Element.METAL), setOf("career", "health"), setOf("autumn", "winter")),
        ActivityCandidate("water-rest", "去临水处做无屏幕休息", 15..30, "傍晚", "不把休息改成刷手机", setOf(Element.WATER), setOf(Element.WATER), setOf("emotion", "health"), setOf("summer", "winter"), EnergyLevel.Low, indoor = false),
        ActivityCandidate("shared-meal", "和一位熟人吃饭或通话", 30..90, "晚间", "不强迫自己参加大型社交", setOf(Element.EARTH), setOf(Element.EARTH), setOf("love", "emotion"), setOf("spring", "summer", "autumn", "winter"))
    ) + ExpandedActionCatalog.activities

    val outings: List<OutingCandidate> = listOf(
        OutingCandidate("waterfront", "临水步道或江河公园", "水边空间能给今天留出缓冲和换气的余地", setOf("waterfront", "coast"), setOf(Element.WATER), setOf(Element.WATER), setOf("summer", "winter")),
        OutingCandidate("culture", "博物馆、书店或小型展览", "文化空间适合把好奇心转成可带走的线索", setOf("culture", "history"), setOf(Element.METAL), setOf(Element.METAL), setOf("autumn", "winter"), indoor = true),
        OutingCandidate("green", "植物园、山林或城市绿地", "绿色环境适合放慢步幅，把注意力放回身体感受", setOf("green", "forest"), setOf(Element.WOOD), setOf(Element.WOOD), setOf("spring", "summer")),
        OutingCandidate("food", "本地市集或茶馆", "有烟火气但不必安排高强度路线", setOf("food", "tea", "slow"), setOf(Element.EARTH), setOf(Element.EARTH), setOf("spring", "autumn")),
        OutingCandidate("night", "夜间灯光街区或安静咖啡馆", "适合在一天收尾时保留一点轻松的社交感", setOf("night", "urban"), setOf(Element.FIRE), setOf(Element.FIRE), setOf("summer", "autumn")),
        OutingCandidate("quiet", "安静的海边、湖边或林间座椅", "如果今天更需要恢复，低刺激环境比打卡数量重要", setOf("quiet", "cool"), setOf(Element.WATER), setOf(Element.WATER), setOf("winter", "autumn"))
    ) + ExpandedActionCatalog.outings

    val careers: List<CareerCandidate> = listOf(
        CareerCandidate("creative", "创意表达与内容", setOf(Element.WOOD, Element.FIRE), setOf(Element.WOOD, Element.FIRE), setOf("艺术", "开放", "内容", "创意"), setOf("culture", "green")),
        CareerCandidate("research", "研究分析与知识整理", setOf(Element.METAL, Element.WATER), setOf(Element.METAL, Element.WATER), setOf("研究", "分析", "理性", "学习"), setOf("quiet", "culture")),
        CareerCandidate("people", "教育、咨询与协作", setOf(Element.EARTH, Element.WATER), setOf(Element.EARTH, Element.WATER), setOf("社会", "教育", "沟通", "共情"), setOf("slow", "culture")),
        CareerCandidate("operations", "项目运营与组织管理", setOf(Element.EARTH, Element.METAL), setOf(Element.EARTH, Element.METAL), setOf("管理", "执行", "现实", "组织"), setOf("urban", "history"))
    )

    val colors: List<ColorCandidate> = listOf(
        ColorCandidate("jade", "玉青", "#7BB8A4", setOf(Element.WOOD), setOf(Element.WOOD), setOf("开放", "自然")),
        ColorCandidate("vermilion", "朱砂", "#C85A4A", setOf(Element.FIRE), setOf(Element.FIRE), setOf("表达", "创意")),
        ColorCandidate("sand", "砂岩", "#B9976B", setOf(Element.EARTH), setOf(Element.EARTH), setOf("稳定", "现实")),
        ColorCandidate("ink", "墨灰", "#41434A", setOf(Element.METAL), setOf(Element.METAL), setOf("理性", "研究")),
        ColorCandidate("deep-water", "深水蓝", "#355C7D", setOf(Element.WATER), setOf(Element.WATER), setOf("安静", "共情"))
    )
}
