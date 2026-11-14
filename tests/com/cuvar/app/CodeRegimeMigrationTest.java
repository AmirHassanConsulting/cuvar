package com.cuvar.app;

import com.cuvar.app.rules.CodeRegimeMigration;
import com.cuvar.app.rules.DailySchedule;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;

/** Režim sa šifrom van perioda postaje zaključavanje, bez popuštanja. */
public final class CodeRegimeMigrationTest {
    private static int checks;

    public static void main(String[] args) {
        Map<String, List<String>> links = new HashMap<>();
        links.put("com.youtube", Collections.singletonList("youtube.com"));
        links.put("com.insta", Collections.singletonList("instagram.com"));
        Function<String, Collection<String>> domainsOf = p -> links.getOrDefault(p, Collections.<String>emptyList());

        DailySchedule.Rule work = rule("w", true, true, Arrays.asList("com.game", "com.youtube", "com.insta"),
                Arrays.asList("reddit.com", "m.instagram.com"));
        DailySchedule.Rule off = rule("o", false, true, Collections.singletonList("com.off"), Collections.singletonList("off.com"));
        DailySchedule.Rule plain = rule("p", true, false, Collections.singletonList("com.plain"), Collections.singletonList("plain.com"));
        List<DailySchedule.Rule> rules = Arrays.asList(work, off, plain);
        Set<String> codeFree = Collections.singleton("com.insta");

        CodeRegimeMigration.Result r = CodeRegimeMigration.migrate(rules, codeFree,
                Arrays.asList("m.youtube.com", "other.com"), domainsOf);
        check(r.changed, "Bilo je režima sa šifrom");
        check(r.lockApps.equals(new HashSet<>(Arrays.asList("com.game", "com.youtube"))), "Zaključane aplikacije: " + r.lockApps);
        check(r.blockSites.equals(new HashSet<>(Arrays.asList("reddit.com", "m.youtube.com"))), "Blokirani sajtovi: " + r.blockSites);
        check(!work.code && !off.code && !plain.code, "Nijedan režim više nema šifru");
        check(!r.lockApps.contains("com.off") && !r.blockSites.contains("off.com"), "Isključen režim ne zaključava");
        check(!r.lockApps.contains("com.plain"), "Režim bez šifre ne zaključava");

        // Drugi put nema šta da se radi.
        CodeRegimeMigration.Result again = CodeRegimeMigration.migrate(rules, codeFree, Collections.<String>emptyList(), domainsOf);
        check(!again.changed && again.lockApps.isEmpty() && again.blockSites.isEmpty(), "Idempotentno");
        // Sajt koji već ima sopstveno pravilo (limit > 0) i režim ga zaključava: ostaje u blokiranim.
        DailySchedule.Rule lim = rule("l", true, true, Collections.<String>emptyList(), Collections.singletonList("limit.com"));
        CodeRegimeMigration.Result rl = CodeRegimeMigration.migrate(Collections.singletonList(lim), Collections.<String>emptySet(),
                Collections.singletonList("limit.com"), domainsOf);
        check(rl.blockSites.equals(Collections.singleton("limit.com")), "Sajt sa limitom postaje blokiran: " + rl.blockSites);

        // Obrnut poddomen: codeFree aplikacija ima m.youtube.com, režim youtube.com -> ostaje blokiran.
        links.put("com.mtube", Collections.singletonList("m.youtube.com"));
        DailySchedule.Rule yt = rule("y", true, true, Collections.<String>emptyList(), Collections.singletonList("youtube.com"));
        CodeRegimeMigration.Result ry = CodeRegimeMigration.migrate(Collections.singletonList(yt), Collections.singleton("com.mtube"),
                Collections.<String>emptyList(), domainsOf);
        check(ry.blockSites.equals(Collections.singleton("youtube.com")), "Širi sajt režima ostaje blokiran: " + ry.blockSites);
        // Uži sajt od domena slobodne aplikacije se oslobađa.
        DailySchedule.Rule yt2 = rule("y2", true, true, Collections.<String>emptyList(), Collections.singletonList("m.youtube.com"));
        CodeRegimeMigration.Result ry2 = CodeRegimeMigration.migrate(Collections.singletonList(yt2), Collections.singleton("com.youtube"),
                Collections.<String>emptyList(), domainsOf);
        check(ry2.blockSites.isEmpty(), "Poddomen slobodne aplikacije se ne blokira: " + ry2.blockSites);

        // Isključen režim ne zaključava, ali sajt zaključane aplikacije iz drugog režima se blokira.
        DailySchedule.Rule offSite = rule("os", false, true, Collections.<String>emptyList(), Collections.singletonList("instagram.com"));
        DailySchedule.Rule onApp = rule("oa", true, true, Collections.singletonList("com.insta"), Collections.<String>emptyList());
        CodeRegimeMigration.Result ro = CodeRegimeMigration.migrate(Arrays.asList(offSite, onApp), Collections.<String>emptySet(),
                Collections.singletonList("instagram.com"), domainsOf);
        check(ro.lockApps.equals(Collections.singleton("com.insta")), "Zaključana aplikacija: " + ro.lockApps);
        check(ro.blockSites.equals(Collections.singleton("instagram.com")), "Sajt zaključane aplikacije: " + ro.blockSites);
        check(!offSite.code, "Isključen režim gubi šifru");
        System.out.println("CodeRegimeMigrationTest OK (" + checks + ")");
    }

    private static DailySchedule.Rule rule(String id, boolean enabled, boolean code, List<String> apps, List<String> sites) {
        DailySchedule.Rule r = new DailySchedule.Rule(id, id, enabled, 9 * 60, 17 * 60);
        r.code = code;
        r.apps.addAll(apps);
        r.sites.addAll(sites);
        return r;
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) throw new AssertionError(what);
    }
}
