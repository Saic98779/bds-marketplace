package com.sidbi.bds.auth.controller;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.dto.AdminDtos;
import com.sidbi.bds.auth.dto.BuyerDtos;
import com.sidbi.bds.auth.dto.SellerDtos;
import com.sidbi.bds.auth.service.AdminUserService;
import com.sidbi.bds.auth.service.RegistrationService;
import com.sidbi.bds.common.api.ApiResponse;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.api.RequiredRemarksRequest;
import com.sidbi.bds.common.api.RemarksRequest;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users", description = "Seller verification, purchaser management, privileges and administrator accounts")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final RegistrationService registrationService;

    @GetMapping
    @Operation(summary = "Search users by role, status, state or keyword")
    public ApiResponse<PageResponse<AdminDtos.AdminUserRow>> list(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Enums.UserStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminUserService.list(new AdminUserService.UserFilter(role, status, q, state), page, size));
    }

    @GetMapping("/permissions")
    @Operation(summary = "List all assignable privileges")
    public ApiResponse<List<AdminDtos.PermissionInfo>> permissions() {
        return ApiResponse.ok(adminUserService.permissionCatalog());
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get full user detail including profile, documents and verification history")
    public ApiResponse<AdminDtos.AdminUserDetail> detail(@PathVariable Long userId) {
        return ApiResponse.ok(adminUserService.detail(userId));
    }

    @PostMapping("/sellers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Onboard a seller on their behalf")
    public ApiResponse<AdminDtos.CreatedUserResponse> createSeller(@Valid @RequestBody SellerDtos.SellerRegistrationRequest request) {
        CurrentUser.require(Permission.SELLER_VERIFY);
        return ApiResponse.created("Seller created", registrationService.adminCreateSeller(request));
    }

    @PostMapping("/buyers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Onboard a purchaser on their behalf")
    public ApiResponse<AdminDtos.CreatedUserResponse> createBuyer(@Valid @RequestBody BuyerDtos.BuyerRegistrationRequest request) {
        CurrentUser.require(Permission.BUYER_MANAGE);
        return ApiResponse.created("Purchaser created", registrationService.adminCreateBuyer(request));
    }

    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an administrator with selected privileges")
    public ApiResponse<AdminDtos.CreatedUserResponse> createAdmin(@Valid @RequestBody AdminDtos.AdminCreateRequest request) {
        CurrentUser.require(Permission.USER_ADMIN);
        return ApiResponse.created("Administrator created", registrationService.adminCreateAdministrator(request));
    }

    @PostMapping("/{userId}/approve")
    @Operation(summary = "Approve an application and grant privileges")
    public ApiResponse<AdminDtos.AdminUserDetail> approve(@PathVariable Long userId, @Valid @RequestBody AdminDtos.AdminActionRequest request) {
        return ApiResponse.ok("Account approved", adminUserService.approve(userId, request));
    }

    @PostMapping("/{userId}/reject")
    @Operation(summary = "Reject an application")
    public ApiResponse<AdminDtos.AdminUserDetail> reject(@PathVariable Long userId, @Valid @RequestBody RequiredRemarksRequest request) {
        return ApiResponse.ok("Application rejected", adminUserService.reject(userId, request.remarks()));
    }

    @PostMapping("/{userId}/request-info")
    @Operation(summary = "Request additional information from the applicant")
    public ApiResponse<AdminDtos.AdminUserDetail> requestInfo(@PathVariable Long userId, @Valid @RequestBody RequiredRemarksRequest request) {
        return ApiResponse.ok("Additional information requested", adminUserService.requestInfo(userId, request.remarks()));
    }

    @PostMapping("/{userId}/suspend")
    @Operation(summary = "Suspend an active account")
    public ApiResponse<AdminDtos.AdminUserDetail> suspend(@PathVariable Long userId, @Valid @RequestBody RequiredRemarksRequest request) {
        return ApiResponse.ok("Account suspended", adminUserService.suspend(userId, request.remarks()));
    }

    @PostMapping("/{userId}/activate")
    @Operation(summary = "Activate a suspended, deactivated or rejected account")
    public ApiResponse<AdminDtos.AdminUserDetail> activate(@PathVariable Long userId, @Valid @RequestBody RemarksRequest request) {
        return ApiResponse.ok("Account activated", adminUserService.activate(userId, request.remarks()));
    }

    @PutMapping("/{userId}/permissions")
    @Operation(summary = "Replace the privileges granted to a user")
    public ApiResponse<AdminDtos.AdminUserDetail> permissions(@PathVariable Long userId, @Valid @RequestBody AdminDtos.PermissionsRequest request) {
        return ApiResponse.ok("Privileges updated", adminUserService.updatePermissions(userId, request.permissions()));
    }

    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Issue a temporary password")
    public ApiResponse<AdminDtos.CreatedUserResponse> resetPassword(@PathVariable Long userId) {
        return ApiResponse.ok("Temporary password issued", adminUserService.resetPassword(userId));
    }

    @PostMapping("/{userId}/unlock")
    @Operation(summary = "Unlock an account locked after failed logins")
    public ApiResponse<Void> unlock(@PathVariable Long userId) {
        adminUserService.unlock(userId);
        return ApiResponse.message("Account unlocked");
    }

    @PutMapping("/{userId}/featured")
    @Operation(summary = "Mark a seller as featured on the homepage")
    public ApiResponse<Void> featured(@PathVariable Long userId, @RequestBody Map<String, Boolean> body) {
        adminUserService.setFeatured(userId, Boolean.TRUE.equals(body.get("featured")));
        return ApiResponse.message("Featured flag updated");
    }

    @GetMapping("/{userId}/documents/{documentId}/download")
    @Operation(summary = "Download a seller verification document")
    public ResponseEntity<Resource> download(@PathVariable Long userId, @PathVariable Long documentId) {
        return adminUserService.downloadDocument(userId, documentId);
    }

    @PutMapping("/{userId}/documents/{documentId}/verify")
    @Operation(summary = "Mark a seller document as verified or not verified")
    public ApiResponse<SellerDtos.DocumentResponse> verifyDocument(@PathVariable Long userId, @PathVariable Long documentId,
                                                                   @Valid @RequestBody AdminDtos.DocumentVerifyRequest request) {
        return ApiResponse.ok("Document updated", adminUserService.verifyDocument(userId, documentId, request));
    }
}
