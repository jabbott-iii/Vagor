---
name: update-store-listing
description: Use when the Google Play listing in fastlane/metadata/android/en-US/ must change because the app changed (its text, release notes, icon, feature graphic or phone screenshots). Keeps the listing true to the app and within Play's limits, and retakes screenshots of invented trips on an emulator.
argument-hint: "<what changed, e.g. 'the event form has a new field'>"
---

# Update the store listing

What changed: `$ARGUMENTS`

The listing lives in `fastlane/metadata/android/en-US/`, in fastlane's
layout, though nothing uses fastlane. The developer pastes and uploads it in
Play Console (`docs/05-release.md`, step 6); you only change the files.
Keep it true to what the app does, as with the privacy policy.

## 1. Find what the change touches

| File | What it is | Play's limit |
| :- | :- | :- |
| `title.txt` | App name (`Vagor: Vacation Planner`) | 30 characters |
| `short_description.txt` | Short description | 80 characters; no final period, since it's one sentence |
| `full_description.txt` | Full description | 4,000 characters |
| `changelogs/<versionCode>.txt` | Release notes for that upload | 500 characters |
| `images/icon.png` | App icon | 512×512, 32-bit PNG, sRGB, 1 MB at most; full square with no shadow |
| `images/featureGraphic.png` | Feature graphic | 1024×500, JPEG or 24-bit PNG with no alpha |
| `images/phoneScreenshots/01_dashboard.png` to `06_light_mode.png` | Phone screenshots, uploaded in filename order | JPEG or 24-bit PNG with no alpha, 2 to 8, each side 320 to 3,840 px, the long side at most twice the short one |

- A screen that a screenshot shows changed: retake that screenshot.
- The launcher icon changed: redraw `icon.png`.
- A feature was added, removed or changed: check both descriptions.
- Leave the tablet screenshot sets empty: Vagor's layouts are phone
  layouts, and Play shows the phone screenshots on tablets.

## 2. Text

- The files have no trailing newline, and they're in US English ("color")
  to match the listing's default language.
- Don't claim features Vagor doesn't have: importing trips, sync or cloud
  backup. Google Calendar may sync an item saved there, but that's Google
  Calendar, not Vagor.
- Name Google Calendar only in plain text saying what Vagor works with: no
  Google logo or Calendar icon in the graphics or screenshots, and no Google
  name in the app's title or icon.
- Don't promise reminders at an exact time ("within the hour before each
  event"): without Alarms & reminders they can come any time in that hour,
  and the phone's battery settings can hold them.
- Release notes: one file per `versionCode`. The first upload keeps
  `versionCode` 1, and `changelogs/1.txt` is its note. Don't raise
  `versionCode` yourself; that's part of an upload (`docs/05-release.md`,
  step 2).
- Check the lengths and the last byte, for example
  `wc -m < fastlane/metadata/android/en-US/title.txt` and
  `tail -c1 fastlane/metadata/android/en-US/title.txt | od -c`.

## 3. Graphics

- **Icon:** drawn from the launcher icon's suitcase paths
  (`app/src/main/res/drawable/ic_launcher_foreground.xml`) on its blue
  `#0072B2`. Redraw it whenever the launcher icon changes.
- **Feature graphic:** the name and tagline in the centre, on the same blue,
  above a dotted route between two map pins (redrawn 2026-11-01). Play's
  feature graphic guidelines ask for no icon-like branding (Play shows the
  icon beside it), no device imagery and no main UI near the edges, so it
  has no suitcase, phone or screenshot.
- Check sizes and alpha with `file` (for example
  `file fastlane/metadata/android/en-US/images/featureGraphic.png` shows
  `8-bit/color RGB` for a PNG with no alpha).

## 4. Screenshots

- Screenshots are public: use **invented trips**, and an **emulator**, never
  a phone that holds real trips. The current six were taken on 2026-11-01
  at 1080×1920; four or more at that size (9:16) let Play use them in its
  promotions.
- Ask the user before running `adb` commands. Use `adb -e`, which reaches
  only an emulator; `adb -d` (a physical phone) is denied.
- Use the commands in `docs/05-release.md`, step 6 ("Retaking the
  screenshots"): the screen size set to 1080×1920, demo mode for a 10:30
  clock, a full battery and full Wi-Fi with mobile hidden, `screencap`, then
  demo mode off and the screen size reset.
- Clear the notification shade first: on the API 37 emulator, demo mode
  didn't hide notification icons.
- `screencap` writes PNGs with an alpha channel, which Play refuses. Remove
  it before saving the file into the listing (`docs/05-release.md`, step 6
  names the ImageMagick command); if ImageMagick isn't installed, say so
  and leave the step to the user.
- Keep the file names, so the upload order stays.

## 5. Alt text

- Each graphic has alt text of 140 characters at most, in the "Alt text"
  table in `docs/05-release.md`, step 6. Update a file's row whenever the
  file changes, so it describes what is now shown.

## 6. Report

- Files changed, and for each text file its new character count.
- Graphics redrawn or retaken, with what `file` reports for each.
- Alt text rows updated.
- Commands run and their real results; anything left for the user (a
  missing tool, Play Console entry).
- Anything in the listing that you think no longer matches the app but
  didn't change.
