# Vagor

<!--
Maintainers: written 2026-10-25 against commit 2092255. The "Current:" notes and the
"Known issues" section are point-in-time; update or delete entries as they get fixed.
Block-level HTML comments like this one are stripped before Claude reads the file.
-->

Vagor is an offline Android vacation planner written in Kotlin: a single `:app` module,
package `com.example.vagor`. It runs entirely on the phone, with no backend and no accounts.

## Product requirements

These define "done" for feature work. The requirements say **events**; the code and UI call them
**excursions**. Keep the existing name unless asked to rename.

1. **Vacations**: create, edit and delete, with title, hotel, start date and end date.
   Current: implemented.
2. **Events (excursions)**: each one is planned on a date inside its vacation's date range.
   Current: implemented.
3. **Reminders**: system notifications for vacation start, vacation end and each excursion.
   Current: implemented. Automatic for every item: a notification at 9:00 AM local time on each
   vacation's start and end dates and on each excursion's date. See "Reminders" below.
4. **Sharing**: vacations and excursions can be shared with other people.
   Current: implemented as plain text through the Sharesheet. The vacation text lists its
   excursions, and each excursion has its own Share button. Importing a shared trip isn't supported.
5. **Local-only**: see the next section.

## Local-only rules (never break these)

- No network access of any kind. Don't add `INTERNET` or `ACCESS_NETWORK_STATE`, HTTP clients,
  remote WebView content, Firebase, analytics, crash reporting, ads, accounts or sync. The
  manifest declares only `POST_NOTIFICATIONS` and `RECEIVE_BOOT_COMPLETED`. After adding any
  dependency, check the merged manifest for permissions it pulls in.
- Data leaves the app only when the user shares it through the Android Sharesheet, plus Android
  Auto Backup (see Known issues).
- All user data lives in the Room database file `vacation_database`, and there is no other copy.
  Never rename it, never use `fallbackToDestructiveMigration`, and never clear tables outside
  tests. For a schema change, bump `AppDatabase.version` and add a `Migration`. Before the first
  one, turn on `exportSchema` with a schema directory and export the current version 1 schema,
  so migrations can be tested with `MigrationTestHelper`.

## Code layout and patterns

- Screens in `app/src/main/java/com/example/vagor/`: `HomeActivity` (launcher), then
  `VacationListActivity`, `VacationDetailActivity`, `ExcursionListActivity` and
  `ExcursionDetailActivity`. Also there: `entities/`, `dao/`, `database/AppDatabase.kt`,
  `DateValidators.kt`, `ReminderSchedule.kt` (plain-Kotlin reminder times and identities),
  `ReminderScheduler.kt` (sets and cancels alarms), `MyReceiver.kt` (the alarm receiver; posts
  the notification) and `ReminderRescheduleReceiver.kt`. `MainActivity` and `activity_main.xml`
  are unused template leftovers.
- UI is XML layouts, `AppCompatActivity`, `findViewById`, and `ListView` with `ArrayAdapter`. The
  code uses no Compose, ViewBinding, ViewModel, DI framework or coroutines. Don't introduce them
  without the user's approval.
- Every Activity that reads or writes data builds its own Room instance with
  `allowMainThreadQueries()`, and DAOs are synchronous. Follow that pattern in small changes.
  A shared instance or background threading is a refactor: propose it first.
- Screens pass data through Intent extras with literal keys (`vacationId`, `title`, `hotel`,
  `startDate`, `endDate`, `excursionId`, `date`). The instrumented tests and the reminder
  notifications use the same keys. Detail screens prefill from the extras.
- User-visible text goes in `res/values/strings.xml`.
- Dates are stored as `MM/dd/yyyy` strings. Parse and validate them only through
  `DateValidators`. Put new date or reminder math in plain Kotlin beside it so it can be unit
  tested (`java.time` is available at minSdk 26).
- Rules enforced today: all fields are required; vacation end date is on or after the start date;
  an excursion's date falls within its vacation's start and end dates, inclusive; and a vacation
  that has excursions can't be deleted. That last check exists only in `VacationDetailActivity`,
  because `excursions.vacationId` has no foreign key.

## Build and test

The build uses AGP 9.4.1 with built-in Kotlin (Kotlin 2.4.20, set through the root
`buildscript` classpath), KSP 2.3.12 for Room, Gradle 9.6.1, compileSdk 37 and targetSdk 35.
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
  `vacation_database` and post real notifications. `ReminderInstrumentedTest` deletes only the
  rows it inserts; `HomeFlowInstrumentedTest` leaves its vacation behind. Run instrumented tests
  only on an emulator or a test device, never on a phone that holds real trips.
- CI (`.github/workflows/`) also calls `detekt`, `ktlintCheck`, `sonarqube`,
  `dependencyCheckAnalyze`, `testDebugUnitTestCoverage` and `publishBundle`. None of those tasks
  exist in this build because no plugin provides them. Those steps use `|| true` or
  `continue-on-error`, so a green run says nothing about them. Never report them as passing.
- Report exactly what you ran. If a change can only be checked on a device, say that it wasn't
  verified.

## Reminders

Decided with the user on 2026-10-25: reminders are automatic for every item, fire at 9:00 AM
local time on the stored date with no lead time, and excursions stay date-only. Changing any of
these is the user's call.

- Post a `NotificationCompat` notification on an app notification channel (channels are required
  on every supported API level). Declare `POST_NOTIFICATIONS`, request it at runtime on API 33+
  when the user sets a reminder, and check `areNotificationsEnabled()`. Tapping the notification
  should open the related detail screen.
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
  `VacationDetailActivity` does, with text built from string resources such as
  `vacation_share_template`. No links to servers and no accounts.
- To share a file (an `.ics` file, for example), serve it through a `FileProvider` `content://`
  URI with `FLAG_GRANT_READ_URI_PERMISSION`. Never use a `file://` URI.
- Decided with the user on 2026-10-25: excursions are shared as plain text. Whether Vagor should
  be able to import a trip someone shared is still open.

## Working rules

- Never commit, amend, push, tag, open PRs or publish anything. Leave changes uncommitted for
  review. Never run `make -f Makefile.txt tag`, `push-tag` or `release`: a pushed `v*` tag
  triggers `cd.yml`, which builds a signed release, creates a GitHub Release and uploads to the
  Google Play internal track.
- Make the smallest complete change. No unrelated refactors, renames or reformatting. Don't
  change dependency, AGP, Gradle, Kotlin or SDK versions unless asked.
- Unless the task explicitly calls for it, don't change `applicationId` or `namespace`, the
  database schema, manifest permissions, backup settings, or the CI/CD workflows.
- `local.properties` (machine-specific `sdk.dir`) is tracked in git even though its own header
  says it shouldn't be. Don't commit changes to it.
- Finish with: files changed, commands run and their results, checks not run, assumptions, and
  open risks.

## Known issues (as of 2026-10-25)

- A Google Play upload would be rejected today. Play requires targetSdk 36 or higher for new apps
  and updates since 2026-08-31, and it rejects `com.example.*` package names. Changing
  `applicationId` gives the app a new identity, so that's the user's decision.
- The README calls date validation strict, but `SimpleDateFormat` parsing ignores trailing text
  and doesn't enforce digit counts: `09/16/2026abc`, `9/16/2026` and `09/16/26` (the year 26)
  all pass `DateValidators`.
- Auto Backup is on (`allowBackup="true"` with the template rules), so the Room database is part
  of the user's Google Drive backup. Whether that fits "local-only" is an open decision.
- The Slack payload in `cd.yml` says "DenariiDolor Release Notification".
