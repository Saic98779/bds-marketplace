package com.sidbi.bds.auth.dto;

import com.sidbi.bds.auth.domain.Enums;

import java.time.LocalDateTime;
import java.util.List;

public final class ProviderDtos {

    private ProviderDtos() {
    }

    public record ProviderCard(
            Long id,
            String referenceNo,
            String organisationName,
            Enums.ProviderType providerType,
            String city,
            String state,
            String district,
            Integer yearsExperience,
            Integer teamSize,
            List<String> expertise,
            List<String> categoryNames,
            List<String> deliveryModes,
            Double averageRating,
            Integer ratingCount,
            boolean verified,
            boolean featured
    ) {
    }

    public record ProviderProfile(
            ProviderCard summary,
            String introduction,
            String website,
            Integer yearEstablished,
            Enums.Constitution constitution,
            String certifications,
            String teamProfile,
            String previousAssignments,
            List<String> industries,
            List<String> geographicCoverage,
            LocalDateTime verifiedAt
    ) {
    }
}
