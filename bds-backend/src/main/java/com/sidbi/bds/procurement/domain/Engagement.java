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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "engagements", indexes = {
        @Index(name = "ix_engagement_seller", columnList = "seller_id"),
        @Index(name = "ix_engagement_buyer", columnList = "buyer_id"),
        @Index(name = "ix_engagement_status", columnList = "status")
})
@Getter
@Setter
public class Engagement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "engagement_number", length = 30, unique = true)
    private String engagementNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id", nullable = false, unique = true)
    private Quotation quotation;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "seller_name", length = 200)
    private String sellerName;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "buyer_name", length = 200)
    private String buyerName;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "quoted_value", precision = 15, scale = 2)
    private BigDecimal quotedValue;

    @Column(name = "contract_value", precision = 15, scale = 2)
    private BigDecimal contractValue;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expected_end_date")
    private LocalDate expectedEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProcEnums.EngagementStatus status;

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    @Column(name = "selection_remarks", length = 1000)
    private String selectionRemarks;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "delivery_note", length = 2000)
    private String deliveryNote;

    @Column(name = "delivery_confirmed")
    private Boolean deliveryConfirmed;

    @Column(name = "delivery_remarks", length = 1000)
    private String deliveryRemarks;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancel_reason", length = 1000)
    private String cancelReason;

    @Column(name = "feedback_submitted", nullable = false)
    private boolean feedbackSubmitted;
}
