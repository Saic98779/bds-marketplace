package com.sidbi.bds.catalog.repository;

import com.sidbi.bds.catalog.domain.CatalogEnums;
import com.sidbi.bds.catalog.domain.ServiceListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceListingRepository extends JpaRepository<ServiceListing, Long>, JpaSpecificationExecutor<ServiceListing> {

    Optional<ServiceListing> findByIdAndSellerId(Long id, Long sellerId);

    long countBySellerId(Long sellerId);

    long countByStatus(CatalogEnums.ListingStatus status);

    @Query("select l.status, count(l) from ServiceListing l where l.sellerId = :sellerId group by l.status")
    List<Object[]> countBySellerGroupedByStatus(@Param("sellerId") Long sellerId);

    @Query("select coalesce(sum(l.viewCount), 0), coalesce(sum(l.enquiryCount), 0) from ServiceListing l where l.sellerId = :sellerId")
    List<Object[]> sellerEngagement(@Param("sellerId") Long sellerId);

    @Query("select l.status, count(l) from ServiceListing l group by l.status")
    List<Object[]> countGroupedByStatus();

    @Query("select l.category.id, l.category.name, count(l) from ServiceListing l where l.status = com.sidbi.bds.catalog.domain.CatalogEnums.ListingStatus.APPROVED and exists (select 1 from CatalogSeller s where s.sellerId = l.sellerId and s.active = true) group by l.category.id, l.category.name")
    List<Object[]> countPublicByCategory();

    @Query("select l.subcategory.id, count(l) from ServiceListing l where l.subcategory is not null and l.status = com.sidbi.bds.catalog.domain.CatalogEnums.ListingStatus.APPROVED and exists (select 1 from CatalogSeller s where s.sellerId = l.sellerId and s.active = true) group by l.subcategory.id")
    List<Object[]> countPublicBySubcategory();

    @Query("select l.category.name, count(l) from ServiceListing l group by l.category.name order by count(l) desc")
    List<Object[]> countAllByCategory();

    @Query("select count(l) from ServiceListing l where l.status = com.sidbi.bds.catalog.domain.CatalogEnums.ListingStatus.APPROVED and exists (select 1 from CatalogSeller s where s.sellerId = l.sellerId and s.active = true)")
    long countPublic();

    @Query("select distinct l.sellerId from ServiceListing l left join l.subcategory sc where l.category.id = :categoryId and (:subcategoryId is null or sc.id = :subcategoryId) and l.status = com.sidbi.bds.catalog.domain.CatalogEnums.ListingStatus.APPROVED and exists (select 1 from CatalogSeller s where s.sellerId = l.sellerId and s.active = true)")
    List<Long> activeSellersForCategory(@Param("categoryId") Long categoryId, @Param("subcategoryId") Long subcategoryId);

    @Modifying
    @Query("update ServiceListing l set l.sellerName = :name where l.sellerId = :sellerId")
    void renameSeller(@Param("sellerId") Long sellerId, @Param("name") String name);

    @Modifying
    @Query("update ServiceListing l set l.viewCount = l.viewCount + 1 where l.id = :id")
    void incrementViews(@Param("id") Long id);

    @Modifying
    @Query("update ServiceListing l set l.enquiryCount = l.enquiryCount + 1 where l.id = :id")
    void incrementEnquiries(@Param("id") Long id);
}
