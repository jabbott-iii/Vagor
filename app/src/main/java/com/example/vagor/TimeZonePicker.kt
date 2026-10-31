package com.example.vagor

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.icu.text.TimeZoneFormat
import android.icu.text.TimeZoneNames
import android.icu.util.ULocale
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.Collator
import java.text.Normalizer
import java.util.Date
import java.util.Locale
import android.icu.util.TimeZone as IcuTimeZone

// A time zone as the event form offers it: its main city and country, such as "Tokyo, Japan",
// then its name and offset from GMT today, such as "Japan Standard Time (GMT+09:00)".
class TimeZoneChoice(val id: String, val label: String, val description: String) {
    val searchText = foldForSearch("$label $description $id")
}

object TimeZoneChoices {
    // Every zone that is the main ID for a place, plus UTC and [include] (the event's own zone,
    // which may be one the list leaves out, such as an older name), sorted by label.
    fun all(context: Context, include: String?): List<TimeZoneChoice> {
        val locale = context.displayLocale()
        val names = TimeZoneNames.getInstance(ULocale.forLocale(locale))
        val gmt = TimeZoneFormat.getInstance(ULocale.forLocale(locale))
        val now = Date()
        // ICU can list these itself (SystemTimeZoneType.CANONICAL_LOCATION) only from Android 9.
        val ids = IcuTimeZone.getAvailableIDs().mapNotNullTo(mutableSetOf()) { id ->
            IcuTimeZone.getCanonicalID(id)?.takeIf { region(it) != null }
        }
        ids += "Etc/UTC"
        if (include != null && ids.none { sameZone(it, include) }) ids += include
        val collator = Collator.getInstance(locale)
        return ids.map { choice(context, it, locale, names, gmt, now) }.sortedWith(compareBy(collator) { it.label })
    }

    // The label alone, such as "Tokyo, Japan", for shared text.
    fun label(context: Context, id: String): String {
        val locale = context.displayLocale()
        return label(context, id, locale, TimeZoneNames.getInstance(ULocale.forLocale(locale)), Date())
    }

    // The event form's field: the zone's name and city, such as "Japan Standard Time (Tokyo)". The
    // field always has a zone, so its "Time Zone" hint never shows, and a bare "Tokyo, Japan"
    // would read as where the event is.
    fun fieldText(context: Context, id: String): String {
        val canonicalId = canonical(id)
        val names = TimeZoneNames.getInstance(ULocale.forLocale(context.displayLocale()))
        val name = longName(canonicalId, names, Date()) ?: return label(context, id)
        val city = names.getExemplarLocationName(canonicalId) ?: return name
        return context.getString(R.string.time_zone_name_and_city, name, city)
    }

    // Whether two IDs name the same zone, such as "Asia/Calcutta" and "Asia/Kolkata".
    fun sameZone(a: String, b: String): Boolean = canonical(a) == canonical(b)

    private fun canonical(id: String): String = IcuTimeZone.getCanonicalID(id) ?: id

    private fun choice(
        context: Context,
        id: String,
        locale: Locale,
        names: TimeZoneNames,
        gmt: TimeZoneFormat,
        now: Date
    ): TimeZoneChoice {
        val canonicalId = canonical(id)
        val offset = gmt.format(TimeZoneFormat.Style.LOCALIZED_GMT, IcuTimeZone.getTimeZone(canonicalId), now.time)
        val name = longName(canonicalId, names, now)
        val description = if (name ==
            null
        ) {
            offset
        } else {
            context.getString(R.string.time_zone_name_and_offset, name, offset)
        }
        return TimeZoneChoice(id, label(context, canonicalId, locale, names, now), description)
    }

    private fun label(context: Context, id: String, locale: Locale, names: TimeZoneNames, now: Date): String {
        val canonicalId = canonical(id)
        val city = names.getExemplarLocationName(canonicalId)
            ?: return longName(canonicalId, names, now) ?: id
        // Some places are named after their country, such as the Pitcairn Islands.
        val country = country(canonicalId, locale)?.takeUnless { it.equals(city, ignoreCase = true) } ?: return city
        return context.getString(R.string.time_zone_city_and_country, city, country)
    }

    // "Eastern Time", or "Japan Standard Time" for a zone that keeps one time all year.
    private fun longName(id: String, names: TimeZoneNames, now: Date): String? =
        names.getDisplayName(id, TimeZoneNames.NameType.LONG_GENERIC, now.time)
            ?: names.getDisplayName(id, TimeZoneNames.NameType.LONG_STANDARD, now.time)

    private fun country(id: String, locale: Locale): String? {
        val region = region(id) ?: return null
        return Locale.Builder().setRegion(region).build().getDisplayCountry(locale).takeIf {
            it.isNotEmpty() &&
                it != region
        }
    }

    // The zone's country or region code, such as "JP"; null for zones that belong to none (ICU
    // gives those "001", the world), such as UTC and the fixed offsets.
    private fun region(id: String): String? {
        val region = try {
            IcuTimeZone.getRegion(id)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return region.takeIf { it.length == 2 }
    }
}

// Lower case without accents, so "sao paulo" finds São Paulo.
private fun foldForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(COMBINING_MARKS, "").lowercase(Locale.ROOT)

private val COMBINING_MARKS = Regex("""\p{Mn}+""")

// The event form's time zone list, with a search field above it. It returns the picked zone's ID
// through the fragment result REQUEST_KEY, which survives the screen being recreated.
class TimeZonePickerDialog : DialogFragment() {
    // A dialog's content has no parent view to inflate into.
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val selectedId = requireArguments().getString(ARG_SELECTED_ZONE_ID)
        val content = layoutInflater.inflate(R.layout.dialog_time_zone, null)
        val search = content.findViewById<EditText>(R.id.timeZoneSearch)
        val list = content.findViewById<ListView>(R.id.timeZoneList)
        val adapter = TimeZoneAdapter(TimeZoneChoices.all(requireContext(), selectedId), selectedId)
        list.adapter = adapter
        list.emptyView = content.findViewById(R.id.timeZoneNoMatches)
        list.setOnItemClickListener { _, _, position, _ ->
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                Bundle().apply {
                    putString(RESULT_ZONE_ID, adapter.getItem(position).id)
                }
            )
            dismiss()
        }
        search.doAfterTextChanged { adapter.filter(it.toString()) }
        // Opens with the event's zone in view.
        if (savedInstanceState == null) adapter.selectedPosition()?.let { list.setSelection(it) }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.event_time_zone)
            .setView(content)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        // The list shrinks above the keyboard while searching, and the keyboard waits for a tap.
        dialog.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        )
        return dialog
    }

    private inner class TimeZoneAdapter(private val all: List<TimeZoneChoice>, private val selectedId: String?) :
        BaseAdapter() {
        private var shown = all

        // Zones whose label, name or ID contains every word typed.
        fun filter(query: String) {
            val words = foldForSearch(query).split(' ').filter { it.isNotEmpty() }
            shown = if (words.isEmpty()) all else all.filter { choice -> words.all { it in choice.searchText } }
            notifyDataSetChanged()
        }

        fun selectedPosition(): Int? = selectedId?.let { id ->
            shown.indexOfFirst { TimeZoneChoices.sameZone(it.id, id) }.takeIf {
                it >=
                    0
            }
        }

        override fun getCount() = shown.size
        override fun getItem(position: Int) = shown[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = convertView ?: layoutInflater.inflate(R.layout.item_time_zone, parent, false)
            val choice = shown[position]
            val selected = selectedId != null && TimeZoneChoices.sameZone(choice.id, selectedId)
            row.findViewById<TextView>(R.id.timeZoneLabel).text = choice.label
            row.findViewById<TextView>(R.id.timeZoneDescription).text = choice.description
            // Marked with a tick, not by colour alone, and read out by screen readers.
            row.findViewById<View>(R.id.timeZoneSelected).isVisible = selected
            ViewCompat.setStateDescription(row, if (selected) getString(R.string.selected) else null)
            return row
        }
    }

    companion object {
        const val REQUEST_KEY = "timeZonePicker"
        const val RESULT_ZONE_ID = "zoneId"
        private const val ARG_SELECTED_ZONE_ID = "selectedZoneId"
        private const val TAG = "timeZonePicker"

        fun show(manager: FragmentManager, selectedZoneId: String) {
            // A second tap, or a screen reader's, while it's opening.
            if (manager.isStateSaved || manager.findFragmentByTag(TAG) != null) return
            val dialog = TimeZonePickerDialog()
            dialog.arguments = Bundle().apply { putString(ARG_SELECTED_ZONE_ID, selectedZoneId) }
            dialog.showNow(manager, TAG)
        }
    }
}

// The time zone dialog's content: it takes all the height the dialog offers rather than fitting
// its rows, so the dialog keeps its size and place while a search narrows the list. A dialog
// that shrank and moved as each letter was typed would move rows out from under a tap.
class FullHeightLinearLayout(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY)
        } else {
            heightMeasureSpec
        }
        super.onMeasure(widthMeasureSpec, height)
    }
}
