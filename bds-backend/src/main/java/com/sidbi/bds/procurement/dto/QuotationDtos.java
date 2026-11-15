package com.sidbi.bds.procurement.dto;

import com.sidbi.bds.procurement.domain.ProcEnums;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class QuotationDtos {

    private QuotationDtos() {
    }

    public record ItemRequest(
            @NotBlank @Size(max = 500) String description,
            @NotNull @DecimalMin("0.01") BigDecimal quantity,
            @Size(max = 40) String unit,
            @NotNull @DecimalMin("0.0") BigDecimal unitPrice
    ) {
    }

    public record QuotationRequest(
            @NotBlank @Size(min = 20, max = 4000) String technicalProposal,
            @Size(max = 4000) String methodology,
            Boolean scopeAccepted,
            @Size(max = 2000) String scopeDeviations,
            @NotBlank @Size(max = 4000) String deliverables,
            @NotNull @Min(1) @Max(1825) Integer timelineDays,
            @Size(max = 2000) String teamComposition,
            @Size(max = 2000) String relevantExperience,
            @DecimalMin("0.0") BigDecimal professionalFee,
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal taxPercent,
            @DecimalMin("0.0") BigDecimal otherExpenses,
            @Size(max = 2000) String paymentMilestones,
            @NotNull LocalDate validUntil,
            @Valid @Size(max = 30) List<ItemRequest> items,
            @Size(max = 1000) String changeSummary,
            Boolean submit
    ) {
    }

    public record ItemResponse(Long id, String description, BigDecimal quantity, String unit, BigDecimal unitPrice, BigDecimal amount) {
    }

    public record QuotationResponse(
            Long id,
            String quotationNumber,
            Long rfqId,
            String rfqNumber,
            String rfqTitle,
            ProcEnums.RfqStatus rfqStatus,
            LocalDateTime rfqDeadline,
            Long sellerId,
            String sellerName,
            Double sellerRating,
            Integer sellerRatingCount,
            Long buyerId,
            String buyerName,
            int versionNo,
            ProcEnums.QuotationStatus status,
            String technicalProposal,
            String methodology,
            boolean scopeAccepted,
            String scopeDeviations,
            String deliverables,
            Integer timelineDays,
            String teamComposition,
            String relevantExperience,
            BigDecimal professionalFee,
            BigDecimal taxPercent,
            BigDecimal taxAmount,
            BigDecimal otherExpenses,
            BigDecimal totalAmount,
            String paymentMilestones,
            LocalDate validUntil,
            LocalDateTime submittedAt,
            LocalDateTime decisionAt,
            String decisionRemarks,
            boolean shortlisted,
            boolean editable,
            Long engagementId,
            List<ItemResponse> items,
            List<RfqDtos.DocumentResponse> attachments,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record VersionResponse(Long id, int versionNo, BigDecimal totalAmount, String changeSummary, String snapshot, LocalDateTime createdAt) {
    }

    public record ClarificationRequest(@NotBlank @Size(max = 250) String subject, @NotBlank @Size(max = 4000) String message) {
    }

    public record AcceptRequest(
            @NotBlank @Size(max = 1000) String remarks,
            @DecimalMin("0.0") BigDecimal contractValue,
            LocalDate startDate,
            LocalDate expectedEndDate,
            @AssertTrue(message = "Please confirm the selection") Boolean confirmed
    ) {
    }

    public record CriterionRequest(@NotBlank @Size(max = 150) String name, @NotNull @Min(1) @Max(100) Integer weight) {
    }

    public record ScoreRequest(@NotNull Long quotationId, @NotNull @Min(0) Integer criterionIndex,
                               @NotNull @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal score) {
    }

    public record ComparisonRequest(@Valid @Size(min = 1, max = 12) List<CriterionRequest> criteria, @Valid List<ScoreRequest> scores) {
    }

    public record CriterionResponse(Long id, String name, int weight) {
    }

    public record ComparisonRow(
            Long quotationId,
            String quotationNumber,
            Long sellerId,
            String sellerName,
            Double sellerRating,
            ProcEnums.QuotationStatus status,
            boolean shortlisted,
            int versionNo,
            BigDecimal professionalFee,
            BigDecimal taxAmount,
            BigDecimal otherExpenses,
            BigDecimal totalAmount,
            Integer timelineDays,
            LocalDate validUntil,
            boolean scopeAccepted,
            String methodology,
            String deliverables,
            String relevantExperience,
            Map<Long, BigDecimal> scores,
            BigDecimal weightedScore,
            Integer rank,
            boolean lowestPrice,
            boolean shortestTimeline
    ) {
    }

    public record ComparisonResponse(
            RfqDtos.RfqSummary rfq,
            List<CriterionResponse> criteria,
            List<ComparisonRow> rows,
            String note
    ) {
    }
}
