package com.sidbi.bds.procurement.domain;

import com.sidbi.bds.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_orders", indexes = {
        @Index(name = "ix_wo_engagement", columnList = "engagement_id"),
        @Index(name = "ix_wo_seller", columnList = "seller_id"),
        @Index(name = "ix_wo_buyer", columnList = "buyer_id")
})
@Getter
@Setter
public class WorkOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_number", length = 30, unique = true)
    private String workOrderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "engagement_id", nullable = false)
    private Engagement engagement;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcEnums.WorkOrderStatus status;

    @Column(name = "request_remarks", length = 1000)
    private String requestRemarks;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "po_reference", length = 100)
    private String poReference;

    @Column(name = "work_order_date")
    private LocalDate workOrderDate;

    @Column(name = "agreed_value", precision = 15, scale = 2)
    private BigDecimal agreedValue;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "scope_summary", length = 4000)
    private String scopeSummary;

    @Column(name = "terms", length = 4000)
    private String terms;

    @Column(name = "buyer_remarks", length = 1000)
    private String buyerRemarks;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "declined_reason", length = 1000)
    private String declinedReason;
}
