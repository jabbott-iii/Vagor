---
paths:
  - "app/src/androidTest/**"
---

# Rules for the instrumented tests

These tests run inside the real app on a phone or emulator, and several of
them change that phone's real state. **Run them only on an emulator or a
test device, never on a phone that holds real trips** (`AGENTS.md`, rule
4). From Claude Code, run them only on an emulator (`CLAUDE.md`). The
quality plan is `docs/04-quality.md`.

- **What a run does to the phone:**
  - `HomeFlowInstrumentedTest`, `ReminderInstrumentedTest`,
    `CalendarDropdownInstrumentedTest`, `EventFormInstrumentedTest` and
    `CalendarInsertInstrumentedTest` write to the app's real
    `vacation_database`. All but the two calendar tests also set real
    alarms, and `ReminderInstrumentedTest` posts real notifications.
  - All five delete only the rows they create, in `@After`, so a run that is
    killed partway can leave a test trip behind.
  - `CalendarInsertInstrumentedTest` catches the calendar intents with an
    `Instrumentation.ActivityMonitor`, so no calendar app opens.
  - `MigrationInstrumentedTest` upgrades a database file of its own
    (`migration-test`) and deletes it; it never opens `vacation_database`.
  - `ReminderLimitInstrumentedTest` sets 520 real event alarms, past
    Android's limit of 500, for events that aren't in the database, and
    cancels them afterwards. A run killed partway leaves them set until they
    fire 30 days later or the app is uninstalled.
  - Every instrumented run starts the app's process, which re-arms every
    stored trip's reminders as any start does (`VagorApplication`).
  - `SettingsInstrumentedTest` opens the real home screen, and it flips the
    dark mode setting and restores it.
  - `CalendarDropdownInstrumentedTest` switches the app's language to Thai,
    US English and British English in two tests and back.
  - The home flow, reminder, settings and event form tests grant the
    notification permission and leave it granted.
  - A `connectedDebugAndroidTest` run also uninstalls the app afterwards,
    which deletes its data.
- **Ask the user before every connected run,** and check with
  `adb devices` first that the only device connected is an emulator.
  `.claude/settings.json` asks before `connectedDebugAndroidTest` and
  `connectedAndroidTest`, and denies `adb -d` (a physical phone). Building
  the tests without running them (`./gradlew assembleDebugAndroidTest`)
  touches no device.
- **Commands:**
  - all: `./gradlew connectedDebugAndroidTest`
  - one class:
    `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.vagor.HomeFlowInstrumentedTest`
- **Other apps' screens:**
  - `SettingsInstrumentedTest` opens Android's Alarms & reminders screen and
    Vagor's App info and leaves them with Back.
  - The event form and home flow tests open them from their offers and leave
    them through `leaveOtherAppsScreen()` (`OtherAppTesting.kt`), which waits
    until Android has closed the screen. An unhandled Back once closed the
    next test's screen on CI. A new test that opens another app's screen
    leaves it the same way.
  - `watchForOwnAppInfo()` counts starts of Vagor's App info screen without
    stopping it.
- **Tests that depend on the phone's state:**
  - The home flow's tests of the hibernation offer run only on Android 12+
    while Android may pause Vagor, as it may after any install
    (`assumeTrue`).
  - The event form's tests of the exact alarm offer run only while exact
    alarms are off, as they are after a fresh install on Android 14+
    (`assumeFalse`).
  - Both classes mark their offer as already made
    (`HibernationOfferDialog.offered`, `ExactAlarmOfferDialog.offered`) in
    `@Before`, so it doesn't cover the screen, clear it only in the tests of
    that offer, and restore it in `@After`.
  - **Never take `SCHEDULE_EXACT_ALARM` away during a test:** Android stops
    the app, test and all.
- **Writing a test that saves data:**
  - Use titles of the test's own, keep a list of them, and in `@After` cancel
    each item's reminders (`ReminderScheduler.cancelEvent`,
    `cancelVacation`) and delete only those rows, as
    `HomeFlowInstrumentedTest` does.
  - Open the database with `AppDatabase.open()` and close it in `@After`.
  - Never clear tables.
  - Pass data through the same literal Intent extra keys the screens use
    (`vacationId`, `title`, `details`, `startDate`, `endDate`, `eventId`).
- **Date fields are pick-only:** set them with `setDate` and check them with
  `withDate` (`DateFieldTesting.kt`), never by typing text.
- **The tests use the debug build, without R8.** Release-only behaviour needs
  the release build on an emulator (`docs/05-release.md`, step 3).
- **CI** runs `connectedAndroidTest` on an API 37 `google_apis` emulator.
  CI can only be checked by pushing, which the user does: never report a CI
  result you haven't seen.
- **Report** which classes you ran, on which emulator and API level, and the
  real result. A test that can't run here (a device-only condition, or no
  emulator) is reported as not run, with the reason.
