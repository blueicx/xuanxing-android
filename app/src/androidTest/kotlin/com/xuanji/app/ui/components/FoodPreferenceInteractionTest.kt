package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.xuanji.app.data.local.FoodPreferenceStore
import com.xuanji.app.data.local.PreferenceBridge
import com.xuanji.app.domain.action.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FoodPreferenceInteractionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun unreadable_preferences_are_visible_and_cannot_be_overwritten_by_default_save() {
        var saved = false
        var cleared = false
        compose.setContent { MaterialTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FoodPreferenceEditor(FoodPreference(), { saved = true }, { cleared = true }, unreadable = true)
            }
        } }
        compose.onNodeWithText("保存偏好").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("清除偏好").performScrollTo().performClick()
        assertFalse(saved)
        assertTrue(cleared)
    }
    @Test fun explicit_practical_controls_persist_and_leave_other_profile_untouched() {
        val data = mutableMapOf<String, String>()
        val store = FoodPreferenceStore(object : PreferenceBridge {
            override suspend fun read(key: String) = data[key]
            override suspend fun write(key: String, value: String) { data[key] = value }
            override suspend fun delete(key: String) { data.remove(key) }
        })
        compose.setContent { MaterialTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FoodPreferenceEditor(FoodPreference(), { runBlocking { store.save("synthetic", it) } }, {})
            }
        } }
        fun toggle(label: String) {
            compose.onNodeWithText(label).performScrollTo().performClick().assertIsOn()
        }
        toggle("限定可用加热设备")
        toggle("灶具")
        toggle("只选简单做法")
        toggle("只选便当形式")
        toggle("优先使用可用同名库存")
        compose.onNodeWithText("保存偏好").performScrollTo().performClick()
        runBlocking {
            val saved = store.read("synthetic")
            assertEquals(setOf(KitchenEquipment.Stove), saved.availableEquipment)
            assertEquals(CookingDifficulty.Easy, saved.maxCookingDifficulty)
            assertTrue(saved.lunchboxOnly && saved.prioritizePantry)
            assertEquals(FoodPreference(), store.read("other"))
        }
    }
}
