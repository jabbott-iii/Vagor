---
paths:
  - "app/src/*/java/com/example/vagor/Date*.kt"
  - "app/src/*/java/com/example/vagor/Calendar*.kt"
  - "app/src/*/java/com/example/vagor/EventTimes*.kt"
  - "app/src/main/java/com/example/vagor/PickOnlyField.kt"
  - "app/src/main/java/com/example/vagor/TimeZonePicker.kt"
  - "app/src/main/java/com/example/vagor/TripText.kt"
---

# Rules for dates, times, time zones and the pickers

Dates and times are stored one way and shown another. Mixing the two up
shows a Thai user the wrong year or writes a value the app can't read back.
The spec is `docs/subsystems/dates-times-and-pickers.md`.

- **Stored values** (parse and validate only through `DateValidators`):
  - dates: `MM/dd/yyyy` strings, strict: exactly two-digit month and day,
    four-digit year, a real calendar date, nothing before or after. Write
    them with `DateValidators.formatDate`.
  - event times: 24-hour `HH:mm` strings, through
    `DateValidators.parseLocalTime` and `formatTime`
  - time zones: IANA zone IDs such as `Asia/Tokyo`, through
    `DateValidators.parseZone`
  - `java.time` formatting is for stored values only.
  - Saved rows may hold older loose dates, so the save-time checks still
    run. An event with an old loose date can't be checked against its
    vacation and doesn't block the vacation's save (added 2026-10-31,
    ADR-0028).
- **Shown values** (asked for by the user on 2026-10-31, ADR-0026):
  - Dates are shown the way the phone writes them: "Jul 6, 2031" in the US,
    "6 Jul 2031" in the UK. `dateText` in `TripText.kt` formats a stored
    date with ICU's `yMMMd` skeleton, always in the Gregorian calendar (a
    Thai phone would otherwise show the Buddhist-era year), and shows a date
    it can't read as it was stored.
  - Everything a person reads goes through it: screens, reminders, shared
    text and calendar descriptions. **Never show a stored date string
    directly.**
  - Display text uses ICU (`android.icu`), which knows the phone's own names
    and patterns. Check each ICU call's API level against minSdk 26: some,
    such as `SystemTimeZoneType.CANONICAL_LOCATION`, arrived later.
- **Fields are filled only from a picker, never typed (ADR-0008):**
  - Every date field opens a month calendar that drops down below it
    (`attachCalendarDropdown()` in `CalendarDropdown.kt`), which writes
    through `DateValidators.formatDate`. The end date calendar starts at the
    start date; an event's calendar offers only its vacation's days.
  - An event's time comes from Material's clock (`MaterialTimePicker`, in the
    phone's 12- or 24-hour style), and its zone from `TimeZonePickerDialog`,
    a searchable list that starts on the phone's zone.
  - All three use `makePickOnly()` (`PickOnlyField.kt`).
- **The calendar is Vagor's own.** Don't switch to the framework
  `CalendarView`: it follows the phone's calendar system (a Thai phone would
  store Buddhist-era years) and marks today by colour alone. Vagor's calendar
  is always Gregorian and marks today with a ring.
- **Calendar sizing:** the days are at least 48dp tall at any font size.
  Where the field is too narrow for seven 48dp days (a 360dp-wide phone), the
  calendar is wider than its field, up to the screen's width less 8dp on each
  side; on narrower screens the days are as wide as that allows. Each day has
  a focus ring (`CalendarDayBackground.kt`).
- **`DateField`:** `date` holds the stored value, and the text is only for
  show, so read and write `date`, never the text. A `DateField` keeps its
  date across rotation itself. Instrumented tests set it with `setDate` and
  check it with `withDate` (`DateFieldTesting.kt`).
- **The event form** keeps the picked time and zone in fields of its own
  (saved across rotation) and shows them as text: the time in the phone's 12-
  or 24-hour style, the zone by name and city ("Japan Standard Time
  (Tokyo)", so it isn't read as the event's place).
- **Events and their times (ADR-0013):**
  - Each event is on a date inside its vacation's range, inclusive, at a
    required time in a time zone.
  - Events saved before 2026-10-27 have no time until they're next edited,
    and can't be saved again without one.
  - `EventTimes.startOf` gives an event's start in its own zone, or in the
    fallback zone it's given when the event has none or an unknown one.
- **Add to Calendar's times (`CalendarTimes` in `CalendarInsert.kt`):**
  - Calendar apps read an all-day intent's times differently (in the phone's
    zone or in UTC; an end at midnight as exclusive or not), and Google
    Calendar documents none of it.
  - `CalendarTimes` starts a vacation at the later of its first day's UTC and
    local midnights, and ends it inside its last day: an hour after that
    day's start, or 1:00 on the phone's clock, whichever is later. The clock
    time matters where clocks go back from 1:00 to midnight, and the hour
    where they skip from midnight to 1:00.
  - Every app checked (Android's original calendar, Etar, Fossify) reads that
    correctly. Google Calendar's reading couldn't be checked without a
    signed-in account.
  - An event goes in at its time in its time zone, an hour long. Calendar
    apps handle `Events.EVENT_TIMEZONE` unevenly, so an event's description
    names its time and zone too.
  - The rest of Add to Calendar is in
    `.claude/rules/manifest-privacy-and-legal.md`.
- **New date or reminder math goes in plain Kotlin** beside the existing
  code (`Dashboard`, `EventTimes`, `CalendarMonth`, `CalendarTimes`,
  `ReminderTimes`), so it can be unit tested in `app/src/test`. `java.time`
  is available at minSdk 26.
- **Before reporting done:**
  - run `./gradlew testDebugUnitTest` (`DateValidatorsTest`,
    `CalendarMonthTest`, `CalendarTimesTest` and `EventTimesTest` cover this
    code)
  - `CalendarDropdownInstrumentedTest` switches the app's language to Thai,
    US English and British English and back; run it only on an emulator,
    after asking (`.claude/rules/instrumented-tests.md`)
  - how another phone's calendar system, locale or calendar app behaves can
    only be checked on that phone: say what wasn't verified
