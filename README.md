# Shappky Async

**Kill background apps your way — side-by-side with stock Shappky.**

Fork of [YasserNull/shappky](https://github.com/YasserNull/shappky)

[![Version](https://img.shields.io/badge/version-2.0.0--async-1B6CA8)](#)
[![Package](https://img.shields.io/badge/id-com.shams.srk.shappky-39FF14)](#)
[![Min SDK](https://img.shields.io/badge/minSdk-24-blue)](#)
[![License](https://img.shields.io/badge/license-GPL--3.0-lightgrey)](LICENSE)

---

## Why Shappky Async?

| | |
|---|---|
| **Side-by-side** | Install next to stock Shappky (`com.shams.srk.shappky`) |
| **Same core** | Force-stop / kill via **Shizuku** or **Root** |
| **Personal fork** | Features added here for daily use (see [CHANGELOG](CHANGELOG.md)) |

Upstream Shappky (Shell App Killer) stops background apps to free RAM and reduce heat. This fork keeps that behavior and adds personal improvements over time.

---

## Identity

| | |
|---|---|
| App name | **Shappky Async** |
| Package | `com.shams.srk.shappky` |
| Version | `2.0.0-async` |
| Kotlin packages | `com.yassernull.shappky` (unchanged; only `applicationId` differs) |

---

## Build & install (USB ADB)

Requirements: JDK 17, Android SDK, device with USB debugging.

```powershell
.\gradlew :app:assembleArm64-v8aDebug
adb install -r app\build\outputs\apk\arm64-v8a\debug\app-arm64-v8a-debug.apk
```

If HyperOS returns `INSTALL_FAILED_USER_RESTRICTED: Invalid apk` (guard provider missing):

```powershell
adb shell cmd package install-existing com.miui.guardprovider
adb install -r app\build\outputs\apk\arm64-v8a\debug\app-arm64-v8a-debug.apk
```

Universal ABI (slower / larger):

```powershell
.\gradlew :app:assembleUniversalDebug
```

Grant **Shizuku** (or Root) when prompted. Smoke-check: list running apps → kill one → open Settings.

---

## Upstream features (inherited)

- Flexible permissions: Shizuku or Root
- Filter / protect apps; kill FAB; Quick Tile + background service
- Triggers, widgets, Tasker / intents (upstream v2)

---

## License

Shappky Async is licensed under the [GNU General Public License v3.0](LICENSE), same as upstream.

Upstream author donations: [PayPal](https://www.paypal.com/ncp/payment/7X44EWSM9KAVW)
