package com.sidbi.bds.procurement.service;

import com.sidbi.bds.common.api.PageRequestFactory;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.client.dto.NotificationRequest;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.security.AuthUser;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.util.References;
import com.sidbi.bds.common.util.Search;
import com.sidbi.bds.procurement.domain.Engagement;
import com.sidbi.bds.procurement.domain.EngagementUpdate;
import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.ProcEnums.EngagementStatus;
import com.sidbi.bds.procurement.domain.ProcEnums.WorkOrderStatus;
import com.sidbi.bds.procurement.domain.Quotation;
import com.sidbi.bds.procurement.domain.Rfq;
import com.sidbi.bds.procurement.domain.WorkOrder;
import com.sidbi.bds.procurement.dto.EngagementDtos;
import com.sidbi.bds.procurement.dto.QueryDtos;
import com.sidbi.bds.procurement.dto.QuotationDtos;
import com.sidbi.bds.procurement.dto.RfqDtos;
import com.sidbi.bds.procurement.repository.EngagementRepository;
import com.sidbi.bds.procurement.repository.EngagementUpdateRepository;
import com.sidbi.bds.procurement.repository.QueryTicketRepository;
import com.sidbi.bds.procurement.repository.ReviewRepository;
import com.sidbi.bds.procurement.repository.WorkOrderRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EngagementService {

    private static final Set<WorkOrderStatus> LIVE_WORK_ORDER = EnumSet.of(WorkOrderStatus.REQUESTED, WorkOrderStatus.ISSUED, WorkOrderStatus.ACKNOWLEDGED);
    private static final String DISCLAIMER = "Provider selection on BDS Marketplace is not by itself proof of a legally executed service contract. "
            + "Payments are made outside the platform and are not processed, held or guaranteed by BDS Marketplace. "
            + "Contract values shown are user-declared.";

    private final EngagementRepository engagementRepository;
    private final EngagementUpdateRepository updateRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ReviewRepository reviewRepository;
    private final QueryTicketRepository ticketRepository;
    private final QueryService queryService;
    private final DocumentService documentService;
    private final NotificationClient notificationClient;
    private final ActorGuard guard;
    private final ProcMapper mapper;

    public record EngagementFilter(EngagementStatus status, String q, Long sellerId, Long buyerId) {
    }

    Engagement createFromSelection(Quotation quotation, AuthUser buyer, QuotationDtos.AcceptRequest request) {
        Rfq rfq = quotation.getRfq();
        Engagement engagement = new Engagement();
        engagement.setRfq(rfq);
        engagement.setQuotation(quotation);
        engagement.setSellerId(quotation.getSellerId());
        engagement.setSellerName(quotation.getSellerName());
        engagement.setBuyerId(rfq.getBuyerId());
        engagement.setBuyerName(rfq.getBuyerName());
        engagement.setTitle(rfq.getTitle());
        engagement.setQuotedValue(quotation.getTotalAmount());
        engagement.setContractValue(request.contractValue() != null ? request.contractValue() : quotation.getTotalAmount());
        engagement.setStartDate(request.startDate());
        engagement.setExpectedEndDate(request.expectedEndDate() != null ? request.expectedEndDate()
                : request.startDate() != null && quotation.getTimelineDays() != null ? request.startDate().plusDays(quotation.getTimelineDays()) : null);
        engagement.setStatus(EngagementStatus.SELECTED);
        engagement.setSelectionRemarks(request.remarks());
        engagementRepository.save(engagement);
        engagement.setEngagementNumber(References.of("ENG", engagement.getId()));
        log(engagement, ProcEnums.UpdateType.STATUS_CHANGE, "Provider selected. " + request.remarks(), null, null, EngagementStatus.SELECTED, buyer);
        notificationClient.notify(NotificationRequest.to(List.of(engagement.getSellerId(), engagement.getBuyerId()), "ENGAGEMENT_CREATED",
                "ENGAGEMENT", "/engagements/" + engagement.getId(), params(engagement)));
        notificationClient.audit("ENGAGEMENT_CREATED", "ENGAGEMENT", engagement.getId(), engagement.getEngagementNumber());
        return engagement;
    }

    @Transactional(readOnly = true)
    public PageResponse<EngagementDtos.EngagementSummary> list(EngagementStatus status, String q, int page, int size) {
        AuthUser user = CurrentUser.get();
        EngagementFilter filter;
        if (user.isSeller()) {
            filter = new EngagementFilter(status, q, user.userId(), null);
        } else if (user.isBuyer()) {
            filter = new EngagementFilter(status, q, null, user.userId());
        } else {
            guard.admin(Permission.RFQ_MONITOR);
            filter = new EngagementFilter(status, q, null, null);
        }
        Page<Engagement> result = engagementRepository.findAll(spec(filter), PageRequestFactory.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        return PageResponse.of(result, e -> mapper.engagement(e, latestWorkOrderStatus(e.getId())));
    }

    @Transactional(readOnly = true)
    public EngagementDtos.EngagementDetail detail(Long id) {
        Engagement engagement = accessible(id);
        AuthUser user = CurrentUser.get();
        List<EngagementDtos.WorkOrderResponse> workOrders = workOrderRepository.findByEngagementIdOrderByCreatedAtDesc(id).stream()
                .map(w -> mapper.workOrder(w, documentService.list(ProcEnums.OwnerType.WORK_ORDER, w.getId())))
                .toList();
        List<QueryDtos.TicketSummary> queries = new ArrayList<>();
        ticketRepository.findByContextTypeAndContextIdOrderByCreatedAtDesc(ProcEnums.ContextType.ENGAGEMENT, id)
                .forEach(t -> queries.add(mapper.ticket(t, user.userId(), user.role().name())));
        Quotation quotation = engagement.getQuotation();
        QuotationDtos.QuotationResponse quotationView = mapper.quotation(quotation, null,
                documentService.list(ProcEnums.OwnerType.QUOTATION, quotation.getId()), false, engagement.getId());
        return new EngagementDtos.EngagementDetail(
                mapper.engagement(engagement, workOrders.isEmpty() ? null : workOrders.getFirst().status()),
                engagement.getSelectionRemarks(),
                engagement.getDeliveredAt(),
                engagement.getDeliveryNote(),
                engagement.getDeliveryRemarks(),
                engagement.getCompletedAt(),
                engagement.getCancelReason(),
                quotationView,
                workOrders,
                updateRepository.findByEngagementIdOrderByCreatedAtDesc(id).stream().map(mapper::update).toList(),
                documentService.list(ProcEnums.OwnerType.ENGAGEMENT, id),
                queries,
                reviewRepository.findByEngagementId(id).map(r -> mapper.review(r, true)).orElse(null),
                DISCLAIMER);
    }

    @Transactional
    public EngagementDtos.WorkOrderResponse requestWorkOrder(Long engagementId, String remarks) {
        AuthUser seller = guard.seller(Permission.ENGAGEMENT_MANAGE);
        Engagement engagement = ownedBySeller(engagementId);
        if (engagement.getStatus() != EngagementStatus.SELECTED) {
            throw BusinessException.conflict("A work order can be requested only after selection and before the engagement starts");
        }
        if (workOrderRepository.existsByEngagementIdAndStatusIn(engagementId, LIVE_WORK_ORDER)) {
            throw BusinessException.conflict("A work order request is already in progress for this engagement");
        }
        WorkOrder workOrder = new WorkOrder();
        workOrder.setEngagement(engagement);
        workOrder.setSellerId(engagement.getSellerId());
        workOrder.setBuyerId(engagement.getBuyerId());
        workOrder.setStatus(WorkOrderStatus.REQUESTED);
        workOrder.setRequestRemarks(remarks);
        workOrder.setRequestedAt(LocalDateTime.now());
        workOrderRepository.save(workOrder);
        workOrder.setWorkOrderNumber(References.of("WO", workOrder.getId()));
        log(engagement, ProcEnums.UpdateType.NOTE, "Work order requested by provider" + (remarks == null || remarks.isBlank() ? "" : ": " + remarks),
                null, null, null, seller);
        notificationClient.notify(engagement.getBuyerId(), "WORK_ORDER_REQUESTED", "WORK_ORDER", "/buyer/engagements/" + engagementId,
                with(params(engagement), "workOrder", workOrder.getWorkOrderNumber()));
        notificationClient.audit("WORK_ORDER_REQUESTED", "WORK_ORDER", workOrder.getId(), engagement.getEngagementNumber());
        return mapper.workOrder(workOrder, List.of());
    }

    @Transactional
    public EngagementDtos.WorkOrderResponse issueWorkOrder(Long workOrderId, EngagementDtos.IssueWorkOrderRequest request, MultipartFile file) {
        AuthUser buyer = guard.buyer(null);
        WorkOrder workOrder = workOrderRepository.findWithEngagementById(workOrderId)
                .filter(w -> CurrentUser.owns(w.getBuyerId()))
                .orElseThrow(() -> BusinessException.notFound("Work order not found"));
        if (workOrder.getStatus() != WorkOrderStatus.REQUESTED) {
            throw BusinessException.conflict("Work order has already been processed");
        }
        return issue(workOrder, request, file, buyer);
    }

    @Transactional
    public EngagementDtos.WorkOrderResponse issueDirect(Long engagementId, EngagementDtos.IssueWorkOrderRequest request, MultipartFile file) {
        AuthUser buyer = guard.buyer(null);
        Engagement engagement = ownedByBuyer(engagementId);
        if (engagement.getStatus() != EngagementStatus.SELECTED) {
            throw BusinessException.conflict("Work order can be issued only before the engagement starts");
        }
        WorkOrder existing = workOrderRepository.findByEngagementIdOrderByCreatedAtDesc(engagementId).stream()
                .filter(w -> w.getStatus() == WorkOrderStatus.REQUESTED).findFirst().orElse(null);
        if (existing != null) {
            return issue(existing, request, file, buyer);
        }
        if (workOrderRepository.existsByEngagementIdAndStatusIn(engagementId, EnumSet.of(WorkOrderStatus.ISSUED, WorkOrderStatus.ACKNOWLEDGED))) {
            throw BusinessException.conflict("A work order has already been issued for this engagement");
        }
        WorkOrder workOrder = new WorkOrder();
        workOrder.setEngagement(engagement);
        workOrder.setSellerId(engagement.getSellerId());
        workOrder.setBuyerId(engagement.getBuyerId());
        workOrder.setStatus(WorkOrderStatus.REQUESTED);
        workOrderRepository.save(workOrder);
        workOrder.setWorkOrderNumber(References.of("WO", workOrder.getId()));
        return issue(workOrder, request, file, buyer);
    }

    @Transactional
    public EngagementDtos.WorkOrderResponse declineWorkOrder(Long workOrderId, String reason) {
        AuthUser buyer = guard.buyer(null);
        WorkOrder workOrder = workOrderRepository.findWithEngagementById(workOrderId)
                .filter(w -> CurrentUser.owns(w.getBuyerId()))
                .orElseThrow(() -> BusinessException.notFound("Work order not found"));
        if (workOrder.getStatus() != WorkOrderStatus.REQUESTED) {
            throw BusinessException.conflict("Only pending work order requests can be declined");
        }
        workOrder.setStatus(WorkOrderStatus.DECLINED);
        workOrder.setDeclinedReason(reason);
        log(workOrder.getEngagement(), ProcEnums.UpdateType.NOTE, "Work order request declined: " + reason, null, null, null, buyer);
        notificationClient.notify(workOrder.getSellerId(), "WORK_ORDER_DECLINED", "WORK_ORDER", "/seller/engagements/" + workOrder.getEngagement().getId(),
                with(with(params(workOrder.getEngagement()), "workOrder", workOrder.getWorkOrderNumber()), "remarks", reason));
        notificationClient.audit("WORK_ORDER_DECLINED", "WORK_ORDER", workOrderId, reason);
        return mapper.workOrder(workOrder, documentService.list(ProcEnums.OwnerType.WORK_ORDER, workOrderId));
    }

    @Transactional
    public EngagementDtos.WorkOrderResponse acknowledgeWorkOrder(Long workOrderId) {
        AuthUser seller = guard.seller(Permission.ENGAGEMENT_MANAGE);
        WorkOrder workOrder = workOrderRepository.findWithEngagementById(workOrderId)
                .filter(w -> CurrentUser.owns(w.getSellerId()))
                .orElseThrow(() -> BusinessException.notFound("Work order not found"));
        if (workOrder.getStatus() != WorkOrderStatus.ISSUED) {
            throw BusinessException.conflict("Only issued work orders can be acknowledged");
        }
        workOrder.setStatus(WorkOrderStatus.ACKNOWLEDGED);
        workOrder.setAcknowledgedAt(LocalDateTime.now());
        log(workOrder.getEngagement(), ProcEnums.UpdateType.NOTE, "Work order " + workOrder.getWorkOrderNumber() + " acknowledged", null, null, null, seller);
        notificationClient.notify(workOrder.getBuyerId(), "WORK_ORDER_ACKNOWLEDGED", "WORK_ORDER", "/buyer/engagements/" + workOrder.getEngagement().getId(),
                with(params(workOrder.getEngagement()), "workOrder", workOrder.getWorkOrderNumber()));
        notificationClient.audit("WORK_ORDER_ACKNOWLEDGED", "WORK_ORDER", workOrderId, null);
        return mapper.workOrder(workOrder, documentService.list(ProcEnums.OwnerType.WORK_ORDER, workOrderId));
    }

    @Transactional(readOnly = true)
    public PageResponse<EngagementDtos.WorkOrderResponse> workOrders(WorkOrderStatus status, int page, int size) {
        AuthUser user = CurrentUser.get();
        Specification<WorkOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (user.isSeller()) {
                predicates.add(cb.equal(root.get("sellerId"), user.userId()));
            } else if (user.isBuyer()) {
                predicates.add(cb.equal(root.get("buyerId"), user.userId()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        if (user.isAdmin()) {
            guard.admin(Permission.RFQ_MONITOR);
        }
        Page<WorkOrder> result = workOrderRepository.findAll(spec, PageRequestFactory.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        return PageResponse.of(result, w -> mapper.workOrder(w, documentService.list(ProcEnums.OwnerType.WORK_ORDER, w.getId())));
    }

    @Transactional
    public EngagementDtos.EngagementDetail start(Long id) {
        AuthUser seller = guard.seller(Permission.ENGAGEMENT_MANAGE);
        Engagement engagement = ownedBySeller(id);
        if (engagement.getStatus() != EngagementStatus.ENGAGEMENT_INITIATED) {
            throw BusinessException.conflict("Engagement can be started after the purchaser issues the work order");
        }
        transition(engagement, EngagementStatus.IN_PROGRESS, "Work started by provider", seller);
        notificationClient.notify(engagement.getBuyerId(), "ENGAGEMENT_STARTED", "ENGAGEMENT", "/buyer/engagements/" + id, params(engagement));
        return detail(id);
    }

    @Transactional
    public EngagementDtos.EngagementDetail progress(Long id, EngagementDtos.ProgressRequest request) {
        AuthUser seller = guard.seller(Permission.ENGAGEMENT_MANAGE);
        Engagement engagement = ownedBySeller(id);
        if (engagement.getStatus() != EngagementStatus.IN_PROGRESS) {
            throw BusinessException.conflict("Progress can be updated only while the engagement is in progress");
        }
        if (request.progressPercent() != null) {
            if (request.progressPercent() < engagement.getProgressPercent()) {
                throw BusinessException.badRequest("Progress cannot move backwards");
            }
            engagement.setProgressPercent(Math.min(request.progressPercent(), 95));
        }
        log(engagement, ProcEnums.UpdateType.PROGRESS, request.message(), engagement.getProgressPercent(), null, null, seller);
        notificationClient.notify(engagement.getBuyerId(), "ENGAGEMENT_PROGRESS", "ENGAGEMENT", "/buyer/engagements/" + id,
                with(with(params(engagement), "progress", String.valueOf(engagement.getProgressPercent())), "message", request.message()));
        return detail(id);
    }

    @Transactional
    public EngagementDtos.EngagementDetail deliver(Long id, EngagementDtos.DeliveryRequest request) {
        AuthUser seller = guard.seller(Permission.ENGAGEMENT_MANAGE);
        Engagement engagement = ownedBySeller(id);
        if (engagement.getStatus() != EngagementStatus.IN_PROGRESS) {
            throw BusinessException.conflict("Only engagements in progress can be marked as delivered");
        }
        engagement.setDeliveredAt(LocalDateTime.now());
        engagement.setDeliveryNote(request.message());
        engagement.setProgressPercent(100);
        engagement.setDeliveryConfirmed(null);
        transition(engagement, EngagementStatus.DELIVERED, "Deliverables submitted: " + request.message(), seller);
        notificationClient.notify(engagement.getBuyerId(), "ENGAGEMENT_DELIVERED", "ENGAGEMENT", "/buyer/engagements/" + id, params(engagement));
        return detail(id);
    }

    @Transactional
    public EngagementDtos.EngagementDetail confirmDelivery(Long id, EngagementDtos.ConfirmDeliveryRequest request) {
        AuthUser buyer = guard.buyer(null);
        Engagement engagement = ownedByBuyer(id);
        if (engagement.getStatus() != EngagementStatus.DELIVERED) {
            throw BusinessException.conflict("Delivery can be confirmed after the provider marks the work as delivered");
        }
        engagement.setDeliveryConfirmed(request.delivered());
        engagement.setDeliveryRemarks(request.remarks());
        if (Boolean.TRUE.equals(request.delivered())) {
            engagement.setCompletedAt(LocalDateTime.now());
            transition(engagement, EngagementStatus.COMPLETED, "Delivery confirmed by purchaser"
                    + (request.remarks() == null || request.remarks().isBlank() ? "" : ": " + request.remarks()), buyer);
            Rfq rfq = engagement.getRfq();
            if (rfq.getStatus() == ProcEnums.RfqStatus.PROVIDER_SELECTED) {
                rfq.setStatus(ProcEnums.RfqStatus.CLOSED);
                rfq.setClosedAt(LocalDateTime.now());
            }
            notificationClient.notify(engagement.getSellerId(), "DELIVERY_CONFIRMED", "ENGAGEMENT", "/seller/engagements/" + id, params(engagement));
            notificationClient.notify(engagement.getBuyerId(), "FEEDBACK_REQUESTED", "FEEDBACK", "/buyer/engagements/" + id + "/feedback", params(engagement));
        } else {
            if (request.remarks() == null || request.remarks().isBlank()) {
                throw BusinessException.badRequest("Describe what has not been delivered");
            }
            engagement.setProgressPercent(90);
            transition(engagement, EngagementStatus.IN_PROGRESS, "Delivery not accepted by purchaser: " + request.remarks(), buyer);
            queryService.openContextTicket(ProcEnums.ContextType.ENGAGEMENT, id, engagement.getEngagementNumber(), engagement.getRfq().getId(), id,
                    engagement.getSellerId(), engagement.getSellerName(), engagement.getBuyerId(), engagement.getBuyerName(), buyer,
                    "Delivery not accepted - " + engagement.getEngagementNumber(), request.remarks(), ProcEnums.Priority.HIGH);
            notificationClient.notify(engagement.getSellerId(), "DELIVERY_DISPUTED", "ENGAGEMENT", "/seller/engagements/" + id,
                    with(params(engagement), "remarks", request.remarks()));
        }
        notificationClient.audit(Boolean.TRUE.equals(request.delivered()) ? "DELIVERY_CONFIRMED" : "DELIVERY_REJECTED", "ENGAGEMENT", id, request.remarks());
        return detail(id);
    }

    @Transactional
    public EngagementDtos.EngagementDetail dispute(Long id, String reason) {
        AuthUser user = CurrentUser.get();
        Engagement engagement = accessible(id);
        if (!EngagementStatus.ACTIVE.contains(engagement.getStatus()) || engagement.getStatus() == EngagementStatus.DISPUTED) {
            throw BusinessException.conflict("A dispute can be raised only on an active engagement");
        }
        transition(engagement, EngagementStatus.DISPUTED, "Dispute raised: " + reason, user);
        QueryDtos.TicketDetail ticket = queryService.openContextTicket(ProcEnums.ContextType.ENGAGEMENT, id, engagement.getEngagementNumber(),
                engagement.getRfq().getId(), id, engagement.getSellerId(), engagement.getSellerName(), engagement.getBuyerId(),
                engagement.getBuyerName(), user, "Dispute - " + engagement.getEngagementNumber(), reason, ProcEnums.Priority.HIGH);
        queryService.escalateInternal(ticket.summary().id(), "Engagement dispute raised", user);
        notificationClient.notify(NotificationRequest.to(otherParties(engagement, user), "ENGAGEMENT_DISPUTED", "ENGAGEMENT", "/engagements/" + id,
                with(params(engagement), "remarks", reason)));
        notificationClient.audit("ENGAGEMENT_DISPUTED", "ENGAGEMENT", id, reason);
        return detail(id);
    }

    @Transactional
    public EngagementDtos.EngagementDetail resume(Long id, String remarks) {
        AuthUser user = CurrentUser.get();
        Engagement engagement = accessible(id);
        if (engagement.getStatus() != EngagementStatus.DISPUTED) {
            throw BusinessException.conflict("Only disputed engagements can be resumed");
        }
        transition(engagement, EngagementStatus.IN_PROGRESS, "Dispute settled. Engagement resumed" + (remarks == null ? "" : ": " + remarks), user);
        notificationClient.notify(NotificationRequest.to(List.of(engagement.getSellerId(), engagement.getBuyerId()), "ENGAGEMENT_RESUMED",
                "ENGAGEMENT", "/engagements/" + id, params(engagement)));
        return detail(id);
    }

    @Transactional
    public EngagementDtos.EngagementDetail cancel(Long id, String reason) {
        AuthUser user = CurrentUser.get();
        Engagement engagement = accessible(id);
        if (user.isSeller() || engagement.getStatus() == EngagementStatus.COMPLETED || engagement.getStatus() == EngagementStatus.CANCELLED) {
            throw BusinessException.conflict("Engagement cannot be cancelled");
        }
        engagement.setCancelReason(reason);
        transition(engagement, EngagementStatus.CANCELLED, "Engagement cancelled: " + reason, user);
        notificationClient.notify(engagement.getSellerId(), "ENGAGEMENT_CANCELLED", "ENGAGEMENT", "/seller/engagements/" + id,
                with(params(engagement), "remarks", reason));
        notificationClient.audit("ENGAGEMENT_CANCELLED", "ENGAGEMENT", id, reason);
        return detail(id);
    }

    @Transactional
    public RfqDtos.DocumentResponse addDocument(Long id, String category, MultipartFile file) {
        AuthUser user = CurrentUser.get();
        Engagement engagement = accessible(id);
        RfqDtos.DocumentResponse response = documentService.upload(ProcEnums.OwnerType.ENGAGEMENT, id, category, file);
        log(engagement, ProcEnums.UpdateType.DOCUMENT, "Document uploaded: " + response.originalName(), null, null, null, user);
        notificationClient.notify(NotificationRequest.to(otherParties(engagement, user), "ENGAGEMENT_DOCUMENT", "ENGAGEMENT", "/engagements/" + id,
                with(params(engagement), "document", response.originalName())));
        return response;
    }

    private EngagementDtos.WorkOrderResponse issue(WorkOrder workOrder, EngagementDtos.IssueWorkOrderRequest request, MultipartFile file, AuthUser buyer) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw BusinessException.badRequest("End date cannot be before start date");
        }
        Engagement engagement = workOrder.getEngagement();
        workOrder.setStatus(WorkOrderStatus.ISSUED);
        workOrder.setPoReference(request.poReference());
        workOrder.setWorkOrderDate(request.workOrderDate() != null ? request.workOrderDate() : LocalDate.now());
        workOrder.setAgreedValue(request.agreedValue() != null ? request.agreedValue() : engagement.getContractValue());
        workOrder.setStartDate(request.startDate());
        workOrder.setEndDate(request.endDate());
        workOrder.setScopeSummary(request.scopeSummary());
        workOrder.setTerms(request.terms());
        workOrder.setBuyerRemarks(request.remarks());
        workOrder.setIssuedAt(LocalDateTime.now());
        if (file != null && !file.isEmpty()) {
            documentService.upload(ProcEnums.OwnerType.WORK_ORDER, workOrder.getId(), "WORK_ORDER", file);
        }
        engagement.setContractValue(workOrder.getAgreedValue());
        engagement.setStartDate(request.startDate());
        if (request.endDate() != null) {
            engagement.setExpectedEndDate(request.endDate());
        }
        transition(engagement, EngagementStatus.ENGAGEMENT_INITIATED, "Work order " + workOrder.getWorkOrderNumber() + " issued (ref "
                + request.poReference() + ")", buyer);
        notificationClient.notify(workOrder.getSellerId(), "WORK_ORDER_ISSUED", "WORK_ORDER", "/seller/engagements/" + engagement.getId(),
                with(params(engagement), "workOrder", workOrder.getWorkOrderNumber()));
        notificationClient.audit("WORK_ORDER_ISSUED", "WORK_ORDER", workOrder.getId(), request.poReference());
        return mapper.workOrder(workOrder, documentService.list(ProcEnums.OwnerType.WORK_ORDER, workOrder.getId()));
    }

    private void transition(Engagement engagement, EngagementStatus to, String message, AuthUser actor) {
        EngagementStatus from = engagement.getStatus();
        engagement.setStatus(to);
        log(engagement, ProcEnums.UpdateType.STATUS_CHANGE, message, engagement.getProgressPercent(), from, to, actor);
        notificationClient.audit("ENGAGEMENT_" + to.name(), "ENGAGEMENT", engagement.getId(), message);
    }

    private void log(Engagement engagement, ProcEnums.UpdateType type, String message, Integer progress,
                     EngagementStatus from, EngagementStatus to, AuthUser actor) {
        EngagementUpdate update = new EngagementUpdate();
        update.setEngagementId(engagement.getId());
        update.setUpdateType(type);
        update.setMessage(message);
        update.setProgressPercent(progress);
        update.setFromStatus(from);
        update.setToStatus(to);
        update.setAuthorId(actor.userId());
        update.setAuthorRole(actor.role().name());
        update.setAuthorName(actor.name());
        updateRepository.save(update);
    }

    private WorkOrderStatus latestWorkOrderStatus(Long engagementId) {
        List<WorkOrder> list = workOrderRepository.findByEngagementIdOrderByCreatedAtDesc(engagementId);
        return list.isEmpty() ? null : list.getFirst().getStatus();
    }

    Engagement engagement(Long id) {
        return engagementRepository.findWithDetailsById(id).orElseThrow(() -> BusinessException.notFound("Engagement not found"));
    }

    Engagement accessible(Long id) {
        Engagement engagement = engagement(id);
        AuthUser user = CurrentUser.get();
        if (user.isAdmin()) {
            guard.admin(Permission.RFQ_MONITOR);
            return engagement;
        }
        if (!user.userId().equals(engagement.getSellerId()) && !user.userId().equals(engagement.getBuyerId())) {
            throw BusinessException.notFound("Engagement not found");
        }
        return engagement;
    }

    private Engagement ownedBySeller(Long id) {
        Engagement engagement = engagement(id);
        if (!CurrentUser.owns(engagement.getSellerId())) {
            throw BusinessException.notFound("Engagement not found");
        }
        return engagement;
    }

    Engagement ownedByBuyer(Long id) {
        Engagement engagement = engagement(id);
        if (!CurrentUser.owns(engagement.getBuyerId())) {
            throw BusinessException.notFound("Engagement not found");
        }
        return engagement;
    }

    /** Parties to notify about an action: the counterparty, or both parties when an administrator acted. */
    private static List<Long> otherParties(Engagement engagement, AuthUser user) {
        if (user.userId().equals(engagement.getSellerId())) {
            return List.of(engagement.getBuyerId());
        }
        if (user.userId().equals(engagement.getBuyerId())) {
            return List.of(engagement.getSellerId());
        }
        return List.of(engagement.getSellerId(), engagement.getBuyerId());
    }

    private static Map<String, String> params(Engagement e) {
        return Map.of("engagement", nz(e.getEngagementNumber()), "title", nz(e.getTitle()), "seller", nz(e.getSellerName()),
                "buyer", nz(e.getBuyerName()));
    }

    private static Map<String, String> with(Map<String, String> base, String key, String value) {
        Map<String, String> copy = new HashMap<>(base);
        copy.put(key, nz(value));
        return copy;
    }

    private static Specification<Engagement> spec(EngagementFilter filter) {
        return (root, query, cb) -> {
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
            if (filter.q() != null && !filter.q().isBlank()) {
                String like = Search.contains(filter.q());
                predicates.add(cb.or(cb.like(cb.lower(root.get("title")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("engagementNumber")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("sellerName")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("buyerName")), like, Search.ESCAPE)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
