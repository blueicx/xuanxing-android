package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.repository.FortuneRepository
import com.xuanji.app.domain.divination.ClassicalAstrology
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClassicalAstrologyViewModel(private val repository: FortuneRepository) : ViewModel() {

    private val _result = MutableStateFlow<ClassicalAstrology.ClassicalResult?>(null)
    val result: StateFlow<ClassicalAstrology.ClassicalResult?> = _result.asStateFlow()

    private val _framework = MutableStateFlow("希腊")
    val framework: StateFlow<String> = _framework.asStateFlow()

    private val _hasProfile = MutableStateFlow(true)
    val hasProfile: StateFlow<Boolean> = _hasProfile.asStateFlow()

    init {
        viewModelScope.launch {
            repository.userProfileFlow.collect { p ->
                if (p == null) _hasProfile.value = false
                else { _hasProfile.value = true; _result.value = ClassicalAstrology.cast(p) }
            }
        }
    }

    fun setFramework(f: String) { _framework.value = f }
}
