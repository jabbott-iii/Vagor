# Publishing Vagor on Google Play

This is the checklist for Vagor's first Google Play release. The code side is done: the items
under "Already in the app" were made on 2026-10-25. Everything else happens in Play Console and
can only be done by the developer.

## Already in the app

- **Package name** `io.github.jabbott_iii.vagor`. Play rejects `com.example.*`, and the package name
  can never change after the first upload. (The Kotlin package in the source is still
  `com.example.vagor` and shows up in class names; Play's `com.example` restriction applies only to
  the package name.)
- **Target API level 36** (Android 16), required for new apps since 2026-08-31. Every screen draws
  edge to edge and keeps its content clear of the system bars.
- **Privacy policy** inside the app (Settings > Privacy Policy). The same text is in
  [PRIVACY.md](PRIVACY.md).
- **No data collection.** No internet permission, no accounts, ads, analytics or crash reporting.
  Cloud backup is off. The app requests only `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` and
  `SCHEDULE_EXACT_ALARM`.
  The bundle also lists AndroidX's app-internal `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which
  needs no declaration.
- **Exact alarms only through `SCHEDULE_EXACT_ALARM`**, for the reminder an hour before each
  event. The user allows it in Android's Alarms & reminders screen (Android 14 and later start
  without it), and without it the reminder comes 50 to 60 minutes before. The app doesn't declare
  `USE_EXACT_ALARM`, which Play allows only for alarm clock and calendar apps. As of 2026-10-27
  Play's policy asks for no declaration for `SCHEDULE_EXACT_ALARM`; if Play Console shows a
  permissions declaration at the first upload, answer it with that use (a reminder an hour before
  each event the user plans), and allow for a review that can take several weeks.
- **No native code**, so the 16 KB page size requirement doesn't apply.
- **Signed App Bundle** support (below).

## 1. Create an upload key (once)

Keep the keystore and its passwords out of the repository; `*.jks` and `*.keystore` are
git-ignored. Play requires a key valid until at least 22 October 2033; 25+ years is recommended.

```bash
keytool -genkeypair -keystore ~/keys/vagor-upload.jks -alias vagor-upload -keyalg RSA -keysize 2048 -validity 10000
```

Back the keystore up somewhere safe. If it's lost, Play Console can reset the upload key, but
that takes time.

## 2. Build the signed bundle

The release build signs only when these environment variables are set, so the key never enters
Gradle files:

```bash
export VAGOR_UPLOAD_KEYSTORE=~/keys/vagor-upload.jks
export VAGOR_UPLOAD_KEY_ALIAS=vagor-upload
read -s -p "Keystore password: " VAGOR_UPLOAD_KEYSTORE_PASSWORD; export VAGOR_UPLOAD_KEYSTORE_PASSWORD
read -s -p "Key password: " VAGOR_UPLOAD_KEY_PASSWORD; export VAGOR_UPLOAD_KEY_PASSWORD
./gradlew bundleRelease
jarsigner -verify app/build/outputs/bundle/release/app-release.aab
```

Upload `app/build/outputs/bundle/release/app-release.aab`. Raise `versionCode` in
`app/build.gradle.kts` before every later upload, including test-track uploads.

There is no release workflow (`cd.yml` was removed on 2026-10-25), so upload the bundle in Play
Console by hand.

## 3. Publish the privacy policy

Play needs the policy at a public, non-PDF URL as well as in the app. Publish
[PRIVACY.md](PRIVACY.md) as a web page (GitHub Pages works) and keep it identical to the in-app
text (`privacy_policy_text` in `app/src/main/res/values/strings.xml`). The policy's privacy contact
is the repository's public GitHub Issues page; to use an email address instead, change the Contact
section in both copies.

## 4. Play Console: App content

| Section | Answer |
| --- | --- |
| Privacy policy | The public URL from step 3 |
| Ads | No ads |
| App access | All functionality is available without special access (no login) |
| Content rating | Complete the questionnaire. Vagor is a planning utility with no violence, gambling, or mature content |
| Target audience | Choose the age groups you intend. Including children under 13 brings in the Families policy |
| Data safety | See below |
| Financial features | None |
| Health apps | None |

### Data safety answers

- **Does your app collect or share any of the required user data types?** No.
  - Play counts data as *collected* only when it is sent off the device. Vagor keeps everything on
    the device and has no internet access.
  - Sharing is only the user's own tap on Share, which opens Android's share menu, or on Add to
    Calendar, which hands one vacation or event to a calendar app. Play exempts transfers the user
    starts and expects ("User-initiated action" in Play's Data safety help).
- Recheck these answers if Vagor ever adds a library that uses the network, analytics, crash
  reporting, or ads.

## 5. Store listing

- **App name** (30 characters max): `Vagor` or `Vagor: Vacation Planner`
- **Short description** (80 max): Plan trips and events offline, with reminders and easy sharing.
- **Full description** (draft):

  > Vagor is a simple vacation planner that keeps everything on your phone.
  >
  > • Add vacations with start and end dates and your trip details.
  > • Plan events on the days of each vacation, each with its time, time zone and details.
  > • Get a reminder notification on the morning a vacation starts, the morning it ends, and an hour before each event.
  > • Share a vacation, with its events, or a single event as text through any app you choose.
  > • Add a vacation or an event to Google Calendar or another calendar app.
  > • Dark mode by default, with a blue colour scheme designed to stay readable for people with colour blindness.
  >
  > Vagor has no account, no ads and no internet access. It never uploads your trips: they leave Vagor only when you share them, add them to a calendar app, or move to a new phone.

  Don't claim features Vagor doesn't have: importing trips, sync or cloud backup (Google Calendar
  may sync an item saved there, but that's Google Calendar, not Vagor). Name Google Calendar only
  in plain text saying what Vagor works with: no Google logo or Calendar icon in the graphics or
  screenshots, and no Google name in the app's title or icon. Don't promise
  reminders to the minute either: they depend on Alarms & reminders and the phone's battery
  settings.
- **Graphics:** a 512×512 PNG icon, a 1024×500 feature graphic, and at least two phone
  screenshots. The launcher icon is still the Android template robot (now on a blue background);
  a custom icon is recommended before launch.

## 6. Testing before production

- Personal developer accounts created after 2023-11-13 must run a **closed test with at least 12
  testers for 14 days in a row** before they can apply for production access.
- Check the pre-launch report in Play Console after the first upload.

## Sources

- Target API: https://developer.android.com/google/play/requirements/target-sdk
- User Data policy (privacy policy for all apps): https://support.google.com/googleplay/android-developer/answer/10144311
- Data safety: https://support.google.com/googleplay/android-developer/answer/10787469
- App signing: https://developer.android.com/studio/publish/app-signing
- Testing requirements for new personal accounts: https://support.google.com/googleplay/android-developer/answer/14151465
- Store listing assets: https://support.google.com/googleplay/android-developer/answer/9866151
