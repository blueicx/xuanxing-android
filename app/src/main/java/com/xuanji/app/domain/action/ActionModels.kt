package com.xuanji.app.domain.action

import com.xuanji.app.data.model.Element

data class DailyActionPlan(
    val dateKey: String,
    val profileKey: String,
    val cityKey: String?,
    val meals: List<MealSuggestion>,
    val activities: List<ActivitySuggestion>,
    val outings: List<OutingSuggestion>,
    val evidence: List<ActionEvidence>,
    val confidence: ConfidenceLevel,
    val disclaimer: String,
    val constraints: ActionConstraints = ActionConstraints()
)

data class MealSuggestion(
    val slot: MealSlot,
    val title: String,
    val ingredients: List<String>,
    val substitute: String,
    val deliveryKeywords: List<String>,
    val score: Int,
    val evidence: List<ActionEvidence>,
    val estimatedPriceCents: Int? = null,
    val prepMinutes: Int? = null
)

data class ActivitySuggestion(
    val title: String,
    val durationMinutes: IntRange,
    val bestPeriod: String,
    val avoid: String?,
    val score: Int,
    val evidence: List<ActionEvidence>
)

data class OutingSuggestion(
    val placeType: String,
    val reason: String,
    val cityLabel: String,
    val score: Int,
    val evidence: List<ActionEvidence>,
    val indoor: Boolean? = null
)

data class ActionEvidence(
    val source: ActionSource,
    val key: String,
    val label: String,
    val contribution: Int
)

enum class ActionSource { FiveElements, Zodiac, DailyFortune, Season, CityProfile, Preference }
enum class ConfidenceLevel { Low, Medium, High }
enum class MealSlot { Breakfast, Lunch, Dinner, Drink }

data class FoodPreference(
    val vegetarian: Boolean = false,
    val halal: Boolean = false,
    val avoidSpicy: Boolean = false,
    val avoidAlcohol: Boolean = false,
    val excludedIngredients: Set<String> = emptySet(),
    val allergens: Set<String> = emptySet(),
    val maxMealBudgetCents: Int? = null,
    val maxPrepMinutes: Int? = null
)

data class ActionConstraints(
    val maxMealBudgetCents: Int? = null,
    val maxPrepMinutes: Int? = null,
    val energy: EnergyLevel = EnergyLevel.Any,
    val indoorOnly: Boolean = false,
    val maxActivityMinutes: Int? = null
)

enum class EnergyLevel { Any, Low, Medium, High }

data class CityProfile(
    val key: String,
    val label: String,
    val tags: Set<String>,
    val country: String,
    val city: String
)

data class Candidate(val key: String, val score: Int)

data class LifeProfile(
    val profileKey: String,
    val careerClusters: List<CareerCluster>,
    val colorPalette: List<ColorCluster>,
    val environmentProfile: EnvironmentProfile,
    val regionCandidates: List<RegionCandidate>,
    val evidence: List<ActionEvidence>,
    val confidence: ConfidenceLevel,
    val missingInputs: List<String>,
    val disclaimer: String
)

data class CareerCluster(
    val key: String,
    val label: String,
    val score: Int,
    val reasons: List<String>
)

data class ColorCluster(
    val key: String,
    val label: String,
    val hex: String,
    val reasons: List<String>
)

data class EnvironmentProfile(
    val tags: List<String>,
    val rhythm: String,
    val reasons: List<String>
)

data class RegionCandidate(
    val country: String,
    val city: String,
    val matchedTags: List<String>,
    val unmatchedFactors: List<String>,
    val score: Int
)
