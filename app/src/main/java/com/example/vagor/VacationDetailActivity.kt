package com.example.vagor

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Vacation

class VacationDetailActivity : AppCompatActivity() {
    private var editTitle: EditText? = null
    private var editTripDetails: EditText? = null
    private var editStartDate: DateField? = null
    private var editEndDate: DateField? = null
    private var buttonSave: Button? = null
    private var db: AppDatabase? = null
    private var buttonDelete: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeContentView(R.layout.activity_vacation_detail)

        editTitle = findViewById<EditText>(R.id.editTitle)
        editTripDetails = findViewById<EditText>(R.id.editTripDetails)
        editStartDate = findViewById<DateField>(R.id.editStartDate)
        editEndDate = findViewById<DateField>(R.id.editEndDate)
        buttonSave = findViewById<Button>(R.id.buttonSave)
        buttonDelete = findViewById<Button>(R.id.buttonDelete)

        val vacationId = getIntent().getIntExtra("vacationId", -1)
        val title = getIntent().getStringExtra("title")
        val details = getIntent().getStringExtra("details")
        val startDate = getIntent().getStringExtra("startDate")
        val endDate = getIntent().getStringExtra("endDate")
        showScreenTitle(isNew = vacationId == -1)

        if (vacationId != -1) {
            editTitle!!.setText(title)
            editTripDetails!!.setText(details)
            editStartDate!!.date = startDate.orEmpty()
            editEndDate!!.date = endDate.orEmpty()
        }
        // A new vacation has nothing to delete yet.
        if (vacationId == -1) {
            buttonDelete!!.visibility = View.GONE
        }

        editStartDate!!.attachCalendarDropdown()
        // The end date can't be before the start date.
        editEndDate!!.attachCalendarDropdown(
            earliest = { DateValidators.parseLocalDate(editStartDate!!.date) }
        )

        db = AppDatabase.open(getApplicationContext())

        // Save button logic
        buttonSave!!.setOnClickListener(
            View.OnClickListener setOnClickListener@{ _: View? ->
                val updatedTitle = editTitle!!.getText().toString().trim { it <= ' ' }
                val updatedDetails = editTripDetails!!.getText().toString().trim { it <= ' ' }
                val updatedStartDate = editStartDate!!.date.trim { it <= ' ' }
                val updatedEndDate = editEndDate!!.date.trim { it <= ' ' }

                // Trip details are optional; everything else is required.
                if (updatedTitle.isEmpty() || updatedStartDate.isEmpty() || updatedEndDate.isEmpty()) {
                    Toast.makeText(
                        this@VacationDetailActivity,
                        R.string.vacation_needs_title_and_dates,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (!DateValidators.isValidDate(updatedStartDate) || !DateValidators.isValidDate(updatedEndDate)) {
                    Toast.makeText(
                        this@VacationDetailActivity,
                        R.string.date_cant_be_read,
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                if (!DateValidators.isEndDateAfterOrEqualStartDate(updatedStartDate, updatedEndDate)) {
                    Toast.makeText(
                        this@VacationDetailActivity,
                        R.string.end_date_must_follow_start,
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                // Moving or shortening a trip mustn't leave its events, and their reminders, outside
                // it.
                if (vacationId != -1 &&
                    db!!.eventDAO().getEventsForVacation(vacationId).any {
                        DateValidators.isOutsideVacation(it.date, updatedStartDate, updatedEndDate)
                    }
                ) {
                    Toast.makeText(
                        this@VacationDetailActivity,
                        R.string.events_outside_vacation_dates,
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                val saved: Vacation
                if (vacationId == -1) {
                    val vacation = Vacation(updatedTitle, updatedDetails, updatedStartDate, updatedEndDate)
                    val newId = db!!.vacationDAO().insert(vacation).toInt()
                    saved = vacation.copy(id = newId)
                    ReminderScheduler.scheduleVacation(this@VacationDetailActivity, saved)
                    Toast.makeText(this@VacationDetailActivity, R.string.vacation_saved, Toast.LENGTH_SHORT).show()
                } else {
                    saved = Vacation(updatedTitle, updatedDetails, updatedStartDate, updatedEndDate, vacationId)
                    db!!.vacationDAO().update(saved)
                    ReminderScheduler.scheduleVacation(this@VacationDetailActivity, saved)
                    Toast.makeText(this@VacationDetailActivity, R.string.vacation_updated, Toast.LENGTH_SHORT).show()
                }
                closeAfterSave(saved)
            }
        )

        // delete vacay logic
        buttonDelete!!.setOnClickListener(
            View.OnClickListener setOnClickListener@{ _: View? ->
                if (vacationId != -1) {
                    val eventCount = db!!.eventDAO().getEventCountForVacation(vacationId)

                    if (eventCount > 0) {
                        Toast.makeText(
                            this@VacationDetailActivity,
                            R.string.cannot_delete_vacation_with_events,
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }

                    val vacation = Vacation(
                        editTitle!!.getText().toString().trim { it <= ' ' },
                        editTripDetails!!.getText().toString().trim { it <= ' ' },
                        editStartDate!!.date.trim { it <= ' ' },
                        editEndDate!!.date.trim { it <= ' ' },
                        vacationId
                    )

                    db!!.vacationDAO().delete(vacation)
                    ReminderScheduler.cancelVacation(this@VacationDetailActivity, vacationId)

                    Toast.makeText(this@VacationDetailActivity, R.string.vacation_deleted, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        )
    }

    // The form closes after a save, so a second tap mustn't save, or insert, again. For a trip far
    // ahead, the offer to keep Android from pausing Vagor closes the form instead.
    private fun closeAfterSave(saved: Vacation) {
        buttonSave!!.isEnabled = false
        buttonDelete!!.isEnabled = false
        if (!HibernationOfferDialog.showAfterSave(supportFragmentManager, this, saved)) finish()
    }

    override fun onDestroy() {
        db?.close()
        db = null
        super.onDestroy()
    }

    // TalkBack announces each screen by its title; the heading shows the same words.
    private fun showScreenTitle(isNew: Boolean) {
        setTitle(if (isNew) R.string.add_vacation else R.string.edit_vacation)
        findViewById<TextView>(R.id.vacationFormHeading).text = title
    }
}
