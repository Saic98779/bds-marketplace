package com.sidbi.bds.procurement.dto;

import com.sidbi.bds.procurement.domain.ProcEnums;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RfqDtos {

    private RfqDtos() {
    }

    public record RfqRequest(
            @NotBlank @Size(min = 5, max = 200) String title,
            @NotNull Long categoryId,
            Long subcategoryId,
            @NotBlank @Size(min = 20, max = 4000) String description,
            @Size(max = 4000) String scopeOfWork,
            @Size(max = 4000) String deliverables,
            @Size(max = 150) String industry,
            @Size(max = 100) String projectState,
            @Size(max = 100) String projectDistrict,
            @Pattern(regexp = "^$|ONLINE|ONSITE|HYBRID", message = "Delivery mode must be ONLINE, ONSITE or HYBRID") String deliveryMode,
            @DecimalMin("0.0") BigDecimal budgetMin,
            @DecimalMin("0.0") BigDecimal budgetMax,
            @Size(max = 100) String expectedDuration,
            @Min(0) @Max(60) Integer requiredExperience,
            LocalDate expectedStartDate,
            @NotNull @Future(message = "Submission deadline must be in the future") LocalDateTime submissionDeadline,
            @NotNull ProcEnums.DistributionMode distributionMode,
            @Size(max = 25) List<Long> sellerIds,
            Long sourceListingId,
            Boolean publish
    ) {
    }

    public record InviteRequest(@NotEmpty @Size(max = 25) List<Long> sellerIds) {
    }

    public record ExtendDeadlineRequest(@NotNull @Future LocalDateTime submissionDeadline, @Size(max = 500) String remarks) {
    }

    public record DeclineRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record InvitationResponse(
            Long sellerId,
            String sellerName,
            ProcEnums.InvitationStatus status,
            String invitedBy,
            LocalDateTime invitedAt,
            LocalDateTime viewedAt,
            LocalDateTime respondedAt,
            String declineReason,
            Double sellerRating
    ) {
    }

    public record RfqSummary(
            Long id,
            String rfqNumber,
            String title,
            Long categoryId,
            String categoryName,
            String subcategoryName,
            Long buyerId,
            String buyerName,
            String projectState,
            String projectDistrict,
            String deliveryMode,
            BigDecimal budgetMin,
            BigDecimal budgetMax,
            LocalDateTime submissionDeadline,
            ProcEnums.RfqStatus status,
            ProcEnums.DistributionMode distributionMode,
            int invitedCount,
            long quotationCount,
            boolean deadlinePassed,
            ProcEnums.InvitationStatus myInvitationStatus,
            ProcEnums.QuotationStatus myQuotationStatus,
            Long myQuotationId,
            LocalDateTime createdAt,
            LocalDateTime publishedAt
    ) {
    }

    public record RfqDetail(
            RfqSummary summary,
            String description,
            String scopeOfWork,
            String deliverables,
            String industry,
            String expectedDuration,
            Integer requiredExperience,
            LocalDate expectedStartDate,
            Long sourceListingId,
            String sourceListingTitle,
            String statusRemarks,
            Long selectedQuotationId,
            List<DocumentResponse> attachments,
            List<InvitationResponse> invitations
    ) {
    }

    public record RecommendedSeller(Long sellerId, String name, String state, String district, Double rating, Integer ratingCount) {
    }

    public record DocumentResponse(
            Long id,
            ProcEnums.OwnerType ownerType,
            Long ownerId,
            String category,
            String originalName,
            String contentType,
            Long fileSize,
            String uploaderRole,
            String uploaderName,
            LocalDateTime uploadedAt
    ) {
    }
}
