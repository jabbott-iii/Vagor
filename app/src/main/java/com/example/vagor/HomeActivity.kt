package com.example.vagor

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.room.Room.databaseBuilder
import com.example.vagor.database.AppDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView

// The landing screen: the Vacations dashboard and the Settings tab, switched by the bottom bar.
class HomeActivity : AppCompatActivity() {
    // Back from the Settings tab returns to Vacations instead of leaving the app.
    private val backToVacations = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.nav_vacations
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeContentViewWithBottomBar(R.layout.activity_home, R.id.homeContent, R.id.bottomNavigation)

        if (savedInstanceState == null) {
            // A force stop clears alarms and an app update may predate them, so rebuild on launch.
            val db = databaseBuilder<AppDatabase>(
                getApplicationContext(),
                AppDatabase::class.java, "vacation_database"
            )
                .allowMainThreadQueries()
                .build()
            ReminderScheduler.rescheduleAll(this, db)
            db.close()

            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.homeContent, VacationsFragment())
                .commit()
        }

        // After a recreation (rotation, or the dark mode switch) the fragment manager restores the
        // open tab and the bar restores its selection; neither calls the listener below.
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            showTab(if (item.itemId == R.id.nav_settings) SettingsFragment() else VacationsFragment())
            true
        }
        bottomNavigation.setOnItemReselectedListener { }
        onBackPressedDispatcher.addCallback(this, backToVacations)
        backToVacations.isEnabled = supportFragmentManager.findFragmentById(R.id.homeContent) is SettingsFragment
    }

    private fun showTab(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.homeContent, fragment)
            .commit()
        backToVacations.isEnabled = fragment is SettingsFragment
    }
}
