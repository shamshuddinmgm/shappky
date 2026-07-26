# Shappky Async

**Kill background apps your way — install side-by-side with stock Shappky.**

Personal fork of [YasserNull/shappky](https://github.com/YasserNull/shappky) (Shell App Killer).

[![Version](https://img.shields.io/badge/version-34.52.03--async-1B6CA8)](#)
[![Package](https://img.shields.io/badge/id-com.shams.srk.shappky-39FF14)](#)
[![Min SDK](https://img.shields.io/badge/minSdk-24-blue)](#)
[![License](https://img.shields.io/badge/license-GPL--3.0-lightgrey)](LICENSE)

---

## What’s different in this fork?

| Change | Detail |
|--------|--------|
| **Own package id** | `com.shams.srk.shappky` — runs next to stock Shappky without uninstalling it |
| **App name** | **Shappky Async** |
| **Version scheme** | `34.52.<revision>-async` (revision bumps with fork feature pushes) |
| **Protected apps UX** | **Select all / Unselect all** on the current filtered list (toolbar + ⋮ menu) |
| **Kill protection** | Unified protected checks on all kill paths (list, widgets, triggers, filters) — no `am kill-all` bypass |
| **HyperOS-friendly builds** | `compileSdk = 36` (not 36.1) so PackageManager can parse the APK on more devices |

Full history: [CHANGELOG.md](CHANGELOG.md)

---

## Identity

| | |
|---|---|
| App name | **Shappky Async** |
| Package | `com.shams.srk.shappky` |
| Version | `34.52.03-async` |
| Kotlin packages | `com.yassernull.shappky` (unchanged; only `applicationId` differs) |
| Upstream | [YasserNull/shappky](https://github.com/YasserNull/shappky) |

---

## What Shappky does

Stops / force-stops background apps (via **Shizuku** or **Root**) to free RAM and cut heat. Upstream already includes:

- Flexible permission mode (Shizuku / Root)
- Filter & protect apps; kill FAB
- Quick Settings tile + background service
- Triggers, home-screen widgets, Tasker / intents

This fork keeps that core and layers personal fixes and UX on top.

---

## Build & install (USB ADB)

**Requirements:** JDK 17, Android SDK, USB debugging on the device.

```powershell
.\gradlew :app:assembleArm64-v8aDebug
adb install -r app\build\outputs\apk\arm64-v8a\debug\Shappky-v34.52.03-async-debug-arm64-v8a.apk
```

Universal ABI (larger / slower):

```powershell
.\gradlew :app:assembleUniversalDebug
```

Grant **Shizuku** (or Root) when prompted. Smoke-check: list running apps → kill one → open Settings.

### HyperOS note

If install fails with `INSTALL_FAILED_USER_RESTRICTED: Invalid apk` and logs mention `Unknown authority guard`, restore MIUI Guard Provider, then reinstall:

```powershell
adb shell cmd package install-existing com.miui.guardprovider
adb install -r app\build\outputs\apk\arm64-v8a\debug\Shappky-v34.52.03-async-debug-arm64-v8a.apk
```

Guard Provider is mainly needed for the HyperOS install scan — not required to run the app afterward.

---

## Versioning

Format: **`34.52.<revision>-async`**

| Piece | Meaning |
|-------|---------|
| `34.52` | Fixed major.minor for this fork line |
| `<revision>` | Feature / push revision on this fork |
| `-async` | Fork suffix |

`versionCode` is encoded as `3452xx` (e.g. `34.52.03-async` → `345203`). Debug builds append `-debug` to the version name.

---

## License

Shappky Async is licensed under the [GNU General Public License v3.0](LICENSE), same as upstream.

Upstream author donations: [PayPal](https://www.paypal.com/ncp/payment/7X44EWSM9KAVW)
