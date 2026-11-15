package com.sidbi.bds.catalog.service;

import com.sidbi.bds.catalog.domain.ServiceCategory;
import com.sidbi.bds.catalog.domain.ServiceSubcategory;
import com.sidbi.bds.catalog.dto.CategoryDtos;
import com.sidbi.bds.catalog.repository.ServiceCategoryRepository;
import com.sidbi.bds.catalog.repository.ServiceListingRepository;
import com.sidbi.bds.catalog.repository.ServiceSubcategoryRepository;
import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.client.dto.CategoryRef;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.util.Csv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final ServiceCategoryRepository categoryRepository;
    private final ServiceSubcategoryRepository subcategoryRepository;
    private final ServiceListingRepository listingRepository;
    private final NotificationClient notificationClient;
    private final ListingMapper mapper;

    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryResponse> publicCategories() {
        Map<Long, Long> categoryCounts = categoryCounts();
        Map<Long, Long> subcategoryCounts = subcategoryCounts();
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(c -> mapper.category(c, categoryCounts, subcategoryCounts, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDtos.CategoryResponse publicCategory(Long id) {
        ServiceCategory category = categoryRepository.findWithSubcategoriesById(id)
                .filter(ServiceCategory::isActive)
                .orElseThrow(() -> BusinessException.notFound("Category not found"));
        return mapper.category(category, categoryCounts(), subcategoryCounts(), true);
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryResponse> adminCategories() {
        Map<Long, Long> categoryCounts = categoryCounts();
        Map<Long, Long> subcategoryCounts = subcategoryCounts();
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(c -> mapper.category(c, categoryCounts, subcategoryCounts, false))
                .toList();
    }

    @Transactional
    public CategoryDtos.CategoryResponse create(CategoryDtos.CategoryRequest request) {
        CurrentUser.require(Permission.CATALOG_MANAGE);
        if (categoryRepository.existsByCodeIgnoreCase(request.code())) {
            throw BusinessException.conflict("A category with this code already exists");
        }
        ServiceCategory category = new ServiceCategory();
        apply(category, request);
        categoryRepository.save(category);
        notificationClient.audit("CATEGORY_CREATED", "CATEGORY", category.getId(), category.getName());
        return mapper.category(category, Map.of(), Map.of(), false);
    }

    @Transactional
    public CategoryDtos.CategoryResponse update(Long id, CategoryDtos.CategoryRequest request) {
        CurrentUser.require(Permission.CATALOG_MANAGE);
        ServiceCategory category = category(id);
        if (!category.getCode().equalsIgnoreCase(request.code()) && categoryRepository.existsByCodeIgnoreCase(request.code())) {
            throw BusinessException.conflict("A category with this code already exists");
        }
        apply(category, request);
        notificationClient.audit("CATEGORY_UPDATED", "CATEGORY", id, category.getName());
        return mapper.category(category, categoryCounts(), subcategoryCounts(), false);
    }

    @Transactional
    public CategoryDtos.CategoryResponse setActive(Long id, boolean active) {
        CurrentUser.require(Permission.CATALOG_MANAGE);
        ServiceCategory category = category(id);
        category.setActive(active);
        notificationClient.audit(active ? "CATEGORY_ACTIVATED" : "CATEGORY_DEACTIVATED", "CATEGORY", id, category.getName());
        return mapper.category(category, categoryCounts(), subcategoryCounts(), false);
    }

    @Transactional
    public CategoryDtos.CategoryResponse addSubcategory(Long categoryId, CategoryDtos.SubcategoryRequest request) {
        CurrentUser.require(Permission.CATALOG_MANAGE);
        ServiceCategory category = category(categoryId);
        boolean duplicate = category.getSubcategories().stream().anyMatch(s -> s.getName().equalsIgnoreCase(request.name().trim()));
        if (duplicate) {
            throw BusinessException.conflict("Subcategory already exists in this category");
        }
        ServiceSubcategory subcategory = new ServiceSubcategory();
        subcategory.setCategory(category);
        applySub(subcategory, request);
        category.getSubcategories().add(subcategory);
        subcategoryRepository.save(subcategory);
        notificationClient.audit("SUBCATEGORY_CREATED", "SUBCATEGORY", subcategory.getId(), subcategory.getName());
        return mapper.category(category, categoryCounts(), subcategoryCounts(), false);
    }

    @Transactional
    public CategoryDtos.CategoryResponse updateSubcategory(Long categoryId, Long subcategoryId, CategoryDtos.SubcategoryRequest request) {
        CurrentUser.require(Permission.CATALOG_MANAGE);
        ServiceSubcategory subcategory = subcategoryRepository.findByIdAndCategoryId(subcategoryId, categoryId)
                .orElseThrow(() -> BusinessException.notFound("Subcategory not found"));
        applySub(subcategory, request);
        notificationClient.audit("SUBCATEGORY_UPDATED", "SUBCATEGORY", subcategoryId, subcategory.getName());
        return mapper.category(category(categoryId), categoryCounts(), subcategoryCounts(), false);
    }

    @Transactional(readOnly = true)
    public CategoryRef resolve(Long categoryId, Long subcategoryId) {
        ServiceCategory category = category(categoryId);
        if (!category.isActive()) {
            throw BusinessException.badRequest("Selected category is not active");
        }
        if (subcategoryId == null) {
            return new CategoryRef(category.getId(), category.getName(), null, null);
        }
        ServiceSubcategory subcategory = subcategoryRepository.findByIdAndCategoryId(subcategoryId, categoryId)
                .orElseThrow(() -> BusinessException.badRequest("Subcategory does not belong to the selected category"));
        return new CategoryRef(category.getId(), category.getName(), subcategory.getId(), subcategory.getName());
    }

    ServiceCategory category(Long id) {
        return categoryRepository.findWithSubcategoriesById(id).orElseThrow(() -> BusinessException.notFound("Category not found"));
    }

    private void apply(ServiceCategory category, CategoryDtos.CategoryRequest request) {
        category.setCode(request.code().trim().toUpperCase());
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setIcon(request.icon() == null || request.icon().isBlank() ? "category" : request.icon().trim());
        category.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        category.setActive(request.active() == null || request.active());
        category.setMandatoryFields(Csv.join(request.mandatoryFields()));
        category.setEligibilityRules(request.eligibilityRules());
    }

    private static void applySub(ServiceSubcategory subcategory, CategoryDtos.SubcategoryRequest request) {
        subcategory.setName(request.name().trim());
        subcategory.setDescription(request.description());
        subcategory.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        subcategory.setActive(request.active() == null || request.active());
    }

    private Map<Long, Long> categoryCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : listingRepository.countPublicByCategory()) {
            counts.put((Long) row[0], ((Number) row[2]).longValue());
        }
        return counts;
    }

    private Map<Long, Long> subcategoryCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : listingRepository.countPublicBySubcategory()) {
            counts.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }
}
