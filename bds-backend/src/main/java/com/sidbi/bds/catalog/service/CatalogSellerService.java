package com.sidbi.bds.catalog.service;

import com.sidbi.bds.catalog.domain.CatalogSeller;
import com.sidbi.bds.catalog.repository.CatalogSellerRepository;
import com.sidbi.bds.catalog.repository.ServiceListingRepository;
import com.sidbi.bds.common.client.IdentityClient;
import com.sidbi.bds.common.client.dto.RatingUpdate;
import com.sidbi.bds.common.client.dto.SellerStatusUpdate;
import com.sidbi.bds.common.client.dto.UserSummary;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.security.AuthUser;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogSellerService {

    private final CatalogSellerRepository repository;
    private final ServiceListingRepository listingRepository;
    private final IdentityClient identityClient;

    @Transactional
    public CatalogSeller requirePublishingSeller() {
        AuthUser user = CurrentUser.get();
        if (!user.isSeller() && !user.isAdmin()) {
            throw BusinessException.forbidden("Only sellers can manage service listings");
        }
        Long sellerId = CurrentUser.subjectId();
        UserSummary summary = identityClient.summary(sellerId);
        if (!"SELLER".equals(summary.role())) {
            throw BusinessException.badRequest("User " + sellerId + " is not a service provider");
        }
        if (!summary.active()) {
            throw BusinessException.forbidden("Your account is " + summary.status().toLowerCase().replace('_', ' ')
                    + ". Only approved and active sellers can publish services.");
        }
        if (summary.permissions() == null || !summary.permissions().contains(Permission.LISTING_PUBLISH.name())) {
            throw BusinessException.forbidden("Service publishing privilege has not been granted to your account");
        }
        return upsert(sellerId, true, summary.displayName(), summary.state(), summary.averageRating(), summary.ratingCount());
    }

    @Transactional
    public CatalogSeller ensureSeller(Long sellerId) {
        UserSummary summary = identityClient.summary(sellerId);
        if (!"SELLER".equals(summary.role())) {
            throw BusinessException.badRequest("Selected user is not a seller");
        }
        boolean active = summary.active() && summary.permissions() != null && summary.permissions().contains(Permission.LISTING_PUBLISH.name());
        return upsert(sellerId, active, summary.displayName(), summary.state(), summary.averageRating(), summary.ratingCount());
    }

    @Transactional
    public void updateStatus(Long sellerId, SellerStatusUpdate update) {
        CatalogSeller seller = upsert(sellerId, update.active(), update.sellerName(), update.state(), null, null);
        if (update.sellerName() != null) {
            listingRepository.renameSeller(sellerId, update.sellerName());
        }
        seller.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    public void updateRating(Long sellerId, RatingUpdate update) {
        CatalogSeller seller = repository.findById(sellerId).orElseGet(() -> {
            CatalogSeller s = new CatalogSeller();
            s.setSellerId(sellerId);
            return s;
        });
        seller.setAverageRating(Math.round(update.average() * 10.0) / 10.0);
        seller.setRatingCount(update.count());
        seller.setUpdatedAt(LocalDateTime.now());
        repository.save(seller);
    }

    @Transactional(readOnly = true)
    public Map<Long, CatalogSeller> lookup(Collection<Long> sellerIds) {
        if (sellerIds.isEmpty()) {
            return Map.of();
        }
        return repository.findBySellerIdIn(sellerIds).stream().collect(Collectors.toMap(CatalogSeller::getSellerId, Function.identity()));
    }

    @Transactional(readOnly = true)
    public CatalogSeller find(Long sellerId) {
        return repository.findById(sellerId).orElse(null);
    }

    private CatalogSeller upsert(Long sellerId, boolean active, String name, String state, Double rating, Integer ratingCount) {
        CatalogSeller seller = repository.findById(sellerId).orElseGet(() -> {
            CatalogSeller s = new CatalogSeller();
            s.setSellerId(sellerId);
            return s;
        });
        seller.setActive(active);
        if (name != null) {
            seller.setSellerName(name);
        }
        if (state != null) {
            seller.setState(state);
        }
        if (rating != null) {
            seller.setAverageRating(rating);
        }
        if (ratingCount != null) {
            seller.setRatingCount(ratingCount);
        }
        seller.setUpdatedAt(LocalDateTime.now());
        return repository.save(seller);
    }
}
