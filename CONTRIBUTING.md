# Contributing to Vagor

Vagor is an offline Android vacation planner, written in Kotlin. It is
proprietary, all rights reserved by Joseph Anthony Abbott III, and every
pull request needs a pitch the maintainer has confirmed first. This guide
covers how to pitch a change, set up a machine, check your work and open a
pull request.

The project's rules are in [`AGENTS.md`](AGENTS.md), which every
contributor follows, person or coding agent. Where this guide and
`AGENTS.md` disagree, `AGENTS.md` wins. The design is in [`docs/`](docs):
start with [`docs/00-overview.md`](docs/00-overview.md) and
[`docs/01-architecture.md`](docs/01-architecture.md), then the subsystem
spec of the area you are changing (the table "Read before working" in
`AGENTS.md` says which).

## Contribution workflow

1. **Pitch first.** Pitch an addition or change by email to
   jabbottpublicsupport@gmail.com before making a pull request. Pull
   requests without a confirmed pitch will be denied. GitHub Issues,
   Discussions and private vulnerability reporting are off for this
   repository (checked 2026-11-07), so email is the way in.
2. **Wait for confirmation.** Confirmation of a pitched concept is required
   before making a pull request to modify the code base.
3. **Read before working.** Read the docs that `AGENTS.md` → "Read before
   working" lists for the files you will touch, and any ADR the change
   relies on in [`docs/decisions.md`](docs/decisions.md).
4. **Make the smallest complete change** on a branch of your fork, keeping
   to the one confirmed pitch.
5. **Check your work** (see [Checks](#checks)).
6. **Open a pull request** that names the confirmed pitch and says what you
   ran, with the real results, and what you could not run (for example a
   check that needs a device). CI runs on the pull request (see
   [Checks](#checks)), and the maintainer reviews it (see `CODEOWNERS`).

### Roadmap work

[`docs/roadmap.md`](docs/roadmap.md) lists what is done and what is open.

- **M6 (the first Google Play release)** is the developer's work in a
  terminal and in Play Console ([`docs/05-release.md`](docs/05-release.md),
  steps 1–8). No contributor or agent can do it.
- **M7 (open decisions and backlog)** holds questions that are the
  maintainer's to decide, such as whether Vagor should import a trip
  someone shared, or which reminders get the 500 alarm places past
  Android's limit, and whether the user is told.

## Ground rules

These are summarized from `AGENTS.md`, which is authoritative.

- **Local-only.** No network access of any kind: no `INTERNET` or
  `ACCESS_NETWORK_STATE`, HTTP clients, remote WebView content, Firebase,
  analytics, crash reporting, ads, accounts or sync. The manifest declares
  only `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` and
  `SCHEDULE_EXACT_ALARM`. Data leaves the app only when the user shares it
  through the Android Sharesheet or hands one vacation or event to a
  calendar app with Add to Calendar (ADR-0001).
- **User data is never lost.** All of it lives in the Room database file
  `vacation_database`, and there is no other copy. Never rename that file,
  never use `fallbackToDestructiveMigration`, and never clear tables outside
  tests. A schema change bumps `AppDatabase.version`, adds a `Migration` to
  `AppDatabase.MIGRATIONS`, keeps the schema JSON Room exports to
  `app/schemas`, and tests the upgrade in `MigrationInstrumentedTest`
  ([`docs/subsystems/data-and-storage.md`](docs/subsystems/data-and-storage.md);
  ADR-0003).
- **What Vagor promises stays true.** The privacy policy exists twice,
  [`PRIVACY.md`](PRIVACY.md) and `privacy_policy_text` in
  `app/src/main/res/values/strings.xml`: change both together, and keep
  them true to what the app does. [`LICENSE`](LICENSE) section 5 (how the
  app handles data) and section 6 (reminders can be late or stopped) must
  stay true too, and so must the store listing in
  `fastlane/metadata/android/en-US/`.
- **Instrumented tests run only on an emulator or a test device,** never on
  a phone that holds real trips. They write to the app's real database, set
  real alarms and post real notifications, and a
  `connectedDebugAndroidTest` run uninstalls the app afterwards, which
  deletes its data ([`docs/04-quality.md`](docs/04-quality.md)).
- **The application ID never changes** (`io.github.jabbott_iii.vagor`):
  after the first Play upload a new ID is a different app (ADR-0020).
- **No new libraries and no version changes** (dependency, AGP, Gradle,
  Kotlin or SDK) without the maintainer's approval. Propose one in your
  pitch, with the evaluation
  [`docs/02-dependency-policy.md`](docs/02-dependency-policy.md) describes.
  After adding any dependency, check the merged manifest for permissions it
  pulls in.
- **The UI stack stays as it is:** XML layouts, `AppCompatActivity` and
  `findViewById`. No Compose, ViewBinding, ViewModel, DI framework or
  coroutines without the maintainer's approval (ADR-0002).
- **Never `USE_EXACT_ALARM`, `READ_CALENDAR` or `WRITE_CALENDAR`.** Google
  Play allows `USE_EXACT_ALARM` only for calendar and alarm-clock apps, and
  Add to Calendar needs no permission and never writes to the calendar
  provider directly.
- **Smallest complete change.** No unrelated refactors, renames or
  reformatting. Unless the pitch explicitly calls for it, don't change
  `namespace`, the database schema, manifest permissions, backup settings,
  or the CI workflows.
- **No secrets.** Never commit a keystore or password. Release signing reads
  the upload key only from environment variables
  ([`docs/05-release.md`](docs/05-release.md), step 3).
- **Never add new findings to `app/detekt-baseline.xml`** to make a check
  pass.
- **Report what you ran, honestly.** Never say a check passed unless you
  ran it and saw it pass, and say which checks you did not run. If a change
  can only be checked on a device, say that it wasn't verified. CI can only
  be checked by pushing, so never report a workflow change as passing until
  a run on GitHub shows it.

## Development setup

### Toolchain

| Tool | Version and notes |
| :- | :- |
| JDK | 17 or later to run Gradle; `gradle/gradle-daemon-jvm.properties` asks for a JDK 25 daemon |
| Android SDK | Platform 37, found through `ANDROID_HOME` or `sdk.dir` in `local.properties` |
| Gradle | 9.6.1, through the wrapper (`./gradlew`) |
| Android Gradle Plugin | 9.4.1, with built-in Kotlin |
| Kotlin | 2.4.20, set through the root `buildscript` classpath |
| KSP | 2.3.12, for Room |
| SDK levels | compileSdk 37, targetSdk 36 (the Google Play minimum since 2026-08-31), minSdk 26 |
| An emulator | For the instrumented tests, release-build checks and screenshots. CI uses an API 37 (`'37.0'`) `google_apis` image |

`local.properties` (machine-specific `sdk.dir`) is tracked in git even
though its own header says it shouldn't be. Don't commit changes to it.

### Build and run

1. Open the repository root in Android Studio.
2. Let Android Studio install Android SDK 37 and any other packages
   requested by Gradle if prompted.
3. Build from Android Studio, or run `./gradlew assembleDebug`.
4. Launch the `app` configuration on an emulator or Android device running
   Android 8.0+.

## Commands

| Command | What it does |
| :- | :- |
| `./gradlew assembleDebug` | Builds the debug APK |
| `./gradlew testDebugUnitTest` | JVM tests in `app/src/test` |
| `./gradlew testDebugUnitTest --tests 'com.example.vagor.DateValidatorsTest'` | One JVM test class |
| `./gradlew lint` | Android lint |
| `./gradlew detekt ktlintCheck` | Kotlin static analysis and style |
| `./gradlew ktlintFormat` | Fixes most ktlint findings |
| `./gradlew connectedDebugAndroidTest` | Espresso tests; needs an emulator or device. Use only an emulator or a test device (see Ground rules) |
| `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.vagor.HomeFlowInstrumentedTest` | One instrumented test class |
| `./gradlew bundleRelease` | The release bundle, built with R8; unsigned while `VAGOR_UPLOAD_KEYSTORE` is unset or blank, as in CI |

[`docs/04-quality.md`](docs/04-quality.md) describes each test and what it
does to the phone it runs on.

### The Makefile

`Makefile.txt`'s `tag`, `push-tag` and `release` targets create and push
git tags. Only the developer runs them: `tag` and then `push-tag` at release
time ([`docs/05-release.md`](docs/05-release.md), step 3, which says not to
use `release` later on), and coding agents never do. There is no release
workflow (`cd.yml` was removed at the user's request on 2026-10-25), so
nothing is built or uploaded from a tag: releases are built and uploaded by
hand.

## Checks

Run these before you open a pull request:

```bash
./gradlew lint
./gradlew detekt ktlintCheck
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Then, as the change needs:

- **Screens, forms, reminders, the calendar or the database:**
  `./gradlew connectedDebugAndroidTest` on an emulator, or the one class
  your change touches.
- **A schema change:** `MigrationInstrumentedTest`, which tests the upgrade
  with `MigrationTestHelper` on a database file of its own.
- **Anything that relies on reflection, class names or resources looked up
  by name:** only release builds run R8, and the instrumented tests use the
  debug build, so check a release build on an emulator
  ([`docs/05-release.md`](docs/05-release.md), step 3).
- **Colours:** `ThemeContrastTest` (in `testDebugUnitTest`) checks that
  every text/background pair meets WCAG AA in both themes.
- **Third-party material that ships in the app:** add it to
  [`NOTICE`](NOTICE) and to `third_party_licenses_text` in strings.xml;
  `ThirdPartyLicensesTest` checks they list the same things. One under
  another license also needs that license's text added.
- **A screen the store listing shows:** retake its screenshot with invented
  trips on an emulator ([`docs/05-release.md`](docs/05-release.md),
  step 6).

CI runs on every push and pull request to `main` and `develop`:

- `ci.yml` runs `lint`, `detekt`, `ktlintCheck`, `assembleDebug`, an
  unsigned `bundleRelease` (to run R8 and lint's release checks) and
  `testDebugUnitTest`, then `connectedAndroidTest` on an API 37
  `google_apis` emulator.
- `security.yml` runs CodeQL on the Kotlin code.
- A pull request from a fork gets a read-only token, so CI skips the step
  that publishes the unit test report as a check run; the test results are
  still uploaded.

## Coding conventions

`AGENTS.md` → "Conventions" holds them all. The ones most often missed:

- **Style:** ktlint's IntelliJ IDEA code style without trailing commas,
  lines up to 120 characters; detekt's default rules.
- **Dates:** stored as `MM/dd/yyyy` strings, parsed and validated only
  through `DateValidators`, and shown only through `dateText` in
  `TripText.kt`. Never show a stored date string directly. A date field is
  a `DateField`: read and write its `date`, never its text.
- **Times and zones:** stored as 24-hour `HH:mm` strings and IANA zone IDs
  such as `Asia/Tokyo`, parsed only through `DateValidators.parseLocalTime`
  and `parseZone`. Display text uses ICU (`android.icu`); `java.time`
  formatting is for stored values. Check each ICU call's API level against
  minSdk 26.
- **Logic:** new date or reminder math goes in plain Kotlin beside
  `DateValidators` so it can be unit tested. Reminder scheduling stays in
  `ReminderScheduler`.
- **Text:** user-visible text goes in `res/values/strings.xml`. The app is
  English only.
- **Colours:** from theme attributes only; never hard-code a text or
  background colour in a layout. Don't convey anything by colour alone, and
  don't add DynamicColors.
- **Sizes:** every button on a page is full width, one above the other,
  styled `Widget.Vagor.Button` or `Widget.Vagor.Button.Outlined`. Text
  fields are `wrap_content` with `minHeight="57dp"` and
  `importantForAutofill="no"`. Touch targets are at least 48dp each way.
- **Dialogs:** every dialog is a `DialogFragment` (`MessageDialog` for a
  message with an OK button), so it's still open after a rotation.
- **Database access:** every Activity, Fragment or receiver that reads or
  writes data opens its own Room instance with `AppDatabase.open()` and
  closes it again; DAOs are synchronous. A shared instance or background
  threading is a refactor: pitch it first.
- **Screens:** every screen except `HomeActivity` calls
  `setEdgeToEdgeContentView()` instead of `setContentView()`, and sets its
  own title. Screens pass data through Intent extras with literal keys
  (`vacationId`, `title`, `details`, `startDate`, `endDate`, `eventId`).

## Tests

- JVM tests live in `app/src/test`, and instrumented (Espresso) tests in
  `app/src/androidTest`.
- The instrumented tests delete only the rows they create, in `@After`, so
  a run that is killed partway can leave a test trip behind. Every
  instrumented run starts the app's process, which re-arms every stored
  trip's reminders as any start does (`VagorApplication`).
- Instrumented tests set a date field with `setDate` and check it with
  `withDate` (`DateFieldTesting.kt`), and leave another app's screen
  through `leaveOtherAppsScreen()` (`OtherAppTesting.kt`), which waits
  until Android has closed it.
- Never take `SCHEDULE_EXACT_ALARM` away during a test: Android stops the
  app, test and all.

## Documentation

Update the docs in the same pull request when the change affects them:

- The subsystem spec in `docs/subsystems/` for a change in behavior or
  design.
- An ADR in [`docs/decisions.md`](docs/decisions.md) for a decision made or
  changed, with the template at the top of that file.
- [`docs/roadmap.md`](docs/roadmap.md) for roadmap work.
- `AGENTS.md` for a new command, file or rule.
- [`PRIVACY.md`](PRIVACY.md) and `privacy_policy_text` together, for any
  change in how the app handles data.
- [`NOTICE`](NOTICE) and `third_party_licenses_text` together, for
  third-party material that ships in the app.
- The store listing in `fastlane/metadata/android/en-US/` when what it
  describes or shows changes.
- [`docs/05-release.md`](docs/05-release.md) when the release process
  changes, and this file and [`SECURITY.md`](SECURITY.md) when the
  contribution or reporting process changes.

## Commits and pull requests

- Keep a pull request to the one confirmed pitch.
- Commit messages start with a type and a short summary, as `git log`
  shows: `feat:`, `fix:`, `docs:`, `build:`, `ci:`, `style:`, `test:` or
  `chore:`, as in `feat: add vacations and events to Google Calendar`.
- Do not commit build output, keystores, bundles or APKs (`.gitignore` lists
  them), or changes to `local.properties`.

## Coding agents

The repository is set up for coding agents: `AGENTS.md` holds the rules for
any agent, and [`CLAUDE.md`](CLAUDE.md) and `.claude/` add Claude Code's
skills (`/next-task`, `/record-decision`, `/evaluate-dependency`,
`/change-database-schema`, `/update-store-listing`), path-scoped rules and
permission guardrails.

- Agents never commit, amend, push, tag, open pull requests or publish
  anything: they leave their changes uncommitted for review and suggest a
  commit message, and a person commits.
- You are responsible for every line you submit, whoever or whatever wrote
  it. Review an agent's change as you would anyone's, and run the checks
  yourself.

## Security issues

Do not report a vulnerability in a pull request or a commit message. Report
it privately by email, as [`SECURITY.md`](SECURITY.md) describes; that file
also says what is in scope.

## License

- Vagor's own files are proprietary, all rights reserved, and not licensed
  to anyone except as the License section of README.md says (GitHub's
  view-and-fork license while the repository is public); they carry no
  license headers. LICENSE is the end-user license agreement for people who
  buy the app. The third-party files listed in NOTICE, such as the Gradle
  wrapper and the Material Design icons, keep their own license.
- Vagor is proprietary (see LICENSE). By opening a pull request, you agree
  that Joseph Anthony Abbott III may use, change, distribute and license
  your contribution as part of Vagor on any terms, and you confirm that you
  have the right to give that permission.
- Don't put Vagor's own code under an open-source license. The only
  third-party license text the app ships is the Apache License 2.0 in
  `res/raw/apache_license_2_0.txt` (added 2026-10-31), because the bundled
  libraries' license asks for it to go with the app; the Settings tab shows
  it under Third-Party Licenses (ADR-0029).
