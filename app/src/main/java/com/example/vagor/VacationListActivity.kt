package com.example.vagor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.Room.databaseBuilder
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Vacation

class VacationListActivity : AppCompatActivity() {
    private var addButton: Button? = null
    private var vacationListView: ListView? = null
    private var db: AppDatabase? = null
    private var vacationObjects: List<Vacation>? = null
    private var notificationsChecked = false
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, R.string.reminders_need_notifications, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeContentView(R.layout.activity_vacation_list)

        addButton = findViewById<Button>(R.id.buttonAddVacation)
        vacationListView = findViewById<ListView>(R.id.vacationListView)

        db = databaseBuilder<AppDatabase>(
            getApplicationContext(),
            AppDatabase::class.java, "vacation_database"
        )
            .allowMainThreadQueries()
            .build()

        addButton!!.setOnClickListener {
            val intent = Intent(this@VacationListActivity, VacationDetailActivity::class.java)
            startActivity(intent)
        }

        vacationListView!!.setOnItemClickListener(OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val selectedVacation = vacationObjects!![position]
            val intent = Intent(this@VacationListActivity, VacationDetailActivity::class.java)
            intent.putExtra("vacationId", selectedVacation.id)
            intent.putExtra("title", selectedVacation.title)
            intent.putExtra("hotel", selectedVacation.hotel)
            intent.putExtra("startDate", selectedVacation.startDate)
            intent.putExtra("endDate", selectedVacation.endDate)
            startActivity(intent)
        })
    }

    override fun onResume() {
        super.onResume()
        loadVacations()
        checkNotifications()
    }

    // Every saved vacation gets reminders, so ask for notifications once there is one.
    private fun checkNotifications() {
        if (notificationsChecked || vacationObjects.isNullOrEmpty()) return
        notificationsChecked = true
        ReminderScheduler.ensureChannel(this)
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Toast.makeText(this, R.string.reminders_need_notifications, Toast.LENGTH_LONG).show()
        }
    }

    private fun loadVacations() {
        vacationObjects = db!!.vacationDAO().getAllVacations()
        val vacationDisplayList = ArrayList<String>()

        for (vacation in vacationObjects!!) {
            vacationDisplayList.add(vacation.title + " - " + vacation.hotel)
        }

        val adapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            vacationDisplayList
        )

        vacationListView!!.setAdapter(adapter)
    }
}