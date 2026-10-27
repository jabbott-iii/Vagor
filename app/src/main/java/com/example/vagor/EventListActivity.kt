package com.example.vagor

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.room.Room.databaseBuilder
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Event
import com.example.vagor.entities.Vacation

// A vacation's page, opened by tapping it on the dashboard: its details, Edit and Share
// buttons, and its events in date order.
class EventListActivity : AppCompatActivity() {
    private var eventRows: LinearLayout? = null
    private var emptyEvents: View? = null
    private var titleView: TextView? = null
    private var datesView: TextView? = null
    private var detailsView: TextView? = null
    private var db: AppDatabase? = null
    private var vacationId = 0
    private var vacation: Vacation? = null
    private var events: List<Event> = emptyList()
    // The event row that had keyboard focus before the screen was recreated (a rotation).
    private var focusedEventIdToRestore: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeContentView(R.layout.activity_event_list)

        eventRows = findViewById(R.id.eventRows)
        emptyEvents = findViewById(R.id.emptyEvents)
        titleView = findViewById(R.id.vacationPageTitle)
        datesView = findViewById(R.id.vacationPageDates)
        detailsView = findViewById(R.id.vacationPageTripDetails)

        vacationId = getIntent().getIntExtra("vacationId", -1)
        focusedEventIdToRestore = savedInstanceState?.getInt("focusedEventId", -1)?.takeIf { it != -1 }

        db = databaseBuilder<AppDatabase>(
            getApplicationContext(),
            AppDatabase::class.java, "vacation_database"
        )
            .allowMainThreadQueries()
            .build()

        findViewById<Button>(R.id.buttonEditVacation).setOnClickListener {
            val current = vacation ?: return@setOnClickListener
            val intent = Intent(this@EventListActivity, VacationDetailActivity::class.java)
            intent.putExtra("vacationId", current.id)
            intent.putExtra("title", current.title)
            intent.putExtra("details", current.details)
            intent.putExtra("startDate", current.startDate)
            intent.putExtra("endDate", current.endDate)
            startActivity(intent)
        }

        findViewById<Button>(R.id.buttonShareVacation).setOnClickListener {
            val current = vacation ?: return@setOnClickListener
            val withoutDetails = getString(R.string.vacation_share_template, current.title, current.startDate, current.endDate)
            val vacationText = if (current.details.isBlank()) {
                withoutDetails
            } else {
                getString(R.string.vacation_share_details, withoutDetails, current.details)
            }
            val shareText = if (events.isEmpty()) {
                vacationText
            } else {
                val eventLines = events.joinToString("\n") { getString(R.string.event_share_line, it.title, it.date) }
                getString(R.string.vacation_share_with_events, vacationText, eventLines)
            }
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.setType("text/plain")
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_vacation_details)))
        }

        findViewById<View>(R.id.buttonAddEvent).setOnClickListener {
            val intent = Intent(this@EventListActivity, EventDetailActivity::class.java)
            intent.putExtra("vacationId", vacationId)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reloaded every time, so edits made on the detail screens show up here. The vacation may
        // have been deleted from its edit screen.
        val current = db!!.vacationDAO().getVacationById(vacationId)
        if (current == null) {
            finish()
            return
        }
        vacation = current
        titleView!!.text = current.title
        datesView!!.text = vacationDateRange(current)
        detailsView!!.text = current.details
        detailsView!!.isVisible = current.details.isNotBlank()

        events = db!!.eventDAO().getEventsForVacation(vacationId).sortedWith(
            compareBy<Event, Long?>(nullsLast()) { DateValidators.parseLocalDate(it.date)?.toEpochDay() }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        )
        emptyEvents!!.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
        showEvents()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        (eventRows?.findFocus()?.tag as? Int)?.let { outState.putInt("focusedEventId", it) }
    }

    // A vacation has a handful of events, so they are plain rows in the scrolling page.
    private fun showEvents() {
        val rows = eventRows!!
        // Rebuilding the rows takes keyboard focus off the one the user opened; give it back.
        val focusedEventId = focusedEventIdToRestore ?: rows.findFocus()?.tag as? Int
        focusedEventIdToRestore = null
        val clearance = resources.getDimensionPixelSize(R.dimen.add_button_clearance)
        rows.removeAllViews()
        for (event in events) {
            val row = layoutInflater.inflate(R.layout.item_event, rows, false)
            row.findViewById<TextView>(R.id.eventTitle).text = event.title
            row.findViewById<TextView>(R.id.eventDate).text = event.date
            row.setOnClickListener {
                val intent = Intent(this@EventListActivity, EventDetailActivity::class.java)
                intent.putExtra("eventId", event.id)
                intent.putExtra("title", event.title)
                intent.putExtra("date", event.date)
                intent.putExtra("vacationId", event.vacationId)
                startActivity(intent)
            }
            row.tag = event.id
            // Keyboard focus scrolls a row only to the screen's bottom edge, under the Add Event
            // button; bring it up above the button.
            row.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) view.requestRectangleOnScreen(Rect(0, 0, view.width, view.height + clearance))
            }
            rows.addView(row)
        }
        focusedEventId?.let { rows.findViewWithTag<View>(it)?.requestFocus() }
    }
}
