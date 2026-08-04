package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.xuanji.app.domain.divination.Lenormand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LenormandViewModel : ViewModel() {

    private val _result = MutableStateFlow<Lenormand.LenormandResult?>(null)
    val result: StateFlow<Lenormand.LenormandResult?> = _result.asStateFlow()

    init { draw() }

    fun draw() { _result.value = Lenormand.draw() }
}
