package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.repository.FortuneRepository
import com.xuanji.app.domain.HourGuide
import com.xuanji.app.domain.HourGuideGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface EasternUiState {
    data object Loading : EasternUiState
    data object Empty : EasternUiState
    data class Ready(
        val full: BaziFull,
        val fortune: EasternDailyFortune,
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
            repository.baziFullFlow.collect { full ->
                if (full == null) {
                    _uiState.value = EasternUiState.Empty
                } else {
                    val fortune = repository.getEasternFortune(full.chart, LocalDate.now(), currentPeriod)
                    _uiState.value = EasternUiState.Ready(
                        full, fortune, HourGuideGenerator.generate(full.chart, LocalDate.now()), currentPeriod
                    )
                }
            }
        }
    }

    fun setPeriod(period: String) {
        if (period == currentPeriod) return
        currentPeriod = period
        viewModelScope.launch(Dispatchers.Default) {
            val full = repository.baziFullFlow.value
            if (full != null) {
                val fortune = repository.getEasternFortune(full.chart, LocalDate.now(), period)
                _uiState.value = EasternUiState.Ready(
                    full,
                    fortune,
                    HourGuideGenerator.generate(full.chart, LocalDate.now()),
                    period
                )
            }
        }
    }
}
