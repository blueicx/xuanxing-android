package com.xuanji.app.ui.components

/** The full character is opt-in; the default presence is a low-attention orb. */
enum class CompanionPresence {
    OrbVisible,
    FullyHidden
}

data class CompanionUiState(
    val presence: CompanionPresence = CompanionPresence.OrbVisible,
    val stageOpen: Boolean = false
)
