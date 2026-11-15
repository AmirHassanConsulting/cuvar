package com.cuvar.app.rules;

/**
 * Cena otključavanja šifrom: najviše MAX (jedno) dnevno, pa nema više do sutra. Broji se i hitno
 * otključavanje (unlocksToday), pa i ono troši dnevnu šifru; hitno uvek dobija najtežu cenu. Bez Android klasa (tests/UnlockCostTest).
 */
public final class UnlockCost {
    /** Koliko otključavanja šifrom ima u jednom danu. */
    public static final int MAX = 1;

    public final int side;
    public final long timeMs;
    public final int answers;

    private UnlockCost(int side, long timeMs, int answers) {
        this.side = side;
        this.timeMs = timeMs;
        this.answers = answers;
    }

    /** Cena sledećeg otključavanja kad ih je danas već bilo done; null kad ih više nema. */
    public static UnlockCost next(int done) {
        if (done < 0) done = 0;
        if (done >= MAX) return null;
        return new UnlockCost(5, 35_000L, 1);
    }

    /** Hitno otključavanje ima svoje ograničenje, pa i posle MAX dobija najtežu igricu. */
    public static UnlockCost emergency(int done) {
        UnlockCost c = next(done);
        return c != null ? c : new UnlockCost(6, 42_000L, 3);
    }
}
