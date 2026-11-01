package com.example.vagor

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.os.BundleCompat

// A date field. [date] holds the stored MM/dd/yyyy date (or an older loose one, or "" for none),
// and the field shows it the way the phone writes dates (dateText). Its text is only for show, so
// read and write [date]. The drop-down calendar fills it (attachCalendarDropdown), and it keeps
// its date across rotation itself.
class DateField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {
    var date: String = ""
        set(value) {
            field = value
            setText(context.dateText(value))
        }

    override fun onSaveInstanceState(): Parcelable = Bundle().apply {
        putParcelable(SUPER_STATE, super.onSaveInstanceState())
        putString(DATE, date)
    }

    // Shown again in the current language, which may have changed since.
    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is Bundle) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(BundleCompat.getParcelable(state, SUPER_STATE, Parcelable::class.java))
        date = state.getString(DATE).orEmpty()
    }

    private companion object {
        const val SUPER_STATE = "superState"
        const val DATE = "date"
    }
}
