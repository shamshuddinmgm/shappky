# Changelog

All notable changes to **Shappky Async** (`com.shams.srk.shappky`) are documented here.

Version format: **`2.0.<revision>-async`**

---

## [2.0.0-async] — 2026-07-26

### Identity
- Package **`com.shams.srk.shappky`** — side-by-side with stock Shappky
- App display name → **Shappky Async**
- Source Code link → this fork (`shamshuddinmgm/shappky`)
- Version scheme **`2.0.x-async`** (`versionCode` 20000+)

### Notes
- Based on upstream [YasserNull/shappky](https://github.com/YasserNull/shappky) `2.0.0`
- Feature work starts after device smoke test; list asks before coding
- HyperOS: if `adb install` fails with `INSTALL_FAILED_USER_RESTRICTED: Invalid apk` and log shows `Unknown authority guard`, restore MIUI Guard Provider: `adb shell cmd package install-existing com.miui.guardprovider`
- Build uses `compileSdk = 36` (not 36.1) for broader PackageManager compatibility

---

## Upstream (pre-fork) highlights

- Updated Shizuku library to 13.1.5
- Minimum SDK 24; root via libsu
- Settings: theme, dynamic colors, permission mode, auto-refresh, RAM bar
- Kotlin + Jetpack Compose; Gradle Kotlin DSL
- Triggers, widgets, Tasker plugin, Quick Tile service
