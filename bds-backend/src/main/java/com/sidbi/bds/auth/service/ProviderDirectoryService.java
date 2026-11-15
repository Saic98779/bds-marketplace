package com.sidbi.bds.auth.service;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.domain.SellerProfile;
import com.sidbi.bds.auth.domain.User;
import com.sidbi.bds.auth.dto.ProviderDtos;
import com.sidbi.bds.auth.repository.SellerProfileRepository;
import com.sidbi.bds.common.api.PageRequestFactory;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.util.Search;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderDirectoryService {

    private final SellerProfileRepository repository;
    private final UserMapper mapper;

    public record ProviderFilter(
            String q,
            String state,
            String district,
            Long categoryId,
            Enums.ProviderType providerType,
            Integer minExperience,
            String deliveryMode,
            Boolean featured,
            String sort
    ) {
    }

    @Transactional(readOnly = true)
    public PageResponse<ProviderDtos.ProviderCard> search(ProviderFilter filter, int page, int size) {
        Sort sort = switch (filter.sort() == null ? "" : filter.sort()) {
            case "experience" -> Sort.by(Sort.Direction.DESC, "yearsExperience");
            case "rating" -> Sort.by(Sort.Order.desc("averageRating").nullsLast());
            case "name" -> Sort.by(Sort.Direction.ASC, "organisationName");
            case "recent" -> Sort.by(Sort.Direction.DESC, "verifiedAt");
            default -> Sort.by(Sort.Order.desc("featured"), Sort.Order.desc("averageRating").nullsLast(), Sort.Order.asc("organisationName"));
        };
        Page<SellerProfile> result = repository.findAll(spec(filter), PageRequestFactory.of(page, size, sort));
        return PageResponse.of(result, mapper::providerCard);
    }

    @Transactional(readOnly = true)
    public ProviderDtos.ProviderProfile profile(Long sellerId) {
        SellerProfile profile = repository.findByUserId(sellerId)
                .filter(p -> p.getUser().getStatus() == Enums.UserStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.notFound("Provider not found"));
        return mapper.providerProfile(profile);
    }

    private static Specification<SellerProfile> spec(ProviderFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<SellerProfile, User> user;
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                Fetch<SellerProfile, User> fetch = root.fetch("user", JoinType.INNER);
                @SuppressWarnings("unchecked")
                Join<SellerProfile, User> joined = (Join<SellerProfile, User>) fetch;
                user = joined;
            } else {
                user = root.join("user", JoinType.INNER);
            }
            predicates.add(cb.equal(user.get("status"), Enums.UserStatus.ACTIVE));
            if (filter.q() != null && !filter.q().isBlank()) {
                String like = Search.contains(filter.q());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("organisationName")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("expertise")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("categoryNames")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("introduction")), like, Search.ESCAPE)));
            }
            if (filter.state() != null && !filter.state().isBlank()) {
                predicates.add(cb.or(
                        cb.equal(root.get("state"), filter.state()),
                        cb.like(root.get("geographicCoverage"), Search.containsExact(filter.state()), Search.ESCAPE)));
            }
            if (filter.district() != null && !filter.district().isBlank()) {
                predicates.add(cb.equal(root.get("district"), filter.district()));
            }
            if (filter.categoryId() != null) {
                predicates.add(cb.like(root.get("categoryIds"), "%," + filter.categoryId() + ",%", Search.ESCAPE));
            }
            if (filter.providerType() != null) {
                predicates.add(cb.equal(root.get("providerType"), filter.providerType()));
            }
            if (filter.minExperience() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("yearsExperience"), filter.minExperience()));
            }
            if (filter.deliveryMode() != null && !filter.deliveryMode().isBlank()) {
                predicates.add(cb.like(root.get("deliveryModes"), Search.containsExact(filter.deliveryMode()), Search.ESCAPE));
            }
            if (Boolean.TRUE.equals(filter.featured())) {
                predicates.add(cb.isTrue(root.get("featured")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
