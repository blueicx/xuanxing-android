package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.data.repository.FortuneRepository
import com.xuanji.app.domain.divination.MeiHua
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.random.Random

class MeiHuaViewModel(private val repository: FortuneRepository) : ViewModel() {

    private val _result = MutableStateFlow<MeiHua.MeiHuaResult?>(null)
    val result: StateFlow<MeiHua.MeiHuaResult?> = _result.asStateFlow()

    private val _benming = MutableStateFlow<MeiHua.GuaView?>(null)
    val benming: StateFlow<MeiHua.GuaView?> = _benming.asStateFlow()

    private val _question = MutableStateFlow("")
    val question: StateFlow<String> = _question.asStateFlow()

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    init {
        viewModelScope.launch {
            repository.userProfileFlow.collect { p ->
                _profile.value = p
                _benming.value = p?.let { MeiHua.benming(it) }
                recast()
            }
        }
    }

    fun setQuestion(q: String) {
        _question.value = q
        // 问题变化时按问题内容确定性重卦（不同问题对应不同卦），避免打字时反复跳动
        val seed = if (q.isBlank()) 0L else q.hashCode().toLong()
        _result.value = MeiHua.castNow(q, LocalDateTime.now(), seed)
    }

    fun recast() {
        // 重新起卦：每次用随机时辰，保证结果明显变化、有「回应感」
        _result.value = MeiHua.castNow(_question.value, LocalDateTime.now(), Random.nextLong())
    }
}
