package com.example.vagor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room.databaseBuilder
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Event

class EventListActivity : AppCompatActivity() {
    private var buttonAddEvent: Button? = null
    private var eventListView: ListView? = null
    private var db: AppDatabase? = null
    private var vacationId = 0
    private var eventObjects: List<Event>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeContentView(R.layout.activity_event_list)

        buttonAddEvent = findViewById<Button>(R.id.buttonAddEvent)
        eventListView = findViewById<ListView>(R.id.eventListView)

        vacationId = getIntent().getIntExtra("vacationId", -1)

        db = databaseBuilder<AppDatabase>(
            getApplicationContext(),
            AppDatabase::class.java, "vacation_database"
        )
            .allowMainThreadQueries()
            .build()

        buttonAddEvent!!.setOnClickListener(View.OnClickListener { _: View? ->
            val intent = Intent(this@EventListActivity, EventDetailActivity::class.java)
            intent.putExtra("vacationId", vacationId)
            startActivity(intent)
        })

        //Make event items clickable
        eventListView!!.setOnItemClickListener(OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val selectedEvent = eventObjects!![position]
            val intent = Intent(this@EventListActivity, EventDetailActivity::class.java)
            intent.putExtra("eventId", selectedEvent.id)
            intent.putExtra("title", selectedEvent.title)
            intent.putExtra("date", selectedEvent.date)
            intent.putExtra("vacationId", selectedEvent.vacationId)
            startActivity(intent)
        })
    }

    override fun onResume() {
        super.onResume()
        loadEvents()
    }

    private fun loadEvents() {
        eventObjects = db!!.eventDAO().getEventsForVacation(vacationId)
        val eventDisplayList = ArrayList<String>()

        for (event in eventObjects!!) {
            eventDisplayList.add(event.title + " - " + event.date)
        }

        val adapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            eventDisplayList
        )

        eventListView!!.setAdapter(adapter)
    }
}