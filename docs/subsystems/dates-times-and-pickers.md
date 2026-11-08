# Subsystem: Dates, times and pickers

How Vagor stores, checks, shows and picks dates, event times and time
zones: `DateValidators.kt`, `DateField.kt`, `CalendarDropdown.kt`,
`CalendarMonth.kt`, `CalendarDayBackground.kt`, `PickOnlyField.kt`,
`TimeZonePicker.kt`, `TripText.kt` and `EventTimes.kt`, all in
`app/src/main/java/com/example/vagor/`. Everything here is built. It covers
product requirements 2 (events) and 10 (calendar date entry) in
`docs/00-overview.md`, and ADR-0008, ADR-0013 and ADR-0026. The forms that
use these fields are in `docs/subsystems/screens-and-navigation.md`; the
columns that hold the values are in `docs/subsystems/data-and-storage.md`.
Path-scoped rules: `.claude/rules/dates-and-times.md`.

## Contents

| File | Provides |
| :- | :- |
| `DateValidators.kt` | `DateValidators`: the only parsing and formatting of stored dates, times and zones, and the date checks the forms run at save time |
| `DateField.kt` | `DateField`: a date field whose `date` is the stored value and whose text is only for show |
| `CalendarDropdown.kt` | `attachCalendarDropdown()`: the month calendar that drops down below a date field |
| `CalendarMonth.kt` | `CalendarMonth`: plain-Kotlin month grid for the calendar |
| `CalendarDayBackground.kt` | `calendarDayBackground()` and `DayCircle`: the calendar days' circles and rings, drawn to fit their cells |
| `PickOnlyField.kt` | `makePickOnly()`: a field filled only from a picker, never typed into |
| `TimeZonePicker.kt` | `TimeZoneChoices`, `TimeZonePickerDialog` and `FullHeightLinearLayout`: the event form's searchable time zone list, labelled from Android's ICU time zone names |
| `TripText.kt` | Dates, times and trip status as display text (`dateText`, `vacationDateRange`, `timeOfDayText`, `eventTimeText`, `eventWhenText`, `tripStatusText`, `displayLocale`) |
| `EventTimes.kt` | `EventTimes`: plain-Kotlin event start times and order |

## Stored formats

| Value | Stored as | Parse with | Write with |
| :- | :- | :- | :- |
| Date | `MM/dd/yyyy` string, such as `07/06/2031` | `DateValidators.parseLocalDate` | `DateValidators.formatDate` |
| Event time | 24-hour `HH:mm` string, such as `14:30` | `DateValidators.parseLocalTime` | `DateValidators.formatTime` |
| Event time zone | IANA zone ID, such as `Asia/Tokyo` | `DateValidators.parseZone` | The ID the time zone list returns |

`Event.time` and `Event.timeZone` are null on events saved before database
version 2 (2026-10-27), and every save since stores both
(`docs/subsystems/data-and-storage.md`).

## Key types

```kotlin
object DateValidators {
    fun isValidDate(dateText: String): Boolean
    fun isEndDateAfterOrEqualStartDate(startDate: String, endDate: String): Boolean
    fun isOutsideVacation(eventDate: String, vacationStart: String, vacationEnd: String): Boolean
    fun isDateWithinVacation(eventDate: String, vacationStart: String, vacationEnd: String): Boolean
    fun formatDate(date: LocalDate): String
    fun parseLocalDate(dateText: String): LocalDate?
    fun formatTime(time: LocalTime): String
    fun parseLocalTime(timeText: String?): LocalTime?
    fun parseZone(zoneText: String?): ZoneId?
}

class DateField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {
    var date: String = ""            // the stored value; setting it sets the shown text
}

fun DateField.attachCalendarDropdown(earliest: () -> LocalDate? = { null }, latest: () -> LocalDate? = { null })
fun EditText.makePickOnly(actionLabel: String, pick: () -> Unit)

object CalendarMonth {
    const val CELLS = 42
    val FIRST_DAY: LocalDate = LocalDate.of(1, 1, 1)
    val LAST_DAY: LocalDate = LocalDate.of(9999, 12, 31)
    fun cells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?>
    fun weekdays(firstDayOfWeek: DayOfWeek): List<DayOfWeek>
}

fun calendarDayBackground(context: Context, isToday: Boolean): Drawable
class DayCircle(
    private val color: ColorStateList,
    private val strokeWidth: Float,
    private val maxDiameter: Float,
    private val clearance: Float
) : Drawable()

object EventTimes {
    val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)
    val ORDER: Comparator<Event>
    fun startOf(event: Event, fallbackZone: ZoneId): ZonedDateTime?
}

// TripText.kt
fun Context.vacationDateRange(vacation: Vacation): String
fun Context.dateText(stored: String): String
fun Context.tripStatusText(status: TripStatus): String
fun Context.displayLocale(): Locale
fun Context.timeOfDayText(time: LocalTime): String
fun Context.eventTimeText(event: Event): String?
fun Context.eventWhenText(event: Event): String

// TimeZonePicker.kt
class TimeZoneChoice(val id: String, val label: String, val description: String)
object TimeZoneChoices {
    fun all(context: Context, include: String?): List<TimeZoneChoice>
    fun label(context: Context, id: String): String
    fun fieldText(context: Context, id: String): String
    fun sameZone(a: String, b: String): Boolean
}
class TimeZonePickerDialog : DialogFragment() {
    companion object {
        const val REQUEST_KEY = "timeZonePicker"
        const val RESULT_ZONE_ID = "zoneId"
        fun show(manager: FragmentManager, selectedZoneId: String)
    }
}
class FullHeightLinearLayout(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs)
```

## Rules

### Storing and checking

- **Parse and validate only through `DateValidators`.** It is strict:
  exactly two-digit month and day, four-digit year, a real calendar date,
  nothing before or after. As built, a regex checks the shape and
  `java.time` parses `MM/dd/uuuu` in `Locale.US` with
  `ResolverStyle.STRICT`, so `02/30/2026`, `9/16/2026` and
  `09/16/2026 ` (a trailing space) are all refused (`DateValidatorsTest`).
- **Times and zones the same way:** parse event times only through
  `parseLocalTime` (exactly two-digit hour, 00 to 23, and minute) and zones
  only through `parseZone`, which returns null for a missing or unknown ID.
- **Date fields are filled only from the drop-down calendar**
  (`attachCalendarDropdown()`), which writes through
  `DateValidators.formatDate`. The save-time checks still run, because
  saved rows may hold older loose dates.
- **New date or reminder math goes in plain Kotlin** beside
  `DateValidators`, so it can be unit tested (`java.time` is available at
  minSdk 26). `CalendarMonth`, `EventTimes`, `ReminderTimes`
  (`docs/subsystems/reminders.md`) and `CalendarTimes`
  (`docs/subsystems/sharing-and-calendar.md`) are built that way.
- **The checks the forms run at save time** (the forms are in
  `docs/subsystems/screens-and-navigation.md`):
  - every field except trip details and event details is required; an
    event's time zone always has a value
  - a vacation's end date is on or after its start date
    (`isEndDateAfterOrEqualStartDate`)
  - an event's date falls within its vacation's start and end dates,
    inclusive (`isDateWithinVacation`)
  - an edited vacation's new dates must still hold its events
    (`isOutsideVacation`, added 2026-10-31, ADR-0028). An event with an old
    loose date can't be checked and doesn't block the save. This check, like
    the one that keeps a vacation with events from being deleted, exists
    only in `VacationDetailActivity`, because `excursions.vacationId` has no
    foreign key.
  - a stored date that can't be read gives "Choose the date again from the
    calendar" (`date_cant_be_read`), or, for the vacation's own dates,
    "Edit the vacation and choose its dates again"
    (`vacation_date_cant_be_read`).

### Showing dates and times (`TripText.kt`)

- **Dates are shown the way the phone writes them** (asked for by the user
  on 2026-10-31, ADR-0026): "Jul 6, 2031" in the US, "6 Jul 2031" in the
  UK.
  - `dateText` formats a stored date with ICU's `yMMMd` skeleton, always in
    the Gregorian calendar (a Thai phone would otherwise show the
    Buddhist-era year).
  - It stays Gregorian before 1582 too, like `java.time` and the calendar:
    ICU switches to the Julian calendar there by default, so the code moves
    the switch to the earliest possible date.
  - A date it can't read, saved before dates were checked strictly, is
    shown as it was stored.
- **Everything a person reads goes through `dateText`:** screens,
  reminders, shared text and calendar descriptions. Never show a stored
  date string directly.
- `vacationDateRange` shows one date when a vacation starts and ends on the
  same day, otherwise "start – end" (`vacation_date_range`).
- `timeOfDayText` shows a time in the phone's 12- or 24-hour style, such as
  "2:30 PM" or "14:30" (`DateFormat.is24HourFormat`, ICU skeleton `hm` or
  `Hm`).
- `eventTimeText` shows an event's time with its zone's short name on that
  day, such as "2:30 PM JST", "2:30 PM EDT" or "2:30 PM GMT+2" (many zones
  have no abbreviation), or null when the event has no time. The time is
  the one picked, as the form shows it, even when a daylight saving change
  skips it. ICU writes fixed offsets as "GMT+05:00" rather than "+05:00".
- `eventWhenText` puts an event's date and time on one line: "Jul 6, 2031,
  2:30 PM JST", or the date alone.
- `displayLocale()` is the language the app's text is shown in: the
  configuration's first locale, or the default locale.
- **ICU for display, `java.time` for stored values.** Display text uses ICU
  (`android.icu`), which knows the phone's own names and patterns;
  `java.time` formatting is for stored values. Check each ICU call's API
  level against minSdk 26: some, such as
  `SystemTimeZoneType.CANONICAL_LOCATION`, arrived later (Android 9). As
  built, the calendar's month title and each day's spoken date are
  formatted with `java.time` from the locale's own pattern (see the
  calendar below).

### `DateField`

- `date` holds the stored value (an `MM/dd/yyyy` date, an older loose one,
  or "" for none). The text is only for show, so read and write `date`,
  never the text.
- A `DateField` keeps its date across rotation itself, and shows it again
  in the current language, which may have changed since.
- Instrumented tests set it with `setDate` and check it with `withDate`
  (`app/src/androidTest/java/com/example/vagor/DateFieldTesting.kt`).

### Pick-only fields (`PickOnlyField.kt`)

- An event's date, time and time zone fields, and a vacation's dates, can't
  be typed into. All of them use `makePickOnly()`, so what they hold is
  always valid:
  - no key listener, not focusable in touch mode, no cursor, no long click
  - a tap closes any keyboard another field has open (it would cover the
    picker), then runs the picker
- **For screen readers** the field is a drop-down list, not an edit box:
  - its click action is named by `actionLabel` ("Choose date", "Choose
    time" or "Choose time zone")
  - it reports itself as a `Spinner`, not editable, with no set-text
    action, and refuses one
  - it reads its name (the hint), then the picked value
    (`picked_field_with_value`)

### The drop-down calendar (`CalendarDropdown.kt`)

- **Every date field opens a month calendar that drops down below the
  field** (asked for by the user on 2026-10-25, ADR-0008), like Material 3's
  docked date picker. The end date calendar starts at the start date; an
  event's calendar offers only its vacation's days, read from the database
  when it opens.
- **The app draws the calendar itself.** Don't switch to the framework
  `CalendarView`: it follows the phone's calendar system (a Thai phone
  would store Buddhist-era years) and marks today by colour alone. The
  calendar is always Gregorian and marks today with a ring.
- **Which month opens:** an empty field opens on its earliest allowed day
  (the start date, or the vacation's first day), or on today when it has
  none. The day shown is kept inside the allowed range.
- **The allowed range** is never wider than 0001-01-01 to 9999-12-31, the
  years `MM/dd/yyyy` can hold (`CalendarMonth.FIRST_DAY`, `LAST_DAY`). A
  range that ends before it starts comes from bad saved dates, and every
  day is offered instead.
- **Picking a day** writes it through `DateValidators.formatDate` to the
  field's `date` and closes the calendar.
- **One calendar at a time:** a screen reader can still activate the field
  behind an open calendar, so a second one isn't opened.
- **It closes with its screen** (a rotation, for example), so its window
  isn't leaked, and fits itself again whenever the screen's layout changes,
  such as when the keyboard closes.
- **Height:** the whole calendar when the screen is tall enough (below the
  field if it fits there, else above it, else over it); on a shorter
  screen, the screen's height, and the calendar scrolls inside the popup.
- **Width:**
  - the field's width, up to `calendar_dropdown_max_width` (412dp), which
    applies in landscape and on tablets
  - where the field is too narrow for seven 48dp days (a 360dp-wide phone,
    or a larger display size), as wide as they need, up to the screen's
    width less `calendar_dropdown_screen_margin` (8dp) on each side, and
    centred under the field
  - on the narrowest screens (about 320dp wide) the days come out a little
    narrower than 48dp
  - a wider calendar is placed from the field's left edge in either
    direction of text, so a right-to-left language centres it too
- **The grid** is always six weeks of seven days (`CalendarMonth.CELLS`,
  42), so the calendar's height stays steady. Weeks start on the locale's
  first day of the week. Cells before the 1st and after the last day are
  blank; days outside the allowed range are shown but can't be picked, and
  a click on one is checked again, because accessibility services can
  click a disabled view.
- **Day size:** each day is at least 48dp tall at any font size (the larger
  of `calendar_day_height`, 48sp, and `calendar_day_min_size`, 48dp).
- **The month title** is "October 2026" in the locale's own order and
  digits. Its pattern comes from the locale without its Unicode extensions,
  because one that asks for another calendar system (`-u-ca-chinese`, for
  example) gives pattern letters `java.time` can't format. It is a heading
  and a polite live region, so a month change is announced.
- **The previous and next month buttons** hide (invisible, not gone, so the
  title stays centred) at the first and last allowed month. A button that
  has keyboard or screen-reader focus when it hides hands that focus to a
  day first.
- **Screen readers** announce the calendar by the field's name when it
  appears. Weekday labels are single letters; every day reads out its full
  date, and today adds "Today".
- **Keyboard and D-pad users** start on the picked day.
- **The days' circles and rings** (`calendarDayBackground`): a filled
  primary-blue circle for the picked day, a ring for today (so today isn't
  marked by colour alone), a rounded-square focus ring that fills the cell,
  and a round ripple. The circles are drawn in code (`DayCircle`) so they
  shrink to fit a narrow cell at large font sizes instead of being cut off
  at the sides: up to `calendar_day_circle` (36sp) across, with
  `calendar_day_circle_clearance` (3dp) to the cell's edge, and today's
  ring `calendar_today_ring_width` (2dp) wide. Their colours are in
  `docs/subsystems/theme-and-accessibility.md`.

### The clock (event time)

- **The time is required and picked from a clock:** Material's
  `MaterialTimePicker`, in the phone's 12- or 24-hour style, titled "Event
  Time". Its keyboard button switches to typing the hour and minute.
- It opens at the picked time, or at 9:00 AM (`EventTimes.DEFAULT_TIME`).
- Its own text stays English, like the app, because
  `androidResources.localeFilters` keeps the libraries' translations out;
  adding a language means adding it there (ADR-0036).
- Events saved before 2026-10-27 have no time until they're next edited,
  and can't be saved again without one.
- A second tap, or a screen reader's, while it's opening doesn't open a
  second clock. A clock that was open when the screen was recreated gets
  its listener back.
- **The event form keeps the picked time and zone in fields of its own**
  (`pickedTime` and `pickedZoneId` in `EventDetailActivity`, saved across
  rotation) and shows them as text; the two text fields don't save their
  own state.
- Material's clock marks its selected AM or PM by hue alone, so Vagor's
  theme overlay fills it with the primary blue
  (`docs/subsystems/theme-and-accessibility.md`).

### The time zone list (`TimeZonePicker.kt`)

- **The zone is picked from a searchable list** (`TimeZonePickerDialog`)
  and starts on the phone's zone for a new event, or the event's saved zone
  when it can be read.
- **Which zones are listed:** every zone that is the main ID for a place
  (one with a two-letter country or region code), plus `Etc/UTC`, plus the
  event's own zone when the list leaves it out (an older name, for
  example), sorted by label in the language's own order. ICU can list these
  itself (`SystemTimeZoneType.CANONICAL_LOCATION`) only from Android 9, so
  the list is built from every ID and its canonical ID.
- **Each row** has a label and a description:
  - label: the zone's main city and country, such as "Tokyo, Japan"; the
    city alone when the country has the same name (the Pitcairn Islands);
    otherwise its long name, or its ID
  - description: its long name and today's offset from GMT, such as "Japan
    Standard Time (GMT+09:00)", or the offset alone
- **Search** finds zones whose label, name or ID contains every word typed,
  ignoring case and accents ("sao paulo" finds São Paulo). Each search
  starts at its first match, rather than keeping the scroll that put the
  event's zone in view. The hint is "Search by city, country or name", and
  no match shows "No matching time zones".
- **It opens with the event's zone in view.** That zone is marked with a
  tick, not by colour alone, and screen readers read "Selected" for it.
- **The result** comes back through the fragment result `REQUEST_KEY`
  (`RESULT_ZONE_ID`), which survives the screen being recreated. It is a
  `DialogFragment` (ADR-0033), and a second tap while it opens is ignored.
- **It keeps its size and place while a search narrows the list**
  (`FullHeightLinearLayout` takes all the height the dialog offers), so rows
  don't move out from under a tap. The list shrinks above the keyboard, and
  the keyboard waits for a tap on the search field.
- **The event form's field** shows the zone's name and city, such as
  "Japan Standard Time (Tokyo)" (`fieldText`), so it isn't read as the
  event's place. The field always has a zone, so its "Time Zone" hint
  never shows.
- `TimeZoneChoices.label` gives the label alone, such as "Tokyo, Japan",
  for shared text and calendar descriptions.
- `sameZone` treats two IDs for one zone, such as `Asia/Calcutta` and
  `Asia/Kolkata`, as the same.

### Event start times and order (`EventTimes.kt`)

- `startOf` is the event's date and time in its own zone, or in the
  fallback zone when it has none or an unknown one; null without a valid
  date and time. A time that a daylight saving change skips moves forward.
- `ORDER` sorts by date (unreadable dates last), then time of day (events
  saved before events had a time first on their date), then title,
  ignoring case. Each event's own time is used, whatever its zone. The
  vacation page lists its events in this order.

## Invariants

- Every date Vagor writes is a strict `MM/dd/yyyy` date from
  `DateValidators.formatDate`; every time it writes is `HH:mm` from
  `formatTime`.
- A stored date string is never shown to a person; it goes through
  `dateText`.
- The calendar and the stored dates are Gregorian on every phone, whatever
  its calendar system.
- A pick-only field's text is never typed by the user.
- Every event saved since database version 2 has a time and a time zone.

## Limits

| Limit | Value | Notes |
| :- | :- | :- |
| Dates the calendar offers | 0001-01-01 to 9999-12-31 | The years `MM/dd/yyyy` can hold |
| Calendar cells | 42 (six weeks) | Fixed, so the height stays steady |
| Day height | At least 48dp at any font size | 48sp, never below 48dp |
| Day width | At least 48dp where the screen allows | A screen narrower than 360dp gives narrower days |
| Calendar width | The field's width, up to 412dp | Wider than the field where seven 48dp days need it, up to the screen less 8dp on each side |
| A new event's clock opens at | 9:00 AM | `EventTimes.DEFAULT_TIME` |

## Tests

- **JVM** (`app/src/test/java/com/example/vagor/`):
  - `DateValidatorsTest`: the strict date pattern, loose dates refused,
    leap days, comparisons refusing loose dates, an end date not before the
    start, an event inside its vacation, `isOutsideVacation` only for a
    readable date outside, parsing and formatting, two-digit times, loose
    times refused, zone IDs.
  - `CalendarMonthTest`: a month starts under its weekday, no leading
    blanks when it starts on the first weekday, six weeks when needed, the
    Gregorian leap year rule, weekday order, the offered range fits the
    stored format.
  - `EventTimesTest`: start in the event's zone, the fallback zone, no
    start without a valid date and time, a skipped time moves forward,
    order by date, then time, then title.
- **Instrumented** (`app/src/androidTest/java/com/example/vagor/`):
  - `CalendarDropdownInstrumentedTest`: tapping a day fills the field;
    Back closes the calendar without changing the date; the month buttons;
    the end date calendar starts at the start date; an empty end date opens
    at a past start date; an event's calendar offers only its vacation's
    days; days at least 48dp each way where the screen allows (CI's
    emulator is 320dp wide); today is marked for screen readers; today is a
    ring and the picked day a filled circle; the calendar stays Gregorian
    in a Thai locale; dates are shown in US and British English formats; a
    picked date survives rotation; the field reads as a drop-down list that
    can't be typed into. It writes to the app's real `vacation_database`
    and deletes only the rows it creates, in `@After`, and it switches the
    app's language to Thai, US English and British English in two tests
    and back.
  - `EventFormInstrumentedTest`: saving needs a time and keeps the
    details; the zone is picked from a searchable list; an event opens with
    its zone and details; rotation keeps the picked time, the picked zone
    and an open clock; the time and zone fields read as drop-down lists
    that can't be typed into; the vacation page shows each event's time and
    details in time order.
- Instrumented tests run only on an emulator or a test device, never on a
  phone that holds real trips (`docs/04-quality.md`,
  `.claude/rules/instrumented-tests.md`).

## Open questions

None recorded.
