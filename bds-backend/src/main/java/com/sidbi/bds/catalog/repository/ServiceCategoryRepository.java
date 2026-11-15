package com.sidbi.bds.catalog.repository;

import com.sidbi.bds.catalog.domain.ServiceCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    @EntityGraph(attributePaths = "subcategories")
    List<ServiceCategory> findAllByOrderByDisplayOrderAscNameAsc();

    @EntityGraph(attributePaths = "subcategories")
    List<ServiceCategory> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    @EntityGraph(attributePaths = "subcategories")
    Optional<ServiceCategory> findWithSubcategoriesById(Long id);

    boolean existsByCodeIgnoreCase(String code);
}
