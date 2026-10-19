package com.xuanji.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.xuanji.app.domain.game.*
import org.junit.Rule
import org.junit.Test

class SilkRoadMapInteractionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun plan_selection_does_not_spend_resources_until_departure_is_confirmed() {
        compose.setContent {
            var state by remember { mutableStateOf(SilkRoadRouteEngine.initial("silk-06")) }
            MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { SilkRoadMap(state, true) { state = SilkRoadRouteEngine.reduce(state, it) } } }
        }
        compose.onNodeWithTag("silkroad-plan-fast").performScrollTo().performClick()
        compose.onNodeWithText("长安 · 剩余 12 天 / 18 点补给").assertExists()
        compose.onNodeWithTag("silkroad-confirm-travel").performScrollTo().performClick()
        compose.onNodeWithText("敦煌 · 剩余 10 天 / 15 点补给").assertExists()
    }
    @Test fun clicking_geographic_node_and_station_list_choose_the_same_leg_after_camera_reset() {
        compose.setContent {
            var state by remember { mutableStateOf(SilkRoadRouteEngine.initial("silk-06")) }
            MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { SilkRoadMap(state, true) { state = SilkRoadRouteEngine.reduce(state, it) } } }
        }
        compose.onNodeWithTag("silkroad-zoom-in").performClick()
        compose.onNodeWithTag("silkroad-camera-reset").performClick()
        val bounds = compose.onNodeWithTag("silkroad-map-canvas").fetchSemanticsNode().boundsInRoot
        val point = SilkRoadCamera().toScreen(SilkRoadGeography.project("兰州"), bounds.width.toDouble(), bounds.height.toDouble())
        compose.onNodeWithTag("silkroad-map-canvas").performTouchInput { click(Offset(point.x.toFloat(), point.y.toFloat())) }
        compose.onNodeWithTag("silkroad-selected-city").assertTextContains("兰州", substring = true)
        compose.onNodeWithTag("silkroad-confirm-travel").performScrollTo().performClick()
        compose.onNodeWithText("兰州 · 剩余 11 天 / 17 点补给").assertExists()
    }
}
