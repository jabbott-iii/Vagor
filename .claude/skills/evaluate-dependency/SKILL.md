---
name: evaluate-dependency
description: Use before proposing any new library, Gradle plugin or version change for Vagor (a dependency, AGP, Gradle, Kotlin, KSP or the Android SDK). Checks it against the local-only rules and the policy in docs/02-dependency-policy.md and writes an evaluation for the user to decide on. Never adds or changes the dependency.
argument-hint: "<library or version change> <what it would be used for>"
---

# Evaluate a proposed dependency or version change

Proposal: `$ARGUMENTS`

Vagor takes no new library and changes no dependency, AGP, Gradle, Kotlin,
KSP or SDK version unless the user asks (`docs/02-dependency-policy.md`).
You are writing an evaluation for the user to decide on. You never approve
it yourself, and you never change `gradle/libs.versions.toml`,
`app/build.gradle.kts`, `build.gradle.kts` or `settings.gradle.kts`.

## Steps

1. **Restate the need.** Describe the capability concretely: what the user
   would see, which screens and files would use it, and the Android versions
   it must work on (minSdk 26 up). Say whether it would ship in the app or
   only in the build or the tests. For a version change, give the current
   and the proposed version and what the change is for.

2. **Try to remove the need.** List what would make the dependency
   unnecessary, each with its cost:
   - an Android framework or AndroidX API the app already has (ICU in
     `android.icu`, `java.time` at minSdk 26, AppCompat, Material, Room)
   - plain Kotlin of our own, like the drop-down calendar, which Vagor draws
     itself instead of using `CalendarView`
   - an intent to another app, like Add to Calendar, which needs no calendar
     permission
   - reduced scope

3. **Check the fixed rules.** Each is "passes", "fails" or "unverified",
   with the evidence. One "fails" means the recommendation is reject.
   1. **No network (ADR-0001):** no HTTP client, remote WebView content,
      Firebase, analytics, crash reporting, ads, accounts or sync, here or in
      its transitive dependencies.
   2. **Manifest:** the permissions and components (exported or not) its
      manifest adds. The merged manifest may hold only
      `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`,
      `SCHEDULE_EXACT_ALARM` and AndroidX's app-internal
      `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. Never `INTERNET`,
      `ACCESS_NETWORK_STATE`, `AD_ID`, `USE_EXACT_ALARM`, `READ_CALENDAR`
      or `WRITE_CALENDAR`.
   3. **Architecture (ADR-0002):** does it bring in Compose, ViewBinding,
      ViewModel, a DI framework or coroutines for Vagor's code to use? That
      needs its own approval.
   4. **Build fit:** minSdk 26, compileSdk 37, targetSdk 36, AGP 9.4.1 with
      built-in Kotlin 2.4.20, KSP 2.3.12, Gradle 9.6.1, a JDK 25 daemon.
   5. **License:** what it is, and whether it lets the library ship in a
      proprietary app sold on Google Play (ADR-0018, ADR-0035). Everything
      shipped today is under the Apache License 2.0.
   6. **R8:** whether it ships its own keep rules, and whether it needs
      reflection or resources looked up by name.
   7. **Play declarations:** whether Data safety ("no data collected or
      shared") or the Advertising ID answer (No) would change
      (`docs/05-release.md`, step 5).

4. **Gather facts about the candidate.**
   - Use the web tools if available: Google's Maven repository, Maven
     Central, the release notes, the source repository.
   - Collect:
     - license
     - latest version and release date, and its release state (stable,
       beta, alpha: detekt's 2.0 alpha is in use only because the last
       stable release predates Kotlin 2.1, AGP 9 and JDK 25)
     - maintainer and release cadence
     - transitive dependencies (and whether each ships)
     - permissions in its manifest
     - its minSdk
     - consumer keep rules
     - app size impact
   - For a version change: the release notes between the two versions, a
     new minSdk or compileSdk, new permissions, behaviour changes, known
     problems.
   - **Never add the library to the build, or change a version, to inspect
     it.** If you can't verify a fact, write "unverified".

5. **Write the evaluation** to `docs/dependency-evaluations/<library>.md`
   using the template below.

6. **Ask the user for a decision.**
   - If they approve, follow `docs/02-dependency-policy.md` ("The exception
     process") as its own change, not inside feature work:
     - add the library to `gradle/libs.versions.toml` with an exact version
       and use it from `app/build.gradle.kts`
     - build, then check the merged manifest's permissions and components
       (`app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml`;
       `app/build/outputs/logs/manifest-merger-release-report.txt` names the
       library each line came from) and report them; if it adds a
       permission, stop and ask the user
     - if it ships: add it to `NOTICE` and to `third_party_licenses_text` in
       `strings.xml`, and run `ThirdPartyLicensesTest`. One not under the
       Apache License 2.0 also needs its license text in the app, and the
       in-app text and the test change with it
     - recheck Play's Data safety and Advertising ID answers
       (`docs/05-release.md`, step 5), and both copies of the privacy policy
       if anything about the app's data changes
     - add keep rules in `app/src/main/keepRules/` if it needs them, and say
       that a release build needs a check on an emulator
       (`docs/05-release.md`, step 3)
     - add its row to the register in `docs/02-dependency-policy.md`
     - write an ADR via `/record-decision`
   - If they reject it, record that in the evaluation file.

## Template

```markdown
# Dependency evaluation: <library> (<version>)

- **Date:** YYYY-MM-DD
- **Requested for:** <capability>, used in <screens/files>
- **Ships in the app:** yes | no (build or tests only)
- **Kind:** new library | new plugin | version change from <x> to <y>

## Need
<concrete description>

## Changes that would avoid it
| Option | Cost | Viable? |
| :- | :- | :- |

## Fixed rules
| Rule | Result | Evidence |
| :- | :- | :- |
| 1. No network | | |
| 2. Manifest | | |
| 3. Architecture (ADR-0002) | | |
| 4. Build fit | | |
| 5. License | | |
| 6. R8 | | |
| 7. Play declarations | | |

## Candidate facts
| Fact | Value | Verified? |
| :- | :- | :- |
| License | | |
| Latest version / date | | |
| Release state | | |
| Maintainer / release cadence | | |
| Transitive dependencies | | |
| Manifest permissions and components | | |
| minSdk | | |
| Keep rules | | |
| App size impact | | |

## Recommendation
<avoid it | adopt with conditions | reject>, with one paragraph of
reasoning.

## Decision
<left blank for the user>
```
