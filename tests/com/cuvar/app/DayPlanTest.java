package com.cuvar.app;

import com.cuvar.app.rules.DailySchedule;
import com.cuvar.app.rules.DayPlan;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Dan stavke od 06:00 do 06:00: režimi, zaključavanje i šifra 17–22. */
public final class DayPlanTest {
    private static int checks;

    public static void main(String[] args) {
        DailySchedule.Rule work = new DailySchedule.Rule("w", "Radno vreme", true, 9 * 60, 17 * 60);
        DailySchedule.Rule night = new DailySchedule.Rule("n", "Noćno zaključavanje", true, 22 * 60 + 30, 6 * 60);
        List<DailySchedule.Rule> both = Arrays.asList(work, night);

        List<DayPlan.Segment> p = DayPlan.plan(both, true, 0);
        eq(DayPlan.today(p), "Danas: 06–09 zaključano · 09–17 blokira Radno vreme · 17–22 otvara se šifrom"
                + " · 22–22:30 zaključano · 22:30–06 blokira Noćno zaključavanje", "Primer iz zadatka");
        eq(DayPlan.now(p, 8 * 60, false), "Sada: zaključano, šifra od 17:00", "Jutro");
        eq(DayPlan.now(p, 10 * 60, false), "Sada: blokira Radno vreme do 17:00", "Radno vreme");
        eq(DayPlan.now(p, 18 * 60, false), "Sada: otvara se šifrom do 22:00", "Šifra");
        eq(DayPlan.now(p, 22 * 60 + 10, false), "Sada: zaključano, šifra od 17:00", "Posle 22 (sutra)");
        // Granice 06:00, 17:00, 22:00, 00:00.
        check(DayPlan.kindAt(p, 6 * 60) == DayPlan.LOCKED, "06:00 zaključano");
        check(DayPlan.kindAt(p, 5 * 60 + 59) == DayPlan.BLOCKED, "05:59 noćni režim");
        check(DayPlan.kindAt(p, 22 * 60 - 1) == DayPlan.CODE, "21:59 šifra");
        check(DayPlan.kindAt(p, 22 * 60) == DayPlan.LOCKED, "22:00 zaključano");
        check(DayPlan.kindAt(p, 0) == DayPlan.BLOCKED, "00:00 noćni režim");
        List<DayPlan.Segment> lk = DayPlan.plan(Collections.<DailySchedule.Rule>emptyList(), true, 0);
        check(DayPlan.kindAt(lk, 17 * 60 - 1) == DayPlan.LOCKED, "16:59 zaključano");
        check(DayPlan.kindAt(lk, 17 * 60) == DayPlan.CODE, "17:00 šifra");
        check(DayPlan.kindAt(lk, 22 * 60 - 1) == DayPlan.CODE, "21:59 šifra bez režima");
        check(DayPlan.kindAt(lk, 22 * 60) == DayPlan.LOCKED, "22:00 bez režima");
        check(DayPlan.kindAt(lk, 0) == DayPlan.LOCKED, "00:00 bez režima");
        check(DayPlan.kindAt(lk, 5 * 60 + 59) == DayPlan.LOCKED, "05:59 bez režima");
        eq(DayPlan.now(lk, 0, false), "Sada: zaključano, šifra od 17:00", "00:00 do šifre");
        eq(DayPlan.now(lk, 3 * 60, false), "Sada: zaključano, šifra od 17:00", "03:00 do šifre");
        eq(DayPlan.now(lk, 6 * 60, false), "Sada: zaključano, šifra od 17:00", "06:00 do šifre");
        eq(DayPlan.now(lk, 17 * 60, false), "Sada: otvara se šifrom do 22:00", "17:00 šifra");
        // Slobodna stavka pre Radnog vremena: posle ponoći gleda kružno.
        List<DayPlan.Segment> wf = DayPlan.plan(Collections.singletonList(work), false, 0);
        eq(DayPlan.now(wf, 3 * 60, false), "Sada: slobodno do 09:00", "03:00 slobodno do radnog");
        eq(DayPlan.now(wf, 23 * 60, false), "Sada: slobodno do 09:00", "23:00 slobodno do radnog");
        eq(DayPlan.now(p, 2 * 60, false), "Sada: blokira Noćno zaključavanje do 06:00", "Posle ponoći");
        eq(DayPlan.now(p, 8 * 60, true), "Sada: limit potrošen, do ponoći", "Limit potrošen");
        eq(DayPlan.now(p, 10 * 60, true), "Sada: blokira Radno vreme do 17:00", "Režim pre limita");

        check(DayPlan.kindAt(p, 10 * 60) == DayPlan.BLOCKED, "Vrsta: režim");
        check(DayPlan.kindAt(p, 18 * 60) == DayPlan.CODE, "Vrsta: šifra");
        check(DayPlan.kindAt(p, 22 * 60 + 15) == DayPlan.LOCKED, "Vrsta: zaključano");

        // Bez zaključavanja: slobodno između režima.
        List<DayPlan.Segment> f = DayPlan.plan(both, false, 0);
        eq(DayPlan.today(f), "Danas: 06–09 slobodno · 09–17 blokira Radno vreme · 17–22:30 slobodno"
                + " · 22:30–06 blokira Noćno zaključavanje", "Bez zaključavanja");
        eq(DayPlan.now(f, 18 * 60, false), "Sada: slobodno do 22:30", "Slobodno do režima");

        // Ceo dan slobodno: nema reda „Danas“.
        List<DayPlan.Segment> none = DayPlan.plan(Collections.<DailySchedule.Rule>emptyList(), false, 0);
        check(DayPlan.today(none) == null, "Slobodno ceo dan");
        eq(DayPlan.now(none, 12 * 60, false), "Sada: slobodno", "Slobodno");
        eq(DayPlan.now(none, 12 * 60, true), "Sada: limit potrošen, do ponoći", "Samo limit");

        // Samo zaključano.
        List<DayPlan.Segment> lock = DayPlan.plan(Collections.<DailySchedule.Rule>emptyList(), true, 0);
        eq(DayPlan.today(lock), "Danas: 06–17 zaključano · 17–22 otvara se šifrom · 22–06 zaključano", "Samo zaključano");

        // Režim preko celog vremena šifre: posle njega nema šifre do sutra.
        DailySchedule.Rule eve = new DailySchedule.Rule("e", "Veče", true, 16 * 60, 23 * 60);
        List<DayPlan.Segment> e = DayPlan.plan(Collections.singletonList(eve), true, 0);
        eq(DayPlan.now(e, 12 * 60, false), "Sada: zaključano do sutra", "Bez šifre danas");

        // Posle ponoći važi sledeći dan u nedelji: noćni režim samo petkom (počinje u petak 22:30).
        DailySchedule.Rule fri = new DailySchedule.Rule("f", "Petak", true, 22 * 60 + 30, 6 * 60);
        fri.days = 1 << 4;
        eq(DayPlan.today(DayPlan.plan(Collections.singletonList(fri), false, 4)),
                "Danas: 06–22:30 slobodno · 22:30–06 blokira Petak", "Petak uveče");
        check(DayPlan.today(DayPlan.plan(Collections.singletonList(fri), false, 5)) == null, "Subota bez režima");
        // Ranojutarnji režim (00–05) pripada sledećem kalendarskom danu.
        DailySchedule.Rule early = new DailySchedule.Rule("x", "Rano", true, 0, 5 * 60);
        early.days = 1 << 5;
        eq(DayPlan.today(DayPlan.plan(Collections.singletonList(early), false, 4)),
                "Danas: 06–00 slobodno · 00–05 blokira Rano · 05–06 slobodno", "Subota rano je deo petka");

        check(DayPlan.rulesDay(5 * 60, 2) == 1, "Pre 06:00 je prethodni dan");
        check(DayPlan.rulesDay(6 * 60, 2) == 2, "Od 06:00 tekući dan");
        check(DayPlan.rulesDay(1, 0) == 6, "Ponedeljak rano je nedelja");
        System.out.println("DayPlanTest OK (" + checks + ")");
    }

    private static void eq(String got, String want, String what) {
        check(want.equals(got), what + ": " + got);
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) throw new AssertionError(what);
    }
}
