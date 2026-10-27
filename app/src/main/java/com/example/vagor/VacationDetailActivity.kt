package com.example.vagor

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Vacation

class VacationDetailActivity : AppCompatActivity() {
    private var editTitle: EditText? = null
    private var editTripDetails: EditText? = null
    private var editStartDate: EditText? = null
    private var editEndDate: EditText? = null
    private var buttonSave: Button? = null
    private var db: AppDatabase? = null
    private var buttonDelete: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeContentView(R.layout.activity_vacation_detail)

        editTitle = findViewById<EditText>(R.id.editTitle)
        editTripDetails = findViewById<EditText>(R.id.editTripDetails)
        editStartDate = findViewById<EditText>(R.id.editStartDate)
        editEndDate = findViewById<EditText>(R.id.editEndDate)
        buttonSave = findViewById<Button>(R.id.buttonSave)
        buttonDelete = findViewById<Button>(R.id.buttonDelete)

        val vacationId = getIntent().getIntExtra("vacationId", -1)
        val title = getIntent().getStringExtra("title")
        val details = getIntent().getStringExtra("details")
        val startDate = getIntent().getStringExtra("startDate")
        val endDate = getIntent().getStringExtra("endDate")

        if (vacationId != -1) {
            editTitle!!.setText(title)
            editTripDetails!!.setText(details)
            editStartDate!!.setText(startDate)
            editEndDate!!.setText(endDate)
        }
        // A new vacation has nothing to delete yet.
        if (vacationId == -1) {
            buttonDelete!!.visibility = View.GONE
        }

        editStartDate!!.attachCalendarDropdown()
        // The end date can't be before the start date.
        editEndDate!!.attachCalendarDropdown(
            earliest = { DateValidators.parseLocalDate(editStartDate!!.getText().toString()) }
        )

        db = AppDatabase.open(getApplicationContext())

        //Save button logic
        buttonSave!!.setOnClickListener(View.OnClickListener setOnClickListener@{ _: View? ->
            val updatedTitle = editTitle!!.getText().toString().trim { it <= ' ' }
            val updatedDetails = editTripDetails!!.getText().toString().trim { it <= ' ' }
            val updatedStartDate = editStartDate!!.getText().toString().trim { it <= ' ' }
            val updatedEndDate = editEndDate!!.getText().toString().trim { it <= ' ' }

            // Trip details are optional; everything else is required.
            if (updatedTitle.isEmpty() || updatedStartDate.isEmpty() || updatedEndDate.isEmpty()) {
                Toast.makeText(this@VacationDetailActivity, R.string.vacation_needs_title_and_dates, Toast.LENGTH_SHORT).show()
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
                val vacation = Vacation(updatedTitle, updatedDetails, updatedStartDate, updatedEndDate)
                val newId = db!!.vacationDAO().insert(vacation).toInt()
                ReminderScheduler.scheduleVacation(this@VacationDetailActivity, vacation.copy(id = newId))
                Toast.makeText(this@VacationDetailActivity, R.string.vacation_saved, Toast.LENGTH_SHORT).show()
            } else {
                val vacation = Vacation(updatedTitle, updatedDetails, updatedStartDate, updatedEndDate, vacationId)
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
                    editTripDetails!!.getText().toString().trim { it <= ' ' },
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
    }
}