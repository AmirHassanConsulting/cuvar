package com.cuvar.app.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dan jedne stavke (aplikacije ili sajta) od 06:00 do 06:00 sutra, kako sada važi: kad je blokira režim,
 * kad je zaključana, kad se otvara šifrom i kad je slobodna. Posle ponoći važi sledeći dan u nedelji.
 */
public final class DayPlan {
    public static final int FREE = 0;
    public static final int LOCKED = 1;     // zaključano van 17–22, šifra ne otvara
    public static final int CODE = 2;       // zaključano, otvara se dnevnom šifrom
    public static final int BLOCKED = 3;    // režim, ne otvara se

    private static final int START = DailyCode.NIGHT_END_HOUR * 60;
    private static final int DAY = 24 * 60;

    private DayPlan() {
    }

    /** Deo dana: od start do end (minuti od ponoći, end nije uključen) i naziv režima kad blokira. */
    public static final class Segment {
        public final int start;
        public final int end;
        public final int kind;
        public final String rule;

        Segment(int start, int end, int kind, String rule) {
            this.start = start;
            this.end = end;
            this.kind = kind;
            this.rule = rule;
        }
    }

    /** Minut od 06:00 dana pravila (0 … 1439). */
    static int offset(int minute) {
        return ((minute - START) % DAY + DAY) % DAY;
    }

    /** Stanje u jednom minutu; rules su režimi koji sadrže stavku, day je dan u nedelji tog minuta. */
    static Segment at(List<DailySchedule.Rule> rules, boolean locked, int minute, int day) {
        for (DailySchedule.Rule r : rules) if (r.active(minute, day)) return new Segment(minute, minute + 1, BLOCKED, r.name);
        if (!locked) return new Segment(minute, minute + 1, FREE, null);
        boolean code = minute >= DailyCode.CHANGE_HOUR * 60 && minute < DailyCode.LOCK_HOUR * 60;
        return new Segment(minute, minute + 1, code ? CODE : LOCKED, null);
    }

    /**
     * Delovi dana pravila koji počinje u 06:00 dana rulesDay (0 = ponedeljak), spojeni susedni isti.
     * Kraj poslednjeg je 06:00 (360).
     */
    public static List<Segment> plan(List<DailySchedule.Rule> rules, boolean locked, int rulesDay) {
        List<Segment> out = new ArrayList<>();
        int kind = -1, from = 0;
        String name = null;
        for (int o = 0; o <= DAY; o++) {
            Segment s = null;
            if (o < DAY) {
                int minute = (START + o) % DAY;
                s = at(rules, locked, minute, START + o < DAY ? rulesDay : (rulesDay + 1) % 7);
                if (s.kind == kind && same(s.rule, name)) continue;
            }
            if (kind >= 0) out.add(new Segment((START + from) % DAY, (START + o) % DAY, kind, name));
            if (s != null) {
                kind = s.kind;
                name = s.rule;
                from = o;
            }
        }
        return out;
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /** Da li je stavka ceo dan slobodna (tada se red „Danas“ ne prikazuje). */
    public static boolean free(List<Segment> plan) {
        for (Segment s : plan) if (s.kind != FREE) return false;
        return true;
    }

    /** "Danas: 06–09 zaključano · 09–17 blokira Radno vreme · …", ili null kad je ceo dan slobodno. */
    public static String today(List<Segment> plan) {
        if (free(plan)) return null;
        StringBuilder sb = new StringBuilder("Danas: ");
        for (int i = 0; i < plan.size(); i++) {
            Segment s = plan.get(i);
            if (i > 0) sb.append(" · ");
            sb.append(hm(s.start)).append('–').append(hm(s.end));
            if (s.kind == BLOCKED) sb.append(" blokira ").append(s.rule);
            else if (s.kind == CODE) sb.append(" otvara se šifrom");
            else if (s.kind == LOCKED) sb.append(" zaključano");
            else sb.append(" slobodno");
        }
        return sb.toString();
    }

    /** Šta važi u ovom minutu i do kad; limitSpent: potrošen dnevni ili ukupni limit (ne otvara se do ponoći). */
    public static String now(List<Segment> plan, int minute, boolean limitSpent) {
        int at = index(plan, minute);
        if (at < 0) return limitSpent ? "Sada: limit potrošen, do ponoći" : "Sada: slobodno";
        Segment s = plan.get(at);
        if (s.kind == BLOCKED) return "Sada: blokira " + s.rule + " do " + DailySchedule.label(s.end);
        if (limitSpent) return "Sada: limit potrošen, do ponoći";
        if (s.kind == CODE) return "Sada: otvara se šifrom do " + DailySchedule.label(s.end);
        // Sledeći deo tražimo kružno: posle kraja plana nastavlja se od početka (npr. 03:00 -> 17:00 istog dana).
        int n = plan.size();
        if (s.kind == LOCKED) {
            for (int k = 1; k < n; k++) {
                Segment c = plan.get((at + k) % n);
                if (c.kind == CODE) return "Sada: zaključano, šifra od " + DailySchedule.label(c.start);
            }
            return "Sada: zaključano do sutra";
        }
        for (int k = 1; k < n; k++) {
            Segment c = plan.get((at + k) % n);
            if (c.kind != FREE) return "Sada: slobodno do " + DailySchedule.label(c.start);
        }
        return "Sada: slobodno";
    }

    /** Vrsta dela dana u ovom minutu (FREE kad plan nema taj minut). */
    public static int kindAt(List<Segment> plan, int minute) {
        int at = index(plan, minute);
        return at < 0 ? FREE : plan.get(at).kind;
    }

    private static int index(List<Segment> plan, int minute) {
        int o = offset(minute);
        for (int i = 0; i < plan.size(); i++) if (o >= offset(plan.get(i).start) && o < endOffset(plan.get(i))) return i;
        return -1;
    }

    private static int endOffset(Segment s) {
        int e = offset(s.end);
        return e == 0 ? DAY : e;
    }

    /** "09" za pun sat, inače "22:30". */
    public static String hm(int minute) {
        return minute % 60 == 0 ? String.format(Locale.ROOT, "%02d", minute / 60) : DailySchedule.label(minute);
    }

    /** Dan pravila (0 = ponedeljak): pre 06:00 je još prethodni dan. */
    public static int rulesDay(int minute, int day) {
        return minute < START ? (day + 6) % 7 : day;
    }
}
