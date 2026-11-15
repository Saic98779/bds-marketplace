package com.sidbi.bds.common.security;

import java.util.Set;

public record AuthUser(
        Long userId,
        String name,
        Role role,
        String status,
        String referenceNo,
        Set<String> permissions
) {
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isSeller() {
        return role == Role.SELLER;
    }

    public boolean isBuyer() {
        return role == Role.BUYER;
    }

    public boolean has(Permission permission) {
        return isAdmin() || (permissions != null && permissions.contains(permission.name()));
    }
}
