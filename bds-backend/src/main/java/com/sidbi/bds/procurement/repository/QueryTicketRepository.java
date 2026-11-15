package com.sidbi.bds.procurement.repository;

import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.QueryTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface QueryTicketRepository extends JpaRepository<QueryTicket, Long>, JpaSpecificationExecutor<QueryTicket> {

    @Query("select count(t) from QueryTicket t where (t.sellerId = :userId or t.buyerId = :userId or t.raisedById = :userId) and t.status in :statuses")
    long countOpenForUser(@Param("userId") Long userId, @Param("statuses") Collection<ProcEnums.TicketStatus> statuses);

    long countByTicketTypeAndStatusIn(ProcEnums.TicketType type, Collection<ProcEnums.TicketStatus> statuses);

    long countByEscalatedTrueAndStatusIn(Collection<ProcEnums.TicketStatus> statuses);

    long countByStatusInAndDueAtBefore(Collection<ProcEnums.TicketStatus> statuses, LocalDateTime now);

    List<QueryTicket> findByContextTypeAndContextIdOrderByCreatedAtDesc(ProcEnums.ContextType type, Long contextId);
}
