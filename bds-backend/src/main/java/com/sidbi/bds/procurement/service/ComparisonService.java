package com.sidbi.bds.procurement.service;

import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.client.dto.UserSummary;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.report.ReportExporter;
import com.sidbi.bds.common.report.ReportFormat;
import com.sidbi.bds.common.report.ReportTable;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.procurement.domain.EvaluationCriterion;
import com.sidbi.bds.procurement.domain.EvaluationScore;
import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.Quotation;
import com.sidbi.bds.procurement.domain.Rfq;
import com.sidbi.bds.procurement.dto.QuotationDtos;
import com.sidbi.bds.procurement.repository.EvaluationCriterionRepository;
import com.sidbi.bds.procurement.repository.EvaluationScoreRepository;
import com.sidbi.bds.procurement.repository.QuotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ComparisonService {

    private static final String NOTE = "Lowest price is not automatically the winning criterion. Evaluate technical and commercial "
            + "factors together and record the reason for your selection.";

    private final RfqService rfqService;
    private final QuotationRepository quotationRepository;
    private final EvaluationCriterionRepository criterionRepository;
    private final EvaluationScoreRepository scoreRepository;
    private final ReportExporter exporter;
    private final NotificationClient notificationClient;
    private final ActorGuard guard;
    private final ProcMapper mapper;

    @Transactional(readOnly = true)
    public QuotationDtos.ComparisonResponse get(Long rfqId) {
        Rfq rfq = access(rfqId);
        return build(rfq);
    }

    @Transactional
    public QuotationDtos.ComparisonResponse save(Long rfqId, QuotationDtos.ComparisonRequest request) {
        guard.buyer(Permission.QUOTATION_EVALUATE);
        Rfq rfq = rfqService.ownedByBuyer(rfqId);
        int totalWeight = request.criteria().stream().mapToInt(QuotationDtos.CriterionRequest::weight).sum();
        if (totalWeight != 100) {
            throw BusinessException.badRequest("Criteria weights must add up to 100 (currently " + totalWeight + ")");
        }
        List<Long> validIds = quotationRepository.findByRfqIdAndStatusIn(rfqId, ProcEnums.QuotationStatus.VISIBLE_TO_BUYER)
                .stream().map(Quotation::getId).toList();
        scoreRepository.deleteByRfq(rfqId);
        criterionRepository.deleteByRfq(rfqId);
        List<EvaluationCriterion> criteria = new ArrayList<>();
        int order = 0;
        for (QuotationDtos.CriterionRequest c : request.criteria()) {
            EvaluationCriterion criterion = new EvaluationCriterion();
            criterion.setRfqId(rfqId);
            criterion.setName(c.name().trim());
            criterion.setWeight(c.weight());
            criterion.setDisplayOrder(order++);
            criteria.add(criterionRepository.save(criterion));
        }
        if (request.scores() != null) {
            for (QuotationDtos.ScoreRequest s : request.scores()) {
                if (!validIds.contains(s.quotationId())) {
                    throw BusinessException.badRequest("Quotation " + s.quotationId() + " is not part of this RFQ");
                }
                if (s.criterionIndex() >= criteria.size()) {
                    throw BusinessException.badRequest("Invalid criterion reference");
                }
                EvaluationScore score = new EvaluationScore();
                score.setRfqId(rfqId);
                score.setQuotationId(s.quotationId());
                score.setCriterionId(criteria.get(s.criterionIndex()).getId());
                score.setScore(s.score());
                score.setScoredBy(CurrentUser.id());
                scoreRepository.save(score);
            }
        }
        if (rfq.getStatus() == ProcEnums.RfqStatus.RESPONSES_RECEIVED) {
            rfq.setStatus(ProcEnums.RfqStatus.UNDER_EVALUATION);
        }
        notificationClient.audit("QUOTATIONS_EVALUATED", "RFQ", rfqId, criteria.size() + " criteria");
        return build(rfq);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> export(Long rfqId, ReportFormat format) {
        Rfq rfq = access(rfqId);
        QuotationDtos.ComparisonResponse comparison = build(rfq);
        List<String> headers = new ArrayList<>(List.of("Rank", "Quotation", "Provider", "Fee", "Taxes", "Other", "Total",
                "Timeline (days)", "Valid Until", "Scope"));
        comparison.criteria().forEach(c -> headers.add(c.name() + " (" + c.weight() + "%)"));
        headers.add("Weighted Score");
        ReportTable table = new ReportTable("Quotation Comparison - " + rfq.getRfqNumber() + " " + rfq.getTitle(), headers.toArray(String[]::new));
        for (QuotationDtos.ComparisonRow row : comparison.rows()) {
            List<Object> values = new ArrayList<>(List.of(
                    row.rank() == null ? "-" : row.rank(), row.quotationNumber(), row.sellerName(), row.professionalFee(),
                    row.taxAmount(), row.otherExpenses(), row.totalAmount(), row.timelineDays() == null ? "-" : row.timelineDays(),
                    row.validUntil() == null ? "-" : row.validUntil().toString(), row.scopeAccepted() ? "Accepted" : "Deviations"));
            comparison.criteria().forEach(c -> values.add(row.scores().get(c.id()) == null ? "-" : row.scores().get(c.id())));
            values.add(row.weightedScore() == null ? "-" : row.weightedScore());
            table.row(values.toArray());
        }
        notificationClient.audit("COMPARISON_EXPORTED", "RFQ", rfqId, format.name());
        return exporter.export(table, format, "comparison-" + rfq.getRfqNumber());
    }

    private Rfq access(Long rfqId) {
        if (CurrentUser.get().isAdmin()) {
            guard.admin(Permission.RFQ_MONITOR);
            return rfqService.rfq(rfqId);
        }
        if (!CurrentUser.get().isBuyer()) {
            throw BusinessException.forbidden("Only the purchaser can compare quotations");
        }
        return rfqService.ownedByBuyer(rfqId);
    }

    private QuotationDtos.ComparisonResponse build(Rfq rfq) {
        List<Quotation> quotations = quotationRepository.findByRfqIdAndStatusInOrderByTotalAmountAsc(rfq.getId(),
                ProcEnums.QuotationStatus.VISIBLE_TO_BUYER);
        List<EvaluationCriterion> criteria = criterionRepository.findByRfqIdOrderByDisplayOrderAsc(rfq.getId());
        Map<Long, Map<Long, BigDecimal>> scores = new HashMap<>();
        for (EvaluationScore s : scoreRepository.findByRfqId(rfq.getId())) {
            scores.computeIfAbsent(s.getQuotationId(), k -> new LinkedHashMap<>()).put(s.getCriterionId(), s.getScore());
        }
        Map<Long, UserSummary> sellers = guard.summaries(quotations.stream().map(Quotation::getSellerId).toList());
        BigDecimal lowest = quotations.stream().map(Quotation::getTotalAmount).filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(null);
        Integer shortest = quotations.stream().map(Quotation::getTimelineDays).filter(Objects::nonNull).min(Integer::compareTo).orElse(null);
        List<QuotationDtos.ComparisonRow> rows = new ArrayList<>();
        for (Quotation q : quotations) {
            Map<Long, BigDecimal> qScores = scores.getOrDefault(q.getId(), Map.of());
            BigDecimal weighted = null;
            if (!criteria.isEmpty() && !qScores.isEmpty()) {
                BigDecimal total = BigDecimal.ZERO;
                for (EvaluationCriterion c : criteria) {
                    BigDecimal score = qScores.getOrDefault(c.getId(), BigDecimal.ZERO);
                    total = total.add(score.multiply(BigDecimal.valueOf(c.getWeight())));
                }
                weighted = total.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            }
            UserSummary seller = sellers.get(q.getSellerId());
            rows.add(new QuotationDtos.ComparisonRow(q.getId(), q.getQuotationNumber(), q.getSellerId(), q.getSellerName(),
                    seller != null ? seller.averageRating() : null, q.getStatus(), q.isShortlisted(), q.getVersionNo(),
                    q.getProfessionalFee(), q.getTaxAmount(), q.getOtherExpenses(), q.getTotalAmount(), q.getTimelineDays(),
                    q.getValidUntil(), q.isScopeAccepted(), q.getMethodology(), q.getDeliverables(), q.getRelevantExperience(),
                    qScores, weighted, null,
                    lowest != null && q.getTotalAmount() != null && lowest.compareTo(q.getTotalAmount()) == 0,
                    shortest != null && shortest.equals(q.getTimelineDays())));
        }
        List<QuotationDtos.ComparisonRow> ranked = rank(rows);
        return new QuotationDtos.ComparisonResponse(
                mapper.rfq(rfq, quotations.size(), null, null),
                criteria.stream().map(c -> new QuotationDtos.CriterionResponse(c.getId(), c.getName(), c.getWeight())).toList(),
                ranked,
                NOTE);
    }

    private static List<QuotationDtos.ComparisonRow> rank(List<QuotationDtos.ComparisonRow> rows) {
        List<QuotationDtos.ComparisonRow> scored = rows.stream().filter(r -> r.weightedScore() != null)
                .sorted(Comparator.comparing(QuotationDtos.ComparisonRow::weightedScore).reversed()).toList();
        Map<Long, Integer> ranks = new HashMap<>();
        for (int i = 0; i < scored.size(); i++) {
            ranks.put(scored.get(i).quotationId(), i + 1);
        }
        return rows.stream().map(r -> new QuotationDtos.ComparisonRow(r.quotationId(), r.quotationNumber(), r.sellerId(), r.sellerName(),
                        r.sellerRating(), r.status(), r.shortlisted(), r.versionNo(), r.professionalFee(), r.taxAmount(), r.otherExpenses(),
                        r.totalAmount(), r.timelineDays(), r.validUntil(), r.scopeAccepted(), r.methodology(), r.deliverables(),
                        r.relevantExperience(), r.scores(), r.weightedScore(), ranks.get(r.quotationId()), r.lowestPrice(), r.shortestTimeline()))
                .sorted(Comparator.comparing((QuotationDtos.ComparisonRow r) -> r.rank() == null ? Integer.MAX_VALUE : r.rank()))
                .toList();
    }
}
