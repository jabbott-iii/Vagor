package com.example.vagor

import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
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
