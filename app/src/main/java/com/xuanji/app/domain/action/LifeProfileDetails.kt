package com.xuanji.app.domain.action

/** Self-authored exploration exercises, not employment or housing forecasts. */
object LifeProfileDetails {
    fun career(value: CareerCluster): CareerCluster = when (value.key) {
        "creative" -> value.copy(typicalTasks = listOf("提炼内容主题并写提纲", "制作草图与收集反馈"), workEnvironment = "项目制协作、留出独立创作时段", skills = listOf("表达与叙事", "设计工具", "需求澄清"), trialActivity = "用二十分钟把熟悉的话题做成一页内容草稿。")
        "research" -> value.copy(typicalTasks = listOf("检索资料并核对来源", "整理数据与比较假设"), workEnvironment = "安静的研究空间与定期评审", skills = listOf("信息检索", "数据整理", "批判性阅读"), trialActivity = "对一个小问题比较三份资料，写出证据和未知项。")
        "people" -> value.copy(typicalTasks = listOf("倾听需求并整理反馈", "准备课程或协作说明"), workEnvironment = "人与人交流较多，兼顾准备和恢复时间", skills = listOf("主动倾听", "讲解", "边界与协作"), trialActivity = "给朋友讲解一个熟悉概念，询问哪里还不清楚。")
        "operations" -> value.copy(typicalTasks = listOf("拆解项目与维护进度", "协调资源并复盘流程"), workEnvironment = "多任务团队，使用明确清单和时间块", skills = listOf("任务拆解", "沟通协调", "流程记录"), trialActivity = "为一件小事写出目标、三步任务和完成条件。")
        else -> value
    }
    fun color(value: ColorCluster): ColorCluster = value.copy(applications = mapOf(
        "穿搭" to "${value.label}作小面积配饰，搭配米白或中性灰；先观察自己的喜好。",
        "桌面" to "用${value.label}作收纳或壁纸点缀，文字与背景保持清晰对比。",
        "房间" to "从${value.label}的软装小物试起，避免直接大面积改装。"
    ))
    fun regionExplanation(preference: RegionPreference): String =
        "用户明确选择：气候${preference.climate}、节奏${preference.rhythm}、${preference.budget}。静态文化环境标签不能验证实际气候或开销；预算资料未提供，不生成现实价格或迁居排名。"
}
