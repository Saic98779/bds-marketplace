package com.sidbi.bds.procurement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_scores", indexes = @Index(name = "ix_scores_rfq", columnList = "rfq_id"))
@Getter
@Setter
public class EvaluationScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rfq_id", nullable = false)
    private Long rfqId;

    @Column(name = "quotation_id", nullable = false)
    private Long quotationId;

    @Column(name = "criterion_id", nullable = false)
    private Long criterionId;

    @Column(name = "score_value", nullable = false, precision = 4, scale = 1)
    private BigDecimal score;

    @Column(name = "scored_by")
    private Long scoredBy;

    @Column(name = "scored_at", nullable = false)
    private LocalDateTime scoredAt = LocalDateTime.now();
}
