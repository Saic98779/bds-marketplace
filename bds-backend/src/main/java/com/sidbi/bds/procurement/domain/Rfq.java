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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rfqs", indexes = {
        @Index(name = "ix_rfq_buyer", columnList = "buyer_id"),
        @Index(name = "ix_rfq_status", columnList = "status"),
        @Index(name = "ix_rfq_deadline", columnList = "submission_deadline")
})
@Getter
@Setter
public class Rfq extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rfq_number", length = 30, unique = true)
    private String rfqNumber;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "buyer_name", length = 200)
    private String buyerName;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "category_name", length = 150)
    private String categoryName;

    @Column(name = "subcategory_id")
    private Long subcategoryId;

    @Column(name = "subcategory_name", length = 150)
    private String subcategoryName;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Column(name = "scope_of_work", length = 4000)
    private String scopeOfWork;

    @Column(name = "deliverables", length = 4000)
    private String deliverables;

    @Column(name = "industry", length = 150)
    private String industry;

    @Column(name = "project_state", length = 100)
    private String projectState;

    @Column(name = "project_district", length = 100)
    private String projectDistrict;

    @Column(name = "delivery_mode", length = 20)
    private String deliveryMode;

    @Column(name = "budget_min", precision = 15, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 15, scale = 2)
    private BigDecimal budgetMax;

    @Column(name = "expected_duration", length = 100)
    private String expectedDuration;

    @Column(name = "required_experience")
    private Integer requiredExperience;

    @Column(name = "expected_start_date")
    private LocalDate expectedStartDate;

    @Column(name = "submission_deadline", nullable = false)
    private LocalDateTime submissionDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution_mode", nullable = false, length = 20)
    private ProcEnums.DistributionMode distributionMode;

    @Column(name = "source_listing_id")
    private Long sourceListingId;

    @Column(name = "source_listing_title", length = 200)
    private String sourceListingTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProcEnums.RfqStatus status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "status_remarks", length = 1000)
    private String statusRemarks;

    @Column(name = "selected_quotation_id")
    private Long selectedQuotationId;

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("invitedAt ASC")
    private List<RfqInvitation> invitations = new ArrayList<>();
}
