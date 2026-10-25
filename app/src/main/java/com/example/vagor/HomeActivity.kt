package com.example.vagor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room.databaseBuilder
import com.example.vagor.database.AppDatabase

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // A force stop clears alarms and an app update may predate them, so rebuild on launch.
        if (savedInstanceState == null) {
            val db = databaseBuilder<AppDatabase>(
                getApplicationContext(),
                AppDatabase::class.java, "vacation_database"
            )
                .allowMainThreadQueries()
                .build()
            ReminderScheduler.rescheduleAll(this, db)
            db.close()
        }

        val button = findViewById<Button>(R.id.buttonGoToVacations)

        button.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(this@HomeActivity, VacationListActivity::class.java)
            startActivity(intent)
        })
    }
}