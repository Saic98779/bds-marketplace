package com.sidbi.bds.auth.service;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.domain.PurchaserProfile;
import com.sidbi.bds.auth.domain.SellerProfile;
import com.sidbi.bds.auth.domain.User;
import com.sidbi.bds.auth.dto.BuyerDtos;
import com.sidbi.bds.auth.dto.SellerDtos;
import com.sidbi.bds.auth.repository.PurchaserProfileRepository;
import com.sidbi.bds.auth.repository.SellerProfileRepository;
import com.sidbi.bds.auth.repository.VerificationRecordRepository;
import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.client.dto.NotificationRequest;
import com.sidbi.bds.common.crypto.FieldCrypto;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.util.Csv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final SellerProfileRepository sellerProfileRepository;
    private final PurchaserProfileRepository purchaserProfileRepository;
    private final VerificationRecordRepository verificationRepository;
    private final SellerDocumentService documentService;
    private final RegistrationService registrationService;
    private final SellerSyncService sellerSyncService;
    private final NotificationClient notificationClient;
    private final FieldCrypto crypto;
    private final UserMapper mapper;

    @Transactional(readOnly = true)
    public SellerDtos.SellerProfileResponse sellerProfile(Long userId) {
        return mapper.seller(seller(userId), true);
    }

    @Transactional
    public SellerDtos.SellerProfileResponse updateSeller(Long userId, SellerDtos.SellerProfileUpdateRequest request) {
        SellerProfile profile = seller(userId);
        User user = profile.getUser();
        String newPan = request.pan().trim().toUpperCase(Locale.ROOT);
        String newGstin = blankToNull(request.gstin());
        boolean criticalChange = !Objects.equals(profile.getOrganisationName(), request.organisationName().trim())
                || profile.getConstitution() != request.constitution()
                || !Objects.equals(profile.getPan(), newPan)
                || !Objects.equals(profile.getGstin(), newGstin);
        if (!Objects.equals(profile.getPan(), newPan) && sellerProfileRepository.existsByPanHash(crypto.blindIndex(newPan))) {
            throw BusinessException.conflict("A service provider is already registered with this PAN");
        }
        user.setFullName(request.fullName().trim());
        profile.setOrganisationName(request.organisationName().trim());
        profile.setConstitution(request.constitution());
        profile.setPan(newPan);
        profile.setPanHash(crypto.blindIndex(newPan));
        profile.setGstin(newGstin);
        profile.setUdyamNumber(blankToNull(request.udyamNumber()));
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
        profile.setYearEstablished(request.yearEstablished());
        profile.setCertifications(request.certifications());
        profile.setTeamProfile(request.teamProfile());
        profile.setPreviousAssignments(request.previousAssignments());
        profile.setClientReferences(request.clientReferences());

        if (criticalChange && user.getStatus() == Enums.UserStatus.ACTIVE) {
            Enums.UserStatus previous = user.getStatus();
            user.setStatus(Enums.UserStatus.PENDING_VERIFICATION);
            user.setStatusRemarks("Identity or legal details changed. Re-verification in progress.");
            user.setStatusChangedAt(LocalDateTime.now());
            registrationService.record(userId, Enums.VerificationAction.REVERIFICATION_TRIGGERED, previous,
                    Enums.UserStatus.PENDING_VERIFICATION, "Critical profile details updated by seller", userId, user.getFullName());
            sellerSyncService.sync(user);
            notificationClient.notify(NotificationRequest.toAdmins(Permission.SELLER_VERIFY.name(), "NEW_SELLER_APPLICATION",
                    "VERIFICATION", "/admin/approvals/" + userId,
                    Map.of("organisation", profile.getOrganisationName(), "reference", user.getReferenceNo())));
        } else if (user.getStatus() == Enums.UserStatus.ACTIVE) {
            sellerSyncService.sync(user);
        }
        notificationClient.audit("SELLER_PROFILE_UPDATED", "USER", userId, criticalChange ? "critical" : "descriptive");
        return mapper.seller(profile, true);
    }

    @Transactional
    public SellerDtos.VerificationStatusResponse resubmit(Long userId, String remarks) {
        SellerProfile profile = seller(userId);
        User user = profile.getUser();
        if (user.getStatus() != Enums.UserStatus.INFO_REQUESTED) {
            throw BusinessException.conflict("Application can be resubmitted only when additional information is requested");
        }
        user.setStatus(Enums.UserStatus.PENDING_VERIFICATION);
        user.setStatusChangedAt(LocalDateTime.now());
        registrationService.record(userId, Enums.VerificationAction.SUBMITTED, Enums.UserStatus.INFO_REQUESTED,
                Enums.UserStatus.PENDING_VERIFICATION, remarks == null ? "Additional information submitted" : remarks, userId, user.getFullName());
        notificationClient.notify(NotificationRequest.toAdmins(Permission.SELLER_VERIFY.name(), "NEW_SELLER_APPLICATION",
                "VERIFICATION", "/admin/approvals/" + userId,
                Map.of("organisation", profile.getOrganisationName(), "reference", user.getReferenceNo())));
        return verification(userId);
    }

    @Transactional(readOnly = true)
    public SellerDtos.VerificationStatusResponse verification(Long userId) {
        SellerProfile profile = seller(userId);
        return new SellerDtos.VerificationStatusResponse(
                profile.getUser().getStatus(),
                profile.getUser().getStatusRemarks(),
                mapper.checklist(profile, documentService.list(userId)),
                verificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(mapper::history).toList());
    }

    @Transactional(readOnly = true)
    public BuyerDtos.BuyerProfileResponse buyerProfile(Long userId) {
        return mapper.buyer(buyer(userId), true);
    }

    @Transactional
    public BuyerDtos.BuyerProfileResponse updateBuyer(Long userId, BuyerDtos.BuyerProfileUpdateRequest request) {
        PurchaserProfile profile = buyer(userId);
        User user = profile.getUser();
        user.setFullName(request.fullName().trim());
        user.setDesignation(request.designation());
        profile.setOrganisationName(request.organisationName().trim());
        profile.setConstitution(request.constitution());
        profile.setPurchaserType(request.purchaserType());
        profile.setContactPerson(request.fullName().trim());
        profile.setDesignation(request.designation());
        profile.setPan(blankToNull(request.pan()));
        profile.setGstin(blankToNull(request.gstin()));
        profile.setUdyamNumber(blankToNull(request.udyamNumber()));
        profile.setIndustrySector(request.industrySector());
        profile.setBusinessActivity(request.businessActivity());
        profile.setAddressLine(request.addressLine());
        profile.setCity(request.city());
        profile.setState(request.state());
        profile.setDistrict(request.district());
        profile.setPincode(request.pincode());
        profile.setWebsite(request.website());
        notificationClient.audit("BUYER_PROFILE_UPDATED", "USER", userId, null);
        return mapper.buyer(profile, true);
    }

    SellerProfile seller(Long userId) {
        return sellerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> BusinessException.notFound("Seller profile not found"));
    }

    PurchaserProfile buyer(Long userId) {
        return purchaserProfileRepository.findByUserId(userId)
                .orElseThrow(() -> BusinessException.notFound("Purchaser profile not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
