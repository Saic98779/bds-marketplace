package com.sidbi.bds.catalog.service;

import com.sidbi.bds.catalog.domain.CatalogEnums;
import com.sidbi.bds.catalog.domain.CatalogSeller;
import com.sidbi.bds.catalog.domain.ServiceListing;
import com.sidbi.bds.catalog.dto.ListingDtos;
import com.sidbi.bds.catalog.repository.CatalogSellerRepository;
import com.sidbi.bds.catalog.repository.ServiceCategoryRepository;
import com.sidbi.bds.catalog.repository.ServiceListingRepository;
import com.sidbi.bds.common.api.PageRequestFactory;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.client.dto.ListingSummary;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.util.Search;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PublicCatalogService {

    private final ServiceListingRepository listingRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final CatalogSellerRepository sellerRepository;
    private final CatalogSellerService sellerService;
    private final ListingMapper mapper;

    public record ServiceFilter(
            String q,
            Long categoryId,
            Long subcategoryId,
            String state,
            CatalogEnums.DeliveryMode deliveryMode,
            CatalogEnums.PricingModel pricingModel,
            Integer minExperience,
            BigDecimal maxFee,
            Long sellerId,
            Boolean featured,
            String industry,
            String sort
    ) {
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingDtos.ListingCard> search(ServiceFilter filter, int page, int size) {
        Sort sort = switch (filter.sort() == null ? "" : filter.sort()) {
            case "experience" -> Sort.by(Sort.Order.desc("experienceYears").nullsLast());
            case "recent" -> Sort.by(Sort.Direction.DESC, "publishedAt");
            case "feeAsc" -> Sort.by(Sort.Order.asc("indicativeFee").nullsLast());
            case "feeDesc" -> Sort.by(Sort.Order.desc("indicativeFee").nullsLast());
            case "popular" -> Sort.by(Sort.Direction.DESC, "enquiryCount", "viewCount");
            case "location" -> Sort.by(Sort.Order.asc("state").nullsLast(), Sort.Order.asc("title"));
            default -> Sort.by(Sort.Order.desc("featured"), Sort.Order.desc("enquiryCount"), Sort.Order.desc("publishedAt"));
        };
        Page<ServiceListing> result = listingRepository.findAll(spec(filter), PageRequestFactory.of(page, size, sort));
        Map<Long, CatalogSeller> sellers = sellerService.lookup(result.getContent().stream().map(ServiceListing::getSellerId).distinct().toList());
        return PageResponse.of(result, l -> mapper.card(l, sellers.get(l.getSellerId())));
    }

    @Transactional
    public ListingDtos.ListingDetail detail(Long id) {
        ServiceListing listing = listingRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Service not found"));
        CatalogSeller seller = sellerService.find(listing.getSellerId());
        if (!ListingMapper.isPublic(listing, seller)) {
            throw BusinessException.notFound("Service not found");
        }
        listingRepository.incrementViews(id);
        return mapper.detail(listing, seller);
    }

    @Transactional(readOnly = true)
    public ListingDtos.PublicStats stats() {
        return new ListingDtos.PublicStats(listingRepository.countPublic(), sellerRepository.countActiveWithListings(),
                categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().size());
    }

    @Transactional(readOnly = true)
    public ListingSummary summary(Long id) {
        ServiceListing listing = listingRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Service not found"));
        CatalogSeller seller = sellerService.find(listing.getSellerId());
        return new ListingSummary(listing.getId(), listing.getReferenceNo(), listing.getTitle(), listing.getSellerId(), listing.getSellerName(),
                listing.getCategory().getId(), listing.getCategory().getName(),
                listing.getSubcategory() != null ? listing.getSubcategory().getId() : null,
                listing.getSubcategory() != null ? listing.getSubcategory().getName() : null,
                listing.getDeliveryMode().name(), listing.getStatus().name(), ListingMapper.isPublic(listing, seller));
    }

    @Transactional
    public void recordEnquiry(Long id) {
        listingRepository.incrementEnquiries(id);
    }

    @Transactional(readOnly = true)
    public List<Long> sellersForCategory(Long categoryId, Long subcategoryId) {
        return listingRepository.activeSellersForCategory(categoryId, subcategoryId);
    }

    private static Specification<ServiceListing> spec(ServiceFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), CatalogEnums.ListingStatus.APPROVED));
            Subquery<Long> activeSeller = query.subquery(Long.class);
            Root<CatalogSeller> seller = activeSeller.from(CatalogSeller.class);
            activeSeller.select(seller.get("sellerId")).where(cb.isTrue(seller.get("active")));
            predicates.add(root.get("sellerId").in(activeSeller));
            if (filter.q() != null && !filter.q().isBlank()) {
                String like = Search.contains(filter.q());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("shortDescription")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("description")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("tags")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("sellerName")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("category").get("name")), like, Search.ESCAPE)));
            }
            if (filter.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.categoryId()));
            }
            if (filter.subcategoryId() != null) {
                predicates.add(cb.equal(root.get("subcategory").get("id"), filter.subcategoryId()));
            }
            if (filter.state() != null && !filter.state().isBlank()) {
                predicates.add(cb.or(
                        cb.equal(root.get("state"), filter.state()),
                        cb.like(root.get("serviceLocations"), Search.containsExact(filter.state()), Search.ESCAPE),
                        cb.equal(root.get("deliveryMode"), CatalogEnums.DeliveryMode.ONLINE)));
            }
            if (filter.deliveryMode() != null) {
                predicates.add(cb.equal(root.get("deliveryMode"), filter.deliveryMode()));
            }
            if (filter.pricingModel() != null) {
                predicates.add(cb.equal(root.get("pricingModel"), filter.pricingModel()));
            }
            if (filter.minExperience() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("experienceYears"), filter.minExperience()));
            }
            if (filter.maxFee() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("indicativeFee"), filter.maxFee()));
            }
            if (filter.sellerId() != null) {
                predicates.add(cb.equal(root.get("sellerId"), filter.sellerId()));
            }
            if (Boolean.TRUE.equals(filter.featured())) {
                predicates.add(cb.isTrue(root.get("featured")));
            }
            if (filter.industry() != null && !filter.industry().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("targetIndustries")), Search.contains(filter.industry()), Search.ESCAPE));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
