package com.sidbi.bds.auth.service;

import com.sidbi.bds.auth.domain.PurchaserProfile;
import com.sidbi.bds.auth.domain.SellerProfile;
import com.sidbi.bds.auth.domain.User;
import com.sidbi.bds.auth.repository.PurchaserProfileRepository;
import com.sidbi.bds.auth.repository.SellerProfileRepository;
import com.sidbi.bds.auth.repository.UserRepository;
import com.sidbi.bds.common.client.dto.RatingUpdate;
import com.sidbi.bds.common.client.dto.UserContact;
import com.sidbi.bds.common.client.dto.UserSummary;
import com.sidbi.bds.common.crypto.FieldCrypto;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternalIdentityService {

    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final PurchaserProfileRepository purchaserProfileRepository;
    private final FieldCrypto crypto;
    private final UserMapper mapper;

    @Transactional(readOnly = true)
    public UserSummary summary(Long userId) {
        List<UserSummary> list = summaries(List.of(userId));
        if (list.isEmpty()) {
            throw BusinessException.notFound("User not found");
        }
        return list.getFirst();
    }

    @Transactional(readOnly = true)
    public List<UserSummary> summaries(Collection<Long> ids) {
        List<User> users = userRepository.findAllById(ids);
        Map<Long, SellerProfile> sellers = sellerProfileRepository.findByUserIdIn(ids).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity()));
        Map<Long, PurchaserProfile> buyers = purchaserProfileRepository.findByUserIdIn(ids).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity()));
        return users.stream().map(u -> {
            SellerProfile s = sellers.get(u.getId());
            PurchaserProfile b = buyers.get(u.getId());
            if (s != null) {
                return mapper.summary(u, s.getOrganisationName(), s.getState(), s.getDistrict(), s.getAverageRating(), s.getRatingCount());
            }
            if (b != null) {
                return mapper.summary(u, b.getOrganisationName(), b.getState(), b.getDistrict(), null, null);
            }
            return mapper.summary(u, null, null, null, null, null);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Long> withPermission(Permission permission) {
        return userRepository.findActiveIdsWithPermission(permission);
    }

    @Transactional(readOnly = true)
    public UserContact contact(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> BusinessException.notFound("User not found"));
        return new UserContact(user.getId(), user.getFullName(), user.isEmailAlerts() ? user.getEmail() : null, user.getMobile());
    }

    @Transactional(readOnly = true)
    public UserSummary lookupByEmail(String email) {
        User user = userRepository.findByEmailHash(crypto.blindIndex(email))
                .orElseThrow(() -> BusinessException.notFound("User not found"));
        return summary(user.getId());
    }

    @Transactional
    public void updateRating(Long sellerId, RatingUpdate update) {
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .filter(p -> p.getUser().getRole() == Role.SELLER)
                .orElseThrow(() -> BusinessException.notFound("Seller not found"));
        profile.setAverageRating(Math.round(update.average() * 10.0) / 10.0);
        profile.setRatingCount(update.count());
    }
}
