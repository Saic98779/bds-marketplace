package com.sidbi.bds.auth.repository;

import com.sidbi.bds.auth.domain.SellerDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerDocumentRepository extends JpaRepository<SellerDocument, Long> {

    List<SellerDocument> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<SellerDocument> findByIdAndUserId(Long id, Long userId);
}
