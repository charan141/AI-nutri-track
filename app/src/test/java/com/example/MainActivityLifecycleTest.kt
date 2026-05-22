package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertNotNull
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
    fun testActivityInitialization() {
        val activity = composeTestRule.activity
        assertNotNull("MainActivity should be successfully created", activity)
        
        // Ensure email input field starts visible on the login screen
        composeTestRule.onNodeWithTag("login_email_tf").assertExists()
        composeTestRule.onNodeWithTag("google_login_submit_btn").assertExists()
    }
}
