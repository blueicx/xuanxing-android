package com.xuanji.app.domain.game

/**
 * Reserved seam for the Go (weiqi) extension. Deliberately inert: without a real GTP
 * provider every query returns an explicit unavailable result instead of a fake game.
 */
object GoContracts {
    const val REASON_PROVIDER_NOT_ENABLED = GoRulesAvailability.PROVIDER_NOT_ENABLED

    fun availability(): GoRulesAvailability = GoRulesAvailability.unavailable(REASON_PROVIDER_NOT_ENABLED)
}
