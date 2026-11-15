package com.sidbi.bds.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "listing_moderations", indexes = @Index(name = "ix_moderation_listing", columnList = "listing_id"))
@Getter
@Setter
public class ListingModeration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private CatalogEnums.ModerationAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private CatalogEnums.ListingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 30)
    private CatalogEnums.ListingStatus toStatus;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_name", length = 200)
    private String actorName;

    @Column(name = "actor_role", length = 20)
    private String actorRole;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
