package com.cuvar.app;

import com.cuvar.app.rules.NameList;
import java.util.Arrays;
import java.util.Collections;

/** Spisak imena u zakazanim promenama. */
public final class NameListTest {
    private static int checks;

    public static void main(String[] args) {
        check(NameList.join(Collections.<String>emptyList(), " i ", " i još ").isEmpty(), "Prazno");
        check(NameList.join(Arrays.asList("WhatsApp"), " i ", " i još ").equals("WhatsApp"), "Jedno");
        check(NameList.join(Arrays.asList("b.com", "Alfa"), " i ", " i još ").equals("Alfa i b.com"), "Dva, sortirano");
        check(NameList.join(Arrays.asList("C", "A", "B", "D"), " i ", " i još ").equals("A, B, C i D"), "Četiri");
        check(NameList.join(Arrays.asList("E", "C", "A", "B", "D", "F"), " i ", " i još ").equals("A, B, C i još 3"), "Šest");
        System.out.println("NameListTest OK (" + checks + ")");
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) throw new AssertionError(what);
    }
}
