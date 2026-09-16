package com.example.vagor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room.databaseBuilder
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Excursion

class ExcursionDetailActivity : AppCompatActivity() {
    private var editExcursionTitle: EditText? = null
    private var editExcursionDate: EditText? = null
    private var buttonSaveExcursion: Button? = null
    private var db: AppDatabase? = null
    private var vacationId = 0
    private var buttonDeleteExcursion: Button? = null
    private var buttonExcursionAlert: Button? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_excursion_detail)

        editExcursionTitle = findViewById<EditText>(R.id.editExcursionTitle)
        editExcursionDate = findViewById<EditText>(R.id.editExcursionDate)
        buttonSaveExcursion = findViewById<Button>(R.id.buttonSaveExcursion)
        buttonDeleteExcursion = findViewById<Button>(R.id.buttonDeleteExcursion)
        buttonExcursionAlert = findViewById<Button>(R.id.buttonExcursionAlert)

        vacationId = getIntent().getIntExtra("vacationId", -1)

        db = databaseBuilder<AppDatabase>(
            getApplicationContext(),
            AppDatabase::class.java, "vacation_database"
        )
            .allowMainThreadQueries()
            .build()

        //excursion data
        val excursionId = getIntent().getIntExtra("excursionId", -1)
        val title = getIntent().getStringExtra("title")
        val date = getIntent().getStringExtra("date")

        if (excursionId != -1) {
            editExcursionTitle!!.setText(title)
            editExcursionDate!!.setText(date)
        }

        buttonSaveExcursion!!.setOnClickListener(View.OnClickListener setOnClickListener@{ _: View? ->
            val updatedTitle = editExcursionTitle!!.getText().toString().trim { it <= ' ' }
            val updatedDate = editExcursionDate!!.getText().toString().trim { it <= ' ' }

            if (updatedTitle.isEmpty() || updatedDate.isEmpty()) {
                Toast.makeText(this@ExcursionDetailActivity, R.string.please_fill_all_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val vacation = db!!.vacationDAO().getVacationById(vacationId)

            if (vacation == null) {
                Toast.makeText(this@ExcursionDetailActivity, R.string.associated_vacation_not_found, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!DateValidators.isDateWithinVacation(updatedDate, vacation.startDate, vacation.endDate)) {
                Toast.makeText(this@ExcursionDetailActivity, R.string.excursion_date_out_of_range, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (excursionId == -1) {
                val excursion = Excursion(updatedTitle, updatedDate, vacationId)
                db!!.excursionDAO().insert(excursion)
                Toast.makeText(this@ExcursionDetailActivity, R.string.excursion_saved, Toast.LENGTH_SHORT).show()
            } else {
                val excursion = Excursion(updatedTitle, updatedDate, vacationId, excursionId)
                db!!.excursionDAO().update(excursion)
                Toast.makeText(this@ExcursionDetailActivity, R.string.excursion_updated, Toast.LENGTH_SHORT).show()
            }
            finish()
        })

        buttonDeleteExcursion!!.setOnClickListener(View.OnClickListener { _: View? ->
            if (excursionId != -1) {
                val excursion = Excursion(
                    editExcursionTitle!!.getText().toString().trim { it <= ' ' },
                    editExcursionDate!!.getText().toString().trim { it <= ' ' },
                    vacationId,
                    excursionId
                )

                db!!.excursionDAO().delete(excursion)

                Toast.makeText(this@ExcursionDetailActivity, R.string.excursion_deleted, Toast.LENGTH_SHORT).show()
                finish()
            }
        })

        //excursion button logic
        buttonExcursionAlert!!.setOnClickListener excursionAlertClick@{
            if (excursionId == -1) {
                Toast.makeText(this@ExcursionDetailActivity, R.string.save_excursion_before_alerts, Toast.LENGTH_SHORT).show()
                return@excursionAlertClick
            }
            val savedExcursion = db!!.excursionDAO().getExcursionById(excursionId)
            if (savedExcursion == null) {
                Toast.makeText(this@ExcursionDetailActivity, R.string.saved_excursion_not_found, Toast.LENGTH_SHORT).show()
                return@excursionAlertClick
            }

            scheduleExcursionAlert(
                savedExcursion.date,
                getString(R.string.excursion_happening_today, savedExcursion.title),
                excursionId + 3000,
                editExcursionDate!!
            )
        }
    }

    //excursion alert helper
    private fun scheduleExcursionAlert(dateText: String, message: String?, requestCode: Int, dateField: EditText) {
        val alertDate = DateValidators.parseDate(dateText)
        if (alertDate == null) {
            dateField.error = getString(R.string.invalid_date_format)
            Toast.makeText(this, R.string.invalid_date_format, Toast.LENGTH_LONG).show()
            return
        }
        dateField.error = null

        val intent = Intent(this@ExcursionDetailActivity, MyReceiver::class.java)
        intent.putExtra("message", message)

        val pendingIntent = PendingIntent.getBroadcast(
            this@ExcursionDetailActivity,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager?
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, alertDate.time, pendingIntent)
            Toast.makeText(this, R.string.excursion_alert_set, Toast.LENGTH_SHORT).show()
        }
    }
}