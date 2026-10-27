package com.example.vagor

import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Spinner
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat

// A field filled only from a picker (the drop-down calendar, the clock or the time zone list) and
// never typed into, so what it holds is always valid. A tap or a screen reader's double-tap runs
// [pick], which [actionLabel] names for screen readers ("Choose date", for example).
fun EditText.makePickOnly(actionLabel: String, pick: () -> Unit) {
    keyListener = null
    // A tap clicks the field instead of focusing it, so the keyboard never opens for it.
    isFocusableInTouchMode = false
    isCursorVisible = false
    isLongClickable = false
    setOnClickListener {
        // Close the keyboard another field may have open; it would cover the picker.
        context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(windowToken, 0)
        pick()
    }
    ViewCompat.replaceAccessibilityAction(this, AccessibilityActionCompat.ACTION_CLICK, actionLabel, null)
    // To screen readers and other accessibility services the field is a drop-down list, not an
    // edit box: they can't type into it, and they read its name and then the picked value.
    ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
        override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            info.className = Spinner::class.java.name
            info.isEditable = false
            info.removeAction(AccessibilityActionCompat.ACTION_SET_TEXT)
            info.contentDescription = if (text.isNullOrEmpty()) {
                hint
            } else {
                context.getString(R.string.picked_field_with_value, hint, text)
            }
        }

        override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean =
            action != AccessibilityNodeInfo.ACTION_SET_TEXT && super.performAccessibilityAction(host, action, args)
    })
}
