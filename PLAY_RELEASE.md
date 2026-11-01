# Publishing Vagor on Google Play

This is the checklist for Vagor's first Google Play release and every one after it. The repository
holds what a release needs: the code, the signing setup, the privacy policy, and the store
listing's text, graphics and screenshots (`fastlane/metadata/android/en-US/`). The steps below
happen in a terminal and in Play Console, and only the developer can do them. Play's rules were
last checked on 2026-10-31.

## Already in the app

- **Package name** `io.github.jabbott_iii.vagor`. Play rejects `com.example.*`, and the package name
  can never change after the first upload. (The Kotlin package in the source is still
  `com.example.vagor` and shows up in class names; Play's `com.example` restriction applies only to
  the package name.)
- **Target API level 36** (Android 16), required for new apps since 2026-08-31. Every screen draws
  edge to edge and keeps its content clear of the system bars.
- **Privacy policy** inside the app (Settings > Privacy Policy). The same text is in
  [PRIVACY.md](PRIVACY.md), and its contact is jabbottpublicsupport@gmail.com.
- **No data collection.** No internet permission, no accounts, ads, analytics or crash reporting,
  and no `AD_ID` permission. Cloud backup is off. The app requests only `POST_NOTIFICATIONS`,
  `RECEIVE_BOOT_COMPLETED` and `SCHEDULE_EXACT_ALARM`. The bundle also lists AndroidX's
  app-internal `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which needs no declaration.
- **Exact alarms only through `SCHEDULE_EXACT_ALARM`**, for the reminder an hour before each
  event. The user allows it in Android's Alarms & reminders screen (Android 14 and later start
  without it), and without it the reminder comes 50 to 60 minutes before. The app doesn't declare
  `USE_EXACT_ALARM`, which Play allows only for alarm clock and calendar apps. As of 2026-10-31
  Play's exact alarm policy asks for a declaration only for `USE_EXACT_ALARM`; if Play Console shows
  a permissions declaration at the first upload, answer it with this use (a reminder an hour before
  each event the user plans), and allow for a review that can take several weeks.
- **No native code**, so the 16 KB page size requirement doesn't apply.
- **R8 on release builds**: the code is shrunk and obfuscated and unused resources are removed.
  The bundle carries R8's mapping file, and Play uses it to deobfuscate crash reports, so there's
  no separate deobfuscation file to upload.
- **Its own launcher icon**, which is also the themed icon and the Android 12+ splash screen.
- **Third-party licenses** in Settings > Third-Party Licenses, matching [NOTICE](NOTICE).
- **The version** in Settings ("Version 1.0 (1)": `versionName`, then `versionCode`), so a tester
  can say which build they have.
- **Upload signing from environment variables only** (step 3), and the build stops if they're set
  up wrong instead of quietly making an unsigned bundle.

## 1. Create the upload key (once)

```bash
mkdir -p ~/keys && chmod 700 ~/keys
keytool -genkeypair -keystore ~/keys/vagor-upload.jks -alias vagor-upload -keyalg RSA -keysize 2048 -validity 10000
```

- keytool asks for a keystore password, then for your name and organisation, which go into the
  certificate. It makes a PKCS12 keystore, which has a single password: the key's password is the
  keystore's, so give that one wherever a key password is asked for.
- 10,000 days is about 27 years, past Play's minimum (a key valid until at least 22 October 2033).
- Back up the keystore and its password somewhere safe, outside the repository. Keystore files
  (`*.jks`, `*.keystore`, `*.p12`, `*.pfx`) are git-ignored, but keep them out of the repository
  folder anyway.
- With Play App Signing (step 7), this is only the upload key. If it's lost or leaked, Play
  Console can reset it; that takes time, but the app keeps its identity.

## 2. Before each upload

1. Raise `versionCode` by 1 in `app/build.gradle.kts`, except for the first upload, which keeps
   1 (its release note, `changelogs/1.txt`, is ready). Every upload needs a higher one than the
   last, test tracks included. Give every upload its own `versionName` too, the version users
   see (`1.0`, then `1.0.1` for a fix during the closed test, `1.1`, and so on), since each one
   gets its own tag (step 3).
2. Write the release note in `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`, 500
   characters at most. It goes into each release you create from that bundle (step 7).
3. Commit, push, and wait for CI to pass for that commit on GitHub. Build only from a clean tree
   (`git status` shows nothing outside `.idea/`, which Android Studio rewrites and which doesn't
   go into the bundle; `git restore .idea` discards those changes): the bundle records the commit
   it was built from even when the tree holds changes that aren't in it.

## 3. Build and check the signed bundle

```bash
export VAGOR_UPLOAD_KEYSTORE=~/keys/vagor-upload.jks
export VAGOR_UPLOAD_KEY_ALIAS=vagor-upload
IFS= read -rs -p "Keystore password: " VAGOR_UPLOAD_KEYSTORE_PASSWORD; echo
export VAGOR_UPLOAD_KEYSTORE_PASSWORD
export VAGOR_UPLOAD_KEY_PASSWORD="$VAGOR_UPLOAD_KEYSTORE_PASSWORD"
./gradlew --no-configuration-cache clean bundleRelease
keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab
```

- **Why `--no-configuration-cache`:** with Gradle's configuration cache, which the project turns
  on, a build stores the passwords (encrypted, but with the key on the same machine) under
  `.gradle/configuration-cache/` in the project folder.
- **Signing set up wrong stops the build.** With `VAGOR_UPLOAD_KEYSTORE` set, Gradle fails when
  there's no keystore at that path or one of the other three variables is missing, whatever the
  task, debug builds and tests included. Without it the bundle is unsigned, which is how CI
  builds it. A relative path is read from the repository root, and the shell expands `~` only
  when it isn't in quotes.
- **keytool must print the certificate** (`Owner:` and a `SHA256:` fingerprint). "Not a signed jar
  file" means the bundle is unsigned: don't upload it. (`jarsigner -verify` is no check: it exits
  successfully on an unsigned bundle too.) The fingerprint must match
  `keytool -list -v -keystore ~/keys/vagor-upload.jks -alias vagor-upload` and, after the first
  upload, the upload key certificate in Play Console under App integrity > App signing.

**Try the release build on an emulator.** Only release builds run R8, and the instrumented tests
use the debug build, so check the release build before every upload. The first command
uninstalls Vagor, which deletes its trips, so run this only with an emulator, never on a phone
that holds real trips (`adb -e` reaches only an emulator):

```bash
adb -e uninstall io.github.jabbott_iii.vagor
serial=$(adb -e get-serialno) && ANDROID_SERIAL=$serial ./gradlew --no-configuration-cache installRelease
unset VAGOR_UPLOAD_KEYSTORE VAGOR_UPLOAD_KEY_ALIAS VAGOR_UPLOAD_KEYSTORE_PASSWORD VAGOR_UPLOAD_KEY_PASSWORD
```

- `installRelease` builds the same R8 output as the bundle and installs it signed with the upload
  key. A debug build is signed with another key, so Android won't replace it with this one:
  that's why the uninstall comes first.
- Gradle's install task installs on every connected device unless `ANDROID_SERIAL` names one, so
  the second line takes the emulator's serial from `adb -e`, and stops if there isn't exactly one
  emulator running.
- The last line clears all four signing variables, since nothing after it needs them. While
  they're set, any Gradle command run without `--no-configuration-cache` stores the passwords in
  the configuration cache, and while `VAGOR_UPLOAD_KEYSTORE` is set without the others, every
  Gradle command in that shell stops.

Then check, on the emulator:

- add a vacation (the drop-down calendar) and an event (the clock and the time zone list), and see
  both on the dashboard and the vacation page;
- share the vacation, and use Add to Calendar;
- switch dark mode off and on, and rotate an open form;
- open the privacy policy and the third-party licenses in Settings;
- for the reminders, add an event about 70 minutes ahead: its notification comes about 10 minutes
  later (up to 20 when Alarms & reminders is off).

**Keep a copy of what you upload.** The next build overwrites `app-release.aab` and
`app/build/outputs/mapping/release/mapping.txt`. Copy both to a folder named after the
`versionCode`, outside the repository: the mapping turns stack traces that users send you back
into readable ones (Play does that for its own crash reports from the copy inside the bundle).

**Tag the build** right away, while the tree is still clean and HEAD is the commit you built,
with `versionName` written in three parts (`1.0` becomes `v1.0.0`), the form
[Makefile.txt](Makefile.txt) accepts: `make -f Makefile.txt tag VERSION=v1.0.0`. Push the tag
after the upload with `make -f Makefile.txt push-tag VERSION=v1.0.0`, or delete it with
`git tag -d v1.0.0` if that bundle is never uploaded. Don't use the Makefile's `release` target
later on: it tags whatever HEAD is by then. The Makefile only tags; there's no release workflow,
so nothing is built or uploaded from a tag.

## 4. Publish the privacy policy

Play needs the policy at a public, non-PDF URL as well as in the app. Publish
[PRIVACY.md](PRIVACY.md) as a web page and keep it identical to the in-app text
(`privacy_policy_text` in `app/src/main/res/values/strings.xml`). Its contact is
jabbottpublicsupport@gmail.com, the same address as the store listing's contact email; to change
it, change the Contact section in both copies.

GitHub Pages works, but on 2026-10-31 `https://jabbott-iii.github.io/` redirected to
`jabbott.dev`, so a Pages site for this repository would be served at `https://jabbott.dev/Vagor/`.
Open the URL you enter in Play Console in a private window to check that it shows the policy.

## 5. Create the app and answer its declarations

**Create the app:** app name `Vagor: Vacation Planner` (from `title.txt`), default language English
(United States) – en-US, App (not a game), and **Free**. A free app can never become paid.

**Trader status:** Play Console asks every active developer account for this declaration once: a new
account when it's created, an existing one by 2027-02-01. The answer must reflect whether you act
commercially as a trader, and misstating it can get the account suspended. A personal account
that declares itself a trader must give a contact phone number, and from 2027-03-01 Google Play
shows it with the account's address. For a non-trader, Play tells users where trader status
must be shown (the EEA and the UK, for example) that consumer protection law doesn't apply to
their dealings with the developer. An account that doesn't distribute apps in the EEA or the UK
can declare non-trader.

**App content** (Policy and programs > App content):

| Section | Answer |
| --- | --- |
| Privacy policy | The public URL from step 4 |
| App access | All functionality is available without special access (no login) |
| Ads | No ads |
| Content rating | Complete the questionnaire. Vagor is a planning utility with no violence, sexual content, gambling or other mature content. It has no chat or other way for users to reach each other inside the app; Share hands text to apps the user picks |
| Target audience | Choose the age groups you intend, such as 18 and over. Including children under 13 brings in the Families policy. See the age laws below |
| Data safety | See below |
| Advertising ID | No: Vagor doesn't use it, and the bundle has no `AD_ID` permission. Recheck after adding any library |
| Government apps | No |
| Financial features | None |
| Health apps | None |
| News apps | No |

**Data safety answers**

- **Does your app collect or share any of the required user data types?** No.
  - Play counts data as *collected* only when it is sent off the device. Vagor keeps everything on
    the device and has no internet access.
  - Sharing is only the user's own tap on Share, which opens Android's share menu, or on Add to
    Calendar, which hands one vacation or event to a calendar app. Play exempts transfers the user
    starts and expects ("User-initiated action" in Play's Data safety help).
- Recheck these answers if Vagor ever adds a library that uses the network, analytics, crash
  reporting, or ads.

**US state app store age laws.** Texas SB 2420 is in effect. Play says these laws' requirements
include taking in users' age ranges, and parental approval for significant changes, from the app
store, and telling the store about significant changes; the developer decides what counts as one,
and can tell Play on the Age Signals page in Play Console without a new release. Play offers an
Age Signals API for this but doesn't require apps to use it, and says it's each developer's
responsibility to decide how these laws apply to their app. Vagor doesn't use Age Signals today,
and whether and how the laws apply to it is the developer's decision, not yet made. Using the API
would add a Google Play library: a dependency change that needs the merged-manifest permission
check in CLAUDE.md and a NOTICE entry. Record the decision and its date here when you make it.

## 6. Store settings and listing

**Store settings** (Grow users > Store presence > Store settings):

- **App category:** Travel and Local.
- **Tags:** up to five of Play's tags that describe a trip planner.
- **Contact email:** jabbottpublicsupport@gmail.com. Play shows it on the listing, and it's also
  the privacy contact. A website is optional (the privacy policy page works); a phone number is
  optional (required on a personal account that declares itself a trader) and is shown publicly
  too.

**Store listing text.** Paste each field from `fastlane/metadata/android/en-US/`. The files follow
fastlane's layout, though nothing here uses fastlane; they have no trailing newline, and they're in
US English ("color") to match the listing's default language.

| Field | File | Limit |
| --- | --- | --- |
| App name | `title.txt` | 30 characters |
| Short description | `short_description.txt` | 80 characters; no final period, since it's one sentence |
| Full description | `full_description.txt` | 4,000 characters |

Keep the text true to the app. Don't claim features Vagor doesn't have: importing trips, sync or
cloud backup (Google Calendar may sync an item saved there, but that's Google Calendar, not Vagor).
Name Google Calendar only in plain text saying what Vagor works with: no Google logo or Calendar
icon in the graphics or screenshots, and no Google name in the app's title or icon. Don't promise
reminders to the minute either ("about an hour before"): they depend on Alarms & reminders and
the phone's battery settings.

**Graphics**, in `fastlane/metadata/android/en-US/images/`:

| Asset | File | Play's rules |
| --- | --- | --- |
| App icon | `icon.png` | 512×512, 32-bit PNG, sRGB, 1 MB at most; full square with no shadow, since Play rounds the corners itself |
| Feature graphic | `featureGraphic.png` | 1024×500, JPEG or 24-bit PNG with no alpha; required to publish |
| Phone screenshots | `phoneScreenshots/01_dashboard.png` to `06_light_mode.png` | JPEG or 24-bit PNG with no alpha, 2 to 8, each side 320 to 3,840 px and the long side at most twice the short one. Four or more at 1080×1920 (9:16) let Play use them in its promotions |

Upload the screenshots in filename order. The icon and feature graphic are drawn from the launcher
icon's suitcase (`app/src/main/res/drawable/ic_launcher_foreground.xml`) on its blue `#0072B2`.
Leave the tablet screenshot sets empty: Vagor's layouts are phone layouts, stretched across a
tablet, and Play shows the phone screenshots on tablets.

**Alt text** for each graphic (140 characters at most):

| File | Alt text |
| --- | --- |
| `icon.png` | Vagor's icon: a white rolling suitcase on a blue square |
| `featureGraphic.png` | Vagor's white suitcase icon on blue, beside the name Vagor and the words Your trips, planned offline |
| `01_dashboard.png` | The Vacations tab listing upcoming trips, soonest first, each with its dates, trip details and days until it starts |
| `02_vacation_page.png` | A trip's page with buttons to edit, share or add it to a calendar, above its events with their dates and times |
| `03_event_form.png` | Editing an event: its title, date, time, time zone and details, with buttons to save, delete, share or add to a calendar |
| `04_calendar.png` | The drop-down calendar for an event's date, where only the days of its trip can be picked |
| `05_time_zone.png` | The searchable time zone list, opened at the event's zone, Lisbon, with each zone's name and offset from GMT |
| `06_light_mode.png` | A trip to Kyoto in light mode, with two events at their times in Japan |

**Retaking the screenshots** after the screens change. Screenshots are public, so use invented
trips, and an emulator, never a phone that holds real trips:

```bash
adb -e shell wm size 1080x1920
adb -e shell settings put global sysui_demo_allowed 1
adb -e shell am broadcast -a com.android.systemui.demo -e command enter
adb -e shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1030
adb -e shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb -e shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e fully true
adb -e shell am broadcast -a com.android.systemui.demo -e command network -e mobile hide
adb -e exec-out screencap -p > 01_dashboard.png
adb -e shell am broadcast -a com.android.systemui.demo -e command exit
adb -e shell wm size reset
```

Clear the notification shade first: on the API 37 emulator, demo mode didn't hide notification
icons.
`screencap` writes PNGs with an alpha channel, which Play refuses, so remove it before uploading
(for example `convert 01_dashboard.png -alpha off PNG24:01_dashboard.png` with ImageMagick 6,
Ubuntu's `imagemagick` package; ImageMagick 7 calls it `magick` instead of `convert`).

## 7. Release: internal, then closed, then production

A new personal developer account can't publish to production until a closed test has run, so the
first upload goes to a test track.

Every release you create, on any track, has its own release notes: paste
`fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` for that release's bundle between
the `<en-US>` and `</en-US>` tags (500 characters at most). Users see the production release's
notes as "What's new".

1. **Internal testing.** Create a release on the Internal testing track and upload the bundle.
   - The first upload sets up Play App Signing. Keep the default, a Google-generated app signing
     key; don't export and upload your own. Play then signs the app, and your key from step 1 is
     only the upload key.
   - Note the app signing key's and the upload key's SHA-256 fingerprints from App integrity >
     App signing, and check the upload one against step 3.
   - Add yourself as a tester (up to 100 are allowed). On the test device, first uninstall the
     build from step 3 (`adb -e uninstall io.github.jabbott_iii.vagor` on an emulator): Play signs
     the app with the app signing key, not the upload key, so Android won't install Play's build
     over it. Then install Vagor from Play's opt-in link, signed in to the Play Store as the
     tester (an emulator needs a Google Play system image, not a `google_apis` one), and repeat
     the checks from step 3. This also tests the APKs Play builds from the bundle.
2. **Closed testing.** Finish steps 4 to 6 first: a closed track can't be published without the
   privacy policy, Data safety and the other App content answers.
   - Create a closed testing release with the bundle you tested (or a newer `versionCode`).
   - Invite at least 12 testers, by email list or Google Group, and keep at least 12 opted in for
     14 days in a row. A tester who opts out and back in starts their 14 days again, so invite a
     few extra.
   - Give the track's feedback channel as jabbottpublicsupport@gmail.com. Keep notes on what testers
     report and what you fix (each fix is a new upload with a higher `versionCode`): the
     production application asks for both.
   - Read the pre-launch report for every upload.
3. **Apply for production.** After the 14 days, choose Apply for production on the Dashboard. The
   application asks about the closed test (how testers were found, how they used the app, their
   feedback and how it was collected), about the app, and about its readiness (what changed
   because of the test). Review usually takes seven days or less; if it isn't approved, the closed
   test goes on.
4. **Production.** Create a production release with the tested bundle. A first release can't be a
   staged rollout, and it goes through Play's review before it appears on the store.

## 8. After each release: Android vitals

Vagor has no crash reporting of its own, by design, so Android vitals in Play Console (Monitor and
improve > Android vitals, Crashes and ANRs) is the only signal from the field. Check it during the
closed test and after every release.

- Play's bad behaviour thresholds, over the last 28 days: a user-perceived crash rate of 1.09%
  overall or 8% on any one phone model, and an ANR rate of 0.47% overall or 8% on any phone model.
  Above them Play may make Vagor harder to find and show a warning on its listing.
- Vitals come only from users who share usage and diagnostics data with Google. At closed-test
  size an empty page means no data, not no crashes, so ask testers to email any crash.
- Play shows stack traces deobfuscated with the bundle's mapping file.

## 9. Repository housekeeping

These are the developer's to do; none of them blocks a release.

- **Old GitHub secrets.** The removed CD and SonarQube workflows read secrets named
  `ANDROID_SIGNING_*`, `GOOGLE_PLAY_SERVICE_ACCOUNT`, `SLACK_WEBHOOK` and `SONAR_*`. Check
  GitHub > Settings > Secrets and variables > Actions and delete any that exist. Vagor's signing
  never goes through CI.
- **Machine-specific files in git.** `local.properties` holds this machine's SDK path and is
  tracked although `.gitignore` lists it, and `.idea/` tracks Android Studio's caches and
  per-user state. To stop tracking them (the files stay on disk):

  ```bash
  git rm --cached local.properties .idea/caches/deviceStreaming.xml .idea/deploymentTargetSelector.xml .idea/markdown.xml
  ```

  Then add `/caches/`, `/deploymentTargetSelector.xml` and `/markdown.xml` to `.idea/.gitignore`,
  and update the `local.properties` sentence in CLAUDE.md's Working rules.

## Sources

- Target API: https://developer.android.com/google/play/requirements/target-sdk
- App signing, upload keys and Play App Signing: https://developer.android.com/studio/publish/app-signing
- Preparing and rolling out a release: https://support.google.com/googleplay/android-developer/answer/9859348
- Testing requirements for new personal accounts and applying for production: https://support.google.com/googleplay/android-developer/answer/14151465
- App content declarations: https://support.google.com/googleplay/android-developer/answer/9859455
- News apps (only apps in scope are asked): https://support.google.com/googleplay/android-developer/answer/10523915
- Trader status declaration: https://support.google.com/googleplay/android-developer/answer/17594752 (announced 2026-10-29: https://support.google.com/googleplay/android-developer/answer/18100766)
- User Data policy (privacy policy for all apps): https://support.google.com/googleplay/android-developer/answer/10144311
- Data safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Exact alarm permissions: https://support.google.com/googleplay/android-developer/answer/16558241
- US state age laws and Play Age Signals: https://support.google.com/googleplay/android-developer/answer/16569691
- Store settings (contact details, category, tags): https://support.google.com/googleplay/android-developer/answer/9859152 and https://support.google.com/googleplay/android-developer/answer/9859673
- Free and paid apps: https://support.google.com/googleplay/android-developer/answer/6334373
- Store listing graphics, screenshots and alt text: https://support.google.com/googleplay/android-developer/answer/9866151
- Icon specifications: https://developer.android.com/distribute/google-play/resources/icon-design-specifications
- Android vitals: https://developer.android.com/topic/performance/vitals
- Gradle's configuration cache and secrets: https://docs.gradle.org/current/userguide/configuration_cache_requirements.html
