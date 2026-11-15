package com.sidbi.bds.auth.service;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.domain.PurchaserProfile;
import com.sidbi.bds.auth.domain.SellerDocument;
import com.sidbi.bds.auth.domain.SellerProfile;
import com.sidbi.bds.auth.domain.User;
import com.sidbi.bds.auth.domain.VerificationRecord;
import com.sidbi.bds.auth.dto.AuthDtos;
import com.sidbi.bds.auth.dto.BuyerDtos;
import com.sidbi.bds.auth.dto.ProviderDtos;
import com.sidbi.bds.auth.dto.SellerDtos;
import com.sidbi.bds.common.client.dto.UserSummary;
import com.sidbi.bds.common.crypto.Masking;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.util.Csv;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public Set<String> permissionCodes(User user) {
        return user.getPermissions().stream().map(Permission::name).collect(Collectors.toCollection(java.util.TreeSet::new));
    }

    public AuthDtos.MeResponse me(User user, String organisationName) {
        return new AuthDtos.MeResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getMobile(),
                user.getRole(),
                user.getStatus(),
                user.getReferenceNo(),
                organisationName,
                user.getDesignation(),
                permissionCodes(user),
                user.isMustChangePassword(),
                user.isEmailAlerts(),
                user.getStatusRemarks(),
                user.getLastLoginAt()
        );
    }

    public SellerDtos.SellerProfileResponse seller(SellerProfile p, boolean revealSensitive) {
        User u = p.getUser();
        return new SellerDtos.SellerProfileResponse(
                u.getId(),
                u.getReferenceNo(),
                u.getFullName(),
                revealSensitive ? u.getEmail() : Masking.email(u.getEmail()),
                revealSensitive ? u.getMobile() : Masking.mobile(u.getMobile()),
                u.getStatus(),
                u.getStatusRemarks(),
                p.getOrganisationName(),
                p.getConstitution(),
                revealSensitive ? p.getPan() : Masking.pan(p.getPan()),
                revealSensitive ? p.getGstin() : Masking.gstin(p.getGstin()),
                revealSensitive ? p.getUdyamNumber() : Masking.generic(p.getUdyamNumber()),
                revealSensitive ? p.getAddressLine() : null,
                p.getCity(),
                p.getState(),
                p.getDistrict(),
                p.getPincode(),
                p.getProviderType(),
                ids(p.getCategoryIds()),
                Csv.split(p.getCategoryNames()),
                Csv.split(p.getExpertise()),
                Csv.split(p.getIndustries()),
                p.getYearsExperience(),
                p.getTeamSize(),
                Csv.split(p.getGeographicCoverage()),
                Csv.split(p.getDeliveryModes()),
                p.getWebsite(),
                p.getIntroduction(),
                p.getYearEstablished(),
                p.getCertifications(),
                p.getTeamProfile(),
                p.getPreviousAssignments(),
                p.getClientReferences(),
                p.getAverageRating(),
                p.getRatingCount(),
                p.getVerifiedAt(),
                p.getReverificationDue(),
                completion(p),
                p.getCreatedAt()
        );
    }

    public BuyerDtos.BuyerProfileResponse buyer(PurchaserProfile p, boolean revealSensitive) {
        User u = p.getUser();
        return new BuyerDtos.BuyerProfileResponse(
                u.getId(),
                u.getReferenceNo(),
                u.getFullName(),
                revealSensitive ? u.getEmail() : Masking.email(u.getEmail()),
                revealSensitive ? u.getMobile() : Masking.mobile(u.getMobile()),
                u.getStatus(),
                u.getStatusRemarks(),
                p.getOrganisationName(),
                p.getConstitution(),
                p.getPurchaserType(),
                p.getContactPerson(),
                p.getDesignation(),
                revealSensitive ? p.getPan() : Masking.pan(p.getPan()),
                revealSensitive ? p.getGstin() : Masking.gstin(p.getGstin()),
                revealSensitive ? p.getUdyamNumber() : Masking.generic(p.getUdyamNumber()),
                p.getIndustrySector(),
                p.getBusinessActivity(),
                revealSensitive ? p.getAddressLine() : null,
                p.getCity(),
                p.getState(),
                p.getDistrict(),
                p.getPincode(),
                p.getWebsite(),
                p.getCreatedAt()
        );
    }

    public SellerDtos.DocumentResponse document(SellerDocument d) {
        return new SellerDtos.DocumentResponse(d.getId(), d.getDocumentType(), d.getOriginalName(), d.getContentType(),
                d.getFileSize(), d.isVerified(), d.getRemarks(), d.getCreatedAt());
    }

    public SellerDtos.VerificationRecordResponse history(VerificationRecord r) {
        return new SellerDtos.VerificationRecordResponse(r.getId(), r.getAction(), r.getFromStatus(), r.getToStatus(),
                r.getRemarks(), r.getActorName(), r.getCreatedAt());
    }

    public List<SellerDtos.RequiredDocument> checklist(SellerProfile profile, List<SellerDocument> documents) {
        Set<Enums.DocumentType> mandatory = mandatoryDocuments(profile.getProviderType(), profile.getGstin(), profile.getUdyamNumber());
        List<SellerDtos.RequiredDocument> result = new ArrayList<>();
        for (Enums.DocumentType type : Arrays.asList(Enums.DocumentType.PAN, Enums.DocumentType.REGISTRATION_CERTIFICATE,
                Enums.DocumentType.GST_CERTIFICATE, Enums.DocumentType.UDYAM_CERTIFICATE, Enums.DocumentType.PROFESSIONAL_CREDENTIAL)) {
            boolean uploaded = documents.stream().anyMatch(d -> d.getDocumentType() == type);
            boolean verified = documents.stream().anyMatch(d -> d.getDocumentType() == type && d.isVerified());
            result.add(new SellerDtos.RequiredDocument(type, mandatory.contains(type), uploaded, verified));
        }
        return result;
    }

    public static Set<Enums.DocumentType> mandatoryDocuments(Enums.ProviderType providerType, String gstin, String udyam) {
        Set<Enums.DocumentType> result = EnumSet.of(Enums.DocumentType.PAN);
        if (providerType != Enums.ProviderType.INDIVIDUAL_CONSULTANT) {
            result.add(Enums.DocumentType.REGISTRATION_CERTIFICATE);
        } else {
            result.add(Enums.DocumentType.PROFESSIONAL_CREDENTIAL);
        }
        if (gstin != null && !gstin.isBlank()) {
            result.add(Enums.DocumentType.GST_CERTIFICATE);
        }
        if (udyam != null && !udyam.isBlank()) {
            result.add(Enums.DocumentType.UDYAM_CERTIFICATE);
        }
        return result;
    }

    public ProviderDtos.ProviderCard providerCard(SellerProfile p) {
        User u = p.getUser();
        return new ProviderDtos.ProviderCard(
                u.getId(),
                u.getReferenceNo(),
                p.getOrganisationName(),
                p.getProviderType(),
                p.getCity(),
                p.getState(),
                p.getDistrict(),
                p.getYearsExperience(),
                p.getTeamSize(),
                Csv.split(p.getExpertise()),
                Csv.split(p.getCategoryNames()),
                Csv.split(p.getDeliveryModes()),
                p.getAverageRating(),
                p.getRatingCount(),
                u.getStatus() == Enums.UserStatus.ACTIVE,
                p.isFeatured()
        );
    }

    public ProviderDtos.ProviderProfile providerProfile(SellerProfile p) {
        return new ProviderDtos.ProviderProfile(
                providerCard(p),
                p.getIntroduction(),
                p.getWebsite(),
                p.getYearEstablished(),
                p.getConstitution(),
                p.getCertifications(),
                p.getTeamProfile(),
                p.getPreviousAssignments(),
                Csv.split(p.getIndustries()),
                Csv.split(p.getGeographicCoverage()),
                p.getVerifiedAt()
        );
    }

    public UserSummary summary(User u, String organisationName, String state, String district, Double rating, Integer ratingCount) {
        return new UserSummary(u.getId(), u.getFullName(), organisationName, u.getRole().name(), u.getStatus().name(),
                u.getReferenceNo(), state, district, rating, ratingCount, permissionCodes(u));
    }

    public static String categoryIdsCsv(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return "," + ids.stream().distinct().map(String::valueOf).collect(Collectors.joining(",")) + ",";
    }

    private static List<Long> ids(String csv) {
        return Csv.split(csv).stream().map(Long::valueOf).toList();
    }

    private static int completion(SellerProfile p) {
        Object[] fields = {p.getOrganisationName(), p.getPan(), p.getAddressLine(), p.getState(), p.getDistrict(),
                p.getCategoryNames(), p.getExpertise(), p.getYearsExperience(), p.getTeamSize(), p.getGeographicCoverage(),
                p.getDeliveryModes(), p.getWebsite(), p.getIntroduction(), p.getYearEstablished(), p.getCertifications(),
                p.getTeamProfile(), p.getPreviousAssignments(), p.getIndustries(), p.getClientReferences(), p.getPincode()};
        long filled = Arrays.stream(fields).filter(f -> f != null && !(f instanceof String s && s.isBlank())).count();
        return (int) Math.round(filled * 100.0 / fields.length);
    }
}
