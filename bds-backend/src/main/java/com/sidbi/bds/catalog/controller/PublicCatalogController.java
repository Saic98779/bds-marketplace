package com.sidbi.bds.catalog.controller;

import com.sidbi.bds.catalog.domain.CatalogEnums;
import com.sidbi.bds.catalog.dto.CategoryDtos;
import com.sidbi.bds.catalog.dto.ListingDtos;
import com.sidbi.bds.catalog.service.CategoryService;
import com.sidbi.bds.catalog.service.PublicCatalogService;
import com.sidbi.bds.common.api.ApiResponse;
import com.sidbi.bds.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Public Catalogue", description = "Approved services from active, verified providers")
public class PublicCatalogController {

    private final CategoryService categoryService;
    private final PublicCatalogService catalogService;

    @GetMapping("/categories")
    @Operation(summary = "List active categories with subcategories and listing counts")
    public ApiResponse<List<CategoryDtos.CategoryResponse>> categories() {
        return ApiResponse.ok(categoryService.publicCategories());
    }

    @GetMapping("/categories/{categoryId}")
    @Operation(summary = "Get an active category")
    public ApiResponse<CategoryDtos.CategoryResponse> category(@PathVariable Long categoryId) {
        return ApiResponse.ok(categoryService.publicCategory(categoryId));
    }

    @GetMapping("/services")
    @Operation(summary = "Search approved service listings")
    public ApiResponse<PageResponse<ListingDtos.ListingCard>> services(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) CatalogEnums.DeliveryMode deliveryMode,
            @RequestParam(required = false) CatalogEnums.PricingModel pricingModel,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) BigDecimal maxFee,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.ok(catalogService.search(new PublicCatalogService.ServiceFilter(q, categoryId, subcategoryId, state,
                deliveryMode, pricingModel, minExperience, maxFee, sellerId, featured, industry, sort), page, size));
    }

    @GetMapping("/services/{serviceId}")
    @Operation(summary = "Get service details")
    public ApiResponse<ListingDtos.ListingDetail> service(@PathVariable Long serviceId) {
        return ApiResponse.ok(catalogService.detail(serviceId));
    }

    @GetMapping("/catalog/stats")
    @Operation(summary = "Public marketplace counters")
    public ApiResponse<ListingDtos.PublicStats> stats() {
        return ApiResponse.ok(catalogService.stats());
    }
}
