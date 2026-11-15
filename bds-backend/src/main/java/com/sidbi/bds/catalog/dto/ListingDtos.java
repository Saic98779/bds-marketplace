package com.sidbi.bds.catalog.dto;

import com.sidbi.bds.catalog.domain.CatalogEnums;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ListingDtos {

    private ListingDtos() {
    }

    public record ListingRequest(
            @NotBlank @Size(min = 5, max = 200) String title,
            @NotNull Long categoryId,
            Long subcategoryId,
            @Size(max = 500) String shortDescription,
            @NotBlank @Size(min = 20, max = 4000) String description,
            @Size(max = 4000) String scopeOfWork,
            @Size(max = 4000) String deliverables,
            @Size(max = 4000) String methodology,
            @Size(max = 20) List<String> targetIndustries,
            @NotNull CatalogEnums.DeliveryMode deliveryMode,
            @Size(max = 40) List<String> serviceLocations,
            @Size(max = 100) String state,
            @Size(max = 100) String district,
            @Min(0) @Max(80) Integer experienceYears,
            @Size(max = 100) String estimatedDuration,
            @DecimalMin("0.0") @DecimalMax("9999999999.99") BigDecimal indicativeFee,
            @NotNull CatalogEnums.PricingModel pricingModel,
            @Size(max = 15) List<String> tags,
            Boolean submit
    ) {
    }

    public record AdminListingRequest(@NotNull Long sellerId, @NotNull @Valid ListingRequest listing) {
    }

    public record AttachmentResponse(Long id, String label, String originalName, String contentType, Long fileSize) {
    }

    public record ListingCard(
            Long id,
            String referenceNo,
            String title,
            String shortDescription,
            Long categoryId,
            String categoryName,
            String categoryIcon,
            Long subcategoryId,
            String subcategoryName,
            Long sellerId,
            String sellerName,
            String sellerState,
            Double sellerRating,
            Integer sellerRatingCount,
            boolean sellerVerified,
            CatalogEnums.DeliveryMode deliveryMode,
            String state,
            Integer experienceYears,
            String estimatedDuration,
            BigDecimal indicativeFee,
            CatalogEnums.PricingModel pricingModel,
            CatalogEnums.ListingStatus status,
            boolean featured,
            LocalDateTime publishedAt,
            LocalDateTime updatedAt
    ) {
    }

    public record ListingDetail(
            ListingCard summary,
            String description,
            String scopeOfWork,
            String deliverables,
            String methodology,
            List<String> targetIndustries,
            List<String> serviceLocations,
            String district,
            List<String> tags,
            List<AttachmentResponse> attachments,
            String moderationRemarks,
            long viewCount,
            long enquiryCount,
            LocalDateTime submittedAt,
            LocalDateTime createdAt,
            boolean publiclyVisible
    ) {
    }

    public record ModerationResponse(
            Long id,
            CatalogEnums.ModerationAction action,
            CatalogEnums.ListingStatus fromStatus,
            CatalogEnums.ListingStatus toStatus,
            String remarks,
            String actorName,
            String actorRole,
            LocalDateTime createdAt
    ) {
    }

    public record SellerListingStats(long total, long published, long pending, long views, long enquiries, Map<String, Long> byStatus) {
    }

    public record CountItem(String label, long count) {
    }

    public record CatalogStats(
            long totalListings,
            long publicListings,
            long pendingModeration,
            long activeProviders,
            Map<String, Long> byStatus,
            List<CountItem> byCategory
    ) {
    }

    public record PublicStats(long services, long providers, long categories) {
    }
}
