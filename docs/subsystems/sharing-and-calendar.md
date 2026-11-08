# Subsystem: Sharing and Add to Calendar

The only two ways a trip leaves Vagor, and both start with the user's tap:
plain text through the Android Sharesheet, and one vacation or event handed
to a calendar app's own new-event screen. The Share buttons are in
`EventListActivity` (Share Vacation) and `EventDetailActivity` (Share
Event); Add to Calendar is `CalendarInsert.kt` (`CalendarTimes` and the
intents), called from the same two screens; the text comes from
`res/values/strings.xml`. Everything here is built. It covers product
requirement 4 in `docs/00-overview.md`, and ADR-0001, ADR-0009 and
ADR-0015. Path-scoped rules: `.claude/rules/manifest-privacy-and-legal.md`
(what the privacy policy says about both),
`.claude/rules/dates-and-times.md` (`CalendarTimes`) and
`.claude/rules/ui-and-accessibility.md`.

## Contents

| File | Provides |
| :- | :- |
| `EventListActivity.kt` | The vacation page's Share Vacation and Add to Calendar buttons, and the vacation's shared text |
| `EventDetailActivity.kt` | The event form's Share Event and Add to Calendar buttons, and the event's shared text |
| `CalendarInsert.kt` | `CalendarTimes` (plain-Kotlin all-day and event times), `vacationCalendarIntent`, `eventCalendarIntent`, `openCalendarInsert` |
| `TripText.kt`, `TimeZonePicker.kt` | The dates, times and zone labels the text uses (`docs/subsystems/dates-times-and-pickers.md`) |
| `res/values/strings.xml` | Every line of shared text and calendar description |

## Key types

```kotlin
object CalendarTimes {
    val EVENT_LENGTH: Duration = Duration.ofHours(1)
    fun allDayMillis(date: LocalDate, phoneZone: ZoneId): Long
    fun allDayEndMillis(date: LocalDate, phoneZone: ZoneId): Long
    fun vacationSpanMillis(vacation: Vacation, phoneZone: ZoneId): Pair<Long, Long>?
}

fun Context.vacationCalendarIntent(vacation: Vacation, events: List<Event>): Intent?
fun Context.eventCalendarIntent(event: Event, vacation: Vacation): Intent?
fun FragmentActivity.openCalendarInsert(insert: Intent)

// When a calendar app was last opened (elapsed realtime); tests reset it.
internal var calendarOpenedAt = Long.MIN_VALUE / 2
```

## Sharing

- **Decided with the user on 2026-10-25: vacations and events are shared
  as plain text** (ADR-0009). Importing a shared trip isn't supported;
  whether Vagor should be able to import a trip someone shared is still
  open.
- **Share only through the Sharesheet:** `Intent.ACTION_SEND` (type
  `text/plain`, the text in `Intent.EXTRA_TEXT`) inside
  `Intent.createChooser`, as `EventListActivity` does, with text built from
  string resources such as `vacation_share_template`. No links to servers
  and no accounts. The choosers are titled "Share vacation details" and
  "Share event details".
- **A vacation's text** lists its title, dates and trip details, then its
  events with their dates and times, in date and time order
  (`EventTimes.ORDER`):

  ```text
  Vacation Title: <title>
  Start Date: <start date>
  End Date: <end date>
  Trip Details: <details>          (only when it has them)

  Events:                          (only when it has events)
  - <event title> (<date>, <time and zone>)
  ```

  The strings are `vacation_share_template`, `vacation_share_details`,
  `vacation_share_with_events` and `event_share_line`; each event's date
  and time come from `eventWhenText`, such as "Jul 6, 2031, 2:30 PM JST".
- **An event's text** has these lines in this order; Time and Details only
  when it has them (`event_share_title`, `event_share_date`,
  `event_share_time`, `event_share_vacation`, `event_share_details`):

  ```text
  Event: <title>
  Date: <date>
  Time: <time and zone abbreviation> (<city, country>)
  Vacation: <vacation title>
  Details: <details>
  ```

  It takes what the form shows, saved or not. Without a valid date there's
  no zone abbreviation, but the time still goes in.
- **To share a file** (an `.ics` file, for example), serve it through a
  `FileProvider` `content://` URI with `FLAG_GRANT_READ_URI_PERMISSION`.
  Never use a `file://` URI. Vagor shares no file today, and the manifest
  declares no `FileProvider`.

## Add to Calendar

- **Asked for by the user on 2026-10-26** (ADR-0015): an Add to Calendar
  button on the vacation page and the event form opens Google Calendar's
  own new-event screen (another calendar app's when Google Calendar isn't
  installed), filled in.
- **The label says "Calendar"** because it may open another app, and
  Google's brand guidance asks for accurate wording.
- **The intent:** `Intent.ACTION_INSERT` with
  `CalendarContract.Events.CONTENT_URI` and the type
  `vnd.android.cursor.dir/event` (given so the intent doesn't depend on
  asking the calendar provider for it), with `Events.TITLE`,
  `Events.DESCRIPTION`, `EXTRA_EVENT_BEGIN_TIME` and
  `EXTRA_EVENT_END_TIME`.
- **A vacation** goes in all day (`EXTRA_EVENT_ALL_DAY`) over its dates,
  with its trip details and events as the description (the details, a
  blank line, then "Events:" and one `event_share_line` per event). No time
  zone goes with it: an all-day item is read in the calendar app's own. A
  stored date that can't be read gives "Edit the vacation and choose its
  dates again" instead.
- **An event** goes in at its time in its own time zone
  (`Events.EVENT_TIMEZONE`), an hour long (`CalendarTimes.EVENT_LENGTH`;
  events have a start but no length, and the user can change it there),
  with its details, time and vacation as the description: the details and
  a blank line when it has them, then "Time: <time and zone> (<city,
  country>)" and "Part of <vacation>". Calendar apps handle
  `Events.EVENT_TIMEZONE` unevenly, so the description names its time and
  zone too.
- **The event form's button takes what the form shows, saved or not,** like
  Share, so it needs a title, a date and a time; a date that can't be read
  gives "Choose the date again from the calendar".
- **Which app:** aimed at Google Calendar (`com.google.android.calendar`)
  and, when that isn't installed, at any calendar app
  (`openCalendarInsert`). Starting the intent needs no `<queries>` entry;
  catch `ActivityNotFoundException` instead.
- **No calendar app:** "No calendar app on this phone can add this.
  Install Google Calendar or another calendar app, then try again." It is a
  dialog (`MessageDialog`), because a toast is cut at two lines.
- **A second tap within 2 seconds is ignored,** because some calendar apps
  save every new-event screen the user leaves.
- **The calendar app decides whether and where to save.** Add to Calendar
  opens the calendar app's own new-event screen; Android's original
  calendar app saves when the user leaves the screen, so never promise that
  nothing is added until the user saves.
- **It needs no permission:** never add `READ_CALENDAR` or
  `WRITE_CALENDAR`, and never write to the calendar provider directly.
  Vagor never reads or writes the calendar itself.

### All-day times (`CalendarTimes`)

- **Calendar apps read an all-day intent's times differently:** in the
  phone's zone or in UTC, and an end at midnight as exclusive or not
  (Android's original calendar and Etar read a midnight end as the day
  before; Fossify as that day). Google Calendar documents none of it.
- **The start** is the later of the first day's UTC and local midnights,
  which is on that date in both readings (`allDayMillis`).
- **The end** is inside the last day: an hour after that day's start, or
  1:00 on the phone's clock, whichever is later (`allDayEndMillis`). The
  clock time matters where clocks go back from 1:00 to midnight (an hour
  after midnight is midnight again), and the hour where they skip from
  midnight to 1:00 (1:00 is then the day's first moment).
- Every app checked (Android's original calendar, Etar, Fossify) reads that
  correctly; Google Calendar's reading couldn't be checked without a
  signed-in account.
- `vacationSpanMillis` is null when a stored date is invalid, and then no
  intent is sent.

## Rules

- **Data leaves the app only** when the user shares it through the Android
  Sharesheet, or hands one vacation or event to a calendar app with Add to
  Calendar (ADR-0001). No other path may send trip data anywhere.
- **Text comes from string resources,** and every date in it goes through
  `dateText` (`docs/subsystems/dates-times-and-pickers.md`); never a stored
  date string.
- **Keep what users are told true.** PRIVACY.md and `privacy_policy_text`
  describe both buttons (its Sharing section), and `LICENSE` section 5 says
  Vagor gives data only to the app the user shares it with or to their
  calendar app, whose own terms then apply. A change here checks both
  (`.claude/rules/manifest-privacy-and-legal.md`).
- **The store listing** names Google Calendar only in plain text saying
  what Vagor works with: no Google logo or Calendar icon in the graphics or
  screenshots, and no Google name in the app's title or icon. It doesn't
  claim importing trips, sync or cloud backup (Google Calendar may sync an
  item saved there, but that's Google Calendar, not Vagor). See
  `docs/05-release.md`, step 6, and `/update-store-listing`.

## Invariants

- Vagor holds no calendar permission and never queries or writes
  `CalendarContract` itself.
- Nothing is sent without a tap on Share or Add to Calendar.
- At most one calendar new-event screen opens per 2 seconds.
- A vacation's all-day span starts on its first date and ends inside its
  last date, never at a midnight, whether it is read in the phone's zone or
  in UTC (`CalendarTimesTest`).

## Limits

| Limit | Value | Notes |
| :- | :- | :- |
| An event's length in the calendar | 1 hour | The user can change it in the calendar app |
| Second tap ignored within | 2 seconds | `SECOND_TAP_MILLIS` |
| All-day end | At least an hour into the last day, and at least 1:00 on its clock | |

## Tests

- **JVM** (`CalendarTimesTest`): an all-day moment falls on its date in
  the phone's zone and in UTC; it exists when midnight is skipped
  (Santiago); a vacation ends inside its last day, including when clocks go
  back over midnight (the Azores, Havana); a one-day vacation ends on its
  day after it begins; no span for an invalid date. It runs each case in
  several zones, from `Pacific/Pago_Pago` to `Pacific/Kiritimati`.
- **Instrumented** (`CalendarInsertInstrumentedTest`): a vacation goes in
  all day over its dates with its details and events; an event goes in at
  its time in its zone for an hour; an event without a time is not sent; a
  second tap while the calendar opens is ignored; an event with an old
  loose date is not sent. It catches the calendar intents with an
  `Instrumentation.ActivityMonitor`, so no calendar app opens; it writes to
  the app's real `vacation_database` and deletes only the rows it creates,
  in `@After`. Run it only on an emulator or a test device
  (`.claude/rules/instrumented-tests.md`).
- **No test checks the shared text.** `docs/05-release.md`, step 3, checks
  sharing a vacation and Add to Calendar by hand on the emulator.

## Open questions

- **Importing a shared trip:** whether Vagor should be able to import a
  trip someone shared is still open (`docs/roadmap.md` M7).
- **Google Calendar's reading of all-day times** couldn't be checked
  without a signed-in account.
