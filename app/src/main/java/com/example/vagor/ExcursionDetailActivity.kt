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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

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

        buttonSaveExcursion!!.setOnClickListener(View.OnClickListener setOnClickListener@{ v: View? ->
            val updatedTitle = editExcursionTitle!!.getText().toString().trim { it <= ' ' }
            val updatedDate = editExcursionDate!!.getText().toString().trim { it <= ' ' }

            if (updatedTitle.isEmpty() || updatedDate.isEmpty()) {
                Toast.makeText(
                    this@ExcursionDetailActivity,
                    "Please fill in all fields",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val vacation = db!!.vacationDAO().getVacationById(vacationId)

            if (vacation == null) {
                Toast.makeText(
                    this@ExcursionDetailActivity,
                    "Associated vacation not found",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!isDateWithinVacation(
                    updatedDate,
                    vacation.getStartDate(),
                    vacation.getEndDate()
                )
            ) {
                Toast.makeText(
                    this@ExcursionDetailActivity,
                    "Excursion date must be within the vacation dates (MM/dd/yyyy)",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (excursionId == -1) {
                val excursion = Excursion(updatedTitle, updatedDate, vacationId)
                db!!.excursionDAO().insert(excursion)
                Toast.makeText(this@ExcursionDetailActivity, "Excursion saved", Toast.LENGTH_SHORT)
                    .show()
            } else {
                val excursion = Excursion(updatedTitle, updatedDate, vacationId)
                excursion.setId(excursionId)
                db!!.excursionDAO().update(excursion)
                Toast.makeText(
                    this@ExcursionDetailActivity,
                    "Excursion updated",
                    Toast.LENGTH_SHORT
                ).show()
            }
            finish()
        })

        buttonDeleteExcursion!!.setOnClickListener(View.OnClickListener { v: View? ->
            if (excursionId != -1) {
                val excursion = Excursion(
                    editExcursionTitle!!.getText().toString().trim { it <= ' ' },
                    editExcursionDate!!.getText().toString().trim { it <= ' ' },
                    vacationId
                )
                excursion.setId(excursionId)

                db!!.excursionDAO().delete(excursion)

                Toast.makeText(
                    this@ExcursionDetailActivity,
                    "Excursion deleted",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        })

        //excursion button logic
        buttonExcursionAlert!!.setOnClickListener(View.OnClickListener { v: View? ->
            val titleText = editExcursionTitle!!.getText().toString().trim { it <= ' ' }
            val dateText = editExcursionDate!!.getText().toString().trim { it <= ' ' }

            val requestCode = if (excursionId != -1) excursionId + 3000 else vacationId + 3000
            scheduleExcursionAlert(dateText, titleText + " is happening today", requestCode)
        })
    }

    private fun isDateWithinVacation(
        excursionDate: String,
        vacationStart: String,
        vacationEnd: String
    ): Boolean {
        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        sdf.setLenient(false)

        try {
            val excursion = sdf.parse(excursionDate)
            val start = sdf.parse(vacationStart)
            val end = sdf.parse(vacationEnd)

            return excursion != null && start != null && end != null && !excursion.before(start) && !excursion.after(
                end
            )
        } catch (e: ParseException) {
            return false
        }
    }

    //excursion alert helper
    private fun scheduleExcursionAlert(dateText: String, message: String?, requestCode: Int) {
        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        sdf.setLenient(false)

        try {
            val alertDate = sdf.parse(dateText)
            if (alertDate == null) return

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
                alarmManager.set(AlarmManager.RTC_WAKEUP, alertDate.getTime(), pendingIntent)
                Toast.makeText(this, "Excursion alert set", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ParseException) {
            Toast.makeText(this, "Invalid date format. Use MM/dd/yyyy", Toast.LENGTH_LONG).show()
        }
    }
}