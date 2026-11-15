package com.sidbi.bds.auth.dto;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.common.security.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record PublicKeyResponse(String keyId, String algorithm, String publicKey) {
    }

    public record OtpSendRequest(
            @NotBlank @Size(max = 150) String target,
            @NotNull Enums.OtpPurpose purpose
    ) {
    }

    public record OtpSendResponse(String maskedTarget, int expiresInSeconds, int resendAfterSeconds, String devOtp) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 150) String username,
            @NotBlank @Size(max = 1024) String password,
            Role portal
    ) {
    }

    public record LoginResponse(
            boolean otpRequired,
            String challengeToken,
            String maskedDestination,
            Integer expiresInSeconds,
            String devOtp,
            TokenResponse tokens
    ) {
    }

    public record OtpVerifyRequest(
            @NotBlank String challengeToken,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "OTP must be 6 digits") String otp
    ) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            MeResponse user
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record ForgotPasswordRequest(@NotBlank @Size(max = 150) String username) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(max = 150) String username,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "OTP must be 6 digits") String otp,
            @NotBlank @Size(max = 1024) String newPassword
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank @Size(max = 1024) String currentPassword,
            @NotBlank @Size(max = 1024) String newPassword
    ) {
    }

    public record PreferencesRequest(boolean emailAlerts) {
    }

    public record MeResponse(
            Long id,
            String fullName,
            String email,
            String mobile,
            Role role,
            Enums.UserStatus status,
            String referenceNo,
            String organisationName,
            String designation,
            Set<String> permissions,
            boolean mustChangePassword,
            boolean emailAlerts,
            String statusRemarks,
            LocalDateTime lastLoginAt
    ) {
    }

    public record RegistrationResponse(Long userId, String referenceNo, Enums.UserStatus status, String message) {
    }

    public record MetaResponse(
            List<String> constitutions,
            List<String> providerTypes,
            List<String> purchaserTypes,
            List<String> documentTypes,
            Map<String, List<String>> mandatoryDocuments
    ) {
    }
}
