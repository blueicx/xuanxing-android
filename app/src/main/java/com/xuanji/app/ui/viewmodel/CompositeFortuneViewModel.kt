package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.repository.FortuneRepository
import com.xuanji.app.domain.CompositeFortuneGenerator
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
    data class Ready(val fortune: CompositeDailyFortune) : CompositeUiState
}

class CompositeFortuneViewModel(private val repository: FortuneRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<CompositeUiState>(CompositeUiState.Loading)
    val uiState: StateFlow<CompositeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            // 等两个命盘缓存都就绪（后台已算好，通常瞬间命中）
            combine(repository.baziFullFlow, repository.natalChartFlow) { bazi, chart ->
                bazi to chart
            }.collect { (bazi, chart) ->
                if (bazi == null || chart == null) {
                    _uiState.value = CompositeUiState.Empty
                } else {
                    val detail = ZodiacCalculator.detailFromChart(chart)
                    val eastern = repository.getEasternFortune(bazi.chart, LocalDate.now())
                    val western = repository.getWesternFortune(detail.sun, LocalDate.now())
                    val composite = CompositeFortuneGenerator.generate(eastern, western, LocalDate.now())
                    _uiState.value = CompositeUiState.Ready(composite)
                }
            }
        }
    }
}
