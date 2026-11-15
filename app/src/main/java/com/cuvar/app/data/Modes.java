package com.cuvar.app.data;

import com.cuvar.app.rules.ItemKey;
import com.cuvar.app.rules.DailySchedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Vremenski režimi: podešeni i oni koji sada važe. */
public final class Modes {
    private final Store store;

    Modes(Store store) {
        this.store = store;
    }

    final List<DailySchedule.Rule> schedules = new ArrayList<>();

    /** Prebacuje stari jedini režim (pre više režima) u prvi režim liste, bez gubitka izbora. */
    void migrateSchedule() {
        if (store.sp.contains("schedules")) {
            return;
        }
        DailySchedule.Rule first = new DailySchedule.Rule(newScheduleId(), "Režim 1",
                store.sp.getBoolean("scheduleEnabled", false),
                store.sp.getInt("scheduleStart", 21 * 60), store.sp.getInt("scheduleEnd", 9 * 60));
        for (String pkg : Store.keysOf(store.apps)) {
            JSONObject o = store.apps.optJSONObject(pkg);
            if (o == null || !o.has("scheduled")) continue;
            if (o.optBoolean("scheduled", false)) first.apps.add(pkg);
            o.remove("scheduled");
            if (!Store.appRule(o).any()) store.apps.remove(pkg);
        }
        first.sites.addAll(store.sp.getStringSet("scheduledSites", Collections.emptySet()));
        boolean hadOld = store.sp.contains("scheduleEnabled") || !first.apps.isEmpty() || !first.sites.isEmpty();
        if (hadOld) schedules.add(first);
        store.sp.edit().putString("apps", store.apps.toString()).putString("schedules", schedulesJson())
                .remove("scheduleEnabled").remove("scheduleStart").remove("scheduleEnd")
                .remove("scheduledSites").apply();
    }

    void loadSchedules() {
        parseSchedules(store.sp.getString("schedules", "[]"), schedules);
    }

    static void parseSchedules(String json, List<DailySchedule.Rule> schedules) {
        schedules.clear();
        JSONArray arr;
        try {
            arr = new JSONArray(json);
        } catch (JSONException e) {
            arr = new JSONArray();
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            schedules.add(parseRule(o, i));
        }
    }

    /** Jedan režim iz JSON-a; i je redni broj za podrazumevani naziv. */
    static DailySchedule.Rule parseRule(JSONObject o, int i) {
        DailySchedule.Rule r = new DailySchedule.Rule(o.optString("id", newScheduleId()),
                o.optString("name", "Režim " + (i + 1)), o.optBoolean("enabled", false),
                o.optInt("start", 21 * 60), o.optInt("end", 9 * 60));
        r.days = o.optInt("days", DailySchedule.ALL_DAYS);
        r.code = o.optBoolean("code", false); // stari režim sa šifrom, samo za migraciju (Store.migrateCodeRegimes)
        JSONArray a = o.optJSONArray("apps");
        for (int k = 0; a != null && k < a.length(); k++) r.apps.add(a.optString(k));
        JSONArray s = o.optJSONArray("sites");
        for (int k = 0; s != null && k < s.length(); k++) r.sites.add(s.optString(k));
        return r;
    }

    String schedulesJson() {
        return schedulesJson(schedules);
    }

    static String schedulesJson(List<DailySchedule.Rule> schedules) {
        JSONArray arr = new JSONArray();
        for (DailySchedule.Rule r : schedules) {
            arr.put(ruleJson(r));
        }
        return arr.toString();
    }

    /** Režim kao JSON (isti zapis kao u "schedules"). */
    static JSONObject ruleJson(DailySchedule.Rule r) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", r.id);
            o.put("name", r.name);
            o.put("enabled", r.enabled);
            o.put("start", r.start);
            o.put("end", r.end);
            o.put("days", r.days);
            o.put("code", r.code);
            o.put("apps", new JSONArray(r.apps));
            o.put("sites", new JSONArray(r.sites));
        } catch (JSONException ignored) {
            // put sa ispravnim ključem i vrednošću ne baca grešku; podaci ostaju kakvi su bili
        }
        return o;
    }

    void saveSchedules() {
        store.sp.edit().putString("schedules", schedulesJson()).apply();
        store.enforce();
    }

    static String newScheduleId() {
        return Long.toString(System.currentTimeMillis(), 36) + Integer.toString((int) (Math.random() * 1296), 36);
    }

    /** Režim sa tim id-jem, ili null. */
    DailySchedule.Rule rule(String id) {
        for (DailySchedule.Rule r : schedules) if (r.id.equals(id)) return r;
        return null;
    }

    static DailySchedule.Rule copy(DailySchedule.Rule r) {
        return DailySchedule.copy(r);
    }

    /** Kopije režima, da ih ekran i servis ne menjaju mimo Store-a. */
    public List<DailySchedule.Rule> schedules() {
        synchronized (store) {
            List<DailySchedule.Rule> out = new ArrayList<>();
            for (DailySchedule.Rule r : schedules) out.add(copy(r));
            return out;
        }
    }

    /** Važeći periodi; jedan izmenjen režim do jutra može imati više segmenata sa istim id-em. */
    public List<DailySchedule.Rule> schedulesNow() {
        synchronized (store) {
            List<DailySchedule.Rule> out = new ArrayList<>();
            for (DailySchedule.Rule r : store.enforcedRaw()) out.add(copy(r));
            return out;
        }
    }

    public DailySchedule.Rule schedule(String id) {
        synchronized (store) {
            DailySchedule.Rule r = rule(id);
            return r == null ? null : copy(r);
        }
    }

    /** Gotov noćni režim: uključen, bez otključavanja, aplikacije bira korisnik. */
    public static final String NIGHT_NAME = "Noćno zaključavanje";

    public DailySchedule.Rule addNightSchedule() {
        synchronized (store) {
            DailySchedule.Rule r = new DailySchedule.Rule(newScheduleId(), NIGHT_NAME, true, 22 * 60, 6 * 60);
            schedules.add(r);
            saveSchedules();
            return copy(r);
        }
    }

    public boolean hasNightSchedule() {
        synchronized (store) {
            for (DailySchedule.Rule r : schedules) if (r.name.equals(NIGHT_NAME)) return true;
            return false;
        }
    }

    /** Gotovo radno vreme: svakog dana 09:00–17:00 bez otključavanja; van perioda ne blokira. */
    public static final String WORK_NAME = "Radno vreme";

    public DailySchedule.Rule addWorkSchedule() {
        synchronized (store) {
            DailySchedule.Rule r = new DailySchedule.Rule(newScheduleId(), WORK_NAME, true, 9 * 60, 17 * 60);
            schedules.add(r);
            saveSchedules();
            return copy(r);
        }
    }

    /** Jednom: postojeće radno vreme sa samo radnim danima važi i vikendom (dodaje dane, ne slabi režim). */
    void workEveryDay() {
        if (store.sp.getBoolean("workEveryDay", false)) return;
        boolean changed = false;
        for (DailySchedule.Rule r : schedules) {
            if (r.name.equals(WORK_NAME) && r.days == DailySchedule.WORK_DAYS) {
                r.days = DailySchedule.ALL_DAYS;
                changed = true;
            }
        }
        if (changed) saveSchedules();
        store.sp.edit().putBoolean("workEveryDay", true).apply();
    }

    public boolean hasWorkSchedule() {
        synchronized (store) {
            for (DailySchedule.Rule r : schedules) if (r.name.equals(WORK_NAME)) return true;
            return false;
        }
    }

    public DailySchedule.Rule addSchedule() {
        synchronized (store) {
            int n = schedules.size() + 1;
            while (true) {
                boolean taken = false;
                for (DailySchedule.Rule r : schedules) if (r.name.equals("Režim " + n)) taken = true;
                if (!taken) break;
                n++;
            }
            DailySchedule.Rule r = new DailySchedule.Rule(newScheduleId(), "Režim " + n, false, 21 * 60, 9 * 60);
            schedules.add(r);
            saveSchedules();
            return copy(r);
        }
    }

    /** Da li režim upravo traje (za prikaz). */
    public boolean scheduleActive(String id) {
        synchronized (store) {
            Calendar now = store.clock.calendarNow();
            for (DailySchedule.Rule r : store.enforced()) {
                if (r.id.equals(id) && r.active(minuteOf(now), dayOf(now))) return true;
            }
            return false;
        }
    }

    /** Podešeni režimi u pref, bez enforce() (to radi Store.change). */
    private void writeSchedules() {
        store.sp.edit().putString("schedules", schedulesJson()).apply();
    }

    /**
     * Briše režim. Kao svako popuštanje (Store.change): van 06:00–10:00 vraća false i ništa ne menja,
     * inače staro važi do sutra u 06:00.
     */
    public boolean removeSchedule(String id) {
        synchronized (store) {
            DailySchedule.Rule r = rule(id);
            if (r == null) return true;
            return store.change(() -> {
                schedules.remove(r);
                writeSchedules();
            });
        }
    }

    /** Vraća false (i ništa ne menja) ako je izmena popuštanje van 06:00–10:00. Dodavanje i naziv prolaze uvek. */
    public boolean setSchedule(String id, String name, boolean enabled, int start, int end) {
        synchronized (store) {
            DailySchedule.Rule r = rule(id);
            if (r == null) return true;
            return store.change(() -> {
                r.name = name;
                r.enabled = enabled;
                r.start = start;
                r.end = end;
                writeSchedules();
            });
        }
    }

    public boolean setScheduleDays(String id, int days) {
        synchronized (store) {
            DailySchedule.Rule r = rule(id);
            if (r == null) return true;
            return store.change(() -> {
                r.days = days;
                writeSchedules();
            });
        }
    }

    public boolean setScheduleApp(String id, String pkg, boolean selected) {
        synchronized (store) {
            DailySchedule.Rule r = rule(id);
            if (r == null) return true;
            return store.change(() -> {
                if (selected) r.apps.add(pkg); else r.apps.remove(pkg);
                writeSchedules();
            });
        }
    }

    public boolean setScheduleSite(String id, String domain, boolean selected) {
        synchronized (store) {
            DailySchedule.Rule r = rule(id);
            if (r == null) return true;
            return store.change(() -> {
                if (selected) r.sites.add(domain); else r.sites.remove(domain);
                writeSchedules();
            });
        }
    }

    static int minuteOf(Calendar c) {
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    /** 0 = ponedeljak … 6 = nedelja. */
    static int dayOf(Calendar c) {
        return (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }

    /** {minut u danu, dan u nedelji (0 = ponedeljak)} po pouzdanom satu. */
    public int[] minuteAndDay() {
        synchronized (store) {
            Calendar now = store.clock.calendarNow();
            return new int[]{minuteOf(now), dayOf(now)};
        }
    }

    /** Režim koji trenutno blokira aplikaciju, ili null. */
    public DailySchedule.Rule scheduleBlockingApp(String pkg) {
        synchronized (store) {
            Calendar now = store.clock.calendarNow();
            DailySchedule.Rule r = DailySchedule.blockingApp(store.enforced(), pkg, minuteOf(now), dayOf(now));
            return r == null ? null : copy(r);
        }
    }

    /** Režim koji trenutno blokira host (i poddomene), ili null. */
    /**
     * Da li je neki sajt sada blokiran bez otključavanja: uključen režim sa sajtovima upravo traje
     * ili je dnevni limit nekog sajta potrošen. Tada se ni pregledač u kome Čuvar ne vidi sajt ne otključava.
     */
    public boolean hardSiteBlockNow() {
        synchronized (store) {
            Calendar now = store.clock.calendarNow();
            int m = minuteOf(now), d = dayOf(now);
            for (DailySchedule.Rule r : store.enforced()) if (!r.sites.isEmpty() && r.active(m, d)) return true;
            for (String dom : store.siteKeysNow()) {
                int limit = store.siteLimitNow(dom);
                if (limit > 0 && store.usedShared(ItemKey.site(dom)) >= limit * 60000L) return true;
            }
            return false;
        }
    }

    public DailySchedule.Rule scheduleBlockingSite(String host) {
        synchronized (store) {
            Calendar now = store.clock.calendarNow();
            DailySchedule.Rule r = DailySchedule.blockingSite(store.enforced(), host, minuteOf(now), dayOf(now));
            return r == null ? null : copy(r);
        }
    }

    /** Režimi koji upravo traju (bez obzira da li imaju aplikacije ili sajtove). */
    public List<DailySchedule.Rule> activeSchedules() {
        synchronized (store) {
            Calendar now = store.clock.calendarNow();
            List<DailySchedule.Rule> out = new ArrayList<>();
            for (DailySchedule.Rule r : store.enforcedRaw()) {
                if (r.active(minuteOf(now), dayOf(now))) out.add(copy(r));
            }
            return out;
        }
    }
}
