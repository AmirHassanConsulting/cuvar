package com.cuvar.app;

import com.cuvar.app.guard.Clones;

/** Kopije aplikacija potpadaju pod pravila originala. */
public final class ClonesTest {
    private static int checks;

    public static void main(String[] args) {
        check(Clones.isCloner("com.lbe.parallel.intl"), "Parallel Space");
        check(Clones.isCloner("com.applisto.appcloner"), "App Cloner");
        check(!Clones.isCloner("com.instagram.android"), "Instagram nije kloner");

        String ig = "com.instagram.android";
        check(Clones.isPackageCopy("com.instagram.android2", ig), "Produžen paket");
        check(!Clones.isPackageCopy(ig, ig), "Isti paket nije kopija");
        check(!Clones.isPackageCopy("com.instagram.lite", ig), "Drugi paket");

        check(Clones.isLabelCopy("Instagram 2", ig, "Instagram"), "Naziv sa brojem");
        check(Clones.isLabelCopy("Instagram (1)", ig, "Instagram"), "Naziv sa zagradom");
        check(Clones.isLabelCopy(" instagram+ ", ig, "Instagram"), "Mala slova i razmaci");
        check(!Clones.isLabelCopy("Instagram Lite", ig, "Instagram"), "Druga aplikacija sa sličnim nazivom");
        check(!Clones.isLabelCopy("Instagram", ig, "Instagram"), "Isti naziv nije kopija");
        check(!Clones.isLabelCopy("X 2", "com.twitter.android", "X"), "Kratak naziv se ne poredi");
        check(!Clones.isLabelCopy("com.foo.bar2", "com.foo.bar", "com.foo.bar"), "Naziv jednak paketu se ne poredi");
        // Grupe istih aplikacija.
        java.util.Set<String> yt = new java.util.HashSet<>(java.util.Arrays.asList("com.google.android.youtube"));
        check("com.google.android.youtube".equals(Clones.sameAppAs("org.schabi.newpipe", yt)), "NewPipe vodi na YouTube");
        check(Clones.sameAppAs("com.google.android.youtube", yt) == null, "Original sa pravilom ostaje svoj");
        check(Clones.sameAppAs("com.instagram.android", yt) == null, "Druga grupa nema pravilo");
        check(Clones.sameAppAs("com.whatsapp", yt) == null, "Paket van grupa");
        java.util.Set<String> lite = new java.util.HashSet<>(java.util.Arrays.asList("com.instagram.lite"));
        check("com.instagram.lite".equals(Clones.sameAppAs("com.instagram.android", lite)), "Original vodi na Lite sa pravilom");
        java.util.Set<String> two = new java.util.LinkedHashSet<>(java.util.Arrays.asList("org.schabi.newpipe", "com.github.libretube"));
        check("org.schabi.newpipe".equals(Clones.sameAppAs("com.google.android.youtube", two)), "Prvi po redu iz grupe");
        check(Clones.sameAppAs("org.schabi.newpipe", two) == null, "Sam ima pravilo");
        check(Clones.sameAppAs(null, yt) == null && Clones.sameAppAs("a", null) == null, "null");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String[] group : Clones.SAME_APP) {
            for (String m : group) check(seen.add(m), "Paket je samo u jednoj grupi: " + m);
        }
        System.out.println("ClonesTest: " + checks + " provera prošlo.");
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) throw new AssertionError(what);
    }
}
