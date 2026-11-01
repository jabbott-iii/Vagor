package com.example.vagor

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Event
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.LocalTime
import java.time.ZoneId

class EventDetailActivity : AppCompatActivity() {
    private var editEventTitle: EditText? = null
    private var editEventDate: EditText? = null
    private var editEventTime: EditText? = null
    private var editEventTimeZone: EditText? = null
    private var editEventDetails: EditText? = null
    private var buttonSaveEvent: Button? = null
    private var db: AppDatabase? = null
    private var vacationId = 0
    private var buttonDeleteEvent: Button? = null
    private var buttonShareEvent: Button? = null
    private var buttonEventToCalendar: Button? = null

    // The picked time and time zone, which their fields show as text. A new event, or one saved
    // before events had a time, has no time until one is picked.
    private var pickedTime: LocalTime? = null
    private var pickedZoneId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeContentView(R.layout.activity_event_detail)

        editEventTitle = findViewById<EditText>(R.id.editEventTitle)
        editEventDate = findViewById<EditText>(R.id.editEventDate)
        editEventTime = findViewById<EditText>(R.id.editEventTime)
        editEventTimeZone = findViewById<EditText>(R.id.editEventTimeZone)
        editEventDetails = findViewById<EditText>(R.id.editEventDetails)
        buttonSaveEvent = findViewById<Button>(R.id.buttonSaveEvent)
        buttonDeleteEvent = findViewById<Button>(R.id.buttonDeleteEvent)
        buttonShareEvent = findViewById<Button>(R.id.buttonShareEvent)
        buttonEventToCalendar = findViewById<Button>(R.id.buttonEventToCalendar)

        vacationId = getIntent().getIntExtra("vacationId", -1)

        db = AppDatabase.open(getApplicationContext())

        // The event is read from the database rather than from extras, which a reminder copied
        // when it was posted and which may be out of date by now.
        val eventId = getIntent().getIntExtra("eventId", -1)
        val saved = if (eventId != -1) db!!.eventDAO().getEventById(eventId) else null
        if (eventId != -1 && saved == null) {
            // Deleted since the reminder or page that opened it.
            finish()
            return
        }

        if (saved != null) {
            editEventTitle!!.setText(saved.title)
            editEventDate!!.setText(saved.date)
            editEventDetails!!.setText(saved.details)
        } else {
            // A new event has nothing to delete yet.
            buttonDeleteEvent!!.visibility = View.GONE
        }

        // After a rotation the picked values come from the saved state, since their fields only
        // show them. The time zone starts on the phone's own.
        val state = savedInstanceState
        pickedTime = DateValidators.parseLocalTime(if (state != null) state.getString(STATE_TIME) else saved?.time)
        pickedZoneId = (if (state != null) state.getString(STATE_TIME_ZONE) else saved?.timeZone)
            ?.takeIf { DateValidators.parseZone(it) != null }
            ?: ZoneId.systemDefault().id
        editEventTime!!.isSaveEnabled = false
        editEventTimeZone!!.isSaveEnabled = false
        showPickedTime()
        showPickedZone()

        // The calendar offers only the days of the event's vacation.
        editEventDate!!.attachCalendarDropdown(
            earliest = {
                db!!.vacationDAO().getVacationById(vacationId)?.let { DateValidators.parseLocalDate(it.startDate) }
            },
            latest = {
                db!!.vacationDAO().getVacationById(vacationId)?.let { DateValidators.parseLocalDate(it.endDate) }
            }
        )
        editEventTime!!.makePickOnly(getString(R.string.choose_time)) { showTimePicker() }
        editEventTimeZone!!.makePickOnly(getString(R.string.choose_time_zone)) {
            TimeZonePickerDialog.show(supportFragmentManager, pickedZoneId)
        }
        // A clock that was open when the screen was recreated comes back without its listener.
        (supportFragmentManager.findFragmentByTag(TIME_PICKER_TAG) as? MaterialTimePicker)?.let { listenTo(it) }
        supportFragmentManager.setFragmentResultListener(TimeZonePickerDialog.REQUEST_KEY, this) { _, result ->
            result.getString(TimeZonePickerDialog.RESULT_ZONE_ID)?.let {
                pickedZoneId = it
                showPickedZone()
            }
        }

        buttonSaveEvent!!.setOnClickListener(
            View.OnClickListener setOnClickListener@{ _: View? ->
                val updatedTitle = editEventTitle!!.getText().toString().trim { it <= ' ' }
                val updatedDate = editEventDate!!.getText().toString().trim { it <= ' ' }
                val updatedDetails = editEventDetails!!.getText().toString().trim { it <= ' ' }
                val updatedTime = pickedTime

                // Details are optional; everything else is required.
                if (updatedTitle.isEmpty() || updatedDate.isEmpty() || updatedTime == null) {
                    Toast.makeText(
                        this@EventDetailActivity,
                        R.string.event_needs_title_date_and_time,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val vacation = db!!.vacationDAO().getVacationById(vacationId)

                if (vacation == null) {
                    Toast.makeText(
                        this@EventDetailActivity,
                        R.string.associated_vacation_not_found,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (!DateValidators.isDateWithinVacation(updatedDate, vacation.startDate, vacation.endDate)) {
                    Toast.makeText(this@EventDetailActivity, R.string.event_date_out_of_range, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val event = Event(
                    updatedTitle,
                    updatedDate,
                    vacationId,
                    details = updatedDetails,
                    time = DateValidators.formatTime(updatedTime),
                    timeZone = pickedZoneId
                )
                val stored: Event
                if (eventId == -1) {
                    val newId = db!!.eventDAO().insert(event).toInt()
                    stored = event.copy(id = newId)
                    Toast.makeText(this@EventDetailActivity, R.string.event_saved, Toast.LENGTH_SHORT).show()
                } else {
                    stored = event.copy(id = eventId)
                    db!!.eventDAO().update(stored)
                    Toast.makeText(this@EventDetailActivity, R.string.event_updated, Toast.LENGTH_SHORT).show()
                }
                // The form closes after a save, so a second tap mustn't save, or insert, again.
                buttonSaveEvent!!.isEnabled = false
                buttonDeleteEvent!!.isEnabled = false
                buttonShareEvent!!.isEnabled = false
                buttonEventToCalendar!!.isEnabled = false
                val timingChanged = saved == null || saved.date != stored.date || saved.time != stored.time ||
                    saved.timeZone != stored.timeZone
                ReminderScheduler.eventSaved(this@EventDetailActivity, stored, timingChanged)
                // When the reminder can't be exact, the offer to allow exact alarms closes the form.
                if (!ExactAlarmOfferDialog.showAfterSave(supportFragmentManager, this@EventDetailActivity, stored)) {
                    finish()
                }
            }
        )

        buttonDeleteEvent!!.setOnClickListener(
            View.OnClickListener { _: View? ->
                if (eventId != -1) {
                    val event = Event(
                        editEventTitle!!.getText().toString().trim { it <= ' ' },
                        editEventDate!!.getText().toString().trim { it <= ' ' },
                        vacationId,
                        eventId
                    )

                    db!!.eventDAO().delete(event)
                    ReminderScheduler.cancelEvent(this@EventDetailActivity, eventId)

                    Toast.makeText(this@EventDetailActivity, R.string.event_deleted, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        )

        // share button logic
        buttonShareEvent!!.setOnClickListener shareClick@{
            val vacation = db!!.vacationDAO().getVacationById(vacationId)
            if (vacation == null) {
                Toast.makeText(
                    this@EventDetailActivity,
                    R.string.associated_vacation_not_found,
                    Toast.LENGTH_SHORT
                ).show()
                return@shareClick
            }
            val shared = Event(
                editEventTitle!!.getText().toString().trim { it <= ' ' },
                editEventDate!!.getText().toString().trim { it <= ' ' },
                vacationId,
                details = editEventDetails!!.getText().toString().trim { it <= ' ' },
                time = pickedTime?.let { DateValidators.formatTime(it) },
                timeZone = pickedZoneId
            )
            val lines = mutableListOf(
                getString(R.string.event_share_title, shared.title),
                getString(R.string.event_share_date, shared.date)
            )
            // Without a valid date there's no zone abbreviation, but the time still goes in.
            pickedTime?.let { time ->
                val timeText = eventTimeText(shared) ?: timeOfDayText(time)
                lines += getString(R.string.event_share_time, timeText, TimeZoneChoices.label(this, pickedZoneId))
            }
            lines += getString(R.string.event_share_vacation, vacation.title)
            if (shared.details.isNotEmpty()) lines += getString(R.string.event_share_details, shared.details)
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.setType("text/plain")
            shareIntent.putExtra(Intent.EXTRA_TEXT, lines.joinToString("\n"))
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_event_details)))
        }

        // Like Share, this takes what the form shows, saved or not.
        buttonEventToCalendar!!.setOnClickListener calendarClick@{
            val vacation = db!!.vacationDAO().getVacationById(vacationId)
            if (vacation == null) {
                Toast.makeText(
                    this@EventDetailActivity,
                    R.string.associated_vacation_not_found,
                    Toast.LENGTH_SHORT
                ).show()
                return@calendarClick
            }
            val title = editEventTitle!!.getText().toString().trim { it <= ' ' }
            val shown = Event(
                title,
                editEventDate!!.getText().toString().trim { it <= ' ' },
                vacationId,
                details = editEventDetails!!.getText().toString().trim { it <= ' ' },
                time = pickedTime?.let { DateValidators.formatTime(it) },
                timeZone = pickedZoneId
            )
            if (title.isEmpty() || shown.date.isEmpty() || shown.time == null) {
                Toast.makeText(
                    this@EventDetailActivity,
                    R.string.event_needs_title_date_and_time,
                    Toast.LENGTH_SHORT
                ).show()
                return@calendarClick
            }
            val insert = eventCalendarIntent(shown, vacation)
            if (insert == null) {
                // A date saved before dates were checked strictly.
                Toast.makeText(this@EventDetailActivity, R.string.dates_must_be_mmddyyyy, Toast.LENGTH_LONG).show()
                return@calendarClick
            }
            openCalendarInsert(insert)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_TIME, pickedTime?.let { DateValidators.formatTime(it) })
        outState.putString(STATE_TIME_ZONE, pickedZoneId)
    }

    override fun onDestroy() {
        db?.close()
        db = null
        super.onDestroy()
    }

    // A clock opening at the picked time, or 9:00 AM, in the phone's 12- or 24-hour style. Its
    // keyboard button switches to typing the hour and minute.
    private fun showTimePicker() {
        val manager = supportFragmentManager
        // A second tap, or a screen reader's, while it's opening.
        if (manager.isStateSaved || manager.findFragmentByTag(TIME_PICKER_TAG) != null) return
        val initial = pickedTime ?: EventTimes.DEFAULT_TIME
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(if (DateFormat.is24HourFormat(this)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(initial.hour)
            .setMinute(initial.minute)
            .setTitleText(R.string.event_time)
            .build()
        listenTo(picker)
        picker.showNow(manager, TIME_PICKER_TAG)
    }

    private fun listenTo(picker: MaterialTimePicker) {
        picker.addOnPositiveButtonClickListener {
            pickedTime = LocalTime.of(picker.hour, picker.minute)
            showPickedTime()
        }
    }

    private fun showPickedTime() {
        editEventTime!!.setText(pickedTime?.let { timeOfDayText(it) } ?: "")
    }

    private fun showPickedZone() {
        editEventTimeZone!!.setText(TimeZoneChoices.fieldText(this, pickedZoneId))
    }

    private companion object {
        const val TIME_PICKER_TAG = "eventTimePicker"
        const val STATE_TIME = "pickedTime"
        const val STATE_TIME_ZONE = "pickedTimeZone"
    }
}
