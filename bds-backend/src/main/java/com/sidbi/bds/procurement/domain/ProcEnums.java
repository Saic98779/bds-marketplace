package com.sidbi.bds.procurement.domain;

import java.util.EnumSet;
import java.util.Set;

public final class ProcEnums {

    private ProcEnums() {
    }

    public enum RfqStatus {
        DRAFT,
        SUBMITTED,
        PUBLISHED,
        RESPONSES_RECEIVED,
        UNDER_EVALUATION,
        PROVIDER_SELECTED,
        CLOSED,
        CANCELLED,
        EXPIRED,
        NO_RESPONSE;

        public static final Set<RfqStatus> OPEN_FOR_QUOTES = EnumSet.of(PUBLISHED, RESPONSES_RECEIVED);
        public static final Set<RfqStatus> EVALUABLE = EnumSet.of(PUBLISHED, RESPONSES_RECEIVED, UNDER_EVALUATION);
        public static final Set<RfqStatus> ACTIVE = EnumSet.of(SUBMITTED, PUBLISHED, RESPONSES_RECEIVED, UNDER_EVALUATION);
    }

    public enum DistributionMode {
        SELECTED,
        RECOMMENDED,
        ADMIN_ASSISTED
    }

    public enum InvitationStatus {
        INVITED,
        VIEWED,
        RESPONDED,
        DECLINED
    }

    public enum QuotationStatus {
        DRAFT,
        SUBMITTED,
        UNDER_REVIEW,
        CLARIFICATION_REQUESTED,
        REVISED,
        SHORTLISTED,
        ACCEPTED,
        REJECTED,
        WITHDRAWN,
        EXPIRED;

        public static final Set<QuotationStatus> LIVE = EnumSet.of(SUBMITTED, UNDER_REVIEW, CLARIFICATION_REQUESTED, REVISED, SHORTLISTED);
        public static final Set<QuotationStatus> VISIBLE_TO_BUYER = EnumSet.of(SUBMITTED, UNDER_REVIEW, CLARIFICATION_REQUESTED,
                REVISED, SHORTLISTED, ACCEPTED, REJECTED, EXPIRED);
    }

    public enum WorkOrderStatus {
        REQUESTED,
        ISSUED,
        ACKNOWLEDGED,
        DECLINED
    }

    public enum EngagementStatus {
        SELECTED,
        ENGAGEMENT_INITIATED,
        IN_PROGRESS,
        DELIVERED,
        COMPLETED,
        CANCELLED,
        DISPUTED;

        public static final Set<EngagementStatus> ACTIVE = EnumSet.of(SELECTED, ENGAGEMENT_INITIATED, IN_PROGRESS, DELIVERED, DISPUTED);
    }

    public enum UpdateType {
        STATUS_CHANGE,
        PROGRESS,
        NOTE,
        DELIVERY,
        DOCUMENT
    }

    public enum ContextType {
        RFQ,
        QUOTATION,
        WORK_ORDER,
        ENGAGEMENT,
        PLATFORM
    }

    public enum TicketType {
        QUERY,
        GRIEVANCE
    }

    public enum TicketStatus {
        OPEN,
        RESPONDED,
        RESOLVED,
        REOPENED,
        ESCALATED,
        CLOSED;

        public static final Set<TicketStatus> UNRESOLVED = EnumSet.of(OPEN, RESPONDED, REOPENED, ESCALATED);
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum QuestionType {
        RATING,
        YES_NO,
        TEXT
    }

    public enum OwnerType {
        RFQ,
        QUOTATION,
        WORK_ORDER,
        ENGAGEMENT,
        QUERY
    }
}
