package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.theme.NothingClockTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.material3.Text
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.test.onNodeWithText

import com.example.ui.viewmodel.CameraCutout

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSettingsDialogRenders() {
        composeTestRule.setContent {
            NothingClockTheme {
                CameraCutoutsSettingsDialog(
                    currentCutout = CameraCutout.NONE,
                    onCutoutSelected = {},
                    is24Hour = false,
                    on24HourToggled = {},
                    activeFontName = "NDOT57",
                    onFontChanged = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onNodeWithText("SETTINGS").assertExists()
    }
}
