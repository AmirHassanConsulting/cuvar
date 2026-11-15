[Srpski](README.md) · **English**

> English translation of [tests/README.md](README.md); the Serbian file is the source
> of truth. Some older scenarios mention features that have since changed (e.g. the
> general night block and the permanent PIN were removed); the current rules are in
> [README.en.md](../README.en.md).

# Running all automated checks

From the project root, with Java 11 or newer and PowerShell:

```powershell
./tests/run.ps1
```

or, on Linux/macOS (tests as in CI plus a debug build, errors only):

```bash
tools/check.sh             # add --no-build to skip assembleDebug
```

The Android SDK is not needed for these tests. The list of pure Java classes is in
`tests/sources.txt`; the script runs all `*Test.java` classes automatically.
On every push to `main`, GitHub runs the same tests (`tests/sources.txt`) before
it builds the APK.

Regression checks cover parent domains, usage interval boundaries, midnight,
daylight saving time, weekly unions of schedules and an unreadable address bar.
Event-based measurement and its connection to Android still need to be checked on
a phone:

1. Use A for four seconds, then B for one: the time must go to the right app, and
   be saved if you turn the screen off right away or open a blocked item.
2. Block `facebook.com` and add `m.facebook.com` with 60 minutes: the parent block
   still applies. When the parent has a time limit, using the subdomain also
   consumes the parent's limit.
3. Without the global night block, change an inactive Monday mode `00:00–01:00` to
   `23:00–02:00`: until the next 06:00 both periods apply. Check that it survives a
   restart and "Cancel delayed changes".
4. After deleting/raising a rule, the list shows the current restriction and,
   separately, the change from 06:00; a deleted item stays visible while the block
   applies.
5. Leave the home screen open across 17:00, 22:00 and midnight: the code and date
   refresh without closing an open dialog.
6. The number `2147483648`, a negative or an empty input must not save a new rule.
   Check dialogs with the keyboard/large font and the checkbox state with TalkBack.
7. In split screen, change the site in the unfocused browser: its rules and the
   site's time still apply. Total phone time counts the focused app, so apps shown
   at the same time do not double the total. The overall daily limit is consumed
   once, even when an app and a site with rules are visible at the same time.
8. A supported browser without a previously read address shows the block
   explanation when there are site rules. A bar hidden while scrolling a long page
   or video keeps the last confirmed domain of the same window (reading an article
   for more than 3 seconds must not lock); this is a heuristic, not content
   recognition.
9. Use up the opens of A, switch to B and quickly back to A: the block stays.
   Repeat with an unfinished pause before opening and with an already allowed session.

Local diagnostics use the `CuvarGuard` tag and log only the operation name and the
error class (no URL, app name, exception message or screen content).

# Time mode

Automatic check of every minute of the day for a night and a day period, an empty
period and midnight boundaries, and for several modes (each with its own period,
apps and sites, including a period across midnight and a disabled mode):

`DailyScheduleTest`, runs with the other tests (`./tests/run.ps1`).

Check on an Android phone:

1. On a phone with the old version (one mode), set a period, an app and a site, then
   install the new version: in Time modes there is "Mode 1" with the same period,
   state (on/off), apps and sites. An app that was only in the mode does not show as
   a rule on the Apps screen.
2. Enable Čuvar in Accessibility. Open Time modes on the home screen and add two
   modes, e.g. one that includes the current time and one that does not, each with a
   different app and site.
3. Check that the app and site (including a subdomain) of the active mode are
   blocked, and that the app and site of the other mode are available. The block
   screen names the mode that is blocking and its period.
4. Check that an earlier PIN unlock of the app does not bypass the mode.
5. While the block is on screen, wait for the end of the period: the block
   disappears at the service's next check (within about 5 seconds). Other PIN and
   daily rules still apply.
6. Change the mode's name and period, turn it off and on again: the selected apps,
   domains and period are kept. Check again after restarting the app.
7. Remove an app or site from the mode and check that the mode no longer applies to
   it. Delete the mode and check that its apps and sites are no longer blocked
   (unless they are in another active mode).
8. Check the night period 21:00–09:00 and that an equal start and end is refused.

The automatic period checks also run in the APK workflow. Phone checks need a
device or emulator with the service running.

# Questions before unlocking

Automatic check of the question bank (every line well-formed, no duplicates, the
correct answer offered) and 20,000 random questions (arithmetic correct, Roman
numerals, no repeat among the last 40):

```powershell
javac -encoding UTF-8 -d .review/quiz app/src/main/java/com/cuvar/app/unlock/Quiz.java app/src/main/java/com/cuvar/app/unlock/QuizBank.java tests/com/cuvar/app/QuizTest.java
java -cp .review/quiz com.cuvar.app.QuizTest
```

New questions are added in `QuizBank.java`, one line per question:
`Question|correct|wrong|wrong|wrong`, or `Question|number` when the answer is typed.

Check on an Android phone:

1. Lock an app with a PIN and open it: a question appears first, and only after a
   correct answer the PIN.
2. For an expired limit or a blocked site: "I still want to unlock", then "Are you
   sure?", then a question, then the PIN.
3. During the pause after unlocking: "Emergency unlock" first asks for an answer,
   then the PIN.
4. A wrong answer shows the correct answer and a new question; the same question
   does not come back right away.
5. Number questions are typed on the keyboard (digits visible), the others have
   four offered answers.
6. A time mode still has no unlock.

# Work hours and the daily code

Automatic check (in the same test as time modes): days of the week, a period across
midnight belonging to the day it starts in, and the daily code (4 digits, changes at
17:00, different for every day and every key).

Check on an Android phone:

1. In Time modes add "Work hours" and choose an app. Every day, weekends included,
   from 09:00 to 17:00 the app is blocked with no unlock at all.
2. While the mode is running, try to turn it off, shorten it, change days, remove
   the app or delete the mode: Čuvar refuses. Adding apps works.
3. While the mode is running, the "Daily code" card on the home screen says
   "Hidden". After 17:00 it shows a 4‑digit code.
4. After 17:00 and before 09:00 the app asks a question and then the daily code;
   the permanent PIN does not unlock it. After an unlock, 5 minutes and a one-hour
   cooldown apply.
5. Move the phone clock forward to 17:01 during the mode: the block stays and the
   code does not change. After a phone restart the clock is read again, but moving
   the clock back has no effect.

# Overall daily limit

The limit consumes only time in apps and on sites that have a rule; maps, calls,
messages and everything else without a rule do not consume it.

Automatic check of the rules (stricter limit immediately, looser or off only from
tomorrow, used-up limit, warning 15 min earlier, time under rules without apps
without rules):

```powershell
javac -encoding UTF-8 -d .review/daylimit app/src/main/java/com/cuvar/app/rules/DayLimit.java tests/com/cuvar/app/DayLimitTest.java
java -cp .review/daylimit com.cuvar.app.DayLimitTest
```

Check on a phone:

1. Use an app without a rule (e.g. maps) for a few minutes: on the Today card the
   total time grows, and the line "Daily limit: … of …" stays the same. Then use an
   app with a rule, then a site with a rule in a browser without a rule: the line
   grows by that time. Time on the block screen is not counted.
2. In "Overall daily limit" choose a value lower than the time in that line: locked
   and limited apps and sites immediately show "Daily limit used up", with no PIN,
   code or emergency unlock. Apps without rules, calls and messages work.
3. Raise the limit: at most 30 % (up to 1 h), 20 % (2 h) or 10 % (from 4 h) is
   added, and the second time on the same day the button is grey. Turning it off
   says it applies from tomorrow.
4. Set the limit 15 min above the time in that line: a warning arrives (once a day).
5. After midnight (by trusted time) the block disappears and the scheduled value
   applies.
6. Upgrade from a version that counted all phone time, on a day when the limit was
   used up mostly by apps without rules: after installing, the line shows only the
   time of apps and sites with a rule, the block disappears and raising is possible
   again. If even that time is over the limit, the block stays.

# Number of opens and the "Close" button

1. In Apps choose an app and enter "Max opens per day", e.g. 2. Open it, go to the
   home screen, open it again: it works. The third time after leaving, it shows
   "Opens for today are used up", with no unlock until midnight.
2. Leaving briefly (sharing, picking a photo, less than 15 s) and returning to the
   same app does not use a new open.
3. The block screen has a big orange "Close" button (goes to the home screen), and
   from the second attempt on the same day it says "This is your Nth attempt today."
4. Raising the number of opens or deleting it applies only from tomorrow 06:00,
   lowering immediately.

# Gaps: split screen and Čuvar turned off

1. Open a locked app in split screen or as a floating window while another app is
   focused (also when that is an app without a rule, navigation or a screen that is
   not read, e.g. Google sign-in): the block appears within about a second and covers
   **only** the locked app's window, with an "Exit split screen" button. The other
   half works normally. The button goes to the home screen. Move the screen divider:
   the block follows the window. (Emulator 1 Nov 2026: Calendar/Google sign-in on
   top, YouTube at the bottom, night block. Before the fix: ~10 s without a block and
   a block over the whole screen.)
2. Turn Čuvar off in Accessibility for a few minutes, then on again: the "Now" card
   says "Čuvar was not running" with the time and reason.
3. Start the phone in Safe Mode, then normally: it says the phone was started
   without Čuvar.

## Three-tab layout and system apps

1. At the bottom you see the tabs Today, Rules and Statistics; a tap switches the
   screen without animation.
2. Today: the Now card, time today and the daily code. The gear at the top right
   opens Theme, Privacy and version.
3. Rules: "For the whole phone" (overall limit, night block) and "Per app and site".
4. Statistics as a tab has no back arrow; tapping Today or Rules returns to the main
   screen.
5. Open the biometrics screen or another system app without an icon: it must not
   appear in time today or in statistics, and does not count toward the overall
   daily limit.

## 6‑second pause

1. Open an app that has a rule (limit, number of opens or a mode outside its
   period): "Pause for a moment" appears with the time and opens for today.
2. "Continue" can be tapped only after 6 s; then the app is visible and only then
   the open is counted.
3. "Close" goes back to the home screen and does not count an open.
4. A short exit (sharing, picking a file) and return within 15 s does not ask for a
   new pause.
5. Apps without a rule, phone and messages open without a pause. After unlocking
   with the code there is no pause.

## Max continuous use

1. In an app (Apps or All rules) enter "Max continuous use" 2 min.
2. Use it without a break: after 1 min the message "one more minute in a row"
   arrives, and after 2 min the screen "Time for a break", which cannot be unlocked
   even with the code.
3. Going to the home screen and coming back before the break ends does not end the
   session or the break.
4. After the 15 min break the app opens again and the session starts from zero. The
   Now card shows how much of the break is left.
5. Lowering or deleting this limit applies only from tomorrow 06:00 (the "From
   tomorrow" card).

## In-app browsers and app copies

1. Put a site on the blocked list. In Instagram, Facebook or Messenger open a link
   to that site: the site block appears with a "Back" button.
2. Open a link to a site that is not on the list: it opens normally (time counts
   toward the site).
3. If Čuvar cannot see the address in that browser and there are site rules, "Link
   is locked" appears with a tip to open the link in Chrome.
4. A cloning app (Parallel Space, Dual Space, App Cloner...) is locked while there
   are app rules.
5. A copy of an app with another package (e.g. "Instagram 2") gets the same rules
   and the same time as the original.

## A quick return does not remove the block

1. Give app A 1 open per day, open it and leave, then open it again: the block
   "Opens are used up".
2. Switch to another app and come back to A in less than 15 s: the block stays.
3. Open an app with a rule, during the 6 s pause switch to another app and quickly
   come back: the pause appears again, and the open is counted only after "Continue".

## Apps without rules are not read

1. Open Google Maps (or calculator, calendar) without a rule and move the map, open
   the bottom panels: maps run smoothly.
2. Time in maps is still visible in statistics and does not consume the overall
   daily limit.
3. From maps switch to an app with a rule: the block or pause appears immediately.
4. Split screen maps + app with a rule: the block still appears.

## Usage access (which app is in the foreground, from Android)

1. On the Today tab "Allow usage access" appears; tap "Open settings" and enable
   Čuvar (works even while uninstall protection is on, for two minutes after the tap).
2. After granting it the card disappears. Maps, camera and calls run smoothly; their
   time is still visible in statistics.
3. From maps switch to an app with a rule: the block or pause appears immediately.
4. From the block, switch to maps via recent apps: the block disappears within 2 s.
5. A new app rule starts applying within 5 s.
6. Focus mode: all apps are tracked as before.

## The browser does not stutter

1. In Chrome scroll a long page: scrolling is as smooth as without Čuvar.
2. Open a blocked site: the block appears within about a quarter of a second
   (emulator: 0.1–0.6 s).

## Blocked site in a browser: goes back in the same tab

1. In Chrome open an allowed site, then type a blocked one in the same tab (e.g.
   instagram.com, also during night mode).
2. Chrome goes back to the previous page by itself, and on top of it is the block
   screen with only "Close" (no "Back"). "Close" only dismisses the screen: the
   previous page is underneath, the number of tabs does not change.
3. HOME while the block screen is up: the screen disappears, the home screen is shown.
4. A blocked site opened from another app (new tab without history): back closes the
   tab, the message stays about 3 s. If the site is still visible after "Close"
   (redirect), Čuvar presses back up to two more times by itself, then home. No new
   tab opens, there is no new block screen and the attempt count does not grow.
5. A site that can be unlocked now (locked with the code, 17–22, outside modes and
   limits): no going back, the site stays under the screen with the game/question.
   "Close" (or system back) goes back in the same tab. After unlocking, the site is
   immediately available.
6. A locked app (not a site): "Close" still goes to the phone's home screen.

## An app without rules is not read at all (Maps)

1. With "Usage access" granted, open Google Maps and use it (search, route, panning)
   for 5 min, also during night mode.
2. Maps do not stutter; Čuvar does not read their window (the foreground app is known
   from usage records).
3. From Maps switch to an app with a rule: block/opens counting work immediately as
   before.
4. From an app with a rule switch to Maps: time is no longer counted for that app.

## Navigation (Google Maps, Waze) fully exempt

1. Without a rule for Maps: open Maps, with and without "Usage access", in night mode
   and during focus.
2. Maps never get a block screen and do not stutter; Čuvar receives no events from
   them and does not read their window.
3. Give Maps a rule (e.g. lock): then they behave like any app with a rule.
4. From Maps switch to an app with a rule: the block works immediately.
