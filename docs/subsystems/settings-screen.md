# Subsystem: Settings screen

The Settings tab on the home screen and the code behind each item on it:
the dark mode switch and where it is stored, what the tab says about
reminders and the ways to Android's settings screens, the privacy policy,
the third-party licenses and the app's version. Dark mode is a switch on
this tab, on by default, asked for by the user on 2026-10-25 as an on/off
switch, not a follow-system option (ADR-0006). Built: product requirement 6
(`docs/00-overview.md`), with the reminder line that goes with ADR-0014;
the third-party licenses (ADR-0029) and the version, both added
2026-10-31; and the hibernation line, added 2026-11-01 (ADR-0032).

Files, under `app/src/main/java/com/example/vagor/` and `app/src/main/res/`:
`SettingsFragment.kt`, `ThemeSettings.kt`, `VagorApplication.kt` (applies
the saved theme), `MessageDialog.kt`, `LicenseText.kt`,
`layout/fragment_settings.xml`, `raw/apache_license_2_0.txt`, and the
strings `privacy_policy_text` and `third_party_licenses_text` in
`values/strings.xml`. The tab also calls helpers that belong to the
reminders (`ExactAlarmOffer.kt`, `NotificationsOff.kt`, `Hibernation.kt`,
`ReminderScheduler.kt`; `docs/subsystems/reminders.md`). Path-scoped
rules: `.claude/rules/ui-and-accessibility.md` and
`.claude/rules/manifest-privacy-and-legal.md`.

## Contents

| File | Provides |
| :- | :- |
| `SettingsFragment.kt` | The tab: wires every item, refreshes the status lines on resume, reads the app's own version |
| `ThemeSettings.kt` | `isDarkMode`, `setDarkMode`, `applySaved`: the switch's stored value and the night mode |
| `VagorApplication.kt` | Applies the saved theme before any screen is created |
| `MessageDialog.kt` | A message with an OK button, as a `DialogFragment`, optionally followed by a text file from `res/raw` |
| `LicenseText.kt` | `reflowParagraphs`: joins each paragraph of the license text onto one line |
| `layout/fragment_settings.xml` | The tab's layout, in a `ScrollView` so it scrolls in landscape and at large font sizes |
| `raw/apache_license_2_0.txt` | The Apache License 2.0, the only third-party license text the app ships |

## What the tab shows

In order, top to bottom:

| Item | View | Shown | Does |
| :- | :- | :- | :- |
| "Settings" heading | — | Always | An accessibility heading |
| Dark mode switch | `switchDarkMode` (`MaterialSwitch`) | Always | `ThemeSettings.setDarkMode` |
| Event reminder status | `eventRemindersStatus` | Always | One of three texts, below |
| Notification Settings | `buttonNotificationSettings` | Only while notifications are off, for the app or only for the reminder channel | `openNotificationSettings()` |
| Alarms & Reminders | `buttonAlarmsAndReminders` | Android 12+ (`exactAlarmSettingsIntent() != null`) | `openExactAlarmSettings()` |
| Hibernation status | `hibernationStatus` | Android 12+ | One of two texts, below |
| App Info | `buttonAppInfo` | Android 12+ | `openAppInfo()` |
| Privacy Policy | `buttonPrivacyPolicy` | Always | `MessageDialog` with `privacy_policy_text` |
| Third-Party Licenses | `buttonThirdPartyLicenses` | Always | `MessageDialog` with `third_party_licenses_text` and the Apache License |
| Version | `appVersion` | Always | "Version %1$s (%2$d)": `versionName`, then `versionCode` |

The buttons are full width, one above the other, styled
`Widget.Vagor.Button.Outlined` (ADR-0016). The tab is a `Fragment` inside
`HomeActivity`, which sets no title of its own; the tab's heading names
it.

## Dark mode

- **Stored** in the SharedPreferences file `settings` under `dark_mode`;
  missing means on. It is a UI preference, not trip data, so it lives there
  rather than in `vacation_database`. It is the only stored state besides
  the database (`docs/subsystems/data-and-storage.md`).
- **Applied** with `AppCompatDelegate.setDefaultNightMode`
  (`MODE_NIGHT_YES` or `MODE_NIGHT_NO`). AppCompat doesn't persist the
  night mode, so `VagorApplication.onCreate` calls
  `ThemeSettings.applySaved` on every process start, before any activity is
  created, so the app's first frame already uses the saved theme.
- **The launch window.** The system draws the launch window before any app
  code runs. On Android 12+ `setDarkMode` also calls
  `UiModeManager.setApplicationNightMode`, which Android remembers, so later
  launches' splash screens match the app setting. Android 8–11 has no
  equivalent and follows the system light/dark setting on every cold
  start, so a dark-mode user on a light-themed phone sees a brief light
  flash. On Android 12+ only the very first launch does (a known issue, not
  captured on a device; `docs/subsystems/theme-and-accessibility.md`).
- **Switching** recreates every open screen in the new theme; the home
  screen comes back on the Settings tab, with the bar's selection restored.
- **Both themes** are the blue Material 3 scheme in `values/themes.xml` and
  `values-night/themes.xml`, and every text/background pair meets WCAG AA
  in both (ADR-0017, `ThemeContrastTest`).

## Reminder status and settings

`onResume` sets the status lines and the Notification Settings button
again, because the user may have turned notifications or exact alarms on or
off in Android's settings and come back. `HomeActivity.onResume` has
already rebuilt the reminders if exact alarms were allowed or taken away
since, so the line matches them (`docs/subsystems/reminders.md`).

| When | `eventRemindersStatus` says |
| :- | :- |
| Notifications are off (`ReminderScheduler.notificationsOn` is false) | "Notifications are off for Vagor, so trip reminders won't appear. You can turn them on in Android Settings." |
| Exact alarms are allowed (`ReminderScheduler.canScheduleExactAlarms`) | "Event reminders come an hour before each event." |
| Otherwise | "Event reminders can come any time in the hour before each event, and if Vagor isn't used for a while, only one a day may come on time. To get them exactly an hour before, allow alarms and reminders." |

- **Notification Settings** opens the reminder channel's settings when only
  that channel is off, otherwise Vagor's notification settings, or its App
  info screen on a phone that lacks them. The dashboard's
  `NotificationsOffDialog` opens the same place. Not a toast: Android cuts
  toasts at two lines.
- **Alarms & Reminders** opens Android's Alarms & reminders screen for
  Vagor (`Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`), or Vagor's App
  info on a phone that lacks it. Before Android 12 there is no such screen
  and event reminders are always exact, so the button is hidden. The event
  form's `ExactAlarmOfferDialog` opens the same screen. No alarm an app can
  set without exact-alarm permission escapes Android's standby bucket
  limits; allowing Alarms & reminders does, which the inexact text says.
  Never add `USE_EXACT_ALARM`.
- **Exact alarms without a broadcast.** Setting Vagor's battery use to
  Unrestricted also allows exact alarms and sends nothing, which is why
  `HomeActivity` checks again on resume.

## Hibernation status and App Info

From Android 12, Android may hibernate an app that hasn't been opened for
about three months, which stops its reminders until it is opened again
(ADR-0032, `docs/subsystems/reminders.md`).

| When | `hibernationStatus` says |
| :- | :- |
| `mayHibernate()` is true | "If Vagor isn't opened for about three months, Android may pause it, and its reminders would stop until it's opened again. To keep reminders for trips further ahead, turn off the unused app setting in App info." |
| Otherwise | "Android won't pause Vagor while it's unused, so reminders for trips far ahead stay set." |

- **`mayHibernate()`** is Android 12+, `PackageManager.isAutoRevokeWhitelisted`
  false, and not a work profile, which Android exempts. Android also
  exempts company-managed phones, and some makers turn hibernation off,
  which no public API reveals, so the text says Android "may" pause Vagor.
- **App Info** opens Vagor's App info screen
  (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`), where the user turns off
  the unused app setting. Its name differs between Android versions
  ("Manage app if unused" on API 37), so the app's text doesn't name it.
  The vacation form's `HibernationOfferDialog` opens the same screen.
- **Before Android 12** Android doesn't stop an unused app, so the line and
  the button are hidden. On Android 11 an unused app only loses its runtime
  permissions, and Vagor has none there.
- `onResume` sets the line again, because the setting may have changed in
  App info.

## Privacy policy

- **Shown** with `MessageDialog` (tag `privacyPolicy`): the title "Privacy
  Policy" and the string `privacy_policy_text`.
- **It exists twice**: PRIVACY.md and `privacy_policy_text`. Change both
  together, and keep them true to what the app does. Play needs the policy
  in the app as well as at a public URL (`docs/05-release.md`, step 4).
- **Its contact** is jabbottpublicsupport@gmail.com, chosen by the user on
  2026-10-31 when GitHub Issues turned out to be off; the Play listing's
  contact email is the same address (ADR-0019).
- **LICENSE** also says how the app handles data (section 5) and that its
  reminders can be late or stopped (section 6): keep those true too.

## Third-party licenses

- **Shown** with `MessageDialog` (tag `thirdPartyLicenses`): the title
  "Third-Party Licenses", the list in `third_party_licenses_text`, then the
  Apache License 2.0 from `res/raw/apache_license_2_0.txt`.
- **Why it ships.** The Apache License 2.0 is the only third-party license
  text the app ships (added 2026-10-31), because the bundled libraries'
  license asks for it to go with the app (ADR-0029).
- **Reflowed.** The license text is wrapped for about 80 columns, which a
  phone's narrower dialog would wrap again, leaving a short line after
  nearly every line. `reflowParagraphs` joins each paragraph (the lines
  between blank lines) into one line, so the text wraps to the screen.
- **Kept in step with NOTICE.** When a library, icon or other third-party
  material that ships in the app is added, add it to `NOTICE` and to
  `third_party_licenses_text`; `ThirdPartyLicensesTest` checks they list
  the same things. One under another license also needs that license's
  text added (`docs/02-dependency-policy.md`, `/evaluate-dependency`).
- **Vagor's own code** is proprietary (ADR-0018): don't put it under an
  open-source license.

## Version

- **Shows** "Version 1.0 (1)" today: `versionName`, then `versionCode`,
  from `app/build.gradle.kts`, read from the app's own `PackageInfo`
  (with `PackageInfoFlags` on Android 13+, which replaced the `Int` flags)
  and `PackageInfoCompat.getLongVersionCode`.
- **Why.** So a tester can say which build they have when reporting a
  problem, in the same terms Google Play shows (`docs/05-release.md`,
  Versioning; SECURITY.md).

## Key types

```kotlin
class SettingsFragment : Fragment(R.layout.fragment_settings)

object ThemeSettings {
    fun isDarkMode(context: Context): Boolean
    fun setDarkMode(context: Context, enabled: Boolean)
    fun applySaved(context: Context)
}

class MessageDialog : DialogFragment() {
    companion object {
        // [titleId] 0 shows no title. [rawTextId], when not 0, is a text file in res/raw whose
        // paragraphs follow the message (reflowParagraphs).
        fun show(
            manager: FragmentManager,
            tag: String,
            @StringRes titleId: Int,
            @StringRes messageId: Int,
            @RawRes rawTextId: Int = 0
        )
    }
}

fun reflowParagraphs(text: String): String

// Called by the tab; defined with the reminders.
fun Context.openNotificationSettings()               // NotificationsOff.kt
fun Context.exactAlarmSettingsIntent(): Intent?      // ExactAlarmOffer.kt; null before Android 12
fun Context.openExactAlarmSettings()                 // ExactAlarmOffer.kt
fun Context.mayHibernate(): Boolean                  // Hibernation.kt
fun Context.openAppInfo()                            // Hibernation.kt
object ReminderScheduler {
    fun notificationsOn(context: Context): Boolean
    fun canScheduleExactAlarms(context: Context): Boolean
}
```

## Rules

- **Every dialog is a `DialogFragment`**, so it is still open after a
  rotation (ADR-0033). Use `MessageDialog` for a message with an OK button.
  `MessageDialog.show` does nothing while the fragment manager's state is
  saved or a dialog with its tag is open, so a second tap, or a screen
  reader's, opens nothing more.
- **The `open...` helpers catch `ActivityNotFoundException`.**
  `openNotificationSettings` and `openExactAlarmSettings` try the next
  screen in turn; `openAppInfo` does nothing.
- **The tab reads no trip data** and opens no database.
- **Text** goes in `res/values/strings.xml`. Say Android "may" pause Vagor,
  and don't name the unused app setting.
- **A text field added here** would need its own keyboard handling:
  `HomeActivity`'s edge-to-edge helper does nothing for the keyboard
  (`docs/subsystems/screens-and-navigation.md`).

## Invariants

- With no stored value, dark mode is on.
- The status lines and the Notification Settings button match the state
  at the last resume.
- The in-app license list and `NOTICE` name the same libraries, and the
  license text is the Apache License 2.0 exactly as Apache publishes it.
- The in-app privacy policy and PRIVACY.md say the same.

## Limits

| Item | Available | Why |
| :- | :- | :- |
| Alarms & Reminders button | Android 12+ | Exact alarms need no permission before Android 12 |
| Hibernation status and App Info button | Android 12+ | Android doesn't stop an unused app before Android 12 |
| Notification Settings button | While notifications are off | Nothing to turn on otherwise |

## Tests

- `SettingsInstrumentedTest` opens the real home screen. It flips the dark
  mode setting and restores it, grants the notification permission and
  leaves it granted, and opens Android's Alarms & reminders screen and
  Vagor's App info and leaves them with Back. Run it only on an emulator
  or a test device (`.claude/rules/instrumented-tests.md`). Its tests:
  - `darkModeIsOnByDefault`: with `dark_mode` removed, dark mode is on.
  - `switchTurnsDarkModeOffAndOn`: the switch sets the stored value and
    the night mode, and the recreated screen is still on the Settings tab.
  - `backFromSettingsReturnsToVacations`.
  - `settingsSaysWhetherEventRemindersAreExact`: the right line, and on
    Android 12+ Alarms & reminders opens and Back returns to the tab.
  - `settingsSaysWhetherAndroidMayPauseVagor`: hidden before Android 12;
    otherwise the right line, and App info opens and Back returns.
  - `notificationSettingsButtonShowsOnlyWhileNotificationsAreOff`.
  - `notificationsOffDialogOpensNotificationSettings`: the dialog's Open
    Settings asks for Vagor's notification settings, caught by an
    `Instrumentation.ActivityMonitor`.
  - `privacyPolicyOpensFromSettings`: and it stays open through a
    rotation.
  - `thirdPartyLicensesOpenFromSettingsWithTheLicenseText`.
  - `settingsShowsTheAppVersion`.
- `ThirdPartyLicensesTest` (JVM): the in-app list matches `NOTICE`; the
  license file's SHA-256 is that of apache.org's `LICENSE-2.0.txt`;
  reflowing joins each paragraph onto one line; reflowing the license
  keeps every word in order.
- `ThemeContrastTest` (JVM): text and non-text UI meet WCAG AA in both
  themes (`docs/subsystems/theme-and-accessibility.md`).

## Open questions

- Nothing in the app shows LICENSE, the end-user license agreement, yet;
  showing it to buyers is open (`docs/roadmap.md`, M7).
