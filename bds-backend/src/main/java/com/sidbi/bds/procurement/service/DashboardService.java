package com.sidbi.bds.procurement.service;

import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.client.dto.UserSummary;
import com.sidbi.bds.common.report.ReportExporter;
import com.sidbi.bds.common.report.ReportFormat;
import com.sidbi.bds.common.report.ReportTable;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.procurement.domain.Engagement;
import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.ProcEnums.EngagementStatus;
import com.sidbi.bds.procurement.domain.ProcEnums.QuotationStatus;
import com.sidbi.bds.procurement.domain.ProcEnums.RfqStatus;
import com.sidbi.bds.procurement.domain.QueryTicket;
import com.sidbi.bds.procurement.domain.Quotation;
import com.sidbi.bds.procurement.domain.Rfq;
import com.sidbi.bds.procurement.dto.DashboardDtos;
import com.sidbi.bds.procurement.dto.RfqDtos;
import com.sidbi.bds.procurement.repository.EngagementRepository;
import com.sidbi.bds.procurement.repository.QueryTicketRepository;
import com.sidbi.bds.procurement.repository.QuotationRepository;
import com.sidbi.bds.procurement.repository.RfqInvitationRepository;
import com.sidbi.bds.procurement.repository.RfqRepository;
import com.sidbi.bds.procurement.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yy");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final EnumSet<QuotationStatus> SUBMITTED = EnumSet.of(QuotationStatus.SUBMITTED, QuotationStatus.UNDER_REVIEW,
            QuotationStatus.CLARIFICATION_REQUESTED, QuotationStatus.REVISED, QuotationStatus.SHORTLISTED, QuotationStatus.ACCEPTED,
            QuotationStatus.REJECTED, QuotationStatus.EXPIRED);

    private final RfqRepository rfqRepository;
    private final RfqInvitationRepository invitationRepository;
    private final QuotationRepository quotationRepository;
    private final EngagementRepository engagementRepository;
    private final WorkOrderRepository workOrderRepository;
    private final QueryTicketRepository ticketRepository;
    private final RfqService rfqService;
    private final QuotationService quotationService;
    private final EngagementService engagementService;
    private final ReportExporter exporter;
    private final NotificationClient notificationClient;
    private final ActorGuard guard;
    private final ProcMapper mapper;

    @Transactional(readOnly = true)
    public DashboardDtos.SellerDashboard seller(Long id) {
        long invitations = invitationRepository.countBySellerId(id);
        long responded = invitationRepository.countBySellerIdAndStatus(id, ProcEnums.InvitationStatus.RESPONDED);
        long submitted = quotationRepository.countBySellerIdAndStatusIn(id, SUBMITTED);
        long accepted = quotationRepository.countBySellerIdAndStatusIn(id, EnumSet.of(QuotationStatus.ACCEPTED));
        long pending = invitationRepository.countPending(id, EnumSet.of(ProcEnums.InvitationStatus.INVITED, ProcEnums.InvitationStatus.VIEWED),
                RfqStatus.OPEN_FOR_QUOTES, LocalDateTime.now());
        UserSummary summary = guard.summaries(List.of(id)).get(id);
        List<DashboardDtos.TrendPoint> trend = monthly(invitationRepository.invitedSince(id, YearMonth.now().minusMonths(5).atDay(1).atStartOfDay()), 6);
        return new DashboardDtos.SellerDashboard(
                pending,
                invitations,
                submitted,
                accepted,
                engagementRepository.countBySellerIdAndStatusIn(id, EngagementStatus.ACTIVE),
                engagementRepository.countBySellerIdAndStatusIn(id, EnumSet.of(EngagementStatus.COMPLETED)),
                workOrderRepository.countBySellerIdAndStatus(id, ProcEnums.WorkOrderStatus.REQUESTED),
                ticketRepository.countOpenForUser(id, ProcEnums.TicketStatus.UNRESOLVED),
                summary != null ? summary.averageRating() : null,
                summary != null ? summary.ratingCount() : null,
                invitations == 0 ? 0 : round(responded * 100.0 / invitations),
                submitted == 0 ? 0 : round(accepted * 100.0 / submitted),
                trend,
                rfqService.sellerInbox(null, null, null, 0, 5).content(),
                quotationService.sellerList(null, null, 0, 5).content(),
                engagementService.list(null, null, 0, 5).content());
    }

    @Transactional(readOnly = true)
    public DashboardDtos.BuyerDashboard buyer(Long id) {
        List<Rfq> recent = rfqRepository.findTop5ByBuyerIdOrderByCreatedAtDesc(id);
        Map<Long, Long> counts = rfqService.quotationCounts(recent.stream().map(Rfq::getId).toList());
        List<RfqDtos.RfqSummary> recentRfqs = recent.stream().map(r -> mapper.rfq(r, counts.getOrDefault(r.getId(), 0L), null, null)).toList();
        long comparisonPending = rfqRepository.countByBuyerIdAndStatusIn(id, EnumSet.of(RfqStatus.RESPONSES_RECEIVED, RfqStatus.UNDER_EVALUATION));
        return new DashboardDtos.BuyerDashboard(
                rfqRepository.countByBuyerId(id),
                rfqRepository.countByBuyerIdAndStatusIn(id, RfqStatus.ACTIVE),
                quotationRepository.countByBuyerIdAndStatusIn(id, SUBMITTED),
                comparisonPending,
                quotationRepository.countByBuyerIdAndStatusIn(id, EnumSet.of(QuotationStatus.ACCEPTED)),
                engagementRepository.countByBuyerIdAndStatusIn(id, EngagementStatus.ACTIVE),
                engagementRepository.countByBuyerIdAndStatusIn(id, EnumSet.of(EngagementStatus.COMPLETED)),
                workOrderRepository.countByBuyerIdAndStatus(id, ProcEnums.WorkOrderStatus.REQUESTED),
                engagementRepository.countByBuyerIdAndStatusIn(id, EnumSet.of(EngagementStatus.DELIVERED)),
                engagementRepository.countByBuyerIdAndStatusAndFeedbackSubmittedFalse(id, EngagementStatus.COMPLETED),
                ticketRepository.countOpenForUser(id, ProcEnums.TicketStatus.UNRESOLVED),
                recentRfqs,
                engagementService.list(null, null, 0, 5).content());
    }

    @Transactional(readOnly = true)
    public DashboardDtos.ProcurementStats stats() {
        guard.admin(Permission.ADMIN_DASHBOARD);
        Map<String, Long> rfqs = grouped(rfqRepository.countGroupedByStatus(), RfqStatus.values());
        Map<String, Long> quotations = grouped(quotationRepository.countGroupedByStatus(), QuotationStatus.values());
        Map<String, Long> engagements = grouped(engagementRepository.countGroupedByStatus(), EngagementStatus.values());
        long totalRfqs = rfqs.values().stream().mapToLong(Long::longValue).sum();
        long totalQuotations = quotations.values().stream().mapToLong(Long::longValue).sum() - quotations.get("DRAFT");
        long totalEngagements = engagements.values().stream().mapToLong(Long::longValue).sum();
        long decided = rfqs.get("PROVIDER_SELECTED") + rfqs.get("CLOSED");
        long published = totalRfqs - rfqs.get("DRAFT") - rfqs.get("SUBMITTED");
        long active = rfqs.get("SUBMITTED") + rfqs.get("PUBLISHED") + rfqs.get("RESPONSES_RECEIVED") + rfqs.get("UNDER_EVALUATION");
        long activeEngagements = engagements.get("SELECTED") + engagements.get("ENGAGEMENT_INITIATED") + engagements.get("IN_PROGRESS")
                + engagements.get("DELIVERED") + engagements.get("DISPUTED");
        YearMonth start = YearMonth.now().minusMonths(11);
        LocalDateTime from = start.atDay(1).atStartOfDay();
        Map<YearMonth, long[]> trend = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            trend.put(start.plusMonths(i), new long[3]);
        }
        bucket(trend, rfqRepository.createdSince(from), 0);
        bucket(trend, quotationRepository.submittedSince(from), 1);
        bucket(trend, engagementRepository.createdSince(from), 2);
        List<DashboardDtos.MultiTrendPoint> monthly = new ArrayList<>();
        trend.forEach((m, v) -> monthly.add(new DashboardDtos.MultiTrendPoint(m.format(MONTH), v[0], v[1], v[2])));
        return new DashboardDtos.ProcurementStats(
                totalRfqs,
                active,
                totalQuotations,
                totalEngagements,
                activeEngagements,
                engagements.get("COMPLETED"),
                published == 0 ? 0 : round(decided * 100.0 / published),
                published == 0 ? 0 : round(totalQuotations * 1.0 / published),
                ticketRepository.countByTicketTypeAndStatusIn(ProcEnums.TicketType.GRIEVANCE, ProcEnums.TicketStatus.UNRESOLVED),
                ticketRepository.countByEscalatedTrueAndStatusIn(ProcEnums.TicketStatus.UNRESOLVED),
                ticketRepository.countByStatusInAndDueAtBefore(ProcEnums.TicketStatus.UNRESOLVED, LocalDateTime.now()),
                rfqs,
                quotations,
                engagements,
                counts(rfqRepository.countByCategory()),
                counts(rfqRepository.countByState()),
                monthly);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> rfqReport(RfqStatus status, Long categoryId, ReportFormat format) {
        guard.admin(Permission.REPORT_VIEW);
        List<Rfq> rfqs = rfqRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(r -> status == null || r.getStatus() == status)
                .filter(r -> categoryId == null || categoryId.equals(r.getCategoryId()))
                .toList();
        Map<Long, Long> counts = rfqService.quotationCounts(rfqs.stream().map(Rfq::getId).toList());
        ReportTable table = new ReportTable("RFQ Report", "RFQ No.", "Title", "Category", "Purchaser", "State", "Distribution",
                "Invited", "Quotations", "Deadline", "Status", "Created On");
        for (Rfq r : rfqs) {
            table.row(r.getRfqNumber(), r.getTitle(), r.getCategoryName(), r.getBuyerName(), r.getProjectState(), r.getDistributionMode(),
                    r.getInvitations().size(), counts.getOrDefault(r.getId(), 0L), r.getSubmissionDeadline().format(DATE), r.getStatus(),
                    r.getCreatedAt() == null ? "" : r.getCreatedAt().format(DATE));
        }
        notificationClient.audit("REPORT_EXPORTED", "REPORT", "rfqs", format.name());
        return exporter.export(table, format, "rfq-report");
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> quotationReport(QuotationStatus status, ReportFormat format) {
        guard.admin(Permission.REPORT_VIEW);
        ReportTable table = new ReportTable("Quotation Response & Selection Report", "Quotation No.", "RFQ No.", "Requirement",
                "Provider", "Version", "Total (INR)", "Timeline (days)", "Valid Until", "Status", "Submitted On");
        for (Quotation q : quotationRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"))) {
            if (q.getStatus() == QuotationStatus.DRAFT || (status != null && q.getStatus() != status)) {
                continue;
            }
            table.row(q.getQuotationNumber(), q.getRfq().getRfqNumber(), q.getRfq().getTitle(), q.getSellerName(), q.getVersionNo(),
                    q.getTotalAmount(), q.getTimelineDays(), q.getValidUntil() == null ? "" : q.getValidUntil().format(DATE), q.getStatus(),
                    q.getSubmittedAt() == null ? "" : q.getSubmittedAt().format(DATE));
        }
        notificationClient.audit("REPORT_EXPORTED", "REPORT", "quotations", format.name());
        return exporter.export(table, format, "quotation-report");
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> engagementReport(EngagementStatus status, ReportFormat format) {
        guard.admin(Permission.REPORT_VIEW);
        ReportTable table = new ReportTable("Engagement & Performance Report", "Engagement No.", "Title", "Provider", "Purchaser",
                "Quoted (INR)", "Declared Contract Value (INR)", "Start", "Expected End", "Progress %", "Status", "Delivery Confirmed", "Feedback");
        for (Engagement e : engagementRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"))) {
            if (status != null && e.getStatus() != status) {
                continue;
            }
            table.row(e.getEngagementNumber(), e.getTitle(), e.getSellerName(), e.getBuyerName(), e.getQuotedValue(), e.getContractValue(),
                    e.getStartDate() == null ? "" : e.getStartDate().format(DATE), e.getExpectedEndDate() == null ? "" : e.getExpectedEndDate().format(DATE),
                    e.getProgressPercent(), e.getStatus(), e.getDeliveryConfirmed() == null ? "-" : e.getDeliveryConfirmed() ? "Yes" : "No",
                    e.isFeedbackSubmitted() ? "Submitted" : "Pending");
        }
        notificationClient.audit("REPORT_EXPORTED", "REPORT", "engagements", format.name());
        return exporter.export(table, format, "engagement-report");
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> grievanceReport(ReportFormat format) {
        guard.admin(Permission.REPORT_VIEW);
        ReportTable table = new ReportTable("Grievance & Escalation Report", "Ticket", "Type", "Context", "Subject", "Raised By", "Role",
                "Priority", "Status", "Escalated", "Created On", "Resolved On");
        for (QueryTicket t : ticketRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))) {
            if (t.getTicketType() != ProcEnums.TicketType.GRIEVANCE && !t.isEscalated()) {
                continue;
            }
            table.row(t.getTicketNumber(), t.getTicketType(), t.getContextReference() == null ? t.getContextType() : t.getContextReference(),
                    t.getSubject(), t.getRaisedByName(), t.getRaisedByRole(), t.getPriority(), t.getStatus(), t.isEscalated() ? "Yes" : "No",
                    t.getCreatedAt() == null ? "" : t.getCreatedAt().format(DATE), t.getResolvedAt() == null ? "" : t.getResolvedAt().format(DATE));
        }
        notificationClient.audit("REPORT_EXPORTED", "REPORT", "grievances", format.name());
        return exporter.export(table, format, "grievance-report");
    }

    private static List<DashboardDtos.TrendPoint> monthly(List<LocalDateTime> dates, int months) {
        YearMonth start = YearMonth.now().minusMonths(months - 1L);
        Map<YearMonth, Long> buckets = new LinkedHashMap<>();
        for (int i = 0; i < months; i++) {
            buckets.put(start.plusMonths(i), 0L);
        }
        for (LocalDateTime date : dates) {
            buckets.computeIfPresent(YearMonth.from(date), (k, v) -> v + 1);
        }
        List<DashboardDtos.TrendPoint> result = new ArrayList<>();
        buckets.forEach((k, v) -> result.add(new DashboardDtos.TrendPoint(k.format(MONTH), v)));
        return result;
    }

    private static void bucket(Map<YearMonth, long[]> trend, List<LocalDateTime> dates, int index) {
        for (LocalDateTime date : dates) {
            if (date != null) {
                long[] values = trend.get(YearMonth.from(date));
                if (values != null) {
                    values[index]++;
                }
            }
        }
    }

    private static <E extends Enum<E>> Map<String, Long> grouped(List<Object[]> rows, E[] values) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (E value : values) {
            map.put(value.name(), 0L);
        }
        for (Object[] row : rows) {
            map.put(((Enum<?>) row[0]).name(), ((Number) row[1]).longValue());
        }
        return map;
    }

    private static List<DashboardDtos.CountItem> counts(List<Object[]> rows) {
        return rows.stream().limit(12)
                .map(r -> new DashboardDtos.CountItem(r[0] == null ? "Unspecified" : String.valueOf(r[0]), ((Number) r[1]).longValue()))
                .toList();
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
