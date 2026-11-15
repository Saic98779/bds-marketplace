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
import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.ProcEnums.TicketStatus;
import com.sidbi.bds.procurement.domain.QueryMessage;
import com.sidbi.bds.procurement.domain.QueryTicket;
import com.sidbi.bds.procurement.domain.Quotation;
import com.sidbi.bds.procurement.domain.Rfq;
import com.sidbi.bds.procurement.domain.RfqInvitation;
import com.sidbi.bds.procurement.domain.WorkOrder;
import com.sidbi.bds.procurement.dto.QueryDtos;
import com.sidbi.bds.procurement.dto.RfqDtos;
import com.sidbi.bds.procurement.repository.EngagementRepository;
import com.sidbi.bds.procurement.repository.QueryTicketRepository;
import com.sidbi.bds.procurement.repository.QuotationRepository;
import com.sidbi.bds.procurement.repository.RfqInvitationRepository;
import com.sidbi.bds.procurement.repository.RfqRepository;
import com.sidbi.bds.procurement.repository.WorkOrderRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QueryService {

    private final QueryTicketRepository ticketRepository;
    private final RfqRepository rfqRepository;
    private final RfqInvitationRepository invitationRepository;
    private final QuotationRepository quotationRepository;
    private final EngagementRepository engagementRepository;
    private final WorkOrderRepository workOrderRepository;
    private final DocumentService documentService;
    private final NotificationClient notificationClient;
    private final ActorGuard guard;
    private final ProcMapper mapper;

    public record TicketFilter(TicketStatus status, ProcEnums.ContextType contextType, ProcEnums.TicketType ticketType,
                               Boolean escalated, String q) {
    }

    @Transactional
    public QueryDtos.TicketDetail create(QueryDtos.CreateTicketRequest request) {
        AuthUser user = CurrentUser.get();
        if (user.isAdmin()) {
            throw BusinessException.forbidden("Administrators respond to escalated queries and grievances");
        }
        if (request.contextType() == ProcEnums.ContextType.PLATFORM) {
            return grievance(new QueryDtos.GrievanceRequest(null, null, request.subject(), request.description(), request.priority()));
        }
        if (request.contextId() == null) {
            throw BusinessException.badRequest("Select the RFQ, quotation, work order or engagement this query relates to");
        }
        Party party = resolve(request.contextType(), request.contextId(), request.counterpartyId(), user);
        return openContextTicket(request.contextType(), request.contextId(), party.reference(), party.rfqId(), party.engagementId(),
                party.sellerId(), party.sellerName(), party.buyerId(), party.buyerName(), user, request.subject(), request.description(),
                request.priority() == null ? ProcEnums.Priority.MEDIUM : request.priority());
    }

    @Transactional
    public QueryDtos.TicketDetail grievance(QueryDtos.GrievanceRequest request) {
        AuthUser user = CurrentUser.get();
        if (user.isAdmin()) {
            throw BusinessException.forbidden("Grievances are raised by sellers and purchasers");
        }
        QueryTicket ticket = new QueryTicket();
        ticket.setTicketType(ProcEnums.TicketType.GRIEVANCE);
        ticket.setContextType(request.contextType() == null ? ProcEnums.ContextType.PLATFORM : request.contextType());
        ticket.setContextId(request.contextId());
        if (user.isSeller()) {
            ticket.setSellerId(user.userId());
            ticket.setSellerName(user.name());
        } else {
            ticket.setBuyerId(user.userId());
            ticket.setBuyerName(user.name());
        }
        fill(ticket, user, request.subject(), request.description(), request.priority() == null ? ProcEnums.Priority.MEDIUM : request.priority());
        ticket.setEscalated(true);
        ticket.setEscalatedAt(LocalDateTime.now());
        ticket.setStatus(TicketStatus.ESCALATED);
        ticketRepository.save(ticket);
        ticket.setTicketNumber(References.of("GRV", ticket.getId()));
        message(ticket, user, request.description(), false);
        notificationClient.notify(user.userId(), "GRIEVANCE_SUBMITTED", "QUERY", "/queries/" + ticket.getId(),
                Map.of("ticket", ticket.getTicketNumber(), "subject", ticket.getSubject()));
        notificationClient.notify(NotificationRequest.toAdmins(Permission.GRIEVANCE_MANAGE.name(), "GRIEVANCE_RECEIVED", "QUERY",
                "/admin/grievances/" + ticket.getId(), Map.of("ticket", ticket.getTicketNumber(), "subject", ticket.getSubject(),
                        "raisedBy", nz(user.name()))));
        notificationClient.audit("GRIEVANCE_SUBMITTED", "TICKET", ticket.getId(), ticket.getSubject());
        return detailOf(ticket, user);
    }

    QueryDtos.TicketDetail openContextTicket(ProcEnums.ContextType type, Long contextId, String reference, Long rfqId, Long engagementId,
                                             Long sellerId, String sellerName, Long buyerId, String buyerName, AuthUser raiser,
                                             String subject, String body, ProcEnums.Priority priority) {
        QueryTicket ticket = new QueryTicket();
        ticket.setTicketType(ProcEnums.TicketType.QUERY);
        ticket.setContextType(type);
        ticket.setContextId(contextId);
        ticket.setContextReference(reference);
        ticket.setRfqId(rfqId);
        ticket.setEngagementId(engagementId);
        ticket.setSellerId(sellerId);
        ticket.setSellerName(sellerName);
        ticket.setBuyerId(buyerId);
        ticket.setBuyerName(buyerName);
        fill(ticket, raiser, subject, body, priority);
        ticket.setStatus(TicketStatus.OPEN);
        ticketRepository.save(ticket);
        ticket.setTicketNumber(References.of("QRY", ticket.getId()));
        message(ticket, raiser, body, false);
        Long recipient = counterparty(ticket, raiser.userId());
        notificationClient.notify(recipient, "QUERY_RAISED", "QUERY", "/queries/" + ticket.getId(),
                Map.of("ticket", ticket.getTicketNumber(), "subject", ticket.getSubject(), "context", nz(reference),
                        "raisedBy", nz(raiser.name())));
        notificationClient.audit("QUERY_RAISED", "TICKET", ticket.getId(), type + " " + nz(reference));
        return detailOf(ticket, raiser);
    }

    @Transactional(readOnly = true)
    public PageResponse<QueryDtos.TicketSummary> list(TicketFilter filter, int page, int size) {
        AuthUser user = CurrentUser.get();
        if (user.isAdmin()) {
            guard.admin(Permission.GRIEVANCE_MANAGE);
        }
        Specification<QueryTicket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (user.isAdmin()) {
                if (filter.escalated() == null && filter.ticketType() == null) {
                    predicates.add(cb.or(cb.isTrue(root.get("escalated")), cb.equal(root.get("ticketType"), ProcEnums.TicketType.GRIEVANCE)));
                }
            } else {
                predicates.add(cb.or(cb.equal(root.get("sellerId"), user.userId()), cb.equal(root.get("buyerId"), user.userId()),
                        cb.equal(root.get("raisedById"), user.userId())));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.contextType() != null) {
                predicates.add(cb.equal(root.get("contextType"), filter.contextType()));
            }
            if (filter.ticketType() != null) {
                predicates.add(cb.equal(root.get("ticketType"), filter.ticketType()));
            }
            if (filter.escalated() != null) {
                predicates.add(cb.equal(root.get("escalated"), filter.escalated()));
            }
            if (filter.q() != null && !filter.q().isBlank()) {
                String like = Search.contains(filter.q());
                predicates.add(cb.or(cb.like(cb.lower(root.get("subject")), like, Search.ESCAPE), cb.like(cb.lower(root.get("ticketNumber")), like, Search.ESCAPE),
                        cb.like(cb.lower(root.get("contextReference")), like, Search.ESCAPE)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<QueryTicket> result = ticketRepository.findAll(spec, PageRequestFactory.of(page, size, Sort.by(Sort.Direction.DESC, "lastActivityAt")));
        return PageResponse.of(result, t -> mapper.ticket(t, user.userId(), user.role().name()));
    }

    @Transactional(readOnly = true)
    public QueryDtos.TicketDetail detail(Long id) {
        AuthUser user = CurrentUser.get();
        return detailOf(accessible(id, user), user);
    }

    @Transactional
    public QueryDtos.TicketDetail reply(Long id, String body) {
        AuthUser user = CurrentUser.get();
        QueryTicket ticket = accessible(id, user);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw BusinessException.conflict("This query is closed");
        }
        if (user.isAdmin() && !ticket.isEscalated() && ticket.getTicketType() != ProcEnums.TicketType.GRIEVANCE) {
            throw BusinessException.forbidden("Administrators can respond only to escalated queries");
        }
        message(ticket, user, body, false);
        if (ticket.getStatus() != TicketStatus.ESCALATED) {
            ticket.setStatus(user.userId().equals(ticket.getRaisedById()) ? TicketStatus.OPEN : TicketStatus.RESPONDED);
        }
        Map<String, String> params = Map.of("ticket", ticket.getTicketNumber(), "subject", ticket.getSubject(), "from", nz(user.name()));
        for (Long recipient : recipients(ticket, user.userId())) {
            notificationClient.notify(recipient, "QUERY_REPLIED", "QUERY", "/queries/" + id, params);
        }
        return detailOf(ticket, user);
    }

    @Transactional
    public QueryDtos.TicketDetail resolve(Long id, String remarks) {
        AuthUser user = CurrentUser.get();
        QueryTicket ticket = accessible(id, user);
        if (!TicketStatus.UNRESOLVED.contains(ticket.getStatus())) {
            throw BusinessException.conflict("Query is not open");
        }
        if (!user.isAdmin() && !user.userId().equals(ticket.getRaisedById())) {
            throw BusinessException.forbidden("Only the person who raised the query can mark it resolved");
        }
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        ticket.setResolutionNote(remarks);
        system(ticket, user, "Marked as resolved" + (remarks == null || remarks.isBlank() ? "" : ": " + remarks));
        for (Long recipient : recipients(ticket, user.userId())) {
            notificationClient.notify(recipient, "QUERY_RESOLVED", "QUERY", "/queries/" + id,
                    Map.of("ticket", ticket.getTicketNumber(), "subject", ticket.getSubject()));
        }
        notificationClient.audit("QUERY_RESOLVED", "TICKET", id, remarks);
        return detailOf(ticket, user);
    }

    @Transactional
    public QueryDtos.TicketDetail reopen(Long id, String remarks) {
        AuthUser user = CurrentUser.get();
        QueryTicket ticket = accessible(id, user);
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw BusinessException.conflict("Only resolved queries can be reopened");
        }
        ticket.setStatus(TicketStatus.REOPENED);
        ticket.setResolvedAt(null);
        system(ticket, user, "Query reopened" + (remarks == null || remarks.isBlank() ? "" : ": " + remarks));
        for (Long recipient : recipients(ticket, user.userId())) {
            notificationClient.notify(recipient, "QUERY_REPLIED", "QUERY", "/queries/" + id,
                    Map.of("ticket", ticket.getTicketNumber(), "subject", ticket.getSubject(), "from", nz(user.name())));
        }
        notificationClient.audit("QUERY_REOPENED", "TICKET", id, remarks);
        return detailOf(ticket, user);
    }

    @Transactional
    public QueryDtos.TicketDetail escalate(Long id, String remarks) {
        AuthUser user = CurrentUser.get();
        QueryTicket ticket = accessible(id, user);
        if (user.isAdmin()) {
            throw BusinessException.forbidden("Escalation is raised by the parties to the query");
        }
        escalateInternal(id, remarks, user);
        return detailOf(ticket, user);
    }

    void escalateInternal(Long id, String reason, AuthUser user) {
        QueryTicket ticket = ticketRepository.findById(id).orElseThrow();
        if (ticket.isEscalated() || ticket.getStatus() == TicketStatus.CLOSED) {
            throw BusinessException.conflict("Query is already escalated or closed");
        }
        ticket.setEscalated(true);
        ticket.setEscalatedAt(LocalDateTime.now());
        ticket.setEscalationReason(reason);
        ticket.setStatus(TicketStatus.ESCALATED);
        system(ticket, user, "Escalated to BDS Marketplace support" + (reason == null || reason.isBlank() ? "" : ": " + reason));
        notificationClient.notify(NotificationRequest.toAdmins(Permission.GRIEVANCE_MANAGE.name(), "QUERY_ESCALATED", "QUERY",
                "/admin/grievances/" + id, Map.of("ticket", ticket.getTicketNumber(), "subject", ticket.getSubject(), "raisedBy", nz(user.name()))));
        Long other = counterparty(ticket, user.userId());
        notificationClient.notify(other, "QUERY_ESCALATED_PARTY", "QUERY", "/queries/" + id,
                Map.of("ticket", ticket.getTicketNumber(), "subject", ticket.getSubject()));
        notificationClient.audit("QUERY_ESCALATED", "TICKET", id, reason);
    }

    @Transactional
    public QueryDtos.TicketDetail close(Long id, String remarks) {
        AuthUser user = CurrentUser.get();
        QueryTicket ticket = accessible(id, user);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw BusinessException.conflict("Query is already closed");
        }
        boolean raiser = user.userId().equals(ticket.getRaisedById());
        if (!user.isAdmin() && !(raiser && ticket.getStatus() == TicketStatus.RESOLVED)) {
            throw BusinessException.forbidden("Queries can be closed by the raiser after resolution, or by an administrator");
        }
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(LocalDateTime.now());
        if (ticket.getResolvedAt() == null) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        if (remarks != null && !remarks.isBlank()) {
            ticket.setResolutionNote(remarks);
        }
        system(ticket, user, "Query closed" + (remarks == null || remarks.isBlank() ? "" : ": " + remarks));
        for (Long recipient : recipients(ticket, user.userId())) {
            notificationClient.notify(recipient, "QUERY_CLOSED", "QUERY", "/queries/" + id,
                    Map.of("ticket", ticket.getTicketNumber(), "subject", ticket.getSubject(), "remarks", nz(remarks)));
        }
        notificationClient.audit("QUERY_CLOSED", "TICKET", id, remarks);
        return detailOf(ticket, user);
    }

    @Transactional
    public RfqDtos.DocumentResponse addAttachment(Long id, MultipartFile file) {
        AuthUser user = CurrentUser.get();
        QueryTicket ticket = accessible(id, user);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw BusinessException.conflict("This query is closed");
        }
        RfqDtos.DocumentResponse response = documentService.upload(ProcEnums.OwnerType.QUERY, id, "QUERY", file);
        system(ticket, user, "Attachment added: " + response.originalName());
        return response;
    }

    private Party resolve(ProcEnums.ContextType type, Long contextId, Long counterpartyId, AuthUser user) {
        return switch (type) {
            case RFQ -> {
                Rfq rfq = rfqRepository.findById(contextId).orElseThrow(() -> BusinessException.notFound("RFQ not found"));
                Long sellerId;
                if (user.isSeller()) {
                    if (!invitationRepository.existsByRfqIdAndSellerId(contextId, user.userId())) {
                        throw BusinessException.notFound("RFQ not found");
                    }
                    sellerId = user.userId();
                } else {
                    if (!rfq.getBuyerId().equals(user.userId())) {
                        throw BusinessException.notFound("RFQ not found");
                    }
                    if (counterpartyId == null || !invitationRepository.existsByRfqIdAndSellerId(contextId, counterpartyId)) {
                        throw BusinessException.badRequest("Select an invited provider for this query");
                    }
                    sellerId = counterpartyId;
                }
                String sellerName = invitationRepository.findByRfqIdAndSellerId(contextId, sellerId).map(RfqInvitation::getSellerName).orElse(null);
                yield new Party(rfq.getRfqNumber(), rfq.getId(), null, sellerId, sellerName, rfq.getBuyerId(), rfq.getBuyerName());
            }
            case QUOTATION -> {
                Quotation q = quotationRepository.findById(contextId).orElseThrow(() -> BusinessException.notFound("Quotation not found"));
                if (!user.userId().equals(q.getSellerId()) && !user.userId().equals(q.getBuyerId())
                        || q.getStatus() == ProcEnums.QuotationStatus.DRAFT) {
                    throw BusinessException.notFound("Quotation not found");
                }
                yield new Party(q.getQuotationNumber(), q.getRfq().getId(), null, q.getSellerId(), q.getSellerName(), q.getBuyerId(),
                        q.getRfq().getBuyerName());
            }
            case ENGAGEMENT -> {
                Engagement e = engagementRepository.findById(contextId).orElseThrow(() -> BusinessException.notFound("Engagement not found"));
                if (!user.userId().equals(e.getSellerId()) && !user.userId().equals(e.getBuyerId())) {
                    throw BusinessException.notFound("Engagement not found");
                }
                yield new Party(e.getEngagementNumber(), e.getRfq().getId(), e.getId(), e.getSellerId(), e.getSellerName(), e.getBuyerId(), e.getBuyerName());
            }
            case WORK_ORDER -> {
                WorkOrder w = workOrderRepository.findWithEngagementById(contextId).orElseThrow(() -> BusinessException.notFound("Work order not found"));
                if (!user.userId().equals(w.getSellerId()) && !user.userId().equals(w.getBuyerId())) {
                    throw BusinessException.notFound("Work order not found");
                }
                Engagement e = w.getEngagement();
                yield new Party(w.getWorkOrderNumber(), e.getRfq().getId(), e.getId(), e.getSellerId(), e.getSellerName(), e.getBuyerId(), e.getBuyerName());
            }
            case PLATFORM -> throw BusinessException.badRequest("Use grievance for platform issues");
        };
    }

    private void fill(QueryTicket ticket, AuthUser raiser, String subject, String body, ProcEnums.Priority priority) {
        ticket.setRaisedById(raiser.userId());
        ticket.setRaisedByRole(raiser.role().name());
        ticket.setRaisedByName(raiser.name());
        ticket.setSubject(subject.trim());
        ticket.setDescription(body);
        ticket.setPriority(priority);
        LocalDateTime now = LocalDateTime.now();
        ticket.setDueAt(switch (priority) {
            case HIGH -> now.plusDays(1);
            case MEDIUM -> now.plusDays(3);
            case LOW -> now.plusDays(5);
        });
        ticket.setLastActivityAt(now);
        ticket.setLastActorRole(raiser.role().name());
    }

    private void message(QueryTicket ticket, AuthUser sender, String body, boolean system) {
        QueryMessage message = new QueryMessage();
        message.setTicket(ticket);
        message.setSenderId(sender.userId());
        message.setSenderRole(sender.role().name());
        message.setSenderName(sender.name());
        message.setBody(body);
        message.setSystemGenerated(system);
        ticket.getMessages().add(message);
        ticket.setLastActivityAt(LocalDateTime.now());
        if (!system) {
            ticket.setLastActorRole(sender.role().name());
        }
    }

    private void system(QueryTicket ticket, AuthUser actor, String text) {
        message(ticket, actor, text, true);
    }

    private QueryTicket accessible(Long id, AuthUser user) {
        QueryTicket ticket = ticketRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Query not found"));
        if (user.isAdmin()) {
            guard.admin(Permission.GRIEVANCE_MANAGE);
            return ticket;
        }
        if (!user.userId().equals(ticket.getSellerId()) && !user.userId().equals(ticket.getBuyerId())
                && !user.userId().equals(ticket.getRaisedById())) {
            throw BusinessException.notFound("Query not found");
        }
        return ticket;
    }

    private QueryDtos.TicketDetail detailOf(QueryTicket ticket, AuthUser user) {
        boolean party = !user.isAdmin();
        boolean open = TicketStatus.UNRESOLVED.contains(ticket.getStatus());
        boolean raiser = user.userId().equals(ticket.getRaisedById());
        boolean adminCanAct = user.isAdmin() && (ticket.isEscalated() || ticket.getTicketType() == ProcEnums.TicketType.GRIEVANCE);
        return new QueryDtos.TicketDetail(
                mapper.ticket(ticket, user.userId(), user.role().name()),
                ticket.getDescription(),
                ticket.getEscalationReason(),
                ticket.getResolutionNote(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                ticket.getMessages().stream().map(mapper::message).toList(),
                documentService.list(ProcEnums.OwnerType.QUERY, ticket.getId()),
                ticket.getStatus() != TicketStatus.CLOSED && (party || adminCanAct),
                open && (raiser || adminCanAct),
                open && party && !ticket.isEscalated() && ticket.getTicketType() == ProcEnums.TicketType.QUERY,
                ticket.getStatus() == TicketStatus.RESOLVED && party,
                ticket.getStatus() != TicketStatus.CLOSED && ((raiser && ticket.getStatus() == TicketStatus.RESOLVED) || adminCanAct));
    }

    private Long counterparty(QueryTicket ticket, Long actorId) {
        if (actorId.equals(ticket.getSellerId())) {
            return ticket.getBuyerId();
        }
        if (actorId.equals(ticket.getBuyerId())) {
            return ticket.getSellerId();
        }
        return ticket.getRaisedById();
    }

    private List<Long> recipients(QueryTicket ticket, Long actorId) {
        Map<Long, Boolean> ids = new HashMap<>();
        for (Long id : new Long[]{ticket.getSellerId(), ticket.getBuyerId(), ticket.getRaisedById()}) {
            if (id != null && !id.equals(actorId)) {
                ids.put(id, true);
            }
        }
        return new ArrayList<>(ids.keySet());
    }

    private record Party(String reference, Long rfqId, Long engagementId, Long sellerId, String sellerName, Long buyerId, String buyerName) {
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
