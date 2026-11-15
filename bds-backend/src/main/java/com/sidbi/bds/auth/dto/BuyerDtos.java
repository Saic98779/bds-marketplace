package com.sidbi.bds.auth.dto;

import com.sidbi.bds.auth.domain.Enums;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

import static com.sidbi.bds.auth.dto.SellerDtos.GSTIN_REGEX;
import static com.sidbi.bds.auth.dto.SellerDtos.MOBILE_REGEX;
import static com.sidbi.bds.auth.dto.SellerDtos.PINCODE_REGEX;
import static com.sidbi.bds.auth.dto.SellerDtos.UDYAM_REGEX;

public final class BuyerDtos {

    public static final String OPTIONAL_PAN_REGEX = "^$|^[A-Z]{5}[0-9]{4}[A-Z]$";

    private BuyerDtos() {
    }

    public record BuyerRegistrationRequest(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Pattern(regexp = MOBILE_REGEX, message = "Enter a valid 10 digit mobile number") String mobile,
            @Size(max = 1024) String password,
            @Size(max = 6) String emailOtp,
            @NotBlank @Size(max = 200) String organisationName,
            Enums.Constitution constitution,
            @NotNull Enums.PurchaserType purchaserType,
            @Size(max = 100) String designation,
            @Pattern(regexp = OPTIONAL_PAN_REGEX, message = "Enter a valid PAN") String pan,
            @Pattern(regexp = GSTIN_REGEX, message = "Enter a valid GSTIN") String gstin,
            @Pattern(regexp = UDYAM_REGEX, message = "Enter a valid Udyam number") String udyamNumber,
            @NotBlank @Size(max = 150) String industrySector,
            @Size(max = 1000) String businessActivity,
            @NotBlank @Size(max = 500) String addressLine,
            @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @NotBlank @Size(max = 100) String district,
            @Pattern(regexp = PINCODE_REGEX, message = "Enter a valid pincode") String pincode,
            @Size(max = 250) String website,
            Boolean declarationAccepted
    ) {
    }

    public record BuyerProfileUpdateRequest(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Size(max = 200) String organisationName,
            Enums.Constitution constitution,
            @NotNull Enums.PurchaserType purchaserType,
            @Size(max = 100) String designation,
            @Pattern(regexp = OPTIONAL_PAN_REGEX, message = "Enter a valid PAN") String pan,
            @Pattern(regexp = GSTIN_REGEX, message = "Enter a valid GSTIN") String gstin,
            @Pattern(regexp = UDYAM_REGEX, message = "Enter a valid Udyam number") String udyamNumber,
            @NotBlank @Size(max = 150) String industrySector,
            @Size(max = 1000) String businessActivity,
            @NotBlank @Size(max = 500) String addressLine,
            @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @NotBlank @Size(max = 100) String district,
            @Pattern(regexp = PINCODE_REGEX, message = "Enter a valid pincode") String pincode,
            @Size(max = 250) String website
    ) {
    }

    public record BuyerProfileResponse(
            Long userId,
            String referenceNo,
            String fullName,
            String email,
            String mobile,
            Enums.UserStatus status,
            String statusRemarks,
            String organisationName,
            Enums.Constitution constitution,
            Enums.PurchaserType purchaserType,
            String contactPerson,
            String designation,
            String pan,
            String gstin,
            String udyamNumber,
            String industrySector,
            String businessActivity,
            String addressLine,
            String city,
            String state,
            String district,
            String pincode,
            String website,
            LocalDateTime registeredAt
    ) {
    }
}
