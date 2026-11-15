package com.sidbi.bds.auth.controller;

import com.sidbi.bds.auth.dto.BuyerDtos;
import com.sidbi.bds.auth.service.ProfileService;
import com.sidbi.bds.common.api.ApiResponse;
import com.sidbi.bds.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/buyers/me")
@PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Purchaser Account", description = "Purchaser organisation profile")
public class BuyerAccountController {

    private final ProfileService profileService;

    @GetMapping("/profile")
    @Operation(summary = "Get own organisation profile")
    public ApiResponse<BuyerDtos.BuyerProfileResponse> profile() {
        return ApiResponse.ok(profileService.buyerProfile(CurrentUser.subjectId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update organisation profile")
    public ApiResponse<BuyerDtos.BuyerProfileResponse> update(@Valid @RequestBody BuyerDtos.BuyerProfileUpdateRequest request) {
        return ApiResponse.ok("Profile updated", profileService.updateBuyer(CurrentUser.subjectId(), request));
    }
}
