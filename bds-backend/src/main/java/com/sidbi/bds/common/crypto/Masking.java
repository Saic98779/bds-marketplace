package com.sidbi.bds.common.crypto;

public final class Masking {

    private Masking() {
    }

    public static String mask(String value, int visibleStart, int visibleEnd) {
        if (value == null || value.isBlank()) {
            return value;
        }
        int length = value.length();
        if (length <= visibleStart + visibleEnd) {
            return "*".repeat(length);
        }
        return value.substring(0, visibleStart)
                + "*".repeat(length - visibleStart - visibleEnd)
                + value.substring(length - visibleEnd);
    }

    public static String pan(String value) {
        return mask(value, 2, 2);
    }

    public static String gstin(String value) {
        return mask(value, 2, 3);
    }

    public static String mobile(String value) {
        return mask(value, 2, 2);
    }

    public static String email(String value) {
        if (value == null || !value.contains("@")) {
            return mask(value, 1, 1);
        }
        int at = value.indexOf('@');
        String local = value.substring(0, at);
        return mask(local, 1, local.length() > 3 ? 1 : 0) + value.substring(at);
    }

    public static String generic(String value) {
        return mask(value, 2, 2);
    }
}
