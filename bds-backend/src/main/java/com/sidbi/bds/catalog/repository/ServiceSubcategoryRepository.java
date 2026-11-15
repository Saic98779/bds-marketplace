package com.sidbi.bds.catalog.repository;

import com.sidbi.bds.catalog.domain.ServiceSubcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceSubcategoryRepository extends JpaRepository<ServiceSubcategory, Long> {

    Optional<ServiceSubcategory> findByIdAndCategoryId(Long id, Long categoryId);
}
