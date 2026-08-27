package com.myt.ui.gauge

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.myt.ui.ConnectionErrorKind
import com.myt.ui.theme.MyTTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectionErrorBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sleepingBanner_isVisible() {
        composeRule.setContent {
            MyTTheme {
                ConnectionErrorBanner(
                    kind = ConnectionErrorKind.Sleeping,
                    onRetry = {},
                )
            }
        }
        composeRule.onNodeWithText("차량이 절전 중입니다").assertIsDisplayed()
    }

    @Test
    fun noneKind_rendersNothing() {
        composeRule.setContent {
            MyTTheme {
                ConnectionErrorBanner(
                    kind = ConnectionErrorKind.None,
                    onRetry = {},
                )
            }
        }
        // ConnectionErrorBanner returns immediately — no error text nodes.
        composeRule.onNodeWithText("Fleet API 오류").assertDoesNotExist()
    }
}
