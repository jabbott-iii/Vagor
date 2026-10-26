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
import com.example.vagor.entities.Vacation

class VacationDetailActivity : AppCompatActivity() {
    private var editTitle: EditText? = null
    private var editHotel: EditText? = null
    private var editStartDate: EditText? = null
    private var editEndDate: EditText? = null
    private var buttonSave: Button? = null
    private var db: AppDatabase? = null
    private var buttonDelete: Button? = null
    private var buttonViewEvents: Button? = null
    private var buttonShare: Button? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeContentView(R.layout.activity_vacation_detail)

        editTitle = findViewById<EditText>(R.id.editTitle)
        editHotel = findViewById<EditText>(R.id.editHotel)
        editStartDate = findViewById<EditText>(R.id.editStartDate)
        editEndDate = findViewById<EditText>(R.id.editEndDate)
        buttonSave = findViewById<Button>(R.id.buttonSave)
        buttonDelete = findViewById<Button>(R.id.buttonDelete)
        buttonViewEvents = findViewById<Button>(R.id.buttonViewEvents)
        buttonShare = findViewById<Button>(R.id.buttonShare)

        val vacationId = getIntent().getIntExtra("vacationId", -1)
        val title = getIntent().getStringExtra("title")
        val hotel = getIntent().getStringExtra("hotel")
        val startDate = getIntent().getStringExtra("startDate")
        val endDate = getIntent().getStringExtra("endDate")

        if (vacationId != -1) {
            editTitle!!.setText(title)
            editHotel!!.setText(hotel)
            editStartDate!!.setText(startDate)
            editEndDate!!.setText(endDate)
        }

        db = databaseBuilder<AppDatabase>(
            getApplicationContext(),
            AppDatabase::class.java,
            "vacation_database"
        )
            .allowMainThreadQueries()
            .build()

        //Save button logic
        buttonSave!!.setOnClickListener(View.OnClickListener setOnClickListener@{ _: View? ->
            val updatedTitle = editTitle!!.getText().toString().trim { it <= ' ' }
            val updatedHotel = editHotel!!.getText().toString().trim { it <= ' ' }
            val updatedStartDate = editStartDate!!.getText().toString().trim { it <= ' ' }
            val updatedEndDate = editEndDate!!.getText().toString().trim { it <= ' ' }

            //vacation date validation
            if (updatedTitle.isEmpty() || updatedHotel.isEmpty() || updatedStartDate.isEmpty() || updatedEndDate.isEmpty()) {
                Toast.makeText(this@VacationDetailActivity, R.string.please_fill_all_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!DateValidators.isValidDate(updatedStartDate) || !DateValidators.isValidDate(updatedEndDate)) {
                Toast.makeText(this@VacationDetailActivity, R.string.dates_must_be_mmddyyyy, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!DateValidators.isEndDateAfterOrEqualStartDate(updatedStartDate, updatedEndDate)) {
                Toast.makeText(this@VacationDetailActivity, R.string.end_date_must_follow_start, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (vacationId == -1) {
                val vacation = Vacation(updatedTitle, updatedHotel, updatedStartDate, updatedEndDate)
                val newId = db!!.vacationDAO().insert(vacation).toInt()
                ReminderScheduler.scheduleVacation(this@VacationDetailActivity, vacation.copy(id = newId))
                Toast.makeText(this@VacationDetailActivity, R.string.vacation_saved, Toast.LENGTH_SHORT).show()
            } else {
                val vacation = Vacation(updatedTitle, updatedHotel, updatedStartDate, updatedEndDate, vacationId)
                db!!.vacationDAO().update(vacation)
                ReminderScheduler.scheduleVacation(this@VacationDetailActivity, vacation)
                Toast.makeText(this@VacationDetailActivity, R.string.vacation_updated, Toast.LENGTH_SHORT).show()
            }
            finish()
        })

        //delete vacay logic
        buttonDelete!!.setOnClickListener(View.OnClickListener setOnClickListener@{ _: View? ->
            if (vacationId != -1) {
                val eventCount = db!!.eventDAO().getEventCountForVacation(vacationId)

                if (eventCount > 0) {
                    Toast.makeText(this@VacationDetailActivity, R.string.cannot_delete_vacation_with_events, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val vacation = Vacation(
                    editTitle!!.getText().toString().trim { it <= ' ' },
                    editHotel!!.getText().toString().trim { it <= ' ' },
                    editStartDate!!.getText().toString().trim { it <= ' ' },
                    editEndDate!!.getText().toString().trim { it <= ' ' },
                    vacationId
                )

                db!!.vacationDAO().delete(vacation)
                ReminderScheduler.cancelVacation(this@VacationDetailActivity, vacationId)

                Toast.makeText(this@VacationDetailActivity, R.string.vacation_deleted, Toast.LENGTH_SHORT).show()
                finish()
            }
        })
        //event button logic
        buttonViewEvents!!.setOnClickListener(View.OnClickListener { _: View? ->
            if (vacationId != -1) {
                val intent = Intent(this@VacationDetailActivity, EventListActivity::class.java)
                intent.putExtra("vacationId", vacationId)
                startActivity(intent)
            } else {
                Toast.makeText(this@VacationDetailActivity, R.string.save_vacation_first, Toast.LENGTH_SHORT).show()
            }
        })

        //share button logic
        buttonShare!!.setOnClickListener(View.OnClickListener { _: View? ->
            val vacationText = getString(
                R.string.vacation_share_template,
                editTitle!!.getText().toString().trim { it <= ' ' },
                editHotel!!.getText().toString().trim { it <= ' ' },
                editStartDate!!.getText().toString().trim { it <= ' ' },
                editEndDate!!.getText().toString().trim { it <= ' ' }
            )
            val events = if (vacationId == -1) emptyList() else db!!.eventDAO().getEventsForVacation(vacationId)
            val shareText = if (events.isEmpty()) {
                vacationText
            } else {
                val eventLines = events
                    .sortedBy { DateValidators.parseLocalDate(it.date)?.toEpochDay() }
                    .joinToString("\n") { getString(R.string.event_share_line, it.title, it.date) }
                getString(R.string.vacation_share_with_events, vacationText, eventLines)
            }
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.setType("text/plain")
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_vacation_details)))
        })
    }
}