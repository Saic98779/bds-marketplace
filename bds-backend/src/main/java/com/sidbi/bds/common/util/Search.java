package com.sidbi.bds.common.util;

import java.util.Locale;

public final class Search {

    public static final char ESCAPE = '!';

    private Search() {
    }

    public static String contains(String value) {
        return "%" + escape(value.trim().toLowerCase(Locale.ROOT)) + "%";
    }

    public static String containsExact(String value) {
        return "%" + escape(value.trim()) + "%";
    }

    public static String escape(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
