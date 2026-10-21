package com.khajabad.app

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.os.Looper
import org.junit.Rule
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit
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

    /**
     * Robolectric's main looper is paused; advancing it (not Thread.sleep) is what
     * lets the splash `delay()` and the entrance animations actually finish.
     */
    private fun settle(seconds: Long = 4) {
        repeat(20) {
            shadowOf(Looper.getMainLooper()).idleFor(200, TimeUnit.MILLISECONDS)
            compose.waitForIdle()
        }
        compose.mainClock.advanceTimeBy(seconds * 1000)
        compose.waitForIdle()
    }

    private fun assertText(text: String) {
        val nodes = compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes()
        assert(nodes.isNotEmpty()) { "Text not found on screen: \"$text\"" }
    }

    @Test
    fun splashTransitionRendersHome() {
        // Splash finishes on its own after ~2.5 s, then Home animates in.
        settle()
        compose.onRoot().assertExists()
        assertText("Recent")
        assertText("khajabad")
        assertText("See all")
    }

    @Test
    fun homeShowsBothBundledBooks() {
        settle()
        assertText("ശജരത്തുൽ മുബാറക്ക")
        assertText("സ്വലാത്ത് ജൽസ")
    }

    @Test
    fun drawerOpensAndListsDestinations() {
        settle()
        compose.onAllNodesWithText("Library", useUnmergedTree = true)[0].performClick()
        settle(1)
        assertText("PDF ചേർക്കുക")
        assertText("സേവ് ചെയ്ത പേജുകൾ")
    }

    @Test
    fun scrollReachesNextEventsAndBrowse() {
        settle()
        // Home is one LazyColumn; scroll it until the lower sections compose.
        compose.onAllNodes(hasScrollAction(), useUnmergedTree = true)[0]
            .performScrollToNode(hasText("Next events"))
        assertText("Next events")
        compose.onAllNodes(hasScrollAction(), useUnmergedTree = true)[0]
            .performScrollToNode(hasText("Browse"))
        assertText("Browse")
    }

    @Test
    fun tabBarNavigatesToCounter() {
        settle()
        compose.onAllNodesWithText("Counter", useUnmergedTree = true)[0].performClick()
        settle(1)
        assertText("ദിക്ർ കൗണ്ടർ · ഓഫ്‌ലൈൻ")
        assertText("ടാപ്പ് ചെയ്യുക")
    }

    @Test
    fun tabBarNavigatesToSearch() {
        settle()
        compose.onAllNodesWithText("Search", useUnmergedTree = true)[0].performClick()
        settle(1)
        assertText("പുസ്തകങ്ങൾ, പേജുകൾ, ബുക്ക്മാർക്കുകൾ")
    }
}
