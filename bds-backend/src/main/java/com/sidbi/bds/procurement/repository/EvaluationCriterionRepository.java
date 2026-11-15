package com.sidbi.bds.procurement.repository;

import com.sidbi.bds.procurement.domain.EvaluationCriterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion, Long> {

    List<EvaluationCriterion> findByRfqIdOrderByDisplayOrderAsc(Long rfqId);

    @Modifying
    @Query("delete from EvaluationCriterion c where c.rfqId = :rfqId")
    void deleteByRfq(@Param("rfqId") Long rfqId);
}
