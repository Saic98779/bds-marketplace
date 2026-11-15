package com.sidbi.bds.common.util;

public final class References {

    private References() {
    }

    public static String of(String prefix, Long id) {
        return "BDS-" + prefix + "-" + String.format("%06d", id);
    }
}
