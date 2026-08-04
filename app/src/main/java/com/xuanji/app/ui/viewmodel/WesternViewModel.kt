package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.model.WesternDailyFortune
import com.xuanji.app.data.repository.FortuneRepository
import com.xuanji.app.domain.ZodiacCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface WesternUiState {
    data object Loading : WesternUiState
    data object Empty : WesternUiState
    data class Ready(
        val detail: ZodiacCalculator.WesternDetail,
        val fortune: WesternDailyFortune,
        val chart: ZodiacCalculator.NatalChart
    ) : WesternUiState
}

class WesternViewModel(private val repository: FortuneRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<WesternUiState>(WesternUiState.Loading)
    val uiState: StateFlow<WesternUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            repository.natalChartFlow.collect { chart ->
                if (chart == null) {
                    _uiState.value = WesternUiState.Empty
                } else {
                    val detail = ZodiacCalculator.detailFromChart(chart)
                    val fortune = repository.getWesternFortune(detail.sun, LocalDate.now())
                    _uiState.value = WesternUiState.Ready(detail, fortune, chart)
                }
            }
        }
    }
}
