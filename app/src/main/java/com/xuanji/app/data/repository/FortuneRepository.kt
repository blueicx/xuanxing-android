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
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.data.model.WesternDailyFortune
import com.xuanji.app.domain.BaziCalculator
import com.xuanji.app.domain.EasternFortuneGenerator
import com.xuanji.app.domain.WesternFortuneGenerator
import com.xuanji.app.domain.ZodiacCalculator
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

    /** 八字命盘缓存（随 profile 后台计算），让东方页切回即显 */
    private val _baziFull = MutableStateFlow<BaziFull?>(null)
    val baziFullFlow: StateFlow<BaziFull?> = _baziFull.asStateFlow()

    /** 西方本命星盘缓存（真月亮+Placidus 较重），让西方页切回即显 */
    private val _natalChart = MutableStateFlow<ZodiacCalculator.NatalChart?>(null)
    val natalChartFlow: StateFlow<ZodiacCalculator.NatalChart?> = _natalChart.asStateFlow()

    init {
        scope.launch {
            userProfileFlow.collect { profile ->
                try {
                    if (profile == null) {
                        _baziFull.value = null
                        _natalChart.value = null
                    } else {
                        _baziFull.value = BaziCalculator.analyze(profile)
                        _natalChart.value = ZodiacCalculator.calculateNatalChart(
                            profile.birthYear, profile.birthMonth, profile.birthDay,
                            profile.birthHour, profile.birthMinute, profile.locationName
                        )
                    }
                } catch (e: Exception) {
                    Log.e("FortuneRepository", "计算命盘失败", e)
                }
            }
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { it[profileKey] = gson.toJson(profile) }
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

    suspend fun getEasternFortune(chart: BaziChart, date: LocalDate): EasternDailyFortune {
        // 缓存键要含命盘指纹：同一天切换生日后不能命中别人/旧的运势
        val key = stringPreferencesKey("eastern_${dateKey(date)}_${chartFingerprint(chart)}")
        context.dataStore.data.map { it[key] }.first()?.let {
            return gson.fromJson(it, EasternDailyFortune::class.java)
        }
        val fortune = EasternFortuneGenerator.generate(chart, date)
        context.dataStore.edit { it[key] = gson.toJson(fortune) }
        return fortune
    }

    suspend fun getWesternFortune(
        info: ZodiacCalculator.ZodiacInfo,
        date: LocalDate
    ): WesternDailyFortune {
        // 同上：缓存键含星座指纹
        val key = stringPreferencesKey("western_${dateKey(date)}_${info.sign}_${info.element}")
        context.dataStore.data.map { it[key] }.first()?.let {
            return gson.fromJson(it, WesternDailyFortune::class.java)
        }
        val fortune = WesternFortuneGenerator.generate(info, date)
        context.dataStore.edit { it[key] = gson.toJson(fortune) }
        return fortune
    }

    /** 四柱拼接本身就是稳定指纹（年柱/月柱/日柱/时柱含天干地支） */
    private fun chartFingerprint(chart: BaziChart): String = chart.display.replace(" ", "_")

    private fun dateKey(date: LocalDate): String = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
}
