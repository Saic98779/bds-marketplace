package com.sidbi.bds.procurement.dto;

import com.sidbi.bds.procurement.domain.ProcEnums;
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

public final class EngagementDtos {

    private EngagementDtos() {
    }

    public record WorkOrderRequestRequest(@Size(max = 1000) String remarks) {
    }

    public record IssueWorkOrderRequest(
            @NotBlank @Size(max = 100) String poReference,
            LocalDate workOrderDate,
            @DecimalMin("0.0") BigDecimal agreedValue,
            @NotNull LocalDate startDate,
            LocalDate endDate,
            @Size(max = 4000) String scopeSummary,
            @Size(max = 4000) String terms,
            @Size(max = 1000) String remarks
    ) {
    }

    public record ProgressRequest(@NotBlank @Size(max = 2000) String message, @Min(0) @Max(100) Integer progressPercent) {
    }

    public record DeliveryRequest(@NotBlank @Size(max = 2000) String message) {
    }

    public record ConfirmDeliveryRequest(@NotNull Boolean delivered, @Size(max = 1000) String remarks) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    public record WorkOrderResponse(
            Long id,
            String workOrderNumber,
            Long engagementId,
            String engagementNumber,
            String engagementTitle,
            Long sellerId,
            String sellerName,
            Long buyerId,
            String buyerName,
            ProcEnums.WorkOrderStatus status,
            String requestRemarks,
            LocalDateTime requestedAt,
            String poReference,
            LocalDate workOrderDate,
            BigDecimal agreedValue,
            LocalDate startDate,
            LocalDate endDate,
            String scopeSummary,
            String terms,
            String buyerRemarks,
            LocalDateTime issuedAt,
            LocalDateTime acknowledgedAt,
            String declinedReason,
            List<RfqDtos.DocumentResponse> documents,
            LocalDateTime createdAt
    ) {
    }

    public record UpdateResponse(
            Long id,
            ProcEnums.UpdateType updateType,
            String message,
            Integer progressPercent,
            ProcEnums.EngagementStatus fromStatus,
            ProcEnums.EngagementStatus toStatus,
            String authorRole,
            String authorName,
            LocalDateTime createdAt
    ) {
    }

    public record EngagementSummary(
            Long id,
            String engagementNumber,
            String title,
            Long rfqId,
            String rfqNumber,
            Long quotationId,
            String quotationNumber,
            Long sellerId,
            String sellerName,
            Long buyerId,
            String buyerName,
            BigDecimal quotedValue,
            BigDecimal contractValue,
            LocalDate startDate,
            LocalDate expectedEndDate,
            ProcEnums.EngagementStatus status,
            int progressPercent,
            Boolean deliveryConfirmed,
            boolean feedbackSubmitted,
            ProcEnums.WorkOrderStatus workOrderStatus,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record EngagementDetail(
            EngagementSummary summary,
            String selectionRemarks,
            LocalDateTime deliveredAt,
            String deliveryNote,
            String deliveryRemarks,
            LocalDateTime completedAt,
            String cancelReason,
            QuotationDtos.QuotationResponse quotation,
            List<WorkOrderResponse> workOrders,
            List<UpdateResponse> updates,
            List<RfqDtos.DocumentResponse> documents,
            List<QueryDtos.TicketSummary> queries,
            FeedbackDtos.ReviewResponse review,
            String disclaimer
    ) {
    }
}
