package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.xuanji.app.domain.divination.QiMen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import kotlin.random.Random

class QiMenViewModel : ViewModel() {

    private val _result = MutableStateFlow<QiMen.QiMenResult?>(null)
    val result: StateFlow<QiMen.QiMenResult?> = _result.asStateFlow()

    init { recast() }

    fun recast() { _result.value = QiMen.cast(LocalDateTime.now(), Random.nextLong()) }
}
