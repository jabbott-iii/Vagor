# Vagor — an offline vacation planner for Android (Kotlin)

<!--
Maintainers: the Status paragraph and the requirement statuses below are
point-in-time (2026-11-07). Update them as things change.
-->

**Status:** the app is built and tested, and not yet on Google Play (as of
2026-11-07). Every product requirement is met in the app itself. What
remains is the developer's release work (`docs/05-release.md`, steps 1–8),
which an agent can't do: the upload key, hosting PRIVACY.md and LICENSE at
public URLs, a payments profile, creating the app as paid at US$1 and
answering Play Console's declarations (trader status, App content, Data
safety), entering the listing from `fastlane/metadata/`, then an internal
test, a 12-tester, 14-day closed test (closed testers must buy the app),
and the application for production access. `docs/roadmap.md` ticks
milestones M0 to M5, which are done. M6 is that release work, and M7 holds
the decisions the user hasn't made yet and the backlog. Everything this
document names exists as of 2026-11-07: check before relying on one
anyway.

## What we are building

- An Android app for planning vacations and the events in them. It runs
  entirely on the phone, with no backend, no accounts and no network
  (ADR-0001). A single `:app` module in Kotlin, ported from
  `jabbott-iii/university-projects` →
  `Mobile-Application-Development-Android`.
- **Identity:** the application ID, the Play and device identity, is
  `io.github.jabbott_iii.vagor` (ADR-0020). The Kotlin package and
  `namespace` are still `com.example.vagor`.
- **Stack:** XML layouts, AppCompat, Material 3 and one Room database
  (ADR-0002, ADR-0003). minSdk 26 (Android 8.0), targetSdk 36, compileSdk
  37 (`docs/03-platforms.md`).
- **License and sale:** proprietary, all rights reserved by Joseph Anthony
  Abbott III (ADR-0018). `LICENSE` is the Vagor Perpetual End-User License
  Agreement, adapted from the user's Denarii Dolor app (ADR-0034), and
  README's License section holds the GitHub view-and-fork exception.
  Vagor is to be sold on Google Play for US$1 (ADR-0035). `NOTICE` lists
  the third-party software that ships in the app.
- **Language:** English only (ADR-0036).
- **Product requirements** (they define "done" for feature work; the full
  table is in `docs/00-overview.md`):

| # | Requirement | Status | See |
| :- | :- | :- | :- |
| 1 | **Vacations:** create, edit and delete, with a title, trip details, a start date and an end date. Trip details replaced the hotel field on 2026-10-26: free text over several lines, and optional | Implemented | ADR-0012 |
| 2 | **Events:** each on a date inside its vacation's date range, at a required time picked from a clock, in a time zone picked from a searchable list that starts on the phone's zone, with optional details. Events saved before 2026-10-27 have no time until they're next edited, and can't be saved again without one | Implemented | ADR-0013 |
| 3 | **Reminders:** automatic notifications around 9:00 AM local time on each vacation's start and end dates, and an hour before each event. A posted reminder leaves the shade once it's out of date | Implemented | ADR-0005, ADR-0014, ADR-0027, ADR-0031 |
| 4 | **Sharing:** plain text through the Sharesheet, for a vacation (listing its events) and for each event. Add to Calendar opens Google Calendar's new-event screen, or another calendar app's. Importing a shared trip isn't supported | Implemented | ADR-0009, ADR-0015 |
| 5 | **Local-only:** rule 1 below | Holds | ADR-0001, ADR-0004 |
| 6 | **Dark mode:** an on/off switch on the Settings tab, on by default, not a follow-system option | Implemented | ADR-0006 |
| 7 | **Accessible colours:** a blue Material 3 scheme for colour-blind users, from the Okabe-Ito blue `#0072B2`. Every text/background pair meets WCAG AA in both themes, nothing is conveyed by colour alone, and keyboard focus is visible | Implemented; `ThemeContrastTest` enforces the contrast | ADR-0017 |
| 8 | **Google Play compliance:** what the app needs is in it. The Play Console steps are the developer's to do | App side done; Play Console steps open (M6) | `docs/05-release.md` |
| 9 | **Dashboard and navigation:** the app opens on every vacation with its dates. Current and upcoming ones come first, soonest at the top; ended ones go below under "Past", most recent first. A blue Add Vacation button; a bottom navigation bar between the Vacations and Settings tabs; tapping a vacation opens its page | Implemented | ADR-0007 |
| 10 | **Calendar date entry:** every date field opens a month calendar that drops down below it, and dates can't be typed. The end date's calendar starts at the start date; an event's offers only its vacation's days. Time and time zone fields can't be typed into either | Implemented | ADR-0008 |

- Events were called excursions until 2026-10-25. The UI and code now say
  **events**, but the Room table keeps its old name `excursions`
  (ADR-0010).
- **Out of scope:** network access of any kind, accounts, sync, cloud
  backup, ads, analytics and crash reporting (ADR-0001, ADR-0004);
  importing a trip someone shared (whether to add it is still open,
  `docs/roadmap.md` M7); reading or writing the phone's calendar itself
  (Add to Calendar hands one item to a calendar app); languages other than
  English (ADR-0036); tablet layouts (Vagor's layouts are phone layouts,
  stretched across a tablet).

## Read before working

| If you are touching… | Read first |
| :- | :- |
| Anything | `docs/00-overview.md`, `docs/01-architecture.md` |
| `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `gradle/`, `gradle.properties`, keep rules, a new library | `docs/02-dependency-policy.md` |
| `entities/`, `dao/`, `database/`, `app/schemas/`, stored formats in `DateValidators.kt`, `res/xml/` backup rules | `docs/subsystems/data-and-storage.md` |
| `HomeActivity`, `VacationsFragment`, `EventListActivity`, `VacationDetailActivity`, `EventDetailActivity`, `Dashboard.kt`, `EdgeToEdge.kt` | `docs/subsystems/screens-and-navigation.md` |
| `SettingsFragment`, `ThemeSettings.kt`, `MessageDialog.kt`, `LicenseText.kt` | `docs/subsystems/settings-screen.md` |
| `DateValidators.kt`, `DateField.kt`, `CalendarDropdown.kt`, `CalendarMonth.kt`, `CalendarDayBackground.kt`, `PickOnlyField.kt`, `TimeZonePicker.kt`, `TripText.kt`, `EventTimes.kt` | `docs/subsystems/dates-times-and-pickers.md` |
| `ReminderSchedule.kt`, `ReminderScheduler.kt`, `MyReceiver.kt`, `ReminderRescheduleReceiver.kt`, `VagorApplication.kt`, `ExactAlarmOffer.kt`, `NotificationsOff.kt`, `Hibernation.kt` | `docs/subsystems/reminders.md`, `docs/03-platforms.md` |
| Share buttons, `CalendarInsert.kt` | `docs/subsystems/sharing-and-calendar.md` |
| Themes, colours, styles, dimens, layouts, drawables, the launcher icon | `docs/subsystems/theme-and-accessibility.md` |
| `AndroidManifest.xml`, behaviour that differs by Android version | `docs/03-platforms.md`, rule 1 below |
| `PRIVACY.md`, `privacy_policy_text`, `LICENSE` | Rule 3 below, `docs/05-release.md` (step 4), ADR-0019, ADR-0034 |
| `NOTICE`, `third_party_licenses_text`, `res/raw/` | `docs/02-dependency-policy.md`, ADR-0029 |
| `fastlane/metadata/` (listing, graphics, screenshots) | `docs/05-release.md` (step 6) |
| Tests, lint, detekt, ktlint, `.github/workflows/` | `docs/04-quality.md` |
| Release builds, signing, versions, Play Console | `docs/05-release.md` |
| What is done and what is next | `docs/roadmap.md` |
| Why something is the way it is | `docs/decisions.md` |
| A security report | `SECURITY.md` |

## Non-negotiable rules

1. **Local-only.** Never break this (ADR-0001).
   - No network access of any kind. Don't add `INTERNET` or
     `ACCESS_NETWORK_STATE`, HTTP clients, remote WebView content,
     Firebase, analytics, crash reporting, ads, accounts or sync.
   - The manifest declares only `POST_NOTIFICATIONS`,
     `RECEIVE_BOOT_COMPLETED` and `SCHEDULE_EXACT_ALARM`. After adding any
     dependency, check the merged manifest for permissions it pulls in.
   - Data leaves the app only when the user shares it through the Android
     Sharesheet, or hands one vacation or event to a calendar app with Add
     to Calendar.
   - Cloud backup stays off: `allowBackup="false"` plus the exclusions in
     `backup_rules.xml` and `data_extraction_rules.xml`. A direct
     device-to-device transfer to a new phone is still allowed (ADR-0004).
   - Play's Data safety answer ("no data collected or shared") and
     PRIVACY.md depend on this rule.
2. **The user's trips have one copy.** All user data lives in the Room
   database file `vacation_database`, and there is no other copy
   (ADR-0003).
   - Never rename that file, never use `fallbackToDestructiveMigration`,
     and never clear tables outside tests.
   - A schema change bumps `AppDatabase.version`, adds a `Migration` to
     `AppDatabase.MIGRATIONS`, keeps the schema JSON Room exports to
     `app/schemas` (it's in git), and tests the upgrade in
     `MigrationInstrumentedTest` with `MigrationTestHelper`
     (`/change-database-schema`).
   - Everything that opens the database does so through
     `AppDatabase.open()`, which adds every migration. A
     `Room.databaseBuilder` without them would crash on an older database.
   - The table `excursions` and the column `hotel` keep their names
     (ADR-0010, ADR-0012). Don't rename a table or column without a
     migration.
   - The only other stored state is the dark mode switch, in the
     SharedPreferences file `settings`.
3. **What the app promises stays true.**
   - The privacy policy exists twice: PRIVACY.md and `privacy_policy_text`
     in `strings.xml`. Change both together, keep them identical, and keep
     them true to what the app does. Its contact is
     jabbottpublicsupport@gmail.com (ADR-0019).
   - `LICENSE` says how the app handles data (section 5) and that its
     reminders can be late or stopped (section 6). Keep those true too.
   - The store listing in `fastlane/metadata/android/en-US/` stays true to
     what the app does. Retake a screenshot when the screen it shows
     changes (`/update-store-listing`).
   - Vagor itself is proprietary. Don't put its own code under an
     open-source license (ADR-0018).
4. **Instrumented tests run only on an emulator or a test device**, never
   on a phone that holds real trips.
   - Five of them write to the app's real `vacation_database`, four set
     real alarms, and one posts real notifications. A run killed partway
     can leave a test trip, or 520 test alarms, behind.
   - A `connectedDebugAndroidTest` run uninstalls the app afterwards,
     which deletes its data.
   - Never take `SCHEDULE_EXACT_ALARM` away during a test: Android stops
     the app, test and all.
   - Every side effect is listed in `docs/04-quality.md`.
5. **Never change `applicationId`.** After the first Play upload a new ID
   is a different app (ADR-0020).
6. **No version changes and no new libraries without approval.**
   - Don't change dependency, AGP, Gradle, Kotlin or SDK versions unless
     asked.
   - Don't add a library unless the user approves it. Write the evaluation
     `docs/02-dependency-policy.md` describes first
     (`/evaluate-dependency`).
   - After adding any dependency, check the merged manifest for
     permissions it pulls in. Third-party material that ships in the app
     goes into `NOTICE` and `third_party_licenses_text`
     (`ThirdPartyLicensesTest` checks they list the same things); one
     under another license than Apache 2.0 also needs that license's text.
7. **No new frameworks without approval.** The code uses no Compose,
   ViewBinding, ViewModel, DI framework or coroutines. Don't introduce them
   without the user's approval (ADR-0002). A shared database instance or
   background threading is a refactor: propose it first.
8. **Permissions Google Play restricts stay out.** Never add
   `USE_EXACT_ALARM`: Google Play allows it only for calendar and
   alarm-clock apps. Never add `READ_CALENDAR` or `WRITE_CALENDAR`, and
   never write to the calendar provider directly.
9. **Git is read-only for agents.**
   - Never commit, amend, rebase, merge, tag, push, open PRs or publish
     anything. Leave all changes uncommitted for review.
   - Never run `make -f Makefile.txt tag`, `push-tag` or `release`: they
     create and push git tags.
   - `local.properties` (machine-specific `sdk.dir`) is tracked in git
     even though its own header says it shouldn't be. Never commit changes
     to it.
10. **No secrets.** Never write a keystore, password or token into the
    repository, and never commit one. Release signing reads the upload key
    only from four environment variables, and signed builds run with
    `--no-configuration-cache` (`docs/05-release.md`, step 3; ADR-0030).
11. **Never add findings to the detekt baseline**
    (`app/detekt-baseline.xml`) to make a check pass (ADR-0022).
12. **Honest verification.**
    - Never state that a build, test or check passed unless you ran it in
      this session and saw it pass.
    - Report exactly what you ran, and what you didn't.
    - If a change can only be checked on a device, say that it wasn't
      verified.
    - CI can only be checked by pushing, so never report a workflow change
      as passing until a run on GitHub shows it.
13. **Smallest complete change.**
    - No unrelated refactors, renames or reformatting.
    - Unless the task explicitly calls for it, don't change `namespace`,
      the database schema, manifest permissions, backup settings, or the
      CI workflows.
14. **Product decisions are the user's.** Reminder times and behaviour,
    which reminders get the 500 alarm places past Android's limit (and
    whether the user is told), importing shared trips, and the open
    decisions in `docs/roadmap.md` (M7) are the user's call: ask, don't
    decide. Record a decision the user makes, with its date, in
    `docs/decisions.md` (`/record-decision`).

## Code map

Kotlin files are in `app/src/main/java/com/example/vagor/`.

| Path | Purpose | Spec |
| :- | :- | :- |
| `HomeActivity.kt` | The launcher activity: a bottom navigation bar and two tabs. Rebuilds the reminders on resume if exact alarms were allowed or taken away since | `docs/subsystems/screens-and-navigation.md` |
| `VacationsFragment.kt` | The Vacations tab: the dashboard (`ListView` of `Dashboard.rows`), the Add Vacation button, the notifications-off dialog | `docs/subsystems/screens-and-navigation.md` |
| `SettingsFragment.kt` | The Settings tab | `docs/subsystems/settings-screen.md` |
| `EventListActivity.kt` | A vacation's page: its details, Edit Vacation, Share Vacation and Add to Calendar buttons, and its events | `docs/subsystems/screens-and-navigation.md` |
| `VacationDetailActivity.kt` | The add/edit vacation form | `docs/subsystems/screens-and-navigation.md` |
| `EventDetailActivity.kt` | The add/edit event form | `docs/subsystems/screens-and-navigation.md` |
| `EdgeToEdge.kt` | `setEdgeToEdgeContentView()` and `setEdgeToEdgeContentViewWithBottomBar()` | `docs/subsystems/screens-and-navigation.md` |
| `Dashboard.kt` | Plain-Kotlin dashboard order and trip status | `docs/subsystems/screens-and-navigation.md` |
| `MessageDialog.kt` | A message with an OK button, as a `DialogFragment` | `docs/subsystems/settings-screen.md` |
| `ThemeSettings.kt` | The dark mode switch, kept in the SharedPreferences file `settings` | `docs/subsystems/settings-screen.md` |
| `LicenseText.kt` | Reflows the license text for the Third-Party Licenses dialog | `docs/subsystems/settings-screen.md` |
| `entities/` | `Vacation` and `Event`; `Event` maps to the table `excursions`, `Vacation.details` to the column `hotel` | `docs/subsystems/data-and-storage.md` |
| `dao/` | `VacationDAO` and `EventDAO`, synchronous | `docs/subsystems/data-and-storage.md` |
| `database/AppDatabase.kt` | The Room database (version 2), `open()` and `MIGRATIONS` | `docs/subsystems/data-and-storage.md` |
| `DateValidators.kt` | Strict parsing and formatting of stored dates, times and zones; `isOutsideVacation` | `docs/subsystems/dates-times-and-pickers.md` |
| `TripText.kt` | Dates, times and status as display text (`dateText`) | `docs/subsystems/dates-times-and-pickers.md` |
| `EventTimes.kt` | Plain-Kotlin event start times and order | `docs/subsystems/dates-times-and-pickers.md` |
| `PickOnlyField.kt` | `makePickOnly()`: fields filled only from a picker | `docs/subsystems/dates-times-and-pickers.md` |
| `DateField.kt` | A date field: the stored date, shown the phone's way | `docs/subsystems/dates-times-and-pickers.md` |
| `CalendarDropdown.kt` | The date fields' drop-down calendar (`attachCalendarDropdown()`) | `docs/subsystems/dates-times-and-pickers.md` |
| `CalendarMonth.kt` | Plain-Kotlin month grid for the calendar | `docs/subsystems/dates-times-and-pickers.md` |
| `CalendarDayBackground.kt` | The calendar days' circles and rings, drawn to fit their cells | `docs/subsystems/dates-times-and-pickers.md` |
| `TimeZonePicker.kt` | The event form's searchable time zone list (`TimeZonePickerDialog`), labelled from Android's ICU time zone names | `docs/subsystems/dates-times-and-pickers.md` |
| `CalendarInsert.kt` | The Add to Calendar intents, with their plain-Kotlin times in `CalendarTimes` | `docs/subsystems/sharing-and-calendar.md` |
| `ReminderSchedule.kt` | Plain-Kotlin reminder times (`ReminderTimes`) and identities (`ReminderKey`) | `docs/subsystems/reminders.md` |
| `ReminderScheduler.kt` | Sets and cancels alarms | `docs/subsystems/reminders.md` |
| `MyReceiver.kt` | The alarm receiver; posts the notification | `docs/subsystems/reminders.md` |
| `ReminderRescheduleReceiver.kt` | Rebuilds the reminders after a reboot, an app update, a clock or time zone change, and when exact alarms are allowed | `docs/subsystems/reminders.md` |
| `ExactAlarmOffer.kt` | Offers Android's Alarms & reminders screen (`ExactAlarmOfferDialog`) | `docs/subsystems/reminders.md` |
| `NotificationsOff.kt` | The notifications-off dialog and the way to Android's notification settings | `docs/subsystems/reminders.md` |
| `Hibernation.kt` | Whether Android may pause an unused Vagor, the way to its App info, and the offer after saving a trip far ahead (`HibernationOfferDialog`) | `docs/subsystems/reminders.md` |
| `VagorApplication.kt` | Applies the saved theme before any screen and rebuilds the reminders in each new process | `docs/subsystems/reminders.md` |
| `app/src/main/AndroidManifest.xml` | The three permissions, the activities and the two receivers | `docs/03-platforms.md` |
| `app/src/main/res/` | Layouts, `values/strings.xml`, themes (`values/`, `values-night/`), styles, dimens, colours, drawables (the launcher icon), `raw/apache_license_2_0.txt`, `xml/` backup rules | `docs/subsystems/theme-and-accessibility.md` |
| `app/src/main/keepRules/` | R8 keep rules (`*.keep`); none are needed today | `docs/05-release.md` |
| `app/schemas/` | Room's exported schema JSON for each database version, in git | `docs/subsystems/data-and-storage.md` |
| `app/src/test/` | JVM unit tests | `docs/04-quality.md` |
| `app/src/androidTest/` | Instrumented (Espresso) tests and their helpers `DateFieldTesting.kt` and `OtherAppTesting.kt` | `docs/04-quality.md` |
| `app/detekt-baseline.xml` | What detekt found in the code on 2026-10-31 | `docs/04-quality.md` |
| `fastlane/metadata/android/en-US/` | The store listing: title, descriptions, release notes, Play graphics, phone screenshots (nothing uses fastlane) | `docs/05-release.md` |
| `.github/workflows/` | `ci.yml` and `security.yml` | `docs/04-quality.md` |
| `Makefile.txt` | Creates and pushes git tags: for the developer, never agents (rule 9) | `docs/05-release.md` |
| `PRIVACY.md`, `LICENSE`, `NOTICE` | The privacy policy, the end-user license agreement, the third-party notices | rule 3 |

## Commands (verify one exists before relying on it)

| Command | What it does |
| :- | :- |
| `./gradlew assembleDebug` | Builds the debug APK |
| `./gradlew testDebugUnitTest` | JVM tests in `app/src/test` |
| `./gradlew testDebugUnitTest --tests 'com.example.vagor.DateValidatorsTest'` | One JVM test class |
| `./gradlew lint` | Android lint |
| `./gradlew detekt ktlintCheck` | Kotlin static analysis and style |
| `./gradlew ktlintFormat` | Fixes most ktlint findings |
| `./gradlew connectedDebugAndroidTest` | Espresso tests; needs an emulator or a test device (rule 4), and uninstalls the app afterwards |
| `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.vagor.HomeFlowInstrumentedTest` | One instrumented test class |
| `./gradlew bundleRelease` | The release bundle, run through R8. Unsigned while `VAGOR_UPLOAD_KEYSTORE` is unset or blank, as CI builds it |
| `./gradlew --no-configuration-cache clean bundleRelease` | The signed bundle, for the developer (`docs/05-release.md`, step 3) |
| `make -f Makefile.txt tag VERSION=v1.0.0` | For the developer, never agents (rule 9): tags HEAD. `push-tag` pushes the tag; `release` does both |

Nothing in the repository uploads or publishes. There is no release
workflow (`cd.yml` was removed at the user's request on 2026-10-25), so
releases are built, signed and uploaded by hand (`docs/05-release.md`,
ADR-0011). `Makefile.txt` only tags, and nothing is built or uploaded from
a tag.

## Conventions

- **Toolchain:** AGP 9.4.1 with built-in Kotlin (Kotlin 2.4.20, set through
  the root `buildscript` classpath), KSP 2.3.12 for Room, Gradle 9.6.1,
  compileSdk 37 and targetSdk 36 (the Google Play minimum since
  2026-08-31). Gradle runs on JDK 17 or later, and
  `gradle/gradle-daemon-jvm.properties` asks for a JDK 25 daemon. The build
  needs an Android SDK with platform 37, found through `ANDROID_HOME` or
  `sdk.dir` in `local.properties`.
- **UI code:** XML layouts, `AppCompatActivity` (plus a `Fragment` for each
  home screen tab), `findViewById`, and `ListView` with `ArrayAdapter` for
  the dashboard. The vacation page shows its few events as plain rows in a
  `ScrollView`, so focus follows the screen.
- **Database access:** every Activity, Fragment or receiver that reads or
  writes data opens its own Room instance with `AppDatabase.open()`
  (main-thread queries allowed) and closes it again: screens in
  `onDestroy`, receivers in `finally`. DAOs are synchronous. Follow that
  pattern in small changes (rule 7).
- **Schema names and nulls:** `Event` maps to the table `excursions`
  (`@Entity(tableName = "excursions")`), which `EventDAO` queries, and
  `Vacation.details` (trip details) to the old column `hotel`
  (`@ColumnInfo(name = "hotel")`). `Event.details` is blank when empty.
  `Event.time` and `Event.timeZone` are null on events saved before
  database version 2 (2026-10-27, `MIGRATION_1_2`), and every save since
  stores both.
- **Intent extras:** screens pass data through extras with literal keys
  (`vacationId`, `title`, `details`, `startDate`, `endDate`, `eventId`).
  The instrumented tests and the reminder notifications use the same keys.
  - The vacation form prefills from the extras.
  - The event form takes only `eventId` (none for a new event) and
    `vacationId`, and reads the event from the database, because a
    reminder's extras are copied when it's posted and Save would write old
    values back. It closes itself if the event was deleted.
  - The vacation page (`EventListActivity`) takes only `vacationId`,
    reloads the vacation from the database on every resume, and closes
    itself if the vacation was deleted.
- **Stored dates, times and zones:**
  - Dates are `MM/dd/yyyy` strings. Parse and validate them only through
    `DateValidators`, which is strict: exactly two-digit month and day, a
    four-digit year, a real calendar date, nothing before or after.
  - Event times are 24-hour `HH:mm` strings and time zones IANA zone IDs
    such as `Asia/Tokyo`. Parse them only through
    `DateValidators.parseLocalTime` and `parseZone`.
  - Saved rows may hold older loose dates, so the save-time checks still
    run even though the fields are filled only from pickers.
  - Put new date or reminder math in plain Kotlin beside `DateValidators`
    so it can be unit tested (`java.time` is available at minSdk 26).
- **Picking dates, times and zones:**
  - Date fields are `DateField`s, filled only from the drop-down calendar
    (`attachCalendarDropdown()` in `CalendarDropdown.kt`), which writes
    through `DateValidators.formatDate`. A `DateField`'s `date` holds the
    stored value and its text is only for show: read and write `date`,
    never the text. It keeps its date across rotation itself.
  - An event's time comes from Material's clock (`MaterialTimePicker`, in
    the phone's 12- or 24-hour style) and its zone from
    `TimeZonePickerDialog`. The event form keeps both in fields of its own,
    saved across rotation. All three kinds of field use `makePickOnly()`.
  - Vagor draws its calendar itself. Don't switch to the framework
    `CalendarView`: it follows the phone's calendar system (a Thai phone
    would store Buddhist-era years) and marks today by colour alone. The
    calendar is always Gregorian and marks today with a ring.
- **Showing dates and times:**
  - Dates are shown the way the phone writes them: "Jul 6, 2031" in the
    US, "6 Jul 2031" in the UK (ADR-0026). `dateText` in `TripText.kt`
    formats a stored date with ICU's `yMMMd` skeleton, always in the
    Gregorian calendar, and shows a date it can't read as it was stored.
  - Everything a person reads goes through it: screens, reminders, shared
    text and calendar descriptions. Never show a stored date string
    directly.
  - The event form shows the time in the phone's 12- or 24-hour style and
    the zone by name and city ("Japan Standard Time (Tokyo)", so it isn't
    read as the event's place).
  - Display text uses ICU (`android.icu`), which knows the phone's own
    names and patterns; `java.time` formatting is for stored values. Check
    each ICU call's API level against minSdk 26: some, such as
    `SystemTimeZoneType.CANONICAL_LOCATION`, arrived later.
- **Validation enforced today:**
  - Every field except trip details and event details is required; an
    event's time zone always has a value.
  - A vacation's end date is on or after its start date.
  - An event's date falls within its vacation's start and end dates,
    inclusive, and an edited vacation's new dates must still hold its
    events (`DateValidators.isOutsideVacation`, ADR-0028). An event with an
    old loose date can't be checked and doesn't block the save.
  - A vacation that has events can't be deleted.
  - The last two checks exist only in `VacationDetailActivity`, because
    `excursions.vacationId` has no foreign key.
- **Text:** user-visible text goes in `res/values/strings.xml`. The app is
  English only, and `androidResources.localeFilters` keeps the libraries'
  translations out, so their text (the time picker's, for example) stays
  English too. Adding a language means adding it there.
- **Colours:** from theme attributes only (`res/values/colors.xml`,
  `values/themes.xml`, `values-night/themes.xml`). Never hard-code a text
  or background colour in a layout. Don't convey anything by colour alone,
  and don't add DynamicColors: it would replace the blue with wallpaper
  colours. Every text/background pair must meet WCAG AA (4.5:1; 3:1 for
  non-text UI) in both themes, which `ThemeContrastTest` enforces
  (ADR-0017).
- **Focus:** keyboard focus must be visible. The theme turns on Material's
  focus rings (`focusRingsEnabled`, not yet public API), the dashboard and
  time zone lists have their own focus rings, and the calendar's days have
  one. Material's time picker marks its selected AM/PM by hue alone, so
  `ThemeOverlay.Vagor.MaterialTimePicker` fills it with the primary blue.
- **Buttons:** every button on a page is the same size (ADR-0016): full
  width, one above the other, styled `Widget.Vagor.Button` or
  `Widget.Vagor.Button.Outlined` with `layout_width="match_parent"` and
  `layout_height="wrap_content"`. The style sets one height
  (`@dimen/button_height`), which grows with the font size. Only the
  floating Add Vacation and Add Event buttons differ
  (`Widget.Vagor.AddButton`).
- **Text fields:** `wrap_content` with `minHeight="57dp"`, never a fixed
  height, so they grow with the font size, and `importantForAutofill="no"`,
  so autofill services leave trip text alone.
- **Touch targets:** at least 48dp each way. The calendar's days are at
  least 48dp tall at any font size. Where a field is too narrow for seven
  48dp days (a 360dp-wide phone), the calendar is wider than its field, up
  to the screen's width less 8dp on each side; on narrower screens the days
  are as wide as that allows.
- **Titles and headings:** every screen but `HomeActivity` sets its own
  title, which TalkBack announces when the screen opens: Add Vacation or
  Edit Vacation, Add Event or Edit Event, and the vacation's title on its
  page. Each form's heading shows the same words and, like every heading,
  sets `accessibilityHeading`. Never give `HomeActivity` an
  `android:label`: on the launcher activity it would rename the app's icon.
- **Dialogs:** every dialog is a `DialogFragment` (`MessageDialog` for a
  message with an OK button), so it's still open after a rotation. A dialog
  shown straight from `MaterialAlertDialogBuilder` closes when the screen
  is recreated (ADR-0033). The notifications-off and "no calendar app"
  messages are dialogs, not toasts, because Android cuts toasts at two
  lines. A form that closes after a save disables its buttons first, so a
  second tap, or one behind an offer that keeps it open, can't save twice.
- **Edge to edge:**
  - Every screen except `HomeActivity` calls `setEdgeToEdgeContentView()`
    instead of `setContentView()`, so content stays clear of the system
    bars (required at targetSdk 36). While the keyboard is open, that
    helper also ends the root view at the keyboard's top edge.
  - The two detail forms are in a `ScrollView` with `adjustResize`, so
    every field and button can be scrolled into view in landscape and with
    the keyboard open (a full-screen landscape keyboard still covers the
    form until it is closed).
  - `HomeActivity` uses `setEdgeToEdgeContentViewWithBottomBar()`. It pads
    the tab content for the status bar and the sides, and the bottom bar
    for the navigation bar and the sides, so the bar's background reaches
    behind the navigation bar with no scrim. It replaces the bar's own
    inset handling, which would add the keyboard's height, and it does
    nothing for the keyboard: a text field added to a home tab would need
    its own keyboard handling.
- **Reminders:** keep scheduling in `ReminderScheduler`; the detail screens
  call it on every save and delete. Every alarm goes through
  `ReminderScheduler.setAlarm`. Check `canScheduleExactAlarms()` before each
  exact alarm and catch `SecurityException`. The rest is in
  `docs/subsystems/reminders.md`.
- **Sharing:** only through the Sharesheet: `Intent.ACTION_SEND` inside
  `Intent.createChooser`, as `EventListActivity` does, with text built from
  string resources such as `vacation_share_template`. No links to servers
  and no accounts. A shared file (an `.ics` file, for example) goes through
  a `FileProvider` `content://` URI with `FLAG_GRANT_READ_URI_PERMISSION`,
  never a `file://` URI. Add to Calendar needs no `<queries>` entry; it
  catches `ActivityNotFoundException` instead
  (`docs/subsystems/sharing-and-calendar.md`).
- **R8:** release builds run R8; debug builds and the instrumented tests
  don't. Check a change that relies on reflection, class names or
  resources looked up by name in a release build on an emulator. Keep
  rules go in `app/src/main/keepRules/*.keep`; none are needed today
  (ADR-0021).
- **Style:** ktlint uses the IntelliJ IDEA code style without trailing
  commas, wrapping at 120 characters, and has no baseline:
  `./gradlew ktlintFormat` fixes most findings. detekt uses its default
  rules, with lines up to 120 characters (ADR-0022).
- **License headers:** Vagor's own files carry none (ADR-0018).
- **Launcher icon:** keep its foreground
  (`drawable/ic_launcher_foreground.xml`) one flat colour with no shadow:
  it is also the monochrome layer for themed icons. The Play icon is drawn
  from the same paths, so redraw it when the icon changes
  (`docs/subsystems/theme-and-accessibility.md`).
- **Tests:**
  - JVM unit tests live in `app/src/test/`; instrumented (Espresso) tests
    in `app/src/androidTest/`.
  - Instrumented tests set a `DateField` with `setDate` and check it with
    `withDate` (`DateFieldTesting.kt`), and leave another app's screen
    through `leaveOtherAppsScreen()` (`OtherAppTesting.kt`).
  - `docs/04-quality.md` says what each test covers and what it changes on
    the device.

## Definition of done

1. The change does what the task asks, and every product requirement and
   rule it touches still holds.
2. The checks that fit the change were run, and each command's real result
   is reported. CI runs `lint`, `detekt`, `ktlintCheck`, `assembleDebug`,
   `bundleRelease`, `testDebugUnitTest` and `connectedAndroidTest`
   (`docs/04-quality.md`); instrumented tests run only as rule 4 allows.
3. A `docs/roadmap.md` box is ticked only after steps 1 and 2.
4. Docs are updated if behaviour or design changed: the subsystem spec,
   this file for a rule or convention, both copies of the privacy policy,
   the store listing and its screenshots, `NOTICE` and
   `third_party_licenses_text`. A decision the user made gets an ADR in
   `docs/decisions.md`.
5. The final message lists:
   - files changed
   - commands run and their results
   - checks not run, and why (a change that can only be checked on a
     device is reported as not verified)
   - assumptions
   - open risks

   It ends with a suggested commit message. The user commits (rule 9).
