package com.sidbi.bds.procurement.domain;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rfq_invited_sellers",
        uniqueConstraints = @UniqueConstraint(name = "uk_rfq_invite", columnNames = {"rfq_id", "seller_id"}),
        indexes = @Index(name = "ix_invite_seller", columnList = "seller_id"))
@Getter
@Setter
public class RfqInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "seller_name", length = 200)
    private String sellerName;

    @Column(name = "invited_by", length = 20)
    private String invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcEnums.InvitationStatus status = ProcEnums.InvitationStatus.INVITED;

    @Column(name = "invited_at", nullable = false)
    private LocalDateTime invitedAt = LocalDateTime.now();

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "decline_reason", length = 500)
    private String declineReason;

    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent;
}
