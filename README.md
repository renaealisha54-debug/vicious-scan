# Vicious Scan — fixed

A native Android (Kotlin + Jetpack Compose) tool that scans your source code
and surfaces missing or recommended permissions and Gradle dependencies.

---

## Features

- **Project folder scan** — recursively reads any folder you pick via the
  system file picker (Storage Access Framework). No root required.
- **Single file scan** — point it at one file for a quick check.
- **Android-aware rules** — detects `LocationManager`, `Camera`, `Bluetooth`,
  `Room`, `Retrofit`, `Hilt`, `WorkManager`, `Coil`, `DataStore`, and more.
- **Generic rules** — catches debug logs, `TODO()` calls, raw file I/O,
  and missing INTERNET permission across any language.
- **Severity tiers** — REQUIRED / RECOMMENDED / OPTIONAL so you know what
  to fix first.
- **Auto-patch** — preview diffs and let the app insert snippets directly
  into your `AndroidManifest.xml` and `build.gradle.kts`.
- **Manual list** — every finding shows the exact snippet to paste yourself.

---

## Project type detection

Vicious Scan auto-detects Android projects by looking for:
- `AndroidManifest.xml`
- `*.gradle` / `*.gradle.kts`
- `android.app.Activity` or `androidx.*` imports

Everything else falls back to generic scanning.

---

## Supported file types

`kt` · `java` · `xml` · `gradle` · `kts` · `json` · `py` · `js` · `ts` ·
`tsx` · `jsx` · `cpp` · `c` · `h` · `swift` · `dart` · `rb` · `go` · `rs` ·
`toml` · `yaml` · `yml`

---

## Build requirements

| Tool        | Version   |
|-------------|-----------|
| Android Studio | Ladybug+ |
| AGP         | 8.5.2     |
| Kotlin      | 2.0.0     |
| Min SDK     | 26 (Android 8.0) |
| Target SDK  | 35        |
| JVM         | 17        |

---

## Getting started

```bash
git clone https://github.com/YOUR_USERNAME/vicious-scan.git
cd vicious-scan
./gradlew assembleDebug
```

Install on device:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Adding rules

Open `ScanEngine.kt` and append a `Rule(...)` to `ANDROID_RULES` or
`GENERIC_RULES`. Each rule needs:

```kotlin
Rule(
    trigger = "SomeClass",      // substring matched against file content
    finding = ScanFinding(
        name    = "the.permission.NAME",
        type    = FindingType.PERMISSION,
        reason  = "Why this is needed.",
        severity = Severity.REQUIRED,
        autoFixSnippet = "<uses-permission ... />"  // null = manual only
    )
)
```

---

## License

MIT © Alisha — see LICENSE
