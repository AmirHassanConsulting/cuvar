# Contributing to Čuvar

Thanks for helping! Čuvar is a small self-control Android app in plain Java. The
maintainer writes Serbian, but issues and pull requests in English are welcome.

Start here:

- [README.en.md](README.en.md) — what the app does, every rule, code map
- [ARCHITECTURE.en.md](ARCHITECTURE.en.md) — packages, dependency rules, where new code goes
- [CLAUDE.en.md](CLAUDE.en.md) — the full project rules (written for AI assistants, useful for people too)
- [tests/README.en.md](tests/README.en.md) — automated tests and manual phone scenarios

Serbian is the source of truth for all docs. `PLAN-GRESKE.md` (error-handling plan)
and `AUDIT-*.md` (bypass audits) are internal notes and exist only in Serbian.

## Workflow

1. **Fork** the repository and create a **branch** in your fork
   (e.g. `light-mode`). Do not work on your fork's `main`.
2. Open or comment on an **issue** first for anything larger than a small fix, so
   we agree on the approach.
3. Make the change, run the checks (below), commit with a short English message.
4. Open a **pull request** to `stefanciriic/cuvar:main`. Describe what changed and
   whether you tested it on a phone or emulator (most features are not yet tested on
   a real device, so please say so either way).

Every push to `main` builds and publishes a new APK release, so pull requests are
merged only when they are complete. CI does not run on pull requests, so please run
the checks locally.

## Build and checks

Requirements: JDK 11+ (17 works), Android SDK with platform 36 (Android Studio is
the easiest way).

```bash
tools/check.sh                 # Linux/macOS: unit tests as in CI + assembleDebug, prints only errors
powershell ./tools/check.ps1   # Windows, same
./gradlew assembleDebug        # just the debug APK
```

The debug APK has the package `com.cuvar.app.debug` and installs next to the
release app. You never need the release signing keys; do not change
`app/cuvar.keystore` or the signing setup in `.github/workflows/apk.yml`.

Unit tests are plain Java classes with `main` in `tests/com/cuvar/app/*Test.java`
and run without the Android SDK. A new Android-free class used by a test must be
added to `tests/sources.txt`. Put new logic (rules, time calculations, detection)
in a small class without Android in `rules/` with a test, and keep only the call in
`Store` / `GuardService`.

## Code rules

- Java 11, `minSdk 26`, `targetSdk 36`. **No Kotlin, no external libraries, no XML
  layouts**: screens are built in code with `Ui` and `Sheet`.
- **No `INTERNET` permission.** The app must never send data anywhere. In browsers
  only the address bar is read (by field ID), never page content. Logs must not
  contain addresses, app names or on-screen text.
- **Never lock the user out.** The dialer, SMS app and home screen are never blocked,
  emergency calls must always work, and nothing may cut the internet or hide the
  network, VPN or battery settings.
- Do not move or rename `GuardService` or activities out of `com.cuvar.app`:
  Android would turn Accessibility off after the update.
- Time always comes from `store.now()` / `store.day()` (trusted clock), never
  `System.currentTimeMillis()`.
- Every entry point from Android (event, click, `Runnable`, receiver, drawing) goes
  through `Safe.click/run/screen` or `GuardService.guarded`, so an error cannot crash
  the process.
- Keep battery use low: `GuardService` runs all the time; apps without rules are not
  read.
- Code comments are in Serbian and short. If you do not write Serbian, an English
  comment is fine; the maintainer may translate it.

## The loosening rule (important for any new setting)

Čuvar makes it easy to get stricter and slow to get looser:

- **Tightening** (a smaller limit, a new lock, adding to a mode) applies **immediately**.
- **Loosening** (a larger limit, removing a lock or a site, turning a mode off or
  shortening it, removing an item from a mode, turning protection off, an exemption)
  can only be **requested between 06:00 and 10:00** by the trusted clock. Outside that
  window the request is refused with a message. An accepted request takes effect at
  **06:00 the next day**; until then the old setting stays in force and the change is
  shown under "Scheduled changes", where it can be cancelled.
- A mode that is running across 06:00 keeps a removed item until its period ends.

Any new setting that could make things easier for the user must go through the same
mechanism in `Store` (`change()`, `enforce()`, `pendingChanges()`, `cancelPending()`).
Spent limits, modes, focus, opens and breaks are "hard" blocks that the daily code
does not unlock; please keep it that way.

## UI text and translations

- UI text is written in the code in **Serbian, Latin script**. Cyrillic is generated
  automatically (`Translit`).
- Every new UI string needs a line in `app/src/main/assets/en.tsv`
  (`serbian<TAB>english`) **and** in `app/src/main/assets/de.tsv`
  (`serbian<TAB>german`). A line can also be a sentence fragment when the code builds
  a sentence from parts; keep the spaces exactly as in the code.
- `LangTest` (run by `tools/check.sh`) lists texts missing an English translation.
- Text that does not go through `Ui.Label` (Toast, hint, chart labels) must be
  wrapped in `Ui.cyr(...)`. Register app names with `Translit.keep(...)` so they are
  not transliterated.
- If you are not comfortable writing the Serbian source text, write your best attempt
  and mention it in the PR; the maintainer will fix the wording.

## Reporting bugs

Open an issue with the phone model, Android version, Čuvar version (Settings → gear),
what you did and what happened. Please do not include personal data (browsing
history, app lists you do not want public).
