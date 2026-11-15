package com.sidbi.bds.procurement.service;

import com.sidbi.bds.common.client.dto.UserSummary;
import com.sidbi.bds.procurement.domain.Engagement;
import com.sidbi.bds.procurement.domain.EngagementUpdate;
import com.sidbi.bds.procurement.domain.FeedbackQuestion;
import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.QueryMessage;
import com.sidbi.bds.procurement.domain.QueryTicket;
import com.sidbi.bds.procurement.domain.Quotation;
import com.sidbi.bds.procurement.domain.QuotationItem;
import com.sidbi.bds.procurement.domain.Review;
import com.sidbi.bds.procurement.domain.Rfq;
import com.sidbi.bds.procurement.domain.RfqInvitation;
import com.sidbi.bds.procurement.domain.WorkOrder;
import com.sidbi.bds.procurement.dto.EngagementDtos;
import com.sidbi.bds.procurement.dto.FeedbackDtos;
import com.sidbi.bds.procurement.dto.QueryDtos;
import com.sidbi.bds.procurement.dto.QuotationDtos;
import com.sidbi.bds.procurement.dto.RfqDtos;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ProcMapper {

    public RfqDtos.RfqSummary rfq(Rfq r, long quotationCount, RfqInvitation myInvitation, Quotation myQuotation) {
        return new RfqDtos.RfqSummary(
                r.getId(),
                r.getRfqNumber(),
                r.getTitle(),
                r.getCategoryId(),
                r.getCategoryName(),
                r.getSubcategoryName(),
                r.getBuyerId(),
                r.getBuyerName(),
                r.getProjectState(),
                r.getProjectDistrict(),
                r.getDeliveryMode(),
                r.getBudgetMin(),
                r.getBudgetMax(),
                r.getSubmissionDeadline(),
                r.getStatus(),
                r.getDistributionMode(),
                r.getInvitations().size(),
                quotationCount,
                r.getSubmissionDeadline().isBefore(LocalDateTime.now()),
                myInvitation != null ? myInvitation.getStatus() : null,
                myQuotation != null ? myQuotation.getStatus() : null,
                myQuotation != null ? myQuotation.getId() : null,
                r.getCreatedAt(),
                r.getPublishedAt()
        );
    }

    public RfqDtos.InvitationResponse invitation(RfqInvitation i, UserSummary seller) {
        return new RfqDtos.InvitationResponse(i.getSellerId(), i.getSellerName(), i.getStatus(), i.getInvitedBy(), i.getInvitedAt(),
                i.getViewedAt(), i.getRespondedAt(), i.getDeclineReason(), seller != null ? seller.averageRating() : null);
    }

    public RfqDtos.RfqDetail rfqDetail(RfqDtos.RfqSummary summary, Rfq r, List<RfqDtos.DocumentResponse> attachments,
                                       List<RfqDtos.InvitationResponse> invitations) {
        return new RfqDtos.RfqDetail(summary, r.getDescription(), r.getScopeOfWork(), r.getDeliverables(), r.getIndustry(),
                r.getExpectedDuration(), r.getRequiredExperience(), r.getExpectedStartDate(), r.getSourceListingId(),
                r.getSourceListingTitle(), r.getStatusRemarks(), r.getSelectedQuotationId(), attachments, invitations);
    }

    public QuotationDtos.QuotationResponse quotation(Quotation q, UserSummary seller, List<RfqDtos.DocumentResponse> attachments,
                                                     boolean editable, Long engagementId) {
        Rfq r = q.getRfq();
        return new QuotationDtos.QuotationResponse(
                q.getId(),
                q.getQuotationNumber(),
                r.getId(),
                r.getRfqNumber(),
                r.getTitle(),
                r.getStatus(),
                r.getSubmissionDeadline(),
                q.getSellerId(),
                q.getSellerName(),
                seller != null ? seller.averageRating() : null,
                seller != null ? seller.ratingCount() : null,
                q.getBuyerId(),
                r.getBuyerName(),
                q.getVersionNo(),
                q.getStatus(),
                q.getTechnicalProposal(),
                q.getMethodology(),
                q.isScopeAccepted(),
                q.getScopeDeviations(),
                q.getDeliverables(),
                q.getTimelineDays(),
                q.getTeamComposition(),
                q.getRelevantExperience(),
                q.getProfessionalFee(),
                q.getTaxPercent(),
                q.getTaxAmount(),
                q.getOtherExpenses(),
                q.getTotalAmount(),
                q.getPaymentMilestones(),
                q.getValidUntil(),
                q.getSubmittedAt(),
                q.getDecisionAt(),
                q.getDecisionRemarks(),
                q.isShortlisted(),
                editable,
                engagementId,
                q.getItems().stream().map(this::item).toList(),
                attachments,
                q.getCreatedAt(),
                q.getUpdatedAt()
        );
    }

    public QuotationDtos.ItemResponse item(QuotationItem i) {
        return new QuotationDtos.ItemResponse(i.getId(), i.getDescription(), i.getQuantity(), i.getUnit(), i.getUnitPrice(), i.getAmount());
    }

    public EngagementDtos.WorkOrderResponse workOrder(WorkOrder w, List<RfqDtos.DocumentResponse> documents) {
        Engagement e = w.getEngagement();
        return new EngagementDtos.WorkOrderResponse(w.getId(), w.getWorkOrderNumber(), e.getId(), e.getEngagementNumber(), e.getTitle(),
                w.getSellerId(), e.getSellerName(), w.getBuyerId(), e.getBuyerName(), w.getStatus(), w.getRequestRemarks(),
                w.getRequestedAt(), w.getPoReference(), w.getWorkOrderDate(), w.getAgreedValue(), w.getStartDate(), w.getEndDate(),
                w.getScopeSummary(), w.getTerms(), w.getBuyerRemarks(), w.getIssuedAt(), w.getAcknowledgedAt(), w.getDeclinedReason(),
                documents, w.getCreatedAt());
    }

    public EngagementDtos.EngagementSummary engagement(Engagement e, ProcEnums.WorkOrderStatus workOrderStatus) {
        return new EngagementDtos.EngagementSummary(e.getId(), e.getEngagementNumber(), e.getTitle(), e.getRfq().getId(),
                e.getRfq().getRfqNumber(), e.getQuotation().getId(), e.getQuotation().getQuotationNumber(), e.getSellerId(),
                e.getSellerName(), e.getBuyerId(), e.getBuyerName(), e.getQuotedValue(), e.getContractValue(), e.getStartDate(),
                e.getExpectedEndDate(), e.getStatus(), e.getProgressPercent(), e.getDeliveryConfirmed(), e.isFeedbackSubmitted(),
                workOrderStatus, e.getCreatedAt(), e.getUpdatedAt());
    }

    public EngagementDtos.UpdateResponse update(EngagementUpdate u) {
        return new EngagementDtos.UpdateResponse(u.getId(), u.getUpdateType(), u.getMessage(), u.getProgressPercent(), u.getFromStatus(),
                u.getToStatus(), u.getAuthorRole(), u.getAuthorName(), u.getCreatedAt());
    }

    public QueryDtos.TicketSummary ticket(QueryTicket t, Long viewerId, String viewerRole) {
        boolean overdue = t.getDueAt() != null && ProcEnums.TicketStatus.UNRESOLVED.contains(t.getStatus())
                && t.getDueAt().isBefore(LocalDateTime.now());
        boolean awaiting = ProcEnums.TicketStatus.UNRESOLVED.contains(t.getStatus())
                && t.getLastActorRole() != null && viewerRole != null && !viewerRole.equals(t.getLastActorRole());
        return new QueryDtos.TicketSummary(t.getId(), t.getTicketNumber(), t.getTicketType(), t.getContextType(), t.getContextId(),
                t.getContextReference(), t.getSellerId(), t.getSellerName(), t.getBuyerId(), t.getBuyerName(), t.getRaisedByRole(),
                t.getRaisedByName(), t.getSubject(), t.getPriority(), t.getStatus(), t.isEscalated(), overdue, awaiting,
                t.getMessages().size(), t.getDueAt(), t.getLastActivityAt(), t.getCreatedAt());
    }

    public QueryDtos.MessageResponse message(QueryMessage m) {
        return new QueryDtos.MessageResponse(m.getId(), m.getSenderId(), m.getSenderRole(), m.getSenderName(), m.getBody(),
                m.isSystemGenerated(), m.getCreatedAt());
    }

    public FeedbackDtos.QuestionResponse question(FeedbackQuestion q) {
        return new FeedbackDtos.QuestionResponse(q.getId(), q.getQuestionText(), q.getQuestionType(), q.getDisplayOrder(),
                q.isMandatory(), q.isActive());
    }

    public FeedbackDtos.ReviewResponse review(Review r, boolean includeAnswers) {
        return new FeedbackDtos.ReviewResponse(r.getId(), r.getEngagementId(), r.getEngagementNumber(), r.getEngagementTitle(),
                r.getSellerId(), r.getSellerName(), r.getBuyerName(), r.getRating(), r.isDelivered(), r.getTitle(), r.getReviewText(),
                r.isVisible(),
                includeAnswers ? r.getAnswers().stream().map(a -> new FeedbackDtos.AnswerResponse(a.getQuestionId(), a.getQuestionText(),
                        a.getQuestionType(), a.getRatingValue(), a.getBooleanValue(), a.getTextValue())).toList() : List.of(),
                r.getCreatedAt());
    }
}
