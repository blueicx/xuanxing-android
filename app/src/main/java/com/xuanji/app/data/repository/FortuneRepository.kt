package com.xuanji.app.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.xuanji.app.data.local.dataStore
import com.xuanji.app.data.model.BaziChart
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.Pillar
import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.data.model.WesternDailyFortune
import com.xuanji.app.domain.BaziCalculator
import com.xuanji.app.domain.CompositeFortuneGenerator
import com.xuanji.app.domain.EasternFortuneGenerator
import com.xuanji.app.domain.WesternFortuneGenerator
import com.xuanji.app.domain.ZodiacCalculator
import com.xuanji.app.domain.divination.TodayOracle
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 离线优先的数据层：用户命盘存于 DataStore，每日运势按日期缓存。
 */
class FortuneRepository(private val context: Context) {

    private val gson = Gson()
    private val profileKey = stringPreferencesKey("user_profile")
    private val dailyReminderKey = booleanPreferencesKey("daily_reminder_on")
    private val mysticGuideEnabledKey = booleanPreferencesKey("mystic_guide_enabled")
    private val todayOracleKey = stringPreferencesKey("today_oracle_cache")

    /** 后台作用域：命盘重算放到 Default 调度器，避免主线程卡顿导致切换转场阻塞。
     *  挂 CoroutineExceptionHandler，任何命盘计算异常都只记日志，绝不杀进程。 */
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e ->
            Log.e("FortuneRepository", "后台命盘缓存出错（已忽略，不影响主流程）", e)
        }
    )

    /** 用户档案流（必须在 init 之前声明，否则 init 启动时它为 null 会崩） */
    val userProfileFlow: Flow<UserProfile?> = context.dataStore.data
        .map { prefs -> prefs[profileKey]?.let { gson.fromJson(it, UserProfile::class.java) } }
        .distinctUntilChanged()

    /** 每日运势提醒开关；默认关闭，与小程序保持一致 */
    val dailyReminderFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[dailyReminderKey] ?: false }
        .distinctUntilChanged()

    /** 运势页微光浮球开关；默认开启，完整人物舞台仍需用户点击浮球召回。 */
    val mysticGuideEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[mysticGuideEnabledKey] ?: true }
        .distinctUntilChanged()

    /** 八字命盘缓存（随 profile 后台计算），让东方页切回即显 */
    private val _baziFull = MutableStateFlow<BaziFull?>(null)
    val baziFullFlow: StateFlow<BaziFull?> = _baziFull.asStateFlow()

    /** 西方本命星盘缓存（真月亮+Placidus 较重），让西方页切回即显 */
    private val _natalChart = MutableStateFlow<ZodiacCalculator.NatalChart?>(null)
    val natalChartFlow: StateFlow<ZodiacCalculator.NatalChart?> = _natalChart.asStateFlow()

    /** 当前大运（供周/月/年解盘使用），随 profile 与命盘缓存派生 */
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfileState: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        scope.launch {
            userProfileFlow.collect { profile ->
                try {
                    if (profile == null) {
                        _baziFull.value = null
                        _natalChart.value = null
                        _userProfile.value = null
                    } else {
                        _baziFull.value = BaziCalculator.analyze(profile)
                        _natalChart.value = ZodiacCalculator.calculateNatalChart(
                            profile.birthYear, profile.birthMonth, profile.birthDay,
                            profile.birthHour, profile.birthMinute, profile.locationName,
                            profile.locationLat, profile.locationLng
                        )
                        _userProfile.value = profile
                    }
                } catch (e: Exception) {
                    Log.e("FortuneRepository", "计算命盘失败", e)
                }
            }
        }
    }

    /**
     * 出生日期在该年所处的大运干支。虚岁按「目标年 - 出生年 + 1」计，
     * 与大运起运年龄表（startAge..endAge 闭区间）对齐；查不到返回 null（解盘器会降级只论流年）。
     */
    private fun currentDayun(date: LocalDate): Pillar? {
        val full = _baziFull.value ?: return null
        val profile = _userProfile.value ?: return null
        if (full.daYun.isEmpty()) return null
        val age = date.year - profile.birthYear + 1
        return full.daYun.firstOrNull { age in it.startAge..it.endAge }?.pillar
            ?: if (age < full.daYun.first().startAge) null else full.daYun.last().pillar
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { it[profileKey] = gson.toJson(profile) }
    }

    suspend fun setDailyReminderOn(enabled: Boolean) {
        context.dataStore.edit { it[dailyReminderKey] = enabled }
    }

    suspend fun setMysticGuideEnabled(enabled: Boolean) {
        context.dataStore.edit { it[mysticGuideEnabledKey] = enabled }
    }

    /** 今日签当天首抽固化；跨天重置，手动彩蛋不写回这里。 */
    suspend fun getOrDrawTodayOracle(date: LocalDate): TodayOracle.OracleResult {
        val dayKey = dateKey(date)
        context.dataStore.data.map { it[todayOracleKey] }.first()?.let { json ->
            runCatching { gson.fromJson(json, TodayOracleCache::class.java) }
                .getOrNull()
                ?.takeIf { it.dateKey == dayKey }
                ?.let { return it.result }
        }

        val result = TodayOracle.randomDraw()
        context.dataStore.edit {
            it[todayOracleKey] = gson.toJson(TodayOracleCache(dayKey, result))
        }
        return result
    }

    suspend fun clearUserProfile() {
        context.dataStore.edit { it.remove(profileKey) }
    }

    suspend fun isDailyReminderOn(): Boolean =
        context.dataStore.data.map { it[dailyReminderKey] ?: false }.first()

    /** 用户主动清除本机 DataStore 中的命盘、缓存与测试记录 */
    suspend fun clearAllLocalData() {
        context.dataStore.edit { it.clear() }
    }

    /** 首次启动写入默认命盘（用户的生日），保证开屏即有内容 */
    suspend fun seedDefaultIfEmpty(default: UserProfile) {
        val existing = context.dataStore.data.map { it[profileKey] }.first()
        if (existing.isNullOrEmpty()) {
            context.dataStore.edit { it[profileKey] = gson.toJson(default) }
        }
    }

    // —— 首次引导页是否已看过 ——
    private val guideSeenKey = booleanPreferencesKey("guide_seen")

    suspend fun isGuideSeen(): Boolean =
        context.dataStore.data.map { it[guideSeenKey] ?: false }.first()

    suspend fun setGuideSeen() {
        context.dataStore.edit { it[guideSeenKey] = true }
    }

    suspend fun getEasternFortune(chart: BaziChart, date: LocalDate, period: String = "day"): EasternDailyFortune {
        // 缓存键要含命盘指纹：同一天切换生日后不能命中别人/旧的运势
        // v2_：解盘器改版（新增 insights / dimensionNotes / dimensionBasis），旧缓存结构不兼容
        val dayun = currentDayun(date)
        val dayunTag = dayun?.display?.replace(" ", "_") ?: "na"
        val key = stringPreferencesKey(
            "v2_eastern_${period}_${dateKey(date)}_${chartFingerprint(chart)}_$dayunTag"
        )
        context.dataStore.data.map { it[key] }.first()?.let {
            return gson.fromJson(it, EasternDailyFortune::class.java)
        }
        val fortune = when (period) {
            "week" -> EasternFortuneGenerator.generateWeekly(chart, date, dayun)
            "month" -> EasternFortuneGenerator.generateMonthly(chart, date, dayun)
            "year" -> EasternFortuneGenerator.generateYearly(chart, date, dayun)
            else -> EasternFortuneGenerator.generate(chart, date, "day", dayun)
        }
        context.dataStore.edit { it[key] = gson.toJson(fortune) }
        return fortune
    }

    suspend fun getWesternFortune(
        info: ZodiacCalculator.ZodiacInfo,
        date: LocalDate,
        period: String = "day",
        natal: ZodiacCalculator.NatalChart? = natalChartFlow.value
    ): WesternDailyFortune {
        // 同上：缓存键含星座指纹
        val key = stringPreferencesKey(
            "v2_western_${period}_${dateKey(date)}_${info.sign}_${info.element}_${natalFingerprint(natal)}"
        )
        context.dataStore.data.map { it[key] }.first()?.let {
            return gson.fromJson(it, WesternDailyFortune::class.java)
        }
        val fortune = when (period) {
            "week" -> WesternFortuneGenerator.generateWeekly(info, date, natal)
            "month" -> WesternFortuneGenerator.generateMonthly(info, date, natal)
            "year" -> WesternFortuneGenerator.generateYearly(info, date, natal)
            else -> WesternFortuneGenerator.generate(info, date, "day", natal)
        }
        context.dataStore.edit { it[key] = gson.toJson(fortune) }
        return fortune
    }

    suspend fun getCompositeFortune(
        chart: BaziChart,
        info: ZodiacCalculator.ZodiacInfo,
        date: LocalDate,
        period: String = "day"
    ): CompositeDailyFortune {
        val natal = natalChartFlow.value
        val dayun = currentDayun(date)
        val key = stringPreferencesKey(
            "v2_composite_${period}_${dateKey(date)}_${chartFingerprint(chart)}_${info.sign}" +
                "_${natalFingerprint(natal)}_${dayun?.display?.replace(" ", "_") ?: "na"}"
        )
        context.dataStore.data.map { it[key] }.first()?.let {
            return gson.fromJson(it, CompositeDailyFortune::class.java)
        }
        val fortune = when (period) {
            "week" -> CompositeFortuneGenerator.generateWeekly(chart, info, date, dayun, natal)
            "month" -> CompositeFortuneGenerator.generateMonthly(chart, info, date, dayun, natal)
            "year" -> CompositeFortuneGenerator.generateYearly(chart, info, date, dayun, natal)
            else -> {
                val easternF = getEasternFortune(chart, date)
                val westernF = getWesternFortune(info, date, natal = natal)
                CompositeFortuneGenerator.generate(easternF, westernF, date)
            }
        }
        context.dataStore.edit { it[key] = gson.toJson(fortune) }
        return fortune
    }

    /** 四柱拼接本身就是稳定指纹（年柱/月柱/日柱/时柱含天干地支） */
    private fun chartFingerprint(chart: BaziChart): String = chart.display.replace(" ", "_")

    /** 本命盘指纹：上升 + 太阳月亮所在星座与度数取整，避免不同出生时刻共用一份行运缓存 */
    private fun natalFingerprint(natal: ZodiacCalculator.NatalChart?): String {
        if (natal == null) return "nonatal"
        val asc = natal.ascendant
        val marks = listOf("太阳", "月亮")
            .mapNotNull { name -> natal.planets.firstOrNull { it.name == name } }
            .joinToString("-") { "${it.sign}${it.degreeInSign.toInt()}" }
        return "n${asc.hashCode().toUInt() % 100000u}_$marks"
    }

    private fun dateKey(date: LocalDate): String = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

    private data class TodayOracleCache(
        val dateKey: String,
        val result: TodayOracle.OracleResult
    )
}
