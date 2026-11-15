package com.sidbi.bds.auth.config;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.domain.PurchaserProfile;
import com.sidbi.bds.auth.domain.SellerProfile;
import com.sidbi.bds.auth.domain.User;
import com.sidbi.bds.auth.repository.PurchaserProfileRepository;
import com.sidbi.bds.auth.repository.SellerProfileRepository;
import com.sidbi.bds.auth.repository.UserRepository;
import com.sidbi.bds.auth.service.UserMapper;
import com.sidbi.bds.common.crypto.FieldCrypto;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.security.Role;
import com.sidbi.bds.common.util.References;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BootstrapInitializer implements ApplicationRunner {

    private final AuthProperties properties;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final PurchaserProfileRepository purchaserProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final FieldCrypto crypto;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AuthProperties.Bootstrap bootstrap = properties.getBootstrap();
        if (!userRepository.existsByRole(Role.ADMIN) && bootstrap.getAdminPassword() != null && !bootstrap.getAdminPassword().isBlank()) {
            User admin = user(bootstrap.getAdminName(), bootstrap.getAdminEmail(), null, bootstrap.getAdminPassword(), Role.ADMIN);
            admin.setDesignation("Super Administrator");
            admin.setPermissions(EnumSet.copyOf(Permission.defaultsFor(Role.ADMIN)));
            userRepository.save(admin);
            admin.setReferenceNo(References.of("ADM", admin.getId()));
        }
        if (bootstrap.isDemoUsers() && bootstrap.getDemoPassword() != null
                && !userRepository.existsByEmailHash(crypto.blindIndex("seller@bdsmarketplace.in"))) {
            createDemoSeller(bootstrap.getDemoPassword());
            createDemoBuyer(bootstrap.getDemoPassword());
        }
    }

    private void createDemoSeller(String password) {
        User seller = user("Ananya Rao", "seller@bdsmarketplace.in", "9876543210", password, Role.SELLER);
        seller.setPermissions(EnumSet.copyOf(Permission.defaultsFor(Role.SELLER)));
        seller.setCatalogSyncPending(true);
        userRepository.save(seller);
        seller.setReferenceNo(References.of("SLR", seller.getId()));
        SellerProfile profile = new SellerProfile();
        profile.setUser(seller);
        profile.setOrganisationName("ABC Business Consulting Pvt Ltd");
        profile.setConstitution(Enums.Constitution.PRIVATE_LIMITED);
        profile.setPan("AAACA1234B");
        profile.setPanHash(crypto.blindIndex("AAACA1234B"));
        profile.setGstin("09AAACA1234B1Z5");
        profile.setAddressLine("12 Hazratganj Business Centre");
        profile.setCity("Lucknow");
        profile.setState("Uttar Pradesh");
        profile.setDistrict("Lucknow");
        profile.setPincode("226001");
        profile.setProviderType(Enums.ProviderType.CONSULTANCY_FIRM);
        profile.setCategoryIds(UserMapper.categoryIdsCsv(List.of(1L, 2L)));
        profile.setCategoryNames("Business & Management Consultancy,Finance & Credit Facilitation");
        profile.setExpertise("DPR,Financial Modelling,Project Consultancy,Credit Facilitation");
        profile.setIndustries("Manufacturing,Food Processing,Textiles");
        profile.setYearsExperience(12);
        profile.setTeamSize(24);
        profile.setGeographicCoverage("Uttar Pradesh,Delhi,Uttarakhand,Bihar");
        profile.setDeliveryModes("ONLINE,ONSITE,HYBRID");
        profile.setWebsite("https://abcconsulting.example.in");
        profile.setIntroduction("ABC Business Consulting supports MSMEs with detailed project reports, financial modelling and credit facilitation across North India.");
        profile.setYearEstablished(2012);
        profile.setCertifications("ISO 9001:2015, Empanelled with SIDBI and state industrial development agencies");
        profile.setTeamProfile("Chartered accountants, MBA finance professionals and sector specialists");
        profile.setPreviousAssignments("Prepared over 300 DPRs for term loans; supported 40 MSMEs in CGTMSE-backed credit");
        profile.setVerifiedAt(LocalDateTime.now());
        profile.setReverificationDue(LocalDate.now().plusYears(1));
        profile.setFeatured(true);
        profile.setRatingCount(0);
        sellerProfileRepository.save(profile);
    }

    private void createDemoBuyer(String password) {
        User buyer = user("Rahul Verma", "buyer@bdsmarketplace.in", "9876501234", password, Role.BUYER);
        buyer.setDesignation("Managing Director");
        buyer.setPermissions(EnumSet.copyOf(Permission.defaultsFor(Role.BUYER)));
        userRepository.save(buyer);
        buyer.setReferenceNo(References.of("PUR", buyer.getId()));
        PurchaserProfile profile = new PurchaserProfile();
        profile.setUser(buyer);
        profile.setOrganisationName("Verma Precision Components");
        profile.setConstitution(Enums.Constitution.PROPRIETORSHIP);
        profile.setPurchaserType(Enums.PurchaserType.SMALL);
        profile.setContactPerson("Rahul Verma");
        profile.setDesignation("Managing Director");
        profile.setUdyamNumber("UDYAM-UP-43-0012345");
        profile.setIndustrySector("Manufacturing");
        profile.setBusinessActivity("Precision machined components for automotive OEMs");
        profile.setAddressLine("Plot 45, Industrial Area Phase II");
        profile.setCity("Kanpur");
        profile.setState("Uttar Pradesh");
        profile.setDistrict("Kanpur Nagar");
        profile.setPincode("208022");
        purchaserProfileRepository.save(profile);
    }

    private User user(String name, String email, String mobile, String password, Role role) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setEmailHash(crypto.blindIndex(email));
        if (mobile != null) {
            user.setMobile(mobile);
            user.setMobileHash(crypto.blindIndex(mobile));
        }
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setRole(role);
        user.setStatus(Enums.UserStatus.ACTIVE);
        user.setStatusChangedAt(LocalDateTime.now());
        user.setEmailVerified(true);
        return user;
    }
}
