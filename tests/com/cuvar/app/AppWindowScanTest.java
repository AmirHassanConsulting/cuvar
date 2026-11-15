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
        // Veliki sistemski prozor (plutajući): bar 20% površine ekrana.
        check(AppWindowScan.largeOverlay(1080, 2400, 1080, 2400), "Ceo ekran je veliki prozor");
        check(AppWindowScan.largeOverlay(1080, 480, 1080, 2400), "Tačno 20% je veliki prozor");
        check(!AppWindowScan.largeOverlay(1080, 470, 1080, 2400), "Statusna traka ili navigacija nisu veliki prozor");
        check(!AppWindowScan.largeOverlay(150, 150, 1080, 2400), "Skupljen chat head nije veliki prozor");
        check(!AppWindowScan.largeOverlay(0, 0, 1080, 2400) && !AppWindowScan.largeOverlay(500, 500, 0, 0), "Prazne mere");
        System.out.println("AppWindowScanTest: " + checks + " provera prošlo.");
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) throw new AssertionError(what);
    }
}
