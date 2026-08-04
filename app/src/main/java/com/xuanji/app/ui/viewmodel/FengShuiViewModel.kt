package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.data.repository.FortuneRepository
import com.xuanji.app.domain.divination.FengShui
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FengShuiViewModel(private val repository: FortuneRepository) : ViewModel() {

    private val _result = MutableStateFlow<FengShui.FengShuiResult?>(null)
    val result: StateFlow<FengShui.FengShuiResult?> = _result.asStateFlow()

    private val _zuoShan = MutableStateFlow("坎")
    val zuoShan: StateFlow<String> = _zuoShan.asStateFlow()

    private val _hasProfile = MutableStateFlow(true)
    val hasProfile: StateFlow<Boolean> = _hasProfile.asStateFlow()

    private var profile: UserProfile? = null

    init {
        viewModelScope.launch {
            repository.userProfileFlow.collect { p ->
                profile = p
                if (p == null) _hasProfile.value = false
                else {
                    _hasProfile.value = true
                    recompute()
                }
            }
        }
    }

    fun setZuoShan(g: String) {
        _zuoShan.value = g
        recompute()
    }

    private fun recompute() {
        profile?.let { _result.value = FengShui.cast(it, _zuoShan.value) }
    }
}
