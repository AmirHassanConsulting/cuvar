package com.cuvar.app.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Sklapa spisak imena: "A, B i C"; preko 4 imena prva 3 pa "i još N". */
public final class NameList {
    private NameList() {}

    public static final int MAX = 4, SHOWN = 3;

    /** Imena se sortiraju (stabilan redosled); {@code and} i {@code more} su veznici (" i ", " i još "). */
    public static String join(Collection<String> names, String and, String more) {
        List<String> sorted = new ArrayList<>(names);
        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        StringBuilder sb = new StringBuilder();
        if (sorted.size() > MAX) {
            for (int i = 0; i < SHOWN; i++) sb.append(i > 0 ? ", " : "").append(sorted.get(i));
            return sb.append(more).append(sorted.size() - SHOWN).toString();
        }
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) sb.append(i == sorted.size() - 1 ? and : ", ");
            sb.append(sorted.get(i));
        }
        return sb.toString();
    }
}
