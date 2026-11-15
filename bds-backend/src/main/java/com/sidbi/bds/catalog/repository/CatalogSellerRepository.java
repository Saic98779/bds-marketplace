package com.sidbi.bds.catalog.repository;

import com.sidbi.bds.catalog.domain.CatalogSeller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface CatalogSellerRepository extends JpaRepository<CatalogSeller, Long> {

    List<CatalogSeller> findBySellerIdIn(Collection<Long> ids);

    @Query("select count(s) from CatalogSeller s where s.active = true and exists (select 1 from ServiceListing l where l.sellerId = s.sellerId and l.status = com.sidbi.bds.catalog.domain.CatalogEnums.ListingStatus.APPROVED)")
    long countActiveWithListings();
}
