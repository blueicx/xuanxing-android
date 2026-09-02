package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.data.repository.FortuneRepository
import com.xuanji.app.domain.SelectedLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: FortuneRepository) : ViewModel() {
    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    val dailyReminderOn: StateFlow<Boolean> = repository.dailyReminderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val mysticGuideEnabled: StateFlow<Boolean> = repository.mysticGuideEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    init {
        viewModelScope.launch {
            repository.userProfileFlow.collect { _profile.value = it }
        }
    }

    fun save(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        location: SelectedLocation,
        gender: String?
    ) {
        viewModelScope.launch {
            repository.saveUserProfile(
                UserProfile(
                    year, month, day, hour, minute,
                    "${location.province.name} / ${location.city.name} / ${location.district.name}",
                    location.district.code,
                    location.district.lat,
                    location.district.lng,
                    gender
                )
            )
        }
    }

    fun clearAllLocalData() {
        viewModelScope.launch {
            repository.clearAllLocalData()
        }
    }

    fun clearUserProfile() {
        viewModelScope.launch {
            repository.clearUserProfile()
        }
    }

    fun setDailyReminderOn(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDailyReminderOn(enabled)
        }
    }

    fun setMysticGuideEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMysticGuideEnabled(enabled)
        }
    }
}
