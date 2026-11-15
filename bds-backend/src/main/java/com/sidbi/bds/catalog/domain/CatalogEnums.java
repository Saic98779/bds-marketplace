package com.sidbi.bds.catalog.domain;

public final class CatalogEnums {

    private CatalogEnums() {
    }

    public enum ListingStatus {
        DRAFT,
        SUBMITTED,
        UNDER_REVIEW,
        CORRECTION_REQUESTED,
        APPROVED,
        REJECTED,
        DEACTIVATED,
        FLAGGED
    }

    public enum DeliveryMode {
        ONLINE,
        ONSITE,
        HYBRID
    }

    public enum PricingModel {
        FIXED_FEE,
        PER_DAY,
        PER_MONTH,
        PER_ASSIGNMENT,
        HOURLY,
        CUSTOM_QUOTATION
    }

    public enum ModerationAction {
        CREATED,
        SUBMITTED,
        REVIEW_STARTED,
        APPROVED,
        REJECTED,
        CORRECTION_REQUESTED,
        FLAGGED,
        DEACTIVATED,
        REACTIVATED,
        UPDATED
    }
}
