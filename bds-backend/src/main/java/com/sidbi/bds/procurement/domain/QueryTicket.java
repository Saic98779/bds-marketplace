package com.sidbi.bds.procurement.domain;

import com.sidbi.bds.common.audit.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "query_tickets", indexes = {
        @Index(name = "ix_ticket_seller", columnList = "seller_id"),
        @Index(name = "ix_ticket_buyer", columnList = "buyer_id"),
        @Index(name = "ix_ticket_raiser", columnList = "raised_by_id"),
        @Index(name = "ix_ticket_status", columnList = "status"),
        @Index(name = "ix_ticket_context", columnList = "context_type, context_id")
})
@Getter
@Setter
public class QueryTicket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", length = 30, unique = true)
    private String ticketNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", nullable = false, length = 20)
    private ProcEnums.TicketType ticketType;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 20)
    private ProcEnums.ContextType contextType;

    @Column(name = "context_id")
    private Long contextId;

    @Column(name = "context_reference", length = 60)
    private String contextReference;

    @Column(name = "rfq_id")
    private Long rfqId;

    @Column(name = "engagement_id")
    private Long engagementId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "seller_name", length = 200)
    private String sellerName;

    @Column(name = "buyer_id")
    private Long buyerId;

    @Column(name = "buyer_name", length = 200)
    private String buyerName;

    @Column(name = "raised_by_id", nullable = false)
    private Long raisedById;

    @Column(name = "raised_by_role", nullable = false, length = 20)
    private String raisedByRole;

    @Column(name = "raised_by_name", length = 200)
    private String raisedByName;

    @Column(name = "subject", nullable = false, length = 250)
    private String subject;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private ProcEnums.Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcEnums.TicketStatus status;

    @Column(name = "escalated", nullable = false)
    private boolean escalated;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "escalation_reason", length = 1000)
    private String escalationReason;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "last_actor_role", length = 20)
    private String lastActorRole;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<QueryMessage> messages = new ArrayList<>();
}
