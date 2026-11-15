package com.sidbi.bds.procurement.repository;

import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.ProcurementDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProcurementDocumentRepository extends JpaRepository<ProcurementDocument, Long> {

    List<ProcurementDocument> findByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(ProcEnums.OwnerType ownerType, Long ownerId);

    long countByOwnerTypeAndOwnerId(ProcEnums.OwnerType ownerType, Long ownerId);

    Optional<ProcurementDocument> findByIdAndOwnerTypeAndOwnerId(Long id, ProcEnums.OwnerType ownerType, Long ownerId);
}
