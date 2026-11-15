package com.sidbi.bds.auth.service;

import com.sidbi.bds.common.exception.BusinessException;

import java.security.SecureRandom;

public final class PasswordPolicy {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "@#$%&*!";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.length() < 8 || password.length() > 64
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*")
                || !password.matches(".*[^A-Za-z0-9].*")) {
            throw BusinessException.badRequest(
                    "Password must be 8-64 characters and include upper case, lower case, a number and a special character");
        }
    }

    public static String generate() {
        StringBuilder builder = new StringBuilder();
        builder.append(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        builder.append(SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length())));
        String all = UPPER + LOWER + DIGITS;
        for (int i = 0; i < 8; i++) {
            builder.append(all.charAt(RANDOM.nextInt(all.length())));
        }
        builder.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        builder.append(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        return builder.toString();
    }
}
