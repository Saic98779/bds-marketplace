package com.sidbi.bds.catalog.repository;

import com.sidbi.bds.catalog.domain.ListingModeration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingModerationRepository extends JpaRepository<ListingModeration, Long> {

    List<ListingModeration> findByListingIdOrderByCreatedAtDesc(Long listingId);
}
