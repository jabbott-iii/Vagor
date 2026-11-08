---
paths:
  - "app/src/main/res/layout/**"
  - "app/src/main/res/values*/**"
  - "app/src/main/res/drawable/**"
  - "app/src/main/res/color/**"
  - "app/src/main/res/menu/**"
  - "app/src/main/res/mipmap-anydpi/**"
  - "app/src/main/java/com/example/vagor/*Activity.kt"
  - "app/src/main/java/com/example/vagor/*Fragment.kt"
  - "app/src/main/java/com/example/vagor/MessageDialog.kt"
  - "app/src/main/java/com/example/vagor/NotificationsOff.kt"
  - "app/src/main/java/com/example/vagor/ExactAlarmOffer.kt"
  - "app/src/main/java/com/example/vagor/Hibernation.kt"
  - "app/src/main/java/com/example/vagor/TimeZonePicker.kt"
  - "app/src/main/java/com/example/vagor/EdgeToEdge.kt"
  - "app/src/main/java/com/example/vagor/ThemeSettings.kt"
  - "app/src/test/java/com/example/vagor/ThemeContrastTest.kt"
---

# Rules for screens, layouts, theme and accessibility

The specs are `docs/subsystems/screens-and-navigation.md`,
`docs/subsystems/settings-screen.md` and
`docs/subsystems/theme-and-accessibility.md`.

- **The toolkit (ADR-0002):** XML layouts, `AppCompatActivity` (plus a
  `Fragment` for each home screen tab), `findViewById`, and `ListView` with
  `ArrayAdapter` for the dashboard. The vacation page shows its few events as
  plain rows in a `ScrollView`, so focus follows the screen. No Compose,
  ViewBinding, ViewModel, DI framework or coroutines without the user's
  approval.
- **Colours come from theme attributes only** (`res/values/colors.xml`,
  `values/themes.xml`, `values-night/themes.xml`). Never hard-code a text or
  background colour in a layout.
- **Accessible colours (ADR-0017):**
  - The scheme is a blue Material 3 scheme for colour-blind users, from the
    Okabe-Ito blue `#0072B2`.
  - Every text/background pair meets WCAG AA (4.5:1; 3:1 for non-text UI)
    in both themes. `ThemeContrastTest` enforces this.
  - Don't convey anything by colour alone.
  - Don't add DynamicColors: it would replace the blue with wallpaper
    colours.
- **Keyboard focus is always visible:**
  - the theme turns on Material's focus rings (`focusRingsEnabled`, not yet
    public API) in both `themes.xml` files
  - the dashboard and time zone lists have their own focus rings, and the
    calendar's days have one
  - Material's time picker marks its selected AM/PM by hue alone, so
    `ThemeOverlay.Vagor.MaterialTimePicker` fills it with the primary blue
    instead. The selected hour or minute keeps Material's colours: the dial's
    numbers also show which is being set.
- **Dark mode (asked for by the user on 2026-10-25, ADR-0006):** a switch on
  the Settings tab, on by default, as an on/off switch, not a follow-system
  option. `ThemeSettings` stores it in the SharedPreferences file
  `settings`, and `VagorApplication` applies it before any screen.
  - Known issue: the system draws the launch window before any app code
    runs. On Android 8–11 it follows the system light/dark setting on every
    cold start, so a dark-mode user on a light-themed phone sees a brief
    light flash. On Android 12+ only the very first launch does; later
    launches match the app setting through
    `UiModeManager.setApplicationNightMode` (not captured on a device).
- **Buttons (asked for by the user on 2026-10-26, ADR-0016):** every button
  on a page is the same size: full width, one above the other, styled
  `Widget.Vagor.Button` or `Widget.Vagor.Button.Outlined` with
  `layout_width="match_parent"` and `layout_height="wrap_content"`. The style
  sets one height (`@dimen/button_height`), which grows with the font size.
  Only the floating Add Vacation and Add Event buttons differ
  (`Widget.Vagor.AddButton`).
- **Text fields** are `wrap_content` with `minHeight="57dp"`, never a fixed
  height, so they grow with the font size, and set
  `importantForAutofill="no"`, so autofill services leave trip text alone.
- **Touch targets** are at least 48dp each way. The calendar's sizing is in
  `.claude/rules/dates-and-times.md`.
- **Titles and headings:**
  - Every screen but `HomeActivity` sets its own title, which TalkBack
    announces when the screen opens: Add Vacation or Edit Vacation, Add Event
    or Edit Event, and the vacation's title on its page.
  - Each form's heading shows the same words and, like every heading, sets
    `accessibilityHeading`.
  - Never give `HomeActivity` an `android:label`: on the launcher activity it
    would rename the app's icon.
- **Edge to edge (required at targetSdk 36):**
  - Every screen except `HomeActivity` calls `setEdgeToEdgeContentView()`
    instead of `setContentView()`, so content stays clear of the system
    bars. While the keyboard is open, that helper also ends the root view at
    the keyboard's top edge.
  - The two detail forms are in a `ScrollView` with `adjustResize`, so every
    field and button can be scrolled into view in landscape and with the
    keyboard open (a full-screen landscape keyboard still covers the form
    until it is closed).
  - `HomeActivity` uses `setEdgeToEdgeContentViewWithBottomBar()`. It pads
    the tab content for the status bar and the sides, and pads the bottom bar
    for the navigation bar and the sides, so the bar's background reaches
    behind the navigation bar with no scrim. It replaces the bar's own inset
    handling, which would add the keyboard's height, and it does nothing for
    the keyboard: a text field added to a home tab would need its own
    keyboard handling.
- **Dialogs (fixed 2026-11-01, ADR-0033):**
  - Every dialog is a `DialogFragment` (`MessageDialog` for a message with
    an OK button), so it's still open after a rotation. A dialog shown
    straight from `MaterialAlertDialogBuilder` closes when the screen is
    recreated.
  - A message that may run past two lines is a dialog, not a toast: Android
    cuts toasts at two lines.
  - A form that closes after a save disables its buttons first, so a second
    tap, or one behind an offer that keeps it open, can't save twice.
- **Data on each screen:**
  - Every Activity or Fragment that reads or writes data opens its own Room
    instance with `AppDatabase.open()` and closes it in `onDestroy`
    (`.claude/rules/database.md`).
  - Screens pass data through Intent extras with literal keys
    (`vacationId`, `title`, `details`, `startDate`, `endDate`, `eventId`).
    The instrumented tests and the reminder notifications use the same keys.
  - The vacation form prefills from the extras.
  - The event form takes only `eventId` (none for a new event) and
    `vacationId`, and reads the event from the database, because a
    reminder's extras are copied when it's posted and Save would write old
    values back. It closes itself if the event was deleted.
  - The vacation page (`EventListActivity`) takes only `vacationId` and
    reloads the vacation from the database on every resume, closing itself
    if the vacation was deleted.
  - The detail screens call `ReminderScheduler` on every save and delete.
- **Validation enforced today:**
  - Every field except trip details and event details is required (an
    event's time zone always has a value).
  - A vacation's end date is on or after its start date.
  - An event's date falls within its vacation's start and end dates,
    inclusive, and an edited vacation's new dates must still hold its events
    (`DateValidators.isOutsideVacation`, added 2026-10-31, ADR-0028).
  - A vacation that has events can't be deleted.
  - The last two checks exist only in `VacationDetailActivity`, because
    `excursions.vacationId` has no foreign key.
- **Text:** user-visible text goes in `res/values/strings.xml`. The app is
  English only (ADR-0036). Dates and times on screen go through
  `TripText.kt`; never show a stored date string
  (`.claude/rules/dates-and-times.md`).
- **The launcher icon** (made 2026-10-31, replacing the template robot):
  - a white rolling suitcase (`drawable/ic_launcher_foreground.xml`) on the
    Okabe-Ito blue `#0072B2` (`ic_launcher_background.xml`), inside the
    adaptive icon's 66dp safe circle, so the Android 12+ splash screen shows
    it whole
  - the foreground is also the monochrome layer for themed icons, so keep it
    one flat colour with no shadow
  - minSdk 26 always uses `mipmap-anydpi`, so there are no density PNGs
  - the Play icon is drawn from the same paths: when the icon changes,
    redraw it with `/update-store-listing`
- **Third-party icons:** Material Design icons are listed in `NOTICE`. A new
  icon from anywhere else is third-party material that ships in the app
  (`.claude/rules/dependencies.md`).
- **Screens in the store listing:** when a screen that a Play screenshot
  shows changes, retake that screenshot (`/update-store-listing`).
- **R8 and resources:** release builds remove unused resources. A resource
  looked up by name needs a check in a release build on an emulator
  (`docs/05-release.md`, step 3).
- **Before reporting done:**
  - run `./gradlew lint` and `./gradlew testDebugUnitTest`
    (`ThemeContrastTest` for any colour change)
  - the screen flows are tested by the instrumented tests in
    `app/src/androidTest`; run them only on an emulator, after asking
    (`.claude/rules/instrumented-tests.md`)
  - what TalkBack reads, how a screen looks at a large font size, and the
    launch-window flash can only be checked on a device or emulator by eye:
    say whether you did
