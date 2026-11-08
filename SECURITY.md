# Security Policy

## Supported versions

Vagor is not on Google Play yet and has no releases
([`docs/05-release.md`](docs/05-release.md)). Until the first release, only
`main` is supported. Security fixes land on `main`, and reach users in the
next Google Play release.

| Version | Supported |
| :- | :- |
| `main` | :white_check_mark: |
| Google Play releases | None yet |

## Reporting a vulnerability

Report vulnerabilities privately, by email to
jabbottpublicsupport@gmail.com.

GitHub's private vulnerability reporting, GitHub Issues and GitHub
Discussions are all off for this repository (checked 2026-11-07), so email
is the only way to report one. Do not report a vulnerability in a pull
request, a commit message, a fork or anywhere else that is public.

Include as much of this as you can:

- The app version from the bottom of the Settings tab, such as
  "Version 1.0 (1)": the `versionName`, then the `versionCode`.
- Where the build came from: Google Play, or built from source, with the
  commit (`git rev-parse HEAD`) and whether it was a debug or a release
  build.
- The Android version and the phone model (or the emulator's system image).
- The steps that trigger it, or a proof of concept: for example the other
  app, or the `adb` command, that reaches Vagor.
- What happens and what you expected, and the impact: what an attacker has
  to control, and what they gain.
- A suggested fix, if you have one.

Use invented trips in anything you attach (screenshots, recordings, a copy
of the database), never your real ones, and remove names, email addresses
and other private data from any log or output.

## What to expect

Vagor has one maintainer, Joseph Anthony Abbott III, who answers reports by
email.

| Step | Commitment |
| :- | :- |
| Acknowledgment | Not set yet |
| Assessment (whether it is a vulnerability, and how severe) | Not set yet |
| Fix | Lands on `main`, and reaches users in the next Google Play release |
| Disclosure and credit | Not set yet |

Please keep the details private until a fix is released, or until you and
the maintainer agree on a date.

## Scope

Vagor runs only on the phone: no network access, no server and no accounts
(ADR-0001). The manifest declares only `POST_NOTIFICATIONS`,
`RECEIVE_BOOT_COMPLETED` and `SCHEDULE_EXACT_ALARM`. What reaches it is
other apps on the same phone, Android's own broadcasts, and the user's own
taps.

In scope:

- **Trip data on the device.** Vagor keeps every vacation and event in the
  Room database file `vacation_database`, and the dark mode switch in the
  SharedPreferences file `settings`, in its private app storage. Another app
  reading or changing them, or Vagor writing them anywhere else, is in
  scope.
- **Exported components.** Vagor's own manifest exports only
  `HomeActivity`, the launcher. The merged manifest also exports AndroidX's
  `ProfileInstallReceiver`, which only callers holding
  `android.permission.DUMP` can reach
  ([`docs/02-dependency-policy.md`](docs/02-dependency-policy.md), "What the
  libraries add to the manifest"). `MyReceiver` (which posts reminders) and
  `ReminderRescheduleReceiver` (which rebuilds them on boot, app updates,
  clock and time zone changes, and exact-alarm permission changes) are
  declared `exported="false"`, and the other activities have no intent
  filters. Another app opening a screen other than the launcher, or making
  Vagor post, set or cancel a reminder, is in scope.
- **PendingIntents.** Each reminder's alarm and its notification tap use a
  `PendingIntent` made with `FLAG_IMMUTABLE`, identified by a data URI from
  `ReminderKey` such as `vagor://reminder/vacation/<id>/start`. A way for
  another app to fill in, change, fire or cancel one is in scope.
- **Backup.** Cloud backup is off: `allowBackup="false"`, plus exclusions in
  `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml`
  (ADR-0004). Trip data reaching a cloud backup is in scope.
- **Shared text and calendar intents.** Share Vacation and Share Event send
  text through the Sharesheet (`Intent.ACTION_SEND` inside
  `Intent.createChooser`). Add to Calendar sends one vacation or event to
  Google Calendar's new-event screen, or to another calendar app's when
  Google Calendar isn't installed (`Intent.ACTION_INSERT`), and needs no
  calendar permission. Either one carrying more than the vacation or event
  the user picked is in scope.
- **Notification content.** A reminder shows its vacation's title and
  dates, or its event's title, start time and vacation, and, expanded, the
  trip or event details. A tap opens the vacation's page or the event. A
  reminder that shows another trip's data, or opens a trip it wasn't for,
  is in scope.
- **Network access.** Any way the app reaches the network, including
  through a library that pulls in a permission or network code
  ([`docs/02-dependency-policy.md`](docs/02-dependency-policy.md)).
- **Release signing.** A way the build exposes the upload key or its
  passwords, which come only from environment variables
  ([`docs/05-release.md`](docs/05-release.md), step 3).
- **CI workflows** in `.github/workflows/`, such as script injection or a
  token with more permissions than its job needs.

Out of scope:

- **A direct device-to-device transfer** to a new phone, which can copy
  Vagor's data. It is allowed by design (decided with the user on
  2026-10-25, ADR-0004).
- **What the receiving app does** with shared text or a calendar item. It is
  handled under that app's own terms ([`LICENSE`](LICENSE), section 5.3;
  [`PRIVACY.md`](PRIVACY.md)), and a calendar app may save it to an account
  that syncs.
- **Someone using the unlocked phone.** Vagor has no PIN, encryption or wipe
  function of its own. Android keeps its data in the app's private storage,
  which other apps can't access.
- **Late or missing reminders** caused by Android's notification, alarm,
  battery, standby or hibernation settings. These are documented limits
  ([`docs/subsystems/reminders.md`](docs/subsystems/reminders.md);
  [`LICENSE`](LICENSE), section 6): report one by email as an ordinary bug.
- **What the instrumented tests do to the phone they run on.** They write
  to the app's real database, set real alarms, post real notifications and
  uninstall the app, by design, which is why they run only on an emulator
  or a test device ([`docs/04-quality.md`](docs/04-quality.md)).
- **Google Play's handling** of downloads, purchases, refunds and crash
  reports, under Google's own policies ([`PRIVACY.md`](PRIVACY.md),
  "Google Play").
- **Bugs in Android, AndroidX, Material Components, Room, or a calendar or
  share-target app.** Report those to their maintainers, and tell us if
  Vagor needs to work around one.

## How the code is kept safe

- **No network.** No `INTERNET` or `ACCESS_NETWORK_STATE` permission, HTTP
  client, remote WebView content, Firebase, analytics, crash reporting, ads,
  accounts or sync. After adding any dependency, the merged manifest is
  checked for permissions it pulls in.
- **A short list of libraries:** AndroidX (Activity, AppCompat,
  ConstraintLayout, Core and Room) and Material Components, with what they
  depend on, all listed in [`NOTICE`](NOTICE). A new one, or a version
  change, needs the maintainer's approval
  ([`docs/02-dependency-policy.md`](docs/02-dependency-policy.md)).
- **No exported component of Vagor's own** but the launcher (AndroidX's
  `ProfileInstallReceiver` is exported only to callers holding
  `android.permission.DUMP`), and every `PendingIntent` is immutable.
- **No calendar permission.** Add to Calendar never reads or writes the
  calendar provider, and `READ_CALENDAR` and `WRITE_CALENDAR` are never
  added.
- **Autofill off on text fields** (`importantForAutofill="no"`), so
  autofill services leave trip text alone.
- **Cloud backup off,** and both backup rule files exclude the `root`,
  `file`, `database`, `sharedpref` and `external` domains.
- **Release signing from environment variables only.** Keystore files
  (`*.jks`, `*.keystore`, `*.p12`, `*.pfx`) are git-ignored, signed builds
  run with `--no-configuration-cache` to keep the passwords out of
  `.gradle/configuration-cache`, and signing never goes through CI.
- **The Gradle wrapper is pinned by checksum** (`distributionSha256Sum` in
  `gradle/wrapper/gradle-wrapper.properties`).
- **CodeQL** (`security.yml`) scans the Kotlin code on every push and pull
  request to `main` and `develop`, and each week. `ci.yml` runs lint,
  detekt and ktlint on the same pushes and pull requests.
- **Read-only workflow tokens by default.** Only the jobs that need it are
  given write access: the CodeQL job to security events, and CI's
  build-and-test job to check runs, for its unit test report.
