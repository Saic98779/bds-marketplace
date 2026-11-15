package com.sidbi.bds.procurement.repository;

import com.sidbi.bds.procurement.domain.EvaluationScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EvaluationScoreRepository extends JpaRepository<EvaluationScore, Long> {

    List<EvaluationScore> findByRfqId(Long rfqId);

    @Modifying
    @Query("delete from EvaluationScore s where s.rfqId = :rfqId")
    void deleteByRfq(@Param("rfqId") Long rfqId);
}
