package com.example.vagor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room.databaseBuilder
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Event

class EventDetailActivity : AppCompatActivity() {
    private var editEventTitle: EditText? = null
    private var editEventDate: EditText? = null
    private var buttonSaveEvent: Button? = null
    private var db: AppDatabase? = null
    private var vacationId = 0
    private var buttonDeleteEvent: Button? = null
    private var buttonShareEvent: Button? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeContentView(R.layout.activity_event_detail)

        editEventTitle = findViewById<EditText>(R.id.editEventTitle)
        editEventDate = findViewById<EditText>(R.id.editEventDate)
        buttonSaveEvent = findViewById<Button>(R.id.buttonSaveEvent)
        buttonDeleteEvent = findViewById<Button>(R.id.buttonDeleteEvent)
        buttonShareEvent = findViewById<Button>(R.id.buttonShareEvent)

        vacationId = getIntent().getIntExtra("vacationId", -1)

        db = databaseBuilder<AppDatabase>(
            getApplicationContext(),
            AppDatabase::class.java, "vacation_database"
        )
            .allowMainThreadQueries()
            .build()

        //event data
        val eventId = getIntent().getIntExtra("eventId", -1)
        val title = getIntent().getStringExtra("title")
        val date = getIntent().getStringExtra("date")

        if (eventId != -1) {
            editEventTitle!!.setText(title)
            editEventDate!!.setText(date)
        } else {
            // A new event has nothing to delete yet.
            buttonDeleteEvent!!.visibility = View.GONE
        }

        // The calendar offers only the days of the event's vacation.
        editEventDate!!.attachCalendarDropdown(
            earliest = { db!!.vacationDAO().getVacationById(vacationId)?.let { DateValidators.parseLocalDate(it.startDate) } },
            latest = { db!!.vacationDAO().getVacationById(vacationId)?.let { DateValidators.parseLocalDate(it.endDate) } }
        )

        buttonSaveEvent!!.setOnClickListener(View.OnClickListener setOnClickListener@{ _: View? ->
            val updatedTitle = editEventTitle!!.getText().toString().trim { it <= ' ' }
            val updatedDate = editEventDate!!.getText().toString().trim { it <= ' ' }

            if (updatedTitle.isEmpty() || updatedDate.isEmpty()) {
                Toast.makeText(this@EventDetailActivity, R.string.please_fill_all_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val vacation = db!!.vacationDAO().getVacationById(vacationId)

            if (vacation == null) {
                Toast.makeText(this@EventDetailActivity, R.string.associated_vacation_not_found, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!DateValidators.isDateWithinVacation(updatedDate, vacation.startDate, vacation.endDate)) {
                Toast.makeText(this@EventDetailActivity, R.string.event_date_out_of_range, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (eventId == -1) {
                val event = Event(updatedTitle, updatedDate, vacationId)
                val newId = db!!.eventDAO().insert(event).toInt()
                ReminderScheduler.scheduleEvent(this@EventDetailActivity, event.copy(id = newId))
                Toast.makeText(this@EventDetailActivity, R.string.event_saved, Toast.LENGTH_SHORT).show()
            } else {
                val event = Event(updatedTitle, updatedDate, vacationId, eventId)
                db!!.eventDAO().update(event)
                ReminderScheduler.scheduleEvent(this@EventDetailActivity, event)
                Toast.makeText(this@EventDetailActivity, R.string.event_updated, Toast.LENGTH_SHORT).show()
            }
            finish()
        })

        buttonDeleteEvent!!.setOnClickListener(View.OnClickListener { _: View? ->
            if (eventId != -1) {
                val event = Event(
                    editEventTitle!!.getText().toString().trim { it <= ' ' },
                    editEventDate!!.getText().toString().trim { it <= ' ' },
                    vacationId,
                    eventId
                )

                db!!.eventDAO().delete(event)
                ReminderScheduler.cancelEvent(this@EventDetailActivity, eventId)

                Toast.makeText(this@EventDetailActivity, R.string.event_deleted, Toast.LENGTH_SHORT).show()
                finish()
            }
        })

        //share button logic
        buttonShareEvent!!.setOnClickListener shareClick@{
            val vacation = db!!.vacationDAO().getVacationById(vacationId)
            if (vacation == null) {
                Toast.makeText(this@EventDetailActivity, R.string.associated_vacation_not_found, Toast.LENGTH_SHORT).show()
                return@shareClick
            }
            val shareText = getString(
                R.string.event_share_template,
                editEventTitle!!.getText().toString().trim { it <= ' ' },
                editEventDate!!.getText().toString().trim { it <= ' ' },
                vacation.title,
                vacation.hotel
            )
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.setType("text/plain")
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_event_details)))
        }
    }
}