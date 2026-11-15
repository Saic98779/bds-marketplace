package com.sidbi.bds.procurement.repository;

import com.sidbi.bds.procurement.domain.EngagementUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EngagementUpdateRepository extends JpaRepository<EngagementUpdate, Long> {

    List<EngagementUpdate> findByEngagementIdOrderByCreatedAtDesc(Long engagementId);
}
