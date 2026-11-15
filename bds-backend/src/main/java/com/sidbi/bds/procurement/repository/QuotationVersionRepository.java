package com.sidbi.bds.procurement.repository;

import com.sidbi.bds.procurement.domain.QuotationVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuotationVersionRepository extends JpaRepository<QuotationVersion, Long> {

    List<QuotationVersion> findByQuotationIdOrderByVersionNoDesc(Long quotationId);
}
