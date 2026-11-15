package com.cuvar.app;

import com.cuvar.app.i18n.Lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/** Nemački prevod: de.tsv se učitava preko en.tsv, delovi se prevode, a red bez nemačkog ostaje engleski. */
public final class LangDeTest {
    private static final String EN = "app/src/main/assets/en.tsv";
    private static final String DE = "app/src/main/assets/de.tsv";
    private static int checks;

    public static void main(String[] args) throws Exception {
        Map<String, String> en = rows(EN), de = rows(DE);

        // Svaki nemački red mora da ima isti srpski tekst u en.tsv (inače je zastareo).
        int stale = 0;
        for (String sr : de.keySet()) {
            if (!en.containsKey(sr)) {
                stale++;
                System.out.println("Nemački red bez reda u en.tsv: " + sr);
            }
        }
        if (stale > 0) throw new AssertionError(stale + " redova u de.tsv nema u en.tsv");

        // Deo rečenice: razmak na početku i kraju mora da ostane, inače se reči slepe.
        for (Map.Entry<String, String> e : de.entrySet()) {
            String sr = e.getKey(), tr = e.getValue();
            if (sr.endsWith(" ") != tr.endsWith(" ")) {
                throw new AssertionError("Razmak na kraju se razlikuje: [" + sr + "] -> [" + tr + "]");
            }
        }

        try (Reader a = open(EN); Reader b = open(DE)) {
            Lang.load("de", a, b);
        }
        check(Lang.loaded("de") && !Lang.loaded("en"), "Učitan je nemački");
        eq("Sačuvaj", "Speichern");
        eq("Danas je utorak. Koji dan je bio pre 3 dana?", "Heute ist Dienstag. Welcher Tag war vor 3 Tagen?");
        eq("Koliko je 5 km u metrima?", "Wie viel sind 5 km in Metern?");
        eq("pon, sre, pet", "Mo, Mi, Fr");
        eq("Najduže u komadu, u minutima, pa pauza od 15 min (0 = bez)",
                "Längste Sitzung in Minuten, danach eine Pause von 15 min (0 = keine)");
        eq("WhatsApp", "WhatsApp");

        // Red koji postoji samo u en.tsv ostaje engleski, ne srpski.
        try (Reader a = new java.io.StringReader("Samo engleski\tOnly English\nSačuvaj\tSave\n");
             Reader b = new java.io.StringReader("Sačuvaj\tSpeichern\n")) {
            Lang.load("de", a, b);
        }
        eq("Samo engleski", "Only English");
        eq("Sačuvaj", "Speichern");

        int missing = 0;
        for (String sr : en.keySet()) if (!de.containsKey(sr)) missing++;
        System.out.println("LangDeTest: " + checks + " provera prošlo, bez nemačkog reda (ostaje engleski): " + missing);
    }

    private static Map<String, String> rows(String path) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (BufferedReader r = new BufferedReader(open(path))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                int tab = line.indexOf('\t');
                if (tab <= 0) continue;
                out.put(Lang.unescape(line.substring(0, tab)), Lang.unescape(line.substring(tab + 1)));
            }
        }
        return out;
    }

    private static Reader open(String path) throws IOException {
        return new InputStreamReader(Files.newInputStream(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private static void eq(String sr, String expected) {
        checks++;
        String got = Lang.translate(sr);
        if (!expected.equals(got)) throw new AssertionError(sr + "\n -> " + got + "\n očekivano " + expected);
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) throw new AssertionError(what);
    }
}
