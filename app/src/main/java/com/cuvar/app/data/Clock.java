package com.cuvar.app.data;

import com.cuvar.app.rules.TrustedClock;
import com.cuvar.app.rules.ZoneSwitch;

import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;

import java.util.Calendar;
import java.util.TimeZone;
import org.json.JSONException;
import org.json.JSONObject;

/** Pouzdano vreme: meri se od paljenja telefona, pa pomeranje sata ne menja pravila. */
public final class Clock {
    private final Store store;

    Clock(Store store) {
        this.store = store;
    }

    android.content.ContentResolver resolver;
    long clockOff;             // pouzdano vreme = vreme od paljenja + clockOff
    long clockSaved;           // kada je pouzdano vreme poslednji put sačuvano (vreme od paljenja)
    long skewSaved;            // koliko je sat telefona bio pomeren pri poslednjem čuvanju
    TimeZone zone;             // vremenska zona koja važi
    String pendingZone;        // nova zona koja čeka sledećih 06:00 (ili null)
    long pendingAt;            // kad zona na čekanju počinje da važi (pouzdano vreme)

    /**
     * U jednom paljenju telefona vreme se meri od paljenja, pa pomeranje sata ne skraćuje režim
     * i ne donosi novu šifru ranije. Pamti se i koliko je sat telefona ručno pomeren u odnosu na
     * pouzdano vreme, pa ni pomeranje sata pa restart ne pomaže: posle paljenja se ta razlika oduzme.
     * Kad je uključeno automatsko vreme (sa mreže), sat telefona je pouzdan i prati se.
     */
    void startClock() {
        JSONObject o = Store.parse(store.sp.getString("clock", "{}"));
        long el = SystemClock.elapsedRealtime();
        long wall = System.currentTimeMillis();
        boolean sameBoot = TrustedClock.sameBoot(o.has("off"), store.boot, o.optInt("boot", -2), el, o.optLong("el", Long.MAX_VALUE));
        if (sameBoot) {
            clockOff = o.optLong("off");
        } else {
            clockOff = TrustedClock.offsetAfterBoot(wall, el, o.optLong("last", 0L), o.optLong("skew", 0L), wallTrusted(wall));
        }
        // Zona i ona na čekanju se pamte i posle restarta; promena zone ide preko zone() tek od 06:00.
        zone = TimeZone.getTimeZone(o.optString("zone", TimeZone.getDefault().getID()));
        pendingZone = o.has("pzone") ? o.optString("pzone") : null;
        pendingAt = o.optLong("pat", 0L);
        saveClock();
    }

    void saveClock() {
        long el = SystemClock.elapsedRealtime();
        try {
            JSONObject o = new JSONObject();
            o.put("off", clockOff);
            o.put("boot", store.boot);
            o.put("el", el);
            o.put("last", el + clockOff);
            skewSaved = System.currentTimeMillis() - (el + clockOff);
            o.put("skew", skewSaved);
            o.put("zone", zone.getID());
            if (pendingZone != null) {
                o.put("pzone", pendingZone);
                o.put("pat", pendingAt);
            }
            store.sp.edit().putString("clock", o.toString()).apply();
        } catch (JSONException ignored) {
            // put sa ispravnim ključem i vrednošću ne baca grešku; podaci ostaju kakvi su bili
        }
        clockSaved = el;
    }

    /** Pouzdano trenutno vreme u milisekundama. */
    public long now() {
        synchronized (store) {
            long el = SystemClock.elapsedRealtime();
            long wall = System.currentTimeMillis();
            int action = TrustedClock.onRead(el, wall, clockOff, skewSaved, clockSaved, () -> wallTrusted(wall));
            if (action == TrustedClock.RESET) clockOff = wall - el;
            if (action != TrustedClock.KEEP) saveClock();
            return el + clockOff;
        }
    }

    long autoTimeAt = -1;
    boolean autoTime;

    /** Da li telefon uzima vreme sa mreže (provera najviše jednom u 10 s). */
    boolean autoTime() {
        long el = SystemClock.elapsedRealtime();
        if (autoTimeAt < 0 || el - autoTimeAt > 10000L) {
            try {
                autoTime = Settings.Global.getInt(resolver, Settings.Global.AUTO_TIME, 0) == 1;
            } catch (Throwable t) {
                autoTime = false;
            }
            autoTimeAt = el;
        }
        return autoTime;
    }

    /**
     * Da li se sat telefona može pratiti: uključeno je automatsko vreme i (Android 13+) vreme sa mreže se
     * slaže sa satom telefona. Bez toga ručno pomeren sat pa automatsko vreme u avionskom režimu ne bi prošlo.
     * Rezultat se kešira 10 s, a pomeranje sata ga poništava.
     */
    boolean wallTrusted(long wall) {
        if (!autoTime()) return false;
        if (Build.VERSION.SDK_INT < 33) return true;
        long el = SystemClock.elapsedRealtime();
        // Keš 10 s, ali samo dok se sat telefona nije pomerio u odnosu na vreme od paljenja.
        if (trustAt >= 0 && el - trustAt <= 10000L && Math.abs(wall - trustWall - (el - trustAt)) < 1000L) return trusted;
        boolean ok;
        try {
            ok = TrustedClock.networkAgrees(wall, SystemClock.currentNetworkTimeClock().millis());
        } catch (Throwable t) {
            ok = false; // nema vremena sa mreže (DateTimeException) ili drugi problem
        }
        trusted = ok;
        trustAt = el;
        trustWall = wall;
        return ok;
    }

    private long trustAt = -1;
    private long trustWall;
    private boolean trusted;

    /**
     * Vremenska zona: svaka promena (i automatska, putovanje) važi tek od sledećih 06:00 po dosadašnjoj
     * zoni. Ručna zona se ne prati.
     */
    TimeZone zone() {
        synchronized (store) {
            // brz put: zona se ne razlikuje od sistemske i ništa ne čeka
            if (pendingZone == null && TimeZone.getDefault().getID().equals(zone.getID())) return zone;
            ZoneSwitch.State st = ZoneSwitch.next(zone.getID(), TimeZone.getDefault().getID(), autoZone(),
                    pendingZone, pendingAt, now(), zone);
            boolean same = st.zone.equals(zone.getID()) && java.util.Objects.equals(st.pending, pendingZone)
                    && st.pendingAt == pendingAt;
            if (!same) {
                zone = TimeZone.getTimeZone(st.zone);
                pendingZone = st.pending;
                pendingAt = st.pendingAt;
                saveClock();
            }
            return zone;
        }
    }

    boolean autoZone() {
        try {
            return Settings.Global.getInt(resolver, Settings.Global.AUTO_TIME_ZONE, 0) == 1;
        } catch (Throwable t) {
            return false;
        }
    }

    Calendar calendarNow() {
        Calendar c = Calendar.getInstance(zone());
        c.setTimeInMillis(now());
        return c;
    }
}
