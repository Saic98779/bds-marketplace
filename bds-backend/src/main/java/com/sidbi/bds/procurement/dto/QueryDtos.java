package com.sidbi.bds.procurement.dto;

import com.sidbi.bds.procurement.domain.ProcEnums;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class QueryDtos {

    private QueryDtos() {
    }

    public record CreateTicketRequest(
            @NotNull ProcEnums.ContextType contextType,
            Long contextId,
            Long counterpartyId,
            @NotBlank @Size(max = 250) String subject,
            @NotBlank @Size(max = 4000) String description,
            ProcEnums.Priority priority
    ) {
    }

    public record GrievanceRequest(
            ProcEnums.ContextType contextType,
            Long contextId,
            @NotBlank @Size(max = 250) String subject,
            @NotBlank @Size(max = 4000) String description,
            ProcEnums.Priority priority
    ) {
    }

    public record MessageRequest(@NotBlank @Size(max = 4000) String body) {
    }

    public record StatusRequest(@Size(max = 1000) String remarks) {
    }

    public record MessageResponse(Long id, Long senderId, String senderRole, String senderName, String body, boolean systemGenerated,
                                  LocalDateTime createdAt) {
    }

    public record TicketSummary(
            Long id,
            String ticketNumber,
            ProcEnums.TicketType ticketType,
            ProcEnums.ContextType contextType,
            Long contextId,
            String contextReference,
            Long sellerId,
            String sellerName,
            Long buyerId,
            String buyerName,
            String raisedByRole,
            String raisedByName,
            String subject,
            ProcEnums.Priority priority,
            ProcEnums.TicketStatus status,
            boolean escalated,
            boolean overdue,
            boolean awaitingMyResponse,
            int messageCount,
            LocalDateTime dueAt,
            LocalDateTime lastActivityAt,
            LocalDateTime createdAt
    ) {
    }

    public record TicketDetail(
            TicketSummary summary,
            String description,
            String escalationReason,
            String resolutionNote,
            LocalDateTime resolvedAt,
            LocalDateTime closedAt,
            List<MessageResponse> messages,
            List<RfqDtos.DocumentResponse> attachments,
            boolean canReply,
            boolean canResolve,
            boolean canEscalate,
            boolean canReopen,
            boolean canClose
    ) {
    }
}
