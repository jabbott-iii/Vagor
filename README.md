# Vagor

Vagor is an offline Kotlin Android vacation planner, ported from
`jabbott-iii/university-projects` → `Mobile-Application-Development-Android`.
Everything stays on the phone: no internet access, no accounts, no ads and
no analytics.

- **Plan:** vacations with their dates and trip details, and the events in
  them, each at a time in a time zone.
- **Remember:** a notification on the day each vacation starts and ends, and
  before each event.
- **Share:** a vacation or an event as plain text through Android's share
  menu, or one of them to a calendar app with Add to Calendar.

It is one Gradle module (`:app`) with XML layouts and AppCompat, for Android
8.0 (API 26) and later. Its data is one Room database on the phone, and
cloud backup is off. Its application ID is `io.github.jabbott_iii.vagor`.

## Status

**Built; not yet on Google Play.** Version 1.0 (`versionCode` 1) has every
feature below. Vagor is to be sold on Google Play for US$1 (decided with the
user on 2026-11-07, ADR-0035).

What remains is the developer's work in
[`docs/05-release.md`](docs/05-release.md) (steps 1–8), in a terminal and in
Play Console, which only the developer can do:

- the upload key;
- hosting PRIVACY.md and LICENSE at public URLs;
- a payments profile;
- creating the app as paid at US$1 and answering Play Console's
  declarations (trader status, App content, Data safety);
- entering the listing from `fastlane/metadata/`;
- then an internal test, a 12-tester, 14-day closed test (closed testers
  must buy the app), and the application for production access.

| Milestone | What it delivers | Status |
| :- | :- | :- |
| M0 | Port to Kotlin (2026-09-12 to 2026-09-22) | Done |
| M1 | Dashboard, dark mode and calendar entry (2026-10-25 to 2026-10-26) | Done |
| M2 | Trip details, event times and time zones, reminders, Add to Calendar (2026-10-27) | Done |
| M3 | Play readiness: license, CI, static analysis, R8, icon, licenses screen, alarm limit, accessibility (2026-10-31) | Done |
| M4 | Dates the phone's way, reminder timeouts, store listing, Doze and hibernation, dialogs (2026-11-01) | Done |
| M5 | Paid release preparation: EULA and US$1 price (2026-11-07) | Done |
| M6 | First Google Play release: the developer's Play Console steps | Open |
| M7 | Open decisions and backlog | Open |

[`docs/roadmap.md`](docs/roadmap.md) lists every task.

## Features

- **Dashboard:** the landing screen — every vacation with its dates, soonest
  first, with ended vacations below under "Past", and a blue Add Vacation
  button
- **Bottom navigation:** switches between the Vacations and Settings tabs
- **Vacation page:** tap a vacation to see its details and events; edit or
  share it (the shared text lists its events), add it to a calendar, and add
  events
- **Vacation details:** add, update, and delete a vacation
- **Event details:** add, update, delete, share, and add to a calendar an
  event, with its date, time, time zone and optional details
- **Calendar date entry:** every date field opens a month calendar that
  drops down below it; an event's calendar offers only its vacation's days
- **Time and time zone entry:** an event's time comes from a clock in the
  phone's 12- or 24-hour style, and its time zone from a searchable list
  that starts on the phone's zone
- **Reminders:** a notification around 9:00 AM local time on the day each
  vacation starts and ends, and within the hour before each event (exactly
  an hour before once Alarms & reminders is allowed); one that arrives on
  time leaves the notification shade once it's out of date, at the end of
  its day or an hour after the event starts, and one that Android delivers
  late is still shown
- **Settings tab:** a dark mode switch (dark is the default), whether event
  reminders are exact (with a button to Android's Alarms & reminders
  screen), a Notification Settings button while notifications are off,
  whether Android may pause Vagor while it's unused (with an App Info
  button, Android 12 and later), the privacy policy, the third-party
  licenses, and the app's version
- **Theme:** a blue Material 3 colour scheme chosen for colour-blind users;
  every text colour meets WCAG AA contrast in light and dark mode
- **Add to Calendar:** opens Google Calendar's new-event screen (or another
  calendar app's when Google Calendar isn't installed) filled in with a
  vacation, all day over its dates, or an event at its time in its time
  zone; the calendar app saves it, and Vagor needs no calendar permission
- **Persistence:** Room database storing vacations and events on the device
  only
- **Dates:** shown the way the phone writes them ("Jul 6, 2031" in the US,
  "6 Jul 2031" in the UK), always in the Gregorian calendar
- **Validation:** dates stored as strict `MM/dd/yyyy` (two-digit month and
  day, four-digit year, nothing extra), event dates constrained to the
  vacation window (moving or shortening a vacation can't leave its events
  outside it), a required event time, and prevention of deleting vacations
  that still have events

## Documentation

| Document | Covers |
| :- | :- |
| [`docs/00-overview.md`](docs/00-overview.md) | Purpose, goals, non-goals, the product requirements, assumptions, risks |
| [`docs/01-architecture.md`](docs/01-architecture.md) | Screens and navigation, code layout, data model, data flow, database access, insets, messages |
| [`docs/02-dependency-policy.md`](docs/02-dependency-policy.md) | The dependency rule, the libraries in use, and the exception process |
| [`docs/03-platforms.md`](docs/03-platforms.md) | Android versions, behaviour by API level, device and locale notes |
| [`docs/04-quality.md`](docs/04-quality.md) | Tests and their side effects, static analysis, accessibility checks, CI |
| [`docs/05-release.md`](docs/05-release.md) | Build types, R8, versioning, signing, and the Play release checklist |
| [`docs/decisions.md`](docs/decisions.md) | Architecture decision records: why things are the way they are |
| [`docs/roadmap.md`](docs/roadmap.md) | Done milestones, the first Play release, open decisions and backlog |
| [`docs/subsystems/data-and-storage.md`](docs/subsystems/data-and-storage.md) | Entities, DAOs, the database and its migrations, stored formats, backup rules |
| [`docs/subsystems/screens-and-navigation.md`](docs/subsystems/screens-and-navigation.md) | The screens, Intent extras, titles, saving, deleting and validation |
| [`docs/subsystems/settings-screen.md`](docs/subsystems/settings-screen.md) | The Settings tab, dark mode, licenses and messages |
| [`docs/subsystems/dates-times-and-pickers.md`](docs/subsystems/dates-times-and-pickers.md) | Date, time and zone parsing, the drop-down calendar, the pickers, display text |
| [`docs/subsystems/reminders.md`](docs/subsystems/reminders.md) | Alarms, notifications, rebuilding, exact alarms, hibernation, the alarm limit |
| [`docs/subsystems/sharing-and-calendar.md`](docs/subsystems/sharing-and-calendar.md) | Sharing through the Sharesheet and Add to Calendar |
| [`docs/subsystems/theme-and-accessibility.md`](docs/subsystems/theme-and-accessibility.md) | Colours, contrast, focus, sizing, TalkBack, the icon and the Play graphics |
| [`PRIVACY.md`](PRIVACY.md) | The privacy policy, also in the app under Settings > Privacy Policy |
| [`LICENSE`](LICENSE) | The Vagor Perpetual End-User License Agreement |
| [`NOTICE`](NOTICE) | The third-party software that ships in the app |

## Build and run

You need a JDK 17 or later to run Gradle
(`gradle/gradle-daemon-jvm.properties` asks for a JDK 25 daemon) and an
Android SDK with platform 37, found through `ANDROID_HOME` or `sdk.dir` in
`local.properties`.

1. Open the repository root in Android Studio.
2. Let Android Studio install Android SDK 37 and any other packages
   requested by Gradle if prompted.
3. Build from Android Studio, or run:
   ```bash
   ./gradlew assembleDebug
   ```
4. Launch the `app` configuration on an emulator or Android device running
   Android 8.0+.

`./gradlew testDebugUnitTest` runs the JVM tests. The instrumented tests
(`./gradlew connectedDebugAndroidTest`) write to the app's real database,
set real alarms, and uninstall the app afterwards, which deletes its data:
run them only on an emulator or a test device, never on a phone that holds
real trips. [`CONTRIBUTING.md`](CONTRIBUTING.md) lists every command.

To build a signed release bundle and publish it on Google Play, see
[`docs/05-release.md`](docs/05-release.md). The store listing's text,
graphics and screenshots are in `fastlane/metadata/android/en-US/`.

## Notes

- This port keeps the original Room-backed persistence while converting the
  implementation to Kotlin. The screens were reorganised on 2026-10-25
  around a dashboard with bottom navigation.
- **Reminders** are scheduled automatically by `ReminderScheduler` with
  `AlarmManager` alarms and posted as notifications by `MyReceiver`
  ([`docs/subsystems/reminders.md`](docs/subsystems/reminders.md)).
  - An event's reminder uses an exact alarm an hour before it starts, in
    the event's time zone. Android 12 and later need the user's permission
    for that (Alarms & reminders, which Vagor offers after saving an event);
    without it the reminder comes some time in the hour before the event,
    and once Vagor has gone unused for a while (8 days on Android 13 and
    later), Android lets only one such reminder a day come on time.
  - Events saved before they had a time are reminded around 9:00 AM on
    their date.
  - Vacation reminders are inexact: on Android 12 and later one usually
    arrives within an hour after 9:00 AM; on Android 8–11 an idle phone or
    Battery Saver can hold it until the phone wakes or Battery Saver is off.
  - Setting Vagor's battery use to Restricted holds every reminder until
    Vagor is opened.
  - From Android 12, Android pauses an app that isn't opened for about
    three months, which stops its reminders until it's opened again; after
    saving a trip that ends more than two months ahead, Vagor offers the App
    info setting that turns this off, and the Settings tab shows it too.
  - Reminders are rescheduled after a reboot, a clock or time zone change,
    an app update, when exact alarms are allowed, and whenever the app
    starts.
- Events were called excursions in earlier versions. The database table
  keeps the name `excursions`, so no schema migration was needed. Database
  version 2 (October 2026) added each event's details, time and time zone;
  events saved before it keep their date and get a time the next time
  they're edited.
- Builds from before the application ID changed to
  `io.github.jabbott_iii.vagor` (they used `com.example.vagor`) are a
  separate app on the phone, and their data doesn't carry over.
- Cloud backup is turned off, so trips are not copied to Google Drive. See
  [`PRIVACY.md`](PRIVACY.md).

## Contributing

Vagor is proprietary. Pitch an addition or change by email to
jabbottpublicsupport@gmail.com before making a pull request: pull requests
without a confirmed pitch will be denied.
[`CONTRIBUTING.md`](CONTRIBUTING.md) covers the rules, the setup and the
checks.

Coding agents follow [`AGENTS.md`](AGENTS.md) (Claude Code also reads
[`CLAUDE.md`](CLAUDE.md)). Report security issues privately by email, as
[`SECURITY.md`](SECURITY.md) describes.

## License

Proprietary. Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.

Vagor is licensed to buyers under the
[Vagor Perpetual End-User License Agreement](LICENSE): a perpetual,
non-transferable license to install and use the app on devices they own or
control. The source code is not licensed to anyone, except for the license
GitHub's Terms of Service give other GitHub users while this repository is
public: to view it and to fork it on GitHub. For any other use, ask for
permission at jabbottpublicsupport@gmail.com. The third-party software the
app includes keeps its own licenses, listed in [NOTICE](NOTICE) and in the
app under Settings > Third-Party Licenses.
