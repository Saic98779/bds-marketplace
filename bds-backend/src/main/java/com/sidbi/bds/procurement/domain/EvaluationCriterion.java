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

@Entity
@Table(name = "quotation_comparisons", indexes = @Index(name = "ix_criteria_rfq", columnList = "rfq_id"))
@Getter
@Setter
public class EvaluationCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rfq_id", nullable = false)
    private Long rfqId;

    @Column(name = "criterion_name", nullable = false, length = 150)
    private String name;

    @Column(name = "weight_pct", nullable = false)
    private int weight;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
