package com.sidbi.bds.procurement.repository;

import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.Rfq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface RfqRepository extends JpaRepository<Rfq, Long>, JpaSpecificationExecutor<Rfq> {

    List<Rfq> findByStatusInAndSubmissionDeadlineBefore(Collection<ProcEnums.RfqStatus> statuses, LocalDateTime before);

    List<Rfq> findTop5ByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    long countByBuyerId(Long buyerId);

    long countByBuyerIdAndStatusIn(Long buyerId, Collection<ProcEnums.RfqStatus> statuses);

    @Query("select r.status, count(r) from Rfq r group by r.status")
    List<Object[]> countGroupedByStatus();

    @Query("select r.categoryName, count(r) from Rfq r group by r.categoryName order by count(r) desc")
    List<Object[]> countByCategory();

    @Query("select r.projectState, count(r) from Rfq r where r.projectState is not null group by r.projectState order by count(r) desc")
    List<Object[]> countByState();

    @Query("select r.createdAt from Rfq r where r.createdAt >= :from")
    List<LocalDateTime> createdSince(@Param("from") LocalDateTime from);
}
