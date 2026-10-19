package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.xuanji.app.domain.action.*
import org.junit.Rule
import org.junit.Test

/** Synthetic profile and saveable UI only: no personal DataStore access. */
class RegionComparisonInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun all_cities_and_individual_sources_can_be_opened_and_collapsed() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    LifeProfileSection(LifeProfile("region-fixture", emptyList(), emptyList(),
                        EnvironmentProfile(emptyList(), "", emptyList()), emptyList(), emptyList(),
                        ConfidenceLevel.Low, emptyList(), "合成资料"))
                }
            }
        }
        compose.onNodeWithTag("region-show-all", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("region-show-all").assertTextContains("收起城市列表", substring = true)
        compose.onNodeWithTag("region-source-suzhou", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("region-evidence-suzhou", useUnmergedTree = true).assertTextContains("NASA POWER / MERRA-2", substring = true)
        compose.onNodeWithTag("region-evidence-suzhou", useUnmergedTree = true).assertTextContains("非当地测站实测", substring = true)
        compose.onNodeWithTag("region-source-suzhou", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithTag("region-evidence-suzhou", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("region-show-all", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithTag("region-source-suzhou").assertDoesNotExist()
    }

    @Test fun selected_climate_and_expanded_source_survive_saved_instance_restore() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    RegionPreferenceComparisonSection("region-restore-fixture")
                }
            }
        }
        compose.onNodeWithText("气候·凉爽", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithTag("region-show-all", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithTag("region-source-amsterdam", useUnmergedTree = true).performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("region-evidence-amsterdam", useUnmergedTree = true).assertTextContains("KNMI", substring = true)
        compose.onNodeWithTag("region-source-amsterdam").assertTextContains("符合气候阈值", substring = true)
        compose.onNodeWithTag("region-show-all").assertTextContains("收起城市列表", substring = true)
    }
}
