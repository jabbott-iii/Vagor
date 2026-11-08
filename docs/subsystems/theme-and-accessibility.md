# Subsystem: Theme and accessibility

Vagor's colours, dark mode, focus rings, sizes and screen-reader support,
and its launcher icon and Play graphics: `res/values/colors.xml`,
`res/values/themes.xml`, `res/values-night/themes.xml`,
`res/values/styles.xml`, `res/values/dimens.xml`,
`res/values-v28/dimens.xml`, `res/color/`, the focus drawables in
`res/drawable/`, `ThemeSettings.kt`, `VagorApplication.kt` and
`CalendarDayBackground.kt`, the launcher icon
(`res/drawable/ic_launcher_foreground.xml`,
`res/drawable/ic_launcher_background.xml`, `res/mipmap-anydpi/`), and the
Play graphics in `fastlane/metadata/android/en-US/images/` (resource paths
are under `app/src/main/`). Everything here is built. It covers product
requirements 6 (dark mode) and 7 (accessible colours) in
`docs/00-overview.md`, and ADR-0006, ADR-0016 and ADR-0017. The Settings
tab's switch is in `docs/subsystems/settings-screen.md`. Path-scoped rules:
`.claude/rules/ui-and-accessibility.md`.

## Contents

| File | Provides |
| :- | :- |
| `res/values/colors.xml` | The light and dark blue schemes, every role as `vagor_light_*` and `vagor_dark_*` |
| `res/values/themes.xml` | `Base.Theme.Vagor` (light), `Theme.Vagor`, `ThemeOverlay.Vagor.MaterialTimePicker` |
| `res/values-night/themes.xml` | `Base.Theme.Vagor` (dark) |
| `res/values/styles.xml` | `Widget.Vagor.Button`, `Widget.Vagor.Button.Outlined`, `Widget.Vagor.AddButton`, `Widget.Vagor.BottomNavigation` |
| `res/values/dimens.xml`, `res/values-v28/dimens.xml` | Button and calendar sizes; ripple opacities, which differ before Android 9 |
| `res/color/` | Colour state lists from theme attributes: calendar days, today, ripples, the bottom bar's icons |
| `res/drawable/*focus*`, `list_row_foreground.xml`, `calendar_month_button_background.xml` | The focus rings Vagor draws itself |
| `ThemeSettings.kt` | `ThemeSettings`: the dark mode switch's stored value and applying it |
| `VagorApplication.kt` | Applies the saved theme before any screen is created |
| `CalendarDayBackground.kt` | The calendar days' circles, today's ring and the focus ring (`docs/subsystems/dates-times-and-pickers.md`) |
| `res/drawable/ic_launcher_*.xml`, `res/mipmap-anydpi/` | The adaptive launcher icon, also the themed icon and the Android 12+ splash screen |
| `fastlane/metadata/android/en-US/images/` | `icon.png`, `featureGraphic.png` and the phone screenshots for Play |

## Key types

```kotlin
object ThemeSettings {
    fun isDarkMode(context: Context): Boolean
    fun setDarkMode(context: Context, enabled: Boolean)
    fun applySaved(context: Context)
}

fun calendarDayBackground(context: Context, isToday: Boolean): Drawable
```

```xml
<style name="Base.Theme.Vagor" parent="Theme.Material3.DayNight.NoActionBar">
<style name="Theme.Vagor" parent="Base.Theme.Vagor">
<style name="ThemeOverlay.Vagor.MaterialTimePicker" parent="ThemeOverlay.Material3.MaterialTimePicker">
<style name="Widget.Vagor.Button" parent="Widget.Material3.Button">
<style name="Widget.Vagor.Button.Outlined" parent="Widget.Material3.Button.OutlinedButton">
<style name="Widget.Vagor.AddButton" parent="Widget.Material3.ExtendedFloatingActionButton.Icon.Primary">
<style name="Widget.Vagor.BottomNavigation" parent="Widget.Material3.BottomNavigationView">
```

## Colours (ADR-0017)

- **A blue Material 3 scheme for colour-blind users,** from the Okabe-Ito
  blue `#0072B2`, generated with Material colour utilities: the tone 40
  primary (`#00639B`) in light, tone 80 (`#97CBFF`) in dark. Blue stays
  distinct for the common colour vision deficiencies.
- **Every text/background pair must meet WCAG AA** (4.5:1; 3:1 for non-text
  UI) in both themes; `ThemeContrastTest` enforces this.
- **Don't convey anything by colour alone.** As built: today in the
  calendar has a ring; the time zone list marks its zone with a tick and
  "Selected"; the selected AM or PM in the clock is filled with blue
  (below); days that can't be picked are dimmed and disabled.
- **Don't add DynamicColors:** it would replace the blue with wallpaper
  colours.
- **Colours come from theme attributes only** (`res/values/colors.xml`,
  `values/themes.xml`, `values-night/themes.xml`). Never hard-code a text or
  background colour in a layout; layouts use attributes such as
  `?attr/colorOnSurface`, and the colour state lists in `res/color/` are
  built from them.
- **Text and controls use the scheme's colours.** Material 3's default text
  and control colours are fixed neutrals, so both themes set
  `android:textColorPrimary`, `textColorSecondary`, `textColorTertiary`,
  `textColorHint`, `textColorHighlight`, `colorControlNormal` and
  `colorControlActivated` from the scheme.
- **Restyled components:**
  - the bottom bar's selected tab indicator is the primary blue, so it
    stands out from the bar (3:1 or more); Material 3's default
    secondaryContainer pill is about 1.1:1. The selected tab's icon is
    onPrimary on it.
  - the blue Add Vacation and Add Event buttons are the primary blue in
    both themes, with a ripple at Material 3's state-layer opacities
    (`vagor_on_primary_ripple`); `values-v28/dimens.xml` holds the Android
    9+ opacities, because RippleDrawable draws them differently before
    Android 9.

## Dark mode (ADR-0006)

- **A switch on the Settings tab, on by default** (asked for by the user on
  2026-10-25 as an on/off switch, not a follow-system option).
- **Stored** in the SharedPreferences file `settings`, key `dark_mode`,
  default true: a UI preference, not trip data, so not in
  `vacation_database`. It is the only stored state besides the database
  (`docs/subsystems/data-and-storage.md`).
- **Applied** with `AppCompatDelegate.setDefaultNightMode` (night yes or
  no). AppCompat doesn't persist the night mode, so `VagorApplication` calls
  `ThemeSettings.applySaved` on every process start, before any activity is
  created, and the app's first frame already uses the saved theme.
- Changing it recreates every open screen in the new theme; the home
  screen comes back on the Settings tab.
- On Android 12+ it also calls `UiModeManager.setApplicationNightMode`,
  which Android remembers, so later launches' splash screens match the app.

### Known issue: the launch window

- The system draws the launch window before any app code runs.
- On Android 8–11 it follows the system light/dark setting on every cold
  start, so a dark-mode user on a light-themed phone sees a brief light
  flash.
- On Android 12+ only the very first launch does; later launches match the
  app setting through `UiModeManager.setApplicationNightMode` (not captured
  on a device).

## Keyboard focus

- **Keyboard focus must be visible.**
- **The theme turns on Material's focus rings** (`focusRingsEnabled`, not
  yet public API, so it carries `tools:ignore="PrivateResource"`; if it is
  renamed, the build fails): two-tone rings, secondary outside and
  onSecondary inside, on buttons, floating action buttons, cards and bottom
  bar tabs.
- **The dashboard and time zone lists have their own focus rings:**
  - the dashboard's `ListView` draws `dashboard_focus_selector`, a 3dp
    primary ring matching the cards' padding and 12dp corners. The cards
    are clickable for touch (their own ripple) but not focusable, so
    keyboard focus stays on the list and its ring; the list's item click
    handles Enter.
  - the time zone list draws `list_focus_selector`, a 2dp onSurface ring.
- **The calendar's days have one:** `calendar_focus_ring`, a rounded square
  that fills the day's cell, so it fits however narrow the calendar is. The
  calendar's ripple shows no fill on focus, which would lower the contrast
  of today's blue number. The month buttons have a round ring
  (`calendar_month_button_background`).
- **The vacation page's event rows** use `list_row_foreground`: the usual
  ripple plus a ring, because the ripple's own focus tint is too faint to
  see. They are plain rows in a `ScrollView`, so focus follows the screen;
  a focused row is scrolled clear of the Add Event button
  (`add_button_clearance`, 88dp), and the focused row is focused again
  after the rows are rebuilt or the screen is recreated.

## The clock's AM and PM

Material's time picker marks its selected AM/PM by hue alone: it fills it
with tertiaryContainer (a purple) about as light as the unselected one.
`ThemeOverlay.Vagor.MaterialTimePicker` (set as `materialTimePickerTheme`
in `Theme.Vagor`) fills it with the primary blue instead, with onPrimary
text, which stands out by lightness too. The selected hour or minute keeps
Material's colours (primaryContainer): the dial's numbers also show which
is being set, and a primary fill would hide the primary text cursor when
the hour and minute are typed.

## Sizes

- **Every button on a page is the same size** (asked for by the user on
  2026-10-26, ADR-0016): full width, one above the other, styled
  `Widget.Vagor.Button` or `Widget.Vagor.Button.Outlined` with
  `layout_width="match_parent"` and `layout_height="wrap_content"`.
  - The style sets one height (`@dimen/button_height`, 56dp, with no top or
    bottom inset), which grows with the font size, so a label is never cut
    off. Stacked buttons are `button_spacing` (8dp) apart.
  - Only the floating Add Vacation and Add Event buttons differ
    (`Widget.Vagor.AddButton`).
- **Text fields** are `wrap_content` with `minHeight="57dp"`, never a fixed
  height, so they grow with the font size, and set
  `importantForAutofill="no"`, so autofill services leave trip text alone.
- **Touch targets are at least 48dp each way.**
- **The calendar's days** are at least 48dp tall at any font size, and the
  calendar is wider than its field where the field is too narrow for seven
  48dp days (a 360dp-wide phone), up to the screen's width less 8dp on each
  side; on narrower screens the days are as wide as that allows. The day
  circles shrink to fit a narrow cell instead of being cut off
  (`docs/subsystems/dates-times-and-pickers.md`).
- **The bottom bar's tab labels** are always shown and grow with the system
  font size, like the rest of the app's text.

## Screen readers

- **Every screen but `HomeActivity` sets its own title,** which TalkBack
  announces when the screen opens: Add Vacation or Edit Vacation, Add Event
  or Edit Event, and the vacation's title on its page.
- **Each form's heading shows the same words** and, like every heading,
  sets `accessibilityHeading` (the dashboard's and Settings' headings, the
  vacation page's headings, the dashboard's Past heading and the calendar's
  month title too).
- **Never give `HomeActivity` an `android:label`:** on the launcher
  activity it would rename the app's icon.
- **Fields filled from a picker** read as drop-down lists, with their name
  and then the picked value, and can't be typed into
  (`docs/subsystems/dates-times-and-pickers.md`).
- **The calendar** is announced by its field's name; its month title is a
  live region; each day reads its full date, and today adds "Today".
- **Dialogs** are `DialogFragment`s, so a dialog is still open after a
  rotation (ADR-0033). The notifications-off message and "no calendar app"
  are dialogs, not toasts, because Android cuts toasts at two lines; short
  messages after a save or a failed check are toasts.

## Launcher icon and Play graphics

- **The launcher icon** (made 2026-10-31, replacing the template robot) is a
  white rolling suitcase (`drawable/ic_launcher_foreground.xml`) on the
  Okabe-Ito blue `#0072B2` (`ic_launcher_background.xml`), inside the
  adaptive icon's 66dp safe circle, so the Android 12+ splash screen shows
  it whole.
- **The foreground is also the monochrome layer** for themed icons, so keep
  it one flat colour with no shadow.
- **minSdk 26 always uses `mipmap-anydpi`,** so there are no density PNGs.
- **The Play icon** (`fastlane/metadata/android/en-US/images/icon.png`) is
  drawn from the same paths: redraw it when the icon changes. Play wants it
  512×512 and a full square with no shadow, since Play rounds the corners
  itself.
- **The feature graphic** (`featureGraphic.png`, 1024×500) is the name and
  tagline ("Your trips, planned offline") in the centre, on the same blue,
  above a dotted route between two map pins, with no icon, device or
  screenshot, as Play's feature graphic guidelines ask (redrawn
  2026-11-01).
- **The six phone screenshots** are of invented trips, taken 2026-11-01 at
  1080×1920; `06_light_mode.png` shows light mode. Retake a screenshot when
  the screen it shows changes. The rules, alt text and commands are in
  `docs/05-release.md`, step 6, and `/update-store-listing`.
- **No Google logo or Calendar icon** in the graphics or screenshots, and no
  Google name in the app's title or icon (`docs/05-release.md`, step 6).

## Invariants

- Every pair in `ThemeContrastTest` meets its minimum in both schemes.
- Every text and background colour in a layout is a theme attribute, or a
  colour state list built from theme attributes.
- Nothing is told apart by colour alone.
- Every button on a page but the Add buttons has the same width and height
  at a given font size.
- Keyboard focus is visible wherever it goes (requirement 7).

## Limits

| Limit | Value | Notes |
| :- | :- | :- |
| Text contrast | 4.5:1 at least | WCAG AA, both themes |
| Non-text UI contrast | 3:1 at least | WCAG AA, both themes |
| Touch target | 48dp each way at least | Calendar days narrower only on screens under 360dp |
| Page button height | 56dp, or more at large font sizes | `@dimen/button_height` |
| Text field height | 57dp at least, growing with the font | `minHeight="57dp"` |
| Calendar day circle | Up to 36sp across | Smaller in a narrow cell |
| Adaptive icon safe area | 66dp circle | |
| Play icon | 512×512 | |
| Feature graphic | 1024×500 | |

## Tests

- **JVM** (`ThemeContrastTest`): reads `src/main/res/values/colors.xml`
  and checks each listed pair in the light and dark schemes, 4.5:1 for text
  (`textMeetsWcagAa`) and 3:1 for non-text UI (`nonTextUiMeetsWcagAa`):
  among them the dashboard cards, the bottom bar's labels and indicator,
  the Add buttons, Material's focus rings, the calendar's weekdays, days,
  today's ring and picked day, and the clock's dial, AM/PM outline and
  selected AM or PM. It checks only the pairs it lists, so a new
  text/background pair is enforced once it's added there.
- **Instrumented:**
  - `SettingsInstrumentedTest`: dark mode is on by default; the switch
    turns it off and on, recreating the screen in the new theme on the
    Settings tab. It flips the dark mode setting and restores it.
  - `HomeFlowInstrumentedTest.eachScreenHasItsOwnTitle`: each screen's
    title, and each form's heading with the same words, marked as a
    heading.
  - `CalendarDropdownInstrumentedTest`: days at least 48dp each way where
    the screen allows; today marked for screen readers; today a ring and
    the picked day a filled circle, shrinking to fit a narrow cell; the
    date field read as a drop-down list.
  - `EventFormInstrumentedTest`: the time and zone fields read as
    drop-down lists that can't be typed into.
- **On a device only:** the launch-window flash (not captured on a
  device). `docs/05-release.md`, step 3, also has dark mode switched off and
  on and an open form rotated on the emulator.

## Open questions

None recorded.
