package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.xuanji.app.domain.divination.Chakra

class ChakraViewModel : ViewModel() {
    val chakras: List<Chakra.ChakraInfo> = Chakra.all()
}
