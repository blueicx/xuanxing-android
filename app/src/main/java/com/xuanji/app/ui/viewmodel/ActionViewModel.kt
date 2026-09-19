package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.repository.ActionRepository
import com.xuanji.app.domain.action.DailyActionPlan
import com.xuanji.app.domain.action.FoodPreference
import com.xuanji.app.domain.action.LifeProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ActionUiState(
    val loading: Boolean = true,
    val profileKey: String? = null,
    val todayPlan: DailyActionPlan? = null,
    val lifeProfile: LifeProfile? = null,
    val preference: FoodPreference = FoodPreference(),
    val preferenceUnreadable: Boolean = false,
    val error: String? = null
)

class ActionViewModel(private val repository: ActionRepository) : ViewModel() {
    private val _state = MutableStateFlow(ActionUiState())
    val state: StateFlow<ActionUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh(date: LocalDate = LocalDate.now()) {
        viewModelScope.launch(Dispatchers.Default) {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                val today = repository.loadToday(date)
                val profile = repository.loadLifeProfile()
                val key = profile?.profileKey ?: today?.profileKey
                val preference = key?.let { repository.readFoodPreference(it) } ?: FoodPreference()
                _state.value = ActionUiState(
                    loading = false,
                    profileKey = key,
                    todayPlan = today,
                    lifeProfile = profile,
                    preference = preference
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(loading = false, error = error.message ?: "离线计算失败")
            }
        }
    }

    fun savePreference(preference: FoodPreference) {
        val key = _state.value.profileKey ?: return
        viewModelScope.launch(Dispatchers.Default) {
            repository.saveFoodPreference(key, preference)
            _state.value = _state.value.copy(preference = preference)
            refresh()
        }
    }

    fun clearPreference() {
        val key = _state.value.profileKey ?: return
        viewModelScope.launch(Dispatchers.Default) {
            repository.clearFoodPreference(key)
            _state.value = _state.value.copy(preference = FoodPreference())
            refresh()
        }
    }
}
