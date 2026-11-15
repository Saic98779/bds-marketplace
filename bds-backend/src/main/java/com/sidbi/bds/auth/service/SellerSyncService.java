package com.sidbi.bds.auth.service;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.domain.SellerProfile;
import com.sidbi.bds.auth.domain.User;
import com.sidbi.bds.auth.repository.SellerProfileRepository;
import com.sidbi.bds.auth.repository.UserRepository;
import com.sidbi.bds.common.client.CatalogClient;
import com.sidbi.bds.common.client.dto.SellerStatusUpdate;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SellerSyncService {

    private final CatalogClient catalogClient;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;

    public void sync(User user) {
        if (user.getRole() != Role.SELLER) {
            return;
        }
        SellerProfile profile = sellerProfileRepository.findByUserId(user.getId()).orElse(null);
        boolean active = user.getStatus() == Enums.UserStatus.ACTIVE && user.getPermissions().contains(Permission.LISTING_PUBLISH);
        try {
            catalogClient.updateSellerStatus(user.getId(), new SellerStatusUpdate(active,
                    profile != null ? profile.getOrganisationName() : user.getFullName(),
                    profile != null ? profile.getState() : null));
            user.setCatalogSyncPending(false);
        } catch (RuntimeException ex) {
            user.setCatalogSyncPending(true);
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    @Transactional
    public void retryPending() {
        for (User user : userRepository.findByCatalogSyncPendingTrue()) {
            sync(user);
        }
    }
}
