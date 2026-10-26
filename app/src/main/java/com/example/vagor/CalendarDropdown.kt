package com.example.vagor

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Rect
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CalendarView
import android.widget.EditText
import android.widget.PopupWindow
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// The calendar offers 1900 to 2099. CalendarView's own range ends in 2100, but Android 8-10 show a
// February 29 in 2100, which isn't a real date.
private val FIRST_DAY: LocalDate = LocalDate.of(1900, 1, 1)
private val LAST_DAY: LocalDate = LocalDate.of(2099, 12, 31)

// Date fields are filled from a month calendar that drops down below the field, like Material 3's
// docked date picker. They can't be typed into, so a picked date is always a valid MM/dd/yyyy
// date. The calendar offers only days from earliest to latest when those are given.
fun EditText.attachCalendarDropdown(
    earliest: () -> LocalDate? = { null },
    latest: () -> LocalDate? = { null },
) {
    keyListener = null
    // A tap clicks the field instead of focusing it, so the keyboard never opens for it.
    isFocusableInTouchMode = false
    isCursorVisible = false
    isLongClickable = false
    setOnClickListener { showCalendarDropdown(earliest(), latest()) }
    ViewCompat.replaceAccessibilityAction(
        this, AccessibilityActionCompat.ACTION_CLICK, context.getString(R.string.choose_date), null
    )
}

// A popup's content has no parent view to inflate into.
@SuppressLint("InflateParams")
private fun EditText.showCalendarDropdown(earliest: LocalDate?, latest: LocalDate?) {
    if (!isAttachedToWindow) return
    // Close the keyboard another field may have open; it would cover the calendar.
    context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(windowToken, 0)

    val themedContext = ContextThemeWrapper(context, R.style.ThemeOverlay_Vagor_CalendarDropdown)
    val content = LayoutInflater.from(themedContext).inflate(R.layout.popup_calendar, null)
    val calendar = content.findViewById<CalendarView>(R.id.calendar)

    // A new CalendarView starts at 1900-2100, so setting the minimum and then the maximum never
    // passes through a range that ends before it starts (which CalendarView doesn't survive).
    // A range that ends before it starts comes from bad saved dates; offer every day instead.
    var first = earliest?.coerceIn(FIRST_DAY, LAST_DAY) ?: FIRST_DAY
    var last = latest?.coerceIn(FIRST_DAY, LAST_DAY) ?: LAST_DAY
    if (last.isBefore(first)) {
        first = FIRST_DAY
        last = LAST_DAY
    }
    calendar.minDate = calendarMillis(first)
    calendar.maxDate = calendarMillis(last)
    // An empty field opens on its earliest allowed day (the start date, or the vacation's first
    // day), or on today when it has none.
    val selected = DateValidators.parseLocalDate(text.toString()) ?: earliest ?: LocalDate.now()
    calendar.setDate(calendarMillis(selected.coerceIn(first, last)), false, true)
    // Screen readers announce the calendar by the field's name when it appears.
    ViewCompat.setAccessibilityPaneTitle(content, hint)

    val width = minOf(width, resources.getDimensionPixelSize(R.dimen.calendar_dropdown_max_width))
    val popup = PopupWindow(content, width, resources.getDimensionPixelSize(R.dimen.calendar_dropdown_estimated_height), true)
    popup.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
    popup.elevation = resources.getDimension(R.dimen.calendar_dropdown_elevation)
    popup.setBackgroundDrawable(
        MaterialShapeDrawable(
            ShapeAppearanceModel.builder()
                .setAllCornerSizes(resources.getDimension(R.dimen.calendar_dropdown_corner))
                .build()
        ).apply {
            fillColor = ColorStateList.valueOf(
                MaterialColors.getColor(this@showCalendarDropdown, com.google.android.material.R.attr.colorSurfaceContainerHigh)
            )
        }
    )

    // Fires on every tap, including on the day that's already selected. Month is 0-based.
    calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
        setText(DateValidators.formatDate(LocalDate.of(year, month + 1, dayOfMonth)))
        // Not during the calendar's own touch handling.
        calendar.post { popup.dismiss() }
    }

    // Close with the screen (a rotation, for example) so the popup's window isn't leaked.
    val closeWithField = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {}
        override fun onViewDetachedFromWindow(view: View) = popup.dismiss()
    }
    // The calendar's height is known only once it is on screen (its month pager is empty until
    // then), so the popup opens at an estimate and is resized when the calendar lays out. An
    // explicit height also lets the popup open above the field when there's no room below.
    var calendarHeight = popup.height
    calendar.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
        if (view.height > 0 && view.height != calendarHeight) {
            calendarHeight = view.height
            if (popup.isShowing) popup.update(this, width, fittedHeight(calendarHeight))
        }
    }
    // Fit again whenever the screen's layout changes, such as when the keyboard closes.
    val refit = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        if (popup.isShowing) popup.update(this, width, fittedHeight(calendarHeight))
    }
    addOnAttachStateChangeListener(closeWithField)
    rootView.addOnLayoutChangeListener(refit)
    popup.setOnDismissListener {
        removeOnAttachStateChangeListener(closeWithField)
        rootView.removeOnLayoutChangeListener(refit)
    }

    popup.height = fittedHeight(calendarHeight)
    popup.showAsDropDown(this)
}

// The whole calendar when the screen is tall enough (the popup goes below the field if it fits
// there, else above it, else over it); on a shorter screen, the screen's height, and the calendar
// scrolls inside the popup.
private fun View.fittedHeight(calendarHeight: Int): Int {
    val visible = Rect()
    getWindowVisibleDisplayFrame(visible)
    return minOf(calendarHeight, visible.height())
}

// CalendarView works in milliseconds in the default time zone. Noon keeps clear of daylight
// saving changes, which happen around midnight.
private fun calendarMillis(date: LocalDate): Long =
    date.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
