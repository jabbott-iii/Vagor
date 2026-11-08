---
paths:
  - "app/src/main/AndroidManifest.xml"
  - "app/src/main/res/xml/**"
  - "app/src/main/res/raw/**"
  - "app/src/main/res/values/strings.xml"
  - "PRIVACY.md"
  - "LICENSE"
  - "NOTICE"
  - "fastlane/**"
  - "app/src/main/java/com/example/vagor/CalendarInsert.kt"
  - "app/src/main/java/com/example/vagor/EventListActivity.kt"
  - "app/src/main/java/com/example/vagor/EventDetailActivity.kt"
  - "app/src/main/java/com/example/vagor/LicenseText.kt"
  - "app/src/test/java/com/example/vagor/ThirdPartyLicensesTest.kt"
---

# Rules for the manifest, privacy, data leaving the app, and legal text

Vagor runs entirely on the phone, with no backend and no accounts
(ADR-0001). What the privacy policy, `LICENSE`, the store listing and Play's
Data safety answer say must stay true to what the app does.

- **Permissions:**
  - The manifest declares only `POST_NOTIFICATIONS`,
    `RECEIVE_BOOT_COMPLETED` and `SCHEDULE_EXACT_ALARM`. The merged manifest
    also lists AndroidX's app-internal
    `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which needs no declaration.
  - Never add `INTERNET`, `ACCESS_NETWORK_STATE` or `AD_ID`.
  - Never add `USE_EXACT_ALARM`: Google Play allows it only for calendar and
    alarm-clock apps.
  - Never add `READ_CALENDAR` or `WRITE_CALENDAR`.
  - Don't change manifest permissions or backup settings unless the task
    explicitly calls for it.
- **Components:** Vagor's own manifest exports only `HomeActivity` (the
  launcher) today. `MyReceiver` and `ReminderRescheduleReceiver` are
  `exported="false"`. The merged manifest also exports AndroidX's
  `ProfileInstallReceiver`, only to callers holding
  `android.permission.DUMP` (`docs/02-dependency-policy.md`). Never give
  `HomeActivity` an `android:label`: on the launcher activity it would
  rename the app's icon.
- **Backup (decided with the user on 2026-10-25, ADR-0004):**
  `allowBackup="false"`, plus exclusions in `backup_rules.xml` (Android 11
  and earlier) and `data_extraction_rules.xml` (Android 12 and later). A
  direct device-to-device transfer to a new phone is still allowed. The
  privacy policy's Backup section and `LICENSE` 5.2 say this.
- **Data leaves the app only two ways,** both started by the user's tap:
  - **Share,** through the Android Sharesheet: `Intent.ACTION_SEND` inside
    `Intent.createChooser`, as `EventListActivity` and `EventDetailActivity`
    do, with text built from string resources such as
    `vacation_share_template`. No links to servers and no accounts. Trips are
    shared as plain text (decided with the user on 2026-10-25, ADR-0009).
  - **Add to Calendar,** which hands one vacation or event to a calendar app
    (ADR-0015, `.claude/rules/dates-and-times.md` for its times):
    - `Intent.ACTION_INSERT` with `CalendarContract.Events.CONTENT_URI` and
      the type `vnd.android.cursor.dir/event`, aimed at Google Calendar
      (`com.google.android.calendar`) and, when that isn't installed, at any
      calendar app (`openCalendarInsert`).
    - It opens the calendar app's own new-event screen, and that app decides
      whether and where to save (Android's original calendar app saves when
      the user leaves the screen). Never promise that nothing is added until
      the user saves.
    - It needs no permission. Never write to the calendar provider directly.
    - Starting the intent needs no `<queries>` entry; catch
      `ActivityNotFoundException` instead.
    - A second tap within 2 seconds is ignored, because some calendar apps
      save every new-event screen the user leaves. "No calendar app" is a
      dialog, because a toast is cut at two lines.
    - The label says "Calendar" because it may open another app, and
      Google's brand guidance asks for accurate wording.
  - To share a file (an `.ics` file, for example), serve it through a
    `FileProvider` `content://` URI with `FLAG_GRANT_READ_URI_PERMISSION`.
    Never use a `file://` URI.
  - Importing a shared trip isn't supported; whether Vagor should is still
    open (`docs/roadmap.md`).
- **The privacy policy exists twice:** `PRIVACY.md` and
  `privacy_policy_text` in `strings.xml`.
  - Change both together, word for word. No test compares them: compare
    them by hand.
  - Keep them true to what the app does: what it stores, what leaves the
    phone, reminders, backup, what Google Play gives the developer.
  - A changed policy gets a new effective date in both copies (the policy's
    Changes section promises one).
  - Its contact is jabbottpublicsupport@gmail.com, chosen by the user on
    2026-10-31 when GitHub Issues turned out to be off (ADR-0019). The Play
    listing's contact email is the same address. To change it, change the
    Contact section in both copies.
  - The published copy must stay identical (`docs/05-release.md`, step 4).
- **`LICENSE` is the Vagor Perpetual End-User License Agreement** (ADR-0034),
  copied from the user's Denarii Dolor app on 2026-11-07 at their request and
  changed to fit Vagor.
  - Change its terms only at the user's request.
  - Keep section 5 (how the app handles data: only on the device, given only
    to the app the user shares with or their calendar app, cloud backup off,
    no recovery except a device-to-device copy) and section 6 (a planning aid
    only; reminders can be late or stopped) true. If a change to the app
    would make either untrue, tell the user.
  - Section 4 points to Settings > Third-Party Licenses and `NOTICE`.
  - Vagor is sold on Google Play for US$1 (decided with the user on
    2026-11-07, ADR-0035), so the agreement's license fee, refund and
    liability cap apply as written.
  - The published `LICENSE.txt` copy is updated whenever `LICENSE` changes
    (`docs/05-release.md`, step 4).
- **Vagor is proprietary** (all rights reserved by Joseph Anthony Abbott III
  since 2026-10-31, Apache 2.0 before; ADR-0018).
  - Don't put Vagor's own code under an open-source license. Its own files
    carry no license headers.
  - The GitHub view-and-fork exception is in `README.md`'s License section.
- **Third-party licenses (ADR-0029):**
  - The only third-party license text the app ships is the Apache License
    2.0 in `res/raw/apache_license_2_0.txt` (added 2026-10-31), because the
    bundled libraries' license asks for it to go with the app. The Settings
    tab shows it under Third-Party Licenses (`LicenseText.kt` reflows it).
  - Never edit that file: `ThirdPartyLicensesTest` checks its SHA-256
    against the text Apache publishes.
  - When a library, icon or other third-party material that ships in the app
    is added, add it to `NOTICE` and to `third_party_licenses_text`, in the
    same order (`ThirdPartyLicensesTest` checks they list the same things).
    One under another license also needs that license's text added.
- **User-visible text** goes in `res/values/strings.xml`. The app is English
  only (ADR-0036); `.claude/rules/ui-and-accessibility.md` has the rest.
  The app's text says Android "may" pause Vagor, because company-managed
  phones are exempt and some makers turn hibernation off, which no public
  API reveals; the listing promises reminders at an exact time only when
  Alarms & reminders is allowed ("exactly an hour before when you allow
  alarms and reminders").
- **The store listing** (`fastlane/metadata/android/en-US/`) stays true to
  the app, like the privacy policy. Change it through
  `/update-store-listing`.
- **Play declarations depend on this file's rules** (`docs/05-release.md`,
  step 5): Data safety is "no data collected or shared", and Advertising ID
  is No. Recheck both after adding any library, and tell the user when one
  would change.
- **Open decisions are the user's call** (`docs/roadmap.md`, M7.1-M7.6;
  `AGENTS.md`, rule 14): showing the agreement to buyers, the refund policy,
  how long sale records and support emails are kept, trader status and EEA
  or UK distribution, the EEA classification, Age Signals. Don't settle any
  of them in text.
- **Before reporting done,** run
  `./gradlew testDebugUnitTest --tests 'com.example.vagor.ThirdPartyLicensesTest'`
  after any change to `NOTICE`, `third_party_licenses_text` or
  `res/raw/`, and say whether you compared the two privacy policy copies.
