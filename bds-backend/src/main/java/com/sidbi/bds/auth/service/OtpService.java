package com.sidbi.bds.auth.service;

import com.sidbi.bds.auth.config.AuthProperties;
import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.domain.OtpToken;
import com.sidbi.bds.auth.repository.OtpTokenRepository;
import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.crypto.FieldCrypto;
import com.sidbi.bds.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository repository;
    private final FieldCrypto crypto;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;
    private final NotificationClient notificationClient;
    private final SecureRandom random = new SecureRandom();

    public record IssuedOtp(String code, String challengeToken, int expiresInSeconds) {
    }

    @Transactional
    public IssuedOtp issue(String target, Enums.OtpPurpose purpose, Long userId, boolean withChallenge) {
        String targetHash = crypto.blindIndex(target);
        repository.findFirstByTargetHashAndPurposeAndConsumedFalseOrderByCreatedAtDesc(targetHash, purpose)
                .ifPresent(existing -> {
                    long age = Duration.between(existing.getCreatedAt(), LocalDateTime.now()).getSeconds();
                    if (age < properties.getOtpResendSeconds()) {
                        throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                                "Please wait " + (properties.getOtpResendSeconds() - age) + " seconds before requesting another OTP");
                    }
                });
        repository.invalidate(targetHash, purpose);
        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpToken token = new OtpToken();
        token.setTargetHash(targetHash);
        token.setPurpose(purpose);
        token.setCodeHash(passwordEncoder.encode(code));
        token.setUserId(userId);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(properties.getOtpExpiryMinutes()));
        String challenge = null;
        if (withChallenge) {
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            token.setChallengeHash(FieldCrypto.sha256(challenge));
        }
        repository.save(token);
        if (target.contains("@")) {
            notificationClient.email(target, "BDS Marketplace verification code",
                    "Your BDS Marketplace one-time password is " + code + ". It is valid for "
                            + properties.getOtpExpiryMinutes() + " minutes. Do not share this code with anyone.");
        }
        return new IssuedOtp(code, challenge, properties.getOtpExpiryMinutes() * 60);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void verify(String target, Enums.OtpPurpose purpose, String code) {
        OtpToken token = repository.findFirstByTargetHashAndPurposeAndConsumedFalseOrderByCreatedAtDesc(crypto.blindIndex(target), purpose)
                .orElseThrow(() -> BusinessException.badRequest("OTP is invalid or has expired. Request a new OTP."));
        check(token, code);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public OtpToken verifyChallenge(String challengeToken, String code) {
        OtpToken token = repository.findByChallengeHashAndConsumedFalse(FieldCrypto.sha256(challengeToken))
                .orElseThrow(() -> BusinessException.badRequest("Login session has expired. Please sign in again."));
        check(token, code);
        return token;
    }

    private void check(OtpToken token, String code) {
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setConsumed(true);
            throw BusinessException.badRequest("OTP has expired. Request a new OTP.");
        }
        if (token.getAttempts() >= properties.getOtpMaxAttempts()) {
            token.setConsumed(true);
            throw BusinessException.badRequest("Maximum OTP attempts exceeded. Request a new OTP.");
        }
        token.setAttempts(token.getAttempts() + 1);
        if (code == null || !passwordEncoder.matches(code, token.getCodeHash())) {
            throw BusinessException.badRequest("Incorrect OTP");
        }
        token.setConsumed(true);
        token.setVerified(true);
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 300_000)
    @Transactional
    public void purge() {
        repository.purgeExpired(LocalDateTime.now().minusDays(1));
    }
}
