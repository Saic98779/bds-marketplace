package com.sidbi.bds.procurement.repository;

import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.WorkOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long>, JpaSpecificationExecutor<WorkOrder> {

    @EntityGraph(attributePaths = "engagement")
    Optional<WorkOrder> findWithEngagementById(Long id);

    List<WorkOrder> findByEngagementIdOrderByCreatedAtDesc(Long engagementId);

    boolean existsByEngagementIdAndStatusIn(Long engagementId, Collection<ProcEnums.WorkOrderStatus> statuses);

    long countByBuyerIdAndStatus(Long buyerId, ProcEnums.WorkOrderStatus status);

    long countBySellerIdAndStatus(Long sellerId, ProcEnums.WorkOrderStatus status);
}
