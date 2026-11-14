package com.cuvar.app.rules;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Jednom: režim „van perioda traži dnevnu šifru“ više ne postoji. Ono što je on držao zaključanim van
 * svog vremena postaje obično zaključavanje (aplikacija) ili stalna blokada (sajt), pa se ništa ne olakšava.
 * Isključen režim sa šifrom samo gubi šifru, jer ni pre nije zaključavao.
 */
public final class CodeRegimeMigration {
    private CodeRegimeMigration() {
    }

    /** Šta treba zaključati: aplikacije (lock) i sajtove (limit 0 = uvek blokiran). */
    public static final class Result {
        public final Set<String> lockApps = new HashSet<>();
        public final Set<String> blockSites = new HashSet<>();
        /** Da li je bilo režima sa šifrom (posle poziva ih više nema). */
        public boolean changed;
    }

    /**
     * Menja režime na mestu (code = false) i vraća šta treba zaključati.
     *
     * @param schedules režimi (podešeni ili važeći)
     * @param codeFree  aplikacije koje su van režima bile slobodne (sa njima i njihov povezan sajt)
     * @param siteKeys  sajtovi sa sopstvenim pravilom; onaj koji pokriva sajt zaključane aplikacije i sam se blokira
     * @param domainsOf povezani sajtovi aplikacije
     */
    public static Result migrate(List<DailySchedule.Rule> schedules, Set<String> codeFree, Collection<String> siteKeys,
                                 Function<String, ? extends Collection<String>> domainsOf) {
        Result out = new Result();
        for (DailySchedule.Rule r : schedules) {
            if (!r.code) continue;
            out.changed = true;
            r.code = false;
            if (!r.enabled) continue;
            for (String pkg : r.apps) if (!codeFree.contains(pkg)) out.lockApps.add(pkg);
            for (String site : r.sites) if (!freed(site, codeFree, domainsOf)) out.blockSites.add(site);
        }
        // Kao „Zaključaj“ u kartici aplikacije: i sopstveno pravilo njenog sajta postaje stalna blokada.
        for (String pkg : out.lockApps) {
            for (String base : domainsOf.apply(pkg)) {
                for (String k : siteKeys) if (covers(base, k)) out.blockSites.add(k);
            }
        }
        return out;
    }

    /** Sajt aplikacije koja je van režima bila slobodna bio je slobodan i sam. */
    private static boolean freed(String site, Set<String> codeFree, Function<String, ? extends Collection<String>> domainsOf) {
        for (String pkg : codeFree) {
            for (String d : domainsOf.apply(pkg)) if (covers(d, site)) return true;
        }
        return false;
    }

    private static boolean covers(String base, String domain) {
        return DailySchedule.matchDomain(domain, Collections.singleton(base)) != null;
    }
}
