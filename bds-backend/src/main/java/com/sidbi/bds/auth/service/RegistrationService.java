package com.sidbi.bds.auth.service;

import com.sidbi.bds.auth.config.AuthProperties;
import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.domain.PurchaserProfile;
import com.sidbi.bds.auth.domain.SellerProfile;
import com.sidbi.bds.auth.domain.User;
import com.sidbi.bds.auth.domain.VerificationRecord;
import com.sidbi.bds.auth.dto.AdminDtos;
import com.sidbi.bds.auth.dto.AuthDtos;
import com.sidbi.bds.auth.dto.BuyerDtos;
import com.sidbi.bds.auth.dto.SellerDtos;
import com.sidbi.bds.auth.repository.PurchaserProfileRepository;
import com.sidbi.bds.auth.repository.SellerProfileRepository;
import com.sidbi.bds.auth.repository.UserRepository;
import com.sidbi.bds.auth.repository.VerificationRecordRepository;
import com.sidbi.bds.auth.security.TransportCrypto;
import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.client.dto.AuditEvent;
import com.sidbi.bds.common.client.dto.NotificationRequest;
import com.sidbi.bds.common.crypto.FieldCrypto;
import com.sidbi.bds.common.crypto.Masking;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.security.AuthUser;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.security.Role;
import com.sidbi.bds.common.util.Csv;
import com.sidbi.bds.common.util.References;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final PurchaserProfileRepository purchaserProfileRepository;
    private final VerificationRecordRepository verificationRepository;
    private final FieldCrypto crypto;
    private final OtpService otpService;
    private final AuthService authService;
    private final AuthProperties properties;
    private final TransportCrypto transportCrypto;
    private final SellerDocumentService documentService;
    private final SellerSyncService sellerSyncService;
    private final NotificationClient notificationClient;

    @Transactional
    public AuthDtos.OtpSendResponse sendOtp(AuthDtos.OtpSendRequest request) {
        String target = request.target().trim().toLowerCase(Locale.ROOT);
        if (request.purpose() == Enums.OtpPurpose.REGISTRATION) {
            boolean exists = target.contains("@")
                    ? userRepository.existsByEmailHash(crypto.blindIndex(target))
                    : userRepository.existsByMobileHash(crypto.blindIndex(target));
            if (exists) {
                throw BusinessException.conflict("An account already exists with this " + (target.contains("@") ? "email" : "mobile number"));
            }
        } else if (request.purpose() != Enums.OtpPurpose.PASSWORD_RESET) {
            throw BusinessException.badRequest("Unsupported OTP purpose");
        }
        OtpService.IssuedOtp otp = otpService.issue(target, request.purpose(), null, false);
        String masked = target.contains("@") ? Masking.email(target) : Masking.mobile(target);
        return new AuthDtos.OtpSendResponse(masked, otp.expiresInSeconds(), properties.getOtpResendSeconds(),
                properties.isExposeOtpInResponse() ? otp.code() : null);
    }

    @Transactional
    public AuthDtos.RegistrationResponse registerSeller(SellerDtos.SellerRegistrationRequest request,
                                                        List<MultipartFile> files, List<Enums.DocumentType> fileTypes) {
        String password = selfRegistrationChecks(request.email(), request.password(), request.emailOtp(), request.declarationAccepted());
        User user = createSeller(request, password, null);
        documentService.storeAll(user.getId(), files, fileTypes);
        record(user.getId(), Enums.VerificationAction.SUBMITTED, null, user.getStatus(), "Application submitted for verification", user.getId(), user.getFullName());
        notificationClient.notify(user.getId(), "SELLER_REGISTERED", "REGISTRATION", "/seller/profile",
                Map.of("name", user.getFullName(), "reference", user.getReferenceNo(), "organisation", request.organisationName()));
        notificationClient.notify(NotificationRequest.toAdmins(Permission.SELLER_VERIFY.name(), "NEW_SELLER_APPLICATION", "VERIFICATION",
                "/admin/approvals/" + user.getId(), Map.of("organisation", request.organisationName(), "reference", user.getReferenceNo())));
        notificationClient.audit(new AuditEvent("auth", user.getId(), "SELLER", user.getFullName(),
                "SELLER_REGISTERED", "USER", String.valueOf(user.getId()), user.getReferenceNo(), null));
        return new AuthDtos.RegistrationResponse(user.getId(), user.getReferenceNo(), user.getStatus(),
                "Registration submitted. Your application " + user.getReferenceNo() + " is pending verification.");
    }

    @Transactional
    public AuthDtos.RegistrationResponse registerBuyer(BuyerDtos.BuyerRegistrationRequest request) {
        String password = selfRegistrationChecks(request.email(), request.password(), request.emailOtp(), request.declarationAccepted());
        User user = createBuyer(request, password, null, properties.isBuyerAutoApprove());
        record(user.getId(), Enums.VerificationAction.SUBMITTED, null, user.getStatus(), "Purchaser registration submitted", user.getId(), user.getFullName());
        notificationClient.notify(user.getId(), "BUYER_REGISTERED", "REGISTRATION", "/buyer/dashboard",
                Map.of("name", user.getFullName(), "reference", user.getReferenceNo(), "organisation", request.organisationName()));
        if (user.getStatus() != Enums.UserStatus.ACTIVE) {
            notificationClient.notify(NotificationRequest.toAdmins(Permission.BUYER_MANAGE.name(), "NEW_BUYER_APPLICATION", "VERIFICATION",
                    "/admin/users/" + user.getId(), Map.of("organisation", request.organisationName(), "reference", user.getReferenceNo())));
        }
        notificationClient.audit(new AuditEvent("auth", user.getId(), "BUYER", user.getFullName(),
                "BUYER_REGISTERED", "USER", String.valueOf(user.getId()), user.getReferenceNo(), null));
        String message = user.getStatus() == Enums.UserStatus.ACTIVE
                ? "Registration successful. You can now sign in."
                : "Registration submitted. Your account " + user.getReferenceNo() + " will be activated after verification.";
        return new AuthDtos.RegistrationResponse(user.getId(), user.getReferenceNo(), user.getStatus(), message);
    }

    @Transactional
    public AdminDtos.CreatedUserResponse adminCreateSeller(SellerDtos.SellerRegistrationRequest request) {
        AuthUser admin = CurrentUser.get();
        String temporary = PasswordPolicy.generate();
        User user = createSeller(request, temporary, admin.userId());
        user.setStatus(Enums.UserStatus.ACTIVE);
        user.setStatusChangedAt(LocalDateTime.now());
        user.setPermissions(EnumSet.copyOf(Permission.defaultsFor(Role.SELLER)));
        user.setMustChangePassword(true);
        user.setEmailVerified(true);
        SellerProfile profile = sellerProfileRepository.findByUserId(user.getId()).orElseThrow();
        profile.setVerifiedAt(LocalDateTime.now());
        profile.setVerifiedBy(admin.userId());
        profile.setReverificationDue(LocalDate.now().plusMonths(properties.getReverificationMonths()));
        record(user.getId(), Enums.VerificationAction.CREATED_BY_ADMIN, null, Enums.UserStatus.ACTIVE, "Seller onboarded by administrator", admin.userId(), admin.name());
        sellerSyncService.sync(user);
        notifyCreated(user, temporary);
        notificationClient.audit("SELLER_CREATED_BY_ADMIN", "USER", user.getId(), user.getReferenceNo());
        return new AdminDtos.CreatedUserResponse(user.getId(), user.getReferenceNo(), temporary);
    }

    @Transactional
    public AdminDtos.CreatedUserResponse adminCreateBuyer(BuyerDtos.BuyerRegistrationRequest request) {
        AuthUser admin = CurrentUser.get();
        String temporary = PasswordPolicy.generate();
        User user = createBuyer(request, temporary, admin.userId(), true);
        user.setMustChangePassword(true);
        user.setEmailVerified(true);
        record(user.getId(), Enums.VerificationAction.CREATED_BY_ADMIN, null, Enums.UserStatus.ACTIVE, "Purchaser onboarded by administrator", admin.userId(), admin.name());
        notifyCreated(user, temporary);
        notificationClient.audit("BUYER_CREATED_BY_ADMIN", "USER", user.getId(), user.getReferenceNo());
        return new AdminDtos.CreatedUserResponse(user.getId(), user.getReferenceNo(), temporary);
    }

    @Transactional
    public AdminDtos.CreatedUserResponse adminCreateAdministrator(AdminDtos.AdminCreateRequest request) {
        AuthUser admin = CurrentUser.get();
        ensureUnique(request.email(), request.mobile());
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        request.permissions().stream().filter(p -> p.getRole() == Role.ADMIN).forEach(permissions::add);
        if (permissions.isEmpty()) {
            throw BusinessException.badRequest("Select at least one administrator privilege");
        }
        String temporary = PasswordPolicy.generate();
        User user = baseUser(request.fullName(), request.email(), request.mobile(), temporary, Role.ADMIN);
        user.setDesignation(request.designation());
        user.setStatus(Enums.UserStatus.ACTIVE);
        user.setPermissions(permissions);
        user.setMustChangePassword(true);
        user.setEmailVerified(true);
        user.setCreatedByAdmin(admin.userId());
        userRepository.save(user);
        user.setReferenceNo(References.of("ADM", user.getId()));
        record(user.getId(), Enums.VerificationAction.CREATED_BY_ADMIN, null, Enums.UserStatus.ACTIVE, "Administrator account created", admin.userId(), admin.name());
        notifyCreated(user, temporary);
        notificationClient.audit("ADMIN_CREATED", "USER", user.getId(), String.join(",", permissions.stream().map(Enum::name).toList()));
        return new AdminDtos.CreatedUserResponse(user.getId(), user.getReferenceNo(), temporary);
    }

    User createSeller(SellerDtos.SellerRegistrationRequest request, String password, Long adminId) {
        ensureUnique(request.email(), request.mobile());
        String panHash = crypto.blindIndex(request.pan());
        if (sellerProfileRepository.existsByPanHash(panHash)) {
            throw BusinessException.conflict("A service provider is already registered with this PAN");
        }
        User user = baseUser(request.fullName(), request.email(), request.mobile(), password, Role.SELLER);
        user.setStatus(Enums.UserStatus.PENDING_VERIFICATION);
        user.setCreatedByAdmin(adminId);
        user.setEmailVerified(adminId == null && properties.isRegistrationOtpRequired());
        userRepository.save(user);
        user.setReferenceNo(References.of("SLR", user.getId()));

        SellerProfile profile = new SellerProfile();
        profile.setUser(user);
        profile.setOrganisationName(request.organisationName().trim());
        profile.setConstitution(request.constitution());
        profile.setPan(upper(request.pan()));
        profile.setPanHash(panHash);
        profile.setGstin(upper(request.gstin()));
        profile.setUdyamNumber(upper(request.udyamNumber()));
        profile.setAddressLine(request.addressLine());
        profile.setCity(request.city());
        profile.setState(request.state());
        profile.setDistrict(request.district());
        profile.setPincode(request.pincode());
        profile.setProviderType(request.providerType());
        profile.setCategoryIds(UserMapper.categoryIdsCsv(request.categoryIds()));
        profile.setCategoryNames(Csv.join(request.categoryNames()));
        profile.setExpertise(Csv.join(request.expertise()));
        profile.setIndustries(Csv.join(request.industries()));
        profile.setYearsExperience(request.yearsExperience());
        profile.setTeamSize(request.teamSize());
        profile.setGeographicCoverage(Csv.join(request.geographicCoverage()));
        profile.setDeliveryModes(Csv.join(request.deliveryModes()));
        profile.setWebsite(request.website());
        profile.setIntroduction(request.introduction());
        profile.setRatingCount(0);
        sellerProfileRepository.save(profile);
        return user;
    }

    User createBuyer(BuyerDtos.BuyerRegistrationRequest request, String password, Long adminId, boolean active) {
        ensureUnique(request.email(), request.mobile());
        User user = baseUser(request.fullName(), request.email(), request.mobile(), password, Role.BUYER);
        user.setStatus(active ? Enums.UserStatus.ACTIVE : Enums.UserStatus.PENDING_VERIFICATION);
        user.setStatusChangedAt(LocalDateTime.now());
        user.setDesignation(request.designation());
        user.setCreatedByAdmin(adminId);
        user.setEmailVerified(adminId == null && properties.isRegistrationOtpRequired());
        if (active) {
            user.setPermissions(EnumSet.copyOf(Permission.defaultsFor(Role.BUYER)));
        }
        userRepository.save(user);
        user.setReferenceNo(References.of("PUR", user.getId()));

        PurchaserProfile profile = new PurchaserProfile();
        profile.setUser(user);
        profile.setOrganisationName(request.organisationName().trim());
        profile.setConstitution(request.constitution());
        profile.setPurchaserType(request.purchaserType());
        profile.setContactPerson(request.fullName());
        profile.setDesignation(request.designation());
        profile.setPan(upper(request.pan()));
        profile.setGstin(upper(request.gstin()));
        profile.setUdyamNumber(upper(request.udyamNumber()));
        profile.setIndustrySector(request.industrySector());
        profile.setBusinessActivity(request.businessActivity());
        profile.setAddressLine(request.addressLine());
        profile.setCity(request.city());
        profile.setState(request.state());
        profile.setDistrict(request.district());
        profile.setPincode(request.pincode());
        profile.setWebsite(request.website());
        purchaserProfileRepository.save(profile);
        return user;
    }

    private String selfRegistrationChecks(String email, String rawPassword, String emailOtp, Boolean declaration) {
        if (!Boolean.TRUE.equals(declaration)) {
            throw BusinessException.badRequest("Please accept the declaration to continue");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw BusinessException.badRequest("Password is required");
        }
        String password = transportCrypto.reveal(rawPassword);
        PasswordPolicy.validate(password);
        if (properties.isRegistrationOtpRequired()) {
            if (emailOtp == null || emailOtp.isBlank()) {
                throw BusinessException.badRequest("Email OTP verification is required");
            }
            otpService.verify(email.trim().toLowerCase(Locale.ROOT), Enums.OtpPurpose.REGISTRATION, emailOtp);
        }
        return password;
    }

    private void ensureUnique(String email, String mobile) {
        if (userRepository.existsByEmailHash(crypto.blindIndex(email))) {
            throw BusinessException.conflict("An account already exists with this email");
        }
        if (mobile != null && !mobile.isBlank() && userRepository.existsByMobileHash(crypto.blindIndex(mobile))) {
            throw BusinessException.conflict("An account already exists with this mobile number");
        }
    }

    private User baseUser(String fullName, String email, String mobile, String password, Role role) {
        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase(Locale.ROOT));
        user.setEmailHash(crypto.blindIndex(email));
        if (mobile != null && !mobile.isBlank()) {
            user.setMobile(mobile.trim());
            user.setMobileHash(crypto.blindIndex(mobile));
        }
        user.setRole(role);
        authService.applyPassword(user, password);
        user.setStatusChangedAt(LocalDateTime.now());
        return user;
    }

    private void notifyCreated(User user, String temporary) {
        notificationClient.email(user.getEmail(), "Your BDS Marketplace account",
                "Dear " + user.getFullName() + ",\n\nAn account has been created for you on BDS Marketplace.\nReference: "
                        + user.getReferenceNo() + "\nTemporary password: " + temporary
                        + "\n\nPlease sign in and change your password immediately.");
        notificationClient.notify(user.getId(), "ACCOUNT_CREATED_BY_ADMIN", "REGISTRATION", null,
                Map.of("name", user.getFullName(), "reference", user.getReferenceNo()));
    }

    void record(Long userId, Enums.VerificationAction action, Enums.UserStatus from, Enums.UserStatus to,
                String remarks, Long actorId, String actorName) {
        VerificationRecord record = new VerificationRecord();
        record.setUserId(userId);
        record.setAction(action);
        record.setFromStatus(from);
        record.setToStatus(to);
        record.setRemarks(remarks);
        record.setActorId(actorId);
        record.setActorName(actorName);
        verificationRepository.save(record);
    }

    private static String upper(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
