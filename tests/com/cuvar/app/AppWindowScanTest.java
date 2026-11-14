package com.cuvar.app;

import com.cuvar.app.web.AppWindowScan;

/** Drugi prozori aplikacija se čitaju samo kad postoji prozor koji check() nije pročitao. */
public final class AppWindowScanTest {
    private static int checks;

    public static void main(String[] args) {
        check(!AppWindowScan.hasOther(0, 7, -1), "Nema prozora aplikacija: ništa");
        check(!AppWindowScan.hasOther(1, 7, 7), "Jedini prozor je već pročitan (uobičajeno): ništa");
        check(AppWindowScan.hasOther(1, 9, 7), "Napred sistemski prozor preko aplikacije: aplikacija se proverava");
        check(!AppWindowScan.hasOther(1, -1, 7), "Ništa nije pročitano: sa jednim prozorom kao ranije");
        check(!AppWindowScan.hasOther(1, -2, 7), "Naš ekran napred ili paket od Androida: kao ranije");
        check(AppWindowScan.hasOther(2, 7, -1), "Podeljen ekran: proverava se");
        check(AppWindowScan.hasOther(2, -1, -1), "Podeljen ekran bez pročitanog prozora: proverava se");
        System.out.println("AppWindowScanTest: " + checks + " provera prošlo.");
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) throw new AssertionError(what);
    }
}
