package com.sidbi.bds.procurement.repository;

import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.RfqInvitation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RfqInvitationRepository extends JpaRepository<RfqInvitation, Long> {

    Optional<RfqInvitation> findByRfqIdAndSellerId(Long rfqId, Long sellerId);

    boolean existsByRfqIdAndSellerId(Long rfqId, Long sellerId);

    long countBySellerId(Long sellerId);

    long countBySellerIdAndStatus(Long sellerId, ProcEnums.InvitationStatus status);

    @Query("select count(i) from RfqInvitation i where i.sellerId = :sellerId and i.status in :statuses and i.rfq.status in :rfqStatuses and i.rfq.submissionDeadline > :now")
    long countPending(@Param("sellerId") Long sellerId,
                      @Param("statuses") Collection<ProcEnums.InvitationStatus> statuses,
                      @Param("rfqStatuses") Collection<ProcEnums.RfqStatus> rfqStatuses,
                      @Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = "rfq")
    @Query("select i from RfqInvitation i where i.reminderSent = false and i.status in :statuses and i.rfq.status in :rfqStatuses and i.rfq.submissionDeadline between :now and :until")
    List<RfqInvitation> dueForReminder(@Param("statuses") Collection<ProcEnums.InvitationStatus> statuses,
                                       @Param("rfqStatuses") Collection<ProcEnums.RfqStatus> rfqStatuses,
                                       @Param("now") LocalDateTime now,
                                       @Param("until") LocalDateTime until);

    @Query("select i.invitedAt from RfqInvitation i where i.sellerId = :sellerId and i.invitedAt >= :from")
    List<LocalDateTime> invitedSince(@Param("sellerId") Long sellerId, @Param("from") LocalDateTime from);
}
