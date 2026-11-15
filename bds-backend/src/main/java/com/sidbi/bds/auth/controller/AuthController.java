package com.sidbi.bds.auth.controller;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.dto.AuthDtos;
import com.sidbi.bds.auth.dto.BuyerDtos;
import com.sidbi.bds.auth.dto.SellerDtos;
import com.sidbi.bds.auth.service.AuthService;
import com.sidbi.bds.auth.service.RegistrationService;
import com.sidbi.bds.auth.service.UserMapper;
import com.sidbi.bds.common.api.ApiResponse;
import com.sidbi.bds.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, OTP and session management")
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;

    @GetMapping("/public-key")
    @Operation(summary = "Get RSA public key used to encrypt passwords in transit")
    public ApiResponse<AuthDtos.PublicKeyResponse> publicKey() {
        return ApiResponse.ok(authService.publicKey());
    }

    @GetMapping("/meta")
    @Operation(summary = "Get onboarding reference data")
    public ApiResponse<AuthDtos.MetaResponse> meta() {
        Map<String, List<String>> mandatory = new LinkedHashMap<>();
        for (Enums.ProviderType type : Enums.ProviderType.values()) {
            mandatory.put(type.name(), UserMapper.mandatoryDocuments(type, null, null).stream().map(Enum::name).toList());
        }
        return ApiResponse.ok(new AuthDtos.MetaResponse(
                names(Enums.Constitution.values()),
                names(Enums.ProviderType.values()),
                names(Enums.PurchaserType.values()),
                names(Enums.DocumentType.values()),
                mandatory));
    }

    @PostMapping("/otp/send")
    @Operation(summary = "Send a verification OTP for registration or password reset")
    public ApiResponse<AuthDtos.OtpSendResponse> sendOtp(@Valid @RequestBody AuthDtos.OtpSendRequest request) {
        return ApiResponse.ok("OTP sent", registrationService.sendOtp(request));
    }

    @PostMapping(value = "/register/seller", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Self-register as a service provider with verification documents")
    public ApiResponse<AuthDtos.RegistrationResponse> registerSellerMultipart(
            @Valid @RequestPart("data") SellerDtos.SellerRegistrationRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "fileTypes", required = false) List<Enums.DocumentType> fileTypes) {
        return ApiResponse.created("Registration submitted", registrationService.registerSeller(request, files, fileTypes));
    }

    @PostMapping(value = "/register/seller", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Self-register as a service provider")
    public ApiResponse<AuthDtos.RegistrationResponse> registerSeller(@Valid @RequestBody SellerDtos.SellerRegistrationRequest request) {
        return ApiResponse.created("Registration submitted", registrationService.registerSeller(request, null, null));
    }

    @PostMapping("/register/buyer")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Self-register as a purchaser")
    public ApiResponse<AuthDtos.RegistrationResponse> registerBuyer(@Valid @RequestBody BuyerDtos.BuyerRegistrationRequest request) {
        return ApiResponse.created("Registration submitted", registrationService.registerBuyer(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email/mobile and password")
    public ApiResponse<AuthDtos.LoginResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        AuthDtos.LoginResponse response = authService.login(request);
        return ApiResponse.ok(response.otpRequired() ? "OTP sent to registered email" : "Login successful", response);
    }

    @PostMapping("/login/verify")
    @Operation(summary = "Complete login with the OTP")
    public ApiResponse<AuthDtos.TokenResponse> verify(@Valid @RequestBody AuthDtos.OtpVerifyRequest request) {
        return ApiResponse.ok("Login successful", authService.verifyLogin(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and obtain a new access token")
    public ApiResponse<AuthDtos.TokenResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the refresh token")
    public ApiResponse<Void> logout(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        authService.logout(request);
        return ApiResponse.message("Logged out");
    }

    @PostMapping("/password/forgot")
    @Operation(summary = "Send a password reset OTP")
    public ApiResponse<AuthDtos.OtpSendResponse> forgot(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        return ApiResponse.ok("If the account exists, an OTP has been sent", authService.forgotPassword(request));
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Reset password using OTP")
    public ApiResponse<Void> reset(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.message("Password reset successfully");
    }

    @PostMapping("/password/change")
    @Operation(summary = "Change password for the signed-in user")
    public ApiResponse<Void> change(@Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        authService.changePassword(CurrentUser.id(), request);
        return ApiResponse.message("Password changed successfully");
    }

    @GetMapping("/me")
    @Operation(summary = "Get the signed-in user")
    public ApiResponse<AuthDtos.MeResponse> me() {
        return ApiResponse.ok(authService.me(CurrentUser.id()));
    }

    @PutMapping("/me/preferences")
    @Operation(summary = "Update notification preferences")
    public ApiResponse<AuthDtos.MeResponse> preferences(@Valid @RequestBody AuthDtos.PreferencesRequest request) {
        return ApiResponse.ok("Preferences updated", authService.updatePreferences(CurrentUser.id(), request));
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }
}
