package com.cuvar.app;

import com.cuvar.app.rules.ZoneSwitch;
import java.util.Calendar;
import java.util.TimeZone;

/** Promena zone važi tek od sledećih 06:00 po dosadašnjoj zoni. */
public final class ZoneSwitchTest {
    private static int checks;

    public static void main(String[] args) {
        TimeZone bg = TimeZone.getTimeZone("Europe/Belgrade");
        long evening = at(bg, 2026, Calendar.NOVEMBER, 15, 20, 30);
        long morning = at(bg, 2026, Calendar.NOVEMBER, 15, 5, 0);
        long next06 = at(bg, 2026, Calendar.NOVEMBER, 16, 6, 0);
        check(ZoneSwitch.switchAt(evening, bg) == next06, "Uveče: sutra u 06:00");
        check(ZoneSwitch.switchAt(morning, bg) == at(bg, 2026, Calendar.NOVEMBER, 15, 6, 0), "Pre 06:00: danas u 06:00");
        long six = at(bg, 2026, Calendar.NOVEMBER, 15, 6, 0);
        check(ZoneSwitch.switchAt(six, bg) == next06, "Tačno 06:00: strogo posle, sutra");

        String zone = "Europe/Belgrade";
        String paris = "America/New_York";
        ZoneSwitch.State s = ZoneSwitch.next(zone, zone, true, null, 0, evening, bg);
        check(s.zone.equals(zone) && s.pending == null, "Ista zona: ništa");
        s = ZoneSwitch.next(zone, paris, true, null, 0, evening, bg);
        check(s.zone.equals(zone) && paris.equals(s.pending) && s.pendingAt == next06, "Nova zona čeka do 06:00");
        s = ZoneSwitch.next(zone, paris, true, paris, next06, evening + 3600000L, bg);
        check(s.zone.equals(zone) && s.pendingAt == next06, "Čekanje se ne produžava");
        s = ZoneSwitch.next(zone, paris, true, paris, next06, next06, bg);
        check(s.zone.equals(paris) && s.pending == null, "U 06:00 nova zona važi");
        s = ZoneSwitch.next(zone, "Asia/Tokyo", true, paris, next06, evening + 1000L, bg);
        check(s.zone.equals(zone) && "Asia/Tokyo".equals(s.pending) && s.pendingAt == next06, "Druga zona menja čekanje");
        s = ZoneSwitch.next(zone, paris, false, paris, next06, next06 + 1000L, bg);
        check(s.zone.equals(zone) && s.pending == null, "Ručna zona se ne prati");
        s = ZoneSwitch.next(zone, zone, true, paris, next06, evening, bg);
        check(s.zone.equals(zone) && s.pending == null, "Povratak na staru zonu poništava čekanje");
        System.out.println("ZoneSwitchTest: " + checks + " provera prošlo.");
    }

    private static long at(TimeZone tz, int y, int mo, int d, int h, int mi) {
        Calendar c = Calendar.getInstance(tz);
        c.clear();
        c.set(y, mo, d, h, mi, 0);
        return c.getTimeInMillis();
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) throw new AssertionError(what);
    }
}
