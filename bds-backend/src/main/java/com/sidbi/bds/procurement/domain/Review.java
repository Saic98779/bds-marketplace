package com.sidbi.bds.procurement.domain;

import com.sidbi.bds.common.audit.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews", indexes = @Index(name = "ix_review_seller", columnList = "seller_id"))
@Getter
@Setter
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "engagement_id", nullable = false, unique = true)
    private Long engagementId;

    @Column(name = "engagement_number", length = 30)
    private String engagementNumber;

    @Column(name = "engagement_title", length = 200)
    private String engagementTitle;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "seller_name", length = 200)
    private String sellerName;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "buyer_name", length = 200)
    private String buyerName;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "delivered", nullable = false)
    private boolean delivered;

    @Column(name = "review_title", length = 200)
    private String title;

    @Column(name = "review_text", length = 2000)
    private String reviewText;

    @Column(name = "visible", nullable = false)
    private boolean visible = true;

    @Column(name = "moderation_remarks", length = 500)
    private String moderationRemarks;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<FeedbackAnswer> answers = new ArrayList<>();
}
