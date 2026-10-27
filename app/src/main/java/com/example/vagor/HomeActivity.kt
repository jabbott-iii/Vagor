package com.example.vagor

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
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
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.homeContent, VacationsFragment())
                .commit()
        }

        // After a recreation (rotation, or the dark mode switch) the fragment manager restores the
        // open tab and the bar restores its selection; neither calls the listener below.
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        // The tab labels grow with the system font size, like the rest of the app's text.
        bottomNavigation.setLabelFontScalingEnabled(true)
        bottomNavigation.setOnItemSelectedListener { item ->
            showTab(if (item.itemId == R.id.nav_settings) SettingsFragment() else VacationsFragment())
            true
        }
        bottomNavigation.setOnItemReselectedListener { }
        onBackPressedDispatcher.addCallback(this, backToVacations)
        backToVacations.isEnabled = supportFragmentManager.findFragmentById(R.id.homeContent) is SettingsFragment
    }

    override fun onResume() {
        super.onResume()
        // VagorApplication rebuilds the reminders in each new process. Setting Vagor's battery use
        // to Unrestricted allows exact alarms without a broadcast, so check again on the way back,
        // before the Settings tab says whether event reminders are exact.
        ReminderScheduler.rebuildIfNeeded(this)
    }

    private fun showTab(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.homeContent, fragment)
            .commit()
        backToVacations.isEnabled = fragment is SettingsFragment
    }
}
