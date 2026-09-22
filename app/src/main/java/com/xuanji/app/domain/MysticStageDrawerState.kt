package com.xuanji.app.domain

/** The stage opens quietly; content expands only after an explicit action. */
enum class MysticDrawerState { Peek, Expanded }

data class MysticStageDrawerSession(
    val activeCharacterId: MysticCharacterId,
    val drawerStates: Map<MysticCharacterId, MysticDrawerState>,
    val requestToken: Long = 0L
) {
    val currentDrawerState: MysticDrawerState
        get() = drawerStates[activeCharacterId] ?: MysticDrawerState.Peek

    companion object {
        fun initial(characterId: MysticCharacterId): MysticStageDrawerSession =
            MysticStageDrawerSession(
                activeCharacterId = characterId,
                drawerStates = MysticCharacterId.entries.associateWith { MysticDrawerState.Peek }
            )
    }
}

sealed interface MysticStageDrawerEvent {
    data object Expand : MysticStageDrawerEvent
    data object Collapse : MysticStageDrawerEvent
    data class SelectCharacter(val characterId: MysticCharacterId) : MysticStageDrawerEvent
    data class NewRequest(val token: Long) : MysticStageDrawerEvent
    data class ApplyAction(val token: Long, val state: MysticDrawerState) : MysticStageDrawerEvent
}

fun reduceMysticStageDrawer(
    state: MysticStageDrawerSession,
    event: MysticStageDrawerEvent
): MysticStageDrawerSession = when (event) {
    MysticStageDrawerEvent.Expand -> state.withCurrent(MysticDrawerState.Expanded)
    MysticStageDrawerEvent.Collapse -> state.withCurrent(MysticDrawerState.Peek)
    is MysticStageDrawerEvent.SelectCharacter -> state.copy(activeCharacterId = event.characterId)
    is MysticStageDrawerEvent.NewRequest -> state.copy(requestToken = event.token)
    is MysticStageDrawerEvent.ApplyAction -> if (event.token == state.requestToken) {
        state.withCurrent(event.state)
    } else state
}

private fun MysticStageDrawerSession.withCurrent(value: MysticDrawerState): MysticStageDrawerSession =
    copy(drawerStates = drawerStates + (activeCharacterId to value))
