package com.sidbi.bds.catalog.domain;

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
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_listings", indexes = {
        @Index(name = "ix_listing_seller", columnList = "seller_id"),
        @Index(name = "ix_listing_status", columnList = "status"),
        @Index(name = "ix_listing_category", columnList = "category_id")
})
@Getter
@Setter
public class ServiceListing extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_no", length = 30, unique = true)
    private String referenceNo;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "seller_name", length = 200)
    private String sellerName;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private ServiceSubcategory subcategory;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Column(name = "scope_of_work", length = 4000)
    private String scopeOfWork;

    @Column(name = "deliverables", length = 4000)
    private String deliverables;

    @Column(name = "methodology", length = 4000)
    private String methodology;

    @Column(name = "target_industries", length = 1000)
    private String targetIndustries;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 20)
    private CatalogEnums.DeliveryMode deliveryMode;

    @Column(name = "service_locations", length = 1000)
    private String serviceLocations;

    @Column(name = "state_name", length = 100)
    private String state;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "estimated_duration", length = 100)
    private String estimatedDuration;

    @Column(name = "indicative_fee", precision = 15, scale = 2)
    private BigDecimal indicativeFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model", nullable = false, length = 30)
    private CatalogEnums.PricingModel pricingModel;

    @Column(name = "tags", length = 500)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CatalogEnums.ListingStatus status;

    @Column(name = "featured", nullable = false)
    private boolean featured;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "enquiry_count", nullable = false)
    private long enquiryCount;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    @Column(name = "moderated_by")
    private Long moderatedBy;

    @Column(name = "moderation_remarks", length = 1000)
    private String moderationRemarks;

    @Column(name = "created_by_admin")
    private Long createdByAdmin;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ServiceAttachment> attachments = new ArrayList<>();
}
