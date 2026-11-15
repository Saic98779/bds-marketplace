package com.sidbi.bds.auth.domain;

public final class Enums {

    private Enums() {
    }

    public enum UserStatus {
        PENDING_VERIFICATION,
        INFO_REQUESTED,
        ACTIVE,
        SUSPENDED,
        REJECTED,
        DEACTIVATED
    }

    public enum Constitution {
        PROPRIETORSHIP,
        PARTNERSHIP,
        LLP,
        PRIVATE_LIMITED,
        PUBLIC_LIMITED,
        SOCIETY_TRUST_SECTION8,
        INDIVIDUAL_PROFESSIONAL,
        OTHER
    }

    public enum ProviderType {
        CONSULTANCY_FIRM,
        INDIVIDUAL_CONSULTANT,
        CHARTERED_ACCOUNTANT_FIRM,
        LEGAL_FIRM,
        TECHNOLOGY_COMPANY,
        MARKETING_AGENCY,
        TRAINING_INSTITUTION,
        CERTIFICATION_BODY,
        OTHER
    }

    public enum PurchaserType {
        MICRO,
        SMALL,
        MEDIUM,
        STARTUP,
        INDUSTRY_ASSOCIATION,
        LARGE_ENTERPRISE,
        INSTITUTIONAL,
        OTHER
    }

    public enum DocumentType {
        PAN,
        REGISTRATION_CERTIFICATE,
        GST_CERTIFICATE,
        UDYAM_CERTIFICATE,
        PROFESSIONAL_CREDENTIAL,
        PORTFOLIO,
        OTHER
    }

    public enum VerificationAction {
        SUBMITTED,
        APPROVE,
        REJECT,
        REQUEST_INFO,
        SUSPEND,
        REACTIVATE,
        REVERIFICATION_TRIGGERED,
        PRIVILEGES_UPDATED,
        CREATED_BY_ADMIN
    }

    public enum OtpPurpose {
        REGISTRATION,
        LOGIN,
        PASSWORD_RESET
    }
}
