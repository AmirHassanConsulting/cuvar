# Čuvar — instructions for Claude (English translation)

[Srpski](CLAUDE.md) · **English**

> Claude Code reads [CLAUDE.md](CLAUDE.md) (Serbian); this file is a translation for
> people. If you change the rules, change CLAUDE.md first. The workflow parts about
> pushing to `main` and the agent pipeline apply to the maintainer's sessions;
> external contributors follow [CONTRIBUTING.md](CONTRIBUTING.md).

An Android app (Java) that limits the user's phone use: rules per app and site,
daily limits, night lock, a daily code 17–22 as the only unlock, quiz questions
when unlocking. The main logic is in `GuardService`
(`app/src/main/java/com/cuvar/app/GuardService.java`) — an Accessibility service
that tracks which app/site is open, measures time and shows a block over everything.
Feature overview and file map: [README.en.md](README.en.md).

## Working rules

- Replies and code comments are in Serbian.
- Before changes, run `git pull --rebase` (cloud sessions also work on the repo).
- Check: `tools/check.sh` (Windows: `powershell ./tools/check.ps1`) runs the tests
  like CI plus `assembleDebug`, and prints only errors. Do not run Gradle by hand
  without a filter. Local `javac` accepts hidden Android APIs that CI rejects; for
  newer system intents use a string instead of the constant.
- Finished task: commit (English, short) and push directly to `main`, no PR, then
  check CI. The procedure is in the `izdanje` skill. Every push to `main` is a new
  release (APK + release `v<run_number+100>`), so do not push half-done changes; a
  push of only `.md`, `.claude/**` or `tools/**` does not start a build.
- Large files (`data/Store.java`, `GuardService.java`, `unlock/QuizBank.java`,
  `assets/en.tsv`): do not read them whole. Find the place with `grep -n`, then read
  only that line range. Package map: [ARCHITECTURE.en.md](ARCHITECTURE.en.md).
- Do not delete or change `app/cuvar.keystore` or the signing setup in CI without
  asking: a wrong key means the new version cannot be installed over the old one.
- When you say something is done, also say whether it was tested on a phone. Most
  newer features have not been tried on a real device.

## Working with agents

The main session is the orchestrator: every change in `app/`, `tests/` or gradle
files follows the `rad-sa-agentima` skill. In short:
1. `tools/review-novi.sh "title"` and `.review/zadatak.md`; the code is written by the `izvodjac` agent.
2. `tools/review-stanje.sh` says which reviewers are required; they run in parallel and write to `.review/nalazi/<name>.md`.
3. Fixes one by one, then review again until all approve the same state (`tools/review-ok.sh`, at most 3 rounds).
4. `proveravac` (`tools/check.sh`), then `tester-emulator` if the change touches screens/`guard/` and the emulator is running.
5. Commit; the hook (`tools/guard-hook.sh`) blocks a code commit while review, check or emulator is missing.

Agents: `.claude/agents/` (review procedure: `.claude/agents/README.md`), scripts:
`tools/`, working files: `.review/` (not in git).

| Agent | Model | Why |
|---|---|---|
| `orkestrator` (main session) | opus | plan, splitting work, merging findings, disagreements |
| `istrazivac` | haiku | only reads and finds the place in the code, so the main session does not spend on reading |
| `izvodjac` | sonnet (opus for `GuardService`, `Store`, `guard/`, `enforce()`) | ordinary changes; reviewers check it |
| `pregled-bezbednosti`, `pregled-pravila` | sonnet (opus for the core, same as the implementer) | a change by a stronger model is not approved only by a weaker one |
| `pregled-stabilnosti`, `pregled-prikaza` | sonnet | patterns, text and layout |
| `proveravac`, `tester-emulator` | haiku | run commands and read output |

Permanent orchestrator: `"agent": "orkestrator"` in `.claude/settings.json` (or
`claude --agent orkestrator`); then the main session splits the work from the start,
and the hook does not let it change `app/` and `tests/` itself. The model is changed
in the `model:` field in `.claude/agents/<name>.md`.

## User safety (most important)

- Never make a change that can cut the user's internet or lock the phone with no way
  out — there must always be a way to exit/unlock.
- The phone (dialer), SMS and the home screen are never blocked (`exemptApps`).
  Emergency calls must always work.
- Site blocking via a local VPN/DNS was removed after the incident of 10 Oct 2026
  (it cut the internet, and the protection was closing the screen for turning the
  VPN off). Do not bring it back without explicit agreement and a phone test.
- Uninstall protection (`guardsSelf`) closes system screens. Do not extend it to
  screens needed to get out of a problem (VPN, network, battery...).

## App rules to respect in code

- Tightening applies immediately. Loosening can only be requested 06:00–10:00 and
  takes effect at 06:00 the next day (~24 h): `Store.change()` refuses outside the
  window and creates a hold (a snapshot of the configuration from before the change,
  `rules/ModeHolds`); a mode running across 06:00 stays until the end of its period
  (`carry`). `Store` keeps the configured state (`apps`, `sites`, `schedules`) and
  the state in effect now (`eApps`, `eSites`, `eSchedules`, `eNight`, `eProtect`);
  the service reads only the `...Now()` methods. Every new setting that can make
  things easier for the user must go through the same mechanism (`change()`,
  `enforce()`, `pendingChanges()`, `cancelPending()`).
- A used-up limit (daily, app, site), night lock, mode, focus, number of opens and
  the break are "hard" blocks (`GuardService.hard`): they cannot be unlocked even
  with the code.
- Always take time from `store.now()` / `store.day()` (trusted clock), not from
  `System.currentTimeMillis()`, otherwise moving the clock bypasses the rules.
- Privacy: the app has no internet permission. Do not add it. In a browser only the
  address bar is read, by field ID, never the page content. `GuardDiagnostics` must
  not log addresses, app names or on-screen text.
- Errors must not crash the process (screens and protection are in the same
  process). Every entry from Android (event, click, `Runnable`, receiver, screen
  drawing) goes through `Safe.click/run/screen` or `GuardService.guarded`;
  rescheduling goes in `finally`. An empty `catch` only with a comment explaining
  why. Errors are visible in "Phone load" (plan and status:
  [PLAN-GRESKE.md](PLAN-GRESKE.md), Serbian).
- Mind the battery: `GuardService` runs all the time. Do not read the whole app
  window without need; apps without rules ("quiet") are not read.

## UI text

- Text in code is written in Serbian, Latin script. The display converts it to
  Cyrillic (`Translit`) or English (`Lang`).
- Every new UI text must get a line in `app/src/main/assets/en.tsv` (Serbian TAB
  English; how to do it without reading the whole file: the `novi-tekst` skill),
  and a German line in `de.tsv`.
- Text that does not go through `Ui.Label` (Toast, hint, chart text) is wrapped in
  `Ui.cyr(...)`. Register an app name with `Translit.keep(...)` so it stays as is.

## Project structure

- The code is split into packages by role (`data`, `rules`, `guard`, `web`,
  `unlock`, `ui`, `i18n`); rules and "where a new class goes" are in
  [ARCHITECTURE.en.md](ARCHITECTURE.en.md). Manifest components (`GuardService`,
  activities) are not moved out of `com.cuvar.app`: renaming the class turns
  Accessibility off after an update.
- Gradle project, module `app/`. `applicationId com.cuvar.app`, `minSdk 26`,
  `compileSdk/targetSdk 36`, Java 11. No Kotlin and no external libraries; screens
  are built in code (`Ui`, `Sheet`), not XML. Launcher activity: `MainActivity`.
- The debug build (`app/build.gradle`) has no custom signing config — Gradle signs
  it with the default debug keystore, so `./gradlew assembleDebug` is enough for a
  local test on an emulator/phone (package `com.cuvar.app.debug`).
- `tests/run.ps1` runs pure Java unit tests (without the Android SDK) over the
  classes in `tests/sources.txt`. A test is a plain class with `main` in
  `tests/com/cuvar/app/*Test.java`. A new class without Android dependencies used by
  a test must be added to `tests/sources.txt`.
- `Store` and `GuardService` depend on Android and cannot be tested on a desktop.
  Write new logic (time calculation, rules, detection) as a small class without
  Android (like `UnlockTimer`, `SessionLimit`, `RuleMerge`, `BlockKind`) with a
  test, and leave only reading, saving and calling that class in
  `Store`/`GuardService`.
- Checking in an emulator via adb: the `test-emulator` skill. Detailed scenarios are
  in `tests/README.md` / [tests/README.en.md](tests/README.en.md) (read only the
  section you need).
- Code comments are short.
