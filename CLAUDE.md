# Vagor

<!--
Maintainers: written 2026-10-25 against commit 2092255. The "Current:" notes and the
"Known issues" section are point-in-time; update or delete entries as they get fixed.
Block-level HTML comments like this one are stripped before Claude reads the file.
-->

Vagor is an offline Android vacation planner written in Kotlin: a single `:app` module. Its
application ID (the Play and device identity) is `io.github.jabbott_iii.vagor`; the Kotlin package
and `namespace` are still `com.example.vagor`. It runs entirely on the phone, with no backend and no
accounts.

## Product requirements

These define "done" for feature work. Events were called excursions until 2026-10-25; the UI and
code now say **events**, but the Room table keeps its old name `excursions` (see Code layout).

1. **Vacations**: create, edit and delete, with title, hotel, start date and end date.
   Current: implemented.
2. **Events**: each one is planned on a date inside its vacation's date range.
   Current: implemented.
3. **Reminders**: system notifications for vacation start, vacation end and each event.
   Current: implemented. Automatic for every item: a notification around 9:00 AM local time on
   each vacation's start and end dates and on each event's date. See "Reminders" below.
4. **Sharing**: vacations and events can be shared with other people.
   Current: implemented as plain text through the Sharesheet. The vacation text lists its
   events, and each event has its own Share button. Importing a shared trip isn't supported.
5. **Local-only**: see the next section.
6. **Dark mode**: a switch on the Settings tab, on by default (asked for by the user on
   2026-10-25 as an on/off switch, not a follow-system option).
   Current: implemented (`ThemeSettings`, `VagorApplication`, `SettingsFragment`).
7. **Accessible colours**: a blue Material 3 scheme for colour-blind users, from the Okabe-Ito blue
   `#0072B2`. Every text/background pair must meet WCAG AA (4.5:1; 3:1 for non-text UI) in both
   themes; `ThemeContrastTest` enforces this. Don't convey anything by colour alone, and don't
   add DynamicColors (it would replace the blue with wallpaper colours).
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
   Current: implemented.

## Local-only rules (never break these)

- No network access of any kind. Don't add `INTERNET` or `ACCESS_NETWORK_STATE`, HTTP clients,
  remote WebView content, Firebase, analytics, crash reporting, ads, accounts or sync. The
  manifest declares only `POST_NOTIFICATIONS` and `RECEIVE_BOOT_COMPLETED`. After adding any
  dependency, check the merged manifest for permissions it pulls in. Play's Data safety answer
  ("no data collected or shared") and PRIVACY.md depend on this.
- Data leaves the app only when the user shares it through the Android Sharesheet. Cloud backup
  is off, decided with the user on 2026-10-25 (`allowBackup="false"` plus exclusions in
  `backup_rules.xml` and `data_extraction_rules.xml`); a direct device-to-device transfer to a new
  phone is still allowed.
- The privacy policy exists twice: PRIVACY.md and `privacy_policy_text` in strings.xml. Change
  both together, and keep them true to what the app does. Its privacy contact is the public GitHub
  Issues page (picked as the default on 2026-10-25; the user may want an email address instead).
- All user data lives in the Room database file `vacation_database`, and there is no other copy.
  Never rename that file, never use `fallbackToDestructiveMigration`, and never clear tables
  outside tests. For a schema change, bump `AppDatabase.version` and add a `Migration`. Before the
  first one, turn on `exportSchema` with a schema directory and export the current version 1
  schema, so migrations can be tested with `MigrationTestHelper`. The only other stored state is
  the dark mode switch, in the SharedPreferences file `settings`.

## Code layout and patterns

- Screens in `app/src/main/java/com/example/vagor/`: `HomeActivity` (launcher) holds a bottom
  navigation bar and two tabs, `VacationsFragment` (the dashboard) and `SettingsFragment`. Tapping
  a vacation opens `EventListActivity`, the vacation's page: its details, Edit Vacation and Share
  Vacation buttons, and its events. `VacationDetailActivity` is the add/edit vacation form, and
  `EventDetailActivity` the add/edit event form. Also there: `entities/` (`Vacation`, `Event`),
  `dao/`, `database/AppDatabase.kt`, `DateValidators.kt`, `Dashboard.kt` (plain-Kotlin dashboard
  order and trip status), `TripText.kt` (dates and status as display text), `CalendarDropdown.kt`
  (the date fields' drop-down calendar), `ReminderSchedule.kt` (plain-Kotlin reminder times and
  identities), `ReminderScheduler.kt` (sets and cancels alarms), `MyReceiver.kt` (the alarm
  receiver; posts the notification), `ReminderRescheduleReceiver.kt`, `ThemeSettings.kt`,
  `VagorApplication.kt` (applies the saved theme before any screen) and `EdgeToEdge.kt`.
  `MainActivity` and `activity_main.xml` are unused template leftovers.
- The `Event` entity maps to the table `excursions` (`@Entity(tableName = "excursions")`), and
  `EventDAO` queries that table. Don't rename the table without a migration.
- Every screen calls `setEdgeToEdgeContentView()` instead of `setContentView()`, so content stays
  clear of the system bars (required at targetSdk 36); `HomeActivity` uses
  `setEdgeToEdgeContentViewWithBottomBar()`, because the bottom bar pads itself. While the keyboard is open it also ends the
  root view at the keyboard's top edge. The two detail forms are in a `ScrollView` with
  `adjustResize`, so every field and button can be scrolled into view in landscape and with the
  keyboard open (a full-screen landscape keyboard still covers the form until it is closed).
- Colours come from theme attributes only (`res/values/colors.xml`, `values/themes.xml`,
  `values-night/themes.xml`). Never hard-code a text or background colour in a layout.
- UI is XML layouts, `AppCompatActivity` (plus a `Fragment` for each home screen tab),
  `findViewById`, and `ListView` with `ArrayAdapter`. The code uses no Compose, ViewBinding,
  ViewModel, DI framework or coroutines. Don't introduce them without the user's approval.
- Every Activity or Fragment that reads or writes data builds its own Room instance with
  `allowMainThreadQueries()`, and DAOs are synchronous. Follow that pattern in small changes.
  A shared instance or background threading is a refactor: propose it first.
- Screens pass data through Intent extras with literal keys (`vacationId`, `title`, `hotel`,
  `startDate`, `endDate`, `eventId`, `date`). The instrumented tests and the reminder
  notifications use the same keys. Detail screens prefill from the extras; the vacation page
  (`EventListActivity`) takes only `vacationId` and reloads the vacation from the database on
  every resume, closing itself if the vacation was deleted.
- User-visible text goes in `res/values/strings.xml`.
- Dates are stored as `MM/dd/yyyy` strings. Parse and validate them only through
  `DateValidators`, which is strict: exactly two-digit month and day, four-digit year, a real
  calendar date, nothing before or after. Date fields are filled only from the drop-down calendar
  (`attachCalendarDropdown()` in `CalendarDropdown.kt`), which writes through
  `DateValidators.formatDate`; the save-time checks still run, because saved rows may hold older
  loose dates. Put new date or reminder math in plain Kotlin beside it so it can be unit tested
  (`java.time` is available at minSdk 26).
- Rules enforced today: all fields are required; vacation end date is on or after the start date;
  an event's date falls within its vacation's start and end dates, inclusive; and a vacation
  that has events can't be deleted. That last check exists only in `VacationDetailActivity`,
  because `excursions.vacationId` has no foreign key.

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
./gradlew connectedDebugAndroidTest          # Espresso tests; needs an emulator or device
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.vagor.HomeFlowInstrumentedTest
```

- IMPORTANT: `HomeFlowInstrumentedTest` and `ReminderInstrumentedTest` write to the app's real
  `vacation_database` and post real notifications. Both delete only the rows they create.
  `SettingsInstrumentedTest` flips the dark mode setting and restores it. Run instrumented tests
  only on an emulator or a test device, never on a phone that holds real trips.
- Release signing reads the upload key only from the `VAGOR_UPLOAD_KEYSTORE`,
  `VAGOR_UPLOAD_KEYSTORE_PASSWORD`, `VAGOR_UPLOAD_KEY_ALIAS` and `VAGOR_UPLOAD_KEY_PASSWORD`
  environment variables; without them `bundleRelease` is unsigned. Never commit a keystore or
  password.
- CI (`.github/workflows/`) also calls `detekt`, `ktlintCheck`, `sonarqube`,
  `dependencyCheckAnalyze` and `testDebugUnitTestCoverage`. None of those tasks
  exist in this build because no plugin provides them. Those steps use `|| true` or
  `continue-on-error`, so a green run says nothing about them. Never report them as passing.
- Report exactly what you ran. If a change can only be checked on a device, say that it wasn't
  verified.

## Reminders

Decided with the user on 2026-10-25: reminders are automatic for every item, fire around 9:00 AM
local time on the stored date with no lead time, and events stay date-only. Alarms are inexact:
on Android 12+ they usually arrive within an hour; on Android 8–11 (`setWindow`) Doze can hold
them until the phone wakes. Changing any of
these is the user's call.

- Post a `NotificationCompat` notification on an app notification channel (channels are required
  on every supported API level). Declare `POST_NOTIFICATIONS`, request it at runtime on API 33+
  when the user sets a reminder, and check `areNotificationsEnabled()`. Tapping a vacation
  reminder opens the vacation's page; tapping an event reminder opens the event, with the
  vacation's page and the dashboard behind it.
- Use inexact alarms (`setAndAllowWhileIdle` or `setWindow`). Exact alarms need
  `SCHEDULE_EXACT_ALARM`, which is denied by default for new installs on Android 14+, or
  `USE_EXACT_ALARM`, which Google Play allows only for calendar and alarm-clock apps. Ask before
  adding either.
- Alarms are cleared on reboot and force stop. `ReminderRescheduleReceiver` rebuilds every
  reminder from the database on `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET` and
  `TIMEZONE_CHANGED` (stored dates have no time zone), and `HomeActivity` does the same on launch.
  Opt-in reminders would have to be saved in the database, which is a schema change (see
  Local-only rules).
- Keep scheduling in `ReminderScheduler`. Detail screens call it on every save and delete.
- A PendingIntent's identity is its request code plus the Intent's action, data, type, component
  and categories. Extras don't count. Each reminder's identity is a data URI from `ReminderKey`,
  such as `vagor://reminder/vacation/<id>/start`, and the notification tag uses the same URI.

## Sharing: how to build it

- Share only through the Sharesheet: `Intent.ACTION_SEND` inside `Intent.createChooser`, as
  `EventListActivity` does, with text built from string resources such as
  `vacation_share_template`. No links to servers and no accounts.
- To share a file (an `.ics` file, for example), serve it through a `FileProvider` `content://`
  URI with `FLAG_GRANT_READ_URI_PERMISSION`. Never use a `file://` URI.
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
- Finish with: files changed, commands run and their results, checks not run, assumptions, and
  open risks.

## Known issues (as of 2026-10-25)

- Not yet on Google Play. The remaining steps are Play Console work listed in PLAY_RELEASE.md:
  hosting PRIVACY.md at a public URL, the upload key, Data safety and other declarations, store
  assets, and a 12-tester, 14-day closed test for new personal developer accounts.
- The launcher icon is still the Android template robot, now on a blue background.
- The system draws the launch window before any app code runs. On Android 8–11 it follows the
  system light/dark setting on every cold start, so a dark-mode user on a light-themed phone sees
  a brief light flash. On Android 12+ only the very first launch does; later launches match the
  app setting through `UiModeManager.setApplicationNightMode` (not captured on a device).
