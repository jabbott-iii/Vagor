---
name: change-database-schema
description: Use whenever a change would alter Vagor's Room schema (an entity's fields, column names, types, defaults or nullability, a table name, or a new entity). Bumps AppDatabase.version, adds a Migration to AppDatabase.MIGRATIONS, keeps the exported schema JSON, and tests the upgrade with MigrationTestHelper.
argument-hint: "<change, e.g. 'add a notes column to vacations'>"
---

# Change the database schema

Change: `$ARGUMENTS`

The Room database file `vacation_database` holds the only copy of every
user's trips: cloud backup is off (ADR-0004) and nothing else stores them. A
schema change that isn't migrated and tested crashes the app on every phone
that has the older version, or loses trips. Follow every step (ADR-0003).

## 1. Confirm it is a schema change, and that it is wanted

- It is a schema change if Room's exported schema would change: a field
  added to, removed from or changed in `Vacation` or `Event`, a
  `@ColumnInfo` name or default, a nullability, a table name, or a new
  entity in `@Database(entities = ...)`.
- Don't change the schema unless the task explicitly calls for it. If it
  doesn't, stop and ask the user.
- Ask whether the need can be met without one.

## 2. Never

- rename the file `vacation_database` (`AppDatabase.NAME`)
- use `fallbackToDestructiveMigration`
- clear tables outside tests
- rename the table `excursions` or the column `hotel`, or any other table or
  column, without a migration
- open the database anywhere except through `AppDatabase.open()`, which adds
  every migration (a `Room.databaseBuilder` without them would crash on an
  older database)
- edit or delete an exported schema JSON in `app/schemas`

## 3. Implement

- Change the entity in `app/src/main/java/com/example/vagor/entities/`.
  Old rows need a value for every new column: a default (as
  `Event.details` has `@ColumnInfo(defaultValue = "")`), or a nullable
  column that old rows leave null (as `Event.time` and `Event.timeZone`).
- In `database/AppDatabase.kt`:
  - bump `version` in `@Database(...)` by one
  - add a `MIGRATION_<old>_<new>` (`Migration(old, new)`) with the SQL that
    turns the old tables into the new ones, and a comment giving the
    version, the date and what it adds, as `MIGRATION_1_2` has
  - add it to `MIGRATIONS`, keeping every earlier migration
- Update every place that reads or writes the changed data: the DAOs, the
  forms, the Intent extras, `TripText.kt`, the share text, `CalendarInsert.kt`
  and the reminders. Stored formats stay as `DateValidators` defines them.
- Build (`./gradlew assembleDebug`). KSP has Room export the new version's
  schema to `app/schemas/com.example.vagor.database.AppDatabase/<new>.json`.
  That file goes into git with the change; the user commits it.

## 4. Test

- In `MigrationInstrumentedTest`, add a test like
  `version1To2KeepsEveryVacationAndEvent` for the new version:
  - `helper.createDatabase(TEST_DATABASE, <old>)`, then insert rows with raw
    SQL in the old schema
  - `helper.runMigrationsAndValidate(TEST_DATABASE, <new>, true, *AppDatabase.MIGRATIONS)`,
    which checks the upgraded tables against the exported schema
  - open it as `AppDatabase.open()` does and check every row came through,
    with the expected values in the new columns
  - a phone may still hold version 1, so also check an upgrade from version
    1 through every migration
- Keep every existing migration test.
- The test uses its own file (`migration-test`) and never opens the app's
  `vacation_database`, but every connected run starts the app and uninstalls
  it afterwards. Run it only on an emulator, after asking the user
  (`.claude/rules/instrumented-tests.md`):
  `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.vagor.MigrationInstrumentedTest`
- Run `./gradlew testDebugUnitTest`, `./gradlew lint` and
  `./gradlew detekt ktlintCheck` too.

## 5. Documentation

- Record the decision with `/record-decision` (ADR-0013 records version 2).
- Update `docs/subsystems/data-and-storage.md` (the versions and columns)
  and the data model in `docs/01-architecture.md`.
- If what Vagor stores changed, update `PRIVACY.md` and
  `privacy_policy_text` together; their "Information Vagor stores" section
  lists it (`.claude/rules/manifest-privacy-and-legal.md`).

## 6. Report

- The schema change, the old and new `version`, and the migration's SQL.
- The schema JSON file added.
- Commands run and their real results, including whether
  `MigrationInstrumentedTest` ran, on which emulator.
- Checks not run, and why. If the migration test didn't run, say the
  upgrade is not verified.
