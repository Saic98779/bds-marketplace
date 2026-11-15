package com.sidbi.bds.procurement.domain;

import com.sidbi.bds.common.audit.BaseEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotations",
        uniqueConstraints = @UniqueConstraint(name = "uk_quotation_rfq_seller", columnNames = {"rfq_id", "seller_id"}),
        indexes = {
                @Index(name = "ix_quotation_seller", columnList = "seller_id"),
                @Index(name = "ix_quotation_buyer", columnList = "buyer_id"),
                @Index(name = "ix_quotation_status", columnList = "status")
        })
@Getter
@Setter
public class Quotation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quotation_number", length = 30, unique = true)
    private String quotationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "seller_name", length = 200)
    private String sellerName;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProcEnums.QuotationStatus status;

    @Column(name = "technical_proposal", length = 4000)
    private String technicalProposal;

    @Column(name = "methodology", length = 4000)
    private String methodology;

    @Column(name = "scope_accepted", nullable = false)
    private boolean scopeAccepted = true;

    @Column(name = "scope_deviations", length = 2000)
    private String scopeDeviations;

    @Column(name = "deliverables", length = 4000)
    private String deliverables;

    @Column(name = "timeline_days")
    private Integer timelineDays;

    @Column(name = "team_composition", length = 2000)
    private String teamComposition;

    @Column(name = "relevant_experience", length = 2000)
    private String relevantExperience;

    @Column(name = "professional_fee", precision = 15, scale = 2)
    private BigDecimal professionalFee;

    @Column(name = "tax_percent", precision = 5, scale = 2)
    private BigDecimal taxPercent;

    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "other_expenses", precision = 15, scale = 2)
    private BigDecimal otherExpenses;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_milestones", length = 2000)
    private String paymentMilestones;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "decision_at")
    private LocalDateTime decisionAt;

    @Column(name = "decision_remarks", length = 1000)
    private String decisionRemarks;

    @Column(name = "shortlisted", nullable = false)
    private boolean shortlisted;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<QuotationItem> items = new ArrayList<>();
}
