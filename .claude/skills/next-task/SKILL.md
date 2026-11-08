---
name: next-task
description: Pick the next open task from docs/roadmap.md (or the task ID given) and carry it through Vagor's definition of done, leaving changes uncommitted for the user to review and commit.
argument-hint: "[task-id, e.g. M7.1]"
disable-model-invocation: true
---

# Work the next roadmap task

Requested task: `$ARGUMENTS` (empty means "pick the next open task").

## 1. Choose the task

1. Open `docs/roadmap.md`.
2. If a task ID was given, use it. If it lists `Depends` tasks that are not
   all ticked, stop and tell the user which ones block it.
3. Otherwise, take the **first** unticked task whose `Depends`, if any, are
   all ticked. Never take an item from the "Declined (not planned)" list.
4. **A task marked `Owner: developer (an agent can't do this)`** (the M6
   Google Play steps from `docs/05-release.md`, and the repository
   housekeeping) is the developer's:
   - Say so, name the `docs/05-release.md` step it comes from, and stop.
     Don't start another task in its place.
   - Do none of it: agents never sign, upload, publish, tag or push, and
     never work in Play Console. You may answer questions about the step.
   - Name the first open task an agent could take, if there is one (a
     `the user decides` task starts with the user's answer), and wait for
     the user to choose.
5. **A task marked `Owner: the user decides`** (the M7 open decisions: the
   refund policy, trader status, importing shared trips, which reminders get
   the 500 alarm places, and so on) is the user's call (`AGENTS.md`, rule
   14):
   - Show the user what is open, the facts the docs give, and the options
     they name, in a few lines. Don't recommend an answer the sources don't
     support, and don't decide it.
   - Once the user answers, record it with `/record-decision` (status
     `Accepted`, today's date). Then, if the decision needs code, carry on
     from section 2.
6. Check the ADRs the task relies on (`docs/decisions.md`). If one is
   `Proposed`, show the user its decision in two or three lines and ask them
   to accept, change or reject it before you write code that depends on it.
7. If the task has no acceptance criteria, write down in two or three lines
   what "done" will mean, and get the user's agreement.
8. Tell the user in one or two sentences which task you're starting and how
   you plan to approach it. Then create a task list. Its last item is
   "Verify and report".

## 2. Load context, and only the context you need

- `AGENTS.md` is already loaded. Re-read its "Non-negotiable rules" if
  unsure.
- Read the docs that `AGENTS.md` → "Read before working" lists for the area
  the task touches, especially the subsystem spec in `docs/subsystems/`.
- The path-scoped rules in `.claude/rules/` load when you open matching
  files. Follow them.
- Read the existing code before writing new code. Follow its patterns and
  naming: XML views, `findViewById`, one `AppDatabase.open()` per screen,
  synchronous DAOs, plain Kotlin for date and reminder math.
- If the spec and the code disagree, or the task is unclear in a way that
  changes stored data, a permission, what data leaves the phone, or
  behaviour an ADR records as the user's decision: stop, describe the
  conflict, propose the safest option, and ask.

## 3. Implement the smallest complete change

- Do exactly what the task needs. Nothing else: no unrelated refactors,
  renames or reformatting.
- **New library, plugin or version change?** Stop and run
  `/evaluate-dependency`. Don't add it.
- **Database schema change?** Only if the task explicitly calls for it, and
  then through `/change-database-schema`.
- **Compose, ViewBinding, ViewModel, DI, coroutines, a shared database
  instance or background threading?** Stop and propose it to the user first.
- **A decision changed?** Run `/record-decision` and update the affected
  docs.
- **Reminder timing, what the user is told, or anything else the docs call
  the user's call?** Ask before changing it.
- **What the app stores or shares changed?** Update `PRIVACY.md` and
  `privacy_policy_text` together, and check `LICENSE` sections 5 and 6 still
  hold (`.claude/rules/manifest-privacy-and-legal.md`).
- **A screen in the store listing changed?** Run `/update-store-listing`.
- User-visible text goes in `res/values/strings.xml`.
- Write the tests beside the code: plain-Kotlin logic in `app/src/test`,
  screen flows in `app/src/androidTest`
  (`.claude/rules/instrumented-tests.md`).

## 4. Verify honestly

- Run what the change touches, and at least:
  - `./gradlew assembleDebug`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lint`
  - `./gradlew detekt ktlintCheck` (`./gradlew ktlintFormat` fixes most
    ktlint findings)
- R8 or release settings changed, or the code relies on reflection, class
  names or resources looked up by name: also run an unsigned
  `./gradlew bundleRelease`, and say that the release build still needs a
  check on an emulator (`docs/05-release.md`, step 3).
- Instrumented tests: only on an emulator (check `adb devices` first), and
  only after the user says yes, since a run writes to the app's real
  database, sets real alarms and uninstalls the app afterwards
  (`.claude/rules/instrumented-tests.md`).
- Fix failures by fixing the code. **Never** add findings to
  `app/detekt-baseline.xml`, weaken lint, detekt or ktlint, delete or
  `@Ignore` a test, or loosen an assertion to make a check pass, unless the
  user explicitly agrees.
- Checks that need a real device, another Android version, another
  calendar app, TalkBack, or time to pass (Doze, standby buckets,
  hibernation):
  1. Do everything possible here.
  2. Leave the box unticked.
  3. Add an indented `⏳ needs <device or tool>: <exact remaining check>`
     line under the task in the roadmap.
- CI can only be checked by pushing, which the user does. Never report a
  workflow change as passing until a run on GitHub shows it.

## 5. Update the roadmap and docs

- Tick the box **only** if the task is done and every check it needs ran
  and passed in this session.
- Add brief indented `Note:` lines under the task where useful: what you
  measured, conventions chosen, follow-ups.
- Update docs only where behaviour or design actually changed: the
  subsystem spec, `AGENTS.md`'s code map for a new file, and any
  `.claude/rules/` file that states the old behaviour.

## 6. Do not commit

- Leave all changes uncommitted. Do not commit, amend, rebase, tag or push.
- Suggest a commit command in the style of `git log` (a `feat:`, `fix:`,
  `docs:`, `build:`, `test:`, `style:`, `ci:` or `chore:` prefix and a
  plain summary):
  `git add -A && git commit -m "<prefix>: <summary>"`. If `git status`
  shows a change to `local.properties`, name the files to add instead of
  `-A`: changes to it are never committed (`AGENTS.md`, rule 9).
- The user commits and says when to continue. Before continuing, check
  `git log` and `git status` to see what was committed.

## 7. Report

End with a summary containing:

- **Task:** ID and title, and whether its box is now ticked.
- **Files changed.**
- **Commands run and their results,** each with its real result.
- **Checks not run,** and why. Include `⏳` items and device-only checks.
- **Assumptions** you made.
- **Open risks.**
- **Suggested commit:** the command from step 6.
- **Next:** the next open task ID, and whether an agent can work it.
