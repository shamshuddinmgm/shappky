# Changelog

All notable changes to **Shappky Async** (`com.shams.srk.shappky`) are documented here.

Version format: **`34.52.<revision>-async`** (`versionCode` = `3452xx`)

---

## [34.52.19-async] — 2026-07-28

### Changed
- Selection UI (row fill, border, accent bar, selected text) now uses maroon brand colors instead of teal

## [34.52.18-async] — 2026-07-28

### Changed
- App icon: maroon background + restored original white mascot glyph; removed decorative white/black edge strips

## [34.52.17-async] — 2026-07-28

### Fixed
- Category swipe stutter (ADB gfxinfo: ~63% legacy jank / 150ms spikes from HorizontalPager composing full LazyColumns mid-gesture). Replaced pager with **single list** + horizontal nested-scroll tab switch.

### Changed
- App icon regenerated: **maroon-red background** + **black edge strips** + cream glyph (adaptive bg `#B4233A`)

## [34.52.16-async] — 2026-07-28

### Fixed
- Category swipe stutter (single composed pager page, icon bitmap cache, pause RAM list updates while swiping, isolate RAM bar recomposition)

### Changed
- App icon: maroon red brim along edges
- App info “All app processes” panel: solid black background for readability

## [34.52.15-async] — 2026-07-28

### Fixed
- Category tab swipe stutter (memoized filters, settled-page toolbar, nearby-page composition only)

### Changed
- Brand system: Obsidian & Maroon (black surfaces + maroon red) — replaces amber
- App icon: black plate + cream glyph + maroon accent (selection teal unchanged)

## [34.52.14-async] — 2026-07-28

### Changed
- Brand system: Graphite & Amber (warm charcoal surfaces + amber accent) — no blue, not flat grey
- Typography polish (settings headers, sort chips, category tabs)
- RAM bar / widgets / XML accents aligned to amber
- App icon: warm charcoal plate + cream glyph + amber accent (selection teal unchanged)

## [34.52.13-async] — 2026-07-28

### Changed
- Brand accent: blue → neutral light/dark grey (Compose theme, XML accents, FAB/headers/buttons)
- App icon background + legacy launcher PNGs recolored to dark grey (selection teal unchanged)

---

## [34.52.12-async] — 2026-07-28

### Fixed
- Select-all selection highlight not updating (stale `remember` on SnapshotStateList)
- Stronger selected-row teal styling for readability

### Removed
- Redundant Settings “All screen filters” (checklist stays in the ⋮ menu only)

---

## [34.52.11-async] — 2026-07-28

### Changed
- Shappky Service toggle moved from More menu → **Settings → Service** (off by default)
- Protected apps: fresh install starts **empty** — no auto launcher/keyboard/system/Google/Xiaomi seeding; Reset clears the list

---

## [34.52.10-async] — 2026-07-28

### Fixed
- **Critical:** Background killer (`ShappkyService`) no longer auto-runs / sticky-restarts. Requires explicit `service_enabled` opt-in from More menu / QS tile / trigger. Accidental enable during QA was killing user apps every ~18s.

---

## [34.52.09-async] — 2026-07-28

### Fixed
- Selection row: teal accent bar + soft fill with clearer text contrast (light/dark)
- More-menu checklist now filters **All screen only**; User/System/Persistent/Protected/Services screens always show their own type
- Settings Main screens: show/hide whole tabs + reorder with up/down
- Checklist toggles no longer trigger a full shell reload (instant filter)
- Pager stutter: precomposed adjacent page, bitmap icons (no AndroidView), loading spinner only on active page

---

## [34.52.08-async] — 2026-07-28

### Added
- More menu / Settings: **Show services / processes** (HAL, media helpers, process aliases, vendor noise)
- Sliding category screens on home: All / User / System / Persistent / Protected / Services
- Settings → **Main screens** toggles to show/hide each category tab

### Changed
- Selected-row highlight: lighter neon blue/cyan gradient (replaces solid dark primary blue)
- Service/process short labels no longer collapse to bare `0` / `0-service` from `@x.y` names
- Select-all scopes to the active category tab
- Services category tab defaults off (opt-in); enabling it also turns on Show services / processes

---

## [34.52.07-async] — 2026-07-27

### Security
- Toybox deploy uses app-scoped `/data/local/tmp/shappky.<pkg>/toybox` with **mandatory `cmp` integrity check** (overwrites planted binaries); removes legacy `/data/local/tmp/toybox`
- Strict Android package-name validation before any shell kill / dumpsys / widget fill-in
- Exported widget config activities require AppWidget ownership of the provider
- Widget `APPWIDGET_UPDATE` ignores forged IDs that are not owned by our providers
- Release builds no longer log full shell command strings
- Dropped unused `FOREGROUND_SERVICE_SPECIAL_USE` and inert `LOCKED_BOOT_COMPLETED`

---

## [34.52.06-async] — 2026-07-27

### Removed
- **Tasker / Locale plugin** (config activity, fire receiver, library, related UI/strings) — external automation subset of Intents
- External Intent API was already removed in 34.52.05

---

## [34.52.05-async] — 2026-07-27

### Removed
- External Intent automation API (`ENABLE_SERVICE` / `DISABLE_SERVICE` / `EXECUTE_TRIGGER` broadcast receiver)

### Security
- Widget kill/refresh actions moved to **non-exported** receivers (blocks other apps from triggering kills)
- `allowBackup="false"`

### Fixed
- Release R8: added `proguard-rules.pro` (Shizuku reflection, libsu)
- Alarm async work uses `goAsync` / blocking wait + ShellManager cleanup
- `kill -9` matches exact process name / `pkg:service` only (no substring siblings)
- Permission mode pref key unified (`permissionMode`)
- SharedPreferences StringSet copies for hidden apps / service exclusions
- Foreground skip uses token-safe dumpsys matching
- Trigger widget no longer falls back to a random first trigger
- Broader Xiaomi/HyperOS default protected regex
- Quick Tile explains missing notification permission
- Service `isRunning` can recover via ActivityManager after process death
- Removed dead `appendKillAll` API

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
- Triggers, widgets, Quick Tile service
