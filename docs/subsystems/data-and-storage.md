# Subsystem: Data and storage

Everything Vagor keeps on the phone, and how it is kept: the Room database
`vacation_database` with its two tables and their DAOs, the migrations and
the schemas Room exports, the stored formats of dates, times and time
zones, the SharedPreferences file `settings` that holds the dark mode
switch, and the backup rules that keep all of it off cloud backup. Nothing
here talks to a network (ADR-0001), and the database is the user's only
copy of their trips (ADR-0003). Built: database version 2. M0 ported the
Room database from the original app, and M2 added version 2
(`docs/roadmap.md`).

Files, under `app/src/main/java/com/example/vagor/` and `app/src/main/res/`:
`entities/Vacation.kt`, `entities/Event.kt`, `dao/VacationDAO.kt`,
`dao/EventDAO.kt`, `database/AppDatabase.kt`, `DateValidators.kt`,
`ThemeSettings.kt`, `xml/backup_rules.xml`, `xml/data_extraction_rules.xml`
and the backup attributes in `AndroidManifest.xml`; and, outside `src/`,
`app/schemas/com.example.vagor.database.AppDatabase/`. Path-scoped rules:
`.claude/rules/database.md`. Skill: `/change-database-schema`.

## Contents

| File | Provides |
| :- | :- |
| `entities/Vacation.kt` | `Vacation`, the `vacations` table |
| `entities/Event.kt` | `Event`, the `excursions` table |
| `dao/VacationDAO.kt` | Insert, update, delete; every vacation; one vacation by id |
| `dao/EventDAO.kt` | Insert, update, delete; every event; a vacation's events; one event by id; a vacation's event count |
| `database/AppDatabase.kt` | `AppDatabase`: the version, the file name `NAME`, `MIGRATION_1_2`, `MIGRATIONS` and `open()` |
| `DateValidators.kt` | Strict parsing and formatting of the stored date, time and zone strings, and the date-range checks |
| `ThemeSettings.kt` | The dark mode switch in the SharedPreferences file `settings` (`docs/subsystems/settings-screen.md`) |
| `app/schemas/com.example.vagor.database.AppDatabase/1.json`, `2.json` | The schema Room exported for each version, kept in git |
| `res/xml/backup_rules.xml` | Backup exclusions for Android 11 and earlier |
| `res/xml/data_extraction_rules.xml` | Cloud backup exclusions for Android 12 and later |

## What is stored

| Store | Holds | Notes |
| :- | :- | :- |
| `vacation_database` (Room over SQLite) | Every vacation and event | All user data. There is no other copy |
| SharedPreferences file `settings` | `dark_mode`, a Boolean, `true` when missing | A UI preference, not trip data, so it is not in the database |

Nothing else is stored:

- **Alarms are not data.** Every reminder is rebuilt from the database
  (`docs/subsystems/reminders.md`). Reminders are automatic for every item
  (ADR-0005); opt-in reminders would have to be saved in the database,
  which is a schema change.
- **Once-per-launch flags** live in memory and reset with the process:
  `VacationsFragment`'s notifications check, `ExactAlarmOfferDialog.offered`
  and `HibernationOfferDialog.offered`.
- **Screen state across a rotation** goes in saved instance state, not in
  storage: a `DateField`'s `date`, the event form's picked time and zone
  (`pickedTime`, `pickedTimeZone`), and the vacation page's focused event
  row (`focusedEventId`).

PRIVACY.md tells users the same: their trips and dark mode setting "are
stored only on your device, in Vagor's private app storage". LICENSE
section 5 says it too. Keep both true
(`.claude/rules/manifest-privacy-and-legal.md`).

## Tables (version 2)

From `app/schemas/com.example.vagor.database.AppDatabase/2.json`.

**`vacations`** (`Vacation`):

| Column | SQL | Field | Holds |
| :- | :- | :- | :- |
| `title` | `TEXT NOT NULL` | `title` | Required |
| `hotel` | `TEXT NOT NULL` | `details` | Trip details: free text over several lines, optional, blank when empty. Replaced the hotel field on 2026-10-26, asked for by the user, and kept the column's old name (ADR-0012) |
| `startDate` | `TEXT NOT NULL` | `startDate` | `MM/dd/yyyy` |
| `endDate` | `TEXT NOT NULL` | `endDate` | `MM/dd/yyyy`, on or after `startDate` |
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL` | `id` | Generated |

**`excursions`** (`Event`):

| Column | SQL | Field | Holds |
| :- | :- | :- | :- |
| `title` | `TEXT NOT NULL` | `title` | Required |
| `date` | `TEXT NOT NULL` | `date` | `MM/dd/yyyy`, within its vacation's dates |
| `vacationId` | `INTEGER NOT NULL` | `vacationId` | The vacation's `id`. No foreign key |
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL` | `id` | Generated |
| `details` | `TEXT NOT NULL DEFAULT ''` | `details` | Optional, blank when empty (version 2) |
| `time` | `TEXT` | `time` | `HH:mm`, 24-hour; null on events saved before version 2 |
| `timeZone` | `TEXT` | `timeZone` | An IANA zone ID such as `Asia/Tokyo`; null on events saved before version 2 |

- Events were called excursions until 2026-10-25. The UI and code now say
  events, but the table keeps its old name `excursions`, so renaming the
  class needed no migration (ADR-0010).
- `Event.details` is blank when empty. `Event.time` and `Event.timeZone`
  are null on events saved before database version 2, and every save
  since stores both. Such an event has no time until it is next edited,
  and can't be saved again without one (ADR-0013).

## Versions and migrations

| Version | Date | Change | Migration |
| :- | :- | :- | :- |
| 1 | Not recorded | `vacations` and `excursions` as ported, with the columns `title`, `hotel`, `startDate`, `endDate`, `id` and `title`, `date`, `vacationId`, `id` | None (the first version) |
| 2 | 2026-10-27 | The event columns `details`, `time` and `timeZone` | `MIGRATION_1_2` |

`MIGRATION_1_2` runs three statements, so existing events get blank details
and no time:

```sql
ALTER TABLE `excursions` ADD COLUMN `details` TEXT NOT NULL DEFAULT ''
ALTER TABLE `excursions` ADD COLUMN `time` TEXT
ALTER TABLE `excursions` ADD COLUMN `timeZone` TEXT
```

How the schemas are kept:

- `@Database(..., exportSchema = true)` and
  `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` in
  `app/build.gradle.kts` make Room (2.8.5, through KSP) write each
  version's schema to `app/schemas`. The files are in git, so every
  migration can be tested against the version it upgrades from.
- `sourceSets { getByName("androidTest").assets.srcDir("$projectDir/schemas") }`
  hands them to `MigrationTestHelper` as test assets.

## Stored formats

| Value | Stored as | Read with | Written with |
| :- | :- | :- | :- |
| Date | `MM/dd/yyyy`, such as `07/04/2031` | `DateValidators.parseLocalDate`, `isValidDate` | `DateValidators.formatDate`, through the drop-down calendar |
| Time | `HH:mm`, 24-hour, such as `14:30` | `DateValidators.parseLocalTime` | `DateValidators.formatTime` |
| Time zone | An IANA zone ID, such as `Asia/Tokyo` | `DateValidators.parseZone` | The zone ID the event form picked |

- **Strict dates.** Exactly two-digit month and day, four-digit year, a
  real calendar date, nothing before or after: a regular expression checks
  the shape, then `java.time` parses `MM/dd/uuuu` in `Locale.US` with
  `ResolverStyle.STRICT`.
- **Strict times.** Exactly two-digit hour (`00` to `23`) and minute, the
  same way.
- **Zones.** `parseZone` returns null for a missing or unknown ID.
- **Old loose dates.** Rows saved before dates were checked strictly may
  hold dates these parsers refuse. The save-time checks still run on every
  save; the dashboard gives such a vacation the status "Check the dates";
  `dateText` shows such a date as it was stored; and
  `DateValidators.isOutsideVacation` can't check such an event, so it
  doesn't block an edited vacation's save.
- **Shown, never raw.** What a person reads goes through `dateText` and
  ICU, never the stored string (ADR-0026,
  `docs/subsystems/dates-times-and-pickers.md`).

## Backup and device transfer

Cloud backup is off, decided with the user on 2026-10-25 (ADR-0004). A
direct device-to-device transfer to a new phone is still allowed.

| Where | Setting | Effect |
| :- | :- | :- |
| `AndroidManifest.xml` | `android:allowBackup="false"` | Turns backup off |
| `AndroidManifest.xml` | `android:fullBackupContent="@xml/backup_rules"` | Android 11 and earlier. `allowBackup="false"` already turns backup off there; the exclusions keep trip data out of backups if that flag is ever changed |
| `AndroidManifest.xml` | `android:dataExtractionRules="@xml/data_extraction_rules"` | Android 12 and later. `<cloud-backup>` excludes trip data from Google Drive. There is no `<device-transfer>` section, so a direct device-to-device transfer can still copy it |

Both rules files exclude the same five domains: `root`, `file`, `database`,
`sharedpref` and `external`.

What follows, and what LICENSE section 5.2 and PRIVACY.md's Backup section
tell users: a lost or replaced phone, a deleted app or cleared storage, or
a deleted vacation or event can't be recovered, except from a copy Android
made in a direct device-to-device transfer.

## Key types

```kotlin
@Entity(tableName = "vacations")
data class Vacation(
    var title: String,
    @ColumnInfo(name = "hotel")
    var details: String,
    var startDate: String,
    var endDate: String,
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
)

@Entity(tableName = "excursions")
data class Event(
    var title: String,
    var date: String,
    var vacationId: Int,
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(defaultValue = "")
    var details: String = "",
    var time: String? = null,
    var timeZone: String? = null
)

@Dao
interface VacationDAO {
    @Insert fun insert(vacation: Vacation): Long
    @Update fun update(vacation: Vacation)
    @Delete fun delete(vacation: Vacation)
    @Query("SELECT * FROM vacations") fun getAllVacations(): List<Vacation>
    @Query("SELECT * FROM vacations WHERE id = :vacationId") fun getVacationById(vacationId: Int): Vacation?
}

@Dao
interface EventDAO {
    @Insert fun insert(event: Event): Long
    @Update fun update(event: Event)
    @Delete fun delete(event: Event)
    @Query("SELECT * FROM excursions") fun getAllEvents(): List<Event>
    @Query("SELECT * FROM excursions WHERE vacationId = :vacationId")
    fun getEventsForVacation(vacationId: Int): List<Event>
    @Query("SELECT * FROM excursions WHERE id = :eventId") fun getEventById(eventId: Int): Event?
    @Query("SELECT COUNT(*) FROM excursions WHERE vacationId = :vacationId")
    fun getEventCountForVacation(vacationId: Int): Int
}

@Database(entities = [Vacation::class, Event::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vacationDAO(): VacationDAO
    abstract fun eventDAO(): EventDAO

    companion object {
        const val NAME = "vacation_database"
        val MIGRATION_1_2 = object : Migration(1, 2) { /* the three ALTER TABLEs above */ }
        val MIGRATIONS = arrayOf(MIGRATION_1_2)

        fun open(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .allowMainThreadQueries()
                .build()
    }
}

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

object ThemeSettings {
    fun isDarkMode(context: Context): Boolean          // "settings" / "dark_mode", default true
    fun setDarkMode(context: Context, enabled: Boolean)
    fun applySaved(context: Context)
}
```

## Who opens the database

| Opened by | Opens in | Closes in | Uses |
| :- | :- | :- | :- |
| `VacationsFragment` | `onCreate` | `onDestroy` | `getAllVacations` |
| `EventListActivity` | `onCreate` | `onDestroy` | `getVacationById`, `getEventsForVacation` |
| `VacationDetailActivity` | `onCreate` | `onDestroy` | Insert, update, delete; `getEventsForVacation`, `getEventCountForVacation` |
| `EventDetailActivity` | `onCreate` | `onDestroy` | Insert, update, delete; `getEventById`, `getVacationById` |
| `MyReceiver` | `onReceive` | `finally` | Reads the vacation or event a reminder is for |
| `ReminderRescheduleReceiver` | `onReceive` (`TIME_SET`, `TIMEZONE_CHANGED`) | `finally` | `ReminderScheduler.rescheduleAll` |
| `ReminderScheduler.rebuildIfNeeded` | When a rebuild is needed | `finally` | `ReminderScheduler.rescheduleAll` |

`SettingsFragment` and `HomeActivity` open no database.

## Rules

- **One file, no other copy.** All user data lives in the Room database
  file `vacation_database` (`AppDatabase.NAME`). Never rename that file,
  never use `fallbackToDestructiveMigration`, and never clear tables
  outside tests.
- **Every open goes through `AppDatabase.open()`**, which adds every
  migration. A `Room.databaseBuilder` without them would crash on an older
  database. The one hand-built instance is in `MigrationInstrumentedTest`,
  on its own file, with `.addMigrations(*AppDatabase.MIGRATIONS)`.
- **A schema change** bumps `AppDatabase.version`, adds a `Migration` to
  `AppDatabase.MIGRATIONS`, keeps the schema JSON Room exports to
  `app/schemas` (it's in git), and tests the upgrade in
  `MigrationInstrumentedTest` with `MigrationTestHelper`. Use
  `/change-database-schema`. Don't change the schema unless the task calls
  for it.
- **Old names stay.** `Event` maps to the table `excursions`
  (`@Entity(tableName = "excursions")`), and `EventDAO` queries that table;
  `Vacation.details` maps to the old column `hotel`
  (`@ColumnInfo(name = "hotel")`). Don't rename a table or column without
  a migration.
- **One instance per component.** Every Activity, Fragment or receiver
  that reads or writes data opens its own Room instance with
  `AppDatabase.open()` (main-thread queries allowed) and closes it again:
  screens in `onDestroy`, receivers in `finally`. DAOs are synchronous.
  Follow that pattern in small changes. A shared instance or background
  threading is a refactor: propose it first (ADR-0002).
- **Formats only through `DateValidators`.** Parse and validate stored
  dates, times and zones only there. Put new date or reminder math in
  plain Kotlin beside it, so it can be unit tested (`java.time` is
  available at minSdk 26).
- **Forms store trimmed text.** Both detail forms trim every field before
  checking and saving it.
- **The settings file holds only the dark mode switch.** The only stored
  state besides the database is `dark_mode` in the SharedPreferences file
  `settings`.
- **Backup stays off.** Don't change `allowBackup`, `backup_rules.xml` or
  `data_extraction_rules.xml` unless the task calls for it (ADR-0004).
- **Data leaves the app** only when the user shares it through the
  Android Sharesheet, or hands one vacation or event to a calendar app with
  Add to Calendar (`docs/subsystems/sharing-and-calendar.md`).

## Invariants

- `AppDatabase.MIGRATIONS` upgrades every earlier version to the current
  one; today that is `MIGRATION_1_2`.
- `app/schemas` holds one JSON file per version, `1.json` and `2.json`.
- `excursions.vacationId` has no foreign key, so the database doesn't stop
  an event from pointing at a vacation that is gone. `VacationDetailActivity`
  enforces it instead: a vacation that has events can't be deleted, and an
  edited vacation's new dates must still hold its events (ADR-0028;
  `docs/subsystems/screens-and-navigation.md`).
- Every event saved since version 2 has a `time` and a `timeZone`; the
  event form always has a zone, starting on the phone's.
- The application ID is `io.github.jabbott_iii.vagor` (ADR-0020). Builds
  from before it changed used `com.example.vagor`; they are a separate app
  on the phone, and their data doesn't carry over.

## Limits

| Limit | Value | Notes |
| :- | :- | :- |
| Database version | 2 | Bumped only with a migration |
| Year | Four digits | The date shape requires `\d{4}` |
| Text length | None set | No layout sets `maxLength` |
| Database instances | One per open screen or running receiver | Opened and closed by each |

## Tests

- `MigrationInstrumentedTest.version1To2KeepsEveryVacationAndEvent`
  creates a version 1 database from `1.json` in a file of its own
  (`migration-test`), inserts a vacation and an event, upgrades it with
  `runMigrationsAndValidate` against the exported version 2 schema, then
  opens it as `AppDatabase.open()` does. The vacation is unchanged and the
  event has blank details and no time or zone. It deletes the file in
  `@After` and never opens `vacation_database`.
- `DateValidatorsTest` (JVM): the stored pattern; loose dates refused;
  leap days; comparisons refuse loose dates; the end date not before the
  start; the event date inside the vacation; `isOutsideVacation` only for a
  readable date outside; parsing and formatting dates; times as two-digit
  hour and minute; loose times refused; zones as zone IDs.
- `SettingsInstrumentedTest.darkModeIsOnByDefault` removes `dark_mode` and
  checks that dark mode reads as on.
- `HomeFlowInstrumentedTest`, `ReminderInstrumentedTest`,
  `CalendarDropdownInstrumentedTest`, `EventFormInstrumentedTest` and
  `CalendarInsertInstrumentedTest` write to the app's real
  `vacation_database`. They delete only the rows they create, in `@After`,
  so a run that is killed partway can leave a test trip behind. Run
  instrumented tests only on an emulator or a test device, never on a phone
  that holds real trips; a `connectedDebugAndroidTest` run also uninstalls
  the app afterwards, which deletes its data (`docs/04-quality.md`,
  `.claude/rules/instrumented-tests.md`).

## Open questions

- A shared database instance or background threading: a refactor to
  propose first (`docs/roadmap.md`, M7).
- Importing a trip someone shared would write into this database; whether
  Vagor should is still open (ADR-0009).
