---
name: novi-tekst
description: New UI text. Add or change text in the Čuvar UI with its English line in en.tsv (and German in de.tsv), without reading the whole file. Use for every new label, button, message or Toast.
---

# New UI text

Text in code is Serbian, Latin script. Cyrillic (`Translit`) and English/German (`Lang`) are
produced at display time.

1. Text through `Ui.text`/`Ui.Label` is handled automatically. Wrap Toasts, hints and chart text
   in `Ui.cyr(...)`. Register app names with `Translit.keep(...)`.
2. **Do not read the whole `app/src/main/assets/en.tsv`** (44 KB). Only check whether the line exists:
   `grep -n -F "Text you are adding" app/src/main/assets/en.tsv`
3. Append a line: Serbian, TAB, English.
   `printf '%s\t%s\n' 'Srpski tekst' 'English text' >> app/src/main/assets/en.tsv`
   Do the same in `app/src/main/assets/de.tsv` (Serbian, TAB, German).
   - A sentence built from parts and values: every part is its own line, with spaces at the ends
     exactly as in the code (`"Danas "` + number).
   - Plurals via `Ui.count(n, "dan", "dana", "dana")`: every form has its own line if it does not
     already exist.
4. `tools/check.sh --no-build`: `LangTest` fails if any text has no line in en.tsv. When the
   English is the same (`" min"`), the line is still needed (Serbian, TAB, same text). Text that
   is never displayed (words for recognizing system screens, Serbian-only quiz questions) is marked
   at the end of its line with `// bez prevoda: reason`. List:
   `java -cp "${TMPDIR:-/tmp}/cuvar-check" com.cuvar.app.LangTest | grep 'Bez engleskog'`.
