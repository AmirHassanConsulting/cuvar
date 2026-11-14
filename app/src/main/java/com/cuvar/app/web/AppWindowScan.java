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
}
