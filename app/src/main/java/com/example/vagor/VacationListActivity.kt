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
import com.example.vagor.entities.Vacation

class VacationListActivity : AppCompatActivity() {
    private var addButton: Button? = null
    private var vacationListView: ListView? = null
    private var db: AppDatabase? = null
    private var vacationObjects: MutableList<Vacation>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vacation_list)

        addButton = findViewById<Button>(R.id.buttonAddVacation)
        vacationListView = findViewById<ListView>(R.id.vacationListView)

        db = databaseBuilder<AppDatabase>(
            getApplicationContext(),
            AppDatabase::class.java, "vacation_database"
        )
            .allowMainThreadQueries()
            .build()

        addButton!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val intent = Intent(this@VacationListActivity, VacationDetailActivity::class.java)
                startActivity(intent)
            }
        })

        vacationListView!!.setOnItemClickListener(OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            val selectedVacation = vacationObjects!!.get(position)
            val intent = Intent(this@VacationListActivity, VacationDetailActivity::class.java)
            intent.putExtra("vacationId", selectedVacation.getId())
            intent.putExtra("title", selectedVacation.getTitle())
            intent.putExtra("hotel", selectedVacation.getHotel())
            intent.putExtra("startDate", selectedVacation.getStartDate())
            intent.putExtra("endDate", selectedVacation.getEndDate())
            startActivity(intent)
        })
    }

    override fun onResume() {
        super.onResume()
        loadVacations()
    }

    private fun loadVacations() {
        vacationObjects = db!!.vacationDAO().getAllVacations()
        val vacationDisplayList: MutableList<String?> = ArrayList<String?>()

        for (vacation in vacationObjects!!) {
            vacationDisplayList.add(vacation.getTitle() + " - " + vacation.getHotel())
        }

        val adapter = ArrayAdapter<String?>(
            this,
            android.R.layout.simple_list_item_1,
            vacationDisplayList
        )

        vacationListView!!.setAdapter(adapter)
    }
}