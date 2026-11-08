# 05 — Build, release, operate

**Status (2026-11-07):** Vagor is built and tested, but not yet on Google
Play. What remains is the developer's work in the "Play release checklist"
below: the upload key, hosting PRIVACY.md and LICENSE at public URLs, a
payments profile, creating the app as paid at US$1 and answering Play
Console's declarations (trader status, App content, Data safety), entering
the listing from `fastlane/metadata/`, then an internal test, a
12-tester, 14-day closed test (closed testers must buy the app), and the
application for production access. Requirement 8, Google Play compliance
(`docs/00-overview.md`), is this checklist. Play Console steps are the
developer's to do: an agent can't do them.

## Toolchain

- AGP 9.4.1 with built-in Kotlin: Kotlin 2.4.20, set through the root
  `buildscript` classpath. KSP 2.3.12 for Room. Gradle 9.6.1, through the
  wrapper, whose distribution checksum is pinned
  (`gradle/wrapper/gradle-wrapper.properties`).
- compileSdk 37, targetSdk 36 (the Google Play minimum since 2026-08-31),
  minSdk 26 (`docs/03-platforms.md`).
- Gradle runs on JDK 17 or later, and `gradle/gradle-daemon-jvm.properties`
  asks for a JDK 25 daemon.
- It needs an Android SDK with platform 37, found through `ANDROID_HOME`
  or `sdk.dir` in `local.properties`. `local.properties` is tracked in git
  even though its own header says it shouldn't be: never commit changes to
  it.
- `gradle.properties` turns on Gradle's configuration cache, which is why
  signed builds run with `--no-configuration-cache` ("Release signing").
- None of these versions changes unless the user asks
  (`docs/02-dependency-policy.md`).

## Build types and R8

| Build type | R8 | Signed with | Built by |
| :- | :- | :- | :- |
| `debug` | No | A debug key, never the upload key | `assembleDebug`, every instrumented test, CI |
| `release` | Yes | The upload key when signing is set up ("Release signing"); otherwise unsigned | `bundleRelease` (unsigned in CI), `installRelease` (step 3) |

- **R8** runs on release builds only (`optimization { enable = true }`,
  turned on 2026-10-31; ADR-0021). It shrinks, optimizes and obfuscates
  the code and removes unused resources, with Android's default keep
  rules plus any of the app's own.
- **Keep rules** go in `app/src/main/keepRules/*.keep`. None are needed
  today, because Vagor uses no reflection of its own and the libraries
  ship their own (Room keeps its generated `_Impl`, and Fragment keeps
  every fragment's no-argument constructor). `rules.keep` there holds only
  a comment saying so.
- **Debug builds and the instrumented tests don't use R8**, so check a
  change that relies on reflection, class names or resources looked up by
  name in a release build on an emulator (step 3).
- **The mapping file.** The bundle carries R8's mapping file, which Play
  uses to deobfuscate crash reports, so there's no separate deobfuscation
  file to upload. Keep `app/build/outputs/mapping/release/mapping.txt`
  with each uploaded build to read stack traces yourself: step 3 copies it
  and `app-release.aab` to a folder named after the `versionCode`, outside
  the repository, since the next build overwrites both.
- **Every build** also keeps the libraries' translations out
  (`androidResources.localeFilters += "en"`: the app is English only,
  ADR-0036) and leaves out `DebugProbesKt.bin`, a file kotlinx.coroutines
  (through Room) ships that only its debugger uses, as its README asks.
- **Outputs.** The bundle is
  `app/build/outputs/bundle/release/app-release.aab`. Bundles, APKs,
  `.idsig` files and `/app/release/` are git-ignored.

## Versioning

There are two version numbers, and they move independently.

### App version

- `versionCode` and `versionName` are in `defaultConfig` in
  `app/build.gradle.kts`: 1 and `1.0` today, since nothing has been
  uploaded yet.
- Settings shows them as "Version 1.0 (1)" (`versionName`, then
  `versionCode`; the string `app_version`), so a tester can say which build
  they have. `SettingsInstrumentedTest` checks it.
- Before each upload (step 2):
  - raise `versionCode` by 1, except for the first upload, which keeps 1;
    every upload needs a higher one than the last, test tracks included;
  - give every upload its own `versionName` (`1.0`, then `1.0.1` for a
    fix during the closed test, `1.1`, and so on);
  - write its release note in
    `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`, 500
    characters at most (`changelogs/1.txt` is ready).
- Each uploaded build gets a tag, `v` plus `versionName` in three parts
  (`1.0` becomes `v1.0.0`), the form `Makefile.txt` accepts. Only the
  developer tags (step 3; "What a release is").
- **The application ID never changes** (`io.github.jabbott_iii.vagor`,
  ADR-0020): after the first Play upload a new ID is a different app. That
  is also why the app is created as paid (step 5): a free app can never
  become paid without a new package name. Builds from before the
  application ID changed (they used `com.example.vagor`) are a separate
  app on the phone, and their data doesn't carry over.

### Database version

- `AppDatabase`'s version is 2 (since 2026-10-27, `MIGRATION_1_2`). It
  changes only with a schema change, through a `Migration` added to
  `AppDatabase.MIGRATIONS`, never `fallbackToDestructiveMigration`
  (ADR-0003; `docs/subsystems/data-and-storage.md`;
  `/change-database-schema`). It is separate from `versionCode` and
  `versionName`.

## Release signing

Release signing reads the upload key only from four environment variables
(ADR-0030): `VAGOR_UPLOAD_KEYSTORE`, `VAGOR_UPLOAD_KEYSTORE_PASSWORD`,
`VAGOR_UPLOAD_KEY_ALIAS` and `VAGOR_UPLOAD_KEY_PASSWORD`.

| `VAGOR_UPLOAD_KEYSTORE` | Result |
| :- | :- |
| Unset or blank | `bundleRelease` is unsigned, as in CI |
| Set, the keystore is there and the other three are set | The release build is signed with the upload key |
| Set, but no keystore at that path | Every Gradle build fails ("VAGOR_UPLOAD_KEYSTORE is set, but there's no keystore at …"), debug builds and tests included |
| Set, but one of the other three is missing | Every Gradle build fails ("VAGOR_UPLOAD_KEYSTORE is set, but … isn't"), debug builds and tests included |

- A broken setup fails the build rather than leaving the bundle unsigned
  (added 2026-11-01).
- A relative path is read from the repository root.
- **Signed builds run with `--no-configuration-cache`** (step 3), which
  keeps the passwords out of `.gradle/configuration-cache`. Step 3 clears
  the four variables once the signed builds are done.
- **Never commit a keystore or password.** Keystore files (`*.jks`,
  `*.keystore`, `*.p12`, `*.pfx`) are git-ignored, but keep them outside
  the repository folder anyway (step 1).
- **Signing never goes through CI.** CI builds the bundle unsigned, and the
  removed CD and SonarQube workflows' old secrets are step 9's to clean up.
- With Play App Signing, this key is only the upload key; Play signs the
  app with its own app signing key (steps 1 and 7).

## What a release is

- **No release workflow.** `cd.yml` was removed at the user's request on
  2026-10-25 (ADR-0011), so releases are built and uploaded by hand by the
  developer, following the checklist below. Nothing is built or uploaded
  from a tag.
- **`Makefile.txt` only tags.** `tag` creates an annotated tag, `push-tag`
  pushes it to `origin`, and `release` does both; each checks that
  `VERSION` looks like `vMAJOR.MINOR.PATCH`. Step 3 uses `tag` and
  `push-tag`; don't use `release` later on, since it tags whatever HEAD is
  by then.
- **Agents never run them.** Never run `make -f Makefile.txt tag`,
  `push-tag` or `release`: they create and push git tags. Agents never
  commit, tag, push or publish (`AGENTS.md`). Every step of the checklist
  needs the upload key, a commit or tag, or Play Console, so every step is
  the developer's.
- **A release, step by step:** a new `versionCode` and `versionName` and a
  release note (step 2), a clean, pushed commit that passed CI (step 2), a
  signed bundle checked on an emulator, copied with its mapping file and
  tagged (step 3), then the tracks: internal, closed, production (step 7),
  and Android vitals afterwards (step 8).
- **Sold for US$1.** Vagor is to be sold on Google Play for US$1, like
  Denarii Dolor (decided with the user on 2026-11-07; ADR-0035), so
  `LICENSE`'s license fee, refund and liability cap apply as written, and
  the checklist creates the app as paid. `LICENSE` is the Vagor Perpetual
  End-User License Agreement (ADR-0034).

## Operating Vagor after release

- **No crash reporting, by design** (ADR-0001). Android vitals in Play
  Console is the only signal from the field (step 8), with whatever users
  and testers email to jabbottpublicsupport@gmail.com.
- **Stack traces users send** are read with the `mapping.txt` kept for
  that build's `versionCode` (step 3). Play deobfuscates its own crash
  reports with the copy inside the bundle.
- **The user's data is only on their phone**, in `vacation_database`.
  Cloud backup is off (ADR-0004), and `LICENSE` says the app can't recover
  lost data except from a device-to-device transfer. An update keeps the
  data only through migrations ("Database version").
- **Keep the listing and policy true.** A release that changes what the
  app does may need the privacy policy (both copies), `LICENSE` sections 5
  and 6, the store listing or its screenshots changed with it (steps 4 and
  6; `/update-store-listing`).

## Play release checklist

This is the checklist for Vagor's first Google Play release and every one
after it. It was `PLAY_RELEASE.md` at the repository root until 2026-11-07,
and the steps keep their numbers, so the rest of the docs cite it as
"`docs/05-release.md`, step 3". The repository holds what a release needs: the
code, the signing setup, the privacy policy, and the store listing's text,
graphics and screenshots (`fastlane/metadata/android/en-US/`). The steps below
happen in a terminal and in Play Console, and only the developer can do them.
Play's rules were last checked on 2026-10-31, and its rules for selling a paid
app on 2026-11-07. Vagor is sold for US$1, like Denarii Dolor.

### Already in the app

- **Package name** `io.github.jabbott_iii.vagor`. Play rejects
  `com.example.*`, and the package name can never change after the first
  upload. (The Kotlin package in the source is still `com.example.vagor` and
  shows up in class names; Play's `com.example` restriction applies only to
  the package name.)
- **Target API level 36** (Android 16), required for new apps since
  2026-08-31. Every screen draws edge to edge and keeps its content clear of
  the system bars.
- **Privacy policy** inside the app (Settings > Privacy Policy). The same text
  is in [PRIVACY.md](../PRIVACY.md), and its contact is
  jabbottpublicsupport@gmail.com.
- **No data collection.** No internet permission, no accounts, ads, analytics
  or crash reporting, and no `AD_ID` permission. Cloud backup is off. The app
  requests only `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` and
  `SCHEDULE_EXACT_ALARM`. The bundle also lists AndroidX's app-internal
  `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which needs no declaration.
- **Exact alarms only through `SCHEDULE_EXACT_ALARM`**, for the reminder an
  hour before each event. The user allows it in Android's Alarms & reminders
  screen (Android 14 and later start without it), and without it the reminder
  comes some time in the hour before. The app doesn't declare
  `USE_EXACT_ALARM`, which Play allows only for alarm clock and calendar apps.
  As of 2026-10-31 Play's exact alarm policy asks for a declaration only for
  `USE_EXACT_ALARM`; if Play Console shows a permissions declaration at the
  first upload, answer it with this use (a reminder an hour before each event
  the user plans), and allow for a review that can take several weeks.
- **No native code**, so the 16 KB page size requirement doesn't apply.
- **R8 on release builds**: the code is shrunk and obfuscated and unused
  resources are removed. The bundle carries R8's mapping file, and Play uses
  it to deobfuscate crash reports, so there's no separate deobfuscation file
  to upload.
- **Its own launcher icon**, which is also the themed icon and the Android 12+
  splash screen.
- **Third-party licenses** in Settings > Third-Party Licenses, matching
  [NOTICE](../NOTICE).
- **The version** in Settings ("Version 1.0 (1)": `versionName`, then
  `versionCode`), so a tester can say which build they have.
- **Upload signing from environment variables only** (step 3), and the build
  stops if they're set up wrong instead of quietly making an unsigned bundle.

### 1. Create the upload key (once)

```bash
mkdir -p ~/keys && chmod 700 ~/keys
keytool -genkeypair -keystore ~/keys/vagor-upload.jks -alias vagor-upload -keyalg RSA -keysize 2048 -validity 10000
```

- keytool asks for a keystore password, then for your name and organisation,
  which go into the certificate. It makes a PKCS12 keystore, which has a
  single password: the key's password is the keystore's, so give that one
  wherever a key password is asked for.
- 10,000 days is about 27 years, past Play's minimum (a key valid until at
  least 22 October 2033).
- Back up the keystore and its password somewhere safe, outside the
  repository. Keystore files (`*.jks`, `*.keystore`, `*.p12`, `*.pfx`) are
  git-ignored, but keep them out of the repository folder anyway.
- With Play App Signing (step 7), this is only the upload key. If it's lost or
  leaked, Play Console can reset it; that takes time, but the app keeps its
  identity.

### 2. Before each upload

1. Raise `versionCode` by 1 in `app/build.gradle.kts`, except for the first
   upload, which keeps 1 (its release note, `changelogs/1.txt`, is ready).
   Every upload needs a higher one than the last, test tracks included. Give
   every upload its own `versionName` too, the version users see (`1.0`, then
   `1.0.1` for a fix during the closed test, `1.1`, and so on), since each one
   gets its own tag (step 3).
2. Write the release note in
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`, 500
   characters at most. It goes into each release you create from that bundle
   (step 7).
3. Commit, push, and wait for CI to pass for that commit on GitHub. Build only
   from a clean tree (`git status` shows nothing outside `.idea/`, which
   Android Studio rewrites and which doesn't go into the bundle;
   `git restore .idea` discards those changes): the bundle records the commit
   it was built from even when the tree holds changes that aren't in it.

### 3. Build and check the signed bundle

```bash
export VAGOR_UPLOAD_KEYSTORE=~/keys/vagor-upload.jks
export VAGOR_UPLOAD_KEY_ALIAS=vagor-upload
IFS= read -rs -p "Keystore password: " VAGOR_UPLOAD_KEYSTORE_PASSWORD; echo
export VAGOR_UPLOAD_KEYSTORE_PASSWORD
export VAGOR_UPLOAD_KEY_PASSWORD="$VAGOR_UPLOAD_KEYSTORE_PASSWORD"
./gradlew --no-configuration-cache clean bundleRelease
keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab
```

- **Why `--no-configuration-cache`:** with Gradle's configuration cache, which
  the project turns on, a build stores the passwords (encrypted, but with the
  key on the same machine) under `.gradle/configuration-cache/` in the project
  folder.
- **Signing set up wrong stops the build.** With `VAGOR_UPLOAD_KEYSTORE` set,
  Gradle fails when there's no keystore at that path or one of the other three
  variables is missing, whatever the task, debug builds and tests included.
  Without it the bundle is unsigned, which is how CI builds it. A relative
  path is read from the repository root, and the shell expands `~` only when
  it isn't in quotes.
- **keytool must print the certificate** (`Owner:` and a `SHA256:`
  fingerprint). "Not a signed jar file" means the bundle is unsigned: don't
  upload it. (`jarsigner -verify` is no check: it exits successfully on an
  unsigned bundle too.) The fingerprint must match
  `keytool -list -v -keystore ~/keys/vagor-upload.jks -alias vagor-upload`
  and, after the first upload, the upload key certificate in Play Console
  under App integrity > App signing.

**Try the release build on an emulator.** Only release builds run R8, and the
instrumented tests use the debug build, so check the release build before
every upload. The first command uninstalls Vagor, which deletes its trips, so
run this only with an emulator, never on a phone that holds real trips
(`adb -e` reaches only an emulator):

```bash
adb -e uninstall io.github.jabbott_iii.vagor
serial=$(adb -e get-serialno) && ANDROID_SERIAL=$serial ./gradlew --no-configuration-cache installRelease
unset VAGOR_UPLOAD_KEYSTORE VAGOR_UPLOAD_KEY_ALIAS VAGOR_UPLOAD_KEYSTORE_PASSWORD VAGOR_UPLOAD_KEY_PASSWORD
```

- `installRelease` builds the same R8 output as the bundle and installs it
  signed with the upload key. A debug build is signed with another key, so
  Android won't replace it with this one: that's why the uninstall comes
  first.
- Gradle's install task installs on every connected device unless
  `ANDROID_SERIAL` names one, so the second line takes the emulator's serial
  from `adb -e`, and stops if there isn't exactly one emulator running.
- The last line clears all four signing variables, since nothing after it
  needs them. While they're set, any Gradle command run without
  `--no-configuration-cache` stores the passwords in the configuration cache,
  and while `VAGOR_UPLOAD_KEYSTORE` is set without the others, every Gradle
  command in that shell stops.

Then check, on the emulator:

- add a vacation (the drop-down calendar) and an event (the clock and the time
  zone list), and see both on the dashboard and the vacation page;
- share the vacation, and use Add to Calendar;
- switch dark mode off and on, and rotate an open form;
- open the privacy policy and the third-party licenses in Settings;
- for the reminders, add an event about 70 minutes ahead: its notification
  comes about 10 minutes later (up to 20 when Alarms & reminders is off).

**Keep a copy of what you upload.** The next build overwrites
`app-release.aab` and `app/build/outputs/mapping/release/mapping.txt`. Copy
both to a folder named after the `versionCode`, outside the repository: the
mapping turns stack traces that users send you back into readable ones (Play
does that for its own crash reports from the copy inside the bundle).

**Tag the build** right away, while the tree is still clean and HEAD is the
commit you built, with `versionName` written in three parts (`1.0` becomes
`v1.0.0`), the form [Makefile.txt](../Makefile.txt) accepts:
`make -f Makefile.txt tag VERSION=v1.0.0`. Push the tag after the upload with
`make -f Makefile.txt push-tag VERSION=v1.0.0`, or delete it with
`git tag -d v1.0.0` if that bundle is never uploaded. Don't use the Makefile's
`release` target later on: it tags whatever HEAD is by then. The Makefile only
tags; there's no release workflow, so nothing is built or uploaded from a tag.

### 4. Publish the privacy policy and the license agreement

Play needs the policy at a public, non-PDF URL as well as in the app. Publish
[PRIVACY.md](../PRIVACY.md) as a web page and keep it identical to the in-app
text (`privacy_policy_text` in `app/src/main/res/values/strings.xml`). Its
contact is jabbottpublicsupport@gmail.com, the same address as the store
listing's contact email; to change it, change the Contact section in both
copies.

Publish [LICENSE](../LICENSE), Vagor's end-user license agreement, too, as an
exact copy named `LICENSE.txt`: GitHub Pages serves a `.txt` file as plain
text, which browsers show, but a file with no extension may be downloaded
instead (a Markdown copy would turn its numbered sections into lists). Update
the copy whenever LICENSE changes. If you declare yourself a trader (step 5),
Play asks for a terms of service link for each app, and this is Vagor's.

GitHub Pages works, but on 2026-10-31 `https://jabbott-iii.github.io/`
redirected to `jabbott.dev`, so a Pages site for this repository would be
served at `https://jabbott.dev/Vagor/`. Open each URL you enter in Play
Console in a private window to check that it shows the text instead of
downloading a file.

### 5. Create the app and answer its declarations

**Payments profile** (Play Console > Settings, then Payments profile under
Monetization; Google's help pages give other paths too, and the live Console
may differ). Set it up first: the app's price can't be set without it, and
closed testers can't buy the app until there's a price. Start early. Verifying
the bank account can take up to 5 days, and once the profile is set up Google
emails the steps of an identity check: usually a government ID, plus proof of
address if the ID's address differs from the profile's. Review typically takes
2 to 3 business days once Google has every document it asked for. There are no
payouts until both are done. An account that already sells an app already has
a payments profile: check that it's verified.

- A legal name, which buyers see on their receipts, and a physical address:
  not a PO box. The profile's country can never be changed.
- **The address is public.** Once an account sells anything, Google Play shows
  the payments profile's full address, whether or not you declare yourself a
  trader.
- Public business information: a website, the product category, a customer
  support email (jabbottpublicsupport@gmail.com) and the name on buyers' card
  statements, which follows `GOOGLE*` and can be at most 14 characters.
- US tax information: an IRS Form W-9, with your SSN if you file as an
  individual or sole proprietor.
- A US bank account (checking or savings), verified by a test deposit of less
  than US$1 or instantly.
- Payouts come once a month, around the 15th, for the month before, once at
  least US$1 is owed.

**Create the app:** app name `Vagor: Vacation Planner` (from `title.txt`),
default language English (United States) – en-US, App (not a game), and
**Paid**. The price applies to every track, so choose Paid even though the
first upload is only a test. A free app can never become paid: charging for it
would need a new package name, which is a new app, and Vagor's `applicationId`
never changes.

**Price** (Monetize with Play > Products > App pricing):

- Set the default price to US$1.00 (Play's US range is US$0.05 to US$999.99)
  and choose the countries. Paid apps reach only the countries where Play lets
  buyers pay. Read Trader status below first: an account can declare
  non-trader only if none of its apps is distributed in the EEA or the UK.
  Play converts the price to each country's currency with its own rounding,
  and adds tax where prices must include it (the EU and the UK, for example),
  so other countries' prices won't be exactly a dollar. You can change any
  country's price with its pencil icon.
- Tax and compliance: for the EEA, classify Vagor as Digital Content or as a
  Service. Buyers can get a refund on a Service for 14 days after buying it.
- Google charges and pays sales tax in Arizona and most other US states, and
  VAT in the EU and the UK, so you don't collect it there. US buyers pay the
  tax on top of the price.
- You, not Google, are the merchant of record everywhere except the EEA, the
  UK and the other places on Google's merchant of record list, which doesn't
  include the US (Developer Distribution Agreement 3.4). Google's tax page
  leaves VAT to the developer in some countries, such as Switzerland and
  Tanzania, and in Colombia and Peru for developers outside them. Only the US,
  the EU, the UK and those four were checked (2026-11-07), so read a country's
  entry on that page before choosing it, or leave the country out.
- Google's fee on a sale to a US buyer, since 2026-06-30: 10% plus a 5%
  billing fee on the first US$1M of earnings a year, about 85 cents of each
  dollar to you; otherwise the standard 20% plus 5%, about 75 cents. Google's
  fee table links the first-US$1M rate to its 15% service fee tier page, and
  neither says whether that tier's enrollment is still needed, so enroll
  before the first sale (a payments profile, an Account Group in Play Console,
  and the tier's Terms of Service), then check the Service Fee % column in the
  first earnings report. Australia, Japan, the EEA and the UK have the same
  fees. The other countries move to the new fee structure later (Korea on
  2026-12-31, the rest by 2027-09-30), and Google hasn't given their billing
  fee yet; until then their first US$1M is charged 15% for accounts in that
  tier.

**Refunds.** A buyer can cancel within two hours of buying, and ask Google for
a refund within 48 hours; a refund Google gives takes the app off their phone.
After 48 hours Google sends them to you. Play makes you responsible for your
own refund policy. LICENSE mentions refunds only in 10.3, which says that
ending a license brings no refund unless the law or the platform's refund
policy requires one, so it leaves your refund policy open. You can refund an
order in full from Play apps order management (Manage account > Play apps
order management), but never partly. A refund costs you what the sale paid you
(Google returns its fee); after a payout it comes out of a later one, and it
can't be reversed. If refunds leave your balance negative for 48 hours, Google
debits the bank account that receives your payouts. For orders after
2026-08-03 a chargeback costs you the price, less Google's fee, plus the
bank's chargeback fee, which Google doesn't state and which may be more than a
US$1 sale brings in. Decide your refund policy and record it here with its
date. It isn't decided yet.

**Trader status:** Play Console asks every active developer account for this
declaration, which covers every app on the account: a new account when it's
created, an existing one by 2027-02-01. It's under EEA and UK consumer law,
and the answer must reflect whether you act for purposes of a trade or
business. Misstating it can get the account suspended. Google won't decide it
for you, and suggests a legal professional if you're unsure. Among its signs
of a trader is a profit-seeking motive, which includes selling paid apps "on a
regular or commercial basis", so selling Vagor in the EEA or the UK may make
you one. A personal account that declares itself a trader must give a contact
phone number. Play also asks for each app's terms of service link (LICENSE's
URL from step 4) and for a self-certification that the apps comply with the
law. From 2027-03-01 Google Play shows a trader's phone number, address and
terms link to users everywhere, and in the EEA and the UK names the trader
instead of Google in "Offered by". For a non-trader, Play tells users where
trader status must be shown (the EEA and the UK, for example) that consumer
protection law doesn't apply to their dealings with the developer. An account
that doesn't distribute apps in the EEA or the UK can declare non-trader. If
an account that declared non-trader shows signs of being a trader, Google may
ask it to confirm or update its status, and may remove its apps if it doesn't
answer in time.

**App content** (Policy and programs > App content):

| Section | Answer |
| :- | :- |
| Privacy policy | The privacy policy's public URL from step 4 |
| App access | All functionality is available without special access (no login) |
| Ads | No ads |
| Content rating | Complete the questionnaire. Vagor is a planning utility with no violence, sexual content, gambling or other mature content. It has no chat or other way for users to reach each other inside the app; Share hands text to apps the user picks |
| Target audience | Choose the age groups you intend, such as 18 and over. Including children under 13 brings in the Families policy. See the age laws below |
| Data safety | See below |
| Advertising ID | No: Vagor doesn't use it, and the bundle has no `AD_ID` permission. Recheck after adding any library |
| Government apps | No |
| Financial features | None |
| Health apps | None |
| News apps | No |

**Data safety answers**

- **Does your app collect or share any of the required user data types?** No.
  - Play counts data as *collected* only when it is sent off the device. Vagor
    keeps everything on the device and has no internet access.
  - Sharing is only the user's own tap on Share, which opens Android's share
    menu, or on Add to Calendar, which hands one vacation or event to a
    calendar app. Play exempts transfers the user starts and expects
    ("User-initiated action" in Play's Data safety help).
  - Buying Vagor happens in Google Play before it's installed, and Vagor never
    sees the purchase. Play doesn't ask an app to declare payment data that a
    service such as Google Play's billing system collects directly, when the
    app never accesses it.
- Recheck these answers if Vagor ever adds a library that uses the network,
  analytics, crash reporting, or ads.

**US state app store age laws.** Texas SB 2420 is in effect. Play says these
laws' requirements include taking in users' age ranges, and parental approval
for significant changes, from the app store, and telling the store about
significant changes; the developer decides what counts as one, and can tell
Play on the Age Signals page in Play Console without a new release. Play
offers an Age Signals API for this but doesn't require apps to use it, and
says it's each developer's responsibility to decide how these laws apply to
their app. Vagor doesn't use Age Signals today, and whether and how the laws
apply to it is the developer's decision, not yet made. Using the API would add
a Google Play library: a dependency change that needs the merged-manifest
permission check in `docs/02-dependency-policy.md` and a NOTICE entry. Record
the decision and its date here when you make it.

### 6. Store settings and listing

**Store settings** (Grow users > Store presence > Store settings):

- **App category:** Travel and Local.
- **Tags:** up to five of Play's tags that describe a trip planner.
- **Contact email:** jabbottpublicsupport@gmail.com. Play shows it on the
  listing, and it's also the privacy contact. A website is optional (the
  privacy policy page works); a phone number is optional (required on a
  personal account that declares itself a trader) and is shown publicly too.
  Play also shows the payments profile's address, because the app is paid.

**Store listing text.** Paste each field from
`fastlane/metadata/android/en-US/`. The files follow fastlane's layout, though
nothing here uses fastlane; they have no trailing newline, and they're in US
English ("color") to match the listing's default language.

| Field | File | Limit |
| :- | :- | :- |
| App name | `title.txt` | 30 characters |
| Short description | `short_description.txt` | 80 characters; no final period, since it's one sentence |
| Full description | `full_description.txt` | 4,000 characters |

Keep the text true to the app. Don't claim features Vagor doesn't have:
importing trips, sync or cloud backup (Google Calendar may sync an item saved
there, but that's Google Calendar, not Vagor). Name Google Calendar only in
plain text saying what Vagor works with: no Google logo or Calendar icon in
the graphics or screenshots, and no Google name in the app's title or icon.
Don't promise reminders at an exact time either ("within the hour before each
event"): without Alarms & reminders they can come any time in that hour, and
the phone's battery settings can hold them.

**Graphics**, in `fastlane/metadata/android/en-US/images/`:

| Asset | File | Play's rules |
| :- | :- | :- |
| App icon | `icon.png` | 512×512, 32-bit PNG, sRGB, 1 MB at most; full square with no shadow, since Play rounds the corners itself |
| Feature graphic | `featureGraphic.png` | 1024×500, JPEG or 24-bit PNG with no alpha; required to publish |
| Phone screenshots | `phoneScreenshots/01_dashboard.png` to `06_light_mode.png` | JPEG or 24-bit PNG with no alpha, 2 to 8, each side 320 to 3,840 px and the long side at most twice the short one. Four or more at 1080×1920 (9:16) let Play use them in its promotions |

Upload the screenshots in filename order. The icon is drawn from the launcher
icon's suitcase (`app/src/main/res/drawable/ic_launcher_foreground.xml`) on
its blue `#0072B2`. The feature graphic is the name and tagline in the centre,
on the same blue, above a dotted route between two map pins. Play's feature
graphic guidelines ask for no icon-like branding (Play shows the icon beside
it), no device imagery and no main UI near the edges, so it has no suitcase,
phone or screenshot. Leave the tablet screenshot sets empty: Vagor's layouts
are phone layouts, stretched across a tablet, and Play shows the phone
screenshots on tablets.

**Alt text** for each graphic (140 characters at most):

| File | Alt text |
| :- | :- |
| `icon.png` | Vagor's icon: a white rolling suitcase on a blue square |
| `featureGraphic.png` | The name Vagor and the words Your trips, planned offline, above a dotted route between two map pins |
| `01_dashboard.png` | The Vacations tab listing upcoming trips, soonest first, each with its dates, trip details and days until it starts |
| `02_vacation_page.png` | A trip's page with buttons to edit, share or add it to a calendar, above its events with their dates and times |
| `03_event_form.png` | Editing an event: its title, date, time, time zone and details, with buttons to save, delete, share or add to a calendar |
| `04_calendar.png` | The drop-down calendar for an event's date, where only the days of its trip can be picked |
| `05_time_zone.png` | The searchable time zone list, opened at the event's zone, Lisbon, with each zone's name and offset from GMT |
| `06_light_mode.png` | A trip to Kyoto in light mode, with two events at their times in Japan |

**Retaking the screenshots** after the screens change. Screenshots are public,
so use invented trips, and an emulator, never a phone that holds real trips:

```bash
adb -e shell wm size 1080x1920
adb -e shell settings put global sysui_demo_allowed 1
adb -e shell am broadcast -a com.android.systemui.demo -e command enter
adb -e shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1030
adb -e shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb -e shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e fully true
adb -e shell am broadcast -a com.android.systemui.demo -e command network -e mobile hide
adb -e exec-out screencap -p > 01_dashboard.png
adb -e shell am broadcast -a com.android.systemui.demo -e command exit
adb -e shell wm size reset
```

Clear the notification shade first: on the API 37 emulator, demo mode didn't
hide notification icons. `screencap` writes PNGs with an alpha channel, which
Play refuses, so remove it before uploading (for example
`convert 01_dashboard.png -alpha off PNG24:01_dashboard.png` with ImageMagick
6, Ubuntu's `imagemagick` package; ImageMagick 7 calls it `magick` instead of
`convert`).

### 7. Release: internal, then closed, then production

A new personal developer account can't publish to production until a closed
test has run, so the first upload goes to a test track. It also has to show
that you have a real Android phone before any app can be made available: Play
Console's home page shows the task, with a QR code to scan on a non-rooted
phone running Android 10 or later, which installs the Play Console app to
confirm it. An emulator doesn't count.

Every release you create, on any track, has its own release notes: paste
`fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` for that
release's bundle between the `<en-US>` and `</en-US>` tags (500 characters at
most). Users see the production release's notes as "What's new".

1. **Internal testing.** Create a release on the Internal testing track and
   upload the bundle. Internal testers install the paid app free.
   - The first upload sets up Play App Signing. Keep the default, a
     Google-generated app signing key; don't export and upload your own. Play
     then signs the app, and your key from step 1 is only the upload key.
   - Note the app signing key's and the upload key's SHA-256 fingerprints from
     App integrity > App signing, and check the upload one against step 3.
   - Add yourself as a tester (up to 100 are allowed). On the test device,
     first uninstall the build from step 3
     (`adb -e uninstall io.github.jabbott_iii.vagor` on an emulator): Play
     signs the app with the app signing key, not the upload key, so Android
     won't install Play's build over it. Then install Vagor from Play's opt-in
     link, signed in to the Play Store as the tester (an emulator needs a
     Google Play system image, not a `google_apis` one), and repeat the checks
     from step 3. This also tests the APKs Play builds from the bundle.
2. **Closed testing.** Finish steps 4 to 6 first: a closed track can't be
   published without the privacy policy, Data safety and the other App content
   answers.
   - Create a closed testing release with the bundle you tested (or a newer
     `versionCode`).
   - Invite at least 12 testers, by email list or Google Group, and keep at
     least 12 opted in for 14 days in a row. A tester who opts out and back in
     starts their 14 days again, so invite a few extra.
   - Closed testers must buy the app, so each tester pays Vagor's price (US$1
     plus any sales tax in the US, the converted local price elsewhere): say
     so in the invitation. Invite only people in the countries you chose under
     Price in step 5, since a paid app can't be bought anywhere else. Play
     counts testers who are opted in, but it can ask for more testing when
     testers use the app too little, and its help doesn't say whether one who
     opts in without buying counts. A tester who doesn't buy can't use Vagor,
     so invite people who will buy, install and use it during the 14 days.
     Someone who opted in to the internal test can't join the closed test
     until they opt out of the internal one. A tester who bought Vagor during
     the test should still have it once it's in production, since Play lets
     buyers reinstall what they bought (Play's help implies this but doesn't
     say it outright).
   - Promo codes (up to 500 a quarter) give the paid app free, but Play's help
     doesn't say whether they work before the app is in production; try one
     before relying on them. Don't refund testers during the 14 days: a refund
     Google gives takes the app off their phone, and Play's help doesn't say
     whether one from Order management does.
   - Give the track's feedback channel as jabbottpublicsupport@gmail.com. Keep
     notes on what testers report and what you fix (each fix is a new upload
     with a higher `versionCode`): the production application asks for both.
   - Read the pre-launch report for every upload.
3. **Apply for production.** After the 14 days, choose Apply for production on
   the Dashboard. The application asks about the closed test (how testers were
   found, how they used the app, their feedback and how it was collected),
   about the app, and about its readiness (what changed because of the test).
   Review usually takes seven days or less. If it isn't approved (for too few
   opted-in testers or too little use of the app, for example), the closed
   test goes on.
4. **Production.** Create a production release with the tested bundle. A first
   release can't be a staged rollout, and it goes through Play's review before
   it appears on the store.

### 8. After each release: Android vitals

Vagor has no crash reporting of its own, by design, so Android vitals in Play
Console (Monitor and improve > Android vitals, Crashes and ANRs) is the only
signal from the field. Check it during the closed test and after every
release.

- Play's bad behaviour thresholds, over the last 28 days: a user-perceived
  crash rate of 1.09% overall or 8% on any one phone model, and an ANR rate of
  0.47% overall or 8% on any phone model. Above them Play may make Vagor
  harder to find and show a warning on its listing.
- Vitals come only from users who share usage and diagnostics data with
  Google. At closed-test size an empty page means no data, not no crashes, so
  ask testers to email any crash.
- Play shows stack traces deobfuscated with the bundle's mapping file.

### 9. Repository housekeeping

These are the developer's to do; none of them blocks a release.

- **Old GitHub secrets.** The removed CD and SonarQube workflows read secrets
  named `ANDROID_SIGNING_*`, `GOOGLE_PLAY_SERVICE_ACCOUNT`, `SLACK_WEBHOOK`
  and `SONAR_*`. Check GitHub > Settings > Secrets and variables > Actions and
  delete any that exist. Vagor's signing never goes through CI.
- **Machine-specific files in git.** `local.properties` holds this machine's
  SDK path and is tracked although `.gitignore` lists it, and `.idea/` tracks
  Android Studio's caches and per-user state. To stop tracking them (the files
  stay on disk):

  ```bash
  git rm --cached local.properties .idea/caches/deviceStreaming.xml .idea/deploymentTargetSelector.xml .idea/markdown.xml
  ```

  Then add `/caches/`, `/deploymentTargetSelector.xml` and `/markdown.xml` to
  `.idea/.gitignore`, and update every doc that says `local.properties` is
  tracked: `AGENTS.md` rule 9, `CONTRIBUTING.md` (Toolchain), this file's
  Toolchain section, `docs/01-architecture.md` (Code layout) and
  `.claude/rules/dependencies.md`.

### Sources

- Target API: https://developer.android.com/google/play/requirements/target-sdk
- App signing, upload keys and Play App Signing: https://developer.android.com/studio/publish/app-signing
- Preparing and rolling out a release: https://support.google.com/googleplay/android-developer/answer/9859348
- Testing requirements for new personal accounts and applying for production: https://support.google.com/googleplay/android-developer/answer/14151465
- Device verification for new developer accounts: https://support.google.com/googleplay/android-developer/answer/14316361
- App content declarations: https://support.google.com/googleplay/android-developer/answer/9859455
- News apps (only apps in scope are asked): https://support.google.com/googleplay/android-developer/answer/10523915
- Trader status declaration: https://support.google.com/googleplay/android-developer/answer/17594752 (announced 2026-10-29: https://support.google.com/googleplay/android-developer/answer/18100766)
- User Data policy (privacy policy for all apps): https://support.google.com/googleplay/android-developer/answer/10144311
- Data safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Exact alarm permissions: https://support.google.com/googleplay/android-developer/answer/16558241
- US state age laws and Play Age Signals: https://support.google.com/googleplay/android-developer/answer/16569691
- Store settings (contact details, category, tags): https://support.google.com/googleplay/android-developer/answer/9859152 and https://support.google.com/googleplay/android-developer/answer/9859673
- Free and paid apps, and setting the price: https://support.google.com/googleplay/android-developer/answer/6334373 and https://support.google.com/googleplay/android-developer/answer/10463498
- Price ranges by country: https://play.google.com/supported-locations
- Payments profile, bank account and merchant verification: https://support.google.com/googleplay/android-developer/answer/7161426, https://support.google.com/googleplay/android-developer/answer/7162811, https://support.google.com/googleplay/android-developer/answer/13628312 and https://support.google.com/paymentscenter/answer/7159033
- The public address of a developer who sells apps: https://support.google.com/googleplay/android-developer/answer/13634081
- US tax information: https://support.google.com/googleplay/android-developer/answer/7163598
- Sales tax and VAT: https://support.google.com/googleplay/android-developer/answer/138000
- Merchant of record: https://support.google.com/googleplay/android-developer/answer/7645364 and the Developer Distribution Agreement, https://play.google/developer-distribution-agreement.html
- Service fees: https://support.google.com/googleplay/android-developer/answer/112622, https://support.google.com/googleplay/android-developer/answer/16954621 and the 15% tier's enrollment, https://support.google.com/googleplay/android-developer/answer/10632485
- Payouts: https://support.google.com/googleplay/android-developer/answer/137997
- Refunds and order management: https://support.google.com/googleplay/android-developer/answer/2741495 and Google Play's refund policy for buyers, https://support.google.com/googleplay/answer/15574908
- Chargebacks: https://support.google.com/googleplay/android-developer/answer/17068375
- Paid apps on test tracks: https://support.google.com/googleplay/android-developer/answer/9845334
- Promo codes: https://support.google.com/googleplay/android-developer/answer/6321495
- Store listing graphics, screenshots and alt text: https://support.google.com/googleplay/android-developer/answer/9866151
- Icon specifications: https://developer.android.com/distribute/google-play/resources/icon-design-specifications
- Android vitals: https://developer.android.com/topic/performance/vitals
- Gradle's configuration cache and secrets: https://docs.gradle.org/current/userguide/configuration_cache_requirements.html
