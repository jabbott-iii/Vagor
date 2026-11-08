# Architecture decision records

New ADRs are appended at the end using the template below; `/record-decision`
does this for you. Never edit an accepted ADR's decision. Supersede it with a
new ADR and set the old one's status to `Superseded by ADR-NNNN`.

An ADR is `Accepted` only when the user made or approved the decision.
Everything else is `Proposed` until the user says otherwise. ADR-0001 to
ADR-0036 were written down on 2026-11-07 from the project's earlier notes
(the old `CLAUDE.md`, `README.md` and `PLAY_RELEASE.md`) and its git
history. Each records a decision the user made or approved, so all of them
are `Accepted`. Where those notes don't say when or why, the field says
"Not recorded." A date is the day the user made or asked for the decision,
which can be a day before the commit that built it (`docs/roadmap.md`).

```markdown
## ADR-NNNN — <title>

- **Status:** Proposed | Accepted | Rejected | Superseded by ADR-NNNN
- **Date:** YYYY-MM-DD
- **Context:** <forces at play, constraints, what prompted this>
- **Decision:** <what we will do>
- **Consequences:** <good and bad results, follow-up work>
- **Alternatives considered:** <option — why not>
- **Revisit when:** <a concrete trigger>
```

---

## ADR-0001 — Offline and local-only: no network, accounts or data collection

- **Status:** Accepted
- **Date:** Not recorded
- **Context:** Vagor is a vacation planner that runs entirely on the phone,
  with no backend and no accounts. Play's Data safety answer ("no data
  collected or shared") and `PRIVACY.md` depend on it staying that way.
- **Decision:**
  - No network access of any kind. No `INTERNET` or
    `ACCESS_NETWORK_STATE`, HTTP clients, remote WebView content, Firebase,
    analytics, crash reporting, ads, accounts or sync.
  - The manifest declares only `POST_NOTIFICATIONS`,
    `RECEIVE_BOOT_COMPLETED` and `SCHEDULE_EXACT_ALARM`.
  - Data leaves the app only when the user shares it through the Android
    Sharesheet (ADR-0009), or hands one vacation or event to a calendar app
    with Add to Calendar (ADR-0015).
  - After adding any dependency, check the merged manifest for permissions
    it pulls in (`docs/02-dependency-policy.md`).
- **Consequences:**
  - Play's Data safety answer is that no data is collected or shared. Play
    counts data as collected only when it's sent off the device, and it
    exempts transfers the user starts and expects (`docs/05-release.md`,
    step 5).
  - The privacy policy exists twice, `PRIVACY.md` and `privacy_policy_text`
    in `strings.xml`, and both say this. `LICENSE` section 5 says how the app
    handles data. All of them have to stay true to what the app does.
  - The bundle also lists AndroidX's app-internal
    `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which needs no declaration,
    and has no `AD_ID` permission.
  - Vagor has no crash reporting of its own, so Android vitals in Play
    Console is the only signal from the field (`docs/05-release.md`, step 8).
  - No sync, no cloud backup (ADR-0004), and no import of shared trips.
- **Alternatives considered:** Not recorded.
- **Revisit when:** Not recorded.

## ADR-0002 — Kotlin with XML views and AppCompat; no Compose, ViewBinding, ViewModel, DI or coroutines without approval

- **Status:** Accepted
- **Date:** Not recorded
- **Context:** Vagor was ported from `jabbott-iii/university-projects` →
  `Mobile-Application-Development-Android` as a Kotlin Android app (pull
  request #4, merged 2026-09-16), keeping the original Room-backed
  persistence, and reconfigured as a full Kotlin app on 2026-10-25
  (`docs/roadmap.md`, M0 and M1).
- **Decision:**
  - One `:app` module, in Kotlin.
  - UI is XML layouts, `AppCompatActivity` (plus a `Fragment` for each
    home screen tab), `findViewById`, and `ListView` with `ArrayAdapter`
    for the dashboard. The vacation page shows its few events as plain rows
    in a `ScrollView`, so focus follows the screen.
  - No Compose, ViewBinding, ViewModel, DI framework or coroutines in the
    code without the user's approval.
  - Every Activity, Fragment or receiver that reads or writes data opens
    its own Room instance with `AppDatabase.open()` (main-thread queries
    allowed) and closes it again: screens in `onDestroy`, receivers in
    `finally`. DAOs are synchronous.
- **Consequences:**
  - Small changes follow the same patterns. A shared database instance or
    background threading is a refactor: propose it first (`docs/roadmap.md`,
    M7).
  - kotlinx.coroutines still ships, through Room (`NOTICE`), and the build
    leaves out its `DebugProbesKt.bin`, which only its debugger uses.
- **Alternatives considered:** Not recorded.
- **Revisit when:** the user approves one of these technologies, or the
  shared-instance and threading refactor.

## ADR-0003 — One Room database file, changed only by migrations

- **Status:** Accepted
- **Date:** Not recorded
- **Context:** All user data lives in the Room database file
  `vacation_database`, and there is no other copy: cloud backup is off
  (ADR-0004). A `Room.databaseBuilder` without the migrations would crash
  on an older database.
- **Decision:**
  - Never rename that file, never use `fallbackToDestructiveMigration`,
    and never clear tables outside tests.
  - For a schema change, bump `AppDatabase.version`, add a `Migration` to
    `AppDatabase.MIGRATIONS`, keep the schema JSON Room exports to
    `app/schemas` (it's in git), and test the upgrade in
    `MigrationInstrumentedTest` with `MigrationTestHelper`
    (`/change-database-schema`).
  - Everything that opens the database does so through
    `AppDatabase.open()`, which adds every migration.
  - Old names stay until a migration changes them: the table `excursions`
    (ADR-0010) and the column `hotel` (ADR-0012).
  - The only other stored state is the dark mode switch, in the
    SharedPreferences file `settings` (ADR-0006).
- **Consequences:**
  - Version 2 (2026-10-27, `MIGRATION_1_2`) added the event columns
    `details`, `time` and `timeZone` (ADR-0013).
  - The migration tests read the exported schemas as test assets.
  - Opt-in reminders would have to be saved in the database, which is a
    schema change (ADR-0005).
  - `excursions.vacationId` has no foreign key, so the checks that keep
    events inside their vacation and stop a vacation with events from
    being deleted live only in `VacationDetailActivity` (ADR-0028).
  - Agents change the schema only when a task calls for it.
- **Alternatives considered:** Not recorded.
- **Revisit when:** Not recorded.

## ADR-0004 — Cloud backup off; device-to-device transfer allowed

- **Status:** Accepted
- **Date:** 2026-10-25
- **Context:** The database is the only copy of the user's trips
  (ADR-0003), and data leaves the app only when the user shares it or adds
  it to a calendar (ADR-0001).
- **Decision:** Decided with the user on 2026-10-25: cloud backup is off,
  with `allowBackup="false"` plus exclusions in `backup_rules.xml` and
  `data_extraction_rules.xml`. A direct device-to-device transfer to a new
  phone is still allowed.
- **Consequences:**
  - Trips aren't copied to Google Drive (`PRIVACY.md`).
  - If the user loses or replaces the phone, deletes Vagor or its data, or
    deletes a vacation or event, nobody can recover that data, except from
    a copy Android made in a direct device-to-device transfer (`LICENSE`,
    section 5.2).
  - The store listing must not claim cloud backup (`docs/05-release.md`,
    step 6).
  - Agents change the backup settings only when a task calls for it.
- **Alternatives considered:** Not recorded.
- **Revisit when:** Not recorded.

## ADR-0005 — Automatic reminders for every vacation and event

- **Status:** Accepted
- **Date:** 2026-10-25
- **Context:** Product requirement 3 asks for system notifications for
  vacation start, vacation end and each event (`docs/00-overview.md`).
- **Decision:**
  - Decided with the user on 2026-10-25: reminders are automatic for every
    item, and a vacation's fire around 9:00 AM local time on its start and
    end dates, with inexact alarms.
  - How it's built:
    - Each reminder is a `NotificationCompat` notification on an app
      notification channel. `POST_NOTIFICATIONS` is requested at runtime
      on API 33+.
    - Scheduling stays in `ReminderScheduler`, and the detail screens call
      it on every save and delete.
    - A reminder whose moment has passed is never set, so one missed while
      the phone was off isn't shown late.
- **Consequences:**
  - On Android 12+ a vacation reminder usually arrives within an hour; on
    Android 8–11 (`setWindow`) Doze can hold it until the phone wakes.
  - Event reminders were decided on 2026-10-26 (ADR-0014). Rebuilding,
    the alarm limit and timeouts are in ADR-0025, ADR-0024 and ADR-0027.
  - Changing any of these is the user's call
    (`docs/subsystems/reminders.md`).
- **Alternatives considered:** opt-in reminders. They would have to be
  saved in the database, which is a schema change (ADR-0003).
- **Revisit when:** the user decides to change when or whether reminders
  come.

## ADR-0006 — Dark mode as an on/off switch, on by default

- **Status:** Accepted
- **Date:** 2026-10-25
- **Context:** The user asked for dark mode on 2026-10-25, as an on/off
  switch.
- **Decision:**
  - A dark mode switch on the Settings tab, on by default.
  - `ThemeSettings` keeps it in the SharedPreferences file `settings`, and
    `VagorApplication` applies the saved theme before any screen.
- **Consequences:**
  - Both themes must meet WCAG AA (ADR-0017).
  - The system draws the launch window before any app code runs. On
    Android 8–11 it follows the system light/dark setting on every cold
    start, so a dark-mode user on a light-themed phone sees a brief light
    flash. On Android 12+ only the very first launch does; later launches
    match the app setting through `UiModeManager.setApplicationNightMode`
    (not captured on a device).
- **Alternatives considered:** a follow-system option. The user asked for
  an on/off switch instead.
- **Revisit when:** Not recorded.

## ADR-0007 — A dashboard of vacations with bottom navigation

- **Status:** Accepted
- **Date:** 2026-10-25
- **Context:** The user asked for a dashboard and navigation on
  2026-10-25, and the screens were reorganised around them that day
  (product requirement 9).
- **Decision:**
  - The app opens on a dashboard of every vacation with its dates. Current
    and upcoming vacations come first, soonest at the top; vacations that
    have ended go below them under a "Past" heading, most recent first
    (`Dashboard.rows`).
  - A blue Add Vacation button sits on the dashboard.
  - `HomeActivity`, the launcher, holds a bottom navigation bar that
    switches between two tabs: Vacations (`VacationsFragment`, the
    dashboard) and Settings (`SettingsFragment`).
  - Tapping a vacation opens its page with its events
    (`EventListActivity`).
- **Consequences:**
  - The dashboard's order is plain Kotlin (`Dashboard.kt`), so it can be
    unit tested.
  - `HomeActivity` uses `setEdgeToEdgeContentViewWithBottomBar()`, which
    does nothing for the keyboard: a text field added to a home tab would
    need its own keyboard handling (`docs/01-architecture.md`).
  - `HomeActivity` sets no title of its own and must never get an
    `android:label`: on the launcher activity it would rename the app's
    icon.
- **Alternatives considered:** Not recorded.
- **Revisit when:** Not recorded.

## ADR-0008 — Dates picked from Vagor's own drop-down calendar, never typed

- **Status:** Accepted
- **Date:** 2026-10-25
- **Context:** The user asked for calendar date entry on 2026-10-25
  (product requirement 10). The framework `CalendarView` follows the
  phone's calendar system, so a Thai phone would store Buddhist-era years,
  and it marks today by colour alone.
- **Decision:**
  - Every date field opens a month calendar that drops down below the
    field (`attachCalendarDropdown()` in `CalendarDropdown.kt`), and dates
    can't be typed.
  - The end date calendar starts at the start date. An event's calendar
    offers only its vacation's days.
  - Vagor draws the calendar itself, replacing the framework `CalendarView`
    in e2048d1 (2026-10-26). It is always Gregorian and marks today with a
    ring.
  - Since event times were added (asked for on 2026-10-26, 7f80138;
    ADR-0013), an event's time comes from Material's clock
    (`MaterialTimePicker`, in the phone's 12- or 24-hour style), and its
    zone from `TimeZonePickerDialog`. Neither field can be typed into.
  - All three fields use `makePickOnly()` (`PickOnlyField.kt`, 7f80138).
  - The calendar writes through `DateValidators.formatDate`.
- **Consequences:**
  - The save-time checks still run, because saved rows may hold older
    loose dates.
  - The calendar's days are at least 48dp tall at any font size. The
    calendar is wider than its field where the field is too narrow for
    seven 48dp days (a 360dp-wide phone), up to the screen's width less 8dp
    on each side; on narrower screens the days are as wide as that allows.
  - Instrumented tests set a date field with `setDate` and check it with
    `withDate` (`DateFieldTesting.kt`).
- **Alternatives considered:** the framework `CalendarView`. It follows the
  phone's calendar system and marks today by colour alone, so don't switch
  to it.
- **Revisit when:** Not recorded.

## ADR-0009 — Share as plain text through the Sharesheet

- **Status:** Accepted
- **Date:** 2026-10-25
- **Context:** Product requirement 4 asks that vacations and events can be
  shared with other people, and Vagor has no servers or accounts
  (ADR-0001).
- **Decision:**
  - Decided with the user on 2026-10-25: events are shared as plain text.
  - How it's built:
    - Share only through the Sharesheet: `Intent.ACTION_SEND` inside
      `Intent.createChooser`, as `EventListActivity` does, with text built
      from string resources such as `vacation_share_template`. No links to
      servers and no accounts.
    - The vacation text lists its events with their dates (and, since
      2026-10-27, their times). Each event has its own Share button, whose
      text adds its time zone and details (also since 2026-10-27).
    - To share a file (an `.ics` file, for example), serve it through a
      `FileProvider` `content://` URI with
      `FLAG_GRANT_READ_URI_PERMISSION`. Never use a `file://` URI.
- **Consequences:**
  - The text goes only to the app the user chooses, and that app's terms
    and privacy policy apply to it (`PRIVACY.md`, `LICENSE` section 5.3).
  - Importing a shared trip isn't supported (`docs/roadmap.md`, M7).
- **Alternatives considered:** Not recorded.
- **Revisit when:** the user decides whether Vagor should be able to import
  a trip someone shared. That question is still open.

## ADR-0010 — "Events" in the app; the table keeps the name excursions

- **Status:** Accepted
- **Date:** 2026-10-25
- **Context:** Events were called excursions until 2026-10-25. Renaming a
  table needs a migration (ADR-0003).
- **Decision:**
  - The UI and code say **events**.
  - The `Event` entity maps to the table `excursions`
    (`@Entity(tableName = "excursions")`), and `EventDAO` queries that
    table.
  - Don't rename the table without a migration.
- **Consequences:**
  - The rename needed no schema migration.
  - Code that reads `excursions` is reading events.
- **Alternatives considered:** Not recorded.
- **Revisit when:** Not recorded.

## ADR-0011 — No release workflow; releases are built and uploaded by hand

- **Status:** Accepted
- **Date:** 2026-10-25
- **Context:** `cd.yml`, the release workflow, was removed at the user's
  request on 2026-10-25.
- **Decision:**
  - There is no release workflow. Releases are built, signed and uploaded
    by hand (`docs/05-release.md`), and Vagor's signing never goes through
    CI.
  - `Makefile.txt` only tags. Nothing is built or uploaded from a tag.
  - Agents never run `make -f Makefile.txt tag`, `push-tag` or `release`:
    they create and push git tags.
- **Consequences:**
  - The developer tags each uploaded build by hand (`docs/05-release.md`,
    step 3).
  - The removed CD and SonarQube workflows read secrets named
    `ANDROID_SIGNING_*`, `GOOGLE_PLAY_SERVICE_ACCOUNT`, `SLACK_WEBHOOK` and
    `SONAR_*`. Any that still exist should be deleted (`docs/05-release.md`,
    step 9).
- **Alternatives considered:** Not recorded.
- **Revisit when:** Not recorded.

## ADR-0012 — Trip details replace the hotel field

- **Status:** Accepted
- **Date:** 2026-10-26
- **Context:** The user asked on 2026-10-26 for trip details in place of
  the vacation's hotel field (product requirement 1). The database column
  is still named `hotel`, and renaming it needs a migration (ADR-0003).
- **Decision:**
  - Trip details are free text over several lines, and optional.
  - `Vacation.details` maps to the old column `hotel`
    (`@ColumnInfo(name = "hotel")`).
- **Consequences:**
  - Trip details and event details are the only fields that aren't
    required.
  - Add to Calendar puts a vacation's trip details in the calendar
    description (ADR-0015).
- **Alternatives considered:** Not recorded.
- **Revisit when:** Not recorded.

## ADR-0013 — Events have a time and a time zone; database version 2

- **Status:** Accepted
- **Date:** 2026-10-26
- **Context:** The user asked on 2026-10-26 for each event to have a time,
  a time zone and details (product requirement 2). They need new columns,
  so a schema change (ADR-0003).
- **Decision:**
  - Each event is planned on a date inside its vacation's date range, at a
    time in a time zone, with optional details.
  - The time is required and picked from a clock. The time zone is picked
    from a searchable list and starts on the phone's zone (ADR-0008).
  - Times are stored as 24-hour `HH:mm` strings and time zones as IANA zone
    IDs such as `Asia/Tokyo`. Parse them only through
    `DateValidators.parseLocalTime` and `parseZone`.
  - The zone is shown by name and city ("Japan Standard Time (Tokyo)"), so
    it isn't read as the event's place.
  - Database version 2 (2026-10-27, `MIGRATION_1_2`) added the columns
    `details`, `time` and `timeZone`.
- **Consequences:**
  - `Event.details` is blank when empty. `Event.time` and `Event.timeZone`
    are null on events saved before database version 2, and every save
    since stores both.
  - Events saved before 2026-10-27 have no time until they're next edited,
    and can't be saved again without one. Their reminder stays at 9:00 AM
    on their date (ADR-0014).
- **Alternatives considered:** Not recorded.
- **Revisit when:** Not recorded.

## ADR-0014 — Event reminders an hour before, exact when Android allows

- **Status:** Accepted; amended by ADR-0031 (the inexact fallback)
- **Date:** 2026-10-26
- **Context:** The user asked on 2026-10-26 for a reminder an hour before
  each event. On Android 12+ an exact alarm needs Alarms & reminders
  (`SCHEDULE_EXACT_ALARM`), which is on by default only on Android 12 and
  13, and denied by default for new installs on Android 14+. Google Play
  allows `USE_EXACT_ALARM` only for calendar and alarm-clock apps.
- **Decision:**
  - An event's reminder fires an hour before it starts, in its own time
    zone (`ReminderTimes.eventTriggerAtMillis`).
  - It uses an exact alarm (`setExactAndAllowWhileIdle`) whenever Android
    allows one: always below Android 12, and on Android 12+ while Alarms &
    reminders is on. Without it, the alarm is inexact: first `setWindow`
    for 50 to 60 minutes before, replaced on 2026-11-01 (ADR-0031).
  - Only event reminders use exact alarms. Check `canScheduleExactAlarms()`
    (`ReminderScheduler`) before each exact alarm and catch
    `SecurityException`.
  - Never add `USE_EXACT_ALARM`.
  - After saving an event whose reminder can't be exact,
    `ExactAlarmOfferDialog` offers Alarms & reminders, once per launch. The
    Settings tab says whether event reminders are exact and opens the same
    screen.
- **Consequences:**
  - An event saved before events had a time keeps the 9:00 AM reminder on
    its date.
  - Taking exact alarms away sends nothing: Android deletes the exact
    alarms and kills the app (not a force stop), and the event reminders
    come back inexact the next time anything starts the app (ADR-0025).
    Setting Vagor's battery use to Unrestricted also allows exact alarms,
    with no broadcast.
  - As of 2026-10-31 Play's exact alarm policy asks for a declaration only
    for `USE_EXACT_ALARM`. If Play Console shows a permissions declaration
    at the first upload, it's answered with this use, and its review can
    take several weeks (`docs/05-release.md`, "Already in the app").
  - Instrumented tests never take `SCHEDULE_EXACT_ALARM` away, because
    Android stops the app, test and all (`docs/04-quality.md`).
- **Alternatives considered:** `USE_EXACT_ALARM`. Google Play allows it
  only for calendar and alarm-clock apps.
- **Revisit when:** the user decides to change it. Changing any of the
  reminder times is the user's call.

## ADR-0015 — Add to Calendar through Google Calendar's new-event screen

- **Status:** Accepted
- **Date:** 2026-10-26
- **Context:** The user asked on 2026-10-26 for adding vacations and events
  to Google Calendar (product requirement 4). Vagor has no network access
  (ADR-0001). Calendar apps read an all-day intent's times differently (in
  the phone's zone or in UTC; an end at midnight as exclusive or not), and
  Google Calendar documents none of it. They also handle
  `Events.EVENT_TIMEZONE` unevenly.
- **Decision:**
  - An Add to Calendar button on the vacation page and the event form
    opens Google Calendar's own new-event screen, or another calendar
    app's when Google Calendar isn't installed, filled in
    (`CalendarInsert.kt`):
    - a vacation all day over its dates, with its trip details and events
      as the description;
    - an event at its time in its time zone, an hour long, with its
      details, time and vacation.
  - It uses `Intent.ACTION_INSERT` with `CalendarContract.Events.CONTENT_URI`
    and the type `vnd.android.cursor.dir/event`, aimed at Google Calendar
    (`com.google.android.calendar`) and, when that isn't installed, at any
    calendar app (`openCalendarInsert`).
  - It needs no permission. Never add `READ_CALENDAR` or `WRITE_CALENDAR`,
    and never write to the calendar provider directly.
  - Starting the intent needs no `<queries>` entry; catch
    `ActivityNotFoundException` instead. "No calendar app" is a dialog,
    because a toast is cut at two lines.
  - A second tap within 2 seconds is ignored, because some calendar apps
    save every new-event screen the user leaves.
  - The label says "Calendar" because it may open another app, and
    Google's brand guidance asks for accurate wording.
  - `CalendarTimes` starts a vacation at the later of its first day's UTC
    and local midnights, and ends it inside its last day: an hour after
    that day's start, or 1:00 on the phone's clock, whichever is later
    (the clock time matters where clocks go back from 1:00 to midnight, and
    the hour where they skip from midnight to 1:00).
  - An event's description names its time and zone too.
- **Consequences:**
  - The calendar app decides whether and where to save (Android's original
    calendar app saves when the user leaves the screen), so never promise
    that nothing is added until the user saves. If it's saved to an
    account, it may sync there (`PRIVACY.md`).
  - Every app checked (Android's original calendar, Etar, Fossify) reads
    `CalendarTimes` correctly. Google Calendar's reading couldn't be
    checked without a signed-in account.
  - The store listing names Google Calendar only in plain text saying what
    Vagor works with: no Google logo or Calendar icon in the graphics or
    screenshots, and no Google name in the app's title or icon
    (`docs/05-release.md`, step 6).
- **Alternatives considered:** Not recorded.
- **Revisit when:** Not recorded.

## ADR-0016 — Every button on a page is the same size

- **Status:** Accepted
- **Date:** 2026-10-26
- **Context:** The user asked on 2026-10-26 for every button on a page to
  be the same size.
- **Decision:**
  - Buttons are full width, one above the other, styled
    `Widget.Vagor.Button` or `Widget.Vagor.Button.Outlined` with
    `layout_width="match_parent"` and `layout_height="wrap_content"`.
  - The style sets one height (`@dimen/button_height`), which grows with
    the font size.
  - Only the floating Add Vacation and Add Event buttons differ
    (`Widget.Vagor.AddButton`).
- **Consequences:** new buttons use these styles
  (`.claude/rules/ui-and-accessibility.md`).
- **Alternatives considered:** Not recorded.
- **Revisit when:** Not recorded.

## ADR-0017 — An accessible blue colour scheme, WCAG AA, no DynamicColors

- **Status:** Accepted
- **Date:** Not recorded
- **Context:** Product requirement 7 asks for a colour scheme for
  colour-blind users, and for keyboard focus to be visible.
- **Decision:**
  - A blue Material 3 scheme from the Okabe-Ito blue `#0072B2`.
  - Every text/background pair meets WCAG AA (4.5:1; 3:1 for non-text UI)
    in both themes. `ThemeContrastTest` enforces this.
  - Don't convey anything by colour alone, and don't add DynamicColors.
  - Colours come from theme attributes only (`res/values/colors.xml`,
    `values/themes.xml`, `values-night/themes.xml`). Never hard-code a text
    or background colour in a layout.
  - Keyboard focus is visible. The theme turns on Material's focus rings
    (`focusRingsEnabled`, not yet public API), the dashboard and time zone
    lists have their own focus rings, and the calendar's days have one.
  - Material's time picker marks its selected AM/PM by hue alone, so
    `ThemeOverlay.Vagor.MaterialTimePicker` fills it with the primary blue
    instead. The selected hour or minute keeps Material's colours: the
    dial's numbers also show which is being set.
- **Consequences:**
  - The calendar marks today with a ring (ADR-0008).
  - The launcher icon and the Play graphics use the same blue
    (`docs/subsystems/theme-and-accessibility.md`).
- **Alternatives considered:** DynamicColors. It would replace the blue
  with wallpaper colours.
- **Revisit when:** Not recorded.

## ADR-0018 — Proprietary, all rights reserved

- **Status:** Accepted; amended by ADR-0034 (`LICENSE` became the end-user
  license agreement)
- **Date:** 2026-10-31
- **Context:** Vagor was under the Apache License 2.0 until 2026-10-31,
  when the user asked for it to be made proprietary.
- **Decision:**
  - Vagor is proprietary, all rights reserved by Joseph Anthony Abbott III.
    Don't put its own code under an open-source license.
  - Vagor's own files carry no license headers.
  - The source code is licensed to no one, except for the license GitHub's
    Terms of Service give other GitHub users while the repository is
    public: to view it and to fork it on GitHub. Any other use needs
    permission, asked for at jabbottpublicsupport@gmail.com. Since
    2026-11-07 this exception is in README's License section (ADR-0034).
  - Contributions are pitched by email to jabbottpublicsupport@gmail.com
    first; pull requests without a confirmed pitch are denied. By opening a
    pull request, a contributor agrees that Joseph Anthony Abbott III may
    use, change, distribute and license the contribution as part of Vagor
    on any terms (`CONTRIBUTING.md`).
- **Consequences:**
  - The third-party software the app includes keeps its own licenses,
    listed in `NOTICE` and in the app (ADR-0029).
- **Alternatives considered:** keeping the Apache License 2.0, Vagor's
  license before 2026-10-31. The user asked for the change; no other reason
  is recorded.
- **Revisit when:** Not recorded.

## ADR-0019 — jabbottpublicsupport@gmail.com is the privacy and listing contact

- **Status:** Accepted
- **Date:** 2026-10-31
- **Context:** The privacy policy needs a contact. GitHub Issues turned out
  to be off for the repository.
- **Decision:** The user chose jabbottpublicsupport@gmail.com on
  2026-10-31 as:
  - the privacy contact, in `PRIVACY.md` and `privacy_policy_text`;
  - the Play listing's contact email, which Play shows on the listing.
- **Consequences:**
  - The same address is the payments profile's customer support email and
    the closed test's feedback channel (`docs/05-release.md`, steps 5 and 7),
    takes contribution pitches and permission requests (`CONTRIBUTING.md`,
    `README.md`), and receives security reports (`SECURITY.md`).
  - To change it, change the Contact section in both copies of the privacy
    policy, and the listing.
- **Alternatives considered:** GitHub Issues. They turned out to be off.
- **Revisit when:** Not recorded.

## ADR-0020 — Application ID io.github.jabbott_iii.vagor

- **Status:** Accepted
- **Date:** Not recorded
- **Context:** Play rejects `com.example.*` package names, and the package
  name can never change after the first upload. The ID first appears in
  commit 47f8bf6 (2026-10-25); when it was decided is not recorded.
- **Decision:**
  - The application ID, the Play and device identity, is
    `io.github.jabbott_iii.vagor`.
  - The Kotlin package and `namespace` stay `com.example.vagor`. They show
    up in class names; Play's `com.example` restriction applies only to the
    package name.
  - Never change `applicationId`. Change `namespace` only when a task calls
    for it.
- **Consequences:**
  - Builds from before the change, which used `com.example.vagor`, are a
    separate app on the phone, and their data doesn't carry over.
  - A free app can never become paid without a new package name, so Vagor
    is created as paid from the first upload (ADR-0035).
- **Alternatives considered:** keeping `com.example.vagor`. Play rejects
  it.
- **Revisit when:** never after the first Play upload: a new ID is a
  different app.

## ADR-0021 — R8 on release builds

- **Status:** Accepted
- **Date:** 2026-10-31
- **Context:** Release builds were not shrunk before 2026-10-31.
- **Decision:**
  - Release builds run R8 (`optimization { enable = true }`). It shrinks,
    optimizes and obfuscates the code and removes unused resources.
  - Keep rules go in `app/src/main/keepRules/*.keep`. None are needed
    today, because the libraries ship their own (Room keeps its generated
    `_Impl`, and Fragment keeps every fragment's no-argument constructor).
- **Consequences:**
  - Debug builds and the instrumented tests don't use R8, so a change that
    relies on reflection, class names or resources looked up by name is
    checked in a release build on an emulator, and every upload's release
    build is tried on an emulator first (`docs/05-release.md`, step 3).
  - The bundle carries R8's mapping file, which Play uses to deobfuscate
    crash reports, so there's no separate deobfuscation file to upload.
    Keep `app/build/outputs/mapping/release/mapping.txt` with each uploaded
    build to read stack traces yourself.
  - CI builds an unsigned `bundleRelease` to run R8 and lint's release
    checks (ADR-0023).
- **Alternatives considered:** Not recorded.
- **Revisit when:** Not recorded.

## ADR-0022 — detekt and ktlint

- **Status:** Accepted
- **Date:** 2026-10-31
- **Context:** The user asked for Kotlin static analysis and style checks
  on 2026-10-31. The last stable detekt release (1.23.8) predates Kotlin
  2.1, AGP 9 and JDK 25.
- **Decision:**
  - ktlint uses the IntelliJ IDEA code style without trailing commas,
    wrapping at 120 characters. All the code was formatted to it once that
    day, so it has no baseline.
  - detekt uses its default rules (lines up to 120 characters, like
    ktlint) and fails only on findings that aren't in
    `app/detekt-baseline.xml`, which lists what the code had then and
    matches by rule and declaration.
  - Never add new findings to the baseline to make a check pass.
  - detekt is the 2.0 alpha.
- **Consequences:**
  - `./gradlew ktlintFormat` fixes most findings. Every file already
    passes, so it changes only what's new.
  - CI runs both (ADR-0023).
- **Alternatives considered:**
  - detekt 1.23.8, the last stable release. It predates Kotlin 2.1, AGP 9
    and JDK 25.
  - A ktlint baseline. Added first, then dropped once all the code was
    formatted (`docs/roadmap.md`, M3).
- **Revisit when:** Not recorded.

## ADR-0023 — CI runs only Gradle tasks that exist, with an API 37 emulator

- **Status:** Accepted
- **Date:** 2026-10-31
- **Context:** The user asked on 2026-10-31 for the failing workflows to be
  fixed. SonarQube, OWASP dependency-check and coverage steps called tasks
  that no plugin provided, and `|| true` hid that. The emulator timed out
  booting: the runner's own command-line tools, 12.0 on Ubuntu 24.04, give
  an `android-37.0` AVD `target=android-0`, and the emulator runner uses
  them whenever they exist.
- **Decision:**
  - CI runs on GitHub's Ubuntu runners, which come with the Android SDK.
  - `ci.yml` runs `lint`, `detekt`, `ktlintCheck`, `assembleDebug`, an
    unsigned `bundleRelease` (to run R8 and lint's release checks) and
    `testDebugUnitTest`, then `connectedAndroidTest` on an API 37
    (`'37.0'`, quoted) `google_apis` emulator, which needs the Enable KVM
    step to run fast enough, and the step that installs command-line tools
    22.0 (added 2026-10-31 after the emulator timed out booting).
  - `security.yml` runs CodeQL, which analyses Kotlin only by watching a
    build (`build-mode: manual`).
  - CI calls only Gradle tasks that exist. Adding one of the removed checks
    back means adding its Gradle plugin first.
- **Consequences:**
  - CI can only be checked by pushing, so never report a workflow change as
    passing until a run on GitHub shows it.
  - Agents change the workflows only when a task calls for it.
- **Alternatives considered:** keeping the SonarQube, OWASP
  dependency-check and coverage steps. No plugin provided their tasks, and
  `|| true` hid that.
- **Revisit when:** one of the removed checks is wanted again: add its
  Gradle plugin first.

## ADR-0024 — Survive Android's 500-alarm limit

- **Status:** Accepted
- **Date:** 2026-10-31
- **Context:** Android lets an app hold 500 alarms at a time, and past that
  every set call throws, even one that replaces an alarm already set
  (`IllegalStateException`, or `SecurityException` on some Samsung
  phones). Uncaught, that crashed every start, since each start rebuilds
  the reminders (ADR-0025), and only clearing the app's storage (every
  trip) got out.
- **Decision:** Every alarm goes through `ReminderScheduler.setAlarm`,
  which cancels the reminder's own old alarm and tries once more, then
  leaves the reminder unset (fixed 2026-10-31).
- **Consequences:**
  - Past the limit, some reminders are unset. Which reminders get the 500
    places past the limit, and whether the user is told, is the user's call
    (`docs/roadmap.md`, M7).
  - `ReminderLimitInstrumentedTest` sets 520 real event alarms for events
    that aren't in the database, and cancels them afterwards. A run killed
    partway leaves them set until they fire 30 days later or the app is
    uninstalled (`docs/04-quality.md`).
- **Alternatives considered:** Not recorded.
- **Revisit when:** the user decides which reminders get the 500 places,
  and whether the user is told.

## ADR-0025 — Rebuild every reminder once in each new process

- **Status:** Accepted
- **Date:** 2026-10-27 (ab51718, with event reminders); the receiver's
  rebuild-once change is 2026-10-31 (130feed)
- **Context:** Alarms are cleared on reboot and force stop. Android also
  kills the app when exact alarms are taken away, and stops it when it
  hibernates (ADR-0032). Some broadcasts can reach a process that is
  already running.
- **Decision:**
  - `VagorApplication` rebuilds every reminder from the database once in
    every new process (`ReminderScheduler.rebuildIfNeeded`), and
    `HomeActivity` again on resume if exact alarms were allowed or taken
    away since.
  - `ReminderRescheduleReceiver` calls `rebuildIfNeeded` on
    `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`, which always start a new
    process that `VagorApplication` has just rebuilt in, so it rebuilds
    once, not twice (changed 2026-10-31).
  - It also calls it on `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`
    (sent when the user allows exact alarms), which may reach a running
    process, where the exact-alarm state now differs, so it rebuilds.
  - On `TIME_SET` and `TIMEZONE_CHANGED` (dates saved without a time zone
    follow the phone's) it always rebuilds, because they can reach a
    process that is already running.
  - A rebuild never cancels a reminder whose moment has passed but which
    may still be on its way, since inexact alarms can come late: an
    event's is kept until the event starts, and a 9:00 AM one until its day
    ends (`keepDue`).
  - Saving an event keeps it too, unless the date, time or time zone
    changed. Then a reminder already posted for it is dismissed, because it
    gives the old time.
- **Consequences:**
  - Taking exact alarms away sends nothing, so the event reminders come
    back, inexact, the next time anything starts the app.
  - Every instrumented run starts the app's process, which re-arms every
    stored trip's reminders (`docs/04-quality.md`).
- **Alternatives considered:** rebuilding again in
  `ReminderRescheduleReceiver` on `BOOT_COMPLETED` and
  `MY_PACKAGE_REPLACED`, which rebuilt twice in the same new process.
  Changed on 2026-10-31.
- **Revisit when:** Not recorded.

## ADR-0026 — Show dates the way the phone writes them

- **Status:** Accepted
- **Date:** 2026-10-31
- **Context:** The user asked on 2026-10-31 for dates to be shown the way
  the phone writes them. Dates are stored as `MM/dd/yyyy` strings. A Thai
  phone would otherwise show the Buddhist-era year.
- **Decision:**
  - "Jul 6, 2031" in the US, "6 Jul 2031" in the UK.
  - `dateText` in `TripText.kt` formats a stored date with ICU's `yMMMd`
    skeleton, always in the Gregorian calendar, and shows a date it can't
    read as it was stored.
  - Everything a person reads goes through it: screens, reminders, shared
    text and calendar descriptions. Never show a stored date string
    directly.
  - The date fields are `DateField`s. `date` holds the stored value, and
    the text is only for show, so read and write `date`, never the text. A
    `DateField` keeps its date across rotation itself.
  - Display text uses ICU (`android.icu`), which knows the phone's own
    names and patterns. `java.time` formatting is for stored values.
- **Consequences:**
  - Each ICU call's API level is checked against minSdk 26: some, such as
    `SystemTimeZoneType.CANONICAL_LOCATION`, arrived later.
  - `CalendarDropdownInstrumentedTest` switches the app's language to
    Thai, US English and British English in two tests, and back.
- **Alternatives considered:** Not recorded.
- **Revisit when:** Not recorded.

## ADR-0027 — Posted reminders leave the shade once out of date; late ones are still posted

- **Status:** Accepted
- **Date:** 2026-10-31
- **Context:** The user asked on 2026-10-31 for a posted reminder to leave
  the notification shade once it's out of date.
- **Decision:**
  - Each reminder is posted with `setTimeoutAfter`: a 9:00 AM one leaves
    when its day ends on the phone, and an event's an hour after the event
    starts (`ReminderTimes.dateReminderTimeoutMillis` and
    `eventReminderTimeoutMillis`).
  - One that Android delivers after that, held back by Doze for example,
    is still posted, with no timeout of its own.
- **Consequences:**
  - On the API 37 emulator Android gives every notification without a
    timeout one of 72 hours anyway.
  - The timeout is a duration fixed when the reminder is posted, so a
    9:00 AM one leaves at midnight in the zone the phone had then, even if
    the phone's zone changes after.
- **Alternatives considered:** skipping late reminders. The user chose not
  to.
- **Revisit when:** Not recorded.

## ADR-0028 — An edited vacation must still hold its events

- **Status:** Accepted
- **Date:** 2026-10-31
- **Context:** An event's date must fall within its vacation's start and
  end dates, inclusive. `excursions.vacationId` has no foreign key
  (ADR-0003), so nothing in the database keeps that true.
- **Decision:**
  - An edited vacation's new dates must still hold its events
    (`DateValidators.isOutsideVacation`). An event with an old loose date
    can't be checked and doesn't block the save (added 2026-10-31).
  - A vacation that has events still can't be deleted (a rule since the
    port, 786a39a and a1d58c9, 2026-09-12 to 2026-09-16).
  - These two checks exist only in `VacationDetailActivity`.
- **Consequences:** moving or shortening a vacation can't leave its events
  outside it.
- **Alternatives considered:** Not recorded.
- **Revisit when:** Not recorded.

## ADR-0029 — Ship the Apache License text and show third-party licenses in Settings

- **Status:** Accepted
- **Date:** 2026-10-31
- **Context:** The bundled libraries are under the Apache License 2.0,
  which asks for its text to go with the app. Vagor itself is proprietary
  (ADR-0018).
- **Decision:**
  - The only third-party license text the app ships is the Apache License
    2.0, in `res/raw/apache_license_2_0.txt` (added 2026-10-31).
  - The Settings tab shows it with the list of third-party software under
    Third-Party Licenses (`LicenseText.kt` reflows it for the dialog).
  - When a library, icon or other third-party material that ships in the
    app is added, add it to `NOTICE` and to `third_party_licenses_text` in
    `strings.xml`. `ThirdPartyLicensesTest` checks they list the same things.
  - One under another license also needs that license's text added.
- **Consequences:** `LICENSE` section 4 points buyers to Settings >
  Third-Party Licenses and to `NOTICE` (`docs/02-dependency-policy.md`).
- **Alternatives considered:** Not recorded.
- **Revisit when:** something that ships in the app is under a license
  other than the Apache License 2.0.

## ADR-0030 — Release signing from environment variables; a broken setup fails the build

- **Status:** Accepted
- **Date:** 2026-10-25 (environment-variable signing, 47f8bf6); 2026-11-01
  for the fail-on-a-broken-setup rule and `--no-configuration-cache`
  (f45b3a0)
- **Context:** Google Play needs the bundle signed with an upload key, and
  the keystore and its passwords must never enter git. With Gradle's
  configuration cache, which the project turns on, a build stores the
  passwords (encrypted, but with the key on the same machine) under
  `.gradle/configuration-cache/` in the project folder.
- **Decision:**
  - Release signing reads the upload key only from the
    `VAGOR_UPLOAD_KEYSTORE`, `VAGOR_UPLOAD_KEYSTORE_PASSWORD`,
    `VAGOR_UPLOAD_KEY_ALIAS` and `VAGOR_UPLOAD_KEY_PASSWORD` environment
    variables. A relative keystore path is read from the repository root
    (since 2026-11-01).
  - With `VAGOR_UPLOAD_KEYSTORE` unset or blank, `bundleRelease` is
    unsigned, as in CI.
  - Once it's set, a missing keystore or variable fails every build, debug
    builds and tests included, rather than leaving the bundle unsigned
    (added 2026-11-01).
  - Signed builds run with `--no-configuration-cache`, which keeps the
    passwords out of `.gradle/configuration-cache` (added 2026-11-01).
  - Never commit a keystore or password.
- **Consequences:**
  - The developer clears the four variables after a signed build, since
    while `VAGOR_UPLOAD_KEYSTORE` is set without the others, every Gradle
    command in that shell stops (`docs/05-release.md`, step 3).
  - `keytool -printcert` checks the bundle is signed; `jarsigner -verify`
    is no check.
  - With Play App Signing, this is only the upload key. If it's lost or
    leaked, Play Console can reset it.
- **Alternatives considered:** quietly making an unsigned bundle when
  signing is set up wrong, as before 2026-11-01.
- **Revisit when:** Not recorded.

## ADR-0031 — Inexact event reminders use allow-while-idle alarms

- **Status:** Accepted
- **Date:** 2026-11-01
- **Context:** Without Alarms & reminders, an event's reminder was a
  `setWindow` alarm for 50 to 60 minutes before the event (ADR-0014). Doze
  and Battery Saver could hold that until after the event.
- **Decision:** Changed on 2026-11-01, with the user: without exact-alarm
  permission, an event's reminder is an inexact allow-while-idle alarm
  (`setAndAllowWhileIdle`, like the vacation reminders on Android 12+).
  - Allow-while-idle fires through Doze and Battery Saver, within a quota.
  - Android 12+ delivers it within an hour after its moment, so any time
    in the hour before the event, and near its start on a phone that
    nothing else wakes in that hour (Android delivers a wakeup alarm when
    one reaches the end of its window).
- **Consequences:**
  - Android 12+ gives an app's inexact allow-while-idle alarms
    `FLAG_ALLOW_WHILE_IDLE_COMPAT`, which app standby buckets still limit.
    Once Android puts an unused Vagor in the restricted bucket (after 8
    days without opening it or tapping its notifications, on Android 13+),
    only one inexact alarm a day is sure to come on time.
  - A Vagor notification the user sees moves Vagor to the working set
    bucket for 12 hours, and charging suspends bucket limits, but no alarm
    an app can set without exact-alarm permission escapes them. Allowing
    Alarms & reminders does, which the app's text says.
  - Setting Vagor's battery use to Restricted holds every reminder until
    Vagor is opened (`README.md`).
  - The store listing promises reminders "within the hour before each
    event", and an exact time only when Alarms & reminders is allowed:
    "exactly an hour before when you allow alarms and reminders"
    (`fastlane/metadata/android/en-US/full_description.txt`).
- **Alternatives considered:** `setWindow` for 50 to 60 minutes before the
  event. Doze and Battery Saver could hold it until after the event.
- **Revisit when:** the user decides to change it. Changing any of the
  reminder times is the user's call.

## ADR-0032 — Offer to keep Android from pausing Vagor for trips far ahead

- **Status:** Accepted
- **Date:** 2026-11-01
- **Context:** From Android 12, Android hibernates an app that hasn't been
  opened for about three months (checked about every 15 days). It stops the
  app, which cancels every alarm, and reminders that fire don't count as
  use. The alarms come back only when Vagor is next started, so a trip
  planned further ahead could lose its reminders.
- **Decision:** Decided with the user on 2026-11-01:
  - After saving a vacation that ends more than two months ahead
    (`ReminderTimes.mayOutlastHibernation`) while Android may hibernate
    Vagor (`mayHibernate`: Android 12+,
    `PackageManager.isAutoRevokeWhitelisted` false, and not a work profile,
    which Android exempts), `HibernationOfferDialog` offers Vagor's App
    info, where the user turns off the unused app setting, once per launch.
  - The offer comes whether or not notifications are on.
  - The Settings tab says whether Android may pause Vagor, and has an App
    Info button.
- **Consequences:**
  - Android also exempts company-managed phones, and some makers turn
    hibernation off, which no public API reveals, so the app's text says
    Android "may" pause Vagor.
  - The setting's name differs between Android versions ("Manage app if
    unused" on API 37), so the app's text doesn't name it.
  - The home flow's tests of the offer run only on Android 12+ while
    Android may pause Vagor, as it may after any install
    (`docs/04-quality.md`).
- **Alternatives considered:** offering it only while notifications are on.
  On Android 13+ the dashboard asks for notifications after the first trip
  is saved, and that first trip is the one the offer is most for.
- **Revisit when:** Not recorded.

## ADR-0033 — Every dialog is a DialogFragment; forms disable their buttons after saving

- **Status:** Accepted
- **Date:** 2026-11-01
- **Context:** A dialog shown straight from `MaterialAlertDialogBuilder`
  closes when the screen is recreated, for example after a rotation. A
  second tap on Save, or one behind an offer that keeps the form open, could
  save twice.
- **Decision:**
  - Every dialog is a `DialogFragment` (`MessageDialog` for a message with
    an OK button), so it's still open after a rotation (fixed 2026-11-01).
  - A form that closes after a save disables its buttons first.
- **Consequences:** a message that a toast would cut at two lines is a
  dialog too: "no calendar app" (ADR-0015) and the notifications-off
  message (`docs/subsystems/reminders.md`).
- **Alternatives considered:** dialogs shown straight from
  `MaterialAlertDialogBuilder`. They close when the screen is recreated.
- **Revisit when:** Not recorded.

## ADR-0034 — The end-user license agreement, adapted from Denarii Dolor's

- **Status:** Accepted
- **Date:** 2026-11-07
- **Context:** Vagor is to be sold to buyers on Google Play (ADR-0035). The
  user's Denarii Dolor app already has a perpetual end-user license
  agreement.
- **Decision:** At the user's request on 2026-11-07, `LICENSE` is the Vagor
  Perpetual End-User License Agreement (version 1.0, effective November 7,
  2026), copied from Denarii Dolor's and changed to fit Vagor, mainly:
  - it drops Denarii Dolor's encryption, PIN and wipe function, exported
    reports and financial advice;
  - it says instead that Vagor gives data only to the app the user shares
    it with or their calendar app, that the receiving app's terms then
    apply, that Vagor turns off cloud backup and can't recover lost data
    except from a device-to-device transfer, and that it's a planning aid
    only, whose reminders can be late or stopped;
  - it says where its third-party notices are.

  The GitHub view-and-fork exception the old `LICENSE` had is now in README's
  License section (ADR-0018).
- **Consequences:**
  - Because Vagor is paid, the agreement's license fee, refund and
    liability cap apply as written.
  - `LICENSE` section 5 (how the app handles data) and section 6 (reminders
    can be late or stopped) must stay true to what the app does.
  - `LICENSE` mentions refunds only in 10.3, which leaves the refund policy
    open (`docs/05-release.md`, step 5; `docs/roadmap.md`, M7).
  - It's published as `LICENSE.txt` at a public URL, and it's Vagor's
    terms of service link if the developer declares trader status
    (`docs/05-release.md`, step 4).
  - Nothing in the app or the store listing shows the agreement yet
    (`docs/roadmap.md`, M7).
- **Alternatives considered:** Not recorded.
- **Revisit when:** Not recorded.

## ADR-0035 — Sold on Google Play for US$1

- **Status:** Accepted
- **Date:** 2026-11-07
- **Context:** Decided with the user on 2026-11-07, like Denarii Dolor. A
  free app can never become paid: charging for it would need a new package
  name, which is a new app, and Vagor's `applicationId` never changes
  (ADR-0020).
- **Decision:**
  - Vagor is sold on Google Play for US$1.
  - The app is created in Play Console as **Paid**, even though the first
    upload is only a test, since the price applies to every track
    (`docs/05-release.md`, step 5).
- **Consequences:**
  - A payments profile comes first, and once an account sells anything,
    Google Play shows the profile's full address.
  - Closed testers must buy the app (`docs/05-release.md`, step 7).
  - The refund policy, trader status, EEA and UK distribution and the EEA
    classification are still to decide (`docs/roadmap.md`, M7).
  - The end-user license agreement's license fee, refund and liability cap
    apply as written (ADR-0034).
  - Buying Vagor happens in Google Play before it's installed, and Vagor
    never sees the purchase, so the Data safety answers don't change
    (ADR-0001).
- **Alternatives considered:** creating the app as free. A free app can
  never become paid.
- **Revisit when:** Not recorded.

## ADR-0036 — English only

- **Status:** Accepted
- **Date:** Not recorded
- **Context:** Vagor's own text is only in English. Without a filter, the
  libraries' translations would show some of their text (the time
  picker's, for example) in the phone's language.
- **Decision:**
  - User-visible text goes in `res/values/strings.xml`. The app is English
    only.
  - `androidResources.localeFilters` keeps the libraries' translations
    out, so their text stays English too (in `app/build.gradle.kts` since
    commit f7bfc82, 2026-10-31).
- **Consequences:**
  - Dates and times still follow the phone's region (ADR-0026).
  - The store listing's default language is English (United States), and
    its text is in US English ("color") (`docs/05-release.md`, step 6).
- **Alternatives considered:** Not recorded.
- **Revisit when:** a language is added. Adding a language means adding it
  to `localeFilters` too.
