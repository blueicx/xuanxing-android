package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.data.repository.FortuneRepository
import com.xuanji.app.domain.divination.MayaTzolkin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class MayaTzolkinViewModel(private val repository: FortuneRepository) : ViewModel() {

    private val _today = MutableStateFlow<MayaTzolkin.MayaResult?>(null)
    val today: StateFlow<MayaTzolkin.MayaResult?> = _today.asStateFlow()

    private val _birth = MutableStateFlow<MayaTzolkin.MayaResult?>(null)
    val birth: StateFlow<MayaTzolkin.MayaResult?> = _birth.asStateFlow()

    init {
        _today.value = MayaTzolkin.forDate(LocalDate.now())
        viewModelScope.launch {
            repository.userProfileFlow.collect { p: UserProfile? ->
                _birth.value = p?.let { MayaTzolkin.forProfile(it) }
            }
        }
    }
}
