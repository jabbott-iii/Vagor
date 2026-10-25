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
import com.example.vagor.entities.Excursion

class ExcursionDetailActivity : AppCompatActivity() {
    private var editExcursionTitle: EditText? = null
    private var editExcursionDate: EditText? = null
    private var buttonSaveExcursion: Button? = null
    private var db: AppDatabase? = null
    private var vacationId = 0
    private var buttonDeleteExcursion: Button? = null
    private var buttonShareExcursion: Button? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_excursion_detail)

        editExcursionTitle = findViewById<EditText>(R.id.editExcursionTitle)
        editExcursionDate = findViewById<EditText>(R.id.editExcursionDate)
        buttonSaveExcursion = findViewById<Button>(R.id.buttonSaveExcursion)
        buttonDeleteExcursion = findViewById<Button>(R.id.buttonDeleteExcursion)
        buttonShareExcursion = findViewById<Button>(R.id.buttonShareExcursion)

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
                val newId = db!!.excursionDAO().insert(excursion).toInt()
                ReminderScheduler.scheduleExcursion(this@ExcursionDetailActivity, excursion.copy(id = newId))
                Toast.makeText(this@ExcursionDetailActivity, R.string.excursion_saved, Toast.LENGTH_SHORT).show()
            } else {
                val excursion = Excursion(updatedTitle, updatedDate, vacationId, excursionId)
                db!!.excursionDAO().update(excursion)
                ReminderScheduler.scheduleExcursion(this@ExcursionDetailActivity, excursion)
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
                ReminderScheduler.cancelExcursion(this@ExcursionDetailActivity, excursionId)

                Toast.makeText(this@ExcursionDetailActivity, R.string.excursion_deleted, Toast.LENGTH_SHORT).show()
                finish()
            }
        })

        //share button logic
        buttonShareExcursion!!.setOnClickListener shareClick@{
            val vacation = db!!.vacationDAO().getVacationById(vacationId)
            if (vacation == null) {
                Toast.makeText(this@ExcursionDetailActivity, R.string.associated_vacation_not_found, Toast.LENGTH_SHORT).show()
                return@shareClick
            }
            val shareText = getString(
                R.string.excursion_share_template,
                editExcursionTitle!!.getText().toString().trim { it <= ' ' },
                editExcursionDate!!.getText().toString().trim { it <= ' ' },
                vacation.title,
                vacation.hotel
            )
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.setType("text/plain")
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_excursion_details)))
        }
    }
}