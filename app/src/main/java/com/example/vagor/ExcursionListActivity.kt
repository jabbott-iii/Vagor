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
import com.example.vagor.entities.Excursion

class ExcursionListActivity : AppCompatActivity() {
    private var buttonAddExcursion: Button? = null
    private var excursionListView: ListView? = null
    private var db: AppDatabase? = null
    private var vacationId = 0
    private var excursionObjects: List<Excursion>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_excursion_list)

        buttonAddExcursion = findViewById<Button>(R.id.buttonAddExcursion)
        excursionListView = findViewById<ListView>(R.id.excursionListView)

        vacationId = getIntent().getIntExtra("vacationId", -1)

        db = databaseBuilder<AppDatabase>(
            getApplicationContext(),
            AppDatabase::class.java, "vacation_database"
        )
            .allowMainThreadQueries()
            .build()

        buttonAddExcursion!!.setOnClickListener(View.OnClickListener { _: View? ->
            val intent = Intent(this@ExcursionListActivity, ExcursionDetailActivity::class.java)
            intent.putExtra("vacationId", vacationId)
            startActivity(intent)
        })

        //Make excursion items clickable
        excursionListView!!.setOnItemClickListener(OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val selectedExcursion = excursionObjects!![position]
            val intent = Intent(this@ExcursionListActivity, ExcursionDetailActivity::class.java)
            intent.putExtra("excursionId", selectedExcursion.id)
            intent.putExtra("title", selectedExcursion.title)
            intent.putExtra("date", selectedExcursion.date)
            intent.putExtra("vacationId", selectedExcursion.vacationId)
            startActivity(intent)
        })
    }

    override fun onResume() {
        super.onResume()
        loadExcursions()
    }

    private fun loadExcursions() {
        excursionObjects = db!!.excursionDAO().getExcursionsForVacation(vacationId)
        val excursionDisplayList = ArrayList<String>()

        for (excursion in excursionObjects!!) {
            excursionDisplayList.add(excursion.title + " - " + excursion.date)
        }

        val adapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            excursionDisplayList
        )

        excursionListView!!.setAdapter(adapter)
    }
}