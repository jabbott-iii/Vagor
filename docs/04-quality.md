# 04 — Quality: testing, checks, CI

Vagor keeps the user's only copy of their trips in one database file on
the phone (ADR-0003) and has no crash reporting of its own, by design
(ADR-0001; `docs/05-release.md`, step 8). The tests and checks below are
how a change is shown to work before it reaches anyone's phone. Every
command named here exists today.

## Test layers

| Layer | Where | Runs with | What it checks |
| :- | :- | :- | :- |
| JVM unit tests | `app/src/test/java/com/example/vagor/` | `./gradlew testDebugUnitTest`; CI | The plain-Kotlin date, time, reminder, calendar and dashboard logic; theme contrast; the third-party license list |
| Instrumented tests | `app/src/androidTest/java/com/example/vagor/` | `./gradlew connectedDebugAndroidTest` on an emulator or a test device; CI runs `connectedAndroidTest` on an API 37 emulator | Screens, forms and pickers through Espresso; reminders, alarms and notifications on a real Android; the calendar intents; the database migration; the 500-alarm limit |
| Android lint | The Android Gradle plugin; the project sets no lint options | `./gradlew lint`; CI. `bundleRelease` also runs lint's release checks (lintVital) | Lint's default checks |
| detekt | `app/detekt-baseline.xml` | `./gradlew detekt`; CI | Kotlin static analysis, default rules |
| ktlint | The `ktlint` block in `app/build.gradle.kts` | `./gradlew ktlintCheck`; CI | Kotlin style |
| CodeQL | `.github/workflows/security.yml` | CI only | Security analysis of the Kotlin code |
| Release build on an emulator | `docs/05-release.md`, step 3 | By hand, before every upload | What only R8 changes: shrunk and obfuscated code, removed resources |

```bash
./gradlew testDebugUnitTest                  # JVM tests in app/src/test
./gradlew testDebugUnitTest --tests 'com.example.vagor.DateValidatorsTest'
./gradlew lint
./gradlew detekt ktlintCheck                 # Kotlin static analysis and style
./gradlew ktlintFormat                       # fixes most ktlint findings
./gradlew connectedDebugAndroidTest          # Espresso tests; needs an emulator or device
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.vagor.HomeFlowInstrumentedTest
```

Read "Instrumented tests" below before running either
`connectedDebugAndroidTest` line.

## JVM unit tests

- JUnit 4, with no Android framework: no unit test imports `android.*`.
  They run on the build machine.
- They run with the module directory (`app/`) as the working directory,
  so the tests that read files open `src/main/res/...` and `../NOTICE`.
- Put new date or reminder math in plain Kotlin beside `DateValidators`, so
  it can be unit tested here (`java.time` is available at minSdk 26).
  The plain-Kotlin code these tests cover is in `DateValidators.kt`,
  `Dashboard.kt`, `EventTimes.kt`, `CalendarMonth.kt`, `CalendarTimes` (in
  `CalendarInsert.kt`), `ReminderSchedule.kt` and `LicenseText.kt`.

| Test class | What it checks |
| :- | :- |
| `DateValidatorsTest` | Stored dates are strict `MM/dd/yyyy`: loose dates rejected, leap days follow the calendar, comparisons reject loose dates. An end date can't precede its start; an event's date stays inside its vacation; `isOutsideVacation` is true only for a readable date outside the vacation (an old loose date can't be checked, so it doesn't block a save). `parseLocalDate`, `formatDate`, `HH:mm` times (loose ones rejected) and IANA zone IDs |
| `DashboardTest` | `Dashboard.rows`: soonest vacation first, a current one before upcoming ones, past ones below a heading, most recent first, the heading even when every vacation is past, no rows for no vacations, bad dates after current vacations and before past ones, ties sorted by end date then title. `Dashboard.status` describes where a vacation stands |
| `EventTimesTest` | An event's start is its date and time in its own zone; a missing or unknown zone uses the fallback; no start without a valid date and time; a time that a daylight saving change skips moves forward; events order by date, then time, then title |
| `ReminderScheduleTest` | `ReminderTimes`: 9:00 AM triggers on the stored date, in the zone they're computed in and across daylight saving changes, and none once 9:00 AM has passed or for a bad date. Event triggers an elapsed hour before the start in the event's own zone (possibly the day before), and none once that hour has passed. A passed reminder may still be due until its day ends (date) or the event starts (event). `mayOutlastHibernation` for trips ending more than two months ahead. Timeouts: a 9:00 AM reminder lasts until its day ends (including the extra hour when clocks go back) and a late one gets none; an event's lasts until an hour after it starts, in its own zone; an event without a time keeps its reminder until its day ends. `ReminderKey` URIs round-trip, stay distinct for large ids (the old id + 1000/2000/3000 request codes collided) and reject unknown URIs |
| `CalendarMonthTest` | The month grid: the 1st under its weekday, no leading blanks when the month starts on the first weekday, six weeks when needed, the Gregorian leap year rule, the week starting on the given day, and the offered range fitting the stored format |
| `CalendarTimesTest` | Add to Calendar's all-day times: a moment that falls on its date both in the phone's zone and in UTC, one that exists where midnight is skipped (Santiago), a vacation ending inside its last day, including where clocks go back over midnight (the Azores, Cuba), a one-day vacation, and no span for a bad date |
| `ThemeContrastTest` | WCAG AA contrast in both themes, from `res/values/colors.xml`: 4.5:1 for text, 3:1 for non-text UI (see "Accessibility checks") |
| `ThirdPartyLicensesTest` | The in-app list (`third_party_licenses_text`) names the same libraries as `NOTICE`; `res/raw/apache_license_2_0.txt` is the Apache License 2.0 exactly as Apache publishes it (by SHA-256); `reflowParagraphs` (`LicenseText.kt`) joins each paragraph onto one line and keeps every word in order |

## Instrumented tests

**IMPORTANT.** These install and run the app on an emulator or a device,
and several change what is on it:

- `HomeFlowInstrumentedTest`, `ReminderInstrumentedTest`,
  `CalendarDropdownInstrumentedTest`, `EventFormInstrumentedTest` and
  `CalendarInsertInstrumentedTest` write to the app's real
  `vacation_database`.
  - All but the two calendar tests also set real alarms, and
    `ReminderInstrumentedTest` posts real notifications. (The two calendar
    tests insert their rows through the DAOs and cancel those rows'
    reminders in `@After` too.)
  - All five delete only the rows they create, in `@After`, so a run that
    is killed partway can leave a test trip behind.
  - `CalendarInsertInstrumentedTest` catches the calendar intents with an
    `Instrumentation.ActivityMonitor`, so no calendar app opens.
- `MigrationInstrumentedTest` upgrades a database file of its own
  (`migration-test`) and deletes it. It never opens `vacation_database`.
- `ReminderLimitInstrumentedTest` sets 520 real event alarms, past
  Android's limit of 500, for events that aren't in the database, and
  cancels them afterwards. A run killed partway leaves them set until they
  fire 30 days later or the app is uninstalled.
- Every instrumented run starts the app's process, which re-arms every
  stored trip's reminders as any start does (`VagorApplication`).
- `SettingsInstrumentedTest` opens the real home screen, and it flips the
  dark mode setting and restores it.
- `CalendarDropdownInstrumentedTest` switches the app's language to Thai,
  US English and British English in two tests, and back.
- The home flow, reminder, settings and event form tests grant the
  notification permission and leave it granted.
- `SettingsInstrumentedTest` opens Android's Alarms & reminders screen and
  Vagor's App info and leaves them with Back. The event form and home flow
  tests open them from their offers and leave them through
  `leaveOtherAppsScreen()`, which waits until Android has closed the screen
  (an unhandled Back once closed the next test's screen on CI).
- The home flow's tests of the hibernation offer run only on Android 12+
  while Android may pause Vagor, as it may after any install.
- The event form's tests of the exact alarm offer run only while exact
  alarms are off, as they are after a fresh install on Android 14+. Never
  take `SCHEDULE_EXACT_ALARM` away during a test, because Android stops
  the app, test and all.
- **Run instrumented tests only on an emulator or a test device, never on a
  phone that holds real trips.** A `connectedDebugAndroidTest` run also
  uninstalls the app afterwards, which deletes its data.

| Test class | What it checks |
| :- | :- |
| `HomeFlowInstrumentedTest` | Creating a vacation from the dashboard; the dashboard's order (soonest first, past ones below); a vacation's page and its events; deleting from Edit returns to the dashboard; editing updates the page; a vacation can't be moved or shortened away from its events; the hibernation offer (once per launch, through a rotation, App info, Back); new vacations and events have no Delete button; each screen's title and heading |
| `ReminderInstrumentedTest` | Saving sets and deleting cancels reminders; an event's reminder until an hour before it starts; a rebuild keeps an alarm that may still be on its way; a new time dismisses a posted reminder; a date moved into the past cancels its reminder; `rescheduleAll` restores reminders from the database; a fired reminder posts a notification with its timeout; an event reminder gives its start time and details; a late reminder is posted without a timeout; tapping a reminder opens the vacation page or the event; a reminder for a deleted item posts nothing |
| `EventFormInstrumentedTest` | Saving needs a time and keeps the details; the searchable time zone list; an event opens with its zone and details; an event deleted since it was opened closes the form; an event saved without a time needs one before saving; rotation keeps the picked time and zone and an open clock; the time and zone fields read as drop-down lists that can't be typed into; the vacation page shows each event's time and details in time order; the exact alarm offer (once, through a rotation, Alarms & reminders, Back) |
| `CalendarDropdownInstrumentedTest` | Picking a day; Back leaves the date unchanged; the month buttons; the end date calendar starts at the start date; an event's calendar offers only its vacation's days; days at least 48dp each way where the screen allows; today marked for screen readers and drawn as a ring; Gregorian in a Thai locale; dates shown the phone's way; a picked date survives rotation; the date field reads as a drop-down list that can't be typed into |
| `CalendarInsertInstrumentedTest` | A vacation goes in all day over its dates with its details and events; an event at its time in its zone for an hour; an event without a time, or with an old loose date, isn't sent; a second tap while the calendar opens is ignored |
| `SettingsInstrumentedTest` | Dark mode on by default, and the switch; Back from Settings returns to Vacations; whether event reminders are exact, and Alarms & reminders; whether Android may pause Vagor, and App info; the Notification Settings button only while notifications are off; the notifications-off dialog opens notification settings; the privacy policy (through a rotation); the third-party licenses with the license text; the app's version |
| `MigrationInstrumentedTest` | Version 1 to 2 keeps every vacation and event: the upgraded tables match the exported version 2 schema, and the event has blank details and no time. It uses `MigrationTestHelper` (`room-testing`) with the schemas in `app/schemas`, which `app/build.gradle.kts` adds to the test assets |
| `ReminderLimitInstrumentedTest` | Past the 500-alarm limit, reminders are left unset instead of crashing, and replacing a reminder already set still works (asserted on Android 12+, which has the limit) |
| `ExampleInstrumentedTest` | The app under test is `io.github.jabbott_iii.vagor` |

### Test helpers

| Helper | File | What it does |
| :- | :- | :- |
| `setDate(stored)` | `DateFieldTesting.kt` | Puts a stored `MM/dd/yyyy` date (or an older loose one) in a `DateField`, as picking it from the calendar would |
| `withDate(stored)` | `DateFieldTesting.kt` | Matches a `DateField` holding that stored date, whatever format it's shown in |
| `leaveOtherAppsScreen()` | `OtherAppTesting.kt` | Checks that another app's screen opened over Vagor, presses Back, and waits until that window has closed |
| `watchForOwnAppInfo()` | `OtherAppTesting.kt` | An `Instrumentation.ActivityMonitor` that counts starts of Vagor's own App info screen without stopping them |

- Tests set and check date fields only with `setDate` and `withDate`: a
  `DateField`'s `date` holds the stored value, and its text is only for
  show (`docs/subsystems/dates-times-and-pickers.md`).
- An `Instrumentation.ActivityMonitor` that returns a result stops an
  intent from opening anything. `CalendarInsertInstrumentedTest` catches
  every `ACTION_INSERT` that way, and `SettingsInstrumentedTest` catches
  the request for Android's notification settings.
- Tests that depend on the device's state skip themselves with a JUnit
  assumption (`assumeTrue`, `assumeFalse`), and several take a different
  branch below Android 12. A skipped test is not a passed one, and a run
  on one emulator covers only the branches that emulator takes.

## Static analysis

### Android lint

- `./gradlew lint` runs lint with its defaults: `app/build.gradle.kts` has
  no `lint` block.
- Only release builds run lint's release checks (lintVital), so CI builds
  `bundleRelease` as well as `assembleDebug`.

### detekt

- Added on 2026-10-31, asked for by the user (ADR-0022). detekt is the 2.0
  alpha (`2.0.0-alpha.6`, plugin `dev.detekt`), because the last stable
  release (1.23.8) predates Kotlin 2.1, AGP 9 and JDK 25.
- It uses its default rules (`buildUponDefaultConfig = true`, no config
  file), so lines can be up to 120 characters, like ktlint. It checks
  `src/main/java`, `src/test/java` and `src/androidTest/java`.
- It fails only on findings that aren't in `app/detekt-baseline.xml`,
  which lists what the code had on 2026-10-31 and matches by rule and
  declaration.
- **Never add new findings to the baseline to make a check pass.**

### ktlint

- Added on 2026-10-31 with detekt, asked for by the user (ADR-0022):
  ktlint 1.8.0 through the ktlint Gradle plugin 14.2.0.
- It uses the IntelliJ IDEA code style without trailing commas, wrapping
  at 120 characters.
- All the code was formatted to it once that day, so it has no baseline.
  `./gradlew ktlintFormat` fixes most findings, and every file already
  passes, so it changes only what's new.

## Accessibility checks

Requirement 7 (`docs/00-overview.md`; ADR-0017;
`docs/subsystems/theme-and-accessibility.md`) is checked in two places.

- **`ThemeContrastTest`** (JVM) reads `res/values/colors.xml` and checks
  that every listed pair of colour roles (`onPrimary` on `primary`, for
  example) meets WCAG AA in the light and dark schemes: 4.5:1 for text,
  3:1 for non-text UI. The pairs are listed in the test itself, so a new
  pair is checked only once it's added there. Among them:
  - text: on the theme's surfaces and containers, selected text on the
    selection highlight, dashboard cards, the bottom navigation bar's
    labels, the drop-down calendar's weekdays and days, and the event time
    clock (`ThemeOverlay.Vagor.MaterialTimePicker`);
  - non-text: the blue Add buttons against the screen, the selected tab's
    indicator, Material's focus rings, the calendar's today ring and
    selected circle, and the clock's AM/PM outline.
- **Instrumented tests** check what only a running screen shows:
  - calendar days at least 48dp each way where the screen allows (on a
    screen narrower than seven 48dp days, such as CI's 320dp-wide
    emulator, as wide as allowed);
  - today marked for screen readers, and drawn as a ring rather than a
    filled circle, since the ring is what marks it without colour;
  - date, time and time zone fields read as drop-down lists that can't be
    typed into;
  - each screen's own title, and each form's heading marked as a heading.
- Focus rings are checked only for contrast: no test moves focus with a
  keyboard.

## CI (GitHub Actions)

CI was fixed on 2026-10-31 at the user's request (ADR-0023). It runs on
GitHub's Ubuntu runners, which come with the Android SDK. Agents never
push, so CI results come from the user's pushes.

- **`.github/workflows/ci.yml`**, on pushes and pull requests to `main`
  and `develop`, and by hand (`workflow_dispatch`):
  1. **`build-and-test`** (Temurin JDK 17): `lint`, `detekt ktlintCheck`,
     `assembleDebug`, an unsigned `bundleRelease` (to run R8 and lint's
     release checks), then `testDebugUnitTest`. It uploads the test
     results and publishes a test report, except on a pull request from a
     fork, whose read-only token can't create the check run.
  2. **`instrumented-tests`**: `connectedAndroidTest` on an API 37
     (`'37.0'`, quoted, since YAML would read 37.0 as the number 37)
     `google_apis` x86_64 emulator, with animations off.
     - It needs the **Enable KVM** step: without it the emulator runs
       unaccelerated, and the UI tests fail waiting for window focus.
     - It needs the step that **installs command-line tools 22.0** (added
       2026-10-31 after the emulator timed out booting). The runner's own
       tools, 12.0 on Ubuntu 24.04, give an `android-37.0` AVD
       `target=android-0`, and the emulator runner uses them whenever they
       exist. The step prints the download's SHA-256 so it can be pinned
       later, and a pre-launch script reports an error unless the AVD says
       `target=android-37.0`.
     - It uploads the instrumented test results.
- **`.github/workflows/security.yml`** runs CodeQL (`java-kotlin`) on the
  same pushes and pull requests and weekly (Sundays, 02:00 UTC). CodeQL
  analyses Kotlin only by watching a build (`build-mode: manual`), so it
  runs `assembleDebug` with no build cache and the Kotlin compiler inside
  the Gradle process.
- **CI calls only Gradle tasks that exist.** SonarQube, OWASP
  dependency-check and coverage steps were removed, because no plugin
  provided their tasks and `|| true` hid that. Adding one back means
  adding its Gradle plugin first (`docs/02-dependency-policy.md`).
- **CI can only be checked by pushing**, so never report a workflow change
  as passing until a run on GitHub shows it.
- CI never signs anything: it builds the release bundle unsigned
  (`docs/05-release.md`, "Release signing").

## Manual and device-only checks

Some things no automated check covers. Do them, or report them as not
verified.

- **The release build, before every upload.** Only release builds run R8,
  and the instrumented tests use the debug build. `docs/05-release.md`,
  step 3, installs the release build on an emulator and lists what to try
  on it.
- **Reflection, class names, resources looked up by name.** Check a change
  that relies on any of them in a release build on an emulator.
- **Calendar apps.** Android's original calendar app, Etar and Fossify
  were checked to read the all-day times correctly; Google Calendar's
  reading couldn't be checked without a signed-in account
  (`docs/subsystems/sharing-and-calendar.md`).
- **The launch window.** On Android 12+, later launches match the app's
  dark mode setting through `UiModeManager.setApplicationNightMode`; this
  was not captured on a device.
- **Reminders on a phone.** When a reminder comes depends on Doze, Battery
  Saver, standby buckets and Alarms & reminders
  (`docs/subsystems/reminders.md`). The by-hand check is step 3's: an
  event about 70 minutes ahead, whose notification comes about 10 minutes
  later (up to 20 when Alarms & reminders is off). On the API 37 emulator
  Android gives every notification without a timeout one of 72 hours
  anyway.
- **Store screenshots** are retaken on an emulator with invented trips
  when the screen they show changes (`docs/05-release.md`, step 6;
  `/update-store-listing`).

## Change review checklist

Use this before reporting any change. Each item repeats a rule from
`AGENTS.md` or the doc named.

- [ ] Did the database schema change? Then `AppDatabase.version` is
      bumped, a `Migration` is in `AppDatabase.MIGRATIONS`, the exported
      schema JSON in `app/schemas` is kept, and `MigrationInstrumentedTest`
      tests the upgrade (`/change-database-schema`).
- [ ] Did a dependency, plugin or version change? Only with the user's
      approval; then the merged manifest is checked for new permissions,
      and anything that ships is in `NOTICE` and `third_party_licenses_text`
      (`docs/02-dependency-policy.md`).
- [ ] Is new date, time or reminder math in plain Kotlin, with a JVM test?
- [ ] Is every new text/background colour pair in `ThemeContrastTest`?
- [ ] Does the change rely on reflection, class names or resources looked
      up by name? Then it was tried in a release build on an emulator.
- [ ] Does the change alter what the app does with data? Then PRIVACY.md
      and `privacy_policy_text` changed together, `LICENSE` sections 5 and
      6 are still true, and so is the store listing.
- [ ] Did a screen that a store screenshot shows change? Then the
      screenshot needs retaking (`docs/05-release.md`, step 6).
- [ ] Is `app/detekt-baseline.xml` unchanged?
- [ ] Were the instrumented tests, if run, run only on an emulator or a
      test device?

## Report exactly what you ran

- Report exactly what you ran, and each command's result.
- If a change can only be checked on a device, say that it wasn't
  verified.
- Never report a workflow change as passing until a run on GitHub shows
  it.
- Finish in the definition-of-done format (`AGENTS.md`): files changed,
  commands run and their results, checks not run, assumptions, and open
  risks.
