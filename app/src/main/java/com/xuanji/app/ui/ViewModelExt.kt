package com.xuanji.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

/** 便捷的 ViewModel 工厂，便于向 ViewModel 注入 Repository */
@Composable
inline fun <reified T : ViewModel> xuanjiViewModel(crossinline factory: () -> T): T {
    return viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = factory() as T
    })
}
