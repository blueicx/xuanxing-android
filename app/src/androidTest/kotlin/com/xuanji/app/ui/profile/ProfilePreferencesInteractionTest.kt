package com.xuanji.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.domain.action.ActivityPreference
import com.xuanji.app.domain.action.EnergyLevel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfilePreferencesInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun profile_readiness_shows_saved_inputs_and_does_not_claim_missing_coordinates_are_known() {
        compose.setContent {
            MaterialTheme {
                Column {
                    ProfileDataReadinessCard(UserProfile(1990, 5, 3, 8, 20, "浙江省/杭州市/西湖区"))
                }
            }
        }

        compose.onNodeWithTag("profile-data-readiness").assertExists()
        compose.onNodeWithTag("profile-readiness-date").assertExists()
        compose.onNodeWithTag("profile-readiness-time").assertExists()
        compose.onNodeWithTag("profile-readiness-place").assertExists()
        compose.onNodeWithText("未保存经纬度；只使用已选地点名称，不推测精确坐标。").assertExists()
    }

    @Test
    fun activity_preference_editor_saves_only_the_user_selected_constraints() {
        val saved = mutableStateOf<ActivityPreference?>(null)
        compose.setContent {
            MaterialTheme {
                ActivityPreferenceEditor(
                    preference = ActivityPreference(),
                    unreadable = false,
                    onSave = { saved.value = it },
                    onClear = { saved.value = ActivityPreference() }
                )
            }
        }

        compose.onNodeWithText("轻松").performClick()
        compose.onNodeWithTag("activity-preference-indoor").performClick()
        compose.onNodeWithTag("activity-preference-duration").performTextInput("35")
        compose.onNodeWithText("保存活动偏好").performClick()

        assertEquals(ActivityPreference(EnergyLevel.Low, indoorOnly = true, maxDurationMinutes = 35), saved.value)
    }
}
