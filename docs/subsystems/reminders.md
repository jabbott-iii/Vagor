# Subsystem: Reminders

The system notifications Vagor posts for each vacation's start and end and
for each event, and the alarms that trigger them: `ReminderSchedule.kt`
(`ReminderType`, `ReminderKey`, `ReminderTimes`), `ReminderScheduler.kt`,
`MyReceiver.kt`, `ReminderRescheduleReceiver.kt`, `VagorApplication.kt`,
`ExactAlarmOffer.kt`, `NotificationsOff.kt` and `Hibernation.kt`, all in
`app/src/main/java/com/example/vagor/`, plus the calls into them from
`HomeActivity`, `VacationsFragment`, `SettingsFragment`,
`VacationDetailActivity` and `EventDetailActivity`. Everything here is
built. It covers product requirement 3 in `docs/00-overview.md`, and
ADR-0005, ADR-0014, ADR-0024, ADR-0025, ADR-0027, ADR-0031 and ADR-0032.
How reminders behave on each Android version is summarized in
`docs/03-platforms.md`. Path-scoped rules: `.claude/rules/reminders.md`.

## Contents

| File | Provides |
| :- | :- |
| `ReminderSchedule.kt` | Plain-Kotlin reminder times and identities: `ReminderType`, `ReminderKey` (the reminder's URI), `ReminderTimes` (trigger moments, timeouts, the hibernation margin) |
| `ReminderScheduler.kt` | `ReminderScheduler`: the only place that sets and cancels reminder alarms; the notification channel; whether exact alarms and notifications are allowed; the rebuild of every reminder |
| `MyReceiver.kt` | The alarm receiver: reads the item from the database and posts the notification |
| `ReminderRescheduleReceiver.kt` | Rebuilds reminders after a reboot, an app update, a clock or time zone change, and when exact alarms are allowed |
| `VagorApplication.kt` | Applies the saved theme before any screen, and rebuilds the reminders once in each new process |
| `ExactAlarmOffer.kt` | The way to Android's Alarms & reminders screen, and `ExactAlarmOfferDialog` after saving an event whose reminder can't be exact |
| `NotificationsOff.kt` | `NotificationsOffDialog` and the way to Android's notification settings |
| `Hibernation.kt` | Whether Android may pause an unused Vagor, the way to its App info, and `HibernationOfferDialog` after saving a trip far ahead |

## What fires when

Decided with the user on 2026-10-25: reminders are automatic for every
item (ADR-0005). Decided on 2026-10-26: an event's reminder fires an hour
before it starts (ADR-0014). Changing any of this is the user's call.

| Reminder | When | Alarm |
| :- | :- | :- |
| Vacation start | Around 9:00 AM local time on the start date | Inexact |
| Vacation end | Around 9:00 AM local time on the end date | Inexact |
| Event | An hour before it starts, in its own time zone (`ReminderTimes.eventTriggerAtMillis`) | Exact (`setExactAndAllowWhileIdle`) whenever Android allows one, otherwise inexact |
| Event saved before events had a time | 9:00 AM on its date | Inexact |

- **Inexact alarms** (`setInexact` in `ReminderScheduler`):
  - On Android 12+ they are `setAndAllowWhileIdle`. Android 12+ caps an
    inexact alarm's delay at an hour, so a vacation reminder usually arrives
    within an hour, and allow-while-idle lets it fire during Doze and
    Battery Saver, within a quota.
  - On Android 8–11 they are `setWindow` with a one-hour window. Older
    versions may otherwise defer an inexact alarm by up to 75% of the time
    left until it's due, which is months for a trip planned far ahead. Doze
    can hold them until the phone wakes; an idle phone or Battery Saver can
    hold one until the phone wakes or Battery Saver is off.
- **Exact alarms** are for event reminders only. Android allows one always
  below Android 12, and on Android 12+ while Alarms & reminders
  (`SCHEDULE_EXACT_ALARM`) is on, which it is by default only on Android 12
  and 13. Android also allows them for an app whose battery use is set to
  Unrestricted.
- **An event reminder without exact alarms** is an inexact allow-while-idle
  alarm (`setAndAllowWhileIdle`, like the vacation reminders on Android
  12+), which Android 12+ delivers within an hour after its moment: any time
  in the hour before the event, and near its start on a phone that nothing
  else wakes in that hour (Android delivers a wakeup alarm when one reaches
  the end of its window). Changed from `setWindow` for 50 to 60 minutes
  before on 2026-11-01, with the user (ADR-0031): Doze and Battery Saver
  could hold that until after the event, and allow-while-idle fires through
  both, within a quota.
- **The hour before an event is elapsed time**, so a daylight saving change
  during it doesn't stretch or shrink it.
- **App standby buckets.** Android 12+ gives an app's inexact
  allow-while-idle alarms `FLAG_ALLOW_WHILE_IDLE_COMPAT`, which app standby
  buckets still limit:
  - Once Android puts an unused Vagor in the restricted bucket (after 8 days
    without opening it or tapping its notifications, on Android 13+), only
    one inexact alarm a day is sure to come on time.
  - A Vagor notification the user sees moves Vagor to the working set
    bucket for 12 hours, and charging suspends bucket limits, but no alarm
    an app can set without exact-alarm permission escapes them.
  - Allowing Alarms & reminders does, which the app's text says
    (`event_reminders_inexact`, `exact_reminders_message`).
- **Battery use set to Restricted** holds every reminder until Vagor is
  opened.
- **A reminder whose moment has passed is never set,** so one missed while
  the phone was off isn't shown late.

## Key types

```kotlin
enum class ReminderType { VACATION_START, VACATION_END, EVENT }

data class ReminderKey(val type: ReminderType, val itemId: Int) {
    fun toUriString(): String        // vagor://reminder/vacation/<id>/start, .../end, vagor://reminder/event/<id>
    companion object {
        fun fromUriString(uri: String?): ReminderKey?
    }
}

object ReminderTimes {
    val REMINDER_TIME: LocalTime = LocalTime.of(9, 0)
    val EVENT_LEAD: Duration = Duration.ofHours(1)
    val HIBERNATION_MARGIN: Period = Period.ofMonths(2)
    val EVENT_REMINDER_EXPIRY: Duration = Duration.ofHours(1)
    fun mayOutlastHibernation(endDateText: String, today: LocalDate): Boolean
    fun triggerAtMillis(dateText: String, zone: ZoneId, nowMillis: Long): Long?
    fun eventTriggerAtMillis(start: ZonedDateTime, nowMillis: Long): Long?
    fun dateReminderMayBeDue(dateText: String, zone: ZoneId, nowMillis: Long): Boolean
    fun eventReminderMayBeDue(start: ZonedDateTime, nowMillis: Long): Boolean
    fun dateReminderTimeoutMillis(dateText: String, zone: ZoneId, nowMillis: Long): Long?
    fun eventReminderTimeoutMillis(event: Event, zone: ZoneId, nowMillis: Long): Long?
}

object ReminderScheduler {
    const val CHANNEL_ID = "reminders"
    fun ensureChannel(context: Context)
    fun scheduleVacation(context: Context, vacation: Vacation, keepDue: Boolean = false)
    fun cancelVacation(context: Context, vacationId: Int)
    fun scheduleEvent(context: Context, event: Event, keepDue: Boolean = false)
    fun eventSaved(context: Context, event: Event, timingChanged: Boolean)
    fun canScheduleExactAlarms(context: Context): Boolean
    fun notificationsOn(context: Context): Boolean
    fun cancelEvent(context: Context, eventId: Int)
    fun rescheduleAll(context: Context, db: AppDatabase)
    fun rebuildIfNeeded(context: Context)
    private fun setAlarm(context: Context, key: ReminderKey, set: (AlarmManager, PendingIntent) -> Unit)
}

class MyReceiver : BroadcastReceiver()
class ReminderRescheduleReceiver : BroadcastReceiver()
class VagorApplication : Application()

fun Context.exactAlarmSettingsIntent(): Intent?
fun Context.openExactAlarmSettings()
class ExactAlarmOfferDialog : DialogFragment() {
    companion object {
        fun showAfterSave(manager: FragmentManager, context: Context, saved: Event): Boolean
    }
}

fun Context.openNotificationSettings()
class NotificationsOffDialog : DialogFragment() {
    companion object {
        fun show(manager: FragmentManager)
    }
}

fun Context.mayHibernate(): Boolean
fun Context.openAppInfo()
class HibernationOfferDialog : DialogFragment() {
    companion object {
        fun showAfterSave(manager: FragmentManager, context: Context, saved: Vacation): Boolean
    }
}
```

## Rules

### Scheduling

- **Keep scheduling in `ReminderScheduler`.** It is the only place that
  sets or cancels reminder alarms. Detail screens call it on every save and
  delete:
  - `VacationDetailActivity`: `scheduleVacation` after an insert or an
    update, `cancelVacation` after a delete
  - `EventDetailActivity`: `eventSaved` after a save, `cancelEvent` after a
    delete
- **Deleting** cancels the item's alarms and dismisses a reminder already
  posted for it.
- **Saving an event** keeps an alarm that may still be on its way, unless
  the date, time or time zone changed (or the event is new); then a
  reminder already posted for it is dismissed, because it gives the old
  time.
- **Exact alarms:** `SCHEDULE_EXACT_ALARM` is denied by default for new
  installs on Android 14+, so check `canScheduleExactAlarms()`
  (`ReminderScheduler`) before each exact alarm and catch
  `SecurityException`. As built, a refused exact alarm falls back to an
  inexact one.
- **Never add `USE_EXACT_ALARM`:** Google Play allows it only for calendar
  and alarm-clock apps. The manifest declares only `POST_NOTIFICATIONS`,
  `RECEIVE_BOOT_COMPLETED` and `SCHEDULE_EXACT_ALARM` (ADR-0001,
  `.claude/rules/manifest-privacy-and-legal.md`). Play's exact alarm policy,
  as checked on 2026-10-31, asks for a declaration only for
  `USE_EXACT_ALARM`; `docs/05-release.md` (the checklist's "Already in the
  app") says how to answer a permissions declaration if Play Console shows
  one at the first upload.
- **Opt-in reminders** would have to be saved in the database, which is a
  schema change (ADR-0003, `/change-database-schema`).
- **Keep what users are told true.** `LICENSE` section 6 says the reminders
  depend on Android and the device's notification, alarm and battery
  settings, which can delay or stop them; the privacy policy (PRIVACY.md
  and `privacy_policy_text`) says reminders are scheduled on the device; the
  store listing promises an exact time only when Alarms & reminders is
  allowed ("within the hour before each event (exactly an hour before when
  you allow alarms and reminders)";
  `fastlane/metadata/android/en-US/full_description.txt`). A change to
  how reminders behave checks all three
  (`.claude/rules/manifest-privacy-and-legal.md`).

### Identity

- **A PendingIntent's identity** is its request code plus the Intent's
  action, data, type, component and categories. Extras don't count.
- **Each reminder's identity is a data URI from `ReminderKey`,** such as
  `vagor://reminder/vacation/<id>/start`, and the notification tag uses the
  same URI. So each reminder can be replaced or cancelled on its own.
- As built, every alarm PendingIntent targets `MyReceiver` with request
  code 0 and `FLAG_IMMUTABLE`, and differs only by that URI.
- **Tapping a reminder** opens a task stack whose last intent carries the
  reminder's URI, which keeps each reminder's PendingIntent separate.
- Alarms left by older versions have no reminder URI. `MyReceiver` ignores
  them; their items get new reminders after the update.

### The 500-alarm limit (ADR-0024)

- Android lets an app hold 500 alarms at a time, and past that every set
  call throws, even one that replaces an alarm already set
  (`IllegalStateException`, or `SecurityException` on some Samsung phones).
- Uncaught, that crashed every start, since each start rebuilds the
  reminders, and only clearing the app's storage (every trip) got out.
- **Every alarm goes through `ReminderScheduler.setAlarm`,** which cancels
  the reminder's own old alarm, which frees a place, and tries once more,
  then leaves the reminder unset (fixed 2026-10-31,
  `ReminderLimitInstrumentedTest`).
- Which reminders get the 500 places past the limit, and whether the user
  is told, is the user's call (Open questions).

### Rebuilding (ADR-0025)

- **Alarms are cleared on reboot and force stop.** `VagorApplication`
  rebuilds every reminder from the database once in every new process
  (`ReminderScheduler.rebuildIfNeeded`), whatever starts it: a launch, a
  screen restored after Android stopped the app, or an alarm or broadcast.
  A force stop or an app update also ends the process.
- **`HomeActivity` checks again on resume** and rebuilds if exact alarms
  were allowed or taken away since the last rebuild. Setting Vagor's
  battery use to Unrestricted allows exact alarms with no broadcast, so
  this catches the way back.
- **`ReminderRescheduleReceiver`** (not exported) handles the broadcasts in
  the table below.

| Broadcast | Calls | Why |
| :- | :- | :- |
| `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED` | `rebuildIfNeeded` | They always start a new process that `VagorApplication` has just rebuilt in, so it rebuilds once, not twice (changed 2026-10-31) |
| `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` | `rebuildIfNeeded` | Sent when the user allows exact alarms; it may reach a running process, where the exact-alarm state now differs, so it rebuilds |
| `TIME_SET`, `TIMEZONE_CHANGED` | `rescheduleAll`, always | They can reach a process that is already running; dates saved without a time zone follow the phone's |

- **Taking exact alarms away sends nothing:** Android deletes the exact
  alarms and kills the app (not a force stop), and the event reminders come
  back inexact the next time anything starts the app.
- **A rebuild never cancels a reminder whose moment has passed but which
  may still be on its way,** since inexact alarms can come late: an event's
  is kept until the event starts, and a 9:00 AM one until its day ends
  (`keepDue`, `ReminderTimes.eventReminderMayBeDue` and
  `dateReminderMayBeDue`).
- In short: reminders are rescheduled after a reboot, a clock or time zone
  change, an app update, when exact alarms are allowed, and whenever the
  app starts.

### Posting (`MyReceiver`)

- **Post a `NotificationCompat` notification on an app notification
  channel** (channels are required on every supported API level). The
  channel is `reminders` ("Trip reminders", default importance), created by
  `ReminderScheduler.ensureChannel`. Each reminder has the reminder
  category, auto-cancels when tapped, and uses `ic_notification`.
- On API 33+ it posts nothing without the `POST_NOTIFICATIONS` permission.
- It opens its own Room instance with `AppDatabase.open()` and closes it in
  `finally`. An item deleted since the alarm was set posts nothing.
- **A vacation reminder:** "<title> is starting today" or "<title> is
  ending today", with the vacation's dates; expanded, it also shows the
  trip details.
- **An event reminder:** "<title> starts at <time>", such as "7:00 PM
  GMT+9", which stays true however late it's read; "<title> is happening
  today" for an event saved without a time. Its text is "Part of
  <vacation>"; expanded, it also shows the event's details.
- Dates and times in the text go through `TripText.kt`
  (`docs/subsystems/dates-times-and-pickers.md`).
- **Tapping** a vacation reminder opens the vacation's page, with the
  dashboard behind it; tapping an event reminder opens the event, with the
  vacation's page and the dashboard behind it. The event form reads the
  event from the database rather than the extras, because a reminder's
  extras are copied when it's posted
  (`docs/subsystems/screens-and-navigation.md`).
- **Out-of-date reminders leave the shade** (`setTimeoutAfter`, asked for
  by the user on 2026-10-31, ADR-0027):
  - a 9:00 AM one when its day ends on the phone
    (`ReminderTimes.dateReminderTimeoutMillis`)
  - an event's an hour after the event starts
    (`ReminderTimes.eventReminderTimeoutMillis`); one for an event saved
    without a time when its day ends
  - one that Android delivers after that, held back by Doze for example, is
    still posted, with no timeout of its own: the user chose not to skip
    late reminders
  - on the API 37 emulator Android gives every notification without a
    timeout one of 72 hours anyway
  - the timeout is a duration fixed when the reminder is posted, so a
    9:00 AM one leaves at midnight in the zone the phone had then, even if
    the phone's zone changes after

### Notifications off

- **Declare `POST_NOTIFICATIONS`, request it at runtime on API 33+ when the
  user sets a reminder, and check `areNotificationsEnabled()`.** As built,
  the dashboard (`VacationsFragment`) does this once per launch as soon as
  there is a vacation, since every saved vacation gets reminders, after
  creating the channel.
- **While notifications are off,** for the app or only for the reminder
  channel (`ReminderScheduler.notificationsOn`):
  - the dashboard shows `NotificationsOffDialog` once per launch when there
    is a trip (on Android 13+ after asking for the permission, if it isn't
    granted)
  - the Settings tab shows a Notification Settings button
    (`docs/subsystems/settings-screen.md`)
- Both open Android's notification settings for Vagor, or the reminder
  channel's when only it is off, or Vagor's App info on a phone that lacks
  them.
- **Not a toast:** Android cuts toasts at two lines.

### Alarms & reminders

- After saving an event whose reminder can't be exact,
  `ExactAlarmOfferDialog` offers Alarms & reminders, once per launch. As
  built, it is shown only when exact alarms aren't allowed, notifications
  are on (without them no reminder shows), and the event's reminder is
  still to come.
- Whichever way the offer is closed (Open Settings, Not now, Back or a tap
  outside), the event form behind it closes too, as it does after any save.
- Open Settings opens Android's Alarms & reminders screen for Vagor, or
  its App info on a phone that lacks it. Before Android 12 there is no such
  screen (`exactAlarmSettingsIntent()` is null), and event reminders are
  always exact.
- The Settings tab says whether event reminders are exact and opens the
  same screen (`docs/subsystems/settings-screen.md`).

### Hibernation (ADR-0032)

- **From Android 12, Android hibernates an app** that hasn't been opened
  for about three months (checked about every 15 days): it stops the app,
  which cancels every alarm, and reminders that fire don't count as use.
  The alarms come back only when Vagor is next started, so a trip planned
  further ahead could lose its reminders.
- **Decided with the user on 2026-11-01:** after saving a vacation that
  ends more than two months ahead (`ReminderTimes.mayOutlastHibernation`,
  `HIBERNATION_MARGIN`) while Android may hibernate Vagor
  (`mayHibernate`), `HibernationOfferDialog` offers Vagor's App info, where
  the user turns off the unused app setting, once per launch.
- **`mayHibernate`** is true on Android 12+ when
  `PackageManager.isAutoRevokeWhitelisted` is false and Vagor isn't in a
  work profile, which Android exempts. On Android 11 an unused app only
  loses its runtime permissions, and Vagor has none there.
- **Not only while notifications are on:** on Android 13+ the dashboard
  asks for them after the first trip is saved, and that first trip is the
  one the offer is most for.
- **The app's text says Android "may" pause Vagor:** Android also exempts
  company-managed phones, and some makers turn hibernation off, which no
  public API reveals.
- **The setting isn't named in the app's text,** because its name differs
  between Android versions ("Manage app if unused" on API 37).
- Whichever way the offer is closed, the vacation form behind it closes
  too, as it does after any save.
- The Settings tab says whether Android may pause Vagor and has an App
  Info button (Android 12+; `docs/subsystems/settings-screen.md`).

### Dialogs

- `ExactAlarmOfferDialog`, `NotificationsOffDialog` and
  `HibernationOfferDialog` are `DialogFragment`s, so they are still open
  after a rotation (ADR-0033); a dialog shown straight from
  `MaterialAlertDialogBuilder` closes when the screen is recreated (fixed
  2026-11-01). Each is shown only when the fragment
  manager's state isn't saved and no copy is already showing.
- A form that closes after a save disables its buttons first, so a second
  tap, or one behind an offer that keeps it open, can't save twice.

## Invariants

- No code outside `ReminderScheduler` sets or cancels a reminder alarm, and
  every alarm it sets goes through `setAlarm`.
- One alarm at most per vacation start, vacation end and event, named by
  its `ReminderKey` URI; a posted reminder's tag is the same URI.
- An alarm is never newly set for a moment that has passed.
- No start of the app crashes from Android's alarm limit.
- An exact alarm is tried only after `canScheduleExactAlarms`, and a
  refused one leaves an inexact alarm instead.
- Each new process rebuilds the reminders before any screen uses them.

## Limits

| Limit | Value | Notes |
| :- | :- | :- |
| Alarms an app can hold | 500 (Android) | Past it, a reminder is left unset |
| Vacation reminders, and those of events saved without a time | 9:00 AM in the phone's time zone | `ReminderTimes.REMINDER_TIME` |
| Event reminder lead | 1 hour of elapsed time | `ReminderTimes.EVENT_LEAD` |
| Inexact window, Android 8–11 | 1 hour | `setWindow` |
| Inexact delay, Android 12+ | Up to an hour after the moment | `setAndAllowWhileIdle` |
| Restricted standby bucket | After 8 days unused, Android 13+ | One inexact alarm a day is sure to come on time |
| Working set bucket after a seen notification | 12 hours | |
| Hibernation | About three months unused, checked about every 15 days, Android 12+ | |
| Hibernation offer | A vacation ending more than two months ahead | `ReminderTimes.HIBERNATION_MARGIN` |
| Posted event reminder | Until an hour after the event starts | `ReminderTimes.EVENT_REMINDER_EXPIRY` |
| Posted 9:00 AM reminder | Until its day ends on the phone | |
| Notification timeout on the API 37 emulator | 72 hours for any notification without one | Android's, not Vagor's |
| Offers per launch | One each: exact alarms, hibernation, notifications off | |

## Tests

- **JVM** (`ReminderScheduleTest`): 9:00 AM local on the stored date;
  the trigger follows its zone; 9:00 AM across a daylight saving change; no
  trigger once 9:00 AM has passed or for an invalid date; an event's
  trigger an hour before it starts in its own zone, possibly the day
  before, an elapsed hour across daylight saving changes, none once that
  hour has passed; a passed reminder may be due until its day ends or the
  event starts; a trip ending more than two months ahead may outlast
  hibernation; the timeouts (a date reminder until its day ends, including
  the extra hour when clocks go back; an event's until an hour after it
  starts; an untimed event's until its day ends); `ReminderKey` URIs round
  trip, stay distinct for large ids, and unknown URIs are refused.
- **Instrumented** (`app/src/androidTest/java/com/example/vagor/`):
  - `ReminderInstrumentedTest`: saving schedules and deleting cancels;
    an event's reminder until an hour before it starts; a rebuild keeps an
    alarm that may still be on its way; a new time dismisses the posted
    reminder; a date moved into the past cancels; `rescheduleAll` restores
    from the database; a fired reminder posts a notification; an event
    reminder gives its start time and details; a late reminder is posted
    without a timeout; tapping opens the vacation page or the event; a
    deleted item posts nothing. It writes to the real `vacation_database`,
    sets real alarms and posts real notifications.
  - `ReminderLimitInstrumentedTest` sets 520 real event alarms, past
    Android's limit of 500, for events that aren't in the database, and
    cancels them afterwards; a run killed partway leaves them set until
    they fire 30 days later or the app is uninstalled.
  - `EventFormInstrumentedTest` (the exact alarm offer, once; it opens
    Alarms & reminders; Back closes the form). These run only while exact
    alarms are off, as they are after a fresh install on Android 14+. Never
    take `SCHEDULE_EXACT_ALARM` away during a test, because Android stops
    the app, test and all.
  - `HomeFlowInstrumentedTest` (the hibernation offer, once; it opens App
    info; Back closes the form). These run only on Android 12+ while
    Android may pause Vagor, as it may after any install.
  - `SettingsInstrumentedTest`: whether event reminders are exact,
    whether Android may pause Vagor, the Notification Settings button only
    while notifications are off, the notifications-off dialog opening
    notification settings.
  - The home flow, reminder, settings and event form tests grant the
    notification permission and leave it granted. Screens of other apps
    are left through `leaveOtherAppsScreen()`
    (`OtherAppTesting.kt`) or Back (`docs/04-quality.md`).
  - Every instrumented run starts the app's process, which re-arms every
    stored trip's reminders as any start does (`VagorApplication`).
- **On a device only:** a reminder's real timing. `docs/05-release.md`,
  step 3, checks it on the emulator: an event added about 70 minutes ahead
  gets its notification about 10 minutes later (up to 20 when Alarms &
  reminders is off).
- Run instrumented tests only on an emulator or a test device, never on a
  phone that holds real trips (`.claude/rules/instrumented-tests.md`).

## Open questions

- **Past the 500-alarm limit:** which reminders get the 500 places, and
  whether the user is told. The user's call; listed in `docs/roadmap.md` M7.
