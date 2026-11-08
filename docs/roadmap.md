# Roadmap

How to use this file:

- Work top to bottom. A task is **unblocked** when everything in its
  `Depends` line is checked.
- Tick a box only after its acceptance criteria are met **and** its checks
  were run and passed (see "Definition of done" in `AGENTS.md`).
- A task marked `Owner: developer (an agent can't do this)` is Play
  Console, signing or account work. `/next-task` says so and stops.
- A task marked `Owner: the user decides` is a decision that is the user's
  call. Ask the user, record the answer with `/record-decision`, and only
  then do the work it leads to.
- If a criterion needs an emulator or a device you can't run, leave the box
  unchecked. Add an indented note saying exactly what remains:
  `  - ⏳ needs an emulator: run HomeFlowInstrumentedTest`.
- Add indented `Note:` lines under a task for what the work settled:
  measured numbers, conventions chosen, follow-ups.
- Exit criteria for a milestone are checked by a human.
- The done milestones, M0 to M5, were written down on 2026-11-07 from the
  git history. Each task lists its commits and their date (`Done:`) and the
  decisions behind it (`See:`). A decision's date can be a day before the
  commit that built it (`docs/decisions.md`).

Status: Vagor is built and tested, and M0 to M5 are done. It isn't on
Google Play yet: M6 is the developer's Play Console work
(`docs/05-release.md`), and M7 holds the open decisions and the backlog.

---

## M0 — Port to Kotlin (2026-09-12 to 2026-09-22)

**Goal:** the vacation planner from `jabbott-iii/university-projects` →
`Mobile-Application-Development-Android`, ported into Vagor as a Kotlin
Android app with its Room-backed persistence.

- [x] **M0.1 — Start the repository and transfer the files**
  - Done: 05bc488, 660a658, c13e776, 786a39a, 9524ac6 (2026-09-12);
    848bad3, e022bbe (2026-09-15)
  - See: ADR-0002, ADR-0003

- [x] **M0.2 — Security scanning workflow**
  - Done: e0172cd (2026-09-12); 7bd9667, 5285f66 (2026-09-15)
  - See: ADR-0023
  - Note: CodeQL first, then Kotlin analysis and dependency scanning, and
    the Android SDK setup step removed from CI. Fixed on 2026-10-31 (M3.2).

- [x] **M0.3 — Port the vacation planner as a Kotlin Android app (pull
  request #4)**
  - Done: 5aaaaae, a1d58c9, 5b491a6, 13413c4, 5efc6cb, f24620a, be93095,
    f350cba, 7cf5922, b20761c, 02b6ed6, 9f45416 (2026-09-16)
  - See: ADR-0002, ADR-0003
  - Note: the pull request also tightened vacation range validation,
    refined the DAO contracts and validation tests, added the core home
    flow instrumentation test, validated alerts against persisted records,
    removed generated artifacts, pinned the Gradle wrapper checksum and
    documented the Android SDK requirement.

- [x] **M0.4 — AndroidX, license owner and the name Vagor**
  - Done: 071474c (2026-09-16); e9dbd74, 8cff1cf (2026-09-21); 8d674c0
    (2026-09-22)
  - See: —
  - Note: 8cff1cf updated the copyright year and owner in `LICENSE`, then
    the Apache License 2.0 (ADR-0018 records the change to proprietary on
    2026-10-31), and 8d674c0 changed the product name to Vagor and removed
    the third-party software list.

---

## M1 — Dashboard, dark mode and calendar entry (2026-10-25 to 2026-10-26)

**Goal:** the screens reorganised around a dashboard with bottom
navigation, with automatic reminders, dark mode, events in place of
excursions, and dates picked from a calendar.

- [x] **M1.1 — Remove stale files**
  - Done: 2092255 (2026-10-25)
  - See: —
  - Note: removed the old `AGENTS.md` and `CODE_OF_CONDUCT.md`.

- [x] **M1.2 — A full Kotlin app with automatic reminders**
  - Done: 3a7fd03 (2026-10-25)
  - See: ADR-0002, ADR-0005
  - Note: added `ReminderSchedule.kt`, `ReminderScheduler.kt`,
    `ReminderRescheduleReceiver.kt`, `ReminderScheduleTest` and
    `ReminderInstrumentedTest`.

- [x] **M1.3 — Dark mode, strict dates, and events in place of excursions**
  - Done: 47f8bf6 (2026-10-25)
  - See: ADR-0001, ADR-0004, ADR-0006, ADR-0010, ADR-0017, ADR-0020, ADR-0030
  - Note: added `ThemeSettings.kt`, `VagorApplication.kt`, `EdgeToEdge.kt`,
    `ThemeContrastTest`, `PRIVACY.md` and the first `PLAY_RELEASE.md` (now
    `docs/05-release.md`); renamed the `Excursion` entity to `Event`;
    changed the backup rules; first used the application ID
    `io.github.jabbott_iii.vagor`.

- [x] **M1.4 — Dashboard, bottom navigation and the drop-down calendar**
  - Done: 16069dc (2026-10-25), e2048d1 (2026-10-26)
  - See: ADR-0007, ADR-0008, ADR-0011
  - Note: 16069dc added `Dashboard.kt`, `VacationsFragment.kt`,
    `SettingsFragment.kt`, `CalendarDropdown.kt`, `TripText.kt` and
    `DashboardTest`, and removed `cd.yml`. e2048d1 drew the calendar itself
    (`CalendarMonth.kt`, `CalendarDayBackground.kt`, `CalendarMonthTest`).

- [x] **M1.5 — Uniform full-width page buttons**
  - Done: 83af75b (2026-10-26)
  - See: ADR-0016

**Note (M1):** ADR-0009, sharing as plain text, was decided with the user on
2026-10-25 and has no commit of its own. ADR-0012 to ADR-0015 were decided
on 2026-10-26 and built in M2.

---

## M2 — Trip details, event times and time zones, reminders, Add to Calendar (2026-10-27)

**Goal:** the requirements the user asked for on 2026-10-26: trip details,
event details with a time and a time zone, a reminder an hour before each
event, and Add to Calendar.

- [x] **M2.1 — Trip details in place of the hotel field**
  - Done: a4939ac (2026-10-27)
  - See: ADR-0012

- [x] **M2.2 — Event details, time and time zone; database version 2**
  - Done: 7f80138 (2026-10-27)
  - See: ADR-0003, ADR-0013

- [x] **M2.3 — A reminder an hour before each event, exact when allowed**
  - Done: ab51718 (2026-10-27)
  - See: ADR-0014, ADR-0025

- [x] **M2.4 — Add vacations and events to Google Calendar**
  - Done: bbd61bb (2026-10-27)
  - See: ADR-0015

---

## M3 — Play readiness: license, CI, static analysis, R8, icon, licenses screen, alarm limit, accessibility (2026-10-31)

**Goal:** the app and the repository made ready for a Google Play release.

- [x] **M3.1 — Proprietary, all rights reserved**
  - Done: 4fd5bd9 (2026-10-31)
  - See: ADR-0018

- [x] **M3.2 — Fix the failing workflows and remove checks that never ran**
  - Done: 8604a25, 75abf6d (2026-10-31)
  - See: ADR-0023
  - Note: 75abf6d added the step that installs command-line tools 22.0,
    and a check that the AVD's target is `android-37.0`.

- [x] **M3.3 — detekt and ktlint**
  - Done: 17ae1ee, 55efd43 (2026-10-31)
  - See: ADR-0022
  - Note: 17ae1ee added both with baselines for the existing code; 55efd43
    formatted the Kotlin to the ktlint style once and dropped the ktlint
    baseline.

- [x] **M3.4 — R8 on release builds; the unused template activity dropped**
  - Done: f7bfc82 (2026-10-31)
  - See: ADR-0021, ADR-0036
  - Note: also added `androidResources.localeFilters`.

- [x] **M3.5 — Vagor's suitcase launcher icon and the Play graphics**
  - Done: 635ed50 (2026-10-31)
  - See: `docs/subsystems/theme-and-accessibility.md`

- [x] **M3.6 — The support email as the privacy contact; Third-Party
  Licenses in Settings**
  - Done: 3626413 (2026-10-31)
  - See: ADR-0019, ADR-0029

- [x] **M3.7 — Survive Android's alarm limit, explain notifications that
  are off, and keep events inside their vacation**
  - Done: 130feed (2026-10-31)
  - See: ADR-0024, ADR-0025, ADR-0028
  - Note: added `NotificationsOff.kt` and `ReminderLimitInstrumentedTest`.

- [x] **M3.8 — Screen titles for TalkBack, fields that grow with the font,
  48dp calendar days, and the version in Settings**
  - Done: 49b869d (2026-10-31)
  - See: `docs/subsystems/theme-and-accessibility.md`,
    `docs/subsystems/settings-screen.md`

---

## M4 — Dates the phone's way, reminder timeouts, store listing, Doze and hibernation, dialogs (2026-11-01)

**Goal:** the rest of the Play-readiness work: dates shown the phone's way,
reminders that leave the shade, the store listing and release checklist,
reminders through Doze, and dialogs that survive rotation.

- [x] **M4.1 — Show dates the way the phone writes them, and clear
  reminders once they're out of date**
  - Done: c23ac3a (2026-11-01)
  - See: ADR-0026, ADR-0027
  - Note: added `DateField.kt` and `DateFieldTesting.kt`.

- [x] **M4.2 — The Play release checklist, store listing and screenshots;
  stop builds when signing is set up wrong; start time zone searches at the
  first match**
  - Done: f45b3a0 (2026-11-01)
  - See: ADR-0030, `docs/05-release.md`

- [x] **M4.3 — Wait for Android's Alarms & reminders screen to close, so
  its Back can't close the next test's form**
  - Done: e7f2051 (2026-11-01)
  - See: `docs/04-quality.md`

- [x] **M4.4 — The hibernation offer, event reminders through Doze and
  Battery Saver, dialogs across rotation, and a redrawn feature graphic**
  - Done: 7fdf0e3 (2026-11-01)
  - See: ADR-0031, ADR-0032, ADR-0033
  - Note: added `Hibernation.kt`, `MessageDialog.kt` and
    `OtherAppTesting.kt`.

---

## M5 — Paid release preparation: EULA and US$1 price (2026-11-07)

**Goal:** Vagor ready to be sold on Google Play for US$1.

- [x] **M5.1 — The Denarii Dolor end-user license agreement, and the Play
  release prepared for a US$1 paid app**
  - Done: 9c3b224 (2026-11-07)
  - See: ADR-0034, ADR-0035

---

## M6 — First Google Play release

**Goal:** Vagor on Google Play's production track, sold for US$1. Every
step is in `docs/05-release.md`, "Play release checklist", and only the
developer can do it. An agent can answer questions about a step, but not
take it.

- [ ] **M6.1 — Create the upload key**
  - Owner: developer (an agent can't do this)
  - Depends: —
  - Do: `docs/05-release.md`, step 1. Create the keystore with
    `keytool`, outside the repository, and back it up with its password.
  - Accept: the keystore exists outside the repository folder, and it and
    its password are backed up somewhere safe.

- [ ] **M6.2 — Get the first upload ready**
  - Owner: developer (an agent can't do this)
  - Depends: —
  - Do: `docs/05-release.md`, step 2. The first upload keeps
    `versionCode` 1, and its release note, `changelogs/1.txt`, is ready.
    Commit, push, and wait for CI to pass for that commit on GitHub.
  - Accept: CI passed on GitHub for the commit to be built, and
    `git status` shows nothing outside `.idea/`.

- [ ] **M6.3 — Build, check, keep and tag the signed bundle**
  - Owner: developer (an agent can't do this)
  - Depends: M6.1, M6.2
  - Do: `docs/05-release.md`, step 3. Build with
    `--no-configuration-cache`, check the certificate, try the release
    build on an emulator, keep a copy of the bundle and `mapping.txt`, and
    tag the build.
  - Accept: `keytool -printcert` prints the certificate (`Owner:` and a
    `SHA256:` fingerprint) and its fingerprint matches the upload key; the
    release build passes step 3's checks on an emulator.

- [ ] **M6.4 — Publish the privacy policy and the license agreement**
  - Owner: developer (an agent can't do this)
  - Depends: —
  - Do: `docs/05-release.md`, step 4. Publish `PRIVACY.md` as a web page,
    identical to the in-app text, and `LICENSE` as an exact copy named
    `LICENSE.txt`.
  - Accept: each URL, opened in a private window, shows the text instead
    of downloading a file.

- [ ] **M6.5 — Payments profile, the paid app and its declarations**
  - Owner: developer (an agent can't do this)
  - Depends: M6.4
  - Do: `docs/05-release.md`, step 5. Set up the payments profile first,
    then create the app as **Paid**, set its price, and answer trader
    status, App content and Data safety.
  - Accept: the payments profile is verified; the app exists as Paid at
    US$1.00; every App content section in step 5's table is answered.
  - Note: M7.2 to M7.6 are decided before or during this step.

- [ ] **M6.6 — Store settings and listing**
  - Owner: developer (an agent can't do this)
  - Depends: M6.5
  - Do: `docs/05-release.md`, step 6. Enter the store settings, paste the
    listing text, and upload the graphics, with their alt text, from
    `fastlane/metadata/android/en-US/`.
  - Accept: the listing in Play Console matches the files in
    `fastlane/metadata/android/en-US/`.

- [ ] **M6.7 — Internal testing**
  - Owner: developer (an agent can't do this)
  - Depends: M6.3, M6.5
  - Do: `docs/05-release.md`, step 7.1, after confirming a real Android
    phone in Play Console (step 7). Upload the bundle, keep Play App
    Signing's Google-generated key, and install Play's build from the opt-in
    link.
  - Accept: the upload key's SHA-256 fingerprint in App integrity > App
    signing matches step 3's, and Play's build passes step 3's checks.

- [ ] **M6.8 — Closed testing: 12 testers for 14 days**
  - Owner: developer (an agent can't do this)
  - Depends: M6.4, M6.5, M6.6, M6.7
  - Do: `docs/05-release.md`, step 7.2. Closed testers must buy the app.
    Keep notes on what testers report and what you fix, and read the
    pre-launch report for every upload.
  - Accept: at least 12 testers stayed opted in for 14 days in a row.

- [ ] **M6.9 — Apply for production**
  - Owner: developer (an agent can't do this)
  - Depends: M6.8
  - Do: `docs/05-release.md`, step 7.3.
  - Accept: Play approves the application. If it doesn't, the closed test
    goes on.

- [ ] **M6.10 — The first production release**
  - Owner: developer (an agent can't do this)
  - Depends: M6.9
  - Do: `docs/05-release.md`, step 7.4.
  - Accept: the release passes Play's review and appears on the store.

- [ ] **M6.11 — Android vitals**
  - Owner: developer (an agent can't do this)
  - Depends: M6.8
  - Do: `docs/05-release.md`, step 8, during the closed test and after
    every release.
  - Accept: Vagor stays below Play's bad behaviour thresholds.

**Exit criteria (M6):** Vagor is on the Play Store as a paid app, from a
bundle that was tested on the internal and closed tracks.

---

## M7 — Open decisions and backlog

**Goal:** the questions still open, and the work waiting on them. Each
decision is recorded with `/record-decision` once the user makes it.

- [ ] **M7.1 — Show the end-user license agreement to buyers**
  - Owner: the user decides
  - Depends: —
  - Do: decide where buyers see `LICENSE`. Nothing in the app or the store
    listing shows it yet (ADR-0034).

- [ ] **M7.2 — Refund policy**
  - Owner: the user decides
  - Depends: —
  - Do: decide the refund policy and record it, with its date, in
    `docs/05-release.md`, step 5. Play makes the developer responsible
    for it, and `LICENSE` mentions refunds only in 10.3, which leaves it
    open.

- [ ] **M7.3 — How long sale records and support emails are kept**
  - Owner: the user decides
  - Depends: —
  - Do: decide how long they're kept. `PRIVACY.md` says what the
    developer uses them for, not how long they're kept. A change to the
    policy changes `PRIVACY.md` and `privacy_policy_text` together.

- [ ] **M7.4 — Trader status, and distribution in the EEA and the UK**
  - Owner: the user decides
  - Depends: —
  - Do: declare trader status in Play Console (`docs/05-release.md`, step
    5). An account can declare non-trader only if none of its apps is
    distributed in the EEA or the UK, and selling Vagor there may make the
    developer a trader. Google suggests a legal professional if you're
    unsure. A new account declares when it's created, an existing one by
    2027-02-01.

- [ ] **M7.5 — EEA classification: Digital Content or Service**
  - Owner: the user decides
  - Depends: —
  - Do: choose the classification in Play Console's tax and compliance
    settings (`docs/05-release.md`, step 5). Buyers can get a refund on a
    Service for 14 days after buying it.

- [ ] **M7.6 — US state app store age laws and Age Signals**
  - Owner: the user decides
  - Depends: —
  - Do: decide whether and how these laws apply to Vagor, and record the
    decision and its date in `docs/05-release.md`, step 5. Vagor doesn't
    use Age Signals today. Using the API would add a Google Play library:
    run `/evaluate-dependency` first (`docs/02-dependency-policy.md`).

- [ ] **M7.7 — Importing shared trips**
  - Owner: the user decides
  - Depends: —
  - Do: decide whether Vagor should be able to import a trip someone
    shared. It's still open (ADR-0009).

- [ ] **M7.8 — Reminders past Android's 500-alarm limit**
  - Owner: the user decides
  - Depends: —
  - Do: decide which reminders get the 500 places past the limit, and
    whether the user is told (ADR-0024).

- [ ] **M7.9 — A shared database instance or background threading**
  - Owner: the user decides
  - Depends: —
  - Do: a refactor of the one-instance-per-screen pattern. Propose it to
    the user first; don't start it unasked (ADR-0002).

- [ ] **M7.10 — Repository housekeeping**
  - Owner: developer (an agent can't do this)
  - Depends: —
  - Do: `docs/05-release.md`, step 9. Delete the old GitHub secrets the
    removed CD and SonarQube workflows read, if any exist, and stop
    tracking `local.properties` and `.idea/`'s machine-specific files. None
    of it blocks a release.
  - Note: once `local.properties` is untracked, update the docs that say
    it is tracked (`docs/05-release.md`, step 9).

### Declined (not planned)

Offered together on 2026-10-31, with the choices that became ADR-0026 and
ADR-0027; the user picked none of these four.

- A separate debug application ID (`.debug` suffix), so debug builds and
  instrumented tests never touch the Play app's trips on the same phone.
- Asking before discarding unsaved edits on a vacation or event form.
- Testing on older Android versions (Android 8 to 16 system images).
- CI hardening: pinning the runner image (`ubuntu-24.04`), checksums, and
  lint reports.
