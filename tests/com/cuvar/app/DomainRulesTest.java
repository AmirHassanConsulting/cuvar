package com.cuvar.app;

import com.cuvar.app.rules.DomainRules;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DomainRulesTest {
    public static void main(String[] args) {
        List<String> rules = Arrays.asList("facebook.com", "m.facebook.com", "video.m.facebook.com", "other.com");
        expect(DomainRules.matching("video.m.facebook.com", rules),
                "video.m.facebook.com", "m.facebook.com", "facebook.com");
        expect(DomainRules.matching("a.video.m.facebook.com", rules),
                "video.m.facebook.com", "m.facebook.com", "facebook.com");
        expect(DomainRules.matching("facebook.com", rules), "facebook.com");
        expect(DomainRules.matching("notfacebook.com", rules));
        expect(DomainRules.matching("facebook.com.evil.example", rules));
        expect(DomainRules.matching(null, rules));
        expect(DomainRules.matching("facebook.com", Collections.emptyList()));

        // Dodavanje blažeg poddomena ne sme da skloni blokadu niti potrošeni limit roditelja.
        List<String> before = DomainRules.matching("m.facebook.com", Arrays.asList("facebook.com"));
        List<String> after = DomainRules.matching("m.facebook.com", rules);
        if (!after.containsAll(before)) throw new AssertionError("Roditeljsko pravilo je izgubljeno");

        // Sajt van ukupnog limita (dayFree "site:domen") se preskače; ostaje sledeći roditelj ili null.
        List<String> m = DomainRules.matching("m.facebook.com", rules);
        java.util.Set<String> free = new java.util.HashSet<>(Arrays.asList("site:m.facebook.com", "com.facebook.katana"));
        check("facebook.com".equals(DomainRules.firstNotFree(m, free)), "Roditelj ostaje pod limitom");
        free.add("site:facebook.com");
        check(DomainRules.firstNotFree(m, free) == null, "Sve izuzeto");
        check("m.facebook.com".equals(DomainRules.firstNotFree(m, Collections.emptySet())), "Bez izuzetka najuži");
        System.out.println("DomainRulesTest: prošlo 11 provera.");
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    private static void expect(List<String> actual, String... expected) {
        if (!actual.equals(Arrays.asList(expected))) throw new AssertionError(actual + " != " + Arrays.toString(expected));
    }
}
