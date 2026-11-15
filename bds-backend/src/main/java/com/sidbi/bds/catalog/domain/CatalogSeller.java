package com.sidbi.bds.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_sellers")
@Getter
@Setter
public class CatalogSeller {

    @Id
    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "seller_name", length = 200)
    private String sellerName;

    @Column(name = "state_name", length = 100)
    private String state;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "average_rating")
    private Double averageRating;

    @Column(name = "rating_count")
    private Integer ratingCount;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
