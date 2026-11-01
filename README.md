# Vagor
Vagor is an offline Kotlin Android vacation planner, ported from `jabbott-iii/university-projects` → `Mobile-Application-Development-Android`. Everything stays on the phone: no internet access, no accounts, no ads and no analytics.

## Build and run

1. Open the repository root in Android Studio.
2. Let Android Studio install Android SDK 37 and any other packages requested by Gradle if prompted.
3. Build from Android Studio, or run:
   ```bash
   ./gradlew assembleDebug
   ```
4. Launch the `app` configuration on an emulator or Android device running Android 8.0+.

To build a signed release bundle and publish it on Google Play, see [PLAY_RELEASE.md](PLAY_RELEASE.md). The store listing's text, graphics and screenshots are in `fastlane/metadata/android/en-US/`.

## Features

- **Dashboard** → the landing screen: every vacation with its dates, soonest first, with ended vacations below under "Past", and a blue Add Vacation button
- **Bottom navigation** → switches between the Vacations and Settings tabs
- **Vacation page** → tap a vacation to see its details and events; edit or share it (the shared text lists its events), add it to a calendar, and add events
- **Vacation details** → add, update, and delete a vacation
- **Event details** → add, update, delete, share, and add to a calendar an event, with its date, time, time zone and optional details
- **Calendar date entry** → every date field opens a month calendar that drops down below it; an event's calendar offers only its vacation's days
- **Time and time zone entry** → an event's time comes from a clock in the phone's 12- or 24-hour style, and its time zone from a searchable list that starts on the phone's zone
- **Reminders** → a notification around 9:00 AM local time on the day each vacation starts and ends, and within the hour before each event (exactly an hour before once Alarms & reminders is allowed); one that arrives on time leaves the notification shade once it's out of date, at the end of its day or an hour after the event starts, and one that Android delivers late is still shown
- **Settings tab** → a dark mode switch (dark is the default), whether event reminders are exact (with a button to Android's Alarms & reminders screen), a Notification Settings button while notifications are off, whether Android may pause Vagor while it's unused (with an App Info button, Android 12 and later), the privacy policy, the third-party licenses, and the app's version
- **Theme** → a blue Material 3 colour scheme chosen for colour-blind users; every text colour meets WCAG AA contrast in light and dark mode
- **Add to Calendar** → opens Google Calendar's new-event screen (or another calendar app's when Google Calendar isn't installed) filled in with a vacation, all day over its dates, or an event at its time in its time zone; the calendar app saves it, and Vagor needs no calendar permission
- **Persistence** → Room database storing vacations and events on the device only
- **Dates** → shown the way the phone writes them ("Jul 6, 2031" in the US, "6 Jul 2031" in the UK), always in the Gregorian calendar
- **Validation** → dates stored as strict `MM/dd/yyyy` (two-digit month and day, four-digit year, nothing extra), event dates constrained to the vacation window (moving or shortening a vacation can't leave its events outside it), a required event time, and prevention of deleting vacations that still have events

## Notes

- This port keeps the original Room-backed persistence while converting the implementation to Kotlin. The screens were reorganised on 2026-10-25 around a dashboard with bottom navigation.
- Reminders are scheduled automatically by `ReminderScheduler` with `AlarmManager` alarms and posted as notifications by `MyReceiver`. An event's reminder uses an exact alarm an hour before it starts, in the event's time zone. Android 12 and later need the user's permission for that (Alarms & reminders, which Vagor offers after saving an event); without it the reminder comes some time in the hour before the event, and once Vagor has gone unused for a while (8 days on Android 13 and later), Android lets only one such reminder a day come on time. Events saved before they had a time are reminded around 9:00 AM on their date. Vacation reminders are inexact: on Android 12 and later one usually arrives within an hour after 9:00 AM; on Android 8–11 an idle phone or Battery Saver can hold it until the phone wakes or Battery Saver is off. Setting Vagor's battery use to Restricted holds every reminder until Vagor is opened. From Android 12, Android pauses an app that isn't opened for about three months, which stops its reminders until it's opened again; after saving a trip that ends more than two months ahead, Vagor offers the App info setting that turns this off, and the Settings tab shows it too. Reminders are rescheduled after a reboot, a clock or time zone change, an app update, when exact alarms are allowed, and whenever the app starts.
- Events were called excursions in earlier versions. The database table keeps the name `excursions`, so no schema migration was needed. Database version 2 (October 2026) added each event's details, time and time zone; events saved before it keep their date and get a time the next time they're edited. Builds from before the application ID changed to `io.github.jabbott_iii.vagor` (they used `com.example.vagor`) are a separate app on the phone, and their data doesn't carry over.
- Cloud backup is turned off, so trips are not copied to Google Drive. See [PRIVACY.md](PRIVACY.md).

## License

Vagor is proprietary software: copyright (c) 2026 Joseph Anthony Abbott III, all rights reserved. See [LICENSE](LICENSE). The third-party software it includes keeps its own licenses, listed in [NOTICE](NOTICE) and in the app under Settings > Third-Party Licenses.
