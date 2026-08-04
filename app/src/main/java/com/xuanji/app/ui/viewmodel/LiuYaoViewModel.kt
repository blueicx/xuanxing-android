package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.model.Hexagram
import com.xuanji.app.data.repository.LiuYaoRepository
import com.xuanji.app.domain.divination.LiuYao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LiuYaoViewModel(private val repository: LiuYaoRepository) : ViewModel() {

    private val _result = MutableStateFlow<LiuYao.LiuYaoResult?>(null)
    val result: StateFlow<LiuYao.LiuYaoResult?> = _result.asStateFlow()

    private val _question = MutableStateFlow("")
    val question: StateFlow<String> = _question.asStateFlow()

    private val _reading = MutableStateFlow<LiuYao.LiuYaoReading?>(null)
    val reading: StateFlow<LiuYao.LiuYaoReading?> = _reading.asStateFlow()

    private var hexagrams: List<Hexagram>? = null

    init {
        viewModelScope.launch {
            hexagrams = repository.loadHexagrams()
            cast()
        }
    }

    fun setQuestion(q: String) {
        _question.value = q
        recompute()
    }

    fun cast() {
        val hs = hexagrams ?: return
        _result.value = LiuYao.cast(hs)
        recompute()
    }

    private fun recompute() {
        val res = _result.value ?: return
        _reading.value = LiuYao.interpret(res, _question.value)
    }
}
