package com.cuvar.app.web;

/** Da li ima prozora aplikacija osim onog koji je check() već pročitao (bez čitanja sadržaja). */
public final class AppWindowScan {
    private AppWindowScan() {
    }

    /**
     * appCount: broj prozora aplikacija; frontWindowId: pročitan prozor (negativno: nije pročitan nijedan
     * ili je napred naš ekran); onlyAppWindowId: id jedinog prozora aplikacije kad je appCount 1.
     * Jedan prozor koji nije pročitan (napred je sistemski prozor preko aplikacije, Xiaomi) se proverava.
     */
    public static boolean hasOther(int appCount, int frontWindowId, int onlyAppWindowId) {
        if (appCount >= 2) return true;
        if (appCount < 1 || frontWindowId < 0) return false;
        return onlyAppWindowId != frontWindowId;
    }

    /** Veliki sistemski prozor (plutajući „chat head“ proširen preko ekrana): površina bar 20% ekrana. */
    public static boolean largeOverlay(int w, int h, int screenW, int screenH) {
        if (w <= 0 || h <= 0 || screenW <= 0 || screenH <= 0) return false;
        return (long) w * h * 5L >= (long) screenW * screenH;
    }
}
