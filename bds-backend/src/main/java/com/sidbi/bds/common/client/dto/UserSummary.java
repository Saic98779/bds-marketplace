package com.sidbi.bds.common.client.dto;

import java.util.Set;

public record UserSummary(
        Long id,
        String name,
        String organisationName,
        String role,
        String status,
        String referenceNo,
        String state,
        String district,
        Double averageRating,
        Integer ratingCount,
        Set<String> permissions
) {
    public boolean active() {
        return "ACTIVE".equals(status);
    }

    public String displayName() {
        return organisationName != null && !organisationName.isBlank() ? organisationName : name;
    }
}
