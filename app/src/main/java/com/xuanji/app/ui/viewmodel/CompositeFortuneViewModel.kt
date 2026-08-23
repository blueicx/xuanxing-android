package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.repository.FortuneRepository
import com.xuanji.app.domain.ZodiacCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface CompositeUiState {
    data object Loading : CompositeUiState
    data object Empty : CompositeUiState
    data class Ready(
        val bazi: BaziFull,
        val fortune: CompositeDailyFortune,
        val period: String = "day"
    ) : CompositeUiState
}

class CompositeFortuneViewModel(private val repository: FortuneRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<CompositeUiState>(CompositeUiState.Loading)
    val uiState: StateFlow<CompositeUiState> = _uiState.asStateFlow()
    private var currentPeriod = "day"

    init {
        viewModelScope.launch(Dispatchers.Default) {
            // 等两个命盘缓存都就绪（后台已算好，通常瞬间命中）
            combine(repository.baziFullFlow, repository.natalChartFlow) { bazi, chart ->
                bazi to chart
            }.collect { (bazi, chart) ->
                if (bazi == null || chart == null) {
                    _uiState.value = CompositeUiState.Empty
                } else {
                    _uiState.value = CompositeUiState.Ready(
                        bazi,
                        repository.getCompositeFortune(bazi.chart, ZodiacCalculator.detailFromChart(chart).sun, LocalDate.now(), currentPeriod),
                        currentPeriod
                    )
                }
            }
        }
    }

    fun setPeriod(period: String) {
        if (period == currentPeriod) return
        currentPeriod = period
        viewModelScope.launch(Dispatchers.Default) {
            val bazi = repository.baziFullFlow.value
            val chart = repository.natalChartFlow.value
            if (bazi != null && chart != null) {
                val composite = repository.getCompositeFortune(bazi.chart, ZodiacCalculator.detailFromChart(chart).sun, LocalDate.now(), period)
                _uiState.value = CompositeUiState.Ready(bazi, composite, period)
            }
        }
    }
}
