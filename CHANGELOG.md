# Changelog

All notable changes to **Shappky Async** (`com.shams.srk.shappky`) are documented here.

Version format: **`34.52.<revision>-async`** (`versionCode` = `3452xx`)

---

## [34.52.04-async] — 2026-07-27

### Changed
- Release APKs signed with the **dedicated Async release keystore** (not the debug key)
- Signature change vs earlier debug-signed builds — uninstall before upgrading

---

## [34.52.03-async] — 2026-07-27

### Changed
- Aligned fork versioning with preferred scheme: **`34.52.xx-async`** (replaces temporary `2.0.x-async`)
- Debug builds append `-debug` to `versionName`

### Docs
- GitHub README rewritten for fork identity, what’s new, build/install, HyperOS note

---

## [34.52.02-async] — 2026-07-27

*(formerly shipped as `2.0.2-async`)*

### Fixed (protection audit)
- Unified `ProtectionManager.isProtected()` (self + set + regex) across kill paths, including `AppModelFilter`
- Removed `am kill-all` bypass that ignored protected apps
- List widget + trigger widget kills respect protected apps and clean up ShellManager
- Service / triggers no longer hardcode old package id `com.yassernull.shappky`
- SharedPreferences StringSet copied on read/write; Xiaomi/Redmi/POCO default regex
- Protected apps dialog shuts down widget ShellManager on dismiss

### Added
- Protected apps list: **Select all** / **Unselect all** for the current filtered list (toolbar icon + ⋮ menu)

---

## [34.52.01-async] — 2026-07-26

*(formerly shipped as `2.0.1-async` / bootstrap `2.0.0-async`)*

### Identity
- Package **`com.shams.srk.shappky`** — side-by-side with stock Shappky
- App display name → **Shappky Async**
- Source Code link → this fork (`shamshuddinmgm/shappky`)
- `compileSdk = 36` (not 36.1) for broader PackageManager compatibility

### Notes
- Based on upstream [YasserNull/shappky](https://github.com/YasserNull/shappky) `2.0.0`
- HyperOS: if `adb install` fails with `INSTALL_FAILED_USER_RESTRICTED: Invalid apk` and log shows `Unknown authority guard`, restore MIUI Guard Provider: `adb shell cmd package install-existing com.miui.guardprovider`

---

## Upstream (pre-fork) highlights

- Updated Shizuku library to 13.1.5
- Minimum SDK 24; root via libsu
- Settings: theme, dynamic colors, permission mode, auto-refresh, RAM bar
- Kotlin + Jetpack Compose; Gradle Kotlin DSL
- Triggers, widgets, Tasker plugin, Quick Tile service
