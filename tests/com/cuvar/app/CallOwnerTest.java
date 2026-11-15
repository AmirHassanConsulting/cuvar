package com.cuvar.app;

import com.cuvar.app.rules.CallOwner;

/** Tokom poziva prolazi samo aplikacija koja zove. */
public final class CallOwnerTest {
    private static int checks;

    public static void main(String[] args) {
        CallOwner o = new CallOwner();
        check(!o.passes("a", false, false), "Bez poziva ne prolazi");
        check(o.passes("a", true, true), "Ekran poziva prolazi i postaje vlasnik");
        check(o.passes("a", false, true), "Vlasnik prolazi dok traje poziv");
        check(!o.passes("b", false, true), "Druga aplikacija ne prolazi tokom tuđeg poziva");
        check(o.passes("a", false, true), "Vlasnik i dalje prolazi");
        check(!o.passes("a", false, false), "Kraj poziva");
        check(o.passes("b", false, true), "Prva aplikacija posle početka poziva postaje vlasnik");
        check(!o.passes("a", false, true), "Ostale ne prolaze");
        check(!o.passes("b", false, false) && !o.passes("a", false, false), "Posle poziva nema vlasnika");
        check(o.passes("c", true, false), "Ekran poziva bez režima komunikacije prolazi");
        CallOwner r = new CallOwner();
        check(r.passes("a", false, true) && r.active(), "Poziv A");
        r.reset();
        check(!r.active() && r.passes("b", false, true) && !r.passes("a", false, true), "Kraj bez upita, poziv B prolazi");
        System.out.println("CallOwnerTest: " + checks + " provera prošlo.");
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) throw new AssertionError(what);
    }
}
