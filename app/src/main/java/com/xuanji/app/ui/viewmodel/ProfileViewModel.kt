package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.data.repository.FortuneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: FortuneRepository) : ViewModel() {
    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

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
        location: String,
        gender: String
    ) {
        viewModelScope.launch {
            repository.saveUserProfile(
                UserProfile(year, month, day, hour, minute, location, gender)
            )
        }
    }
}
