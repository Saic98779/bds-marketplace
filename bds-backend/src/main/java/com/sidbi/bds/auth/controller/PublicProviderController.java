package com.sidbi.bds.auth.controller;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.dto.ProviderDtos;
import com.sidbi.bds.auth.service.ProviderDirectoryService;
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

@RestController
@RequestMapping("/api/v1/public/providers")
@RequiredArgsConstructor
@Tag(name = "Provider Directory", description = "Public directory of verified and active service providers")
public class PublicProviderController {

    private final ProviderDirectoryService directoryService;

    @GetMapping
    @Operation(summary = "Search verified providers")
    public ApiResponse<PageResponse<ProviderDtos.ProviderCard>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Enums.ProviderType providerType,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) String deliveryMode,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.ok(directoryService.search(new ProviderDirectoryService.ProviderFilter(
                q, state, district, categoryId, providerType, minExperience, deliveryMode, featured, sort), page, size));
    }

    @GetMapping("/{sellerId}")
    @Operation(summary = "Get a verified provider profile")
    public ApiResponse<ProviderDtos.ProviderProfile> profile(@PathVariable Long sellerId) {
        return ApiResponse.ok(directoryService.profile(sellerId));
    }
}
