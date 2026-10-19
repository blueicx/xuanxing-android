package com.xuanji.app.domain.action

import com.xuanji.app.data.model.BaziChart
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.Element
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class DailyActionInput(
    val profileKey: String,
    val date: LocalDate,
    val chart: BaziChart,
    val zodiacKey: String,
    val zodiacElement: Element,
    val fortune: CompositeDailyFortune,
    val city: CityProfile?,
    val preference: FoodPreference,
    val constraints: ActionConstraints = ActionConstraints(),
    val feedback: List<ActionFeedback> = emptyList(),
    val pantry: List<com.xuanji.app.domain.PantryItem> = emptyList()
)

class DailyActionPlanner {
    fun plan(input: DailyActionInput): DailyActionPlan = build(input, includeAllMeals = false)

    /** Same filters, sources, cultural scores and feedback order as the daily recommendation. */
    fun mealPool(input: DailyActionInput): List<MealSuggestion> = build(input, includeAllMeals = true).meals

    private fun build(input: DailyActionInput, includeAllMeals: Boolean): DailyActionPlan {
        val dateKey = input.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val season = seasonOf(input.date)
        val favorable = input.chart.favorableElements.toSet().ifEmpty { setOf(input.chart.dayMasterElement) }
        val overall = input.fortune.overallScore

        val eligibleMeals = ActionCatalog.meals.filter { candidate ->
            val ingredients = candidate.ingredients.joinToString("|").lowercase()
            val excluded = input.preference.excludedIngredients.any { token ->
                token.isNotBlank() && ingredients.contains(token.trim().lowercase())
            }
            val allergens = input.preference.allergens.any { token ->
                token.isNotBlank() && (ingredients.contains(token.trim().lowercase()) || candidate.allergenTags.any { it.equals(token.trim(), ignoreCase = true) })
            }
            val maxBudget = input.constraints.maxMealBudgetCents ?: input.preference.maxMealBudgetCents
            val maxPrep = input.constraints.maxPrepMinutes ?: input.preference.maxPrepMinutes
            !excluded && (!input.preference.vegetarian || candidate.vegetarian) &&
                (!input.preference.halal || candidate.halalCompatible) &&
                (!input.preference.avoidSpicy || !candidate.spicy) &&
                (!input.preference.avoidAlcohol || !candidate.alcohol) &&
                !allergens &&
                (maxBudget == null || candidate.estimatedPriceCents <= maxBudget) &&
                (maxPrep == null || candidate.prepMinutes <= maxPrep) &&
                (input.preference.availableEquipment?.containsAll(candidate.requiredEquipment) != false) &&
                (input.preference.maxCookingDifficulty?.let { candidate.difficulty.ordinal <= it.ordinal } != false) &&
                (!input.preference.lunchboxOnly || candidate.lunchboxSuitable)
        }

        val mealBaseRanked = eligibleMeals.map { candidate ->
            val score = score(
                elementFit = elementFit(favorable, candidate.elementTags),
                zodiacFit = zodiacFit(input.zodiacElement, candidate.zodiacTags),
                fortuneFit = overall,
                seasonOrCityFit = seasonFit(season, candidate.seasonTags)
            )
            Candidate(candidate.key, score) to candidate
        }.sortedWith(compareByDescending<Pair<Candidate, MealCandidate>> { it.first.score }
            .thenBy { StableScore.tieBreak(input.profileKey, dateKey, it.first.key) })
        val stocked = input.pantry.filter { com.xuanji.app.domain.PantryRules.available(it, input.date) }
            .map { com.xuanji.app.domain.PantryRules.nameKey(it.ingredient) }.toSet()
        fun pantryMatches(candidate: MealCandidate): List<String> = candidate.ingredients.filter {
            com.xuanji.app.domain.PantryRules.nameKey(it) in stocked
        }
        val feedbackRanked = applyFeedbackOrder(mealBaseRanked, input.feedback, "meal", dateKey)
        val mealRanked = if (input.preference.prioritizePantry) feedbackRanked.sortedByDescending {
            pantryMatches(it.second).size
        } else feedbackRanked

        val selectedMeals = if (includeAllMeals) mealRanked else MealSlot.values().mapNotNull { slot -> mealRanked.firstOrNull { it.second.slot == slot } }
        val meals = selectedMeals.map { (rank, candidate) ->
                val slot = candidate.slot
                MealSuggestion(
                    slot = slot,
                    title = candidate.title,
                    ingredients = candidate.ingredients,
                    substitute = mealRanked.firstOrNull { it.second.slot == slot && it.second.key != candidate.key }?.second?.title
                        ?: "当前约束下无同餐次替代候选",
                    deliveryKeywords = candidate.deliveryKeywords,
                    score = rank.score,
                    evidence = evidenceForMeal(input, candidate, season, favorable, rank.score) +
                        if (input.preference.prioritizePantry) listOf(ActionEvidence(ActionSource.Preference, "pantry-priority",
                            "库存优先排序（不计文化分）：${pantryMatches(candidate).joinToString("、").ifBlank { "无可用同名库存" }}；数量是否足够需自查", 0)) else emptyList(),
                    estimatedPriceCents = candidate.estimatedPriceCents,
                    prepMinutes = candidate.prepMinutes,
                    candidateKey = candidate.key,
                    steps = candidate.steps,
                    substituteCandidateKey = mealRanked.firstOrNull { it.second.slot == slot && it.second.key != candidate.key }?.second?.key,
                    requiredEquipment = candidate.requiredEquipment,
                    difficulty = candidate.difficulty,
                    lunchboxSuitable = candidate.lunchboxSuitable
                )
        }

        val activityBaseRanked = ActionCatalog.activities.filter { candidate ->
            val maxMinutes = effectiveActivityLimit(input.constraints)
            val maxEnergy = input.constraints.energy
            (!input.constraints.indoorOnly || candidate.indoor) &&
                (input.constraints.scenario !in setOf(ActionScenario.Home, ActionScenario.Work) || candidate.indoor) &&
                (maxMinutes == null || candidate.durationMinutes.first <= maxMinutes) &&
                (maxEnergy == EnergyLevel.Any || candidate.energy.ordinal <= maxEnergy.ordinal)
        }.map { candidate ->
            val score = score(
                elementFit = elementFit(favorable, candidate.elementTags),
                zodiacFit = zodiacFit(input.zodiacElement, candidate.zodiacTags),
                fortuneFit = fortuneFit(input.fortune, candidate.fortuneKeys),
                seasonOrCityFit = seasonFit(season, candidate.seasonTags)
            )
            Candidate(candidate.key, score) to candidate
        }.sortedWith(compareByDescending<Pair<Candidate, ActivityCandidate>> { it.first.score }
            .thenBy { StableScore.tieBreak(input.profileKey, dateKey, it.first.key) })
        val activityRanked = applyFeedbackOrder(activityBaseRanked, input.feedback, "activity", dateKey).take(3)

        val activities = activityRanked.map { (rank, candidate) ->
            ActivitySuggestion(
                title = candidate.title,
                durationMinutes = candidate.durationMinutes.first..minOf(candidate.durationMinutes.last,
                    effectiveActivityLimit(input.constraints) ?: candidate.durationMinutes.last),
                bestPeriod = candidate.bestPeriod,
                avoid = candidate.avoid,
                score = rank.score,
                evidence = evidenceForActivity(input, candidate, season, rank.score),
                candidateKey = candidate.key,
                indoor = candidate.indoor
            )
        }

        val outingBaseRanked = ActionCatalog.outings.filter { candidate ->
            (!input.constraints.indoorOnly || candidate.indoor) && input.constraints.scenario != ActionScenario.Home
        }.map { candidate ->
            val score = score(
                elementFit = elementFit(favorable, candidate.elementTags),
                zodiacFit = zodiacFit(input.zodiacElement, candidate.zodiacTags),
                fortuneFit = fortuneFit(input.fortune, setOf("emotion", "health")),
                seasonOrCityFit = cityOrSeasonFit(input.city, season, candidate)
            )
            Candidate(candidate.key, score) to candidate
        }.sortedWith(compareByDescending<Pair<Candidate, OutingCandidate>> { it.first.score }
            .thenBy { StableScore.tieBreak(input.profileKey, dateKey, it.first.key) })
        val outingRanked = applyFeedbackOrder(outingBaseRanked, input.feedback, "outing", dateKey).take(3)

        val outings = outingRanked.map { (rank, candidate) ->
            val cityLabel = input.city?.label ?: "附近的${candidate.placeType}环境"
            OutingSuggestion(
                placeType = candidate.placeType,
                reason = candidate.reason,
                cityLabel = cityLabel,
                score = rank.score,
                evidence = evidenceForOuting(input, candidate, season, rank.score),
                indoor = candidate.indoor,
                candidateKey = candidate.key
            )
        }

        val confidence = if (input.city != null) ConfidenceLevel.High else ConfidenceLevel.Medium
        val disclaimer = buildString {
            append("这是基于五行、星座、今日盘面与季节/城市标签的离线生活方式灵感，不是医疗、营养、过敏或安全结论。")
            if (input.city == null) append("未选择目录城市，出游部分按环境类型表达。")
            if (input.preference.lunchboxOnly) append("便当标签仅表示携带形式，不保证储存安全；运输、冷藏和复热条件请自行核对。")
            if (eligibleMeals.isEmpty()) append("当前饮食偏好、预算、准备时间、设备、难度或便当条件过滤掉了全部候选，请放宽条件或自选食物；本次不伪造具体菜名。")
        }
        val planEvidence = listOf(
            ActionEvidence(ActionSource.FiveElements, "favorable", "权重 40% · 喜用五行：${favorable.joinToString { elementName(it) }}", 40),
            ActionEvidence(ActionSource.Zodiac, input.zodiacKey, "权重 25% · 星座元素：${elementName(input.zodiacElement)}", 25),
            ActionEvidence(ActionSource.DailyFortune, input.date.toString(), "权重 20% · 今日综合运势：$overall 分", 20),
            ActionEvidence(if (input.city == null) ActionSource.Season else ActionSource.CityProfile,
                input.city?.key ?: season, "权重 15% · 季节/城市环境：${input.city?.label ?: season}", 15)
        )

        return DailyActionPlan(
            dateKey = dateKey,
            profileKey = input.profileKey,
            cityKey = input.city?.key,
            meals = meals,
            activities = activities,
            outings = outings,
            evidence = planEvidence,
            confidence = confidence,
            disclaimer = disclaimer,
            constraints = input.constraints,
            appliedFeedback = input.feedback
        )
    }

    private fun <T> applyFeedbackOrder(
        ranked: List<Pair<Candidate, T>>,
        feedback: List<ActionFeedback>,
        category: String,
        dateKey: String
    ): List<Pair<Candidate, T>> {
        val byKey = ranked.associateBy { it.first.key }
        return applyExplicitFeedback(ranked.map { it.first }, feedback, category, dateKey)
            .mapNotNull { byKey[it.key] }
    }

    private fun score(elementFit: Int, zodiacFit: Int, fortuneFit: Int, seasonOrCityFit: Int): Int =
        StableScore.combine(elementFit, zodiacFit, fortuneFit, seasonOrCityFit)

    private fun effectiveActivityLimit(constraints: ActionConstraints): Int? =
        if (constraints.scenario == ActionScenario.ShortOnTime) minOf(constraints.maxActivityMinutes ?: 15, 15)
        else constraints.maxActivityMinutes

    private fun elementFit(favorable: Set<Element>, candidate: Set<Element>): Int =
        if (candidate.isEmpty()) 0 else ((favorable.intersect(candidate).size.toDouble() / candidate.size) * 100).roundToInt().coerceIn(0, 100)

    private fun zodiacFit(zodiac: Element, candidate: Set<Element>): Int = if (zodiac in candidate) 100 else 35

    private fun fortuneFit(fortune: CompositeDailyFortune, keys: Set<String>): Int {
        if (keys.isEmpty()) return fortune.overallScore
        val scores = keys.mapNotNull { key -> fortune.dimensions.firstOrNull { it.key == key }?.score }
        return if (scores.isEmpty()) fortune.overallScore else scores.average().roundToInt()
    }

    private fun seasonFit(season: String, candidate: Set<String>): Int = if (season in candidate) 100 else 45

    private fun cityOrSeasonFit(city: CityProfile?, season: String, candidate: OutingCandidate): Int =
        if (city == null) seasonFit(season, candidate.seasonTags)
        else if (city.tags.intersect(candidate.cityTags).isNotEmpty()) 100 else seasonFit(season, candidate.seasonTags)

    private fun seasonOf(date: LocalDate): String = when (date.monthValue) {
        3, 4, 5 -> "spring"
        6, 7, 8 -> "summer"
        9, 10, 11 -> "autumn"
        else -> "winter"
    }

    private fun evidenceForMeal(
        input: DailyActionInput,
        candidate: MealCandidate,
        season: String,
        favorable: Set<Element>,
        score: Int
    ): List<ActionEvidence> = withContributions(listOf(
        ActionEvidence(ActionSource.FiveElements, candidate.key, "${candidate.title}对应${candidate.elementTags.joinToString { elementName(it) }}与喜用${favorable.joinToString { elementName(it) }}", (score * StableScore.FIVE_ELEMENT_WEIGHT).roundToInt()),
        ActionEvidence(ActionSource.Zodiac, input.zodiacKey, "星座元素${elementName(input.zodiacElement)}与候选标签匹配", (score * StableScore.ZODIAC_WEIGHT).roundToInt()),
        ActionEvidence(ActionSource.DailyFortune, "overall", "今日综合运势 ${input.fortune.overallScore} 分", (score * StableScore.DAILY_FORTUNE_WEIGHT).roundToInt()),
        ActionEvidence(ActionSource.Season, season, "${season}时令标签", (score * StableScore.SEASON_OR_CITY_WEIGHT).roundToInt())
    ), elementFit(favorable, candidate.elementTags), zodiacFit(input.zodiacElement, candidate.zodiacTags),
        input.fortune.overallScore, seasonFit(season, candidate.seasonTags)) + if (input.preference != FoodPreference()) listOf(
        ActionEvidence(ActionSource.Preference, "local", "已按本机饮食偏好过滤", 0)
    ) else emptyList()

    private fun evidenceForActivity(
        input: DailyActionInput,
        candidate: ActivityCandidate,
        season: String,
        score: Int
    ): List<ActionEvidence> = withContributions(listOf(
        ActionEvidence(ActionSource.FiveElements, candidate.key, "行动标签${candidate.elementTags.joinToString { elementName(it) }}", (score * StableScore.FIVE_ELEMENT_WEIGHT).roundToInt()),
        ActionEvidence(ActionSource.Zodiac, input.zodiacKey, "星座元素${elementName(input.zodiacElement)}", (score * StableScore.ZODIAC_WEIGHT).roundToInt()),
        ActionEvidence(ActionSource.DailyFortune, candidate.fortuneKeys.joinToString(), "对应今日${candidate.fortuneKeys.joinToString("、")}维度", (score * StableScore.DAILY_FORTUNE_WEIGHT).roundToInt()),
        ActionEvidence(ActionSource.Season, season, "${season}时段建议", (score * StableScore.SEASON_OR_CITY_WEIGHT).roundToInt())
    ), elementFit(input.chart.favorableElements.toSet().ifEmpty { setOf(input.chart.dayMasterElement) }, candidate.elementTags),
        zodiacFit(input.zodiacElement, candidate.zodiacTags), fortuneFit(input.fortune, candidate.fortuneKeys),
        seasonFit(season, candidate.seasonTags))

    private fun evidenceForOuting(
        input: DailyActionInput,
        candidate: OutingCandidate,
        season: String,
        score: Int
    ): List<ActionEvidence> = withContributions(listOf(
        ActionEvidence(ActionSource.FiveElements, candidate.key, "场所元素${candidate.elementTags.joinToString { elementName(it) }}", (score * StableScore.FIVE_ELEMENT_WEIGHT).roundToInt()),
        ActionEvidence(ActionSource.Zodiac, input.zodiacKey, "星座元素${elementName(input.zodiacElement)}", (score * StableScore.ZODIAC_WEIGHT).roundToInt()),
        ActionEvidence(ActionSource.DailyFortune, "emotion-health", "情绪与健康维度作为节奏参考", (score * StableScore.DAILY_FORTUNE_WEIGHT).roundToInt()),
        ActionEvidence(if (input.city != null) ActionSource.CityProfile else ActionSource.Season, input.city?.key ?: season, input.city?.tags?.joinToString("、") ?: "按季节环境类型匹配", (score * StableScore.SEASON_OR_CITY_WEIGHT).roundToInt())
    ), elementFit(input.chart.favorableElements.toSet().ifEmpty { setOf(input.chart.dayMasterElement) }, candidate.elementTags),
        zodiacFit(input.zodiacElement, candidate.zodiacTags), fortuneFit(input.fortune, setOf("emotion", "health")),
        cityOrSeasonFit(input.city, season, candidate))

    /** Largest-remainder rounding: displayed components add up to the actual weighted score. */
    private fun withContributions(evidence: List<ActionEvidence>, five: Int, zodiac: Int, fortune: Int, environment: Int): List<ActionEvidence> {
        val raw = listOf(five * StableScore.FIVE_ELEMENT_WEIGHT, zodiac * StableScore.ZODIAC_WEIGHT,
            fortune * StableScore.DAILY_FORTUNE_WEIGHT, environment * StableScore.SEASON_OR_CITY_WEIGHT)
        val points = raw.map { kotlin.math.floor(it).toInt() }.toMutableList()
        val residual = StableScore.combine(five, zodiac, fortune, environment) - points.sum()
        raw.indices.sortedWith(compareByDescending<Int> { raw[it] - points[it] }.thenBy { it })
            .take(residual).forEach { points[it]++ }
        return evidence.mapIndexed { index, item -> item.copy(contribution = points[index]) }
    }
}

private fun elementName(element: Element): String = when (element) {
    Element.WOOD -> "木"
    Element.FIRE -> "火"
    Element.EARTH -> "土"
    Element.METAL -> "金"
    Element.WATER -> "水"
}
