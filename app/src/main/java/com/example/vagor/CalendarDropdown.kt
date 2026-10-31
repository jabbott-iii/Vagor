package com.example.vagor

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Rect
import android.text.format.DateFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

// Date fields are filled from a month calendar that drops down below the field, like Material 3's
// docked date picker. They can't be typed into, so a picked date is always a valid MM/dd/yyyy
// date. The calendar offers only days from earliest to latest when those are given.
//
// The app draws the calendar itself rather than using the framework CalendarView, which follows
// the phone's calendar system (a Buddhist-era year in Thai, for example) and marks today by colour
// alone. This one is always Gregorian, like the stored dates, and marks today with a ring.
fun EditText.attachCalendarDropdown(earliest: () -> LocalDate? = { null }, latest: () -> LocalDate? = { null }) {
    makePickOnly(context.getString(R.string.choose_date)) { showCalendarDropdown(earliest(), latest()) }
}

// A popup's content has no parent view to inflate into.
@SuppressLint("InflateParams")
private fun EditText.showCalendarDropdown(earliest: LocalDate?, latest: LocalDate?) {
    // A screen reader can still activate the field behind an open calendar; don't open a second.
    if (!isAttachedToWindow || (getTag(R.id.calendar) as? PopupWindow)?.isShowing == true) return

    // A range that ends before it starts comes from bad saved dates; offer every day instead.
    var first = earliest?.coerceIn(CalendarMonth.FIRST_DAY, CalendarMonth.LAST_DAY) ?: CalendarMonth.FIRST_DAY
    var last = latest?.coerceIn(CalendarMonth.FIRST_DAY, CalendarMonth.LAST_DAY) ?: CalendarMonth.LAST_DAY
    if (last.isBefore(first)) {
        first = CalendarMonth.FIRST_DAY
        last = CalendarMonth.LAST_DAY
    }
    // An empty field opens on its earliest allowed day (the start date, or the vacation's first
    // day), or on today when it has none.
    val selected = (DateValidators.parseLocalDate(text.toString()) ?: earliest ?: LocalDate.now()).coerceIn(first, last)

    val content = LayoutInflater.from(context).inflate(R.layout.popup_calendar, null)
    // Screen readers announce the calendar by the field's name when it appears.
    ViewCompat.setAccessibilityPaneTitle(content, hint)
    val width = minOf(width, resources.getDimensionPixelSize(R.dimen.calendar_dropdown_max_width))
    val popup = PopupWindow(content, width, ViewGroup.LayoutParams.WRAP_CONTENT, true)
    popup.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
    popup.elevation = resources.getDimension(R.dimen.calendar_dropdown_elevation)
    popup.setBackgroundDrawable(
        MaterialShapeDrawable(
            ShapeAppearanceModel.builder()
                .setAllCornerSizes(resources.getDimension(R.dimen.calendar_dropdown_corner))
                .build()
        ).apply {
            fillColor = ColorStateList.valueOf(
                MaterialColors.getColor(
                    this@showCalendarDropdown,
                    com.google.android.material.R.attr.colorSurfaceContainerHigh
                )
            )
        }
    )

    val month = MonthGrid(content, first, last, selected) { picked ->
        setText(DateValidators.formatDate(picked))
        // Not during the day's own click handling.
        post { popup.dismiss() }
    }

    // Close with the screen (a rotation, for example) so the popup's window isn't leaked.
    val closeWithField = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {}
        override fun onViewDetachedFromWindow(view: View) = popup.dismiss()
    }
    // Fit again whenever the screen's layout changes, such as when the keyboard closes.
    val refit = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        if (popup.isShowing) popup.update(this, width, fittedHeight(content, width))
    }
    addOnAttachStateChangeListener(closeWithField)
    rootView.addOnLayoutChangeListener(refit)
    setTag(R.id.calendar, popup)
    popup.setOnDismissListener {
        removeOnAttachStateChangeListener(closeWithField)
        rootView.removeOnLayoutChangeListener(refit)
        setTag(R.id.calendar, null)
    }

    // An explicit height, rather than wrap_content, lets the popup open above the field when
    // there's no room below.
    popup.height = fittedHeight(content, width)
    popup.showAsDropDown(this)
    // Keyboard and D-pad users start on the picked day, once the popup's window is attached.
    if (!isInTouchMode) content.post { month.focusSelectedDay() }
}

// The whole calendar when the screen is tall enough (the popup goes below the field if it fits
// there, else above it, else over it); on a shorter screen, the screen's height, and the calendar
// scrolls inside the popup.
private fun View.fittedHeight(content: View, width: Int): Int {
    content.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    val visible = Rect()
    getWindowVisibleDisplayFrame(visible)
    return minOf(content.measuredHeight, visible.height())
}

// One month of days with previous and next month buttons. Days outside first..last are shown but
// can't be picked.
private class MonthGrid(
    content: View,
    private val first: LocalDate,
    private val last: LocalDate,
    private val selected: LocalDate,
    private val onPick: (LocalDate) -> Unit
) {
    private val context = content.context
    private val locale: Locale =
        ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.getDefault()
    private val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    private val titleFormat = monthTitleFormat(locale)
    private val dayDescription = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        .withLocale(locale)
        .withDecimalStyle(DecimalStyle.of(locale))
    private val title = content.findViewById<TextView>(R.id.monthTitle)
    private val previous = content.findViewById<View>(R.id.previousMonth)
    private val next = content.findViewById<View>(R.id.nextMonth)
    private val days = mutableListOf<TextView>()
    private val today = LocalDate.now()
    private var shown = YearMonth.from(selected)

    init {
        val weekdays = content.findViewById<LinearLayout>(R.id.weekdays)
        val weekdayColor = MaterialColors.getColor(content, com.google.android.material.R.attr.colorOnSurfaceVariant)
        for (weekday in CalendarMonth.weekdays(firstDayOfWeek)) {
            val label = TextView(context)
            label.gravity = Gravity.CENTER
            label.text = weekday.getDisplayName(TextStyle.NARROW_STANDALONE, locale)
            label.setTextColor(weekdayColor)
            weekdays.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }

        val inflater = LayoutInflater.from(context)
        val rows = content.findViewById<LinearLayout>(R.id.days)
        repeat(CalendarMonth.CELLS / 7) {
            val row = LinearLayout(context)
            repeat(7) {
                val day = inflater.inflate(R.layout.item_calendar_day, row, false) as TextView
                row.addView(day)
                days += day
            }
            rows.addView(row, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        previous.setOnClickListener { show(shown.minusMonths(1)) }
        next.setOnClickListener { show(shown.plusMonths(1)) }
        show(shown)
    }

    fun focusSelectedDay() {
        days.firstOrNull { it.isSelected }?.requestFocus()
    }

    // Moves screen-reader focus only to keep it from being lost when the focused button hides.
    @SuppressLint("AccessibilityFocus")
    private fun show(month: YearMonth) {
        val showPrevious = month > YearMonth.from(first)
        val showNext = month < YearMonth.from(last)
        // Hiding the month button that has keyboard or screen-reader focus (at the first or last
        // allowed month) would drop that focus, so it is moved to a day before the button hides.
        val leaving = when {
            !showPrevious && previous.isVisible -> previous
            !showNext && next.isVisible -> next
            else -> null
        }
        val hadFocus = leaving?.isFocused == true
        val hadAccessibilityFocus = leaving?.createAccessibilityNodeInfo()?.isAccessibilityFocused == true

        shown = month
        title.text = month.format(titleFormat)
        CalendarMonth.cells(month, firstDayOfWeek).forEachIndexed { index, date ->
            val day = days[index]
            if (date == null) {
                day.visibility = View.INVISIBLE
                day.text = null
                day.contentDescription = null
                day.setOnClickListener(null)
                return@forEachIndexed
            }
            val isToday = date == today
            day.visibility = View.VISIBLE
            day.text = String.format(locale, "%d", date.dayOfMonth)
            day.contentDescription = date.format(dayDescription)
            ViewCompat.setStateDescription(day, if (isToday) context.getString(R.string.today) else null)
            day.isSelected = date == selected
            day.isEnabled = !date.isBefore(first) && !date.isAfter(last)
            day.background = calendarDayBackground(context, isToday)
            day.setTextColor(
                ContextCompat.getColorStateList(
                    context,
                    if (isToday) R.color.calendar_today_text else R.color.calendar_day_text
                )
            )
            // Accessibility services can click a disabled view, so check again.
            day.setOnClickListener { if (day.isEnabled) onPick(date) }
        }

        if (hadFocus || hadAccessibilityFocus) {
            val shownDays = days.filter { it.isVisible && it.isEnabled }
            val target = shownDays.firstOrNull { it.isSelected } ?: shownDays.firstOrNull()
                ?: (if (leaving === next) previous.takeIf { showPrevious } else next.takeIf { showNext })
            if (hadFocus) target?.requestFocus()
            if (hadAccessibilityFocus) {
                target?.performAccessibilityAction(
                    AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
                    null
                )
            }
        }
        // Invisible rather than gone, so the title stays centred.
        previous.visibility = if (showPrevious) View.VISIBLE else View.INVISIBLE
        next.visibility = if (showNext) View.VISIBLE else View.INVISIBLE
    }
}

// "October 2026" in the locale's own order. The pattern comes from the locale without its Unicode
// extensions, because one that asks for another calendar system (-u-ca-chinese, for example) gives
// pattern letters java.time can't format; the calendar is always Gregorian.
private fun monthTitleFormat(locale: Locale): DateTimeFormatter {
    val pattern = DateFormat.getBestDateTimePattern(locale.stripExtensions(), "MMMMyyyy")
    val format = try {
        DateTimeFormatter.ofPattern(pattern, locale)
    } catch (_: IllegalArgumentException) {
        DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    }
    // The locale's own digits, like the day numbers.
    return format.withDecimalStyle(DecimalStyle.of(locale))
}
