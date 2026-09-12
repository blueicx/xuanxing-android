package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.repository.FortuneRepository
import com.xuanji.app.domain.HourGuide
import com.xuanji.app.domain.HourGuideGenerator
import com.xuanji.app.domain.ZodiacCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface EasternUiState {
    data object Loading : EasternUiState
    data object Empty : EasternUiState
    data class Ready(
        val full: BaziFull,
        val fortune: EasternDailyFortune,
        val composite: CompositeDailyFortune?,
        val hourGuides: List<HourGuide>,
        val period: String = "day"
    ) : EasternUiState
}

class EasternViewModel(private val repository: FortuneRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<EasternUiState>(EasternUiState.Loading)
    val uiState: StateFlow<EasternUiState> = _uiState.asStateFlow()
    private var currentPeriod = "day"

    init {
        viewModelScope.launch(Dispatchers.Default) {
            combine(repository.baziFullFlow, repository.natalChartFlow) { full, chart -> full to chart }
                .collect { (full, chart) ->
                    if (full == null || chart == null) {
                        _uiState.value = EasternUiState.Empty
                        return@collect
                    }
                    val fortune = repository.getEasternFortune(full.chart, LocalDate.now(), currentPeriod)
                    val composite = repository.getCompositeFortune(
                        full.chart,
                        ZodiacCalculator.detailFromChart(chart).sun,
                        LocalDate.now(),
                        currentPeriod
                    )
                    _uiState.value = EasternUiState.Ready(
                        full,
                        fortune,
                        composite,
                        HourGuideGenerator.generate(full.chart, LocalDate.now()),
                        currentPeriod
                    )
                }
        }
    }

    fun setPeriod(period: String) {
        if (period == currentPeriod) return
        currentPeriod = period
        viewModelScope.launch(Dispatchers.Default) {
            val full = repository.baziFullFlow.value
            val chart = repository.natalChartFlow.value
            if (full != null && chart != null) {
                val fortune = repository.getEasternFortune(full.chart, LocalDate.now(), period)
                val composite = repository.getCompositeFortune(
                    full.chart,
                    ZodiacCalculator.detailFromChart(chart).sun,
                    LocalDate.now(),
                    period
                )
                _uiState.value = EasternUiState.Ready(
                    full,
                    fortune,
                    composite,
                    HourGuideGenerator.generate(full.chart, LocalDate.now()),
                    period
                )
            }
        }
    }
}
