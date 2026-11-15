package com.sidbi.bds.catalog.controller;

import com.sidbi.bds.catalog.domain.CatalogEnums;
import com.sidbi.bds.catalog.dto.ListingDtos;
import com.sidbi.bds.catalog.service.ListingService;
import com.sidbi.bds.common.api.ApiResponse;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.api.RemarksRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

@RestController
@RequestMapping("/api/v1/seller/services")
@RequiredArgsConstructor
@Tag(name = "Seller - Service Listings", description = "Create and manage service listings subject to moderation")
public class SellerListingController {

    private final ListingService listingService;

    @GetMapping
    @Operation(summary = "List own service listings")
    public ApiResponse<PageResponse<ListingDtos.ListingCard>> list(@RequestParam(required = false) CatalogEnums.ListingStatus status,
                                                                   @RequestParam(required = false) String q,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(listingService.mine(status, q, page, size));
    }

    @GetMapping("/stats")
    @Operation(summary = "Listing counts and engagement")
    public ApiResponse<ListingDtos.SellerListingStats> stats() {
        return ApiResponse.ok(listingService.sellerStats());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a listing as draft or submit for approval")
    public ApiResponse<ListingDtos.ListingDetail> create(@Valid @RequestBody ListingDtos.ListingRequest request) {
        return ApiResponse.created("Service listing saved", listingService.create(request));
    }

    @GetMapping("/{listingId}")
    @Operation(summary = "Get own listing")
    public ApiResponse<ListingDtos.ListingDetail> detail(@PathVariable Long listingId) {
        return ApiResponse.ok(listingService.mineDetail(listingId));
    }

    @PutMapping("/{listingId}")
    @Operation(summary = "Update a listing; published listings return to moderation")
    public ApiResponse<ListingDtos.ListingDetail> update(@PathVariable Long listingId, @Valid @RequestBody ListingDtos.ListingRequest request) {
        return ApiResponse.ok("Service listing updated", listingService.update(listingId, request));
    }

    @PostMapping("/{listingId}/submit")
    @Operation(summary = "Submit listing for administrator approval")
    public ApiResponse<ListingDtos.ListingDetail> submit(@PathVariable Long listingId) {
        return ApiResponse.ok("Submitted for approval", listingService.submit(listingId));
    }

    @PostMapping("/{listingId}/deactivate")
    @Operation(summary = "Deactivate a listing")
    public ApiResponse<ListingDtos.ListingDetail> deactivate(@PathVariable Long listingId, @Valid @RequestBody RemarksRequest request) {
        return ApiResponse.ok("Listing deactivated", listingService.deactivate(listingId, request.remarks()));
    }

    @DeleteMapping("/{listingId}")
    @Operation(summary = "Delete a draft listing")
    public ApiResponse<Void> delete(@PathVariable Long listingId) {
        listingService.delete(listingId);
        return ApiResponse.message("Listing deleted");
    }

    @PostMapping(value = "/{listingId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Attach a supporting document or portfolio file")
    public ApiResponse<ListingDtos.AttachmentResponse> attach(@PathVariable Long listingId,
                                                              @RequestParam(value = "label", required = false) String label,
                                                              @RequestPart("file") MultipartFile file) {
        return ApiResponse.created("Attachment uploaded", listingService.addAttachment(listingId, label, file));
    }

    @DeleteMapping("/{listingId}/attachments/{attachmentId}")
    @Operation(summary = "Remove an attachment")
    public ApiResponse<Void> detach(@PathVariable Long listingId, @PathVariable Long attachmentId) {
        listingService.removeAttachment(listingId, attachmentId);
        return ApiResponse.message("Attachment removed");
    }

    @GetMapping("/{listingId}/history")
    @Operation(summary = "Moderation history of a listing")
    public ApiResponse<List<ListingDtos.ModerationResponse>> history(@PathVariable Long listingId) {
        return ApiResponse.ok(listingService.history(listingId));
    }
}
