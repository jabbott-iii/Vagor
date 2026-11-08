# 01 — Architecture

## Principles

1. **Everything stays on the phone.** Vagor has no backend, no accounts and
   no network access. Data leaves the app only when the user shares it
   through the Android Sharesheet, or hands one vacation or event to a
   calendar app with Add to Calendar (ADR-0001, ADR-0009, ADR-0015).
2. **One module, plain Android views.** A single `:app` module: XML layouts,
   `AppCompatActivity` (plus a `Fragment` for each home screen tab),
   `findViewById`, and `ListView` with `ArrayAdapter` for the dashboard. The
   code uses no Compose, ViewBinding, ViewModel, DI framework or coroutines,
   and doesn't take them up without the user's approval (ADR-0002).
3. **The database is the only copy.** All user data lives in the Room
   database file `vacation_database`, and there is no other copy.
   Everything opens it through `AppDatabase.open()`, and its schema changes
   only by migrations (ADR-0003).
4. **The database, not the Intent, is the truth.** A screen that shows a
   saved item reads it from the database (the vacation page on every resume,
   the event form by `eventId`) and closes itself when the item has been
   deleted. Only the vacation form prefills from Intent extras.
5. **The math is plain Kotlin.** Date, order, reminder and calendar-intent
   times live in objects that don't touch Android (`DateValidators`,
   `Dashboard`, `EventTimes`, `CalendarMonth`, `ReminderKey`,
   `ReminderTimes`, `CalendarTimes`), so JVM unit tests cover them. New date
   or reminder math goes beside them (`java.time` is available at minSdk 26).
6. **One owner per side effect.**
   - Only `ReminderScheduler` sets and cancels alarms. The detail screens
     call it on every save and delete.
   - Only `MyReceiver` posts reminders.
   - Only `CalendarInsert.kt` builds and starts Add to Calendar intents.
7. **Reminders are rebuilt, not trusted.** Alarms don't survive a reboot or a
   force stop, so every new process rebuilds every reminder from the
   database (ADR-0025).
8. **ICU for what people read, `java.time` for what is stored.** Display text
   uses `android.icu`, which knows the phone's own names and patterns.
   `java.time` formatting is for stored values (ADR-0026).

## Screens and navigation

| Screen | Class | Layout | Opened from | Title (TalkBack announces it) |
| :- | :- | :- | :- | :- |
| Home (launcher) | `HomeActivity` | `activity_home.xml` | The launcher; the bottom of every reminder's back stack | None of its own. Never give it an `android:label`: on the launcher activity that would rename the app's icon |
| Vacations tab (the dashboard) | `VacationsFragment` | `fragment_vacations.xml` | `HomeActivity`, first; the bottom bar | — |
| Settings tab | `SettingsFragment` | `fragment_settings.xml` | The bottom bar | — |
| Vacation page | `EventListActivity` | `activity_event_list.xml` | A vacation on the dashboard; a vacation reminder; the event reminder's back stack | The vacation's title |
| Vacation form | `VacationDetailActivity` | `activity_vacation_detail.xml` | Add Vacation (dashboard); Edit Vacation (vacation page) | Add Vacation or Edit Vacation |
| Event form | `EventDetailActivity` | `activity_event_detail.xml` | Add Event or an event row (vacation page); an event reminder | Add Event or Edit Event |

Each form's heading shows the same words as its title and, like every
heading, sets `accessibilityHeading`.

```
launcher ─► HomeActivity ─┬─ Vacations tab (VacationsFragment)
                          │     ├─ Add Vacation ─► VacationDetailActivity (new)
                          │     └─ a vacation ──► EventListActivity
                          │                          ├─ Edit Vacation ─► VacationDetailActivity (edit)
                          │                          ├─ Share Vacation ─► Sharesheet
                          │                          ├─ Add to Calendar ─► calendar app
                          │                          ├─ Add Event ─► EventDetailActivity (new)
                          │                          └─ an event ─► EventDetailActivity (edit)
                          │                                            ├─ Share ─► Sharesheet
                          │                                            └─ Add to Calendar ─► calendar app
                          └─ Settings tab (SettingsFragment)

vacation reminder tapped: HomeActivity ▸ EventListActivity
event reminder tapped:    HomeActivity ▸ EventListActivity ▸ EventDetailActivity
```

Rules:

- **Back from the Settings tab** returns to the Vacations tab instead of
  leaving the app (an `OnBackPressedCallback` in `HomeActivity`).
  Reselecting the open tab does nothing.
- **Recreation** (a rotation, or the dark mode switch) restores the open tab
  through the fragment manager, and the bar restores its selection; neither
  calls the bar's listener.
- **Reminders open their item** above the screens Back returns to, built
  with `TaskStackBuilder` in `MyReceiver`. The last intent in the stack
  carries the reminder's URI, which keeps each reminder's `PendingIntent`
  separate.
- **Deleted items close their screens.** The vacation page reloads its
  vacation on every resume and closes itself when it's gone. The event form
  closes itself when its event was deleted since the page or reminder that
  opened it.
- **Forms close after a save.** They disable their buttons first, so a
  second tap, or one behind an offer that keeps the form open, can't save
  twice (ADR-0033). The exact alarm offer and the hibernation offer close
  the form behind them whichever way they are closed.

`docs/subsystems/screens-and-navigation.md` has each screen's fields,
buttons and validation rules.

## Code layout

```
.
├── build.gradle.kts            # root: the buildscript classpath sets the Kotlin version; plugins applied false
├── settings.gradle.kts         # plugin and library repositories, the foojay toolchain resolver, include(":app")
├── gradle.properties           # configuration cache on, AndroidX, the official Kotlin code style
├── gradle/
│   ├── libs.versions.toml      # every library and plugin version except the foojay resolver's (settings.gradle.kts)
│   ├── gradle-daemon-jvm.properties  # asks for a JDK 25 daemon
│   └── wrapper/                # Gradle 9.6.1, checksum pinned (third-party: NOTICE)
├── gradlew, gradlew.bat        # the Gradle wrapper (third-party: NOTICE)
├── local.properties            # machine-specific sdk.dir; tracked in git, but never commit changes to it
├── app/
│   ├── build.gradle.kts        # SDK levels, application ID, signing, R8, locale filter, detekt, ktlint, dependencies
│   ├── detekt-baseline.xml     # detekt findings in the code of 2026-10-31
│   ├── schemas/com.example.vagor.database.AppDatabase/
│   │   ├── 1.json              # Room's exported schema, version 1
│   │   └── 2.json              # version 2 (2026-10-27)
│   └── src/
│       ├── main/AndroidManifest.xml
│       ├── main/java/com/example/vagor/      # all of Vagor's code (below)
│       ├── main/keepRules/rules.keep         # R8 keep rules go here; none are needed today
│       ├── main/res/                          # layouts, values, drawables, colours, raw, xml (below)
│       ├── test/java/com/example/vagor/       # JVM unit tests (docs/04-quality.md)
│       └── androidTest/java/com/example/vagor/  # instrumented tests (docs/04-quality.md)
├── fastlane/metadata/android/en-US/  # the store listing; fastlane's layout, though nothing uses fastlane
├── .github/workflows/          # ci.yml, security.yml (docs/04-quality.md)
├── Makefile.txt                # git tag helpers; agents never run its tag, push-tag or release targets
├── LICENSE                     # the Vagor Perpetual End-User License Agreement (ADR-0034)
├── NOTICE                      # the third-party software that ships in the app
├── PRIVACY.md                  # the privacy policy; the app holds a second copy in strings.xml
├── CODEOWNERS
├── README.md, CONTRIBUTING.md, SECURITY.md
├── AGENTS.md                   # the project rules for every coding agent
├── CLAUDE.md                   # imports AGENTS.md; Claude Code specifics
├── .claude/                    # settings.json, rules/ (path-scoped rules), skills/ (project skills)
├── .gitignore                  # build output, signing keys, bundles and APKs stay out of git
├── .idea/                      # Android Studio settings, some machine-specific (docs/05-release.md, step 9)
└── docs/                       # this documentation
```

The Kotlin package and `namespace` are `com.example.vagor`; the application
ID (the Play and device identity) is `io.github.jabbott_iii.vagor`
(ADR-0020). Builds from before the application ID changed (they used
`com.example.vagor`) are a separate app on the phone, and their data doesn't
carry over. Every source file is in the `com.example.vagor` package, except
those in `entities/`, `dao/` and `database/`, which are in its subpackages
of the same names.

### Screens

| File | Declares | Purpose |
| :- | :- | :- |
| `HomeActivity.kt` | `HomeActivity` | The launcher: the bottom navigation bar and its two tabs. Calls `ReminderScheduler.rebuildIfNeeded` on every resume, which rebuilds only if exact alarms were allowed or taken away since |
| `VacationsFragment.kt` | `VacationsFragment` | The dashboard: every vacation in `Dashboard.rows` order, the Add Vacation button, and the once-per-launch notifications check when there is a trip |
| `SettingsFragment.kt` | `SettingsFragment` | The Settings tab (`docs/subsystems/settings-screen.md`) |
| `EventListActivity.kt` | `EventListActivity` | A vacation's page: its details, Edit Vacation, Share Vacation and Add to Calendar buttons, and its events as plain rows in a `ScrollView`, in `EventTimes.ORDER` |
| `VacationDetailActivity.kt` | `VacationDetailActivity` | The add/edit vacation form, its checks, Save and Delete |
| `EventDetailActivity.kt` | `EventDetailActivity` | The add/edit event form: the clock, the time zone list, Save, Delete, Share and Add to Calendar |

### Dialogs and offers

Every dialog is a `DialogFragment`, so it's still open after a rotation
(ADR-0033).

| File | Declares | Purpose |
| :- | :- | :- |
| `MessageDialog.kt` | `MessageDialog` | A message with an OK button, an optional title, and optional `res/raw` text after it |
| `NotificationsOff.kt` | `NotificationsOffDialog`, `Context.openNotificationSettings()` | The notifications-off dialog, and the way to Android's notification settings for Vagor or its reminder channel |
| `ExactAlarmOffer.kt` | `ExactAlarmOfferDialog`, `Context.exactAlarmSettingsIntent()`, `Context.openExactAlarmSettings()` | Offers Android's Alarms & reminders screen after saving an event whose reminder can't be exact |
| `Hibernation.kt` | `HibernationOfferDialog`, `Context.mayHibernate()`, `Context.openAppInfo()` | Whether Android may pause an unused Vagor, the way to its App info, and the offer after saving a trip far ahead |
| `TimeZonePicker.kt` | `TimeZonePickerDialog`, `TimeZoneChoices`, `TimeZoneChoice`, `FullHeightLinearLayout` | The event form's searchable time zone list, labelled from Android's ICU time zone names |

### Fields and pickers

| File | Declares | Purpose |
| :- | :- | :- |
| `PickOnlyField.kt` | `EditText.makePickOnly()` | A field filled only from a picker and never typed into; screen readers see a drop-down list |
| `DateField.kt` | `DateField` | A date field: `date` holds the stored date, the text shows it the phone's way. Keeps its date across rotation itself |
| `CalendarDropdown.kt` | `DateField.attachCalendarDropdown()` | The date fields' month calendar, which drops down below the field |
| `CalendarDayBackground.kt` | `calendarDayBackground()`, `DayCircle` | The calendar days' circles and rings, drawn to fit their cells |

`docs/subsystems/dates-times-and-pickers.md` covers these.

### Plain Kotlin, unit tested

| File | Declares | Purpose | JVM test |
| :- | :- | :- | :- |
| `DateValidators.kt` | `DateValidators` | The only parser and formatter of stored dates, times and zones, and the date checks | `DateValidatorsTest` |
| `Dashboard.kt` | `Dashboard`, `TripStatus`, `DashboardRow` | Dashboard order and each trip's status | `DashboardTest` |
| `EventTimes.kt` | `EventTimes` | Event start times, the default 9:00 AM, and event order | `EventTimesTest` |
| `CalendarMonth.kt` | `CalendarMonth` | The month grid for the drop-down calendar | `CalendarMonthTest` |
| `ReminderSchedule.kt` | `ReminderType`, `ReminderKey`, `ReminderTimes` | Reminder identities, times, and timeouts | `ReminderScheduleTest` |
| `CalendarInsert.kt` | `CalendarTimes` | The times in Add to Calendar intents | `CalendarTimesTest` |
| `LicenseText.kt` | `reflowParagraphs()` | Reflows the license text for the Third-Party Licenses dialog | `ThirdPartyLicensesTest` |

### Text, reminders and the rest

| File | Declares | Purpose |
| :- | :- | :- |
| `TripText.kt` | `dateText()`, `vacationDateRange()`, `tripStatusText()`, `timeOfDayText()`, `eventTimeText()`, `eventWhenText()`, `displayLocale()` | Dates, times and status as display text. Everything a person reads goes through `dateText` |
| `CalendarInsert.kt` | `vacationCalendarIntent()`, `eventCalendarIntent()`, `openCalendarInsert()` | The Add to Calendar intents (`docs/subsystems/sharing-and-calendar.md`) |
| `ReminderScheduler.kt` | `ReminderScheduler` | Sets and cancels alarms, creates the notification channel, rebuilds every reminder (`docs/subsystems/reminders.md`) |
| `MyReceiver.kt` | `MyReceiver` | The alarm receiver: posts the notification |
| `ReminderRescheduleReceiver.kt` | `ReminderRescheduleReceiver` | Rebuilds reminders after a boot, an app update, a clock or time zone change, or exact alarms being allowed |
| `VagorApplication.kt` | `VagorApplication` | Applies the saved theme before any screen, and rebuilds the reminders in each new process |
| `ThemeSettings.kt` | `ThemeSettings` | The dark mode switch's stored value and its night mode |
| `EdgeToEdge.kt` | `setEdgeToEdgeContentView()`, `setEdgeToEdgeContentViewWithBottomBar()` | Content views that keep clear of the system bars and the keyboard |

### Data

| File | Declares | Purpose |
| :- | :- | :- |
| `entities/Vacation.kt` | `Vacation` | The `vacations` table |
| `entities/Event.kt` | `Event` | The `excursions` table |
| `dao/VacationDAO.kt` | `VacationDAO` | Synchronous insert, update, delete and queries for vacations |
| `dao/EventDAO.kt` | `EventDAO` | The same for events, plus events by vacation and their count |
| `database/AppDatabase.kt` | `AppDatabase` | The Room database: `NAME`, `MIGRATION_1_2`, `MIGRATIONS`, `open()` |

`docs/subsystems/data-and-storage.md` is the full spec.

### Resources

| Path | Holds |
| :- | :- |
| `res/layout/` | The screens (`activity_*.xml`, `fragment_*.xml`), list rows (`item_*.xml`), the time zone dialog (`dialog_time_zone.xml`) and the calendar popup (`popup_calendar.xml`) |
| `res/values/strings.xml` | All user-visible text, including `privacy_policy_text` and `third_party_licenses_text`. English only (ADR-0036) |
| `res/values/colors.xml`, `values/themes.xml`, `values-night/themes.xml` | The blue colour scheme in light and dark. Colours come from theme attributes only (ADR-0017) |
| `res/values/styles.xml` | `Widget.Vagor.Button`, `Widget.Vagor.Button.Outlined`, `Widget.Vagor.AddButton` and the bottom bar's styles (ADR-0016) |
| `res/values/dimens.xml`, `values-v28/dimens.xml` | Sizes, including `button_height` and the calendar's; ripple opacities before and from Android 9 |
| `res/color/`, `res/drawable/` | State colours, focus rings, icons, and the launcher icon's layers |
| `res/mipmap-anydpi/` | The adaptive launcher icon; minSdk 26 always uses it, so there are no density PNGs |
| `res/menu/bottom_navigation.xml` | The Vacations and Settings tabs |
| `res/raw/apache_license_2_0.txt` | The Apache License 2.0, shown under Third-Party Licenses (ADR-0029) |
| `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml` | Keep trip data out of cloud backup (ADR-0004) |

`docs/subsystems/theme-and-accessibility.md` covers the theme, sizes and
the icon.

## Data model

### Tables

The `Event` entity maps to the table `excursions`: events were called
excursions until 2026-10-25, and the table keeps its old name, so renaming
the class needed no migration (ADR-0010). Likewise `Vacation.details`
(trip details) maps to the old column `hotel` (ADR-0012). Don't rename
either without a migration.

`vacations`:

| Column | Property | Type | Notes |
| :- | :- | :- | :- |
| `id` | `id` | INTEGER, primary key, auto-generated | |
| `title` | `title` | TEXT, not null | Required |
| `hotel` | `details` | TEXT, not null | Trip details; optional, blank when empty |
| `startDate` | `startDate` | TEXT, not null | `MM/dd/yyyy` |
| `endDate` | `endDate` | TEXT, not null | `MM/dd/yyyy`, on or after the start |

`excursions`:

| Column | Property | Type | Notes |
| :- | :- | :- | :- |
| `id` | `id` | INTEGER, primary key, auto-generated | |
| `title` | `title` | TEXT, not null | Required |
| `date` | `date` | TEXT, not null | `MM/dd/yyyy`, inside its vacation's dates |
| `vacationId` | `vacationId` | INTEGER, not null | No foreign key (see below) |
| `details` | `details` | TEXT, not null, default `''` | Optional; blank when empty. Version 2 |
| `time` | `time` | TEXT, nullable | 24-hour `HH:mm`. Null on events saved before version 2; every save since stores it. Version 2 |
| `timeZone` | `timeZone` | TEXT, nullable | An IANA zone ID such as `Asia/Tokyo`, stored whenever the time is. Version 2 |

`excursions.vacationId` has no foreign key. So the two checks that keep
events inside their vacation, that a vacation with events can't be deleted
and that an edited vacation's new dates must still hold its events
(ADR-0028), exist only in `VacationDetailActivity`.

### Versions

| Version | Date | Change | Migration |
| :- | :- | :- | :- |
| 1 | Not recorded | `vacations` (`title`, `hotel`, `startDate`, `endDate`, `id`) and `excursions` (`title`, `date`, `vacationId`, `id`) | — |
| 2 | 2026-10-27 | `excursions` gets `details`, `time` and `timeZone` (ADR-0013) | `MIGRATION_1_2`: existing events get blank details and no time |

Room exports each version's schema to `app/schemas`, which is in git, and
`MigrationInstrumentedTest` tests each upgrade with `MigrationTestHelper`
against the version it upgrades from. `/change-database-schema` walks
through a schema change.

### Stored formats

- **Dates** are `MM/dd/yyyy` strings: exactly two-digit month and day, a
  four-digit year, a real calendar date, nothing before or after. Rows saved
  before dates were checked strictly may hold looser ones.
- **Times** are 24-hour `HH:mm` strings.
- **Time zones** are IANA zone IDs.
- They are parsed and formatted only through `DateValidators`
  (`parseLocalDate`, `formatDate`, `parseLocalTime`, `formatTime`,
  `parseZone`).

### Other state

| State | Where | Lifetime |
| :- | :- | :- |
| The dark mode switch | SharedPreferences file `settings`, key `dark_mode`, default `true` (ADR-0006) | Until the app's data is cleared. The only stored state outside the database |
| Whether the notifications check, the exact alarm offer and the hibernation offer have run | `VacationsFragment.notificationsChecked`, `ExactAlarmOfferDialog.offered`, `HibernationOfferDialog.offered` | In memory, so once per process ("once per launch") |
| Whether exact alarms were allowed at the last rebuild | `ReminderScheduler.builtWithExact` | In memory; null until the process has rebuilt |
| When a calendar app was last opened | `calendarOpenedAt` in `CalendarInsert.kt` | In memory; a second tap within 2 seconds is ignored |
| Alarms and posted reminders | `AlarmManager`, the notification shade | Derived from the database and rebuilt from it |

### Backup

Cloud backup is off, decided with the user on 2026-10-25 (ADR-0004):

- `android:allowBackup="false"` in the manifest.
- `backup_rules.xml` (Android 11 and earlier) excludes every domain (`root`,
  `file`, `database`, `sharedpref`, `external`), in case that flag is ever
  changed.
- `data_extraction_rules.xml` (Android 12 and later) excludes the same
  domains from `<cloud-backup>` only, so a direct device-to-device transfer
  to a new phone can still copy them.

## Data flow

```
VacationDetailActivity ── Save ──► VacationDAO.insert / update ──► ReminderScheduler.scheduleVacation
                       └─ Delete ► VacationDAO.delete ──────────► ReminderScheduler.cancelVacation
EventDetailActivity ──── Save ──► EventDAO.insert / update ──────► ReminderScheduler.eventSaved
                       └─ Delete ► EventDAO.delete ──────────────► ReminderScheduler.cancelEvent

VagorApplication.onCreate ─────────┐
HomeActivity.onResume ─────────────┼─► ReminderScheduler.rebuildIfNeeded ─► rescheduleAll (every row)
ReminderRescheduleReceiver ────────┘                                          (TIME_SET and TIMEZONE_CHANGED
                                                                               call rescheduleAll directly)
ReminderScheduler.setAlarm ─► AlarmManager (PendingIntent to MyReceiver, data = the ReminderKey URI)
                                   │ fires
                                   ▼
                              MyReceiver ─► reads the item from the database ─► NotificationManagerCompat.notify
                                                                                (tag = the same URI)
                                   │ tapped
                                   ▼
                              TaskStackBuilder ─► HomeActivity ▸ EventListActivity [▸ EventDetailActivity]

EventListActivity / EventDetailActivity ─► ACTION_SEND in createChooser ─► the app the user picks
                                       └─► openCalendarInsert ─► Google Calendar, or any calendar app
```

- **A reminder's identity** is a data URI from `ReminderKey`, such as
  `vagor://reminder/vacation/<id>/start`. A `PendingIntent`'s identity is
  its request code plus the Intent's action, data, type, component and
  categories; extras don't count. The notification tag is the same URI, so
  a reminder can be dismissed by its item.
- **`MyReceiver` reads the item when the alarm fires,** and posts nothing if
  it was deleted since. Alarms left by older versions have no reminder URI
  and are ignored.

### Intent extras

Screens pass data through Intent extras with literal keys. The
instrumented tests and the reminder notifications use the same keys. A
missing `vacationId` (in the vacation form) or `eventId` (in the event
form) reads as `-1`, which means a new item. The vacation page closes
itself without a `vacationId`, and the event form can't save without one.

| Key | Type | Set by | Read by |
| :- | :- | :- | :- |
| `vacationId` | Int | `VacationsFragment`, `EventListActivity` (Edit Vacation, Add Event, an event row), `MyReceiver` | `EventListActivity`, `VacationDetailActivity`, `EventDetailActivity` |
| `title`, `details`, `startDate`, `endDate` | String | `EventListActivity` (Edit Vacation) | `VacationDetailActivity`, which prefills from them |
| `eventId` | Int | `EventListActivity` (an event row), `MyReceiver` | `EventDetailActivity` |

- **The event form takes only `eventId`** (none for a new event) **and
  `vacationId`,** and reads the event from the database. A reminder's extras
  are copied when it's posted, and Save would write those old values back.
- **The vacation page takes only `vacationId`** and reloads the vacation from
  the database on every resume.

### State kept across rotation

| Screen or view | Kept in | What |
| :- | :- | :- |
| `DateField` | Its own saved state (`date`) | The stored date, shown again in the current language |
| `EventDetailActivity` | `pickedTime`, `pickedTimeZone` | The picked time and zone, which their fields only show (the fields' own saving is off) |
| `EventListActivity` | `focusedEventId` | The event row that had keyboard focus |
| `TimeZonePickerDialog` | The fragment result `timeZonePicker` (`zoneId`) | The picked zone, which survives the screen being recreated |
| `MaterialTimePicker` | Fragment tag `eventTimePicker` | An open clock gets its listener back |

## Database access and threading

- Every Activity, Fragment or receiver that reads or writes data opens its
  own Room instance with `AppDatabase.open()` and closes it again: screens
  in `onDestroy`, receivers (and `ReminderScheduler.rebuildIfNeeded`) in
  `finally`.
- `AppDatabase.open()` adds every migration in `AppDatabase.MIGRATIONS` and
  allows main-thread queries. A `Room.databaseBuilder` without the
  migrations would crash on an older database.
- DAOs are synchronous. Every query runs on the thread that calls it, which
  is the main thread: Vagor's code starts no threads of its own.
- Follow that pattern in small changes. A shared instance or background
  threading is a refactor: propose it to the user first (`docs/roadmap.md`,
  M7).

## Edge to edge and insets

From Android 15, screens draw behind the status and navigation bars, with no
opt-out from targetSdk 36 (`docs/03-platforms.md`).

- **Every screen except `HomeActivity` calls `setEdgeToEdgeContentView()`**
  instead of `setContentView()`. It pads the layout's root for the system
  bars and any display cutout, on top of its own padding.
  - While the keyboard is open, it also ends the root view at the
    keyboard's top edge.
  - The two detail forms are in a `ScrollView` with `adjustResize`, so every
    field and button can be scrolled into view in landscape and with the
    keyboard open. A full-screen landscape keyboard still covers the form
    until it is closed.
  - The insets are passed on rather than consumed: on Android 10 and lower,
    consuming them would stop them reaching sibling views.
- **`HomeActivity` calls `setEdgeToEdgeContentViewWithBottomBar()`.**
  - It pads the tab content for the status bar and the sides, and pads the
    bottom bar for the navigation bar and the sides, so the bar's background
    reaches behind the navigation bar with no scrim.
  - It replaces the bar's own inset handling, which would add the keyboard's
    height.
  - It does nothing for the keyboard: a text field added to a home tab would
    need its own keyboard handling.
  - The navigation bar style is a fixed light or dark one matching the
    theme, not auto, which would turn the system's contrast scrim back on
    whenever the window's configuration changes.
- System bar icons follow the app's light or dark theme.

## Errors and messages

- **Toasts** carry short messages: a form that's missing a required field,
  a date that can't be read, an end date before the start date, an event
  date outside its vacation, an event whose vacation no longer exists, an
  edited vacation whose new dates would leave events outside it, a vacation
  that still has events, and the saved, updated and deleted confirmations.
- **Dialogs** carry anything longer, or anything that offers a way out,
  because Android cuts a toast at two lines: the notifications-off dialog,
  "no calendar app", and the exact alarm and hibernation offers. The privacy
  policy and the third-party licenses are `MessageDialog`s too.
  - Every dialog is a `DialogFragment`. A dialog shown straight from
    `MaterialAlertDialogBuilder` closes when the screen is recreated (fixed
    2026-11-01, ADR-0033).
  - Each `show` does nothing while the fragment manager's state is saved or
    the same dialog is already open, so a second tap, or a screen reader's,
    doesn't open a second one.
- **Missing items are not errors.** A screen whose item was deleted closes
  itself, and `MyReceiver` posts nothing for a deleted item.
- **Unreadable stored values are shown, not hidden.** `dateText` shows a date
  it can't read as it was stored, and the dashboard lists vacations whose
  dates can't be read after the current and upcoming ones, with their own
  status text.
- **Platform calls that can fail are caught where they're made.**
  - `ActivityNotFoundException` when opening Android's settings screens or a
    calendar app: the next intent is tried.
  - `SecurityException` and `IllegalStateException` when setting an alarm
    (`ReminderScheduler.setAlarm`, ADR-0024).
  - Parse failures in `DateValidators`, which return null.
- **No crash reporting** of its own, by design. Android vitals in Play
  Console is the only signal from the field (`docs/05-release.md`, step 8).
