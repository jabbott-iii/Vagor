package com.example.vagor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room.databaseBuilder
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Event
import com.example.vagor.entities.Vacation

// A vacation's page, opened by tapping it on the dashboard: its details, Edit and Share
// buttons, and its events in date order.
class EventListActivity : AppCompatActivity() {
    private var eventListView: ListView? = null
    private var emptyEvents: View? = null
    private var titleView: TextView? = null
    private var datesView: TextView? = null
    private var hotelView: TextView? = null
    private var db: AppDatabase? = null
    private var vacationId = 0
    private var vacation: Vacation? = null
    private var events: List<Event> = emptyList()
    private var eventAdapter: EventAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeContentView(R.layout.activity_event_list)

        eventListView = findViewById<ListView>(R.id.eventListView)
        val header = layoutInflater.inflate(R.layout.header_vacation, eventListView, false)
        val footer = layoutInflater.inflate(R.layout.footer_no_events, eventListView, false)
        eventListView!!.addHeaderView(header, null, false)
        eventListView!!.addFooterView(footer, null, false)
        eventAdapter = EventAdapter(this)
        eventListView!!.adapter = eventAdapter
        titleView = header.findViewById(R.id.vacationPageTitle)
        datesView = header.findViewById(R.id.vacationPageDates)
        hotelView = header.findViewById(R.id.vacationPageHotel)
        emptyEvents = footer.findViewById(R.id.emptyEvents)

        vacationId = getIntent().getIntExtra("vacationId", -1)

        db = databaseBuilder<AppDatabase>(
            getApplicationContext(),
            AppDatabase::class.java, "vacation_database"
        )
            .allowMainThreadQueries()
            .build()

        header.findViewById<Button>(R.id.buttonEditVacation).setOnClickListener {
            val current = vacation ?: return@setOnClickListener
            val intent = Intent(this@EventListActivity, VacationDetailActivity::class.java)
            intent.putExtra("vacationId", current.id)
            intent.putExtra("title", current.title)
            intent.putExtra("hotel", current.hotel)
            intent.putExtra("startDate", current.startDate)
            intent.putExtra("endDate", current.endDate)
            startActivity(intent)
        }

        header.findViewById<Button>(R.id.buttonShareVacation).setOnClickListener {
            val current = vacation ?: return@setOnClickListener
            val vacationText = getString(
                R.string.vacation_share_template,
                current.title,
                current.hotel,
                current.startDate,
                current.endDate
            )
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

        // The header and footer aren't events, so they map to no item.
        eventListView!!.setOnItemClickListener { parent, _, position, _ ->
            val selectedEvent = parent.getItemAtPosition(position) as? Event ?: return@setOnItemClickListener
            val intent = Intent(this@EventListActivity, EventDetailActivity::class.java)
            intent.putExtra("eventId", selectedEvent.id)
            intent.putExtra("title", selectedEvent.title)
            intent.putExtra("date", selectedEvent.date)
            intent.putExtra("vacationId", selectedEvent.vacationId)
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
        hotelView!!.text = getString(R.string.vacation_hotel, current.hotel)

        events = db!!.eventDAO().getEventsForVacation(vacationId).sortedWith(
            compareBy<Event, Long?>(nullsLast()) { DateValidators.parseLocalDate(it.date)?.toEpochDay() }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        )
        emptyEvents!!.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
        // Refilled rather than replaced, so the list keeps its scroll position.
        eventAdapter!!.clear()
        eventAdapter!!.addAll(events)
    }

    private class EventAdapter(context: Context) : ArrayAdapter<Event>(context, 0) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_event, parent, false)
            val event = getItem(position)!!
            view.findViewById<TextView>(R.id.eventTitle).text = event.title
            view.findViewById<TextView>(R.id.eventDate).text = event.date
            return view
        }
    }
}
