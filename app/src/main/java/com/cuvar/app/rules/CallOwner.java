package com.cuvar.app.rules;

/**
 * Samo aplikacija koja zove prolazi kroz blokadu dok traje poziv: prva koja otvori ekran poziva (ili prva
 * posle početka poziva) postaje vlasnik poziva. Bez Android klasa (tests/CallOwnerTest).
 */
public final class CallOwner {
    private String owner;

    /** Poziv je završen: vlasnik se briše. */
    public void reset() {
        owner = null;
    }

    /** Da li postoji vlasnik poziva. */
    public boolean active() {
        return owner != null;
    }

    /** callScreen: napred je ekran poziva; inCall: traje poziv (režim komunikacije). */
    public boolean passes(String pkg, boolean callScreen, boolean inCall) {
        if (callScreen) {
            owner = pkg;
            return true;
        }
        if (inCall) {
            if (owner == null) owner = pkg;
            return pkg.equals(owner);
        }
        owner = null;
        return false;
    }
}
