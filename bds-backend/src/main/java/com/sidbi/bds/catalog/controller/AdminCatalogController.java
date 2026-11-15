package com.sidbi.bds.catalog.controller;

import com.sidbi.bds.catalog.domain.CatalogEnums;
import com.sidbi.bds.catalog.dto.CategoryDtos;
import com.sidbi.bds.catalog.dto.ListingDtos;
import com.sidbi.bds.catalog.service.CategoryService;
import com.sidbi.bds.catalog.service.ListingService;
import com.sidbi.bds.catalog.service.ModerationService;
import com.sidbi.bds.common.api.ApiResponse;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.api.RemarksRequest;
import com.sidbi.bds.common.api.RequiredRemarksRequest;
import com.sidbi.bds.common.report.ReportFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin - Catalogue", description = "Taxonomy management and service listing moderation")
public class AdminCatalogController {

    private final CategoryService categoryService;
    private final ModerationService moderationService;
    private final ListingService listingService;

    @GetMapping("/categories")
    @Operation(summary = "List all categories including inactive")
    public ApiResponse<List<CategoryDtos.CategoryResponse>> categories() {
        return ApiResponse.ok(categoryService.adminCategories());
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category")
    public ApiResponse<CategoryDtos.CategoryResponse> createCategory(@Valid @RequestBody CategoryDtos.CategoryRequest request) {
        return ApiResponse.created("Category created", categoryService.create(request));
    }

    @PutMapping("/categories/{categoryId}")
    @Operation(summary = "Update a category")
    public ApiResponse<CategoryDtos.CategoryResponse> updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryDtos.CategoryRequest request) {
        return ApiResponse.ok("Category updated", categoryService.update(categoryId, request));
    }

    @PatchMapping("/categories/{categoryId}/status")
    @Operation(summary = "Activate or deactivate a category")
    public ApiResponse<CategoryDtos.CategoryResponse> categoryStatus(@PathVariable Long categoryId, @RequestBody Map<String, Boolean> body) {
        return ApiResponse.ok("Category status updated", categoryService.setActive(categoryId, Boolean.TRUE.equals(body.get("active"))));
    }

    @PostMapping("/categories/{categoryId}/subcategories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a subcategory")
    public ApiResponse<CategoryDtos.CategoryResponse> addSubcategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryDtos.SubcategoryRequest request) {
        return ApiResponse.created("Subcategory added", categoryService.addSubcategory(categoryId, request));
    }

    @PutMapping("/categories/{categoryId}/subcategories/{subcategoryId}")
    @Operation(summary = "Update a subcategory")
    public ApiResponse<CategoryDtos.CategoryResponse> updateSubcategory(@PathVariable Long categoryId, @PathVariable Long subcategoryId,
                                                                        @Valid @RequestBody CategoryDtos.SubcategoryRequest request) {
        return ApiResponse.ok("Subcategory updated", categoryService.updateSubcategory(categoryId, subcategoryId, request));
    }

    @GetMapping("/services")
    @Operation(summary = "Search listings for moderation")
    public ApiResponse<PageResponse<ListingDtos.ListingCard>> listings(@RequestParam(required = false) CatalogEnums.ListingStatus status,
                                                                       @RequestParam(required = false) String q,
                                                                       @RequestParam(required = false) Long categoryId,
                                                                       @RequestParam(required = false) Long sellerId,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(moderationService.list(new ModerationService.AdminListingFilter(status, q, categoryId, sellerId), page, size));
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a service listing on behalf of an approved seller")
    public ApiResponse<ListingDtos.ListingDetail> createListing(@Valid @RequestBody ListingDtos.AdminListingRequest request) {
        return ApiResponse.created("Service listing created", moderationService.createOnBehalf(request));
    }

    @GetMapping("/services/{listingId}")
    @Operation(summary = "Get listing detail for moderation")
    public ApiResponse<ListingDtos.ListingDetail> listing(@PathVariable Long listingId) {
        return ApiResponse.ok(moderationService.detail(listingId));
    }

    @GetMapping("/services/{listingId}/history")
    @Operation(summary = "Moderation history")
    public ApiResponse<List<ListingDtos.ModerationResponse>> history(@PathVariable Long listingId) {
        return ApiResponse.ok(listingService.history(listingId));
    }

    @PostMapping(value = "/services/{listingId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Attach a file to a listing")
    public ApiResponse<ListingDtos.AttachmentResponse> attach(@PathVariable Long listingId,
                                                              @RequestParam(value = "label", required = false) String label,
                                                              @RequestPart("file") MultipartFile file) {
        return ApiResponse.created("Attachment uploaded", listingService.addAttachment(listingId, label, file));
    }

    @PostMapping("/services/{listingId}/start-review")
    @Operation(summary = "Take a submitted listing under review")
    public ApiResponse<ListingDtos.ListingDetail> startReview(@PathVariable Long listingId) {
        return ApiResponse.ok("Review started", moderationService.startReview(listingId));
    }

    @PostMapping("/services/{listingId}/approve")
    @Operation(summary = "Approve and publish a listing")
    public ApiResponse<ListingDtos.ListingDetail> approve(@PathVariable Long listingId, @Valid @RequestBody RemarksRequest request) {
        return ApiResponse.ok("Listing approved and published", moderationService.approve(listingId, request.remarks()));
    }

    @PostMapping("/services/{listingId}/reject")
    @Operation(summary = "Reject a listing")
    public ApiResponse<ListingDtos.ListingDetail> reject(@PathVariable Long listingId, @Valid @RequestBody RequiredRemarksRequest request) {
        return ApiResponse.ok("Listing rejected", moderationService.reject(listingId, request.remarks()));
    }

    @PostMapping("/services/{listingId}/request-correction")
    @Operation(summary = "Request corrections from the seller")
    public ApiResponse<ListingDtos.ListingDetail> correction(@PathVariable Long listingId, @Valid @RequestBody RequiredRemarksRequest request) {
        return ApiResponse.ok("Correction requested", moderationService.requestCorrection(listingId, request.remarks()));
    }

    @PostMapping("/services/{listingId}/flag")
    @Operation(summary = "Flag a published listing and hide it")
    public ApiResponse<ListingDtos.ListingDetail> flag(@PathVariable Long listingId, @Valid @RequestBody RequiredRemarksRequest request) {
        return ApiResponse.ok("Listing flagged", moderationService.flag(listingId, request.remarks()));
    }

    @PostMapping("/services/{listingId}/deactivate")
    @Operation(summary = "Deactivate a listing")
    public ApiResponse<ListingDtos.ListingDetail> deactivate(@PathVariable Long listingId, @Valid @RequestBody RequiredRemarksRequest request) {
        return ApiResponse.ok("Listing deactivated", moderationService.deactivate(listingId, request.remarks()));
    }

    @PutMapping("/services/{listingId}/featured")
    @Operation(summary = "Feature a listing on the homepage")
    public ApiResponse<ListingDtos.ListingDetail> featured(@PathVariable Long listingId, @RequestBody Map<String, Boolean> body) {
        return ApiResponse.ok("Featured flag updated", moderationService.setFeatured(listingId, Boolean.TRUE.equals(body.get("featured"))));
    }

    @GetMapping("/catalog/stats")
    @Operation(summary = "Catalogue statistics")
    public ApiResponse<ListingDtos.CatalogStats> stats() {
        return ApiResponse.ok(moderationService.stats());
    }

    @GetMapping("/catalog/reports/services")
    @Operation(summary = "Export service catalogue")
    public ResponseEntity<byte[]> report(@RequestParam(required = false) CatalogEnums.ListingStatus status,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) Long sellerId,
                                         @RequestParam(defaultValue = "XLSX") ReportFormat format) {
        return moderationService.report(new ModerationService.AdminListingFilter(status, null, categoryId, sellerId), format);
    }
}
