package com.sidbi.bds.procurement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sidbi.bds.common.api.PageRequestFactory;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.client.dto.NotificationRequest;
import com.sidbi.bds.common.client.dto.UserSummary;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.security.AuthUser;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.util.References;
import com.sidbi.bds.common.util.Search;
import com.sidbi.bds.procurement.domain.Engagement;
import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.ProcEnums.QuotationStatus;
import com.sidbi.bds.procurement.domain.ProcEnums.RfqStatus;
import com.sidbi.bds.procurement.domain.Quotation;
import com.sidbi.bds.procurement.domain.QuotationItem;
import com.sidbi.bds.procurement.domain.QuotationVersion;
import com.sidbi.bds.procurement.domain.Rfq;
import com.sidbi.bds.procurement.domain.RfqInvitation;
import com.sidbi.bds.procurement.dto.EngagementDtos;
import com.sidbi.bds.procurement.dto.QuotationDtos;
import com.sidbi.bds.procurement.dto.RfqDtos;
import com.sidbi.bds.procurement.repository.EngagementRepository;
import com.sidbi.bds.procurement.repository.QuotationRepository;
import com.sidbi.bds.procurement.repository.QuotationVersionRepository;
import com.sidbi.bds.procurement.repository.RfqInvitationRepository;
import com.sidbi.bds.procurement.repository.RfqRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuotationService {

    private static final Set<QuotationStatus> REVISABLE = EnumSet.of(QuotationStatus.SUBMITTED, QuotationStatus.UNDER_REVIEW,
            QuotationStatus.CLARIFICATION_REQUESTED, QuotationStatus.REVISED);
    private static final Set<QuotationStatus> ACCEPTABLE = EnumSet.of(QuotationStatus.SUBMITTED, QuotationStatus.UNDER_REVIEW,
            QuotationStatus.REVISED, QuotationStatus.SHORTLISTED, QuotationStatus.CLARIFICATION_REQUESTED);

    private final QuotationRepository quotationRepository;
    private final QuotationVersionRepository versionRepository;
    private final RfqRepository rfqRepository;
    private final RfqInvitationRepository invitationRepository;
    private final EngagementRepository engagementRepository;
    private final EngagementService engagementService;
    private final QueryService queryService;
    private final DocumentService documentService;
    private final QuotationPdfService pdfService;
    private final NotificationClient notificationClient;
    private final ActorGuard guard;
    private final ProcMapper mapper;
    private final ObjectMapper objectMapper;

    public record QuotationFilter(QuotationStatus status, String q, Long sellerId, Long buyerId, Long rfqId) {
    }

    @Transactional
    public QuotationDtos.QuotationResponse create(Long rfqId, QuotationDtos.QuotationRequest request) {
        AuthUser seller = guard.sellerActor(Permission.RFQ_RESPOND);
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow(() -> BusinessException.notFound("RFQ not found"));
        RfqInvitation invitation = invitationRepository.findByRfqIdAndSellerId(rfqId, seller.userId())
                .orElseThrow(() -> BusinessException.forbidden("Only invited providers can respond to this RFQ"));
        if (invitation.getStatus() == ProcEnums.InvitationStatus.DECLINED) {
            throw BusinessException.conflict("You have declined this RFQ");
        }
        assertOpen(rfq);
        if (quotationRepository.findByRfqIdAndSellerId(rfqId, seller.userId()).isPresent()) {
            throw BusinessException.conflict("A quotation already exists for this RFQ. Edit or revise the existing quotation.");
        }
        Quotation quotation = new Quotation();
        quotation.setRfq(rfq);
        quotation.setSellerId(seller.userId());
        quotation.setSellerName(seller.name());
        quotation.setBuyerId(rfq.getBuyerId());
        quotation.setStatus(QuotationStatus.DRAFT);
        quotation.setVersionNo(0);
        apply(quotation, request);
        quotationRepository.save(quotation);
        quotation.setQuotationNumber(References.of("QTN", quotation.getId()));
        notificationClient.audit("QUOTATION_DRAFTED", "QUOTATION", quotation.getId(), rfq.getRfqNumber());
        if (Boolean.TRUE.equals(request.submit())) {
            submitInternal(quotation, invitation, request.changeSummary());
        }
        return sellerView(quotation);
    }

    @Transactional
    public QuotationDtos.QuotationResponse update(Long id, QuotationDtos.QuotationRequest request) {
        guard.seller(Permission.RFQ_RESPOND);
        Quotation quotation = ownedBySeller(id);
        Rfq rfq = quotation.getRfq();
        if (quotation.getStatus() == QuotationStatus.DRAFT) {
            apply(quotation, request);
            if (Boolean.TRUE.equals(request.submit())) {
                assertOpen(rfq);
                submitInternal(quotation, invitationRepository.findByRfqIdAndSellerId(rfq.getId(), quotation.getSellerId()).orElseThrow(),
                        request.changeSummary());
            }
            return sellerView(quotation);
        }
        if (!REVISABLE.contains(quotation.getStatus())) {
            throw BusinessException.conflict("This quotation can no longer be revised");
        }
        boolean clarification = quotation.getStatus() == QuotationStatus.CLARIFICATION_REQUESTED;
        if (!clarification && (RfqService.deadlinePassed(rfq) || !RfqStatus.EVALUABLE.contains(rfq.getStatus()))) {
            throw BusinessException.conflict("Revisions are permitted only before the deadline or when clarification is requested");
        }
        if (request.changeSummary() == null || request.changeSummary().isBlank()) {
            throw BusinessException.badRequest("Describe what changed in this revision");
        }
        apply(quotation, request);
        quotation.setVersionNo(quotation.getVersionNo() + 1);
        quotation.setStatus(clarification ? QuotationStatus.REVISED : QuotationStatus.SUBMITTED);
        quotation.setSubmittedAt(LocalDateTime.now());
        snapshot(quotation, request.changeSummary());
        notificationClient.notify(quotation.getBuyerId(), "QUOTATION_REVISED", "QUOTATION", "/buyer/quotations/" + id,
                Map.of("quotation", quotation.getQuotationNumber(), "rfq", rfq.getRfqNumber(), "seller", nz(quotation.getSellerName()),
                        "version", String.valueOf(quotation.getVersionNo())));
        notificationClient.audit("QUOTATION_REVISED", "QUOTATION", id, "v" + quotation.getVersionNo() + ": " + request.changeSummary());
        return sellerView(quotation);
    }

    @Transactional
    public QuotationDtos.QuotationResponse submit(Long id) {
        guard.seller(Permission.RFQ_RESPOND);
        Quotation quotation = ownedBySeller(id);
        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw BusinessException.conflict("Quotation has already been submitted");
        }
        assertOpen(quotation.getRfq());
        submitInternal(quotation, invitationRepository.findByRfqIdAndSellerId(quotation.getRfq().getId(), quotation.getSellerId()).orElseThrow(), null);
        return sellerView(quotation);
    }

    @Transactional
    public QuotationDtos.QuotationResponse withdraw(Long id, String reason) {
        guard.seller(null);
        Quotation quotation = ownedBySeller(id);
        if (quotation.getStatus() == QuotationStatus.ACCEPTED || quotation.getStatus() == QuotationStatus.WITHDRAWN
                || quotation.getStatus() == QuotationStatus.REJECTED) {
            throw BusinessException.conflict("Quotation cannot be withdrawn in its current status");
        }
        boolean wasDraft = quotation.getStatus() == QuotationStatus.DRAFT;
        quotation.setStatus(QuotationStatus.WITHDRAWN);
        quotation.setDecisionRemarks(reason);
        quotation.setDecisionAt(LocalDateTime.now());
        if (!wasDraft) {
            notificationClient.notify(quotation.getBuyerId(), "QUOTATION_WITHDRAWN", "QUOTATION", "/buyer/rfqs/" + quotation.getRfq().getId(),
                    Map.of("quotation", quotation.getQuotationNumber(), "rfq", quotation.getRfq().getRfqNumber(),
                            "seller", nz(quotation.getSellerName()), "reason", nz(reason)));
        }
        notificationClient.audit("QUOTATION_WITHDRAWN", "QUOTATION", id, reason);
        return sellerView(quotation);
    }

    @Transactional(readOnly = true)
    public PageResponse<QuotationDtos.QuotationResponse> sellerList(QuotationStatus status, String q, int page, int size) {
        return list(new QuotationFilter(status, q, CurrentUser.scopeId(), null, null), page, size, true);
    }

    @Transactional(readOnly = true)
    public QuotationDtos.QuotationResponse sellerDetail(Long id) {
        return sellerView(ownedBySeller(id));
    }

    @Transactional(readOnly = true)
    public List<QuotationDtos.VersionResponse> versions(Long id) {
        AuthUser user = CurrentUser.get();
        Quotation quotation = quotation(id);
        boolean allowed = (user.isSeller() && quotation.getSellerId().equals(user.userId()))
                || (user.isBuyer() && quotation.getBuyerId().equals(user.userId()) && quotation.getStatus() != QuotationStatus.DRAFT)
                || (user.isAdmin() && user.has(Permission.RFQ_MONITOR));
        if (!allowed) {
            throw BusinessException.notFound("Quotation not found");
        }
        return versionRepository.findByQuotationIdOrderByVersionNoDesc(id).stream()
                .map(v -> new QuotationDtos.VersionResponse(v.getId(), v.getVersionNo(), v.getTotalAmount(), v.getChangeSummary(),
                        v.getSnapshot(), v.getCreatedAt()))
                .toList();
    }

    @Transactional
    public RfqDtos.DocumentResponse addAttachment(Long id, String category, MultipartFile file) {
        Quotation quotation = ownedBySeller(id);
        if (quotation.getStatus() != QuotationStatus.DRAFT && quotation.getStatus() != QuotationStatus.CLARIFICATION_REQUESTED) {
            throw BusinessException.conflict("Attachments can be added to draft quotations or in response to a clarification");
        }
        return documentService.upload(ProcEnums.OwnerType.QUOTATION, id, category, file);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> pdf(Long id) {
        AuthUser user = CurrentUser.get();
        Quotation quotation = quotation(id);
        boolean allowed = (user.isSeller() && quotation.getSellerId().equals(user.userId()))
                || (user.isBuyer() && quotation.getBuyerId().equals(user.userId()) && quotation.getStatus() != QuotationStatus.DRAFT)
                || (user.isAdmin() && user.has(Permission.RFQ_MONITOR));
        if (!allowed) {
            throw BusinessException.notFound("Quotation not found");
        }
        byte[] content = pdfService.render(quotation);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(quotation.getQuotationNumber() + "-v" + Math.max(1, quotation.getVersionNo()) + ".pdf").build().toString())
                .body(content);
    }

    @Transactional(readOnly = true)
    public List<QuotationDtos.QuotationResponse> forRfq(Long rfqId) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow(() -> BusinessException.notFound("RFQ not found"));
        AuthUser user = CurrentUser.get();
        if (user.isBuyer() && !rfq.getBuyerId().equals(user.userId())) {
            throw BusinessException.notFound("RFQ not found");
        }
        if (user.isSeller()) {
            throw BusinessException.forbidden("Providers cannot view other quotations");
        }
        List<Quotation> quotations = quotationRepository.findByRfqIdAndStatusInOrderByTotalAmountAsc(rfqId, QuotationStatus.VISIBLE_TO_BUYER);
        Map<Long, UserSummary> sellers = guard.summaries(quotations.stream().map(Quotation::getSellerId).toList());
        return quotations.stream().map(q -> view(q, sellers.get(q.getSellerId()), false)).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<QuotationDtos.QuotationResponse> buyerList(QuotationStatus status, String q, Long rfqId, int page, int size) {
        return list(new QuotationFilter(status, q, null, CurrentUser.scopeId(), rfqId), page, size, false);
    }

    @Transactional
    public QuotationDtos.QuotationResponse buyerDetail(Long id) {
        Quotation quotation = ownedByBuyer(id);
        if (quotation.getStatus() == QuotationStatus.SUBMITTED || quotation.getStatus() == QuotationStatus.REVISED) {
            quotation.setStatus(QuotationStatus.UNDER_REVIEW);
            Rfq rfq = quotation.getRfq();
            if (rfq.getStatus() == RfqStatus.RESPONSES_RECEIVED || rfq.getStatus() == RfqStatus.PUBLISHED) {
                if (RfqService.deadlinePassed(rfq)) {
                    rfq.setStatus(RfqStatus.UNDER_EVALUATION);
                }
            }
        }
        return view(quotation, safeSummary(quotation.getSellerId()), false);
    }

    @Transactional
    public QuotationDtos.QuotationResponse shortlist(Long id, boolean shortlisted) {
        guard.buyer(Permission.QUOTATION_EVALUATE);
        Quotation quotation = ownedByBuyer(id);
        if (!QuotationStatus.LIVE.contains(quotation.getStatus())) {
            throw BusinessException.conflict("Only active quotations can be shortlisted");
        }
        quotation.setShortlisted(shortlisted);
        quotation.setStatus(shortlisted ? QuotationStatus.SHORTLISTED : QuotationStatus.UNDER_REVIEW);
        if (shortlisted) {
            notificationClient.notify(quotation.getSellerId(), "QUOTATION_SHORTLISTED", "QUOTATION", "/seller/quotations/" + id,
                    Map.of("quotation", quotation.getQuotationNumber(), "rfq", quotation.getRfq().getRfqNumber(), "title", quotation.getRfq().getTitle()));
        }
        notificationClient.audit(shortlisted ? "QUOTATION_SHORTLISTED" : "QUOTATION_UNSHORTLISTED", "QUOTATION", id, null);
        return view(quotation, safeSummary(quotation.getSellerId()), false);
    }

    @Transactional
    public QuotationDtos.QuotationResponse reject(Long id, String remarks) {
        guard.buyer(Permission.QUOTATION_EVALUATE);
        Quotation quotation = ownedByBuyer(id);
        if (!QuotationStatus.LIVE.contains(quotation.getStatus())) {
            throw BusinessException.conflict("Only active quotations can be rejected");
        }
        quotation.setStatus(QuotationStatus.REJECTED);
        quotation.setShortlisted(false);
        quotation.setDecisionAt(LocalDateTime.now());
        quotation.setDecisionRemarks(remarks);
        notificationClient.notify(quotation.getSellerId(), "QUOTATION_REJECTED", "QUOTATION", "/seller/quotations/" + id,
                Map.of("quotation", quotation.getQuotationNumber(), "rfq", quotation.getRfq().getRfqNumber(),
                        "title", quotation.getRfq().getTitle(), "remarks", nz(remarks)));
        notificationClient.audit("QUOTATION_REJECTED", "QUOTATION", id, remarks);
        return view(quotation, safeSummary(quotation.getSellerId()), false);
    }

    @Transactional
    public QuotationDtos.QuotationResponse requestClarification(Long id, QuotationDtos.ClarificationRequest request) {
        AuthUser buyer = guard.buyer(Permission.QUOTATION_EVALUATE);
        Quotation quotation = ownedByBuyer(id);
        if (!QuotationStatus.LIVE.contains(quotation.getStatus())) {
            throw BusinessException.conflict("Clarification can be requested only for active quotations");
        }
        quotation.setStatus(QuotationStatus.CLARIFICATION_REQUESTED);
        queryService.openContextTicket(ProcEnums.ContextType.QUOTATION, quotation.getId(), quotation.getQuotationNumber(),
                quotation.getRfq().getId(), null, quotation.getSellerId(), quotation.getSellerName(), quotation.getBuyerId(),
                quotation.getRfq().getBuyerName(), buyer, request.subject(), request.message(), ProcEnums.Priority.MEDIUM);
        notificationClient.audit("QUOTATION_CLARIFICATION_REQUESTED", "QUOTATION", id, request.subject());
        return view(quotation, safeSummary(quotation.getSellerId()), false);
    }

    @Transactional
    public EngagementDtos.EngagementDetail accept(Long id, QuotationDtos.AcceptRequest request) {
        AuthUser buyer = guard.buyer(Permission.QUOTATION_EVALUATE);
        Quotation quotation = ownedByBuyer(id);
        Rfq rfq = quotation.getRfq();
        if (!ACCEPTABLE.contains(quotation.getStatus())) {
            throw BusinessException.conflict("This quotation cannot be accepted in its current status");
        }
        if (!RfqStatus.EVALUABLE.contains(rfq.getStatus())) {
            throw BusinessException.conflict("RFQ is not open for provider selection");
        }
        if (quotation.getValidUntil() != null && quotation.getValidUntil().isBefore(LocalDate.now())) {
            throw BusinessException.conflict("Quotation validity has expired. Request a revised quotation.");
        }
        UserSummary seller = guard.summary(quotation.getSellerId());
        if (!seller.active()) {
            throw BusinessException.conflict("The provider account is not active. Selection is not permitted.");
        }
        LocalDateTime now = LocalDateTime.now();
        quotation.setStatus(QuotationStatus.ACCEPTED);
        quotation.setDecisionAt(now);
        quotation.setDecisionRemarks(request.remarks());
        rfq.setStatus(RfqStatus.PROVIDER_SELECTED);
        rfq.setSelectedQuotationId(quotation.getId());
        rfq.setStatusRemarks(request.remarks());
        List<Long> others = new ArrayList<>();
        for (Quotation other : quotationRepository.findByRfqIdAndStatusIn(rfq.getId(), QuotationStatus.LIVE)) {
            if (!other.getId().equals(quotation.getId())) {
                other.setStatus(QuotationStatus.REJECTED);
                other.setShortlisted(false);
                other.setDecisionAt(now);
                other.setDecisionRemarks("Another provider has been selected for this requirement");
                others.add(other.getSellerId());
            }
        }
        Engagement engagement = engagementService.createFromSelection(quotation, buyer, request);
        notificationClient.notify(quotation.getSellerId(), "QUOTATION_ACCEPTED", "QUOTATION", "/seller/engagements/" + engagement.getId(),
                Map.of("quotation", quotation.getQuotationNumber(), "rfq", rfq.getRfqNumber(), "title", rfq.getTitle(),
                        "engagement", engagement.getEngagementNumber(), "buyer", nz(rfq.getBuyerName())));
        if (!others.isEmpty()) {
            notificationClient.notify(NotificationRequest.to(others, "QUOTATION_REJECTED", "QUOTATION",
                    "/seller/quotations", Map.of("rfq", rfq.getRfqNumber(), "title", rfq.getTitle(), "quotation", "",
                            "remarks", "Another provider has been selected for this requirement")));
        }
        notificationClient.audit("PROVIDER_SELECTED", "RFQ", rfq.getId(), quotation.getQuotationNumber() + " - " + request.remarks());
        return engagementService.detail(engagement.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<QuotationDtos.QuotationResponse> adminList(QuotationFilter filter, int page, int size) {
        guard.admin(Permission.RFQ_MONITOR);
        return list(filter, page, size, false);
    }

    @Transactional(readOnly = true)
    public QuotationDtos.QuotationResponse adminDetail(Long id) {
        guard.admin(Permission.RFQ_MONITOR);
        Quotation quotation = quotation(id);
        if (quotation.getStatus() == QuotationStatus.DRAFT) {
            throw BusinessException.notFound("Quotation not found");
        }
        return view(quotation, safeSummary(quotation.getSellerId()), false);
    }

    private PageResponse<QuotationDtos.QuotationResponse> list(QuotationFilter filter, int page, int size, boolean sellerView) {
        Specification<Quotation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.sellerId() != null) {
                predicates.add(cb.equal(root.get("sellerId"), filter.sellerId()));
            }
            if (filter.buyerId() != null) {
                predicates.add(cb.equal(root.get("buyerId"), filter.buyerId()));
            }
            if (!sellerView) {
                predicates.add(cb.notEqual(root.get("status"), QuotationStatus.DRAFT));
                predicates.add(cb.notEqual(root.get("status"), QuotationStatus.WITHDRAWN));
            }
            if (filter.rfqId() != null) {
                predicates.add(cb.equal(root.get("rfq").get("id"), filter.rfqId()));
            }
            if (filter.q() != null && !filter.q().isBlank()) {
                String like = Search.contains(filter.q());
                predicates.add(cb.or(cb.like(cb.lower(root.get("quotationNumber")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("sellerName")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("rfq").get("title")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("rfq").get("rfqNumber")), like, Search.ESCAPE)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<Quotation> result = quotationRepository.findAll(spec, PageRequestFactory.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        Map<Long, UserSummary> sellers = sellerView ? Map.of() : guard.summaries(result.getContent().stream().map(Quotation::getSellerId).toList());
        return PageResponse.of(result, q -> sellerView ? sellerViewLight(q) : view(q, sellers.get(q.getSellerId()), false));
    }

    private void submitInternal(Quotation quotation, RfqInvitation invitation, String changeSummary) {
        validateForSubmission(quotation);
        Rfq rfq = quotation.getRfq();
        quotation.setStatus(QuotationStatus.SUBMITTED);
        quotation.setVersionNo(1);
        quotation.setSubmittedAt(LocalDateTime.now());
        snapshot(quotation, changeSummary == null || changeSummary.isBlank() ? "Initial submission" : changeSummary);
        invitation.setStatus(ProcEnums.InvitationStatus.RESPONDED);
        invitation.setRespondedAt(LocalDateTime.now());
        if (rfq.getStatus() == RfqStatus.PUBLISHED) {
            rfq.setStatus(RfqStatus.RESPONSES_RECEIVED);
        }
        notificationClient.notify(rfq.getBuyerId(), "QUOTATION_RECEIVED", "QUOTATION", "/buyer/rfqs/" + rfq.getId(),
                Map.of("quotation", quotation.getQuotationNumber(), "rfq", rfq.getRfqNumber(), "title", rfq.getTitle(),
                        "seller", nz(quotation.getSellerName())));
        notificationClient.audit("QUOTATION_SUBMITTED", "QUOTATION", quotation.getId(), rfq.getRfqNumber());
    }

    private void validateForSubmission(Quotation quotation) {
        if (quotation.getTotalAmount() == null || quotation.getTotalAmount().signum() <= 0) {
            throw BusinessException.unprocessable("Enter the professional fee or line items before submitting");
        }
        if (quotation.getValidUntil() == null || quotation.getValidUntil().isBefore(LocalDate.now())) {
            throw BusinessException.unprocessable("Quotation validity must be today or later");
        }
        if (!quotation.isScopeAccepted() && (quotation.getScopeDeviations() == null || quotation.getScopeDeviations().isBlank())) {
            throw BusinessException.unprocessable("Describe deviations from the requested scope");
        }
    }

    private void assertOpen(Rfq rfq) {
        if (!RfqStatus.OPEN_FOR_QUOTES.contains(rfq.getStatus())) {
            throw BusinessException.conflict("RFQ is not accepting quotations");
        }
        if (RfqService.deadlinePassed(rfq)) {
            throw BusinessException.conflict("The submission deadline for this RFQ has passed");
        }
    }

    private void apply(Quotation quotation, QuotationDtos.QuotationRequest request) {
        quotation.setTechnicalProposal(request.technicalProposal());
        quotation.setMethodology(request.methodology());
        quotation.setScopeAccepted(request.scopeAccepted() == null || request.scopeAccepted());
        quotation.setScopeDeviations(request.scopeDeviations());
        quotation.setDeliverables(request.deliverables());
        quotation.setTimelineDays(request.timelineDays());
        quotation.setTeamComposition(request.teamComposition());
        quotation.setRelevantExperience(request.relevantExperience());
        quotation.setPaymentMilestones(request.paymentMilestones());
        quotation.setValidUntil(request.validUntil());
        quotation.getItems().clear();
        BigDecimal fee = request.professionalFee();
        if (request.items() != null && !request.items().isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            int order = 0;
            for (QuotationDtos.ItemRequest i : request.items()) {
                QuotationItem item = new QuotationItem();
                item.setQuotation(quotation);
                item.setDescription(i.description());
                item.setQuantity(i.quantity());
                item.setUnit(i.unit());
                item.setUnitPrice(i.unitPrice());
                item.setAmount(i.quantity().multiply(i.unitPrice()).setScale(2, RoundingMode.HALF_UP));
                item.setDisplayOrder(order++);
                quotation.getItems().add(item);
                sum = sum.add(item.getAmount());
            }
            fee = sum;
        }
        BigDecimal professionalFee = fee == null ? BigDecimal.ZERO : fee.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxPercent = request.taxPercent() == null ? new BigDecimal("18") : request.taxPercent();
        BigDecimal tax = professionalFee.multiply(taxPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal other = request.otherExpenses() == null ? BigDecimal.ZERO : request.otherExpenses().setScale(2, RoundingMode.HALF_UP);
        quotation.setProfessionalFee(professionalFee);
        quotation.setTaxPercent(taxPercent);
        quotation.setTaxAmount(tax);
        quotation.setOtherExpenses(other);
        quotation.setTotalAmount(professionalFee.add(tax).add(other));
    }

    private void snapshot(Quotation quotation, String summary) {
        QuotationVersion version = new QuotationVersion();
        version.setQuotationId(quotation.getId());
        version.setVersionNo(quotation.getVersionNo());
        version.setTotalAmount(quotation.getTotalAmount());
        version.setChangeSummary(summary);
        version.setCreatedBy(CurrentUser.id());
        try {
            version.setSnapshot(objectMapper.writeValueAsString(mapper.quotation(quotation, null, List.of(), false, null)));
        } catch (JsonProcessingException ex) {
            version.setSnapshot("{}");
        }
        versionRepository.save(version);
    }

    private QuotationDtos.QuotationResponse sellerView(Quotation q) {
        boolean editable = q.getStatus() == QuotationStatus.DRAFT
                || (REVISABLE.contains(q.getStatus()) && (q.getStatus() == QuotationStatus.CLARIFICATION_REQUESTED
                || (!RfqService.deadlinePassed(q.getRfq()) && RfqStatus.EVALUABLE.contains(q.getRfq().getStatus()))));
        Long engagementId = q.getStatus() == QuotationStatus.ACCEPTED
                ? engagementRepository.findByQuotationId(q.getId()).map(Engagement::getId).orElse(null) : null;
        return mapper.quotation(q, null, documentService.list(ProcEnums.OwnerType.QUOTATION, q.getId()), editable, engagementId);
    }

    private QuotationDtos.QuotationResponse sellerViewLight(Quotation q) {
        return mapper.quotation(q, null, List.of(), q.getStatus() == QuotationStatus.DRAFT, null);
    }

    private QuotationDtos.QuotationResponse view(Quotation q, UserSummary seller, boolean editable) {
        Long engagementId = q.getStatus() == QuotationStatus.ACCEPTED
                ? engagementRepository.findByQuotationId(q.getId()).map(Engagement::getId).orElse(null) : null;
        return mapper.quotation(q, seller, documentService.list(ProcEnums.OwnerType.QUOTATION, q.getId()), editable, engagementId);
    }

    private UserSummary safeSummary(Long sellerId) {
        return guard.summaries(List.of(sellerId)).get(sellerId);
    }

    Quotation quotation(Long id) {
        return quotationRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Quotation not found"));
    }

    private Quotation ownedBySeller(Long id) {
        Quotation quotation = quotation(id);
        if (!CurrentUser.owns(quotation.getSellerId())) {
            throw BusinessException.notFound("Quotation not found");
        }
        return quotation;
    }

    private Quotation ownedByBuyer(Long id) {
        Quotation quotation = quotation(id);
        if (!CurrentUser.owns(quotation.getBuyerId()) || quotation.getStatus() == QuotationStatus.DRAFT
                || quotation.getStatus() == QuotationStatus.WITHDRAWN) {
            throw BusinessException.notFound("Quotation not found");
        }
        return quotation;
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
