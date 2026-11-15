package com.sidbi.bds.procurement.repository;

import com.sidbi.bds.procurement.domain.Engagement;
import com.sidbi.bds.procurement.domain.ProcEnums;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EngagementRepository extends JpaRepository<Engagement, Long>, JpaSpecificationExecutor<Engagement> {

    @EntityGraph(attributePaths = {"rfq", "quotation"})
    Optional<Engagement> findWithDetailsById(Long id);

    Optional<Engagement> findByQuotationId(Long quotationId);

    long countBySellerIdAndStatusIn(Long sellerId, Collection<ProcEnums.EngagementStatus> statuses);

    long countByBuyerIdAndStatusIn(Long buyerId, Collection<ProcEnums.EngagementStatus> statuses);

    long countByBuyerIdAndStatusAndFeedbackSubmittedFalse(Long buyerId, ProcEnums.EngagementStatus status);

    long countBySellerId(Long sellerId);

    List<Engagement> findTop5BySellerIdOrderByUpdatedAtDesc(Long sellerId);

    List<Engagement> findTop5ByBuyerIdOrderByUpdatedAtDesc(Long buyerId);

    @Query("select e.status, count(e) from Engagement e group by e.status")
    List<Object[]> countGroupedByStatus();

    @Query("select e.createdAt from Engagement e where e.createdAt >= :from")
    List<LocalDateTime> createdSince(@Param("from") LocalDateTime from);
}
