package com.example.vagor

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

// Screens draw behind the status and navigation bars (Android 15+, with no opt-out from
// targetSdk 36), so the layout's root is padded by the bars and any display cutout on top of its
// own padding. While the keyboard is open the root ends at the keyboard's top edge, so the
// scrolling detail forms (adjustResize) can bring every field and button above it. System bar
// icons follow the app's light or dark theme.
fun AppCompatActivity.setEdgeToEdgeContentView(@LayoutRes layoutResId: Int) {
    enableEdgeToEdge()
    setContentView(layoutResId)

    val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
    val left = root.paddingLeft
    val top = root.paddingTop
    val right = root.paddingRight
    val bottom = root.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + if (keyboard > 0) 0 else bars.bottom)
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = keyboard }
        // Passed on rather than consumed: on Android 10 and lower, consuming here would stop the
        // insets from reaching sibling views.
        insets
    }
}

// For a screen with a bottom navigation bar, whose background reaches the bottom edge of the screen
// behind the navigation bar. The content view above it is padded for the status bar and the sides;
// the bar is padded for the navigation bar and the sides. This replaces the bar's own inset handling,
// which would also add the keyboard's height under adjustResize.
fun AppCompatActivity.setEdgeToEdgeContentViewWithBottomBar(
    @LayoutRes layoutResId: Int,
    @IdRes contentId: Int,
    @IdRes barId: Int,
) {
    // The bar gives the navigation bar its background, so no translucent scrim is drawn over it
    // with 3-button navigation.
    enableEdgeToEdge(navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
    setContentView(layoutResId)
    val types = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()

    val content = findViewById<View>(contentId)
    val contentLeft = content.paddingLeft
    val contentTop = content.paddingTop
    val contentRight = content.paddingRight
    ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
        val bars = insets.getInsets(types)
        view.setPadding(contentLeft + bars.left, contentTop + bars.top, contentRight + bars.right, view.paddingBottom)
        // The fragment container passes these on to the tab's layout, which must not pad again.
        insets
    }

    val bar = findViewById<View>(barId)
    val barLeft = bar.paddingLeft
    val barRight = bar.paddingRight
    val barBottom = bar.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(bar) { view, insets ->
        val bars = insets.getInsets(types)
        view.setPadding(barLeft + bars.left, view.paddingTop, barRight + bars.right, barBottom + bars.bottom)
        insets
    }
}
