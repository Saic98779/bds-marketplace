package com.sidbi.bds.auth.service;

import com.sidbi.bds.auth.config.AuthProperties;
import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.domain.PurchaserProfile;
import com.sidbi.bds.auth.domain.SellerDocument;
import com.sidbi.bds.auth.domain.SellerProfile;
import com.sidbi.bds.auth.domain.User;
import com.sidbi.bds.auth.dto.AdminDtos;
import com.sidbi.bds.auth.dto.BuyerDtos;
import com.sidbi.bds.auth.dto.SellerDtos;
import com.sidbi.bds.auth.repository.PurchaserProfileRepository;
import com.sidbi.bds.auth.repository.RefreshTokenRepository;
import com.sidbi.bds.auth.repository.SellerProfileRepository;
import com.sidbi.bds.auth.repository.UserRepository;
import com.sidbi.bds.auth.repository.VerificationRecordRepository;
import com.sidbi.bds.common.api.PageRequestFactory;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.crypto.FieldCrypto;
import com.sidbi.bds.common.crypto.Masking;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.security.AuthUser;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.security.Role;
import com.sidbi.bds.common.util.Search;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final PurchaserProfileRepository purchaserProfileRepository;
    private final VerificationRecordRepository verificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SellerDocumentService documentService;
    private final RegistrationService registrationService;
    private final AuthService authService;
    private final SellerSyncService sellerSyncService;
    private final NotificationClient notificationClient;
    private final AuthProperties properties;
    private final FieldCrypto crypto;
    private final UserMapper mapper;

    public record UserFilter(Role role, Enums.UserStatus status, String q, String state) {
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminDtos.AdminUserRow> list(UserFilter filter, int page, int size) {
        if (filter.role() == Role.SELLER) {
            CurrentUser.require(Permission.SELLER_VERIFY);
        } else if (filter.role() == Role.BUYER) {
            CurrentUser.require(Permission.BUYER_MANAGE);
        } else if (filter.role() == Role.ADMIN) {
            CurrentUser.require(Permission.USER_ADMIN);
        }
        Page<User> users = userRepository.findAll(spec(filter), PageRequestFactory.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Long> ids = users.getContent().stream().map(User::getId).toList();
        Map<Long, SellerProfile> sellers = sellerProfileRepository.findByUserIdIn(ids).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity()));
        Map<Long, PurchaserProfile> buyers = purchaserProfileRepository.findByUserIdIn(ids).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity()));
        return PageResponse.of(users, u -> row(u, sellers.get(u.getId()), buyers.get(u.getId())));
    }

    @Transactional(readOnly = true)
    public AdminDtos.AdminUserDetail detail(Long userId) {
        notificationClient.audit("ADMIN_VIEWED_USER", "USER", userId, null);
        return buildDetail(userId);
    }

    private AdminDtos.AdminUserDetail buildDetail(Long userId) {
        User user = user(userId);
        requireFor(user.getRole());
        SellerDtos.SellerProfileResponse seller = null;
        BuyerDtos.BuyerProfileResponse buyer = null;
        List<SellerDtos.DocumentResponse> documents = List.of();
        List<SellerDtos.RequiredDocument> checklist = List.of();
        if (user.getRole() == Role.SELLER) {
            SellerProfile profile = sellerProfileRepository.findByUserId(userId).orElseThrow();
            List<SellerDocument> docs = documentService.list(userId);
            seller = mapper.seller(profile, true);
            documents = docs.stream().map(mapper::document).toList();
            checklist = mapper.checklist(profile, docs);
        } else if (user.getRole() == Role.BUYER) {
            buyer = purchaserProfileRepository.findByUserId(userId).map(p -> mapper.buyer(p, true)).orElse(null);
        }
        return new AdminDtos.AdminUserDetail(user.getId(), user.getReferenceNo(), user.getFullName(), user.getEmail(), user.getMobile(),
                user.getRole(), user.getStatus(), user.getStatusRemarks(), user.getDesignation(), mapper.permissionCodes(user),
                user.isEmailVerified(), user.isLocked(), user.getCreatedAt(), user.getLastLoginAt(), seller, buyer, documents, checklist,
                verificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(mapper::history).toList());
    }

    @Transactional
    public AdminDtos.AdminUserDetail approve(Long userId, AdminDtos.AdminActionRequest request) {
        User user = user(userId);
        requireFor(user.getRole());
        if (user.getStatus() != Enums.UserStatus.PENDING_VERIFICATION && user.getStatus() != Enums.UserStatus.INFO_REQUESTED) {
            throw BusinessException.conflict("Only pending applications can be approved");
        }
        if (user.getRole() == Role.SELLER && user.getCreatedByAdmin() == null) {
            SellerProfile profile = sellerProfileRepository.findByUserId(userId).orElseThrow();
            List<SellerDocument> docs = documentService.list(userId);
            List<String> missing = mapper.checklist(profile, docs).stream()
                    .filter(c -> c.mandatory() && !c.uploaded())
                    .map(c -> c.documentType().name())
                    .toList();
            if (!missing.isEmpty()) {
                throw BusinessException.unprocessable("Mandatory documents missing: " + String.join(", ", missing)
                        + ". Request additional information from the applicant.");
            }
        }
        AuthUser admin = CurrentUser.get();
        Enums.UserStatus previous = user.getStatus();
        user.setStatus(Enums.UserStatus.ACTIVE);
        user.setStatusRemarks(request.remarks());
        user.setStatusChangedAt(LocalDateTime.now());
        user.setPermissions(resolvePermissions(user.getRole(), request.permissions()));
        if (user.getRole() == Role.SELLER) {
            SellerProfile profile = sellerProfileRepository.findByUserId(userId).orElseThrow();
            profile.setVerifiedAt(LocalDateTime.now());
            profile.setVerifiedBy(admin.userId());
            profile.setReverificationDue(LocalDate.now().plusMonths(properties.getReverificationMonths()));
        }
        registrationService.record(userId, Enums.VerificationAction.APPROVE, previous, Enums.UserStatus.ACTIVE, request.remarks(), admin.userId(), admin.name());
        sellerSyncService.sync(user);
        notificationClient.notify(userId, "ACCOUNT_APPROVED", "VERIFICATION", dashboardFor(user.getRole()),
                Map.of("name", user.getFullName(), "reference", user.getReferenceNo(), "remarks", nz(request.remarks())));
        notificationClient.audit("USER_APPROVED", "USER", userId, request.remarks());
        return buildDetail(userId);
    }

    @Transactional
    public AdminDtos.AdminUserDetail reject(Long userId, String remarks) {
        User user = user(userId);
        requireFor(user.getRole());
        if (user.getStatus() != Enums.UserStatus.PENDING_VERIFICATION && user.getStatus() != Enums.UserStatus.INFO_REQUESTED) {
            throw BusinessException.conflict("Only pending applications can be rejected");
        }
        transition(user, Enums.UserStatus.REJECTED, Enums.VerificationAction.REJECT, remarks);
        user.setPermissions(EnumSet.noneOf(Permission.class));
        refreshTokenRepository.revokeAllForUser(userId);
        notificationClient.notify(userId, "ACCOUNT_REJECTED", "VERIFICATION", null,
                Map.of("name", user.getFullName(), "reference", user.getReferenceNo(), "remarks", nz(remarks)));
        notificationClient.audit("USER_REJECTED", "USER", userId, remarks);
        return buildDetail(userId);
    }

    @Transactional
    public AdminDtos.AdminUserDetail requestInfo(Long userId, String remarks) {
        User user = user(userId);
        requireFor(user.getRole());
        if (user.getStatus() != Enums.UserStatus.PENDING_VERIFICATION) {
            throw BusinessException.conflict("Additional information can be requested only for pending applications");
        }
        transition(user, Enums.UserStatus.INFO_REQUESTED, Enums.VerificationAction.REQUEST_INFO, remarks);
        notificationClient.notify(userId, "ACCOUNT_INFO_REQUESTED", "VERIFICATION", user.getRole() == Role.SELLER ? "/seller/profile" : "/buyer/profile",
                Map.of("name", user.getFullName(), "reference", user.getReferenceNo(), "remarks", nz(remarks)));
        notificationClient.audit("USER_INFO_REQUESTED", "USER", userId, remarks);
        return buildDetail(userId);
    }

    @Transactional
    public AdminDtos.AdminUserDetail suspend(Long userId, String remarks) {
        User user = user(userId);
        requireFor(user.getRole());
        if (user.getId().equals(CurrentUser.id())) {
            throw BusinessException.conflict("You cannot suspend your own account");
        }
        if (user.getStatus() != Enums.UserStatus.ACTIVE) {
            throw BusinessException.conflict("Only active accounts can be suspended");
        }
        transition(user, Enums.UserStatus.SUSPENDED, Enums.VerificationAction.SUSPEND, remarks);
        refreshTokenRepository.revokeAllForUser(userId);
        sellerSyncService.sync(user);
        notificationClient.notify(userId, "ACCOUNT_SUSPENDED", "VERIFICATION", null,
                Map.of("name", user.getFullName(), "reference", user.getReferenceNo(), "remarks", nz(remarks)));
        notificationClient.audit("USER_SUSPENDED", "USER", userId, remarks);
        return buildDetail(userId);
    }

    @Transactional
    public AdminDtos.AdminUserDetail activate(Long userId, String remarks) {
        User user = user(userId);
        requireFor(user.getRole());
        if (user.getStatus() != Enums.UserStatus.SUSPENDED && user.getStatus() != Enums.UserStatus.DEACTIVATED
                && user.getStatus() != Enums.UserStatus.REJECTED) {
            throw BusinessException.conflict("Only suspended, deactivated or rejected accounts can be activated");
        }
        transition(user, Enums.UserStatus.ACTIVE, Enums.VerificationAction.REACTIVATE, remarks);
        if (user.getPermissions().isEmpty()) {
            user.setPermissions(EnumSet.copyOf(Permission.defaultsFor(user.getRole())));
        }
        if (user.getRole() == Role.SELLER) {
            sellerProfileRepository.findByUserId(userId).ifPresent(p -> {
                if (p.getVerifiedAt() == null) {
                    p.setVerifiedAt(LocalDateTime.now());
                    p.setVerifiedBy(CurrentUser.id());
                }
                p.setReverificationDue(LocalDate.now().plusMonths(properties.getReverificationMonths()));
            });
        }
        sellerSyncService.sync(user);
        notificationClient.notify(userId, "ACCOUNT_REACTIVATED", "VERIFICATION", dashboardFor(user.getRole()),
                Map.of("name", user.getFullName(), "reference", user.getReferenceNo(), "remarks", nz(remarks)));
        notificationClient.audit("USER_ACTIVATED", "USER", userId, remarks);
        return buildDetail(userId);
    }

    @Transactional
    public AdminDtos.AdminUserDetail updatePermissions(Long userId, Set<Permission> permissions) {
        CurrentUser.require(Permission.USER_ADMIN);
        User user = user(userId);
        if (user.getId().equals(CurrentUser.id()) && !permissions.contains(Permission.USER_ADMIN)) {
            throw BusinessException.conflict("You cannot remove your own user administration privilege");
        }
        Set<Permission> filtered = EnumSet.noneOf(Permission.class);
        permissions.stream().filter(p -> p.getRole() == user.getRole()).forEach(filtered::add);
        user.setPermissions(filtered);
        AuthUser admin = CurrentUser.get();
        registrationService.record(userId, Enums.VerificationAction.PRIVILEGES_UPDATED, user.getStatus(), user.getStatus(),
                filtered.stream().map(Enum::name).collect(Collectors.joining(", ")), admin.userId(), admin.name());
        sellerSyncService.sync(user);
        notificationClient.notify(userId, "PRIVILEGES_UPDATED", "VERIFICATION", null,
                Map.of("name", user.getFullName(), "privileges", filtered.isEmpty() ? "None" :
                        filtered.stream().map(Permission::getDescription).collect(Collectors.joining(", "))));
        notificationClient.audit("PRIVILEGES_UPDATED", "USER", userId, filtered.toString());
        return buildDetail(userId);
    }

    @Transactional
    public AdminDtos.CreatedUserResponse resetPassword(Long userId) {
        User user = user(userId);
        requireFor(user.getRole());
        String temporary = PasswordPolicy.generate();
        authService.applyPassword(user, temporary);
        user.setMustChangePassword(true);
        user.setLockedUntil(null);
        user.setFailedAttempts(0);
        refreshTokenRepository.revokeAllForUser(userId);
        notificationClient.email(user.getEmail(), "BDS Marketplace password reset",
                "Your password has been reset by an administrator. Temporary password: " + temporary + ". Please change it after signing in.");
        notificationClient.audit("PASSWORD_RESET_BY_ADMIN", "USER", userId, null);
        return new AdminDtos.CreatedUserResponse(userId, user.getReferenceNo(), temporary);
    }

    @Transactional
    public void unlock(Long userId) {
        User user = user(userId);
        requireFor(user.getRole());
        user.setLockedUntil(null);
        user.setFailedAttempts(0);
        notificationClient.audit("USER_UNLOCKED", "USER", userId, null);
    }

    @Transactional
    public void setFeatured(Long userId, boolean featured) {
        CurrentUser.require(Permission.SELLER_VERIFY);
        SellerProfile profile = sellerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> BusinessException.notFound("Seller not found"));
        profile.setFeatured(featured);
        notificationClient.audit(featured ? "SELLER_FEATURED" : "SELLER_UNFEATURED", "USER", userId, null);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadDocument(Long userId, Long documentId) {
        CurrentUser.require(Permission.SELLER_VERIFY);
        notificationClient.audit("SELLER_DOCUMENT_DOWNLOADED", "SELLER_DOCUMENT", documentId, "user " + userId);
        return documentService.download(userId, documentId);
    }

    @Transactional
    public SellerDtos.DocumentResponse verifyDocument(Long userId, Long documentId, AdminDtos.DocumentVerifyRequest request) {
        CurrentUser.require(Permission.SELLER_VERIFY);
        SellerDocument document = documentService.verify(userId, documentId, request.verified(), request.remarks(), CurrentUser.id());
        notificationClient.audit(request.verified() ? "DOCUMENT_VERIFIED" : "DOCUMENT_UNVERIFIED", "SELLER_DOCUMENT", documentId, request.remarks());
        return mapper.document(document);
    }

    public List<AdminDtos.PermissionInfo> permissionCatalog() {
        List<AdminDtos.PermissionInfo> list = new ArrayList<>();
        for (Permission p : Permission.values()) {
            list.add(new AdminDtos.PermissionInfo(p.name(), p.getRole(), p.getDescription()));
        }
        return list;
    }

    AdminDtos.AdminUserRow row(User u, SellerProfile seller, PurchaserProfile buyer) {
        String organisation = seller != null ? seller.getOrganisationName() : buyer != null ? buyer.getOrganisationName() : null;
        String state = seller != null ? seller.getState() : buyer != null ? buyer.getState() : null;
        String district = seller != null ? seller.getDistrict() : buyer != null ? buyer.getDistrict() : null;
        String type = seller != null ? seller.getProviderType().name() : buyer != null ? buyer.getPurchaserType().name() : u.getDesignation();
        return new AdminDtos.AdminUserRow(u.getId(), u.getReferenceNo(), u.getFullName(), organisation, Masking.email(u.getEmail()),
                Masking.mobile(u.getMobile()), u.getRole(), u.getStatus(), state, district, type, mapper.permissionCodes(u),
                u.getCreatedAt(), u.getLastLoginAt());
    }

    private void transition(User user, Enums.UserStatus to, Enums.VerificationAction action, String remarks) {
        AuthUser admin = CurrentUser.get();
        Enums.UserStatus previous = user.getStatus();
        user.setStatus(to);
        user.setStatusRemarks(remarks);
        user.setStatusChangedAt(LocalDateTime.now());
        registrationService.record(user.getId(), action, previous, to, remarks, admin.userId(), admin.name());
    }

    private Set<Permission> resolvePermissions(Role role, Set<Permission> requested) {
        if (requested == null || requested.isEmpty()) {
            return EnumSet.copyOf(Permission.defaultsFor(role));
        }
        Set<Permission> result = EnumSet.noneOf(Permission.class);
        requested.stream().filter(p -> p.getRole() == role).forEach(result::add);
        return result;
    }

    private void requireFor(Role role) {
        switch (role) {
            case SELLER -> CurrentUser.require(Permission.SELLER_VERIFY);
            case BUYER -> CurrentUser.require(Permission.BUYER_MANAGE);
            case ADMIN -> CurrentUser.require(Permission.USER_ADMIN);
        }
    }

    private User user(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> BusinessException.notFound("User not found"));
    }

    private Specification<User> spec(UserFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.role() != null) {
                predicates.add(cb.equal(root.get("role"), filter.role()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.state() != null && !filter.state().isBlank()) {
                Subquery<Long> sellerState = query.subquery(Long.class);
                Root<SellerProfile> sp = sellerState.from(SellerProfile.class);
                sellerState.select(sp.get("user").get("id")).where(cb.equal(sp.get("state"), filter.state()));
                Subquery<Long> buyerState = query.subquery(Long.class);
                Root<PurchaserProfile> bp = buyerState.from(PurchaserProfile.class);
                buyerState.select(bp.get("user").get("id")).where(cb.equal(bp.get("state"), filter.state()));
                predicates.add(cb.or(root.get("id").in(sellerState), root.get("id").in(buyerState)));
            }
            if (filter.q() != null && !filter.q().isBlank()) {
                String q = filter.q().trim();
                String like = Search.contains(q);
                Subquery<Long> sellerOrg = query.subquery(Long.class);
                Root<SellerProfile> sp = sellerOrg.from(SellerProfile.class);
                sellerOrg.select(sp.get("user").get("id")).where(cb.like(cb.lower(sp.get("organisationName")), like, Search.ESCAPE));
                Subquery<Long> buyerOrg = query.subquery(Long.class);
                Root<PurchaserProfile> bp = buyerOrg.from(PurchaserProfile.class);
                buyerOrg.select(bp.get("user").get("id")).where(cb.like(cb.lower(bp.get("organisationName")), like, Search.ESCAPE));
                List<Predicate> any = new ArrayList<>();
                any.add(cb.like(cb.lower(root.get("fullName")), like, Search.ESCAPE));
                any.add(cb.like(cb.lower(root.get("referenceNo")), like, Search.ESCAPE));
                any.add(root.get("id").in(sellerOrg));
                any.add(root.get("id").in(buyerOrg));
                any.add(cb.equal(root.get("emailHash"), crypto.blindIndex(q)));
                any.add(cb.equal(root.get("mobileHash"), crypto.blindIndex(q)));
                predicates.add(cb.or(any.toArray(Predicate[]::new)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String dashboardFor(Role role) {
        return switch (role) {
            case SELLER -> "/seller/dashboard";
            case BUYER -> "/buyer/dashboard";
            case ADMIN -> "/admin/dashboard";
        };
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
