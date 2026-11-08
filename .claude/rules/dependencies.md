---
paths:
  - "**/build.gradle.kts"
  - "settings.gradle.kts"
  - "gradle/**"
  - "gradle.properties"
  - "app/src/main/keepRules/**"
  - "app/detekt-baseline.xml"
  - ".github/workflows/**"
  - "Makefile.txt"
---

# Rules for build files, dependencies and CI

The register of what the build uses, and why, is in
`docs/02-dependency-policy.md`. Releases are in `docs/05-release.md`.

- **No new libraries, plugins or version changes unless the user asked.**
  This covers every dependency in `gradle/libs.versions.toml` and
  `app/build.gradle.kts`, and the versions of AGP, Gradle, Kotlin, KSP and
  the Android SDK. Test-only and build-only libraries count too.
- **If you think one is needed, stop.**
  1. Run `/evaluate-dependency <library> <need>`.
  2. Present the evaluation to the user.
  3. Change nothing until the user approves.
- **Nothing that reaches the network, ever (ADR-0001):** no HTTP clients,
  remote WebView content, Firebase, analytics, crash reporting, ads,
  accounts or sync. Play's Data safety answer ("no data collected or
  shared") and `PRIVACY.md` depend on this.
- **No Compose, ViewBinding, ViewModel, DI framework or coroutines** without
  the user's approval (ADR-0002). kotlinx.coroutines arrives through Room,
  and `packaging` leaves out its `DebugProbesKt.bin` (its README asks apps
  to); that doesn't let Vagor's own code use coroutines.
- **After adding or upgrading any dependency, check the merged manifest for
  permissions it pulls in.**
  - Allowed: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` and
    `SCHEDULE_EXACT_ALARM`, plus AndroidX's app-internal
    `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which needs no declaration.
    No `INTERNET`, no `ACCESS_NETWORK_STATE`, no `AD_ID`.
  - After a build, the merged manifest is at
    `app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml`,
    and `app/build/outputs/logs/manifest-merger-release-report.txt` names the
    library each line came from.
  - Report the permissions you found. If a library adds a permission, stop
    and ask the user: manifest permissions don't change unless the task
    calls for it.
- **Third-party material that ships in the app** (a library, an icon,
  anything else):
  - add it to `NOTICE` and to `third_party_licenses_text` in
    `app/src/main/res/values/strings.xml`, in the same order and wording:
    `ThirdPartyLicensesTest` checks that they list the same things
  - one under a license other than the Apache License 2.0 also needs that
    license's text added; the test expects every `NOTICE` bullet to end
    with ": Apache License 2.0", and the in-app text says the listed
    software is under that license, so both change with it
  - never edit `app/src/main/res/raw/apache_license_2_0.txt`: the test
    checks its SHA-256 against the text Apache publishes
  - `NOTICE`'s bullet list (which `ThirdPartyLicensesTest` compares with
    `third_party_licenses_text`) holds only what ships in the app, not build
    or test tools; the Gradle wrapper, which is in the repository but
    doesn't ship, has a paragraph of its own after the list
- **Fixed values in `app/build.gradle.kts`:**
  - Never change `applicationId` (`io.github.jabbott_iii.vagor`): after the
    first Play upload a new ID is a different app (ADR-0020).
  - Don't change `namespace` (`com.example.vagor`) unless the task calls for
    it.
  - `minSdk` 26, `targetSdk` 36 (the Google Play minimum since 2026-08-31)
    and `compileSdk` 37 change only when the user asks.
  - `versionCode` and `versionName` change only for an upload, as
    `docs/05-release.md`, step 2, says.
  - `androidResources.localeFilters += "en"` keeps the libraries'
    translations out, so their text stays English like Vagor's (ADR-0036).
    Adding a language means adding it there.
- **Room's schema export** (`ksp { arg("room.schemaLocation", ...) }`) and
  the `androidTest` assets folder that points at `app/schemas` stay:
  `MigrationTestHelper` reads the exported schemas from there
  (`.claude/rules/database.md`).
- **R8 runs on release builds** (`optimization { enable = true }`, turned on
  2026-10-31, ADR-0021).
  - Keep rules go in `app/src/main/keepRules/*.keep`. None are needed today:
    the libraries ship their own (Room keeps its generated `_Impl`, and
    Fragment keeps every fragment's no-argument constructor), so
    `rules.keep` holds only a comment.
  - Debug builds and the instrumented tests don't use R8. A change that
    relies on reflection, class names or resources looked up by name is
    checked in a release build on an emulator (`docs/05-release.md`,
    step 3).
- **Release signing (ADR-0030):**
  - The upload key comes only from the `VAGOR_UPLOAD_KEYSTORE`,
    `VAGOR_UPLOAD_KEYSTORE_PASSWORD`, `VAGOR_UPLOAD_KEY_ALIAS` and
    `VAGOR_UPLOAD_KEY_PASSWORD` environment variables.
  - With `VAGOR_UPLOAD_KEYSTORE` unset or blank, `bundleRelease` is
    unsigned, as in CI. Once it's set, a missing keystore or variable fails
    every build, debug builds and tests included (added 2026-11-01). Keep
    that check: it stops a quietly unsigned bundle.
  - Signed builds run with `--no-configuration-cache`. The project turns the
    configuration cache on (`gradle.properties`), and it would store the
    passwords under `.gradle/configuration-cache/`.
  - Never commit a keystore or password. Never read one.
- **detekt and ktlint** (added 2026-10-31 at the user's request, ADR-0022):
  - ktlint uses the IntelliJ IDEA style without trailing commas, wrapping at
    120 characters, and has no baseline. `./gradlew ktlintFormat` fixes most
    findings.
  - detekt uses its default rules and fails only on findings that aren't in
    `app/detekt-baseline.xml`, which lists what the code had on 2026-10-31
    and matches by rule and declaration. **Never add new findings to the
    baseline to make a check pass.**
  - detekt is the 2.0 alpha because the last stable release (1.23.8)
    predates Kotlin 2.1, AGP 9 and JDK 25.
- **Toolchain:** AGP 9.4.1 with built-in Kotlin (Kotlin 2.4.20, set through
  the root `buildscript` classpath), KSP 2.3.12, Gradle 9.6.1. Gradle runs on
  JDK 17 or later, and `gradle/gradle-daemon-jvm.properties` asks for a JDK
  25 daemon. The SDK (platform 37) is found through `ANDROID_HOME` or
  `sdk.dir` in `local.properties`. `local.properties` is tracked in git;
  never commit changes to it.
- **CI (`.github/workflows/`, ADR-0023):**
  - Don't change the workflows unless the task explicitly calls for it.
  - `ci.yml` runs `lint`, `detekt`, `ktlintCheck`, `assembleDebug`, an
    unsigned `bundleRelease` (to run R8 and lint's release checks) and
    `testDebugUnitTest`, then `connectedAndroidTest` on an API 37 (`'37.0'`,
    quoted) `google_apis` emulator.
  - The emulator job needs the Enable KVM step, and the step that installs
    command-line tools 22.0 (added 2026-10-31 after the emulator timed out
    booting): the runner's own tools, 12.0 on Ubuntu 24.04, give an
    `android-37.0` AVD `target=android-0`, and the emulator runner uses them
    whenever they exist.
  - `security.yml` runs CodeQL, which analyses Kotlin only by watching a
    build (`build-mode: manual`).
  - CI calls only Gradle tasks that exist. SonarQube, OWASP
    dependency-check and coverage steps were removed because no plugin
    provided their tasks and `|| true` hid that. Adding one back means
    adding its Gradle plugin first, which is a new dependency.
  - CI can only be checked by pushing, and agents never push. Never report a
    workflow change as passing until a run on GitHub shows it.
  - There is no release workflow: `cd.yml` was removed at the user's request
    on 2026-10-25 (ADR-0011), and signing never goes through CI.
- **`Makefile.txt` only creates and pushes git tags.** Never run
  `make -f Makefile.txt tag`, `push-tag` or `release`.
- **Before reporting done,** run what the change touches and report each
  result: `./gradlew assembleDebug`, `./gradlew lint`,
  `./gradlew detekt ktlintCheck` and `./gradlew testDebugUnitTest`, plus an
  unsigned `./gradlew bundleRelease` when R8 or release settings changed.
