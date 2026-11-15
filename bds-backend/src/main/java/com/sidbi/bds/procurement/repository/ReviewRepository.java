package com.sidbi.bds.procurement.repository;

import com.sidbi.bds.procurement.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByEngagementId(Long engagementId);

    @EntityGraph(attributePaths = "answers")
    Optional<Review> findWithAnswersById(Long id);

    Page<Review> findBySellerIdAndVisibleTrue(Long sellerId, Pageable pageable);

    Page<Review> findBySellerId(Long sellerId, Pageable pageable);

    @Query("select r from Review r")
    Page<Review> findAllReviews(Pageable pageable);

    @Query("select r.rating, count(r) from Review r where r.sellerId = :sellerId and r.visible = true group by r.rating")
    List<Object[]> distribution(@Param("sellerId") Long sellerId);

    @Query("select coalesce(avg(r.rating), 0), count(r) from Review r where r.sellerId = :sellerId and r.visible = true")
    List<Object[]> aggregate(@Param("sellerId") Long sellerId);
}
