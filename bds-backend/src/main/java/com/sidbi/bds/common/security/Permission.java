package com.sidbi.bds.common.security;

import java.util.EnumSet;
import java.util.Set;

public enum Permission {
    ADMIN_DASHBOARD(Role.ADMIN, "View administrator dashboard"),
    SELLER_VERIFY(Role.ADMIN, "Verify, approve and suspend sellers"),
    BUYER_MANAGE(Role.ADMIN, "Manage purchaser accounts"),
    USER_ADMIN(Role.ADMIN, "Create users and manage privileges"),
    CATALOG_MANAGE(Role.ADMIN, "Manage categories and taxonomy"),
    LISTING_MODERATE(Role.ADMIN, "Moderate service listings"),
    RFQ_MONITOR(Role.ADMIN, "Monitor RFQs, quotations and engagements"),
    GRIEVANCE_MANAGE(Role.ADMIN, "Handle escalations and grievances"),
    FEEDBACK_MANAGE(Role.ADMIN, "Manage feedback questions and reviews"),
    REPORT_VIEW(Role.ADMIN, "View and export MIS reports"),
    AUDIT_VIEW(Role.ADMIN, "View audit logs"),
    SETTINGS_MANAGE(Role.ADMIN, "Manage templates and platform settings"),
    LISTING_PUBLISH(Role.SELLER, "Create and publish service listings"),
    RFQ_RESPOND(Role.SELLER, "View RFQs and submit quotations"),
    ENGAGEMENT_MANAGE(Role.SELLER, "Manage work orders and engagements"),
    RFQ_CREATE(Role.BUYER, "Create and publish RFQs"),
    QUOTATION_EVALUATE(Role.BUYER, "Compare and accept quotations"),
    FEEDBACK_SUBMIT(Role.BUYER, "Submit ratings and feedback");

    private final Role role;
    private final String description;

    Permission(Role role, String description) {
        this.role = role;
        this.description = description;
    }

    public Role getRole() {
        return role;
    }

    public String getDescription() {
        return description;
    }

    public String authority() {
        return "PERM_" + name();
    }

    public static Set<Permission> defaultsFor(Role role) {
        Set<Permission> result = EnumSet.noneOf(Permission.class);
        for (Permission p : values()) {
            if (p.role == role) {
                result.add(p);
            }
        }
        return result;
    }
}
