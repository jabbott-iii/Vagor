# Vagor
Vagor is a Kotlin Android vacation planning app ported from `jabbott-iii/university-projects` → `Mobile-Application-Development-Android`.

## Build and run

1. Open the repository root in Android Studio.
2. Let Android Studio install Android SDK 37 and any other packages requested by Gradle if prompted.
3. Build from Android Studio, or run:
   ```bash
   ./gradlew assembleDebug
   ```
4. Launch the `app` configuration on an emulator or Android device running Android 8.0+.

## Feature mapping from the source app

- **Home screen** → entry screen with navigation into the vacation list
- **Vacation list** → create and open saved vacations
- **Vacation details** → save, update, delete, and share (the shared text lists the vacation's excursions)
- **Excursion list** → manage excursions for a selected vacation
- **Excursion details** → save, update, delete, and share
- **Reminders** → a notification at 9:00 AM local time on the day each vacation starts and ends and on each excursion's date
- **Persistence** → Room database storing vacations and excursions locally
- **Validation** → strict `MM/dd/yyyy` dates, excursion dates constrained to the vacation window, and prevention of deleting vacations that still have excursions

## Notes

- This port keeps the original activity flow and Room-backed persistence while converting the implementation to Kotlin.
- Reminders are scheduled automatically by `ReminderScheduler` with inexact `AlarmManager` alarms and posted as notifications by `MyReceiver`. They are rescheduled after a reboot, a clock or time zone change, an app update, and on app launch.
