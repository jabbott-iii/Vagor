package com.example.vagor

import android.accessibilityservice.AccessibilityService
import android.app.Instrumentation
import android.content.IntentFilter
import android.os.PatternMatcher
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue

// Checks that another app's screen, such as one of Android's settings screens, opened over Vagor,
// and leaves it with Back. Android handles the Back later, so this waits until that screen's
// window has closed: on a slow emulator the next test's screen could open first, and the Back
// would close it instead. The window, not the app: another screen of the same app may be behind.
fun leaveOtherAppsScreen() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val ownPackage = instrumentation.targetContext.packageName
    val shown = awaitFrontWindow { it.packageName != ownPackage }
    assertTrue(
        "Expected another app's screen on top, not ${shown?.packageName}",
        shown != null && shown.packageName != ownPackage
    )
    val shownWindow = shown!!.windowId
    instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    val front = awaitFrontWindow { it.windowId != shownWindow }
    assertTrue("Back didn't close ${shown.packageName}'s screen", front != null && front.windowId != shownWindow)
}

// Counts each start of Vagor's own App info screen, without stopping it. Check it with
// checkMonitorHit, and remove it with removeMonitor.
fun watchForOwnAppInfo(): Instrumentation.ActivityMonitor {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val filter = IntentFilter(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        addDataScheme("package")
        addDataSchemeSpecificPart(instrumentation.targetContext.packageName, PatternMatcher.PATTERN_LITERAL)
    }
    return instrumentation.addMonitor(filter, null, false)
}

// The window in front, once [done] holds for it; otherwise the last one seen within 5 seconds.
private fun awaitFrontWindow(done: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    val deadline = SystemClock.uptimeMillis() + 5000
    var front: AccessibilityNodeInfo? = null
    while (SystemClock.uptimeMillis() < deadline) {
        front = automation.rootInActiveWindow
        if (front != null && done(front)) break
        SystemClock.sleep(100)
    }
    return front
}
