---
name: test-emulator
description: Emulator test. Install Čuvar on an emulator or phone via adb, enable Accessibility and check the block. Use only locally (there is no emulator in the cloud).
---

# Checking in the emulator

Works only on a computer with the Android SDK and a running emulator (API 34/35) or a phone with
USB debugging enabled.

1. `tools/check.sh` (builds `app/build/outputs/apk/debug/app-debug.apk`).
2. `adb install -r app/build/outputs/apk/debug/app-debug.apk` (package `com.cuvar.app.debug`).
3. Accessibility and usage access without tapping through settings:
   ```
   adb shell settings put secure enabled_accessibility_services com.cuvar.app.debug/com.cuvar.app.GuardService
   adb shell settings put secure accessibility_enabled 1
   adb shell appops set com.cuvar.app.debug GET_USAGE_STATS allow
   ```
4. What is on screen, cheaply (text instead of an image):
   `adb shell uiautomator dump /sdcard/u.xml >/dev/null && adb shell cat /sdcard/u.xml | grep -o 'text="[^"]*"' | grep -v 'text=""'`
   The block screen is an Accessibility window and is not always visible in the dump: then
   `adb exec-out screencap -p > /tmp/x.png` (an image costs ~1,500 tokens, use rarely).
5. Tap: `adb shell input tap X Y`, back: `adb shell input keyevent BACK`,
   home: `adb shell input keyevent HOME`.
6. Crashes: `adb logcat -d | grep -E "AndroidRuntime|CuvarGuard" | tail -30`.
7. Reset: `adb shell pm clear com.cuvar.app.debug`. Loosening a rule applies only from 06:00 the
   next day; that is intentional.

When the `tester-emulator` agent runs this: it does not move the clock to pass time-based
scenarios (it records them as not checked), writes the result to `.review/emulator.md` with the
last line `Rezultat: prošlo` (passed) or `Rezultat: nije prošlo` (failed), and runs
`tools/emulator-ok.sh` only after a pass.

Detailed scenarios (limits, night, modes, quiz): `tests/README.md` (English: `tests/README.en.md`);
read only the section you need (`grep -n '^## ' tests/README.md`).
