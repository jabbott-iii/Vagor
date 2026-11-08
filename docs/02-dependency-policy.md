# 02 — Dependency policy

## The rule

> **Nothing changes in the build without the user, and nothing that ships
> may reach the network.** Don't add a library, or change any dependency,
> AGP, Gradle, Kotlin or SDK version, unless the user asked for it or
> approved it. Every library that ships in the app is listed in `NOTICE`
> and in Settings > Third-Party Licenses, and after adding one, the merged
> manifest is checked for the permissions it pulls in (ADR-0001, ADR-0029).

Why it matters:

- **Local-only is a promise.** Play's Data safety answer ("no data collected
  or shared") and PRIVACY.md depend on Vagor having no network access of any
  kind (ADR-0001). A library can pull in permissions the app's own code never
  asked for, and one that uses the network, analytics, crash reporting or
  ads would change those answers.
- **The bundled libraries' license asks for its text to go with the app**
  (ADR-0029), so every one that ships has to be listed where users see it.
- **Smallest complete change.** Versions change only when a task asks for
  it, never as a side effect of other work.

## What counts as a dependency

| Counts (needs the user's approval) | Does not count |
| :- | :- |
| Any library in `app/build.gradle.kts`'s `dependencies { }`: `implementation`, `testImplementation`, `androidTestImplementation` or `ksp` | The Android platform APIs available at minSdk 26, such as `java.time`, `android.icu`, `AlarmManager`, the Sharesheet and `CalendarContract`'s intents. Check each call's API level against minSdk 26 (`docs/03-platforms.md`) |
| A Gradle plugin, in a `plugins { }` block, the root `buildscript` classpath, or `settings.gradle.kts` | The apps the user picks at run time: the app they share with, and the calendar app Add to Calendar opens |
| A version change of any of those, or of AGP, Kotlin, KSP, Gradle (the wrapper), or `compileSdk`, `targetSdk` or `minSdk` | Vagor's own code and resources |
| A new repository in `settings.gradle.kts` | GitHub Actions and the tools CI downloads. They are not part of the app, but changing them is a CI change, which needs a task that calls for it (`docs/04-quality.md`) |
| Third-party material copied into the app: icons, images, fonts, license texts, or code, even "just one file" | |
| Taking up something a dependency already offers but the code doesn't use: Compose, ViewBinding, ViewModel, a DI framework, coroutines in Vagor's own code (ADR-0002), or Material's DynamicColors (ADR-0017) | |

### Transitive libraries that ship

The declared libraries bring others into the app. `NOTICE` lists them, and
they need the same care as a declared one:

- the AndroidX libraries that Activity, AppCompat, ConstraintLayout, Core,
  Room and Material Components depend on (AndroidX Fragment, for every
  `Fragment` and `DialogFragment`, comes through AppCompat)
- the Kotlin standard library, kotlinx.coroutines (through Room) and the
  JetBrains annotations
- Guava ListenableFuture and the Error Prone annotations
- the JSpecify annotations

kotlinx.coroutines ships a file only its debugger uses, `DebugProbesKt.bin`.
Its README asks apps to leave it out, so `app/build.gradle.kts` excludes it
from packaging. Vagor's own code uses no coroutines.

### Third-party material

| Material | Where | License | Ships in the app? |
| :- | :- | :- | :- |
| Material Design icons, from Google | Vector drawables in `res/drawable/` (NOTICE doesn't say which files; the launcher icon is Vagor's own) | Apache License 2.0 | Yes |
| The Apache License 2.0 text | `res/raw/apache_license_2_0.txt`, shown under Settings > Third-Party Licenses (added 2026-10-31) | — | Yes. `ThirdPartyLicensesTest` checks it's the unchanged text Apache publishes |
| The Gradle wrapper | `gradlew`, `gradlew.bat`, `gradle/wrapper/` | Apache License 2.0 | No (in the repository; NOTICE lists it) |

## Current dependencies

Every version is in `gradle/libs.versions.toml`, except Gradle's own
(`gradle/wrapper/gradle-wrapper.properties`) and the foojay resolver's
(`settings.gradle.kts`). The licenses of build-only tools are those their
published POM files state, read from the local Gradle cache on 2026-11-07;
"Not recorded" means the POM names none.

### In the app

| # | Library | Version | Used for | License |
| :- | :- | :- | :- | :- |
| 1 | AndroidX Activity (`androidx.activity:activity-ktx`) | 1.13.0 | `enableEdgeToEdge` and `SystemBarStyle` (`EdgeToEdge.kt`), `OnBackPressedCallback` (`HomeActivity`), the notification permission request (`VacationsFragment`) | Apache 2.0 |
| 2 | AndroidX AppCompat (`androidx.appcompat:appcompat`) | 1.8.0 | `AppCompatActivity` on every screen, `AppCompatDelegate` night mode (`ThemeSettings`), `AppCompatEditText` (`DateField`) | Apache 2.0 |
| 3 | AndroidX ConstraintLayout (`androidx.constraintlayout:constraintlayout`) | 2.2.2 | Declared and listed in NOTICE, but no layout or code in `app/src` uses it (checked 2026-11-07). Removing it is a dependency change like any other | Apache 2.0 |
| 4 | AndroidX Core (`androidx.core:core-ktx`) | 1.19.0 | Notifications and their channel, `AlarmManagerCompat`, `TaskStackBuilder`, `ViewCompat` and window insets, accessibility delegates, `ContextCompat`, `ConfigurationCompat`, `BundleCompat`, `PackageInfoCompat` | Apache 2.0 |
| 5 | Material Components for Android (`com.google.android.material:material`) | 1.14.0 | The Material 3 theme and its focus rings, `BottomNavigationView`, `MaterialSwitch`, `MaterialCardView`, `ExtendedFloatingActionButton`, `MaterialAlertDialogBuilder` (every dialog), `MaterialTimePicker`, `MaterialShapeDrawable` and `MaterialColors` (the calendar) | Apache 2.0 |
| 6 | Room runtime (`androidx.room:room-runtime`) | 2.8.5 | The database: `AppDatabase`, the DAOs, `Migration` | Apache 2.0 |

### Tests only

| # | Library | Version | Used for | License |
| :- | :- | :- | :- | :- |
| 7 | JUnit (`junit:junit`) | 4.13.2 | JVM unit tests in `app/src/test`. The instrumented tests get JUnit through AndroidX Test's JUnit extensions | Eclipse Public License 1.0 |
| 8 | AndroidX Test JUnit extensions (`androidx.test.ext:junit`) | 1.3.0 | `AndroidJUnit4` in the instrumented tests | Apache 2.0 |
| 9 | Espresso (`androidx.test.espresso:espresso-core`) | 3.7.0 | The instrumented UI tests. Brings `androidx.test:runner`, whose `AndroidJUnitRunner` is the `testInstrumentationRunner` | Apache 2.0 |
| 10 | Room testing (`androidx.room:room-testing`) | 2.8.5 | `MigrationTestHelper` in `MigrationInstrumentedTest`, which reads the exported schemas from `app/schemas` as test assets | Apache 2.0 |

### Build only

| # | Tool | Version | Used for | License |
| :- | :- | :- | :- | :- |
| 11 | Android Gradle Plugin (`com.android.application`) | 9.4.1 | Builds the app, compiles Kotlin itself (built-in Kotlin), runs lint and R8 | Apache 2.0 |
| 12 | Kotlin Gradle plugin (`org.jetbrains.kotlin:kotlin-gradle-plugin`, root `buildscript` classpath) | 2.4.20 | Sets the Kotlin version AGP's built-in Kotlin uses. (The Kotlin standard library ships in the app: NOTICE) | Apache 2.0 |
| 13 | KSP (`com.google.devtools.ksp`) | 2.3.12 | Runs Room's compiler | Apache 2.0 |
| 14 | Room compiler (`androidx.room:room-compiler`, through `ksp`) | 2.8.5 | Generates the database and DAO code, and exports each version's schema to `app/schemas` | Apache 2.0 |
| 15 | detekt (`dev.detekt`) | 2.0.0-alpha.6 | Kotlin static analysis against `app/detekt-baseline.xml` (ADR-0022). The 2.0 alpha, because the last stable release (1.23.8) predates Kotlin 2.1, AGP 9 and JDK 25 | Apache 2.0 |
| 16 | ktlint Gradle plugin (`org.jlleitschuh.gradle.ktlint`) | 14.2.0 | Runs ktlint (`ktlintCheck`, `ktlintFormat`) | Not recorded |
| 17 | ktlint | 1.8.0 | The Kotlin style: IntelliJ IDEA style without trailing commas, wrapping at 120 characters (ADR-0022) | MIT |
| 18 | Foojay toolchains resolver (`org.gradle.toolchains.foojay-resolver-convention`, `settings.gradle.kts`) | 1.0.0 | Gradle's JDK toolchain resolution. `gradle/gradle-daemon-jvm.properties`, generated by `updateDaemonJvm`, asks for a JDK 25 daemon and lists foojay download URLs for it | Not recorded |
| 19 | Gradle (the wrapper, `distributionSha256Sum` pinned) | 9.6.1 | Runs the build | Apache 2.0 (NOTICE) |

### The platform and the JDK

| Setting | Value | Where |
| :- | :- | :- |
| `compileSdk` | 37 | `app/build.gradle.kts`; needs an Android SDK with platform 37, found through `ANDROID_HOME` or `sdk.dir` in `local.properties` |
| `targetSdk` | 36, the Google Play minimum since 2026-08-31 | `app/build.gradle.kts` |
| `minSdk` | 26 | `app/build.gradle.kts` |
| Java source and target | 11 | `compileOptions` in `app/build.gradle.kts` |
| The JDK Gradle runs on | 17 or later; the daemon asks for JDK 25 | `gradle/gradle-daemon-jvm.properties` |

`docs/03-platforms.md` has what each Android version changes.

### Repositories

- **Plugins:** `google()` (only `com.android.*`, `com.google.*` and
  `androidx.*` groups), `mavenCentral()` and `gradlePluginPortal()`.
- **Libraries:** `google()` and `mavenCentral()`. `FAIL_ON_PROJECT_REPOS`
  stops a module from adding its own.

### Build settings that shape what the libraries ship

- **`androidResources.localeFilters += "en"`** keeps the libraries'
  translations out, so their text (the time picker's, for example) stays
  English like Vagor's own (ADR-0036). Adding a language means adding it
  there.
- **R8 keep rules** go in `app/src/main/keepRules/*.keep`. None are needed
  today: Vagor uses no reflection of its own, and the libraries ship their
  own rules (Room keeps its generated `_Impl` classes, and Fragment keeps
  every fragment's no-argument constructor). `rules.keep` holds only that
  note (ADR-0021).
- **`packaging.resources.excludes += "DebugProbesKt.bin"`** (see
  "Transitive libraries that ship").

### What the libraries add to the manifest

Vagor's own manifest declares only `POST_NOTIFICATIONS`,
`RECEIVE_BOOT_COMPLETED` and `SCHEDULE_EXACT_ALARM` (ADR-0001). The merged
release manifest of the last local build (2026-11-01,
`app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml`)
adds, from the libraries:

| Entry | From | Notes |
| :- | :- | :- |
| `io.github.jabbott_iii.vagor.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX | A `signature` permission, declared and used by the app itself. App-internal, and needs no Play declaration |
| `androidx.startup.InitializationProvider` | AndroidX Startup | Not exported. Starts EmojiCompat, ProcessLifecycle and ProfileInstaller |
| `androidx.room.MultiInstanceInvalidationService` | Room | Not exported |
| `androidx.profileinstaller.ProfileInstallReceiver` | AndroidX ProfileInstaller | Exported, but only to callers holding `android.permission.DUMP` |

There is no `INTERNET`, `ACCESS_NETWORK_STATE` or `AD_ID` permission. Build
again before relying on this list: it is from an earlier build.

## Never, whatever the need

- **Anything that reaches the network:** `INTERNET`,
  `ACCESS_NETWORK_STATE`, HTTP clients, remote WebView content, Firebase,
  analytics, crash reporting, ads, accounts or sync (ADR-0001).
- **The `AD_ID` permission.** Play's Advertising ID answer is "No"
  (`docs/05-release.md`, step 5).
- **`READ_CALENDAR` or `WRITE_CALENDAR`,** or a library that writes to the
  calendar provider. Add to Calendar needs no permission (ADR-0015).
- **`USE_EXACT_ALARM`.** Google Play allows it only for calendar and
  alarm-clock apps (ADR-0014).

## The exception process

Before proposing a library, first try to **make the need disappear**:

- the platform at minSdk 26 may already have it (`java.time` for date math,
  `android.icu` for the phone's own names and patterns)
- a library the app already ships may already have it (AndroidX Core,
  Material Components)
- an Intent to an app the user already has may do it, as Add to Calendar
  does instead of writing to the calendar provider

If the need remains:

1. **Run `/evaluate-dependency <library> <need>`.** The evaluation must
   cover:
   - the need, and the ways of meeting it without a new library
   - whether the library, or anything it pulls in, can reach the network
   - what it adds to the merged manifest: permissions, and components,
     exported or not
   - which transitive libraries it brings into the app
   - its license, and whether its text has to ship with the app
   - whether it supports minSdk 26
   - whether it relies on reflection, class names or resources looked up by
     name, and whether it ships its own R8 keep rules
   - whether it brings in something on the approval list above (Compose,
     ViewModel, a DI framework, coroutines in Vagor's code)
   - its maintainers, its release cadence and its release state (detekt's
     2.0 alpha is in use only because the last stable release predates
     Kotlin 2.1, AGP 9 and JDK 25)
   - for a version change: what changes between the two versions
2. **The user approves or rejects it.** Agents never approve dependencies,
   and change no build file before the user has approved.
3. **If approved,** as a change of its own:
   - Add the version to `gradle/libs.versions.toml` and the dependency to
     `app/build.gradle.kts`.
   - Build, and check the merged manifest for the permissions and components
     it pulls in. If it adds a permission, stop and ask the user: manifest
     permissions don't change unless the task calls for it.
   - If it ships in the app, add it to `NOTICE` and to
     `third_party_licenses_text` in `res/values/strings.xml`.
     `ThirdPartyLicensesTest` checks they list the same things. The test
     expects each NOTICE bullet to end with ": Apache License 2.0", and the
     in-app text says the listed software is under that license, so a
     library under another license also needs that license's text added to
     the app, and the in-app text and the test changed with it.
   - If it relies on reflection, class names or resources looked up by name,
     add keep rules to `app/src/main/keepRules/` and check a release build on
     an emulator: debug builds and the instrumented tests don't run R8.
   - Recheck Play's Data safety and Advertising ID answers
     (`docs/05-release.md`, step 5), and PRIVACY.md and
     `privacy_policy_text` if anything about the app's data changes. The two
     copies of the policy change together.
   - Record the decision with `/record-decision`, and add the library to the
     register above.
   - Run CI's build, static-analysis and unit-test checks, and report what
     ran:

     ```bash
     ./gradlew lint detekt ktlintCheck assembleDebug bundleRelease testDebugUnitTest
     ```

An example still waiting: using Play's Age Signals API would add a Google
Play library, a dependency change that needs this process, the
merged-manifest check and a NOTICE entry (`docs/05-release.md`, step 5;
`docs/roadmap.md`, M7).

## Enforcement

- **`ThirdPartyLicensesTest`** (`./gradlew testDebugUnitTest`) fails if
  NOTICE and the in-app list differ, or if the shipped Apache License text
  isn't the unchanged one.
- **The merged-manifest check is by hand.** Nothing in the build compares the
  permissions with the three allowed ones.
- **CI** (`ci.yml`) runs `lint`, `detekt`, `ktlintCheck`, `assembleDebug`, an
  unsigned `bundleRelease` (R8 and lint's release checks) and
  `testDebugUnitTest`, then `connectedAndroidTest` on an API 37 (`'37.0'`)
  `google_apis` emulator. CI calls only Gradle tasks that exist: the
  SonarQube, OWASP dependency-check and coverage steps were removed, because
  no plugin provided their tasks and `|| true` hid that. Adding one back
  means adding its Gradle plugin first, which is a dependency under this
  policy.
- **detekt's baseline** lists only what the code had on 2026-10-31. Never add
  new findings to it to make a check pass.
- **Path-scoped rule** `.claude/rules/dependencies.md` loads whenever an
  agent opens a build file.
