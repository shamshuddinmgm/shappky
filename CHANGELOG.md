# Changelog

All notable changes to **Shappky Async** (`com.shams.srk.shappky`) are documented here.

Version format: **`2.0.<revision>-async`**

---

## [2.0.2-async] — 2026-07-27

### Fixed (protection audit)
- Unified `ProtectionManager.isProtected()` (self + set + regex) across kill paths, including `AppModelFilter`
- Removed `am kill-all` bypass that ignored protected apps
- List widget + trigger widget kills respect protected apps and clean up ShellManager
- Service / triggers no longer hardcode old package id `com.yassernull.shappky`
- SharedPreferences StringSet copied on read/write; Xiaomi/Redmi/POCO default regex
- Protected apps dialog shuts down widget ShellManager on dismiss

### Added (2.0.1)
- Protected apps list: **Select all** / **Unselect all** for the current filtered list (toolbar icon + ⋮ menu)

## [2.0.1-async] — 2026-07-26

### Added
- Protected apps list: **Select all** / **Unselect all** for the current filtered list (toolbar icon + ⋮ menu)

## [2.0.0-async] — 2026-07-26

### Identity
- Package **`com.shams.srk.shappky`** — side-by-side with stock Shappky
- App display name → **Shappky Async**
- Source Code link → this fork (`shamshuddinmgm/shappky`)
- Version scheme **`2.0.x-async`** (`versionCode` 20000+)

### Notes
- Based on upstream [YasserNull/shappky](https://github.com/YasserNull/shappky) `2.0.0`
- HyperOS: if `adb install` fails with `INSTALL_FAILED_USER_RESTRICTED: Invalid apk` and log shows `Unknown authority guard`, restore MIUI Guard Provider: `adb shell cmd package install-existing com.miui.guardprovider`
- Build uses `compileSdk = 36` (not 36.1) for broader PackageManager compatibility

---

## Upstream (pre-fork) highlights

- Updated Shizuku library to 13.1.5
- Minimum SDK 24; root via libsu
- Settings: theme, dynamic colors, permission mode, auto-refresh, RAM bar
- Kotlin + Jetpack Compose; Gradle Kotlin DSL
- Triggers, widgets, Tasker plugin, Quick Tile service
