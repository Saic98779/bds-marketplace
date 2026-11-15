package com.sidbi.bds.auth.dto;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record AdminUserRow(
            Long id,
            String referenceNo,
            String fullName,
            String organisationName,
            String email,
            String mobile,
            Role role,
            Enums.UserStatus status,
            String state,
            String district,
            String typeLabel,
            Set<String> permissions,
            LocalDateTime registeredAt,
            LocalDateTime lastLoginAt
    ) {
    }

    public record AdminUserDetail(
            Long id,
            String referenceNo,
            String fullName,
            String email,
            String mobile,
            Role role,
            Enums.UserStatus status,
            String statusRemarks,
            String designation,
            Set<String> permissions,
            boolean emailVerified,
            boolean locked,
            LocalDateTime registeredAt,
            LocalDateTime lastLoginAt,
            SellerDtos.SellerProfileResponse sellerProfile,
            BuyerDtos.BuyerProfileResponse buyerProfile,
            List<SellerDtos.DocumentResponse> documents,
            List<SellerDtos.RequiredDocument> checklist,
            List<SellerDtos.VerificationRecordResponse> history
    ) {
    }

    public record AdminActionRequest(
            @Size(max = 1000) String remarks,
            Set<Permission> permissions
    ) {
    }

    public record PermissionsRequest(@NotNull Set<Permission> permissions) {
    }

    public record AdminCreateRequest(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Email @Size(max = 150) String email,
            @Pattern(regexp = "^$|^[6-9][0-9]{9}$", message = "Enter a valid 10 digit mobile number") String mobile,
            @Size(max = 100) String designation,
            @NotNull Set<Permission> permissions
    ) {
    }

    public record CreatedUserResponse(Long userId, String referenceNo, String temporaryPassword) {
    }

    public record DocumentVerifyRequest(boolean verified, @Size(max = 500) String remarks) {
    }

    public record PermissionInfo(String code, Role role, String description) {
    }

    public record CountItem(String label, long count) {
    }

    public record TrendPoint(String period, long sellers, long buyers) {
    }

    public record IdentityStats(
            long totalSellers,
            long totalBuyers,
            long pendingSellers,
            long pendingBuyers,
            Map<String, Long> sellersByStatus,
            Map<String, Long> buyersByStatus,
            List<TrendPoint> registrationTrend,
            List<CountItem> sellersByState,
            List<CountItem> sellersByProviderType,
            List<CountItem> buyersByType,
            List<CountItem> buyersByIndustry
    ) {
    }
}
