package com.example.vagor

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors

// The background of a day in the drop-down calendar: a filled primary-blue circle when it's the
// picked day, a ring when it's today (so today isn't marked by colour alone), a rounded-square
// ring when it has keyboard focus (calendar_focus_ring, which fills the cell), and a round ripple.
// The circles are drawn in code so they shrink to fit a narrow cell at large font sizes instead
// of being cut off at the sides.
fun calendarDayBackground(context: Context, isToday: Boolean): Drawable {
    val resources = context.resources
    val maxDiameter = resources.getDimension(R.dimen.calendar_day_circle)
    val clearance = resources.getDimension(R.dimen.calendar_day_circle_clearance)
    val primary = ColorStateList.valueOf(
        MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, "colorPrimary")
    )
    val todayRing = ContextCompat.getColorStateList(context, R.color.calendar_today_ring)!!
    val ringWidth = resources.getDimension(R.dimen.calendar_today_ring_width)

    // A drawable can sit in only one place, so each state gets its own.
    fun circle() = DayCircle(primary, 0f, maxDiameter, clearance)
    fun ring() = DayCircle(todayRing, ringWidth, maxDiameter, clearance)
    fun focusRing() = ContextCompat.getDrawable(context, R.drawable.calendar_focus_ring)!!

    val focused = android.R.attr.state_focused
    val selected = android.R.attr.state_selected
    val states = StateListDrawable()
    states.addState(intArrayOf(focused, selected), LayerDrawable(arrayOf(focusRing(), circle())))
    states.addState(intArrayOf(selected), circle())
    states.addState(intArrayOf(focused), if (isToday) LayerDrawable(arrayOf(focusRing(), ring())) else focusRing())
    if (isToday) states.addState(intArrayOf(), ring())

    val mask = DayCircle(ColorStateList.valueOf(Color.WHITE), 0f, maxDiameter, clearance)
    return RippleDrawable(ContextCompat.getColorStateList(context, R.color.calendar_ripple)!!, states, mask)
}

// A circle centred in its bounds: maxDiameter across, or less when the bounds minus the clearance
// on each side are smaller. Filled, or a ring when strokeWidth is above zero.
class DayCircle(
    private val color: ColorStateList,
    private val strokeWidth: Float,
    private val maxDiameter: Float,
    private val clearance: Float
) : Drawable() {
    private var drawableAlpha = 255
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.style = if (strokeWidth > 0f) Paint.Style.STROKE else Paint.Style.FILL
        it.strokeWidth = strokeWidth
    }

    override fun draw(canvas: Canvas) {
        val diameter = minOf(maxDiameter, bounds.width() - 2 * clearance, bounds.height() - 2 * clearance)
        if (diameter <= 0f) return
        val stateColor = color.getColorForState(state, color.defaultColor)
        paint.color = stateColor
        paint.alpha = Color.alpha(stateColor) * drawableAlpha / 255
        canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), (diameter - strokeWidth) / 2, paint)
    }

    override fun isStateful() = color.isStateful

    override fun onStateChange(state: IntArray): Boolean {
        invalidateSelf()
        return color.isStateful
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
