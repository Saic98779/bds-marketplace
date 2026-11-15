package com.sidbi.bds.procurement.service;

import com.sidbi.bds.common.api.PageRequestFactory;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.client.CatalogClient;
import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.client.dto.CategoryRef;
import com.sidbi.bds.common.client.dto.ListingSummary;
import com.sidbi.bds.common.client.dto.NotificationRequest;
import com.sidbi.bds.common.client.dto.UserSummary;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.security.AuthUser;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.util.References;
import com.sidbi.bds.common.util.Search;
import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.ProcEnums.RfqStatus;
import com.sidbi.bds.procurement.domain.Quotation;
import com.sidbi.bds.procurement.domain.Rfq;
import com.sidbi.bds.procurement.domain.RfqInvitation;
import com.sidbi.bds.procurement.dto.RfqDtos;
import com.sidbi.bds.procurement.repository.QuotationRepository;
import com.sidbi.bds.procurement.repository.RfqInvitationRepository;
import com.sidbi.bds.procurement.repository.RfqRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RfqService {

    private static final int MAX_INVITES = 25;
    private static final int RECOMMENDED_LIMIT = 10;
    private static final DateTimeFormatter DEADLINE = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final RfqRepository rfqRepository;
    private final RfqInvitationRepository invitationRepository;
    private final QuotationRepository quotationRepository;
    private final CatalogClient catalogClient;
    private final NotificationClient notificationClient;
    private final DocumentService documentService;
    private final ActorGuard guard;
    private final ProcMapper mapper;

    public record RfqFilter(RfqStatus status, String q, Long categoryId, Long buyerId) {
    }

    @Transactional
    public RfqDtos.RfqDetail create(RfqDtos.RfqRequest request) {
        AuthUser buyer = guard.buyerActor(Permission.RFQ_CREATE);
        validate(request);
        Rfq rfq = new Rfq();
        rfq.setBuyerId(buyer.userId());
        rfq.setBuyerName(buyer.name());
        rfq.setStatus(RfqStatus.DRAFT);
        apply(rfq, request);
        Set<Long> sellerIds = new LinkedHashSet<>(request.sellerIds() == null ? List.of() : request.sellerIds());
        if (request.sourceListingId() != null) {
            ListingSummary listing = catalogClient.listing(request.sourceListingId());
            if (!listing.publiclyVisible()) {
                throw BusinessException.badRequest("The selected service is not currently available");
            }
            rfq.setSourceListingId(listing.id());
            rfq.setSourceListingTitle(listing.title());
            sellerIds.add(listing.sellerId());
            catalogClient.recordEnquiry(listing.id());
        }
        rfqRepository.save(rfq);
        rfq.setRfqNumber(References.of("RFQ", rfq.getId()));
        if (!sellerIds.isEmpty() && rfq.getDistributionMode() != ProcEnums.DistributionMode.ADMIN_ASSISTED) {
            addInvitations(rfq, sellerIds, "BUYER", false);
        }
        notificationClient.audit("RFQ_CREATED", "RFQ", rfq.getId(), rfq.getRfqNumber());
        if (Boolean.TRUE.equals(request.publish())) {
            publishInternal(rfq);
        }
        return buyerDetailOf(rfq);
    }

    @Transactional
    public RfqDtos.RfqDetail update(Long id, RfqDtos.RfqRequest request) {
        guard.buyer(Permission.RFQ_CREATE);
        Rfq rfq = ownedByBuyer(id);
        if (rfq.getStatus() != RfqStatus.DRAFT) {
            throw BusinessException.conflict("Only draft RFQs can be edited. Use extend deadline or invite providers for published RFQs.");
        }
        validate(request);
        apply(rfq, request);
        if (request.sellerIds() != null && rfq.getDistributionMode() == ProcEnums.DistributionMode.SELECTED) {
            rfq.getInvitations().removeIf(i -> !request.sellerIds().contains(i.getSellerId()));
            addInvitations(rfq, request.sellerIds(), "BUYER", false);
        }
        notificationClient.audit("RFQ_UPDATED", "RFQ", id, null);
        if (Boolean.TRUE.equals(request.publish())) {
            publishInternal(rfq);
        }
        return buyerDetailOf(rfq);
    }

    @Transactional
    public RfqDtos.RfqDetail publish(Long id) {
        guard.buyer(Permission.RFQ_CREATE);
        Rfq rfq = ownedByBuyer(id);
        if (rfq.getStatus() != RfqStatus.DRAFT) {
            throw BusinessException.conflict("RFQ has already been submitted");
        }
        publishInternal(rfq);
        return buyerDetailOf(rfq);
    }

    @Transactional
    public RfqDtos.RfqDetail invite(Long id, List<Long> sellerIds) {
        guard.buyer(Permission.RFQ_CREATE);
        Rfq rfq = ownedByBuyer(id);
        if (!RfqStatus.OPEN_FOR_QUOTES.contains(rfq.getStatus()) || deadlinePassed(rfq)) {
            throw BusinessException.conflict("Providers can be invited only while the RFQ is open for quotations");
        }
        addInvitations(rfq, sellerIds, "BUYER", true);
        return buyerDetailOf(rfq);
    }

    @Transactional
    public RfqDtos.RfqDetail extendDeadline(Long id, RfqDtos.ExtendDeadlineRequest request) {
        guard.buyer(Permission.RFQ_CREATE);
        Rfq rfq = ownedByBuyer(id);
        if (!RfqStatus.ACTIVE.contains(rfq.getStatus()) && rfq.getStatus() != RfqStatus.NO_RESPONSE) {
            throw BusinessException.conflict("Deadline can be extended only for active RFQs");
        }
        if (!request.submissionDeadline().isAfter(rfq.getSubmissionDeadline())) {
            throw BusinessException.badRequest("New deadline must be later than the current deadline");
        }
        rfq.setSubmissionDeadline(request.submissionDeadline());
        if (rfq.getStatus() == RfqStatus.NO_RESPONSE || rfq.getStatus() == RfqStatus.UNDER_EVALUATION) {
            rfq.setStatus(quotationRepository.countByRfqIdAndStatusIn(id, ProcEnums.QuotationStatus.VISIBLE_TO_BUYER) > 0
                    ? RfqStatus.RESPONSES_RECEIVED : RfqStatus.PUBLISHED);
        }
        rfq.getInvitations().forEach(i -> i.setReminderSent(false));
        notifySellers(rfq, rfq.getInvitations().stream().map(RfqInvitation::getSellerId).toList(), "RFQ_DEADLINE_EXTENDED");
        notificationClient.audit("RFQ_DEADLINE_EXTENDED", "RFQ", id, request.submissionDeadline().toString());
        return buyerDetailOf(rfq);
    }

    @Transactional
    public RfqDtos.RfqDetail cancel(Long id, String reason) {
        guard.buyer(null);
        Rfq rfq = ownedByBuyer(id);
        if (rfq.getStatus() == RfqStatus.PROVIDER_SELECTED || rfq.getStatus() == RfqStatus.CLOSED || rfq.getStatus() == RfqStatus.CANCELLED) {
            throw BusinessException.conflict("RFQ cannot be cancelled in its current status");
        }
        rfq.setStatus(RfqStatus.CANCELLED);
        rfq.setStatusRemarks(reason);
        rfq.setClosedAt(LocalDateTime.now());
        for (Quotation q : quotationRepository.findByRfqIdAndStatusIn(id, ProcEnums.QuotationStatus.LIVE)) {
            q.setStatus(ProcEnums.QuotationStatus.REJECTED);
            q.setDecisionAt(LocalDateTime.now());
            q.setDecisionRemarks("RFQ cancelled by purchaser: " + reason);
        }
        notifySellers(rfq, rfq.getInvitations().stream().map(RfqInvitation::getSellerId).toList(), "RFQ_CANCELLED");
        notificationClient.audit("RFQ_CANCELLED", "RFQ", id, reason);
        return buyerDetailOf(rfq);
    }

    @Transactional
    public RfqDtos.RfqDetail close(Long id, String remarks) {
        guard.buyer(null);
        Rfq rfq = ownedByBuyer(id);
        if (rfq.getStatus() == RfqStatus.CLOSED || rfq.getStatus() == RfqStatus.CANCELLED || rfq.getStatus() == RfqStatus.DRAFT) {
            throw BusinessException.conflict("RFQ cannot be closed in its current status");
        }
        if (rfq.getStatus() != RfqStatus.PROVIDER_SELECTED) {
            for (Quotation q : quotationRepository.findByRfqIdAndStatusIn(id, ProcEnums.QuotationStatus.LIVE)) {
                q.setStatus(ProcEnums.QuotationStatus.REJECTED);
                q.setDecisionAt(LocalDateTime.now());
                q.setDecisionRemarks("RFQ closed without selection");
            }
        }
        rfq.setStatus(RfqStatus.CLOSED);
        rfq.setStatusRemarks(remarks);
        rfq.setClosedAt(LocalDateTime.now());
        notificationClient.audit("RFQ_CLOSED", "RFQ", id, remarks);
        return buyerDetailOf(rfq);
    }

    @Transactional
    public RfqDtos.DocumentResponse addAttachment(Long id, String category, MultipartFile file) {
        Rfq rfq = ownedByBuyer(id);
        if (rfq.getStatus() == RfqStatus.CLOSED || rfq.getStatus() == RfqStatus.CANCELLED) {
            throw BusinessException.conflict("Attachments cannot be added to a closed RFQ");
        }
        return documentService.upload(ProcEnums.OwnerType.RFQ, id, category, file);
    }

    @Transactional(readOnly = true)
    public PageResponse<RfqDtos.RfqSummary> buyerList(RfqStatus status, String q, int page, int size) {
        Long buyerId = CurrentUser.scopeId();
        return list(new RfqFilter(status, q, null, buyerId), page, size, null);
    }

    @Transactional(readOnly = true)
    public RfqDtos.RfqDetail buyerDetail(Long id) {
        return buyerDetailOf(ownedByBuyer(id));
    }

    @Transactional(readOnly = true)
    public List<RfqDtos.RecommendedSeller> recommended(Long categoryId, Long subcategoryId) {
        List<Long> ids = catalogClient.sellersForCategory(categoryId, subcategoryId);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, UserSummary> summaries = guard.summaries(ids);
        return ids.stream()
                .map(summaries::get)
                .filter(s -> s != null && s.active() && s.permissions() != null && s.permissions().contains(Permission.RFQ_RESPOND.name()))
                .sorted((a, b) -> Double.compare(nz(b.averageRating()), nz(a.averageRating())))
                .limit(RECOMMENDED_LIMIT)
                .map(s -> new RfqDtos.RecommendedSeller(s.id(), s.displayName(), s.state(), s.district(), s.averageRating(), s.ratingCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<RfqDtos.RfqSummary> sellerInbox(RfqStatus status, ProcEnums.InvitationStatus invitationStatus, String q, int page, int size) {
        Long sellerId = CurrentUser.scopeId();
        Specification<Rfq> spec = filterSpec(new RfqFilter(status, q, null, null)).and((root, query, cb) -> {
            if (sellerId == null) {
                Predicate published = cb.notEqual(root.get("status"), RfqStatus.DRAFT);
                if (invitationStatus == null) {
                    return published;
                }
                Subquery<Long> withStatus = query.subquery(Long.class);
                Root<RfqInvitation> inv = withStatus.from(RfqInvitation.class);
                withStatus.select(inv.get("rfq").get("id")).where(cb.equal(inv.get("status"), invitationStatus));
                return cb.and(root.get("id").in(withStatus), published);
            }
            Subquery<Long> invited = query.subquery(Long.class);
            Root<RfqInvitation> inv = invited.from(RfqInvitation.class);
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.equal(inv.get("sellerId"), sellerId));
            if (invitationStatus != null) {
                conditions.add(cb.equal(inv.get("status"), invitationStatus));
            }
            invited.select(inv.get("rfq").get("id")).where(conditions.toArray(Predicate[]::new));
            return cb.and(root.get("id").in(invited), cb.notEqual(root.get("status"), RfqStatus.DRAFT));
        });
        Page<Rfq> result = rfqRepository.findAll(spec, PageRequestFactory.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt")));
        return PageResponse.of(result, r -> mapper.rfq(r, 0,
                sellerId == null ? null : invitationRepository.findByRfqIdAndSellerId(r.getId(), sellerId).orElse(null),
                sellerId == null ? null : quotationRepository.findByRfqIdAndSellerId(r.getId(), sellerId).orElse(null)));
    }

    @Transactional
    public RfqDtos.RfqDetail sellerDetail(Long id) {
        Long sellerId = CurrentUser.scopeId();
        Rfq rfq = rfq(id);
        if (sellerId == null) {
            return mapper.rfqDetail(mapper.rfq(rfq, 0, null, null), rfq, documentService.list(ProcEnums.OwnerType.RFQ, id), List.of());
        }
        RfqInvitation invitation = invitationRepository.findByRfqIdAndSellerId(id, sellerId)
                .orElseThrow(() -> BusinessException.notFound("RFQ not found"));
        if (rfq.getStatus() == RfqStatus.DRAFT) {
            throw BusinessException.notFound("RFQ not found");
        }
        if (invitation.getStatus() == ProcEnums.InvitationStatus.INVITED) {
            invitation.setStatus(ProcEnums.InvitationStatus.VIEWED);
            invitation.setViewedAt(LocalDateTime.now());
        }
        Quotation mine = quotationRepository.findByRfqIdAndSellerId(id, sellerId).orElse(null);
        RfqDtos.RfqSummary summary = mapper.rfq(rfq, 0, invitation, mine);
        return mapper.rfqDetail(summary, rfq, documentService.list(ProcEnums.OwnerType.RFQ, id), List.of());
    }

    @Transactional
    public RfqDtos.RfqDetail decline(Long id, String reason) {
        Long sellerId = guard.sellerActor(null).userId();
        Rfq rfq = rfq(id);
        RfqInvitation invitation = invitationRepository.findByRfqIdAndSellerId(id, sellerId)
                .orElseThrow(() -> BusinessException.notFound("RFQ not found"));
        if (invitation.getStatus() == ProcEnums.InvitationStatus.RESPONDED) {
            throw BusinessException.conflict("You have already submitted a quotation for this RFQ");
        }
        invitation.setStatus(ProcEnums.InvitationStatus.DECLINED);
        invitation.setDeclineReason(reason);
        invitation.setRespondedAt(LocalDateTime.now());
        notificationClient.notify(rfq.getBuyerId(), "RFQ_DECLINED", "RFQ", "/buyer/rfqs/" + id,
                Map.of("rfq", rfq.getRfqNumber(), "title", rfq.getTitle(), "seller", nzs(invitation.getSellerName()), "reason", reason));
        notificationClient.audit("RFQ_DECLINED", "RFQ", id, reason);
        return sellerDetail(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<RfqDtos.RfqSummary> adminList(RfqFilter filter, int page, int size) {
        guard.admin(Permission.RFQ_MONITOR);
        return list(filter, page, size, null);
    }

    @Transactional(readOnly = true)
    public RfqDtos.RfqDetail adminDetail(Long id) {
        guard.admin(Permission.RFQ_MONITOR);
        return buyerDetailOf(rfq(id));
    }

    @Transactional
    public RfqDtos.RfqDetail adminInvite(Long id, List<Long> sellerIds) {
        guard.admin(Permission.RFQ_MONITOR);
        Rfq rfq = rfq(id);
        if (rfq.getStatus() != RfqStatus.SUBMITTED && !RfqStatus.OPEN_FOR_QUOTES.contains(rfq.getStatus())) {
            throw BusinessException.conflict("Providers can be matched only for submitted or open RFQs");
        }
        if (deadlinePassed(rfq)) {
            throw BusinessException.conflict("RFQ deadline has passed. Ask the purchaser to extend the deadline.");
        }
        boolean wasSubmitted = rfq.getStatus() == RfqStatus.SUBMITTED;
        addInvitations(rfq, sellerIds, "ADMIN", !wasSubmitted);
        if (wasSubmitted) {
            rfq.setStatus(RfqStatus.PUBLISHED);
            rfq.setPublishedAt(LocalDateTime.now());
            notifySellers(rfq, rfq.getInvitations().stream().map(RfqInvitation::getSellerId).toList(), "RFQ_INVITATION");
            notificationClient.notify(rfq.getBuyerId(), "RFQ_PUBLISHED", "RFQ", "/buyer/rfqs/" + id,
                    Map.of("rfq", rfq.getRfqNumber(), "title", rfq.getTitle(), "count", String.valueOf(rfq.getInvitations().size())));
        }
        notificationClient.audit("RFQ_ADMIN_MATCHED", "RFQ", id, sellerIds.toString());
        return buyerDetailOf(rfq);
    }

    PageResponse<RfqDtos.RfqSummary> list(RfqFilter filter, int page, int size, Long sellerId) {
        Page<Rfq> result = rfqRepository.findAll(filterSpec(filter), PageRequestFactory.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<Long, Long> counts = quotationCounts(result.getContent().stream().map(Rfq::getId).toList());
        return PageResponse.of(result, r -> mapper.rfq(r, counts.getOrDefault(r.getId(), 0L), null, null));
    }

    Map<Long, Long> quotationCounts(Collection<Long> rfqIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (rfqIds.isEmpty()) {
            return counts;
        }
        for (Object[] row : quotationRepository.countByRfqIds(rfqIds, ProcEnums.QuotationStatus.VISIBLE_TO_BUYER)) {
            counts.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    RfqDtos.RfqDetail buyerDetailOf(Rfq rfq) {
        long count = quotationRepository.countByRfqIdAndStatusIn(rfq.getId(), ProcEnums.QuotationStatus.VISIBLE_TO_BUYER);
        Map<Long, UserSummary> sellers = guard.summaries(rfq.getInvitations().stream().map(RfqInvitation::getSellerId).toList());
        List<RfqDtos.InvitationResponse> invitations = rfq.getInvitations().stream()
                .map(i -> mapper.invitation(i, sellers.get(i.getSellerId())))
                .toList();
        return mapper.rfqDetail(mapper.rfq(rfq, count, null, null), rfq, documentService.list(ProcEnums.OwnerType.RFQ, rfq.getId()), invitations);
    }

    Rfq rfq(Long id) {
        return rfqRepository.findById(id).orElseThrow(() -> BusinessException.notFound("RFQ not found"));
    }

    Rfq ownedByBuyer(Long id) {
        Rfq rfq = rfq(id);
        if (!CurrentUser.owns(rfq.getBuyerId())) {
            throw BusinessException.notFound("RFQ not found");
        }
        return rfq;
    }

    private void publishInternal(Rfq rfq) {
        if (deadlinePassed(rfq)) {
            throw BusinessException.badRequest("Submission deadline must be in the future");
        }
        if (rfq.getDistributionMode() == ProcEnums.DistributionMode.ADMIN_ASSISTED) {
            rfq.setStatus(RfqStatus.SUBMITTED);
            notificationClient.notify(NotificationRequest.toAdmins(Permission.RFQ_MONITOR.name(), "RFQ_ADMIN_ASSIST", "RFQ",
                    "/admin/rfqs/" + rfq.getId(), Map.of("rfq", rfq.getRfqNumber(), "title", rfq.getTitle(), "buyer", nzs(rfq.getBuyerName()))));
            notificationClient.notify(rfq.getBuyerId(), "RFQ_SUBMITTED", "RFQ", "/buyer/rfqs/" + rfq.getId(),
                    Map.of("rfq", rfq.getRfqNumber(), "title", rfq.getTitle()));
            notificationClient.audit("RFQ_SUBMITTED", "RFQ", rfq.getId(), "admin-assisted");
            return;
        }
        if (rfq.getDistributionMode() == ProcEnums.DistributionMode.RECOMMENDED) {
            List<Long> recommended = recommended(rfq.getCategoryId(), rfq.getSubcategoryId()).stream().map(RfqDtos.RecommendedSeller::sellerId).toList();
            if (recommended.isEmpty() && rfq.getSubcategoryId() != null) {
                recommended = recommended(rfq.getCategoryId(), null).stream().map(RfqDtos.RecommendedSeller::sellerId).toList();
            }
            addInvitations(rfq, recommended, "SYSTEM", false);
        }
        if (rfq.getInvitations().isEmpty()) {
            throw BusinessException.unprocessable("Select at least one verified provider, or choose administrator-assisted matching");
        }
        rfq.setStatus(RfqStatus.PUBLISHED);
        rfq.setPublishedAt(LocalDateTime.now());
        notifySellers(rfq, rfq.getInvitations().stream().map(RfqInvitation::getSellerId).toList(), "RFQ_INVITATION");
        notificationClient.notify(rfq.getBuyerId(), "RFQ_PUBLISHED", "RFQ", "/buyer/rfqs/" + rfq.getId(),
                Map.of("rfq", rfq.getRfqNumber(), "title", rfq.getTitle(), "count", String.valueOf(rfq.getInvitations().size())));
        notificationClient.audit("RFQ_PUBLISHED", "RFQ", rfq.getId(), rfq.getInvitations().size() + " providers invited");
    }

    private void addInvitations(Rfq rfq, Collection<Long> sellerIds, String invitedBy, boolean notify) {
        if (sellerIds == null || sellerIds.isEmpty()) {
            return;
        }
        Set<Long> existing = new LinkedHashSet<>(rfq.getInvitations().stream().map(RfqInvitation::getSellerId).toList());
        List<Long> fresh = sellerIds.stream().filter(id -> !existing.contains(id)).distinct().toList();
        if (fresh.isEmpty()) {
            return;
        }
        if (existing.size() + fresh.size() > MAX_INVITES) {
            throw BusinessException.badRequest("An RFQ can be shared with at most " + MAX_INVITES + " providers");
        }
        Map<Long, UserSummary> summaries = guard.summaries(fresh);
        if (summaries.isEmpty()) {
            throw BusinessException.conflict("Provider details could not be verified. Please try again.");
        }
        List<Long> added = new ArrayList<>();
        for (Long sellerId : fresh) {
            UserSummary s = summaries.get(sellerId);
            if (s == null || !"SELLER".equals(s.role())) {
                throw BusinessException.badRequest("Provider " + sellerId + " was not found");
            }
            if (!s.active() || s.permissions() == null || !s.permissions().contains(Permission.RFQ_RESPOND.name())) {
                throw BusinessException.badRequest(s.displayName() + " is not an approved and active provider");
            }
            RfqInvitation invitation = new RfqInvitation();
            invitation.setRfq(rfq);
            invitation.setSellerId(sellerId);
            invitation.setSellerName(s.displayName());
            invitation.setInvitedBy(invitedBy);
            rfq.getInvitations().add(invitation);
            added.add(sellerId);
        }
        if (notify && !added.isEmpty()) {
            notifySellers(rfq, added, "RFQ_INVITATION");
            notificationClient.audit("RFQ_PROVIDERS_INVITED", "RFQ", rfq.getId(), added.toString());
        }
    }

    private void notifySellers(Rfq rfq, List<Long> sellerIds, String template) {
        if (sellerIds.isEmpty()) {
            return;
        }
        notificationClient.notify(NotificationRequest.to(sellerIds, template, "RFQ", "/seller/rfqs/" + rfq.getId(),
                Map.of("rfq", rfq.getRfqNumber(), "title", rfq.getTitle(), "buyer", nzs(rfq.getBuyerName()),
                        "deadline", rfq.getSubmissionDeadline().format(DEADLINE), "reason", nzs(rfq.getStatusRemarks()))));
    }

    private void validate(RfqDtos.RfqRequest request) {
        if (request.budgetMin() != null && request.budgetMax() != null && request.budgetMin().compareTo(request.budgetMax()) > 0) {
            throw BusinessException.badRequest("Minimum budget cannot exceed maximum budget");
        }
        if (request.submissionDeadline().isAfter(LocalDateTime.now().plusDays(180))) {
            throw BusinessException.badRequest("Submission deadline cannot be more than 180 days away");
        }
        if (request.submissionDeadline().isBefore(LocalDateTime.now().plusHours(1))) {
            throw BusinessException.badRequest("Allow at least one hour for providers to respond");
        }
        if (request.distributionMode() == ProcEnums.DistributionMode.SELECTED
                && (request.sellerIds() == null || request.sellerIds().isEmpty()) && request.sourceListingId() == null
                && Boolean.TRUE.equals(request.publish())) {
            throw BusinessException.badRequest("Select at least one provider to share this RFQ with");
        }
    }

    private void apply(Rfq rfq, RfqDtos.RfqRequest request) {
        CategoryRef category = catalogClient.resolveCategory(request.categoryId(), request.subcategoryId());
        rfq.setTitle(request.title().trim());
        rfq.setCategoryId(category.categoryId());
        rfq.setCategoryName(category.categoryName());
        rfq.setSubcategoryId(category.subcategoryId());
        rfq.setSubcategoryName(category.subcategoryName());
        rfq.setDescription(request.description());
        rfq.setScopeOfWork(request.scopeOfWork());
        rfq.setDeliverables(request.deliverables());
        rfq.setIndustry(request.industry());
        rfq.setProjectState(request.projectState());
        rfq.setProjectDistrict(request.projectDistrict());
        rfq.setDeliveryMode(request.deliveryMode() == null || request.deliveryMode().isBlank() ? null : request.deliveryMode());
        rfq.setBudgetMin(request.budgetMin());
        rfq.setBudgetMax(request.budgetMax());
        rfq.setExpectedDuration(request.expectedDuration());
        rfq.setRequiredExperience(request.requiredExperience());
        rfq.setExpectedStartDate(request.expectedStartDate());
        rfq.setSubmissionDeadline(request.submissionDeadline());
        rfq.setDistributionMode(request.distributionMode());
    }

    static boolean deadlinePassed(Rfq rfq) {
        return rfq.getSubmissionDeadline().isBefore(LocalDateTime.now());
    }

    private static Specification<Rfq> filterSpec(RfqFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.categoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), filter.categoryId()));
            }
            if (filter.buyerId() != null) {
                predicates.add(cb.equal(root.get("buyerId"), filter.buyerId()));
            }
            if (filter.q() != null && !filter.q().isBlank()) {
                String like = Search.contains(filter.q());
                predicates.add(cb.or(cb.like(cb.lower(root.get("title")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("rfqNumber")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("categoryName")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("buyerName")), like, Search.ESCAPE)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static double nz(Double value) {
        return value == null ? 0 : value;
    }

    private static String nzs(String value) {
        return value == null ? "" : value;
    }
}
