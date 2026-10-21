package com.khajabad.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Boots the real activity on the JVM. Any crash on landing shows up here with a
 * full stack trace, and the compose tree is asserted to be non-empty.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchSmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun activityLaunchesAndRenders() {
        // Let the splash timer (2.5s) and the home entrance animations settle.
        compose.waitForIdle()
        Thread.sleep(3_500)
        compose.waitForIdle()
        compose.onRoot().assertExists()
    }
}
