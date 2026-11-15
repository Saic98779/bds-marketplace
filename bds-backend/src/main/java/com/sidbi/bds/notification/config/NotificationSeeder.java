package com.sidbi.bds.notification.config;

import com.sidbi.bds.notification.domain.NotificationTemplate;
import com.sidbi.bds.notification.domain.PlatformSetting;
import com.sidbi.bds.notification.repository.NotificationTemplateRepository;
import com.sidbi.bds.notification.repository.PlatformSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationSeeder implements ApplicationRunner {

    private final NotificationTemplateRepository templateRepository;
    private final PlatformSettingRepository settingRepository;

    private record T(String code, String category, String severity, String title, String message, boolean email) {
    }

    private record S(String key, String value, String group, String description, boolean publicSetting) {
    }

    private static final List<T> TEMPLATES = List.of(
            new T("SELLER_REGISTERED", "REGISTRATION", "SUCCESS", "Application {{reference}} received",
                    "Dear {{name}}, your service provider application for {{organisation}} has been received and is pending verification. Track the status from your dashboard.", true),
            new T("NEW_SELLER_APPLICATION", "VERIFICATION", "ACTION", "New seller application {{reference}}",
                    "{{organisation}} has submitted a service provider application for verification.", false),
            new T("BUYER_REGISTERED", "REGISTRATION", "SUCCESS", "Welcome to BDS Marketplace",
                    "Dear {{name}}, the purchaser account for {{organisation}} has been registered with reference {{reference}}.", true),
            new T("NEW_BUYER_APPLICATION", "VERIFICATION", "ACTION", "New purchaser registration {{reference}}",
                    "{{organisation}} has registered as a purchaser and is awaiting account verification.", false),
            new T("ACCOUNT_CREATED_BY_ADMIN", "REGISTRATION", "INFO", "Your account has been created",
                    "Dear {{name}}, an administrator has created your BDS Marketplace account ({{reference}}). Please sign in and change your temporary password.", false),
            new T("ACCOUNT_APPROVED", "VERIFICATION", "SUCCESS", "Your account has been approved",
                    "Dear {{name}}, your account {{reference}} has been verified and activated. {{remarks}}", true),
            new T("ACCOUNT_REJECTED", "VERIFICATION", "WARNING", "Application not approved",
                    "Dear {{name}}, your application {{reference}} could not be approved. Reason: {{remarks}}", true),
            new T("ACCOUNT_INFO_REQUESTED", "VERIFICATION", "ACTION", "Additional information required",
                    "Dear {{name}}, the verification team needs more information on application {{reference}}: {{remarks}}. Update your profile or documents and resubmit.", true),
            new T("ACCOUNT_SUSPENDED", "VERIFICATION", "WARNING", "Account suspended",
                    "Dear {{name}}, your account {{reference}} has been suspended. Reason: {{remarks}}. New publications and quotations are restricted.", true),
            new T("ACCOUNT_REACTIVATED", "VERIFICATION", "SUCCESS", "Account reactivated",
                    "Dear {{name}}, your account {{reference}} is active again. {{remarks}}", true),
            new T("PRIVILEGES_UPDATED", "VERIFICATION", "INFO", "Your access privileges were updated",
                    "Dear {{name}}, your platform privileges are now: {{privileges}}.", false),
            new T("PASSWORD_CHANGED", "SECURITY", "INFO", "Password changed",
                    "Dear {{name}}, the password for your BDS Marketplace account was changed. If this was not you, contact support immediately.", true),
            new T("LISTING_SUBMITTED", "LISTING", "ACTION", "Service listing awaiting moderation",
                    "{{seller}} submitted \"{{title}}\" ({{reference}}) for approval.", false),
            new T("LISTING_APPROVED", "LISTING", "SUCCESS", "Listing published",
                    "Your service \"{{title}}\" ({{reference}}) has been approved and is now visible in the public catalogue.", true),
            new T("LISTING_REJECTED", "LISTING", "WARNING", "Listing not approved",
                    "Your service \"{{title}}\" ({{reference}}) was not approved. Reason: {{remarks}}", true),
            new T("LISTING_CORRECTION", "LISTING", "ACTION", "Corrections requested on your listing",
                    "Please update \"{{title}}\" ({{reference}}) and resubmit: {{remarks}}", true),
            new T("LISTING_DEACTIVATED", "LISTING", "WARNING", "Listing hidden from catalogue",
                    "Your service \"{{title}}\" ({{reference}}) has been removed from public view. Reason: {{remarks}}", true),
            new T("RFQ_PUBLISHED", "RFQ", "SUCCESS", "RFQ {{rfq}} published",
                    "Your requirement \"{{title}}\" has been shared with {{count}} verified provider(s).", true),
            new T("RFQ_SUBMITTED", "RFQ", "INFO", "RFQ {{rfq}} submitted",
                    "Your requirement \"{{title}}\" has been submitted. The BDS team will match suitable verified providers.", true),
            new T("RFQ_ADMIN_ASSIST", "RFQ", "ACTION", "Provider matching requested for {{rfq}}",
                    "{{buyer}} requested administrator-assisted matching for \"{{title}}\".", false),
            new T("RFQ_INVITATION", "RFQ", "ACTION", "New business opportunity: {{rfq}}",
                    "{{buyer}} has invited you to quote for \"{{title}}\". Submission deadline: {{deadline}}.", true),
            new T("RFQ_DEADLINE_REMINDER", "RFQ", "WARNING", "Quotation deadline approaching for {{rfq}}",
                    "Submit your quotation for \"{{title}}\" before {{deadline}}.", true),
            new T("RFQ_DEADLINE_EXTENDED", "RFQ", "INFO", "Deadline extended for {{rfq}}",
                    "The submission deadline for \"{{title}}\" is now {{deadline}}.", true),
            new T("RFQ_CANCELLED", "RFQ", "WARNING", "RFQ {{rfq}} cancelled",
                    "The purchaser has cancelled \"{{title}}\". Reason: {{reason}}", true),
            new T("RFQ_DECLINED", "RFQ", "INFO", "Provider declined {{rfq}}",
                    "{{seller}} declined to quote for \"{{title}}\". Reason: {{reason}}", false),
            new T("RFQ_DEADLINE_CLOSED", "RFQ", "ACTION", "Quotations ready for evaluation - {{rfq}}",
                    "The deadline for \"{{title}}\" has passed with {{count}} quotation(s). Compare and select a provider.", true),
            new T("RFQ_NO_RESPONSE", "RFQ", "WARNING", "No quotations received - {{rfq}}",
                    "No provider responded to \"{{title}}\" before the deadline. Extend the deadline or invite more providers.", true),
            new T("QUOTATION_RECEIVED", "QUOTATION", "ACTION", "New quotation received for {{rfq}}",
                    "{{seller}} submitted quotation {{quotation}} for \"{{title}}\".", true),
            new T("QUOTATION_REVISED", "QUOTATION", "INFO", "Quotation {{quotation}} revised",
                    "{{seller}} submitted version {{version}} of quotation {{quotation}} for {{rfq}}.", true),
            new T("QUOTATION_WITHDRAWN", "QUOTATION", "WARNING", "Quotation withdrawn",
                    "{{seller}} withdrew quotation {{quotation}} for {{rfq}}. {{reason}}", false),
            new T("QUOTATION_SHORTLISTED", "QUOTATION", "SUCCESS", "Your quotation has been shortlisted",
                    "Quotation {{quotation}} for \"{{title}}\" ({{rfq}}) has been shortlisted by the purchaser.", true),
            new T("QUOTATION_ACCEPTED", "QUOTATION", "SUCCESS", "Your quotation has been accepted",
                    "{{buyer}} selected your quotation {{quotation}} for \"{{title}}\". Engagement {{engagement}} has been created. Request the work order to proceed.", true),
            new T("QUOTATION_REJECTED", "QUOTATION", "INFO", "Quotation not selected - {{rfq}}",
                    "Your quotation for \"{{title}}\" was not selected. {{remarks}}", true),
            new T("QUOTATION_EXPIRED", "QUOTATION", "WARNING", "Quotation validity lapsed",
                    "Quotation {{quotation}} for {{rfq}} has expired.", false),
            new T("ENGAGEMENT_CREATED", "ENGAGEMENT", "SUCCESS", "Engagement {{engagement}} created",
                    "{{buyer}} and {{seller}} are now engaged for \"{{title}}\". Contract and payment terms are finalised outside the platform.", true),
            new T("WORK_ORDER_REQUESTED", "WORK_ORDER", "ACTION", "Work order requested for {{engagement}}",
                    "{{seller}} has requested a work order ({{workOrder}}) for \"{{title}}\".", true),
            new T("WORK_ORDER_ISSUED", "WORK_ORDER", "SUCCESS", "Work order issued for {{engagement}}",
                    "{{buyer}} issued work order {{workOrder}} for \"{{title}}\". You can start the engagement.", true),
            new T("WORK_ORDER_DECLINED", "WORK_ORDER", "WARNING", "Work order request declined",
                    "{{buyer}} declined work order request {{workOrder}}: {{remarks}}", true),
            new T("WORK_ORDER_ACKNOWLEDGED", "WORK_ORDER", "INFO", "Work order acknowledged",
                    "{{seller}} acknowledged work order {{workOrder}} for {{engagement}}.", false),
            new T("ENGAGEMENT_STARTED", "ENGAGEMENT", "INFO", "Work started on {{engagement}}",
                    "{{seller}} has started work on \"{{title}}\".", false),
            new T("ENGAGEMENT_PROGRESS", "ENGAGEMENT", "INFO", "Progress update on {{engagement}} ({{progress}}%)",
                    "{{seller}}: {{message}}", false),
            new T("ENGAGEMENT_DELIVERED", "ENGAGEMENT", "ACTION", "Deliverables submitted for {{engagement}}",
                    "{{seller}} has marked \"{{title}}\" as delivered. Please confirm whether the work has been delivered.", true),
            new T("DELIVERY_CONFIRMED", "ENGAGEMENT", "SUCCESS", "Delivery confirmed for {{engagement}}",
                    "{{buyer}} confirmed delivery of \"{{title}}\". The engagement is complete.", true),
            new T("DELIVERY_DISPUTED", "ENGAGEMENT", "WARNING", "Delivery not accepted for {{engagement}}",
                    "{{buyer}} did not accept the delivery: {{remarks}}. A query has been raised in the tracker.", true),
            new T("FEEDBACK_REQUESTED", "FEEDBACK", "ACTION", "Rate your experience with {{seller}}",
                    "Engagement {{engagement}} is complete. Share a star rating and feedback to help other MSMEs.", true),
            new T("FEEDBACK_RECEIVED", "FEEDBACK", "INFO", "New {{rating}}-star rating received",
                    "{{buyer}} rated engagement {{engagement}} ({{title}}) {{rating}} out of 5.", true),
            new T("ENGAGEMENT_DISPUTED", "ENGAGEMENT", "WARNING", "Dispute raised on {{engagement}}",
                    "A dispute has been raised on \"{{title}}\": {{remarks}}. Platform support has been notified.", true),
            new T("ENGAGEMENT_RESUMED", "ENGAGEMENT", "INFO", "Engagement {{engagement}} resumed",
                    "The dispute on \"{{title}}\" has been settled and the engagement has resumed.", false),
            new T("ENGAGEMENT_CANCELLED", "ENGAGEMENT", "WARNING", "Engagement {{engagement}} cancelled",
                    "\"{{title}}\" has been cancelled. Reason: {{remarks}}", true),
            new T("ENGAGEMENT_DOCUMENT", "ENGAGEMENT", "INFO", "New document on {{engagement}}",
                    "{{document}} was uploaded to \"{{title}}\".", false),
            new T("QUERY_RAISED", "QUERY", "ACTION", "New query {{ticket}}",
                    "{{raisedBy}} raised a query on {{context}}: {{subject}}", true),
            new T("QUERY_REPLIED", "QUERY", "INFO", "New reply on {{ticket}}",
                    "{{from}} replied to \"{{subject}}\".", false),
            new T("QUERY_RESOLVED", "QUERY", "SUCCESS", "Query {{ticket}} resolved",
                    "\"{{subject}}\" has been marked as resolved.", false),
            new T("QUERY_CLOSED", "QUERY", "INFO", "Query {{ticket}} closed",
                    "\"{{subject}}\" has been closed. {{remarks}}", false),
            new T("QUERY_ESCALATED", "QUERY", "ACTION", "Query escalated: {{ticket}}",
                    "{{raisedBy}} escalated \"{{subject}}\" to platform support.", true),
            new T("QUERY_ESCALATED_PARTY", "QUERY", "WARNING", "Query {{ticket}} escalated",
                    "\"{{subject}}\" has been escalated to BDS Marketplace support.", false),
            new T("GRIEVANCE_SUBMITTED", "QUERY", "SUCCESS", "Grievance {{ticket}} registered",
                    "Your grievance \"{{subject}}\" has been registered. Our support team will respond shortly.", true),
            new T("GRIEVANCE_RECEIVED", "QUERY", "ACTION", "New grievance {{ticket}}",
                    "{{raisedBy}} submitted a grievance: {{subject}}", true)
    );

    private static final List<S> SETTINGS = List.of(
            new S("platform.name", "BDS Marketplace", "GENERAL", "Platform display name", true),
            new S("platform.tagline", "Grow Your MSME with the Right Expertise", "GENERAL", "Homepage headline", true),
            new S("support.email", "support@bdsmarketplace.in", "SUPPORT", "Support email address", true),
            new S("support.phone", "1800-226-753", "SUPPORT", "Support helpline", true),
            new S("support.hours", "Mon-Sat, 9:30 am - 6:00 pm", "SUPPORT", "Support hours", true),
            new S("announcement.text", "", "CMS", "Banner announcement shown on the public website", true),
            new S("rfq.max-invites", "25", "RFQ", "Maximum providers per RFQ", false),
            new S("rfq.reminder-hours", "24", "RFQ", "Hours before deadline to send reminders", false),
            new S("documents.max-size-mb", "5", "DOCUMENTS", "Maximum upload size in MB", true),
            new S("documents.allowed-types", "pdf,jpg,jpeg,png,doc,docx,xls,xlsx", "DOCUMENTS", "Allowed file extensions", true),
            new S("verification.renewal-months", "12", "VERIFICATION", "Seller re-verification period in months", false),
            new S("notifications.sms-enabled", "false", "NOTIFICATIONS", "Enable SMS channel when a provider is configured", false),
            new S("disclaimer.payments", "BDS Marketplace does not collect, hold or process payments. Contract and payment terms are agreed directly between purchaser and provider.", "CMS", "Payment disclaimer", true)
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (T t : TEMPLATES) {
            if (!templateRepository.existsByCode(t.code())) {
                NotificationTemplate template = new NotificationTemplate();
                template.setCode(t.code());
                template.setCategory(t.category());
                template.setSeverity(t.severity());
                template.setTitle(t.title());
                template.setMessage(t.message());
                template.setEmailEnabled(t.email());
                template.setActive(true);
                templateRepository.save(template);
            }
        }
        for (S s : SETTINGS) {
            if (!settingRepository.existsByKey(s.key())) {
                PlatformSetting setting = new PlatformSetting();
                setting.setKey(s.key());
                setting.setValue(s.value());
                setting.setGroup(s.group());
                setting.setDescription(s.description());
                setting.setPublicSetting(s.publicSetting());
                settingRepository.save(setting);
            }
        }
    }
}
