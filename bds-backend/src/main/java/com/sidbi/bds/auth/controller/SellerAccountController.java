package com.sidbi.bds.auth.controller;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.dto.SellerDtos;
import com.sidbi.bds.auth.service.ProfileService;
import com.sidbi.bds.auth.service.SellerDocumentService;
import com.sidbi.bds.auth.service.UserMapper;
import com.sidbi.bds.common.api.ApiResponse;
import com.sidbi.bds.common.api.RemarksRequest;
import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/sellers/me")
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Seller Account", description = "Seller business profile, documents and verification")
public class SellerAccountController {

    private final ProfileService profileService;
    private final SellerDocumentService documentService;
    private final NotificationClient notificationClient;
    private final UserMapper mapper;

    @GetMapping("/profile")
    @Operation(summary = "Get own business profile")
    public ApiResponse<SellerDtos.SellerProfileResponse> profile() {
        return ApiResponse.ok(profileService.sellerProfile(CurrentUser.subjectId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update business profile; identity changes trigger re-verification")
    public ApiResponse<SellerDtos.SellerProfileResponse> update(@Valid @RequestBody SellerDtos.SellerProfileUpdateRequest request) {
        return ApiResponse.ok("Profile updated", profileService.updateSeller(CurrentUser.subjectId(), request));
    }

    @GetMapping("/verification")
    @Operation(summary = "Get verification status, document checklist and history")
    public ApiResponse<SellerDtos.VerificationStatusResponse> verification() {
        return ApiResponse.ok(profileService.verification(CurrentUser.subjectId()));
    }

    @PostMapping("/verification/resubmit")
    @Operation(summary = "Resubmit application after additional information was requested")
    public ApiResponse<SellerDtos.VerificationStatusResponse> resubmit(@Valid @RequestBody RemarksRequest request) {
        return ApiResponse.ok("Application resubmitted", profileService.resubmit(CurrentUser.subjectId(), request.remarks()));
    }

    @GetMapping("/documents")
    @Operation(summary = "List uploaded verification documents")
    public ApiResponse<List<SellerDtos.DocumentResponse>> documents() {
        return ApiResponse.ok(documentService.list(CurrentUser.subjectId()).stream().map(mapper::document).toList());
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a verification document")
    public ApiResponse<SellerDtos.DocumentResponse> upload(@RequestParam("documentType") Enums.DocumentType documentType,
                                                           @RequestPart("file") MultipartFile file) {
        Long userId = CurrentUser.subjectId();
        if (CurrentUser.get().isAdmin()) {
            profileService.verification(userId);
        }
        SellerDtos.DocumentResponse response = mapper.document(documentService.store(userId, documentType, file));
        notificationClient.audit("SELLER_DOCUMENT_UPLOADED", "SELLER_DOCUMENT", response.id(), documentType.name());
        return ApiResponse.created("Document uploaded", response);
    }

    @GetMapping("/documents/{documentId}/download")
    @Operation(summary = "Download own document")
    public ResponseEntity<Resource> download(@PathVariable Long documentId) {
        return documentService.download(CurrentUser.subjectId(), documentId);
    }

    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "Delete an unverified document")
    public ApiResponse<Void> delete(@PathVariable Long documentId) {
        documentService.delete(CurrentUser.subjectId(), documentId);
        notificationClient.audit("SELLER_DOCUMENT_DELETED", "SELLER_DOCUMENT", documentId, null);
        return ApiResponse.message("Document deleted");
    }
}
