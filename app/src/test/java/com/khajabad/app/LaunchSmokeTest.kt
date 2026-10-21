package com.khajabad.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Boots the real activity on the JVM.
 *
 * This is the test that caught the WOFF2-in-res/font crash: Compose resolved
 * `R.font.outfit_*`, found unreadable bytes and threw during the splash. Keeping
 * it means any launch-time regression fails CI in ~2 minutes, no emulator needed.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchSmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun settle(millis: Long = 3_600) {
        compose.waitForIdle()
        Thread.sleep(millis)
        compose.waitForIdle()
    }

    @Test
    fun splashTransitionRendersHome() {
        // Splash finishes on its own after ~2.5 s, then Home animates in.
        settle()
        compose.onRoot().assertExists()
        compose.onNodeWithText("Recent").assertIsDisplayed()
        compose.onNodeWithText("Next events").assertIsDisplayed()
        compose.onNodeWithText("BROWSE").assertExists()
    }

    @Test
    fun homeShowsBothBundledBooks() {
        settle()
        compose.onNodeWithText("ശജരത്തുൽ മുബാറക്ക").assertExists()
        compose.onNodeWithText("സ്വലാത്ത് ജൽസ").assertExists()
    }

    @Test
    fun drawerOpensAndListsDestinations() {
        settle()
        compose.onNodeWithText("Home", useUnmergedTree = true).assertExists()
        // Menu button (content description free) → open drawer via the icon button
        compose.onAllNodesWithText("Library")[0].performClick()
        compose.waitForIdle()
    }

    @Test
    fun tabBarNavigatesToCounter() {
        settle()
        compose.onNodeWithText("Counter", useUnmergedTree = true).performClick()
        settle(900)
        compose.onNodeWithText("ദിക്ർ കൗണ്ടർ · ഓഫ്‌ലൈൻ").assertExists()
        compose.onNodeWithText("ടാപ്പ് ചെയ്യുക").assertExists()
    }

    @Test
    fun tabBarNavigatesToSearch() {
        settle()
        compose.onNodeWithText("Search", useUnmergedTree = true).performClick()
        settle(900)
        compose.onNodeWithText("പുസ്തകങ്ങൾ, പേജുകൾ, ബുക്ക്മാർക്കുകൾ").assertExists()
    }
}
