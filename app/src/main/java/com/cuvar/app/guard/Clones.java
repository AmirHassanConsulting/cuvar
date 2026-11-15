package com.cuvar.app.guard;

import java.util.Collection;
import java.util.Locale;

/** Kopije aplikacija i aplikacije koje ih prave, da se pravila ne zaobiđu kloniranjem. */
public final class Clones {
    /** Aplikacije koje pokreću kopije drugih aplikacija pod svojim imenom, pa ih Čuvar ne bi prepoznao. */
    public static final String[] CLONERS = {"com.lbe.parallel", "com.parallel.space", "com.excelliance.multiaccount",
            "com.excelliance.dualaid", "com.ludashi.dualspace", "com.ludashi.superclone", "com.polestar.multiaccount",
            "com.polestar.domultiple", "com.jumobile.multiapp", "com.applisto.appcloner", "com.dualspace", "com.multiple.account",
            "com.trendmicro.dualapps", "do.multiple.cloner", "com.cloneapp", "com.waxmoon.ma.gp", "com.oasisfeng.island",
            "com.lody.virtual", "io.va.exposed", "com.vmos"};

    private Clones() {
    }

    /** Grupe istih aplikacija: original, izmenjeni klijenti, Lite verzije i druge aplikacije za isti servis. */
    public static final String[][] SAME_APP = {
            {"com.google.android.youtube", "com.google.android.apps.youtube.mango", "com.google.android.apps.youtube.kids",
                    "app.revanced.android.youtube", "app.rvx.android.youtube", "com.vanced.android.youtube",
                    "org.schabi.newpipe", "org.polymorphicshade.newpipe", "org.polymorphicshade.tubular",
                    "InfinityLoop1309.NewPipeEnhanced", "com.github.libretube", "free.rm.skytube.oss",
                    "free.rm.skytube.extra", "com.futo.platformplayer"},
            {"com.google.android.apps.youtube.music", "app.revanced.android.apps.youtube.music",
                    "app.rvx.android.apps.youtube.music", "com.vanced.android.apps.youtube.music",
                    "it.vfsfitvnm.vimusic", "com.zionhuang.music"},
            {"com.instagram.android", "com.instagram.lite", "me.austinhuang.instagrabber"},
            {"com.facebook.katana", "com.facebook.lite", "com.pitchedapps.frost"},
            {"com.facebook.orca", "com.facebook.mlite"},
            {"com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.zhiliaoapp.musically.go", "com.tiktok.lite.go"},
            {"com.twitter.android", "com.twitter.android.lite"},
            {"com.reddit.frontpage", "ml.docilealligator.infinityforreddit", "com.rubenmayayo.reddit",
                    "com.laurencedawson.reddit_sync", "com.laurencedawson.reddit_sync.pro", "org.quantumbadger.redreader",
                    "free.reddit.news", "reddit.news", "me.ccrama.redditslide", "o.o.joey", "com.andrewshu.android.reddit",
                    "com.andrewshu.android.redditdonation", "com.onelouder.baconreader"},
            {"tv.twitch.android.app", "com.github.andreyasadchy.xtra"}};

    /**
     * Prvi član iste grupe koji ima pravilo (redom iz grupe), ili null kad paket nije u grupi,
     * kad ni jedan član nema pravilo ili kad sam paket ima pravilo.
     */
    public static String sameAppAs(String pkg, Collection<String> guarded) {
        if (pkg == null || guarded == null || guarded.contains(pkg)) return null;
        for (String[] group : SAME_APP) {
            boolean in = false;
            for (String m : group) if (m.equals(pkg)) in = true;
            if (!in) continue;
            for (String m : group) if (guarded.contains(m)) return m;
            return null;
        }
        return null;
    }

    /** Poznata aplikacija za kloniranje (po početku naziva paketa). */
    public static boolean isCloner(String pkg) {
        for (String c : CLONERS) if (pkg.startsWith(c)) return true;
        return false;
    }

    /** Kopija po paketu: paket originala produžen (com.instagram.android2). */
    public static boolean isPackageCopy(String pkg, String originalPkg) {
        return pkg.startsWith(originalPkg) && pkg.length() > originalPkg.length();
    }

    /**
     * Kopija po nazivu: naziv originala produžen brojem ili zagradom („Instagram 2“, „Instagram (1)“).
     * Kratki nazivi (manje od 4 slova) i nazivi jednaki paketu se ne porede, da ne bi bilo lažnih poklapanja.
     */
    public static boolean isLabelCopy(String label, String originalPkg, String originalLabel) {
        String l = label.toLowerCase(Locale.ROOT).trim();
        String gl = originalLabel.toLowerCase(Locale.ROOT).trim();
        return gl.length() >= 4 && !gl.equals(originalPkg) && l.length() > gl.length() && l.startsWith(gl)
                && l.substring(gl.length()).matches("[\\s\\d()+.-]+");
    }
}
