package com.sidbi.bds.catalog.service;

import com.sidbi.bds.catalog.domain.CatalogEnums.ListingStatus;
import com.sidbi.bds.catalog.domain.CatalogEnums.ModerationAction;
import com.sidbi.bds.catalog.domain.CatalogSeller;
import com.sidbi.bds.catalog.domain.ListingModeration;
import com.sidbi.bds.catalog.domain.ServiceAttachment;
import com.sidbi.bds.catalog.domain.ServiceCategory;
import com.sidbi.bds.catalog.domain.ServiceListing;
import com.sidbi.bds.catalog.domain.ServiceSubcategory;
import com.sidbi.bds.catalog.dto.ListingDtos;
import com.sidbi.bds.catalog.repository.ListingModerationRepository;
import com.sidbi.bds.catalog.repository.ServiceListingRepository;
import com.sidbi.bds.catalog.repository.ServiceSubcategoryRepository;
import com.sidbi.bds.common.api.PageRequestFactory;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.client.dto.NotificationRequest;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.security.AuthUser;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.storage.FileStorageService;
import com.sidbi.bds.common.storage.StoredFile;
import com.sidbi.bds.common.util.Csv;
import com.sidbi.bds.common.util.References;
import com.sidbi.bds.common.util.Search;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ListingService {

    private static final Set<ListingStatus> SELLER_EDITABLE = EnumSet.of(ListingStatus.DRAFT, ListingStatus.CORRECTION_REQUESTED,
            ListingStatus.REJECTED, ListingStatus.APPROVED, ListingStatus.DEACTIVATED, ListingStatus.SUBMITTED);
    private static final int MAX_ATTACHMENTS = 10;

    private final ServiceListingRepository listingRepository;
    private final ServiceSubcategoryRepository subcategoryRepository;
    private final ListingModerationRepository moderationRepository;
    private final CategoryService categoryService;
    private final CatalogSellerService sellerService;
    private final FileStorageService storage;
    private final NotificationClient notificationClient;
    private final ListingMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<ListingDtos.ListingCard> mine(ListingStatus status, String q, int page, int size) {
        Long sellerId = CurrentUser.scopeId();
        Specification<ServiceListing> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (sellerId != null) {
                predicates.add(cb.equal(root.get("sellerId"), sellerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), Search.contains(q), Search.ESCAPE));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<ServiceListing> result = listingRepository.findAll(spec, PageRequestFactory.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        if (sellerId == null) {
            return PageResponse.of(result, l -> mapper.card(l, sellerService.find(l.getSellerId())));
        }
        CatalogSeller seller = sellerService.find(sellerId);
        return PageResponse.of(result, l -> mapper.card(l, seller));
    }

    @Transactional(readOnly = true)
    public ListingDtos.ListingDetail mineDetail(Long id) {
        ServiceListing listing = owned(id);
        return mapper.detail(listing, sellerService.find(listing.getSellerId()));
    }

    @Transactional
    public ListingDtos.ListingDetail create(ListingDtos.ListingRequest request) {
        CatalogSeller seller = sellerService.requirePublishingSeller();
        ServiceListing listing = new ServiceListing();
        listing.setSellerId(seller.getSellerId());
        listing.setSellerName(seller.getSellerName());
        listing.setStatus(ListingStatus.DRAFT);
        apply(listing, request);
        listingRepository.save(listing);
        listing.setReferenceNo(References.of("SVC", listing.getId()));
        record(listing, ModerationAction.CREATED, null, ListingStatus.DRAFT, null);
        if (Boolean.TRUE.equals(request.submit())) {
            submitInternal(listing);
        }
        notificationClient.audit("LISTING_CREATED", "SERVICE_LISTING", listing.getId(), listing.getTitle());
        return mapper.detail(listing, seller);
    }

    @Transactional
    public ListingDtos.ListingDetail update(Long id, ListingDtos.ListingRequest request) {
        CatalogSeller seller = sellerService.requirePublishingSeller();
        ServiceListing listing = owned(id);
        if (!SELLER_EDITABLE.contains(listing.getStatus())) {
            throw BusinessException.conflict("Listing cannot be edited while it is " + listing.getStatus().name().toLowerCase().replace('_', ' '));
        }
        ListingStatus previous = listing.getStatus();
        apply(listing, request);
        if (previous == ListingStatus.APPROVED) {
            listing.setStatus(ListingStatus.SUBMITTED);
            listing.setSubmittedAt(LocalDateTime.now());
            record(listing, ModerationAction.UPDATED, previous, ListingStatus.SUBMITTED, "Published listing updated; re-moderation required");
            notifyModerators(listing);
        } else {
            record(listing, ModerationAction.UPDATED, previous, previous, null);
        }
        if (Boolean.TRUE.equals(request.submit()) && listing.getStatus() != ListingStatus.SUBMITTED) {
            submitInternal(listing);
        }
        notificationClient.audit("LISTING_UPDATED", "SERVICE_LISTING", id, listing.getTitle());
        return mapper.detail(listing, seller);
    }

    @Transactional
    public ListingDtos.ListingDetail submit(Long id) {
        CatalogSeller seller = sellerService.requirePublishingSeller();
        ServiceListing listing = owned(id);
        if (listing.getStatus() != ListingStatus.DRAFT && listing.getStatus() != ListingStatus.CORRECTION_REQUESTED
                && listing.getStatus() != ListingStatus.REJECTED && listing.getStatus() != ListingStatus.DEACTIVATED) {
            throw BusinessException.conflict("Listing has already been submitted");
        }
        submitInternal(listing);
        return mapper.detail(listing, seller);
    }

    @Transactional
    public ListingDtos.ListingDetail deactivate(Long id, String remarks) {
        ServiceListing listing = owned(id);
        if (listing.getStatus() == ListingStatus.DEACTIVATED) {
            throw BusinessException.conflict("Listing is already deactivated");
        }
        ListingStatus previous = listing.getStatus();
        listing.setStatus(ListingStatus.DEACTIVATED);
        record(listing, ModerationAction.DEACTIVATED, previous, ListingStatus.DEACTIVATED, remarks);
        notificationClient.audit("LISTING_DEACTIVATED", "SERVICE_LISTING", id, remarks);
        return mapper.detail(listing, sellerService.find(listing.getSellerId()));
    }

    @Transactional
    public void delete(Long id) {
        ServiceListing listing = owned(id);
        if (listing.getStatus() != ListingStatus.DRAFT) {
            throw BusinessException.conflict("Only draft listings can be deleted. Deactivate the listing instead.");
        }
        listing.getAttachments().forEach(a -> storage.delete(folder(listing.getId()), a.getStoredName()));
        listingRepository.delete(listing);
        notificationClient.audit("LISTING_DELETED", "SERVICE_LISTING", id, listing.getTitle());
    }

    @Transactional
    public ListingDtos.AttachmentResponse addAttachment(Long id, String label, MultipartFile file) {
        ServiceListing listing = CurrentUser.get().isAdmin() ? listing(id) : owned(id);
        if (listing.getAttachments().size() >= MAX_ATTACHMENTS) {
            throw BusinessException.badRequest("A listing can have at most " + MAX_ATTACHMENTS + " attachments");
        }
        StoredFile stored = storage.store(file, folder(id));
        ServiceAttachment attachment = new ServiceAttachment();
        attachment.setListing(listing);
        attachment.setLabel(label == null || label.isBlank() ? stored.originalName() : label.trim());
        attachment.setOriginalName(stored.originalName());
        attachment.setStoredName(stored.storedName());
        attachment.setContentType(stored.contentType());
        attachment.setFileSize(stored.size());
        listing.getAttachments().add(attachment);
        listingRepository.flush();
        notificationClient.audit("LISTING_ATTACHMENT_ADDED", "SERVICE_LISTING", id, stored.originalName());
        return mapper.attachment(attachment);
    }

    @Transactional
    public void removeAttachment(Long id, Long attachmentId) {
        ServiceListing listing = owned(id);
        ServiceAttachment attachment = listing.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("Attachment not found"));
        storage.delete(folder(id), attachment.getStoredName());
        listing.getAttachments().remove(attachment);
        notificationClient.audit("LISTING_ATTACHMENT_REMOVED", "SERVICE_LISTING", id, attachment.getOriginalName());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadAttachment(Long id, Long attachmentId) {
        AuthUser user = CurrentUser.get();
        ServiceListing listing = listing(id);
        boolean owner = user.isSeller() && listing.getSellerId().equals(user.userId());
        boolean visible = ListingMapper.isPublic(listing, sellerService.find(listing.getSellerId()));
        if (!owner && !user.isAdmin() && !visible) {
            throw BusinessException.notFound("Attachment not found");
        }
        ServiceAttachment attachment = listing.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("Attachment not found"));
        return storage.download(folder(id), attachment.getStoredName(), attachment.getOriginalName(), attachment.getContentType());
    }

    @Transactional(readOnly = true)
    public List<ListingDtos.ModerationResponse> history(Long id) {
        AuthUser user = CurrentUser.get();
        if (!user.isAdmin()) {
            owned(id);
        } else {
            listing(id);
        }
        return moderationRepository.findByListingIdOrderByCreatedAtDesc(id).stream().map(mapper::moderation).toList();
    }

    @Transactional(readOnly = true)
    public ListingDtos.SellerListingStats sellerStats() {
        Long sellerId = CurrentUser.subjectId();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ListingStatus status : ListingStatus.values()) {
            byStatus.put(status.name(), 0L);
        }
        long total = 0;
        for (Object[] row : listingRepository.countBySellerGroupedByStatus(sellerId)) {
            long count = ((Number) row[1]).longValue();
            byStatus.put(((ListingStatus) row[0]).name(), count);
            total += count;
        }
        Object[] engagement = listingRepository.sellerEngagement(sellerId).getFirst();
        long pending = byStatus.get("SUBMITTED") + byStatus.get("UNDER_REVIEW");
        return new ListingDtos.SellerListingStats(total, byStatus.get("APPROVED"), pending,
                ((Number) engagement[0]).longValue(), ((Number) engagement[1]).longValue(), byStatus);
    }

    void apply(ServiceListing listing, ListingDtos.ListingRequest request) {
        ServiceCategory category = categoryService.category(request.categoryId());
        if (!category.isActive()) {
            throw BusinessException.badRequest("Selected category is not active");
        }
        ServiceSubcategory subcategory = null;
        if (request.subcategoryId() != null) {
            subcategory = subcategoryRepository.findByIdAndCategoryId(request.subcategoryId(), category.getId())
                    .orElseThrow(() -> BusinessException.badRequest("Subcategory does not belong to the selected category"));
        }
        listing.setTitle(request.title().trim());
        listing.setCategory(category);
        listing.setSubcategory(subcategory);
        listing.setShortDescription(request.shortDescription());
        listing.setDescription(request.description());
        listing.setScopeOfWork(request.scopeOfWork());
        listing.setDeliverables(request.deliverables());
        listing.setMethodology(request.methodology());
        listing.setTargetIndustries(Csv.join(request.targetIndustries()));
        listing.setDeliveryMode(request.deliveryMode());
        listing.setServiceLocations(Csv.join(request.serviceLocations()));
        listing.setState(request.state());
        listing.setDistrict(request.district());
        listing.setExperienceYears(request.experienceYears());
        listing.setEstimatedDuration(request.estimatedDuration());
        listing.setIndicativeFee(request.indicativeFee());
        listing.setPricingModel(request.pricingModel());
        listing.setTags(Csv.join(request.tags()));
    }

    void submitInternal(ServiceListing listing) {
        validateForSubmission(listing);
        ListingStatus previous = listing.getStatus();
        listing.setStatus(ListingStatus.SUBMITTED);
        listing.setSubmittedAt(LocalDateTime.now());
        record(listing, ModerationAction.SUBMITTED, previous, ListingStatus.SUBMITTED, null);
        notifyModerators(listing);
    }

    void record(ServiceListing listing, ModerationAction action, ListingStatus from, ListingStatus to, String remarks) {
        AuthUser user = CurrentUser.find().orElse(null);
        ListingModeration moderation = new ListingModeration();
        moderation.setListingId(listing.getId());
        moderation.setAction(action);
        moderation.setFromStatus(from);
        moderation.setToStatus(to);
        moderation.setRemarks(remarks);
        if (user != null) {
            moderation.setActorId(user.userId());
            moderation.setActorName(user.name());
            moderation.setActorRole(user.role().name());
        } else {
            moderation.setActorName("System");
            moderation.setActorRole("SYSTEM");
        }
        moderationRepository.save(moderation);
    }

    ServiceListing listing(Long id) {
        return listingRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Service listing not found"));
    }

    private ServiceListing owned(Long id) {
        ServiceListing listing = listing(id);
        if (!CurrentUser.owns(listing.getSellerId())) {
            throw BusinessException.notFound("Service listing not found");
        }
        return listing;
    }

    private void validateForSubmission(ServiceListing listing) {
        List<String> missing = new ArrayList<>();
        if (blank(listing.getScopeOfWork())) {
            missing.add("scope of work");
        }
        if (blank(listing.getDeliverables())) {
            missing.add("deliverables");
        }
        if (listing.getDeliveryMode() != null && listing.getDeliveryMode() != com.sidbi.bds.catalog.domain.CatalogEnums.DeliveryMode.ONLINE
                && blank(listing.getServiceLocations()) && blank(listing.getState())) {
            missing.add("service location");
        }
        for (String field : Csv.split(listing.getCategory().getMandatoryFields())) {
            Object value = switch (field) {
                case "experienceYears" -> listing.getExperienceYears();
                case "estimatedDuration" -> listing.getEstimatedDuration();
                case "indicativeFee" -> listing.getIndicativeFee();
                case "methodology" -> listing.getMethodology();
                case "targetIndustries" -> listing.getTargetIndustries();
                case "subcategory" -> listing.getSubcategory();
                default -> "ok";
            };
            if (value == null || (value instanceof String s && s.isBlank())) {
                missing.add(field);
            }
        }
        if (!missing.isEmpty()) {
            throw BusinessException.unprocessable("Complete the following before submitting for approval: " + String.join(", ", missing));
        }
    }

    private void notifyModerators(ServiceListing listing) {
        notificationClient.notify(NotificationRequest.toAdmins(Permission.LISTING_MODERATE.name(), "LISTING_SUBMITTED", "LISTING",
                "/admin/listings/" + listing.getId(), Map.of("title", listing.getTitle(), "seller", nz(listing.getSellerName()),
                        "reference", nz(listing.getReferenceNo()))));
    }

    static String folder(Long listingId) {
        return "listings/" + listingId;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
