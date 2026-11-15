package com.sidbi.bds.catalog.service;

import com.sidbi.bds.catalog.domain.CatalogEnums.ListingStatus;
import com.sidbi.bds.catalog.domain.CatalogEnums.ModerationAction;
import com.sidbi.bds.catalog.domain.CatalogSeller;
import com.sidbi.bds.catalog.domain.ServiceListing;
import com.sidbi.bds.catalog.dto.ListingDtos;
import com.sidbi.bds.catalog.repository.CatalogSellerRepository;
import com.sidbi.bds.catalog.repository.ServiceListingRepository;
import com.sidbi.bds.common.api.PageRequestFactory;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.report.ReportExporter;
import com.sidbi.bds.common.report.ReportFormat;
import com.sidbi.bds.common.report.ReportTable;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.util.References;
import com.sidbi.bds.common.util.Search;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final Set<ListingStatus> REVIEWABLE = EnumSet.of(ListingStatus.SUBMITTED, ListingStatus.UNDER_REVIEW);

    private final ServiceListingRepository listingRepository;
    private final CatalogSellerRepository sellerRepository;
    private final ListingService listingService;
    private final CatalogSellerService sellerService;
    private final NotificationClient notificationClient;
    private final ReportExporter exporter;
    private final ListingMapper mapper;

    public record AdminListingFilter(ListingStatus status, String q, Long categoryId, Long sellerId) {
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingDtos.ListingCard> list(AdminListingFilter filter, int page, int size) {
        CurrentUser.require(Permission.LISTING_MODERATE);
        Page<ServiceListing> result = listingRepository.findAll(spec(filter),
                PageRequestFactory.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        Map<Long, CatalogSeller> sellers = sellerService.lookup(result.getContent().stream().map(ServiceListing::getSellerId).distinct().toList());
        return PageResponse.of(result, l -> mapper.card(l, sellers.get(l.getSellerId())));
    }

    @Transactional(readOnly = true)
    public ListingDtos.ListingDetail detail(Long id) {
        CurrentUser.require(Permission.LISTING_MODERATE);
        ServiceListing listing = listingService.listing(id);
        return mapper.detail(listing, sellerService.find(listing.getSellerId()));
    }

    @Transactional
    public ListingDtos.ListingDetail createOnBehalf(ListingDtos.AdminListingRequest request) {
        CurrentUser.require(Permission.LISTING_MODERATE);
        CatalogSeller seller = sellerService.ensureSeller(request.sellerId());
        if (!seller.isActive()) {
            throw BusinessException.conflict("Listings can be added only for approved and active sellers");
        }
        ServiceListing listing = new ServiceListing();
        listing.setSellerId(request.sellerId());
        listing.setSellerName(seller.getSellerName());
        listing.setStatus(ListingStatus.DRAFT);
        listing.setCreatedByAdmin(CurrentUser.id());
        listingService.apply(listing, request.listing());
        listingRepository.save(listing);
        listing.setReferenceNo(References.of("SVC", listing.getId()));
        listingService.record(listing, ModerationAction.CREATED, null, ListingStatus.DRAFT, "Created by administrator");
        if (Boolean.TRUE.equals(request.listing().submit())) {
            approveInternal(listing, "Added and approved by administrator");
        }
        notificationClient.audit("LISTING_CREATED_BY_ADMIN", "SERVICE_LISTING", listing.getId(), listing.getTitle());
        return mapper.detail(listing, seller);
    }

    @Transactional
    public ListingDtos.ListingDetail startReview(Long id) {
        CurrentUser.require(Permission.LISTING_MODERATE);
        ServiceListing listing = listingService.listing(id);
        if (listing.getStatus() != ListingStatus.SUBMITTED) {
            throw BusinessException.conflict("Only submitted listings can be taken up for review");
        }
        transition(listing, ListingStatus.UNDER_REVIEW, ModerationAction.REVIEW_STARTED, null);
        return mapper.detail(listing, sellerService.find(listing.getSellerId()));
    }

    @Transactional
    public ListingDtos.ListingDetail approve(Long id, String remarks) {
        CurrentUser.require(Permission.LISTING_MODERATE);
        ServiceListing listing = listingService.listing(id);
        if (!REVIEWABLE.contains(listing.getStatus()) && listing.getStatus() != ListingStatus.FLAGGED
                && listing.getStatus() != ListingStatus.DRAFT) {
            throw BusinessException.conflict("Listing is not awaiting approval");
        }
        approveInternal(listing, remarks);
        return mapper.detail(listing, sellerService.find(listing.getSellerId()));
    }

    @Transactional
    public ListingDtos.ListingDetail reject(Long id, String remarks) {
        CurrentUser.require(Permission.LISTING_MODERATE);
        ServiceListing listing = listingService.listing(id);
        if (!REVIEWABLE.contains(listing.getStatus())) {
            throw BusinessException.conflict("Only listings awaiting approval can be rejected");
        }
        transition(listing, ListingStatus.REJECTED, ModerationAction.REJECTED, remarks);
        notifySeller(listing, "LISTING_REJECTED", remarks);
        return mapper.detail(listing, sellerService.find(listing.getSellerId()));
    }

    @Transactional
    public ListingDtos.ListingDetail requestCorrection(Long id, String remarks) {
        CurrentUser.require(Permission.LISTING_MODERATE);
        ServiceListing listing = listingService.listing(id);
        if (!REVIEWABLE.contains(listing.getStatus()) && listing.getStatus() != ListingStatus.FLAGGED) {
            throw BusinessException.conflict("Corrections can be requested only for listings under moderation");
        }
        transition(listing, ListingStatus.CORRECTION_REQUESTED, ModerationAction.CORRECTION_REQUESTED, remarks);
        notifySeller(listing, "LISTING_CORRECTION", remarks);
        return mapper.detail(listing, sellerService.find(listing.getSellerId()));
    }

    @Transactional
    public ListingDtos.ListingDetail flag(Long id, String remarks) {
        CurrentUser.require(Permission.LISTING_MODERATE);
        ServiceListing listing = listingService.listing(id);
        if (listing.getStatus() != ListingStatus.APPROVED) {
            throw BusinessException.conflict("Only published listings can be flagged");
        }
        transition(listing, ListingStatus.FLAGGED, ModerationAction.FLAGGED, remarks);
        notifySeller(listing, "LISTING_DEACTIVATED", remarks);
        return mapper.detail(listing, sellerService.find(listing.getSellerId()));
    }

    @Transactional
    public ListingDtos.ListingDetail deactivate(Long id, String remarks) {
        CurrentUser.require(Permission.LISTING_MODERATE);
        ServiceListing listing = listingService.listing(id);
        if (listing.getStatus() == ListingStatus.DEACTIVATED) {
            throw BusinessException.conflict("Listing is already deactivated");
        }
        transition(listing, ListingStatus.DEACTIVATED, ModerationAction.DEACTIVATED, remarks);
        notifySeller(listing, "LISTING_DEACTIVATED", remarks);
        return mapper.detail(listing, sellerService.find(listing.getSellerId()));
    }

    @Transactional
    public ListingDtos.ListingDetail setFeatured(Long id, boolean featured) {
        CurrentUser.require(Permission.LISTING_MODERATE);
        ServiceListing listing = listingService.listing(id);
        listing.setFeatured(featured);
        notificationClient.audit(featured ? "LISTING_FEATURED" : "LISTING_UNFEATURED", "SERVICE_LISTING", id, null);
        return mapper.detail(listing, sellerService.find(listing.getSellerId()));
    }

    @Transactional(readOnly = true)
    public ListingDtos.CatalogStats stats() {
        CurrentUser.require(Permission.ADMIN_DASHBOARD);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ListingStatus s : ListingStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        long total = 0;
        for (Object[] row : listingRepository.countGroupedByStatus()) {
            long count = ((Number) row[1]).longValue();
            byStatus.put(((ListingStatus) row[0]).name(), count);
            total += count;
        }
        List<ListingDtos.CountItem> byCategory = new ArrayList<>();
        for (Object[] row : listingRepository.countAllByCategory()) {
            byCategory.add(new ListingDtos.CountItem(String.valueOf(row[0]), ((Number) row[1]).longValue()));
        }
        return new ListingDtos.CatalogStats(total, listingRepository.countPublic(),
                byStatus.get("SUBMITTED") + byStatus.get("UNDER_REVIEW"), sellerRepository.countActiveWithListings(), byStatus, byCategory);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> report(AdminListingFilter filter, ReportFormat format) {
        CurrentUser.require(Permission.REPORT_VIEW);
        List<ServiceListing> listings = listingRepository.findAll(spec(filter), Sort.by("category.name", "title"));
        Map<Long, CatalogSeller> sellers = sellerService.lookup(listings.stream().map(ServiceListing::getSellerId).distinct().toList());
        ReportTable table = new ReportTable("Service Catalogue Report", "Reference", "Title", "Category", "Subcategory", "Provider",
                "Delivery Mode", "Pricing Model", "Indicative Fee", "Status", "Provider Active", "Views", "Enquiries", "Published On");
        for (ServiceListing l : listings) {
            CatalogSeller seller = sellers.get(l.getSellerId());
            table.row(l.getReferenceNo(), l.getTitle(), l.getCategory().getName(),
                    l.getSubcategory() != null ? l.getSubcategory().getName() : "", l.getSellerName(), l.getDeliveryMode(),
                    l.getPricingModel(), l.getIndicativeFee(), l.getStatus(), seller != null && seller.isActive() ? "Yes" : "No",
                    l.getViewCount(), l.getEnquiryCount(), l.getPublishedAt() == null ? "" : l.getPublishedAt().format(DATE));
        }
        notificationClient.audit("REPORT_EXPORTED", "REPORT", "service-catalogue", format.name());
        return exporter.export(table, format, "service-catalogue");
    }

    private void approveInternal(ServiceListing listing, String remarks) {
        CatalogSeller seller = sellerService.find(listing.getSellerId());
        if (seller == null || !seller.isActive()) {
            throw BusinessException.conflict("The seller is not approved and active. Listing cannot be published.");
        }
        transition(listing, ListingStatus.APPROVED, ModerationAction.APPROVED, remarks);
        if (listing.getPublishedAt() == null) {
            listing.setPublishedAt(LocalDateTime.now());
        }
        notifySeller(listing, "LISTING_APPROVED", remarks);
    }

    private void transition(ServiceListing listing, ListingStatus to, ModerationAction action, String remarks) {
        ListingStatus from = listing.getStatus();
        listing.setStatus(to);
        listing.setModeratedAt(LocalDateTime.now());
        listing.setModeratedBy(CurrentUser.id());
        if (remarks != null) {
            listing.setModerationRemarks(remarks);
        }
        listingService.record(listing, action, from, to, remarks);
        notificationClient.audit("LISTING_" + action.name(), "SERVICE_LISTING", listing.getId(), remarks);
    }

    private void notifySeller(ServiceListing listing, String template, String remarks) {
        notificationClient.notify(listing.getSellerId(), template, "LISTING", "/seller/services/" + listing.getId(),
                Map.of("title", listing.getTitle(), "reference", listing.getReferenceNo() == null ? "" : listing.getReferenceNo(),
                        "remarks", remarks == null ? "" : remarks));
    }

    private static Specification<ServiceListing> spec(AdminListingFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.categoryId()));
            }
            if (filter.sellerId() != null) {
                predicates.add(cb.equal(root.get("sellerId"), filter.sellerId()));
            }
            if (filter.q() != null && !filter.q().isBlank()) {
                String like = Search.contains(filter.q());
                predicates.add(cb.or(cb.like(cb.lower(root.get("title")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("sellerName")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("referenceNo")), like, Search.ESCAPE)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
