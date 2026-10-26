package com.example.vagor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.room.Room.databaseBuilder
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Vacation
import java.time.LocalDate

// The Vacations tab on the home screen: a dashboard of every vacation and its dates.
class VacationsFragment : Fragment(R.layout.fragment_vacations) {
    private var db: AppDatabase? = null
    private var vacationListView: ListView? = null
    private var dashboardAdapter: DashboardAdapter? = null
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(requireContext(), R.string.reminders_need_notifications, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = databaseBuilder<AppDatabase>(
            requireContext().applicationContext,
            AppDatabase::class.java, "vacation_database"
        )
            .allowMainThreadQueries()
            .build()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vacationListView = view.findViewById(R.id.vacationListView)
        vacationListView!!.emptyView = view.findViewById(R.id.emptyVacations)
        dashboardAdapter = DashboardAdapter(requireContext())
        vacationListView!!.adapter = dashboardAdapter

        view.findViewById<View>(R.id.buttonAddVacation).setOnClickListener {
            startActivity(Intent(requireContext(), VacationDetailActivity::class.java))
        }

        // Taps on a card go through the card itself; this covers keyboard and D-pad selection.
        vacationListView!!.setOnItemClickListener { parent, _, position, _ ->
            (parent.getItemAtPosition(position) as? DashboardRow.Trip)?.let { openVacation(it.vacation) }
        }
    }

    override fun onDestroyView() {
        vacationListView = null
        dashboardAdapter = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        db?.close()
        db = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        val vacations = db!!.vacationDAO().getAllVacations()
        // Refilled rather than replaced, so the list keeps its scroll position.
        dashboardAdapter!!.clear()
        dashboardAdapter!!.addAll(Dashboard.rows(vacations, LocalDate.now()))
        if (vacations.isNotEmpty()) checkNotifications()
    }

    private fun openVacation(vacation: Vacation) {
        startActivity(Intent(requireContext(), EventListActivity::class.java).putExtra("vacationId", vacation.id))
    }

    // Every saved vacation gets reminders, so ask for notifications once there is one.
    private fun checkNotifications() {
        if (notificationsChecked) return
        notificationsChecked = true
        val context = requireContext()
        ReminderScheduler.ensureChannel(context)
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Toast.makeText(context, R.string.reminders_need_notifications, Toast.LENGTH_LONG).show()
        }
    }

    private inner class DashboardAdapter(context: Context) : ArrayAdapter<DashboardRow>(context, 0) {

        override fun getViewTypeCount() = 2

        override fun getItemViewType(position: Int) = if (getItem(position) is DashboardRow.Trip) 0 else 1

        override fun areAllItemsEnabled() = false

        // The Past heading can't be selected or clicked.
        override fun isEnabled(position: Int) = getItem(position) is DashboardRow.Trip

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val inflater = LayoutInflater.from(context)
            val row = getItem(position)
            if (row !is DashboardRow.Trip) {
                return convertView ?: inflater.inflate(R.layout.item_dashboard_heading, parent, false)
            }

            val view = convertView ?: inflater.inflate(R.layout.item_vacation, parent, false)
            val vacation = row.vacation
            view.findViewById<TextView>(R.id.vacationTitle).text = vacation.title
            view.findViewById<TextView>(R.id.vacationDates).text = context.vacationDateRange(vacation)
            view.findViewById<TextView>(R.id.vacationHotel).text = context.getString(R.string.vacation_hotel, vacation.hotel)
            view.findViewById<TextView>(R.id.vacationStatus).text = context.tripStatusText(row.status)
            view.findViewById<View>(R.id.vacationCard).setOnClickListener { openVacation(vacation) }
            return view
        }
    }

    private companion object {
        // Checked once per app launch, so switching tabs doesn't ask again.
        var notificationsChecked = false
    }
}
