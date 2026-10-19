package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.xuanji.app.data.local.*
import com.xuanji.app.domain.*
import com.xuanji.app.ui.history.PantryShoppingPanel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PantryShoppingInteractionTest {
    @get:Rule val compose = createComposeRule()
    private class Memory : PreferenceBridge {
        val data = mutableMapOf<String, String>()
        override suspend fun read(key: String) = data[key]
        override suspend fun write(key: String, value: String) { data[key] = value }
        override suspend fun delete(key: String) { data.remove(key) }
    }
    @Test fun real_purchase_fields_checkbox_and_explicit_transfer_save_only_synthetic_profile() {
        val store = DailyLifeStore(Memory())
        runBlocking { store.upsertAgenda("synthetic", DailyAgendaItem("meal", "2026-10-19", "", "番茄面", "meal", listOf("番茄"))) }
        compose.setContent { MaterialTheme {
            val revision by store.revision.collectAsState()
            var document by remember { mutableStateOf(DailyLifeDocument()) }
            LaunchedEffect(revision) { document = store.load("synthetic") }
            Column(Modifier.verticalScroll(rememberScrollState())) { PantryShoppingPanel(store, "synthetic", document, "2026-10-19") }
        } }
        compose.onNodeWithTag("purchase-edit-番茄").performScrollTo().performClick()
        compose.onNodeWithTag("purchase-quantity").performTextReplacement("2")
        compose.onNodeWithTag("purchase-unit").performTextReplacement("个")
        compose.onNodeWithTag("purchase-save").performClick()
        compose.onNodeWithTag("purchase-check-番茄").performScrollTo().performClick()
        compose.waitForIdle()
        runBlocking { assertTrue(store.load("synthetic").pantry.isEmpty()) }
        compose.onNodeWithTag("purchase-transfer-番茄").performScrollTo().performClick()
        compose.onNodeWithTag("pantry-expiry").performTextReplacement("2026-10-23")
        compose.onNodeWithTag("pantry-transfer-confirm").performClick()
        compose.waitForIdle()
        runBlocking {
            val document = store.load("synthetic")
            assertEquals("2", document.pantry.single().quantity)
            assertEquals("个", document.pantry.single().unit)
            assertEquals("2026-10-23", document.pantry.single().expiresOn)
            assertTrue(document.shoppingPurchases.single().transferredToPantry)
            assertTrue(store.load("other").pantry.isEmpty())
        }
    }
}
