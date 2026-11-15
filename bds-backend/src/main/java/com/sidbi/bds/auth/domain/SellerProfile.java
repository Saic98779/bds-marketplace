package com.sidbi.bds.auth.domain;

import com.sidbi.bds.common.audit.BaseEntity;
import com.sidbi.bds.common.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller_profiles", indexes = {
        @Index(name = "ix_seller_state", columnList = "state_name"),
        @Index(name = "ix_seller_provider_type", columnList = "provider_type")
})
@Getter
@Setter
public class SellerProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "organisation_name", nullable = false, length = 200)
    private String organisationName;

    @Enumerated(EnumType.STRING)
    @Column(name = "constitution", nullable = false, length = 40)
    private Enums.Constitution constitution;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "pan", length = 256)
    private String pan;

    @Column(name = "pan_hash", length = 64)
    private String panHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "gstin", length = 256)
    private String gstin;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "udyam_number", length = 256)
    private String udyamNumber;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "address_line", length = 1024)
    private String addressLine;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_name", length = 100)
    private String state;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 40)
    private Enums.ProviderType providerType;

    @Column(name = "category_ids", length = 500)
    private String categoryIds;

    @Column(name = "category_names", length = 1000)
    private String categoryNames;

    @Column(name = "expertise", length = 1000)
    private String expertise;

    @Column(name = "industries", length = 1000)
    private String industries;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Column(name = "team_size")
    private Integer teamSize;

    @Column(name = "geographic_coverage", length = 1000)
    private String geographicCoverage;

    @Column(name = "delivery_modes", length = 100)
    private String deliveryModes;

    @Column(name = "website", length = 250)
    private String website;

    @Column(name = "introduction", length = 4000)
    private String introduction;

    @Column(name = "year_established")
    private Integer yearEstablished;

    @Column(name = "certifications", length = 2000)
    private String certifications;

    @Column(name = "team_profile", length = 2000)
    private String teamProfile;

    @Column(name = "previous_assignments", length = 4000)
    private String previousAssignments;

    @Column(name = "client_references", length = 2000)
    private String clientReferences;

    @Column(name = "average_rating")
    private Double averageRating;

    @Column(name = "rating_count")
    private Integer ratingCount;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "reverification_due")
    private LocalDate reverificationDue;

    @Column(name = "featured", nullable = false)
    private boolean featured;
}
