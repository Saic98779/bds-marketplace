package com.sidbi.bds.procurement.repository;

import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.Quotation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long>, JpaSpecificationExecutor<Quotation> {

    Optional<Quotation> findByRfqIdAndSellerId(Long rfqId, Long sellerId);

    @EntityGraph(attributePaths = {"rfq", "items"})
    List<Quotation> findByRfqIdAndStatusInOrderByTotalAmountAsc(Long rfqId, Collection<ProcEnums.QuotationStatus> statuses);

    List<Quotation> findByRfqIdAndStatusIn(Long rfqId, Collection<ProcEnums.QuotationStatus> statuses);

    long countByRfqIdAndStatusIn(Long rfqId, Collection<ProcEnums.QuotationStatus> statuses);

    long countBySellerIdAndStatusIn(Long sellerId, Collection<ProcEnums.QuotationStatus> statuses);

    long countByBuyerIdAndStatusIn(Long buyerId, Collection<ProcEnums.QuotationStatus> statuses);

    List<Quotation> findByStatusInAndValidUntilBefore(Collection<ProcEnums.QuotationStatus> statuses, LocalDate date);

    @EntityGraph(attributePaths = "rfq")
    List<Quotation> findTop5BySellerIdOrderByUpdatedAtDesc(Long sellerId);

    @Query("select q.rfq.id, count(q) from Quotation q where q.rfq.id in :rfqIds and q.status in :statuses group by q.rfq.id")
    List<Object[]> countByRfqIds(@Param("rfqIds") Collection<Long> rfqIds, @Param("statuses") Collection<ProcEnums.QuotationStatus> statuses);

    @Query("select q.status, count(q) from Quotation q group by q.status")
    List<Object[]> countGroupedByStatus();

    @Query("select q.submittedAt from Quotation q where q.submittedAt >= :from")
    List<LocalDateTime> submittedSince(@Param("from") LocalDateTime from);
}
