package com.xuanji.app.data.local

import com.xuanji.app.domain.external.ExternalContextConsent

class ExternalContextConsentStore(private val bridge: PreferenceBridge) {
    suspend fun read(): ExternalContextConsent {
        val raw = bridge.read(KEY) ?: return ExternalContextConsent()
        val values = raw.split('|')
        return ExternalContextConsent(
            networkEnabled = values.getOrNull(0) == "1",
            manualCityKey = values.getOrNull(1)?.takeIf { it.isNotBlank() },
            cacheEnabled = values.getOrNull(2) != "0"
        )
    }

    suspend fun save(consent: ExternalContextConsent) {
        bridge.write(KEY, listOf(if (consent.networkEnabled) "1" else "0", consent.manualCityKey.orEmpty(), if (consent.cacheEnabled) "1" else "0").joinToString("|"))
    }

    suspend fun clear() = bridge.delete(KEY)

    companion object { const val KEY = "external_context_consent_v1" }
}

fun android.content.Context.externalContextConsentStore(): ExternalContextConsentStore =
    ExternalContextConsentStore(DataStorePreferenceBridge(this))
