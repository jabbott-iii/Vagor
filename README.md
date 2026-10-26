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

To build a signed release bundle for Google Play, see [PLAY_RELEASE.md](PLAY_RELEASE.md).

## Features

- **Dashboard** → the landing screen: every vacation with its dates, soonest first, with ended vacations below under "Past", and a blue Add Vacation button
- **Bottom navigation** → switches between the Vacations and Settings tabs
- **Vacation page** → tap a vacation to see its details and events; edit or share it (the shared text lists its events) and add events
- **Vacation details** → add, update, and delete a vacation
- **Event details** → add, update, delete, and share an event
- **Calendar date entry** → every date field opens a month calendar that drops down below it; an event's calendar offers only its vacation's days
- **Reminders** → a notification around 9:00 AM local time on the day each vacation starts and ends and on each event's date
- **Settings tab** → a dark mode switch (dark is the default) and the privacy policy
- **Theme** → a blue Material 3 colour scheme chosen for colour-blind users; every text colour meets WCAG AA contrast in light and dark mode
- **Persistence** → Room database storing vacations and events on the device only
- **Validation** → strict `MM/dd/yyyy` dates (two-digit month and day, four-digit year, nothing extra), event dates constrained to the vacation window, and prevention of deleting vacations that still have events

## Notes

- This port keeps the original Room-backed persistence while converting the implementation to Kotlin. The screens were reorganised on 2026-10-25 around a dashboard with bottom navigation.
- Reminders are scheduled automatically by `ReminderScheduler` with inexact `AlarmManager` alarms and posted as notifications by `MyReceiver`. On Android 12 and later a reminder usually arrives within an hour after 9:00 AM; on Android 8–11 an idle phone (Doze) can hold it until the phone wakes. Reminders are rescheduled after a reboot, a clock or time zone change, an app update, and on app launch.
- Events were called excursions in earlier versions. The database table keeps the name `excursions`, so no schema migration was needed. Builds from before the application ID changed to `io.github.jabbott_iii.vagor` (they used `com.example.vagor`) are a separate app on the phone, and their data doesn't carry over.
- Cloud backup is turned off, so trips are not copied to Google Drive. See [PRIVACY.md](PRIVACY.md).
