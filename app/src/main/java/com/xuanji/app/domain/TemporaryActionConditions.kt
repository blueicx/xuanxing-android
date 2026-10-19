package com.xuanji.app.domain

import com.xuanji.app.domain.action.*

data class TemporaryActionConditions(
    val maxMinutes: Int? = null, val excludedIngredients: Set<String> = emptySet(),
    val indoorOnly: Boolean? = null, val maxBudgetCents: Int? = null,
    val cheaper: Boolean = false, val scenario: ActionScenario? = null
) {
    val active: Boolean get() = this != TemporaryActionConditions()
    private val effectiveMinutes: Int? get() = if (scenario == ActionScenario.ShortOnTime) minOf(maxMinutes ?: 15, 15) else maxMinutes
    fun apply(saved: FoodPreference): FoodPreference = saved.copy(
        excludedIngredients = saved.excludedIngredients + excludedIngredients,
        maxPrepMinutes = effectiveMinutes?.let { minOf(it, saved.maxPrepMinutes ?: it) } ?: saved.maxPrepMinutes,
        maxMealBudgetCents = maxBudgetCents?.let { minOf(it, saved.maxMealBudgetCents ?: it) } ?: saved.maxMealBudgetCents
    )
    fun apply(saved: ActionConstraints): ActionConstraints = saved.copy(
        maxActivityMinutes = effectiveMinutes?.let { minOf(it, saved.maxActivityMinutes ?: it) } ?: saved.maxActivityMinutes,
        indoorOnly = indoorOnly ?: saved.indoorOnly,
        maxMealBudgetCents = maxBudgetCents?.let { minOf(it, saved.maxMealBudgetCents ?: it) } ?: saved.maxMealBudgetCents,
        scenario = scenario ?: saved.scenario
    )
    fun merge(next: TemporaryActionConditions): TemporaryActionConditions = copy(
        maxMinutes = next.maxMinutes ?: maxMinutes, excludedIngredients = excludedIngredients + next.excludedIngredients,
        indoorOnly = next.indoorOnly ?: indoorOnly, maxBudgetCents = next.maxBudgetCents ?: maxBudgetCents,
        cheaper = next.cheaper || cheaper, scenario = next.scenario ?: scenario
    )
    companion object {
        fun parse(input: String): TemporaryActionConditions {
            val minuteText = Regex("(?:只有|最多|不超过|控制在|时间是|可用)([0-9一二三四五六七八九十两]+)分钟").find(input)?.groupValues?.get(1)
            val minutes = minuteText?.let(::number)?.takeIf { it in 5..480 }
            val budget = Regex("(?:预算|不超过)([0-9]+)元").find(input)?.groupValues?.get(1)?.toIntOrNull()
                ?.takeIf { it in 1..10000 }?.times(100)
            val excludes = Regex("(?:不要|不吃|排除)([^，。！!?？、\\s]{1,10})").findAll(input)
                .map { it.groupValues[1].removeSuffix("了").removeSuffix("的") }
                .filter { token -> ActionCatalog.meals.any { meal -> meal.ingredients.any { token == it || it.contains(token) } } }.toSet()
            val scenario = when { "在家" in input -> ActionScenario.Home; "上班" in input -> ActionScenario.Work
                "周末" in input -> ActionScenario.Weekend; "时间紧" in input -> ActionScenario.ShortOnTime; else -> null }
            return TemporaryActionConditions(minutes, excludes, if ("室内" in input) true else null, budget,
                "便宜" in input || "省钱" in input, scenario)
        }
        private fun number(value: String): Int? {
            value.toIntOrNull()?.let { return it }
            val digits = mapOf('一' to 1, '二' to 2, '两' to 2, '三' to 3, '四' to 4, '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9)
            if (value == "十") return 10
            val parts = value.split('十')
            if (parts.size == 2) return (parts[0].firstOrNull()?.let(digits::get) ?: 1) * 10 + (parts[1].firstOrNull()?.let(digits::get) ?: 0)
            return value.singleOrNull()?.let(digits::get)
        }
    }
}

data class MysticReference(val id: String, val title: String, val body: String, val evidence: String = "")
object MysticReferenceResolver {
    fun evidenceLines(plan: DailyActionPlan?, category: String, id: String?, conditions: TemporaryActionConditions,
        fallback: List<String>): List<String> = buildList {
        if (id != null) {
            val selected = fromPlan(plan, category).firstOrNull { it.id == id }
            if (selected == null) add("当前候选已不可用；没有替换为其他候选的依据。") else {
                add("当前候选：${selected.title}")
                add("候选匹配依据：${selected.evidence.ifBlank { "未提供计算依据" }}")
            }
        }
        if (conditions.active) {
            val filters = buildList {
                conditions.maxMinutes?.let { add("最多 $it 分钟") }
                if (conditions.excludedIngredients.isNotEmpty()) add("排除 ${conditions.excludedIngredients.sorted().joinToString("、")}")
                if (conditions.indoorOnly == true) add("室内")
                conditions.maxBudgetCents?.let { add("预算约 ¥${it / 100.0}") }
                conditions.scenario?.let { add("情境 ${it.name}") }
            }
            add("本次筛选（不计匹配分）：${filters.joinToString("；").ifBlank { "便宜一点" }}；长期偏好未自动更改。")
        }
        addAll(fallback)
    }.distinct()

    /** A presentation copy only: the original deterministic ranking remains unchanged. */
    fun selectPlan(plan: DailyActionPlan?, category: String, id: String?): DailyActionPlan? {
        if (plan == null || id == null) return null
        return when (category) {
            "meal" -> plan.meals.firstOrNull { it.candidateKey == id }?.let { selected ->
                plan.copy(meals = listOf(selected) + plan.meals.filterNot { it.candidateKey == id })
            }
            "activity" -> plan.activities.firstOrNull { it.candidateKey == id }?.let { selected ->
                plan.copy(activities = listOf(selected) + plan.activities.filterNot { it.candidateKey == id })
            }
            "outing" -> plan.outings.firstOrNull { it.candidateKey == id }?.let { selected ->
                plan.copy(outings = listOf(selected) + plan.outings.filterNot { it.candidateKey == id })
            }
            else -> null
        }
    }

    fun requestedIndex(input: String): Int? {
        val match = Regex("第([一二三四五六七八九]|[1-9])个").find(input)?.groupValues?.get(1) ?: return null
        return match.toIntOrNull() ?: ("一二三四五六七八九".indexOf(match) + 1).takeIf { it > 0 }
    }
    fun fromPlan(plan: DailyActionPlan?, category: String): List<MysticReference> = when (category) {
        "meal" -> plan?.meals?.map { MysticReference(it.candidateKey, it.title,
            "${it.title}：${it.ingredients.joinToString("、")}；${it.prepMinutes ?: "未标注"} 分钟；${it.steps.joinToString("；")}", it.evidence.joinToString("；") { e -> e.label }) }.orEmpty()
        "activity" -> plan?.activities?.map { MysticReference(it.candidateKey, it.title,
            "${it.title}，${it.durationMinutes.first}–${it.durationMinutes.last} 分钟，${it.bestPeriod}", it.evidence.joinToString("；") { e -> e.label }) }.orEmpty()
        "outing" -> plan?.outings?.map { MysticReference(it.candidateKey, it.placeType,
            "${it.cityLabel} · ${it.placeType}：${it.reason}", it.evidence.joinToString("；") { e -> e.label }) }.orEmpty()
        else -> emptyList()
    }
}
