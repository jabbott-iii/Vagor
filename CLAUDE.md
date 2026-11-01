# Vagor

<!--
Maintainers: written 2026-10-25 against commit 2092255. The "Current:" notes and the
"Known issues" section are point-in-time; update or delete entries as they get fixed.
Block-level HTML comments like this one are stripped before Claude reads the file.
-->

Vagor is an offline Android vacation planner written in Kotlin: a single `:app` module. Its
application ID (the Play and device identity) is `io.github.jabbott_iii.vagor`; the Kotlin package
and `namespace` are still `com.example.vagor`. It runs entirely on the phone, with no backend and no
accounts. It's proprietary, all rights reserved by Joseph Anthony Abbott III (`LICENSE`; made
proprietary on 2026-10-31 at the user's request, Apache 2.0 before), and `NOTICE` lists the
third-party software that ships in the app.

## Product requirements

These define "done" for feature work. Events were called excursions until 2026-10-25; the UI and
code now say **events**, but the Room table keeps its old name `excursions` (see Code layout).

1. **Vacations**: create, edit and delete, with title, trip details, start date and end date.
   Trip details replaced the hotel field on 2026-10-26 (asked for by the user): free text over
   several lines, and optional.
   Current: implemented.
2. **Events**: each one is planned on a date inside its vacation's date range, at a time in a
   time zone, with optional details (time, time zone and details asked for by the user on
   2026-10-26). The time is required and picked from a clock; the time zone is picked from a
   searchable list and starts on the phone's zone. Events saved before 2026-10-27 have no time
   until they're next edited, and can't be saved again without one.
   Current: implemented.
3. **Reminders**: system notifications for vacation start, vacation end and each event.
   Current: implemented. Automatic for every item: a notification around 9:00 AM local time on
   each vacation's start and end dates, and an hour before each event (asked for by the user on
   2026-10-26). A posted reminder leaves the shade once it's out of date (asked for on
   2026-10-31). See "Reminders" below.
4. **Sharing**: vacations and events can be shared with other people.
   Current: implemented as plain text through the Sharesheet. The vacation text lists its
   events with their dates and times, and each event has its own Share button, whose text adds
   its time zone and details. Importing a shared trip isn't supported. Adding to Google Calendar
   was asked for by the user on 2026-10-26: an Add to Calendar button on the vacation page and the
   event form opens Google Calendar's own new-event screen (another calendar app's when Google
   Calendar isn't installed) filled in: a vacation all day over its dates, with its trip details
   and events as the description; an event at its time in its time zone, an hour long, with its
   details, time and vacation (`CalendarInsert.kt`). The label says "Calendar" because it may open
   another app, and Google's brand guidance asks for accurate wording.
5. **Local-only**: see the next section.
6. **Dark mode**: a switch on the Settings tab, on by default (asked for by the user on
   2026-10-25 as an on/off switch, not a follow-system option).
   Current: implemented (`ThemeSettings`, `VagorApplication`, `SettingsFragment`).
7. **Accessible colours**: a blue Material 3 scheme for colour-blind users, from the Okabe-Ito blue
   `#0072B2`. Every text/background pair must meet WCAG AA (4.5:1; 3:1 for non-text UI) in both
   themes; `ThemeContrastTest` enforces this. Don't convey anything by colour alone, and don't
   add DynamicColors (it would replace the blue with wallpaper colours). Keyboard focus must be
   visible: the theme turns on Material's focus rings (`focusRingsEnabled`, not yet public API),
   the dashboard and time zone lists have their own focus rings, and the calendar's days have
   one. Material's time picker marks its selected AM/PM by hue alone, so
   `ThemeOverlay.Vagor.MaterialTimePicker` fills it with the primary blue instead (the selected
   hour or minute keeps Material's colours: the dial's numbers also show which is being set).
8. **Google Play compliance**: see [PLAY_RELEASE.md](PLAY_RELEASE.md). Play Console steps are the
   developer's to do.
9. **Dashboard and navigation** (asked for by the user on 2026-10-25): the app opens on a
   dashboard of every vacation with its dates. Current and upcoming vacations come first, soonest
   at the top; vacations that have ended go below them under a "Past" heading, most recent first
   (`Dashboard.rows`). A blue Add Vacation button sits on the dashboard. A bottom navigation bar
   switches between the Vacations and Settings tabs. Tapping a vacation opens its page with its
   events.
   Current: implemented (`HomeActivity`, `VacationsFragment`, `EventListActivity`).
10. **Calendar date entry** (asked for by the user on 2026-10-25): every date field opens a month
   calendar that drops down below the field, and dates can't be typed (`CalendarDropdown.kt`).
   The end date calendar starts at the start date; an event's calendar offers only its
   vacation's days.
   Current: implemented. The app draws the calendar itself. Don't switch to the framework
   `CalendarView`: it follows the phone's calendar system (a Thai phone would store Buddhist-era
   years) and marks today by colour alone. The calendar is always Gregorian and marks today
   with a ring. An event's time and time zone fields can't be typed into either: the time comes
   from Material's clock (`MaterialTimePicker`, in the phone's 12- or 24-hour style) and the
   zone from `TimeZonePickerDialog`. All three use `makePickOnly()` (`PickOnlyField.kt`).

## Local-only rules (never break these)

- No network access of any kind. Don't add `INTERNET` or `ACCESS_NETWORK_STATE`, HTTP clients,
  remote WebView content, Firebase, analytics, crash reporting, ads, accounts or sync. The
  manifest declares only `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` and
  `SCHEDULE_EXACT_ALARM` (see Reminders). After adding any
  dependency, check the merged manifest for permissions it pulls in. Play's Data safety answer
  ("no data collected or shared") and PRIVACY.md depend on this.
- Data leaves the app only when the user shares it through the Android Sharesheet, or hands one
  vacation or event to a calendar app with Add to Calendar (see Sharing). Cloud backup
  is off, decided with the user on 2026-10-25 (`allowBackup="false"` plus exclusions in
  `backup_rules.xml` and `data_extraction_rules.xml`); a direct device-to-device transfer to a new
  phone is still allowed.
- The privacy policy exists twice: PRIVACY.md and `privacy_policy_text` in strings.xml. Change
  both together, and keep them true to what the app does. Its privacy contact is
  jabbottpublicsupport@gmail.com, chosen by the user on 2026-10-31 when GitHub Issues turned out to
  be off; the Play listing's contact email is the same address.
- All user data lives in the Room database file `vacation_database`, and there is no other copy.
  Never rename that file, never use `fallbackToDestructiveMigration`, and never clear tables
  outside tests. For a schema change, bump `AppDatabase.version`, add a `Migration` to
  `AppDatabase.MIGRATIONS`, keep the schema JSON Room exports to `app/schemas` (it's in git), and
  test the upgrade in `MigrationInstrumentedTest` with `MigrationTestHelper`. Version 2
  (2026-10-27, `MIGRATION_1_2`) added the event columns `details`, `time` and `timeZone`.
  Everything that opens the database does so through `AppDatabase.open()`, which adds every
  migration; a `Room.databaseBuilder` without them would crash on an older database. The only
  other stored state is the dark mode switch, in the SharedPreferences file `settings`.

## Code layout and patterns

- Screens in `app/src/main/java/com/example/vagor/`: `HomeActivity` (launcher) holds a bottom
  navigation bar and two tabs, `VacationsFragment` (the dashboard) and `SettingsFragment`. Tapping
  a vacation opens `EventListActivity`, the vacation's page: its details, Edit Vacation, Share
  Vacation and Add to Calendar buttons, and its events. `VacationDetailActivity` is the add/edit
  vacation form, and
  `EventDetailActivity` the add/edit event form. Also there: `entities/` (`Vacation`, `Event`),
  `dao/`, `database/AppDatabase.kt`, `DateValidators.kt`, `Dashboard.kt` (plain-Kotlin dashboard
  order and trip status), `TripText.kt` (dates, times and status as display text),
  `EventTimes.kt` (plain-Kotlin event start times and order), `PickOnlyField.kt` (fields filled
  only from a picker), `DateField.kt` (a date field: the stored date, shown the phone's way),
  `CalendarDropdown.kt` (the date fields' drop-down calendar),
  `CalendarMonth.kt` (plain-Kotlin month grid for the calendar), `CalendarDayBackground.kt` (the
  calendar days' circles and rings, drawn to fit their cells), `TimeZonePicker.kt` (the event
  form's searchable time zone list, labelled from Android's ICU time zone names),
  `CalendarInsert.kt` (the Add to Calendar intents, with their plain-Kotlin times in
  `CalendarTimes`), `ReminderSchedule.kt` (plain-Kotlin reminder times and
  identities), `ReminderScheduler.kt` (sets and cancels alarms), `ExactAlarmOffer.kt` (offers
  Android's Alarms & reminders screen), `NotificationsOff.kt` (the notifications-off dialog and
  the way to Android's notification settings), `LicenseText.kt` (reflows the license text for
  the Third-Party Licenses dialog), `MyReceiver.kt` (the alarm
  receiver; posts the notification), `ReminderRescheduleReceiver.kt`, `ThemeSettings.kt`,
  `VagorApplication.kt` (applies the saved theme before any screen and rebuilds the reminders in
  each new process) and `EdgeToEdge.kt`.
- The `Event` entity maps to the table `excursions` (`@Entity(tableName = "excursions")`), and
  `EventDAO` queries that table. Don't rename the table without a migration. Likewise
  `Vacation.details` (trip details) maps to the old column `hotel` (`@ColumnInfo(name = "hotel")`).
  `Event.details` is blank when empty; `Event.time` and `Event.timeZone` are null on events saved
  before database version 2, and every save since stores both.
- Every screen except `HomeActivity` calls `setEdgeToEdgeContentView()` instead of
  `setContentView()`, so content stays clear of the system bars (required at targetSdk 36).
  While the keyboard is open, that helper also ends the root view at the keyboard's top edge. The
  two detail forms are in a `ScrollView` with `adjustResize`, so every field and button can be
  scrolled into view in landscape and with the keyboard open (a full-screen landscape keyboard
  still covers the form until it is closed).
- `HomeActivity` uses `setEdgeToEdgeContentViewWithBottomBar()` instead. It pads the tab content
  for the status bar and the sides, and pads the bottom bar for the navigation bar and the
  sides, so the bar's background reaches behind the navigation bar with no scrim. It replaces
  the bar's own inset handling, which would add the keyboard's height, and it does nothing for
  the keyboard: a text field added to a home tab would need its own keyboard handling.
- Colours come from theme attributes only (`res/values/colors.xml`, `values/themes.xml`,
  `values-night/themes.xml`). Never hard-code a text or background colour in a layout.
- The launcher icon (made 2026-10-31, replacing the template robot) is a white rolling suitcase
  (`drawable/ic_launcher_foreground.xml`) on the Okabe-Ito blue `#0072B2`
  (`ic_launcher_background.xml`), inside the adaptive icon's 66dp safe circle, so the Android 12+
  splash screen shows it whole. The foreground is also the monochrome layer for themed icons, so
  keep it one flat colour with no shadow. minSdk 26 always uses `mipmap-anydpi`, so there are no
  density PNGs. The Play icon and feature graphic (`fastlane/metadata/android/en-US/images/`)
  are drawn from the same paths: redraw them when the icon changes.
- Every button on a page is the same size (asked for by the user on 2026-10-26): full width,
  one above the other, styled `Widget.Vagor.Button` or `Widget.Vagor.Button.Outlined` with
  `layout_width="match_parent"` and `layout_height="wrap_content"`. The style sets one height
  (`@dimen/button_height`), which grows with the font size. Only the floating Add Vacation and Add
  Event buttons differ (`Widget.Vagor.AddButton`).
- Every screen but `HomeActivity` sets its own title, which TalkBack announces when the screen
  opens: Add Vacation or Edit Vacation, Add Event or Edit Event, and the vacation's title on its
  page. Each form's heading shows the same words and, like every heading, sets
  `accessibilityHeading`. Never give `HomeActivity` an `android:label`: on the launcher activity it
  would rename the app's icon.
- Text fields are `wrap_content` with `minHeight="57dp"`, never a fixed height, so they grow with
  the font size, and set `importantForAutofill="no"`, so autofill services leave trip text alone.
  Touch targets are at least 48dp each way. The calendar's days are at least 48dp tall at any font
  size, and the calendar is wider than its field where the field is too narrow for seven 48dp
  days (a 360dp-wide phone), up to the screen's width less 8dp on each side; on narrower screens
  the days are as wide as that allows.
- UI is XML layouts, `AppCompatActivity` (plus a `Fragment` for each home screen tab),
  `findViewById`, and `ListView` with `ArrayAdapter` for the dashboard. The vacation page shows
  its few events as plain rows in a `ScrollView`, so focus follows the screen. The code uses no Compose, ViewBinding,
  ViewModel, DI framework or coroutines. Don't introduce them without the user's approval.
- Every Activity, Fragment or receiver that reads or writes data opens its own Room instance with
  `AppDatabase.open()` (main-thread queries allowed) and closes it again (screens in `onDestroy`,
  receivers in `finally`), and DAOs are synchronous. Follow that
  pattern in small changes. A shared instance or background threading is a refactor: propose it
  first.
- Screens pass data through Intent extras with literal keys (`vacationId`, `title`, `details`,
  `startDate`, `endDate`, `eventId`). The instrumented tests and the reminder notifications use
  the same keys. The vacation form prefills from the extras. The event form takes only `eventId`
  (none for a new event) and `vacationId`, and reads the event from the database, because a
  reminder's extras are copied when it's posted and Save would write old values back; it closes
  itself if the event was deleted. The vacation page (`EventListActivity`) takes only
  `vacationId` and reloads the vacation from the database on every resume, closing itself if the
  vacation was deleted.
- User-visible text goes in `res/values/strings.xml`. The app is English only, and
  `androidResources.localeFilters` keeps the libraries' translations out, so their text (the time
  picker's, for example) stays English too. Adding a language means adding it there.
- Dates are stored as `MM/dd/yyyy` strings. Parse and validate them only through
  `DateValidators`, which is strict: exactly two-digit month and day, four-digit year, a real
  calendar date, nothing before or after. Date fields are filled only from the drop-down calendar
  (`attachCalendarDropdown()` in `CalendarDropdown.kt`), which writes through
  `DateValidators.formatDate`; the save-time checks still run, because saved rows may hold older
  loose dates. Put new date or reminder math in plain Kotlin beside it so it can be unit tested
  (`java.time` is available at minSdk 26).
- Dates are shown the way the phone writes them (asked for by the user on 2026-10-31): "Jul 6,
  2031" in the US, "6 Jul 2031" in the UK. `dateText` in `TripText.kt` formats a stored date with
  ICU's `yMMMd` skeleton, always in the Gregorian calendar (a Thai phone would otherwise show the
  Buddhist-era year), and shows a date it can't read as it was stored. Everything a person reads
  goes through it: screens, reminders, shared text and calendar descriptions. Never show a stored
  date string directly. The date fields are `DateField`s: `date` holds the stored value, and the
  text is only for show, so read and write `date`, never the text. A `DateField` keeps its date
  across rotation itself. Instrumented tests set it with `setDate` and check it with `withDate`
  (`DateFieldTesting.kt`).
- Event times are stored as 24-hour `HH:mm` strings and time zones as IANA zone IDs such as
  `Asia/Tokyo`; parse them only through `DateValidators.parseLocalTime` and `parseZone`. The
  event form keeps the picked time and zone in fields of its own (saved across rotation) and
  shows them as text: the time in the phone's 12- or 24-hour style, the zone by name and city
  ("Japan Standard Time (Tokyo)", so it isn't read as the event's place). Display text uses ICU
  (`android.icu`), which knows the phone's own names and patterns; `java.time` formatting is for
  stored values. Check each ICU call's API level against minSdk 26: some, such as
  `SystemTimeZoneType.CANONICAL_LOCATION`, arrived later.
- Rules enforced today: every field except trip details and event details is required (an
  event's time zone always has a value); vacation end date is on or after the start date;
  an event's date falls within its vacation's start and end dates, inclusive, and an edited
  vacation's new dates must still hold its events (`DateValidators.isOutsideVacation`; an event
  with an old loose date can't be checked and doesn't block the save, added 2026-10-31); and a
  vacation that has events can't be deleted. The last two checks exist only in
  `VacationDetailActivity`, because `excursions.vacationId` has no foreign key.

## Build and test

The build uses AGP 9.4.1 with built-in Kotlin (Kotlin 2.4.20, set through the root
`buildscript` classpath), KSP 2.3.12 for Room, Gradle 9.6.1, compileSdk 37 and targetSdk 36 (the
Google Play minimum since 2026-08-31).
Gradle runs on JDK 17 or later, and `gradle/gradle-daemon-jvm.properties` asks for a JDK 25
daemon. It needs an Android SDK with platform 37, found through `ANDROID_HOME` or `sdk.dir` in
`local.properties`.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest                  # JVM tests in app/src/test
./gradlew testDebugUnitTest --tests 'com.example.vagor.DateValidatorsTest'
./gradlew lint
./gradlew detekt ktlintCheck                 # Kotlin static analysis and style
./gradlew ktlintFormat                       # fixes most ktlint findings
./gradlew connectedDebugAndroidTest          # Espresso tests; needs an emulator or device
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.vagor.HomeFlowInstrumentedTest
```

- IMPORTANT: `HomeFlowInstrumentedTest`, `ReminderInstrumentedTest`,
  `CalendarDropdownInstrumentedTest`, `EventFormInstrumentedTest` and
  `CalendarInsertInstrumentedTest` write to the app's real `vacation_database`; all but the two
  calendar tests also set real alarms, and `ReminderInstrumentedTest` posts real notifications.
  All five delete only the rows they create, in `@After`, so a run that is killed partway can
  leave a test trip behind. `CalendarInsertInstrumentedTest` catches the calendar intents with an
  `Instrumentation.ActivityMonitor`, so no calendar app opens.
  `MigrationInstrumentedTest` upgrades a database file of its own (`migration-test`) and deletes
  it. `ReminderLimitInstrumentedTest` sets 520 real event alarms, past Android's limit of 500, for
  events that aren't in the database, and cancels them afterwards; a run killed partway leaves
  them set until they fire 30 days later or the app is uninstalled.
  Every instrumented run starts the app's process, which re-arms every stored trip's reminders
  as any start does (`VagorApplication`). `SettingsInstrumentedTest` opens the real home screen,
  and it flips the dark mode setting and restores it. `CalendarDropdownInstrumentedTest` switches the app's language to Thai, US English
  and British English in two tests and back. The home flow, reminder, settings and event form tests grant the notification
  permission and leave it granted. `SettingsInstrumentedTest` opens Android's Alarms & reminders
  screen and leaves it with Back. The event form's tests of the exact alarm offer run only while
  exact alarms are off, as they are after a fresh install on Android 14+; never take
  `SCHEDULE_EXACT_ALARM` away during a test, because Android stops the app, test and all.
  Run instrumented tests
  only on an emulator or a test device, never on a phone that holds real trips. A
  `connectedDebugAndroidTest` run also uninstalls the app afterwards, which deletes its data.
- Release signing reads the upload key only from the `VAGOR_UPLOAD_KEYSTORE`,
  `VAGOR_UPLOAD_KEYSTORE_PASSWORD`, `VAGOR_UPLOAD_KEY_ALIAS` and `VAGOR_UPLOAD_KEY_PASSWORD`
  environment variables; without them `bundleRelease` is unsigned. Never commit a keystore or
  password.
- Release builds run R8 (`optimization { enable = true }`, turned on 2026-10-31): it shrinks,
  optimizes and obfuscates the code and removes unused resources. Debug builds and the
  instrumented tests don't use it, so check a change that relies on reflection, class names or
  resources looked up by name in a release build on an emulator. Keep rules go in
  `app/src/main/keepRules/*.keep`; none are needed today, because the libraries ship their own
  (Room keeps its generated `_Impl`, and Fragment keeps every fragment's no-argument constructor).
  The bundle carries R8's mapping file, which Play uses to deobfuscate crash reports; keep
  `app/build/outputs/mapping/release/mapping.txt` with each uploaded build to read stack traces
  yourself.
- CI (`.github/workflows/`, fixed on 2026-10-31 at the user's request) runs on GitHub's Ubuntu
  runners, which come with the Android SDK. `ci.yml` runs `lint`, `detekt`, `ktlintCheck`,
  `assembleDebug`, an unsigned `bundleRelease` (to run R8 and lint's release checks) and
  `testDebugUnitTest`, then `connectedAndroidTest` on an API 37 (`'37.0'`,
  quoted) `google_apis` emulator, which needs the Enable KVM step to run fast enough. It also
  needs the step that installs command-line tools 22.0 (added 2026-10-31 after the emulator
  timed out booting): the runner's own tools, 12.0 on Ubuntu 24.04, give an `android-37.0` AVD
  `target=android-0`, and the emulator runner uses them whenever they exist.
  `security.yml` runs CodeQL, which analyses Kotlin only by watching a build
  (`build-mode: manual`). CI calls only Gradle tasks that exist: SonarQube, OWASP
  dependency-check and coverage steps were removed, because no plugin provided their tasks and
  `|| true` hid that. Adding one back means adding its Gradle plugin first. CI can only be
  checked by pushing, so never report a workflow change as passing until a run on GitHub shows it.
- detekt and ktlint were added on 2026-10-31, asked for by the user. ktlint uses the IntelliJ IDEA
  code style without trailing commas, wrapping at 120 characters, and all the code was formatted
  to it once that day, so it has no baseline: `./gradlew ktlintFormat` fixes most findings, and
  every file already passes, so it changes only what's new. detekt uses its default rules (lines
  up to 120 characters, like ktlint) and fails only on findings that aren't in
  `app/detekt-baseline.xml`, which lists what the code had then and matches by rule and
  declaration. Never add new findings to the baseline to make a check pass. detekt is the 2.0
  alpha, because the last stable release (1.23.8) predates Kotlin 2.1, AGP 9 and JDK 25.
- Report exactly what you ran. If a change can only be checked on a device, say that it wasn't
  verified.

## Reminders

Decided with the user on 2026-10-25: reminders are automatic for every item. A vacation's fire
around 9:00 AM local time on its start and end dates, with inexact alarms: on Android 12+ they
usually arrive within an hour; on Android 8–11 (`setWindow`) Doze can hold them until the phone
wakes. Decided on 2026-10-26: an event's reminder fires an hour before it starts, in its own
time zone (`ReminderTimes.eventTriggerAtMillis`), with an exact alarm
(`setExactAndAllowWhileIdle`) whenever Android allows one: always below Android 12, and on
Android 12+ while Alarms & reminders (`SCHEDULE_EXACT_ALARM`) is on, which it is by default only
on Android 12 and 13. Without that it uses `setWindow` for 50 to 60 minutes before, which Doze
holds to its next maintenance window and Battery Saver holds until it's off or Vagor is opened,
so it can come after the event. An event saved before events had a time keeps the 9:00 AM
reminder on its date. A reminder whose moment has passed is never set, so one missed while the
phone was off isn't shown late. Changing any of these is the user's call.

- Post a `NotificationCompat` notification on an app notification channel (channels are required
  on every supported API level). Declare `POST_NOTIFICATIONS`, request it at runtime on API 33+
  when the user sets a reminder, and check `areNotificationsEnabled()`. While notifications are
  off, for the app or only for the reminder channel (`ReminderScheduler.notificationsOn`), the
  dashboard shows `NotificationsOffDialog` once per launch when there is a trip (on Android 13+
  after asking for the permission, if it isn't granted), and the Settings tab shows a Notification
  Settings button. Both open Android's notification settings for Vagor, or the reminder channel's
  when only it is off. Not a toast: Android cuts toasts at two lines. Tapping a vacation
  reminder opens the vacation's page; tapping an event reminder opens the event, with the
  vacation's page and the dashboard behind it.
- Only event reminders use exact alarms. `SCHEDULE_EXACT_ALARM` is denied by default for new
  installs on Android 14+, so check `canScheduleExactAlarms()` (`ReminderScheduler`) before
  each exact alarm and catch `SecurityException`. Never add `USE_EXACT_ALARM`: Google Play
  allows it only for calendar and alarm-clock apps. After saving an event whose reminder can't
  be exact, `ExactAlarmOfferDialog` offers Alarms & reminders, once per launch; the Settings tab
  says whether event reminders are exact and opens the same screen.
- Alarms are cleared on reboot and force stop. `VagorApplication` rebuilds every reminder from
  the database once in every new process (`ReminderScheduler.rebuildIfNeeded`), and `HomeActivity`
  again on resume if exact alarms were allowed or taken away since. `ReminderRescheduleReceiver`
  calls `rebuildIfNeeded` on `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`, which always start a new
  process that `VagorApplication` has just rebuilt in, so it rebuilds once, not twice (changed
  2026-10-31), and on `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` (sent when the user allows
  exact alarms), which may reach a running process, where the exact-alarm state now differs, so
  it rebuilds. On `TIME_SET` and `TIMEZONE_CHANGED` (dates saved without a time zone follow the
  phone's) it always rebuilds, because they can reach a process that is already running. Taking
  exact alarms away sends nothing: Android deletes the exact alarms and kills the app (not a force
  stop), and the event reminders come back as `setWindow` the next time anything starts the app.
  Setting Vagor's battery use to Unrestricted also allows exact alarms, with no broadcast.
- A rebuild never cancels a reminder whose moment has passed but which may still be on its way,
  since inexact alarms can come late: an event's is kept until the event starts, and a 9:00 AM
  one until its day ends (`keepDue`). Saving an event keeps it too, unless the date, time or time
  zone changed; then a reminder already posted for it is dismissed, because it gives the old time.
  Opt-in reminders would have to be saved in the database, which is a schema change (see
  Local-only rules).
- A posted reminder leaves the shade once it's out of date (`setTimeoutAfter`, asked for by the
  user on 2026-10-31): a 9:00 AM one when its day ends on the phone, and an event's an hour after
  the event starts (`ReminderTimes.dateReminderTimeoutMillis` and `eventReminderTimeoutMillis`).
  One that Android delivers after that, held back by Doze for example, is still posted, with no
  timeout of its own: the user chose not to skip late reminders. On the API 37 emulator Android
  gives every notification without a timeout one of 72 hours anyway. The timeout is a duration
  fixed when the reminder is posted, so a 9:00 AM one leaves at midnight in the zone the phone had
  then, even if the phone's zone changes after.
- Keep scheduling in `ReminderScheduler`. Detail screens call it on every save and delete.
- Android lets an app hold 500 alarms at a time, and past that every set call throws, even one
  that replaces an alarm already set (`IllegalStateException`, or `SecurityException` on some
  Samsung phones). Uncaught, that crashed every start, since each start rebuilds the reminders, and
  only clearing the app's storage (every trip) got out. Every alarm goes through
  `ReminderScheduler.setAlarm`, which cancels the reminder's own old alarm and tries once more,
  then leaves the reminder unset (fixed 2026-10-31, `ReminderLimitInstrumentedTest`). Which
  reminders get the 500 places past the limit, and whether the user is told, is the user's call.
- A PendingIntent's identity is its request code plus the Intent's action, data, type, component
  and categories. Extras don't count. Each reminder's identity is a data URI from `ReminderKey`,
  such as `vagor://reminder/vacation/<id>/start`, and the notification tag uses the same URI.

## Sharing: how to build it

- Share only through the Sharesheet: `Intent.ACTION_SEND` inside `Intent.createChooser`, as
  `EventListActivity` does, with text built from string resources such as
  `vacation_share_template`. No links to servers and no accounts.
- To share a file (an `.ics` file, for example), serve it through a `FileProvider` `content://`
  URI with `FLAG_GRANT_READ_URI_PERMISSION`. Never use a `file://` URI.
- Add to Calendar uses `Intent.ACTION_INSERT` with `CalendarContract.Events.CONTENT_URI` and the
  type `vnd.android.cursor.dir/event`, aimed at Google Calendar (`com.google.android.calendar`)
  and, when that isn't installed, at any calendar app (`openCalendarInsert`). It opens the
  calendar app's own new-event screen; that app decides whether and where to save (Android's
  original calendar app saves when the user leaves the screen), so never promise that nothing is
  added until the user saves. It needs no permission: never add `READ_CALENDAR` or
  `WRITE_CALENDAR`, and never write to the calendar provider directly. Starting the intent needs no
  `<queries>` entry; catch `ActivityNotFoundException` instead. A second tap within 2 seconds
  is ignored, because some calendar apps save every new-event screen the user leaves, and "no
  calendar app" is a dialog, because a toast is cut at two lines.
- Calendar apps read an all-day intent's times differently (in the phone's zone or in UTC; an
  end at midnight as exclusive or not), and Google Calendar documents none of it.
  `CalendarTimes` starts a vacation at the later of its first day's UTC and local midnights, and
  ends it inside its last day: an hour after that day's start, or 1:00 on the phone's clock,
  whichever is later (the clock time matters where clocks go back from 1:00 to midnight, and the
  hour where they skip from midnight to 1:00). Every app checked (Android's original calendar,
  Etar, Fossify) reads that correctly; Google Calendar's reading couldn't be checked without a
  signed-in account.
  Calendar apps also handle `Events.EVENT_TIMEZONE` unevenly, so an event's description names
  its time and zone too.
- Decided with the user on 2026-10-25: events are shared as plain text. Whether Vagor should
  be able to import a trip someone shared is still open.

## Working rules

- Never commit, amend, push, tag, open PRs or publish anything. Leave changes uncommitted for
  review. Never run `make -f Makefile.txt tag`, `push-tag` or `release`: they create and push git
  tags. There is no release workflow (`cd.yml` was removed at the user's request on 2026-10-25),
  so releases are built and uploaded by hand (PLAY_RELEASE.md).
- Make the smallest complete change. No unrelated refactors, renames or reformatting. Don't
  change dependency, AGP, Gradle, Kotlin or SDK versions unless asked.
- Unless the task explicitly calls for it, don't change `namespace`, the database schema,
  manifest permissions, backup settings, or the CI/CD workflows. Never change `applicationId`:
  after the first Play upload a new ID is a different app.
- `local.properties` (machine-specific `sdk.dir`) is tracked in git even though its own header
  says it shouldn't be. Don't commit changes to it.
- Vagor itself is proprietary: don't put its own code under an open-source license. The only
  third-party license text the app ships is the Apache License 2.0 in
  `res/raw/apache_license_2_0.txt`
  (added 2026-10-31), because the bundled libraries' license asks for it to go with the app; the
  Settings tab shows it under Third-Party Licenses. When a library, icon or other third-party
  material that ships in the app is added, add it to `NOTICE` and to `third_party_licenses_text`
  in strings.xml (`ThirdPartyLicensesTest` checks they list the same things); one under another
  license also needs that license's text added.
- Finish with: files changed, commands run and their results, checks not run, assumptions, and
  open risks.

## Known issues (as of 2026-10-25)

- Not yet on Google Play. The remaining steps are Play Console work listed in PLAY_RELEASE.md:
  hosting PRIVACY.md at a public URL, the upload key, Data safety and other declarations, store
  assets, and a 12-tester, 14-day closed test for new personal developer accounts.
- The system draws the launch window before any app code runs. On Android 8–11 it follows the
  system light/dark setting on every cold start, so a dark-mode user on a light-themed phone sees
  a brief light flash. On Android 12+ only the very first launch does; later launches match the
  app setting through `UiModeManager.setApplicationNightMode` (not captured on a device).
