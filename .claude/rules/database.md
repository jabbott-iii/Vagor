---
paths:
  - "app/src/main/java/com/example/vagor/entities/**"
  - "app/src/main/java/com/example/vagor/dao/**"
  - "app/src/main/java/com/example/vagor/database/**"
  - "app/schemas/**"
  - "app/src/androidTest/java/com/example/vagor/MigrationInstrumentedTest.kt"
  - "app/src/main/java/com/example/vagor/DateValidators.kt"
  - "app/src/main/java/com/example/vagor/ThemeSettings.kt"
---

# Rules for the database and stored data

All user data lives in the Room database file `vacation_database`
(`AppDatabase.NAME`), and there is no other copy: cloud backup is off
(ADR-0004). A mistake here loses someone's trips for good. The spec is
`docs/subsystems/data-and-storage.md`.

- **Never, in any change (ADR-0003):**
  - rename the file `vacation_database`
  - use `fallbackToDestructiveMigration`
  - clear tables outside tests
  - rename a table or column without a migration
- **Don't change the schema unless the task explicitly calls for it.** A
  schema change goes through `/change-database-schema`:
  1. bump `AppDatabase.version`
  2. add a `Migration` to `AppDatabase.MIGRATIONS`
  3. keep the schema JSON Room exports to `app/schemas` (it's in git; never
     edit or delete an older version's file)
  4. test the upgrade in `MigrationInstrumentedTest` with
     `MigrationTestHelper`
- **Changing an entity is a schema change.** That includes a field, a
  `@ColumnInfo`, a default value, a nullability or a table name. So is adding
  an entity to `@Database(entities = ...)`.
- **Old names stay (ADR-0010, ADR-0012):**
  - The `Event` entity maps to the table `excursions`
    (`@Entity(tableName = "excursions")`), and `EventDAO` queries that
    table. Events were called excursions until 2026-10-25.
  - `Vacation.details` (trip details) maps to the old column `hotel`
    (`@ColumnInfo(name = "hotel")`).
- **What the columns hold:**
  - `Event.details` is blank when empty (`NOT NULL DEFAULT ''`).
  - `Event.time` and `Event.timeZone` are null on events saved before
    database version 2, and every save since stores both.
  - Version 2 (2026-10-27, `MIGRATION_1_2`) added the event columns
    `details`, `time` and `timeZone` (ADR-0013).
- **`excursions.vacationId` has no foreign key.** The checks that an edited
  vacation's dates still hold its events and that a vacation with events
  can't be deleted exist only in `VacationDetailActivity`; code elsewhere
  that edits or deletes a vacation has no database check behind it.
- **Opening the database:**
  - Everything opens it through `AppDatabase.open()`, which adds every
    migration. A `Room.databaseBuilder` without them would crash on an
    older database.
  - Every Activity, Fragment or receiver that reads or writes data opens its
    own instance and closes it again: screens in `onDestroy`, receivers in
    `finally`. Main-thread queries are allowed, and DAOs are synchronous.
  - Follow that pattern in small changes. A shared instance or background
    threading is a refactor: propose it to the user first.
- **Stored formats** (parse and validate only through `DateValidators`):
  - dates: `MM/dd/yyyy` strings. The check is strict: exactly two-digit
    month and day, four-digit year, a real calendar date, nothing before or
    after. Write them with `DateValidators.formatDate`.
  - event times: 24-hour `HH:mm` strings, through `parseLocalTime` and
    `formatTime`
  - time zones: IANA zone IDs such as `Asia/Tokyo`, through `parseZone`
  - Saved rows may hold older loose dates, so the save-time checks still run
    even though the forms only write valid ones.
  - `java.time` formatting is for stored values; what a person reads goes
    through `TripText.kt` (`.claude/rules/dates-and-times.md`).
- **The only other stored state** is the dark mode switch, in the
  SharedPreferences file `settings` (`ThemeSettings`, key `dark_mode`).
  Storing anything new changes what the privacy policy says Vagor stores:
  update `PRIVACY.md` and `privacy_policy_text` together
  (`.claude/rules/manifest-privacy-and-legal.md`).
- **Backup:** `allowBackup="false"`, plus exclusions in
  `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` that
  keep the root, file, database, sharedpref and external domains out of
  cloud backup. A direct device-to-device transfer to a new phone is still
  allowed (decided with the user on 2026-10-25). Don't change these unless
  the task explicitly calls for it.
- **Reminders aren't stored.** Opt-in reminders would have to be saved in the
  database, which is a schema change.
- **Tests:** the instrumented tests that write to the real
  `vacation_database` delete only the rows they create, in `@After`.
  `MigrationInstrumentedTest` upgrades a file of its own (`migration-test`)
  and deletes it. See `.claude/rules/instrumented-tests.md`.
