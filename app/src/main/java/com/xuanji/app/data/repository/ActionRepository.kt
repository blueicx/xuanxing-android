package com.xuanji.app.data.repository

import com.xuanji.app.data.local.FoodPreferenceStore
import com.xuanji.app.data.local.ActionFeedbackDataStore
import com.xuanji.app.data.local.ActionConstraintStore
import com.xuanji.app.data.local.ActionPreferenceSnapshot
import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.domain.action.CityProfileCatalog
import com.xuanji.app.domain.action.DailyActionInput
import com.xuanji.app.domain.action.DailyActionPlan
import com.xuanji.app.domain.action.DailyActionPlanner
import com.xuanji.app.domain.action.FoodPreference
import com.xuanji.app.domain.action.ActivityPreference
import com.xuanji.app.domain.action.LifeProfile
import com.xuanji.app.domain.action.LifeProfileInput
import com.xuanji.app.domain.action.LifeProfilePlanner
import com.xuanji.app.data.model.Element
import com.xuanji.app.domain.ZodiacCalculator
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.time.LocalDate

class ActionRepository(
    private val fortuneRepository: FortuneRepository,
    private val testRecords: TestRecordRepository,
    private val preferences: FoodPreferenceStore,
    private val dailyPlanner: DailyActionPlanner = DailyActionPlanner(),
    private val lifePlanner: LifeProfilePlanner = LifeProfilePlanner(),
    private val feedbackStore: ActionFeedbackDataStore? = null,
    private val activityPreferenceStore: ActionConstraintStore? = null,
    private val dailyLifeStore: com.xuanji.app.data.local.DailyLifeStore? = null
) {
    val dailyLifeRevision: kotlinx.coroutines.flow.StateFlow<Long>? get() = dailyLifeStore?.revision
    suspend fun loadToday(date: LocalDate = LocalDate.now(), temporary: com.xuanji.app.domain.TemporaryActionConditions = com.xuanji.app.domain.TemporaryActionConditions()): DailyActionPlan? {
        return loadInput(date, temporary)?.let(dailyPlanner::plan)
    }

    suspend fun loadMealPool(expectedProfileKey: String): List<com.xuanji.app.domain.action.MealSuggestion>? {
        val input = loadInput(LocalDate.now(), com.xuanji.app.domain.TemporaryActionConditions()) ?: return null
        if (input.profileKey != expectedProfileKey) return null
        val result = dailyPlanner.mealPool(input)
        if (fortuneRepository.userProfileState.value?.let(::profileKey) != expectedProfileKey) return null
        return result
    }

    private suspend fun loadInput(date: LocalDate, temporary: com.xuanji.app.domain.TemporaryActionConditions): DailyActionInput? {
        val profile = fortuneRepository.userProfileState.value ?: return null
        val bazi = fortuneRepository.baziFullFlow.value ?: return null
        val natal = fortuneRepository.natalChartFlow.value ?: return null
        val western = ZodiacCalculator.detailFromChart(natal).sun
        val fortune = fortuneRepository.getCompositeFortune(
            bazi.chart,
            western,
            date,
            "day"
        )
        val profileKey = profileKey(profile)
        val snapshot = preferences.readSnapshot(profileKey)
        if (snapshot.unreadable) return null // Never silently remove allergy/practical filters on corrupt or future data.
        val savedPreference = snapshot.preference
        val preference = temporary.apply(savedPreference)
        val activityPreference = activityPreferenceStore?.read(profileKey) ?: ActivityPreference()
        val feedback = feedbackStore?.list(profileKey).orEmpty()
        val pantry = if (preference.prioritizePantry) dailyLifeStore?.load(profileKey)?.takeIf { it.readable }?.pantry.orEmpty() else emptyList()
        if (fortuneRepository.userProfileState.value?.let(::profileKey) != profileKey) return null
        return DailyActionInput(
                profileKey = profileKey,
                date = date,
                chart = bazi.chart,
                zodiacKey = western.sign,
                zodiacElement = western.element.toElement(),
                fortune = fortune,
                city = cityFrom(profile),
                preference = preference,
                constraints = temporary.apply(activityPreference.toConstraints().copy(
                    maxMealBudgetCents = preference.maxMealBudgetCents,
                    maxPrepMinutes = preference.maxPrepMinutes
                )),
                feedback = feedback,
                pantry = pantry
        )
    }

    suspend fun loadLifeProfile(regionPreference: com.xuanji.app.domain.action.RegionPreference = com.xuanji.app.domain.action.RegionPreference()): LifeProfile? {
        val profile = fortuneRepository.userProfileState.value ?: return null
        val bazi = fortuneRepository.baziFullFlow.value ?: return null
        val natal = fortuneRepository.natalChartFlow.value ?: return null
        val western = ZodiacCalculator.detailFromChart(natal).sun
        val key = profileKey(profile)
        return lifePlanner.plan(
            LifeProfileInput(
                profileKey = key,
                chart = bazi.chart,
                zodiacKey = western.sign,
                zodiacElement = western.element.toElement(),
                tests = testRecords.records.first(),
                preferredCityKey = cityFrom(profile)?.key,
                regionPreference = regionPreference
            )
        )
    }

    suspend fun readFoodPreference(profileKey: String): FoodPreference = preferences.read(profileKey)
    suspend fun readFoodPreferenceSnapshot(profileKey: String): com.xuanji.app.data.local.FoodPreferenceSnapshot = preferences.readSnapshot(profileKey)

    suspend fun saveFoodPreference(profileKey: String, preference: FoodPreference) {
        preferences.save(profileKey, preference)
    }

    suspend fun clearFoodPreference(profileKey: String) {
        preferences.clear(profileKey)
    }

    suspend fun readActivityPreference(profileKey: String): ActivityPreference =
        activityPreferenceStore?.read(profileKey) ?: ActivityPreference()

    suspend fun readActivityPreferenceSnapshot(profileKey: String): ActionPreferenceSnapshot =
        activityPreferenceStore?.readSnapshot(profileKey) ?: ActionPreferenceSnapshot()

    suspend fun saveActivityPreference(profileKey: String, preference: ActivityPreference) {
        activityPreferenceStore?.save(profileKey, preference)
    }

    suspend fun clearActivityPreference(profileKey: String) {
        activityPreferenceStore?.clear(profileKey)
    }

    fun profileKey(profile: UserProfile): String = Companion.profileKey(profile)

    private fun cityFrom(profile: UserProfile) =
        CityProfileCatalog.find(profile.locationName.split('/').getOrNull(1)?.trim())
            ?: CityProfileCatalog.find(profile.locationName)

    companion object {
        fun profileKey(profile: UserProfile): String {
            val raw = listOf(
                profile.birthYear, profile.birthMonth, profile.birthDay,
                profile.birthHour, profile.birthMinute, profile.locationCode.orEmpty()
            ).joinToString("|")
            return MessageDigest.getInstance("SHA-256")
                .digest(raw.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }
    }
}

private fun String.toElement(): Element = when (this) {
    "木" -> Element.WOOD
    "火" -> Element.FIRE
    "土" -> Element.EARTH
    "金" -> Element.METAL
    else -> Element.WATER
}
