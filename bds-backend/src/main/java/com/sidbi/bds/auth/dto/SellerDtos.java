package com.sidbi.bds.auth.dto;

import com.sidbi.bds.auth.domain.Enums;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class SellerDtos {

    public static final String PAN_REGEX = "^[A-Z]{5}[0-9]{4}[A-Z]$";
    public static final String GSTIN_REGEX = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$";
    public static final String UDYAM_REGEX = "^$|^UDYAM-[A-Z]{2}-[0-9]{2}-[0-9]{7}$";
    public static final String MOBILE_REGEX = "^[6-9][0-9]{9}$";
    public static final String PINCODE_REGEX = "^$|^[1-9][0-9]{5}$";

    private SellerDtos() {
    }

    public record SellerRegistrationRequest(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Pattern(regexp = MOBILE_REGEX, message = "Enter a valid 10 digit mobile number") String mobile,
            @Size(max = 1024) String password,
            @Size(max = 6) String emailOtp,
            @NotBlank @Size(max = 200) String organisationName,
            @NotNull Enums.Constitution constitution,
            @NotBlank @Pattern(regexp = PAN_REGEX, message = "Enter a valid PAN") String pan,
            @Pattern(regexp = GSTIN_REGEX, message = "Enter a valid GSTIN") String gstin,
            @Pattern(regexp = UDYAM_REGEX, message = "Enter a valid Udyam number (UDYAM-XX-00-0000000)") String udyamNumber,
            @NotBlank @Size(max = 500) String addressLine,
            @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @NotBlank @Size(max = 100) String district,
            @Pattern(regexp = PINCODE_REGEX, message = "Enter a valid pincode") String pincode,
            @NotNull Enums.ProviderType providerType,
            @Size(max = 10) List<Long> categoryIds,
            @Size(max = 10) List<String> categoryNames,
            @Size(max = 25) List<String> expertise,
            @Size(max = 25) List<String> industries,
            @NotNull @Min(0) @Max(80) Integer yearsExperience,
            @Min(1) @Max(100000) Integer teamSize,
            @Size(max = 40) List<String> geographicCoverage,
            @Size(max = 3) List<String> deliveryModes,
            @Size(max = 250) String website,
            @Size(max = 4000) String introduction,
            Boolean declarationAccepted
    ) {
    }

    public record SellerProfileUpdateRequest(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Size(max = 200) String organisationName,
            @NotNull Enums.Constitution constitution,
            @NotBlank @Pattern(regexp = PAN_REGEX, message = "Enter a valid PAN") String pan,
            @Pattern(regexp = GSTIN_REGEX, message = "Enter a valid GSTIN") String gstin,
            @Pattern(regexp = UDYAM_REGEX, message = "Enter a valid Udyam number") String udyamNumber,
            @NotBlank @Size(max = 500) String addressLine,
            @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @NotBlank @Size(max = 100) String district,
            @Pattern(regexp = PINCODE_REGEX, message = "Enter a valid pincode") String pincode,
            @NotNull Enums.ProviderType providerType,
            @Size(max = 10) List<Long> categoryIds,
            @Size(max = 10) List<String> categoryNames,
            @Size(max = 25) List<String> expertise,
            @Size(max = 25) List<String> industries,
            @NotNull @Min(0) @Max(80) Integer yearsExperience,
            @Min(1) @Max(100000) Integer teamSize,
            @Size(max = 40) List<String> geographicCoverage,
            @Size(max = 3) List<String> deliveryModes,
            @Size(max = 250) String website,
            @Size(max = 4000) String introduction,
            @Min(1900) @Max(2100) Integer yearEstablished,
            @Size(max = 2000) String certifications,
            @Size(max = 2000) String teamProfile,
            @Size(max = 4000) String previousAssignments,
            @Size(max = 2000) String clientReferences
    ) {
    }

    public record SellerProfileResponse(
            Long userId,
            String referenceNo,
            String fullName,
            String email,
            String mobile,
            Enums.UserStatus status,
            String statusRemarks,
            String organisationName,
            Enums.Constitution constitution,
            String pan,
            String gstin,
            String udyamNumber,
            String addressLine,
            String city,
            String state,
            String district,
            String pincode,
            Enums.ProviderType providerType,
            List<Long> categoryIds,
            List<String> categoryNames,
            List<String> expertise,
            List<String> industries,
            Integer yearsExperience,
            Integer teamSize,
            List<String> geographicCoverage,
            List<String> deliveryModes,
            String website,
            String introduction,
            Integer yearEstablished,
            String certifications,
            String teamProfile,
            String previousAssignments,
            String clientReferences,
            Double averageRating,
            Integer ratingCount,
            LocalDateTime verifiedAt,
            LocalDate reverificationDue,
            int profileCompletion,
            LocalDateTime registeredAt
    ) {
    }

    public record DocumentResponse(
            Long id,
            Enums.DocumentType documentType,
            String originalName,
            String contentType,
            Long fileSize,
            boolean verified,
            String remarks,
            LocalDateTime uploadedAt
    ) {
    }

    public record RequiredDocument(Enums.DocumentType documentType, boolean mandatory, boolean uploaded, boolean verified) {
    }

    public record VerificationRecordResponse(
            Long id,
            Enums.VerificationAction action,
            Enums.UserStatus fromStatus,
            Enums.UserStatus toStatus,
            String remarks,
            String actorName,
            LocalDateTime createdAt
    ) {
    }

    public record VerificationStatusResponse(
            Enums.UserStatus status,
            String statusRemarks,
            List<RequiredDocument> checklist,
            List<VerificationRecordResponse> history
    ) {
    }
}
