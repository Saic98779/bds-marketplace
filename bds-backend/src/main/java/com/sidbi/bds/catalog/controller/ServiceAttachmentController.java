package com.sidbi.bds.catalog.controller;

import com.sidbi.bds.catalog.service.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Tag(name = "Service Attachments", description = "Authenticated download of listing attachments")
public class ServiceAttachmentController {

    private final ListingService listingService;

    @GetMapping("/{listingId}/attachments/{attachmentId}/download")
    @Operation(summary = "Download a listing attachment")
    public ResponseEntity<Resource> download(@PathVariable Long listingId, @PathVariable Long attachmentId) {
        return listingService.downloadAttachment(listingId, attachmentId);
    }
}
