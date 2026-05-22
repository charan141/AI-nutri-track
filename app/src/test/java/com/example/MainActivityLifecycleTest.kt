package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class MainActivityLifecycleTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testLoginAndRecomposition() {
        // Find email field, enter text
        composeTestRule.onNodeWithTag("login_email_tf").performTextInput("test@gmail.com")
        // Click the submit button
        composeTestRule.onNodeWithTag("google_login_submit_btn").performClick()
        
        // Wait for idle to complete the login transitions and load the dashboard
        composeTestRule.waitForIdle()
    }
}
