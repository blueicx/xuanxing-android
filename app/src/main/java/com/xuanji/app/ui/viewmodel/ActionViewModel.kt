package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.repository.ActionRepository
import com.xuanji.app.domain.action.DailyActionPlan
import com.xuanji.app.domain.action.FoodPreference
import com.xuanji.app.domain.action.ActivityPreference
import com.xuanji.app.domain.action.LifeProfile
import com.xuanji.app.domain.action.ActionScenario
import com.xuanji.app.domain.TemporaryActionConditions
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
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
    val activityPreference: ActivityPreference = ActivityPreference(),
    val activityPreferenceUnreadable: Boolean = false,
    val preferenceUnreadable: Boolean = false,
    val error: String? = null
)

class ActionViewModel(private val repository: ActionRepository) : ViewModel() {
    private val _state = MutableStateFlow(ActionUiState())
    private var refreshJob: Job? = null
    private var scenario = ActionScenario.Any
    private var scenarioDate: LocalDate? = null
    private var requestSequence = 0L
    val state: StateFlow<ActionUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            repository.dailyLifeRevision?.collect {
                if (_state.value.preference.prioritizePantry) refresh()
            }
        }
    }

    fun refresh(date: LocalDate = LocalDate.now()) {
        if (scenarioDate != date) { scenario = ActionScenario.Any; scenarioDate = date }
        val selectedScenario = scenario
        val sequence = ++requestSequence
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.Default) {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                val today = repository.loadToday(date, TemporaryActionConditions(scenario = selectedScenario))
                val profile = repository.loadLifeProfile()
                val key = profile?.profileKey ?: today?.profileKey
                val foodSnapshot = key?.let { repository.readFoodPreferenceSnapshot(it) }
                    ?: com.xuanji.app.data.local.FoodPreferenceSnapshot()
                val activityPreferenceSnapshot = key?.let { repository.readActivityPreferenceSnapshot(it) }
                    ?: com.xuanji.app.data.local.ActionPreferenceSnapshot()
                if (sequence != requestSequence) return@launch
                _state.value = ActionUiState(
                    loading = false,
                    profileKey = key,
                    todayPlan = today,
                    lifeProfile = profile,
                    preference = foodSnapshot.preference,
                    preferenceUnreadable = foodSnapshot.unreadable,
                    activityPreference = activityPreferenceSnapshot.preference,
                    activityPreferenceUnreadable = activityPreferenceSnapshot.unreadable
                )
            }.onFailure { error ->
                if (error is CancellationException) throw error
                if (sequence == requestSequence) _state.value = _state.value.copy(loading = false, error = error.message ?: "离线计算失败")
            }
        }
    }

    fun selectScenario(value: ActionScenario, date: LocalDate = LocalDate.now()) {
        scenarioDate = date
        scenario = value
        refresh(date)
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

    fun saveActivityPreference(preference: ActivityPreference) {
        val key = _state.value.profileKey ?: return
        viewModelScope.launch(Dispatchers.Default) {
            repository.saveActivityPreference(key, preference)
            _state.value = _state.value.copy(activityPreference = preference)
            refresh()
        }
    }

    fun clearActivityPreference() {
        val key = _state.value.profileKey ?: return
        viewModelScope.launch(Dispatchers.Default) {
            repository.clearActivityPreference(key)
            _state.value = _state.value.copy(activityPreference = ActivityPreference())
            refresh()
        }
    }
}
