---
paths:
  - "app/src/*/java/com/example/vagor/Reminder*.kt"
  - "app/src/main/java/com/example/vagor/MyReceiver.kt"
  - "app/src/main/java/com/example/vagor/ExactAlarmOffer.kt"
  - "app/src/main/java/com/example/vagor/NotificationsOff.kt"
  - "app/src/main/java/com/example/vagor/Hibernation.kt"
  - "app/src/main/java/com/example/vagor/VagorApplication.kt"
---

# Rules for reminders, alarms and notifications

Reminders are automatic for every item (decided with the user on
2026-10-25, ADR-0005). How and when they fire was decided with the user
step by step, and **changing any of it is the user's call**: ask, then
record the answer with `/record-decision`. The spec is
`docs/subsystems/reminders.md`.

- **When each reminder fires:**
  - **Vacations:** around 9:00 AM local time on the start and end dates,
    with inexact alarms. On Android 12+ (`setAndAllowWhileIdle`) they
    usually arrive within an hour; on Android 8–11 (`setWindow`) Doze can
    hold them until the phone wakes.
  - **Events:** an hour before the event starts, in its own time zone
    (`ReminderTimes.eventTriggerAtMillis`; decided on 2026-10-26, ADR-0014),
    with an exact alarm (`setExactAndAllowWhileIdle`) whenever Android
    allows one: always below Android 12, and on Android 12+ while Alarms &
    reminders (`SCHEDULE_EXACT_ALARM`) is on, which it is by default only on
    Android 12 and 13.
  - **Events without exact alarms:** an inexact allow-while-idle alarm
    (`setAndAllowWhileIdle`), which Android 12+ delivers within an hour
    after its moment: any time in the hour before the event, and near its
    start on a phone that nothing else wakes in that hour. Changed from
    `setWindow` for 50 to 60 minutes before on 2026-11-01, with the user
    (ADR-0031): Doze and Battery Saver could hold that until after the
    event, and allow-while-idle fires through both, within a quota.
  - **Events saved before events had a time** keep the 9:00 AM reminder on
    their date.
  - **A reminder whose moment has passed is never set,** so one missed while
    the phone was off isn't shown late.
- **Standby buckets still limit inexact alarms.** Android 12+ gives an app's
  inexact allow-while-idle alarms `FLAG_ALLOW_WHILE_IDLE_COMPAT`. Once
  Android puts an unused Vagor in the restricted bucket (after 8 days without
  opening it or tapping its notifications, on Android 13+), only one inexact
  alarm a day is sure to come on time. A Vagor notification the user sees
  moves Vagor to the working set bucket for 12 hours, and charging suspends
  bucket limits, but no alarm an app can set without exact-alarm permission
  escapes them. Allowing Alarms & reminders does, which the app's text says.
- **Exact alarms:**
  - Only event reminders use them.
  - `SCHEDULE_EXACT_ALARM` is denied by default for new installs on Android
    14+, so check `ReminderScheduler.canScheduleExactAlarms` before each
    exact alarm and catch `SecurityException`.
  - Never add `USE_EXACT_ALARM`: Google Play allows it only for calendar and
    alarm-clock apps.
  - After saving an event whose reminder can't be exact,
    `ExactAlarmOfferDialog` offers Alarms & reminders, once per launch. The
    Settings tab says whether event reminders are exact and opens the same
    screen.
- **Keep scheduling in `ReminderScheduler`.** The detail screens call it on
  every save and delete.
- **Every alarm goes through `ReminderScheduler.setAlarm`** (fixed
  2026-10-31, ADR-0024). Android lets an app hold 500 alarms at a time, and
  past that every set call throws, even one that replaces an alarm already
  set (`IllegalStateException`, or `SecurityException` on some Samsung
  phones). Uncaught, that crashed every start, since each start rebuilds the
  reminders, and only clearing the app's storage (every trip) got out.
  `setAlarm` cancels the reminder's own old alarm and tries once more, then
  leaves the reminder unset. Which reminders get the 500 places past the
  limit, and whether the user is told, is the user's call.
- **Rebuilds (ADR-0025):** alarms are cleared on reboot and force stop.
  - `VagorApplication` rebuilds every reminder from the database once in
    every new process (`ReminderScheduler.rebuildIfNeeded`), and
    `HomeActivity` again on resume if exact alarms were allowed or taken away
    since.
  - `ReminderRescheduleReceiver` calls `rebuildIfNeeded` on `BOOT_COMPLETED`
    and `MY_PACKAGE_REPLACED`, which always start a new process that
    `VagorApplication` has just rebuilt in, so it rebuilds once, not twice
    (changed 2026-10-31). It does the same on
    `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` (sent when the user
    allows exact alarms), which may reach a running process, where the
    exact-alarm state now differs, so it rebuilds.
  - On `TIME_SET` and `TIMEZONE_CHANGED` it always rebuilds, because they can
    reach a process that is already running. Dates saved without a time zone
    follow the phone's.
  - Taking exact alarms away sends nothing: Android deletes the exact alarms
    and kills the app (not a force stop), and the event reminders come back
    inexact the next time anything starts the app. Setting Vagor's battery
    use to Unrestricted also allows exact alarms, with no broadcast.
- **A rebuild never cancels a reminder that may still be on its way,** since
  inexact alarms can come late: an event's is kept until the event starts,
  and a 9:00 AM one until its day ends (`keepDue`). Saving an event keeps it
  too, unless the date, time or time zone changed; then a reminder already
  posted for it is dismissed, because it gives the old time
  (`ReminderScheduler.eventSaved`).
- **Posted reminders leave the shade once out of date** (`setTimeoutAfter`,
  asked for by the user on 2026-10-31, ADR-0027):
  - a 9:00 AM one when its day ends on the phone, and an event's an hour
    after the event starts (`ReminderTimes.dateReminderTimeoutMillis` and
    `eventReminderTimeoutMillis`)
  - one that Android delivers after that, held back by Doze for example, is
    still posted, with no timeout of its own: the user chose not to skip late
    reminders
  - the timeout is a duration fixed when the reminder is posted, so a 9:00
    AM one leaves at midnight in the zone the phone had then, even if the
    phone's zone changes after
  - on the API 37 emulator Android gives every notification without a
    timeout one of 72 hours anyway
- **Notifications:**
  - Post a `NotificationCompat` notification on the app's notification
    channel (`ReminderScheduler.CHANNEL_ID`; channels are required on every
    supported API level).
  - Request `POST_NOTIFICATIONS` at runtime on API 33+ when the user sets a
    reminder, and check `areNotificationsEnabled()`.
  - While notifications are off, for the app or only for the reminder
    channel (`ReminderScheduler.notificationsOn`), the dashboard shows
    `NotificationsOffDialog` once per launch when there is a trip (on
    Android 13+ after asking for the permission, if it isn't granted), and
    the Settings tab shows a Notification Settings button. Both open
    Android's notification settings for Vagor, or the reminder channel's when
    only it is off.
  - Not a toast: Android cuts toasts at two lines.
  - Tapping a vacation reminder opens the vacation's page; tapping an event
    reminder opens the event, with the vacation's page and the dashboard
    behind it.
- **Hibernation (decided with the user on 2026-11-01, ADR-0032):**
  - From Android 12, Android hibernates an app that hasn't been opened for
    about three months (checked about every 15 days). It stops the app,
    which cancels every alarm, and reminders that fire don't count as use.
    The alarms come back only when Vagor is next started.
  - After saving a vacation that ends more than two months ahead
    (`ReminderTimes.mayOutlastHibernation`) while Android may hibernate
    Vagor (`mayHibernate`: Android 12+,
    `PackageManager.isAutoRevokeWhitelisted` false, and not a work profile,
    which Android exempts), `HibernationOfferDialog` offers Vagor's App
    info, where the user turns off the unused app setting, once per launch.
  - Not only while notifications are on: on Android 13+ the dashboard asks
    for them after the first trip is saved, and that first trip is the one
    the offer is most for.
  - Company-managed phones are exempt too, and some makers turn hibernation
    off, which no public API reveals, so the app's text says Android "may"
    pause Vagor. The setting's name differs between Android versions
    ("Manage app if unused" on API 37), so the app's text doesn't name it.
- **Identity:** a PendingIntent's identity is its request code plus the
  Intent's action, data, type, component and categories. Extras don't count.
  Each reminder's identity is a data URI from `ReminderKey`, such as
  `vagor://reminder/vacation/<id>/start`, and the notification tag uses the
  same URI. A reminder's extras are copied when it's posted, which is why the
  event form reads the event from the database instead.
- **Reminders aren't stored.** Opt-in reminders would have to be saved in the
  database, which is a schema change (`/change-database-schema`).
- **Keep the promises true:** `LICENSE` section 6 says reminders can be late
  or stopped, and the listing promises an exact time only when Alarms &
  reminders is allowed ("exactly an hour before when you allow alarms and
  reminders").
- **Before reporting done:**
  - run `./gradlew testDebugUnitTest --tests 'com.example.vagor.ReminderScheduleTest'`
    for any change to reminder times or identities
  - `ReminderInstrumentedTest` posts real notifications and
    `ReminderLimitInstrumentedTest` sets 520 real alarms: run them only on an
    emulator, after asking (`.claude/rules/instrumented-tests.md`)
  - behaviour under real Doze, Battery Saver, standby buckets and
    hibernation can only be checked on a device: say it wasn't verified
