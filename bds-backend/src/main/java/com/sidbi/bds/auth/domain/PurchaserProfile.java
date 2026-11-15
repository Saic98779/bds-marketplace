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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "purchaser_profiles")
@Getter
@Setter
public class PurchaserProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "organisation_name", nullable = false, length = 200)
    private String organisationName;

    @Enumerated(EnumType.STRING)
    @Column(name = "constitution", length = 40)
    private Enums.Constitution constitution;

    @Enumerated(EnumType.STRING)
    @Column(name = "purchaser_type", nullable = false, length = 40)
    private Enums.PurchaserType purchaserType;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column(name = "designation", length = 100)
    private String designation;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "pan", length = 256)
    private String pan;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "gstin", length = 256)
    private String gstin;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "udyam_number", length = 256)
    private String udyamNumber;

    @Column(name = "industry_sector", length = 150)
    private String industrySector;

    @Column(name = "business_activity", length = 1000)
    private String businessActivity;

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

    @Column(name = "website", length = 250)
    private String website;
}
