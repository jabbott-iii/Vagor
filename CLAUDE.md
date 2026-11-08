@AGENTS.md

## Claude Code specifics

The project rules are in `AGENTS.md` (imported above), shared with any other
coding agent. This section covers only what is specific to Claude Code.

**Project skills** (in `.claude/skills`):

| Skill | Use it to… |
| :- | :- |
| `/next-task [task-id]` | Pick the next unblocked task in `docs/roadmap.md`, or the one named, and carry it through the definition of done. M6 tasks are the developer's Play Console steps: it says so and stops |
| `/record-decision <title>` | Append an ADR to `docs/decisions.md` |
| `/evaluate-dependency <library> <need>` | Run the evaluation `docs/02-dependency-policy.md` asks for before anyone proposes a new library or a version change |
| `/change-database-schema <change>` | Change the Room schema safely: bump `AppDatabase.version`, add a `Migration` to `AppDatabase.MIGRATIONS`, keep the exported schema JSON, test the upgrade in `MigrationInstrumentedTest` |
| `/update-store-listing <what changed>` | Keep `fastlane/metadata/` true to the app: the text, screenshots of invented trips retaken on an emulator, alt text, the icon and the feature graphic |

**Path-scoped rules** in `.claude/rules` load automatically when you open
matching files:

| Rule file | Applies to |
| :- | :- |
| `dependencies.md` | The Gradle build files, `gradle/`, `gradle.properties`, the R8 keep rules, the detekt baseline, `.github/workflows/`, `Makefile.txt` |
| `database.md` | `entities/`, `dao/`, `database/`, `app/schemas/`, `MigrationInstrumentedTest`, `DateValidators.kt`, `ThemeSettings.kt` |
| `manifest-privacy-and-legal.md` | `AndroidManifest.xml`, `res/xml/`, `res/raw/`, `res/values/strings.xml`, `PRIVACY.md`, `LICENSE`, `NOTICE`, `fastlane/`, the share and Add to Calendar code, `LicenseText.kt`, `ThirdPartyLicensesTest` |
| `reminders.md` | The reminder, receiver, offer, notifications-off, hibernation and application files, and their tests |
| `dates-and-times.md` | The date, time, time zone, calendar and display-text files, and their tests |
| `ui-and-accessibility.md` | Layouts, values, drawables and colours under `res/`, every activity, fragment and dialog, `EdgeToEdge.kt` |
| `instrumented-tests.md` | `app/src/androidTest/` |

**Permissions:**

- `.claude/settings.json` pre-approves the routine Gradle tasks
  (`assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`,
  `lint`, `detekt`, `ktlintCheck`, `ktlintFormat`, an unsigned
  `bundleRelease`, `tasks`) and read-only Git commands.
- It asks before `connectedDebugAndroidTest` and `connectedAndroidTest`
  (they write to the app's real database, set real alarms, and uninstall
  the app afterwards), Gradle's install and uninstall tasks, any `adb`
  install, uninstall, shell or exec-out command, and signed builds
  (`--no-configuration-cache`).
- It denies Git operations that publish or finalize changes,
  `make -f Makefile.txt tag`, `push-tag` and `release` (which tag and push),
  any `adb -d` command (a physical phone), and reading `.env` files,
  keystores (`*.jks`, `*.keystore`, `*.p12`, `*.pfx`) and `~/keys/`.
- These are guardrails, not a security boundary. The rules in `AGENTS.md`
  still apply when a command slips past a pattern.

**Working style:**

- Use the task list for any task with more than two steps. Make the last
  item "Verify and report", and run the checks that fit the change for it.
- Work in chunks. After each chunk, stop and report in the
  definition-of-done format (`AGENTS.md`), and suggest a commit for the
  user to run: `git add -A && git commit -m "<type>: <summary>"`, with a
  type as `git log` uses them (`feat`, `fix`, `docs`, `test`, `build`,
  `ci`, `style`, `chore`). If `git status` shows a change to
  `local.properties`, name the files to add instead of `-A` (`AGENTS.md`
  rule 9). The user commits and says to continue. Check `git log` before
  continuing, to see what was committed.
- When a change can only be checked on a device (a reminder arriving, a
  notification, a calendar app's new-event screen, the launch window, a
  release build's R8 output), say plainly that it wasn't verified, unless
  you checked it on an emulator in this session.
- Ask before anything that is the user's call (`AGENTS.md` rule 14), and
  before a dependency, version or schema change the task doesn't call for.
  Record what the user decides with `/record-decision`.
- Run instrumented tests, release-build checks and screenshots only on an
  emulator, not on a test device either (stricter than `AGENTS.md` rule
  4). Gradle's install task installs on every connected device unless
  `ANDROID_SERIAL` names one (`docs/05-release.md`, step 3), so check
  `adb devices` before any connected or install task. `adb -e` reaches
  only an emulator, and the settings deny `adb -d`.
- CI can only be checked by pushing, which only the user does. Say which
  workflow change still needs a run on GitHub.
