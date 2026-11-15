package com.sidbi.bds.auth.service;

import com.sidbi.bds.auth.config.AuthProperties;
import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.domain.OtpToken;
import com.sidbi.bds.auth.domain.RefreshToken;
import com.sidbi.bds.auth.domain.User;
import com.sidbi.bds.auth.dto.AuthDtos;
import com.sidbi.bds.auth.repository.PurchaserProfileRepository;
import com.sidbi.bds.auth.repository.RefreshTokenRepository;
import com.sidbi.bds.auth.repository.SellerProfileRepository;
import com.sidbi.bds.auth.repository.UserRepository;
import com.sidbi.bds.auth.security.TransportCrypto;
import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.client.dto.AuditEvent;
import com.sidbi.bds.common.crypto.FieldCrypto;
import com.sidbi.bds.common.crypto.Masking;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.security.AuthUser;
import com.sidbi.bds.common.security.JwtService;
import com.sidbi.bds.common.security.Role;
import com.sidbi.bds.common.security.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final PurchaserProfileRepository purchaserProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final FieldCrypto crypto;
    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final AuthProperties properties;
    private final OtpService otpService;
    private final TransportCrypto transportCrypto;
    private final UserMapper mapper;
    private final NotificationClient notificationClient;
    private final SecureRandom random = new SecureRandom();

    public AuthDtos.PublicKeyResponse publicKey() {
        return new AuthDtos.PublicKeyResponse(transportCrypto.keyId(), "RSA-OAEP-256", transportCrypto.publicKeyBase64());
    }

    public Optional<User> findByUsername(String username) {
        String value = username == null ? "" : username.trim();
        if (value.contains("@")) {
            return userRepository.findByEmailHash(crypto.blindIndex(value));
        }
        return userRepository.findByMobileHash(crypto.blindIndex(value));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        String password = transportCrypto.reveal(request.password());
        User user = findByUsername(request.username())
                .orElseThrow(() -> BusinessException.unauthorized("Invalid credentials"));
        if (user.isLocked()) {
            throw new BusinessException(HttpStatus.LOCKED, "Account is temporarily locked due to repeated failed attempts. Try again later or reset your password.");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            if (user.getFailedAttempts() >= properties.getMaxLoginAttempts()) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(properties.getLockMinutes()));
                user.setFailedAttempts(0);
                notificationClient.audit("ACCOUNT_LOCKED", "USER", user.getId(), "Locked after repeated failed logins");
            }
            throw BusinessException.unauthorized("Invalid credentials");
        }
        if (request.portal() != null && request.portal() != user.getRole()) {
            throw BusinessException.unauthorized("This account is not registered for the " + request.portal().name().toLowerCase() + " portal");
        }
        assertLoginAllowed(user);
        user.setFailedAttempts(0);
        if (properties.isLoginOtpEnabled()) {
            OtpService.IssuedOtp otp = otpService.issue(user.getEmail(), Enums.OtpPurpose.LOGIN, user.getId(), true);
            return new AuthDtos.LoginResponse(true, otp.challengeToken(), Masking.email(user.getEmail()), otp.expiresInSeconds(),
                    properties.isExposeOtpInResponse() ? otp.code() : null, null);
        }
        return new AuthDtos.LoginResponse(false, null, null, null, null, issueTokens(user));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthDtos.TokenResponse verifyLogin(AuthDtos.OtpVerifyRequest request) {
        OtpToken token = otpService.verifyChallenge(request.challengeToken(), request.otp());
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> BusinessException.unauthorized("Invalid login session"));
        assertLoginAllowed(user);
        user.setEmailVerified(true);
        return issueTokens(user);
    }

    @Transactional
    public AuthDtos.TokenResponse refresh(AuthDtos.RefreshRequest request) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(FieldCrypto.sha256(request.refreshToken()))
                .orElseThrow(() -> BusinessException.unauthorized("Session expired. Please sign in again."));
        if (token.isRevoked() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            if (token.isRevoked()) {
                refreshTokenRepository.revokeAllForUser(token.getUserId());
            }
            throw BusinessException.unauthorized("Session expired. Please sign in again.");
        }
        token.setRevoked(true);
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> BusinessException.unauthorized("Session expired. Please sign in again."));
        assertLoginAllowed(user);
        return createTokens(user);
    }

    @Transactional
    public void logout(AuthDtos.RefreshRequest request) {
        refreshTokenRepository.findByTokenHash(FieldCrypto.sha256(request.refreshToken()))
                .ifPresent(t -> t.setRevoked(true));
    }

    @Transactional
    public AuthDtos.OtpSendResponse forgotPassword(AuthDtos.ForgotPasswordRequest request) {
        Optional<User> user = findByUsername(request.username());
        if (user.isEmpty()) {
            return new AuthDtos.OtpSendResponse(Masking.email(request.username()), properties.getOtpExpiryMinutes() * 60,
                    properties.getOtpResendSeconds(), null);
        }
        OtpService.IssuedOtp otp = otpService.issue(user.get().getEmail(), Enums.OtpPurpose.PASSWORD_RESET, user.get().getId(), false);
        return new AuthDtos.OtpSendResponse(Masking.email(user.get().getEmail()), otp.expiresInSeconds(),
                properties.getOtpResendSeconds(), properties.isExposeOtpInResponse() ? otp.code() : null);
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest request) {
        User user = findByUsername(request.username())
                .orElseThrow(() -> BusinessException.badRequest("OTP is invalid or has expired"));
        otpService.verify(user.getEmail(), Enums.OtpPurpose.PASSWORD_RESET, request.otp());
        String password = transportCrypto.reveal(request.newPassword());
        PasswordPolicy.validate(password);
        applyPassword(user, password);
        user.setLockedUntil(null);
        user.setFailedAttempts(0);
        refreshTokenRepository.revokeAllForUser(user.getId());
        notificationClient.notify(user.getId(), "PASSWORD_CHANGED", "SECURITY", null, Map.of("name", user.getFullName()));
        notificationClient.audit("PASSWORD_RESET", "USER", user.getId(), "Password reset via OTP");
    }

    @Transactional
    public void changePassword(Long userId, AuthDtos.ChangePasswordRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> BusinessException.notFound("User not found"));
        if (!passwordEncoder.matches(transportCrypto.reveal(request.currentPassword()), user.getPasswordHash())) {
            throw BusinessException.badRequest("Current password is incorrect");
        }
        String password = transportCrypto.reveal(request.newPassword());
        PasswordPolicy.validate(password);
        if (passwordEncoder.matches(password, user.getPasswordHash())) {
            throw BusinessException.badRequest("New password must be different from the current password");
        }
        applyPassword(user, password);
        notificationClient.notify(user.getId(), "PASSWORD_CHANGED", "SECURITY", null, Map.of("name", user.getFullName()));
        notificationClient.audit("PASSWORD_CHANGED", "USER", user.getId(), null);
    }

    @Transactional(readOnly = true)
    public AuthDtos.MeResponse me(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> BusinessException.notFound("User not found"));
        return mapper.me(user, organisationName(user));
    }

    @Transactional
    public AuthDtos.MeResponse updatePreferences(Long userId, AuthDtos.PreferencesRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> BusinessException.notFound("User not found"));
        user.setEmailAlerts(request.emailAlerts());
        return mapper.me(user, organisationName(user));
    }

    public void applyPassword(User user, String password) {
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(false);
    }

    public String organisationName(User user) {
        if (user.getRole() == Role.SELLER) {
            return sellerProfileRepository.findByUserId(user.getId()).map(p -> p.getOrganisationName()).orElse(null);
        }
        if (user.getRole() == Role.BUYER) {
            return purchaserProfileRepository.findByUserId(user.getId()).map(p -> p.getOrganisationName()).orElse(null);
        }
        return "BDS Marketplace";
    }

    private void assertLoginAllowed(User user) {
        if (user.getStatus() == Enums.UserStatus.REJECTED) {
            throw BusinessException.forbidden("Your application was not approved. Contact support for assistance.");
        }
        if (user.getStatus() == Enums.UserStatus.DEACTIVATED) {
            throw BusinessException.forbidden("This account has been deactivated.");
        }
        if (user.getRole() == Role.ADMIN && user.getStatus() != Enums.UserStatus.ACTIVE) {
            throw BusinessException.forbidden("Administrator account is not active.");
        }
    }

    private AuthDtos.TokenResponse issueTokens(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        notificationClient.audit(new AuditEvent("auth", user.getId(), user.getRole().name(),
                user.getFullName(), "LOGIN", "USER", String.valueOf(user.getId()), null, null));
        return createTokens(user);
    }

    private AuthDtos.TokenResponse createTokens(User user) {
        String organisation = organisationName(user);
        AuthUser authUser = new AuthUser(user.getId(),
                organisation != null && user.getRole() != Role.ADMIN ? organisation : user.getFullName(),
                user.getRole(), user.getStatus().name(), user.getReferenceNo(), mapper.permissionCodes(user));
        String accessToken = jwtService.createAccessToken(authUser);
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setTokenHash(FieldCrypto.sha256(refresh));
        token.setExpiresAt(LocalDateTime.now().plusDays(securityProperties.getRefreshTokenDays()));
        refreshTokenRepository.save(token);
        return new AuthDtos.TokenResponse(accessToken, refresh, "Bearer", jwtService.accessTokenSeconds(), mapper.me(user, organisation));
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 600_000)
    @Transactional
    public void purgeTokens() {
        refreshTokenRepository.purgeExpired(LocalDateTime.now().minusDays(1));
    }
}
