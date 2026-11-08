# Subsystem: Screens and navigation

The screens and the two home tabs, how they hand data to each other, and
what they check before saving or deleting. The home screen opens on a
dashboard of every vacation; a bottom navigation bar switches between the
Vacations and Settings tabs; tapping a vacation opens its page with its
events (asked for by the user on 2026-10-25, ADR-0007). Built: product
requirements 1, 2 and 9 (`docs/00-overview.md`); M1 built the dashboard
and navigation (`docs/roadmap.md`).

Files, under `app/src/main/java/com/example/vagor/` and `app/src/main/res/`:
`HomeActivity.kt`, `VacationsFragment.kt`, `Dashboard.kt`,
`EventListActivity.kt`, `VacationDetailActivity.kt`,
`EventDetailActivity.kt`, `EdgeToEdge.kt`; the layouts `activity_home.xml`,
`fragment_vacations.xml`, `item_vacation.xml`, `item_dashboard_heading.xml`,
`activity_event_list.xml`, `item_event.xml`, `activity_vacation_detail.xml`
and `activity_event_detail.xml`; `menu/bottom_navigation.xml`; and the
activities in `AndroidManifest.xml`. Path-scoped rules:
`.claude/rules/ui-and-accessibility.md`.

Elsewhere: the Settings tab is `docs/subsystems/settings-screen.md`; the
date, time and zone fields are `docs/subsystems/dates-times-and-pickers.md`;
reminders and their offers are `docs/subsystems/reminders.md`; Share and
Add to Calendar are `docs/subsystems/sharing-and-calendar.md`; colours,
sizes and TalkBack are `docs/subsystems/theme-and-accessibility.md`.

## Contents

| File | Provides |
| :- | :- |
| `HomeActivity.kt` | The launcher activity: the tab container, the bottom navigation bar, Back from Settings |
| `VacationsFragment.kt` | The Vacations tab (the dashboard): the list, Add Vacation, the notifications check |
| `Dashboard.kt` | Plain-Kotlin dashboard order and trip status: `Dashboard`, `TripStatus`, `DashboardRow` |
| `EventListActivity.kt` | The vacation's page: its details, Edit Vacation, Share Vacation, Add to Calendar, its events, Add Event |
| `VacationDetailActivity.kt` | The add/edit vacation form |
| `EventDetailActivity.kt` | The add/edit event form, with Share and Add to Calendar |
| `EdgeToEdge.kt` | `setEdgeToEdgeContentView()` and `setEdgeToEdgeContentViewWithBottomBar()` |
| `menu/bottom_navigation.xml` | The two tabs: `nav_vacations` and `nav_settings` |

## Screen map

```
HomeActivity (launcher)
├── Vacations tab: VacationsFragment, the dashboard
│   ├── Add Vacation ─────────────▶ VacationDetailActivity (new)
│   └── a vacation ───────────────▶ EventListActivity, the vacation's page
│       ├── Edit Vacation ────────▶ VacationDetailActivity (edit)
│       ├── Share Vacation ───────▶ the Sharesheet
│       ├── Add to Calendar ──────▶ a calendar app's new-event screen
│       ├── Add Event ────────────▶ EventDetailActivity (new)
│       └── an event ─────────────▶ EventDetailActivity (edit)
│           ├── Share ────────────▶ the Sharesheet
│           └── Add to Calendar ──▶ a calendar app's new-event screen
└── Settings tab: SettingsFragment
```

Tapping a reminder (`MyReceiver`, through a `TaskStackBuilder`):

| Reminder | Opens | Behind it |
| :- | :- | :- |
| Vacation start or end | `EventListActivity` | `HomeActivity` |
| Event | `EventDetailActivity` | `EventListActivity`, then `HomeActivity` |

## Intent extras

Screens pass data through Intent extras with literal keys: `vacationId`,
`title`, `details`, `startDate`, `endDate`, `eventId`. The instrumented
tests and the reminder notifications use the same keys.

| Screen | Reads | Sent by |
| :- | :- | :- |
| `EventListActivity` | `vacationId` only | The dashboard; a vacation reminder; the event reminder's back stack |
| `VacationDetailActivity` | `vacationId` (`-1` or missing: a new vacation), `title`, `details`, `startDate`, `endDate` | Edit Vacation sends all five; Add Vacation sends none |
| `EventDetailActivity` | `eventId` (missing: a new event), `vacationId` | Add Event (`vacationId` only); an event row and an event reminder (both) |

- **The vacation form prefills from the extras.**
- **The event form takes only `eventId` and `vacationId`** and reads the
  event from the database, because a reminder's extras are copied when it
  is posted and Save would write old values back. It closes itself if the
  event was deleted.
- **The vacation page takes only `vacationId`** and reloads the vacation
  from the database on every resume, closing itself if the vacation was
  deleted (from its own edit screen, for example).

## Home screen (`HomeActivity`)

- **Launcher.** The only exported activity, with the `MAIN`/`LAUNCHER`
  intent filter. Never give it an `android:label`: on the launcher
  activity it would rename the app's icon. It is the one screen that sets
  no title of its own.
- **Tabs.** `homeContent` (a `FragmentContainerView`) above
  `bottomNavigation` (a `BottomNavigationView` styled
  `Widget.Vagor.BottomNavigation`). The menu has `nav_vacations`
  ("Vacations", `ic_flight`) and `nav_settings` ("Settings",
  `ic_settings`). The first create adds `VacationsFragment`; choosing a tab
  replaces the fragment; reselecting a tab does nothing.
- **Recreation.** After a rotation, or the dark mode switch, the fragment
  manager restores the open tab and the bar restores its selection;
  neither calls the tab listener.
- **Back.** Back from the Settings tab returns to Vacations instead of
  leaving the app (an `OnBackPressedCallback` enabled while Settings is
  shown).
- **Font size.** The tab labels grow with the system font size
  (`setLabelFontScalingEnabled(true)`).
- **Resume.** `onResume` calls `ReminderScheduler.rebuildIfNeeded`:
  setting Vagor's battery use to Unrestricted allows exact alarms without a
  broadcast, so it checks again on the way back, before the Settings tab
  says whether event reminders are exact (`docs/subsystems/reminders.md`).

## Dashboard (`VacationsFragment`, `Dashboard`)

The app opens here: every vacation with its dates, and a blue Add Vacation
button (`Widget.Vagor.AddButton`, an extended floating action button at the
bottom end).

- **The list** is a `ListView` with an `ArrayAdapter<DashboardRow>` of two
  view types: a vacation card (`item_vacation.xml`) and the "Past" heading
  (`item_dashboard_heading.xml`, an accessibility heading that can't be
  selected or clicked). With no vacations it shows "No vacations yet. Tap
  Add Vacation to plan one."
- **A card** shows the title, the dates (`vacationDateRange`: one date, or
  "start – end"), the start of the trip details (at most two lines, hidden
  when blank) and the trip status.
- **Order** (`Dashboard.rows`):
  1. Current and upcoming vacations, soonest at the top: by start date,
     then end date, then title (ignoring case), then id.
  2. Vacations whose dates need fixing (`TripStatus.InvalidDates`: a date
     that isn't strict `MM/dd/yyyy`, or an end before the start), by title,
     then id.
  3. A "Past" heading, only when some vacation has ended, then the
     vacations that have ended, most recent first: by end date, then start
     date (both latest first), then title, then id.
- **Status** (`Dashboard.status` and `tripStatusText`):

  | `TripStatus` | Text |
  | :- | :- |
  | `Upcoming`, 1 day | "Starts tomorrow" |
  | `Upcoming`, n days | "Starts in n days" (a plural) |
  | `InProgress`, starts and ends today | "Today" |
  | `InProgress`, starts today | "Starts today" |
  | `InProgress`, ends today | "Ends today" |
  | `InProgress` | "Happening now" |
  | `Past` | "Ended" |
  | `InvalidDates` | "Check the dates" |

- **Reload.** Every resume reloads every vacation. The adapter is refilled
  rather than replaced, with one change notification, so the list keeps its
  scroll position and focus; clearing it on its own would briefly show the
  empty view and reset the list.
- **Taps and keys.** A tap goes through the card itself (`vacationCard`,
  clickable but not focusable). Keyboard focus stays on the list and its
  focus ring (`dashboard_focus_selector`), and the list's item click
  handles Enter and D-pad selection.
- **Clearance.** The list has 88dp of bottom padding, so the last card
  scrolls clear of the Add Vacation button.
- **Notifications check.** Once per launch, when there is at least one
  vacation: if notifications are off, the dashboard asks for
  `POST_NOTIFICATIONS` on Android 13+ when it isn't granted, and shows
  `NotificationsOffDialog` if the user turns that down; otherwise it shows
  the dialog straight away (`docs/subsystems/reminders.md`).

## Vacation page (`EventListActivity`)

- **Title.** The screen's title is the vacation's title, which TalkBack
  announces; the heading shows the same words.
- **Details.** The dates (`vacationDateRange`) and the trip details
  (hidden when blank).
- **Buttons.** Edit Vacation, Share Vacation and Add to Calendar, full
  width, one above the other (ADR-0016).
- **Events** under an "Events" heading, in date and time order
  (`EventTimes.ORDER`: date, then time of day with untimed events first on
  their date, then title). Each row (`item_event.xml`) shows the title,
  `eventWhenText` (such as "Jul 6, 2031, 2:30 PM JST") and the first two
  lines of the details. With none it shows "No events yet. Tap Add Event to
  plan one."
- **Plain rows, not a list.** A vacation has a handful of events, so they
  are plain rows in one `ScrollView` rather than a list with a header, so
  keyboard and D-pad focus follow the screen's order.
- **Focus.** Rebuilding the rows on resume gives keyboard focus back to the
  row that had it, and so does a rotation (`focusedEventId` in saved state).
  A focused row scrolls up above the Add Event button
  (`@dimen/add_button_clearance`, 88dp), which the page's bottom padding
  also clears. The `ScrollView` has an id, so its scroll position survives
  rotation.
- **Share Vacation** sends `ACTION_SEND` text inside `Intent.createChooser`:
  the title and dates, the trip details if any, and each event with its
  date and time (`docs/subsystems/sharing-and-calendar.md`, ADR-0009).
- **Add to Calendar** builds `vacationCalendarIntent(vacation, events)`.
  For a date saved before dates were checked strictly there is none, and a
  toast says "Edit the vacation and choose its dates again"; otherwise
  `openCalendarInsert` (ADR-0015).
- **Add Event** opens `EventDetailActivity` with `vacationId`.

## Vacation form (`VacationDetailActivity`)

- **Title.** "Add Vacation" or "Edit Vacation"; the heading
  (`vacationFormHeading`) shows the same words.
- **Fields.** Title (required); trip details (optional, several lines,
  growing with each line typed); start date and end date (`DateField`s,
  filled only from the drop-down calendar; the end date's calendar starts
  at the start date, ADR-0008).
- **New or edit.** A `vacationId` of `-1` is a new vacation, which has no
  Delete button. Otherwise the fields are prefilled from the extras.
- **Save** checks, in order, and stops at the first that fails:

  | Check | Toast |
  | :- | :- |
  | Title, start date and end date are filled | "Please enter a title, start date and end date" |
  | Both dates are strict dates (`isValidDate`) | "Choose the date again from the calendar" |
  | The end date is on or after the start date | "End date must be after or equal to start date" |
  | An edited vacation's events all fall within the new dates (`isOutsideVacation`) | "Events are outside these dates: move or delete them first" |

  The last check was added on 2026-10-31 (ADR-0028). An event with an old
  loose date can't be checked and doesn't block the save.
- **After Save** the form inserts or updates the row, calls
  `ReminderScheduler.scheduleVacation`, shows "Vacation saved" or
  "Vacation updated", disables Save and Delete, and closes. For a vacation
  that ends more than two months ahead, while Android may hibernate Vagor,
  `HibernationOfferDialog` comes first, once per launch, and closes the
  form whichever way it is closed (ADR-0032).
- **Delete** is refused while the vacation has events: "Cannot delete
  vacation with associated events". Otherwise it deletes the row, calls
  `ReminderScheduler.cancelVacation`, shows "Vacation deleted" and closes;
  the vacation page behind it then closes itself, back to the dashboard.

## Event form (`EventDetailActivity`)

- **Title.** "Add Event" or "Edit Event"; the heading (`eventFormHeading`)
  shows the same words.
- **Fields.** Title (required); date (a `DateField` whose calendar offers
  only its vacation's days, read from the database each time it opens,
  ADR-0008);
  time (required, from Material's clock, `MaterialTimePicker`, in the
  phone's 12- or 24-hour style, opening at the picked time or 9:00 AM);
  time zone (from the searchable `TimeZonePickerDialog`, starting on the
  event's saved zone or the phone's); details (optional). The time and
  zone fields can't be typed into (`makePickOnly()`,
  `docs/subsystems/dates-times-and-pickers.md`).
- **Picked values.** The time and zone are kept in fields of their own
  (`pickedTime`, `pickedZoneId`), saved across rotation as `pickedTime` and
  `pickedTimeZone`; the two text fields only show them and don't save their
  own text. A clock that was open when the screen was recreated gets its
  listener back.
- **New or edit.** No `eventId` is a new event, which has no Delete button.
- **Save** checks, in order:

  | Check | Toast |
  | :- | :- |
  | Title, date and time are filled | "Please enter a title, date and time" |
  | The vacation still exists | "Associated vacation not found" |
  | The event's date is a strict date | "Choose the date again from the calendar" |
  | The vacation's dates are strict dates | "Edit the vacation and choose its dates again" |
  | The date is within the vacation's dates, inclusive (`isDateWithinVacation`) | "Event date must be within the vacation dates" |

- **After Save** the form inserts or updates the row with the time
  (`DateValidators.formatTime`) and zone, shows "Event saved" or "Event
  updated", disables Save, Delete, Share and Add to Calendar, and calls
  `ReminderScheduler.eventSaved` with whether the date, time or zone
  changed (a new event counts as changed). When the reminder can't be
  exact, `ExactAlarmOfferDialog` comes first, once per launch, and closes
  the form whichever way it is closed (ADR-0014); otherwise the form
  closes.
- **Delete** deletes the row, calls `ReminderScheduler.cancelEvent`, shows
  "Event deleted" and closes. An event can always be deleted.
- **Share and Add to Calendar** take what the form shows, saved or not.
  Share sends the title, date, time with its zone, vacation and details as
  text; Add to Calendar needs a title, date and time ("Please enter a
  title, date and time") and a readable date ("Choose the date again from
  the calendar").
- **Old events.** Events saved before 2026-10-27 have no time until
  they're next edited, and can't be saved again without one (ADR-0013).

## Edge to edge and the keyboard

Screens draw behind the status and navigation bars (Android 15+, with no
opt-out from targetSdk 36), so `EdgeToEdge.kt` pads them.

- **Every screen except `HomeActivity`** calls
  `setEdgeToEdgeContentView()` instead of `setContentView()`, so content
  stays clear of the system bars. It pads the layout's root by the bars
  and any display cutout, on top of the root's own padding. While the
  keyboard is open the root ends at the keyboard's top edge. System bar
  icons follow the app's light or dark theme. Insets are passed on rather
  than consumed: on Android 10 and lower, consuming them would stop them
  reaching sibling views.
- **The two detail forms** are in a `ScrollView` with `adjustResize`
  (`adjustResize|stateHidden` in the manifest), so every field and button
  can be scrolled into view in landscape and with the keyboard open. A
  full-screen landscape keyboard still covers the form until it is closed.
- **`HomeActivity`** uses `setEdgeToEdgeContentViewWithBottomBar()`
  instead. It pads the tab content for the status bar and the sides, and
  the bottom bar for the navigation bar and the sides, so the bar's
  background reaches behind the navigation bar with no scrim. The
  navigation bar style is a fixed light or dark one, not auto: auto turns
  the system's contrast scrim back on whenever the window's configuration
  changes, and the screen is recreated when the theme switches anyway. It
  replaces the bar's own inset handling, which would add the keyboard's
  height, and it does nothing for the keyboard: a text field added to a
  home tab would need its own keyboard handling. The fragment container
  passes the insets on to the tab's layout, which must not pad again.

## Dialogs and messages

- **Every dialog is a `DialogFragment`**, so it is still open after a
  rotation; a dialog shown straight from `MaterialAlertDialogBuilder`
  closes when the screen is recreated (fixed 2026-11-01, ADR-0033). On
  these screens: `NotificationsOffDialog` (dashboard),
  `HibernationOfferDialog` (vacation form), `ExactAlarmOfferDialog`,
  `TimeZonePickerDialog` and the `MaterialTimePicker` clock (event form),
  and `MessageDialog` for "no calendar app" (Add to Calendar).
- **A form that closes after a save disables its buttons first**, so a
  second tap, or one behind an offer that keeps it open, can't save twice.
- **Second taps.** Each dialog's `show` returns early while the fragment
  manager's state is saved or a dialog with its tag is already there, so a
  second tap, or a screen reader's, opens nothing more.
- **Toasts or dialogs.** The form checks above and the saved and deleted
  confirmations are toasts. A message that needs more than two lines, or
  offers a way out, is a dialog instead, because Android cuts a toast at
  two lines: notifications off and "no calendar app" are dialogs.

## Key types

```kotlin
class HomeActivity : AppCompatActivity()
class VacationsFragment : Fragment(R.layout.fragment_vacations)
class EventListActivity : AppCompatActivity()
class VacationDetailActivity : AppCompatActivity()
class EventDetailActivity : AppCompatActivity()

// Where a vacation stands on a given day.
sealed interface TripStatus {
    data class Upcoming(val daysUntilStart: Long) : TripStatus
    data class InProgress(val startsToday: Boolean, val endsToday: Boolean) : TripStatus
    data object Past : TripStatus
    data object InvalidDates : TripStatus
}

sealed interface DashboardRow {
    data class Trip(val vacation: Vacation, val status: TripStatus) : DashboardRow
    data object PastHeader : DashboardRow
}

object Dashboard {
    fun status(vacation: Vacation, today: LocalDate): TripStatus
    fun rows(vacations: List<Vacation>, today: LocalDate): List<DashboardRow>
}

fun AppCompatActivity.setEdgeToEdgeContentView(@LayoutRes layoutResId: Int)
fun AppCompatActivity.setEdgeToEdgeContentViewWithBottomBar(
    @LayoutRes layoutResId: Int,
    @IdRes contentId: Int,
    @IdRes barId: Int
)
```

## Rules

- **Views, not frameworks.** UI is XML layouts, `AppCompatActivity` (plus a
  `Fragment` for each home tab), `findViewById`, and `ListView` with
  `ArrayAdapter` for the dashboard. No Compose, ViewBinding, ViewModel, DI
  framework or coroutines without the user's approval (ADR-0002).
- **Titles.** Every screen but `HomeActivity` sets its own title, which
  TalkBack announces when the screen opens: Add Vacation or Edit Vacation,
  Add Event or Edit Event, and the vacation's title on its page. Each
  form's heading shows the same words and, like every heading, sets
  `accessibilityHeading`.
- **Buttons.** Every button on a page is the same size: full width, one
  above the other, `Widget.Vagor.Button` or `Widget.Vagor.Button.Outlined`
  with `layout_width="match_parent"` and `layout_height="wrap_content"`.
  Only the floating Add Vacation and Add Event buttons differ
  (`Widget.Vagor.AddButton`) (ADR-0016,
  `docs/subsystems/theme-and-accessibility.md`).
- **Text fields** are `wrap_content` with `minHeight="57dp"`, never a fixed
  height, and set `importantForAutofill="no"`. Touch targets are at least
  48dp each way.
- **Colours** come from theme attributes only; never hard-code a text or
  background colour in a layout. **Text** goes in `res/values/strings.xml`.
- **Edge to edge.** New screens call `setEdgeToEdgeContentView()`, never
  `setContentView()`.
- **Dates.** Read and write a `DateField`'s `date`, never its text, and
  never show a stored date string directly (ADR-0026).
- **Reminders.** Keep scheduling in `ReminderScheduler`. The detail screens
  call it on every save and delete.
- **Database.** Each screen opens its own instance with
  `AppDatabase.open()` and closes it in `onDestroy`
  (`docs/subsystems/data-and-storage.md`).
- **Checks that the database can't make.** That an edited vacation still
  holds its events, and that a vacation with events can't be deleted, exist
  only in `VacationDetailActivity`, because `excursions.vacationId` has no
  foreign key.

## Invariants

- Every field except trip details and event details is required, and an
  event's time zone always has a value.
- A vacation's end date is on or after its start date, and an event's date
  falls within its vacation's start and end dates, inclusive, for every
  date the parsers can read.
- A vacation that has events is never deleted from the app.
- The vacation page and the event form never show an item that was
  deleted: they close.
- A form saves at most once each time it opens.
- Back from the Settings tab always lands on the Vacations tab.

## Limits

| Limit | Value | Notes |
| :- | :- | :- |
| Events on a page | No limit in code | One row each; meant for a handful |
| Detail lines on a card or event row | 2 | `maxLines="2"`, ellipsized |
| Button height | 56dp (`@dimen/button_height`) | Grows with the font size |
| Text field height | At least 57dp | Grows with the font size |
| Touch targets | At least 48dp each way | Event rows are at least 72dp tall |
| Add-button clearance | 88dp (`@dimen/add_button_clearance`) | Under the last card or event row |

## Tests

- `DashboardTest` (JVM): the soonest vacation first and the latest last; a
  current vacation before upcoming ones; past vacations below a heading,
  most recent first; the heading with only past vacations; no rows for no
  vacations; vacations with bad dates after current ones and before past
  ones; the same start date sorted by end, then title; and each status.
- `HomeFlowInstrumentedTest`: creating a vacation from the dashboard; the
  dashboard's order; tapping a vacation shows its events; deleting from
  Edit returns to the dashboard; editing updates the vacation's page; a
  vacation can't be moved away from its events; the hibernation offer
  (once, through a rotation, opening App info, and Back closing the form);
  no Delete button on a new vacation or event; and each screen's own
  title and heading.
- `EventFormInstrumentedTest`: saving needs a time and keeps the details;
  the searchable time zone list; an event opens with its zone and details;
  an event deleted since it was opened closes the form; an event saved
  without a time needs one; rotation keeps the picked time and zone and an
  open clock; the time and zone fields can't be typed into; the vacation
  page's events in time order with their times and details; and the exact
  alarm offer (once, through a rotation, opening Alarms & reminders, and
  Back closing the form).
- `SettingsInstrumentedTest.backFromSettingsReturnsToVacations`.
- `ReminderInstrumentedTest.tappingAReminderOpensTheVacationPageOrTheEvent`.
- `HomeFlowInstrumentedTest`, `EventFormInstrumentedTest` and
  `ReminderInstrumentedTest` write to the app's real `vacation_database`
  and set real alarms: run them only on an emulator or a test device. The
  home flow's tests of the hibernation offer run only on Android 12+ while
  Android may pause Vagor; the event form's tests of the exact alarm offer
  run only while exact alarms are off. Both leave the other app's screen
  through `leaveOtherAppsScreen()` (`docs/04-quality.md`,
  `.claude/rules/instrumented-tests.md`).

## Open questions

- Asking before discarding unsaved edits was offered to the user on
  2026-10-31 and not picked (`docs/roadmap.md`, Declined).
- Importing a trip someone shared isn't supported; whether Vagor should is
  still open (ADR-0009).
- A text field on a home tab would need its own keyboard handling, since
  `setEdgeToEdgeContentViewWithBottomBar()` does nothing for the keyboard.
