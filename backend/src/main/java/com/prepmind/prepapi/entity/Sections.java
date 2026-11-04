package com.prepmind.prepapi.entity;

import java.util.List;
import java.util.Locale;

/** The four study sections. DSA links to coding problems; OOPS, OS and CN link to GeeksforGeeks articles. */
public final class Sections {

    public static final String DSA = "DSA";
    public static final String OOPS = "OOPS";
    public static final String OS = "OS";
    public static final String CN = "CN";

    public static final List<String> ALL = List.of(DSA, OOPS, OS, CN);

    private Sections() {
    }

    /** Upper-cases and validates; returns null for null/blank, throws for unknown values. */
    public static String normalize(String section) {
        if (section == null || section.isBlank()) {
            return null;
        }
        String s = section.trim().toUpperCase(Locale.ROOT);
        if (!ALL.contains(s)) {
            throw new IllegalArgumentException("Unknown section '" + section + "'. Use one of " + ALL);
        }
        return s;
    }
}
