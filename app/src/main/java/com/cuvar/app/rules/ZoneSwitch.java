package com.cuvar.app.rules;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Promena vremenske zone (i automatska) važi tek od sledećih 06:00 po dosadašnjoj zoni, da pomeranje zone
 * ne bi odmah pomerilo režime i dnevnu šifru. Ručna zona se ne prati. Bez Android klasa (tests/ZoneSwitchTest).
 */
public final class ZoneSwitch {
    /** Sat kad nova zona počinje da važi. */
    public static final int SWITCH_HOUR = 6;

    /** Zona koja važi, zona na čekanju (ili null) i kad počinje da važi. */
    public static final class State {
        public final String zone;
        public final String pending;
        public final long pendingAt;

        State(String zone, String pending, long pendingAt) {
            this.zone = zone;
            this.pending = pending;
            this.pendingAt = pendingAt;
        }
    }

    private ZoneSwitch() {
    }

    /** Prvi trenutak 06:00 u zoni current strogo posle now. */
    public static long switchAt(long now, TimeZone current) {
        Calendar c = Calendar.getInstance(current);
        c.setTimeInMillis(now);
        c.set(Calendar.HOUR_OF_DAY, SWITCH_HOUR);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= now) c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    /** Sledeće stanje: zone važi, sys je sistemska zona, pending/pendingAt je zona na čekanju. */
    public static State next(String zone, String sys, boolean autoZone, String pending, long pendingAt,
                             long now, TimeZone current) {
        if (sys.equals(zone) || !autoZone) return new State(zone, null, 0L);
        if (!sys.equals(pending)) {
            pending = sys;
            pendingAt = switchAt(now, current);
        }
        if (now >= pendingAt) return new State(pending, null, 0L);
        return new State(zone, pending, pendingAt);
    }
}
