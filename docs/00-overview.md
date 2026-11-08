# 00 — Overview

## Purpose

Vagor is an offline vacation planner for Android, written in Kotlin as a
single `:app` module. It was ported from `jabbott-iii/university-projects`
→ `Mobile-Application-Development-Android`, keeping that app's Room-backed
persistence (`docs/roadmap.md`, M0). A person plans each vacation (a
title, trip details, a start date and an end date) and the events in it (a
date inside the vacation, a time in a time zone, and details), and Vagor
reminds them of each one.

Everything stays on the phone: no internet access, no backend, no accounts,
no ads and no analytics (ADR-0001). Data leaves the app only when the user
shares it as text through Android's Sharesheet (ADR-0009), or hands one
vacation or event to a calendar app with Add to Calendar (ADR-0015).

- **Identity:** the application ID, the Play and device identity, is
  `io.github.jabbott_iii.vagor` (ADR-0020). The Kotlin package and
  `namespace` are still `com.example.vagor`.
- **Ownership:** proprietary, all rights reserved by Joseph Anthony Abbott
  III (ADR-0018). `LICENSE` is the Vagor Perpetual End-User License
  Agreement for people who buy the app (ADR-0034). `NOTICE` lists the
  third-party software that ships in the app.
- **Distribution:** to be sold on Google Play for US$1 (ADR-0035). It isn't
  on Google Play yet. What remains is the developer's Play Console work in
  `docs/05-release.md` (`docs/roadmap.md`, M6).
- **Devices:** Android 8.0 (API 26) and later. Its layouts are phone
  layouts (`docs/03-platforms.md`).

## Goals

1. **Never lose a trip.** The Room database file `vacation_database` holds
   the user's only copy, so it changes only by tested migrations, never
   destructively (ADR-0003). Cloud backup is off (ADR-0004).
2. **Collect nothing.** No network access of any kind, and only three
   permissions: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` and
   `SCHEDULE_EXACT_ALARM` (ADR-0001). Play's Data safety answer ("no data
   collected or shared"), `PRIVACY.md` and `LICENSE` section 5 depend on it.
3. **Remind on time wherever Android allows it.**
   - Every vacation and event gets its reminders automatically (ADR-0005).
   - An event's comes an hour before it, exactly when Android allows
     (ADR-0014), and through Doze and Battery Saver otherwise (ADR-0031).
   - Reminders come back after a reboot, an update or a clock change
     (ADR-0025), and Vagor offers the Android settings that keep them coming
     (ADR-0014, ADR-0032).
4. **Accessible.** A blue scheme for colour-blind users, with WCAG AA
   contrast in both themes, nothing shown by colour alone and visible
   keyboard focus (ADR-0017). Touch targets of at least 48dp, text that
   grows with the font size, and screen titles that TalkBack announces
   (`docs/subsystems/theme-and-accessibility.md`).
5. **Dates and times the phone's way.** "Jul 6, 2031" in the US and "6 Jul
   2031" in the UK, always in the Gregorian calendar (ADR-0026). Times in
   the phone's 12- or 24-hour style. Dates, times and time zones come only
   from pickers, and none of their fields can be typed into (the clock's
   keyboard mode still lets the user type the hour and minute; ADR-0008).
6. **Meet Google Play's rules** for a paid app (ADR-0035,
   `docs/05-release.md`).

## Non-goals

- Network access of any kind: HTTP clients, remote WebView content,
  Firebase, analytics, crash reporting, ads, accounts or sync (ADR-0001).
- Cloud backup. A direct device-to-device transfer to a new phone is still
  allowed (ADR-0004).
- Importing a trip someone shared. It isn't supported, and whether Vagor
  should do it is still open (ADR-0009, `docs/roadmap.md` M7).
- Reading or writing the phone's calendar. Add to Calendar opens a calendar
  app's own new-event screen and needs no permission (ADR-0015).
- Booking travel, or checking plans against any outside source (`LICENSE`,
  section 6).
- Opt-in reminders. Reminders are automatic; opt-in ones would have to be
  saved in the database, which is a schema change (ADR-0005).
- `USE_EXACT_ALARM`, which Google Play allows only for calendar and
  alarm-clock apps (ADR-0014).
- A follow-system dark mode option. Dark mode is an on/off switch
  (ADR-0006).
- Wallpaper colours (DynamicColors), which would replace the blue
  (ADR-0017).
- Date, time and time zone fields that can be typed into (ADR-0008).
- Languages other than English (ADR-0036).
- Tablet layouts. Vagor's layouts are phone layouts, stretched across a
  tablet, and the store listing has no tablet screenshots
  (`docs/05-release.md`, step 6).
- Compose, ViewBinding, ViewModel, a DI framework or coroutines in Vagor's
  code, without the user's approval (ADR-0002).
- A release workflow. Releases are built and uploaded by hand (ADR-0011).

## Product requirements

These define "done" for feature work. Events were called excursions until
2026-10-25. The UI and code now say **events**, but the Room table keeps
its old name `excursions` (ADR-0010).

The Status column is point-in-time, as of 2026-11-07. The list was first
written down on 2026-10-25, against commit 2092255. Update a row when its
requirement changes, and record a changed decision with
`/record-decision`.

| # | Requirement | Asked for | Status |
| :- | :- | :- | :- |
| 1 | **Vacations:** create, edit and delete, with title, trip details, start date and end date. Trip details are free text over several lines, and optional; they replaced the hotel field (ADR-0012) | Trip details: by the user, 2026-10-26 | Implemented |
| 2 | **Events:** each one is planned on a date inside its vacation's date range, at a time in a time zone, with optional details. The time is required and picked from a clock; the time zone is picked from a searchable list and starts on the phone's zone. Events saved before 2026-10-27 have no time until they're next edited, and can't be saved again without one (ADR-0013) | Time, time zone and details: by the user, 2026-10-26 | Implemented |
| 3 | **Reminders:** system notifications for vacation start, vacation end and each event. Automatic for every item: a notification around 9:00 AM local time on each vacation's start and end dates (ADR-0005), and an hour before each event (ADR-0014, ADR-0031). A posted reminder leaves the shade once it's out of date (ADR-0027). See `docs/subsystems/reminders.md` | Automatic reminders: decided with the user, 2026-10-25. An hour before each event: 2026-10-26. Leaving the shade: 2026-10-31 | Implemented |
| 4 | **Sharing:** vacations and events can be shared with other people, as plain text through the Sharesheet. The vacation text lists its events with their dates and times, and each event has its own Share button, whose text adds its time zone and details. Importing a shared trip isn't supported (ADR-0009). An Add to Calendar button on the vacation page and the event form opens Google Calendar's own new-event screen (another calendar app's when Google Calendar isn't installed), filled in: a vacation all day over its dates, with its trip details and events as the description; an event at its time in its time zone, an hour long, with its details, time and vacation (`CalendarInsert.kt`). The label says "Calendar" because it may open another app, and Google's brand guidance asks for accurate wording (ADR-0015) | Plain text: decided with the user, 2026-10-25. Add to Calendar: by the user, 2026-10-26 | Implemented. Import: not supported, still open |
| 5 | **Local-only:** no network access; data leaves the app only through the Sharesheet or Add to Calendar; cloud backup off (ADR-0001, ADR-0004). See `AGENTS.md`, "Non-negotiable rules" | Cloud backup off: decided with the user, 2026-10-25. The rest: not recorded | Implemented |
| 6 | **Dark mode:** a switch on the Settings tab, on by default. An on/off switch, not a follow-system option (ADR-0006) | By the user, 2026-10-25 | Implemented (`ThemeSettings`, `VagorApplication`, `SettingsFragment`) |
| 7 | **Accessible colours:** a blue Material 3 scheme for colour-blind users, from the Okabe-Ito blue `#0072B2`. Every text/background pair meets WCAG AA (4.5:1; 3:1 for non-text UI) in both themes. Nothing is conveyed by colour alone, and there's no DynamicColors. Keyboard focus is visible: Material's focus rings (`focusRingsEnabled`), the dashboard's and the time zone list's own focus rings, and one on the calendar's days. `ThemeOverlay.Vagor.MaterialTimePicker` fills the selected AM/PM with the primary blue, since Material marks it by hue alone (ADR-0017) | Not recorded | Implemented; `ThemeContrastTest` enforces the contrast |
| 8 | **Google Play compliance:** see `docs/05-release.md`. Play Console steps are the developer's to do | Paid at US$1: decided with the user, 2026-11-07 (ADR-0035) | What the app needs is in place (`docs/05-release.md`, "Already in the app"). Not yet on Google Play: `docs/roadmap.md`, M6 |
| 9 | **Dashboard and navigation:** the app opens on a dashboard of every vacation with its dates. Current and upcoming vacations come first, soonest at the top; vacations that have ended go below them under a "Past" heading, most recent first (`Dashboard.rows`). A blue Add Vacation button sits on the dashboard. A bottom navigation bar switches between the Vacations and Settings tabs. Tapping a vacation opens its page with its events (ADR-0007) | By the user, 2026-10-25 | Implemented (`HomeActivity`, `VacationsFragment`, `EventListActivity`) |
| 10 | **Calendar date entry:** every date field opens a month calendar that drops down below the field, and dates can't be typed (`CalendarDropdown.kt`). The end date calendar starts at the start date; an event's calendar offers only its vacation's days. Vagor draws the calendar itself, always Gregorian, and marks today with a ring. An event's time comes from Material's clock (`MaterialTimePicker`, in the phone's 12- or 24-hour style) and its zone from `TimeZonePickerDialog`; neither can be typed into. All three use `makePickOnly()` (`PickOnlyField.kt`) (ADR-0008) | By the user, 2026-10-25 | Implemented |

## Assumptions

Revisit these if any turns out to be wrong.

- One developer, Joseph Anthony Abbott III, owns Vagor and does every Play
  Console, signing and account step. Coding agents work in the repository
  and leave their changes uncommitted for the user to review and commit.
- A vacation holds a few events, so the vacation page shows them as plain
  rows in a `ScrollView` (`docs/01-architecture.md`).
- People use Vagor on a phone running Android 8.0 or later. On a tablet the
  phone layouts stretch (`docs/03-platforms.md`).
- People read English (ADR-0036), but write dates and times the way their
  phone's region does (ADR-0026).
- The phone's calendar system may not be Gregorian (a Thai phone counts
  Buddhist-era years), so Vagor draws its own calendar and always formats
  dates in the Gregorian one (ADR-0008, ADR-0026).
- Travellers change time zones. An event keeps its own zone; a vacation's
  dates have none and follow the phone's, and so do their 9:00 AM reminders
  (`docs/subsystems/reminders.md`).
- Google Calendar may not be installed, and another calendar app, or none,
  may answer Add to Calendar (ADR-0015).
- A trip may be planned months ahead and Vagor left unopened until then,
  which Android's app standby buckets and hibernation work against
  (ADR-0031, ADR-0032).
- Google Play's rules are as last checked: on 2026-10-31, and its rules for
  selling a paid app on 2026-11-07 (`docs/05-release.md`).

## Top risks and mitigations

| Risk | Mitigation |
| :- | :- |
| Trips are lost: the database file is the only copy, and cloud backup is off | Migrations only, tested in `MigrationInstrumentedTest`; never `fallbackToDestructiveMigration`; the file is never renamed; every open goes through `AppDatabase.open()` (ADR-0003, `/change-database-schema`). Device-to-device transfer stays allowed (ADR-0004). `LICENSE` section 5.2 tells buyers that lost data can't be recovered |
| A test run deletes someone's real trips | Instrumented tests write the app's real `vacation_database`, and a `connectedDebugAndroidTest` run uninstalls the app, so they run only on an emulator or a test device, never on a phone that holds real trips (`docs/04-quality.md`, `.claude/rules/instrumented-tests.md`). Release-build checks reach only an emulator (`adb -e`, `docs/05-release.md`, step 3) |
| Reminders come late or not at all: Doze, Battery Saver, app standby buckets, exact alarms denied by default on Android 14+, a Restricted battery setting, notifications turned off | Exact alarms when Android allows, with an offer of Alarms & reminders (ADR-0014); allow-while-idle alarms otherwise (ADR-0031); the notifications-off dialog and the Notification Settings button (`docs/subsystems/reminders.md`). The listing promises an exact time only when Alarms & reminders is allowed ("within the hour before each event (exactly an hour before when you allow alarms and reminders)"), and `LICENSE` section 6 calls Vagor a planning aid whose reminders can be late or stopped |
| Alarms are wiped by a reboot or a force stop, and the exact ones (event reminders) when Android's exact alarms are taken away | Every reminder is rebuilt from the database in each new process, and on boot, update, clock and time zone broadcasts (ADR-0025) |
| Android's limit of 500 alarms makes every set call throw, which once crashed every start | Every alarm goes through `ReminderScheduler.setAlarm`, which tries once more and then leaves the reminder unset (ADR-0024). Which reminders get the 500 places is still open (`docs/roadmap.md`, M7) |
| Android 12+ hibernates an unused Vagor, which cancels every alarm | The offer of App info after saving a vacation that ends more than two months ahead, and the Settings tab's App Info button (ADR-0032). Company-managed phones and some makers' phones can't be detected, so the app says Android "may" pause Vagor |
| A privacy promise stops being true: a library pulls in a permission, or the policy's two copies drift apart | Check the merged manifest after adding any dependency (`docs/02-dependency-policy.md`). Change `PRIVACY.md` and `privacy_policy_text` together, and keep `LICENSE` sections 5 and 6 and the store listing true (`.claude/rules/manifest-privacy-and-legal.md`). `ThirdPartyLicensesTest` keeps `NOTICE` and the in-app list alike |
| Google Play rejects the app or the account: exact alarm policy, Data safety, trader status, target API | Only `SCHEDULE_EXACT_ALARM`, never `USE_EXACT_ALARM` (ADR-0014); targetSdk 36; the checklist in `docs/05-release.md`. Open Play decisions are in `docs/roadmap.md`, M7 |
| A release build breaks where debug builds work, because only release builds run R8 | Try the release build on an emulator before every upload (`docs/05-release.md`, step 3). Keep rules go in `app/src/main/keepRules/` (ADR-0021) |
| The upload key or its passwords leak | Signing reads only environment variables, a broken setup fails the build, signed builds skip the configuration cache, and no keystore is ever committed (ADR-0030). With Play App Signing, Play Console can reset a lost or leaked upload key |
| A calendar app reads a vacation's all-day times wrongly | `CalendarTimes` picks times that every app checked (Android's original calendar, Etar, Fossify) reads correctly. Google Calendar's reading couldn't be checked without a signed-in account (ADR-0015) |
| A changed application ID makes a different app | Never change `applicationId` (ADR-0020) |
