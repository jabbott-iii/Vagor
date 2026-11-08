# 03 — Platforms

Vagor runs on Android phones from Android 8.0 (API 26) up. This file
collects what each Android version, device and locale changes, and how the
code handles it; the Where column names the file that does.
`docs/subsystems/reminders.md` has the full reminder rules, and
`docs/subsystems/dates-times-and-pickers.md` the date and time ones.

## Targets

| Setting | Value | Notes |
| :- | :- | :- |
| `minSdk` | 26 (Android 8.0) | `java.time` is available. The adaptive launcher icon in `mipmap-anydpi` serves every version, so there are no density PNGs |
| `targetSdk` | 36 (Android 16) | The Google Play minimum for new apps since 2026-08-31. At this level every screen draws edge to edge |
| `compileSdk` | 37 | Needs Android SDK platform 37 |
| Instrumented tests | An API 37 (Android 17) `google_apis` x86_64 emulator | Where CI runs them, and where they are checked locally (`ci.yml`) |
| Devices | Phones | The layouts are phone layouts, stretched across a tablet. The Play listing has no tablet screenshots, and Play shows the phone ones on tablets |
| Native code | None | So Play's 16 KB page size requirement doesn't apply |

Which Android versions have been checked on a real device is not recorded.
Where a behaviour hasn't been captured on a device, this file says so.

## Behaviour by Android version

| Android (API) | What changes | What Vagor does | Where |
| :- | :- | :- | :- |
| 8.0 and later (26+, every supported version) | Notifications need a channel | Reminders post on the `reminders` channel, which `ensureChannel` creates | `ReminderScheduler.kt`, `MyReceiver.kt` |
| 8 to 11 (26–30) | Exact alarms need no permission, and there is no Alarms & reminders screen | Event reminders are always exact (`setExactAndAllowWhileIdle`). The Settings tab hides its Alarms & Reminders button | `ReminderScheduler.kt`, `ExactAlarmOffer.kt`, `SettingsFragment.kt` |
| 8 to 11 (26–30) | An inexact alarm may be deferred by up to 75% of the time left until it's due: months, for a trip planned far ahead | Vacation reminders use `setWindow` with a one-hour window. Doze can still hold one until the phone wakes, and Battery Saver until it's off | `ReminderScheduler.setInexact` |
| 8 to 11 (26–30) | The system draws the launch window before any app code runs, following the system light/dark setting on every cold start | A known issue: a dark-mode user on a light-themed phone sees a brief light flash | `ThemeSettings.kt` |
| 8 to 11 (26–30) | Backup follows `fullBackupContent` | `allowBackup="false"` turns backup off; `backup_rules.xml` excludes every domain too, in case the flag is ever changed | `AndroidManifest.xml`, `res/xml/backup_rules.xml` |
| Before 9 (26–27) | `RippleDrawable` draws state-layer opacities differently | `vagor_on_primary_ripple` uses Material Components' values for each: `values/dimens.xml` before Android 9, `values-v28/dimens.xml` from it | `res/values*/dimens.xml` |
| Before 9 (26–27) | ICU's `SystemTimeZoneType.CANONICAL_LOCATION` arrived only in Android 9 | The time zone list is built from `getAvailableIDs`, `getCanonicalID` and each zone's region instead | `TimeZoneChoices.all` |
| 10 and lower (26–29) | Consuming window insets in a listener stops them reaching sibling views | The inset listeners pass the insets on | `EdgeToEdge.kt` |
| 11 (30) | An unused app only loses its runtime permissions, and Vagor has none there | `mayHibernate()` is false before Android 12 | `Hibernation.kt` |
| 12 and later (31+) | Exact alarms need Alarms & reminders (`SCHEDULE_EXACT_ALARM`), on by default only on Android 12 and 13 | `canScheduleExactAlarms()` before each exact alarm, and `SecurityException` caught. Without it, `setAndAllowWhileIdle` | `ReminderScheduler.kt` |
| 12 and later (31+) | An inexact alarm comes within an hour after its moment. Allow-while-idle alarms fire through Doze and Battery Saver, within a quota, and get `FLAG_ALLOW_WHILE_IDLE_COMPAT`, which app standby buckets still limit | Vacation reminders, and event reminders that can't be exact, use `setAndAllowWhileIdle` (ADR-0031) | `ReminderScheduler.setInexact` |
| 12 and later (31+) | Allowing exact alarms sends `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`. Taking them away sends nothing: Android deletes the exact alarms and kills the app (not a force stop). Setting Vagor's battery use to Unrestricted also allows them, with no broadcast | `ReminderRescheduleReceiver` rebuilds on the broadcast. Every new process rebuilds, so event reminders come back inexact the next time anything starts the app. `HomeActivity` rebuilds on resume if the exact-alarm state changed | `ReminderRescheduleReceiver.kt`, `VagorApplication.kt`, `HomeActivity.kt` |
| 12 and later (31+) | Hibernation: Android stops an app that hasn't been opened for about three months (checked about every 15 days), which cancels every alarm. Reminders that fire don't count as use | After saving a vacation that ends more than two months ahead, `HibernationOfferDialog` offers Vagor's App info, once per launch. The Settings tab says whether Android may pause Vagor and has an App Info button (ADR-0032) | `Hibernation.kt`, `SettingsFragment.kt` |
| 12 and later (31+) | The splash screen shows the launcher icon | The suitcase sits inside the adaptive icon's 66dp safe circle, so the splash screen shows it whole | `res/drawable/ic_launcher_foreground.xml` |
| 12 and later (31+) | `UiModeManager.setApplicationNightMode` lets the system remember the app's night mode | Later launches' splash screens match the dark mode switch; only the very first launch follows the system setting (not captured on a device) | `ThemeSettings.kt` |
| 12 and later (31+) | Backup follows `dataExtractionRules` | Cloud backup excludes every domain; a direct device-to-device transfer can still copy them (ADR-0004) | `res/xml/data_extraction_rules.xml` |
| 13 and later (33+) | `POST_NOTIFICATIONS` is a runtime permission | The dashboard asks for it once per launch when there is a trip, and shows `NotificationsOffDialog` if it's refused. `MyReceiver` posts nothing without it | `VacationsFragment.kt`, `MyReceiver.kt` |
| 13 and later (33+) | App standby: after 8 days without opening Vagor or tapping its notifications, Android puts it in the restricted bucket, where only one inexact alarm a day is sure to come on time | Allowing Alarms & reminders escapes the limit, which the app's text says. A Vagor notification the user sees moves it to the working set bucket for 12 hours, and charging suspends bucket limits | `ReminderScheduler.kt`, `res/values/strings.xml` |
| 13 and later (33+) | `getPackageInfo` takes `PackageInfoFlags` | `ownPackageInfo()` picks the call by version | `SettingsFragment.kt` |
| 14 and later (34+) | `SCHEDULE_EXACT_ALARM` is denied by default for new installs | `ExactAlarmOfferDialog` offers Alarms & reminders after saving an event whose reminder can't be exact, once per launch. The Settings tab says whether event reminders are exact | `ExactAlarmOffer.kt`, `SettingsFragment.kt` |
| 15 and later (35+) | Apps draw behind the status and navigation bars, with no opt-out from targetSdk 36 | Every screen pads for the bars and cutouts (`docs/01-architecture.md`, "Edge to edge and insets") | `EdgeToEdge.kt` |
| 17 (37) | The unused app setting is called "Manage app if unused". On the API 37 emulator, Android gives every notification without a timeout one of 72 hours | The app's text doesn't name the setting, because its name differs between Android versions. A late reminder, posted with no timeout of its own, still leaves the shade after 72 hours on that emulator | `res/values/strings.xml`, `MyReceiver.kt` |

## Rules that follow

- **Check every call's API level against minSdk 26,** and guard a newer one
  with a `Build.VERSION.SDK_INT` test, as the code does. ICU calls need
  this too: some, such as `SystemTimeZoneType.CANONICAL_LOCATION`, arrived
  later.
- **Never assume an exact alarm.** Check `canScheduleExactAlarms()` before
  each one, catch `SecurityException`, and fall back to an inexact alarm.
  Only event reminders use exact alarms. Never add `USE_EXACT_ALARM`: Google
  Play allows it only for calendar and alarm-clock apps (ADR-0014).
- **Ask for notifications where they matter.** Request `POST_NOTIFICATIONS`
  at runtime on API 33+ when the user sets a reminder, and check
  `areNotificationsEnabled()` and the reminder channel
  (`ReminderScheduler.notificationsOn`).
- **Expect alarms to vanish.** A reboot, a force stop and hibernation clear
  every alarm; taking exact alarms away deletes the exact ones (event
  reminders) and kills the app, which is not a force stop. Every new process
  rebuilds every reminder from the database (ADR-0025).
- **Say "may".** Android exempts work profiles and company-managed phones
  from hibernation, and some makers turn it off, which no public API
  reveals. So the app's text says Android "may" pause Vagor.
- **Never take `SCHEDULE_EXACT_ALARM` away during a test.** Android stops the
  app, test and all (`docs/04-quality.md`).

## Power and standby

| Condition | Effect on reminders | What helps |
| :- | :- | :- |
| Doze or Battery Saver, Android 8 to 11 | A vacation reminder (`setWindow`) can be held until the phone wakes, or until Battery Saver is off. Event reminders are exact and allowed while idle | — |
| Doze or Battery Saver, Android 12+ | Allow-while-idle alarms fire through both, within a quota. Event reminders that can't be exact changed from `setWindow` for 50 to 60 minutes before to `setAndAllowWhileIdle` on 2026-11-01, with the user, because Doze and Battery Saver could hold `setWindow` until after the event (ADR-0031) | — |
| The restricted standby bucket, Android 13+ | Only one inexact alarm a day is sure to come on time | Allowing Alarms & reminders. Nothing else an app can set escapes the bucket limits |
| Battery use set to Restricted | Every reminder is held until Vagor is opened | — |
| Battery use set to Unrestricted | Exact alarms are allowed, with no broadcast | `HomeActivity` rebuilds on resume |
| Hibernation, Android 12+ | Every alarm is cancelled until Vagor is next started | The hibernation offer, and the Settings tab's App Info button |
| A reboot | Alarms are cleared | `BOOT_COMPLETED` starts a new process, which rebuilds once |
| A force stop | Alarms are cleared | The next start rebuilds |
| An app update | The process ends | `MY_PACKAGE_REPLACED` starts a new process, which rebuilds once |
| The phone is off at a reminder's moment | A reminder whose moment has passed is never set, so it isn't shown late | By design; changing it is the user's call |

## Devices

- **The 500-alarm limit.** Android lets an app hold 500 alarms at a time.
  Past that, every set call throws, even one that replaces an alarm already
  set: `IllegalStateException`, or `SecurityException` on some Samsung
  phones. `ReminderScheduler.setAlarm` handles both (ADR-0024).
- **Phone makers and hibernation.** Some makers turn hibernation off, and
  company-managed phones are exempt; no public API reveals either.
  `mayHibernate()` checks Android 12+,
  `PackageManager.isAutoRevokeWhitelisted` and whether Vagor runs in a work
  profile, which Android exempts.
- **Narrow screens.** The drop-down calendar spans its field, up to 412dp
  (`calendar_dropdown_max_width`). Where the field is too narrow for seven
  48dp days (a 360dp-wide phone, or a larger display size), the calendar is
  wider than its field, up to the screen's width less 8dp on each side. On
  the narrowest screens (about 320dp) the days come out a little narrower
  than 48dp.
- **Short screens.** The calendar goes below its field if it fits there,
  else above it, else over it. On a screen too short for it, it scrolls
  inside its popup.
- **Landscape.** The detail forms scroll, with `adjustResize`. A full-screen
  landscape keyboard still covers the form until it is closed.
- **Font size.** Text fields and buttons grow with the font size, the
  bottom bar's tab labels do too, and the calendar's days are at least 48dp
  tall at any font size (`docs/subsystems/theme-and-accessibility.md`).
- **Keyboards and D-pads.** Keyboard focus is visible: Material's focus
  rings, the dashboard's and the time zone list's own, and the calendar
  days'. The calendar opens on the picked day for keyboard and D-pad users.
- **Display cutouts and 3-button navigation.** Screens pad for cutouts as
  for the system bars, and the home screen's bottom bar reaches behind the
  navigation bar with no scrim (`docs/01-architecture.md`, "Edge to edge and
  insets").

## Locales, calendars and clocks

- **English only** (ADR-0036). `androidResources.localeFilters` keeps the
  libraries' translations out, so their text (the time picker's, for
  example) stays English too.
- **Dates are shown the way the phone writes them** (ADR-0026): ICU's
  `yMMMd` skeleton in the app's locale, "Jul 6, 2031" in the US and "6 Jul
  2031" in the UK. Always in the Gregorian calendar, before 1582 too, where
  ICU would otherwise switch to the Julian calendar. A Thai phone would
  otherwise show the Buddhist-era year.
- **The drop-down calendar is Vagor's own** (ADR-0008). The framework
  `CalendarView` follows the phone's calendar system (a Thai phone would
  store Buddhist-era years) and marks today by colour alone; don't switch to
  it. Vagor's calendar:
  - is always Gregorian, from year 1 to 9999 (the years `MM/dd/yyyy` can
    hold), and marks today with a ring
  - starts each week on the locale's first day, with its narrow weekday
    names and its own digits
  - takes the month title's pattern from the locale without its Unicode
    extensions: one that asks for another calendar system (`-u-ca-chinese`,
    for example) gives pattern letters `java.time` can't format
  - is checked in Thai, US English and British English by
    `CalendarDropdownInstrumentedTest`, which switches the app's language
    and back
- **Right to left.** The manifest sets `supportsRtl`. A calendar wider than
  its field is placed from the field's left edge, in either direction of
  text.
- **12- and 24-hour clocks.** The time picker (`MaterialTimePicker`) and all
  time text follow the phone's setting (`DateFormat.is24HourFormat`):
  "2:30 PM" or "14:30".
- **Time zones** are stored as IANA zone IDs and named from ICU in the
  phone's language.
  - The list holds every zone that is the main ID for a place, plus UTC,
    plus the event's own zone when the list leaves it out (an older name,
    for example). Aliases such as `Asia/Calcutta` and `Asia/Kolkata` are the
    same zone.
  - The form's field names the zone and its city ("Japan Standard Time
    (Tokyo)"), so it isn't read as the event's place.
  - Many zones have no abbreviation, so a time can read "2:30 PM GMT+2".
  - A search ignores accents: "sao paulo" finds São Paulo.
- **The phone's own time zone.**
  - A new event's zone starts on it.
  - Vacation reminders, and those of events saved before events had a time,
    come around 9:00 AM in it. An event with no zone, or an unknown one, is
    placed in it.
  - `TIME_SET` and `TIMEZONE_CHANGED` always rebuild the reminders, because
    they can reach a process that is already running.
  - A posted reminder's timeout is a duration fixed when it's posted, so a
    9:00 AM one leaves at midnight in the zone the phone had then, even if
    the phone's zone changes after.
- **Daylight saving.**
  - An event's reminder comes an hour of elapsed time before it starts, so
    a change during that hour doesn't stretch or shrink it.
  - An event's time is shown as picked, even when a change skips it.
  - Add to Calendar ends an all-day vacation at least an hour into its last
    day and at 1:00 or later on the phone's clock, for zones whose clocks go
    back from 1:00 to midnight or skip from midnight to 1:00
    (`docs/subsystems/sharing-and-calendar.md`).

## Other apps Vagor opens

| What | Intent | When that fails |
| :- | :- | :- |
| Share | `ACTION_SEND`, `text/plain`, inside `Intent.createChooser` | — |
| Add to Calendar | `ACTION_INSERT` with `CalendarContract.Events.CONTENT_URI` and the type `vnd.android.cursor.dir/event`, aimed at Google Calendar (`com.google.android.calendar`) | Any calendar app, then the "no calendar app" dialog |
| Alarms & reminders (Android 12+) | `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` for Vagor | Vagor's App info |
| Notification settings | `ACTION_CHANNEL_NOTIFICATION_SETTINGS` when only the reminder channel is off, else `ACTION_APP_NOTIFICATION_SETTINGS` | Vagor's App info |
| App info | `ACTION_APPLICATION_DETAILS_SETTINGS` | Nothing opens |

Each one but Share catches `ActivityNotFoundException` and tries the next.
Add to Calendar needs no `<queries>` entry and no calendar permission
(ADR-0015).

Calendar apps differ (`docs/subsystems/sharing-and-calendar.md`):

- They read an all-day intent's times differently: in the phone's zone or
  in UTC, and an end at midnight as exclusive or not. Google Calendar
  documents none of it.
- Android's original calendar, Etar and Fossify read Vagor's times
  correctly. Google Calendar's reading couldn't be checked without a
  signed-in account.
- Android's original calendar saves when the user leaves its new-event
  screen, and some calendar apps save every new-event screen the user
  leaves, so a second tap within 2 seconds is ignored.
- They handle `Events.EVENT_TIMEZONE` unevenly, so an event's description
  names its time and zone too.

## Emulators

- **CI's emulator** is API 37 (`'37.0'`, quoted, because YAML would read
  37.0 as the number 37, and there is no android-37 image), `google_apis`,
  x86_64. It needs the Enable KVM step to run fast enough, and command-line
  tools 22.0: the runner's own tools, 12.0 on Ubuntu 24.04, give an
  `android-37.0` AVD `target=android-0`, and the emulator runner uses them
  whenever they exist (`docs/04-quality.md`).
- **On the API 37 emulator,** Android gives every notification without a
  timeout one of 72 hours, and demo mode didn't hide notification icons
  (clear the shade before a screenshot, `docs/05-release.md`, step 6).
- **Play's test-track builds** install on an emulator only with a Google Play
  system image, not a `google_apis` one (`docs/05-release.md`, step 7).
- **Play Console's device check** needs a real, non-rooted phone running
  Android 10 or later; an emulator doesn't count (`docs/05-release.md`,
  step 7).
- **`adb -e` reaches only an emulator.** Release-build checks and
  screenshots run only on an emulator (`docs/05-release.md`, steps 3 and 6);
  instrumented tests run only on an emulator or a test device. Never use a
  phone that holds real trips.

## Holds on every supported version

- Nothing reaches the network, and the manifest asks for the same three
  permissions: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` and
  `SCHEDULE_EXACT_ALARM` (`POST_NOTIFICATIONS` is asked for at run time only
  from Android 13).
- Dates are stored, picked and shown in the Gregorian calendar.
- Every new process rebuilds every reminder from the database.
- A reminder whose moment has passed is never set.
